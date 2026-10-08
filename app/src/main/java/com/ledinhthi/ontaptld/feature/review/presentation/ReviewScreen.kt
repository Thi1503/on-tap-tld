package com.ledinhthi.ontaptld.feature.review.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.ledinhthi.ontaptld.feature.review.domain.model.CardSource
import com.ledinhthi.ontaptld.feature.review.domain.model.ExcerptLine
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

/** Thời gian tấm thẻ lật từ mặt câu hỏi sang mặt đáp án. */
private const val FlipDurationMillis = 400

/**
 * Khoảng cách "máy quay" khi lật thẻ — số càng lớn thẻ càng ít méo lúc nghiêng. Đã thử 12 (con
 * số hay gặp trong các bài mẫu lật thẻ): với tấm thẻ rộng cỡ này mép thẻ vẫn phình quá to.
 */
private const val FlipCameraDistance = 32f

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
        onViewSourceImage = viewModel::onViewSourceImage,
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
    onViewSourceImage: () -> Unit,
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
                onViewSourceImage = onViewSourceImage,
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
    onViewSourceImage: () -> Unit,
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
    // Cũng nhờ `key` này mà thẻ MỚI luôn hiện thẳng mặt câu hỏi: góc lật bên dưới được tạo lại
    // từ 0, không quay ngược từ mặt đáp án của thẻ trước.
    key(current.card.id) {
        val showAnswer = state.phase == ReviewPhase.ANSWER
        // Góc lật của tấm thẻ: 0° = mặt câu hỏi, 180° = mặt đáp án. animateFloatAsState tự chạy
        // con số từ giá trị cũ sang giá trị mới trong 0,4 giây mỗi khi `showAnswer` đổi.
        val rotation by animateFloatAsState(
            targetValue = if (showAnswer) 180f else 0f,
            animationSpec = tween(durationMillis = FlipDurationMillis, easing = FastOutSlowInEasing),
            label = "cardFlip",
        )
        Box(
            Modifier
                .weight(1f)
                // graphicsLayer chỉ đổi cách VẼ (xoay quanh trục dọc), không đo / xếp lại bố cục
                // nên chạy mượt. cameraDistance đẩy "máy quay" ra xa: tấm thẻ này rộng gần hết
                // màn, để gần thì lúc nghiêng mép thẻ phình to, chờm cả lên thanh tiến độ.
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = FlipCameraDistance * density
                },
        ) {
            // Nửa đầu vòng lật vẽ mặt câu hỏi, qua 90° (lúc thẻ đang quay cạnh về phía người
            // xem) thì đổi sang mặt đáp án.
            if (rotation <= 90f) {
                ReviewQuestionCard(
                    card = current.card,
                    onShowAnswer = onShowAnswer,
                    onEdit = onEditCard,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ReviewAnswerCard(
                    card = current.card,
                    source = current.source,
                    onViewSourceImage = onViewSourceImage,
                    modifier = Modifier
                        .fillMaxSize()
                        // Cả khung đã quay quá nửa vòng nên mặt này đang bị nhìn từ "sau lưng";
                        // quay riêng nó thêm 180° để chữ không bị ngược như soi gương.
                        .graphicsLayer { rotationY = 180f },
                )
            }
        }
        // Phần đáy đổi theo mặt thẻ: nút "Hiện đáp án" mờ đi, 4 nút chấm hiện dần lên sau khi
        // thẻ đã quay qua nửa vòng. AnimatedContent cũng tự co giãn chiều cao giữa hai nội dung
        // nên tấm thẻ phía trên không bị giật.
        //
        // 4 nút chấm có mặt (và bấm được) ngay từ đầu hoạt ảnh dù lúc đó còn trong suốt, lại nằm
        // đúng chỗ nút "Hiện đáp án" vừa bấm. Vì vậy chỉ nhận lần chấm khi thẻ đã lật XONG —
        // lỡ bấm đúp "Hiện đáp án" sẽ không thành chấm nhầm một thẻ chưa kịp xem đáp án.
        val flipFinished = rotation == 180f
        AnimatedContent(
            targetState = showAnswer,
            transitionSpec = {
                fadeIn(tween(FlipDurationMillis / 2, delayMillis = FlipDurationMillis / 2)) togetherWith
                    fadeOut(tween(FlipDurationMillis / 2))
            },
            label = "reviewActions",
        ) { answerShown ->
            if (answerShown) {
                GradeButtons(onGrade = { grade -> if (flipFinished) onGrade(grade) })
            } else {
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
    source = CardSource(
        capturedAt = 1_791_158_400_000, // 5/10/2026
        excerpt = listOf(
            ExcerptLine("– Diễn ra ở pha S của kì trung gian.", isSource = false),
            ExcerptLine("– Nguyên tắc: bổ sung (A–T, G–X) và bán bảo toàn.", isSource = true),
            ExcerptLine("– ADN pôlimeraza tổng hợp mạch mới theo chiều 5'→3'.", isSource = false),
        ),
    ),
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
            onViewSourceImage = {},
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
