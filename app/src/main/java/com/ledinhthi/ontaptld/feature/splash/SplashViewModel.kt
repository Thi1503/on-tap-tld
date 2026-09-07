package com.ledinhthi.ontaptld.feature.splash

import androidx.lifecycle.ViewModel
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.navigation.HomeRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Sprint 1: không kiểm tra auth — vào thẳng Home ngay khi Compose gọi [enter]. */
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
