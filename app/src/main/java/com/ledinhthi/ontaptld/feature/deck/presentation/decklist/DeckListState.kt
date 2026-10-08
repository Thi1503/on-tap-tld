package com.ledinhthi.ontaptld.feature.deck.presentation.decklist

import com.ledinhthi.ontaptld.core.presentation.mvi.UiEffect
import com.ledinhthi.ontaptld.core.presentation.mvi.UiState
import com.ledinhthi.ontaptld.core.presentation.mvi.UiStatus
import com.ledinhthi.ontaptld.feature.deck.domain.model.DeckSummary

/**
 * Toàn bộ dữ liệu màn Home cần để vẽ. Là `data class` bất biến: muốn đổi thì ViewModel tạo bản
 * mới bằng `copy(...)` (xem `setState { copy(...) }`), không sửa trực tiếp field.
 */
data class DeckListState(
    override val status: UiStatus = UiStatus(),
    val decks: List<DeckSummary> = emptyList(),
    /** false cho tới khi Room phát lần đầu — lúc đó màn hiện skeleton, không hiện "rỗng". */
    val isLoaded: Boolean = false,
    val loadFailed: Boolean = false,
) : UiState {
    // Các giá trị dưới đây có `get()` nên được TÍNH từ `decks` mỗi lần đọc, không lưu riêng —
    // nhờ vậy không bao giờ lệch với danh sách bộ thẻ.
    val dueCount: Int get() = decks.sumOf { it.dueCount }
    val cardCount: Int get() = decks.sumOf { it.cardCount }

    /** Các bộ thẻ đang có thẻ cần ôn, nhiều nhất đứng trước — cho thanh phân bổ ở thẻ hero. */
    val dueDecks: List<DeckSummary>
        get() = decks.filter { it.dueCount > 0 }.sortedByDescending { it.dueCount }

    override fun withStatus(status: UiStatus) = copy(status = status)
}

/** Sự kiện một lần ViewModel gửi cho màn hình (nhận ở `ObserveEffects` trong DeckListScreen). */
sealed interface DeckListEffect : UiEffect {
    data object OpenCreateDeck : DeckListEffect
}
