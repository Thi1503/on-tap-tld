package com.ledinhthi.ontaptld.feature.capture.domain.usecase

import com.ledinhthi.ontaptld.core.data.ai.AiQuotaGuard
import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.core.exception.AiErrorKind
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.feature.capture.domain.model.SuggestedCard
import com.ledinhthi.ontaptld.feature.capture.domain.repository.CardSuggestionRepository
import javax.inject.Inject

/**
 * Nhờ AI soạn thẻ từ văn bản ghi chú, có tính lượt dùng trong ngày.
 *
 * Chỉ lần gọi THÀNH CÔNG (có ít nhất một thẻ) mới bị trừ lượt. Mất mạng, AI lỗi, AI không soạn
 * được thẻ nào, hay người dùng bấm Huỷ giữa chừng đều không tính — họ chưa nhận được gì.
 */
class GenerateFlashcardsWithAiUseCase @Inject constructor(
    private val repository: CardSuggestionRepository,
    private val quotaGuard: AiQuotaGuard,
) : UseCase<String, List<SuggestedCard>>() {

    override suspend fun invoke(input: String): List<SuggestedCard> {
        quotaGuard.ensureCanCall() // hết lượt hôm nay thì ném lỗi ngay, không gọi mạng
        val cards = repository.suggestCards(input)
        if (cards.isEmpty()) throw AppException.AiException(AiErrorKind.EMPTY_RESPONSE)
        quotaGuard.recordCall()
        return cards
    }
}
