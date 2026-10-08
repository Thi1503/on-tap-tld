package com.ledinhthi.ontaptld.feature.capture.presentation.aicards.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.exception.AiErrorKind
import com.ledinhthi.ontaptld.core.presentation.components.AppTopBar
import com.ledinhthi.ontaptld.core.presentation.components.BadgeTone
import com.ledinhthi.ontaptld.core.presentation.components.PrimaryButton
import com.ledinhthi.ontaptld.core.presentation.components.SecondaryButton
import com.ledinhthi.ontaptld.core.presentation.components.colors
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

/*
 * Hai màn báo "AI chưa tạo được thẻ" của bước 3/3: gọi AI lỗi (AiErrorContent) và hết lượt
 * trong ngày (AiQuotaContent). Cả hai cùng một khuôn — hình tròn có icon, tiêu đề, mô tả, một
 * khối thông tin, các nút — nên dùng chung AiNoticeLayout ở cuối file.
 */

/**
 * Chặng `Failed`: lần gọi AI không ra thẻ. [kind] quyết định icon và dòng mô tả; hai nút thì
 * lỗi nào cũng có: thử lại, hoặc bỏ AI và tự gõ thẻ.
 */
@Composable
fun AiErrorContent(
    kind: AiErrorKind,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onTypeManually: () -> Unit,
) {
    val colors = appColors()
    AiNoticeLayout(
        // Mất mạng có icon riêng (đám mây gạch chéo); các lỗi khác dùng dấu cảnh báo chung.
        icon = if (kind == AiErrorKind.NETWORK) R.drawable.ic_cloud_off else R.drawable.ic_warning,
        tone = BadgeTone.Danger,
        title = stringResource(R.string.ai_error_title),
        message = stringResource(kind.messageRes()),
        onBack = onBack,
        details = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.cardBackground, MaterialTheme.shapes.medium)
                    .border(1.dp, colors.cardBorder, MaterialTheme.shapes.medium)
                    .padding(horizontal = 14.dp, vertical = AppDimens.paddingSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.padding10),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.sizeIcon),
                    tint = colors.textSecondary,
                )
                Text(
                    text = stringResource(R.string.ai_error_text_kept),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textStrong,
                )
            }
        },
        actions = {
            PrimaryButton(
                text = stringResource(R.string.common_retry),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = stringResource(R.string.ai_type_manually),
                onClick = onTypeManually,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

/**
 * Chặng `QuotaExceeded`: hôm nay đã dùng hết [quotaMax] lượt AI. Không có nút thử lại (thử cũng
 * vẫn hết lượt) — chỉ còn lối tự gõ thẻ.
 */
@Composable
fun AiQuotaContent(
    quotaMax: Int,
    onBack: () -> Unit,
    onTypeManually: () -> Unit,
) {
    val colors = appColors()
    AiNoticeLayout(
        icon = R.drawable.ic_clock,
        tone = BadgeTone.Primary,
        title = stringResource(R.string.ai_quota_title),
        message = stringResource(R.string.ai_quota_message, quotaMax, quotaMax),
        onBack = onBack,
        details = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall)) {
                // mergeDescendants: trình đọc màn hình đọc nhãn và con số liền thành một câu.
                Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }) {
                    Text(
                        text = stringResource(R.string.ai_quota_usage_label),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = stringResource(R.string.ai_quota_usage_value, quotaMax, quotaMax),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textPrimary,
                    )
                }
                // Thanh lượt dùng. Ở màn này nó luôn đầy, nên chỉ cần một khối màu cam.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(AppDimens.paddingVerySmall)
                        .background(colors.primary, RoundedCornerShape(AppDimens.radius4)),
                )
            }
        },
        actions = {
            PrimaryButton(
                text = stringResource(R.string.ai_type_manually),
                onClick = onTypeManually,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

/** Dòng mô tả cho từng loại lỗi. `when` phải kể đủ mọi loại, thêm loại mới mà quên là báo lỗi biên dịch. */
@StringRes
private fun AiErrorKind.messageRes(): Int = when (this) {
    AiErrorKind.NETWORK -> R.string.ai_error_message_network
    AiErrorKind.QUOTA_EXCEEDED_SERVER -> R.string.ai_error_message_busy
    AiErrorKind.EMPTY_RESPONSE -> R.string.ai_error_message_no_cards
    AiErrorKind.CONTENT_BLOCKED -> R.string.ai_error_message_blocked
    AiErrorKind.APP_CHECK_FAILED -> R.string.ai_error_message_app_check
    AiErrorKind.NOT_CONFIGURED -> R.string.ai_error_message_not_configured
    AiErrorKind.RESPONSE_PARSE_ERROR,
    AiErrorKind.MODEL_UNAVAILABLE,
    AiErrorKind.UNKNOWN,
    // Hết lượt trong ngày có màn riêng (AiQuotaContent), không đi qua đây; để cho đủ nhánh.
    AiErrorKind.QUOTA_EXCEEDED_LOCAL,
    -> R.string.ai_error_message_generic
}

/**
 * Khuôn chung: top bar "Tạo thẻ bằng AI" + một cột canh giữa màn. [details] và [actions] là hai
 * "chỗ trống" để mỗi màn tự điền khối thông tin và các nút của mình.
 */
@Composable
private fun AiNoticeLayout(
    @DrawableRes icon: Int,
    tone: BadgeTone,
    title: String,
    message: String,
    onBack: () -> Unit,
    details: @Composable ColumnScope.() -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
) {
    val colors = appColors()
    val (emblemBackground, emblemContent) = tone.colors()
    Column(Modifier.fillMaxSize().background(colors.scaffoldBackground)) {
        AppTopBar(title = stringResource(R.string.ai_cards_title), onNavigationClick = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                // Màn thấp (máy nhỏ, xoay ngang) thì cuộn được; đủ chỗ thì nội dung nằm giữa.
                .verticalScroll(rememberScrollState())
                .padding(AppDimens.padding24),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.padding24, Alignment.CenterVertically),
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
            Column(
                // Trình đọc màn hình tự đọc lời báo này ngay khi màn hiện ra.
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            details()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
                content = actions,
            )
        }
    }
}
