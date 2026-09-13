package com.ledinhthi.ontaptld.feature.deck.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.IdGenerator
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.repository.DeckRepository
import javax.inject.Inject

class CreateDeckUseCase @Inject constructor(
    private val repository: DeckRepository,
    private val clock: Clock,
    private val ids: IdGenerator,
) : UseCase<CreateDeckUseCase.Params, Deck>() {

    data class Params(val name: String, val colorHex: String)

    override suspend fun invoke(input: Params): Deck {
        val name = input.name.trim()
        if (name.isEmpty()) throw DeckException(DeckException.Kind.BLANK_NAME)
        val now = clock.nowMillis()
        val deck = Deck(
            id = ids.newId(),
            name = name,
            colorHex = input.colorHex,
            createdAt = now,
            updatedAt = now,
        )
        repository.upsert(deck)
        return deck
    }
}
