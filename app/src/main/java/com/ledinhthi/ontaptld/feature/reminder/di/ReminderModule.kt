package com.ledinhthi.ontaptld.feature.reminder.di

import com.ledinhthi.ontaptld.feature.reminder.data.AndroidReminderNotifier
import com.ledinhthi.ontaptld.feature.reminder.data.WorkManagerReminderScheduler
import com.ledinhthi.ontaptld.feature.reminder.domain.ReminderNotifier
import com.ledinhthi.ontaptld.feature.reminder.domain.ReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface ReminderModule {
    @Binds
    @Singleton
    fun scheduler(impl: WorkManagerReminderScheduler): ReminderScheduler

    @Binds
    @Singleton
    fun notifier(impl: AndroidReminderNotifier): ReminderNotifier
}
