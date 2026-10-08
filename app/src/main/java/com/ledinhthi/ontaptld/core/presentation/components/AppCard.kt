package com.ledinhthi.ontaptld.core.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

/**
 * Thẻ nền chuẩn của app: nền `cardBackground`, viền mảnh `cardBorder`, bo góc 12, không đổ bóng.
 * Truyền [onClick] để cả thẻ bấm được (có ripple + vai trò button cho trình đọc màn hình).
 *
 * Tham số cuối [content] là một "slot": nơi gọi viết nội dung thẻ trong cặp ngoặc nhọn, vd
 * `AppCard { Text("Xin chào") }`. Kiểu `ColumnScope.() -> Unit` nghĩa là nội dung nằm trong một
 * Column, nên dùng được các Modifier riêng của Column như `weight` hay `align`.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    containerColor: Color = appColors().cardBackground,
    borderColor: Color = appColors().cardBorder,
    contentPadding: PaddingValues = PaddingValues(AppDimens.paddingSmall),
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = BorderStroke(1.dp, borderColor)
    val contentColor = appColors().textPrimary
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(contentPadding), content = content)
    }
    // Material có 2 phiên bản Surface: có `onClick` (bấm được) và không có. Không thể truyền
    // onClick = null vào bản bấm được, nên phải rẽ nhánh và gọi đúng phiên bản.
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            border = border,
            content = body,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            border = border,
            content = body,
        )
    }
}
