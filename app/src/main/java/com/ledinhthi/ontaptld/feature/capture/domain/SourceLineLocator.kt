package com.ledinhthi.ontaptld.feature.capture.domain

import com.ledinhthi.ontaptld.feature.capture.domain.model.OcrLine
import com.ledinhthi.ontaptld.feature.deck.domain.model.SourceBox
import javax.inject.Inject
import kotlin.math.abs

/**
 * Tìm xem một dòng của GHI CHÚ (văn bản người dùng đã kiểm tra, có thể đã sửa) nằm ở đâu trên
 * ảnh, bằng cách so nó với các dòng mà bộ nhận dạng chữ đọc được — mỗi dòng đó có kèm vị trí.
 *
 * Không so theo số thứ tự được, vì ở bước kiểm tra văn bản người dùng có thể thêm, xoá hay gộp
 * dòng. Vì vậy so theo NỘI DUNG: dòng giống hệt thì lấy luôn, không thì lấy dòng giống nhất nếu
 * đủ giống; khác hẳn (vd dòng người dùng tự gõ thêm) thì coi như không có trên ảnh.
 */
class SourceLineLocator @Inject constructor() {

    /**
     * @param noteLine nội dung dòng cần tìm.
     * @param noteLineIndex vị trí của dòng đó trong ghi chú (đếm từ 0) — dùng để chọn khi trên
     * ảnh có vài dòng giống nhau: lấy dòng ở gần vị trí này nhất.
     * @return khung của dòng tương ứng trên ảnh; null nếu không tìm thấy dòng nào đủ giống.
     */
    fun locate(noteLine: String, noteLineIndex: Int, ocrLines: List<OcrLine>): SourceBox? {
        val target = normalize(noteLine)
        if (target.isEmpty()) return null

        // `withIndex()` ghép mỗi phần tử với vị trí của nó, để còn biết dòng nào gần vị trí cần tìm.
        val candidates = ocrLines.withIndex().filter { it.value.box != null }

        val exact = candidates
            .filter { normalize(it.value.text) == target }
            .minByOrNull { abs(it.index - noteLineIndex) }
        if (exact != null) return exact.value.box

        // `maxWithOrNull(compareBy(…))`: lấy dòng giống nhất; bằng điểm nhau thì lấy dòng gần hơn
        // (dấu trừ vì khoảng cách càng nhỏ càng tốt, trong khi đang tìm giá trị LỚN nhất).
        val best = candidates
            .map { it to similarity(target, normalize(it.value.text)) }
            .maxWithOrNull(compareBy({ it.second }, { -abs(it.first.index - noteLineIndex) }))
        return best?.takeIf { it.second >= MinSimilarity }?.first?.value?.box
    }

    /** Bỏ khác biệt không đáng kể trước khi so: chữ hoa / thường, khoảng trắng thừa. */
    private fun normalize(text: String): String =
        text.trim().lowercase().replace(Regex("\\s+"), " ")

    /**
     * Độ giống nhau của hai chuỗi, từ 0 (khác hẳn) tới 1 (giống hệt), theo hệ số Dice trên các
     * CẶP KÝ TỰ liền nhau: "abcd" có các cặp ab, bc, cd; hai chuỗi càng chung nhiều cặp càng
     * giống. Cách này chịu được vài ký tự bị sửa (người dùng chữa lỗi nhận dạng) mà vẫn nhận ra
     * cùng một dòng.
     */
    private fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.length < 2 || b.length < 2) return 0.0
        val pairsA = a.windowed(2)
        // Đếm số lần xuất hiện của từng cặp trong b, mỗi lần khớp thì trừ đi một — để cặp lặp
        // lại nhiều lần trong a không được tính khớp quá số lần nó có trong b.
        val remaining = b.windowed(2).groupingBy { it }.eachCount().toMutableMap()
        var shared = 0
        for (pair in pairsA) {
            val left = remaining[pair] ?: 0
            if (left > 0) {
                shared++
                remaining[pair] = left - 1
            }
        }
        return 2.0 * shared / (pairsA.size + (b.length - 1))
    }

    private companion object {
        /** Dưới mức này coi như hai dòng không phải là một. */
        const val MinSimilarity = 0.6
    }
}
