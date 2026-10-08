package com.ledinhthi.ontaptld.feature.review.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.review.domain.ReviewGrade

/**
 * Bốn nút chấm dưới mặt đáp án. Mỗi nút ghi TÊN MỨC và một dòng mô tả cảm giác nhớ — cố ý
 * không ghi "ôn lại sau mấy ngày" (xem Mục 4 của docs/KE_HOACH_PHAT_TRIEN.md).
 */
@Composable
fun GradeButtons(onGrade: (ReviewGrade) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = AppDimens.defaultPadding,
                end = AppDimens.defaultPadding,
                top = AppDimens.defaultPadding,
                bottom = AppDimens.padding24,
            ),
        verticalArrangement = Arrangement.spacedBy(AppDimens.padding10),
    ) {
        Text(
            text = stringResource(R.string.review_grade_prompt),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleSmall,
            color = appColors().textPrimary,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall)) {
            // `entries` = mọi giá trị của enum theo thứ tự khai báo: Quên, Khó, Dễ, Rất dễ.
            ReviewGrade.entries.forEach { grade ->
                GradeButton(grade = grade, onClick = { onGrade(grade) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GradeButton(grade: ReviewGrade, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = appColors()
    val style = grade.style()
    Surface(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        shape = MaterialTheme.shapes.medium,
        color = style.background,
        contentColor = colors.textPrimary,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmallest, Alignment.CenterVertically),
        ) {
            Box(Modifier.size(width = 20.dp, height = 4.dp).background(style.accent, RoundedCornerShape(AppDimens.radius2)))
            Text(
                text = stringResource(style.label),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W800),
                maxLines = 1,
            )
            Text(
                text = stringResource(style.description),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
