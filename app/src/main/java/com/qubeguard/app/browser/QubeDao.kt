package com.qubeguard.app.browser

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object (DAO) for Qube profiles, bookmarks, and history.
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

    // Bookmarks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("SELECT * FROM bookmarks WHERE qubeId = :qubeId ORDER BY createdAt DESC")
    suspend fun getBookmarksByQube(qubeId: String): List<BookmarkEntity>

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: String)

    // History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Query("SELECT * FROM history_entries WHERE qubeId = :qubeId ORDER BY visitedAt DESC LIMIT 100")
    suspend fun getHistoryByQube(qubeId: String): List<HistoryEntity>

    @Query("DELETE FROM history_entries WHERE qubeId = :qubeId")
    suspend fun clearHistoryForQube(qubeId: String)
}
