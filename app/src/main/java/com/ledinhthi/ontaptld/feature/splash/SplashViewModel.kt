package com.ledinhthi.ontaptld.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.navigation.HomeRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App dùng được ngay không cần tài khoản (docs_tld Mục 8.1): Splash hiện xong là vào thẳng
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
        viewModelScope.launch {
            // Splash không phải chờ tải gì. Khoảng dừng ngắn này chỉ để màn chào kịp nhìn thấy,
            // thay vì loé lên một khung hình rồi biến mất trông như lỗi hiển thị.
            delay(MIN_VISIBLE_MILLIS)
            navigator.replaceAll(HomeRoute)
        }
    }

    companion object {
        const val MIN_VISIBLE_MILLIS = 800L
    }
}
