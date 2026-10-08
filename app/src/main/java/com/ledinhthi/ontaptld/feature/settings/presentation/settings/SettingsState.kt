package com.ledinhthi.ontaptld.feature.settings.presentation.settings

import com.ledinhthi.ontaptld.core.data.local.prefs.ReminderSettings
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode
import com.ledinhthi.ontaptld.core.presentation.mvi.UiState
import com.ledinhthi.ontaptld.core.presentation.mvi.UiStatus
import com.ledinhthi.ontaptld.feature.auth.domain.model.AuthUser
import com.ledinhthi.ontaptld.feature.capture.domain.model.AiQuota

data class SettingsState(
    override val status: UiStatus = UiStatus(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val reminder: ReminderSettings = ReminderSettings.Default,
    /** null khi chưa đọc xong bộ đếm lượt AI. */
    val aiQuota: AiQuota? = null,
    /** Người đang đăng nhập Google; null khi chưa đăng nhập. */
    val account: AuthUser? = null,
) : UiState {
    /** Số lượt AI đã dùng hôm nay (bộ đếm lưu số lượt CÒN LẠI). */
    val aiUsed: Int get() = aiQuota?.let { it.max - it.remaining } ?: 0

    override fun withStatus(status: UiStatus) = copy(status = status)
}
