package com.ledinhthi.ontaptld.feature.review.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Sm2CalculatorTest {

    @Test
    fun `quen (q=1) reset repetitions ve 0 va interval ve 1`() {
        val out = Sm2Calculator.next(Sm2Input(easeFactor = 2.5, interval = 10, repetitions = 5), ReviewGrade.FORGOT)
        assertEquals(0, out.repetitions)
        assertEquals(1, out.interval)
        assertTrue(out.easeFactor >= 1.3)
    }

    @Test
    fun `lan on dau (repetitions=0, q gte 3) cho interval 1`() {
        val out = Sm2Calculator.next(Sm2Input(2.5, 0, 0), ReviewGrade.EASY)
        assertEquals(1, out.interval)
        assertEquals(1, out.repetitions)
    }

    @Test
    fun `lan on thu hai (repetitions=1) cho interval 6`() {
        val out = Sm2Calculator.next(Sm2Input(2.5, 1, 1), ReviewGrade.EASY)
        assertEquals(6, out.interval)
        assertEquals(2, out.repetitions)
    }

    @Test
    fun `tu lan thu ba interval = round(interval_cu nhan ease)`() {
        val out = Sm2Calculator.next(Sm2Input(easeFactor = 2.5, interval = 6, repetitions = 2), ReviewGrade.EASY)
        assertEquals(15, out.interval) // round(6 * 2.5)
    }

    @Test
    fun `ease factor khong bao gio duoi 1_3`() {
        var s = Sm2Input(1.3, 5, 3)
        repeat(10) {
            val o = Sm2Calculator.next(s, ReviewGrade.HARD)
            s = Sm2Input(o.easeFactor, o.interval, o.repetitions)
        }
        assertTrue(s.easeFactor >= 1.3)
    }
}
