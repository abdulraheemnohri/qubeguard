package com.qubeguard.app.data.blocklist

/**
 * A Radix Tree (Trie) implementation for fast domain/subdomain matching.
 * Indexes domains right-to-left (TLD first) so lookups match parent domain rules.
 */
class RadixTree {
    private val root = RadixNode()

    /**
     * Inserts a domain into the Radix Tree.
     * @param domain The domain to insert (e.g., "example.com").
     * @param isBlocked Whether this domain is blocked or allowed.
     */
    fun insert(domain: String, isBlocked: Boolean) {
        val normalizedDomain = domain.lowercase().trim().trimEnd('.')
        if (normalizedDomain.isEmpty()) return

        var currentNode = root
        val segments = normalizedDomain.split('.').filter { it.isNotEmpty() }.reversed()

        for (segment in segments) {
            var childNode = currentNode.children.find { it.segment == segment }
            if (childNode == null) {
                childNode = RadixNode(segment)
                currentNode.children.add(childNode)
            }
            currentNode = childNode
        }

        currentNode.isBlocked = isBlocked
        currentNode.isTerminal = true
    }

    /**
     * Checks if a domain is blocked by the Radix Tree.
     * @param domain The domain to check (e.g., "sub.example.com").
     * @return True if the domain or any parent domain is blocked.
     */
    fun isBlocked(domain: String): Boolean {
        val normalizedDomain = domain.lowercase().trim().trimEnd('.')
        if (normalizedDomain.isEmpty()) return false

        var currentNode = root
        val segments = normalizedDomain.split('.').filter { it.isNotEmpty() }.reversed()

        for (segment in segments) {
            val childNode = currentNode.children.find { it.segment == segment } ?: return false
            if (childNode.isTerminal && childNode.isBlocked) {
                return true
            }
            currentNode = childNode
        }

        return currentNode.isTerminal && currentNode.isBlocked
    }

    /**
     * Checks if a domain or any of its subdomains are blocked.
     * @param domain The domain to check.
     * @return True if the domain or parent domain is blocked.
     */
    fun isBlockedOrSubdomainBlocked(domain: String): Boolean = isBlocked(domain)

    /**
     * Clears all nodes from the Radix Tree.
     */
    fun clear() {
        root.children.clear()
    }

    private class RadixNode(
        val segment: String = "",
        var isTerminal: Boolean = false,
        var isBlocked: Boolean = false
    ) {
        val children = mutableListOf<RadixNode>()
    }
}
