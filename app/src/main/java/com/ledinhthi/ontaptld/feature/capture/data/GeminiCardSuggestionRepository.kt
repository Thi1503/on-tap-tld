package com.ledinhthi.ontaptld.feature.capture.data

import com.ledinhthi.ontaptld.core.data.ai.FlashcardPrompt
import com.ledinhthi.ontaptld.core.data.ai.GeminiClient
import com.ledinhthi.ontaptld.feature.capture.domain.model.SuggestedCard
import com.ledinhthi.ontaptld.feature.capture.domain.repository.CardSuggestionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lấy thẻ đề xuất từ Gemini rồi "làm sạch" trước khi đưa cho phần còn lại của app: AI có thể
 * trả thẻ thiếu câu hỏi, thừa khoảng trắng, hay số dòng không tồn tại.
 */
@Singleton
class GeminiCardSuggestionRepository @Inject constructor(
    private val gemini: GeminiClient,
) : CardSuggestionRepository {

    override suspend fun suggestCards(noteText: String): List<SuggestedCard> {
        val lineCount = FlashcardPrompt.noteLines(noteText).size
        return gemini.generateFlashcards(noteText, MaxSuggestedCards)
            .map { dto ->
                SuggestedCard(
                    question = dto.question.trim(),
                    answer = dto.answer.trim(),
                    // Số dòng nằm ngoài ghi chú thì coi như không biết nguồn. `1..lineCount` là
                    // khoảng từ 1 tới lineCount; `in` kiểm tra số có thuộc khoảng đó không.
                    sourceLine = dto.sourceLine?.takeIf { it in 1..lineCount },
                )
            }
            .filter { it.question.isNotEmpty() && it.answer.isNotEmpty() }
            .take(MaxSuggestedCards) // phòng khi AI trả nhiều hơn số đã dặn
    }

    private companion object {
        /** Mỗi lần tạo tối đa bấy nhiêu thẻ — đủ cho một trang ghi chú mà vẫn duyệt hết được. */
        const val MaxSuggestedCards = 10
    }
}
