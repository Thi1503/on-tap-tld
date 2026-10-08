package com.ledinhthi.ontaptld.feature.reminder

import com.ledinhthi.ontaptld.core.data.local.prefs.AppPreferences
import com.ledinhthi.ontaptld.core.data.local.prefs.ReminderSettings
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.dueCutoffMillis
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import com.ledinhthi.ontaptld.feature.reminder.domain.KeepReminderScheduledUseCase
import com.ledinhthi.ontaptld.feature.reminder.domain.ReminderEnqueueMode
import com.ledinhthi.ontaptld.feature.reminder.domain.ReminderNotifier
import com.ledinhthi.ontaptld.feature.reminder.domain.ReminderScheduler
import com.ledinhthi.ontaptld.feature.reminder.domain.RunReviewReminderUseCase
import com.ledinhthi.ontaptld.feature.reminder.domain.millisUntilNextReminder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderTest {

    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val minute = 60_000L
    private val hour = 60 * minute

    private fun millisAt(hourOfDay: Int, minuteOfHour: Int, day: Int = 8) =
        LocalDateTime.of(2026, 10, day, hourOfDay, minuteOfHour).atZone(zone).toInstant().toEpochMilli()

    private val eightPm = ReminderSettings(enabled = true, minuteOfDay = 20 * 60)
    private val prefs = mockk<AppPreferences>()
    private val scheduler = mockk<ReminderScheduler>(relaxed = true)
    private val notifier = mockk<ReminderNotifier>(relaxed = true)
    private val flashcards = mockk<FlashcardRepository>()
    private val now = millisAt(20, 0)
    private val clock = mockk<Clock> { every { nowMillis() } returns now }

    // ---- Tính giờ ----

    @Test
    fun `chua toi gio nhac hom nay - hen trong hom nay`() {
        assertEquals(1 * hour + 30 * minute, millisUntilNextReminder(millisAt(18, 30), 20 * 60, zone))
    }

    @Test
    fun `da qua gio nhac hom nay - hen sang ngay mai`() {
        assertEquals(23 * hour + 30 * minute, millisUntilNextReminder(millisAt(20, 30), 20 * 60, zone))
    }

    @Test
    fun `dang dung gio nhac - hen sang ngay mai chu khong nhac lai ngay`() {
        // Lần nhắc 20:00 vừa chạy xong và tự hẹn lần sau: phải ra đúng 24 giờ, không phải 0.
        assertEquals(24 * hour, millisUntilNextReminder(millisAt(20, 0), 20 * 60, zone))
    }

    @Test
    fun `gio nhac 0 gio 00 va 23 gio 59 - van ra so duong`() {
        assertEquals(1 * minute, millisUntilNextReminder(millisAt(23, 59), 0, zone))
        assertTrue(millisUntilNextReminder(millisAt(23, 59), 23 * 60 + 59, zone) == 24 * hour)
    }

    // ---- Giữ lịch khớp với cài đặt ----

    @Test
    fun `app khoi dong khi dang bat nhac - giu lich dang co, khong hen de`() = runTest {
        every { prefs.reminder } returns flowOf(eightPm)

        KeepReminderScheduledUseCase(prefs, scheduler).invoke()

        verify(exactly = 1) { scheduler.schedule(eightPm, ReminderEnqueueMode.KeepExisting) }
    }

    @Test
    fun `bat nhac, doi gio, roi tat - hen, hen lai, roi huy`() = runTest {
        val settings = MutableStateFlow(ReminderSettings.Default) // đang tắt
        every { prefs.reminder } returns settings
        // Use case này nghe mãi không dừng, nên cho nó chạy ở một coroutine riêng rồi huỷ cuối test.
        val job = launch { KeepReminderScheduledUseCase(prefs, scheduler).invoke() }
        advanceUntilIdle()

        settings.value = eightPm
        advanceUntilIdle()
        val sevenAm = eightPm.copy(minuteOfDay = 7 * 60)
        settings.value = sevenAm
        advanceUntilIdle()
        settings.value = sevenAm.copy(enabled = false)
        advanceUntilIdle()
        job.cancel()

        verifyOrder {
            scheduler.cancel() // lúc khởi động: đang tắt
            scheduler.schedule(eightPm, ReminderEnqueueMode.Replace)
            scheduler.schedule(sevenAm, ReminderEnqueueMode.Replace)
            scheduler.cancel()
        }
    }

    // ---- Tới giờ nhắc ----

    private fun runReminder() = RunReviewReminderUseCase(prefs, flashcards, notifier, scheduler, clock)

    @Test
    fun `toi gio va co the den han - gui thong bao dung so the roi hen ngay mai`() = runTest {
        every { prefs.reminder } returns flowOf(eightPm)
        // Phải đếm theo mốc "hết hôm nay", cùng quy ước với Home.
        every { flashcards.observeDueCount(dueCutoffMillis(now)) } returns flowOf(9)

        runReminder().invoke()

        coVerify { notifier.showDueCards(9) }
        verify { scheduler.schedule(eightPm, ReminderEnqueueMode.AfterCurrentRun) }
    }

    @Test
    fun `toi gio nhung khong co the nao den han - im lang, van hen ngay mai`() = runTest {
        every { prefs.reminder } returns flowOf(eightPm)
        every { flashcards.observeDueCount(any()) } returns flowOf(0)

        runReminder().invoke()

        coVerify(exactly = 0) { notifier.showDueCards(any()) }
        verify { scheduler.schedule(eightPm, ReminderEnqueueMode.AfterCurrentRun) }
    }

    @Test
    fun `nguoi dung vua tat nhac truoc gio hen - khong bao, khong hen tiep`() = runTest {
        every { prefs.reminder } returns flowOf(eightPm.copy(enabled = false))

        runReminder().invoke()

        coVerify(exactly = 0) { notifier.showDueCards(any()) }
        verify(exactly = 0) { scheduler.schedule(any(), any()) }
    }

    @Test
    fun `gui thong bao loi - chuoi nhac hang ngay van khong dut`() = runTest {
        every { prefs.reminder } returns flowOf(eightPm)
        every { flashcards.observeDueCount(any()) } returns flowOf(3)
        coEvery { notifier.showDueCards(any()) } throws IllegalStateException("lỗi")

        val result = runCatching { runReminder().invoke() }

        assertTrue(result.isFailure)
        verify { scheduler.schedule(eightPm, ReminderEnqueueMode.AfterCurrentRun) }
    }
}
