package com.ledinhthi.ontaptld.feature.deck.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.IdGenerator
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.deck.domain.model.SourceBox
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import javax.inject.Inject

/** Điểm "một kho dữ liệu": feature/capture gọi use case NÀY (của deck), không tự ghi flashcard. */
class CreateFlashcardsFromNoteUseCase @Inject constructor(
    private val repository: FlashcardRepository,
    private val clock: Clock,
    private val ids: IdGenerator,
) : UseCase<CreateFlashcardsFromNoteUseCase.Params, List<Flashcard>>() {

    data class Draft(val question: String, val answer: String, val box: SourceBox?)
    data class Params(val deckId: String, val noteId: String, val drafts: List<Draft>)

    override suspend fun invoke(input: Params): List<Flashcard> {
        val now = clock.nowMillis()
        val cards = input.drafts.map { d ->
            Flashcard(
                id = ids.newId(),
                deckId = input.deckId,
                noteId = input.noteId,
                source = FlashcardSource.AI,
                question = d.question.trim(),
                answer = d.answer.trim(),
                sourceBox = d.box,
                dueDate = now,
                createdAt = now,
                updatedAt = now,
            )
        }
        repository.upsertAll(cards)
        return cards
    }
}
