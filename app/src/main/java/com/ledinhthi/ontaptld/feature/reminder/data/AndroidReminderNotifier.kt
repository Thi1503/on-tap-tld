package com.ledinhthi.ontaptld.feature.reminder.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.data.local.prefs.AppPreferences
import com.ledinhthi.ontaptld.core.presentation.language.withAppLanguage
import com.ledinhthi.ontaptld.feature.reminder.domain.ReminderNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AppPreferences,
) : ReminderNotifier {

    override suspend fun showDueCards(dueCount: Int) {
        if (!canPostNotifications(context)) return

        // Thông báo được dựng lúc app có thể đang đóng, không có màn hình nào để "mượn" ngôn ngữ.
        // Trước Android 13 hệ thống không tự áp ngôn ngữ riêng của app cho trường hợp này, nên
        // phải tự lấy lại lựa chọn đã lưu.
        val savedTag = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) prefs.languageTag.first() else null
        val localized = context.withAppLanguage(fallbackTag = savedTag)

        ensureChannel(localized)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            // Icon nhỏ trên thanh trạng thái: Android chỉ dùng HÌNH DÁNG của nó rồi tự tô một màu.
            .setSmallIcon(R.drawable.ic_cards)
            .setColor(ContextCompat.getColor(context, R.color.brand_primary))
            .setContentTitle(localized.getString(R.string.reminder_notification_title))
            .setContentText(
                localized.resources.getQuantityString(R.plurals.reminder_notification_text, dueCount, dueCount),
            )
            .setContentIntent(openAppIntent())
            .setAutoCancel(true) // bấm vào thì thông báo tự biến mất
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        try {
            // Cùng một id cho mọi lần: thông báo hôm nay thay thế thông báo hôm qua còn sót lại,
            // không chồng thành nhiều dòng.
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Quyền vừa bị thu hồi đúng giữa lúc kiểm tra và lúc gửi — coi như không gửi.
        }
    }

    /**
     * Từ Android 8, mọi thông báo phải thuộc một "kênh" — người dùng tắt / chỉnh âm từng kênh
     * trong Cài đặt của máy. Gọi lại với cùng id chỉ cập nhật tên (vd sau khi đổi ngôn ngữ).
     */
    private fun ensureChannel(localized: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            localized.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = localized.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Bấm thông báo = mở app như bấm icon ngoài màn hình chính (vào Home, hoặc màn đang dở). */
    private fun openAppIntent(): PendingIntent? {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        // PendingIntent: "giấy uỷ quyền" để hệ thống thay app mở màn hình khi người dùng bấm.
        return PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val CHANNEL_ID = "review_reminder"
        private const val NOTIFICATION_ID = 1001

        /**
         * App có đang được phép hiện thông báo không: quyền lúc chạy (Android 13+) đã cấp, và
         * người dùng không tắt thông báo của app trong Cài đặt của máy.
         */
        fun canPostNotifications(context: Context): Boolean = !needsRuntimePermission(context) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

        /** Android 13+ mới có quyền `POST_NOTIFICATIONS` phải xin lúc chạy. */
        fun needsRuntimePermission(context: Context): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
    }
}
