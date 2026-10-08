package com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.BadgeTone
import com.ledinhthi.ontaptld.core.presentation.components.StatusPill
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.deck.domain.model.DeckSummary
import com.ledinhthi.ontaptld.feature.deck.presentation.components.deckTint

/** Tối đa bao nhiêu bộ thẻ được nêu tên riêng trên thanh phân bổ; phần còn lại gộp vào "Khác". */
private const val MaxNamedSegments = 3

private data class DueSegment(val label: String, val count: Int, val color: Color)

/**
 * Thẻ nổi bật đầu Home: tổng số thẻ cần ôn hôm nay + nút "Ôn ngay" + thanh cho biết số thẻ đó
 * nằm ở những bộ nào.
 *
 * @param dueDecks các bộ có thẻ cần ôn, ĐÃ xếp giảm dần theo số thẻ.
 */
@Composable
fun DueHeroCard(
    dueCount: Int,
    dueDecks: List<DeckSummary>,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = appColors()
    val shape = MaterialTheme.shapes.large
    Column(
        // THỨ TỰ Modifier có ý nghĩa — áp dụng từ trên xuống: tô nền và viền TRƯỚC, rồi mới
        // `padding`, nên phần đệm nằm BÊN TRONG nền. Đảo lại (padding trước) thì nền bị thu nhỏ.
        modifier = modifier
            .fillMaxWidth()
            .background(colors.heroBackground, shape)
            .border(1.dp, colors.heroBorder, shape)
            .padding(AppDimens.paddingMedium),
        verticalArrangement = Arrangement.spacedBy(14.dp), // các con cách nhau 14dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.home_due_label),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = colors.heroTextSecondary,
            )
            if (dueDecks.isNotEmpty()) {
                Text(
                    text = pluralStringResource(R.plurals.home_due_in_decks, dueDecks.size, dueDecks.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.heroTextSecondary,
                )
            }
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Row(Modifier.weight(1f)) {
                // alignByBaseline: số "24" (cỡ 52) và chữ "thẻ" (cỡ 16) khác cỡ nhưng đứng chung
                // một đường chân chữ, thay vì canh theo mép trên/dưới của ô chữ.
                Text(
                    text = dueCount.toString(),
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.displayLarge,
                    color = colors.heroText,
                )
                Spacer(Modifier.width(AppDimens.paddingVerySmall))
                Text(
                    text = pluralStringResource(R.plurals.common_card_unit, dueCount),
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
                    color = colors.heroTextSecondary,
                )
            }
            if (dueCount > 0) {
                ReviewNowButton(onReviewClick)
            } else {
                StatusPill(
                    text = stringResource(R.string.home_all_done),
                    modifier = Modifier.padding(bottom = AppDimens.paddingVerySmall),
                    tone = BadgeTone.Success,
                    icon = R.drawable.ic_check,
                )
            }
        }

        if (dueCount > 0) {
            val segments = dueSegments(dueDecks)
            DueBreakdownBar(segments)
            DueLegend(segments)
        }
    }
}

@Composable
private fun ReviewNowButton(onClick: () -> Unit) {
    val colors = appColors()
    Button(
        onClick = onClick,
        modifier = Modifier.height(AppDimens.btnHeight46),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.textOnAccent,
        ),
        contentPadding = PaddingValues(start = AppDimens.paddingMedium, end = AppDimens.defaultPadding),
    ) {
        Text(stringResource(R.string.home_review_now), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(6.dp))
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(AppDimens.sizeIconSmall),
        )
    }
}

/**
 * Đổi danh sách bộ thẻ thành các đoạn của thanh phân bổ. Hàm này TRẢ VỀ dữ liệu chứ không vẽ
 * gì, nhưng vẫn phải là `@Composable` vì bên trong đọc màu theo theme và chuỗi theo ngôn ngữ.
 * (Quy ước: hàm Composable trả về giá trị thì đặt tên chữ thường.)
 */
@Composable
private fun dueSegments(dueDecks: List<DeckSummary>): List<DueSegment> {
    val named = dueDecks.take(MaxNamedSegments).map { summary ->
        DueSegment(
            label = summary.deck.name,
            count = summary.dueCount,
            color = deckTint(summary.deck.colorHex).onDarkSurface,
        )
    }
    val otherCount = dueDecks.drop(MaxNamedSegments).sumOf { it.dueCount }
    return if (otherCount == 0) {
        named
    } else {
        named + DueSegment(
            label = stringResource(R.string.home_legend_other),
            count = otherCount,
            color = appColors().heroTextSecondary,
        )
    }
}

/** Thanh ngang chia theo tỉ lệ số thẻ cần ôn của từng bộ. Chú giải bên dưới đã mang đủ thông tin. */
@Composable
private fun DueBreakdownBar(segments: List<DueSegment>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmallest),
    ) {
        segments.forEach { segment ->
            Box(
                Modifier
                    // weight = số thẻ -> đoạn nào nhiều thẻ hơn thì dài hơn đúng theo tỉ lệ.
                    .weight(segment.count.toFloat())
                    .fillMaxHeight()
                    .background(segment.color, RoundedCornerShape(AppDimens.radius4)),
            )
        }
    }
}

@Composable
private fun DueLegend(segments: List<DueSegment>) {
    val colors = appColors()
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        segments.forEach { segment ->
            Row(
                // fill = false: mỗi mục ĐƯỢC PHÉP rộng tối đa bằng phần chia đều, nhưng tên ngắn
                // thì chỉ chiếm vừa đủ. Tên dài quá phần của mình sẽ bị cắt bằng dấu "…".
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(segment.color, CircleShape),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = segment.label,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.heroTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(AppDimens.paddingSmallest))
                Text(
                    text = segment.count.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.heroTextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}
