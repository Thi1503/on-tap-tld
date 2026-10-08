package com.ledinhthi.ontaptld.core.domain.util

import java.time.Instant
import java.time.ZoneId

/**
 * Mốc "cần ôn hôm nay": một thẻ được tính là đến hạn nếu `dueDate` rơi vào bất kỳ lúc nào
 * trong HÔM NAY (tới 23:59:59.999 giờ máy), không phải chỉ khi đã qua đúng giờ-phút hẹn.
 *
 * Lý do: SM-2 hẹn theo ngày. Thẻ ôn lúc 21:00 hôm qua với interval = 1 có `dueDate` là 21:00
 * hôm nay — nếu so với "bây giờ" thì cả ngày hôm nay nó chưa hiện, người dùng mở app buổi
 * sáng sẽ thấy 0 thẻ rồi tối mới thấy. Mọi nơi đếm/lấy thẻ đến hạn (Home, Ôn tập, Widget)
 * phải dùng chung hàm này để con số khớp nhau.
 */
fun dueCutoffMillis(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(nowMillis)
        .atZone(zone)          // thời điểm hiện tại theo múi giờ của máy
        .toLocalDate()         // chỉ lấy ngày (bỏ giờ phút)
        .plusDays(1)           // sang ngày mai
        .atStartOfDay(zone)    // 00:00 ngày mai
        .toInstant()
        .toEpochMilli() - 1    // lùi 1 mili giây = 23:59:59.999 hôm nay
