package com.ledinhthi.ontaptld

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class OnTapTldApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        // Sprint 1: Firebase chưa gắn (repo chưa có google-services.json).
        // Khi bật AI: Firebase.initialize(this) + AppCheckInstaller.install() (docs Mục 15).
    }
}
