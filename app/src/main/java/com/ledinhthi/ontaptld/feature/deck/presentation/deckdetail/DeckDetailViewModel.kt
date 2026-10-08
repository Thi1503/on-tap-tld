package com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.presentation.mvi.BaseViewModel
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.SnackBarType
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.DeleteDeckUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.DeleteFlashcardUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveDeckUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveFlashcardsUseCase
import com.ledinhthi.ontaptld.navigation.DeckDetailRoute
import com.ledinhthi.ontaptld.navigation.ManualCardRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

/** ViewModel của màn Chi tiết bộ thẻ (route `DeckDetailRoute`) — cùng khuôn với DeckListViewModel. */
@HiltViewModel
class DeckDetailViewModel @Inject constructor(
    toolbox: ViewModelToolbox,
    savedState: SavedStateHandle,
    private val observeDeck: ObserveDeckUseCase,
    private val observeCards: ObserveFlashcardsUseCase,
    private val deleteFlashcard: DeleteFlashcardUseCase,
    private val deleteDeck: DeleteDeckUseCase,
    private val clock: Clock,
) : BaseViewModel<DeckDetailState>(DeckDetailState(), toolbox) {

    // Navigation cất từng tham số của route vào SavedStateHandle dưới đúng tên thuộc tính, nên
    // `deckId` của DeckDetailRoute lấy ra bằng khoá "deckId". `DeckDetailRoute::deckId.name` cho
    // ra chuỗi "deckId" mà không phải gõ tay (đổi tên thuộc tính thì chỗ này tự đổi theo).
    private val deckId: String = checkNotNull(savedState[DeckDetailRoute::deckId.name])

    private var observeJob: Job? = null

    /** Đang xoá bộ thẻ: bỏ qua dữ liệu database phát về để màn không nháy sang "không tìm thấy". */
    private var isDeletingDeck = false

    init {
        observe()
    }

    private fun observe() {
        observeJob?.cancel()
        setState { copy(loadFailed = false, nowMillis = clock.nowMillis()) }
        // Bộ thẻ và danh sách thẻ là hai bảng khác nhau; `combine` gộp lại để màn chỉ chuyển từ
        // "đang tải" sang "có dữ liệu" khi ĐÃ CÓ CẢ HAI (không hiện tên trước, thẻ sau).
        observeJob = combine(observeDeck(deckId), observeCards(deckId)) { deck, cards -> deck to cards }
            .onEach { (deck, cards) ->
                if (!isDeletingDeck) {
                    setState { copy(deck = deck, cards = cards, isLoaded = true, loadFailed = false) }
                }
            }
            .catch { e ->
                if (!isTestMode) Timber.e(e)
                setState { copy(loadFailed = true) }
            }
            .launchIn(viewModelScope)
    }

    fun onRetry() = observe()

    fun onBack() = navigator.back()

    fun onFilterChange(filter: CardFilter) = setState { copy(filter = filter) }

    fun onAddCardClick() = navigator.to(ManualCardRoute(deckId))

    fun onEditCard(cardId: String) = navigator.to(ManualCardRoute(deckId, cardId))

    fun onDeleteCard(cardId: String) = launchGuarded {
        deleteFlashcard(cardId)
        navigator.showSnackBar(strings.get(R.string.deck_detail_card_deleted), SnackBarType.SUCCESS)
    }

    fun onDeleteDeck() {
        isDeletingDeck = true
        launchGuarded(
            showLoadingOverlay = true,
            onError = { e ->
                isDeletingDeck = false // xoá hỏng -> nghe dữ liệu lại như cũ
                e
            },
        ) {
            deleteDeck(deckId)
            navigator.showSnackBar(strings.get(R.string.deck_detail_deck_deleted), SnackBarType.SUCCESS)
            navigator.back()
        }
    }

    // Ôn tập và chụp ghi chú thuộc bước 3 và 4 của docs/KE_HOACH_PHAT_TRIEN.md.
    fun onReviewClick() = showComingSoon()
    fun onCaptureClick() = showComingSoon()

    private fun showComingSoon() =
        navigator.showSnackBar(strings.get(R.string.common_coming_soon), SnackBarType.INFO)
}
