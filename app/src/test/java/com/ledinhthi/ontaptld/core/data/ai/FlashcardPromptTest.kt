package com.ledinhthi.ontaptld.core.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashcardPromptTest {

    @Test
    fun `dong ghi chu - bo dong trong va khoang trang thua`() {
        val text = "  Bài 1 · Nhân đôi ADN  \n\n– pha S\n   \n– bán bảo toàn\n"

        assertEquals(listOf("Bài 1 · Nhân đôi ADN", "– pha S", "– bán bảo toàn"), FlashcardPrompt.noteLines(text))
    }

    @Test
    fun `loi yeu cau - danh so dong tu 1 va neu so the toi da`() {
        val prompt = FlashcardPrompt.userPrompt("Dòng đầu\n\nDòng hai", maxCards = 7)

        assertTrue(prompt.contains("at most 7 flashcards"))
        assertTrue(prompt.endsWith("1| Dòng đầu\n2| Dòng hai"))
    }
}
