package com.ledinhthi.ontaptld.feature.capture.presentation.ocrreview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.image.BitmapLoader
import com.ledinhthi.ontaptld.core.presentation.components.TopBarNavigation
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.capture.presentation.components.CaptureStepHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MÀN TẠM cho bước 2/3. Màn "Kiểm tra văn bản" thật (nhận dạng chữ + sửa văn bản) sẽ thay file
 * này ở phần việc kế tiếp. Hiện giờ nó chỉ hiện ảnh đã cắt, để kiểm tra khung cắt ở màn chụp
 * cắt đúng chỗ.
 */
@Composable
fun OcrReviewPlaceholderScreen(imagePath: String, onBack: () -> Unit) {
    val colors = appColors()
    val image by produceState<ImageBitmap?>(initialValue = null, imagePath) {
        value = withContext(Dispatchers.IO) { BitmapLoader.decodeUpright(imagePath, 1600)?.asImageBitmap() }
    }
    Column(Modifier.fillMaxSize().background(colors.scaffoldBackground)) {
        CaptureStepHeader(
            title = stringResource(R.string.ocr_review_title),
            step = 2,
            onNavigationClick = onBack,
            navigation = TopBarNavigation.Back,
        )
        HorizontalDivider(color = colors.cardBorder)
        Column(
            modifier = Modifier.fillMaxSize().padding(AppDimens.defaultPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
        ) {
            Text(
                text = stringResource(R.string.ocr_review_cropped_image),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            image?.let {
                Image(
                    bitmap = it,
                    contentDescription = stringResource(R.string.ocr_review_cropped_image),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(
                text = stringResource(R.string.common_coming_soon),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
    }
}
