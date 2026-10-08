package com.ledinhthi.ontaptld.feature.deck.presentation.decklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode
import com.ledinhthi.ontaptld.core.presentation.components.ErrorState
import com.ledinhthi.ontaptld.core.presentation.components.LoadingOverlay
import com.ledinhthi.ontaptld.core.presentation.components.ObserveEffects
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.OnTapTldTheme
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.model.DeckSummary
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components.CreateDeckSheet
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components.DeckSummaryRow
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components.DecksSectionHeader
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components.DueHeroCard
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components.HomeEmptyContent
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components.HomeHeader
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components.HomeSkeleton
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components.QuickActions

/*
 * CÁCH ĐỌC FILE NÀY (khuôn chung cho mọi màn trong app):
 *
 *   DeckListScreen   — "có trạng thái": cầm ViewModel, đọc state, nối sự kiện. Rất mỏng.
 *        │
 *   DeckListContent  — "không trạng thái": chỉ nhận `state` + các hàm callback rồi vẽ.
 *        │             Vì không dính ViewModel nên dựng được trong @Preview (cuối file).
 *   DeckListBody     — phần danh sách khi đã có dữ liệu.
 *
 * Các khối con (HomeHeader, DueHeroCard…) nằm ở thư mục `components/` cùng cấp.
 */

/** Home của app (route `HomeRoute`): tổng quan thẻ cần ôn + lối tạo thẻ + danh sách bộ thẻ. */
@Composable
fun DeckListScreen(viewModel: DeckListViewModel = hiltViewModel()) {
    // `by` + collectAsState…: mỗi khi ViewModel đổi state, Compose tự vẽ lại những chỗ đọc `state`.
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // "Bảng tạo bộ thẻ đang mở hay đóng" là trạng thái THUẦN GIAO DIỆN nên giữ ngay tại đây,
    // không đưa vào ViewModel. `rememberSaveable` (khác `remember`) còn giữ được giá trị khi
    // xoay màn hình.
    var showCreateDeck by rememberSaveable { mutableStateOf(false) }

    // Effect = sự kiện xảy ra MỘT LẦN do ViewModel phát (khác state là thứ tồn tại lâu dài).
    ObserveEffects(viewModel.effect) { effect ->
        when (effect) {
            DeckListEffect.OpenCreateDeck -> showCreateDeck = true
        }
    }

    LoadingOverlay(isLoading = state.status.isLoadingOverlay) {
        DeckListContent(
            state = state,
            // `viewModel::onSettingsClick` = truyền chính hàm đó làm callback,
            // viết gọn của `{ viewModel.onSettingsClick() }`.
            onSettingsClick = viewModel::onSettingsClick,
            onReviewClick = viewModel::onReviewClick,
            onCaptureClick = viewModel::onCaptureClick,
            onManualCardClick = viewModel::onManualCardClick,
            onNewDeckClick = viewModel::onNewDeckClick,
            onDeckClick = viewModel::onDeckClick,
            onRetry = viewModel::onRetry,
        )
    }

    if (showCreateDeck) {
        CreateDeckSheet(
            onDismiss = {
                showCreateDeck = false
                viewModel.onCreateDeckDismissed()
            },
            onConfirm = { name, colorHex ->
                showCreateDeck = false
                viewModel.onCreateDeck(name, colorHex)
            },
        )
    }
}

@Composable
private fun DeckListContent(
    state: DeckListState,
    onSettingsClick: () -> Unit,
    onReviewClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onManualCardClick: () -> Unit,
    onNewDeckClick: () -> Unit,
    onDeckClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(appColors().scaffoldBackground),
    ) {
        HomeHeader(onSettingsClick = onSettingsClick)
        // Đầu trang luôn hiện; phần thân là 1 trong 4 trạng thái: lỗi / đang tải / rỗng / có dữ liệu.
        when {
            state.loadFailed -> ErrorState(
                title = stringResource(R.string.home_error_title),
                message = stringResource(R.string.home_error_message),
                onRetry = onRetry,
            )

            !state.isLoaded -> HomeSkeleton()

            state.decks.isEmpty() -> HomeEmptyContent(
                onCaptureClick = onCaptureClick,
                onManualCardClick = onManualCardClick,
            )

            else -> DeckListBody(
                state = state,
                onReviewClick = onReviewClick,
                onCaptureClick = onCaptureClick,
                onManualCardClick = onManualCardClick,
                onNewDeckClick = onNewDeckClick,
                onDeckClick = onDeckClick,
            )
        }
    }
}

@Composable
private fun DeckListBody(
    state: DeckListState,
    onReviewClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onManualCardClick: () -> Unit,
    onNewDeckClick: () -> Unit,
    onDeckClick: (String) -> Unit,
) {
    // LazyColumn = danh sách cuộn chỉ dựng những dòng đang nằm trong màn hình (như RecyclerView /
    // ListView.builder), nên bộ thẻ nhiều đến mấy cũng không chậm.
    //
    // Khoảng cách giữa các khối không đều nhau (16 / 16 / 8 / 10 theo thiết kế) nên đặt bằng
    // padding-top riêng cho từng item thay vì `Arrangement.spacedBy`.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = AppDimens.defaultPadding,
            end = AppDimens.defaultPadding,
            top = AppDimens.paddingSmallest,
            bottom = AppDimens.padding24,
        ),
    ) {
        item(key = "hero", contentType = "hero") {
            DueHeroCard(
                dueCount = state.dueCount,
                dueDecks = state.dueDecks,
                onReviewClick = onReviewClick,
            )
        }
        item(key = "actions", contentType = "actions") {
            QuickActions(
                onCaptureClick = onCaptureClick,
                onManualCardClick = onManualCardClick,
                modifier = Modifier.padding(top = AppDimens.defaultPadding),
            )
        }
        item(key = "decksHeader", contentType = "header") {
            DecksSectionHeader(
                deckCount = state.decks.size,
                cardCount = state.cardCount,
                onNewDeckClick = onNewDeckClick,
                modifier = Modifier.padding(top = AppDimens.defaultPadding),
            )
        }
        itemsIndexed(
            items = state.decks,
            // `key` giúp Compose nhận ra "dòng này vẫn là bộ thẻ đó" khi danh sách thêm/xoá/đổi
            // thứ tự -> không vẽ lại thừa và giữ đúng trạng thái của từng dòng.
            key = { _, summary -> summary.deck.id },
            contentType = { _, _ -> "deck" },
        ) { index, summary ->
            DeckSummaryRow(
                summary = summary,
                onClick = { onDeckClick(summary.deck.id) },
                modifier = Modifier.padding(
                    top = if (index == 0) AppDimens.paddingVerySmall else AppDimens.padding10,
                ),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------
// Preview — xem thử giao diện ngay trong Android Studio (tab Split/Design), không cần chạy
// app. Mỗi hàm @Preview bên dưới dựng DeckListContent với một state giả cho từng tình huống.
// ---------------------------------------------------------------------------------------

private fun previewDeck(id: String, name: String, colorHex: String, cards: Int, due: Int) =
    DeckSummary(Deck(id, name, colorHex, createdAt = 0, updatedAt = 0), cardCount = cards, dueCount = due)

private val previewDecks = listOf(
    previewDeck("1", "IELTS Vocabulary", "#4F46E5", 128, 12),
    previewDeck("2", "Sinh học 12 – Di truyền", "#0D9488", 74, 8),
    previewDeck("3", "Lịch sử Việt Nam", "#DB2777", 62, 4),
    previewDeck("4", "Kotlin Coroutines", "#0284C7", 52, 0),
)

@Composable
private fun PreviewHost(state: DeckListState, themeMode: ThemeMode = ThemeMode.LIGHT) {
    OnTapTldTheme(themeMode = themeMode) {
        DeckListContent(
            state = state,
            onSettingsClick = {},
            onReviewClick = {},
            onCaptureClick = {},
            onManualCardClick = {},
            onNewDeckClick = {},
            onDeckClick = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Home", widthDp = 390, heightDp = 844)
@Composable
private fun HomePreview() = PreviewHost(DeckListState(decks = previewDecks, isLoaded = true))

@Preview(name = "Home — Dark", widthDp = 390, heightDp = 844)
@Composable
private fun HomeDarkPreview() =
    PreviewHost(DeckListState(decks = previewDecks, isLoaded = true), ThemeMode.DARK)

@Preview(name = "Home — chưa có dữ liệu", widthDp = 390, heightDp = 844)
@Composable
private fun HomeEmptyPreview() = PreviewHost(DeckListState(isLoaded = true))

@Preview(name = "Home — đang tải", widthDp = 390, heightDp = 844)
@Composable
private fun HomeLoadingPreview() = PreviewHost(DeckListState())

@Preview(name = "Home — lỗi", widthDp = 390, heightDp = 844)
@Composable
private fun HomeErrorPreview() = PreviewHost(DeckListState(loadFailed = true))
