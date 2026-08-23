package com.qubeguard.app.vpn

import android.content.Context
import com.qubeguard.app.data.blocklist.BlocklistDao
import com.qubeguard.app.data.blocklist.DnsLogEntity
import com.qubeguard.app.policy.PolicyEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/** Local DNS resolver with cache, response validation, UDP/TCP fallback and upstream failover. */
class DnsProxy @Inject constructor(
    @ApplicationContext private val context: Context,
    private val policyEngine: PolicyEngine,
    private val blocklistDao: BlocklistDao,
    private val dnsCache: DnsCache
) {
    private var socket: DatagramSocket? = null
    private var worker: Thread? = null
    @Volatile private var isRunning = false
    @Volatile private var socketProtector: ((DatagramSocket) -> Boolean)? = null
    private val port = 5353

    fun setSocketProtector(protector: (DatagramSocket) -> Boolean) { socketProtector = protector }

    fun start() {
        if (isRunning) return
        runCatching {
            socket = DatagramSocket(port, InetAddress.getByName("127.0.0.1"))
            isRunning = true
            worker = Thread(::serve, "QubeGuard-DNS-Proxy").also { it.start() }
        }.onFailure {
            isRunning = false
            socket = null
        }
    }

    private fun serve() {
        while (isRunning) {
            try {
                val buffer = ByteArray(MAX_UDP_DNS_PACKET)
                val packet = DatagramPacket(buffer, buffer.size)
                socket?.receive(packet) ?: break
                val request = DnsRequest.parse(packet.data, packet.length)
                val cacheKey = "${request.domain}|${request.qType}|${request.qClass}"
                dnsCache.get(cacheKey)?.let { cached ->
                    val response = rewriteTransactionId(cached, request.id)
                    socket?.send(DatagramPacket(response, response.size, packet.address, packet.port))
                    log(request.domain, false, "DNS cache hit")
                    continue
                }

                val localRecord = runBlocking { blocklistDao.getLocalDnsRecordForDomain(request.domain) }
                if (localRecord != null) {
                    val response = DnsResponse.createIpResponse(request, localRecord.ipAddress, packet.data, packet.length)
                    socket?.send(DatagramPacket(response, response.size, packet.address, packet.port))
                    log(request.domain, false, "Local DNS (${localRecord.ipAddress})")
                    continue
                }

                val decision = runBlocking { policyEngine.decide(request.domain, isDnsRequest = true) }
                log(request.domain, decision.isBlocked, decision.reason)
                if (decision.isBlocked) {
                    val response = blockedResponse(request, packet.data, packet.length)
                    socket?.send(DatagramPacket(response, response.size, packet.address, packet.port))
                    continue
                }

                val response = forwardWithFailover(packet, request, getConditionalTargetServer(request.domain))
                if (response != null) {
                    val ttl = DnsResponse.minimumAnswerTtl(response).coerceIn(1, MAX_CACHE_TTL_SECONDS)
                    dnsCache.put(cacheKey, response, ttl)
                    socket?.send(DatagramPacket(response, response.size, packet.address, packet.port))
                }
            } catch (_: Exception) {
                if (!isRunning) break
            }
        }
    }

    private fun blockedResponse(request: DnsRequest, query: ByteArray, queryLength: Int): ByteArray = when {
        request.qType == 28 -> DnsResponse.createNoDataResponse(request, query, queryLength)
        getSinkholeMode() == "NULL_IP" || getSinkholeMode() == "0.0.0.0" -> DnsResponse.createIpResponse(request, "0.0.0.0", query, queryLength)
        getSinkholeMode() == "NODATA" -> DnsResponse.createNoDataResponse(request, query, queryLength)
        getSinkholeMode() == "REFUSED" -> DnsResponse.createRefusedResponse(request, query, queryLength)
        else -> DnsResponse.createNxDomainResponse(request, query, queryLength)
    }

    private fun rewriteTransactionId(response: ByteArray, id: Int): ByteArray {
        val copy = response.copyOf()
        if (copy.size >= 2) { copy[0] = (id ushr 8).toByte(); copy[1] = id.toByte() }
        return copy
    }

    private fun log(domain: String, blocked: Boolean, reason: String) {
        runBlocking { blocklistDao.insertDnsLog(DnsLogEntity(UUID.randomUUID().toString(), domain, blocked, reason, System.currentTimeMillis().toString())) }
    }

    private fun getSinkholeMode(): String = prefs().getString("pihole_sinkhole_mode", "NXDOMAIN") ?: "NXDOMAIN"

    private fun getConditionalTargetServer(domain: String): String? {
        if (!prefs().getBoolean("pihole_conditional_enabled", false)) return null
        val suffix = (prefs().getString("pihole_conditional_domain", "home.arpa") ?: "home.arpa").trim().trimEnd('.').lowercase()
        val normalized = domain.trim().trimEnd('.').lowercase()
        return if (normalized == suffix || normalized.endsWith(".$suffix")) prefs().getString("pihole_conditional_ip", "192.168.1.1") else null
    }

    private fun upstreams(conditional: String?): List<String> {
        if (conditional != null) return listOf(conditional)
        val configured = prefs().getString("upstream_dns_servers", null).orEmpty()
            .split(',', '\n', ' ', ';').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (configured.isNotEmpty()) return configured.take(MAX_UPSTREAMS)
        val legacy = prefs().getString("upstream_dns_ip", "1.1.1.1") ?: "1.1.1.1"
        return listOf(legacy, "1.0.0.1", "8.8.8.8").distinct()
    }

    private fun forwardWithFailover(packet: DatagramPacket, request: DnsRequest, conditional: String?): ByteArray? {
        for (server in upstreams(conditional)) {
            val response = forward(packet, request, server) ?: continue
            if (response.size <= MAX_UDP_DNS_PACKET && DnsResponse.isTruncated(response)) {
                val tcp = forwardTcp(packet.data.copyOf(packet.length), request, server)
                if (tcp != null) return tcp
            }
            return response
        }
        return null
    }

    private fun forward(packet: DatagramPacket, request: DnsRequest, dnsServerIp: String): ByteArray? = runCatching {
        DatagramSocket().use { upstream ->
            upstream.soTimeout = UPSTREAM_TIMEOUT_MS
            check(socketProtector?.invoke(upstream) == true) { "Unable to protect upstream socket" }
            val address = InetAddress.getByName(dnsServerIp)
            upstream.send(DatagramPacket(packet.data, packet.length, address, 53))
            val responseBuffer = ByteArray(MAX_UDP_DNS_PACKET)
            val response = DatagramPacket(responseBuffer, responseBuffer.size)
            upstream.receive(response)
            val result = response.data.copyOf(response.length)
            require(DnsResponse.isValidForRequest(result, request))
            result
        }
    }.getOrNull()

    private fun forwardTcp(query: ByteArray, request: DnsRequest, dnsServerIp: String): ByteArray? = runCatching {
        Socket().use { tcp ->
            tcp.soTimeout = UPSTREAM_TIMEOUT_MS
            tcp.connect(InetSocketAddress(dnsServerIp, 53), UPSTREAM_TIMEOUT_MS)
            // The socket is outside the VPN only after Android protect() is applied.
            // A TCP protect callback is not interchangeable with DatagramSocket protection,
            // so TCP fallback is enabled only when the platform adapter supplies one.
            val protector = tcpSocketProtector ?: return null
            check(protector(tcp)) { "Unable to protect upstream TCP socket" }
            val output = BufferedOutputStream(tcp.getOutputStream())
            val input = BufferedInputStream(tcp.getInputStream())
            output.write((query.size ushr 8) and 0xFF)
            output.write(query.size and 0xFF)
            output.write(query)
            output.flush()
            val hi = input.read(); val lo = input.read()
            require(hi >= 0 && lo >= 0)
            val length = (hi shl 8) or lo
            require(length in 12..MAX_TCP_DNS_MESSAGE)
            val response = ByteArray(length)
            var offset = 0
            while (offset < length) {
                val read = input.read(response, offset, length - offset)
                require(read > 0)
                offset += read
            }
            require(DnsResponse.isValidForRequest(response, request))
            response
        }
    }.getOrNull()

    private fun prefs() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun stop() {
        isRunning = false
        socket?.close(); socket = null
        worker?.interrupt(); worker = null
        dnsCache.clear()
    }

    data class DnsRequest(val id: Int, val domain: String, val qType: Int = 1, val qClass: Int = 1) {
        companion object {
            fun parse(data: ByteArray, length: Int): DnsRequest {
                require(length >= 17)
                val id = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                var position = 12; val builder = StringBuilder(); var labels = 0
                while (position < length) {
                    val labelLength = data[position++].toInt() and 0xFF
                    if (labelLength == 0) break
                    require(labelLength <= 63 && position + labelLength <= length)
                    if (builder.isNotEmpty()) builder.append('.')
                    builder.append(String(data, position, labelLength, Charsets.US_ASCII)); position += labelLength
                    require(++labels <= 127)
                }
                require(position + 4 <= length)
                val qType = ((data[position].toInt() and 0xFF) shl 8) or (data[position + 1].toInt() and 0xFF)
                val qClass = ((data[position + 2].toInt() and 0xFF) shl 8) or (data[position + 3].toInt() and 0xFF)
                require(qClass == 1 || qClass == 255)
                return DnsRequest(id, builder.toString().trimEnd('.').lowercase(), qType, qClass)
            }
        }
    }

    object DnsResponse {
        fun createNxDomainResponse(request: DnsRequest, query: ByteArray, queryLength: Int): ByteArray = headerOnly(request, query, queryLength, 0x83)
        fun createNoDataResponse(request: DnsRequest, query: ByteArray, queryLength: Int): ByteArray = headerOnly(request, query, queryLength, 0x80)
        fun createRefusedResponse(request: DnsRequest, query: ByteArray, queryLength: Int): ByteArray = headerOnly(request, query, queryLength, 0x85)

        private fun headerOnly(request: DnsRequest, query: ByteArray, queryLength: Int, flagsLow: Int): ByteArray {
            val response = query.copyOf(queryLength)
            response[0] = (request.id ushr 8).toByte(); response[1] = request.id.toByte()
            response[2] = 0x81.toByte(); response[3] = flagsLow.toByte()
            response[4] = 0; response[5] = 1; response[6] = 0; response[7] = 0; response[8] = 0; response[9] = 0; response[10] = 0; response[11] = 0
            return response
        }

        fun createIpResponse(request: DnsRequest, ipAddress: String, query: ByteArray, queryLength: Int): ByteArray {
            val rawIp = InetAddress.getByName(ipAddress).address
            if (request.qType == 28 && rawIp.size != 16) return createNoDataResponse(request, query, queryLength)
            if (request.qType != 28 && rawIp.size != 4) return createNoDataResponse(request, query, queryLength)
            val answer = ByteArray(12 + rawIp.size)
            answer[0] = 0xC0.toByte(); answer[1] = 0x0C.toByte(); answer[2] = (request.qType ushr 8).toByte(); answer[3] = request.qType.toByte()
            answer[4] = 0; answer[5] = 1; answer[6] = 0; answer[7] = 0; answer[8] = 1; answer[9] = 0x2C
            answer[10] = (rawIp.size ushr 8).toByte(); answer[11] = rawIp.size.toByte(); System.arraycopy(rawIp, 0, answer, 12, rawIp.size)
            val response = ByteArray(queryLength + answer.size); System.arraycopy(query, 0, response, 0, queryLength); System.arraycopy(answer, 0, response, queryLength, answer.size)
            response[0] = (request.id ushr 8).toByte(); response[1] = request.id.toByte(); response[2] = 0x81.toByte(); response[3] = 0x80.toByte(); response[4] = 0; response[5] = 1; response[6] = 0; response[7] = 1
            return response
        }

        fun isTruncated(message: ByteArray): Boolean = message.size >= 4 && (readU16(message, 2) and 0x0200) != 0

        fun isValidForRequest(message: ByteArray, request: DnsRequest): Boolean {
            if (message.size < 12) return false
            val id = readU16(message, 0); val flags = readU16(message, 2)
            if (id != request.id || flags and 0x8000 == 0) return false
            if (readU16(message, 4) != 1) return false
            val parsed = runCatching { DnsRequest.parse(message, message.size) }.getOrNull() ?: return false
            return parsed.domain == request.domain && parsed.qType == request.qType && (parsed.qClass == request.qClass || parsed.qClass == 1)
        }

        fun minimumAnswerTtl(message: ByteArray): Int {
            if (message.size < 12) return 1
            val answerCount = readU16(message, 6); if (answerCount == 0) return 1
            var pos = skipName(message, 12) + 4; var minTtl = Int.MAX_VALUE
            repeat(answerCount) {
                pos = skipName(message, pos); if (pos + 10 > message.size) return@repeat
                val type = readU16(message, pos); val ttl = readU32(message, pos + 4); val rdLength = readU16(message, pos + 8); pos += 10
                if (pos + rdLength > message.size) return@repeat
                if (type != 41) minTtl = minOf(minTtl, ttl); pos += rdLength
            }
            return if (minTtl == Int.MAX_VALUE) 1 else minTtl
        }

        private fun skipName(data: ByteArray, start: Int): Int {
            var pos = start; var jumps = 0
            while (pos < data.size && jumps++ < 128) {
                val len = data[pos].toInt() and 0xFF
                if (len == 0) return pos + 1
                if ((len and 0xC0) == 0xC0) return if (pos + 2 <= data.size) pos + 2 else data.size
                if (len > 63 || pos + 1 + len > data.size) return data.size
                pos += len + 1
            }
            return data.size
        }
        private fun readU16(data: ByteArray, offset: Int): Int = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
        private fun readU32(data: ByteArray, offset: Int): Int = ((data[offset].toInt() and 0xFF) shl 24) or ((data[offset + 1].toInt() and 0xFF) shl 16) or ((data[offset + 2].toInt() and 0xFF) shl 8) or (data[offset + 3].toInt() and 0xFF)
    }

    companion object {
        private const val PREFS = "qubeguard_settings"
        private const val MAX_CACHE_TTL_SECONDS = 3600
        private const val MAX_UPSTREAMS = 5
        private const val UPSTREAM_TIMEOUT_MS = 2500
        private const val MAX_UDP_DNS_PACKET = 4096
        private const val MAX_TCP_DNS_MESSAGE = 65535
        @Volatile private var tcpSocketProtector: ((Socket) -> Boolean)? = null
        fun setTcpSocketProtector(protector: (Socket) -> Boolean) { tcpSocketProtector = protector }
    }
}