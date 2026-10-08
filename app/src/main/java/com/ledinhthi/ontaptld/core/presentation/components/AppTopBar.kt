package com.ledinhthi.ontaptld.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

enum class TopBarNavigation { None, Back, Close }

/**
 * Top bar chung của app (thay `TopAppBar` Material để khớp chiều cao/khoảng cách thiết kế).
 * Inset status bar do `Scaffold` gốc ở `AppNavHost` lo, component này không tự cộng thêm.
 *
 * @param titleLeading phần tử nhỏ đứng trước tiêu đề (vd chấm màu của bộ thẻ).
 * @param actions các nút bên phải — dùng [AppIconButton] hoặc chữ ngắn.
 *
 * Ví dụ:
 * ```
 * AppTopBar(title = "Cài đặt", onNavigationClick = viewModel::onBack)
 * AppTopBar(title = "Thêm thẻ", navigation = TopBarNavigation.Close, onNavigationClick = …) {
 *     AppIconButton(R.drawable.ic_more_vert, "Tuỳ chọn", onClick = …)   // slot `actions`
 * }
 * ```
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigation: TopBarNavigation = TopBarNavigation.Back,
    onNavigationClick: () -> Unit = {},
    showDivider: Boolean = true,
    containerColor: Color = appColors().appBarBackground,
    contentColor: Color = appColors().textPrimary,
    titleLeading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth().background(containerColor)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppDimens.paddingVerySmall,
                    end = AppDimens.paddingVerySmall,
                    top = AppDimens.paddingSmall,
                    bottom = AppDimens.paddingVerySmall,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (navigation) {
                TopBarNavigation.Back -> AppIconButton(
                    icon = R.drawable.ic_arrow_back,
                    contentDescription = stringResource(R.string.common_back),
                    onClick = onNavigationClick,
                    tint = contentColor,
                )

                TopBarNavigation.Close -> AppIconButton(
                    icon = R.drawable.ic_close,
                    contentDescription = stringResource(R.string.common_close),
                    onClick = onNavigationClick,
                    tint = contentColor,
                )

                TopBarNavigation.None -> Spacer(Modifier.width(AppDimens.paddingVerySmall))
            }
            Spacer(Modifier.width(AppDimens.paddingSmallest))
            if (titleLeading != null) {
                titleLeading()
                Spacer(Modifier.width(AppDimens.padding10))
            }
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            actions()
        }
        if (showDivider) HorizontalDivider(color = appColors().divider)
    }
}
