package com.ledinhthi.ontaptld

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledinhthi.ontaptld.core.data.local.prefs.AppPreferences
import com.ledinhthi.ontaptld.core.presentation.components.AppSystemBars
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.theme.OnTapTldTheme
import com.ledinhthi.ontaptld.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

// Kế thừa AppCompatActivity (thay cho ComponentActivity) để dùng được tính năng đổi ngôn ngữ
// riêng của app trên mọi đời Android — xem AppCompatLanguageManager.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var navigator: AppNavigator

    @Inject
    lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Đọc giao diện đã lưu NGAY, trước khi vẽ khung hình đầu tiên. `runBlocking` chặn luồng
        // chính trong lúc đọc — bình thường nên tránh, nhưng ở đây chỉ là một file cài đặt nhỏ
        // (vài mili giây), và đổi lại app không bị loé giao diện sáng rồi mới chuyển sang tối.
        val savedTheme = runBlocking { prefs.themeMode.first() }
        setContent {
            val theme by prefs.themeMode.collectAsStateWithLifecycle(initialValue = savedTheme)
            OnTapTldTheme(themeMode = theme) {
                AppSystemBars()
                AppNavHost(navigator = navigator)
            }
        }
    }
}
