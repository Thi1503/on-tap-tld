package com.ledinhthi.ontaptld.core.di

import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.IdGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun clock(): Clock = object : Clock {
        override fun nowMillis() = System.currentTimeMillis()
    }

    @Provides
    @Singleton
    fun idGenerator(): IdGenerator = object : IdGenerator {
        override fun newId() = UUID.randomUUID().toString()
    }

    @Provides
    @Singleton
    fun json(): Json = Json { ignoreUnknownKeys = true }
}
