package com.ledinhthi.ontaptld.feature.capture

import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.OcrErrorKind
import com.ledinhthi.ontaptld.feature.capture.domain.model.OcrLine
import com.ledinhthi.ontaptld.feature.capture.domain.repository.OcrRepository
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.RunOcrUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class RunOcrUseCaseTest {

    private val repository = mockk<OcrRepository>()
    private val runOcr = RunOcrUseCase(repository)

    @Test
    fun `co chu - ghep cac dong thanh van ban, bo khoang trang thua hai dau`() = runTest {
        coEvery { repository.recognizeLines("/a.jpg") } returns listOf(
            OcrLine("  Nhân đôi ADN", box = null),
            OcrLine("– pha S  ", box = null),
        )

        assertEquals("Nhân đôi ADN\n– pha S", runOcr("/a.jpg"))
    }

    @Test
    fun `anh khong co chu - nem loi NO_TEXT_FOUND`() = runTest {
        coEvery { repository.recognizeLines(any()) } returns emptyList()

        try {
            runOcr("/a.jpg")
            fail("Phải ném OcrException")
        } catch (e: AppException.OcrException) {
            assertEquals(OcrErrorKind.NO_TEXT_FOUND, e.kind)
        }
    }
}
