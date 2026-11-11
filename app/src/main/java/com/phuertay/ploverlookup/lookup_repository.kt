package com.phuertay.ploverlookup

import android.content.Context
import java.io.InputStream

private const val OUTLINE_KEY_CHUNK = 500

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
        val exactRaw = ArrayList<RankedEntry>()
        var truncated = collect(dao.byExactTranslation(match.rawTrimmed), exactRaw)
        truncated = collect(dao.byTranslationKey(match.variants), exactRaw) || truncated
        truncated = collect(dao.byTranslationTrim(match.variants), exactRaw) || truncated
        val prefixRaw = ArrayList<RankedEntry>()
        val prefixEnd = match.prefixEnd
        if (prefixEnd != null) {
            truncated = collect(dao.byTranslationKeyPrefix(match.normalized, prefixEnd), prefixRaw) || truncated
            truncated = collect(dao.byTranslationTrimPrefix(match.normalized, prefixEnd), prefixRaw) || truncated
        }
        val candidateKeys = ArrayList<String>(1 + exactRaw.size + prefixRaw.size)
        candidateKeys.add(match.outlineKey)
        for (row in exactRaw) candidateKeys.add(row.outlineKey)
        for (row in prefixRaw) candidateKeys.add(row.outlineKey)
        val competitors = entriesForKeys(candidateKeys)
        val winners = winningEntryIds(competitors.map { it.toCandidate() })
        val exact = LinkedHashMap<String, TranslationRow>()
        absorb(keepWinners(exactRaw, winners).map { it.toTranslationRow() }, exact)
        val prefix = LinkedHashMap<String, TranslationRow>()
        absorb(
            keepWinners(prefixRaw, winners)
                .filter { translationStartsWithQuery(it.translation, match.rawTrimmed) }
                .map { it.toTranslationRow() },
            prefix,
        )
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
        val strokeHits = keepWinners(
            competitors.filter { it.outlineKey == match.outlineKey },
            winners,
        ).map { row ->
            StrokeHit(row.outline, row.translation, row.dictionaryName)
        }
        return LookupResults(
            strokeHits = strokeHits,
            translations = groupTranslations(chosen),
            truncated = truncated,
        )
    }

    /**
     * Parses [input] as a stream and inserts batches inside one database transaction.
     * [onProgress] receives the number of strokes stored so far. A repeated stroke is
     * not stored again.
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
                private val seenOutlineKeys = HashSet<String>()

                override fun begin() {}

                override fun insert(entries: List<DictionaryEntryDraft>) {
                    val fresh = unseenStrokes(entries, seenOutlineKeys)
                    if (fresh.isNotEmpty()) {
                        dao.insertEntries(fresh.map { it.toEntry(dictionaryId) })
                        count += fresh.size
                    }
                    onProgress(count)
                }

                override fun commit() {
                    dao.setEntryCount(dictionaryId, count)
                    imported = count
                }

                override fun rollback() {}
            }
            DictionaryImporter().import(input, sink)
        }
        return imported
    }

    private fun collect(rows: List<RankedEntry>, target: MutableList<RankedEntry>): Boolean {
        target.addAll(rows)
        return rows.size >= LOOKUP_ROW_LIMIT
    }

    private fun entriesForKeys(outlineKeys: List<String>): List<RankedEntry> {
        val keys = outlineKeys.filter { it.isNotEmpty() }.distinct()
        if (keys.isEmpty()) return emptyList()
        val rows = ArrayList<RankedEntry>()
        for (chunk in keys.chunked(OUTLINE_KEY_CHUNK)) {
            rows.addAll(dao.byOutlineKeys(chunk))
        }
        return rows
    }

    private fun keepWinners(rows: List<RankedEntry>, winners: Set<Long>): List<RankedEntry> {
        if (rows.isEmpty() || winners.isEmpty()) return emptyList()
        val seen = HashSet<Long>()
        val kept = ArrayList<RankedEntry>()
        for (row in rows) {
            if (row.id in winners && seen.add(row.id)) kept.add(row)
        }
        return kept
    }

    private fun absorb(
        rows: List<TranslationRow>,
        target: LinkedHashMap<String, TranslationRow>,
    ) {
        for (row in rows) {
            val key = row.dictionaryName + "\u0000" + row.translation + "\u0000" + row.outline
            target.putIfAbsent(key, row)
        }
    }

    private fun RankedEntry.toCandidate(): StrokeCandidate {
        return StrokeCandidate(
            id = id,
            outlineKey = outlineKey,
            translation = translation,
            builtIn = builtIn,
            dictionaryId = dictionaryId,
        )
    }

    private fun RankedEntry.toTranslationRow(): TranslationRow {
        return TranslationRow(
            outline = outline,
            translation = translation,
            strokeCount = strokeCount,
            dictionaryName = dictionaryName,
        )
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
