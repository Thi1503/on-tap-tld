package com.ledinhthi.ontaptld.feature.deck.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey val id: String,
    val userId: String? = null,     // null ở Sprint 1
    val name: String,
    val colorHex: String,
    val createdAt: Long,
    val updatedAt: Long,            // Last-Write-Wins khi sync — Sprint 2
    val synced: Boolean = false,    // Sprint 2
    val isDeleted: Boolean = false, // soft delete — Sprint 2
)
