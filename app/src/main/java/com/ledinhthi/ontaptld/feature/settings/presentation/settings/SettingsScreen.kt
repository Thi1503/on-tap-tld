package com.ledinhthi.ontaptld.feature.settings.presentation.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledinhthi.ontaptld.BuildConfig
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.data.local.prefs.ReminderSettings
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode
import com.ledinhthi.ontaptld.core.presentation.components.AppBadge
import com.ledinhthi.ontaptld.core.presentation.components.AppCard
import com.ledinhthi.ontaptld.core.presentation.components.AppSwitch
import com.ledinhthi.ontaptld.core.presentation.components.AppTextButton
import com.ledinhthi.ontaptld.core.presentation.components.AppTopBar
import com.ledinhthi.ontaptld.core.presentation.components.ConfirmDialog
import com.ledinhthi.ontaptld.core.presentation.components.LoadingOverlay
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.OnTapTldTheme
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.capture.domain.model.AiQuota
import com.ledinhthi.ontaptld.feature.reminder.data.AndroidReminderNotifier
import java.util.Calendar
import java.util.Locale

// Số đo lấy từ bản design (Settings.dc.html).
private val RowPadding = 14.dp
private val RowHeight = 56.dp
private val CompactRowHeight = 52.dp
private val ThemeOptionHeight = 44.dp
private val QuotaBarHeight = 8.dp

/**
 * Màn Cài đặt (route `SettingsRoute`), mở từ nút góc phải trên Home. Cùng khuôn Screen → Content
 * như các màn khác: hàm này nối với ViewModel và lo các hộp thoại, [SettingsContent] chỉ vẽ.
 */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var askDeleteAll by rememberSaveable { mutableStateOf(false) }

    // Bật nhắc ôn = phải gửi được thông báo. Từ Android 13 app phải XIN quyền này lúc chạy.
    val context = LocalContext.current
    var askOpenNotificationSettings by rememberSaveable { mutableStateOf(false) }
    // "Launcher" mở hộp thoại xin quyền của hệ thống; khối lệnh phía sau chạy khi người dùng trả
    // lời. Đã từ chối hẳn từ trước thì hệ thống không hỏi nữa mà trả về "không" ngay.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.onReminderToggle(true) else askOpenNotificationSettings = true
    }
    val onReminderToggle: (Boolean) -> Unit = { enabled ->
        when {
            !enabled -> viewModel.onReminderToggle(false)
            AndroidReminderNotifier.needsRuntimePermission(context) ->
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            // Có quyền nhưng người dùng đã tắt thông báo của app trong Cài đặt của máy.
            !NotificationManagerCompat.from(context).areNotificationsEnabled() -> askOpenNotificationSettings = true
            else -> viewModel.onReminderToggle(true)
        }
    }

    LoadingOverlay(isLoading = state.status.isLoadingOverlay) {
        SettingsContent(
            state = state,
            onBack = viewModel::onBack,
            onThemeSelected = viewModel::onThemeSelected,
            onLanguageClick = viewModel::onLanguageClick,
            onReminderToggle = onReminderToggle,
            onReminderTimeClick = { showTimePicker = true },
            onPrivacyPolicyClick = viewModel::onPrivacyPolicyClick,
            onDeleteAllClick = { askDeleteAll = true },
        )
    }

    if (showTimePicker) {
        ReminderTimeDialog(
            reminder = state.reminder,
            onConfirm = { hour, minute ->
                showTimePicker = false
                viewModel.onReminderTimePicked(hour, minute)
            },
            onDismiss = { showTimePicker = false },
        )
    }

    if (askOpenNotificationSettings) {
        ConfirmDialog(
            title = stringResource(R.string.settings_notification_off_title),
            message = stringResource(R.string.settings_notification_off_message),
            confirmText = stringResource(R.string.capture_permission_settings),
            onConfirm = {
                askOpenNotificationSettings = false
                context.openNotificationSettings()
            },
            onDismiss = { askOpenNotificationSettings = false },
        )
    }

    if (askDeleteAll) {
        ConfirmDialog(
            title = stringResource(R.string.settings_delete_all_title),
            message = stringResource(R.string.settings_delete_all_message),
            confirmText = stringResource(R.string.settings_delete_all_confirm),
            destructive = true,
            onConfirm = {
                askDeleteAll = false
                viewModel.onDeleteAllConfirmed()
            },
            onDismiss = { askDeleteAll = false },
        )
    }
}

/** Mở trang cài đặt thông báo của riêng app này trong Cài đặt của máy. */
private fun Context.openNotificationSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        // Android 7.x chưa có trang riêng cho thông báo: mở trang thông tin chung của app.
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    }
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    onBack: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onLanguageClick: () -> Unit,
    onReminderToggle: (Boolean) -> Unit,
    onReminderTimeClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onDeleteAllClick: () -> Unit,
) {
    val colors = appColors()
    Column(Modifier.fillMaxSize().background(colors.scaffoldBackground)) {
        AppTopBar(title = stringResource(R.string.settings_title), onNavigationClick = onBack)

        // Bản design vừa khít một màn 844dp; máy thấp hơn hoặc cỡ chữ lớn thì phải cuộn được.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(AppDimens.defaultPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.defaultPadding),
        ) {
            SettingsSection(stringResource(R.string.settings_section_appearance)) {
                ThemeSelector(selected = state.themeMode, onSelect = onThemeSelected)
            }

            // Mục này không có trong artboard Settings (chỉ có ở artboard đã đăng nhập) — Thi
            // chốt 8/10/2026: đặt thành một mục riêng ngay dưới Giao diện.
            SettingsSection(stringResource(R.string.settings_section_language)) {
                SettingsCard {
                    SettingsRow(
                        title = stringResource(R.string.settings_language_row),
                        onClick = onLanguageClick,
                    ) {
                        ValueWithChevron(
                            value = stringResource(R.string.app_language_name),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }

            SettingsSection(stringResource(R.string.settings_section_reminder)) {
                ReminderCard(
                    reminder = state.reminder,
                    onToggle = onReminderToggle,
                    onTimeClick = onReminderTimeClick,
                )
            }

            SettingsSection(stringResource(R.string.settings_section_ai)) {
                AiUsageCard(used = state.aiUsed, quota = state.aiQuota)
            }

            SettingsSection(stringResource(R.string.settings_section_sync)) {
                GoogleSignInCard()
            }

            SettingsSection(stringResource(R.string.settings_section_data)) {
                SettingsCard {
                    SettingsRow(
                        title = stringResource(R.string.settings_privacy_policy),
                        minHeight = CompactRowHeight,
                        onClick = onPrivacyPolicyClick,
                    ) { Chevron() }
                    RowDivider()
                    // Chỉ để xem số phiên bản, không bấm được (Thi chốt 8/10/2026).
                    SettingsRow(
                        title = stringResource(R.string.settings_about),
                        minHeight = CompactRowHeight,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                    RowDivider()
                    DeleteAllRow(onClick = onDeleteAllClick)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// Khối dựng chung của màn
// ---------------------------------------------------------------------------------------

/** Một mục của màn: dòng tiêu đề nhỏ viết hoa, bên dưới là nội dung (thường là một thẻ). */
@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall)) {
        Text(
            // Viết hoa bằng code để chuỗi trong strings.xml vẫn viết thường, dễ dịch.
            text = title.uppercase(Locale.getDefault()),
            // `heading()`: trình đọc màn hình biết đây là tiêu đề mục, cho phép nhảy giữa các mục.
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
            color = appColors().textSecondary,
        )
        content()
    }
}

/** Thẻ chứa các dòng cài đặt: các dòng tự lo lề trong, nên thẻ không thêm lề. */
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) { content() }
}

/**
 * Một dòng cài đặt: tên ở bên trái, [trailing] (giá trị, mũi tên, công tắc…) ở bên phải.
 * [onClick] = null thì dòng chỉ để xem.
 */
@Composable
private fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = RowHeight,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // `then(...)` nối thêm một Modifier có điều kiện vào chuỗi đang có.
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = minHeight)
            .padding(horizontal = RowPadding, vertical = AppDimens.paddingVerySmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = appColors().textPrimary,
        )
        trailing()
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(Modifier.padding(horizontal = RowPadding), color = appColors().divider)
}

@Composable
private fun Chevron(tint: Color = appColors().textSecondary) {
    Icon(
        painter = painterResource(R.drawable.ic_chevron_right),
        contentDescription = null, // chỉ để trang trí; cả dòng đã mang vai trò nút bấm
        modifier = Modifier.size(AppDimens.sizeIconSmall),
        tint = tint,
    )
}

@Composable
private fun ValueWithChevron(value: String, style: TextStyle) {
    val colors = appColors()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.padding6),
    ) {
        Text(text = value, style = style, color = colors.textSecondary)
        Chevron()
    }
}

// ---------------------------------------------------------------------------------------
// Giao diện
// ---------------------------------------------------------------------------------------

/** Ba ô Theo máy / Sáng / Tối nằm trong một thẻ; ô đang chọn được tô đậm. */
@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(AppDimens.padding6)) {
        Row(
            // selectableGroup: báo cho trình đọc màn hình biết các ô này là MỘT nhóm chọn-một.
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmallest),
        ) {
            // `entries` = mọi giá trị của enum theo thứ tự khai báo: SYSTEM, LIGHT, DARK.
            ThemeMode.entries.forEach { mode ->
                ThemeOption(
                    label = stringResource(
                        when (mode) {
                            ThemeMode.SYSTEM -> R.string.settings_theme_system
                            ThemeMode.LIGHT -> R.string.settings_theme_light
                            ThemeMode.DARK -> R.string.settings_theme_dark
                        },
                    ),
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    modifier = Modifier.weight(1f), // ba ô chia đều bề ngang
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = appColors()
    val background by animateColorAsState(
        targetValue = if (selected) colors.chipSelectedBackground else Color.Transparent,
        label = "themeOptionBackground",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) colors.chipSelectedText else colors.textStrong,
        label = "themeOptionText",
    )
    Box(
        modifier = modifier
            .height(ThemeOptionHeight)
            .clip(RoundedCornerShape(AppDimens.radius8))
            .background(background)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
            color = textColor,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------------------
// Nhắc ôn tập
// ---------------------------------------------------------------------------------------

@Composable
private fun ReminderCard(reminder: ReminderSettings, onToggle: (Boolean) -> Unit, onTimeClick: () -> Unit) {
    SettingsCard {
        SettingsRow(
            title = stringResource(R.string.settings_reminder_daily),
            // `toggleable` đặt ở cả dòng: chạm vào chữ hay công tắc đều đổi được, và trình đọc
            // màn hình đọc cả dòng như MỘT công tắc có nhãn.
            modifier = Modifier.toggleable(value = reminder.enabled, role = Role.Switch, onValueChange = onToggle),
        ) { AppSwitch(checked = reminder.enabled) }
        RowDivider()
        // Tắt nhắc thì giờ nhắc không còn ý nghĩa: làm mờ và không cho bấm.
        SettingsRow(
            title = stringResource(R.string.settings_reminder_time),
            modifier = Modifier.alpha(if (reminder.enabled) 1f else 0.4f),
            onClick = if (reminder.enabled) onTimeClick else null,
        ) {
            ValueWithChevron(
                value = formatReminderTime(reminder),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/** Giờ nhắc viết theo thói quen của máy: "20:00" hoặc "8:00 PM". */
@Composable
private fun formatReminderTime(reminder: ReminderSettings): String {
    val context = LocalContext.current
    // remember(khoá): chỉ tính lại khi giờ nhắc đổi, không tính lại mỗi lần màn vẽ lại.
    return remember(reminder.minuteOfDay, context) {
        val time = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
        }
        DateFormat.getTimeFormat(context).format(time.time)
    }
}

/** Hộp thoại chọn giờ nhắc — đồng hồ chọn giờ chuẩn của Material (design không vẽ). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(reminder: ReminderSettings, onConfirm: (hour: Int, minute: Int) -> Unit, onDismiss: () -> Unit) {
    val colors = appColors()
    val pickerState = rememberTimePickerState(
        initialHour = reminder.hour,
        initialMinute = reminder.minute,
        // Máy đang dùng kiểu 24 giờ hay kiểu sáng / chiều thì đồng hồ theo kiểu đó.
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
    TimePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AppTextButton(
                text = stringResource(R.string.common_save),
                onClick = { onConfirm(pickerState.hour, pickerState.minute) },
            )
        },
        dismissButton = {
            AppTextButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                contentColor = colors.textStrong,
            )
        },
        title = {
            Text(
                text = stringResource(R.string.settings_reminder_time),
                style = MaterialTheme.typography.titleMedium,
            )
        },
    ) {
        TimePicker(state = pickerState)
    }
}

// ---------------------------------------------------------------------------------------
// Lượt AI, đồng bộ, xoá dữ liệu
// ---------------------------------------------------------------------------------------

@Composable
private fun AiUsageCard(used: Int, quota: AiQuota?) {
    val colors = appColors()
    val max = quota?.max ?: 0
    // Phần đã dùng trên tổng, từ 0 tới 1. Chưa đọc xong bộ đếm (max = 0) thì coi như 0.
    val fraction by animateFloatAsState(
        targetValue = if (max > 0) (used.toFloat() / max).coerceIn(0f, 1f) else 0f,
        label = "aiUsage",
    )
    AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(RowPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.settings_ai_usage_today),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (quota != null) {
                Text(
                    text = stringResource(R.string.settings_ai_usage_value, used, max),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
        Box(
            Modifier
                .padding(vertical = AppDimens.padding10)
                .fillMaxWidth()
                .height(QuotaBarHeight)
                .clip(RoundedCornerShape(AppDimens.radius4))
                .background(colors.neutralSoft)
                // Con số "2 / 10" bên trên đã nói đủ; thanh này báo thêm cho trình đọc màn hình.
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) },
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(colors.primary, RoundedCornerShape(AppDimens.radius4)),
            )
        }
        Text(
            text = stringResource(R.string.settings_ai_usage_note),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}

/** Đăng nhập Google làm ở bước 6 — hiện tại chỉ là dòng giới thiệu kèm nhãn "Sắp có". */
@Composable
private fun GoogleSignInCard() {
    val colors = appColors()
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = RowPadding, vertical = AppDimens.paddingSmall),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = stringResource(R.string.settings_sync_google), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.settings_sync_google_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            AppBadge(text = stringResource(R.string.settings_badge_coming_soon))
        }
    }
}

@Composable
private fun DeleteAllRow(onClick: () -> Unit) {
    val colors = appColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = CompactRowHeight)
            .padding(horizontal = RowPadding, vertical = AppDimens.paddingVerySmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.padding10),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = null,
            modifier = Modifier.size(AppDimens.sizeIcon),
            tint = colors.statusRedText,
        )
        Text(
            text = stringResource(R.string.settings_delete_all),
            style = MaterialTheme.typography.titleMedium,
            color = colors.statusRedText,
        )
    }
}

// ---------------------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------------------

private val previewState = SettingsState(
    reminder = ReminderSettings(enabled = true, minuteOfDay = 20 * 60),
    aiQuota = AiQuota(remaining = 8, max = 10),
)

@Composable
private fun PreviewHost(state: SettingsState, themeMode: ThemeMode = ThemeMode.LIGHT) {
    OnTapTldTheme(themeMode = themeMode) {
        SettingsContent(
            state = state,
            onBack = {},
            onThemeSelected = {},
            onLanguageClick = {},
            onReminderToggle = {},
            onReminderTimeClick = {},
            onPrivacyPolicyClick = {},
            onDeleteAllClick = {},
        )
    }
}

@Preview(name = "Cài đặt", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsPreview() = PreviewHost(previewState)

@Preview(name = "Cài đặt — Dark", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsDarkPreview() = PreviewHost(previewState.copy(themeMode = ThemeMode.DARK), ThemeMode.DARK)

@Preview(name = "Cài đặt — tắt nhắc, hết lượt AI", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsReminderOffPreview() = PreviewHost(
    previewState.copy(reminder = ReminderSettings.Default, aiQuota = AiQuota(remaining = 0, max = 10)),
)
