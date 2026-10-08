package com.ledinhthi.ontaptld.feature.capture.presentation.ocrreview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode
import com.ledinhthi.ontaptld.core.image.BitmapLoader
import com.ledinhthi.ontaptld.core.presentation.components.AppCard
import com.ledinhthi.ontaptld.core.presentation.components.AppTextButton
import com.ledinhthi.ontaptld.core.presentation.components.ConfirmDialog
import com.ledinhthi.ontaptld.core.presentation.components.InfoBanner
import com.ledinhthi.ontaptld.core.presentation.components.LabeledTextField
import com.ledinhthi.ontaptld.core.presentation.components.LoadingOverlay
import com.ledinhthi.ontaptld.core.presentation.components.PrimaryButton
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.OnTapTldTheme
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.capture.domain.model.AiQuota
import com.ledinhthi.ontaptld.feature.capture.presentation.components.CaptureStepHeader
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components.CreateDeckSheet
import com.ledinhthi.ontaptld.feature.deck.presentation.manualcard.components.DeckSelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Số đo lấy từ bản design (OcrReview.dc.html).
private val ThumbnailSize = 60.dp
private val TextFieldHeight = 272.dp

/** Ảnh nhỏ 60dp chỉ cần đọc ở cỡ này (pixel) là đủ nét trên mọi màn hình. */
private const val ThumbnailMaxSide = 320

/** Hai lối rời màn có thể làm mất phần văn bản đã sửa — cần nhớ người dùng định đi lối nào. */
private enum class LeaveAction { Back, Retake }

/**
 * Màn Kiểm tra văn bản (route `OcrReviewRoute`) — bước 2/3 của luồng tạo thẻ bằng AI.
 * Cùng khuôn Screen → Content như các màn khác: hàm này nối với ViewModel và lo các hộp thoại,
 * [OcrReviewContent] chỉ vẽ.
 */
@Composable
fun OcrReviewScreen(viewModel: OcrReviewViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDeck by rememberSaveable { mutableStateOf(false) }

    // Đã sửa văn bản mà rời màn thì hỏi lại trước. null = không có câu hỏi nào đang chờ.
    var pendingLeave by rememberSaveable { mutableStateOf<LeaveAction?>(null) }
    fun leave(action: LeaveAction) = when (action) {
        LeaveAction.Back -> viewModel.onBack()
        LeaveAction.Retake -> viewModel.onRetake()
    }
    fun requestLeave(action: LeaveAction) {
        if (state.hasEditedText) pendingLeave = action else leave(action)
    }
    BackHandler(enabled = state.hasEditedText) { pendingLeave = LeaveAction.Back }

    val thumbnail by produceState<ImageBitmap?>(initialValue = null, state.imagePath) {
        value = withContext(Dispatchers.IO) {
            BitmapLoader.decodeUpright(state.imagePath, ThumbnailMaxSide)?.asImageBitmap()
        }
    }

    LoadingOverlay(isLoading = state.status.isLoadingOverlay) {
        OcrReviewContent(
            state = state,
            thumbnail = thumbnail,
            onBack = { requestLeave(LeaveAction.Back) },
            onRetake = { requestLeave(LeaveAction.Retake) },
            onTextChange = viewModel::onTextChange,
            onDeckSelected = viewModel::onDeckSelected,
            onCreateDeckClick = { showCreateDeck = true },
            onGenerateClick = viewModel::onGenerateClick,
        )
    }

    if (showCreateDeck) {
        CreateDeckSheet(
            onDismiss = { showCreateDeck = false },
            onConfirm = { name, colorHex ->
                showCreateDeck = false
                viewModel.onCreateDeck(name, colorHex)
            },
        )
    }

    // `?.let { … }`: chỉ chạy khối lệnh khi giá trị khác null, và trong khối gọi nó là `action`.
    pendingLeave?.let { action ->
        ConfirmDialog(
            title = stringResource(R.string.ocr_review_discard_title),
            message = stringResource(R.string.ocr_review_discard_message),
            // Nút xác nhận nói rõ việc sắp làm; nút còn lại là "Huỷ" mặc định = ở lại màn.
            confirmText = stringResource(
                when (action) {
                    LeaveAction.Back -> R.string.ocr_review_discard_confirm
                    LeaveAction.Retake -> R.string.capture_retake
                },
            ),
            destructive = true,
            onConfirm = {
                pendingLeave = null
                leave(action)
            },
            onDismiss = { pendingLeave = null },
        )
    }
}

@Composable
private fun OcrReviewContent(
    state: OcrReviewState,
    thumbnail: ImageBitmap?,
    onBack: () -> Unit,
    onRetake: () -> Unit,
    onTextChange: (String) -> Unit,
    onDeckSelected: (String) -> Unit,
    onCreateDeckClick: () -> Unit,
    onGenerateClick: () -> Unit,
) {
    val colors = appColors()
    val recognizing = state.ocrStatus == OcrStatus.Running
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.scaffoldBackground)
            // Bàn phím hiện lên thì cả màn co lại phía trên nó; phần giữa tự cuộn được.
            .imePadding(),
    ) {
        CaptureStepHeader(
            title = stringResource(R.string.ocr_review_title),
            step = 2,
            onNavigationClick = onBack,
        )
        HorizontalDivider(color = colors.cardBorder)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(AppDimens.defaultPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.defaultPadding),
        ) {
            CroppedImageCard(thumbnail = thumbnail, onRetake = onRetake)

            LabeledTextField(
                label = stringResource(R.string.ocr_review_text_label),
                value = state.text,
                onValueChange = onTextChange,
                // Chiều cao cố định như design: văn bản dài thì cuộn BÊN TRONG ô, để ô chọn bộ
                // thẻ phía dưới không bị đẩy đi xa.
                fieldModifier = Modifier.height(TextFieldHeight),
                placeholder = if (recognizing) stringResource(R.string.ocr_review_recognizing) else null,
                trailingLabel = if (recognizing) {
                    null
                } else {
                    pluralStringResource(R.plurals.ocr_review_char_count, state.text.length, state.text.length)
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
                enabled = !recognizing,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            when (state.ocrStatus) {
                OcrStatus.NoText -> InfoBanner(
                    text = stringResource(R.string.ocr_review_note_no_text),
                    icon = R.drawable.ic_warning,
                )

                OcrStatus.Failed -> InfoBanner(
                    text = stringResource(R.string.ocr_review_note_failed),
                    icon = R.drawable.ic_warning,
                )

                else -> InfoBanner(
                    text = stringResource(R.string.ocr_review_note_edit),
                    icon = R.drawable.ic_edit,
                )
            }

            DeckSelector(
                decks = state.decks,
                selected = state.selectedDeck,
                onSelect = onDeckSelected,
                label = stringResource(R.string.ocr_review_deck_label),
                placeholder = stringResource(R.string.ocr_review_deck_placeholder),
                onCreateNew = onCreateDeckClick,
            )
        }

        GenerateBar(quota = state.quota, enabled = state.canGenerate, onGenerateClick = onGenerateClick)
    }
}

/** Thẻ đầu màn: ảnh thu nhỏ của phần đã cắt, lời cam kết "ảnh không rời máy" và nút "Chụp lại". */
@Composable
private fun CroppedImageCard(thumbnail: ImageBitmap?, onRetake: () -> Unit) {
    val colors = appColors()
    AppCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
        ) {
            Box(
                Modifier
                    .size(ThumbnailSize)
                    .clip(RoundedCornerShape(AppDimens.radius8))
                    .background(colors.neutralSoft), // nền giữ chỗ trong lúc ảnh chưa đọc xong
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = null, // dòng "Ảnh đã cắt" bên cạnh đã mô tả
                        modifier = Modifier.fillMaxSize(),
                        // Crop: ảnh phủ kín ô vuông, phần thừa bị cắt đều hai phía.
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.ocr_review_cropped_image),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.ocr_review_on_device),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                    color = colors.textSecondary,
                )
            }
            AppTextButton(text = stringResource(R.string.capture_retake), onClick = onRetake)
        }
    }
}

/** Thanh đáy: số lượt AI còn lại hôm nay và nút hành động chính của màn. */
@Composable
private fun GenerateBar(quota: AiQuota?, enabled: Boolean, onGenerateClick: () -> Unit) {
    val colors = appColors()
    Column(Modifier.fillMaxWidth().background(colors.cardBackground)) {
        HorizontalDivider(color = colors.cardBorder)
        Column(
            modifier = Modifier.padding(
                start = AppDimens.defaultPadding,
                end = AppDimens.defaultPadding,
                top = AppDimens.padding10,
                bottom = AppDimens.paddingMedium,
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall),
        ) {
            if (quota != null) {
                Text(
                    text = stringResource(R.string.ocr_review_quota, quota.remaining, quota.max),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            PrimaryButton(
                text = stringResource(R.string.ocr_review_generate),
                onClick = onGenerateClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                leadingIcon = R.drawable.ic_sparkle,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------------------

private val previewDecks = listOf(
    Deck("1", "Sinh học 12 – Di truyền", "#0D9488", createdAt = 0, updatedAt = 0),
    Deck("2", "IELTS Vocabulary", "#4F46E5", createdAt = 0, updatedAt = 0),
)

private val previewText = """
    Bài 1 · Nhân đôi ADN
    – Diễn ra ở pha S của kì trung gian.
    – Nguyên tắc: bổ sung (A–T, G–X) và bán bảo toàn.
    – ADN pôlimeraza tổng hợp mạch mới theo chiều 5'→3'.
    – Mạch chậm: tổng hợp gián đoạn thành các đoạn Okazaki.
""".trimIndent()

private val previewState = OcrReviewState(
    imagePath = "",
    ocrStatus = OcrStatus.Found,
    text = previewText,
    recognizedText = previewText,
    decks = previewDecks,
    quota = AiQuota(remaining = 8, max = 10),
)

@Composable
private fun PreviewHost(state: OcrReviewState, themeMode: ThemeMode = ThemeMode.LIGHT) {
    OnTapTldTheme(themeMode = themeMode) {
        OcrReviewContent(
            state = state,
            thumbnail = null,
            onBack = {},
            onRetake = {},
            onTextChange = {},
            onDeckSelected = {},
            onCreateDeckClick = {},
            onGenerateClick = {},
        )
    }
}

@Preview(name = "Kiểm tra văn bản", widthDp = 390, heightDp = 844)
@Composable
private fun OcrReviewPreview() = PreviewHost(previewState)

@Preview(name = "Kiểm tra văn bản — Dark", widthDp = 390, heightDp = 844)
@Composable
private fun OcrReviewDarkPreview() = PreviewHost(previewState, ThemeMode.DARK)

@Preview(name = "Kiểm tra văn bản — đang nhận dạng", widthDp = 390, heightDp = 844)
@Composable
private fun OcrReviewRunningPreview() =
    PreviewHost(previewState.copy(ocrStatus = OcrStatus.Running, text = "", recognizedText = ""))

@Preview(name = "Kiểm tra văn bản — không có chữ, chưa có bộ thẻ", widthDp = 390, heightDp = 844)
@Composable
private fun OcrReviewNoTextPreview() = PreviewHost(
    previewState.copy(ocrStatus = OcrStatus.NoText, text = "", recognizedText = "", decks = emptyList()),
)
