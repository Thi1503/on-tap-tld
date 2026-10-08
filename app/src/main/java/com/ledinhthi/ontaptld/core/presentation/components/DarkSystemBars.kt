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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.ledinhthi.ontaptld.core.presentation.theme.LocalIsDarkTheme

/**
 * Đặt MỘT lần ở gốc app (trong `OnTapTldTheme`): cho icon trên thanh trạng thái / thanh điều
 * hướng đổi màu theo giao diện của APP. Mặc định Android chọn màu icon theo chế độ sáng / tối
 * của MÁY, nên khi người dùng chọn "Tối" trong Cài đặt mà máy đang sáng, icon đen sẽ chìm vào
 * nền tối nếu thiếu hàm này.
 */
@Composable
fun AppSystemBars() {
    val activity = LocalContext.current.findActivity() as? ComponentActivity ?: return
    val isDark = LocalIsDarkTheme.current
    // LaunchedEffect(khoá): khối lệnh chạy lại mỗi khi một "khoá" đổi giá trị — ở đây là mỗi lần
    // app chuyển giữa sáng và tối.
    LaunchedEffect(activity, isDark) { activity.applySystemBars(isDark) }
}

/**
 * Đặt trong một màn LUÔN TỐI (màn chụp ảnh, màn xem ảnh nguồn): khi màn đó đang hiện, icon trên
 * thanh trạng thái / thanh điều hướng đổi sang màu sáng cho hợp nền tối; rời màn thì trả về kiểu
 * của giao diện app đang dùng.
 *
 * Màu nền của hai dải đó do `AppNavHost` tô — xem `systemBarBackdrop` ở đó.
 */
@Composable
fun DarkSystemBars() {
    val activity = LocalContext.current.findActivity() as? ComponentActivity ?: return
    // rememberUpdatedState: để `onDispose` (chạy muộn, lúc rời màn) đọc được giá trị MỚI NHẤT.
    val isDark by rememberUpdatedState(LocalIsDarkTheme.current)
    // DisposableEffect: khối chính chạy lúc màn xuất hiện, `onDispose` chạy lúc màn biến mất.
    DisposableEffect(activity) {
        activity.applySystemBars(isDark = true)
        onDispose { activity.applySystemBars(isDark) }
    }
}

/**
 * `SystemBarStyle.auto` cần hai màu phủ mờ cho thanh điều hướng kiểu 3 nút (máy dùng cử chỉ
 * vuốt thì thanh trong suốt hẳn). Hai màu dưới đây đúng bằng mặc định của `enableEdgeToEdge()`.
 */
private val NavigationLightScrim = AndroidColor.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val NavigationDarkScrim = AndroidColor.argb(0x80, 0x1b, 0x1b, 0x1b)

private fun ComponentActivity.applySystemBars(isDark: Boolean) {
    // Tham số cuối là câu trả lời cho "đang tối à?" — thay cho việc để Android tự xem chế độ của máy.
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { isDark },
        navigationBarStyle = SystemBarStyle.auto(NavigationLightScrim, NavigationDarkScrim) { isDark },
    )
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
