package com.ledinhthi.ontaptld.feature.review.data.repository

import com.ledinhthi.ontaptld.core.data.local.db.wrapLocal
import com.ledinhthi.ontaptld.feature.review.data.local.ReviewLogDao
import com.ledinhthi.ontaptld.feature.review.data.local.ReviewLogEntity
import com.ledinhthi.ontaptld.feature.review.domain.model.ReviewLog
import com.ledinhthi.ontaptld.feature.review.domain.repository.ReviewLogRepository
import javax.inject.Inject

class ReviewLogRepositoryImpl @Inject constructor(
    private val dao: ReviewLogDao,
) : ReviewLogRepository {

    override suspend fun add(log: ReviewLog) = wrapLocal {
        dao.upsert(
            ReviewLogEntity(
                id = log.id,
                cardId = log.cardId,
                deckId = log.deckId,
                grade = log.grade,
                reviewedAt = log.reviewedAt,
            ),
        )
    }
}
