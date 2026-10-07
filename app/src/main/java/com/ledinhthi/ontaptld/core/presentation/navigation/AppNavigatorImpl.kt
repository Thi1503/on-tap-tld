package com.ledinhthi.ontaptld.core.presentation.navigation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dùng [Channel] (không phải `SharedFlow`) cho các lệnh một-lần (navigate/dialog/snackbar):
 * Channel giữ lại sự kiện cho tới khi có consumer nhận, dù consumer (AppNavHost) gắn
 * `collect` trễ hơn thời điểm `tryEmit` (vd Compose bị giật khung hình lúc khởi động app).
 * `SharedFlow(replay = 0)` cũ có thể làm mất sự kiện trong đúng tình huống này.
 */
@Singleton
class AppNavigatorImpl @Inject constructor() : AppNavigator {

    private fun <T> event() = Channel<T>(Channel.BUFFERED)

    private val _intents = event<NavIntent>()
    private val _dialogs = event<AppDialog>()
    private val _snackBars = event<SnackBarMessage>()

    // Khởi tạo 1 lần (không dùng `get()`) -> instance ổn định để `LaunchedEffect(flow)` không restart.
    override val intents = _intents.receiveAsFlow()
    override val dialogs = _dialogs.receiveAsFlow()
    override val snackBars = _snackBars.receiveAsFlow()

    override fun to(route: Any) {
        _intents.trySend(NavIntent.To(route))
    }

    override fun replaceAll(route: Any) {
        _intents.trySend(NavIntent.ReplaceAll(route))
    }

    override fun back() {
        _intents.trySend(NavIntent.Back)
    }

    override fun showErrorDialog(message: String, onClose: (() -> Unit)?) {
        _dialogs.trySend(AppDialog(message, isError = true, onClose = onClose))
    }

    override fun showNotificationDialog(message: String, onClose: (() -> Unit)?) {
        _dialogs.trySend(AppDialog(message, isError = false, onClose = onClose))
    }

    override fun showSnackBar(message: String, type: SnackBarType) {
        _snackBars.trySend(SnackBarMessage(message, type))
    }
}
