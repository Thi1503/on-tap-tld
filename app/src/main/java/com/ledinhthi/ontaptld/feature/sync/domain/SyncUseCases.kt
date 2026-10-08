package com.ledinhthi.ontaptld.feature.sync.domain

import com.ledinhthi.ontaptld.core.domain.usecase.NoInputFlowUseCase
import com.ledinhthi.ontaptld.core.domain.usecase.NoInputUseCase
import com.ledinhthi.ontaptld.core.sync.SyncScheduler
import com.ledinhthi.ontaptld.feature.auth.domain.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Chạy một lượt đồng bộ ngay bây giờ và chờ nó xong — nút "Đồng bộ ngay" và việc nền dùng chung. */
class SyncNowUseCase @Inject constructor(
    private val repository: SyncRepository,
) : NoInputUseCase<Unit>() {
    override suspend fun invoke() = repository.sync()
}

class ObserveSyncStatusUseCase @Inject constructor(
    private val repository: SyncRepository,
) : NoInputFlowUseCase<SyncStatus>() {
    override fun invoke(): Flow<SyncStatus> = repository.status
}

/**
 * Giữ cho việc đồng bộ tự chạy đúng lúc: gọi một lần ở `OnTapTldApp` và chạy suốt đời app (hàm
 * này KHÔNG tự kết thúc). Mỗi khi có người đăng nhập — kể cả trường hợp mở app lên đã đăng nhập
 * sẵn — thì hẹn một lượt đồng bộ; đăng xuất thì huỷ lượt đang hẹn.
 *
 * Lượt đồng bộ sau mỗi thay đổi trên máy do `SyncQueueRecorder` hẹn, không phải ở đây.
 */
class KeepSyncedUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val scheduler: SyncScheduler,
) : NoInputUseCase<Unit>() {
    override suspend fun invoke() {
        auth.currentUser
            .map { it?.uid }
            // Chỉ phản ứng khi NGƯỜI đăng nhập đổi, không phải mỗi lần Firebase báo lại.
            .distinctUntilChanged()
            .collect { uid -> if (uid != null) scheduler.requestSync() else scheduler.cancel() }
    }
}
