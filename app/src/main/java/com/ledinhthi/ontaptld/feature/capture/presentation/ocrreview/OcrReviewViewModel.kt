package com.ledinhthi.ontaptld.feature.capture.presentation.ocrreview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.OcrErrorKind
import com.ledinhthi.ontaptld.core.presentation.mvi.BaseViewModel
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.ObserveAiQuotaUseCase
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.RunOcrUseCase
import com.ledinhthi.ontaptld.feature.capture.presentation.CaptureFlowEvents
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.CreateDeckUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveDecksUseCase
import com.ledinhthi.ontaptld.feature.deck.presentation.displayMessage
import com.ledinhthi.ontaptld.navigation.AiCardsRoute
import com.ledinhthi.ontaptld.navigation.OcrReviewRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel của màn Kiểm tra văn bản (route `OcrReviewRoute`) — bước 2/3 của luồng tạo thẻ bằng
 * AI. Vừa mở là chạy nhận dạng chữ trên ảnh đã cắt; người dùng sửa văn bản, chọn bộ thẻ để lưu
 * rồi bấm "Tạo thẻ bằng AI".
 */
@HiltViewModel
class OcrReviewViewModel @Inject constructor(
    toolbox: ViewModelToolbox,
    savedState: SavedStateHandle,
    observeDecks: ObserveDecksUseCase,
    observeAiQuota: ObserveAiQuotaUseCase,
    private val runOcr: RunOcrUseCase,
    private val createDeck: CreateDeckUseCase,
    private val flowEvents: CaptureFlowEvents,
) : BaseViewModel<OcrReviewState>(initialState(savedState), toolbox) {

    init {
        observeDecks()
            .onEach { decks -> setState { copy(decks = decks) } }
            .catch { e -> if (!isTestMode) Timber.e(e) }
            .launchIn(viewModelScope)
        // Không đọc được bộ đếm lượt thì chỉ thiếu dòng "Còn x lượt", không chặn màn.
        observeAiQuota()
            .onEach { quota -> setState { copy(quota = quota) } }
            .catch { e -> if (!isTestMode) Timber.e(e) }
            .launchIn(viewModelScope)
        recognizeText()
    }

    private fun recognizeText() = launchGuarded(
        // Mọi lỗi nhận dạng đều được báo ngay trong màn (dòng nhắc đổi nội dung), không bật
        // snackbar hay hộp thoại — trả `null` để bộ xử lý lỗi chung không làm gì thêm.
        onError = { e ->
            val noText = e is AppException.OcrException && e.kind == OcrErrorKind.NO_TEXT_FOUND
            setState { copy(ocrStatus = if (noText) OcrStatus.NoText else OcrStatus.Failed) }
            null
        },
    ) {
        val text = runOcr(currentState.imagePath)
        setState { copy(ocrStatus = OcrStatus.Found, text = text, recognizedText = text) }
    }

    fun onTextChange(value: String) = setState { copy(text = value) }

    fun onDeckSelected(deckId: String) = setState { copy(selectedDeckId = deckId) }

    /** Tạo bộ thẻ mới từ dòng "Bộ thẻ mới" trong danh sách, rồi chọn luôn bộ vừa tạo. */
    fun onCreateDeck(name: String, colorHex: String) = launchGuarded(
        showLoadingOverlay = true,
        onError = { e ->
            if (e is DeckException) {
                navigator.showSnackBar(e.displayMessage(strings))
                null
            } else {
                e
            }
        },
    ) {
        val deck = createDeck(CreateDeckUseCase.Params(name, colorHex))
        setState { copy(selectedDeckId = deck.id) }
    }

    /** Nút ← / Back: lùi một bước, về ảnh đang đứng yên để kéo lại khung cắt. */
    fun onBack() = navigator.back()

    /** "Chụp lại": bỏ ảnh này, về camera đang chạy. Nhắn màn chụp trước rồi mới lùi về nó. */
    fun onRetake() {
        flowEvents.requestRetake()
        navigator.back()
    }

    /** "Tạo thẻ bằng AI": sang bước 3 với đúng văn bản đang có trong ô và bộ thẻ đang chọn. */
    fun onGenerateClick() {
        val state = currentState
        val deck = state.selectedDeck
        if (!state.canGenerate || deck == null) return
        navigator.to(AiCardsRoute(imagePath = state.imagePath, noteText = state.text.trim(), deckId = deck.id))
    }

    private companion object {
        fun initialState(savedState: SavedStateHandle) = OcrReviewState(
            imagePath = checkNotNull(savedState[OcrReviewRoute::imagePath.name]),
            selectedDeckId = savedState[OcrReviewRoute::deckId.name],
        )
    }
}
