package com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.AppCard
import com.ledinhthi.ontaptld.core.presentation.components.SkeletonBlock
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens

/** Khung giữ chỗ của Home lúc đang đọc dữ liệu — cùng bố cục với nội dung thật. */
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    val loadingLabel = stringResource(R.string.common_loading)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.defaultPadding)
            .padding(top = AppDimens.paddingSmallest)
            .semantics { contentDescription = loadingLabel },
        verticalArrangement = Arrangement.spacedBy(AppDimens.defaultPadding),
    ) {
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            contentPadding = PaddingValues(AppDimens.paddingMedium),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SkeletonBlock(Modifier.size(width = 120.dp, height = 14.dp))
                SkeletonBlock(Modifier.size(width = 96.dp, height = 48.dp), RoundedCornerShape(10.dp))
                SkeletonBlock(Modifier.fillMaxWidth().height(8.dp))
                SkeletonBlock(Modifier.size(width = 220.dp, height = 12.dp))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)) {
            repeat(2) { // 2 ô giữ chỗ cho "Chụp ghi chú" và "Gõ thẻ mới"
                AppCard(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.padding10)) {
                        SkeletonBlock(Modifier.size(36.dp), RoundedCornerShape(10.dp))
                        SkeletonBlock(Modifier.size(width = 100.dp, height = 14.dp))
                        SkeletonBlock(Modifier.size(width = 72.dp, height = 10.dp))
                    }
                }
            }
        }

        SkeletonBlock(Modifier.padding(top = AppDimens.paddingVerySmall).size(width = 150.dp, height = 18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.padding10)) {
            listOf(170.dp, 140.dp, 190.dp).forEach { nameWidth ->
                AppCard(Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
                    ) {
                        SkeletonBlock(Modifier.size(44.dp), MaterialTheme.shapes.medium)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall),
                        ) {
                            SkeletonBlock(Modifier.width(nameWidth).height(14.dp))
                            SkeletonBlock(Modifier.size(width = 64.dp, height = 10.dp))
                        }
                        SkeletonBlock(Modifier.size(width = 72.dp, height = 24.dp), RoundedCornerShape(AppDimens.radius12))
                    }
                }
            }
        }
    }
}
