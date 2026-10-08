package com.ledinhthi.ontaptld.feature.capture.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.AppTopBar
import com.ledinhthi.ontaptld.core.presentation.components.TopBarNavigation
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

/** Luồng "chụp ghi chú → kiểm tra văn bản → duyệt thẻ AI" có bấy nhiêu bước. */
const val CaptureFlowSteps = 3

/**
 * Phần đầu chung của cả ba màn trong luồng tạo thẻ bằng AI: top bar + chữ "Bước x/3" + ba vạch
 * tiến độ. Bước 1 dùng nút ✕ (thoát cả luồng), các bước sau dùng ← (lùi một bước).
 */
@Composable
fun CaptureStepHeader(
    title: String,
    step: Int,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier,
    navigation: TopBarNavigation = TopBarNavigation.Back,
    containerColor: Color = appColors().appBarBackground,
    /** Số vạch được tô. Mặc định bằng [step]; nhỏ hơn khi bước đang đứng còn dang dở. */
    filledSteps: Int = step,
    /** Nếu có, hiện thay cho chữ "Bước x/3" ở góc phải (vd nút báo cáo ở màn duyệt thẻ). */
    action: (@Composable () -> Unit)? = null,
) {
    val colors = appColors()
    Column(modifier.fillMaxWidth().background(containerColor)) {
        AppTopBar(
            title = title,
            navigation = navigation,
            onNavigationClick = onNavigationClick,
            showDivider = false,
            containerColor = containerColor,
        ) {
            if (action != null) {
                action()
            } else {
                Text(
                    text = stringResource(R.string.capture_step, step, CaptureFlowSteps),
                    modifier = Modifier.padding(end = AppDimens.paddingVerySmall),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.defaultPadding)
                .padding(bottom = AppDimens.paddingSmall)
                // Dòng "Bước 1/3" đã nói đủ, nên ba vạch này ẩn khỏi trình đọc màn hình.
                .clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmallest),
        ) {
            // `repeat(3) { index -> … }` chạy khối lệnh 3 lần với index = 0, 1, 2.
            repeat(CaptureFlowSteps) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(AppDimens.paddingSmallest)
                        .background(
                            color = if (index < filledSteps) colors.primary else colors.neutralSoft,
                            shape = RoundedCornerShape(AppDimens.radius2),
                        ),
                )
            }
        }
    }
}
