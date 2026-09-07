package com.ledinhthi.ontaptld.feature.deck.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.IdGenerator
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import javax.inject.Inject

/** Tạo thẻ THỦ CÔNG: noteId = null, source = MANUAL, dueDate = now (ôn được ngay). */
class CreateManualFlashcardUseCase @Inject constructor(
    private val repository: FlashcardRepository,
    private val clock: Clock,
    private val ids: IdGenerator,
) : UseCase<CreateManualFlashcardUseCase.Params, Flashcard>() {

    data class Params(val deckId: String, val question: String, val answer: String)

    override suspend fun invoke(input: Params): Flashcard {
        val q = input.question.trim()
        val a = input.answer.trim()
        if (q.isEmpty() || a.isEmpty()) throw DeckException(DeckException.Kind.BLANK_CARD)
        val now = clock.nowMillis()
        val card = Flashcard(
            id = ids.newId(),
            deckId = input.deckId,
            noteId = null,
            source = FlashcardSource.MANUAL,
            question = q,
            answer = a,
            dueDate = now,
            createdAt = now,
            updatedAt = now,
        )
        repository.upsert(card)
        return card
    }
}
