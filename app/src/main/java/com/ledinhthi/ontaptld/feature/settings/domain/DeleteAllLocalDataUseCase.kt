package com.ledinhthi.ontaptld.feature.settings.domain

import com.ledinhthi.ontaptld.core.domain.usecase.NoInputUseCase
import com.ledinhthi.ontaptld.feature.auth.domain.AuthRepository
import com.ledinhthi.ontaptld.feature.sync.domain.SyncRepository
import javax.inject.Inject

/**
 * "Xoá toàn bộ dữ liệu trên máy" ở màn Cài đặt.
 *
 * Đang đăng nhập thì ĐĂNG XUẤT luôn (Thi chốt 9/10/2026): nếu vẫn đăng nhập, lượt đồng bộ kế
 * tiếp sẽ tải hết dữ liệu từ đám mây về lại, coi như chưa xoá gì. Bản trên đám mây KHÔNG bị xoá —
 * đăng nhập lại là có lại. Thay đổi chưa kịp đồng bộ thì mất cùng dữ liệu trên máy.
 */
class DeleteAllLocalDataUseCase @Inject constructor(
    private val repository: LocalDataRepository,
    private val auth: AuthRepository,
    private val sync: SyncRepository,
) : NoInputUseCase<Unit>() {
    override suspend fun invoke() {
        // Thứ tự quan trọng: đăng xuất trước để không lượt đồng bộ MỚI nào bắt đầu; `forgetAccount`
        // chờ lượt đang chạy (nếu có) xong rồi mới quên mốc đồng bộ; cuối cùng mới xoá dữ liệu —
        // nhờ vậy không có lượt đồng bộ nào ghi dữ liệu vào lại sau khi đã xoá.
        if (auth.currentUserId != null) auth.signOut()
        sync.forgetAccount()
        repository.deleteAll()
    }
}
