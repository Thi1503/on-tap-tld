package com.ledinhthi.ontaptld.feature.deck.presentation.manualcard

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.ledinhthi.ontaptld.core.presentation.mvi.BaseViewModel
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.SnackBarType
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.CreateManualFlashcardUseCase
import com.ledinhthi.ontaptld.navigation.ManualCardRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ManualCardViewModel @Inject constructor(
    toolbox: ViewModelToolbox,
    savedState: SavedStateHandle,
    private val createManualFlashcard: CreateManualFlashcardUseCase,
) : BaseViewModel<ManualCardState>(ManualCardState(), toolbox) {

    private val args = savedState.toRoute<ManualCardRoute>()

    fun onQuestionChange(value: String) = setState { copy(question = value) }
    fun onAnswerChange(value: String) = setState { copy(answer = value) }

    fun onSave() = launchGuarded(
        showLoadingOverlay = true,
        onError = { e ->
            if (e is DeckException) {
                navigator.showSnackBar(e.userMessage ?: "")
                null
            } else {
                e
            }
        },
    ) {
        createManualFlashcard(
            CreateManualFlashcardUseCase.Params(
                deckId = args.deckId,
                question = currentState.question,
                answer = currentState.answer,
            ),
        )
        navigator.showSnackBar("Đã thêm thẻ.", SnackBarType.SUCCESS)
        navigator.back()
    }
}
