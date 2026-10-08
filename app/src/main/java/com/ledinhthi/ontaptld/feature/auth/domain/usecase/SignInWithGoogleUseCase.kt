package com.ledinhthi.ontaptld.feature.auth.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.feature.auth.domain.AuthRepository
import com.ledinhthi.ontaptld.feature.auth.domain.model.AuthUser
import javax.inject.Inject

/** Đăng nhập bằng ID token Google (đầu vào), trả về người vừa đăng nhập. */
class SignInWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository,
) : UseCase<String, AuthUser>() {
    override suspend fun invoke(input: String): AuthUser = repository.signInWithGoogle(input)
}
