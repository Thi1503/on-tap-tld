package com.ledinhthi.ontaptld.feature.deck.presentation.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.core.graphics.toColorInt
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

/** Các màu nhận diện người dùng chọn được khi tạo bộ thẻ. `hex` là giá trị lưu vào `Deck.colorHex`. */
enum class DeckColorOption(val hex: String, @StringRes val labelRes: Int) {
    PURPLE("#4F46E5", R.string.deck_color_purple),
    TEAL("#0D9488", R.string.deck_color_teal),
    PINK("#DB2777", R.string.deck_color_pink),
    BLUE("#0284C7", R.string.deck_color_blue),
    AMBER("#B45309", R.string.deck_color_amber),
    GRAY("#6B7280", R.string.deck_color_gray),
}

/**
 * Bộ màu suy ra từ 1 màu bộ thẻ, đã chỉnh theo theme để đủ tương phản:
 * - [base]: màu gốc — chấm màu, nền ô chọn màu.
 * - [tileBackground] / [onTile]: nền nhạt và icon của ô vuông đại diện bộ thẻ.
 * - [onDarkSurface]: bản sáng hơn để vẽ trên nền tối (thanh phân bổ trong thẻ "Cần ôn hôm nay").
 */
@Immutable
data class DeckTint(
    val base: Color,
    val tileBackground: Color,
    val onTile: Color,
    val onDarkSurface: Color,
)

/** Hex lỗi/định dạng lạ (dữ liệu cũ, dữ liệu đồng bộ về) rơi về màu mặc định thay vì crash. */
fun parseDeckColor(colorHex: String): Color =
    runCatching { Color(colorHex.toColorInt()) }.getOrDefault(Color(0xFF4F46E5))

@Composable
fun deckTint(colorHex: String): DeckTint {
    val isDark = appColors().isDark
    // remember(khoá…): chỉ tính lại khi màu hoặc theme đổi; các lần vẽ lại khác dùng kết quả cũ.
    // `lerp(a, b, t)` = pha màu a với màu b theo tỉ lệ t (0 = toàn a, 1 = toàn b).
    return remember(colorHex, isDark) {
        val base = parseDeckColor(colorHex)
        DeckTint(
            base = base,
            tileBackground = base.copy(alpha = if (isDark) 0.24f else 0.14f),
            onTile = if (isDark) lerp(base, Color.White, 0.5f) else lerp(base, Color.Black, 0.12f),
            onDarkSurface = lerp(base, Color.White, 0.3f),
        )
    }
}
