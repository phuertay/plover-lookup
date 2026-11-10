package com.phuertay.ploverlookup

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry",
    foreignKeys = [
        ForeignKey(
            entity = Dictionary::class,
            parentColumns = ["id"],
            childColumns = ["dictionary_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(name = "idx_entry_outline", value = ["outline"]),
        Index(name = "idx_entry_outline_key", value = ["outline_key"]),
        Index(name = "idx_entry_translation", value = ["translation"]),
        Index(name = "idx_entry_translation_key", value = ["translation_key"]),
        Index(name = "idx_entry_translation_trim", value = ["translation_trim"]),
        Index(name = "idx_entry_dictionary_id", value = ["dictionary_id"]),
    ],
)
data class Entry(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "dictionary_id")
    val dictionaryId: Long,
    @ColumnInfo(name = "outline")
    val outline: String,
    @ColumnInfo(name = "outline_key")
    val outlineKey: String,
    @ColumnInfo(name = "translation")
    val translation: String,
    @ColumnInfo(name = "translation_key")
    val translationKey: String,
    @ColumnInfo(name = "translation_trim")
    val translationTrim: String,
    @ColumnInfo(name = "stroke_count")
    val strokeCount: Int,
)
