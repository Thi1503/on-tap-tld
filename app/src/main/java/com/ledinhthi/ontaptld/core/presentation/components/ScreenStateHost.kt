package com.ledinhthi.ontaptld.core.presentation.components

import androidx.compose.runtime.Composable

/**
 * Mọi màn danh sách BẮT BUỘC đủ 3 trạng thái Empty / Loading / Error (docs_tld Mục 9).
 *
 * Mặc định: Loading là vòng xoay, Empty là [EmptyState] chỉ có [emptyText], Error là
 * [ErrorState]. Màn nào có thiết kế riêng thì truyền slot [loading] (skeleton) / [empty]
 * (minh hoạ + nút hành động) để thay phần mặc định.
 */
@Composable
fun <T> ScreenStateHost(
    isLoading: Boolean,
    items: List<T>,
    error: String?,
    onRetry: () -> Unit,
    emptyText: String,
    errorMessage: String? = null,
    loading: @Composable () -> Unit = { LoadingState() },
    empty: @Composable () -> Unit = { EmptyState(title = emptyText) },
    content: @Composable (List<T>) -> Unit,
) = when {
    isLoading && items.isEmpty() -> loading()

    error != null && items.isEmpty() ->
        ErrorState(title = error, message = errorMessage, onRetry = onRetry)

    items.isEmpty() -> empty()

    else -> content(items)
}
