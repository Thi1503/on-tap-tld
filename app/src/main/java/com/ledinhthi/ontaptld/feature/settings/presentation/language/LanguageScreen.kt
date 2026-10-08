package com.ledinhthi.ontaptld.feature.settings.presentation.language

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode
import com.ledinhthi.ontaptld.core.presentation.components.AppTopBar
import com.ledinhthi.ontaptld.core.presentation.language.AppLanguage
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.OnTapTldTheme
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

// Số đo lấy từ bản design (Language.dc.html).
private val OptionHeight = 64.dp
private val OptionPadding = 14.dp

/**
 * Màn Ngôn ngữ (route `LanguageRoute`), mở từ màn Cài đặt. Chọn là áp dụng ngay, không có nút
 * Lưu: Android dựng lại màn hình bằng ngôn ngữ mới và người dùng vẫn đứng ở màn này.
 */
@Composable
fun LanguageScreen(viewModel: LanguageViewModel = hiltViewModel()) {
    // Hỏi "app đang nói tiếng gì?" bằng chính bộ chuỗi đang được dùng: `app_language_tag` là
    // "vi" trong values/ và "en" trong values-en/. Cách này đúng cả khi người dùng chưa chọn gì
    // và app đang theo ngôn ngữ của máy.
    val current = AppLanguage.fromTag(stringResource(R.string.app_language_tag))
    LanguageContent(
        current = current,
        onBack = viewModel::onBack,
        onSelect = { language -> if (language != current) viewModel.onLanguageSelected(language) },
    )
}

@Composable
private fun LanguageContent(current: AppLanguage, onBack: () -> Unit, onSelect: (AppLanguage) -> Unit) {
    val colors = appColors()
    Column(Modifier.fillMaxSize().background(colors.scaffoldBackground)) {
        AppTopBar(title = stringResource(R.string.language_title), onNavigationClick = onBack)
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(AppDimens.defaultPadding)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
        ) {
            LanguageOption(
                name = stringResource(R.string.language_vietnamese),
                note = stringResource(R.string.language_vietnamese_note),
                selected = current == AppLanguage.Vietnamese,
                onClick = { onSelect(AppLanguage.Vietnamese) },
            )
            LanguageOption(
                name = stringResource(R.string.language_english),
                note = stringResource(R.string.language_english_note),
                selected = current == AppLanguage.English,
                onClick = { onSelect(AppLanguage.English) },
            )
            Text(
                text = stringResource(R.string.language_note),
                modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = AppDimens.paddingSmallest),
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = colors.textSecondary,
            )
        }
    }
}

/** Một lựa chọn ngôn ngữ: nút tròn chọn-một, tên ngôn ngữ và dòng chú thích. */
@Composable
private fun LanguageOption(name: String, note: String, selected: Boolean, onClick: () -> Unit) {
    val colors = appColors()
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = OptionHeight)
            .clip(shape)
            .background(colors.cardBackground)
            // Lựa chọn đang dùng có viền cam dày hơn.
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.borderStrong, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = OptionPadding, vertical = AppDimens.paddingVerySmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OptionPadding),
    ) {
        // onClick = null: nút tròn chỉ để nhìn; thao tác chạm do cả dòng (`selectable`) nhận.
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = colors.primary,
                unselectedColor = colors.borderStrong,
            ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Text(text = note, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}

// ---------------------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------------------

@Preview(name = "Ngôn ngữ", widthDp = 390, heightDp = 844)
@Composable
private fun LanguagePreview() = OnTapTldTheme(themeMode = ThemeMode.LIGHT) {
    LanguageContent(current = AppLanguage.Vietnamese, onBack = {}, onSelect = {})
}

@Preview(name = "Ngôn ngữ — Dark", widthDp = 390, heightDp = 844)
@Composable
private fun LanguageDarkPreview() = OnTapTldTheme(themeMode = ThemeMode.DARK) {
    LanguageContent(current = AppLanguage.English, onBack = {}, onSelect = {})
}
