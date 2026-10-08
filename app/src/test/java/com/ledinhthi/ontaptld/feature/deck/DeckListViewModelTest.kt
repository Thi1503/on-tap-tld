package com.ledinhthi.ontaptld.feature.deck

import app.cash.turbine.test
import com.ledinhthi.ontaptld.core.MainDispatcherRule
import com.ledinhthi.ontaptld.core.exception.GlobalExceptionHandler
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.text.StringProvider
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.model.DeckSummary
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.CreateDeckUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveDeckSummariesUseCase
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.DeckListEffect
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.DeckListViewModel
import com.ledinhthi.ontaptld.navigation.ManualCardRoute
import com.ledinhthi.ontaptld.navigation.ReviewRoute
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeckListViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val observeDecks = mockk<ObserveDeckSummariesUseCase>()
    private val createDeck = mockk<CreateDeckUseCase>()
    private val navigator = mockk<AppNavigator>(relaxed = true)
    private val exceptionHandler = mockk<GlobalExceptionHandler>(relaxed = true)
    private val strings = mockk<StringProvider>(relaxed = true)
    private val toolbox = ViewModelToolbox(navigator, exceptionHandler, strings)

    private fun deck(id: String) = Deck(id, "Deck $id", "#4F46E5", createdAt = 1, updatedAt = 1)

    private fun viewModel() = DeckListViewModel(toolbox, observeDecks, createDeck).apply { isTestMode = true }

    @Test
    fun `tao deck ten rong - hien snackbar, khong goi ExceptionHandler`() = runTest {
        every { observeDecks.invoke() } returns flowOf(emptyList())
        coEvery { createDeck.invoke(any()) } throws DeckException(DeckException.Kind.BLANK_NAME)

        val vm = viewModel()
        vm.onCreateDeck(name = "  ", colorHex = "#fff")
        advanceUntilIdle()

        verify { navigator.showSnackBar(any(), any()) }
        verify(exactly = 0) { exceptionHandler.handle(any()) }
    }

    @Test
    fun `nhan du lieu - tinh tong the can on va xep bo co the can on theo thu tu giam dan`() = runTest {
        every { observeDecks.invoke() } returns flowOf(
            listOf(
                DeckSummary(deck("a"), cardCount = 10, dueCount = 2),
                DeckSummary(deck("b"), cardCount = 4, dueCount = 0),
                DeckSummary(deck("c"), cardCount = 20, dueCount = 7),
            ),
        )

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isLoaded)
        assertEquals(9, state.dueCount)
        assertEquals(34, state.cardCount)
        assertEquals(listOf("c", "a"), state.dueDecks.map { it.deck.id })
    }

    @Test
    fun `doc du lieu loi - bao loadFailed, thu lai thanh cong thi het loi`() = runTest {
        every { observeDecks.invoke() } returns flow { throw IllegalStateException("db") }

        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.loadFailed)

        every { observeDecks.invoke() } returns flowOf(emptyList())
        vm.onRetry()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.loadFailed)
        assertTrue(vm.uiState.value.isLoaded)
    }

    @Test
    fun `bam go the khi chua co bo the - mo bang tao bo the, tao xong vao thang man them the`() = runTest {
        every { observeDecks.invoke() } returns flowOf(emptyList())
        coEvery { createDeck.invoke(any()) } returns deck("moi")

        val vm = viewModel()
        advanceUntilIdle()

        vm.effect.test {
            vm.onManualCardClick()
            assertEquals(DeckListEffect.OpenCreateDeck, awaitItem())
        }
        vm.onCreateDeck(name = "Hoá học", colorHex = "#B45309")
        advanceUntilIdle()

        verify { navigator.to(ManualCardRoute("moi")) }
    }

    @Test
    fun `bam Bo moi roi tao - o lai Home, khong tu chuyen man`() = runTest {
        every { observeDecks.invoke() } returns flowOf(emptyList())
        coEvery { createDeck.invoke(any()) } returns deck("moi")

        val vm = viewModel()
        vm.onNewDeckClick()
        vm.onCreateDeck(name = "Hoá học", colorHex = "#B45309")
        advanceUntilIdle()

        verify(exactly = 0) { navigator.to(any()) }
    }

    @Test
    fun `bam On ngay - mo phien on cho moi bo the`() = runTest {
        every { observeDecks.invoke() } returns flowOf(emptyList())

        viewModel().onReviewClick()

        verify { navigator.to(ReviewRoute(deckId = null)) }
    }

    @Test
    fun `bam go the khi da co bo the - vao man them the cua bo dung dau danh sach`() = runTest {
        every { observeDecks.invoke() } returns flowOf(
            listOf(DeckSummary(deck("gan-nhat")), DeckSummary(deck("cu-hon"))),
        )

        val vm = viewModel()
        advanceUntilIdle()
        vm.onManualCardClick()

        verify { navigator.to(ManualCardRoute("gan-nhat")) }
    }
}
