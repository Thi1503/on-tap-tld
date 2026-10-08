package com.ledinhthi.ontaptld.feature.settings

import com.ledinhthi.ontaptld.core.MainDispatcherRule
import com.ledinhthi.ontaptld.core.data.local.prefs.AppPreferences
import com.ledinhthi.ontaptld.core.data.local.prefs.ReminderSettings
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.GlobalExceptionHandler
import com.ledinhthi.ontaptld.core.exception.LocalErrorKind
import com.ledinhthi.ontaptld.core.presentation.language.AppLanguage
import com.ledinhthi.ontaptld.core.presentation.language.AppLanguageManager
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.navigation.SnackBarType
import com.ledinhthi.ontaptld.core.presentation.text.StringProvider
import com.ledinhthi.ontaptld.core.exception.AuthErrorKind
import com.ledinhthi.ontaptld.feature.auth.domain.AuthRepository
import com.ledinhthi.ontaptld.feature.auth.domain.model.AuthUser
import com.ledinhthi.ontaptld.feature.auth.domain.usecase.ObserveAuthUserUseCase
import com.ledinhthi.ontaptld.feature.auth.domain.usecase.SignOutUseCase
import com.ledinhthi.ontaptld.feature.capture.domain.model.AiQuota
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.ObserveAiQuotaUseCase
import com.ledinhthi.ontaptld.feature.settings.domain.DeleteAllLocalDataUseCase
import com.ledinhthi.ontaptld.feature.settings.presentation.language.LanguageViewModel
import com.ledinhthi.ontaptld.feature.settings.presentation.settings.SettingsViewModel
import com.ledinhthi.ontaptld.navigation.GoogleSignInRoute
import com.ledinhthi.ontaptld.navigation.LanguageRoute
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    // MutableStateFlow đóng vai file cài đặt: ghi giá trị mới vào là ViewModel nhận được ngay.
    private val themeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    private val reminderFlow = MutableStateFlow(ReminderSettings.Default)
    private val prefs = mockk<AppPreferences>(relaxed = true)
    private val observeAiQuota = mockk<ObserveAiQuotaUseCase>()
    private val deleteAllLocalData = mockk<DeleteAllLocalDataUseCase>()

    // Kho tài khoản giả: `userFlow` đóng vai "ai đang đăng nhập", đăng xuất thì nó về null —
    // giống hệt cách Firebase báo lại cho app.
    private val userFlow = MutableStateFlow<AuthUser?>(null)
    private val authRepository = mockk<AuthRepository> {
        every { currentUser } returns userFlow
        coEvery { signOut() } coAnswers { userFlow.value = null }
    }
    private val observeAuthUser = ObserveAuthUserUseCase(authRepository)
    private val signOut = SignOutUseCase(authRepository)
    private val signedInUser = AuthUser(uid = "u1", displayName = "Minh Anh", email = "minhanh@example.com")
    private val navigator = mockk<AppNavigator>(relaxed = true)
    private val exceptionHandler = mockk<GlobalExceptionHandler>(relaxed = true)
    private val strings = mockk<StringProvider>(relaxed = true)
    private val toolbox = ViewModelToolbox(navigator, exceptionHandler, strings)

    private fun viewModel(): SettingsViewModel {
        every { prefs.themeMode } returns themeFlow
        every { prefs.reminder } returns reminderFlow
        coEvery { prefs.setThemeMode(any()) } coAnswers { themeFlow.value = firstArg() }
        coEvery { prefs.setReminderEnabled(any()) } coAnswers {
            reminderFlow.value = reminderFlow.value.copy(enabled = firstArg())
        }
        coEvery { prefs.setReminderTime(any(), any()) } coAnswers {
            reminderFlow.value = reminderFlow.value.copy(minuteOfDay = firstArg<Int>() * 60 + secondArg<Int>())
        }
        every { observeAiQuota.invoke() } returns flowOf(AiQuota(remaining = 8, max = 10))
        return SettingsViewModel(toolbox, prefs, observeAiQuota, deleteAllLocalData, observeAuthUser, signOut).apply { isTestMode = true }
    }

    @Test
    fun `mo man - hien dung cai dat da luu va so luot AI da dung`() = runTest {
        themeFlow.value = ThemeMode.DARK
        reminderFlow.value = ReminderSettings(enabled = true, minuteOfDay = 7 * 60 + 30)

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(ThemeMode.DARK, state.themeMode)
        assertTrue(state.reminder.enabled)
        assertEquals(7, state.reminder.hour)
        assertEquals(30, state.reminder.minute)
        assertEquals(2, state.aiUsed) // còn 8 trên 10 -> đã dùng 2
    }

    @Test
    fun `chua doc xong bo dem luot AI - coi nhu chua dung luot nao`() = runTest {
        every { prefs.themeMode } returns themeFlow
        every { prefs.reminder } returns reminderFlow
        every { observeAiQuota.invoke() } returns flow { } // dòng dữ liệu không phát gì
        val vm = SettingsViewModel(toolbox, prefs, observeAiQuota, deleteAllLocalData, observeAuthUser, signOut)
        advanceUntilIdle()

        assertNull(vm.uiState.value.aiQuota)
        assertEquals(0, vm.uiState.value.aiUsed)
    }

    @Test
    fun `chon giao dien - luu lai va man doi theo`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onThemeSelected(ThemeMode.LIGHT)
        advanceUntilIdle()

        coVerify { prefs.setThemeMode(ThemeMode.LIGHT) }
        assertEquals(ThemeMode.LIGHT, vm.uiState.value.themeMode)
    }

    @Test
    fun `bat nhac on roi doi gio - luu ca hai`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.reminder.enabled) // mặc định chưa bật

        vm.onReminderToggle(true)
        vm.onReminderTimePicked(hour = 6, minute = 45)
        advanceUntilIdle()

        val reminder = vm.uiState.value.reminder
        assertTrue(reminder.enabled)
        assertEquals(6 * 60 + 45, reminder.minuteOfDay)
    }

    @Test
    fun `doc cai dat loi - man van mo voi gia tri mac dinh`() = runTest {
        every { prefs.themeMode } returns flow { throw IllegalStateException("hỏng file") }
        every { prefs.reminder } returns reminderFlow
        every { observeAiQuota.invoke() } returns flowOf(AiQuota(remaining = 10, max = 10))

        val vm = SettingsViewModel(toolbox, prefs, observeAiQuota, deleteAllLocalData, observeAuthUser, signOut).apply { isTestMode = true }
        advanceUntilIdle()

        assertEquals(ThemeMode.SYSTEM, vm.uiState.value.themeMode)
        assertEquals(AiQuota(10, 10), vm.uiState.value.aiQuota)
    }

    @Test
    fun `bam Ngon ngu - mo man chon ngon ngu`() = runTest {
        viewModel().onLanguageClick()

        verify { navigator.to(LanguageRoute) }
    }

    @Test
    fun `bam Dang nhap Google - mo man dang nhap`() = runTest {
        viewModel().onGoogleSignInClick()

        verify { navigator.to(GoogleSignInRoute) }
    }

    @Test
    fun `chua dang nhap thi khong co tai khoan - dang nhap xong the tai khoan hien ngay`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertNull(vm.uiState.value.account)

        userFlow.value = signedInUser // người dùng vừa đăng nhập ở màn Đăng nhập Google
        advanceUntilIdle()

        assertEquals(signedInUser, vm.uiState.value.account)
    }

    @Test
    fun `xac nhan dang xuat - dang xuat, bao thanh cong, man tro ve trang thai chua dang nhap`() = runTest {
        userFlow.value = signedInUser
        val vm = viewModel()
        advanceUntilIdle()

        vm.onSignOutConfirmed()
        advanceUntilIdle()

        coVerify(exactly = 1) { authRepository.signOut() }
        verify { navigator.showSnackBar(any(), SnackBarType.SUCCESS) }
        assertNull(vm.uiState.value.account)
        // Đăng xuất chỉ rời tài khoản, không đụng tới dữ liệu trên máy.
        coVerify(exactly = 0) { deleteAllLocalData.invoke() }
        assertFalse(vm.uiState.value.status.isLoadingOverlay)
    }

    @Test
    fun `dang xuat loi - de bo xu ly loi chung bao, van con dang nhap`() = runTest {
        userFlow.value = signedInUser
        coEvery { authRepository.signOut() } throws AppException.AuthException(AuthErrorKind.UNKNOWN)
        val vm = viewModel()
        advanceUntilIdle()

        vm.onSignOutConfirmed()
        advanceUntilIdle()

        verify { exceptionHandler.handle(any()) }
        verify(exactly = 0) { navigator.showSnackBar(any(), SnackBarType.SUCCESS) }
        assertEquals(signedInUser, vm.uiState.value.account)
    }

    @Test
    fun `bam Chinh sach quyen rieng tu - tam bao sap co`() = runTest {
        viewModel().onPrivacyPolicyClick()

        verify { navigator.showSnackBar(any(), SnackBarType.INFO) }
    }

    @Test
    fun `xac nhan xoa du lieu - xoa roi bao thanh cong, khong roi man`() = runTest {
        coEvery { deleteAllLocalData.invoke() } returns Unit
        val vm = viewModel()

        vm.onDeleteAllConfirmed()
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteAllLocalData.invoke() }
        verify { navigator.showSnackBar(any(), SnackBarType.SUCCESS) }
        verify(exactly = 0) { navigator.back() }
        assertFalse(vm.uiState.value.status.isLoadingOverlay)
    }

    @Test
    fun `xoa du lieu loi - de bo xu ly loi chung bao, khong bao thanh cong`() = runTest {
        coEvery { deleteAllLocalData.invoke() } throws AppException.LocalException(LocalErrorKind.DISK_FULL)
        val vm = viewModel()

        vm.onDeleteAllConfirmed()
        advanceUntilIdle()

        verify { exceptionHandler.handle(any()) }
        verify(exactly = 0) { navigator.showSnackBar(any(), SnackBarType.SUCCESS) }
        assertFalse(vm.uiState.value.status.isLoadingOverlay)
    }

    @Test
    fun `man Ngon ngu - chon ngon ngu thi ap dung ngay va luu ban sao, nut quay lai lui mot buoc`() = runTest {
        val languageManager = mockk<AppLanguageManager>(relaxed = true)
        val vm = LanguageViewModel(navigator, languageManager, prefs)

        vm.onLanguageSelected(AppLanguage.English)
        vm.onBack()
        advanceUntilIdle()

        verify { languageManager.setLanguage(AppLanguage.English) }
        // Bản sao cho thông báo nhắc ôn đọc khi app đang đóng.
        coVerify { prefs.setLanguageTag("en") }
        verify { navigator.back() }
    }

    @Test
    fun `ma ngon ngu la - coi la tieng Viet`() {
        assertEquals(AppLanguage.English, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.Vietnamese, AppLanguage.fromTag("vi"))
        assertEquals(AppLanguage.Vietnamese, AppLanguage.fromTag("fr"))
    }
}
