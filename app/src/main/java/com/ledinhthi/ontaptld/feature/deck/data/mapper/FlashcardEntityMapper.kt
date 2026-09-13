package com.ledinhthi.ontaptld.feature.deck.data.mapper

import com.ledinhthi.ontaptld.core.data.mapper.EntityMapper
import com.ledinhthi.ontaptld.core.data.mapper.ModelMapper
import com.ledinhthi.ontaptld.feature.deck.data.local.FlashcardEntity
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.SourceBox
import javax.inject.Inject

class FlashcardEntityMapper @Inject constructor() :
    EntityMapper<FlashcardEntity, Flashcard>, ModelMapper<Flashcard, FlashcardEntity> {

    override fun toDomain(entity: FlashcardEntity) = Flashcard(
        id = entity.id,
        deckId = entity.deckId,
        noteId = entity.noteId,
        source = entity.source,
        question = entity.question,
        answer = entity.answer,
        sourceBox = box(entity),
        easeFactor = entity.easeFactor,
        interval = entity.interval,
        repetitions = entity.repetitions,
        dueDate = entity.dueDate,
        lastReviewedAt = entity.lastReviewedAt,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    override fun toEntity(model: Flashcard) = FlashcardEntity(
        id = model.id,
        deckId = model.deckId,
        noteId = model.noteId,
        source = model.source,
        question = model.question,
        answer = model.answer,
        sourceBoxLeft = model.sourceBox?.left,
        sourceBoxTop = model.sourceBox?.top,
        sourceBoxRight = model.sourceBox?.right,
        sourceBoxBottom = model.sourceBox?.bottom,
        easeFactor = model.easeFactor,
        interval = model.interval,
        repetitions = model.repetitions,
        dueDate = model.dueDate,
        lastReviewedAt = model.lastReviewedAt,
        createdAt = model.createdAt,
        updatedAt = model.updatedAt,
    )

    private fun box(e: FlashcardEntity): SourceBox? {
        val l = e.sourceBoxLeft
        val t = e.sourceBoxTop
        val r = e.sourceBoxRight
        val b = e.sourceBoxBottom
        return if (l != null && t != null && r != null && b != null) SourceBox(l, t, r, b) else null
    }
}
