package com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.AppCard
import com.ledinhthi.ontaptld.core.presentation.components.PrimaryButton
import com.ledinhthi.ontaptld.core.presentation.components.SecondaryButton
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.AppPalette
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

/** Home khi chưa có bộ thẻ nào: minh hoạ, lời mời, hai lối tạo thẻ và 3 bước "cách hoạt động". */
@Composable
fun HomeEmptyContent(
    onCaptureClick: () -> Unit,
    onManualCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = appColors()
    Column(
        modifier = modifier
            .fillMaxSize()
            // Cho cuộn dọc: trên máy nhỏ hoặc khi cỡ chữ hệ thống to, nội dung dài hơn màn hình.
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.padding24)
            .padding(top = AppDimens.padding24, bottom = AppDimens.padding24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.padding24),
    ) {
        Spacer(Modifier.height(AppDimens.padding24))
        StackedCardsIllustration()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall),
        ) {
            Text(
                text = stringResource(R.string.home_empty_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.home_empty_message),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = MaterialTheme.typography.bodyLarge.lineHeight),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
        ) {
            PrimaryButton(
                text = stringResource(R.string.home_empty_capture),
                onClick = onCaptureClick,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = R.drawable.ic_camera,
            )
            SecondaryButton(
                text = stringResource(R.string.home_empty_manual),
                onClick = onManualCardClick,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = R.drawable.ic_edit,
            )
        }

        HowItWorksCard()
    }
}

@Composable
private fun HowItWorksCard() {
    val colors = appColors()
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppDimens.defaultPadding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = stringResource(R.string.home_how_title).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
                color = colors.textSecondary,
            )
            listOf(
                R.string.home_how_step1,
                R.string.home_how_step2,
                R.string.home_how_step3,
            ).forEachIndexed { index, textRes ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(colors.primarySoft, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.W800),
                            color = colors.primaryStrong,
                        )
                    }
                    Text(
                        text = stringResource(textRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                    )
                }
            }
        }
    }
}

/** Ba tấm thẻ xếp lệch nhau, tấm trên cùng có dấu cộng. Thuần trang trí. */
@Composable
private fun StackedCardsIllustration() {
    val colors = appColors()
    val accentBack = if (colors.isDark) Color(0xFF5A2A18) else AppPalette.ColorFFD4BE
    val cardShape = RoundedCornerShape(AppDimens.radius12)
    // Box xếp các con CHỒNG lên nhau (con viết sau nằm trên). Mỗi tấm thẻ được đặt vào vị trí
    // bằng `offset` rồi xoay nhẹ bằng `rotate`. `clearAndSetSemantics { }` giấu cả hình khỏi
    // trình đọc màn hình vì nó chỉ để trang trí.
    Box(
        Modifier
            .size(width = 200.dp, height = 150.dp)
            .clearAndSetSemantics { },
    ) {
        Box(
            Modifier
                .offset(x = 14.dp, y = 26.dp)
                .size(width = 150.dp, height = 104.dp)
                .rotate(-9f)
                .background(accentBack, cardShape),
        )
        Box(
            Modifier
                .offset(x = 34.dp, y = 18.dp)
                .size(width = 150.dp, height = 104.dp)
                .rotate(5f)
                .background(colors.primarySoft, cardShape)
                .border(1.dp, accentBack, cardShape),
        )
        Column(
            modifier = Modifier
                .offset(x = 25.dp, y = 22.dp)
                .size(width = 150.dp, height = 104.dp)
                .shadow(elevation = 6.dp, shape = cardShape, ambientColor = colors.cardShadow, spotColor = colors.cardShadow)
                .background(colors.cardBackground, cardShape)
                .border(1.dp, colors.border, cardShape)
                .padding(AppDimens.defaultPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(colors.primary, RoundedCornerShape(AppDimens.radius8)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.sizeIconDefault),
                    tint = colors.textOnAccent,
                )
            }
            Box(
                Modifier
                    .size(width = 100.dp, height = 8.dp)
                    .background(colors.neutralSoft, RoundedCornerShape(AppDimens.radius4)),
            )
            Box(
                Modifier
                    .width(70.dp)
                    .height(8.dp)
                    .background(colors.neutralSoft, RoundedCornerShape(AppDimens.radius4)),
            )
        }
    }
}
