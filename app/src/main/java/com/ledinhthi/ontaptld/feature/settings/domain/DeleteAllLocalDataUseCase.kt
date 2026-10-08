package com.ledinhthi.ontaptld.feature.settings.domain

import com.ledinhthi.ontaptld.core.domain.usecase.NoInputUseCase
import javax.inject.Inject

/** "Xoá toàn bộ dữ liệu trên máy" ở màn Cài đặt. */
class DeleteAllLocalDataUseCase @Inject constructor(
    private val repository: LocalDataRepository,
) : NoInputUseCase<Unit>() {
    override suspend fun invoke() = repository.deleteAll()
}
