package com.ledinhthi.ontaptld.feature.auth.domain

import com.ledinhthi.ontaptld.feature.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

/**
 * Tài khoản của người dùng. Tầng domain chỉ biết interface này; phần nói chuyện với Firebase nằm
 * ở `data/FirebaseAuthRepository`.
 */
interface AuthRepository {
    /** Người đang đăng nhập, `null` khi chưa đăng nhập. Phát lại mỗi lần đăng nhập / đăng xuất. */
    val currentUser: Flow<AuthUser?>

    /**
     * Đăng nhập bằng "ID token" mà Google cấp sau khi người dùng chọn tài khoản.
     * Lỗi thì ném `AppException.AuthException`.
     */
    suspend fun signInWithGoogle(idToken: String): AuthUser

    /** Đăng xuất. Dữ liệu trên máy không bị đụng tới. */
    suspend fun signOut()
}
