package com.ledinhthi.ontaptld.core.data.ai

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.APINotConfiguredException
import com.google.firebase.ai.type.ContentBlockedException
import com.google.firebase.ai.type.FirebaseAIException
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ServiceDisabledException
import com.google.firebase.ai.type.UnsupportedUserLocationException
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.ledinhthi.ontaptld.core.exception.AiErrorKind
import com.ledinhthi.ontaptld.core.exception.AppException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.channels.UnresolvedAddressException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gọi Gemini qua Firebase AI Logic. App KHÔNG giữ API key: yêu cầu đi qua Firebase, và App Check
 * xác nhận nó đến từ đúng app này (bản debug dùng debug token đã đăng ký trên Firebase Console).
 */
@Singleton
class FirebaseGeminiClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) : GeminiClient {

    // `by lazy`: chỉ dựng model ở lần gọi đầu, khi Firebase chắc chắn đã khởi tạo xong.
    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                // Bắt AI trả về JSON đúng khuôn [cardsSchema] thay vì văn xuôi — nhờ đó đọc kết
                // quả bằng code được, không phải "đoán" đâu là câu hỏi, đâu là câu trả lời.
                responseMimeType = "application/json"
                responseSchema = cardsSchema
                // Nhiệt độ thấp = ít "bay bổng": cùng một ghi chú cho ra thẻ ổn định, bám nội dung.
                temperature = 0.2f
            },
            systemInstruction = content { text(FlashcardPrompt.SystemInstruction) },
            requestOptions = RequestOptions(TIMEOUT_MILLIS),
        )
    }

    override suspend fun generateFlashcards(noteText: String, maxCards: Int): List<AiFlashcardDto> {
        // Máy build không có google-services.json thì Firebase không được khởi tạo (xem OnTapTldApp).
        if (FirebaseApp.getApps(context).isEmpty()) {
            throw AppException.AiException(AiErrorKind.NOT_CONFIGURED)
        }
        val response = try {
            model.generateContent(FlashcardPrompt.userPrompt(noteText, maxCards))
        } catch (e: FirebaseAIException) {
            throw e.toAiException()
        }
        // `takeIf { … }` giữ lại giá trị nếu điều kiện đúng, ngược lại trả null.
        val raw = response.text?.takeIf { it.isNotBlank() }
            ?: throw AppException.AiException(AiErrorKind.EMPTY_RESPONSE)
        return try {
            json.decodeFromString<List<AiFlashcardDto>>(raw)
        } catch (e: SerializationException) {
            throw AppException.AiException(AiErrorKind.RESPONSE_PARSE_ERROR, cause = e)
        }
    }

    private companion object {
        /**
         * Dòng Flash-Lite: gói miễn phí cho 500 lượt/ngày, trong khi các model Flash chỉ 20
         * (bảng Rate limits trong AI Studio, 8/10/2026). Việc soạn thẻ không cần model mạnh hơn.
         */
        const val MODEL_NAME = "gemini-3.5-flash-lite"

        /** Chờ tối đa bấy lâu; mặc định của thư viện là 3 phút, quá dài với một màn chờ. */
        const val TIMEOUT_MILLIS = 45_000L

        /** Khuôn JSON: một danh sách, mỗi phần tử gồm câu hỏi, câu trả lời và số dòng nguồn. */
        val cardsSchema: Schema = Schema.array(
            Schema.obj(
                properties = mapOf(
                    "question" to Schema.string(),
                    "answer" to Schema.string(),
                    "sourceLine" to Schema.integer(),
                ),
                optionalProperties = listOf("sourceLine"),
            ),
        )
    }
}

/**
 * Đổi lỗi của thư viện Firebase sang loại lỗi của app. Phần còn lại của app (màn hình, bộ xử lý
 * lỗi chung) chỉ biết `AiErrorKind`, không phụ thuộc vào thư viện.
 */
private fun FirebaseAIException.toAiException(): AppException.AiException {
    val kind = when (this) {
        is QuotaExceededException -> AiErrorKind.QUOTA_EXCEEDED_SERVER
        is RequestTimeoutException -> AiErrorKind.NETWORK
        is PromptBlockedException, is ContentBlockedException -> AiErrorKind.CONTENT_BLOCKED
        // AI dừng giữa chừng: do bộ lọc an toàn, hoặc câu trả lời bị cắt cụt nên không dùng được.
        is ResponseStoppedException ->
            if (message.orEmpty().contains("SAFETY", ignoreCase = true)) {
                AiErrorKind.CONTENT_BLOCKED
            } else {
                AiErrorKind.RESPONSE_PARSE_ERROR
            }

        is com.google.firebase.ai.type.SerializationException -> AiErrorKind.RESPONSE_PARSE_ERROR
        is InvalidAPIKeyException, is APINotConfiguredException, is ServiceDisabledException ->
            AiErrorKind.NOT_CONFIGURED

        is UnsupportedUserLocationException -> AiErrorKind.MODEL_UNAVAILABLE
        // Các lỗi còn lại không có lớp riêng: phải nhìn vào nguyên nhân gốc và lời báo lỗi.
        else -> when {
            isNetworkFailure() -> AiErrorKind.NETWORK
            message.orEmpty().contains("App Check", ignoreCase = true) -> AiErrorKind.APP_CHECK_FAILED
            else -> AiErrorKind.UNKNOWN
        }
    }
    return AppException.AiException(kind = kind, cause = this)
}

/** Lần theo chuỗi "lỗi này do lỗi kia gây ra" xem có lỗi mạng nào (mất mạng, không phân giải được tên miền) không. */
private fun Throwable.isNetworkFailure(): Boolean =
    // `generateSequence(a) { tiếp theo }` tạo dãy a, a.cause, a.cause.cause… cho tới khi gặp null.
    generateSequence(this) { it.cause }
        .take(10) // phòng hai lỗi trỏ vòng vào nhau
        .any { it is IOException || it is UnresolvedAddressException }
