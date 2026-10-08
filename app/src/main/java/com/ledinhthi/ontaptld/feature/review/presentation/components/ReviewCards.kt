package com.ledinhthi.ontaptld.feature.review.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.AppBadge
import com.ledinhthi.ontaptld.core.presentation.components.AppIconButton
import com.ledinhthi.ontaptld.core.presentation.components.BadgeTone
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.AppPalette
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.review.domain.model.CardSource
import com.ledinhthi.ontaptld.feature.review.domain.model.ExcerptLine
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Câu hỏi dài hơn ngưỡng này thì hạ cỡ chữ một bậc để vẫn đọc gọn trong thẻ. */
private const val LongQuestionLength = 140

/**
 * Mặt CÂU HỎI của thẻ đang ôn, vẽ như tấm trên cùng của một xấp thẻ (hai tấm mờ lấp ló phía
 * sau). Chạm vào đâu trên tấm thẻ cũng lật sang đáp án.
 */
@Composable
fun ReviewQuestionCard(
    card: Flashcard,
    onShowAnswer: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = appColors()
    val shape = MaterialTheme.shapes.large
    // Màu hai tấm phía sau chỉ dùng riêng ở đây nên không đưa vào bảng token chung.
    val backLayer = if (colors.isDark) Color(0xFF18191C) else AppPalette.ColorE3E4E8
    val midLayer = if (colors.isDark) Color(0xFF1E2024) else colors.neutralSoft
    val midBorder = if (colors.isDark) Color(0xFF2A2C31) else AppPalette.ColorE3E4E8
    val showAnswerLabel = stringResource(R.string.review_show_answer)

    // Ba tấm chồng lên nhau trong một Box: tấm viết sau nằm trên. Mỗi tấm phía sau hẹp hơn và
    // thấp hơn tấm trước một chút (nhờ `padding`) nên chỉ lộ ra phần mép dưới.
    Box(
        modifier.padding(
            start = AppDimens.defaultPadding,
            end = AppDimens.defaultPadding,
            top = AppDimens.paddingMedium,
            bottom = AppDimens.paddingVerySmall,
        ),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp)
                .background(backLayer, shape),
        )
        Box(
            Modifier
                .matchParentSize()
                .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
                .background(midLayer, shape)
                .border(1.dp, midBorder, shape),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
                .shadow(8.dp, shape, ambientColor = colors.cardShadow, spotColor = colors.cardShadow)
                .clip(shape)
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, shape)
                .clickable(onClickLabel = showAnswerLabel, onClick = onShowAnswer)
                .padding(AppDimens.defaultPadding),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SourceBadge(card.source)
                Spacer(Modifier.weight(1f))
                AppIconButton(
                    icon = R.drawable.ic_edit,
                    contentDescription = stringResource(R.string.review_edit_card),
                    onClick = onEdit,
                    // Nút có vùng chạm 44dp nhưng icon chỉ 20dp: đẩy nhẹ ra mép để icon thẳng
                    // hàng với lề phải của nội dung thẻ.
                    modifier = Modifier.offset(x = 8.dp),
                    tint = colors.textSecondary,
                    iconSize = AppDimens.sizeIcon,
                )
            }
            // Box canh giữa + Column cuộn bên trong: câu hỏi ngắn thì nằm giữa thẻ, câu hỏi dài
            // quá chiều cao thẻ thì cuộn được.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.paddingVerySmall),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
                ) {
                    FaceLabel(stringResource(R.string.review_question_label), colors.textSecondary)
                    Text(
                        text = card.question,
                        style = if (card.question.length > LongQuestionLength) {
                            MaterialTheme.typography.headlineSmall
                        } else {
                            MaterialTheme.typography.headlineLarge
                        },
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppDimens.paddingSmallest),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.padding6, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.sizeIconDefault),
                    tint = colors.textSecondary,
                )
                Text(
                    text = stringResource(R.string.review_tap_to_flip),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

/**
 * Mặt ĐÁP ÁN: câu hỏi thu nhỏ ở trên để đối chiếu, đáp án chữ lớn bên dưới. Thẻ AI còn ghi chú
 * gốc ([source] khác null) thì đáy thẻ có thêm phần "trích từ ghi chú" và nút [onViewSourceImage].
 */
@Composable
fun ReviewAnswerCard(
    card: Flashcard,
    source: CardSource?,
    onViewSourceImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = appColors()
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = modifier
            .padding(horizontal = AppDimens.defaultPadding)
            .padding(top = AppDimens.paddingMedium)
            .shadow(8.dp, shape, ambientColor = colors.cardShadow, spotColor = colors.cardShadow)
            .clip(shape)
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, shape)
            .padding(AppDimens.defaultPadding),
        verticalArrangement = Arrangement.spacedBy(AppDimens.defaultPadding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.padding6)) {
            FaceLabel(stringResource(R.string.review_question_label), colors.textSecondary)
            Text(
                text = card.question,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
                color = colors.textStrong,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(color = colors.divider)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.padding6),
        ) {
            FaceLabel(stringResource(R.string.review_answer_label), colors.primaryStrong)
            Text(
                text = card.answer,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )
        }
        if (source != null) {
            SourceSection(cardSource = card.source, source = source, onViewSourceImage = onViewSourceImage)
        } else {
            SourceBadge(card.source)
        }
    }
}

// Màu của khung trích ghi chú: nền giấy ngà, chữ mực xanh, dòng nguồn tô cam nhạt. Giống nhau ở
// cả giao diện sáng và tối (đúng bản design — nó mô phỏng một mẩu giấy), nên không lấy từ theme.
private val ExcerptPaper = Color(0xFFF3EEE2)
private val ExcerptInk = Color(0xFF1F3A6E)
private val ExcerptInkFaded = Color(0xFF7A8AA8)
private val ExcerptHighlight = AppPalette.ColorFFD4BE

/**
 * Đáy mặt đáp án của thẻ AI: nhãn nguồn + "Trích từ ghi chú chụp 05/10" + nút "Xem cả ảnh", rồi
 * tới đoạn trích ghi chú với dòng nguồn được tô sáng.
 */
@Composable
private fun SourceSection(cardSource: FlashcardSource, source: CardSource, onViewSourceImage: () -> Unit) {
    val colors = appColors()
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall),
        ) {
            SourceBadge(cardSource)
            Text(
                text = stringResource(R.string.review_source_from_note, capturedDateLabel(source.capturedAt)),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.review_view_full_image),
                modifier = Modifier
                    .overflowVertically(ViewImageTouchOverflow)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.Button, onClick = onViewSourceImage)
                    .padding(horizontal = AppDimens.paddingSmallest)
                    // Chữ nằm giữa vùng chạm cao 44dp.
                    .height(44.dp)
                    .wrapContentHeight(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.primaryStrong,
            )
        }
        if (source.excerpt.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ExcerptPaper, RoundedCornerShape(AppDimens.radius8))
                    .padding(horizontal = AppDimens.paddingVerySmall, vertical = AppDimens.paddingSmall),
                verticalArrangement = Arrangement.spacedBy(AppDimens.padding6),
            ) {
                source.excerpt.forEach { line -> ExcerptText(line) }
            }
        }
    }
}

/** Nút "Xem cả ảnh" cao 44dp (cho dễ chạm) được phép lấn ra ngoài hàng chứa nó bấy nhiêu mỗi phía. */
private val ViewImageTouchOverflow = 10.dp

/**
 * Báo với bố cục rằng phần tử này THẤP hơn thật [amount] ở cả phía trên lẫn phía dưới, rồi vẽ nó
 * lấn ra ngoài phần đã báo. Dùng cho nút nhỏ cần vùng chạm cao (44dp) mà không được đẩy hàng
 * chứa nó cao lên theo — cùng ý với "margin âm" trong bản design.
 */
private fun Modifier.overflowVertically(amount: Dp) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val overflow = amount.roundToPx()
    val reportedHeight = (placeable.height - 2 * overflow).coerceAtLeast(0)
    layout(placeable.width, reportedHeight) { placeable.place(0, -overflow) }
}

/** Một dòng của đoạn trích. Dòng nguồn của thẻ: chữ đậm trên nền cam nhạt; dòng lân cận: chữ nhạt. */
@Composable
private fun ExcerptText(line: ExcerptLine) {
    val style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 20.sp)
    Text(
        text = line.text,
        modifier = if (line.isSource) {
            Modifier
                .fillMaxWidth()
                .background(ExcerptHighlight, RoundedCornerShape(AppDimens.radius4))
                .padding(horizontal = AppDimens.padding6, vertical = 2.dp)
        } else {
            Modifier.padding(horizontal = AppDimens.padding6)
        },
        style = if (line.isSource) style.copy(fontWeight = FontWeight.W700) else style,
        color = if (line.isSource) ExcerptInk else ExcerptInkFaded,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Ngày chụp ghi chú, viết ngắn ("05/10"). Mẫu ngày nằm trong strings.xml vì mỗi ngôn ngữ một kiểu. */
@Composable
private fun capturedDateLabel(capturedAt: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val pattern = stringResource(R.string.review_source_date_pattern)
    return remember(capturedAt, locale, pattern) {
        Instant.ofEpochMilli(capturedAt)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern(pattern, locale))
    }
}

/** Nhãn nhỏ viết hoa, giãn chữ ("CÂU HỎI", "ĐÁP ÁN"). */
@Composable
private fun FaceLabel(text: String, color: Color) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
        color = color,
    )
}

@Composable
private fun SourceBadge(source: FlashcardSource) {
    if (source == FlashcardSource.AI) {
        AppBadge(
            text = stringResource(R.string.deck_source_ai),
            tone = BadgeTone.Purple,
            icon = R.drawable.ic_sparkle,
        )
    } else {
        AppBadge(text = stringResource(R.string.deck_source_manual))
    }
}
