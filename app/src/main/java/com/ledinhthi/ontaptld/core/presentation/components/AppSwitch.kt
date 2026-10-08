package com.ledinhthi.ontaptld.core.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

// Số đo lấy từ bản design (Settings.dc.html): rãnh 48×28, núm tròn 22, hở mép 3.
private val TrackWidth = 48.dp
private val TrackHeight = 28.dp
private val ThumbSize = 22.dp
private val ThumbInset = 3.dp

/**
 * Công tắc bật / tắt theo đúng kích thước của bản thiết kế (Switch của Material to hơn và có
 * viền). Component này CHỈ VẼ: nó không tự bắt thao tác chạm. Nơi dùng đặt `Modifier.toggleable`
 * lên cả dòng chứa nó, để chạm vào chữ cũng bật / tắt được — xem `SettingsScreen`.
 */
@Composable
fun AppSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = appColors()
    // animate…AsState: khi giá trị đích đổi, giá trị trả về chạy dần tới đó qua từng khung hình
    // thay vì nhảy ngay — nhờ vậy núm trượt và rãnh đổi màu mượt.
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.primary else colors.borderStrong,
        label = "switchTrack",
    )
    val thumbOffset by animateDpAsState(
        // Quãng đường núm đi được = bề rộng rãnh trừ hai mép hở và chính cái núm.
        targetValue = if (checked) TrackWidth - ThumbInset * 2 - ThumbSize else 0.dp,
        label = "switchThumb",
    )
    Box(
        modifier
            .size(TrackWidth, TrackHeight)
            .background(trackColor, CircleShape)
            .padding(ThumbInset),
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(ThumbSize)
                .background(colors.textOnAccent, CircleShape),
        )
    }
}
