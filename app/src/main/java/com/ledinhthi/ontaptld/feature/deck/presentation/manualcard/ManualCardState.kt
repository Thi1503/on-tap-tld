package com.ledinhthi.ontaptld.feature.deck.presentation.manualcard

import com.ledinhthi.ontaptld.core.presentation.mvi.UiEffect
import com.ledinhthi.ontaptld.core.presentation.mvi.UiState
import com.ledinhthi.ontaptld.core.presentation.mvi.UiStatus
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource

/** Câu hỏi và câu trả lời của một thẻ dài tối đa bấy nhiêu ký tự. */
const val MaxCardTextLength = 250

/**
 * Dữ liệu màn Thêm / sửa thẻ. Một màn dùng cho hai việc, phân biệt bằng [isEditing]:
 * thêm thẻ mới vào một bộ, hoặc sửa nội dung một thẻ đã có.
 */
data class ManualCardState(
    override val status: UiStatus = UiStatus(),
    val isEditing: Boolean = false,
    /** Nguồn của thẻ đang sửa (để hiện badge "AI" / "Thủ công"); thẻ thêm mới luôn là thủ công. */
    val source: FlashcardSource = FlashcardSource.MANUAL,
    val decks: List<Deck> = emptyList(),
    val selectedDeckId: String = "",
    val question: String = "",
    val answer: String = "",
    /** Nội dung lúc mới nạp thẻ (khi sửa) — mốc so sánh để biết người dùng đã đổi gì chưa. */
    val originalQuestion: String = "",
    val originalAnswer: String = "",
    /** Ô tick "Lưu xong thêm thẻ tiếp" — chỉ có ý nghĩa khi thêm thẻ. */
    val keepAdding: Boolean = true,
    /** Số thẻ đã lưu kể từ lúc mở màn (khi nhập liền nhiều thẻ) — để báo "Đã lưu N thẻ". */
    val savedCount: Int = 0,
) : UiState {
    /** null khi danh sách bộ thẻ chưa tải xong (hoặc bộ thẻ đã bị xoá). */
    val selectedDeck: Deck? get() = decks.firstOrNull { it.id == selectedDeckId }

    val canSave: Boolean get() = question.isNotBlank() && answer.isNotBlank()

    /**
     * Đang có nội dung chưa lưu? Khi thêm thẻ, hai mốc "original" là chuỗi rỗng nên chỉ cần gõ
     * gì đó là true; khi sửa thì phải khác với nội dung cũ. `trim()` để vài dấu cách thừa không
     * bị coi là thay đổi.
     */
    val hasUnsavedChanges: Boolean
        get() = question.trim() != originalQuestion.trim() || answer.trim() != originalAnswer.trim()

    override fun withStatus(status: UiStatus) = copy(status = status)
}

sealed interface ManualCardEffect : UiEffect {
    /** Vừa lưu xong một thẻ và ở lại màn: đưa con trỏ về ô Câu hỏi để gõ thẻ kế tiếp. */
    data object FocusQuestion : ManualCardEffect
}
