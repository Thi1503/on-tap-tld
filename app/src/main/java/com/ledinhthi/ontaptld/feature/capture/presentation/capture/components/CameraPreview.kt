package com.ledinhthi.ontaptld.feature.capture.presentation.capture.components

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import timber.log.Timber
import java.io.File

/**
 * Tạo bộ điều khiển camera của CameraX và giữ nó qua các lần vẽ lại. `LifecycleCameraController`
 * gói sẵn mọi việc lặt vặt: mở camera sau, nối với khung ngắm, chụp ảnh, bật đèn.
 */
@Composable
fun rememberCameraController(): LifecycleCameraController {
    val context = LocalContext.current
    return remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            // Chỉ cần chụp ảnh — không bật quay phim hay phân tích khung hình.
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }
}

/**
 * Khung ngắm: hình camera đang chạy. Camera mở khi composable này xuất hiện và ĐÓNG khi nó biến
 * mất (rời màn, hoặc chuyển sang chặng xem ảnh đứng yên) — không để camera chạy ngầm tốn pin.
 */
@Composable
fun CameraPreview(controller: LifecycleCameraController, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    // DisposableEffect: khối chính chạy lúc xuất hiện, `onDispose` chạy lúc biến mất.
    // Gắn với vòng đời của màn để camera cũng tự tắt khi app xuống nền.
    DisposableEffect(controller, lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }
    // CameraX chưa có khung ngắm viết bằng Compose đủ ổn định, nên dùng `PreviewView` của hệ
    // View cũ và nhúng vào Compose qua `AndroidView`.
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                // FILL_CENTER: hình camera phủ kín ô, phần thừa bị cắt đều hai bên.
                scaleType = PreviewView.ScaleType.FILL_CENTER
                // COMPATIBLE (vẽ bằng TextureView) thì khung ngắm mới bo góc được và lớp phủ
                // Compose mới nằm đúng phía trên; chế độ mặc định nhanh hơn chút nhưng không bo được.
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                this.controller = controller
            }
        },
        modifier = modifier,
    )
}

/**
 * Chụp một tấm và ghi vào [outputPath]. Vì khung ngắm đã nối với bộ điều khiển, CameraX tự cắt
 * ảnh cho khớp đúng phần đang thấy trên khung ngắm ("thấy sao chụp vậy").
 * Kết quả báo về qua [onSaved] / [onError], đều trên luồng chính.
 */
fun LifecycleCameraController.takePictureTo(
    context: Context,
    outputPath: String,
    onSaved: () -> Unit,
    onError: () -> Unit,
) {
    val output = ImageCapture.OutputFileOptions.Builder(File(outputPath)).build()
    takePicture(
        output,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) = onSaved()

            override fun onError(exception: ImageCaptureException) {
                Timber.w(exception, "Chụp ảnh lỗi")
                onError()
            }
        },
    )
}
