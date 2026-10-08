package com.ledinhthi.ontaptld.feature.capture.domain.model

/** Bốn góc của khung cắt — người dùng kéo góc nào thì hai cạnh chạm góc đó dịch theo. */
enum class CropCorner { TopLeft, TopRight, BottomLeft, BottomRight }

/**
 * Khung cắt, tính theo TỈ LỆ của ảnh chứ không theo pixel: 0 là mép trái / mép trên, 1 là mép
 * phải / mép dưới. Nhờ vậy cùng một khung dùng được cho cả hình đang hiện trên màn (vài trăm
 * pixel) lẫn file ảnh gốc (vài nghìn pixel) — chỉ việc nhân với kích thước thật lúc cần.
 */
data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    /**
     * Trả về khung MỚI sau khi kéo [corner] đi một đoạn ([dx], [dy]) — cũng tính theo tỉ lệ.
     * Góc không ra khỏi ảnh, và khung không nhỏ hơn [minWidth] × [minHeight] (để hai góc không
     * chồng lên nhau, không còn kéo được nữa).
     */
    fun moveCorner(
        corner: CropCorner,
        dx: Float,
        dy: Float,
        minWidth: Float = MinSize,
        minHeight: Float = MinSize,
    ): CropRect {
        val movesLeft = corner == CropCorner.TopLeft || corner == CropCorner.BottomLeft
        val movesTop = corner == CropCorner.TopLeft || corner == CropCorner.TopRight
        // `coerceIn(a, b)` ép một số vào trong khoảng [a, b].
        return copy(
            left = if (movesLeft) (left + dx).coerceIn(0f, right - minWidth) else left,
            right = if (!movesLeft) (right + dx).coerceIn(left + minWidth, 1f) else right,
            top = if (movesTop) (top + dy).coerceIn(0f, bottom - minHeight) else top,
            bottom = if (!movesTop) (bottom + dy).coerceIn(top + minHeight, 1f) else bottom,
        )
    }

    companion object {
        /** Cạnh nhỏ nhất của khung khi nơi gọi không tự tính (màn hình tính theo dp, xem CropOverlay). */
        const val MinSize = 0.1f

        /** Vị trí khung trên khung ngắm camera, đo từ bản design (khung 298×318 trong ô 358×640). */
        val CameraDefault = CropRect(left = 0.084f, top = 0.15f, right = 0.916f, bottom = 0.647f)

        /** Ảnh chọn từ thư viện thường đã là thứ cần lấy, nên khung mở đầu ôm gần hết ảnh. */
        val GalleryDefault = CropRect(left = 0.05f, top = 0.05f, right = 0.95f, bottom = 0.95f)
    }
}
