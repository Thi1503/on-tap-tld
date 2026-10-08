package com.ledinhthi.ontaptld.feature.capture.domain.model

/** Hạn mức tạo thẻ bằng AI trong ngày: còn [remaining] lượt trên tổng [max] lượt. */
data class AiQuota(val remaining: Int, val max: Int)
