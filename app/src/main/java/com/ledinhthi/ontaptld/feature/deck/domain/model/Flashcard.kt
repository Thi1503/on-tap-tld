package com.ledinhthi.ontaptld.feature.deck.domain.model

import com.ledinhthi.ontaptld.core.domain.model.DomainModel

enum class FlashcardSource { MANUAL, AI }

data class Flashcard(
    val id: String,
    val deckId: String,
    val noteId: String?, // null nếu tạo thủ công
    val source: FlashcardSource,
    val question: String,
    val answer: String,
    val sourceBox: SourceBox? = null, // chỉ có khi source = AI
    // --- SM-2 (có từ schema v1) ---
    val easeFactor: Double = 2.5,
    val interval: Int = 0,
    val repetitions: Int = 0,
    val dueDate: Long,
    val lastReviewedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
) : DomainModel

data class SourceBox(val left: Float, val top: Float, val right: Float, val bottom: Float)
