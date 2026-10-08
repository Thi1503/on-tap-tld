package com.ledinhthi.ontaptld.feature.reminder.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ledinhthi.ontaptld.core.data.local.prefs.ReminderSettings
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.feature.reminder.domain.ReminderEnqueueMode
import com.ledinhthi.ontaptld.feature.reminder.domain.ReminderScheduler
import com.ledinhthi.ontaptld.feature.reminder.domain.RunReviewReminderUseCase
import com.ledinhthi.ontaptld.feature.reminder.domain.millisUntilNextReminder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Việc nền chạy khi tới giờ nhắc. WorkManager là thư viện của Android để hẹn việc "sẽ chạy dù
 * app đang đóng": nó tự lưu lịch xuống máy, sống qua lần khởi động lại, và tự đánh thức app.
 * Đổi lại giờ chạy không chính xác tới từng giây — máy đang tiết kiệm pin có thể lùi vài phút.
 *
 * Lớp này do WorkManager tự tạo nên không dùng `@Inject constructor` được; nó lấy các phụ thuộc
 * từ Hilt qua một "cửa ngách" là [ReminderEntryPoint].
 */
class ReviewReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        EntryPointAccessors.fromApplication(applicationContext, ReminderEntryPoint::class.java)
            .runReviewReminder()
            .invoke()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "Lần nhắc ôn bị lỗi")
        // Không xin chạy lại: lần nhắc của ngày mai đã được hẹn trong use case.
        Result.failure()
    }

    /** Khai báo cho Hilt biết lớp ngoài tầm với của nó (worker) cần lấy những gì. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderEntryPoint {
        fun runReviewReminder(): RunReviewReminderUseCase
    }
}

@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) : ReminderScheduler {

    // `get()`: mỗi lần dùng mới lấy WorkManager, không giữ sẵn từ lúc lớp này được tạo.
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    override fun schedule(settings: ReminderSettings, mode: ReminderEnqueueMode) {
        val delayMillis = millisUntilNextReminder(clock.nowMillis(), settings.minuteOfDay)
        val request = OneTimeWorkRequestBuilder<ReviewReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
        // "Unique work": mọi lần hẹn dùng chung MỘT cái tên, nên không bao giờ có hai lịch nhắc
        // cùng tồn tại; tham số giữa quyết định số phận của lịch đang có.
        val policy = when (mode) {
            ReminderEnqueueMode.KeepExisting -> ExistingWorkPolicy.KEEP
            ReminderEnqueueMode.Replace -> ExistingWorkPolicy.REPLACE
            // Nối vào sau việc đang chạy (chính là lần nhắc hôm nay). Dùng REPLACE ở đây thì việc
            // đang chạy sẽ tự huỷ chính nó.
            ReminderEnqueueMode.AfterCurrentRun -> ExistingWorkPolicy.APPEND_OR_REPLACE
        }
        workManager.enqueueUniqueWork(WORK_NAME, policy, request)
    }

    override fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "review_reminder"
    }
}
