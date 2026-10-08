package com.ledinhthi.ontaptld.feature.capture.presentation.aicards

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode
import com.ledinhthi.ontaptld.core.exception.AiErrorKind
import com.ledinhthi.ontaptld.core.presentation.components.ConfirmDialog
import com.ledinhthi.ontaptld.core.presentation.components.LoadingOverlay
import com.ledinhthi.ontaptld.core.presentation.components.ObserveEffects
import com.ledinhthi.ontaptld.core.presentation.theme.OnTapTldTheme
import com.ledinhthi.ontaptld.feature.capture.presentation.aicards.components.AiErrorContent
import com.ledinhthi.ontaptld.feature.capture.presentation.aicards.components.AiQuotaContent
import com.ledinhthi.ontaptld.feature.capture.presentation.aicards.components.CardEditorSheet
import com.ledinhthi.ontaptld.feature.capture.presentation.aicards.components.GeneratingContent
import com.ledinhthi.ontaptld.feature.capture.presentation.aicards.components.SuggestionsContent

/** Giá trị đặc biệt của "đang sửa thẻ nào": không sửa thẻ có sẵn mà đang gõ thẻ mới. */
private const val NewCardId = -1

/**
 * Bước 3/3 của luồng tạo thẻ bằng AI (route `AiCardsRoute`). Một route, nhiều chặng: hàm này
 * nhìn `state.phase` rồi vẽ đúng nội dung của chặng đó.
 */
@Composable
fun AiCardsScreen(viewModel: AiCardsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (state.phase) {
        AiCardsPhase.Generating -> {
            // Back của hệ thống trong lúc chờ phải đi cùng một đường với nút "Huỷ": nếu để hệ
            // thống tự lùi màn thì lần gọi AI vẫn chạy ngầm cho tới khi xong.
            BackHandler(onBack = viewModel::onCancel)
            GeneratingContent(onCancel = viewModel::onCancel)
        }

        AiCardsPhase.Suggestions -> SuggestionsPhase(state = state, viewModel = viewModel)

        // Hai màn dưới không chặn Back của hệ thống: không có gì để mất, cứ lùi về bước 2.
        AiCardsPhase.Failed -> AiErrorContent(
            kind = state.failure ?: AiErrorKind.UNKNOWN,
            onBack = viewModel::onBack,
            onRetry = viewModel::onRetry,
            onTypeManually = viewModel::onTypeManually,
        )

        AiCardsPhase.QuotaExceeded -> AiQuotaContent(
            quotaMax = state.quotaMax,
            onBack = viewModel::onBack,
            onTypeManually = viewModel::onTypeManually,
        )
    }
}

/** Chặng duyệt thẻ, kèm các bảng / hộp thoại của nó: sửa thẻ, hỏi xoá thẻ, hỏi rời màn. */
@Composable
private fun SuggestionsPhase(state: AiCardsState, viewModel: AiCardsViewModel) {
    val context = LocalContext.current
    val reportSubject = stringResource(R.string.ai_report_email_subject)
    val reportIntro = stringResource(R.string.ai_report_email_intro)
    val supportEmail = stringResource(R.string.support_email)

    // Ba "câu hỏi đang mở" của màn, đều là state thuần giao diện. null = không có gì đang mở.
    var editingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var askLeave by rememberSaveable { mutableStateOf(false) }

    // Rời màn là mất các thẻ AI vừa soạn (và lượt AI đã dùng), nên luôn hỏi lại trước.
    BackHandler { askLeave = true }

    ObserveEffects(viewModel.effect) { effect ->
        when (effect) {
            is AiCardsEffect.ReportAiContent -> {
                val opened = context.openEmailComposer(
                    to = supportEmail,
                    subject = reportSubject,
                    body = "$reportIntro\n\n${effect.cardsText}",
                )
                if (!opened) viewModel.onReportUnavailable()
            }
        }
    }

    LoadingOverlay(isLoading = state.status.isLoadingOverlay) {
        SuggestionsContent(
            state = state,
            onBack = { askLeave = true },
            onReportClick = viewModel::onReportClick,
            onAddClick = { editingId = NewCardId },
            onToggle = viewModel::onToggleSelected,
            onEditClick = { editingId = it },
            onDeleteClick = { deletingId = it },
            onSave = viewModel::onSave,
        )
    }

    editingId?.let { id ->
        val item = state.items.firstOrNull { it.id == id }
        CardEditorSheet(
            isNew = id == NewCardId,
            initialQuestion = item?.question.orEmpty(),
            initialAnswer = item?.answer.orEmpty(),
            onDismiss = { editingId = null },
            onConfirm = { question, answer ->
                editingId = null
                if (id == NewCardId) viewModel.onCardAdded(question, answer) else viewModel.onCardEdited(id, question, answer)
            },
        )
    }

    deletingId?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.ai_suggestions_delete_title),
            message = stringResource(R.string.ai_suggestions_delete_message),
            confirmText = stringResource(R.string.common_delete),
            destructive = true,
            onConfirm = {
                deletingId = null
                viewModel.onDeleteCard(id)
            },
            onDismiss = { deletingId = null },
        )
    }

    if (askLeave) {
        ConfirmDialog(
            title = stringResource(R.string.ai_suggestions_leave_title),
            message = stringResource(R.string.ai_suggestions_leave_message),
            confirmText = stringResource(R.string.ocr_review_discard_confirm),
            destructive = true,
            onConfirm = {
                askLeave = false
                viewModel.onBack()
            },
            onDismiss = { askLeave = false },
        )
    }
}

/**
 * Mở app email của máy với thư đã điền sẵn người nhận, tiêu đề và nội dung; người dùng tự bấm
 * Gửi. Trả về false nếu máy không có app email nào.
 */
private fun Context.openEmailComposer(to: String, subject: String, body: String): Boolean {
    // `mailto:` + ACTION_SENDTO: chỉ các app EMAIL nhận lời mời này (không lẫn app nhắn tin, mạng xã hội).
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    return try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

// ---------------------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------------------

private val previewState = AiCardsState(
    phase = AiCardsPhase.Suggestions,
    deckName = "Sinh học 12",
    items = listOf(
        SuggestionItem(0, "Nhân đôi ADN diễn ra ở pha nào của chu kì tế bào?", "Pha S của kì trung gian.", sourceLine = 1),
        SuggestionItem(1, "Hai nguyên tắc của quá trình nhân đôi ADN là gì?", "Nguyên tắc bổ sung (A–T, G–X) và nguyên tắc bán bảo toàn.", sourceLine = 2),
        SuggestionItem(2, "ADN pôlimeraza tổng hợp mạch mới theo chiều nào?", "Chiều 5'→3'.", sourceLine = 3, selected = false),
        SuggestionItem(3, "Đoạn Okazaki là gì?", "Các đoạn ADN ngắn được tổng hợp gián đoạn trên mạch chậm.", sourceLine = 4),
    ),
)

@Composable
private fun SuggestionsPreviewHost(state: AiCardsState, themeMode: ThemeMode = ThemeMode.LIGHT) {
    OnTapTldTheme(themeMode = themeMode) {
        SuggestionsContent(
            state = state,
            onBack = {},
            onReportClick = {},
            onAddClick = {},
            onToggle = {},
            onEditClick = {},
            onDeleteClick = {},
            onSave = {},
        )
    }
}

@Preview(name = "Đang tạo thẻ", widthDp = 390, heightDp = 844)
@Composable
private fun GeneratingPreview() =
    OnTapTldTheme(themeMode = ThemeMode.LIGHT) { GeneratingContent(onCancel = {}) }

@Preview(name = "Đang tạo thẻ — Dark", widthDp = 390, heightDp = 844)
@Composable
private fun GeneratingDarkPreview() =
    OnTapTldTheme(themeMode = ThemeMode.DARK) { GeneratingContent(onCancel = {}) }

@Preview(name = "Duyệt thẻ đề xuất", widthDp = 390, heightDp = 844)
@Composable
private fun SuggestionsPreview() = SuggestionsPreviewHost(previewState)

@Preview(name = "Duyệt thẻ đề xuất — Dark", widthDp = 390, heightDp = 844)
@Composable
private fun SuggestionsDarkPreview() = SuggestionsPreviewHost(previewState, ThemeMode.DARK)

@Preview(name = "Duyệt thẻ đề xuất — đã xoá hết", widthDp = 390, heightDp = 844)
@Composable
private fun SuggestionsEmptyPreview() = SuggestionsPreviewHost(previewState.copy(items = emptyList()))

@Preview(name = "Lỗi mạng", widthDp = 390, heightDp = 844)
@Composable
private fun AiErrorPreview() = OnTapTldTheme(themeMode = ThemeMode.LIGHT) {
    AiErrorContent(kind = AiErrorKind.NETWORK, onBack = {}, onRetry = {}, onTypeManually = {})
}

@Preview(name = "Lỗi mạng — Dark", widthDp = 390, heightDp = 844)
@Composable
private fun AiErrorDarkPreview() = OnTapTldTheme(themeMode = ThemeMode.DARK) {
    AiErrorContent(kind = AiErrorKind.NETWORK, onBack = {}, onRetry = {}, onTypeManually = {})
}

@Preview(name = "Hết lượt", widthDp = 390, heightDp = 844)
@Composable
private fun AiQuotaPreview() = OnTapTldTheme(themeMode = ThemeMode.LIGHT) {
    AiQuotaContent(quotaMax = 10, onBack = {}, onTypeManually = {})
}

@Preview(name = "Hết lượt — Dark", widthDp = 390, heightDp = 844)
@Composable
private fun AiQuotaDarkPreview() = OnTapTldTheme(themeMode = ThemeMode.DARK) {
    AiQuotaContent(quotaMax = 10, onBack = {}, onTypeManually = {})
}
