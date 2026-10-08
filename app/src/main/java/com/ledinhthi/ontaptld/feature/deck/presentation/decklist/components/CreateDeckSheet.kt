package com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.AppIconButton
import com.ledinhthi.ontaptld.core.presentation.components.LabeledTextField
import com.ledinhthi.ontaptld.core.presentation.components.PrimaryButton
import com.ledinhthi.ontaptld.core.presentation.components.SecondaryButton
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.deck.presentation.components.DeckColorOption
import com.ledinhthi.ontaptld.feature.deck.presentation.components.parseDeckColor

private const val MaxDeckNameLength = 60

/** Bảng trượt từ dưới lên để tạo bộ thẻ: tên + màu nhận diện. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDeckSheet(
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String) -> Unit,
) {
    val colors = appColors()
    // Tên và màu đang chọn là state thuần UI cho tới khi bấm "Tạo" — không cần đi qua ViewModel.
    var name by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(DeckColorOption.PURPLE) }
    val canSubmit = name.isNotBlank()

    // Tự đặt con trỏ vào ô tên (và bật bàn phím) ngay khi bảng mở. `LaunchedEffect(Unit)` chạy
    // khối lệnh đúng MỘT LẦN lúc thành phần này xuất hiện, không chạy lại mỗi lần vẽ lại.
    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { nameFocus.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss, // vuốt xuống / bấm ra ngoài / nút Back
        // Mở thẳng hết cỡ, bỏ nấc "mở lưng chừng" mặc định của bottom sheet.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.bottomSheetBackground,
        contentColor = colors.textPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.defaultPadding)
                .padding(bottom = AppDimens.padding24),
            verticalArrangement = Arrangement.spacedBy(AppDimens.defaultPadding),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.create_deck_title),
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
                label = stringResource(R.string.create_deck_name_label),
                value = name,
                onValueChange = { name = it },
                fieldModifier = Modifier.focusRequester(nameFocus),
                placeholder = stringResource(R.string.create_deck_name_placeholder),
                singleLine = true,
                maxLength = MaxDeckNameLength,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (canSubmit) onConfirm(name, selected.hex) },
                ),
            )

            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall)) {
                Text(
                    text = stringResource(R.string.create_deck_color_label),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DeckColorOption.entries.forEach { option ->
                        ColorSwatch(
                            color = parseDeckColor(option.hex),
                            label = stringResource(option.labelRes),
                            selected = option == selected,
                            onClick = { selected = option },
                        )
                    }
                }
            }

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
                    text = stringResource(R.string.create_deck_confirm),
                    onClick = { onConfirm(name, selected.hex) },
                    modifier = Modifier.weight(2f),
                    enabled = canSubmit,
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            // `selectable` thay cho `clickable`: ngoài việc bấm được, nó báo cho trình đọc màn
            // hình rằng đây là một lựa chọn trong nhóm chỉ-chọn-một (như radio) và đang chọn hay chưa.
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color.White,
            )
        }
    }
}
