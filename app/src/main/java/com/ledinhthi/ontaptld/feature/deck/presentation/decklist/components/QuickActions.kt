package com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

/** Hai lối tạo thẻ đặt cạnh nhau: tự động (chụp ghi chú → AI) và thủ công (gõ tay). */
@Composable
fun QuickActions(
    onCaptureClick: () -> Unit,
    onManualCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = appColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min), // hai ô luôn cao bằng nhau dù phụ đề xuống dòng khác nhau
        horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
    ) {
        ActionTile(
            icon = R.drawable.ic_camera,
            title = stringResource(R.string.home_action_capture_title),
            subtitle = stringResource(R.string.home_action_capture_subtitle),
            onClick = onCaptureClick,
            containerColor = colors.primarySoft,
            borderColor = colors.primarySoft,
            iconBackground = colors.primary,
            iconTint = colors.textOnAccent,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        ActionTile(
            icon = R.drawable.ic_edit,
            title = stringResource(R.string.home_action_manual_title),
            subtitle = stringResource(R.string.home_action_manual_subtitle),
            onClick = onManualCardClick,
            containerColor = colors.cardBackground,
            borderColor = colors.cardBorder,
            iconBackground = colors.neutralSoft,
            iconTint = colors.textStrong,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun ActionTile(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    containerColor: Color,
    borderColor: Color,
    iconBackground: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    val colors = appColors()
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(AppDimens.padding10),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBackground, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.sizeIcon),
                    tint = iconTint,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
    }
}
