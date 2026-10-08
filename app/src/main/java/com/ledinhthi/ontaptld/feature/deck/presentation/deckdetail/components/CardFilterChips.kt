package com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail.CardFilter

/** Hàng chip lọc thẻ theo nguồn: Tất cả / Thủ công / AI, mỗi chip kèm số thẻ của nhóm đó. */
@Composable
fun CardFilterChips(
    selected: CardFilter,
    totalCount: Int,
    manualCount: Int,
    aiCount: Int,
    onSelect: (CardFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Cỡ chữ hệ thống to hoặc màn hẹp thì ba chip có thể dài hơn màn hình -> cho cuộn ngang.
            // `horizontalScroll` đặt TRƯỚC `padding` để chip cuộn ra tới sát mép màn hình.
            .horizontalScroll(rememberScrollState())
            // Thiết kế: chip cao 36dp, cách trên 12dp và cách thẻ đầu tiên 12dp. Material tự nới
            // vùng chạm của mọi thứ bấm được lên 48dp (thêm 6dp trên + 6dp dưới), nên phần đệm ở
            // đây và ở đầu danh sách thẻ đã bớt đi đúng 6dp mỗi phía để hình vẫn khớp thiết kế.
            .padding(horizontal = AppDimens.defaultPadding)
            .padding(top = AppDimens.padding6)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall),
    ) {
        FilterChip(
            text = stringResource(R.string.deck_detail_filter_all, totalCount),
            selected = selected == CardFilter.ALL,
            onClick = { onSelect(CardFilter.ALL) },
        )
        FilterChip(
            text = stringResource(R.string.deck_detail_filter_manual, manualCount),
            selected = selected == CardFilter.MANUAL,
            onClick = { onSelect(CardFilter.MANUAL) },
        )
        FilterChip(
            text = stringResource(R.string.deck_detail_filter_ai, aiCount),
            selected = selected == CardFilter.AI,
            onClick = { onSelect(CardFilter.AI) },
        )
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = appColors()
    // Bản Surface có `selected`: bấm được, và báo cho trình đọc màn hình chip nào đang được chọn.
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(AppDimens.radius30),
        color = if (selected) colors.chipSelectedBackground else colors.cardBackground,
        contentColor = if (selected) colors.chipSelectedText else colors.textStrong,
        // Chip đang chọn có nền đặc nên không cần viền.
        border = if (selected) null else BorderStroke(1.dp, colors.borderStrong),
    ) {
        // Chiều cao 36dp đặt ở NỘI DUNG chứ không đặt lên Surface: hình chip cao 36dp, còn vùng
        // chạm 48dp bao quanh thì vẫn được Material giữ nguyên.
        Box(Modifier.height(36.dp).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            // Chữ của chip đang chọn đậm hơn nên rộng hơn một chút. Để chip không co giãn (và đẩy
            // các chip bên cạnh xê dịch) mỗi lần đổi lựa chọn, luôn đặt sẵn một bản chữ đậm trong
            // suốt để GIỮ CHỖ, rồi vẽ chữ thật chồng lên trên.
            Text(
                text = text,
                modifier = Modifier
                    .alpha(0f)
                    .clearAndSetSemantics { },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Text(
                text = text,
                style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}
