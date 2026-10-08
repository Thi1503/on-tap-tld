package com.ledinhthi.ontaptld.core.di

import com.ledinhthi.ontaptld.core.data.ai.FirebaseGeminiClient
import com.ledinhthi.ontaptld.core.data.ai.GeminiClient
import com.ledinhthi.ontaptld.core.data.local.prefs.AppPreferences
import com.ledinhthi.ontaptld.core.data.local.prefs.DataStorePreferences
import com.ledinhthi.ontaptld.core.presentation.language.AppCompatLanguageManager
import com.ledinhthi.ontaptld.core.presentation.language.AppLanguageManager
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

    @Binds
    @Singleton
    fun languageManager(impl: AppCompatLanguageManager): AppLanguageManager

    @Binds
    @Singleton
    fun gemini(impl: FirebaseGeminiClient): GeminiClient
}
