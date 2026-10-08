package com.ledinhthi.ontaptld.feature.review.presentation.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.theme.AppPalette
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.review.domain.ReviewGrade

/**
 * Hình thức của một mức chấm: màu nhấn (vạch trên nút, đoạn trên thanh tổng kết), nền nút, tên
 * mức và dòng mô tả. Gom vào một chỗ để nút chấm và màn tổng kết luôn dùng cùng màu, cùng chữ.
 */
@Immutable
data class GradeStyle(
    val accent: Color,
    val background: Color,
    @StringRes val label: Int,
    @StringRes val description: Int,
)

@Composable
fun ReviewGrade.style(): GradeStyle {
    val colors = appColors()
    return when (this) {
        ReviewGrade.FORGOT -> GradeStyle(
            accent = AppPalette.ColorE11D48,
            background = colors.statusRedBg,
            label = R.string.review_grade_forgot,
            description = R.string.review_grade_forgot_desc,
        )

        ReviewGrade.HARD -> GradeStyle(
            accent = AppPalette.ColorD97706,
            background = colors.statusOrangeBg,
            label = R.string.review_grade_hard,
            description = R.string.review_grade_hard_desc,
        )

        ReviewGrade.EASY -> GradeStyle(
            accent = AppPalette.Color0B8E3F,
            background = colors.statusGreenBg,
            label = R.string.review_grade_easy,
            description = R.string.review_grade_easy_desc,
        )

        ReviewGrade.VERY_EASY -> GradeStyle(
            accent = AppPalette.Color2563EB,
            // Token `statusBlueBg` ở dark ngả xanh ngọc; thiết kế dùng đúng xanh dương của vạch.
            background = if (colors.isDark) AppPalette.Color2563EB.copy(alpha = 0.26f) else colors.statusBlueBg,
            label = R.string.review_grade_very_easy,
            description = R.string.review_grade_very_easy_desc,
        )
    }
}
