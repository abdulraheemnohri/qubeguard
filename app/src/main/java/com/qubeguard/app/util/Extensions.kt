package com.qubeguard.app.util

import android.content.Context
import android.widget.Toast
import java.util.regex.Pattern

/**
 * Kotlin extensions for QubeGuard.
 */

// Context Extensions
fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

// String Extensions
fun String.isValidUrl(): Boolean {
    val urlPattern = Pattern.compile(
        "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
    )
    return urlPattern.matcher(this).matches()
}

fun String.extractDomain(): String {
    val normalized = this.lowercase()
        .replace("http://".toRegex(), "")
        .replace("https://".toRegex(), "")

    val slashIndex = normalized.indexOf('/')
    val domain = if (slashIndex != -1) normalized.substring(0, slashIndex) else normalized

    val colonIndex = domain.indexOf(':')
    return if (colonIndex != -1) domain.substring(0, colonIndex) else domain
}

fun String.isIpAddress(): Boolean {
    val ipv4Pattern = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    )
    return ipv4Pattern.matcher(this).matches()
}

// Collection Extensions
fun <T> List<T>.toggle(item: T, predicate: (T) -> Boolean = { it == item }): List<T> {
    return if (this.any(predicate)) {
        this.filterNot(predicate)
    } else {
        this + item
    }
}

// Int Extensions
fun Int.toHexColor(): String {
    return String.format("#%06X", this and 0xFFFFFF)
}
