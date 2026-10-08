package com.ledinhthi.ontaptld.feature.review.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.AppCard
import com.ledinhthi.ontaptld.core.presentation.components.AppIconButton
import com.ledinhthi.ontaptld.core.presentation.components.PrimaryButton
import com.ledinhthi.ontaptld.core.presentation.components.SecondaryButton
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.review.domain.ReviewGrade
import com.ledinhthi.ontaptld.feature.review.presentation.ReviewSchedule
import com.ledinhthi.ontaptld.feature.review.presentation.ReviewState

/** Màn tổng kết sau khi chấm hết thẻ của một lượt ôn. */
@Composable
fun ColumnScope.ReviewDoneContent(
    state: ReviewState,
    onClose: () -> Unit,
    onGoHome: () -> Unit,
    onReviewForgotten: () -> Unit,
) {
    val colors = appColors()
    val reviewedCount = state.results.size
    val forgotCount = state.forgotCount

    Row(Modifier.padding(start = AppDimens.paddingVerySmall, top = AppDimens.paddingSmall)) {
        AppIconButton(
            icon = R.drawable.ic_close,
            contentDescription = stringResource(R.string.common_close),
            onClick = onClose,
        )
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.defaultPadding)
            .padding(top = AppDimens.paddingVerySmall, bottom = AppDimens.paddingVerySmall),
        verticalArrangement = Arrangement.spacedBy(AppDimens.paddingMedium),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppDimens.paddingVerySmall, bottom = AppDimens.paddingSmallest),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(colors.statusGreenBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = colors.statusGreenText,
                )
            }
            Text(
                text = stringResource(R.string.review_done_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(
                    R.string.review_done_summary,
                    pluralStringResource(R.plurals.common_cards, reviewedCount, reviewedCount),
                    pluralStringResource(R.plurals.review_deck_count, state.reviewedDeckCount, state.reviewedDeckCount),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        RetentionCard(state)
        ScheduleCard(state.schedule)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = AppDimens.defaultPadding,
                end = AppDimens.defaultPadding,
                top = AppDimens.paddingSmall,
                bottom = AppDimens.padding24,
            ),
        verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
    ) {
        PrimaryButton(
            text = stringResource(R.string.review_go_home),
            onClick = onGoHome,
            modifier = Modifier.fillMaxWidth(),
        )
        if (forgotCount > 0) {
            SecondaryButton(
                text = pluralStringResource(R.plurals.review_again_forgotten, forgotCount, forgotCount),
                onClick = onReviewForgotten,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Thẻ "Mức độ ghi nhớ": thanh chia theo tỉ lệ 4 mức chấm + con số của từng mức. */
@Composable
private fun RetentionCard(state: ReviewState) {
    val colors = appColors()
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(AppDimens.defaultPadding)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = stringResource(R.string.review_retention_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clearAndSetSemantics { }, // các con số bên dưới đã mang đủ thông tin
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                ReviewGrade.entries.forEach { grade ->
                    val count = state.gradeCount(grade)
                    // Mức không có thẻ nào thì bỏ đoạn của nó (weight phải lớn hơn 0).
                    if (count > 0) {
                        Box(
                            Modifier
                                .weight(count.toFloat())
                                .fillMaxHeight()
                                .background(grade.style().accent, RoundedCornerShape(AppDimens.radius4)),
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall)) {
                ReviewGrade.entries.forEach { grade ->
                    val style = grade.style()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .semantics(mergeDescendants = true) { },
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = state.gradeCount(grade).toString(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                            color = colors.textPrimary,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(Modifier.size(8.dp).background(style.accent, CircleShape))
                            Text(
                                text = stringResource(style.label),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Thẻ "Lịch ôn tiếp theo": các thẻ vừa ôn sẽ quay lại khi nào. Nhóm không có thẻ thì ẩn. */
@Composable
private fun ScheduleCard(schedule: ReviewSchedule) {
    val colors = appColors()
    // Gom các dòng CÓ thẻ thành danh sách (nhãn, số thẻ) rồi mới vẽ, để biết chỗ nào cần đường kẻ.
    val rows = buildList {
        if (schedule.tomorrow > 0) add(stringResource(R.string.review_schedule_tomorrow) to schedule.tomorrow)
        if (schedule.soon > 0) {
            val label = schedule.soonDays?.let { pluralStringResource(R.plurals.review_schedule_in_days, it, it) }
                ?: stringResource(R.string.review_schedule_within_two_weeks)
            add(label to schedule.soon)
        }
        if (schedule.later > 0) add(stringResource(R.string.review_schedule_two_weeks_plus) to schedule.later)
    }
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(AppDimens.defaultPadding)) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)) {
            Text(
                text = stringResource(R.string.review_schedule_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            rows.forEachIndexed { index, (label, count) ->
                if (index > 0) HorizontalDivider(color = colors.divider)
                Row(
                    modifier = Modifier.semantics(mergeDescendants = true) { },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = pluralStringResource(R.plurals.common_cards, count, count),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                    )
                }
            }
        }
    }
}
