package com.ledinhthi.ontaptld.feature.capture.domain.usecase

import com.ledinhthi.ontaptld.core.data.ai.AiQuotaGuard
import com.ledinhthi.ontaptld.core.domain.usecase.NoInputFlowUseCase
import com.ledinhthi.ontaptld.feature.capture.domain.model.AiQuota
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Theo dõi số lượt tạo thẻ bằng AI còn lại hôm nay (tự cập nhật mỗi khi dùng một lượt). */
class ObserveAiQuotaUseCase @Inject constructor(
    private val quotaGuard: AiQuotaGuard,
) : NoInputFlowUseCase<AiQuota>() {
    override fun invoke(): Flow<AiQuota> = quotaGuard.observeRemaining().map { remaining ->
        AiQuota(remaining = remaining, max = AiQuotaGuard.MAX_CALLS_PER_DAY)
    }
}
