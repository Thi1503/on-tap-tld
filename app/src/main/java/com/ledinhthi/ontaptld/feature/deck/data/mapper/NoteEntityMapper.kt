package com.ledinhthi.ontaptld.feature.deck.data.mapper

import com.ledinhthi.ontaptld.core.data.mapper.EntityMapper
import com.ledinhthi.ontaptld.core.data.mapper.ModelMapper
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteEntity
import com.ledinhthi.ontaptld.feature.deck.domain.model.Note
import javax.inject.Inject

class NoteEntityMapper @Inject constructor() :
    EntityMapper<NoteEntity, Note>, ModelMapper<Note, NoteEntity> {

    override fun toDomain(entity: NoteEntity) = Note(
        id = entity.id,
        deckId = entity.deckId,
        imagePath = entity.imagePath,
        ocrText = entity.ocrText,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    override fun toEntity(model: Note) = NoteEntity(
        id = model.id,
        deckId = model.deckId,
        imagePath = model.imagePath,
        ocrText = model.ocrText,
        createdAt = model.createdAt,
        updatedAt = model.updatedAt,
    )
}
