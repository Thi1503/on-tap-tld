package com.ledinhthi.ontaptld.feature.capture.presentation.aicards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode
import com.ledinhthi.ontaptld.core.presentation.components.AppCard
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.OnTapTldTheme
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.capture.domain.model.SuggestedCard
import com.ledinhthi.ontaptld.feature.capture.presentation.aicards.components.GeneratingContent
import com.ledinhthi.ontaptld.feature.capture.presentation.components.CaptureStepHeader

/**
 * Bước 3/3 của luồng tạo thẻ bằng AI (route `AiCardsRoute`). Một route, nhiều chặng: hàm này
 * chỉ việc nhìn `state.phase` rồi vẽ đúng nội dung của chặng đó.
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

        AiCardsPhase.Suggestions -> SuggestionsPreviewContent(cards = state.cards, onBack = viewModel::onBack)
    }
}

/**
 * TẠM: chỉ liệt kê thẻ AI vừa soạn để kiểm tra kết quả gọi Gemini. Màn "Duyệt thẻ đề xuất" thật
 * (chọn / sửa / xoá / thêm / lưu) sẽ thay hàm này ở phần việc kế tiếp.
 */
@Composable
private fun SuggestionsPreviewContent(cards: List<SuggestedCard>, onBack: () -> Unit) {
    val colors = appColors()
    Column(Modifier.fillMaxSize().background(colors.scaffoldBackground)) {
        CaptureStepHeader(
            title = stringResource(R.string.ai_suggestions_title),
            step = 3,
            onNavigationClick = onBack,
        )
        HorizontalDivider(color = colors.cardBorder)
        // LazyColumn = danh sách cuộn chỉ dựng những dòng đang nằm trong màn hình.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppDimens.defaultPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.padding10),
        ) {
            items(cards) { card ->
                AppCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmallest)) {
                        Text(
                            text = card.question,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = card.answer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                        if (card.sourceLine != null) {
                            Text(
                                text = stringResource(R.string.ai_suggestions_source_line, card.sourceLine),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.common_coming_soon),
                    modifier = Modifier.padding(top = AppDimens.paddingVerySmall),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------------------

@Preview(name = "Đang tạo thẻ", widthDp = 390, heightDp = 844)
@Composable
private fun GeneratingPreview() =
    OnTapTldTheme(themeMode = ThemeMode.LIGHT) { GeneratingContent(onCancel = {}) }

@Preview(name = "Đang tạo thẻ — Dark", widthDp = 390, heightDp = 844)
@Composable
private fun GeneratingDarkPreview() =
    OnTapTldTheme(themeMode = ThemeMode.DARK) { GeneratingContent(onCancel = {}) }

@Preview(name = "Thẻ đề xuất (tạm)", widthDp = 390, heightDp = 844)
@Composable
private fun SuggestionsPreview() = OnTapTldTheme(themeMode = ThemeMode.LIGHT) {
    SuggestionsPreviewContent(
        cards = listOf(
            SuggestedCard("Nhân đôi ADN diễn ra ở pha nào của chu kì tế bào?", "Pha S của kì trung gian.", 1),
            SuggestedCard("Đoạn Okazaki là gì?", "Các đoạn ADN ngắn được tổng hợp gián đoạn trên mạch chậm.", 4),
        ),
        onBack = {},
    )
}
