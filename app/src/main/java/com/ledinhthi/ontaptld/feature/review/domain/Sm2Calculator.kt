package com.ledinhthi.ontaptld.feature.review.domain

import kotlin.math.roundToInt

enum class ReviewGrade(val q: Int) { FORGOT(1), HARD(3), EASY(4), VERY_EASY(5) }

data class Sm2Input(val easeFactor: Double, val interval: Int, val repetitions: Int)
data class Sm2Output(val easeFactor: Double, val interval: Int, val repetitions: Int)

/** SM-2 gốc, ánh xạ 4 nút -> q như docs_tld Mục 8.4. Không phụ thuộc Android — test kỹ nhất ở đây. */
object Sm2Calculator {
    fun next(current: Sm2Input, grade: ReviewGrade): Sm2Output {
        val q = grade.q
        var repetitions = current.repetitions
        val interval: Int
        if (q < 3) {
            repetitions = 0
            interval = 1
        } else {
            interval = when (repetitions) {
                0 -> 1
                1 -> 6
                else -> (current.interval * current.easeFactor).roundToInt()
            }
            repetitions += 1
        }
        val ease = (current.easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)))
            .coerceAtLeast(1.3)
        return Sm2Output(easeFactor = ease, interval = interval, repetitions = repetitions)
    }
}
