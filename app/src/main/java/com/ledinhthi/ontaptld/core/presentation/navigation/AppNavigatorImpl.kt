package com.ledinhthi.ontaptld.core.presentation.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNavigatorImpl @Inject constructor() : AppNavigator {

    private fun <T> event() = MutableSharedFlow<T>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val _intents = event<NavIntent>()
    private val _dialogs = event<AppDialog>()
    private val _snackBars = event<SnackBarMessage>()

    // Khởi tạo 1 lần (không dùng `get()`) -> instance ổn định để `LaunchedEffect(flow)` không restart.
    override val intents = _intents.asSharedFlow()
    override val dialogs = _dialogs.asSharedFlow()
    override val snackBars = _snackBars.asSharedFlow()

    override fun to(route: Any) {
        _intents.tryEmit(NavIntent.To(route))
    }

    override fun replaceAll(route: Any) {
        _intents.tryEmit(NavIntent.ReplaceAll(route))
    }

    override fun back() {
        _intents.tryEmit(NavIntent.Back)
    }

    override fun showErrorDialog(message: String, onClose: (() -> Unit)?) {
        _dialogs.tryEmit(AppDialog(message, isError = true, onClose = onClose))
    }

    override fun showNotificationDialog(message: String, onClose: (() -> Unit)?) {
        _dialogs.tryEmit(AppDialog(message, isError = false, onClose = onClose))
    }

    override fun showSnackBar(message: String, type: SnackBarType) {
        _snackBars.tryEmit(SnackBarMessage(message, type))
    }
}
