package com.ledinhthi.ontaptld.feature.deck

import com.ledinhthi.ontaptld.core.MainDispatcherRule
import com.ledinhthi.ontaptld.core.exception.GlobalExceptionHandler
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.CreateDeckUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveDecksUseCase
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.DeckListViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeckListViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val observeDecks = mockk<ObserveDecksUseCase>()
    private val createDeck = mockk<CreateDeckUseCase>()
    private val navigator = mockk<AppNavigator>(relaxed = true)
    private val exceptionHandler = mockk<GlobalExceptionHandler>(relaxed = true)
    private val toolbox = ViewModelToolbox(navigator, exceptionHandler)

    @Test
    fun `tao deck ten rong - hien snackbar, khong goi ExceptionHandler`() = runTest {
        every { observeDecks.invoke() } returns flowOf(emptyList())
        coEvery { createDeck.invoke(any()) } throws DeckException(DeckException.Kind.BLANK_NAME)

        val vm = DeckListViewModel(toolbox, observeDecks, createDeck).apply { isTestMode = true }
        vm.onCreateDeck(name = "  ", colorHex = "#fff")
        advanceUntilIdle()

        verify { navigator.showSnackBar(any(), any()) }
        verify(exactly = 0) { exceptionHandler.handle(any()) }
    }
}
