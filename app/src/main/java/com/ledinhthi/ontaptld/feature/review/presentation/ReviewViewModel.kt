package com.ledinhthi.ontaptld.feature.review.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ledinhthi.ontaptld.core.presentation.mvi.BaseViewModel
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.GetFlashcardUseCase
import com.ledinhthi.ontaptld.feature.review.domain.ReviewGrade
import com.ledinhthi.ontaptld.feature.review.domain.usecase.GetDueReviewCardsUseCase
import com.ledinhthi.ontaptld.feature.review.domain.usecase.ReviewFlashcardUseCase
import com.ledinhthi.ontaptld.navigation.HomeRoute
import com.ledinhthi.ontaptld.navigation.ManualCardRoute
import com.ledinhthi.ontaptld.navigation.ReviewRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel của phiên ôn tập (route `ReviewRoute`). Route có `deckId` = chỉ ôn thẻ của bộ đó;
 * không có = ôn mọi thẻ đến hạn.
 *
 * Một phiên gồm một hay nhiều LƯỢT: lượt đầu là các thẻ đến hạn; sau màn tổng kết người dùng có
 * thể mở lượt mới chỉ gồm những thẻ vừa chấm "Quên".
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    toolbox: ViewModelToolbox,
    savedState: SavedStateHandle,
    private val getDueCards: GetDueReviewCardsUseCase,
    private val reviewFlashcard: ReviewFlashcardUseCase,
    private val getFlashcard: GetFlashcardUseCase,
) : BaseViewModel<ReviewState>(ReviewState(), toolbox) {

    private val deckId: String? = savedState[ReviewRoute::deckId.name]

    /** Đang lưu kết quả chấm của một thẻ — chặn bấm hai nút liên tiếp chấm nhầm sang thẻ sau. */
    private var isGrading = false

    init {
        load()
    }

    private fun load() {
        setState { copy(phase = ReviewPhase.LOADING) }
        viewModelScope.launch {
            try {
                val cards = getDueCards(deckId)
                setState {
                    copy(
                        phase = if (cards.isEmpty()) ReviewPhase.EMPTY else ReviewPhase.QUESTION,
                        cards = cards,
                        index = 0,
                        results = emptyList(),
                    )
                }
            } catch (e: CancellationException) {
                throw e // coroutine bị huỷ (màn đã đóng) không phải lỗi — phải để nó lan tiếp
            } catch (e: Exception) {
                if (!isTestMode) Timber.e(e)
                setState { copy(phase = ReviewPhase.ERROR) }
            }
        }
    }

    fun onRetry() = load()

    fun onShowAnswer() {
        if (currentState.phase == ReviewPhase.QUESTION) setState { copy(phase = ReviewPhase.ANSWER) }
    }

    fun onGrade(grade: ReviewGrade) {
        val reviewing = currentState.current ?: return
        if (currentState.phase != ReviewPhase.ANSWER || isGrading) return
        isGrading = true
        launchGuarded(onFinally = { isGrading = false }) {
            val updated = reviewFlashcard(ReviewFlashcardUseCase.Params(reviewing.card.id, grade))
            setState {
                val isLast = index + 1 >= cards.size
                copy(
                    results = results + ReviewResult(reviewing, grade, updated.interval),
                    index = if (isLast) index else index + 1,
                    phase = if (isLast) ReviewPhase.DONE else ReviewPhase.QUESTION,
                )
            }
        }
    }

    /** Mở lượt mới chỉ gồm các thẻ vừa chấm "Quên". Mỗi lần chấm ở lượt này vẫn được ghi nhận. */
    fun onReviewForgotten() {
        val forgotten = currentState.results.filter { it.grade == ReviewGrade.FORGOT }.map { it.card }
        if (forgotten.isEmpty()) return
        setState { copy(phase = ReviewPhase.QUESTION, cards = forgotten, index = 0, results = emptyList()) }
    }

    fun onEditCard() {
        val reviewing = currentState.current ?: return
        navigator.to(ManualCardRoute(reviewing.card.deckId, reviewing.card.id))
    }

    /**
     * Màn ôn hiện lại (vd vừa quay về từ màn sửa thẻ): đọc lại thẻ đang ôn để câu hỏi / đáp án
     * trên màn là nội dung mới nhất, vì hàng thẻ của phiên chỉ là bản chụp lúc bắt đầu.
     */
    fun onResume() {
        val reviewing = currentState.current ?: return
        val phase = currentState.phase
        if (phase != ReviewPhase.QUESTION && phase != ReviewPhase.ANSWER) return
        launchGuarded(handleError = false) {
            val fresh = getFlashcard(reviewing.card.id) ?: return@launchGuarded
            setState {
                copy(cards = cards.map { if (it.card.id == fresh.id) it.copy(card = fresh) else it })
            }
        }
    }

    fun onClose() = navigator.back()

    fun onGoHome() = navigator.replaceAll(HomeRoute)
}
