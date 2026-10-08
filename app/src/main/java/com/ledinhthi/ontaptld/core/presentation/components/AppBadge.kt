package com.ledinhthi.ontaptld.core.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

enum class BadgeTone { Neutral, Purple, Primary, Success, Danger }

/** Nhãn chữ nhật bo nhẹ — phân loại (vd nguồn thẻ "AI" / "Thủ công", "Sắp có"). */
@Composable
fun AppBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.Neutral,
    @DrawableRes icon: Int? = null,
) = ToneLabel(
    text = text,
    tone = tone,
    icon = icon,
    shape = RoundedCornerShape(AppDimens.radius6),
    padding = PaddingValues(horizontal = AppDimens.paddingVerySmall, vertical = 3.dp),
    modifier = modifier,
)

/** Nhãn bo tròn hẳn — trạng thái kèm số liệu (vd "12 cần ôn", "Đã ôn xong"). */
@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.Primary,
    @DrawableRes icon: Int? = null,
) = ToneLabel(
    text = text,
    tone = tone,
    icon = icon,
    shape = RoundedCornerShape(AppDimens.radius30),
    padding = PaddingValues(horizontal = AppDimens.padding10, vertical = AppDimens.paddingSmallest),
    modifier = modifier,
)

/**
 * Cặp (nền, chữ) của từng tông — dùng lại cho emblem ở [StateMessage].
 *
 * `fun BadgeTone.colors()` là hàm MỞ RỘNG: gọi như thể nó là hàm của enum (`tone.colors()`).
 * `a to b` tạo một `Pair`; nơi dùng tách ra bằng `val (nen, chu) = tone.colors()`.
 * `internal` = chỉ dùng được trong module app, không lộ ra ngoài.
 */
@Composable
internal fun BadgeTone.colors(): Pair<Color, Color> = with(appColors()) {
    when (this@colors) {
        BadgeTone.Neutral -> neutralSoft to textStrong
        BadgeTone.Purple -> statusPurpleBg to statusPurpleText
        BadgeTone.Primary -> primarySoft to primaryStrong
        BadgeTone.Success -> statusGreenBg to statusGreenText
        BadgeTone.Danger -> statusRedBg to statusRedText
    }
}

@Composable
private fun ToneLabel(
    text: String,
    tone: BadgeTone,
    @DrawableRes icon: Int?,
    shape: Shape,
    padding: PaddingValues,
    modifier: Modifier,
) {
    val (background, content) = tone.colors()
    Row(
        modifier = modifier
            .background(background, shape)
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmallest),
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = content,
            )
        }
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = content, maxLines = 1)
    }
}
