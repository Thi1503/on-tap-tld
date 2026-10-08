package com.ledinhthi.ontaptld.feature.capture

import androidx.lifecycle.SavedStateHandle
import com.ledinhthi.ontaptld.core.MainDispatcherRule
import com.ledinhthi.ontaptld.core.exception.AiErrorKind
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.GlobalExceptionHandler
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.text.StringProvider
import com.ledinhthi.ontaptld.feature.capture.domain.model.SuggestedCard
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.GenerateFlashcardsWithAiUseCase
import com.ledinhthi.ontaptld.feature.capture.presentation.aicards.AiCardsPhase
import com.ledinhthi.ontaptld.feature.capture.presentation.aicards.AiCardsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiCardsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val generateFlashcards = mockk<GenerateFlashcardsWithAiUseCase>()
    private val navigator = mockk<AppNavigator>(relaxed = true)
    private val exceptionHandler = mockk<GlobalExceptionHandler>(relaxed = true)
    private val strings = mockk<StringProvider>(relaxed = true)
    private val toolbox = ViewModelToolbox(navigator, exceptionHandler, strings)

    private val cards = listOf(
        SuggestedCard("Nhân đôi ADN diễn ra ở pha nào?", "Pha S.", sourceLine = 1),
        SuggestedCard("Đoạn Okazaki là gì?", "Đoạn ADN ngắn trên mạch chậm.", sourceLine = 4),
    )

    private fun viewModel() = AiCardsViewModel(
        toolbox = toolbox,
        savedState = SavedStateHandle(
            mapOf("imagePath" to "/cache/crop.jpg", "noteText" to "Bài 1 · Nhân đôi ADN", "deckId" to "d1"),
        ),
        generateFlashcards = generateFlashcards,
    ).apply { isTestMode = true }

    @Test
    fun `mo man - gui van ban cho AI, co the thi sang chang duyet`() = runTest {
        coEvery { generateFlashcards.invoke("Bài 1 · Nhân đôi ADN") } returns cards

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(AiCardsPhase.Suggestions, vm.uiState.value.phase)
        assertEquals(cards, vm.uiState.value.cards)
    }

    @Test
    fun `dang cho AI - o chang dang tao, chua co the nao`() = runTest {
        val pending = CompletableDeferred<List<SuggestedCard>>()
        coEvery { generateFlashcards.invoke(any()) } coAnswers { pending.await() }

        val vm = viewModel()

        assertEquals(AiCardsPhase.Generating, vm.uiState.value.phase)
        assertTrue(vm.uiState.value.cards.isEmpty())
    }

    @Test
    fun `huy luc dang cho - lui ve buoc 2, ket qua ve sau khong duoc dung`() = runTest {
        val pending = CompletableDeferred<List<SuggestedCard>>()
        coEvery { generateFlashcards.invoke(any()) } coAnswers { pending.await() }
        val vm = viewModel()

        vm.onCancel()
        pending.complete(cards) // AI trả lời sau khi người dùng đã huỷ
        advanceUntilIdle()

        verify(exactly = 1) { navigator.back() }
        assertEquals(AiCardsPhase.Generating, vm.uiState.value.phase)
        assertTrue(vm.uiState.value.cards.isEmpty())
        verify(exactly = 0) { exceptionHandler.handle(any()) }
    }

    @Test
    fun `goi AI loi - lui ve buoc 2 va de bo xu ly loi chung bao`() = runTest {
        coEvery { generateFlashcards.invoke(any()) } throws AppException.AiException(AiErrorKind.NETWORK)

        val vm = viewModel()
        advanceUntilIdle()

        verify { navigator.back() }
        verify { exceptionHandler.handle(match { (it.exception as? AppException.AiException)?.kind == AiErrorKind.NETWORK }) }
        assertEquals(AiCardsPhase.Generating, vm.uiState.value.phase)
        coVerify(exactly = 1) { generateFlashcards.invoke(any()) }
    }
}
