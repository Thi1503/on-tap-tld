package com.ledinhthi.ontaptld.feature.sync.di

import com.ledinhthi.ontaptld.core.sync.SyncScheduler
import com.ledinhthi.ontaptld.feature.sync.data.SyncRepositoryImpl
import com.ledinhthi.ontaptld.feature.sync.data.WorkManagerSyncScheduler
import com.ledinhthi.ontaptld.feature.sync.data.remote.FirestoreSyncDataSource
import com.ledinhthi.ontaptld.feature.sync.data.remote.SyncRemoteDataSource
import com.ledinhthi.ontaptld.feature.sync.domain.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface SyncModule {
    @Binds
    @Singleton
    fun syncRepository(impl: SyncRepositoryImpl): SyncRepository

    @Binds
    @Singleton
    fun remoteDataSource(impl: FirestoreSyncDataSource): SyncRemoteDataSource

    @Binds
    @Singleton
    fun scheduler(impl: WorkManagerSyncScheduler): SyncScheduler
}
