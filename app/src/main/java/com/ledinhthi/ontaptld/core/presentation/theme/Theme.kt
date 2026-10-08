package com.ledinhthi.ontaptld.core.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode

/**
 * Ported từ `lib/base/themes/base_theme.dart` (`getThemeByAppTheme(bool isDark)`).
 * Khác biệt so với bản Flutter: `isDarkMode` không còn là cờ static toàn cục —
 * `themeMode` được truyền từ [com.ledinhthi.ontaptld.core.data.local.prefs.AppPreferences]
 * (DataStore) qua MainActivity, tương đương `ThemeCubit` bên Flutter.
 */
// Ánh xạ token ngữ nghĩa sang đủ các slot Material3 mà component gốc (TextField, Switch,
// Checkbox, BottomSheet, Snackbar…) tự đọc — để chúng ra đúng màu thiết kế mà không phải
// truyền `colors = …` ở từng nơi dùng.
//
// `with(x) { … }`: bên trong khối, viết `primary` được hiểu là `x.primary` — đỡ lặp tên.
//
// App có 2 "cửa" lấy màu, cùng một nguồn:
//   - appColors().xxx            -> token riêng của app (đầy đủ nhất, ưu tiên dùng trong màn hình)
//   - MaterialTheme.colorScheme  -> để component Material tự đọc; ít khi cần gọi trực tiếp
private val DarkColorScheme = with(DarkAppExtendedColors) {
    darkColorScheme(
        primary = primary,
        onPrimary = textOnAccent,
        primaryContainer = primarySoft,
        onPrimaryContainer = primaryStrong,
        secondary = primaryStrong,
        onSecondary = AppPalette.GrayDark7,
        secondaryContainer = neutralSoft,
        onSecondaryContainer = textStrong,
        background = scaffoldBackground,
        onBackground = textPrimary,
        surface = cardBackground,
        onSurface = textPrimary,
        // Material tự pha thêm màu `surfaceTint` (mặc định là màu cam chính) vào nền của các
        // bề mặt "nổi" như hộp chọn giờ, khiến chúng ám nâu. Đặt nó trùng màu nền thẻ = tắt
        // việc pha màu đó: mọi bề mặt giữ đúng màu trong bản thiết kế.
        surfaceTint = cardBackground,
        surfaceVariant = neutralSoft,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = scaffoldBackground,
        surfaceContainerLow = cardBackground,
        surfaceContainer = cardBackground,
        surfaceContainerHigh = cardBackground,
        surfaceContainerHighest = neutralSoft,
        outline = borderStrong,
        outlineVariant = border,
        error = errorText,
        onError = AppPalette.GrayDark7,
        errorContainer = statusRedBg,
        onErrorContainer = statusRedText,
        scrim = AppPalette.Black,
    )
}

private val LightColorScheme = with(LightAppExtendedColors) {
    lightColorScheme(
        primary = primary,
        onPrimary = textOnAccent,
        primaryContainer = primarySoft,
        onPrimaryContainer = primaryStrong,
        secondary = primaryStrong,
        onSecondary = AppPalette.White,
        secondaryContainer = neutralSoft,
        onSecondaryContainer = textStrong,
        background = scaffoldBackground,
        onBackground = textPrimary,
        surface = cardBackground,
        onSurface = textPrimary,
        surfaceTint = cardBackground, // xem ghi chú ở DarkColorScheme
        surfaceVariant = neutralSoft,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = cardBackground,
        surfaceContainerLow = cardBackground,
        surfaceContainer = cardBackground,
        surfaceContainerHigh = cardBackground,
        surfaceContainerHighest = neutralSoft,
        outline = borderStrong,
        outlineVariant = border,
        error = errorText,
        onError = AppPalette.White,
        errorContainer = statusRedBg,
        onErrorContainer = statusRedText,
        scrim = AppPalette.Black,
    )
}

/**
 * App đang hiển thị ở giao diện tối hay sáng — theo lựa chọn trong Cài đặt, có thể KHÁC với
 * chế độ của máy. Đọc bằng `LocalIsDarkTheme.current` ở những nơi cần biết điều này mà không
 * suy ra được từ màu (vd chọn màu icon cho thanh trạng thái).
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun OnTapTldTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Mặc định false để giữ đúng bộ nhận diện màu cam của TLD Tracker thay vì
    // để Material You lấy theo wallpaper người dùng (Android 12+). Bật true
    // nếu muốn tận dụng dynamic color của hệ thống thay vì brand cố định.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val extendedColors = if (darkTheme) DarkAppExtendedColors else LightAppExtendedColors
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalAppColors provides extendedColors, LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content,
        )
    }
}
