package com.ledinhthi.ontaptld.core.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Dùng cho PHẦN CUỘN của màn có thanh nút cố định ở đáy (Thêm thẻ, Kiểm tra văn bản): khi bàn
 * phím hiện lên, phần cuộn co lại vừa tới mép trên bàn phím, còn thanh nút ĐỨNG YÊN ở đáy (bị
 * bàn phím che) thay vì bị đẩy lên chiếm chỗ của ô đang gõ — Thi chốt 8/10/2026.
 *
 * `imePadding()` chừa đúng bằng chiều cao bàn phím tính từ đáy màn hình. Nhưng đáy của phần cuộn
 * vốn đã cao hơn đáy màn hình một đoạn bằng thanh nút, nên phải trừ đoạn đó đi trước
 * (`consumeWindowInsets`), nếu không sẽ thừa ra một khoảng trống ngay trên bàn phím.
 *
 * Thứ tự gắn: đặt TRƯỚC `verticalScroll`, để thứ co lại là khung nhìn chứ không phải nội dung.
 */
fun Modifier.imePaddingAbove(bottomBarHeight: Dp): Modifier =
    consumeWindowInsets(PaddingValues(bottom = bottomBarHeight)).imePadding()
