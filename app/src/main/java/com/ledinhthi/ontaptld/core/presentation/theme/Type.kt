package com.ledinhthi.ontaptld.core.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.ledinhthi.ontaptld.R
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Nunito Sans — `res/font/nunito_sans.ttf` là bản VARIABLE font lấy từ kho Google Fonts
 * (giấy phép OFL, xem `assets/licenses/nunito_sans_ofl.txt`). Kho này không còn phát hành
 * file tĩnh từng độ đậm, nên mỗi độ đậm được khai bằng trục `wght` của cùng một file.
 * Trục biến thiên chỉ được Android hỗ trợ từ API 26; ở API 24–25 mọi độ đậm đều rơi về
 * nét Regular (hệ thống tự làm đậm giả cho Bold).
 */
@OptIn(ExperimentalTextApi::class)
private fun nunitoSans(weight: FontWeight) = Font(
    resId = R.font.nunito_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(weight, FontStyle.Normal),
)

val NunitoSansFontFamily = FontFamily(
    nunitoSans(FontWeight.W400),
    nunitoSans(FontWeight.W600),
    nunitoSans(FontWeight.W700),
    nunitoSans(FontWeight.W800),
)

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

private fun style(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
    fontFamily = NunitoSansFontFamily,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
)

/**
 * Typography Material3 theo thang chữ của bản thiết kế UI/UX — dùng qua
 * `MaterialTheme.typography.<slot>`. Tên slot ↔ vai trò:
 *  - display*: con số lớn (số thẻ cần ôn, chỉ số thống kê)
 *  - headline*: tiêu đề trang đầy màn (onboarding, hoàn thành, trạng thái rỗng)
 *  - title*: tiêu đề top bar / mục / tên thẻ
 *  - body*: nội dung
 *  - label*: nút, nhãn, badge
 */
val Typography = Typography(
    displayLarge = style(52, 52, FontWeight.W800),
    displayMedium = style(40, 44, FontWeight.W800),
    displaySmall = style(24, 30, FontWeight.W800),
    headlineLarge = style(26, 34, FontWeight.W800),
    headlineMedium = style(24, 32, FontWeight.W800),
    headlineSmall = style(20, 28, FontWeight.W800),
    titleLarge = style(18, 24, FontWeight.W800),
    titleMedium = style(16, 22, FontWeight.W700),
    titleSmall = style(14, 20, FontWeight.W700),
    bodyLarge = style(16, 23, FontWeight.W400),
    bodyMedium = style(14, 20, FontWeight.W400),
    bodySmall = style(12, 16, FontWeight.W400),
    labelLarge = style(16, 22, FontWeight.W700),
    labelMedium = style(14, 20, FontWeight.W600),
    labelSmall = style(12, 16, FontWeight.W700),
)
