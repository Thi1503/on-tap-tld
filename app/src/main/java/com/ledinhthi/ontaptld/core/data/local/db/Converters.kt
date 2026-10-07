package com.ledinhthi.ontaptld.core.data.local.db

import androidx.room.TypeConverter
import com.ledinhthi.ontaptld.core.sync.SyncAction
import com.ledinhthi.ontaptld.core.sync.SyncEntityType
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.review.domain.ReviewGrade

class Converters {
    // Lưu enum dưới dạng String để migration dễ đọc (docs Mục 7.3).
    @TypeConverter
    fun sourceToString(s: FlashcardSource): String = s.name

    @TypeConverter
    fun stringToSource(s: String): FlashcardSource = FlashcardSource.valueOf(s)

    @TypeConverter
    fun syncEntityTypeToString(t: SyncEntityType): String = t.name

    @TypeConverter
    fun stringToSyncEntityType(s: String): SyncEntityType = SyncEntityType.valueOf(s)

    @TypeConverter
    fun syncActionToString(a: SyncAction): String = a.name

    @TypeConverter
    fun stringToSyncAction(s: String): SyncAction = SyncAction.valueOf(s)

    @TypeConverter
    fun reviewGradeToString(g: ReviewGrade): String = g.name

    @TypeConverter
    fun stringToReviewGrade(s: String): ReviewGrade = ReviewGrade.valueOf(s)
}
