package com.ledinhthi.ontaptld.core.di

import com.ledinhthi.ontaptld.core.data.ai.GeminiClient
import com.ledinhthi.ontaptld.core.data.ai.StubGeminiClient
import com.ledinhthi.ontaptld.core.data.local.prefs.AppPreferences
import com.ledinhthi.ontaptld.core.data.local.prefs.DataStorePreferences
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigatorImpl
import com.ledinhthi.ontaptld.core.presentation.text.AndroidStringProvider
import com.ledinhthi.ontaptld.core.presentation.text.StringProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface CoreBindsModule {
    @Binds
    @Singleton
    fun navigator(impl: AppNavigatorImpl): AppNavigator

    @Binds
    @Singleton
    fun prefs(impl: DataStorePreferences): AppPreferences

    @Binds
    @Singleton
    fun strings(impl: AndroidStringProvider): StringProvider

    // Sprint 1: stub. Đổi sang FirebaseGeminiClient khi gắn Firebase AI Logic.
    @Binds
    @Singleton
    fun gemini(impl: StubGeminiClient): GeminiClient
}
