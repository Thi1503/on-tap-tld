package com.ledinhthi.ontaptld.feature.auth.presentation.signin

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.ledinhthi.ontaptld.core.exception.AuthErrorKind
import timber.log.Timber

/** Kết quả của việc hỏi người dùng chọn tài khoản Google. */
sealed interface GoogleIdTokenResult {
    /** Người dùng đã chọn tài khoản; [idToken] là "vé" Google cấp để app mang đi đăng nhập. */
    data class Success(val idToken: String) : GoogleIdTokenResult

    /** Người dùng đóng hộp chọn tài khoản — không phải lỗi, không cần báo gì. */
    data object Cancelled : GoogleIdTokenResult

    data class Failure(val kind: AuthErrorKind) : GoogleIdTokenResult
}

/**
 * Mở hộp "Đăng nhập bằng Google" của hệ thống (Credential Manager) và chờ người dùng chọn tài
 * khoản. App không nhìn thấy mật khẩu: Google chỉ trả về một ID token.
 *
 * [activityContext] phải là Activity đang hiện, vì hộp chọn tài khoản mở đè lên nó. Đó là lý do
 * hàm này được gọi từ màn hình (Composable) chứ không nằm trong ViewModel — ViewModel sống lâu
 * hơn Activity nên không được giữ Activity.
 */
suspend fun requestGoogleIdToken(activityContext: Context): GoogleIdTokenResult {
    val webClientId = activityContext.webClientIdOrNull()
        ?: return GoogleIdTokenResult.Failure(AuthErrorKind.NOT_CONFIGURED)

    // "SignInWithGoogle": kiểu dành cho nút "Tiếp tục với Google" do người dùng tự bấm — luôn
    // hiện hộp chọn tài khoản đầy đủ.
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
        .build()

    return try {
        val credential = CredentialManager.create(activityContext)
            .getCredential(activityContext, request)
            .credential
        // Credential Manager trả về nhiều loại "giấy tờ" (mật khẩu, passkey…). Ở đây chỉ nhận
        // đúng loại ID token của Google.
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleIdTokenResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            GoogleIdTokenResult.Failure(AuthErrorKind.UNKNOWN)
        }
    } catch (e: GetCredentialCancellationException) {
        GoogleIdTokenResult.Cancelled
    } catch (e: NoCredentialException) {
        Timber.w(e, "Không có tài khoản Google để chọn")
        GoogleIdTokenResult.Failure(AuthErrorKind.NO_GOOGLE_ACCOUNT)
    } catch (e: GetCredentialException) {
        Timber.w(e, "Credential Manager báo lỗi")
        GoogleIdTokenResult.Failure(AuthErrorKind.UNKNOWN)
    } catch (e: GoogleIdTokenParsingException) {
        Timber.w(e, "Không đọc được ID token của Google")
        GoogleIdTokenResult.Failure(AuthErrorKind.UNKNOWN)
    }
}

/**
 * "Web client ID" cho Google biết ID token được cấp cho project Firebase nào. Plugin
 * google-services sinh chuỗi `default_web_client_id` từ `google-services.json`; máy build không
 * có file đó thì chuỗi không tồn tại. Vì vậy phải tìm theo TÊN lúc chạy (không viết thẳng
 * `R.string.default_web_client_id`, nếu không bản build thiếu file sẽ không biên dịch được).
 */
@SuppressLint("DiscouragedApi")
private fun Context.webClientIdOrNull(): String? {
    val id = resources.getIdentifier("default_web_client_id", "string", packageName)
    return if (id == 0) null else getString(id).takeIf { it.isNotBlank() }
}
