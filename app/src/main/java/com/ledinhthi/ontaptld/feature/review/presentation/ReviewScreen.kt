package com.ledinhthi.ontaptld.feature.review.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode
import com.ledinhthi.ontaptld.core.presentation.components.AppIconButton
import com.ledinhthi.ontaptld.core.presentation.components.BadgeTone
import com.ledinhthi.ontaptld.core.presentation.components.ErrorState
import com.ledinhthi.ontaptld.core.presentation.components.LoadingState
import com.ledinhthi.ontaptld.core.presentation.components.PrimaryButton
import com.ledinhthi.ontaptld.core.presentation.components.StateMessage
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.OnTapTldTheme
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.review.domain.ReviewGrade
import com.ledinhthi.ontaptld.feature.review.domain.model.ReviewCard
import com.ledinhthi.ontaptld.feature.review.presentation.components.GradeButtons
import com.ledinhthi.ontaptld.feature.review.presentation.components.ReviewAnswerCard
import com.ledinhthi.ontaptld.feature.review.presentation.components.ReviewDeckLabel
import com.ledinhthi.ontaptld.feature.review.presentation.components.ReviewDoneContent
import com.ledinhthi.ontaptld.feature.review.presentation.components.ReviewProgressHeader
import com.ledinhthi.ontaptld.feature.review.presentation.components.ReviewQuestionCard

/*
 * Cả phiên ôn là MỘT màn (một route, một ViewModel) đi qua nhiều chặng — xem `ReviewPhase`:
 * câu hỏi → đáp án + chấm → (thẻ kế tiếp…) → tổng kết. Không tách thành ba màn riêng vì ba
 * chặng dùng chung một trạng thái: hàng thẻ, vị trí hiện tại, kết quả đã chấm.
 */

/** Màn ôn tập (route `ReviewRoute`). */
@Composable
fun ReviewScreen(viewModel: ReviewViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Chạy mỗi lần màn này trở lại phía trước (kể cả khi quay về từ màn sửa thẻ) để nạp lại nội
    // dung thẻ đang ôn. LaunchedEffect không làm được việc này vì nó chỉ chạy lúc màn mới dựng.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    ReviewContent(
        state = state,
        onClose = viewModel::onClose,
        onRetry = viewModel::onRetry,
        onShowAnswer = viewModel::onShowAnswer,
        onEditCard = viewModel::onEditCard,
        onGrade = viewModel::onGrade,
        onGoHome = viewModel::onGoHome,
        onReviewForgotten = viewModel::onReviewForgotten,
    )
}

@Composable
private fun ReviewContent(
    state: ReviewState,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onShowAnswer: () -> Unit,
    onEditCard: () -> Unit,
    onGrade: (ReviewGrade) -> Unit,
    onGoHome: () -> Unit,
    onReviewForgotten: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(appColors().scaffoldBackground),
    ) {
        val current = state.current
        when {
            state.phase == ReviewPhase.DONE -> ReviewDoneContent(
                state = state,
                onClose = onClose,
                onGoHome = onGoHome,
                onReviewForgotten = onReviewForgotten,
            )

            state.phase == ReviewPhase.ERROR -> {
                CloseRow(onClose)
                ErrorState(
                    title = stringResource(R.string.review_error_title),
                    message = stringResource(R.string.home_error_message),
                    onRetry = onRetry,
                )
            }

            state.phase == ReviewPhase.EMPTY -> {
                CloseRow(onClose)
                StateMessage(
                    title = stringResource(R.string.review_empty_title),
                    message = stringResource(R.string.review_empty_message),
                    icon = R.drawable.ic_check,
                    tone = BadgeTone.Success,
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.common_back),
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Còn lại là LOADING, hoặc chặng câu hỏi / đáp án (khi đó luôn có thẻ hiện tại).
            current == null -> {
                CloseRow(onClose)
                LoadingState()
            }

            else -> ReviewingContent(
                state = state,
                current = current,
                onClose = onClose,
                onShowAnswer = onShowAnswer,
                onEditCard = onEditCard,
                onGrade = onGrade,
            )
        }
    }
}

/** Thân màn khi đang ôn một thẻ: đầu trang tiến độ + mặt câu hỏi hoặc mặt đáp án. */
@Composable
private fun ColumnScope.ReviewingContent(
    state: ReviewState,
    current: ReviewCard,
    onClose: () -> Unit,
    onShowAnswer: () -> Unit,
    onEditCard: () -> Unit,
    onGrade: (ReviewGrade) -> Unit,
) {
    ReviewProgressHeader(
        position = state.position,
        total = state.total,
        progress = state.progress,
        onClose = onClose,
    )
    ReviewDeckLabel(current.deck)
    // key(id thẻ): sang thẻ khác thì dựng lại phần thân từ đầu, để vị trí cuộn của thẻ trước
    // không dính sang thẻ sau.
    key(current.card.id) {
        if (state.phase == ReviewPhase.ANSWER) {
            ReviewAnswerCard(card = current.card, modifier = Modifier.weight(1f))
            GradeButtons(onGrade = onGrade)
        } else {
            ReviewQuestionCard(
                card = current.card,
                onShowAnswer = onShowAnswer,
                onEdit = onEditCard,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = stringResource(R.string.review_show_answer),
                onClick = onShowAnswer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppDimens.defaultPadding,
                        end = AppDimens.defaultPadding,
                        top = AppDimens.paddingSmall,
                        bottom = AppDimens.padding24,
                    ),
            )
        }
    }
}

/** Hàng chỉ có nút đóng — cho các chặng không có thanh tiến độ. */
@Composable
private fun CloseRow(onClose: () -> Unit) {
    Row(Modifier.padding(start = AppDimens.paddingVerySmall, top = AppDimens.paddingSmall)) {
        AppIconButton(
            icon = R.drawable.ic_close,
            contentDescription = stringResource(R.string.common_close),
            onClick = onClose,
        )
    }
}

// ---------------------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------------------

private val previewDeck = Deck("1", "Sinh học 12 – Di truyền", "#0D9488", createdAt = 0, updatedAt = 0)

private fun previewCard(id: String) = ReviewCard(
    card = Flashcard(
        id = id,
        deckId = "1",
        noteId = null,
        source = FlashcardSource.AI,
        question = "Hai nguyên tắc của quá trình nhân đôi ADN là gì?",
        answer = "Nguyên tắc bổ sung (A–T, G–X) và nguyên tắc bán bảo toàn.",
        dueDate = 0,
        createdAt = 0,
        updatedAt = 0,
    ),
    deck = previewDeck,
)

private val previewCards = List(24) { previewCard(it.toString()) }

private fun previewResults(): List<ReviewResult> {
    val grades = List(3) { ReviewGrade.FORGOT to 1 } + List(6) { ReviewGrade.HARD to 1 } +
        List(11) { ReviewGrade.EASY to 6 } + List(4) { ReviewGrade.VERY_EASY to 15 }
    return grades.mapIndexed { i, (grade, interval) -> ReviewResult(previewCards[i], grade, interval) }
}

@Composable
private fun PreviewHost(state: ReviewState, themeMode: ThemeMode = ThemeMode.LIGHT) {
    OnTapTldTheme(themeMode = themeMode) {
        ReviewContent(
            state = state,
            onClose = {},
            onRetry = {},
            onShowAnswer = {},
            onEditCard = {},
            onGrade = {},
            onGoHome = {},
            onReviewForgotten = {},
        )
    }
}

@Preview(name = "Ôn tập — câu hỏi", widthDp = 390, heightDp = 844)
@Composable
private fun ReviewQuestionPreview() =
    PreviewHost(ReviewState(phase = ReviewPhase.QUESTION, cards = previewCards, index = 4))

@Preview(name = "Ôn tập — đáp án", widthDp = 390, heightDp = 844)
@Composable
private fun ReviewAnswerPreview() =
    PreviewHost(ReviewState(phase = ReviewPhase.ANSWER, cards = previewCards, index = 4))

@Preview(name = "Ôn tập — đáp án — Dark", widthDp = 390, heightDp = 844)
@Composable
private fun ReviewAnswerDarkPreview() =
    PreviewHost(ReviewState(phase = ReviewPhase.ANSWER, cards = previewCards, index = 4), ThemeMode.DARK)

@Preview(name = "Ôn tập — tổng kết", widthDp = 390, heightDp = 844)
@Composable
private fun ReviewDonePreview() =
    PreviewHost(ReviewState(phase = ReviewPhase.DONE, cards = previewCards, index = 23, results = previewResults()))

@Preview(name = "Ôn tập — không có thẻ", widthDp = 390, heightDp = 844)
@Composable
private fun ReviewEmptyPreview() = PreviewHost(ReviewState(phase = ReviewPhase.EMPTY))
