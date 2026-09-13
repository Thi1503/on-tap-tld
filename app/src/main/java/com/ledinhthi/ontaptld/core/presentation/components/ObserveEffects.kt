package com.ledinhthi.ontaptld.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

@Composable
fun <T> ObserveEffects(flow: Flow<T>, onEffect: suspend (T) -> Unit) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(flow, owner) {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { onEffect(it) }
        }
    }
}
