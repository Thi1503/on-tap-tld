package com.ledinhthi.ontaptld.feature.deck.presentation.manualcard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.mvi.BaseViewModel
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.SnackBarType
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.CreateManualFlashcardUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.EditFlashcardUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.GetFlashcardUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveDecksUseCase
import com.ledinhthi.ontaptld.feature.deck.presentation.displayMessage
import com.ledinhthi.ontaptld.navigation.ManualCardRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel của màn Thêm / sửa thẻ (route `ManualCardRoute`).
 * Route có `cardId` = đang SỬA thẻ đó; không có = đang THÊM thẻ mới vào bộ `deckId`.
 */
@HiltViewModel
class ManualCardViewModel @Inject constructor(
    toolbox: ViewModelToolbox,
    savedState: SavedStateHandle,
    observeDecks: ObserveDecksUseCase,
    private val getFlashcard: GetFlashcardUseCase,
    private val createManualFlashcard: CreateManualFlashcardUseCase,
    private val editFlashcard: EditFlashcardUseCase,
) : BaseViewModel<ManualCardState>(initialState(savedState), toolbox) {

    // Đọc tham số route từ SavedStateHandle theo tên thuộc tính — xem ghi chú ở DeckDetailViewModel.
    private val cardId: String? = savedState[ManualCardRoute::cardId.name]

    init {
        observeDecks()
            .onEach { decks -> setState { copy(decks = decks) } }
            // Không tải được danh sách bộ thẻ thì vẫn lưu được thẻ vào bộ đang chọn, chỉ là không
            // đổi được sang bộ khác — nên chỉ ghi log, không chặn màn.
            .catch { e -> if (!isTestMode) Timber.e(e) }
            .launchIn(viewModelScope)
        if (cardId != null) loadCard(cardId)
    }

    private fun loadCard(cardId: String) = launchGuarded(showLoadingOverlay = true) {
        val card = getFlashcard(cardId)
        if (card == null) {
            navigator.showSnackBar(strings.get(R.string.deck_error_card_not_found))
            navigator.back()
        } else {
            setState {
                copy(
                    selectedDeckId = card.deckId,
                    source = card.source,
                    question = card.question,
                    answer = card.answer,
                    originalQuestion = card.question,
                    originalAnswer = card.answer,
                )
            }
        }
    }

    /** Rời màn. Việc hỏi lại "bỏ nội dung chưa lưu?" do màn hình lo trước khi gọi hàm này. */
    fun onClose() = navigator.back()

    fun onDeckSelected(deckId: String) = setState { copy(selectedDeckId = deckId) }
    fun onQuestionChange(value: String) = setState { copy(question = value) }
    fun onAnswerChange(value: String) = setState { copy(answer = value) }
    fun onKeepAddingChange(value: Boolean) = setState { copy(keepAdding = value) }

    fun onSave() = launchGuarded(
        showLoadingOverlay = true,
        onError = { e ->
            if (e is DeckException) {
                navigator.showSnackBar(e.displayMessage(strings))
                null
            } else {
                e
            }
        },
    ) {
        val state = currentState
        when {
            cardId != null -> {
                editFlashcard(EditFlashcardUseCase.Params(cardId, state.question, state.answer))
                navigator.showSnackBar(strings.get(R.string.manual_card_updated), SnackBarType.SUCCESS)
                navigator.back()
            }

            state.keepAdding -> {
                createCard(state)
                // Ở lại màn: xoá trắng hai ô để gõ thẻ kế tiếp. Không dùng snackbar ở đây vì nó
                // nằm sau bàn phím / đè lên nút Lưu; số thẻ đã lưu hiện ngay trong màn.
                setState { copy(question = "", answer = "", savedCount = savedCount + 1) }
                sendEffect(ManualCardEffect.FocusQuestion)
            }

            else -> {
                createCard(state)
                navigator.showSnackBar(strings.get(R.string.manual_card_added), SnackBarType.SUCCESS)
                navigator.back()
            }
        }
    }

    private suspend fun createCard(state: ManualCardState) {
        createManualFlashcard(
            CreateManualFlashcardUseCase.Params(
                deckId = state.selectedDeckId,
                question = state.question,
                answer = state.answer,
            ),
        )
    }

    private companion object {
        /** State ban đầu phải biết ngay mình đang thêm hay sửa, và bộ thẻ nào đang được chọn. */
        fun initialState(savedState: SavedStateHandle) = ManualCardState(
            isEditing = savedState.get<String>(ManualCardRoute::cardId.name) != null,
            selectedDeckId = checkNotNull(savedState[ManualCardRoute::deckId.name]),
            noteText = savedState.get<String>(ManualCardRoute::noteText.name).orEmpty(),
        )
    }
}
