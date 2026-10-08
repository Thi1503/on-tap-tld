package com.ledinhthi.ontaptld.feature.capture.presentation.ocrreview

import com.ledinhthi.ontaptld.core.presentation.mvi.UiState
import com.ledinhthi.ontaptld.core.presentation.mvi.UiStatus
import com.ledinhthi.ontaptld.feature.capture.domain.model.AiQuota
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck

/** Việc nhận dạng chữ trong ảnh đang ở đâu. */
enum class OcrStatus {
    /** Đang chạy — ô văn bản tạm khoá. */
    Running,

    /** Đã đọc được chữ và điền vào ô văn bản. */
    Found,

    /** Ảnh không có chữ nào đọc được — người dùng chụp lại hoặc tự gõ. */
    NoText,

    /** Bộ nhận dạng gặp lỗi — xử lý như [NoText] nhưng lời nhắc khác. */
    Failed,
}

data class OcrReviewState(
    override val status: UiStatus = UiStatus(),
    /** File ảnh đã cắt do màn chụp tạo ra. */
    val imagePath: String = "",
    val ocrStatus: OcrStatus = OcrStatus.Running,
    val text: String = "",
    /** Văn bản đúng như lúc nhận dạng xong — mốc so sánh để biết người dùng đã sửa gì chưa. */
    val recognizedText: String = "",
    val decks: List<Deck> = emptyList(),
    /** Bộ thẻ người dùng chọn (hoặc bộ được truyền sẵn từ Chi tiết bộ thẻ); null = chưa chọn. */
    val selectedDeckId: String? = null,
    /** null khi chưa đọc xong bộ đếm lượt. */
    val quota: AiQuota? = null,
) : UiState {
    /**
     * Bộ thẻ sẽ nhận các thẻ sắp tạo. Chưa chọn (hoặc bộ đã chọn vừa bị xoá) thì lấy bộ đầu
     * danh sách — danh sách xếp theo lần dùng gần nhất, nên đó là bộ vừa làm việc.
     */
    val selectedDeck: Deck?
        get() = decks.firstOrNull { it.id == selectedDeckId } ?: decks.firstOrNull()

    /** `trim()` để vài dấu cách thừa ở đầu / cuối không bị coi là đã sửa. */
    val hasEditedText: Boolean get() = text.trim() != recognizedText.trim()

    val canGenerate: Boolean
        get() = ocrStatus != OcrStatus.Running && text.isNotBlank() && selectedDeck != null

    override fun withStatus(status: UiStatus) = copy(status = status)
}
