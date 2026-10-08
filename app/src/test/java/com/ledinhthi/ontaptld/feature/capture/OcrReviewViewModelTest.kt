package com.ledinhthi.ontaptld.feature.capture

import androidx.lifecycle.SavedStateHandle
import com.ledinhthi.ontaptld.core.MainDispatcherRule
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.GlobalExceptionHandler
import com.ledinhthi.ontaptld.core.exception.OcrErrorKind
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.text.StringProvider
import com.ledinhthi.ontaptld.feature.capture.domain.model.AiQuota
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.ObserveAiQuotaUseCase
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.RunOcrUseCase
import com.ledinhthi.ontaptld.feature.capture.presentation.CaptureFlowEvents
import com.ledinhthi.ontaptld.feature.capture.presentation.ocrreview.OcrReviewViewModel
import com.ledinhthi.ontaptld.feature.capture.presentation.ocrreview.OcrStatus
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.CreateDeckUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveDecksUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OcrReviewViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val observeDecks = mockk<ObserveDecksUseCase>()
    private val observeAiQuota = mockk<ObserveAiQuotaUseCase>()
    private val runOcr = mockk<RunOcrUseCase>()
    private val createDeck = mockk<CreateDeckUseCase>()
    private val flowEvents = mockk<CaptureFlowEvents>(relaxed = true)
    private val navigator = mockk<AppNavigator>(relaxed = true)
    private val exceptionHandler = mockk<GlobalExceptionHandler>(relaxed = true)
    private val strings = mockk<StringProvider>(relaxed = true)
    private val toolbox = ViewModelToolbox(navigator, exceptionHandler, strings)

    // Danh sách xếp theo lần dùng gần nhất: d1 là bộ vừa làm việc.
    private val decks = listOf(
        Deck("d1", "IELTS", "#4F46E5", createdAt = 1, updatedAt = 2),
        Deck("d2", "Sinh học", "#0D9488", createdAt = 1, updatedAt = 1),
    )

    /** [deckId] có giá trị = luồng chụp được mở từ Chi tiết bộ thẻ đó. */
    private fun viewModel(
        deckId: String? = null,
        deckFlow: Flow<List<Deck>> = flowOf(decks),
    ): OcrReviewViewModel {
        every { observeDecks.invoke() } returns deckFlow
        every { observeAiQuota.invoke() } returns flowOf(AiQuota(remaining = 8, max = 10))
        return OcrReviewViewModel(
            toolbox = toolbox,
            savedState = SavedStateHandle(mapOf("imagePath" to "/cache/crop.jpg", "deckId" to deckId)),
            observeDecks = observeDecks,
            observeAiQuota = observeAiQuota,
            runOcr = runOcr,
            createDeck = createDeck,
            flowEvents = flowEvents,
        ).apply { isTestMode = true }
    }

    @Test
    fun `mo man - nhan dang chu tren anh da cat roi dien vao o van ban`() = runTest {
        coEvery { runOcr.invoke("/cache/crop.jpg") } returns "Nhân đôi ADN"

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(OcrStatus.Found, state.ocrStatus)
        assertEquals("Nhân đôi ADN", state.text)
        assertFalse(state.hasEditedText)
        assertEquals(AiQuota(8, 10), state.quota)
        assertTrue(state.canGenerate)
    }

    @Test
    fun `dang nhan dang - chua tao the duoc`() = runTest {
        // CompletableDeferred chưa có kết quả = việc nhận dạng "treo" ở giữa chừng.
        val pending = CompletableDeferred<String>()
        coEvery { runOcr.invoke(any()) } coAnswers { pending.await() }

        val vm = viewModel()

        assertEquals(OcrStatus.Running, vm.uiState.value.ocrStatus)
        assertFalse(vm.uiState.value.canGenerate)

        pending.complete("Xong")
        advanceUntilIdle()
        assertEquals(OcrStatus.Found, vm.uiState.value.ocrStatus)
    }

    @Test
    fun `anh khong co chu - bao ngay trong man, cho tu go, khong bat thong bao nao`() = runTest {
        coEvery { runOcr.invoke(any()) } throws AppException.OcrException(OcrErrorKind.NO_TEXT_FOUND)

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(OcrStatus.NoText, vm.uiState.value.ocrStatus)
        assertFalse(vm.uiState.value.canGenerate)
        verify(exactly = 0) { exceptionHandler.handle(any()) }
        verify(exactly = 0) { navigator.showSnackBar(any(), any()) }

        vm.onTextChange("Tự gõ ghi chú")
        assertTrue(vm.uiState.value.canGenerate)
        assertTrue(vm.uiState.value.hasEditedText)
    }

    @Test
    fun `bo nhan dang loi - cung bao trong man voi trang thai rieng`() = runTest {
        coEvery { runOcr.invoke(any()) } throws AppException.OcrException(OcrErrorKind.RECOGNITION_FAILED)

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(OcrStatus.Failed, vm.uiState.value.ocrStatus)
        verify(exactly = 0) { exceptionHandler.handle(any()) }
    }

    @Test
    fun `sua van ban - tinh la da sua, go lai dung nhu cu thi khong`() = runTest {
        coEvery { runOcr.invoke(any()) } returns "Dòng một"
        val vm = viewModel()
        advanceUntilIdle()

        vm.onTextChange("Dòng một đã sửa")
        assertTrue(vm.uiState.value.hasEditedText)

        vm.onTextChange("Dòng một  ") // chỉ thêm dấu cách ở cuối
        assertFalse(vm.uiState.value.hasEditedText)

        vm.onTextChange("   ")
        assertFalse(vm.uiState.value.canGenerate)
    }

    @Test
    fun `bo the mac dinh - mo tu Home lay bo vua dung, mo tu Chi tiet bo the lay bo do`() = runTest {
        coEvery { runOcr.invoke(any()) } returns "x"

        val fromHome = viewModel()
        advanceUntilIdle()
        assertEquals("d1", fromHome.uiState.value.selectedDeck?.id)

        val fromDeck = viewModel(deckId = "d2")
        advanceUntilIdle()
        assertEquals("d2", fromDeck.uiState.value.selectedDeck?.id)

        fromDeck.onDeckSelected("d1")
        assertEquals("d1", fromDeck.uiState.value.selectedDeck?.id)
    }

    @Test
    fun `chua co bo the nao - chua tao the duoc, tao bo moi xong thi chon luon bo do`() = runTest {
        coEvery { runOcr.invoke(any()) } returns "Có chữ"
        // MutableStateFlow đóng vai database: đổi `value` là mọi nơi đang nghe nhận danh sách mới.
        val deckFlow = MutableStateFlow(emptyList<Deck>())
        val vm = viewModel(deckFlow = deckFlow)
        advanceUntilIdle()
        assertNull(vm.uiState.value.selectedDeck)
        assertFalse(vm.uiState.value.canGenerate)

        val newDeck = Deck("moi", "Lịch sử", "#DB2777", createdAt = 5, updatedAt = 5)
        coEvery { createDeck.invoke(CreateDeckUseCase.Params("Lịch sử", "#DB2777")) } coAnswers {
            deckFlow.value = listOf(newDeck)
            newDeck
        }
        vm.onCreateDeck("Lịch sử", "#DB2777")
        advanceUntilIdle()

        assertEquals("moi", vm.uiState.value.selectedDeck?.id)
        assertTrue(vm.uiState.value.canGenerate)
    }

    @Test
    fun `tao bo the ten rong - bao bang snackbar, giu nguyen bo dang chon`() = runTest {
        coEvery { runOcr.invoke(any()) } returns "x"
        coEvery { createDeck.invoke(any()) } throws DeckException(DeckException.Kind.BLANK_NAME)
        val vm = viewModel()
        advanceUntilIdle()

        vm.onCreateDeck("  ", "#4F46E5")
        advanceUntilIdle()

        verify { navigator.showSnackBar(any(), any()) }
        verify(exactly = 0) { exceptionHandler.handle(any()) }
        assertEquals("d1", vm.uiState.value.selectedDeck?.id)
    }

    @Test
    fun `nut quay lai - chi lui mot buoc, khong yeu cau chup lai`() = runTest {
        coEvery { runOcr.invoke(any()) } returns "x"
        val vm = viewModel()
        advanceUntilIdle()

        vm.onBack()

        verify { navigator.back() }
        verify(exactly = 0) { flowEvents.requestRetake() }
    }

    @Test
    fun `chup lai - nhan man chup truoc roi moi lui ve`() = runTest {
        coEvery { runOcr.invoke(any()) } returns "x"
        val vm = viewModel()
        advanceUntilIdle()

        vm.onRetake()

        verifyOrder {
            flowEvents.requestRetake()
            navigator.back()
        }
    }
}
