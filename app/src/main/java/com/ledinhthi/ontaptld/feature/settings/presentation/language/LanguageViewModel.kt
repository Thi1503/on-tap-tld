package com.ledinhthi.ontaptld.feature.settings.presentation.language

import androidx.lifecycle.ViewModel
import com.ledinhthi.ontaptld.core.presentation.language.AppLanguage
import com.ledinhthi.ontaptld.core.presentation.language.AppLanguageManager
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel của màn Ngôn ngữ (route `LanguageRoute`). Màn này không có state riêng: ngôn ngữ
 * đang dùng do chính hệ thống giữ, màn hình đọc thẳng từ tài nguyên chuỗi (xem LanguageScreen),
 * nên ở đây chỉ kế thừa `ViewModel` thường thay vì `BaseViewModel`.
 */
@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val navigator: AppNavigator,
    private val languageManager: AppLanguageManager,
) : ViewModel() {

    fun onBack() = navigator.back()

    /** Áp dụng ngay: Android dựng lại màn hình bằng ngôn ngữ mới, người dùng vẫn ở màn này. */
    fun onLanguageSelected(language: AppLanguage) = languageManager.setLanguage(language)
}
