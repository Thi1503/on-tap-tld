package com.ledinhthi.ontaptld.core.presentation.mvi

import com.ledinhthi.ontaptld.core.exception.AppExceptionWrapper

/** Gom 3 field trạng thái mà base Flutter để rời trên State. */
data class UiStatus(
    val isLoading: Boolean = false,
    val isLoadingOverlay: Boolean = false,
    val exceptionWrapper: AppExceptionWrapper? = null,
)

interface UiState {
    val status: UiStatus

    /** Concrete state là data class -> chỉ cần `copy(status = status)`. */
    fun withStatus(status: UiStatus): UiState
}

/** Sự kiện 1 lần (điều hướng nội bộ màn, focus field, toast cục bộ…). */
interface UiEffect
