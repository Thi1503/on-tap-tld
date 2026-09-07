package com.ledinhthi.ontaptld.feature.deck.domain.model

import com.ledinhthi.ontaptld.core.domain.model.DomainModel

/** Ảnh gốc + text OCR — chỉ tồn tại khi thẻ tạo qua đường AI (docs_tld 6.2). */
data class Note(
    val id: String,
    val deckId: String,
    val imagePath: String,
    val ocrText: String,
    val createdAt: Long,
    val updatedAt: Long,
) : DomainModel
