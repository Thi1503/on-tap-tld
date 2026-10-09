package com.ledinhthi.ontaptld.feature.auth.domain.model

/**
 * Người đang đăng nhập. App chỉ dùng ba thứ này từ tài khoản Google: mã định danh, tên hiển thị
 * và địa chỉ email. [displayName] / [email] có thể trống nếu tài khoản không cung cấp.
 */
data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
)
