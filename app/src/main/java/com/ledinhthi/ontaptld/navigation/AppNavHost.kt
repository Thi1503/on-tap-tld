package com.ledinhthi.ontaptld.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.ObserveEffects
import com.ledinhthi.ontaptld.core.presentation.navigation.AppDialog
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.navigation.NavIntent
import com.ledinhthi.ontaptld.core.presentation.theme.DarkAppExtendedColors
import com.ledinhthi.ontaptld.feature.capture.presentation.aicards.AiCardsScreen
import com.ledinhthi.ontaptld.feature.capture.presentation.capture.CaptureScreen
import com.ledinhthi.ontaptld.feature.capture.presentation.ocrreview.OcrReviewScreen
import com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail.DeckDetailScreen
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.DeckListScreen
import com.ledinhthi.ontaptld.feature.deck.presentation.manualcard.ManualCardScreen
import com.ledinhthi.ontaptld.feature.review.presentation.ReviewScreen
import com.ledinhthi.ontaptld.feature.review.presentation.sourceimage.SourceImageScreen
import com.ledinhthi.ontaptld.feature.settings.SettingsScreen
import com.ledinhthi.ontaptld.feature.splash.SplashScreen
import kotlinx.coroutines.withTimeoutOrNull

/** Thời gian một thông báo ngắn (snackbar) nằm trên màn. */
private const val SnackbarVisibleMillis = 3_000L

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
    // Thông báo ngắn tự tắt sau 3 giây. `showSnackbar` của Material chỉ có sẵn mức 4 giây và 10
    // giây, nên ở đây cho hiện "vô thời hạn" rồi tự cắt: hết giờ thì `withTimeoutOrNull` huỷ
    // lệnh hiện, thông báo biến mất theo. Người bật trợ năng (vd TalkBack) được hệ thống đề nghị
    // thời gian dài hơn để kịp đọc.
    val accessibilityManager = LocalAccessibilityManager.current
    ObserveEffects(navigator.snackBars) { message ->
        val visibleMillis = accessibilityManager
            ?.calculateRecommendedTimeoutMillis(SnackbarVisibleMillis, containsText = true)
            ?: SnackbarVisibleMillis
        withTimeoutOrNull(visibleMillis) {
            snackbarHostState.showSnackbar(message.text, duration = SnackbarDuration.Indefinite)
        }
    }
    ObserveEffects(navigator.dialogs) { dialog = it }

    // Scaffold tô màu cho hai dải nằm sau thanh trạng thái và thanh điều hướng của hệ thống.
    // Màn chụp ảnh và màn xem ảnh nguồn tối toàn bộ kể cả khi app đang ở giao diện sáng, nên
    // khi chúng đang hiện thì hai dải này cũng phải tối theo (đổi màu từ từ cho khớp hiệu ứng
    // chuyển màn).
    val currentEntry by navController.currentBackStackEntryAsState()
    val destination = currentEntry?.destination
    val isAlwaysDarkScreen =
        destination?.hasRoute<CaptureRoute>() == true || destination?.hasRoute<SourceImageRoute>() == true
    val systemBarBackdrop by animateColorAsState(
        targetValue = if (isAlwaysDarkScreen) {
            DarkAppExtendedColors.scaffoldBackground
        } else {
            MaterialTheme.colorScheme.background
        },
        label = "systemBarBackdrop",
    )

    Scaffold(containerColor = systemBarBackdrop) { innerPadding ->
        // Box xếp các phần tử con chồng lên nhau: thông báo nằm đè lên màn đang hiện.
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Báo cho các màn bên trong biết phần thanh hệ thống đã được chừa ở đây rồi. Nhờ
                // vậy màn nào dùng `imePadding()` (né bàn phím) chỉ cộng thêm đúng phần chênh,
                // không chừa thanh điều hướng lần thứ hai.
                .consumeWindowInsets(innerPadding),
        ) {
            NavHost(navController = navController, startDestination = SplashRoute) {
                composable<SplashRoute> { SplashScreen() }
                // Màn Login email/mật khẩu cũ đã gỡ khỏi luồng — đăng nhập Google (tuỳ chọn, từ
                // Cài đặt) sẽ thay vào ở bước 6 của docs/KE_HOACH_PHAT_TRIEN.md.
                composable<HomeRoute> { DeckListScreen() }
                composable<DeckDetailRoute> { DeckDetailScreen() }
                composable<ManualCardRoute> { ManualCardScreen() }
                composable<SettingsRoute> { SettingsScreen() }
                composable<ReviewRoute> { ReviewScreen() }
                composable<SourceImageRoute> { SourceImageScreen() }
                composable<CaptureRoute> { CaptureScreen() }
                composable<OcrReviewRoute> { OcrReviewScreen() }
                composable<AiCardsRoute> { AiCardsScreen() }
            }
            // Thông báo hiện ở ĐỈNH màn (Thi chốt 8/10/2026), không đặt vào khe `snackbarHost`
            // của Scaffold vì khe đó luôn nằm sát đáy — đè lên hàng nút và lọt sau bàn phím.
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
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
