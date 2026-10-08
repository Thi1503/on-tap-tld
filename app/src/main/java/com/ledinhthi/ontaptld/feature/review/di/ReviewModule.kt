package com.ledinhthi.ontaptld.feature.review.di

import com.ledinhthi.ontaptld.feature.review.data.repository.ReviewLogRepositoryImpl
import com.ledinhthi.ontaptld.feature.review.domain.repository.ReviewLogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface ReviewModule {
    @Binds
    @Singleton
    fun reviewLogRepository(impl: ReviewLogRepositoryImpl): ReviewLogRepository
}
