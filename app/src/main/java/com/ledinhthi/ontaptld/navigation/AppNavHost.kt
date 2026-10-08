package com.ledinhthi.ontaptld.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.ObserveEffects
import com.ledinhthi.ontaptld.core.presentation.navigation.AppDialog
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.navigation.NavIntent
import com.ledinhthi.ontaptld.core.presentation.theme.DarkAppExtendedColors
import com.ledinhthi.ontaptld.feature.capture.presentation.capture.CaptureScreen
import com.ledinhthi.ontaptld.feature.capture.presentation.ocrreview.OcrReviewPlaceholderScreen
import com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail.DeckDetailScreen
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.DeckListScreen
import com.ledinhthi.ontaptld.feature.deck.presentation.manualcard.ManualCardScreen
import com.ledinhthi.ontaptld.feature.review.presentation.ReviewScreen
import com.ledinhthi.ontaptld.feature.settings.SettingsScreen
import com.ledinhthi.ontaptld.feature.splash.SplashScreen

@Composable
fun AppNavHost(navigator: AppNavigator) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<AppDialog?>(null) }

    // ViewModel phát lệnh -> đây là nơi DUY NHẤT chạm NavController.
    ObserveEffects(navigator.intents) { intent ->
        when (intent) {
            is NavIntent.To -> navController.navigate(intent.route) {
                launchSingleTop = intent.singleTop
            }

            // Xoá HẾT các màn đang xếp chồng rồi mới mở màn mới. Dùng id của cả đồ thị (thay vì
            // màn bắt đầu) vì Splash — màn bắt đầu — đã bị gỡ khỏi chồng ngay sau khi vào Home.
            is NavIntent.ReplaceAll -> navController.navigate(intent.route) {
                popUpTo(navController.graph.id) { inclusive = true }
            }

            NavIntent.Back -> navController.popBackStack()
        }
    }
    ObserveEffects(navigator.snackBars) { snackbarHostState.showSnackbar(it.text) }
    ObserveEffects(navigator.dialogs) { dialog = it }

    // Scaffold tô màu cho hai dải nằm sau thanh trạng thái và thanh điều hướng của hệ thống.
    // Màn chụp ảnh tối toàn bộ kể cả khi app đang ở giao diện sáng, nên khi nó đang hiện thì
    // hai dải này cũng phải tối theo (đổi màu từ từ cho khớp hiệu ứng chuyển màn).
    val currentEntry by navController.currentBackStackEntryAsState()
    val isCameraScreen = currentEntry?.destination?.hasRoute<CaptureRoute>() == true
    val systemBarBackdrop by animateColorAsState(
        targetValue = if (isCameraScreen) {
            DarkAppExtendedColors.scaffoldBackground
        } else {
            MaterialTheme.colorScheme.background
        },
        label = "systemBarBackdrop",
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = systemBarBackdrop,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SplashRoute,
            modifier = Modifier
                .padding(innerPadding)
                // Báo cho các màn bên trong biết phần thanh hệ thống đã được chừa ở đây rồi. Nhờ
                // vậy màn nào dùng `imePadding()` (né bàn phím) chỉ cộng thêm đúng phần chênh,
                // không chừa thanh điều hướng lần thứ hai.
                .consumeWindowInsets(innerPadding),
        ) {
            composable<SplashRoute> { SplashScreen() }
            // Màn Login email/mật khẩu cũ đã gỡ khỏi luồng — đăng nhập Google (tuỳ chọn, từ
            // Cài đặt) sẽ thay vào ở bước 6 của docs/KE_HOACH_PHAT_TRIEN.md.
            composable<HomeRoute> { DeckListScreen() }
            composable<DeckDetailRoute> { DeckDetailScreen() }
            composable<ManualCardRoute> { ManualCardScreen() }
            composable<SettingsRoute> { SettingsScreen() }
            composable<ReviewRoute> { ReviewScreen() }
            composable<CaptureRoute> { CaptureScreen() }
            // TẠM: màn Kiểm tra văn bản (bước 2/3) chưa làm — hiện ảnh đã cắt để kiểm tra khung cắt.
            composable<OcrReviewRoute> { entry ->
                OcrReviewPlaceholderScreen(
                    imagePath = entry.toRoute<OcrReviewRoute>().imagePath,
                    onBack = navigator::back,
                )
            }
        }
    }

    dialog?.let { d ->
        AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    d.onClose?.invoke()
                }) { Text(stringResource(R.string.common_close)) }
            },
            text = { Text(d.message) },
        )
    }
}
