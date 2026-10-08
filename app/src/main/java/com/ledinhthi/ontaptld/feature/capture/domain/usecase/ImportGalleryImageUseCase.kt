package com.ledinhthi.ontaptld.feature.capture.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.feature.capture.domain.repository.CaptureImageRepository
import javax.inject.Inject

/** Đưa ảnh người dùng chọn từ thư viện (địa chỉ dạng chuỗi) vào app; trả về đường dẫn file. */
class ImportGalleryImageUseCase @Inject constructor(
    private val repository: CaptureImageRepository,
) : UseCase<String, String>() {
    override suspend fun invoke(input: String): String = repository.importImage(input)
}
