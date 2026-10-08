package com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.AppCard
import com.ledinhthi.ontaptld.core.presentation.components.SkeletonBlock
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

/** Khung giữ chỗ lúc đang đọc bộ thẻ — cùng bố cục với màn thật: dải số, chip lọc, vài thẻ. */
@Composable
fun DeckDetailSkeleton(modifier: Modifier = Modifier) {
    val colors = appColors()
    val loadingLabel = stringResource(R.string.common_loading)
    val pill = RoundedCornerShape(AppDimens.radius30)
    Column(modifier.fillMaxWidth().semantics { contentDescription = loadingLabel }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.appBarBackground)
                .padding(horizontal = AppDimens.defaultPadding)
                .padding(top = AppDimens.paddingSmallest, bottom = AppDimens.defaultPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall)) {
                repeat(3) { // ba ô số liệu
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall),
                    ) {
                        SkeletonBlock(Modifier.size(width = 44.dp, height = 24.dp))
                        SkeletonBlock(Modifier.size(width = 72.dp, height = 10.dp))
                    }
                }
            }
            SkeletonBlock(Modifier.fillMaxWidth().height(AppDimens.btnHeight46), MaterialTheme.shapes.medium)
        }
        HorizontalDivider(color = colors.cardBorder)

        Row(
            modifier = Modifier.padding(
                start = AppDimens.defaultPadding,
                top = AppDimens.paddingSmall,
                bottom = AppDimens.paddingSmall,
            ),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall),
        ) {
            listOf(92.dp, 104.dp, 64.dp).forEach { SkeletonBlock(Modifier.size(width = it, height = 36.dp), pill) }
        }

        Column(
            modifier = Modifier.padding(horizontal = AppDimens.defaultPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.padding10),
        ) {
            listOf(150.dp, 210.dp, 120.dp).forEach { questionWidth ->
                AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                    SkeletonBlock(Modifier.size(width = questionWidth, height = 16.dp))
                    Spacer(Modifier.height(AppDimens.padding10))
                    SkeletonBlock(Modifier.fillMaxWidth().height(12.dp))
                    Spacer(Modifier.height(14.dp))
                    Row {
                        SkeletonBlock(Modifier.size(width = 56.dp, height = 20.dp), RoundedCornerShape(AppDimens.radius6))
                        Spacer(Modifier.weight(1f))
                        SkeletonBlock(Modifier.width(96.dp).height(12.dp))
                    }
                }
            }
        }
    }
}
