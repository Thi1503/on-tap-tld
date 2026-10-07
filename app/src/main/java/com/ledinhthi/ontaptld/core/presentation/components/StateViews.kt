package com.ledinhthi.ontaptld.core.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

/**
 * Khối thông điệp giữa màn: emblem tròn + tiêu đề + mô tả + (tuỳ chọn) các nút.
 * Là nền chung cho trạng thái rỗng, lỗi, hết lượt… — xem [EmptyState], [ErrorState].
 */
@Composable
fun StateMessage(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    @DrawableRes icon: Int = R.drawable.ic_cards,
    tone: BadgeTone = BadgeTone.Primary,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val colors = appColors()
    val (emblemBackground, emblemContent) = tone.colors()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppDimens.padding24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(emblemBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = emblemContent,
            )
        }
        Spacer(Modifier.height(AppDimens.padding24))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Spacer(Modifier.height(AppDimens.paddingVerySmall))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(AppDimens.padding24))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
            content = actions,
        )
    }
}

/** Trạng thái rỗng của một màn danh sách. */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    @DrawableRes icon: Int = R.drawable.ic_cards,
    actions: @Composable ColumnScope.() -> Unit = {},
) = StateMessage(
    title = title,
    modifier = modifier,
    message = message,
    icon = icon,
    tone = BadgeTone.Primary,
    actions = actions,
)

/** Trạng thái lỗi — được trình đọc màn hình đọc ngay khi xuất hiện. */
@Composable
fun ErrorState(
    title: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    retryText: String = stringResource(R.string.common_retry),
) = StateMessage(
    title = title,
    modifier = modifier.semantics { liveRegion = LiveRegionMode.Assertive },
    message = message,
    icon = R.drawable.ic_warning,
    tone = BadgeTone.Danger,
    actions = {
        PrimaryButton(
            text = retryText,
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = R.drawable.ic_refresh,
        )
    },
)

/** Vòng xoay giữa màn — dùng khi chưa có skeleton riêng cho màn đó. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = appColors().primary, trackColor = appColors().neutralSoft)
    }
}

/**
 * Một khối giữ chỗ khi đang tải (nhấp nháy nhẹ). Ghép nhiều khối theo đúng bố cục thật của
 * màn để người dùng thấy trước hình dạng nội dung. Bị ẩn khỏi trình đọc màn hình.
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppDimens.radius8),
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Box(
        modifier
            .alpha(alpha)
            .background(appColors().neutralSoft, shape)
            .clearAndSetSemantics { },
    )
}
