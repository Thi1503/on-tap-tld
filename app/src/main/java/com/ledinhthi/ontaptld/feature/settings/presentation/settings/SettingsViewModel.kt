package com.ledinhthi.ontaptld.feature.settings.presentation.settings

import androidx.lifecycle.viewModelScope
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.data.local.prefs.AppPreferences
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode
import com.ledinhthi.ontaptld.core.presentation.mvi.BaseViewModel
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.SnackBarType
import com.ledinhthi.ontaptld.feature.auth.domain.usecase.ObserveAuthUserUseCase
import com.ledinhthi.ontaptld.feature.auth.domain.usecase.SignOutUseCase
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.ObserveAiQuotaUseCase
import com.ledinhthi.ontaptld.feature.settings.domain.DeleteAllLocalDataUseCase
import com.ledinhthi.ontaptld.navigation.GoogleSignInRoute
import com.ledinhthi.ontaptld.navigation.LanguageRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel của màn Cài đặt (route `SettingsRoute`). Mọi lựa chọn được ghi ngay vào
 * [AppPreferences]; màn hình không tự giữ giá trị mà vẽ lại theo đúng cái vừa lưu — nhờ vậy
 * thứ nhìn thấy trên màn luôn là thứ đang có hiệu lực.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    toolbox: ViewModelToolbox,
    private val prefs: AppPreferences,
    observeAiQuota: ObserveAiQuotaUseCase,
    private val deleteAllLocalData: DeleteAllLocalDataUseCase,
    observeAuthUser: ObserveAuthUserUseCase,
    private val signOut: SignOutUseCase,
) : BaseViewModel<SettingsState>(SettingsState(), toolbox) {

    init {
        prefs.themeMode.collectInto { copy(themeMode = it) }
        prefs.reminder.collectInto { copy(reminder = it) }
        observeAiQuota().collectInto { copy(aiQuota = it) }
        // Đăng nhập xong ở màn Đăng nhập Google rồi quay lại là thẻ tài khoản hiện ngay.
        observeAuthUser().collectInto { copy(account = it) }
    }

    /**
     * Nghe một dòng dữ liệu và chép từng giá trị mới vào state. Đọc lỗi thì chỉ ghi log: phần
     * đó của màn giữ giá trị mặc định, các phần khác vẫn dùng được.
     */
    private fun <T> Flow<T>.collectInto(reduce: SettingsState.(T) -> SettingsState) {
        onEach { value -> setState { reduce(value) } }
            .catch { e -> if (!isTestMode) Timber.e(e) }
            .launchIn(viewModelScope)
    }

    fun onBack() = navigator.back()

    fun onThemeSelected(mode: ThemeMode) = launchGuarded { prefs.setThemeMode(mode) }

    fun onReminderToggle(enabled: Boolean) = launchGuarded { prefs.setReminderEnabled(enabled) }

    fun onReminderTimePicked(hour: Int, minute: Int) = launchGuarded { prefs.setReminderTime(hour, minute) }

    fun onLanguageClick() = navigator.to(LanguageRoute)

    fun onGoogleSignInClick() = navigator.to(GoogleSignInRoute)

    /**
     * Gọi SAU khi người dùng đã xác nhận trong hộp thoại hỏi lại. Không phải tự xoá `account`
     * khỏi state: đăng xuất xong thì dòng dữ liệu "ai đang đăng nhập" phát `null`.
     */
    fun onSignOutConfirmed() = launchGuarded(showLoadingOverlay = true) {
        signOut()
        navigator.showSnackBar(strings.get(R.string.settings_sign_out_done), SnackBarType.SUCCESS)
    }

    /** Chưa có trang chính sách — tạm báo "sắp có" (Thi chốt 8/10/2026). */
    fun onPrivacyPolicyClick() =
        navigator.showSnackBar(strings.get(R.string.common_coming_soon), SnackBarType.INFO)

    /** Gọi SAU khi người dùng đã xác nhận trong hộp thoại hỏi lại. */
    fun onDeleteAllConfirmed() = launchGuarded(showLoadingOverlay = true) {
        deleteAllLocalData()
        navigator.showSnackBar(strings.get(R.string.settings_delete_all_done), SnackBarType.SUCCESS)
    }
}
