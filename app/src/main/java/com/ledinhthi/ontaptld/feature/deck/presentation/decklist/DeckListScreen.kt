package com.ledinhthi.ontaptld.feature.deck.presentation.decklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledinhthi.ontaptld.core.presentation.components.LoadingOverlay
import com.ledinhthi.ontaptld.core.presentation.components.ScreenStateHost
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components.CreateDeckDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckListScreen(viewModel: DeckListViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    LoadingOverlay(isLoading = state.status.isLoadingOverlay) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Deck của tôi") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = { showCreate = true }) { Text("+") }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                ScreenStateHost(
                    isLoading = state.status.isLoading,
                    items = state.decks,
                    error = state.status.exceptionWrapper?.let { "Không tải được danh sách deck." },
                    onRetry = { /* Flow tự re-emit; nút retry chỉ để UX */ },
                    emptyText = "Chưa có deck nào. Bấm + để tạo deck đầu tiên.",
                ) { decks ->
                    LazyColumn {
                        items(decks, key = { it.id }) { deck ->
                            ListItem(
                                headlineContent = { Text(deck.name) },
                                modifier = Modifier.clickable { viewModel.onDeckClick(deck.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateDeckDialog(
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                showCreate = false
                viewModel.onCreateDeck(name, colorHex = "#4F7CFF")
            },
        )
    }
}
