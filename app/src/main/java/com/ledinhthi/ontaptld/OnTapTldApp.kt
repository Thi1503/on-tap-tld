package com.ledinhthi.ontaptld

import android.app.Application
import com.google.firebase.FirebaseApp
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.CleanUpOrphanNotesUseCase
import com.ledinhthi.ontaptld.feature.reminder.domain.KeepReminderScheduledUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class OnTapTldApp : Application() {

    // Hilt gán giá trị cho biến `@Inject lateinit` ngay trong `super.onCreate()`.
    @Inject
    lateinit var cleanUpOrphanNotes: CleanUpOrphanNotesUseCase

    @Inject
    lateinit var keepReminderScheduled: KeepReminderScheduledUseCase

    /**
     * Phạm vi coroutine sống suốt đời app, cho việc nền không thuộc về màn nào. `SupervisorJob`:
     * một việc hỏng không kéo các việc khác trong phạm vi hỏng theo.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        cleanUpOrphanNotesInBackground()
        // Nghe cài đặt nhắc ôn suốt đời app: bật / tắt / đổi giờ ở màn Cài đặt là lịch đổi theo.
        appScope.launch { keepReminderScheduled() }

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

    /** Dọn ảnh ghi chú không còn dùng. Chỉ là việc dọn dẹp: hỏng thì ghi log, lần mở app sau thử lại. */
    private fun cleanUpOrphanNotesInBackground() {
        appScope.launch {
            try {
                cleanUpOrphanNotes()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Không dọn được ghi chú mồ côi")
            }
        }
    }
}
