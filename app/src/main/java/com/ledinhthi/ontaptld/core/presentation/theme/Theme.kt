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

    CompositionLocalProvider(LocalAppColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content,
        )
    }
}
