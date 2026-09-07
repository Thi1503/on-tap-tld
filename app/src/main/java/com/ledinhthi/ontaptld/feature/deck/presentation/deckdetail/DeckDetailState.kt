package com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail

import com.ledinhthi.ontaptld.core.presentation.mvi.UiState
import com.ledinhthi.ontaptld.core.presentation.mvi.UiStatus
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard

data class DeckDetailState(
    override val status: UiStatus = UiStatus(),
    val deck: Deck? = null,
    val cards: List<Flashcard> = emptyList(),
) : UiState {
    override fun withStatus(status: UiStatus) = copy(status = status)
}
