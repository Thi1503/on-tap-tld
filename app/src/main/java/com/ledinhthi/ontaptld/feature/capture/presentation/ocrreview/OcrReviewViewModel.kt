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
    private val savedState: SavedStateHandle,
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
        // Trạng thái ban đầu còn "đang nhận dạng" = màn mới mở lần đầu. Nếu app vừa được hệ thống
        // dựng lại sau khi bị tắt dưới nền thì kết quả cũ đã được nạp lại (xem `initialState`),
        // không nhận dạng lần nữa — kẻo đè mất phần người dùng đã sửa.
        if (currentState.ocrStatus == OcrStatus.Running) recognizeText()
    }

    private fun recognizeText() = launchGuarded(
        // Mọi lỗi nhận dạng đều được báo ngay trong màn (dòng nhắc đổi nội dung), không bật
        // snackbar hay hộp thoại — trả `null` để bộ xử lý lỗi chung không làm gì thêm.
        onError = { e ->
            val noText = e is AppException.OcrException && e.kind == OcrErrorKind.NO_TEXT_FOUND
            val status = if (noText) OcrStatus.NoText else OcrStatus.Failed
            setState { copy(ocrStatus = status) }
            savedState[KEY_OCR_STATUS] = status.name
            null
        },
    ) {
        val text = runOcr(currentState.imagePath)
        setState { copy(ocrStatus = OcrStatus.Found, text = text, recognizedText = text) }
        savedState[KEY_OCR_STATUS] = OcrStatus.Found.name
        savedState[KEY_RECOGNIZED_TEXT] = text
        savedState[KEY_TEXT] = text
    }

    // Mỗi thay đổi của người dùng được chép ngay vào SavedStateHandle — "ngăn kéo" mà Android
    // cất giúp khi tắt app dưới nền và trả lại khi người dùng quay về màn này.
    fun onTextChange(value: String) {
        setState { copy(text = value) }
        savedState[KEY_TEXT] = value
    }

    fun onDeckSelected(deckId: String) {
        setState { copy(selectedDeckId = deckId) }
        savedState[KEY_SELECTED_DECK] = deckId
    }

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
        onDeckSelected(deck.id)
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
        // Khoá của những thứ màn này tự cất. Phải khác tên tham số của route (`imagePath`,
        // `deckId`) vì Navigation cũng để tham số trong cùng SavedStateHandle.
        const val KEY_OCR_STATUS = "ocrStatus"
        const val KEY_RECOGNIZED_TEXT = "recognizedText"
        const val KEY_TEXT = "editedText"
        const val KEY_SELECTED_DECK = "selectedDeckId"

        fun initialState(savedState: SavedStateHandle): OcrReviewState {
            // Có trạng thái nhận dạng đã cất = app được dựng lại; không có = mở màn lần đầu.
            val savedStatus = savedState.get<String>(KEY_OCR_STATUS)
                ?.let { name -> OcrStatus.entries.firstOrNull { it.name == name } }
            return OcrReviewState(
                imagePath = checkNotNull(savedState[OcrReviewRoute::imagePath.name]),
                ocrStatus = savedStatus ?: OcrStatus.Running,
                text = savedState[KEY_TEXT] ?: "",
                recognizedText = savedState[KEY_RECOGNIZED_TEXT] ?: "",
                // Bộ thẻ người dùng tự chọn ở màn này được ưu tiên hơn bộ truyền sẵn theo route.
                selectedDeckId = savedState[KEY_SELECTED_DECK] ?: savedState[OcrReviewRoute::deckId.name],
            )
        }
    }
}
