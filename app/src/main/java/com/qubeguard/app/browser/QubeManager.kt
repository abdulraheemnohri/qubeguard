package com.qubeguard.app.browser

import com.qubeguard.app.data.blocklist.BlocklistDao
import javax.inject.Inject

/**
 * Manages Qube profiles, including creation, deletion, and switching.
 */
class QubeManager @Inject constructor(
    private val qubeDao: QubeDao
) {

    /**
     * Creates a new Qube profile.
     * @param name The name of the Qube.
     * @param color The color for the Qube (default: random predefined color).
     * @param isIncognito Whether the Qube is incognito (no persistent data).
     * @return The ID of the newly created Qube.
     */
    suspend fun createQube(
        name: String,
        color: Int = QubeProfile.predefinedColors.random(),
        isIncognito: Boolean = false
    ): String {
        val id = generateQubeId()
        val createdAt = System.currentTimeMillis().toString()

        val qube = QubeProfile(
            id = id,
            name = name,
            color = color,
            icon = null,
            isDefault = false,
            isIncognito = isIncognito,
            createdAt = createdAt,
            lastUsedAt = null
        )

        qubeDao.insertQube(qube)
        return id
    }

    /**
     * Deletes a Qube profile.
     * @param qubeId The ID of the Qube to delete.
     */
    suspend fun deleteQube(qubeId: String) {
        qubeDao.deleteQube(qubeId)
    }

    /**
     * Gets a Qube profile by its ID.
     * @param qubeId The ID of the Qube.
     * @return The QubeProfile, or null if not found.
     */
    suspend fun getQubeById(qubeId: String): QubeProfile? {
        return qubeDao.getQubeById(qubeId)
    }

    /**
     * Gets the default Qube profile.
     * @return The default QubeProfile, or null if not found.
     */
    suspend fun getDefaultQube(): QubeProfile? {
        return qubeDao.getDefaultQube()
    }

    /**
     * Gets the incognito Qube profile.
     * @return The incognito QubeProfile, or null if not found.
     */
    suspend fun getIncognitoQube(): QubeProfile? {
        return qubeDao.getIncognitoQube()
    }

    /**
     * Gets all Qube profiles.
     * @return A list of all QubeProfile objects.
     */
    suspend fun getAllQubes(): List<QubeProfile> {
        return qubeDao.getAllQubes()
    }

    /**
     * Sets a Qube as the default.
     * @param qubeId The ID of the Qube to set as default.
     */
    suspend fun setDefaultQube(qubeId: String) {
        // First, unset the current default Qube
        val currentDefault = qubeDao.getDefaultQube()
        if (currentDefault != null) {
            qubeDao.updateQube(currentDefault.copy(isDefault = false))
        }

        // Set the new default Qube
        val qube = qubeDao.getQubeById(qubeId)
        if (qube != null) {
            qubeDao.updateQube(qube.copy(isDefault = true))
        }
    }

    /**
     * Updates the last used timestamp for a Qube.
     * @param qubeId The ID of the Qube.
     */
    suspend fun updateLastUsed(qubeId: String) {
        val qube = qubeDao.getQubeById(qubeId)
        if (qube != null) {
            qubeDao.updateQube(qube.copy(lastUsedAt = System.currentTimeMillis().toString()))
        }
    }

    /**
     * Gets the last used Qube profile.
     * @return The last used QubeProfile, or null if not found.
     */
    suspend fun getLastUsedQube(): QubeProfile? {
        return qubeDao.getLastUsedQube()
    }

    /**
     * Generates a unique ID for a new Qube.
     */
    private fun generateQubeId(): String {
        return "qube_" + java.util.UUID.randomUUID().toString().substring(0, 8)
    }

    /**
     * Initializes the default Qube profiles if they don't exist.
     */
    suspend fun initializeDefaultQubes() {
        val allQubes = qubeDao.getAllQubes()
        if (allQubes.isEmpty()) {
            // Create default Qube
            val defaultQube = QubeProfile(
                id = QubeProfile.DEFAULT_QUBE_ID,
                name = "Default",
                color = 0xFF3F51B5.toInt(), // Indigo
                icon = null,
                isDefault = true,
                isIncognito = false,
                createdAt = System.currentTimeMillis().toString(),
                lastUsedAt = null
            )
            qubeDao.insertQube(defaultQube)

            // Create incognito Qube
            val incognitoQube = QubeProfile(
                id = QubeProfile.INCOGNITO_QUBE_ID,
                name = "Incognito",
                color = 0xFF795548.toInt(), // Brown
                icon = null,
                isDefault = false,
                isIncognito = true,
                createdAt = System.currentTimeMillis().toString(),
                lastUsedAt = null
            )
            qubeDao.insertQube(incognitoQube)
        }
    }
}
