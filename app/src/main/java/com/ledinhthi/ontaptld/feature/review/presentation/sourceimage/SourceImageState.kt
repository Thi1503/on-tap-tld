package com.ledinhthi.ontaptld.feature.review.presentation.sourceimage

import com.ledinhthi.ontaptld.core.presentation.mvi.UiState
import com.ledinhthi.ontaptld.core.presentation.mvi.UiStatus
import com.ledinhthi.ontaptld.feature.deck.domain.model.SourceBox

/**
 * Dữ liệu màn xem ảnh nguồn của một thẻ AI.
 *
 * @param isLoading đang tìm xem thẻ thuộc ghi chú nào.
 * @param imagePath đường dẫn file ảnh ghi chú; null khi đang tìm, hoặc khi tìm không ra (thẻ /
 * ghi chú không còn) — phân biệt hai trường hợp bằng [isLoading].
 * @param box vùng của thẻ trên ảnh, tính theo tỉ lệ của ảnh; null = không biết, không tô sáng.
 */
data class SourceImageState(
    override val status: UiStatus = UiStatus(),
    val isLoading: Boolean = true,
    val imagePath: String? = null,
    val box: SourceBox? = null,
) : UiState {
    override fun withStatus(status: UiStatus) = copy(status = status)
}
