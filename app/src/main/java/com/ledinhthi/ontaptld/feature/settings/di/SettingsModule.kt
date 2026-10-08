package com.ledinhthi.ontaptld.feature.settings.di

import com.ledinhthi.ontaptld.feature.settings.data.LocalDataRepositoryImpl
import com.ledinhthi.ontaptld.feature.settings.domain.LocalDataRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface SettingsModule {
    @Binds
    @Singleton
    fun localDataRepository(impl: LocalDataRepositoryImpl): LocalDataRepository
}
