package com.ledinhthi.ontaptld.core.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DueCutoffTest {

    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0, nano: Int = 0) =
        ZonedDateTime.of(year, month, day, hour, minute, second, nano, zone).toInstant().toEpochMilli()

    @Test
    fun `giua ngay - moc la mili giay cuoi cung cua hom nay`() {
        val now = millis(2026, 10, 7, 9, 0)
        val endOfToday = millis(2026, 10, 7, 23, 59, 59, 999_000_000)

        assertEquals(endOfToday, dueCutoffMillis(now, zone))
    }

    @Test
    fun `the hen 21h toi nay duoc tinh la den han ngay tu sang`() {
        val morning = millis(2026, 10, 7, 7, 30)
        val dueTonight = millis(2026, 10, 7, 21, 0)
        val dueTomorrowEarly = millis(2026, 10, 8, 0, 0)

        val cutoff = dueCutoffMillis(morning, zone)

        assert(dueTonight <= cutoff)
        assert(dueTomorrowEarly > cutoff)
    }

    @Test
    fun `dung 0h - van thuoc ngay moi`() {
        val midnight = millis(2026, 10, 8, 0, 0)
        val endOfThatDay = millis(2026, 10, 8, 23, 59, 59, 999_000_000)

        assertEquals(endOfThatDay, dueCutoffMillis(midnight, zone))
    }

    @Test
    fun `daysUntilDue - dem theo ngay lich, khong theo so gio`() {
        val lateEvening = millis(2026, 10, 7, 23, 50)

        assertEquals(0, daysUntilDue(millis(2026, 10, 7, 23, 59), lateEvening, zone))
        // Chỉ cách 15 phút nhưng đã sang ngày khác -> "ngày mai".
        assertEquals(1, daysUntilDue(millis(2026, 10, 8, 0, 5), lateEvening, zone))
        assertEquals(6, daysUntilDue(millis(2026, 10, 13, 8, 0), lateEvening, zone))
    }

    @Test
    fun `daysUntilDue - the qua han tinh la hom nay, khong ra so am`() {
        val now = millis(2026, 10, 7, 9, 0)

        assertEquals(0, daysUntilDue(millis(2026, 10, 1, 9, 0), now, zone))
    }
}
