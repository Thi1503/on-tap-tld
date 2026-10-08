package com.ledinhthi.ontaptld.core.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

/**
 * Dòng nhắc trên nền cam nhạt: icon nhỏ bên trái + một hai câu chữ. Dùng cho lời dặn nằm ngay
 * trong màn (không phải lỗi, không cần người dùng bấm gì).
 */
@Composable
fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = R.drawable.ic_info,
) {
    val colors = appColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.primarySoft, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = AppDimens.paddingSmall),
        // Chữ dài xuống dòng thì icon vẫn nằm ngang hàng với dòng chữ đầu tiên.
        horizontalArrangement = Arrangement.spacedBy(AppDimens.padding10),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(AppDimens.sizeIcon),
            tint = colors.primaryStrong,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textStrong,
        )
    }
}
