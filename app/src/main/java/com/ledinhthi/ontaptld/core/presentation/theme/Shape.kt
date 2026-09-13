package com.ledinhthi.ontaptld.core.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * Không có file `shape.dart` tương ứng bên Flutter (radius dùng trực tiếp từ
 * AppDimens tại nơi vẽ BorderRadius), nhưng Material3 cần một bộ [Shapes] cho
 * MaterialTheme — dựng từ đúng thang radius đã port ở Dimens.kt để đồng bộ.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(AppDimens.radius4),
    small = RoundedCornerShape(AppDimens.radius8),
    medium = RoundedCornerShape(AppDimens.radius12),
    large = RoundedCornerShape(AppDimens.radius20),
    extraLarge = RoundedCornerShape(AppDimens.radius30),
)
