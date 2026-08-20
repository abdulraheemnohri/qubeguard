package com.qubeguard.app.browser

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object (DAO) for QubeProfile.
 * Provides methods to interact with the Room Database for Qube profiles.
 */
@Dao
interface QubeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQube(qube: QubeProfile)

    @Update
    suspend fun updateQube(qube: QubeProfile)

    @Query("SELECT * FROM qube_profiles WHERE id = :id")
    suspend fun getQubeById(id: String): QubeProfile?

    @Query("SELECT * FROM qube_profiles WHERE isDefault = 1")
    suspend fun getDefaultQube(): QubeProfile?

    @Query("SELECT * FROM qube_profiles WHERE isIncognito = 1")
    suspend fun getIncognitoQube(): QubeProfile?

    @Query("SELECT * FROM qube_profiles")
    suspend fun getAllQubes(): List<QubeProfile>

    @Query("DELETE FROM qube_profiles WHERE id = :id")
    suspend fun deleteQube(id: String)

    @Query("SELECT * FROM qube_profiles ORDER BY lastUsedAt DESC LIMIT 1")
    suspend fun getLastUsedQube(): QubeProfile?
}
