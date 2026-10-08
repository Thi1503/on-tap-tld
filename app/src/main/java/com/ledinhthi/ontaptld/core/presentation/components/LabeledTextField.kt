package com.ledinhthi.ontaptld.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

/**
 * Ô nhập theo thiết kế: nhãn đậm nằm TRÊN ô (không phải nhãn nổi của Material), viền xám, khi
 * focus viền cam dày 2dp. [maxLength] chặn nhập quá dài; ô nhiều dòng hiện kèm bộ đếm "n/max".
 */
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLength: Int? = null,
    showCounter: Boolean = maxLength != null && !singleLine,
    /** Chữ nhỏ bên phải nhãn; mặc định là bộ đếm "n/max". Truyền vào để tự ghi (vd "214 ký tự"). */
    trailingLabel: String? = if (showCounter && maxLength != null) "${value.length}/$maxLength" else null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = appColors()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            if (trailingLabel != null) {
                Text(
                    text = trailingLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
        OutlinedTextField(
            value = value,
            // Chặn gõ vượt [maxLength], nhưng luôn cho XOÁ BỚT: nội dung có sẵn dài quá giới hạn
            // (vd thẻ do AI tạo) vẫn phải sửa ngắn lại được.
            onValueChange = {
                if (maxLength == null || it.length <= maxLength || it.length < value.length) onValueChange(it)
            },
            modifier = fieldModifier
                .fillMaxWidth()
                // Nhãn nằm ngoài TextField nên phải gắn lại để trình đọc màn hình đọc đúng tên ô.
                .semantics { contentDescription = label },
            enabled = enabled,
            textStyle = textStyle,
            placeholder = placeholder?.let {
                { Text(it, style = textStyle) }
            },
            singleLine = singleLine,
            minLines = if (singleLine) 1 else minLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedContainerColor = colors.inputBackground,
                unfocusedContainerColor = colors.inputBackground,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.borderStrong,
                disabledContainerColor = colors.inputBackground,
                disabledBorderColor = colors.borderStrong,
                disabledPlaceholderColor = colors.textHint,
                disabledTextColor = colors.textSecondary,
                cursorColor = colors.primary,
                focusedPlaceholderColor = colors.textHint,
                unfocusedPlaceholderColor = colors.textHint,
            ),
        )
    }
}
