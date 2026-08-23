package com.qubeguard.app.data.blocklist

import java.net.URI
import java.security.MessageDigest

/** Security policy for remote blocklist sources before download/activation. */
object BlocklistSourcePolicy {
    fun validateUrl(raw: String): Boolean = runCatching {
        val uri = URI(raw.trim())
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank() &&
            uri.userInfo == null && uri.fragment == null
    }.getOrDefault(false)

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
}
