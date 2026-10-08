package com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Đầu trang Home: logo, tên app + ngày hôm nay, nút vào Cài đặt. */
@Composable
fun HomeHeader(onSettingsClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = appColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = AppDimens.defaultPadding,
                end = AppDimens.defaultPadding,
                top = AppDimens.paddingMedium,
                bottom = AppDimens.paddingSmall,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppLogoTile()
        Spacer(Modifier.width(AppDimens.paddingSmall))
        // weight(1f): cột này chiếm hết chỗ còn lại của hàng, đẩy nút Cài đặt sát mép phải.
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_display_name),
                // `semantics` không đổi hình thức — nó khai thông tin cho trình đọc màn hình
                // (TalkBack). Ở đây: "dòng này là tiêu đề" để người khiếm thị nhảy nhanh tới.
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
            )
            Text(
                text = todayLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        // Surface có `onClick` = một ô bấm được (tự có hiệu ứng gợn sóng và vai trò "nút").
        // 44dp là kích thước vùng chạm tối thiểu để ngón tay bấm trúng.
        Surface(
            onClick = onSettingsClick,
            modifier = Modifier.size(44.dp),
            shape = MaterialTheme.shapes.medium,
            color = colors.cardBackground,
            border = BorderStroke(1.dp, colors.cardBorder),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.home_settings),
                    modifier = Modifier.size(22.dp),
                    tint = colors.textStrong,
                )
            }
        }
    }
}

/**
 * Ô logo vuông bo góc "TLD". Mặc định là cỡ nhỏ ở đầu trang Home; Splash truyền [size], [shape],
 * [textStyle] lớn hơn. Thuần trang trí với trình đọc màn hình.
 */
@Composable
fun AppLogoTile(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = MaterialTheme.shapes.medium,
    textStyle: TextStyle = MaterialTheme.typography.titleSmall.copy(
        fontWeight = FontWeight.W800,
        letterSpacing = 0.5.sp,
    ),
) {
    val colors = appColors()
    Box(
        modifier = modifier
            .size(size)
            .background(colors.primary, shape)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "TLD", style = textStyle, color = colors.textOnAccent)
    }
}

/**
 * "Thứ Tư, 7 tháng 10" / "Wednesday, 7 October". Mẫu định dạng nằm trong strings.xml vì mỗi
 * ngôn ngữ viết ngày theo một kiểu (và tên tháng tiếng Việt do hệ thống trả về bị viết hoa).
 */
@Composable
private fun todayLabel(): String {
    val locale = LocalConfiguration.current.locales[0]
    val pattern = stringResource(R.string.home_date_pattern)
    return remember(locale, pattern) {
        LocalDate.now()
            .format(DateTimeFormatter.ofPattern(pattern, locale))
            .replaceFirstChar { it.titlecase(locale) }
    }
}
