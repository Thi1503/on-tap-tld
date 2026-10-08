package com.ledinhthi.ontaptld.feature.capture.presentation.capture

import com.ledinhthi.ontaptld.core.presentation.mvi.UiEffect
import com.ledinhthi.ontaptld.core.presentation.mvi.UiState
import com.ledinhthi.ontaptld.core.presentation.mvi.UiStatus
import com.ledinhthi.ontaptld.feature.capture.domain.model.CropRect

/** Màn chụp có hai chặng trên cùng một bố cục. */
enum class CapturePhase {
    /** Camera đang chạy: ngắm, đặt khung, bấm chụp (hoặc chọn ảnh từ thư viện). */
    Camera,

    /** Đã có ảnh đứng yên: kéo 4 góc khung cho chuẩn rồi bấm ✓, hoặc chụp lại. */
    Adjust,
}

enum class CameraPermission {
    /** Chưa hỏi lần nào trong lượt mở màn này. */
    Unknown,
    Granted,

    /** Bị từ chối nhưng vẫn hỏi lại được. */
    Denied,

    /** Bị từ chối hẳn: hệ thống không hiện hộp thoại nữa, phải bật trong Cài đặt của máy. */
    DeniedForever,
}

data class CaptureState(
    override val status: UiStatus = UiStatus(),
    val phase: CapturePhase = CapturePhase.Camera,
    val permission: CameraPermission = CameraPermission.Unknown,
    val torchOn: Boolean = false,
    /** Đang chờ camera ghi ảnh xuống file — khoá nút chụp để không chụp chồng hai lần. */
    val isCapturing: Boolean = false,
    /** File ảnh đầy đủ (chưa cắt) đang hiện ở chặng [CapturePhase.Adjust]. */
    val photoPath: String? = null,
    val crop: CropRect = CropRect.CameraDefault,
) : UiState {
    val canShoot: Boolean
        get() = phase == CapturePhase.Camera && permission == CameraPermission.Granted && !isCapturing

    override fun withStatus(status: UiStatus) = copy(status = status)
}

sealed interface CaptureEffect : UiEffect {
    /**
     * Nhờ màn hình bấm máy và ghi ảnh vào [outputPath]. ViewModel không tự chụp được vì bộ điều
     * khiển camera gắn với màn hình (vòng đời, khung ngắm) — thứ ViewModel không được giữ.
     */
    data class TakePicture(val outputPath: String) : CaptureEffect
}
