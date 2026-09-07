package com.ledinhthi.ontaptld.core.exception

data class AppExceptionWrapper(
    val exception: AppException,
    val stackTrace: String? = null,
    val overrideMessage: String? = null,
) {
    val isConnectivityIssue: Boolean
        get() = exception is AppException.AiException &&
            exception.kind == AiErrorKind.NETWORK
}
