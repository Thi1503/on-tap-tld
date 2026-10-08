package com.ledinhthi.ontaptld.core.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

/*
 * Bộ nút dùng chung. Màn hình KHÔNG gọi thẳng Button/OutlinedButton của Material mà gọi các
 * hàm ở đây, để mọi nút trong app cùng chiều cao, bo góc, màu và cỡ chữ theo thiết kế.
 *
 * Về các tham số có giá trị mặc định (`modifier = Modifier`, `enabled = true`…): nơi gọi chỉ
 * cần truyền những gì khác mặc định, vd `PrimaryButton(text = "Lưu", onClick = { … })`.
 */

/** Vùng chạm tối thiểu cho mọi nút chỉ có icon / chữ. */
private val MinTouchTarget = 44.dp

/** Nút hành động chính của màn (nền cam, chữ trắng). Mỗi màn chỉ nên có 1 nút loại này. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes leadingIcon: Int? = null,
    height: Dp = AppDimens.btnLoginFigmaHeight,
    containerColor: Color = appColors().primary,
) {
    val colors = appColors()
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = colors.textOnAccent,
            disabledContainerColor = colors.neutralSoft,
            disabledContentColor = colors.textHint,
        ),
        contentPadding = PaddingValues(horizontal = AppDimens.paddingMedium),
    ) { ButtonLabel(text, leadingIcon) }
}

/**
 * Nút phụ (viền xám, nền thẻ) — đi kèm [PrimaryButton] cho lựa chọn thứ hai.
 *
 * [leadingIcon] được tô theo màu chữ của nút. Hình cần giữ màu riêng (vd logo Google) thì tự vẽ
 * trong slot [leadingContent].
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes leadingIcon: Int? = null,
    height: Dp = AppDimens.btnLoginFigmaHeight,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    val colors = appColors()
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.cardBackground,
            contentColor = colors.textPrimary,
            disabledContentColor = colors.textHint,
        ),
        border = BorderStroke(1.dp, colors.borderStrong),
        contentPadding = PaddingValues(horizontal = AppDimens.paddingMedium),
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(Modifier.width(AppDimens.paddingSmall))
        }
        ButtonLabel(text, leadingIcon)
    }
}

/** Nút chữ (không nền) cho hành động nhẹ: "Bộ mới", "Chụp lại", "Bỏ qua"… */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes leadingIcon: Int? = null,
    contentColor: Color = appColors().primaryStrong,
    textStyle: TextStyle = MaterialTheme.typography.titleSmall,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = MinTouchTarget),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = AppDimens.paddingVerySmall),
    ) {
        if (leadingIcon != null) {
            Icon(painterResource(leadingIcon), contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(AppDimens.paddingSmallest))
        }
        Text(text, style = textStyle)
    }
}

/** Nút chỉ có icon. [contentDescription] bắt buộc vì không có chữ nào khác mô tả hành động. */
@Composable
fun AppIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = appColors().textPrimary,
    iconSize: Dp = AppDimens.sizeIconMedium,
) {
    IconButton(onClick = onClick, modifier = modifier.size(MinTouchTarget)) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = tint,
        )
    }
}

@Composable
private fun ButtonLabel(text: String, @DrawableRes leadingIcon: Int?) {
    if (leadingIcon != null) {
        Icon(
            painter = painterResource(leadingIcon),
            contentDescription = null,
            modifier = Modifier.size(AppDimens.sizeIcon),
        )
        Spacer(Modifier.width(AppDimens.paddingVerySmall))
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
