package com.ledinhthi.ontaptld.feature.auth.di

import com.ledinhthi.ontaptld.feature.auth.data.FirebaseAuthRepository
import com.ledinhthi.ontaptld.feature.auth.domain.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AuthModule {
    @Binds
    @Singleton
    fun authRepository(impl: FirebaseAuthRepository): AuthRepository
}
