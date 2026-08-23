package com.qubeguard.app.data.blocklist

/** Reverse-label domain trie. Parent-domain rules match all subdomains. */
class RadixTree {
    private val root = RadixNode()

    fun insert(domain: String, isBlocked: Boolean) {
        val normalized = normalize(domain) ?: return
        var node = root
        normalized.split('.').asReversed().forEach { segment ->
            node = node.children.getOrPut(segment) { RadixNode() }
        }
        node.isTerminal = true
        node.isBlocked = isBlocked
    }

    fun isBlocked(domain: String): Boolean {
        val normalized = normalize(domain) ?: return false
        var node = root
        for (segment in normalized.split('.').asReversed()) {
            node = node.children[segment] ?: return false
            if (node.isTerminal && node.isBlocked) return true
        }
        return false
    }

    fun isBlockedOrSubdomainBlocked(domain: String): Boolean = isBlocked(domain)

    fun clear() = root.children.clear()

    private fun normalize(value: String): String? {
        val result = value.trim().trimEnd('.').lowercase()
        if (result.isEmpty() || result.length > 253) return null
        if (result.any { it.isWhitespace() }) return null
        return result
    }

    private class RadixNode {
        val children = HashMap<String, RadixNode>()
        var isTerminal = false
        var isBlocked = false
    }
}
