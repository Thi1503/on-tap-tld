package com.ledinhthi.ontaptld.feature.review.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.IdGenerator
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import com.ledinhthi.ontaptld.feature.review.domain.ReviewGrade
import com.ledinhthi.ontaptld.feature.review.domain.Sm2Calculator
import com.ledinhthi.ontaptld.feature.review.domain.Sm2Input
import com.ledinhthi.ontaptld.feature.review.domain.model.ReviewLog
import com.ledinhthi.ontaptld.feature.review.domain.repository.ReviewLogRepository
import javax.inject.Inject

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/**
 * Chấm một thẻ: tính lịch ôn mới bằng SM-2, lưu vào thẻ, và ghi một dòng vào lịch sử ôn.
 * Trả về thẻ SAU khi cập nhật — `interval` của nó là số ngày tới lần ôn kế tiếp.
 */
class ReviewFlashcardUseCase @Inject constructor(
    private val repository: FlashcardRepository,
    private val reviewLogs: ReviewLogRepository,
    private val clock: Clock,
    private val ids: IdGenerator,
) : UseCase<ReviewFlashcardUseCase.Params, Flashcard>() {

    data class Params(val cardId: String, val grade: ReviewGrade)

    override suspend fun invoke(input: Params): Flashcard {
        val card = repository.getById(input.cardId)
            ?: throw DeckException(DeckException.Kind.CARD_NOT_FOUND)
        val out = Sm2Calculator.next(
            Sm2Input(card.easeFactor, card.interval, card.repetitions),
            input.grade,
        )
        val now = clock.nowMillis()
        val updated = card.copy(
            easeFactor = out.easeFactor,
            interval = out.interval,
            repetitions = out.repetitions,
            dueDate = now + out.interval * DAY_MILLIS,
            lastReviewedAt = now,
            updatedAt = now,
        )
        repository.upsert(updated)
        reviewLogs.add(
            ReviewLog(
                id = ids.newId(),
                cardId = card.id,
                deckId = card.deckId,
                grade = input.grade,
                reviewedAt = now,
            ),
        )
        return updated
    }
}
