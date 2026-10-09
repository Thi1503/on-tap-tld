package com.ledinhthi.ontaptld.feature.auth.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.NoInputUseCase
import com.ledinhthi.ontaptld.feature.auth.domain.AuthRepository
import javax.inject.Inject

/** Đăng xuất khỏi tài khoản Google. Bộ thẻ, thẻ và ảnh trên máy được giữ nguyên. */
class SignOutUseCase @Inject constructor(
    private val repository: AuthRepository,
) : NoInputUseCase<Unit>() {
    override suspend fun invoke() = repository.signOut()
}
