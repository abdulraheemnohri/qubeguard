package com.qubeguard.app.vpn

import android.content.Context
import com.qubeguard.app.data.blocklist.BlocklistDao
import com.qubeguard.app.data.blocklist.DnsLogEntity
import com.qubeguard.app.policy.PolicyEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/** Local Pi-hole-style DNS resolver used behind the TUN DNS bridge. */
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
                val buffer = ByteArray(8192)
                val packet = DatagramPacket(buffer, buffer.size)
                socket?.receive(packet) ?: break
                val request = DnsRequest.parse(packet.data, packet.length)
                val cacheKey = "${request.domain}|${request.qType}|1"

                val cached = dnsCache.get(cacheKey)
                if (cached != null) {
                    val response = cached.copyOf()
                    if (response.size >= 2) {
                        response[0] = (request.id ushr 8).toByte()
                        response[1] = request.id.toByte()
                    }
                    socket?.send(DatagramPacket(response, response.size, packet.address, packet.port))
                    log(request.domain, false, "DNS cache hit")
                    continue
                }

                val localRecord = runBlocking { blocklistDao.getLocalDnsRecordForDomain(request.domain) }
                if (localRecord != null) {
                    log(request.domain, false, "Local DNS (${localRecord.ipAddress})")
                    val response = DnsResponse.createIpResponse(request, localRecord.ipAddress, packet.data, packet.length)
                    socket?.send(DatagramPacket(response, response.size, packet.address, packet.port))
                    continue
                }

                val decision = runBlocking { policyEngine.decide(request.domain, isDnsRequest = true) }
                log(request.domain, decision.isBlocked, decision.reason)

                if (decision.isBlocked) {
                    val response = when {
                        request.qType == 28 -> DnsResponse.createNoDataResponse(request, packet.data, packet.length)
                        getSinkholeMode() == "NULL_IP" || getSinkholeMode() == "0.0.0.0" -> DnsResponse.createIpResponse(request, "0.0.0.0", packet.data, packet.length)
                        getSinkholeMode() == "NODATA" -> DnsResponse.createNoDataResponse(request, packet.data, packet.length)
                        getSinkholeMode() == "REFUSED" -> DnsResponse.createRefusedResponse(request, packet.data, packet.length)
                        else -> DnsResponse.createNxDomainResponse(request, packet.data, packet.length)
                    }
                    socket?.send(DatagramPacket(response, response.size, packet.address, packet.port))
                    continue
                }

                val response = forward(packet, getConditionalTargetServer(request.domain) ?: getUpstreamDnsServer())
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

    private fun log(domain: String, blocked: Boolean, reason: String) {
        runBlocking {
            blocklistDao.insertDnsLog(DnsLogEntity(UUID.randomUUID().toString(), domain, blocked, reason, System.currentTimeMillis().toString()))
        }
    }

    private fun getSinkholeMode(): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("pihole_sinkhole_mode", "NXDOMAIN") ?: "NXDOMAIN"
    private fun getUpstreamDnsServer(): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("upstream_dns_ip", "1.1.1.1") ?: "1.1.1.1"

    private fun getConditionalTargetServer(domain: String): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("pihole_conditional_enabled", false)) return null
        val suffix = (prefs.getString("pihole_conditional_domain", "home.arpa") ?: "home.arpa").trim().trimEnd('.').lowercase()
        val normalized = domain.trim().trimEnd('.').lowercase()
        return if (normalized == suffix || normalized.endsWith(".$suffix")) prefs.getString("pihole_conditional_ip", "192.168.1.1") else null
    }

    private fun forward(packet: DatagramPacket, dnsServerIp: String): ByteArray? = runCatching {
        DatagramSocket().use { upstream ->
            upstream.soTimeout = 5000
            check(socketProtector?.invoke(upstream) != false) { "Unable to protect upstream socket" }
            val address = InetAddress.getByName(dnsServerIp)
            upstream.send(DatagramPacket(packet.data, packet.length, address, 53))
            val responseBuffer = ByteArray(8192)
            val response = DatagramPacket(responseBuffer, responseBuffer.size)
            upstream.receive(response)
            response.data.copyOf(response.length)
        }
    }.getOrNull()

    fun stop() {
        isRunning = false
        socket?.close()
        socket = null
        worker?.interrupt()
        worker = null
        dnsCache.clear()
    }

    data class DnsRequest(val id: Int, val domain: String, val qType: Int = 1) {
        companion object {
            fun parse(data: ByteArray, length: Int): DnsRequest {
                require(length >= 17) { "Packet length too short" }
                val id = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                var position = 12
                val builder = StringBuilder()
                var labels = 0
                while (position < length) {
                    val labelLength = data[position++].toInt() and 0xFF
                    if (labelLength == 0) break
                    require(labelLength <= 63 && position + labelLength <= length) { "Invalid DNS label" }
                    if (builder.isNotEmpty()) builder.append('.')
                    builder.append(String(data, position, labelLength, Charsets.US_ASCII))
                    position += labelLength
                    if (++labels > 127) error("Too many DNS labels")
                }
                require(position + 4 <= length) { "Missing DNS question fields" }
                val qType = ((data[position].toInt() and 0xFF) shl 8) or (data[position + 1].toInt() and 0xFF)
                val qClass = ((data[position + 2].toInt() and 0xFF) shl 8) or (data[position + 3].toInt() and 0xFF)
                require(qClass == 1 || qClass == 255) { "Unsupported DNS class" }
                return DnsRequest(id, builder.toString().trimEnd('.').lowercase(), qType)
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
            response[2] = 0x85.toByte(); response[3] = flagsLow.toByte()
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

        fun minimumAnswerTtl(message: ByteArray): Int {
            if (message.size < 12) return 1
            val answerCount = readU16(message, 6)
            if (answerCount == 0) return 1
            var pos = 12
            pos = skipName(message, pos) + 4
            var minTtl = Int.MAX_VALUE
            repeat(answerCount) {
                pos = skipName(message, pos)
                if (pos + 10 > message.size) return@repeat
                val type = readU16(message, pos)
                val ttl = readU32(message, pos + 4)
                val rdLength = readU16(message, pos + 8)
                pos += 10
                if (pos + rdLength > message.size) return@repeat
                if (type != 41) minTtl = minOf(minTtl, ttl)
                pos += rdLength
            }
            return if (minTtl == Int.MAX_VALUE) 1 else minTtl
        }

        private fun skipName(data: ByteArray, start: Int): Int {
            var pos = start
            var jumps = 0
            while (pos < data.size && jumps++ < 128) {
                val len = data[pos].toInt() and 0xFF
                if (len == 0) return pos + 1
                if ((len and 0xC0) == 0xC0) return pos + 2
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
    }
}
