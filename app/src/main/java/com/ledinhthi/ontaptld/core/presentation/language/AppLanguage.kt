package com.ledinhthi.ontaptld.core.presentation.language

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Các ngôn ngữ giao diện app có bản dịch. [tag] là mã ngôn ngữ chuẩn (BCP 47), trùng với tên
 * thư mục chuỗi: `values/` (mặc định, tiếng Việt) và `values-en/`.
 */
enum class AppLanguage(val tag: String) {
    Vietnamese("vi"),
    English("en"),
    ;

    companion object {
        /** Mã lạ thì coi là tiếng Việt — ngôn ngữ mặc định của app. */
        fun fromTag(tag: String): AppLanguage = entries.firstOrNull { it.tag == tag } ?: Vietnamese
    }
}

/** Đổi ngôn ngữ giao diện. Tách thành interface để ViewModel test được mà không cần Android. */
interface AppLanguageManager {
    fun setLanguage(language: AppLanguage)
}

/**
 * Giao việc cho thư viện AppCompat: nó nhớ lựa chọn, áp dụng ngay (màn hình tự vẽ lại bằng ngôn
 * ngữ mới) và chạy được từ Android 7 — trên Android 13+ thì gọi thẳng tính năng "ngôn ngữ ứng
 * dụng" của hệ thống. Muốn vậy `MainActivity` phải kế thừa `AppCompatActivity`.
 */
@Singleton
class AppCompatLanguageManager @Inject constructor() : AppLanguageManager {
    override fun setLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }
}

/**
 * Trả về một `Context` nói đúng ngôn ngữ người dùng đã chọn cho app.
 *
 * Cần tới nó vì trước Android 13, lựa chọn ngôn ngữ của app chỉ được áp vào Activity; `Context`
 * của Application (thứ mà các lớp không thuộc màn hình nào cầm) vẫn theo ngôn ngữ của máy.
 */
fun Context.withAppLanguage(): Context {
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales.isEmpty) return this // người dùng chưa chọn -> theo máy như bình thường
    val configuration = Configuration(resources.configuration)
    configuration.setLocales(LocaleList.forLanguageTags(locales.toLanguageTags()))
    return createConfigurationContext(configuration)
}
