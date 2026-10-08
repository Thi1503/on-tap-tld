package com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.AppCard
import com.ledinhthi.ontaptld.core.presentation.components.AppTextButton
import com.ledinhthi.ontaptld.core.presentation.components.BadgeTone
import com.ledinhthi.ontaptld.core.presentation.components.StatusPill
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.deck.domain.model.DeckSummary
import com.ledinhthi.ontaptld.feature.deck.presentation.components.deckTint

/** Tiêu đề mục "Bộ thẻ của bạn" + tổng số bộ/thẻ + nút tạo bộ mới. */
@Composable
fun DecksSectionHeader(
    deckCount: Int,
    cardCount: Int,
    onNewDeckClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = appColors()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_decks_title),
                modifier = Modifier
                    .alignByBaseline()
                    .semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
            )
            Spacer(Modifier.width(AppDimens.paddingVerySmall))
            Text(
                // pluralStringResource chọn dạng số ít/nhiều theo ngôn ngữ ("1 card" / "2 cards").
                // Tham số thứ 2 để CHỌN dạng, tham số thứ 3 là số điền vào chỗ %d.
                text = stringResource(
                    R.string.home_decks_summary,
                    pluralStringResource(R.plurals.common_decks, deckCount, deckCount),
                    pluralStringResource(R.plurals.common_cards, cardCount, cardCount),
                ),
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AppTextButton(
            text = stringResource(R.string.home_new_deck),
            onClick = onNewDeckClick,
            leadingIcon = R.drawable.ic_add,
        )
    }
}

/** 1 bộ thẻ trong danh sách Home: ô màu, tên, số thẻ, và trạng thái ôn hôm nay. */
@Composable
fun DeckSummaryRow(
    summary: DeckSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = appColors()
    val tint = deckTint(summary.deck.colorHex)
    AppCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(AppDimens.paddingSmall),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(tint.tileBackground, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cards),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = tint.onTile,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = summary.deck.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(R.plurals.common_cards, summary.cardCount, summary.cardCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            // Nhãn bên phải có 3 dạng: chưa có thẻ / còn thẻ cần ôn / đã ôn xong.
            when {
                summary.cardCount == 0 -> StatusPill(
                    text = stringResource(R.string.deck_no_cards),
                    tone = BadgeTone.Neutral,
                )

                summary.dueCount > 0 -> StatusPill(
                    text = pluralStringResource(R.plurals.deck_due_count, summary.dueCount, summary.dueCount),
                    tone = BadgeTone.Primary,
                )

                else -> StatusPill(
                    text = stringResource(R.string.home_all_done),
                    tone = BadgeTone.Success,
                    icon = R.drawable.ic_check,
                )
            }
        }
    }
}
