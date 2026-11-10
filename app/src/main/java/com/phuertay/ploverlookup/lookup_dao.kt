package com.phuertay.ploverlookup

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LookupDao {
    @Query("SELECT COUNT(*) FROM dictionary")
    fun dictionaryCount(): Int

    @Query("SELECT * FROM dictionary ORDER BY built_in ASC, id ASC")
    fun dictionaries(): List<Dictionary>

    @Insert
    fun insertDictionary(dictionary: Dictionary): Long

    @Query("UPDATE dictionary SET enabled = :enabled WHERE id = :id")
    fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE dictionary SET entry_count = :entryCount WHERE id = :id")
    fun setEntryCount(id: Long, entryCount: Int)

    @Query("DELETE FROM dictionary WHERE id = :id AND built_in = 0")
    fun deleteImported(id: Long)

    @Insert
    fun insertEntries(entries: List<Entry>)

    @Query(
        """
        SELECT e.id AS id,
               e.outline AS outline,
               e.outline_key AS outline_key,
               e.translation AS translation,
               e.stroke_count AS stroke_count,
               d.name AS dictionary_name,
               d.built_in AS built_in,
               d.id AS dictionary_id
        FROM entry AS e
        INNER JOIN dictionary AS d ON d.id = e.dictionary_id
        WHERE d.enabled = 1 AND e.outline_key IN (:outlineKeys)
        """,
    )
    fun byOutlineKeys(outlineKeys: List<String>): List<RankedEntry>

    @Query(
        """
        SELECT e.id AS id,
               e.outline AS outline,
               e.outline_key AS outline_key,
               e.translation AS translation,
               e.stroke_count AS stroke_count,
               d.name AS dictionary_name,
               d.built_in AS built_in,
               d.id AS dictionary_id
        FROM entry AS e
        INNER JOIN dictionary AS d ON d.id = e.dictionary_id
        WHERE d.enabled = 1 AND e.translation = :translation
        ORDER BY e.translation ASC, e.stroke_count ASC, e.outline ASC
        LIMIT 500
        """,
    )
    fun byExactTranslation(translation: String): List<RankedEntry>

    @Query(
        """
        SELECT e.id AS id,
               e.outline AS outline,
               e.outline_key AS outline_key,
               e.translation AS translation,
               e.stroke_count AS stroke_count,
               d.name AS dictionary_name,
               d.built_in AS built_in,
               d.id AS dictionary_id
        FROM entry AS e
        INNER JOIN dictionary AS d ON d.id = e.dictionary_id
        WHERE d.enabled = 1 AND e.translation_key IN (:variants)
        ORDER BY e.translation ASC, e.stroke_count ASC, e.outline ASC
        LIMIT 500
        """,
    )
    fun byTranslationKey(variants: List<String>): List<RankedEntry>

    @Query(
        """
        SELECT e.id AS id,
               e.outline AS outline,
               e.outline_key AS outline_key,
               e.translation AS translation,
               e.stroke_count AS stroke_count,
               d.name AS dictionary_name,
               d.built_in AS built_in,
               d.id AS dictionary_id
        FROM entry AS e
        INNER JOIN dictionary AS d ON d.id = e.dictionary_id
        WHERE d.enabled = 1 AND e.translation_trim IN (:variants)
        ORDER BY e.translation ASC, e.stroke_count ASC, e.outline ASC
        LIMIT 500
        """,
    )
    fun byTranslationTrim(variants: List<String>): List<RankedEntry>

    @Query(
        """
        SELECT e.id AS id,
               e.outline AS outline,
               e.outline_key AS outline_key,
               e.translation AS translation,
               e.stroke_count AS stroke_count,
               d.name AS dictionary_name,
               d.built_in AS built_in,
               d.id AS dictionary_id
        FROM entry AS e
        INNER JOIN dictionary AS d ON d.id = e.dictionary_id
        WHERE d.enabled = 1
          AND e.translation_key >= :prefix
          AND e.translation_key < :prefixEnd
        ORDER BY e.translation ASC, e.stroke_count ASC, e.outline ASC
        LIMIT 500
        """,
    )
    fun byTranslationKeyPrefix(prefix: String, prefixEnd: String): List<RankedEntry>

    @Query(
        """
        SELECT e.id AS id,
               e.outline AS outline,
               e.outline_key AS outline_key,
               e.translation AS translation,
               e.stroke_count AS stroke_count,
               d.name AS dictionary_name,
               d.built_in AS built_in,
               d.id AS dictionary_id
        FROM entry AS e
        INNER JOIN dictionary AS d ON d.id = e.dictionary_id
        WHERE d.enabled = 1
          AND e.translation_trim >= :prefix
          AND e.translation_trim < :prefixEnd
        ORDER BY e.translation ASC, e.stroke_count ASC, e.outline ASC
        LIMIT 500
        """,
    )
    fun byTranslationTrimPrefix(prefix: String, prefixEnd: String): List<RankedEntry>
}
