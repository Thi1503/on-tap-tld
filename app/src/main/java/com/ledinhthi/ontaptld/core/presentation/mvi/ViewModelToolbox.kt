package com.ledinhthi.ontaptld.core.presentation.mvi

import com.ledinhthi.ontaptld.core.exception.GlobalExceptionHandler
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import javax.inject.Inject

/** Gom phụ thuộc chung của mọi ViewModel để chữ ký constructor subclass gọn. */
class ViewModelToolbox @Inject constructor(
    val navigator: AppNavigator,
    val exceptionHandler: GlobalExceptionHandler,
)
