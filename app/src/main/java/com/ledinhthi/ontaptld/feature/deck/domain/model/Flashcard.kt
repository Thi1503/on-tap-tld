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
    /**
     * Thẻ AI được rút ra từ dòng thứ mấy của ghi chú (đếm từ 1, chỉ tính dòng có chữ — đúng cách
     * đánh số khi gửi ghi chú cho AI). null nếu AI không báo dòng, hoặc thẻ không phải thẻ AI.
     */
    val sourceLine: Int? = null,
    // --- SM-2 (có từ schema v1) ---
    val easeFactor: Double = 2.5,
    val interval: Int = 0,
    val repetitions: Int = 0,
    val dueDate: Long,
    val lastReviewedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
) : DomainModel

/**
 * Vùng chữ trên ảnh ghi chú mà một thẻ AI được rút ra. Bốn cạnh tính theo TỈ LỆ của ảnh (0 = mép
 * trái / trên, 1 = mép phải / dưới) chứ không theo pixel, nên ảnh hiển thị to nhỏ thế nào cũng
 * đặt đúng chỗ.
 */
data class SourceBox(val left: Float, val top: Float, val right: Float, val bottom: Float)
