package com.ledinhthi.ontaptld.core.data.ai

import com.ledinhthi.ontaptld.core.data.local.prefs.AppPreferences
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.exception.AiErrorKind
import com.ledinhthi.ontaptld.core.exception.AppException
import kotlinx.coroutines.flow.first
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

    // LocalDate.ofInstant cần API 26 — đã bật core library desugaring (docs Mục 16).
    private fun todayString(): String =
        LocalDate.ofInstant(Instant.ofEpochMilli(clock.nowMillis()), ZoneId.systemDefault()).toString()

    companion object {
        const val MAX_CALLS_PER_DAY = 20
    }
}
