package com.ledinhthi.ontaptld.feature.sync.data.remote

import com.ledinhthi.ontaptld.feature.deck.data.local.DeckEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.FlashcardEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteEntity
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource

/*
 * Đổi qua lại giữa dòng dữ liệu trên máy (Room entity) và bản ghi trên Firestore (một Map tên
 * trường -> giá trị). Cấu trúc trên đám mây: docs_tld Mục 7.1.
 *
 * Chiều đọc về trả `null` khi bản ghi thiếu trường bắt buộc hoặc sai kiểu — bản ghi hỏng thì bỏ
 * qua, không để nó làm hỏng cả lượt đồng bộ. Số nguyên Firestore trả về luôn là Long, số thực là
 * Double, nên phải đổi về Int / Float của entity.
 */

fun DeckEntity.toRemoteFields(): Map<String, Any?> = mapOf(
    "name" to name,
    "colorHex" to colorHex,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "isDeleted" to isDeleted,
)

fun Map<String, Any?>.toDeckEntity(id: String, uid: String): DeckEntity? {
    return DeckEntity(
        id = id,
        userId = uid,
        name = string("name") ?: return null,
        colorHex = string("colorHex") ?: return null,
        createdAt = long("createdAt") ?: return null,
        updatedAt = long("updatedAt") ?: return null,
        synced = true, // vừa lấy từ đám mây về: hai nơi đang giống nhau
        isDeleted = boolean("isDeleted"),
    )
}

/** Ghi chú chỉ lên đám mây phần CHỮ; ảnh ở lại trên máy đã chụp (docs_tld Mục 6.2). */
fun NoteEntity.toRemoteFields(): Map<String, Any?> = mapOf(
    "deckId" to deckId,
    "ocrText" to ocrText,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "isDeleted" to isDeleted,
)

fun Map<String, Any?>.toNoteEntity(id: String): NoteEntity? {
    return NoteEntity(
        id = id,
        deckId = string("deckId") ?: return null,
        imagePath = "", // nơi trộn dữ liệu (SyncDao.mergePulledNotes) điền đường dẫn ảnh của máy này
        ocrText = string("ocrText").orEmpty(),
        createdAt = long("createdAt") ?: return null,
        updatedAt = long("updatedAt") ?: return null,
        synced = true,
        isDeleted = boolean("isDeleted"),
    )
}

fun FlashcardEntity.toRemoteFields(): Map<String, Any?> = mapOf(
    "deckId" to deckId,
    "noteId" to noteId,
    "source" to source.name,
    "question" to question,
    "answer" to answer,
    "sourceBoxLeft" to sourceBoxLeft,
    "sourceBoxTop" to sourceBoxTop,
    "sourceBoxRight" to sourceBoxRight,
    "sourceBoxBottom" to sourceBoxBottom,
    "sourceLine" to sourceLine,
    // Tiến độ ôn (SM-2) đi cùng thẻ, để ôn trên máy nào thì máy kia cũng biết.
    "easeFactor" to easeFactor,
    "interval" to interval,
    "repetitions" to repetitions,
    "dueDate" to dueDate,
    "lastReviewedAt" to lastReviewedAt,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "isDeleted" to isDeleted,
)

fun Map<String, Any?>.toFlashcardEntity(id: String): FlashcardEntity? {
    // `runCatching { … }.getOrNull()`: tên nguồn lạ (bản app mới hơn ghi lên) -> null thay vì văng lỗi.
    val source = string("source")?.let { runCatching { FlashcardSource.valueOf(it) }.getOrNull() }
    return FlashcardEntity(
        id = id,
        deckId = string("deckId") ?: return null,
        noteId = string("noteId"),
        source = source ?: return null,
        question = string("question") ?: return null,
        answer = string("answer") ?: return null,
        sourceBoxLeft = double("sourceBoxLeft")?.toFloat(),
        sourceBoxTop = double("sourceBoxTop")?.toFloat(),
        sourceBoxRight = double("sourceBoxRight")?.toFloat(),
        sourceBoxBottom = double("sourceBoxBottom")?.toFloat(),
        sourceLine = long("sourceLine")?.toInt(),
        easeFactor = double("easeFactor") ?: 2.5,
        interval = long("interval")?.toInt() ?: 0,
        repetitions = long("repetitions")?.toInt() ?: 0,
        dueDate = long("dueDate") ?: return null,
        lastReviewedAt = long("lastReviewedAt"),
        createdAt = long("createdAt") ?: return null,
        updatedAt = long("updatedAt") ?: return null,
        synced = true,
        isDeleted = boolean("isDeleted"),
    )
}

// `as?` = thử ép kiểu, sai kiểu thì ra null thay vì văng lỗi.
private fun Map<String, Any?>.string(key: String): String? = this[key] as? String
private fun Map<String, Any?>.long(key: String): Long? = (this[key] as? Number)?.toLong()
private fun Map<String, Any?>.double(key: String): Double? = (this[key] as? Number)?.toDouble()
private fun Map<String, Any?>.boolean(key: String): Boolean = this[key] as? Boolean ?: false
