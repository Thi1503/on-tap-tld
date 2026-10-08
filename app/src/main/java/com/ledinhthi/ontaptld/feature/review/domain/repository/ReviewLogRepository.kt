package com.ledinhthi.ontaptld.feature.review.domain.repository

import com.ledinhthi.ontaptld.feature.review.domain.model.ReviewLog

interface ReviewLogRepository {
    suspend fun add(log: ReviewLog)
}
