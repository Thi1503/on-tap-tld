package com.ledinhthi.ontaptld.core.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Font gốc bên Flutter khai `fontFamily: 'NunitoSans'` nhưng KHÔNG có font file
 * thật trong `assets/fonts/` (chỉ `.gitkeep`) và không dùng package `google_fonts`
 * — tức bản Flutter đang fallback về font hệ thống mặc định. Giữ `FontFamily.Default`
 * ở đây là đúng hành vi thực tế hiện tại; nếu sau này muốn dùng đúng Nunito Sans,
 * tải `.ttf` vào `res/font/nunito_sans_*.ttf` rồi khai báo FontFamily(Font(...)).
 */
val NunitoSansFontFamily = FontFamily.Default

/**
 * Ported 1:1 từ `lib/base/themes/app_text_style.dart` (class `AppTextStyle`).
 * Tên hàm giữ đúng quy ước gốc `font<size><weight>` để dễ đối chiếu 2 codebase.
 */
object AppTextStyle {
    private fun base(size: TextUnit, weight: FontWeight, color: Color) =
        TextStyle(
            fontFamily = NunitoSansFontFamily,
            fontSize = size,
            fontWeight = weight,
            color = color,
        )

    @Composable
    fun font12Re() = base(AppDimens.fontSmallest, FontWeight.W400, appColors().textPrimary)
    @Composable
    fun font12Semi() = base(AppDimens.fontSmallest, FontWeight.W600, appColors().textPrimary)
    @Composable
    fun font14Re() = base(AppDimens.fontSmall, FontWeight.W400, appColors().textPrimary)
    @Composable
    fun font14Semi() = base(AppDimens.fontSmall, FontWeight.W600, appColors().textPrimary)
    @Composable
    fun font14Bo() = base(AppDimens.fontSmall, FontWeight.W700, appColors().textPrimary)
    @Composable
    fun font16Semi() = base(AppDimens.fontMedium, FontWeight.W600, appColors().textPrimary)
    @Composable
    fun font16Bo() = base(AppDimens.fontMedium, FontWeight.W700, appColors().textPrimary)
    @Composable
    fun font18Ex() = base(AppDimens.fontBig, FontWeight.W800, appColors().textPrimary)
    @Composable
    fun font20Semi() = base(AppDimens.fontBiggest, FontWeight.W600, appColors().textPrimary)
}

/** Ported từ `lib/const/style.dart` (class `AppStyle`). */
object AppStyle {
    @Composable
    fun textStyleDefault() = TextStyle(color = appColors().textPrimary, fontSize = AppDimens.fontMedium)

    @Composable
    fun textHintStyle() = TextStyle(color = AppPalette.PrefixIconColor, fontSize = AppDimens.fontMedium)

    @Composable
    fun textTitleStyle() = TextStyle(
        fontWeight = FontWeight.Bold,
        color = AppPalette.White,
        fontSize = AppDimens.fontBiggest,
    )

    @Composable
    fun textTitleMediumStyle() = TextStyle(
        fontWeight = FontWeight.Bold,
        color = AppPalette.White,
        fontSize = AppDimens.fontBig,
    )

    @Composable
    fun textSubTitleStyle() = TextStyle(
        fontWeight = FontWeight.Bold,
        color = AppPalette.White,
        fontSize = AppDimens.fontMedium,
    )

    @Composable
    fun textTitleWhiteStyle() = TextStyle(
        fontWeight = FontWeight.Bold,
        color = appColors().textPrimary,
        fontSize = AppDimens.fontBiggest,
    )
}

/** Typography Material3 dựng từ cùng thang size — dùng trong MaterialTheme(typography = Typography). */
val Typography = Typography(
    bodySmall = TextStyle(fontFamily = NunitoSansFontFamily, fontSize = 12.sp, fontWeight = FontWeight.W400),
    bodyMedium = TextStyle(fontFamily = NunitoSansFontFamily, fontSize = 14.sp, fontWeight = FontWeight.W400),
    bodyLarge = TextStyle(fontFamily = NunitoSansFontFamily, fontSize = 16.sp, fontWeight = FontWeight.W400),
    titleSmall = TextStyle(fontFamily = NunitoSansFontFamily, fontSize = 16.sp, fontWeight = FontWeight.W600),
    titleMedium = TextStyle(fontFamily = NunitoSansFontFamily, fontSize = 18.sp, fontWeight = FontWeight.W800),
    titleLarge = TextStyle(fontFamily = NunitoSansFontFamily, fontSize = 20.sp, fontWeight = FontWeight.W600),
    labelLarge = TextStyle(fontFamily = NunitoSansFontFamily, fontSize = 14.sp, fontWeight = FontWeight.W700),
)
