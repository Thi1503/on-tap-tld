package com.ledinhthi.ontaptld.core.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ledinhthi.ontaptld.feature.deck.data.local.DeckEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.FlashcardEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteEntity

/** Một lệnh trong hàng đợi vừa đẩy lên xong, kèm `updatedAt` của dòng dữ liệu LÚC được đẩy. */
data class PushedCommand(val command: SyncQueueEntity, val rowUpdatedAt: Long)

/**
 * Luật Last-Write-Wins khi kéo một bản ghi từ đám mây về: bản nào sửa SAU thì thắng.
 * [localUpdatedAt] = null nghĩa là máy này chưa có bản ghi đó.
 *
 * Trả về true nếu phải ghi bản trên đám mây đè lên máy.
 */
fun remoteWins(localUpdatedAt: Long?, remoteUpdatedAt: Long, remoteDeleted: Boolean): Boolean =
    if (localUpdatedAt == null) {
        // Chưa có trên máy: tạo mới — trừ khi nó vốn đã bị xoá ở máy khác thì khỏi cần tạo.
        !remoteDeleted
    } else {
        remoteUpdatedAt > localUpdatedAt
    }

/**
 * Mọi câu SQL phục vụ đồng bộ (docs_tld Mục 8.5). Gom vào một DAO riêng để các DAO của bộ thẻ /
 * thẻ / ghi chú không phải biết gì về đồng bộ.
 *
 * Ba bảng có cùng một bộ câu lệnh, chỉ khác tên bảng — SQL của Room không cho truyền tên bảng
 * làm tham số nên mỗi câu phải viết ba lần.
 */
@Dao
interface SyncDao {

    // -----------------------------------------------------------------------------------
    // Ghi lệnh vào hàng đợi
    //
    // Một dòng dữ liệu chỉ có MỘT lệnh chờ: lệnh lấy luôn id của dòng làm khoá chính, nên
    // `INSERT OR REPLACE` tự đè lệnh cũ (sửa một thẻ mười lần lúc mất mạng vẫn chỉ một lệnh).
    // Loại lệnh được suy ra từ chính dòng dữ liệu:
    //   - đã đánh dấu xoá                                  -> DELETE
    //   - vừa tạo (createdAt = updatedAt), hoặc lệnh đang
    //     chờ của nó vẫn là CREATE (chưa từng lên đám mây) -> CREATE
    //   - còn lại                                          -> UPDATE
    // -----------------------------------------------------------------------------------

    @Query(
        "INSERT OR REPLACE INTO sync_queue (id, entityType, entityId, action, createdAt, retryCount, lastError) " +
            "SELECT d.id, 'DECK', d.id, CASE WHEN d.isDeleted = 1 THEN 'DELETE' " +
            "WHEN d.createdAt = d.updatedAt OR EXISTS (SELECT 1 FROM sync_queue q WHERE q.entityId = d.id AND q.action = 'CREATE') " +
            "THEN 'CREATE' ELSE 'UPDATE' END, :now, 0, NULL " +
            "FROM decks d WHERE d.id IN (:ids)",
    )
    suspend fun enqueueDecks(ids: List<String>, now: Long)

    @Query(
        "INSERT OR REPLACE INTO sync_queue (id, entityType, entityId, action, createdAt, retryCount, lastError) " +
            "SELECT n.id, 'NOTE', n.id, CASE WHEN n.isDeleted = 1 THEN 'DELETE' " +
            "WHEN n.createdAt = n.updatedAt OR EXISTS (SELECT 1 FROM sync_queue q WHERE q.entityId = n.id AND q.action = 'CREATE') " +
            "THEN 'CREATE' ELSE 'UPDATE' END, :now, 0, NULL " +
            "FROM notes n WHERE n.id IN (:ids)",
    )
    suspend fun enqueueNotes(ids: List<String>, now: Long)

    @Query(
        "INSERT OR REPLACE INTO sync_queue (id, entityType, entityId, action, createdAt, retryCount, lastError) " +
            "SELECT f.id, 'FLASHCARD', f.id, CASE WHEN f.isDeleted = 1 THEN 'DELETE' " +
            "WHEN f.createdAt = f.updatedAt OR EXISTS (SELECT 1 FROM sync_queue q WHERE q.entityId = f.id AND q.action = 'CREATE') " +
            "THEN 'CREATE' ELSE 'UPDATE' END, :now, 0, NULL " +
            "FROM flashcards f WHERE f.id IN (:ids)",
    )
    suspend fun enqueueFlashcards(ids: List<String>, now: Long)

    // "Quét": ghi lệnh cho mọi dòng chưa đồng bộ (synced = 0) mà CHƯA có lệnh nào. Dùng cho các
    // dòng bị đổi gián tiếp (xoá bộ thẻ kéo theo thẻ và ghi chú), cho lần đăng nhập đầu tiên
    // (mọi thứ trên máy đều chưa đồng bộ), và làm lưới an toàn nếu app bị tắt giữa lúc ghi.

    @Query(
        "INSERT INTO sync_queue (id, entityType, entityId, action, createdAt, retryCount, lastError) " +
            "SELECT d.id, 'DECK', d.id, CASE WHEN d.isDeleted = 1 THEN 'DELETE' " +
            "WHEN d.createdAt = d.updatedAt THEN 'CREATE' ELSE 'UPDATE' END, :now, 0, NULL " +
            "FROM decks d WHERE d.synced = 0 AND d.id NOT IN (SELECT entityId FROM sync_queue)",
    )
    suspend fun enqueueUnsyncedDecks(now: Long)

    @Query(
        "INSERT INTO sync_queue (id, entityType, entityId, action, createdAt, retryCount, lastError) " +
            "SELECT n.id, 'NOTE', n.id, CASE WHEN n.isDeleted = 1 THEN 'DELETE' " +
            "WHEN n.createdAt = n.updatedAt THEN 'CREATE' ELSE 'UPDATE' END, :now, 0, NULL " +
            "FROM notes n WHERE n.synced = 0 AND n.id NOT IN (SELECT entityId FROM sync_queue)",
    )
    suspend fun enqueueUnsyncedNotes(now: Long)

    @Query(
        "INSERT INTO sync_queue (id, entityType, entityId, action, createdAt, retryCount, lastError) " +
            "SELECT f.id, 'FLASHCARD', f.id, CASE WHEN f.isDeleted = 1 THEN 'DELETE' " +
            "WHEN f.createdAt = f.updatedAt THEN 'CREATE' ELSE 'UPDATE' END, :now, 0, NULL " +
            "FROM flashcards f WHERE f.synced = 0 AND f.id NOT IN (SELECT entityId FROM sync_queue)",
    )
    suspend fun enqueueUnsyncedFlashcards(now: Long)

    @Transaction
    suspend fun enqueueUnsynced(now: Long) {
        enqueueUnsyncedDecks(now)
        enqueueUnsyncedNotes(now)
        enqueueUnsyncedFlashcards(now)
    }

    // -----------------------------------------------------------------------------------
    // Đổi tài khoản đồng bộ (docs_tld Mục 8.6)
    // -----------------------------------------------------------------------------------

    @Query("UPDATE decks SET synced = 0, userId = :uid")
    suspend fun claimDecks(uid: String)

    @Query("UPDATE notes SET synced = 0")
    suspend fun markAllNotesUnsynced()

    @Query("UPDATE flashcards SET synced = 0")
    suspend fun markAllFlashcardsUnsynced()

    @Query("DELETE FROM sync_queue")
    suspend fun clearQueue()

    /**
     * Máy này bắt đầu đồng bộ với tài khoản [uid] (lần đăng nhập đầu, hoặc đổi sang tài khoản
     * khác): mọi dữ liệu đang có trên máy được coi là "chưa lên đám mây của tài khoản này" và
     * được xếp hết vào hàng đợi.
     */
    @Transaction
    suspend fun claimAllFor(uid: String, now: Long) {
        claimDecks(uid)
        markAllNotesUnsynced()
        markAllFlashcardsUnsynced()
        clearQueue()
        enqueueUnsynced(now)
    }

    /** Bộ thẻ tạo sau lần đăng nhập chưa có `userId` — gán nốt. Không coi là một thay đổi cần đẩy. */
    @Query("UPDATE decks SET userId = :uid WHERE userId IS NULL OR userId != :uid")
    suspend fun assignDecksTo(uid: String)

    // -----------------------------------------------------------------------------------
    // Đẩy lên (Push)
    // -----------------------------------------------------------------------------------

    // Đọc cả dòng đã đánh dấu xoá: việc "đã xoá" cũng phải được đẩy lên cho máy khác biết.
    @Query("SELECT * FROM decks WHERE id IN (:ids)")
    suspend fun decksByIds(ids: List<String>): List<DeckEntity>

    @Query("SELECT * FROM notes WHERE id IN (:ids)")
    suspend fun notesByIds(ids: List<String>): List<NoteEntity>

    @Query("SELECT * FROM flashcards WHERE id IN (:ids)")
    suspend fun flashcardsByIds(ids: List<String>): List<FlashcardEntity>

    // `AND updatedAt = …`: nếu người dùng kịp sửa dòng này trong lúc nó đang được đẩy lên thì
    // KHÔNG đánh dấu "đã đồng bộ" — bản vừa lên đám mây đã cũ.
    @Query("UPDATE decks SET synced = 1 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markDeckSynced(id: String, updatedAt: Long)

    @Query("UPDATE notes SET synced = 1 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markNoteSynced(id: String, updatedAt: Long)

    @Query("UPDATE flashcards SET synced = 1 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markFlashcardSynced(id: String, updatedAt: Long)

    // Cùng ý trên: lệnh đã bị một lệnh mới hơn đè (createdAt khác) thì giữ lại cho lượt sau.
    @Query("DELETE FROM sync_queue WHERE id = :id AND createdAt = :createdAt")
    suspend fun deleteCommandIfUnchanged(id: String, createdAt: Long)

    @Query("DELETE FROM sync_queue WHERE entityId IN (:entityIds)")
    suspend fun deleteCommandsFor(entityIds: List<String>)

    /** Đẩy xong một lô: đánh dấu các dòng là đã đồng bộ và rút lệnh khỏi hàng đợi. */
    @Transaction
    suspend fun markPushed(pushed: List<PushedCommand>) {
        pushed.forEach { (command, rowUpdatedAt) ->
            when (command.entityType) {
                SyncEntityType.DECK -> markDeckSynced(command.entityId, rowUpdatedAt)
                SyncEntityType.NOTE -> markNoteSynced(command.entityId, rowUpdatedAt)
                SyncEntityType.FLASHCARD -> markFlashcardSynced(command.entityId, rowUpdatedAt)
            }
            deleteCommandIfUnchanged(command.id, command.createdAt)
        }
    }

    // -----------------------------------------------------------------------------------
    // Kéo về (Pull)
    // -----------------------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDecks(rows: List<DeckEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotes(rows: List<NoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFlashcards(rows: List<FlashcardEntity>)

    /**
     * Trộn các bộ thẻ vừa kéo về vào máy theo luật [remoteWins]. Đọc bản trên máy, so và ghi nằm
     * trong CÙNG một transaction, nên người dùng có sửa đúng lúc đó cũng không bị ghi đè nhầm.
     * Bản nào thua thì bỏ qua (nếu bản trên máy mới hơn, lượt đẩy ngay sau sẽ đưa nó lên).
     *
     * Trả về số dòng đã ghi vào máy.
     */
    @Transaction
    suspend fun mergePulledDecks(remote: List<DeckEntity>): Int {
        val local = decksByIds(remote.map { it.id }).associateBy { it.id }
        val winners = remote.filter { remoteWins(local[it.id]?.updatedAt, it.updatedAt, it.isDeleted) }
        upsertDecks(winners)
        // Thay đổi trên máy của các dòng này vừa thua, lệnh đang chờ của chúng không còn ý nghĩa.
        deleteCommandsFor(winners.map { it.id })
        return winners.size
    }

    @Transaction
    suspend fun mergePulledNotes(remote: List<NoteEntity>): Int {
        val local = notesByIds(remote.map { it.id }).associateBy { it.id }
        val winners = remote
            .filter { remoteWins(local[it.id]?.updatedAt, it.updatedAt, it.isDeleted) }
            // Ảnh ghi chú không lên đám mây: giữ đường dẫn ảnh đang có trên máy này (nếu có).
            .map { it.copy(imagePath = local[it.id]?.imagePath.orEmpty()) }
        upsertNotes(winners)
        deleteCommandsFor(winners.map { it.id })
        return winners.size
    }

    @Transaction
    suspend fun mergePulledFlashcards(remote: List<FlashcardEntity>): Int {
        val local = flashcardsByIds(remote.map { it.id }).associateBy { it.id }
        val winners = remote.filter { remoteWins(local[it.id]?.updatedAt, it.updatedAt, it.isDeleted) }
        upsertFlashcards(winners)
        deleteCommandsFor(winners.map { it.id })
        return winners.size
    }

    // Bộ thẻ bị xoá ở máy khác trong khi máy này vừa thêm thẻ vào nó: thẻ đó thành "mồ côi"
    // (không thấy ở đâu nhưng vẫn bị đếm là cần ôn). Xoá nốt cho khớp, rồi để lượt đẩy báo lên.

    @Query(
        "UPDATE flashcards SET isDeleted = 1, synced = 0, updatedAt = :now " +
            "WHERE isDeleted = 0 AND deckId IN (SELECT id FROM decks WHERE isDeleted = 1)",
    )
    suspend fun softDeleteFlashcardsOfDeletedDecks(now: Long): Int

    @Query(
        "UPDATE notes SET isDeleted = 1, synced = 0, updatedAt = :now " +
            "WHERE isDeleted = 0 AND deckId IN (SELECT id FROM decks WHERE isDeleted = 1)",
    )
    suspend fun softDeleteNotesOfDeletedDecks(now: Long): Int

    @Transaction
    suspend fun softDeleteContentsOfDeletedDecks(now: Long) {
        val changed = softDeleteFlashcardsOfDeletedDecks(now) + softDeleteNotesOfDeletedDecks(now)
        if (changed > 0) enqueueUnsynced(now)
    }
}
