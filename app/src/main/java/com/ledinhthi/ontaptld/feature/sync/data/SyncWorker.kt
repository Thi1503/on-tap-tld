package com.ledinhthi.ontaptld.feature.sync.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.SyncErrorKind
import com.ledinhthi.ontaptld.core.sync.SyncScheduler
import com.ledinhthi.ontaptld.feature.auth.domain.AuthRepository
import com.ledinhthi.ontaptld.feature.sync.domain.SyncNowUseCase
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
 * Việc nền chạy một lượt đồng bộ. Dùng WorkManager (như lời nhắc ôn — xem `ReviewReminderWorker`)
 * để lượt đồng bộ vẫn chạy khi người dùng đã rời app, và CHỈ chạy khi máy có mạng.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        EntryPointAccessors.fromApplication(applicationContext, SyncEntryPoint::class.java).syncNow().invoke()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: AppException.SyncException) {
        Timber.w(e, "Đồng bộ nền lỗi (%s), lần thử %d", e.kind, runAttemptCount)
        // Lỗi tạm thời thì xin chạy lại; WorkManager tự giãn khoảng chờ dài dần giữa các lần
        // (xem `setBackoffCriteria` bên dưới). Thử mãi không được thì thôi — lần mở app sau, hoặc
        // lần sửa thẻ kế tiếp, sẽ hẹn lượt mới.
        if (e.kind == SyncErrorKind.NETWORK && runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
    } catch (e: Exception) {
        Timber.w(e, "Đồng bộ nền lỗi")
        Result.failure()
    }

    /** Khai báo cho Hilt biết lớp ngoài tầm với của nó (worker) cần lấy những gì. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncEntryPoint {
        fun syncNow(): SyncNowUseCase
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}

@Singleton
class WorkManagerSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: AuthRepository,
) : SyncScheduler {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    override fun requestSync() {
        // Chưa đăng nhập thì không có đám mây nào để đồng bộ. Lệnh vẫn nằm trong hàng đợi và sẽ
        // được đẩy lên sau lần đăng nhập đầu tiên.
        if (auth.currentUserId == null) return

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            // Chờ vài giây rồi mới chạy. Cùng với REPLACE bên dưới, mỗi thay đổi mới sẽ đặt lại
            // khoảng chờ này: chấm liền mười thẻ thì chỉ đồng bộ MỘT lần, sau thẻ cuối cùng.
            .setInitialDelay(DELAY_SECONDS, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "cloud_sync"
        const val DELAY_SECONDS = 3L
        const val BACKOFF_SECONDS = 10L
    }
}
