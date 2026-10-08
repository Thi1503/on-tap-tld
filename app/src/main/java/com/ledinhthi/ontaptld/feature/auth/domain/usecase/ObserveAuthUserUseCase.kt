package com.ledinhthi.ontaptld.feature.auth.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.NoInputFlowUseCase
import com.ledinhthi.ontaptld.feature.auth.domain.AuthRepository
import com.ledinhthi.ontaptld.feature.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Nghe xem ai đang đăng nhập (`null` = chưa đăng nhập); phát lại khi đăng nhập / đăng xuất. */
class ObserveAuthUserUseCase @Inject constructor(
    private val repository: AuthRepository,
) : NoInputFlowUseCase<AuthUser?>() {
    override fun invoke(): Flow<AuthUser?> = repository.currentUser
}
