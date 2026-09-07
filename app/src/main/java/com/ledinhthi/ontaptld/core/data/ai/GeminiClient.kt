package com.ledinhthi.ontaptld.core.data.ai

import kotlinx.serialization.Serializable

interface GeminiClient {
    /** Trả danh sách thẻ đề xuất từ text OCR. Ném AppException.AiException khi lỗi. */
    suspend fun generateFlashcards(ocrText: String, maxCards: Int): List<AiFlashcardDto>
}

@Serializable
data class AiFlashcardDto(
    val question: String,
    val answer: String,
    val sourceBoxLeft: Float? = null,
    val sourceBoxTop: Float? = null,
    val sourceBoxRight: Float? = null,
    val sourceBoxBottom: Float? = null,
)
