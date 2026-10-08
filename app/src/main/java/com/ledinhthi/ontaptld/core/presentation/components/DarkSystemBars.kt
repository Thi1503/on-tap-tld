package com.ledinhthi.ontaptld.core.presentation.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Đặt trong một màn LUÔN TỐI (màn chụp ảnh, màn xem ảnh nguồn): khi màn đó đang hiện, icon trên
 * thanh trạng thái / thanh điều hướng đổi sang màu sáng cho hợp nền tối; rời màn thì trả về kiểu
 * mặc định (theo giao diện sáng / tối của máy).
 *
 * Màu nền của hai dải đó do `AppNavHost` tô — xem `systemBarBackdrop` ở đó.
 */
@Composable
fun DarkSystemBars() {
    val activity = LocalContext.current.findActivity() as? ComponentActivity ?: return
    // DisposableEffect: khối chính chạy lúc màn xuất hiện, `onDispose` chạy lúc màn biến mất.
    DisposableEffect(activity) {
        val dark = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        activity.enableEdgeToEdge(statusBarStyle = dark, navigationBarStyle = dark)
        onDispose { activity.enableEdgeToEdge() }
    }
}

/**
 * Lần ngược từ `Context` của Compose về `Activity` đang chứa nó. Context thường được bọc nhiều
 * lớp (ContextWrapper), nên phải bóc dần từng lớp cho tới khi gặp Activity.
 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
