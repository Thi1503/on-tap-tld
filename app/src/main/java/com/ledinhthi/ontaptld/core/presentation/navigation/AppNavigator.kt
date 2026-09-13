package com.ledinhthi.ontaptld.core.presentation.navigation

import kotlinx.coroutines.flow.Flow

sealed interface NavIntent {
    data class To(val route: Any, val singleTop: Boolean = true) : NavIntent
    data class ReplaceAll(val route: Any) : NavIntent // xoá hết back stack (vd sau login/splash)
    data object Back : NavIntent
}

enum class SnackBarType { SUCCESS, FAILURE, INFO }

data class SnackBarMessage(val text: String, val type: SnackBarType)

data class AppDialog(
    val message: String,
    val isError: Boolean = false,
    val onClose: (() -> Unit)? = null,
)

/** Interface gần như giữ nguyên chữ ký `AppNavigator` của base Flutter. */
interface AppNavigator {
    val intents: Flow<NavIntent>
    val dialogs: Flow<AppDialog>
    val snackBars: Flow<SnackBarMessage>

    fun to(route: Any)
    fun replaceAll(route: Any)
    fun back()

    fun showErrorDialog(message: String, onClose: (() -> Unit)? = null)
    fun showNotificationDialog(message: String, onClose: (() -> Unit)? = null)
    fun showSnackBar(message: String, type: SnackBarType = SnackBarType.FAILURE)
}
