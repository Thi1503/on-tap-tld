package com.ledinhthi.ontaptld.feature.capture.presentation.capture

import androidx.lifecycle.SavedStateHandle
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.presentation.mvi.BaseViewModel
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.feature.capture.domain.exception.CaptureException
import com.ledinhthi.ontaptld.feature.capture.domain.model.CropRect
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.CropNoteImageUseCase
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.ImportGalleryImageUseCase
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.PreparePhotoFileUseCase
import com.ledinhthi.ontaptld.feature.capture.presentation.displayMessage
import com.ledinhthi.ontaptld.navigation.CaptureRoute
import com.ledinhthi.ontaptld.navigation.OcrReviewRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel của màn Chụp ghi chú (route `CaptureRoute`) — bước 1/3 của luồng tạo thẻ bằng AI.
 *
 * Luồng: [CapturePhase.Camera] → bấm chụp / chọn ảnh → [CapturePhase.Adjust] (chỉnh khung cắt)
 * → bấm ✓ → cắt ảnh → sang màn Kiểm tra văn bản.
 */
@HiltViewModel
class CaptureViewModel @Inject constructor(
    toolbox: ViewModelToolbox,
    savedState: SavedStateHandle,
    private val preparePhotoFile: PreparePhotoFileUseCase,
    private val importGalleryImage: ImportGalleryImageUseCase,
    private val cropNoteImage: CropNoteImageUseCase,
) : BaseViewModel<CaptureState>(CaptureState(), toolbox) {

    /** Bộ thẻ sẽ được chọn sẵn ở bước 2 — chỉ có khi mở màn này từ Chi tiết bộ thẻ. */
    private val deckId: String? = savedState[CaptureRoute::deckId.name]

    fun onClose() = navigator.back()

    /** [canAskAgain] = false khi hệ thống sẽ không hiện hộp thoại xin quyền thêm lần nào nữa. */
    fun onCameraPermissionResult(granted: Boolean, canAskAgain: Boolean = true) = setState {
        copy(
            permission = when {
                granted -> CameraPermission.Granted
                canAskAgain -> CameraPermission.Denied
                else -> CameraPermission.DeniedForever
            },
        )
    }

    fun onTorchToggle() = setState { copy(torchOn = !torchOn) }

    fun onShutterClick() {
        if (!currentState.canShoot) return
        setState { copy(isCapturing = true) }
        launchGuarded(onError = ::showCaptureError) {
            sendEffect(CaptureEffect.TakePicture(preparePhotoFile()))
        }
    }

    /** Màn hình báo về: camera đã ghi xong ảnh vào [path]. */
    fun onPictureSaved(path: String) = setState {
        // Giữ nguyên `crop`: ảnh chụp ra đúng bằng phần đang thấy trên khung ngắm, nên khung
        // người dùng vừa đặt vẫn nằm đúng chỗ. Camera tắt thì đèn cũng tắt theo.
        copy(phase = CapturePhase.Adjust, photoPath = path, isCapturing = false, torchOn = false)
    }

    fun onPictureFailed() {
        setState { copy(isCapturing = false) }
        navigator.showSnackBar(strings.get(R.string.capture_error_camera))
    }

    /** [uri] là địa chỉ ảnh do trình chọn ảnh của hệ thống trả về, ở dạng chuỗi. */
    fun onGalleryImagePicked(uri: String) = launchGuarded(
        showLoadingOverlay = true,
        onError = ::showCaptureError,
    ) {
        val path = importGalleryImage(uri)
        setState {
            copy(
                phase = CapturePhase.Adjust,
                photoPath = path,
                crop = CropRect.GalleryDefault,
                torchOn = false,
            )
        }
    }

    fun onCropChange(crop: CropRect) = setState { copy(crop = crop) }

    /** Bỏ ảnh đang chỉnh, quay về camera với khung ở vị trí ban đầu. */
    fun onRetake() = setState {
        copy(phase = CapturePhase.Camera, photoPath = null, crop = CropRect.CameraDefault)
    }

    fun onConfirmCrop() {
        val path = currentState.photoPath ?: return
        launchGuarded(showLoadingOverlay = true, onError = ::showCaptureError) {
            val cropped = cropNoteImage(CropNoteImageUseCase.Params(path, currentState.crop))
            navigator.to(OcrReviewRoute(imagePath = cropped, deckId = deckId))
        }
    }

    /** Lỗi của riêng luồng chụp thì tự báo bằng snackbar; lỗi lạ trả lại cho bộ xử lý chung. */
    private fun showCaptureError(e: AppException): AppException? {
        setState { copy(isCapturing = false) }
        return if (e is CaptureException) {
            navigator.showSnackBar(e.displayMessage(strings))
            null
        } else {
            e
        }
    }
}
