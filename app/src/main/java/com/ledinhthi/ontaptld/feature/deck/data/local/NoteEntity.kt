package com.ledinhthi.ontaptld.feature.deck.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val deckId: String,
    val imagePath: String,       // đường dẫn ảnh local — KHÔNG sync lên Cloud (docs_tld 6.2)
    val ocrText: String,
    val createdAt: Long,
    val updatedAt: Long,
    val synced: Boolean = false, // chỉ sync phần text, Sprint 2
    val isDeleted: Boolean = false,
)
