package com.ledinhthi.ontaptld.feature.capture.domain.repository

import com.ledinhthi.ontaptld.feature.capture.domain.model.SuggestedCard

/** Nguồn thẻ đề xuất từ văn bản ghi chú. Bản cài đặt hiện tại gọi Gemini. */
interface CardSuggestionRepository {
    /**
     * Trả về các thẻ AI soạn từ [noteText]; danh sách rỗng nếu AI không tìm ra gì để hỏi.
     * Ném `AppException.AiException` khi gọi AI lỗi (mất mạng, hết hạn mức phía máy chủ…).
     */
    suspend fun suggestCards(noteText: String): List<SuggestedCard>
}
