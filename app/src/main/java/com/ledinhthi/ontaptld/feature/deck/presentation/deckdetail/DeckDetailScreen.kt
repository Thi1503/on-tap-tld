package com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail

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
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledinhthi.ontaptld.core.presentation.components.LoadingOverlay
import com.ledinhthi.ontaptld.core.presentation.components.ScreenStateHost
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(viewModel: DeckDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LoadingOverlay(isLoading = state.status.isLoadingOverlay) {
        Scaffold(
            topBar = { TopAppBar(title = { Text(state.deck?.name ?: "Deck") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = viewModel::onAddManualCard) { Text("+") }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                ScreenStateHost(
                    isLoading = state.status.isLoading,
                    items = state.cards,
                    error = state.status.exceptionWrapper?.let { "Không tải được thẻ." },
                    onRetry = {},
                    emptyText = "Deck này chưa có thẻ. Bấm + để thêm thẻ thủ công.",
                ) { cards ->
                    LazyColumn {
                        items(cards, key = { it.id }) { card ->
                            ListItem(
                                headlineContent = { Text(card.question) },
                                supportingContent = { Text(card.answer) },
                                trailingContent = {
                                    val badge = if (card.source == FlashcardSource.AI) "AI" else "Thủ công"
                                    Text(badge)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
