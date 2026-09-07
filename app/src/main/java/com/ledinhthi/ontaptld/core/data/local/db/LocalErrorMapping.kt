package com.ledinhthi.ontaptld.core.data.local.db

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteFullException
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.LocalErrorKind
import kotlinx.coroutines.CancellationException

/**
 * Biến lỗi Room thô thành `AppException.LocalException`. Bọc mọi thao tác ghi DAO.
 * `inline` -> lambda vẫn gọi được hàm `suspend` khi wrapLocal được gọi từ context suspend.
 */
inline fun <T> wrapLocal(block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: SQLiteFullException) {
    throw AppException.LocalException(LocalErrorKind.DISK_FULL, e)
} catch (e: SQLiteConstraintException) {
    throw AppException.LocalException(LocalErrorKind.CONSTRAINT_VIOLATION, e)
} catch (e: Exception) {
    throw AppException.LocalException(LocalErrorKind.UNKNOWN, e)
}
