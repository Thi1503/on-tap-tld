package com.ledinhthi.ontaptld.feature.capture.presentation.aicards.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.AppIconButton
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.capture.presentation.aicards.SuggestionItem

/**
 * Một thẻ trong danh sách duyệt: ô tích, câu hỏi, câu trả lời, dòng nguồn và hai nút sửa / xoá.
 * Thẻ bỏ tích thì viền nét đứt và chữ nhạt đi — vẫn sửa được, nhưng sẽ không được lưu.
 *
 * @param position thứ tự của thẻ trong danh sách (từ 1), chỉ để đặt tên nút cho trình đọc màn hình.
 */
@Composable
fun SuggestionCardRow(
    item: SuggestionItem,
    position: Int,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = appColors()
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.cardBackground, shape)
            // `then(...)` nối thêm một Modifier được chọn theo điều kiện.
            .then(
                if (item.selected) {
                    Modifier.border(1.dp, colors.cardBorder, shape)
                } else {
                    Modifier.dashedBorder(colors.borderStrong, cornerRadius = AppDimens.radius12)
                },
            )
            .padding(start = AppDimens.paddingSmall, top = AppDimens.paddingSmall, end = AppDimens.paddingVerySmall, bottom = AppDimens.paddingSmallest),
    ) {
        // Chạm vào ô tích HOẶC phần chữ đều đổi trạng thái chọn — vùng chạm rộng hơn hẳn một ô
        // tích 22dp, và trình đọc màn hình đọc cả cụm như MỘT ô tích có nhãn là câu hỏi.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = item.selected, role = Role.Checkbox, onValueChange = { onToggle() })
                .padding(end = AppDimens.paddingSmallest),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
        ) {
            Checkbox(
                checked = item.selected,
                onCheckedChange = null, // việc bấm do Row bên ngoài lo
                modifier = Modifier.padding(top = 2.dp).size(22.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = colors.primary,
                    uncheckedColor = colors.borderStrong,
                    checkmarkColor = colors.textOnAccent,
                ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmallest)) {
                Text(
                    text = item.question,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (item.selected) colors.textPrimary else colors.textSecondary,
                )
                Text(
                    text = item.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }
        Row(
            // Thụt vào bằng đúng ô tích + khoảng cách, để dòng nguồn thẳng hàng với câu hỏi.
            modifier = Modifier.fillMaxWidth().padding(start = 22.dp + AppDimens.paddingSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceLabel(item, Modifier.weight(1f))
            AppIconButton(
                icon = R.drawable.ic_edit,
                contentDescription = stringResource(R.string.ai_suggestions_edit_card, position),
                onClick = onEdit,
                tint = colors.textStrong,
                iconSize = AppDimens.sizeIcon,
            )
            AppIconButton(
                icon = R.drawable.ic_delete,
                contentDescription = stringResource(R.string.ai_suggestions_delete_card, position),
                onClick = onDelete,
                tint = colors.errorText,
                iconSize = AppDimens.sizeIcon,
            )
        }
    }
}

/** Dòng chữ nhỏ dưới câu trả lời: thẻ lấy từ đâu, hoặc lời nhắc "sẽ không lưu" khi đã bỏ tích. */
@Composable
private fun SourceLabel(item: SuggestionItem, modifier: Modifier = Modifier) {
    val colors = appColors()
    val text = when {
        !item.selected -> stringResource(R.string.ai_suggestions_unselected)
        !item.fromAi -> stringResource(R.string.ai_suggestions_added_by_you)
        item.sourceLine != null -> stringResource(R.string.ai_suggestions_source_line, item.sourceLine)
        else -> null
    }
    Row(
        modifier = modifier.heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (text != null) {
            // Chỉ dòng "Nguồn: dòng N trong ảnh" mới có icon ảnh đứng trước.
            if (item.selected && item.fromAi) {
                Icon(
                    painter = painterResource(R.drawable.ic_image),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colors.textSecondary,
                )
                Spacer(Modifier.width(AppDimens.paddingSmallest))
            }
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}

/**
 * Viền nét đứt bo góc. Compose không có sẵn `border` nét đứt nên phải tự vẽ: `drawBehind` cho
 * vẽ thêm phía sau nội dung, còn `PathEffect.dashPathEffect` biến nét liền thành các gạch ngắn.
 */
private fun Modifier.dashedBorder(color: Color, cornerRadius: Dp) = drawBehind {
    val stroke = 1.dp.toPx()
    val dash = 4.dp.toPx()
    drawRoundRect(
        color = color,
        // Lùi vào nửa nét để nét vẽ nằm trọn trong khung, không bị cắt mất nửa ngoài.
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
    )
}
