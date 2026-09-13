package com.ledinhthi.ontaptld.core.data.ai

import com.ledinhthi.ontaptld.core.exception.AiErrorKind
import com.ledinhthi.ontaptld.core.exception.AppException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder Sprint 1: chưa gắn Firebase AI Logic (repo chưa có google-services.json).
 *
 * Khi bật Firebase: thêm `firebase-ai` + `google-services` plugin, tạo `FirebaseGeminiClient`
 * theo docs Mục 7.4, rồi đổi `@Binds` trong [com.ledinhthi.ontaptld.core.di.CoreBindsModule].
 */
@Singleton
class StubGeminiClient @Inject constructor() : GeminiClient {
    override suspend fun generateFlashcards(ocrText: String, maxCards: Int): List<AiFlashcardDto> {
        throw AppException.AiException(AiErrorKind.NOT_CONFIGURED)
    }
}
