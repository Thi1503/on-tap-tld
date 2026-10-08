package com.ledinhthi.ontaptld.feature.splash

import androidx.lifecycle.ViewModel
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.navigation.HomeRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * App dùng được ngay không cần tài khoản (docs_tld Mục 8.1): Splash đọc xong là vào thẳng
 * Home. Đăng nhập Google là tuỳ chọn, vào từ Cài đặt — không chặn ở đây.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val navigator: AppNavigator,
) : ViewModel() {
    private var done = false

    fun enter() {
        if (done) return
        done = true
        navigator.replaceAll(HomeRoute)
    }
}
