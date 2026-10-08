package com.ledinhthi.ontaptld.feature.deck.presentation.manualcard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode
import com.ledinhthi.ontaptld.core.presentation.components.AppBadge
import com.ledinhthi.ontaptld.core.presentation.components.AppTopBar
import com.ledinhthi.ontaptld.core.presentation.components.BadgeTone
import com.ledinhthi.ontaptld.core.presentation.components.ConfirmDialog
import com.ledinhthi.ontaptld.core.presentation.components.LabeledTextField
import com.ledinhthi.ontaptld.core.presentation.components.LoadingOverlay
import com.ledinhthi.ontaptld.core.presentation.components.ObserveEffects
import com.ledinhthi.ontaptld.core.presentation.components.PrimaryButton
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.OnTapTldTheme
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.deck.presentation.manualcard.components.DeckSelector

/**
 * Màn Thêm / sửa thẻ (route `ManualCardRoute`). Cùng khuôn Screen → Content như DeckListScreen.
 * Một bố cục cho hai chế độ; những phần chỉ dành cho THÊM (ô tick, dòng nhắc) ẩn đi khi SỬA.
 */
@Composable
fun ManualCardScreen(viewModel: ManualCardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // FocusRequester = "cái điều khiển" để ra lệnh đặt con trỏ vào một ô nhập từ bên ngoài ô đó.
    val questionFocus = remember { FocusRequester() }
    // Thêm thẻ: mở màn là gõ được ngay. Sửa thẻ: để người dùng đọc lại trước, không bật bàn phím.
    LaunchedEffect(Unit) { if (!state.isEditing) questionFocus.requestFocus() }
    ObserveEffects(viewModel.effect) { effect ->
        when (effect) {
            ManualCardEffect.FocusQuestion -> questionFocus.requestFocus()
        }
    }

    // Rời màn khi còn nội dung chưa lưu thì hỏi lại trước. Có HAI lối rời màn và cả hai phải đi
    // qua cùng một chỗ: nút ← trên top bar, và nút / cử chỉ Back của hệ thống.
    var askDiscard by rememberSaveable { mutableStateOf(false) }
    val onCloseRequest: () -> Unit = {
        if (state.hasUnsavedChanges) askDiscard = true else viewModel.onClose()
    }
    // BackHandler "chặn" Back của hệ thống khi `enabled` = true; lúc không có gì để mất thì để
    // hệ thống tự quay lại như bình thường.
    BackHandler(enabled = state.hasUnsavedChanges) { askDiscard = true }

    LoadingOverlay(isLoading = state.status.isLoadingOverlay) {
        ManualCardContent(
            state = state,
            questionFocus = questionFocus,
            onClose = onCloseRequest,
            onDeckSelected = viewModel::onDeckSelected,
            onQuestionChange = viewModel::onQuestionChange,
            onAnswerChange = viewModel::onAnswerChange,
            onKeepAddingChange = viewModel::onKeepAddingChange,
            onSave = viewModel::onSave,
        )
    }

    if (askDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.manual_card_discard_title),
            message = stringResource(R.string.manual_card_discard_message),
            // Nút còn lại dùng chữ "Huỷ" mặc định của ConfirmDialog = huỷ việc thoát, ở lại màn.
            confirmText = stringResource(R.string.manual_card_discard_confirm),
            destructive = true,
            onConfirm = {
                askDiscard = false
                viewModel.onClose()
            },
            onDismiss = { askDiscard = false },
        )
    }
}

@Composable
private fun ManualCardContent(
    state: ManualCardState,
    questionFocus: FocusRequester,
    onClose: () -> Unit,
    onDeckSelected: (String) -> Unit,
    onQuestionChange: (String) -> Unit,
    onAnswerChange: (String) -> Unit,
    onKeepAddingChange: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    val colors = appColors()
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.scaffoldBackground)
            // Bàn phím hiện lên thì cả màn co lại phía trên nó: nút Lưu luôn nằm ngay trên bàn
            // phím và phần giữa tự cuộn được, không bị che.
            .imePadding(),
    ) {
        AppTopBar(
            title = stringResource(
                if (state.isEditing) R.string.manual_card_title_edit else R.string.manual_card_title_add,
            ),
            // Thiết kế vẽ nút ✕; Thi chốt dùng mũi tên ← cho đồng bộ với các màn con khác.
            onNavigationClick = onClose,
        ) {
            if (state.source == FlashcardSource.AI) {
                AppBadge(
                    text = stringResource(R.string.deck_source_ai),
                    modifier = Modifier.padding(end = AppDimens.paddingVerySmall),
                    tone = BadgeTone.Purple,
                    icon = R.drawable.ic_sparkle,
                )
            } else {
                AppBadge(
                    text = stringResource(R.string.deck_source_manual),
                    modifier = Modifier.padding(end = AppDimens.paddingVerySmall),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(AppDimens.defaultPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.defaultPadding),
        ) {
            DeckSelector(
                decks = state.decks,
                selected = state.selectedDeck,
                onSelect = onDeckSelected,
                // Sửa thẻ chỉ đổi nội dung; thẻ vẫn ở bộ cũ.
                enabled = !state.isEditing,
            )
            // heightIn(min = …): ô cao tối thiểu như thiết kế, gõ dài hơn thì ô tự cao thêm.
            LabeledTextField(
                label = stringResource(R.string.manual_card_question_label),
                value = state.question,
                onValueChange = onQuestionChange,
                fieldModifier = Modifier
                    .focusRequester(questionFocus)
                    .heightIn(min = 112.dp),
                maxLength = MaxCardTextLength,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            LabeledTextField(
                label = stringResource(R.string.manual_card_answer_label),
                value = state.answer,
                onValueChange = onAnswerChange,
                fieldModifier = Modifier.heightIn(min = 156.dp),
                maxLength = MaxCardTextLength,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            if (!state.isEditing) {
                KeepAddingToggle(checked = state.keepAdding, onCheckedChange = onKeepAddingChange)
                InfoNote(savedCount = state.savedCount)
            }
        }

        Column(Modifier.fillMaxWidth().background(colors.cardBackground)) {
            HorizontalDivider(color = colors.cardBorder)
            PrimaryButton(
                text = stringResource(
                    if (state.isEditing) R.string.manual_card_save_changes else R.string.manual_card_save,
                ),
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppDimens.defaultPadding,
                        end = AppDimens.defaultPadding,
                        top = AppDimens.paddingSmall,
                        bottom = AppDimens.paddingMedium,
                    ),
                enabled = state.canSave,
            )
        }
    }
}

/** Ô tick "Lưu xong thêm thẻ tiếp" — chạm vào đâu trên cả dòng cũng đổi được. */
@Composable
private fun KeepAddingToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = appColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            // `toggleable` đặt ở cả dòng (và Checkbox bên trong để onCheckedChange = null): vùng
            // chạm rộng hơn, và trình đọc màn hình đọc cả dòng như MỘT ô tick có nhãn.
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = colors.primary,
                uncheckedColor = colors.borderStrong,
                checkmarkColor = colors.textOnAccent,
            ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.manual_card_keep_adding_title),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.manual_card_keep_adding_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

/**
 * Dòng nhắc trên nền cam nhạt. Lúc đầu nhắc "thẻ mới ôn được ngay"; sau mỗi lần lưu-và-ở-lại thì
 * đổi thành "Đã lưu N thẻ" để người dùng biết thẻ vừa gõ đã được ghi nhận.
 */
@Composable
private fun InfoNote(savedCount: Int) {
    val colors = appColors()
    val saved = savedCount > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primarySoft, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = AppDimens.paddingSmall)
            // Khi nội dung đổi sang "Đã lưu…", trình đọc màn hình tự đọc lên.
            .semantics { if (saved) liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.padding10),
    ) {
        Icon(
            painter = painterResource(if (saved) R.drawable.ic_check else R.drawable.ic_info),
            contentDescription = null,
            modifier = Modifier.size(AppDimens.sizeIcon),
            tint = colors.primaryStrong,
        )
        Text(
            text = if (saved) {
                pluralStringResource(R.plurals.manual_card_saved_count, savedCount, savedCount)
            } else {
                stringResource(R.string.manual_card_new_card_note)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textStrong,
        )
    }
}

// ---------------------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------------------

private val previewDecks = listOf(
    Deck("1", "IELTS Vocabulary", "#4F46E5", createdAt = 0, updatedAt = 0),
    Deck("2", "Sinh học 12 – Di truyền", "#0D9488", createdAt = 0, updatedAt = 0),
)

private val previewState = ManualCardState(
    decks = previewDecks,
    selectedDeckId = "1",
    question = "Phân biệt affect và effect?",
    answer = "Affect là động từ (gây ảnh hưởng); effect là danh từ (kết quả, tác động).",
)

@Composable
private fun PreviewHost(state: ManualCardState, themeMode: ThemeMode = ThemeMode.LIGHT) {
    OnTapTldTheme(themeMode = themeMode) {
        ManualCardContent(
            state = state,
            questionFocus = remember { FocusRequester() },
            onClose = {},
            onDeckSelected = {},
            onQuestionChange = {},
            onAnswerChange = {},
            onKeepAddingChange = {},
            onSave = {},
        )
    }
}

@Preview(name = "Thêm thẻ", widthDp = 390, heightDp = 844)
@Composable
private fun ManualCardAddPreview() = PreviewHost(previewState)

@Preview(name = "Thêm thẻ — Dark", widthDp = 390, heightDp = 844)
@Composable
private fun ManualCardAddDarkPreview() = PreviewHost(previewState, ThemeMode.DARK)

@Preview(name = "Thêm thẻ — vừa lưu 2 thẻ", widthDp = 390, heightDp = 844)
@Composable
private fun ManualCardSavedPreview() =
    PreviewHost(previewState.copy(question = "", answer = "", savedCount = 2))

@Preview(name = "Sửa thẻ AI", widthDp = 390, heightDp = 844)
@Composable
private fun ManualCardEditPreview() =
    PreviewHost(previewState.copy(isEditing = true, source = FlashcardSource.AI))
