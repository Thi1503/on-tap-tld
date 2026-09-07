package com.ledinhthi.ontaptld.core.domain.util

/** Bọc thời gian & id để test deterministic (luôn mock trong test domain). */
interface Clock {
    fun nowMillis(): Long
}

interface IdGenerator {
    fun newId(): String
}
