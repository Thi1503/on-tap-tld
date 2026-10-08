package com.ledinhthi.ontaptld.feature.capture.presentation.aicards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.mvi.BaseViewModel
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.SnackBarType
import com.ledinhthi.ontaptld.feature.capture.domain.exception.CaptureException
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.GenerateFlashcardsWithAiUseCase
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.SaveSuggestedCardsUseCase
import com.ledinhthi.ontaptld.feature.capture.presentation.displayMessage
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveDeckUseCase
import com.ledinhthi.ontaptld.navigation.AiCardsRoute
import com.ledinhthi.ontaptld.navigation.DeckDetailRoute
import com.ledinhthi.ontaptld.navigation.HomeRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel của bước 3/3 (route `AiCardsRoute`): vừa mở là gửi văn bản ghi chú cho AI, chờ thẻ
 * đề xuất về, cho người dùng duyệt (chọn / sửa / xoá / thêm) rồi lưu vào bộ thẻ.
 */
@HiltViewModel
class AiCardsViewModel @Inject constructor(
    toolbox: ViewModelToolbox,
    private val savedState: SavedStateHandle,
    observeDeck: ObserveDeckUseCase,
    private val generateFlashcards: GenerateFlashcardsWithAiUseCase,
    private val saveSuggestedCards: SaveSuggestedCardsUseCase,
    private val json: Json,
) : BaseViewModel<AiCardsState>(AiCardsState(), toolbox) {

    private val imagePath: String = checkNotNull(savedState[AiCardsRoute::imagePath.name])
    private val noteText: String = checkNotNull(savedState[AiCardsRoute::noteText.name])
    private val deckId: String = checkNotNull(savedState[AiCardsRoute::deckId.name])

    /** Giữ lại việc đang chạy để huỷ được nó khi người dùng bấm "Huỷ" / Back. */
    private var generateJob: Job? = null

    /** Số sẽ cấp cho dòng kế tiếp trong danh sách duyệt — mỗi dòng một số, không dùng lại. */
    private var nextItemId = 0

    init {
        observeDeck(deckId)
            .onEach { deck -> setState { copy(deckName = deck?.name.orEmpty()) } }
            .catch { e -> if (!isTestMode) Timber.e(e) }
            .launchIn(viewModelScope)

        // Android có thể tắt hẳn app khi nó nằm dưới nền (vd lúc người dùng sang app email để gửi
        // báo cáo) rồi dựng lại khi quay về. ViewModel bị tạo mới, nhưng `SavedStateHandle` thì
        // được hệ thống giữ hộ. Có danh sách thẻ cất sẵn = đang duyệt dở: hiện lại đúng danh
        // sách đó, KHÔNG gọi AI lần nữa (vừa tốn lượt vừa ra thẻ khác).
        val restored = restoreItems()
        if (restored != null) {
            nextItemId = (restored.maxOfOrNull { it.id } ?: -1) + 1
            setState { copy(phase = AiCardsPhase.Suggestions, items = restored) }
        } else {
            generate()
        }
        // Từ lúc có thẻ để duyệt, mỗi lần danh sách đổi (tích, sửa, xoá, thêm) thì cất bản mới.
        // `distinctUntilChanged` bỏ các lần phát trùng nhau (state đổi chỗ khác, danh sách y nguyên).
        uiState
            .filter { it.phase == AiCardsPhase.Suggestions }
            .map { it.items }
            .distinctUntilChanged()
            .onEach { items -> savedState[KEY_ITEMS] = json.encodeToString(items) }
            .launchIn(viewModelScope)
    }

    /** Danh sách thẻ đã cất từ trước khi app bị tắt; null nếu chưa từng có (hoặc không đọc được). */
    private fun restoreItems(): List<SuggestionItem>? {
        val saved = savedState.get<String>(KEY_ITEMS) ?: return null
        return try {
            json.decodeFromString<List<SuggestionItem>>(saved)
        } catch (e: SerializationException) {
            null
        }
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
            val items = generateFlashcards(noteText).map { card ->
                SuggestionItem(
                    id = nextItemId++,
                    question = card.question,
                    answer = card.answer,
                    sourceLine = card.sourceLine,
                )
            }
            setState { copy(phase = AiCardsPhase.Suggestions, items = items) }
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

    /** Rời màn duyệt, bỏ các thẻ đề xuất. Việc hỏi lại trước khi bỏ do màn hình lo. */
    fun onBack() = navigator.back()

    // ---- Duyệt thẻ ---------------------------------------------------------------------------

    fun onToggleSelected(id: Int) = updateItem(id) { it.copy(selected = !it.selected) }

    fun onCardEdited(id: Int, question: String, answer: String) =
        updateItem(id) { it.copy(question = question.trim(), answer = answer.trim()) }

    /** "Thêm thẻ": thẻ người dùng tự gõ, đặt cuối danh sách và được chọn sẵn. */
    fun onCardAdded(question: String, answer: String) = setState {
        copy(
            items = items + SuggestionItem(
                id = nextItemId++,
                question = question.trim(),
                answer = answer.trim(),
                fromAi = false,
            ),
        )
    }

    fun onDeleteCard(id: Int) = setState { copy(items = items.filterNot { it.id == id }) }

    /** Thay dòng có [id] bằng kết quả của [change]; các dòng khác giữ nguyên. */
    private fun updateItem(id: Int, change: (SuggestionItem) -> SuggestionItem) = setState {
        copy(items = items.map { if (it.id == id) change(it) else it })
    }

    // ---- Lưu và báo cáo ----------------------------------------------------------------------

    fun onSave() {
        val selected = currentState.items.filter { it.selected }
        if (selected.isEmpty()) return
        launchGuarded(
            showLoadingOverlay = true,
            onError = { e ->
                if (e is CaptureException) {
                    navigator.showSnackBar(e.displayMessage(strings))
                    null
                } else {
                    e
                }
            },
        ) {
            saveSuggestedCards(
                SaveSuggestedCardsUseCase.Params(
                    deckId = deckId,
                    imagePath = imagePath,
                    noteText = noteText,
                    cards = selected.map {
                        SaveSuggestedCardsUseCase.CardDraft(it.question, it.answer, it.fromAi)
                    },
                ),
            )
            navigator.showSnackBar(
                strings.get(R.string.ai_suggestions_saved, selected.size),
                SnackBarType.SUCCESS,
            )
            // Gỡ cả luồng chụp (3 bước) khỏi chồng màn rồi mở bộ thẻ vừa nhận thẻ: bấm Back ở
            // đó sẽ về Home chứ không quay lại các bước đã xong.
            navigator.replaceAll(HomeRoute)
            navigator.to(DeckDetailRoute(deckId))
        }
    }

    /** Nút lá cờ: soạn sẵn nội dung các thẻ AI đang hiện để người dùng gửi báo cáo. */
    fun onReportClick() {
        val cardsText = currentState.items
            .filter { it.fromAi }
            .joinToString(separator = "\n\n") { "Q: ${it.question}\nA: ${it.answer}" }
        sendEffect(AiCardsEffect.ReportAiContent(cardsText))
    }

    /** Màn hình báo về: máy không có app email nào để mở. */
    fun onReportUnavailable() =
        navigator.showSnackBar(strings.get(R.string.ai_report_no_email_app))

    private companion object {
        /** Tên ngăn trong SavedStateHandle chứa danh sách thẻ đang duyệt (dạng JSON). */
        const val KEY_ITEMS = "suggestionItems"
    }
}
