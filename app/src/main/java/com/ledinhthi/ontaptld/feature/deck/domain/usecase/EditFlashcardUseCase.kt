package com.ledinhthi.ontaptld.feature.deck.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import javax.inject.Inject

class EditFlashcardUseCase @Inject constructor(
    private val repository: FlashcardRepository,
    private val clock: Clock,
) : UseCase<EditFlashcardUseCase.Params, Flashcard>() {

    data class Params(val cardId: String, val question: String, val answer: String)

    override suspend fun invoke(input: Params): Flashcard {
        val q = input.question.trim()
        val a = input.answer.trim()
        if (q.isEmpty() || a.isEmpty()) throw DeckException(DeckException.Kind.BLANK_CARD)
        val card = repository.getById(input.cardId)
            ?: throw DeckException(DeckException.Kind.CARD_NOT_FOUND)
        val updated = card.copy(question = q, answer = a, updatedAt = clock.nowMillis())
        repository.upsert(updated)
        return updated
    }
}
