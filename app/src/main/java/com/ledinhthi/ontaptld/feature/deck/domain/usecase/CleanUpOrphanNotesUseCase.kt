package com.ledinhthi.ontaptld.feature.deck.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.NoInputUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.repository.NoteRepository
import javax.inject.Inject

/**
 * Quét dọn ghi chú và ảnh ghi chú "mồ côi". Chạy một lần mỗi khi app khởi động (xem
 * `OnTapTldApp`), ở luồng nền — người dùng không thấy gì.
 */
class CleanUpOrphanNotesUseCase @Inject constructor(
    private val repository: NoteRepository,
) : NoInputUseCase<Unit>() {
    override suspend fun invoke() = repository.cleanUpOrphans()
}
