package com.ledinhthi.ontaptld.feature.deck.presentation.decklist

import com.ledinhthi.ontaptld.core.presentation.mvi.UiState
import com.ledinhthi.ontaptld.core.presentation.mvi.UiStatus
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck

data class DeckListState(
    override val status: UiStatus = UiStatus(),
    val decks: List<Deck> = emptyList(),
    val dueCount: Int = 0,
) : UiState {
    override fun withStatus(status: UiStatus) = copy(status = status)
}
