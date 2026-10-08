package com.ledinhthi.ontaptld.feature.capture

import com.ledinhthi.ontaptld.core.data.ai.AiFlashcardDto
import com.ledinhthi.ontaptld.core.data.ai.AiQuotaGuard
import com.ledinhthi.ontaptld.core.data.ai.GeminiClient
import com.ledinhthi.ontaptld.core.exception.AiErrorKind
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.feature.capture.data.GeminiCardSuggestionRepository
import com.ledinhthi.ontaptld.feature.capture.domain.model.SuggestedCard
import com.ledinhthi.ontaptld.feature.capture.domain.repository.CardSuggestionRepository
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.GenerateFlashcardsWithAiUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/** Kiểm thử hai lớp nằm giữa màn hình và Gemini: lớp làm sạch kết quả và lớp tính lượt. */
class AiCardGenerationTest {

    // ---- GeminiCardSuggestionRepository: làm sạch thẻ AI trả về ----

    private val gemini = mockk<GeminiClient>()
    private val suggestionRepository = GeminiCardSuggestionRepository(gemini)

    @Test
    fun `lam sach the AI - bo khoang trang, bo the thieu noi dung, bo so dong khong ton tai`() = runTest {
        // Ghi chú có 2 dòng có chữ, nên số dòng hợp lệ là 1 và 2.
        coEvery { gemini.generateFlashcards(any(), any()) } returns listOf(
            AiFlashcardDto("  Câu 1?  ", " Đáp 1 ", sourceLine = 2),
            AiFlashcardDto("Câu 2?", "Đáp 2", sourceLine = 9),
            AiFlashcardDto("Câu 3?", "Đáp 3", sourceLine = 0),
            AiFlashcardDto("   ", "Đáp không có câu hỏi", sourceLine = 1),
            AiFlashcardDto("Câu không có đáp?", "", sourceLine = 1),
        )

        val cards = suggestionRepository.suggestCards("Dòng một\n\nDòng hai")

        assertEquals(
            listOf(
                SuggestedCard("Câu 1?", "Đáp 1", sourceLine = 2),
                SuggestedCard("Câu 2?", "Đáp 2", sourceLine = null),
                SuggestedCard("Câu 3?", "Đáp 3", sourceLine = null),
            ),
            cards,
        )
    }

    @Test
    fun `AI tra nhieu hon so da dan - chi lay 10 the dau`() = runTest {
        coEvery { gemini.generateFlashcards(any(), any()) } returns
            List(14) { AiFlashcardDto("Câu $it?", "Đáp $it") }

        val cards = suggestionRepository.suggestCards("Một dòng")

        assertEquals(10, cards.size)
        coVerify { gemini.generateFlashcards("Một dòng", 10) }
    }

    // ---- GenerateFlashcardsWithAiUseCase: tính lượt dùng ----

    private val repository = mockk<CardSuggestionRepository>()
    private val quotaGuard = mockk<AiQuotaGuard>(relaxed = true)
    private val generate = GenerateFlashcardsWithAiUseCase(repository, quotaGuard)

    @Test
    fun `tao the thanh cong - kiem tra luot truoc, goi AI, roi moi tru luot`() = runTest {
        val cards = listOf(SuggestedCard("Q", "A", 1))
        coEvery { repository.suggestCards("ghi chú") } returns cards

        assertEquals(cards, generate("ghi chú"))

        coVerifyOrder {
            quotaGuard.ensureCanCall()
            repository.suggestCards("ghi chú")
            quotaGuard.recordCall()
        }
    }

    @Test
    fun `het luot hom nay - bao loi ngay, khong goi AI`() = runTest {
        coEvery { quotaGuard.ensureCanCall() } throws
            AppException.AiException(AiErrorKind.QUOTA_EXCEEDED_LOCAL)

        assertAiError(AiErrorKind.QUOTA_EXCEEDED_LOCAL) { generate("ghi chú") }

        coVerify(exactly = 0) { repository.suggestCards(any()) }
    }

    @Test
    fun `mat mang khi goi AI - khong tru luot`() = runTest {
        coEvery { repository.suggestCards(any()) } throws AppException.AiException(AiErrorKind.NETWORK)

        assertAiError(AiErrorKind.NETWORK) { generate("ghi chú") }

        coVerify(exactly = 0) { quotaGuard.recordCall() }
    }

    @Test
    fun `AI khong soan duoc the nao - bao EMPTY_RESPONSE, khong tru luot`() = runTest {
        coEvery { repository.suggestCards(any()) } returns emptyList()

        assertAiError(AiErrorKind.EMPTY_RESPONSE) { generate("ghi chú") }

        coVerify(exactly = 0) { quotaGuard.recordCall() }
    }

    /** Chạy [block] và đòi nó phải ném `AiException` đúng loại [expected]. */
    private suspend fun assertAiError(expected: AiErrorKind, block: suspend () -> Unit) {
        try {
            block()
            fail("Phải ném AiException($expected)")
        } catch (e: AppException.AiException) {
            assertEquals(expected, e.kind)
        }
    }
}
