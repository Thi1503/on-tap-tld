package com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.domain.util.daysUntilDue
import com.ledinhthi.ontaptld.core.presentation.components.AppBadge
import com.ledinhthi.ontaptld.core.presentation.components.AppCard
import com.ledinhthi.ontaptld.core.presentation.components.BadgeTone
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Bề rộng vùng nút Sửa / Xoá lộ ra khi kéo thẻ sang trái (theo thiết kế). */
private val RevealWidth = 132.dp

/** Vuốt nhanh hơn ngưỡng này (px/giây) thì mở/đóng theo hướng vuốt, không cần kéo quá nửa. */
private const val FlingVelocity = 600f

/**
 * Một thẻ trong danh sách. CHẠM vào thẻ = mở thẻ đó ([onClick]). KÉO thẻ sang trái = lộ hai nút
 * Sửa / Xoá nằm bên dưới; kéo ngược lại (hoặc chạm vào thẻ đang mở) để đóng.
 *
 * Việc "thẻ nào đang mở" do nơi gọi giữ ([revealed] + [onRevealChange]) chứ không giữ ở đây,
 * để danh sách bảo đảm mỗi lúc chỉ MỘT thẻ mở: mở thẻ khác thì thẻ này tự đóng.
 */
@Composable
fun FlashcardRow(
    card: Flashcard,
    nowMillis: Long,
    revealed: Boolean,
    onRevealChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = appColors()
    val shape = MaterialTheme.shapes.medium
    val revealPx = with(LocalDensity.current) { RevealWidth.toPx() }
    val scope = rememberCoroutineScope()

    // Độ lệch ngang của thẻ, tính bằng pixel: 0 = đóng, -revealPx = mở hết. `Animatable` là một
    // con số có thể vừa "nhảy" tức thì theo ngón tay (snapTo) vừa chạy mượt tới đích (animateTo).
    val offsetX = remember { Animatable(0f) }
    // Khi nơi gọi đổi `revealed` (vd thẻ khác vừa được mở) thì trượt thẻ này về đúng vị trí.
    LaunchedEffect(revealed) { offsetX.animateTo(if (revealed) -revealPx else 0f) }

    val editLabel = stringResource(R.string.deck_detail_edit_card)
    val deleteLabel = stringResource(R.string.deck_detail_delete_card)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            // Nền đỏ nhạt chỉ vẽ khi thẻ đã lệch khỏi chỗ; `drawBehind` đọc `offsetX` ở bước VẼ
            // nên kéo thẻ không làm cả dòng phải dựng lại từng khung hình.
            .drawBehind { if (offsetX.value < 0f) drawRect(colors.statusRedBg) },
    ) {
        Row(
            modifier = Modifier
                .matchParentSize() // cao đúng bằng tấm thẻ nằm trên
                .padding(end = AppDimens.paddingVerySmall)
                // Khi thẻ đang đóng, hai nút bị che nên cũng ẩn khỏi trình đọc màn hình (người
                // dùng TalkBack có "hành động tuỳ chỉnh" khai ở tấm thẻ bên dưới).
                .then(if (revealed) Modifier else Modifier.clearAndSetSemantics { }),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmallest, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SwipeAction(R.drawable.ic_edit, stringResource(R.string.common_edit), colors.textStrong, onEdit)
            SwipeAction(R.drawable.ic_delete, stringResource(R.string.common_delete), colors.statusRedText, onDelete)
        }

        AppCard(
            modifier = Modifier
                // Bản `offset { }` nhận lambda: vị trí được tính ở bước xếp chỗ, không dựng lại nội dung.
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        // Thẻ đi theo ngón tay nhưng bị chặn trong khoảng [mở hết, đóng].
                        scope.launch { offsetX.snapTo((offsetX.value + delta).coerceIn(-revealPx, 0f)) }
                    },
                    onDragStopped = { velocity ->
                        // Thả tay: vuốt nhanh thì theo hướng vuốt, còn không thì xét đã kéo quá nửa chưa.
                        val open = if (abs(velocity) > FlingVelocity) velocity < 0 else offsetX.value < -revealPx / 2
                        onRevealChange(open)
                        offsetX.animateTo(if (open) -revealPx else 0f)
                    },
                )
                // Vuốt là cử chỉ người dùng trình đọc màn hình không làm được, nên hai nút ẩn được
                // khai thêm thành "hành động tuỳ chỉnh" của thẻ (TalkBack đọc trong menu hành động).
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(editLabel) { onEdit(); true },
                        CustomAccessibilityAction(deleteLabel) { onDelete(); true },
                    )
                },
            // Thẻ đang lộ nút thì chạm chỉ để đóng lại, tránh vừa đóng vừa nhảy sang màn khác.
            onClick = { if (revealed) onRevealChange(false) else onClick() },
            contentPadding = PaddingValues(14.dp),
        ) {
            CardBody(card, nowMillis)
        }
    }
}

@Composable
private fun CardBody(card: Flashcard, nowMillis: Long) {
    val colors = appColors()
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.padding6)) {
        // Câu hỏi / câu trả lời tối đa 250 ký tự; trong danh sách chỉ hiện vài dòng đầu.
        Text(
            text = card.question,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = card.answer,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppDimens.paddingSmallest),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall),
            ) {
                if (card.source == FlashcardSource.AI) {
                    AppBadge(
                        text = stringResource(R.string.deck_source_ai),
                        tone = BadgeTone.Purple,
                        icon = R.drawable.ic_sparkle,
                    )
                    // Thẻ AI sinh ra từ một ghi chú đã chụp thì còn giữ ảnh gốc để xem lại.
                    if (card.noteId != null) {
                        Text(
                            text = stringResource(R.string.deck_detail_has_source_image),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    AppBadge(text = stringResource(R.string.deck_source_manual), tone = BadgeTone.Neutral)
                }
            }
            DueLabel(daysUntilDue(card.dueDate, nowMillis))
        }
    }
}

/** "Ôn hôm nay" (nổi bật) / "Ôn lại ngày mai" / "Ôn lại sau N ngày". */
@Composable
private fun DueLabel(days: Int) {
    val colors = appColors()
    when (days) {
        0 -> Text(
            text = stringResource(R.string.deck_detail_due_today),
            style = MaterialTheme.typography.labelSmall,
            color = colors.primaryStrong,
        )

        1 -> Text(
            text = stringResource(R.string.deck_detail_due_tomorrow),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )

        else -> Text(
            text = pluralStringResource(R.plurals.deck_detail_due_in_days, days, days),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}

/** Nút vuông 56dp gồm icon + chữ bên dưới, nằm ở lớp sau tấm thẻ. */
@Composable
private fun SwipeAction(@DrawableRes icon: Int, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(56.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null, // đã có chữ ngay bên dưới
            modifier = Modifier.size(AppDimens.sizeIcon),
            tint = tint,
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
