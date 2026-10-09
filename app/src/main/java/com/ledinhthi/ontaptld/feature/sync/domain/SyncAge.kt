package com.ledinhthi.ontaptld.feature.sync.domain

/**
 * "Lần đồng bộ gần nhất cách đây bao lâu", đã làm tròn về đơn vị dễ đọc. Màn hình đổi nó thành
 * chữ ("vừa xong", "5 phút trước"…); tách phần tính ra đây để test được mà không cần giao diện.
 */
sealed interface SyncAge {
    data object JustNow : SyncAge
    data class Minutes(val count: Int) : SyncAge
    data class Hours(val count: Int) : SyncAge
    data class Days(val count: Int) : SyncAge
}

private const val MINUTE_MILLIS = 60_000L
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
private const val DAY_MILLIS = 24 * HOUR_MILLIS

fun syncAgeOf(nowMillis: Long, syncedAtMillis: Long): SyncAge {
    // Đồng hồ máy bị chỉnh lùi có thể cho ra số âm — coi như vừa xong.
    val elapsed = (nowMillis - syncedAtMillis).coerceAtLeast(0)
    return when {
        elapsed < MINUTE_MILLIS -> SyncAge.JustNow
        elapsed < HOUR_MILLIS -> SyncAge.Minutes((elapsed / MINUTE_MILLIS).toInt())
        elapsed < DAY_MILLIS -> SyncAge.Hours((elapsed / HOUR_MILLIS).toInt())
        else -> SyncAge.Days((elapsed / DAY_MILLIS).toInt())
    }
}
