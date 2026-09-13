package com.ledinhthi.ontaptld.feature.deck.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource

@Entity(
    tableName = "flashcards",
    indices = [Index("deckId"), Index("dueDate"), Index("noteId")],
)
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val deckId: String,
    val noteId: String? = null,
    val source: FlashcardSource,
    val question: String,
    val answer: String,
    val sourceBoxLeft: Float? = null,
    val sourceBoxTop: Float? = null,
    val sourceBoxRight: Float? = null,
    val sourceBoxBottom: Float? = null,
    val easeFactor: Double = 2.5,
    val interval: Int = 0,
    val repetitions: Int = 0,
    val dueDate: Long,
    val lastReviewedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val synced: Boolean = false,
    val isDeleted: Boolean = false,
)
