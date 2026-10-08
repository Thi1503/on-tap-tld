package com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.PrimaryButton
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

/**
 * Khối ngay dưới top bar (cùng nền với top bar nên nhìn như một mảng liền): ba con số của bộ
 * thẻ và nút bắt đầu ôn.
 */
@Composable
fun DeckStatsHeader(
    totalCount: Int,
    dueCount: Int,
    newCount: Int,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = appColors()
    Column(modifier.fillMaxWidth().background(colors.appBarBackground)) {
        Column(
            modifier = Modifier.padding(
                start = AppDimens.defaultPadding,
                end = AppDimens.defaultPadding,
                top = AppDimens.paddingSmallest,
                bottom = AppDimens.defaultPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall)) {
                // weight(1f) cho cả ba -> ba cột rộng bằng nhau dù con số dài ngắn khác nhau.
                StatCell(totalCount, stringResource(R.string.deck_detail_stat_total), Modifier.weight(1f))
                StatCell(
                    value = dueCount,
                    label = stringResource(R.string.home_due_label),
                    modifier = Modifier.weight(1f),
                    valueColor = colors.primaryStrong,
                )
                StatCell(newCount, stringResource(R.string.deck_detail_stat_new), Modifier.weight(1f))
            }
            PrimaryButton(
                text = if (dueCount > 0) {
                    pluralStringResource(R.plurals.deck_detail_review_cards, dueCount, dueCount)
                } else {
                    stringResource(R.string.deck_detail_review_none)
                },
                onClick = onReviewClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = dueCount > 0,
                height = AppDimens.btnHeight46,
            )
        }
        HorizontalDivider(color = colors.cardBorder)
    }
}

@Composable
private fun StatCell(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = appColors().textPrimary,
) {
    Column(
        // mergeDescendants: trình đọc màn hình đọc liền "128, Tổng số thẻ" thành một mục thay vì
        // dừng ở con số rồi mới tới nhãn.
        modifier = modifier.semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.displaySmall,
            color = valueColor,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = appColors().textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
