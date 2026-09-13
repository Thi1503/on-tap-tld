package com.ledinhthi.ontaptld.feature.review.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import com.ledinhthi.ontaptld.feature.review.domain.ReviewGrade
import com.ledinhthi.ontaptld.feature.review.domain.Sm2Calculator
import com.ledinhthi.ontaptld.feature.review.domain.Sm2Input
import javax.inject.Inject

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

class ReviewFlashcardUseCase @Inject constructor(
    private val repository: FlashcardRepository,
    private val clock: Clock,
) : UseCase<ReviewFlashcardUseCase.Params, Unit>() {

    data class Params(val cardId: String, val grade: ReviewGrade)

    override suspend fun invoke(input: Params) {
        val card = repository.getById(input.cardId)
            ?: throw DeckException(DeckException.Kind.CARD_NOT_FOUND)
        val out = Sm2Calculator.next(
            Sm2Input(card.easeFactor, card.interval, card.repetitions),
            input.grade,
        )
        val now = clock.nowMillis()
        repository.upsert(
            card.copy(
                easeFactor = out.easeFactor,
                interval = out.interval,
                repetitions = out.repetitions,
                dueDate = now + out.interval * DAY_MILLIS,
                lastReviewedAt = now,
                updatedAt = now,
            ),
        )
    }
}
