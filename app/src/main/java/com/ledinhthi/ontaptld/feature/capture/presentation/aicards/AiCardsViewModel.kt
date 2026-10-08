package com.ledinhthi.ontaptld.feature.capture.presentation.aicards

import androidx.lifecycle.SavedStateHandle
import com.ledinhthi.ontaptld.core.presentation.mvi.BaseViewModel
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.GenerateFlashcardsWithAiUseCase
import com.ledinhthi.ontaptld.navigation.AiCardsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import javax.inject.Inject

/**
 * ViewModel của bước 3/3 (route `AiCardsRoute`): vừa mở là gửi văn bản ghi chú cho AI, chờ thẻ
 * đề xuất về rồi cho người dùng duyệt.
 */
@HiltViewModel
class AiCardsViewModel @Inject constructor(
    toolbox: ViewModelToolbox,
    savedState: SavedStateHandle,
    private val generateFlashcards: GenerateFlashcardsWithAiUseCase,
) : BaseViewModel<AiCardsState>(AiCardsState(), toolbox) {

    private val noteText: String = checkNotNull(savedState[AiCardsRoute::noteText.name])

    /** Giữ lại việc đang chạy để huỷ được nó khi người dùng bấm "Huỷ" / Back. */
    private var generateJob: Job? = null

    init {
        generate()
    }

    private fun generate() {
        // TẠM (tới khi có màn lỗi mạng / hết lượt riêng): lỗi nào cũng để bộ xử lý lỗi chung báo
        // bằng hộp thoại hoặc snackbar, rồi lùi về bước 2 — văn bản ở đó vẫn còn nguyên.
        generateJob = launchGuarded(
            onError = { e ->
                navigator.back()
                e // trả lại lỗi = "chưa xử lý xong", để bộ xử lý lỗi chung hiện thông báo
            },
        ) {
            val cards = generateFlashcards(noteText)
            setState { copy(phase = AiCardsPhase.Suggestions, cards = cards) }
        }
    }

    /**
     * Nút "Huỷ" và Back trong lúc chờ: bỏ lần gọi đang dở rồi lùi về bước 2. Kết quả của lần gọi
     * bị huỷ không được dùng và không bị trừ lượt.
     */
    fun onCancel() {
        generateJob?.cancel()
        navigator.back()
    }

    /** Nút ← khi đã có thẻ đề xuất. */
    fun onBack() = navigator.back()
}
