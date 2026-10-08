package com.ledinhthi.ontaptld.feature.review.presentation.sourceimage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode
import com.ledinhthi.ontaptld.core.image.BitmapLoader
import com.ledinhthi.ontaptld.core.presentation.components.AppTopBar
import com.ledinhthi.ontaptld.core.presentation.components.DarkSystemBars
import com.ledinhthi.ontaptld.core.presentation.components.TopBarNavigation
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.DarkAppExtendedColors
import com.ledinhthi.ontaptld.core.presentation.theme.OnTapTldTheme
import com.ledinhthi.ontaptld.feature.deck.domain.model.SourceBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/*
 * Màn xem cả ảnh ghi chú mà một thẻ AI được rút ra (route `SourceImageRoute`), mở từ nút "Xem
 * cả ảnh" ở mặt đáp án lúc ôn. Ảnh hiện vừa khít màn; vùng của thẻ được viền cam, phần còn lại
 * của ảnh tối đi một chút. Chụm hai ngón để phóng to, kéo để di chuyển.
 *
 * Màn này luôn tối kể cả khi app ở giao diện sáng (như mọi trình xem ảnh), nên lấy màu thẳng từ
 * bảng màu tối thay vì `appColors()`.
 */

private val ViewerColors = DarkAppExtendedColors

/** Phóng to tối đa bấy nhiêu lần so với lúc ảnh vừa khít màn. */
private const val MaxZoom = 5f

/** Cạnh dài tối đa của ảnh đem hiển thị — bằng cỡ ảnh đã lưu, đủ nét khi phóng to. */
private const val DisplayMaxSide = 2560

/** Viền tô sáng nới rộng ra ngoài dòng chữ bấy nhiêu, cho chữ khỏi sát viền. */
private val HighlightPadding = 4.dp
private val HighlightCorner = 6.dp
private val HighlightStroke = 2.dp

/** Ba trạng thái của việc đọc file ảnh. */
private sealed interface LoadedImage {
    data object Loading : LoadedImage
    data object Failed : LoadedImage
    data class Ready(val bitmap: ImageBitmap) : LoadedImage
}

@Composable
fun SourceImageScreen(viewModel: SourceImageViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DarkSystemBars()

    // produceState: chạy khối lệnh ở nền mỗi khi đường dẫn ảnh đổi, kết quả gán vào `value` thì
    // màn tự vẽ lại. Đọc và giải nén ảnh là việc chậm nên làm ở luồng IO.
    val image by produceState<LoadedImage>(LoadedImage.Loading, state.isLoading, state.imagePath) {
        val path = state.imagePath
        value = when {
            state.isLoading -> LoadedImage.Loading
            path == null -> LoadedImage.Failed
            else -> {
                val bitmap = withContext(Dispatchers.IO) { BitmapLoader.decodeUpright(path, DisplayMaxSide) }
                if (bitmap == null) LoadedImage.Failed else LoadedImage.Ready(bitmap.asImageBitmap())
            }
        }
    }

    SourceImageContent(image = image, box = state.box, onClose = viewModel::onClose)
}

@Composable
private fun SourceImageContent(image: LoadedImage, box: SourceBox?, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ViewerColors.scaffoldBackground),
    ) {
        AppTopBar(
            title = stringResource(R.string.source_image_title),
            navigation = TopBarNavigation.Close,
            onNavigationClick = onClose,
            showDivider = false,
            containerColor = ViewerColors.scaffoldBackground,
            contentColor = ViewerColors.textPrimary,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when (image) {
                LoadedImage.Loading -> CircularProgressIndicator(
                    color = ViewerColors.primary,
                    trackColor = ViewerColors.neutralSoft,
                )

                LoadedImage.Failed -> Text(
                    text = stringResource(R.string.source_image_error),
                    modifier = Modifier.padding(horizontal = AppDimens.padding24),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ViewerColors.textSecondary,
                    textAlign = TextAlign.Center,
                )

                is LoadedImage.Ready -> ZoomableSourceImage(image = image.bitmap, box = box)
            }
        }
        if (image is LoadedImage.Ready) {
            Text(
                text = stringResource(
                    if (box != null) R.string.source_image_hint else R.string.source_image_hint_no_box,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.padding24, vertical = AppDimens.defaultPadding),
                style = MaterialTheme.typography.bodySmall,
                color = ViewerColors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Ảnh ghi chú phóng to / kéo được, có tô sáng vùng [box].
 *
 * Ảnh và lớp tô sáng được VẼ TAY trong một `Canvas` thay vì dùng `Image`: cả hai cùng tính từ một
 * bộ số (`scale`, `offset`) nên viền cam luôn nằm đúng trên dòng chữ dù ảnh đang ở cỡ nào.
 */
@Composable
private fun ZoomableSourceImage(image: ImageBitmap, box: SourceBox?) {
    // scale = 1 là ảnh vừa khít màn. offset = ảnh đang bị kéo lệch khỏi chính giữa bao nhiêu pixel.
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val description = stringResource(R.string.source_image_description)

    Canvas(
        Modifier
            .fillMaxSize()
            // Ảnh phóng to sẽ lớn hơn khung; không cắt thì nó vẽ đè lên top bar và dòng chú thích.
            .clipToBounds()
            .semantics { contentDescription = description }
            .pointerInput(image) {
                // detectTransformGestures báo về mỗi lần ngón tay nhích: `centroid` là điểm giữa
                // các ngón, `pan` là quãng vừa kéo, `zoom` là tỉ lệ vừa chụm / xoè (1 = không đổi).
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val container = size.toSize()
                    val fitted = fittedSize(image, container)
                    val newScale = (scale * zoom).coerceIn(1f, MaxZoom)
                    // Giữ cho điểm ảnh nằm dưới các ngón tay đứng yên trong lúc phóng: điểm đó
                    // cách tâm ảnh bao xa thì sau khi phóng cũng phải nằm đúng dưới ngón tay.
                    val fromCenter = centroid - container.center
                    val moved = fromCenter - (fromCenter - offset) * (newScale / scale) + pan
                    // Không cho kéo ảnh hở mép: chỉ được lệch tối đa bằng phần ảnh thừa ra ngoài
                    // khung mỗi phía (ảnh nhỏ hơn khung ở chiều nào thì chiều đó đứng yên).
                    val maxX = ((fitted.width * newScale - container.width) / 2f).coerceAtLeast(0f)
                    val maxY = ((fitted.height * newScale - container.height) / 2f).coerceAtLeast(0f)
                    scale = newScale
                    offset = Offset(moved.x.coerceIn(-maxX, maxX), moved.y.coerceIn(-maxY, maxY))
                }
            },
    ) {
        val drawn = fittedSize(image, size) * scale
        val topLeft = Offset(
            x = (size.width - drawn.width) / 2f + offset.x,
            y = (size.height - drawn.height) / 2f + offset.y,
        )
        drawImage(
            image = image,
            dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
            dstSize = IntSize(drawn.width.roundToInt(), drawn.height.roundToInt()),
        )

        if (box != null) {
            // Khung của thẻ lưu theo tỉ lệ của ảnh (0..1): nhân với cỡ ảnh đang vẽ là ra pixel.
            val padding = HighlightPadding.toPx()
            val highlight = Rect(
                left = topLeft.x + box.left * drawn.width - padding,
                top = topLeft.y + box.top * drawn.height - padding,
                right = topLeft.x + box.right * drawn.width + padding,
                bottom = topLeft.y + box.bottom * drawn.height + padding,
            )
            val corner = CornerRadius(HighlightCorner.toPx())
            // Lớp phủ tối lên cả ảnh TRỪ vùng của thẻ: một đường vẽ gồm hai hình lồng nhau với
            // kiểu tô EvenOdd thì phần nằm trong cả hai hình được để trống.
            val dim = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(topLeft, drawn))
                addRoundRect(RoundRect(highlight, corner))
            }
            drawPath(dim, color = Color.Black.copy(alpha = 0.5f))
            drawRoundRect(
                color = ViewerColors.primary,
                topLeft = highlight.topLeft,
                size = highlight.size,
                cornerRadius = corner,
                style = Stroke(width = HighlightStroke.toPx()),
            )
        }
    }
}

/** Cỡ của ảnh khi được thu / phóng cho vừa khít trong khung [container] mà không méo. */
private fun fittedSize(image: ImageBitmap, container: Size): Size {
    if (image.width <= 0 || image.height <= 0) return Size.Zero
    val fit = minOf(container.width / image.width, container.height / image.height)
    return Size(image.width * fit, image.height * fit)
}

// ---------------------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------------------

/** Ảnh giả cho preview: một tấm xám nhạt, vì preview không có file ảnh thật để đọc. */
private fun previewImage(): ImageBitmap = ImageBitmap(width = 600, height = 800).also { bitmap ->
    androidx.compose.ui.graphics.Canvas(bitmap).drawRect(
        Rect(0f, 0f, 600f, 800f),
        androidx.compose.ui.graphics.Paint().apply { color = Color(0xFFE9E4D8) },
    )
}

@Preview(name = "Ảnh nguồn — có vùng tô sáng", widthDp = 390, heightDp = 844)
@Composable
private fun SourceImagePreview() = OnTapTldTheme(themeMode = ThemeMode.LIGHT) {
    SourceImageContent(
        image = LoadedImage.Ready(previewImage()),
        box = SourceBox(left = 0.1f, top = 0.3f, right = 0.9f, bottom = 0.36f),
        onClose = {},
    )
}

@Preview(name = "Ảnh nguồn — không mở được ảnh", widthDp = 390, heightDp = 844)
@Composable
private fun SourceImageFailedPreview() = OnTapTldTheme(themeMode = ThemeMode.LIGHT) {
    SourceImageContent(image = LoadedImage.Failed, box = null, onClose = {})
}
