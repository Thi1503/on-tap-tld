package com.ledinhthi.ontaptld.core.data.local.db

import androidx.room.TypeConverter
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource

class Converters {
    // Lưu enum dưới dạng String để migration dễ đọc (docs Mục 7.3).
    @TypeConverter
    fun sourceToString(s: FlashcardSource): String = s.name

    @TypeConverter
    fun stringToSource(s: String): FlashcardSource = FlashcardSource.valueOf(s)
}
