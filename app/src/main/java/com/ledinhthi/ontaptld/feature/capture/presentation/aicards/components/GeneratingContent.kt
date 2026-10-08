package com.ledinhthi.ontaptld.feature.capture.presentation.aicards.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.AppCard
import com.ledinhthi.ontaptld.core.presentation.components.SecondaryButton
import com.ledinhthi.ontaptld.core.presentation.components.SkeletonBlock
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.capture.presentation.components.CaptureStepHeader

/**
 * Chặng chờ AI ("Đang tạo thẻ"): vòng xoay, lời nhắn, ba thẻ giữ chỗ và nút Huỷ. Không có dữ
 * liệu gì để hiện, nên chỉ nhận đúng một hành động.
 */
@Composable
fun GeneratingContent(onCancel: () -> Unit) {
    val colors = appColors()
    Column(Modifier.fillMaxSize().background(colors.scaffoldBackground)) {
        CaptureStepHeader(
            title = stringResource(R.string.ai_generating_title),
            step = 3,
            // Đang ở bước 3 nhưng bước này chưa xong, nên vạch thứ ba chưa tô.
            filledSteps = 2,
            onNavigationClick = onCancel,
        )
        HorizontalDivider(color = colors.cardBorder)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AppDimens.padding24, end = AppDimens.padding24, top = 36.dp, bottom = 28.dp)
                // Trình đọc màn hình tự đọc phần này lên khi màn hiện ra.
                .semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spinner()
            Text(
                text = stringResource(R.string.ai_generating_headline),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.ai_generating_message),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = AppDimens.defaultPadding)
                // Chỉ là hình giữ chỗ, không có gì để đọc.
                .clearAndSetSemantics { },
            verticalArrangement = Arrangement.spacedBy(AppDimens.padding10),
        ) {
            // Bề rộng ba vạch của từng thẻ, đo từ bản design — lệch nhau cho giống chữ thật.
            SkeletonCard(250.dp, 180.dp, 110.dp)
            SkeletonCard(210.dp, 280.dp, 90.dp)
            SkeletonCard(270.dp, 140.dp, 120.dp)
        }

        SecondaryButton(
            text = stringResource(R.string.common_cancel),
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AppDimens.defaultPadding, end = AppDimens.defaultPadding, top = AppDimens.paddingSmall, bottom = AppDimens.padding24),
        )
    }
}

/** Vòng tròn xám có một cung cam quay đều, mỗi giây một vòng (đúng như bản design). */
@Composable
private fun Spinner() {
    val colors = appColors()
    // rememberInfiniteTransition = "đồng hồ" chạy mãi; `angle` tự đi từ 0 tới 360 rồi lặp lại.
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1000, easing = LinearEasing), RepeatMode.Restart),
        label = "spinnerAngle",
    )
    Canvas(Modifier.size(56.dp)) {
        val stroke = 5.dp.toPx()
        // Nét vẽ nằm đè lên đường viền, nên lùi vào nửa nét để không tràn ra ngoài khung 56dp.
        val topLeft = Offset(stroke / 2, stroke / 2)
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(colors.borderStrong, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
        // -90° là điểm trên cùng của vòng tròn; cung cam dài một phần tư vòng.
        drawArc(
            color = colors.primary,
            startAngle = angle - 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Butt),
        )
    }
}

/** Một thẻ giữ chỗ: ba vạch xám nhấp nháy ở vị trí của câu hỏi, câu trả lời và dòng nguồn. */
@Composable
private fun SkeletonCard(titleWidth: Dp, lineWidth: Dp, footerWidth: Dp) {
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(AppDimens.defaultPadding)) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.padding10)) {
            SkeletonBlock(Modifier.width(titleWidth).height(14.dp), RoundedCornerShape(7.dp))
            SkeletonBlock(Modifier.width(lineWidth).height(12.dp), RoundedCornerShape(AppDimens.radius6))
            SkeletonBlock(Modifier.width(footerWidth).height(12.dp), RoundedCornerShape(AppDimens.radius6))
        }
    }
}
