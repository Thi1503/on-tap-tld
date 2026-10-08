package com.ledinhthi.ontaptld.core.data.ai

import kotlinx.serialization.Serializable

interface GeminiClient {
    /**
     * Nhờ AI soạn tối đa [maxCards] thẻ hỏi–đáp từ [noteText] (văn bản ghi chú người dùng đã
     * kiểm tra). Ném `AppException.AiException` khi lỗi.
     */
    suspend fun generateFlashcards(noteText: String, maxCards: Int): List<AiFlashcardDto>
}

/**
 * Một thẻ đúng như AI trả về (JSON). `@Serializable` cho phép thư viện kotlinx.serialization
 * tự chuyển chuỗi JSON thành đối tượng này.
 *
 * [sourceLine]: thẻ được rút ra từ dòng thứ mấy của ghi chú (đánh số từ 1, xem [FlashcardPrompt]).
 */
@Serializable
data class AiFlashcardDto(
    val question: String,
    val answer: String,
    val sourceLine: Int? = null,
)
