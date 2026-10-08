package com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode
import com.ledinhthi.ontaptld.core.presentation.components.AppIconButton
import com.ledinhthi.ontaptld.core.presentation.components.AppTopBar
import com.ledinhthi.ontaptld.core.presentation.components.BadgeTone
import com.ledinhthi.ontaptld.core.presentation.components.ConfirmDialog
import com.ledinhthi.ontaptld.core.presentation.components.EmptyState
import com.ledinhthi.ontaptld.core.presentation.components.ErrorState
import com.ledinhthi.ontaptld.core.presentation.components.LoadingOverlay
import com.ledinhthi.ontaptld.core.presentation.components.PrimaryButton
import com.ledinhthi.ontaptld.core.presentation.components.SecondaryButton
import com.ledinhthi.ontaptld.core.presentation.components.StateMessage
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.OnTapTldTheme
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.deck.presentation.components.deckTint
import com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail.components.CardFilterChips
import com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail.components.DeckDetailSkeleton
import com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail.components.DeckStatsHeader
import com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail.components.FlashcardRow

/*
 * Cùng khuôn với DeckListScreen (xem ghi chú đầu file đó):
 *   DeckDetailScreen  — cầm ViewModel + các hộp thoại xác nhận.
 *   DeckDetailContent — không trạng thái, chọn 1 trong 5 kiểu thân màn.
 *   DeckDetailBody    — thân màn khi bộ thẻ đã có thẻ.
 */

/** Màn Chi tiết bộ thẻ (route `DeckDetailRoute`): số liệu, danh sách thẻ, lối thêm thẻ. */
@Composable
fun DeckDetailScreen(viewModel: DeckDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // "Đang hỏi xoá thẻ nào / có đang hỏi xoá bộ thẻ không" là trạng thái thuần giao diện.
    var cardToDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var askDeleteDeck by rememberSaveable { mutableStateOf(false) }

    LoadingOverlay(isLoading = state.status.isLoadingOverlay) {
        DeckDetailContent(
            state = state,
            onBack = viewModel::onBack,
            onRetry = viewModel::onRetry,
            onDeleteDeckClick = { askDeleteDeck = true },
            onReviewClick = viewModel::onReviewClick,
            onFilterChange = viewModel::onFilterChange,
            onCaptureClick = viewModel::onCaptureClick,
            onAddCardClick = viewModel::onAddCardClick,
            onEditCard = viewModel::onEditCard,
            onDeleteCardClick = { cardToDelete = it },
        )
    }

    // `?.let { … }`: chỉ chạy khối lệnh khi giá trị khác null — ở đây là "đang hỏi xoá một thẻ".
    cardToDelete?.let { cardId ->
        ConfirmDialog(
            title = stringResource(R.string.deck_detail_delete_card_title),
            message = stringResource(R.string.deck_detail_delete_card_message),
            confirmText = stringResource(R.string.common_delete),
            destructive = true,
            onConfirm = {
                cardToDelete = null
                viewModel.onDeleteCard(cardId)
            },
            onDismiss = { cardToDelete = null },
        )
    }

    if (askDeleteDeck) {
        ConfirmDialog(
            title = stringResource(R.string.deck_detail_delete_deck_title, state.deck?.name.orEmpty()),
            message = stringResource(R.string.deck_detail_delete_deck_message),
            confirmText = stringResource(R.string.common_delete),
            destructive = true,
            onConfirm = {
                askDeleteDeck = false
                viewModel.onDeleteDeck()
            },
            onDismiss = { askDeleteDeck = false },
        )
    }
}

@Composable
private fun DeckDetailContent(
    state: DeckDetailState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDeleteDeckClick: () -> Unit,
    onReviewClick: () -> Unit,
    onFilterChange: (CardFilter) -> Unit,
    onCaptureClick: () -> Unit,
    onAddCardClick: () -> Unit,
    onEditCard: (String) -> Unit,
    onDeleteCardClick: (String) -> Unit,
) {
    val colors = appColors()
    val deck = state.deck
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.scaffoldBackground),
    ) {
        AppTopBar(
            title = deck?.name.orEmpty(),
            onNavigationClick = onBack,
            // Đường kẻ dưới top bar do từng kiểu thân màn tự vẽ: khi có thẻ, dải số liệu nối liền
            // với top bar nên đường kẻ phải nằm dưới dải số liệu chứ không phải ở đây.
            showDivider = false,
            titleLeading = deck?.let {
                { Box(Modifier.size(12.dp).background(deckTint(it.colorHex).base, CircleShape)) }
            },
            actions = { if (deck != null) DeckOptionsMenu(onDeleteDeckClick) },
        )
        when {
            state.loadFailed -> BelowTopBar {
                ErrorState(
                    title = stringResource(R.string.deck_detail_error_title),
                    message = stringResource(R.string.home_error_message),
                    onRetry = onRetry,
                )
            }

            !state.isLoaded -> DeckDetailSkeleton()

            // Đã tải xong mà không có bộ thẻ: nó vừa bị xoá ở nơi khác (vd từ thiết bị khác, khi
            // có đồng bộ).
            deck == null -> BelowTopBar {
                StateMessage(
                    title = stringResource(R.string.deck_error_deck_not_found),
                    message = stringResource(R.string.deck_detail_not_found_message),
                    icon = R.drawable.ic_warning,
                    tone = BadgeTone.Danger,
                ) {
                    SecondaryButton(
                        text = stringResource(R.string.common_back),
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            state.cards.isEmpty() -> BelowTopBar {
                EmptyState(
                    title = stringResource(R.string.deck_detail_empty_title),
                    message = stringResource(R.string.deck_detail_empty_message),
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.deck_detail_empty_capture),
                        onClick = onCaptureClick,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = R.drawable.ic_camera,
                    )
                    SecondaryButton(
                        text = stringResource(R.string.home_empty_manual),
                        onClick = onAddCardClick,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = R.drawable.ic_edit,
                    )
                }
            }

            else -> DeckDetailBody(
                state = state,
                onReviewClick = onReviewClick,
                onFilterChange = onFilterChange,
                onCaptureClick = onCaptureClick,
                onAddCardClick = onAddCardClick,
                onEditCard = onEditCard,
                onDeleteCardClick = onDeleteCardClick,
            )
        }
    }
}

/** Đường kẻ dưới top bar + nội dung chiếm phần còn lại — dùng cho các trạng thái không có dải số liệu. */
@Composable
private fun ColumnScope.BelowTopBar(content: @Composable () -> Unit) {
    HorizontalDivider(color = appColors().cardBorder)
    Box(Modifier.weight(1f)) { content() }
}

@Composable
private fun ColumnScope.DeckDetailBody(
    state: DeckDetailState,
    onReviewClick: () -> Unit,
    onFilterChange: (CardFilter) -> Unit,
    onCaptureClick: () -> Unit,
    onAddCardClick: () -> Unit,
    onEditCard: (String) -> Unit,
    onDeleteCardClick: (String) -> Unit,
) {
    // Id của thẻ đang trượt mở để lộ nút Sửa / Xoá (null = không thẻ nào). Giữ ở đây thay vì
    // trong từng dòng để mỗi lúc chỉ một thẻ mở.
    var revealedCardId by rememberSaveable { mutableStateOf<String?>(null) }
    val visibleCards = state.visibleCards

    DeckStatsHeader(
        totalCount = state.cards.size,
        dueCount = state.dueCount,
        newCount = state.newCount,
        onReviewClick = onReviewClick,
    )
    CardFilterChips(
        selected = state.filter,
        totalCount = state.cards.size,
        manualCount = state.manualCount,
        aiCount = state.aiCount,
        onSelect = {
            revealedCardId = null
            onFilterChange(it)
        },
    )
    // `key(filter)`: mỗi chip lọc có một danh sách RIÊNG. Đổi chip là bỏ danh sách cũ, dựng danh
    // sách mới từ đầu -> luôn bắt đầu ở thẻ đầu tiên và không chạy hoạt ảnh nào. Nếu dùng chung
    // một danh sách, Compose coi đổi chip như "xoá 85 thẻ cùng lúc": hàng loạt thẻ mờ dần / trượt
    // đi làm màn hình nháy, và vị trí cuộn bám theo thẻ cũ nên thẻ đầu danh sách bị khuất.
    key(state.filter) {
        LazyColumn(
            // weight(1f): danh sách nhận hết chiều cao còn lại giữa hàng chip và thanh đáy.
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = AppDimens.defaultPadding,
                end = AppDimens.defaultPadding,
                top = AppDimens.padding6, // xem ghi chú về vùng chạm 48dp trong CardFilterChips
                bottom = AppDimens.defaultPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.padding10),
        ) {
            if (visibleCards.isEmpty()) {
                item(key = "filterEmpty", contentType = "filterEmpty") {
                    Text(
                        text = stringResource(R.string.deck_detail_filter_empty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppDimens.paddingHuge),
                        style = MaterialTheme.typography.bodyMedium,
                        color = appColors().textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(visibleCards, key = { it.id }, contentType = { "card" }) { card ->
                FlashcardRow(
                    card = card,
                    nowMillis = state.nowMillis,
                    revealed = revealedCardId == card.id,
                    onRevealChange = { open ->
                        // Mở thẻ này -> ghi id của nó (thẻ đang mở khác tự đóng). Đóng thì chỉ xoá
                        // id nếu chính thẻ này đang là thẻ mở.
                        if (open) revealedCardId = card.id else if (revealedCardId == card.id) revealedCardId = null
                    },
                    // Chạm vào thẻ và bấm nút "Sửa" cùng dẫn tới màn sửa thẻ.
                    onClick = {
                        revealedCardId = null
                        onEditCard(card.id)
                    },
                    onEdit = {
                        revealedCardId = null
                        onEditCard(card.id)
                    },
                    onDelete = { onDeleteCardClick(card.id) },
                    // Xoá MỘT thẻ thì các thẻ bên dưới trượt lên lấp chỗ trống thay vì nhảy cóc.
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
    DeckDetailBottomBar(onCaptureClick = onCaptureClick, onAddCardClick = onAddCardClick)
}

/** Thanh cố định ở đáy: hai lối thêm thẻ vào bộ này. */
@Composable
private fun DeckDetailBottomBar(onCaptureClick: () -> Unit, onAddCardClick: () -> Unit) {
    val colors = appColors()
    Column(Modifier.fillMaxWidth().background(colors.cardBackground)) {
        HorizontalDivider(color = colors.cardBorder)
        Row(
            modifier = Modifier.padding(
                start = AppDimens.defaultPadding,
                end = AppDimens.defaultPadding,
                top = AppDimens.paddingSmall,
                bottom = AppDimens.paddingMedium,
            ),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
        ) {
            SecondaryButton(
                text = stringResource(R.string.deck_detail_capture),
                onClick = onCaptureClick,
                modifier = Modifier.weight(1f),
                leadingIcon = R.drawable.ic_camera,
                height = AppDimens.btnMedium,
            )
            PrimaryButton(
                text = stringResource(R.string.deck_detail_add_card),
                onClick = onAddCardClick,
                modifier = Modifier.weight(1f),
                leadingIcon = R.drawable.ic_add,
                height = AppDimens.btnMedium,
            )
        }
    }
}

/** Nút ⋮ trên top bar và menu thả xuống của nó. */
@Composable
private fun DeckOptionsMenu(onDeleteDeckClick: () -> Unit) {
    val colors = appColors()
    // Menu đang mở hay đóng: `remember` là đủ (xoay màn hình thì menu đóng lại cũng không sao).
    var expanded by remember { mutableStateOf(false) }
    // Bọc trong Box để menu "neo" vào nút: DropdownMenu hiện ra ngay cạnh phần tử chứa nó.
    Box {
        AppIconButton(
            icon = R.drawable.ic_more_vert,
            contentDescription = stringResource(R.string.deck_detail_options),
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
            containerColor = colors.bottomSheetBackground,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.deck_detail_delete_deck), style = MaterialTheme.typography.bodyLarge) },
                onClick = {
                    expanded = false
                    onDeleteDeckClick()
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.sizeIcon),
                    )
                },
                colors = MenuDefaults.itemColors(
                    textColor = colors.statusRedText,
                    leadingIconColor = colors.statusRedText,
                ),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------------------

private const val PreviewNow = 1_800_000_000_000L
private const val PreviewDay = 24 * 60 * 60 * 1000L

private val previewDeck = Deck("1", "IELTS Vocabulary", "#4F46E5", createdAt = 0, updatedAt = 0)

private fun previewCard(
    id: String,
    question: String,
    answer: String,
    source: FlashcardSource,
    dueInDays: Int,
) = Flashcard(
    id = id,
    deckId = "1",
    noteId = if (source == FlashcardSource.AI) "note" else null,
    source = source,
    question = question,
    answer = answer,
    dueDate = PreviewNow + dueInDays * PreviewDay,
    lastReviewedAt = if (dueInDays == 0) null else PreviewNow - PreviewDay,
    createdAt = 0,
    updatedAt = 0,
)

private val previewCards = listOf(
    previewCard("a", "ubiquitous (adj)", "Có mặt ở khắp nơi; phổ biến đến mức đâu cũng thấy.", FlashcardSource.AI, 0),
    previewCard(
        "b",
        "Phân biệt affect và effect?",
        "Affect là động từ (gây ảnh hưởng); effect là danh từ (kết quả, tác động).",
        FlashcardSource.MANUAL,
        6,
    ),
    previewCard("c", "mitigate (v)", "Làm giảm nhẹ mức độ nghiêm trọng của một vấn đề.", FlashcardSource.AI, 1),
    previewCard("d", "a double-edged sword", "Con dao hai lưỡi — vừa lợi vừa hại.", FlashcardSource.MANUAL, 15),
)

@Composable
private fun PreviewHost(state: DeckDetailState, themeMode: ThemeMode = ThemeMode.LIGHT) {
    OnTapTldTheme(themeMode = themeMode) {
        DeckDetailContent(
            state = state,
            onBack = {},
            onRetry = {},
            onDeleteDeckClick = {},
            onReviewClick = {},
            onFilterChange = {},
            onCaptureClick = {},
            onAddCardClick = {},
            onEditCard = {},
            onDeleteCardClick = {},
        )
    }
}

private val previewLoaded =
    DeckDetailState(deck = previewDeck, cards = previewCards, nowMillis = PreviewNow, isLoaded = true)

@Preview(name = "Chi tiết bộ thẻ", widthDp = 390, heightDp = 844)
@Composable
private fun DeckDetailPreview() = PreviewHost(previewLoaded)

@Preview(name = "Chi tiết bộ thẻ — Dark", widthDp = 390, heightDp = 844)
@Composable
private fun DeckDetailDarkPreview() = PreviewHost(previewLoaded, ThemeMode.DARK)

@Preview(name = "Chi tiết bộ thẻ — chưa có thẻ", widthDp = 390, heightDp = 844)
@Composable
private fun DeckDetailEmptyPreview() =
    PreviewHost(DeckDetailState(deck = previewDeck, nowMillis = PreviewNow, isLoaded = true))

@Preview(name = "Chi tiết bộ thẻ — đang tải", widthDp = 390, heightDp = 844)
@Composable
private fun DeckDetailLoadingPreview() = PreviewHost(DeckDetailState())

@Preview(name = "Chi tiết bộ thẻ — lỗi", widthDp = 390, heightDp = 844)
@Composable
private fun DeckDetailErrorPreview() = PreviewHost(DeckDetailState(loadFailed = true))
