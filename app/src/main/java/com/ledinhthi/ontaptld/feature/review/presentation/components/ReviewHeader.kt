package com.ledinhthi.ontaptld.feature.review.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.AppIconButton
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.presentation.components.parseDeckColor

/** Hàng trên cùng của phiên ôn: nút thoát, thanh tiến độ và "thẻ thứ mấy / tổng số". */
@Composable
fun ReviewProgressHeader(
    position: Int,
    total: Int,
    progress: Float,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = appColors()
    val barShape = RoundedCornerShape(AppDimens.radius4)
    // Thanh không nhảy cóc sang giá trị mới mà chạy mượt tới đó mỗi khi `progress` đổi.
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "reviewProgress")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = AppDimens.paddingVerySmall,
                end = AppDimens.defaultPadding,
                top = AppDimens.paddingSmall,
                bottom = AppDimens.paddingVerySmall,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(
            icon = R.drawable.ic_close,
            contentDescription = stringResource(R.string.review_exit),
            onClick = onClose,
        )
        Spacer(Modifier.width(AppDimens.paddingVerySmall))
        Box(
            Modifier
                .weight(1f)
                .height(8.dp)
                .background(colors.neutralSoft, barShape)
                // Con số "5/24" bên cạnh đã nói đủ, nên thanh này ẩn khỏi trình đọc màn hình.
                .clearAndSetSemantics { },
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(colors.primary, barShape),
            )
        }
        Spacer(Modifier.width(AppDimens.paddingSmall))
        Text(
            text = stringResource(R.string.review_progress, position, total),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
    }
}

/** Dòng cho biết thẻ đang ôn thuộc bộ nào: chấm màu + tên bộ thẻ. */
@Composable
fun ReviewDeckLabel(deck: Deck, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.defaultPadding)
            .padding(top = AppDimens.paddingSmallest),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall),
    ) {
        Box(Modifier.size(10.dp).background(parseDeckColor(deck.colorHex), CircleShape))
        Text(
            text = deck.name,
            style = MaterialTheme.typography.labelMedium,
            color = appColors().textStrong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
