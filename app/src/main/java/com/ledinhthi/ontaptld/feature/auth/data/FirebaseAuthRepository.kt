package com.ledinhthi.ontaptld.feature.auth.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.AuthErrorKind
import com.ledinhthi.ontaptld.feature.auth.domain.AuthRepository
import com.ledinhthi.ontaptld.feature.auth.domain.model.AuthUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tài khoản người dùng qua Firebase Auth. Firebase tự nhớ phiên đăng nhập trên máy, nên mở lại
 * app vẫn còn đăng nhập mà app không phải tự lưu gì.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : AuthRepository {

    /**
     * `null` khi bản build không có `google-services.json`: Firebase không được khởi tạo (xem
     * `OnTapTldApp`) và gọi `FirebaseAuth.getInstance()` lúc đó sẽ làm app crash.
     */
    private val auth: FirebaseAuth?
        get() = if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseAuth.getInstance()

    override val currentUser: Flow<AuthUser?>
        get() {
            val auth = auth ?: return flowOf(null)
            // Firebase báo thay đổi qua listener. `callbackFlow` đổi kiểu "gọi lại" đó thành một
            // Flow: mỗi lần listener được gọi thì `trySend` phát một giá trị. Firebase gọi listener
            // ngay lúc gắn, nên Flow luôn có giá trị đầu tiên.
            return callbackFlow {
                val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAuthUser()) }
                auth.addAuthStateListener(listener)
                // Chạy khi không còn ai nghe Flow này nữa: gỡ listener để không rò rỉ bộ nhớ.
                awaitClose { auth.removeAuthStateListener(listener) }
            }
        }

    override val currentUserId: String?
        get() = auth?.currentUser?.uid

    override suspend fun signInWithGoogle(idToken: String): AuthUser {
        val auth = auth ?: throw AppException.AuthException(AuthErrorKind.NOT_CONFIGURED)
        try {
            // Đổi ID token của Google thành "giấy chứng nhận" mà Firebase hiểu, rồi đăng nhập.
            // `.await()` tạm dừng coroutine tới khi Firebase trả lời.
            val result = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            return result.user?.toAuthUser() ?: throw AppException.AuthException(AuthErrorKind.UNKNOWN)
        } catch (e: FirebaseNetworkException) {
            throw AppException.AuthException(AuthErrorKind.NETWORK, e)
        } catch (e: FirebaseException) {
            throw AppException.AuthException(AuthErrorKind.UNKNOWN, e)
        }
    }

    override suspend fun signOut() {
        auth?.signOut()
        // Báo Credential Manager quên tài khoản vừa dùng, để lần đăng nhập sau người dùng được
        // chọn lại tài khoản. Không báo được cũng không sao: người dùng đã đăng xuất khỏi app rồi.
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: ClearCredentialException) {
            Timber.w(e, "Không xoá được trạng thái Credential Manager")
        }
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        // Chuỗi rỗng cũng coi như "không có" để màn hình chỉ phải xét một trường hợp.
        displayName = displayName?.takeIf { it.isNotBlank() },
        email = email?.takeIf { it.isNotBlank() },
    )
}
