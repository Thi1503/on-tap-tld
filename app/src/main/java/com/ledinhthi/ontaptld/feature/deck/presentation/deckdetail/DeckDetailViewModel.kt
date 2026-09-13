package com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ledinhthi.ontaptld.core.presentation.mvi.BaseViewModel
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.DeleteFlashcardUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveDeckUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveFlashcardsUseCase
import com.ledinhthi.ontaptld.navigation.DeckDetailRoute
import com.ledinhthi.ontaptld.navigation.ManualCardRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class DeckDetailViewModel @Inject constructor(
    toolbox: ViewModelToolbox,
    savedState: SavedStateHandle,
    observeDeck: ObserveDeckUseCase,
    observeCards: ObserveFlashcardsUseCase,
    private val deleteFlashcard: DeleteFlashcardUseCase,
) : BaseViewModel<DeckDetailState>(DeckDetailState(), toolbox) {

    private val args = savedState.toRoute<DeckDetailRoute>()
    val deckId: String get() = args.deckId

    init {
        observeDeck(deckId)
            .onEach { deck -> setState { copy(deck = deck) } }
            .launchIn(viewModelScope)
        observeCards(deckId)
            .onEach { cards -> setState { copy(cards = cards) } }
            .launchIn(viewModelScope)
    }

    fun onAddManualCard() = navigator.to(ManualCardRoute(deckId))

    fun onDeleteCard(cardId: String) = launchGuarded {
        deleteFlashcard(cardId)
    }
}
