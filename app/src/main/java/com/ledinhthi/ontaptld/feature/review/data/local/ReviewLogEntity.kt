package com.ledinhthi.ontaptld.feature.review.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ledinhthi.ontaptld.feature.review.domain.ReviewGrade

/**
 * Nhật ký mỗi lần chấm 1 thẻ. `flashcards` chỉ giữ trạng thái SM-2 MỚI NHẤT nên không đủ để
 * dựng thống kê (số thẻ ôn mỗi ngày, tỉ lệ nhớ, chuỗi ngày ôn) — cần bảng lịch sử riêng.
 * Chỉ lưu local, không đồng bộ lên Cloud (cấu trúc Firestore ở docs_tld Mục 7.1 không có nó).
 */
@Entity(
    tableName = "review_logs",
    indices = [Index("reviewedAt"), Index("cardId")],
)
data class ReviewLogEntity(
    @PrimaryKey val id: String,
    val cardId: String,
    val deckId: String,
    val grade: ReviewGrade,
    val reviewedAt: Long,
)
