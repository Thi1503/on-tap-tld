package com.ledinhthi.ontaptld.feature.capture

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ledinhthi.ontaptld.core.MainDispatcherRule
import com.ledinhthi.ontaptld.core.exception.GlobalExceptionHandler
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.text.StringProvider
import com.ledinhthi.ontaptld.feature.capture.domain.exception.CaptureException
import com.ledinhthi.ontaptld.feature.capture.domain.model.CropRect
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.CropNoteImageUseCase
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.ImportGalleryImageUseCase
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.PreparePhotoFileUseCase
import com.ledinhthi.ontaptld.feature.capture.presentation.capture.CameraPermission
import com.ledinhthi.ontaptld.feature.capture.presentation.capture.CaptureEffect
import com.ledinhthi.ontaptld.feature.capture.presentation.capture.CapturePhase
import com.ledinhthi.ontaptld.feature.capture.presentation.capture.CaptureViewModel
import com.ledinhthi.ontaptld.navigation.OcrReviewRoute
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val preparePhotoFile = mockk<PreparePhotoFileUseCase>()
    private val importGalleryImage = mockk<ImportGalleryImageUseCase>()
    private val cropNoteImage = mockk<CropNoteImageUseCase>()
    private val navigator = mockk<AppNavigator>(relaxed = true)
    private val exceptionHandler = mockk<GlobalExceptionHandler>(relaxed = true)
    private val strings = mockk<StringProvider>(relaxed = true)
    private val toolbox = ViewModelToolbox(navigator, exceptionHandler, strings)

    /** [deckId] có giá trị = mở màn chụp từ Chi tiết bộ thẻ. */
    private fun viewModel(deckId: String? = null) = CaptureViewModel(
        toolbox = toolbox,
        savedState = SavedStateHandle(mapOf("deckId" to deckId)),
        preparePhotoFile = preparePhotoFile,
        importGalleryImage = importGalleryImage,
        cropNoteImage = cropNoteImage,
    ).apply { isTestMode = true }

    /** ViewModel đã có quyền camera — điểm xuất phát của đa số ca kiểm thử. */
    private fun readyViewModel(deckId: String? = null) =
        viewModel(deckId).apply { onCameraPermissionResult(granted = true) }

    @Test
    fun `ket qua xin quyen - duoc cap, bi tu choi, bi tu choi han`() {
        val vm = viewModel()
        assertEquals(CameraPermission.Unknown, vm.uiState.value.permission)

        vm.onCameraPermissionResult(granted = false, canAskAgain = true)
        assertEquals(CameraPermission.Denied, vm.uiState.value.permission)

        vm.onCameraPermissionResult(granted = false, canAskAgain = false)
        assertEquals(CameraPermission.DeniedForever, vm.uiState.value.permission)

        vm.onCameraPermissionResult(granted = true)
        assertEquals(CameraPermission.Granted, vm.uiState.value.permission)
        assertTrue(vm.uiState.value.canShoot)
    }

    @Test
    fun `bam chup khi chua co quyen - khong lam gi`() = runTest {
        val vm = viewModel()

        vm.onShutterClick()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isCapturing)
        coVerify(exactly = 0) { preparePhotoFile.invoke() }
    }

    @Test
    fun `bam chup - nho man hinh chup vao file moi va khoa nut chup`() = runTest {
        coEvery { preparePhotoFile.invoke() } returns "/cache/photo.jpg"
        val vm = readyViewModel()

        vm.effect.test {
            vm.onShutterClick()
            assertEquals(CaptureEffect.TakePicture("/cache/photo.jpg"), awaitItem())
        }
        assertTrue(vm.uiState.value.isCapturing)

        vm.onShutterClick() // bấm lần nữa khi ảnh trước chưa ghi xong
        advanceUntilIdle()
        coVerify(exactly = 1) { preparePhotoFile.invoke() }
    }

    @Test
    fun `chup xong - sang chang chinh khung, giu nguyen khung da dat, tat den`() = runTest {
        coEvery { preparePhotoFile.invoke() } returns "/cache/photo.jpg"
        val vm = readyViewModel()
        val myCrop = CropRect(0.1f, 0.1f, 0.5f, 0.5f)
        vm.onCropChange(myCrop)
        vm.onTorchToggle()
        vm.onShutterClick()

        vm.onPictureSaved("/cache/photo.jpg")

        val state = vm.uiState.value
        assertEquals(CapturePhase.Adjust, state.phase)
        assertEquals("/cache/photo.jpg", state.photoPath)
        assertEquals(myCrop, state.crop)
        assertFalse(state.torchOn)
        assertFalse(state.isCapturing)
    }

    @Test
    fun `chup loi - bao loi, van o chang camera va chup lai duoc`() = runTest {
        coEvery { preparePhotoFile.invoke() } returns "/cache/photo.jpg"
        val vm = readyViewModel()
        vm.onShutterClick()

        vm.onPictureFailed()

        verify { navigator.showSnackBar(any(), any()) }
        assertEquals(CapturePhase.Camera, vm.uiState.value.phase)
        assertTrue(vm.uiState.value.canShoot)
    }

    @Test
    fun `chon anh tu thu vien - sang chang chinh khung voi khung om gan het anh`() = runTest {
        coEvery { importGalleryImage.invoke("content://media/1") } returns "/cache/picked.jpg"
        val vm = viewModel() // không cần quyền camera để chọn ảnh có sẵn

        vm.onGalleryImagePicked("content://media/1")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(CapturePhase.Adjust, state.phase)
        assertEquals("/cache/picked.jpg", state.photoPath)
        assertEquals(CropRect.GalleryDefault, state.crop)
    }

    @Test
    fun `chon phai file khong doc duoc - bao loi bang snackbar, o lai chang camera`() = runTest {
        coEvery { importGalleryImage.invoke(any()) } throws
            CaptureException(CaptureException.Kind.IMAGE_UNREADABLE)
        val vm = readyViewModel()

        vm.onGalleryImagePicked("content://media/hong")
        advanceUntilIdle()

        verify { navigator.showSnackBar(any(), any()) }
        verify(exactly = 0) { exceptionHandler.handle(any()) }
        assertEquals(CapturePhase.Camera, vm.uiState.value.phase)
        assertNull(vm.uiState.value.photoPath)
    }

    @Test
    fun `chup lai - ve camera, bo anh, khung ve vi tri ban dau`() = runTest {
        coEvery { importGalleryImage.invoke(any()) } returns "/cache/picked.jpg"
        val vm = readyViewModel()
        vm.onGalleryImagePicked("content://media/1")
        advanceUntilIdle()

        vm.onRetake()

        val state = vm.uiState.value
        assertEquals(CapturePhase.Camera, state.phase)
        assertNull(state.photoPath)
        assertEquals(CropRect.CameraDefault, state.crop)
        assertTrue(state.canShoot)
    }

    @Test
    fun `xac nhan khung - cat anh theo khung dang dat roi sang buoc kiem tra van ban`() = runTest {
        coEvery { importGalleryImage.invoke(any()) } returns "/cache/picked.jpg"
        coEvery { cropNoteImage.invoke(any()) } returns "/cache/crop.jpg"
        val vm = readyViewModel(deckId = "d1")
        vm.onGalleryImagePicked("content://media/1")
        advanceUntilIdle()
        val myCrop = CropRect(0.2f, 0.3f, 0.7f, 0.9f)
        vm.onCropChange(myCrop)

        vm.onConfirmCrop()
        advanceUntilIdle()

        coVerify { cropNoteImage.invoke(CropNoteImageUseCase.Params("/cache/picked.jpg", myCrop)) }
        verify { navigator.to(OcrReviewRoute(imagePath = "/cache/crop.jpg", deckId = "d1")) }
    }

    @Test
    fun `cat anh loi - bao loi, khong chuyen man, van chinh khung tiep duoc`() = runTest {
        coEvery { importGalleryImage.invoke(any()) } returns "/cache/picked.jpg"
        coEvery { cropNoteImage.invoke(any()) } throws CaptureException(CaptureException.Kind.SAVE_FAILED)
        val vm = readyViewModel()
        vm.onGalleryImagePicked("content://media/1")
        advanceUntilIdle()

        vm.onConfirmCrop()
        advanceUntilIdle()

        verify { navigator.showSnackBar(any(), any()) }
        verify(exactly = 0) { navigator.to(any()) }
        assertEquals(CapturePhase.Adjust, vm.uiState.value.phase)
    }

    @Test
    fun `bat tat den - doi qua lai`() {
        val vm = readyViewModel()

        vm.onTorchToggle()
        assertTrue(vm.uiState.value.torchOn)
        vm.onTorchToggle()
        assertFalse(vm.uiState.value.torchOn)
    }
}
