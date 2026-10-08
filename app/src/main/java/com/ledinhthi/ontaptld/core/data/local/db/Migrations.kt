package com.ledinhthi.ontaptld.core.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Mọi thay đổi schema đều đi qua một Migration tường minh ở đây — không dùng
 * `fallbackToDestructiveMigration()` (sẽ xoá sạch dữ liệu ôn tập của người dùng).
 * Câu SQL phải khớp từng chữ với `createSql` trong `app/schemas/.../<version>.json`.
 */

/** v2: thêm `sync_queue` (hàng đợi đồng bộ) và `review_logs` (lịch sử chấm thẻ cho thống kê). */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sync_queue` (" +
                "`id` TEXT NOT NULL, `entityType` TEXT NOT NULL, `entityId` TEXT NOT NULL, " +
                "`action` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`retryCount` INTEGER NOT NULL, `lastError` TEXT, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_sync_queue_entityId` ON `sync_queue` (`entityId`)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `review_logs` (" +
                "`id` TEXT NOT NULL, `cardId` TEXT NOT NULL, `deckId` TEXT NOT NULL, " +
                "`grade` TEXT NOT NULL, `reviewedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_review_logs_reviewedAt` ON `review_logs` (`reviewedAt`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_review_logs_cardId` ON `review_logs` (`cardId`)",
        )
    }
}

/**
 * v3: thêm cột `sourceLine` cho thẻ — thẻ AI được rút ra từ dòng thứ mấy của ghi chú. Cột cho
 * phép NULL nên mọi thẻ đã có giữ nguyên, chỉ là chưa có số dòng.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `flashcards` ADD COLUMN `sourceLine` INTEGER")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
