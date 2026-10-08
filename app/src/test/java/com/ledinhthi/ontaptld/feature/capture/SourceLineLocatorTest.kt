package com.ledinhthi.ontaptld.feature.capture

import com.ledinhthi.ontaptld.feature.capture.domain.SourceLineLocator
import com.ledinhthi.ontaptld.feature.capture.domain.model.OcrLine
import com.ledinhthi.ontaptld.feature.deck.domain.model.SourceBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceLineLocatorTest {

    private val locator = SourceLineLocator()

    /** Khung giả cho dòng thứ [row] của ảnh — mỗi dòng một khung khác nhau để nhận ra. */
    private fun box(row: Int) = SourceBox(left = 0.1f, top = row * 0.1f, right = 0.9f, bottom = row * 0.1f + 0.08f)

    private val ocrLines = listOf(
        OcrLine("Bài 1 · Nhân đôi ADN", box(0)),
        OcrLine("– Diễn ra ở pha S của kì trung gian.", box(1)),
        OcrLine("– Nguyên tắc: bổ sung và bán bảo toàn.", box(2)),
    )

    @Test
    fun `dong giong het - lay dung khung cua dong do`() {
        assertEquals(box(1), locator.locate("– Diễn ra ở pha S của kì trung gian.", 1, ocrLines))
    }

    @Test
    fun `khac chu hoa va khoang trang thua - van coi la giong het`() {
        assertEquals(box(0), locator.locate("  bài 1 ·  nhân đôi adn ", 0, ocrLines))
    }

    @Test
    fun `nguoi dung da sua vai ky tu nhan dang sai - van tim ra dong goc`() {
        // Bộ nhận dạng đọc sai "pha S" thành "pha 5", người dùng đã sửa lại ở bước kiểm tra.
        val recognized = listOf(ocrLines[0], OcrLine("– Dien ra ở pha 5 của kì trung gian.", box(1)), ocrLines[2])

        assertEquals(box(1), locator.locate("– Diễn ra ở pha S của kì trung gian.", 1, recognized))
    }

    @Test
    fun `dong nguoi dung tu go them - khong co tren anh`() {
        assertNull(locator.locate("ADN pôlimeraza tổng hợp mạch mới theo chiều 5'→3'.", 3, ocrLines))
    }

    @Test
    fun `tren anh co hai dong giong nhau - lay dong gan vi tri trong ghi chu hon`() {
        val repeated = listOf(
            OcrLine("Ví dụ:", box(0)),
            OcrLine("A liên kết với T", box(1)),
            OcrLine("Ví dụ:", box(2)),
            OcrLine("G liên kết với X", box(3)),
        )

        assertEquals(box(0), locator.locate("Ví dụ:", 0, repeated))
        assertEquals(box(2), locator.locate("Ví dụ:", 2, repeated))
    }

    @Test
    fun `dong khong co vi tri - bi bo qua`() {
        val withoutBox = listOf(OcrLine("Bài 1 · Nhân đôi ADN", box = null))

        assertNull(locator.locate("Bài 1 · Nhân đôi ADN", 0, withoutBox))
    }

    @Test
    fun `khong co dong nao tren anh hoac dong can tim trong - tra ve null`() {
        assertNull(locator.locate("Bài 1", 0, emptyList()))
        assertNull(locator.locate("   ", 0, ocrLines))
    }
}
