package com.ledinhthi.ontaptld.feature.capture.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.NoInputUseCase
import com.ledinhthi.ontaptld.feature.capture.domain.repository.CaptureImageRepository
import javax.inject.Inject

/** Chuẩn bị chỗ cho camera ghi ảnh sắp chụp; trả về đường dẫn file. */
class PreparePhotoFileUseCase @Inject constructor(
    private val repository: CaptureImageRepository,
) : NoInputUseCase<String>() {
    override suspend fun invoke(): String = repository.newPhotoFile()
}
