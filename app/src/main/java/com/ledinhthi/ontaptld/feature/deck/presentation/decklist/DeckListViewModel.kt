package com.ledinhthi.ontaptld.feature.deck.presentation.decklist

import androidx.lifecycle.viewModelScope
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.mvi.BaseViewModel
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.SnackBarType
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.CreateDeckUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveDeckSummariesUseCase
import com.ledinhthi.ontaptld.feature.deck.presentation.displayMessage
import com.ledinhthi.ontaptld.navigation.DeckDetailRoute
import com.ledinhthi.ontaptld.navigation.ManualCardRoute
import com.ledinhthi.ontaptld.navigation.ReviewRoute
import com.ledinhthi.ontaptld.navigation.SettingsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

/**
 * "Bộ não" của màn Home: giữ [DeckListState] và xử lý mọi thao tác của người dùng.
 * Màn hình chỉ gọi các hàm `onXxx()` ở đây; ViewModel không biết gì về Compose.
 *
 * `@HiltViewModel` + `@Inject constructor`: Hilt tự tạo và đưa các tham số vào — không ai
 * `new DeckListViewModel(...)` bằng tay (trừ unit test, xem DeckListViewModelTest).
 */
@HiltViewModel
class DeckListViewModel @Inject constructor(
    toolbox: ViewModelToolbox,
    private val observeDeckSummaries: ObserveDeckSummariesUseCase,
    private val createDeck: CreateDeckUseCase,
) : BaseViewModel<DeckListState>(DeckListState(), toolbox) {

    /** Giữ lại "đường ống" đang nghe database để huỷ được nó khi người dùng bấm Thử lại. */
    private var observeJob: Job? = null

    /** Người dùng bấm "gõ thẻ" khi chưa có bộ thẻ nào: tạo bộ thẻ xong thì vào thẳng màn thêm thẻ. */
    private var addCardAfterCreate = false

    init {
        observeDecks()
    }

    /**
     * Bắt đầu NGHE database, không phải đọc một lần: `observeDeckSummaries()` trả về một Flow
     * (dòng dữ liệu), và Room tự phát danh sách mới mỗi khi bảng đổi. Vì thế thêm/xoá bộ thẻ ở
     * đâu thì Home cũng tự cập nhật, không cần lệnh "tải lại".
     */
    private fun observeDecks() {
        observeJob?.cancel()
        setState { copy(loadFailed = false) }
        observeJob = observeDeckSummaries()
            // Mỗi lần có dữ liệu mới:
            .onEach { decks -> setState { copy(decks = decks, isLoaded = true, loadFailed = false) } }
            // Lỗi khi đọc (vd database hỏng) -> màn hiện trạng thái lỗi kèm nút Thử lại:
            .catch { e ->
                if (!isTestMode) Timber.e(e)
                setState { copy(loadFailed = true) }
            }
            // Chạy dòng trên trong "phạm vi sống" của ViewModel: màn bị đóng hẳn thì tự dừng nghe.
            .launchIn(viewModelScope)
    }

    fun onRetry() = observeDecks()

    fun onNewDeckClick() {
        addCardAfterCreate = false
        sendEffect(DeckListEffect.OpenCreateDeck)
    }

    fun onCreateDeckDismissed() {
        addCardAfterCreate = false
    }

    // `launchGuarded` (của BaseViewModel) chạy khối lệnh ở nền, tự bật/tắt lớp phủ loading và tự
    // bắt lỗi. `onError` cho phép màn này tự xử lý vài lỗi trước; trả `null` = "đã xử lý xong".
    fun onCreateDeck(name: String, colorHex: String) = launchGuarded(
        showLoadingOverlay = true,
        onError = { e ->
            if (e is DeckException) {
                navigator.showSnackBar(e.displayMessage(strings))
                null // đã xử lý xong
            } else {
                e // để GlobalExceptionHandler lo
            }
        },
    ) {
        val deck = createDeck(CreateDeckUseCase.Params(name, colorHex))
        if (addCardAfterCreate) {
            addCardAfterCreate = false
            navigator.to(ManualCardRoute(deck.id))
        }
    }

    fun onDeckClick(deckId: String) = navigator.to(DeckDetailRoute(deckId))

    fun onManualCardClick() {
        // Danh sách xếp theo lần sửa gần nhất -> bộ đầu tiên là bộ người dùng vừa làm việc.
        val recentDeck = currentState.decks.firstOrNull()
        if (recentDeck == null) {
            addCardAfterCreate = true
            sendEffect(DeckListEffect.OpenCreateDeck)
        } else {
            navigator.to(ManualCardRoute(recentDeck.deck.id))
        }
    }

    fun onSettingsClick() = navigator.to(SettingsRoute)

    /** "Ôn ngay" trên Home: ôn mọi thẻ đến hạn, không giới hạn theo bộ. */
    fun onReviewClick() = navigator.to(ReviewRoute())

    // Chụp ghi chú thuộc bước 4 của docs/KE_HOACH_PHAT_TRIEN.md.
    fun onCaptureClick() = showComingSoon()

    private fun showComingSoon() =
        navigator.showSnackBar(strings.get(R.string.common_coming_soon), SnackBarType.INFO)
}
