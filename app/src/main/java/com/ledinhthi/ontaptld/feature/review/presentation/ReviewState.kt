package com.ledinhthi.ontaptld.feature.review.presentation

import com.ledinhthi.ontaptld.core.presentation.mvi.UiState
import com.ledinhthi.ontaptld.core.presentation.mvi.UiStatus
import com.ledinhthi.ontaptld.feature.review.domain.ReviewGrade
import com.ledinhthi.ontaptld.feature.review.domain.model.ReviewCard

/** Màn ôn tập đang ở chặng nào — mỗi chặng vẽ một kiểu thân màn khác nhau. */
enum class ReviewPhase {
    LOADING,

    /** Đang hiện mặt câu hỏi của thẻ hiện tại. */
    QUESTION,

    /** Đã lật: hiện đáp án và 4 nút chấm. */
    ANSWER,

    /** Đã chấm hết thẻ của lượt này — hiện màn tổng kết. */
    DONE,

    /** Mở phiên ôn nhưng không có thẻ nào đến hạn. */
    EMPTY,
    ERROR,
}

/** Kết quả chấm một thẻ trong lượt này. [intervalDays] = số ngày tới lần ôn kế tiếp của thẻ. */
data class ReviewResult(
    val card: ReviewCard,
    val grade: ReviewGrade,
    val intervalDays: Int,
)

/**
 * "Lịch ôn tiếp theo" trên màn tổng kết: các thẻ vừa ôn sẽ quay lại khi nào, gom thành 3 nhóm.
 * [soonDays] có giá trị khi mọi thẻ trong nhóm giữa cùng hẹn một số ngày (để ghi "Sau 6 ngày").
 */
data class ReviewSchedule(
    val tomorrow: Int,
    val soon: Int,
    val soonDays: Int?,
    val later: Int,
)

private const val TwoWeeks = 14

data class ReviewState(
    override val status: UiStatus = UiStatus(),
    val phase: ReviewPhase = ReviewPhase.LOADING,
    /** Hàng thẻ của LƯỢT ôn hiện tại — chốt lúc bắt đầu lượt, không đổi trong lúc ôn. */
    val cards: List<ReviewCard> = emptyList(),
    /** Vị trí thẻ đang ôn trong [cards], đếm từ 0. */
    val index: Int = 0,
    val results: List<ReviewResult> = emptyList(),
) : UiState {
    val current: ReviewCard? get() = cards.getOrNull(index)
    val total: Int get() = cards.size

    /** Số thứ tự hiện cho người dùng ("5/24") — đếm từ 1. */
    val position: Int get() = (index + 1).coerceAtMost(total)
    val progress: Float get() = if (total == 0) 0f else position.toFloat() / total

    fun gradeCount(grade: ReviewGrade): Int = results.count { it.grade == grade }
    val forgotCount: Int get() = gradeCount(ReviewGrade.FORGOT)

    /** Lượt này đã ôn thẻ của bao nhiêu bộ khác nhau. */
    val reviewedDeckCount: Int get() = results.map { it.card.deck.id }.distinct().size

    val schedule: ReviewSchedule
        get() {
            val soonIntervals = results.map { it.intervalDays }.filter { it in 2 until TwoWeeks }
            return ReviewSchedule(
                tomorrow = results.count { it.intervalDays <= 1 },
                soon = soonIntervals.size,
                // distinct() bỏ giá trị trùng; còn đúng 1 giá trị nghĩa là cả nhóm hẹn cùng ngày.
                soonDays = soonIntervals.distinct().singleOrNull(),
                later = results.count { it.intervalDays >= TwoWeeks },
            )
        }

    override fun withStatus(status: UiStatus) = copy(status = status)
}
