package com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail

import com.ledinhthi.ontaptld.core.domain.util.dueCutoffMillis
import com.ledinhthi.ontaptld.core.presentation.mvi.UiState
import com.ledinhthi.ontaptld.core.presentation.mvi.UiStatus
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource

/** Ba chip lọc phía trên danh sách thẻ. */
enum class CardFilter { ALL, MANUAL, AI }

/**
 * Dữ liệu màn Chi tiết bộ thẻ. Chỉ LƯU những thứ gốc (bộ thẻ, toàn bộ thẻ, chip đang chọn, mốc
 * thời gian); mọi con số hiển thị đều được tính từ đó ở các `get()` bên dưới.
 */
data class DeckDetailState(
    override val status: UiStatus = UiStatus(),
    /** null sau khi đã tải xong = bộ thẻ không còn tồn tại (đã bị xoá). */
    val deck: Deck? = null,
    val cards: List<Flashcard> = emptyList(),
    val filter: CardFilter = CardFilter.ALL,
    /** "Bây giờ" lúc mở màn — mốc để biết thẻ nào đến hạn hôm nay và còn mấy ngày nữa. */
    val nowMillis: Long = 0,
    val isLoaded: Boolean = false,
    val loadFailed: Boolean = false,
) : UiState {
    val manualCount: Int get() = cards.count { it.source == FlashcardSource.MANUAL }
    val aiCount: Int get() = cards.size - manualCount

    /** Cùng mốc "cần ôn hôm nay" với Home (xem `dueCutoffMillis`) để hai màn ra cùng một con số. */
    val dueCount: Int
        get() {
            val cutoff = dueCutoffMillis(nowMillis)
            return cards.count { it.dueDate <= cutoff }
        }

    /** Thẻ mới = chưa được ôn lần nào. */
    val newCount: Int get() = cards.count { it.lastReviewedAt == null }

    /** Danh sách sau khi áp chip lọc — đây mới là thứ được vẽ ra. */
    val visibleCards: List<Flashcard>
        get() = when (filter) {
            CardFilter.ALL -> cards
            CardFilter.MANUAL -> cards.filter { it.source == FlashcardSource.MANUAL }
            CardFilter.AI -> cards.filter { it.source == FlashcardSource.AI }
        }

    override fun withStatus(status: UiStatus) = copy(status = status)
}
