package com.ledinhthi.ontaptld.feature.capture.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.feature.capture.domain.model.CropRect
import com.ledinhthi.ontaptld.feature.capture.domain.repository.CaptureImageRepository
import javax.inject.Inject

/** Cắt ảnh ghi chú theo khung người dùng đã chọn; trả về đường dẫn file ảnh đã cắt. */
class CropNoteImageUseCase @Inject constructor(
    private val repository: CaptureImageRepository,
) : UseCase<CropNoteImageUseCase.Params, String>() {

    data class Params(val sourcePath: String, val crop: CropRect)

    override suspend fun invoke(input: Params): String =
        repository.cropImage(input.sourcePath, input.crop)
}
