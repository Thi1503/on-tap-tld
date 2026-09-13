package com.ledinhthi.ontaptld.feature.deck.data.mapper

import com.ledinhthi.ontaptld.core.data.mapper.EntityMapper
import com.ledinhthi.ontaptld.core.data.mapper.ModelMapper
import com.ledinhthi.ontaptld.feature.deck.data.local.DeckEntity
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import javax.inject.Inject

class DeckEntityMapper @Inject constructor() :
    EntityMapper<DeckEntity, Deck>, ModelMapper<Deck, DeckEntity> {

    override fun toDomain(entity: DeckEntity) = Deck(
        id = entity.id,
        name = entity.name,
        colorHex = entity.colorHex,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    override fun toEntity(model: Deck) = DeckEntity(
        id = model.id,
        name = model.name,
        colorHex = model.colorHex,
        createdAt = model.createdAt,
        updatedAt = model.updatedAt,
    )
}
