package com.ledinhthi.ontaptld.feature.capture.presentation.aicards

import com.ledinhthi.ontaptld.core.presentation.mvi.UiState
import com.ledinhthi.ontaptld.core.presentation.mvi.UiStatus
import com.ledinhthi.ontaptld.feature.capture.domain.model.SuggestedCard

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

data class AiCardsState(
    override val status: UiStatus = UiStatus(),
    val phase: AiCardsPhase = AiCardsPhase.Generating,
    val cards: List<SuggestedCard> = emptyList(),
) : UiState {
    override fun withStatus(status: UiStatus) = copy(status = status)
}
