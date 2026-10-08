package com.ledinhthi.ontaptld.core.data.ai

import com.ledinhthi.ontaptld.core.data.local.prefs.AppPreferences
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.exception.AiErrorKind
import com.ledinhthi.ontaptld.core.exception.AppException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Chặn gọi AI khi vượt hạn mức/ngày/thiết bị (docs_tld NFR 10). */
class AiQuotaGuard @Inject constructor(
    private val prefs: AppPreferences,
    private val clock: Clock,
) {
    suspend fun ensureCanCall() {
        val usage = prefs.aiUsageToday.first()
        val count = if (usage.date == todayString()) usage.count else 0
        if (count >= MAX_CALLS_PER_DAY) {
            throw AppException.AiException(AiErrorKind.QUOTA_EXCEEDED_LOCAL)
        }
    }

    suspend fun recordCall() = prefs.incrementAiUsage(todayString())

    /** Số lượt còn lại hôm nay; phát giá trị mới mỗi khi bộ đếm đổi. Sang ngày mới thì đầy lại. */
    fun observeRemaining(): Flow<Int> = prefs.aiUsageToday.map { usage ->
        val used = if (usage.date == todayString()) usage.count else 0
        (MAX_CALLS_PER_DAY - used).coerceAtLeast(0)
    }

    // LocalDate.ofInstant cần API 26 — đã bật core library desugaring (docs Mục 16).
    private fun todayString(): String =
        LocalDate.ofInstant(Instant.ofEpochMilli(clock.nowMillis()), ZoneId.systemDefault()).toString()

    companion object {
        /**
         * Số lượt tạo thẻ bằng AI mỗi máy được dùng trong một ngày. Hạn mức miễn phí của Google
         * tính cho CẢ project (mọi người dùng chung): 500 lượt/ngày với dòng Flash-Lite, theo
         * bảng Rate limits trong AI Studio ngày 8/10/2026. 10 lượt mỗi máy = đủ cho khoảng 50
         * máy dùng hết lượt trong cùng một ngày. Hạn mức của Google đổi thì xem lại số này.
         */
        const val MAX_CALLS_PER_DAY = 10
    }
}
