package com.qubeguard.app.browser

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey
    val id: String,
    val qubeId: String,
    val title: String,
    val url: String,
    val createdAt: String
)

@Entity(tableName = "history_entries")
data class HistoryEntity(
    @PrimaryKey
    val id: String,
    val qubeId: String,
    val title: String,
    val url: String,
    val visitedAt: String
)
