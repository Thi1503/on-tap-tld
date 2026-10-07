package com.ledinhthi.ontaptld.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.ObserveEffects
import com.ledinhthi.ontaptld.core.presentation.navigation.AppDialog
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.navigation.NavIntent
import com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail.DeckDetailScreen
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.DeckListScreen
import com.ledinhthi.ontaptld.feature.deck.presentation.manualcard.ManualCardScreen
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

            is NavIntent.ReplaceAll -> navController.navigate(intent.route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            }

            NavIntent.Back -> navController.popBackStack()
        }
    }
    ObserveEffects(navigator.snackBars) { snackbarHostState.showSnackbar(it.text) }
    ObserveEffects(navigator.dialogs) { dialog = it }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SplashRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<SplashRoute> { SplashScreen() }
            // Màn Login email/mật khẩu cũ đã gỡ khỏi luồng — đăng nhập Google (tuỳ chọn, từ
            // Cài đặt) sẽ thay vào ở bước 6 của docs/KE_HOACH_PHAT_TRIEN.md.
            composable<HomeRoute> { DeckListScreen() }
            composable<DeckDetailRoute> { DeckDetailScreen() }
            composable<ManualCardRoute> { ManualCardScreen() }
            composable<SettingsRoute> { SettingsScreen() }
            // composable<CaptureRoute> { CaptureScreen() }        // Sprint 1 tuần 2
            // composable<ReviewRoute>(                             // Sprint 1 tuần 3
            //     deepLinks = listOf(navDeepLink { uriPattern = "ontaptld://review" }),
            // ) { ReviewScreen() }
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
