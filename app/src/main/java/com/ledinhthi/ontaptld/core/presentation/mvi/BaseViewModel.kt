package com.ledinhthi.ontaptld.core.presentation.mvi

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.AppExceptionWrapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class BaseViewModel<S : UiState>(
    initialState: S,
    protected val toolbox: ViewModelToolbox,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _effect = Channel<UiEffect>(Channel.BUFFERED)
    val effect: Flow<UiEffect> = _effect.receiveAsFlow()

    protected val navigator get() = toolbox.navigator
    protected val currentState: S get() = _uiState.value

    @VisibleForTesting
    var isTestMode = false

    /** Cập nhật phần nghiệp vụ của state. */
    protected fun setState(reducer: S.() -> S) = _uiState.update(reducer)

    protected fun sendEffect(effect: UiEffect) {
        _effect.trySend(effect)
    }

    fun onErrorConsumed() = updateStatus { it.copy(exceptionWrapper = null) }

    @Suppress("UNCHECKED_CAST")
    private fun updateStatus(transform: (UiStatus) -> UiStatus) {
        _uiState.update { it.withStatus(transform(it.status)) as S }
    }

    /** Tương đương `guard()` / `buildState()` của base Flutter. */
    protected fun launchGuarded(
        showLoading: Boolean = false,
        showLoadingOverlay: Boolean = false,
        handleError: Boolean = true,
        overrideErrorMessage: String? = null,
        onError: (suspend (AppException) -> AppException?)? = null,
        onFinally: (suspend () -> Unit)? = null,
        block: suspend () -> Unit,
    ): Job = viewModelScope.launch {
        if (showLoadingOverlay) updateStatus { it.copy(isLoadingOverlay = true) }
        if (showLoading) updateStatus { it.copy(isLoading = true) }
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (!isTestMode) Timber.e(e)
            val appException = e as? AppException ?: AppException.UncaughtException(e)

            val unhandled = onError?.invoke(appException)
            if (onError != null && unhandled == null) return@launch // ViewModel đã tự xử lý

            if (handleError) {
                val wrapper = AppExceptionWrapper(
                    exception = unhandled ?: appException,
                    stackTrace = e.stackTraceToString(),
                    overrideMessage = overrideErrorMessage,
                )
                updateStatus { it.copy(exceptionWrapper = wrapper) }
                toolbox.exceptionHandler.handle(wrapper)
            }
        } finally {
            if (showLoading) updateStatus { it.copy(isLoading = false) }
            if (showLoadingOverlay) updateStatus { it.copy(isLoadingOverlay = false) }
            onFinally?.invoke()
        }
    }
}
