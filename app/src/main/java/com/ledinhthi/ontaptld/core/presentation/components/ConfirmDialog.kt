package com.ledinhthi.ontaptld.core.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

/**
 * Hộp thoại hỏi lại trước một hành động khó hoàn tác (xoá thẻ, xoá bộ thẻ…).
 * Bấm ra ngoài hoặc nút Back = huỷ. Với [destructive] = true, nút xác nhận có màu đỏ.
 *
 * Nơi gọi tự quyết định lúc nào hiện: `if (dangHoi) ConfirmDialog(…)`.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    dismissText: String = stringResource(R.string.common_cancel),
) {
    val colors = appColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            AppTextButton(
                text = confirmText,
                onClick = onConfirm,
                contentColor = if (destructive) colors.statusRedText else colors.primaryStrong,
            )
        },
        dismissButton = {
            AppTextButton(text = dismissText, onClick = onDismiss, contentColor = colors.textStrong)
        },
        shape = MaterialTheme.shapes.large,
        containerColor = colors.bottomSheetBackground,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
    )
}
