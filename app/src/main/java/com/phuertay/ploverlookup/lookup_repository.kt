package com.phuertay.ploverlookup

import android.content.Context
import java.io.InputStream

class LookupRepository(
    private val context: Context,
    private val database: LookupDatabase,
) {
    private val dao = database.lookupDao()

    fun dictionaries(): List<Dictionary> = dao.dictionaries()

    fun ensureSample() {
        if (dao.dictionaryCount() > 0) return
        context.assets.open("sample_dictionary.json").use { input ->
            importStream("Sample", input, builtIn = true) {}
        }
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        dao.setEnabled(id, enabled)
    }

    fun deleteImported(id: Long) {
        dao.deleteImported(id)
    }

    fun search(rawQuery: String): LookupResults {
        val match = translationMatch(rawQuery) ?: return LookupResults.empty
        val strokes = dao.exactOutlines(match.outlineKey)
        val exact = LinkedHashMap<String, TranslationRow>()
        var truncated = absorb(dao.byExactTranslation(match.rawTrimmed), exact)
        truncated = absorb(dao.byTranslationKey(match.variants), exact) || truncated
        truncated = absorb(dao.byTranslationTrim(match.variants), exact) || truncated
        val prefix = LinkedHashMap<String, TranslationRow>()
        val prefixEnd = match.prefixEnd
        if (prefixEnd != null) {
            truncated = absorb(dao.byTranslationKeyPrefix(match.normalized, prefixEnd), prefix) || truncated
            truncated = absorb(dao.byTranslationTrimPrefix(match.normalized, prefixEnd), prefix) || truncated
        }
        val prefixOnly = prefix.filterKeys { it !in exact }
        val combined = ArrayList<TranslationRow>(exact.size + prefixOnly.size)
        combined.addAll(exact.values)
        combined.addAll(prefixOnly.values)
        val chosen = if (combined.size <= LOOKUP_ROW_LIMIT) {
            combined
        } else {
            truncated = true
            val exactRows = exact.values.toList()
            if (exactRows.size >= LOOKUP_ROW_LIMIT) {
                exactRows.take(LOOKUP_ROW_LIMIT)
            } else {
                exactRows + prefixOnly.values.take(LOOKUP_ROW_LIMIT - exactRows.size)
            }
        }
        return LookupResults(
            strokeHits = strokes,
            translations = groupTranslations(chosen),
            truncated = truncated,
        )
    }

    /**
     * Parses [input] as a stream and inserts batches inside one database transaction.
     * [onProgress] receives the number of entries read so far.
     */
    fun importStream(
        name: String,
        input: InputStream,
        builtIn: Boolean,
        onProgress: (Int) -> Unit,
    ): Int {
        var imported = 0
        database.runInTransaction {
            val dictionaryId = dao.insertDictionary(
                Dictionary(
                    name = name.ifBlank { "Dictionary" },
                    builtIn = builtIn,
                    enabled = true,
                    entryCount = 0,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            val sink = object : EntryBatchSink {
                private var count = 0

                override fun begin() {}

                override fun insert(entries: List<DictionaryEntryDraft>) {
                    dao.insertEntries(entries.map { it.toEntry(dictionaryId) })
                    count += entries.size
                    onProgress(count)
                }

                override fun commit() {
                    dao.setEntryCount(dictionaryId, count)
                }

                override fun rollback() {}
            }
            imported = DictionaryImporter().import(input, sink)
        }
        return imported
    }

    private fun absorb(
        rows: List<TranslationRow>,
        target: LinkedHashMap<String, TranslationRow>,
    ): Boolean {
        for (row in rows) {
            val key = row.dictionaryName + "\u0000" + row.translation + "\u0000" + row.outline
            target.putIfAbsent(key, row)
        }
        return rows.size >= LOOKUP_ROW_LIMIT
    }

    private fun DictionaryEntryDraft.toEntry(dictionaryId: Long): Entry {
        return Entry(
            dictionaryId = dictionaryId,
            outline = outline,
            outlineKey = normalizeOutline(outline),
            translation = translation,
            translationKey = normalizeTranslation(translation),
            translationTrim = translationTrim(translation),
            strokeCount = strokeCount(outline),
        )
    }
}
