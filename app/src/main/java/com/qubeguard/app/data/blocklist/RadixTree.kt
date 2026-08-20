package com.qubeguard.app.data.blocklist

/**
 * A Radix Tree (Trie) implementation for fast domain/subdomain matching.
 * Used for O(L) lookups where L is the length of the domain.
 */
class RadixTree {
    private val root = RadixNode()

    /**
     * Inserts a domain into the Radix Tree.
     * @param domain The domain to insert (e.g., "example.com").
     * @param isBlocked Whether this domain is blocked or allowed.
     */
    fun insert(domain: String, isBlocked: Boolean) {
        val normalizedDomain = domain.lowercase().trim()
        if (normalizedDomain.isEmpty()) return

        var currentNode = root
        var remainingDomain = normalizedDomain

        while (remainingDomain.isNotEmpty()) {
            val nextDotIndex = remainingDomain.indexOf('.')
            val segment = if (nextDotIndex == -1) remainingDomain else remainingDomain.substring(0, nextDotIndex)

            var childNode = currentNode.children.find { it.segment == segment }
            if (childNode == null) {
                childNode = RadixNode(segment)
                currentNode.children.add(childNode)
            }

            currentNode = childNode
            remainingDomain = if (nextDotIndex == -1) "" else remainingDomain.substring(nextDotIndex + 1)
        }

        currentNode.isBlocked = isBlocked
        currentNode.isTerminal = true
    }

    /**
     * Checks if a domain is blocked by the Radix Tree.
     * @param domain The domain to check (e.g., "sub.example.com").
     * @return True if the domain or any of its subdomains are blocked.
     */
    fun isBlocked(domain: String): Boolean {
        val normalizedDomain = domain.lowercase().trim()
        if (normalizedDomain.isEmpty()) return false

        var currentNode = root
        var remainingDomain = normalizedDomain

        while (remainingDomain.isNotEmpty()) {
            val nextDotIndex = remainingDomain.indexOf('.')
            val segment = if (nextDotIndex == -1) remainingDomain else remainingDomain.substring(0, nextDotIndex)

            val childNode = currentNode.children.find { it.segment == segment }
            if (childNode == null) {
                return false
            }

            // If this node is terminal and blocked, the domain is blocked
            if (childNode.isTerminal && childNode.isBlocked) {
                return true
            }

            currentNode = childNode
            remainingDomain = if (nextDotIndex == -1) "" else remainingDomain.substring(nextDotIndex + 1)
        }

        return currentNode.isTerminal && currentNode.isBlocked
    }

    /**
     * Checks if a domain or any of its subdomains are blocked.
     * @param domain The domain to check (e.g., "example.com").
     * @return True if the domain or any subdomain is blocked.
     */
    fun isBlockedOrSubdomainBlocked(domain: String): Boolean {
        val normalizedDomain = domain.lowercase().trim()
        if (normalizedDomain.isEmpty()) return false

        var currentNode = root
        var remainingDomain = normalizedDomain

        while (remainingDomain.isNotEmpty()) {
            val nextDotIndex = remainingDomain.indexOf('.')
            val segment = if (nextDotIndex == -1) remainingDomain else remainingDomain.substring(0, nextDotIndex)

            val childNode = currentNode.children.find { it.segment == segment }
            if (childNode == null) {
                return false
            }

            // If this node is terminal and blocked, the domain is blocked
            if (childNode.isTerminal && childNode.isBlocked) {
                return true
            }

            currentNode = childNode
            remainingDomain = if (nextDotIndex == -1) "" else remainingDomain.substring(nextDotIndex + 1)
        }

        // Check if any subdomain is blocked
        return currentNode.isTerminal && currentNode.isBlocked
    }

    /**
     * Clears all nodes from the Radix Tree.
     */
    fun clear() {
        root.children.clear()
    }

    /**
     * Represents a node in the Radix Tree.
     */
    private class RadixNode(
        val segment: String = "",
        var isTerminal: Boolean = false,
        var isBlocked: Boolean = false
    ) {
        val children = mutableListOf<RadixNode>()
    }
}