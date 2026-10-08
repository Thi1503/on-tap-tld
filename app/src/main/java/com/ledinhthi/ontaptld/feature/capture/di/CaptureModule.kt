package com.ledinhthi.ontaptld.feature.capture.di

import com.ledinhthi.ontaptld.feature.capture.data.CaptureImageRepositoryImpl
import com.ledinhthi.ontaptld.feature.capture.data.MlKitOcrRepository
import com.ledinhthi.ontaptld.feature.capture.domain.repository.CaptureImageRepository
import com.ledinhthi.ontaptld.feature.capture.domain.repository.OcrRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface CaptureModule {
    @Binds
    @Singleton
    fun captureImageRepository(impl: CaptureImageRepositoryImpl): CaptureImageRepository

    @Binds
    @Singleton
    fun ocrRepository(impl: MlKitOcrRepository): OcrRepository
}
