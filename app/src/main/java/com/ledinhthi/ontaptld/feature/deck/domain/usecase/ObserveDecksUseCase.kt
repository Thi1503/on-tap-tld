package com.ledinhthi.ontaptld.feature.deck.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.NoInputFlowUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.repository.DeckRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDecksUseCase @Inject constructor(
    private val repository: DeckRepository,
) : NoInputFlowUseCase<List<Deck>>() {
    override fun invoke(): Flow<List<Deck>> = repository.observeDecks()
}
