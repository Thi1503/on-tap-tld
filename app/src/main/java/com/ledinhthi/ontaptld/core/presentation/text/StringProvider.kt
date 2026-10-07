package com.ledinhthi.ontaptld.core.presentation.text

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cửa lấy chuỗi `R.string.*` cho những nơi KHÔNG có `Context`/Compose — ViewModel và
 * `GlobalExceptionHandler` (docs kiến trúc Mục 6.2). Trong `@Composable` vẫn dùng
 * `stringResource()` như thường; `domain/` thì không bao giờ tạo chuỗi hiển thị.
 */
interface StringProvider {
    fun get(@StringRes id: Int, vararg args: Any): String
}

@Singleton
class AndroidStringProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : StringProvider {
    override fun get(id: Int, vararg args: Any): String =
        if (args.isEmpty()) context.getString(id) else context.getString(id, *args)
}
