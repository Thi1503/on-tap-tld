package com.ledinhthi.ontaptld.feature.capture.presentation.capture

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode
import com.ledinhthi.ontaptld.core.image.BitmapLoader
import com.ledinhthi.ontaptld.core.presentation.components.DarkSystemBars
import com.ledinhthi.ontaptld.core.presentation.components.LoadingOverlay
import com.ledinhthi.ontaptld.core.presentation.components.LoadingState
import com.ledinhthi.ontaptld.core.presentation.components.ObserveEffects
import com.ledinhthi.ontaptld.core.presentation.components.PrimaryButton
import com.ledinhthi.ontaptld.core.presentation.components.StateMessage
import com.ledinhthi.ontaptld.core.presentation.components.TopBarNavigation
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.AppTextStyle
import com.ledinhthi.ontaptld.core.presentation.theme.OnTapTldTheme
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.capture.domain.model.CropRect
import com.ledinhthi.ontaptld.feature.capture.presentation.capture.components.CameraPreview
import com.ledinhthi.ontaptld.feature.capture.presentation.capture.components.CaptureControls
import com.ledinhthi.ontaptld.feature.capture.presentation.capture.components.CropOverlay
import com.ledinhthi.ontaptld.feature.capture.presentation.capture.components.rememberCameraController
import com.ledinhthi.ontaptld.feature.capture.presentation.capture.components.takePictureTo
import com.ledinhthi.ontaptld.feature.capture.presentation.components.CaptureStepHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Dải sát đáy khung ngắm dành cho dòng nhắc (16dp lề + chiều cao dòng nhắc + khoảng hở cho núm). */
private val HintZoneHeight = 72.dp

/** Cạnh dài tối đa của ảnh đứng yên khi đem hiển thị — đủ nét trên màn hình điện thoại. */
private const val DisplayMaxSide = 1600

/**
 * Màn Chụp ghi chú (route `CaptureRoute`) — bước 1/3 của luồng tạo thẻ bằng AI.
 *
 * Hàm này lo mọi thứ phải đụng tới hệ thống Android: xin quyền camera, mở trình chọn ảnh, điều
 * khiển camera, đổi màu thanh trạng thái. Phần vẽ nằm ở [CaptureContent] bên dưới, không biết
 * gì về các thứ đó nên xem trước (Preview) được.
 */
@Composable
fun CaptureScreen(viewModel: CaptureViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val cameraController = rememberCameraController()

    DarkSystemBars()

    // --- Quyền camera -----------------------------------------------------------------------
    // `rememberLauncherForActivityResult` = cách Compose "mở một màn của hệ thống rồi nhận kết
    // quả": gọi `launch(...)` để mở, khối lambda chạy khi người dùng trả lời xong.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // Sau khi bị từ chối, hệ thống cho biết còn nên giải thích-rồi-hỏi-lại không. Trả lời
        // "không" nghĩa là hộp thoại sẽ không hiện nữa -> chỉ còn cách bật trong Cài đặt của máy.
        val canAskAgain = context.findActivity()?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } ?: true
        viewModel.onCameraPermissionResult(granted, canAskAgain)
    }
    // Chạy mỗi lần màn hiện lại (kể cả khi vừa quay về từ Cài đặt của máy): đã có quyền thì dùng luôn.
    LifecycleResumeEffect(Unit) {
        if (context.hasCameraPermission()) viewModel.onCameraPermissionResult(granted = true)
        onPauseOrDispose { }
    }
    // Lần đầu vào màn mà chưa có quyền thì hỏi ngay, không bắt người dùng bấm thêm nút nào.
    LaunchedEffect(Unit) {
        if (state.permission == CameraPermission.Unknown && !context.hasCameraPermission()) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // --- Thư viện ảnh -------------------------------------------------------------------------
    // Trình chọn ảnh của hệ thống: người dùng tự chọn MỘT ảnh và app chỉ nhận đúng ảnh đó, nên
    // không cần xin quyền đọc toàn bộ thư viện.
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.onGalleryImagePicked(uri.toString())
    }

    // --- Camera -------------------------------------------------------------------------------
    LaunchedEffect(state.torchOn) { cameraController.enableTorch(state.torchOn) }
    ObserveEffects(viewModel.effect) { effect ->
        when (effect) {
            is CaptureEffect.TakePicture -> cameraController.takePictureTo(
                context = context,
                outputPath = effect.outputPath,
                onSaved = { viewModel.onPictureSaved(effect.outputPath) },
                onError = viewModel::onPictureFailed,
            )
        }
    }
    val torchAvailable = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    // Đang chỉnh ảnh mà bấm Back của hệ thống thì quay về camera, chưa thoát màn.
    BackHandler(enabled = state.phase == CapturePhase.Adjust) { viewModel.onRetake() }

    // Đọc file ảnh thành hình để vẽ. `produceState` chạy khối lệnh ở nền rồi đưa kết quả vào
    // một State; `photoPath` đổi thì nó tự chạy lại (và trả về null trong lúc chờ).
    val photo by produceState<ImageBitmap?>(initialValue = null, state.photoPath) {
        value = state.photoPath?.let { path ->
            withContext(Dispatchers.IO) { BitmapLoader.decodeUpright(path, DisplayMaxSide)?.asImageBitmap() }
        }
    }

    // Màn này luôn tối như app camera, kể cả khi app đang ở giao diện sáng. Bọc trong theme tối
    // để `appColors()` bên trong tự trả về bộ màu tối, không phải ghi tay từng mã màu.
    OnTapTldTheme(themeMode = ThemeMode.DARK) {
        LoadingOverlay(isLoading = state.status.isLoadingOverlay) {
            CaptureContent(
                state = state,
                photo = photo,
                torchAvailable = torchAvailable,
                cameraPreview = { modifier -> CameraPreview(cameraController, modifier) },
                onClose = viewModel::onClose,
                onCropChange = viewModel::onCropChange,
                onPermissionAction = {
                    if (state.permission == CameraPermission.DeniedForever) {
                        context.openAppSettings()
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onGalleryClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onShutterClick = viewModel::onShutterClick,
                onTorchToggle = viewModel::onTorchToggle,
                onRetakeClick = viewModel::onRetake,
                onConfirmClick = viewModel::onConfirmCrop,
            )
        }
    }
}

@Composable
private fun CaptureContent(
    state: CaptureState,
    photo: ImageBitmap?,
    torchAvailable: Boolean,
    cameraPreview: @Composable (Modifier) -> Unit,
    onClose: () -> Unit,
    onCropChange: (CropRect) -> Unit,
    onPermissionAction: () -> Unit,
    onGalleryClick: () -> Unit,
    onShutterClick: () -> Unit,
    onTorchToggle: () -> Unit,
    onRetakeClick: () -> Unit,
    onConfirmClick: () -> Unit,
) {
    val colors = appColors()
    Column(Modifier.fillMaxSize().background(colors.scaffoldBackground)) {
        CaptureStepHeader(
            title = stringResource(R.string.capture_title),
            step = 1,
            onNavigationClick = onClose,
            navigation = TopBarNavigation.Close,
            containerColor = colors.scaffoldBackground,
        )
        Viewfinder(
            state = state,
            photo = photo,
            cameraPreview = cameraPreview,
            onCropChange = onCropChange,
            onPermissionAction = onPermissionAction,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = AppDimens.defaultPadding),
        )
        CaptureControls(
            phase = state.phase,
            canShoot = state.canShoot,
            canConfirm = photo != null,
            torchOn = state.torchOn,
            torchAvailable = torchAvailable && state.permission == CameraPermission.Granted,
            onGalleryClick = onGalleryClick,
            onShutterClick = onShutterClick,
            onTorchToggle = onTorchToggle,
            onRetakeClick = onRetakeClick,
            onConfirmClick = onConfirmClick,
        )
    }
}

/**
 * Ô lớn giữa màn. Tuỳ tình huống mà hiện: hình camera đang chạy, ảnh đứng yên vừa chụp / vừa
 * chọn, hoặc lời nhắc cấp quyền camera. Hai trường hợp đầu có khung cắt phủ lên trên.
 */
@Composable
private fun Viewfinder(
    state: CaptureState,
    photo: ImageBitmap?,
    cameraPreview: @Composable (Modifier) -> Unit,
    onCropChange: (CropRect) -> Unit,
    onPermissionAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = appColors()
    // BoxWithConstraints giống Box nhưng cho biết kích thước thật của ô (`constraints`), cần
    // để tính ảnh nằm ở đâu trong ô.
    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(AppDimens.radius20))
            .background(colors.cardBackground),
    ) {
        val boxSize = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val hintZoneTop = boxSize.height - with(LocalDensity.current) { HintZoneHeight.toPx() }
        when {
            // Ảnh vừa chụp cần một thoáng để đọc từ file lên: trong lúc đó hiện vòng xoay.
            state.phase == CapturePhase.Adjust -> if (photo == null) {
                LoadingState()
            } else {
                Image(
                    bitmap = photo,
                    contentDescription = stringResource(R.string.capture_photo_description),
                    modifier = Modifier.fillMaxSize(),
                    // Fit: thấy trọn ảnh, không bị cắt mép (ảnh thư viện có thể hẹp hơn ô).
                    contentScale = ContentScale.Fit,
                )
                val photoRect = fitInside(Size(photo.width.toFloat(), photo.height.toFloat()), boxSize)
                CropOverlay(crop = state.crop, contentRect = photoRect, onCropChange = onCropChange)
                if (state.crop.bottomIn(photoRect) < hintZoneTop) {
                    CropHint(Modifier.align(Alignment.BottomCenter))
                }
            }

            state.permission == CameraPermission.Granted -> {
                val previewRect = Rect(Offset.Zero, boxSize) // hình camera phủ kín cả ô
                cameraPreview(Modifier.fillMaxSize())
                CropOverlay(crop = state.crop, contentRect = previewRect, onCropChange = onCropChange)
                if (state.crop.bottomIn(previewRect) < hintZoneTop) {
                    CropHint(Modifier.align(Alignment.BottomCenter))
                }
            }

            // Đang chờ người dùng trả lời hộp thoại xin quyền: để ô trống.
            state.permission == CameraPermission.Unknown -> Unit

            else -> StateMessage(
                title = stringResource(R.string.capture_permission_title),
                message = stringResource(R.string.capture_permission_message),
                icon = R.drawable.ic_camera,
            ) {
                PrimaryButton(
                    text = stringResource(
                        if (state.permission == CameraPermission.DeniedForever) {
                            R.string.capture_permission_settings
                        } else {
                            R.string.capture_permission_grant
                        },
                    ),
                    onClick = onPermissionAction,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Mép dưới của khung cắt, tính bằng pixel, khi ảnh chiếm vùng [content] của ô. */
private fun CropRect.bottomIn(content: Rect): Float = content.top + bottom * content.height

/**
 * Dòng nhắc nổi ở đáy khung ngắm. Nơi gọi ẩn nó đi khi khung cắt kéo xuống chạm vùng này, để
 * chữ không che mất hai núm phía dưới.
 */
@Composable
private fun CropHint(modifier: Modifier = Modifier) {
    val colors = appColors()
    Text(
        text = stringResource(R.string.capture_crop_hint),
        modifier = modifier
            .padding(bottom = AppDimens.defaultPadding)
            .padding(horizontal = AppDimens.defaultPadding)
            .background(colors.scaffoldBackground.copy(alpha = 0.82f), RoundedCornerShape(AppDimens.radius30))
            .padding(horizontal = 14.dp, vertical = AppDimens.paddingVerySmall),
        style = AppTextStyle.font12Semi(),
        color = colors.textPrimary,
    )
}

/** Vùng mà một ảnh cỡ [image] chiếm khi được thu / phóng cho vừa khít trong ô cỡ [box], đặt giữa ô. */
private fun fitInside(image: Size, box: Size): Rect {
    if (image.width <= 0f || image.height <= 0f) return Rect.Zero
    val scale = minOf(box.width / image.width, box.height / image.height)
    val fitted = Size(image.width * scale, image.height * scale)
    return Rect(
        offset = Offset((box.width - fitted.width) / 2f, (box.height - fitted.height) / 2f),
        size = fitted,
    )
}

// ---------------------------------------------------------------------------------------
// Tiện ích Android nhỏ dùng riêng cho màn này
// ---------------------------------------------------------------------------------------

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/**
 * `LocalContext` trong Compose chưa chắc là Activity — có khi là một lớp vỏ bọc quanh nó. Lần
 * ngược qua các lớp vỏ cho tới khi gặp Activity.
 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Mở trang thông tin của chính app này trong Cài đặt của máy (nơi bật / tắt quyền). */
private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
    )
}

// ---------------------------------------------------------------------------------------
// Preview — không có camera thật nên khung ngắm được thay bằng một ô màu
// ---------------------------------------------------------------------------------------

@Composable
private fun PreviewHost(state: CaptureState) {
    OnTapTldTheme(themeMode = ThemeMode.DARK) {
        CaptureContent(
            state = state,
            photo = null,
            torchAvailable = true,
            cameraPreview = { modifier -> Box(modifier.background(Color(0xFF3A3128))) },
            onClose = {},
            onCropChange = {},
            onPermissionAction = {},
            onGalleryClick = {},
            onShutterClick = {},
            onTorchToggle = {},
            onRetakeClick = {},
            onConfirmClick = {},
        )
    }
}

@Preview(name = "Chụp — đang ngắm", widthDp = 390, heightDp = 844)
@Composable
private fun CaptureCameraPreview() =
    PreviewHost(CaptureState(permission = CameraPermission.Granted))

@Preview(name = "Chụp — đèn bật", widthDp = 390, heightDp = 844)
@Composable
private fun CaptureTorchPreview() =
    PreviewHost(CaptureState(permission = CameraPermission.Granted, torchOn = true))

@Preview(name = "Chụp — chưa có quyền camera", widthDp = 390, heightDp = 844)
@Composable
private fun CapturePermissionPreview() =
    PreviewHost(CaptureState(permission = CameraPermission.Denied))

@Preview(name = "Chụp — phải bật quyền trong Cài đặt", widthDp = 390, heightDp = 844)
@Composable
private fun CapturePermissionForeverPreview() =
    PreviewHost(CaptureState(permission = CameraPermission.DeniedForever))
