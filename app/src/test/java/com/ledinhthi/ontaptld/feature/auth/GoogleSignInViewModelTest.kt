package com.ledinhthi.ontaptld.feature.auth

import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.MainDispatcherRule
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.AppExceptionWrapper
import com.ledinhthi.ontaptld.core.exception.AuthErrorKind
import com.ledinhthi.ontaptld.core.exception.GlobalExceptionHandler
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.navigation.SnackBarType
import com.ledinhthi.ontaptld.core.presentation.text.StringProvider
import com.ledinhthi.ontaptld.feature.auth.domain.AuthRepository
import com.ledinhthi.ontaptld.feature.auth.domain.model.AuthUser
import com.ledinhthi.ontaptld.feature.auth.domain.usecase.SignInWithGoogleUseCase
import com.ledinhthi.ontaptld.feature.auth.presentation.signin.GoogleIdTokenResult
import com.ledinhthi.ontaptld.feature.auth.presentation.signin.GoogleSignInViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleSignInViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = mockk<AuthRepository>()
    private val navigator = mockk<AppNavigator>(relaxed = true)
    private val exceptionHandler = mockk<GlobalExceptionHandler>(relaxed = true)
    private val strings = mockk<StringProvider>(relaxed = true)
    private val toolbox = ViewModelToolbox(navigator, exceptionHandler, strings)

    private val user = AuthUser(uid = "u1", displayName = "Minh Anh", email = "minhanh@example.com")

    private fun viewModel() =
        GoogleSignInViewModel(toolbox, SignInWithGoogleUseCase(repository)).apply { isTestMode = true }

    @Test
    fun `chon tai khoan xong - dang nhap bang token do, bao thanh cong roi ve Cai dat`() = runTest {
        coEvery { repository.signInWithGoogle("token-1") } returns user
        every { strings.get(R.string.auth_signed_in_as, "minhanh@example.com") } returns "Đã đăng nhập bằng minhanh@example.com"
        val vm = viewModel()

        vm.onGoogleIdTokenResult(GoogleIdTokenResult.Success("token-1"))
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.signInWithGoogle("token-1") }
        verify { navigator.showSnackBar("Đã đăng nhập bằng minhanh@example.com", SnackBarType.SUCCESS) }
        verify(exactly = 1) { navigator.back() }
        assertFalse(vm.uiState.value.status.isLoadingOverlay)
    }

    @Test
    fun `tai khoan khong co email va ten - van bao da dang nhap`() = runTest {
        coEvery { repository.signInWithGoogle(any()) } returns AuthUser("u2", displayName = null, email = null)
        val vm = viewModel()

        vm.onGoogleIdTokenResult(GoogleIdTokenResult.Success("token-2"))
        advanceUntilIdle()

        verify { strings.get(R.string.auth_signed_in) }
        verify { navigator.showSnackBar(any(), SnackBarType.SUCCESS) }
        verify(exactly = 1) { navigator.back() }
    }

    @Test
    fun `dong hop chon tai khoan - khong lam gi, khong bao loi`() = runTest {
        val vm = viewModel()

        vm.onGoogleIdTokenResult(GoogleIdTokenResult.Cancelled)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.signInWithGoogle(any()) }
        verify(exactly = 0) { exceptionHandler.handle(any()) }
        verify(exactly = 0) { navigator.back() }
        verify(exactly = 0) { navigator.showSnackBar(any(), any()) }
    }

    @Test
    fun `hop chon tai khoan bao loi - chuyen dung loai loi cho bo xu ly chung, o lai man`() = runTest {
        val handled = slot<AppExceptionWrapper>()
        every { exceptionHandler.handle(capture(handled)) } returns Unit
        val vm = viewModel()

        vm.onGoogleIdTokenResult(GoogleIdTokenResult.Failure(AuthErrorKind.NO_GOOGLE_ACCOUNT))
        advanceUntilIdle()

        val exception = handled.captured.exception
        assertTrue(exception is AppException.AuthException)
        assertEquals(AuthErrorKind.NO_GOOGLE_ACCOUNT, (exception as AppException.AuthException).kind)
        coVerify(exactly = 0) { repository.signInWithGoogle(any()) }
        verify(exactly = 0) { navigator.back() }
        assertFalse(vm.uiState.value.status.isLoadingOverlay)
    }

    @Test
    fun `dang nhap Firebase loi mang - bao loi, khong bao thanh cong, o lai man`() = runTest {
        coEvery { repository.signInWithGoogle(any()) } throws AppException.AuthException(AuthErrorKind.NETWORK)
        val vm = viewModel()

        vm.onGoogleIdTokenResult(GoogleIdTokenResult.Success("token-3"))
        advanceUntilIdle()

        verify { exceptionHandler.handle(match { (it.exception as? AppException.AuthException)?.kind == AuthErrorKind.NETWORK }) }
        verify(exactly = 0) { navigator.showSnackBar(any(), SnackBarType.SUCCESS) }
        verify(exactly = 0) { navigator.back() }
        assertFalse(vm.uiState.value.status.isLoadingOverlay)
    }

    @Test
    fun `dang dang nhap do ma nhan them ket qua - chi dang nhap mot lan`() = runTest {
        // CompletableDeferred đóng vai lệnh đăng nhập CHƯA trả lời: ViewModel đứng chờ ở `await()`.
        val pending = CompletableDeferred<AuthUser>()
        coEvery { repository.signInWithGoogle(any()) } coAnswers { pending.await() }
        val vm = viewModel()

        vm.onGoogleIdTokenResult(GoogleIdTokenResult.Success("token-4"))
        assertTrue(vm.uiState.value.status.isLoadingOverlay) // lớp "đang xử lý" đang hiện
        vm.onGoogleIdTokenResult(GoogleIdTokenResult.Success("token-4"))
        pending.complete(user)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.signInWithGoogle(any()) }
        verify(exactly = 1) { navigator.back() }
    }

    @Test
    fun `nut quay lai va nut De sau - lui ve man truoc`() = runTest {
        viewModel().onBack()

        verify(exactly = 1) { navigator.back() }
    }
}
