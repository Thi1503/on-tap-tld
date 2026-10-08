package com.ledinhthi.ontaptld.feature.capture.presentation.aicards

import com.ledinhthi.ontaptld.core.presentation.mvi.UiEffect
import com.ledinhthi.ontaptld.core.presentation.mvi.UiState
import com.ledinhthi.ontaptld.core.presentation.mvi.UiStatus
import kotlinx.serialization.Serializable

/**
 * Bước 3/3 là MỘT route đi qua nhiều chặng (cùng cách với phiên ôn tập — xem `ReviewPhase`),
 * để danh sách thẻ AI vừa soạn không phải chuyền qua lại giữa các màn.
 */
enum class AiCardsPhase {
    /** Đang chờ AI trả lời. */
    Generating,

    /** Đã có thẻ đề xuất để duyệt. */
    Suggestions,
}

/**
 * Một dòng trong danh sách duyệt: thẻ CHƯA lưu, người dùng còn chọn / sửa / xoá được.
 *
 * @param id số riêng của dòng trong phiên duyệt này (thẻ chưa lưu nên chưa có id thật).
 * @param sourceLine dòng ghi chú mà AI rút thẻ ra; null nếu không rõ hoặc thẻ do người dùng thêm.
 * @param fromAi false cho thẻ người dùng tự gõ thêm ngay trong màn này.
 *
 * `@Serializable` để cả danh sách ghi được thành chuỗi JSON cất vào `SavedStateHandle` (xem
 * AiCardsViewModel).
 */
@Serializable
data class SuggestionItem(
    val id: Int,
    val question: String,
    val answer: String,
    val sourceLine: Int? = null,
    val selected: Boolean = true,
    val fromAi: Boolean = true,
)

data class AiCardsState(
    override val status: UiStatus = UiStatus(),
    val phase: AiCardsPhase = AiCardsPhase.Generating,
    val items: List<SuggestionItem> = emptyList(),
    /** Tên bộ thẻ sẽ nhận thẻ — để ghi lên nút Lưu. Rỗng khi chưa tải xong. */
    val deckName: String = "",
) : UiState {
    val selectedCount: Int get() = items.count { it.selected }

    val canSave: Boolean get() = selectedCount > 0

    override fun withStatus(status: UiStatus) = copy(status = status)
}

sealed interface AiCardsEffect : UiEffect {
    /**
     * Nhờ màn hình mở app email với thư báo cáo soạn sẵn; [cardsText] là nội dung các thẻ AI
     * đang hiện. ViewModel không tự mở app khác được vì việc đó cần `Context` của màn hình.
     */
    data class ReportAiContent(val cardsText: String) : AiCardsEffect
}
