package com.ledinhthi.ontaptld.feature.deck.domain.model

import com.ledinhthi.ontaptld.core.domain.model.DomainModel

data class Deck(
    val id: String,
    val name: String,
    val colorHex: String,
    val createdAt: Long,
    val updatedAt: Long,
) : DomainModel
