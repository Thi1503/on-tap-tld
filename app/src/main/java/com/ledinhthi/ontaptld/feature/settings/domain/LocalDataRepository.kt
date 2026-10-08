package com.ledinhthi.ontaptld.feature.settings.domain

/** Dữ liệu học người dùng tạo ra trên máy này, xét như MỘT khối (không phân biệt bộ thẻ nào). */
interface LocalDataRepository {
    /**
     * Xoá hết nội dung học: bộ thẻ, thẻ, ghi chú, ảnh ghi chú và lịch sử ôn. KHÔNG đụng tới các
     * cài đặt (giao diện, ngôn ngữ, giờ nhắc) và bộ đếm lượt AI trong ngày.
     */
    suspend fun deleteAll()
}
