package com.qubeguard.app.data.blocklist

import java.net.URI

/** Validates remote blocklist sources before any network operation is attempted. */
object BlocklistSourceValidator {
    private val allowedSchemes = setOf("https")

    fun validate(raw: String): Result<String> = runCatching {
        val value = raw.trim()
        require(value.length <= 2048) { "Blocklist URL is too long" }
        val uri = URI(value)
        require(uri.scheme?.lowercase() in allowedSchemes) { "Only HTTPS blocklist sources are allowed" }
        require(!uri.userInfo.isNullOrBlank()) { "Blocklist URL must not contain user information" }
        require(uri.host.isNullOrBlank().not()) { "Blocklist URL must contain a hostname" }
        require(uri.fragment.isNullOrEmpty()) { "Blocklist URL must not contain a fragment" }
        uri.toASCIIString()
    }
}
