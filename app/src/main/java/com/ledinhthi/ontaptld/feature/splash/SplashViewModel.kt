package com.ledinhthi.ontaptld.feature.splash

import androidx.lifecycle.ViewModel
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.navigation.LoginRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Sprint 1: chưa có phiên đăng nhập lưu trữ (Sprint 2 mới có AuthUseCase/token thật) —
 * tạm coi như luôn chưa đăng nhập, vào thẳng Login ngay khi Compose gọi [enter].
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val navigator: AppNavigator,
) : ViewModel() {
    private var done = false

    fun enter() {
        if (done) return
        done = true
        navigator.replaceAll(LoginRoute)
    }
}
