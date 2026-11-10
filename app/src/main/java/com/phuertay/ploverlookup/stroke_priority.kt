package com.phuertay.ploverlookup

import java.util.Locale

/**
 * One outline has one translation. Imported dictionaries are checked in the
 * order they were added, and the built-in sample is checked after them.
 * Inside one dictionary, the earlier entry wins. A winning `{plover:deleted}`
 * removes the stroke, so a later dictionary cannot bring it back.
 */
internal data class StrokeCandidate(
    val id: Long,
    val outlineKey: String,
    val translation: String,
    val builtIn: Boolean,
    val dictionaryId: Long,
)

internal fun isDeletedStroke(translation: String): Boolean {
    return translation.lowercase(Locale.US) == "{plover:deleted}"
}

internal fun winningEntryIds(entries: List<StrokeCandidate>): Set<Long> {
    val best = HashMap<String, StrokeCandidate>()
    for (entry in entries) {
        val current = best[entry.outlineKey]
        if (current == null || strokeComesFirst(entry, current)) {
            best[entry.outlineKey] = entry
        }
    }
    val winners = HashSet<Long>()
    for (entry in best.values) {
        if (!isDeletedStroke(entry.translation)) winners.add(entry.id)
    }
    return winners
}

internal fun unseenStrokes(
    entries: List<DictionaryEntryDraft>,
    seenOutlineKeys: MutableSet<String>,
): List<DictionaryEntryDraft> {
    if (entries.isEmpty()) return emptyList()
    val kept = ArrayList<DictionaryEntryDraft>(entries.size)
    for (entry in entries) {
        if (seenOutlineKeys.add(normalizeOutline(entry.outline))) {
            kept.add(entry)
        }
    }
    return kept
}

private fun strokeComesFirst(left: StrokeCandidate, right: StrokeCandidate): Boolean {
    if (left.builtIn != right.builtIn) return !left.builtIn
    if (left.dictionaryId != right.dictionaryId) return left.dictionaryId < right.dictionaryId
    return left.id < right.id
}
