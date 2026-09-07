package com.ledinhthi.ontaptld.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Mọi màn danh sách BẮT BUỘC đủ 3 trạng thái Empty / Loading / Error (docs_tld Mục 9). */
@Composable
fun <T> ScreenStateHost(
    isLoading: Boolean,
    items: List<T>,
    error: String?,
    onRetry: () -> Unit,
    emptyText: String,
    content: @Composable (List<T>) -> Unit,
) = when {
    isLoading && items.isEmpty() ->
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

    error != null && items.isEmpty() -> Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(error)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Thử lại") }
    }

    items.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(emptyText) }

    else -> content(items)
}
