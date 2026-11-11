package com.phuertay.ploverlookup

import androidx.room.ColumnInfo

const val LOOKUP_ROW_LIMIT = 500

data class StrokeHit(
    @ColumnInfo(name = "outline") val outline: String,
    @ColumnInfo(name = "translation") val translation: String,
    @ColumnInfo(name = "dictionary_name") val dictionaryName: String,
)

data class TranslationRow(
    @ColumnInfo(name = "outline") val outline: String,
    @ColumnInfo(name = "translation") val translation: String,
    @ColumnInfo(name = "stroke_count") val strokeCount: Int,
    @ColumnInfo(name = "dictionary_name") val dictionaryName: String,
)

data class RankedEntry(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "outline") val outline: String,
    @ColumnInfo(name = "outline_key") val outlineKey: String,
    @ColumnInfo(name = "translation") val translation: String,
    @ColumnInfo(name = "stroke_count") val strokeCount: Int,
    @ColumnInfo(name = "dictionary_name") val dictionaryName: String,
    @ColumnInfo(name = "built_in") val builtIn: Boolean,
    @ColumnInfo(name = "dictionary_id") val dictionaryId: Long,
)

data class OutlineRow(
    val outline: String,
    val strokeCount: Int,
    val extraStrokes: Int,
)

data class TranslationGroup(
    val translation: String,
    val dictionaryName: String,
    val outlines: List<OutlineRow>,
)

data class OutlineLine(
    val outline: String,
    val dictionaryName: String,
)

data class TranslationLines(
    val translation: String,
    val outlines: List<OutlineLine>,
)

data class LookupResults(
    val strokeHits: List<StrokeHit>,
    val translations: List<TranslationGroup>,
    val truncated: Boolean,
) {
    companion object {
        val empty = LookupResults(emptyList(), emptyList(), false)
    }
}

fun translationLines(groups: List<TranslationGroup>): List<TranslationLines> {
    val grouped = LinkedHashMap<String, MutableList<OutlineLine>>()
    for (group in groups) {
        val outlines = grouped.getOrPut(group.translation) { mutableListOf() }
        for (row in group.outlines) {
            outlines.add(OutlineLine(row.outline, group.dictionaryName))
        }
    }
    return grouped.map { (translation, outlines) ->
        TranslationLines(translation, outlines)
    }
}

fun groupTranslations(rows: List<TranslationRow>): List<TranslationGroup> {
    val sorted = rows.sortedWith(compareBy({ it.translation }, { it.strokeCount }, { it.outline }))
    val grouped = LinkedHashMap<Pair<String, String>, MutableList<TranslationRow>>()
    for (row in sorted) {
        grouped.getOrPut(row.translation to row.dictionaryName) { mutableListOf() }.add(row)
    }
    return grouped.map { (key, items) ->
        val distinct = items.distinctBy { it.outline }
        val shortest = distinct.minOf { it.strokeCount }
        TranslationGroup(
            translation = key.first,
            dictionaryName = key.second,
            outlines = distinct.map { row ->
                OutlineRow(
                    outline = row.outline,
                    strokeCount = row.strokeCount,
                    extraStrokes = row.strokeCount - shortest,
                )
            },
        )
    }
}
