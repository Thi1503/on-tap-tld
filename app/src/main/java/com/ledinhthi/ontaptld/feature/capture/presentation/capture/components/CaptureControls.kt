package com.ledinhthi.ontaptld.feature.capture.presentation.capture.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.AppTextStyle
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.capture.presentation.capture.CapturePhase

private val SideButtonSize = 64.dp
private val ShutterSize = 76.dp
private val ShutterRing = 4.dp
private val ShutterCore = 58.dp
private const val DisabledAlpha = 0.4f

/**
 * Hàng nút dưới cùng của màn chụp. Nút giữa to và hai nút phụ hai bên; ý nghĩa đổi theo chặng:
 *  - đang ngắm:   Thư viện · CHỤP · Đèn
 *  - đang chỉnh:  Chụp lại · DÙNG ẢNH NÀY (✓) · (trống)
 */
@Composable
fun CaptureControls(
    phase: CapturePhase,
    canShoot: Boolean,
    canConfirm: Boolean,
    torchOn: Boolean,
    torchAvailable: Boolean,
    onGalleryClick: () -> Unit,
    onShutterClick: () -> Unit,
    onTorchToggle: () -> Unit,
    onRetakeClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = AppDimens.paddingHuge, end = AppDimens.paddingHuge, top = AppDimens.paddingMedium, bottom = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (phase) {
            CapturePhase.Camera -> {
                SideButton(
                    icon = R.drawable.ic_image,
                    label = stringResource(R.string.capture_gallery),
                    onClick = onGalleryClick,
                )
                ShutterButton(
                    contentDescription = stringResource(R.string.capture_shutter),
                    enabled = canShoot,
                    onClick = onShutterClick,
                )
                SideButton(
                    icon = R.drawable.ic_flash,
                    label = stringResource(if (torchOn) R.string.capture_torch_on else R.string.capture_torch_off),
                    onClick = onTorchToggle,
                    enabled = torchAvailable,
                    highlighted = torchOn,
                )
            }

            CapturePhase.Adjust -> {
                SideButton(
                    icon = R.drawable.ic_camera,
                    label = stringResource(R.string.capture_retake),
                    onClick = onRetakeClick,
                )
                ShutterButton(
                    contentDescription = stringResource(R.string.capture_use_photo),
                    enabled = canConfirm,
                    onClick = onConfirmClick,
                    icon = R.drawable.ic_check,
                )
                // Ô trống đúng bằng một nút phụ, để nút giữa vẫn nằm chính giữa hàng.
                Spacer(Modifier.size(SideButtonSize))
            }
        }
    }
}

/** Nút phụ: icon ở trên, chữ nhỏ ở dưới. [highlighted] đổi sang màu nhấn (vd đèn đang bật). */
@Composable
private fun SideButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    highlighted: Boolean = false,
) {
    val colors = appColors()
    val tint = if (highlighted) colors.primaryStrong else colors.textPrimary
    Column(
        modifier = Modifier
            .size(SideButtonSize)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else DisabledAlpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmallest, Alignment.CenterVertically),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null, // chữ bên dưới đã mô tả nút
            modifier = Modifier.size(26.dp),
            tint = tint,
        )
        Text(text = label, style = AppTextStyle.font12Semi(), color = tint, maxLines = 1)
    }
}

/**
 * Nút tròn lớn ở giữa: vòng trắng bao lõi cam. Không có [icon] là nút chụp; có [icon] (dấu ✓)
 * là nút xác nhận ảnh.
 */
@Composable
private fun ShutterButton(
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    @DrawableRes icon: Int? = null,
) {
    val colors = appColors()
    Box(
        modifier = Modifier
            .size(ShutterSize)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .clip(CircleShape)
            .border(ShutterRing, colors.textPrimary, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            // Nút không có chữ, nên phải tự khai tên cho trình đọc màn hình.
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(ShutterCore).background(colors.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = colors.textOnAccent,
                )
            }
        }
    }
}
