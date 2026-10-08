package com.ledinhthi.ontaptld.feature.capture.presentation.capture.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.capture.domain.model.CropCorner
import com.ledinhthi.ontaptld.feature.capture.domain.model.CropRect

// Các số đo lấy từ bản design (Capture.dc.html).
private val FrameStroke = 2.dp
private val FrameRadius = 6.dp
private val HandleRadius = 9.dp   // núm tròn 18dp
private val HandleBorder = 3.dp
private const val ScrimAlpha = 0.55f

/** Ngón tay chạm trong bán kính này quanh một góc thì tính là bắt được góc đó. */
private val HandleTouchRadius = 32.dp

/** Khung không co nhỏ hơn cỡ này, để hai núm kề nhau không chồng lên nhau. */
private val MinFrameSide = 72.dp

/**
 * Lớp phủ khung cắt: làm tối phần ngoài khung, vẽ viền cam và 4 núm tròn ở 4 góc; kéo núm nào
 * thì góc đó đi theo ngón tay.
 *
 * @param crop khung hiện tại, theo tỉ lệ 0..1 của [contentRect].
 * @param contentRect vùng (tính bằng pixel, trong lớp phủ này) mà ảnh thật sự chiếm. Lúc camera
 *   chạy thì là cả ô; lúc xem ảnh đứng yên thì ảnh có thể hẹp hơn ô và nằm giữa.
 */
@Composable
fun CropOverlay(
    crop: CropRect,
    contentRect: Rect,
    onCropChange: (CropRect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val frameColor = appColors().primary
    // Khối `pointerInput` bên dưới chỉ khởi động MỘT lần và sống rất lâu. Nếu nó dùng thẳng
    // `crop` thì sẽ nhớ mãi giá trị lúc khởi động; `rememberUpdatedState` cho nó một "cửa sổ"
    // luôn nhìn thấy giá trị mới nhất.
    val currentCrop by rememberUpdatedState(crop)
    val currentOnCropChange by rememberUpdatedState(onCropChange)

    Canvas(
        modifier
            .fillMaxSize()
            // Truyền `contentRect` làm khoá: ảnh đổi kích thước thì khối xử lý chạm được dựng lại.
            .pointerInput(contentRect) {
                if (contentRect.isEmpty) return@pointerInput
                val touchRadius = HandleTouchRadius.toPx()
                val minWidth = (MinFrameSide.toPx() / contentRect.width).coerceAtMost(1f)
                val minHeight = (MinFrameSide.toPx() / contentRect.height).coerceAtMost(1f)
                // Mỗi vòng = một lần chạm, từ lúc ngón tay đặt xuống tới lúc nhấc lên.
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val corner = nearestCorner(down.position, currentCrop.toPixels(contentRect), touchRadius)
                        ?: return@awaitEachGesture // chạm ngoài 4 góc: bỏ qua
                    down.consume()
                    drag(down.id) { change ->
                        val moved = change.positionChange()
                        change.consume()
                        currentOnCropChange(
                            currentCrop.moveCorner(
                                corner = corner,
                                // Đổi quãng kéo từ pixel sang tỉ lệ của ảnh.
                                dx = moved.x / contentRect.width,
                                dy = moved.y / contentRect.height,
                                minWidth = minWidth,
                                minHeight = minHeight,
                            ),
                        )
                    }
                }
            },
    ) {
        if (contentRect.isEmpty) return@Canvas
        val frame = crop.toPixels(contentRect)
        val scrim = Color.Black.copy(alpha = ScrimAlpha)

        // Phần tối = bốn dải chữ nhật bao quanh khung (trên, dưới, trái, phải).
        drawRect(scrim, Offset.Zero, Size(size.width, frame.top))
        drawRect(scrim, Offset(0f, frame.bottom), Size(size.width, size.height - frame.bottom))
        drawRect(scrim, Offset(0f, frame.top), Size(frame.left, frame.height))
        drawRect(scrim, Offset(frame.right, frame.top), Size(size.width - frame.right, frame.height))

        drawRoundRect(
            color = frameColor,
            topLeft = frame.topLeft,
            size = frame.size,
            cornerRadius = CornerRadius(FrameRadius.toPx()),
            style = Stroke(width = FrameStroke.toPx()),
        )

        // Núm: hình tròn cam, bên trong là hình tròn trắng nhỏ hơn -> ra núm trắng viền cam.
        listOf(frame.topLeft, frame.topRight, frame.bottomLeft, frame.bottomRight).forEach { center ->
            drawCircle(frameColor, HandleRadius.toPx(), center)
            drawCircle(Color.White, (HandleRadius - HandleBorder).toPx(), center)
        }
    }
}

/** Đổi khung tỉ lệ (0..1) sang toạ độ pixel bên trong [content]. */
private fun CropRect.toPixels(content: Rect) = Rect(
    left = content.left + left * content.width,
    top = content.top + top * content.height,
    right = content.left + right * content.width,
    bottom = content.top + bottom * content.height,
)

/** Góc gần điểm chạm nhất, hoặc null nếu điểm chạm cách mọi góc xa hơn [maxDistance]. */
private fun nearestCorner(touch: Offset, frame: Rect, maxDistance: Float): CropCorner? =
    listOf(
        CropCorner.TopLeft to frame.topLeft,
        CropCorner.TopRight to frame.topRight,
        CropCorner.BottomLeft to frame.bottomLeft,
        CropCorner.BottomRight to frame.bottomRight,
    )
        .map { (corner, position) -> corner to (position - touch).getDistance() }
        .filter { (_, distance) -> distance <= maxDistance }
        .minByOrNull { (_, distance) -> distance }
        ?.first
