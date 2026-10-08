package com.ledinhthi.ontaptld.feature.capture.presentation.aicards.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.AppIconButton
import com.ledinhthi.ontaptld.core.presentation.components.LabeledTextField
import com.ledinhthi.ontaptld.core.presentation.components.PrimaryButton
import com.ledinhthi.ontaptld.core.presentation.components.SecondaryButton
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.deck.presentation.manualcard.MaxCardTextLength

/**
 * Bảng trượt từ dưới lên để sửa một thẻ đề xuất, hoặc gõ thêm thẻ mới (khi hai ô ban đầu rỗng).
 * Thẻ ở màn duyệt chưa được lưu vào database nên không dùng lại màn "Sửa thẻ" được — màn đó
 * sửa thẻ ĐÃ lưu theo id.
 *
 * @param isNew true = đang thêm thẻ (đổi tiêu đề, và đặt con trỏ sẵn vào ô Câu hỏi).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditorSheet(
    isNew: Boolean,
    initialQuestion: String,
    initialAnswer: String,
    onDismiss: () -> Unit,
    onConfirm: (question: String, answer: String) -> Unit,
) {
    val colors = appColors()
    // Nội dung đang gõ là state thuần UI cho tới khi bấm "Lưu" — không cần đi qua ViewModel.
    var question by rememberSaveable { mutableStateOf(initialQuestion) }
    var answer by rememberSaveable { mutableStateOf(initialAnswer) }
    val canSubmit = question.isNotBlank() && answer.isNotBlank()

    val questionFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (isNew) questionFocus.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.bottomSheetBackground,
        contentColor = colors.textPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Máy thấp + bàn phím đang mở thì bảng không đủ chỗ: cho cuộn để vẫn tới được nút Lưu.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.defaultPadding)
                .padding(bottom = AppDimens.padding24),
            verticalArrangement = Arrangement.spacedBy(AppDimens.defaultPadding),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        if (isNew) R.string.manual_card_title_add else R.string.manual_card_title_edit,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                )
                AppIconButton(
                    icon = R.drawable.ic_close,
                    contentDescription = stringResource(R.string.common_close),
                    onClick = onDismiss,
                    iconSize = 22.dp,
                )
            }
            LabeledTextField(
                label = stringResource(R.string.manual_card_question_label),
                value = question,
                onValueChange = { question = it },
                fieldModifier = Modifier
                    .focusRequester(questionFocus)
                    .heightIn(min = 88.dp),
                maxLength = MaxCardTextLength,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            LabeledTextField(
                label = stringResource(R.string.manual_card_answer_label),
                value = answer,
                onValueChange = { answer = it },
                fieldModifier = Modifier.heightIn(min = 112.dp),
                maxLength = MaxCardTextLength,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Row(
                modifier = Modifier.padding(top = AppDimens.paddingSmallest),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
            ) {
                SecondaryButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = stringResource(R.string.common_save),
                    onClick = { onConfirm(question, answer) },
                    modifier = Modifier.weight(2f),
                    enabled = canSubmit,
                )
            }
        }
    }
}
