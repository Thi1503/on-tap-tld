package com.ledinhthi.ontaptld.feature.capture

import com.ledinhthi.ontaptld.feature.capture.domain.model.CropCorner
import com.ledinhthi.ontaptld.feature.capture.domain.model.CropRect
import org.junit.Assert.assertEquals
import org.junit.Test

class CropRectTest {

    private val rect = CropRect(left = 0.2f, top = 0.2f, right = 0.8f, bottom = 0.8f)

    // So sánh số thực phải cho phép lệch một chút xíu (tham số thứ ba của assertEquals).
    private fun assertRect(expected: CropRect, actual: CropRect) {
        assertEquals(expected.left, actual.left, 0.0001f)
        assertEquals(expected.top, actual.top, 0.0001f)
        assertEquals(expected.right, actual.right, 0.0001f)
        assertEquals(expected.bottom, actual.bottom, 0.0001f)
    }

    @Test
    fun `keo goc tren-trai - chi canh trai va canh tren dich theo`() {
        val moved = rect.moveCorner(CropCorner.TopLeft, dx = 0.1f, dy = -0.1f)
        assertRect(CropRect(0.3f, 0.1f, 0.8f, 0.8f), moved)
    }

    @Test
    fun `keo goc duoi-phai - chi canh phai va canh duoi dich theo`() {
        val moved = rect.moveCorner(CropCorner.BottomRight, dx = 0.1f, dy = -0.2f)
        assertRect(CropRect(0.2f, 0.2f, 0.9f, 0.6f), moved)
    }

    @Test
    fun `keo goc tren-phai va duoi-trai - moi goc dung hai canh cua no`() {
        assertRect(
            CropRect(0.2f, 0.3f, 0.7f, 0.8f),
            rect.moveCorner(CropCorner.TopRight, dx = -0.1f, dy = 0.1f),
        )
        assertRect(
            CropRect(0.1f, 0.2f, 0.8f, 0.9f),
            rect.moveCorner(CropCorner.BottomLeft, dx = -0.1f, dy = 0.1f),
        )
    }

    @Test
    fun `keo ra ngoai anh - goc dung lai o mep anh`() {
        assertRect(
            CropRect(0f, 0f, 0.8f, 0.8f),
            rect.moveCorner(CropCorner.TopLeft, dx = -5f, dy = -5f),
        )
        assertRect(
            CropRect(0.2f, 0.2f, 1f, 1f),
            rect.moveCorner(CropCorner.BottomRight, dx = 5f, dy = 5f),
        )
    }

    @Test
    fun `keo goc vuot qua goc doi dien - khung dung lai o co nho nhat`() {
        val moved = rect.moveCorner(
            corner = CropCorner.TopLeft,
            dx = 5f,
            dy = 5f,
            minWidth = 0.25f,
            minHeight = 0.1f,
        )
        assertRect(CropRect(0.55f, 0.7f, 0.8f, 0.8f), moved)
    }
}
