package com.ledinhthi.ontaptld.feature.splash

import com.ledinhthi.ontaptld.core.MainDispatcherRule
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.navigation.HomeRoute
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val navigator = mockk<AppNavigator>(relaxed = true)

    @Test
    fun `giu man chao mot thoang roi moi vao Home`() = runTest {
        val vm = SplashViewModel(navigator)

        vm.enter()
        advanceTimeBy(SplashViewModel.MIN_VISIBLE_MILLIS - 1)
        verify(exactly = 0) { navigator.replaceAll(any()) }

        advanceUntilIdle()
        verify(exactly = 1) { navigator.replaceAll(HomeRoute) }
    }

    @Test
    fun `goi enter nhieu lan - chi dieu huong mot lan`() = runTest {
        val vm = SplashViewModel(navigator)

        vm.enter()
        vm.enter() // vd màn bị dựng lại khi xoay máy
        advanceUntilIdle()

        verify(exactly = 1) { navigator.replaceAll(HomeRoute) }
    }
}
