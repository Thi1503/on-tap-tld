package com.ledinhthi.ontaptld.core.domain.util

/**
 * Các dòng CÓ NỘI DUNG của một ghi chú: bỏ dòng trống và khoảng trắng thừa ở hai đầu mỗi dòng.
 *
 * Vị trí trong danh sách này (tính từ 1) là "số dòng" dùng thống nhất ở mọi nơi: lời gửi cho AI
 * đánh số ghi chú theo nó, AI báo lại thẻ lấy từ dòng nào, màn duyệt thẻ ghi "Nguồn: dòng 2
 * trong ảnh", và mặt đáp án lúc ôn trích lại đúng dòng đó. Vì vậy chỉ có MỘT hàm này tách dòng.
 */
fun noteLines(noteText: String): List<String> =
    noteText.lines().map { it.trim() }.filter { it.isNotEmpty() }
