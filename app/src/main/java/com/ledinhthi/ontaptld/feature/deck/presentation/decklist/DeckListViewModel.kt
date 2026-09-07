package com.ledinhthi.ontaptld.feature.deck.presentation.decklist

import androidx.lifecycle.viewModelScope
import com.ledinhthi.ontaptld.core.presentation.mvi.BaseViewModel
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.CreateDeckUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveDecksUseCase
import com.ledinhthi.ontaptld.navigation.DeckDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class DeckListViewModel @Inject constructor(
    toolbox: ViewModelToolbox,
    observeDecks: ObserveDecksUseCase,
    private val createDeck: CreateDeckUseCase,
) : BaseViewModel<DeckListState>(DeckListState(), toolbox) {

    init {
        observeDecks()
            .onEach { decks -> setState { copy(decks = decks) } }
            .launchIn(viewModelScope)
    }

    fun onCreateDeck(name: String, colorHex: String) = launchGuarded(
        showLoadingOverlay = true,
        onError = { e ->
            if (e is DeckException) {
                navigator.showSnackBar(e.userMessage ?: "")
                null // đã xử lý xong
            } else {
                e // để GlobalExceptionHandler lo
            }
        },
    ) {
        createDeck(CreateDeckUseCase.Params(name, colorHex))
    }

    fun onDeckClick(deckId: String) = navigator.to(DeckDetailRoute(deckId))
}
