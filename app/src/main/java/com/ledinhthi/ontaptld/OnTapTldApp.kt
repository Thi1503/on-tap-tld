package com.ledinhthi.ontaptld

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class OnTapTldApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        // Trả về null khi build không có `google-services.json` (file không commit — xem
        // app/build.gradle.kts). Khi đó app vẫn chạy đủ phần offline, chỉ AI/sync không dùng được.
        if (FirebaseApp.initializeApp(this) == null) {
            Timber.w("Không có cấu hình Firebase — bỏ qua App Check, AI và đồng bộ sẽ không hoạt động.")
            return
        }
        // App Check BẮT BUỘC trước lần gọi AI đầu tiên (docs_tld NFR). Provider khác nhau theo
        // build type nên tách source set, để bản `release` không kéo theo `firebase-appcheck-debug`.
        AppCheckInstaller.install()
    }
}
