package com.ledinhthi.ontaptld.feature.capture.presentation.aicards.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.AppBadge
import com.ledinhthi.ontaptld.core.presentation.components.AppIconButton
import com.ledinhthi.ontaptld.core.presentation.components.AppTextButton
import com.ledinhthi.ontaptld.core.presentation.components.BadgeTone
import com.ledinhthi.ontaptld.core.presentation.components.EmptyState
import com.ledinhthi.ontaptld.core.presentation.components.PrimaryButton
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.capture.presentation.aicards.AiCardsState
import com.ledinhthi.ontaptld.feature.capture.presentation.components.CaptureStepHeader

/**
 * Chặng duyệt thẻ ("Duyệt thẻ đề xuất"): danh sách thẻ AI soạn, mỗi thẻ có ô tích + sửa + xoá;
 * nút Lưu ở đáy ghi rõ sẽ lưu bao nhiêu thẻ vào bộ nào.
 */
@Composable
fun SuggestionsContent(
    state: AiCardsState,
    onBack: () -> Unit,
    onReportClick: () -> Unit,
    onAddClick: () -> Unit,
    onToggle: (Int) -> Unit,
    onEditClick: (Int) -> Unit,
    onDeleteClick: (Int) -> Unit,
    onSave: () -> Unit,
) {
    val colors = appColors()
    Column(Modifier.fillMaxSize().background(colors.scaffoldBackground)) {
        CaptureStepHeader(
            title = stringResource(R.string.ai_suggestions_title),
            step = 3,
            onNavigationClick = onBack,
            // Ở chặng này chỗ của chữ "Bước 3/3" nhường cho nút báo cáo nội dung AI.
            action = {
                AppIconButton(
                    icon = R.drawable.ic_flag,
                    contentDescription = stringResource(R.string.ai_report_button),
                    onClick = onReportClick,
                    tint = colors.textStrong,
                    iconSize = 22.dp,
                )
            },
        )
        HorizontalDivider(color = colors.cardBorder)

        SelectionSummary(
            selectedCount = state.selectedCount,
            totalCount = state.items.size,
            onAddClick = onAddClick,
        )

        if (state.items.isEmpty()) {
            // Người dùng đã xoá hết thẻ: vẫn ở lại màn để gõ thêm thẻ của riêng mình nếu muốn.
            EmptyState(
                title = stringResource(R.string.ai_suggestions_empty_title),
                modifier = Modifier.weight(1f),
                message = stringResource(R.string.ai_suggestions_empty_message),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = AppDimens.defaultPadding,
                    end = AppDimens.defaultPadding,
                    top = AppDimens.paddingSmallest,
                    bottom = AppDimens.defaultPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(AppDimens.padding10),
            ) {
                // `key` cho Compose biết dòng nào là dòng nào khi danh sách đổi (xoá / thêm),
                // để nó không vẽ lại cả danh sách và không lẫn trạng thái giữa các dòng.
                itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
                    SuggestionCardRow(
                        item = item,
                        position = index + 1,
                        onToggle = { onToggle(item.id) },
                        onEdit = { onEditClick(item.id) },
                        onDelete = { onDeleteClick(item.id) },
                    )
                }
            }
        }

        SaveBar(state = state, onSave = onSave)
    }
}

/** Hàng ngay dưới header: nhãn "AI", dòng "Đã chọn x / y thẻ" và nút "Thêm thẻ". */
@Composable
private fun SelectionSummary(selectedCount: Int, totalCount: Int, onAddClick: () -> Unit) {
    val colors = appColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = AppDimens.defaultPadding, end = AppDimens.paddingSmall, top = AppDimens.paddingVerySmall, bottom = AppDimens.paddingSmallest),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppBadge(text = stringResource(R.string.deck_source_ai), tone = BadgeTone.Purple, icon = R.drawable.ic_sparkle)
        Text(
            text = stringResource(R.string.ai_suggestions_selected_count, selectedCount, totalCount),
            modifier = Modifier
                .weight(1f)
                .padding(start = AppDimens.paddingVerySmall)
                // Con số đổi mỗi lần tích / bỏ tích: trình đọc màn hình tự đọc lại.
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        AppTextButton(
            text = stringResource(R.string.deck_detail_add_card),
            onClick = onAddClick,
            leadingIcon = R.drawable.ic_add,
        )
    }
}

/** Thanh đáy: lời nhắc đọc lại thẻ AI và nút Lưu. */
@Composable
private fun SaveBar(state: AiCardsState, onSave: () -> Unit) {
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
            Text(
                text = stringResource(R.string.ai_suggestions_disclaimer),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            PrimaryButton(
                text = pluralStringResource(
                    R.plurals.ai_suggestions_save,
                    state.selectedCount,
                    state.selectedCount,
                    state.deckName,
                ),
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSave,
            )
        }
    }
}
