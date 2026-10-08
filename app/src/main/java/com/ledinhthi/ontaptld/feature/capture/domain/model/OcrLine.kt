package com.ledinhthi.ontaptld.feature.capture.domain.model

import com.ledinhthi.ontaptld.feature.deck.domain.model.SourceBox

/**
 * Một dòng chữ đọc được trong ảnh ghi chú, kèm vị trí của nó trên ảnh.
 *
 * @param box khung bao quanh dòng chữ, tính theo tỉ lệ của ảnh (xem [SourceBox]); null khi bộ
 * nhận dạng không cho biết vị trí.
 */
data class OcrLine(val text: String, val box: SourceBox?)
