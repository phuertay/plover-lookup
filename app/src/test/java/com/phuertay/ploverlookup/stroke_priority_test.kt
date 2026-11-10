package com.phuertay.ploverlookup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokePriorityTest {
    @Test
    fun isTheKeepsOnlyTheFirstDefinitionOfEachStroke() {
        val entries = listOf(
            entry(1, "S*T", "is the", dictionaryId = 1),
            entry(2, "S-T", "is it", dictionaryId = 1),
            entry(3, "ST", "is it", dictionaryId = 1),
            entry(4, "S-T", "is the", dictionaryId = 2),
            entry(5, "S*T", "St.{-|}", dictionaryId = 2),
        )

        assertEquals(listOf("S*T"), outlinesFor("is the", entries))
        assertEquals(listOf("S-T", "ST"), outlinesFor("is it", entries))
        assertEquals("is it", translationFor("S-T", entries))
        assertEquals("is the", translationFor("S*T", entries))
        assertEquals("Dictionary 1", entryFor("S*T", entries)?.dictionaryName)
    }

    @Test
    fun repeatedStrokeInsideOneDictionaryKeepsTheFirstEntry() {
        val entries = listOf(
            entry(1, "S-T", "is it"),
            entry(2, "S*T", "is the"),
            entry(3, "ST", "is it"),
            entry(4, "s - t", "is the"),
        )

        assertEquals(listOf("S*T"), outlinesFor("is the", entries))
        assertEquals(listOf("S-T", "ST"), outlinesFor("is it", entries))
        assertEquals("is it", translationFor("S-T", entries))
        assertEquals("S-T", entryFor("s-t", entries)?.outline)
    }

    @Test
    fun laterDictionaryCanStillDefineAStrokeTheFirstOneDoesNotHave() {
        val entries = listOf(
            entry(1, "S-T", "is it", dictionaryId = 1),
            entry(2, "ST", "is it", dictionaryId = 2),
        )

        assertEquals(listOf("S-T", "ST"), outlinesFor("is it", entries))
        assertEquals("is it", translationFor("ST", entries))
    }

    @Test
    fun deletedStrokeHidesLaterCopies() {
        val entries = listOf(
            entry(1, "S-T", "{PLOVER:deleted}", dictionaryId = 1),
            entry(2, "S-T", "is the", dictionaryId = 2),
            entry(3, "S*T", "is the", dictionaryId = 2),
        )

        assertNull(translationFor("S-T", entries))
        assertEquals(listOf("S*T"), outlinesFor("is the", entries))
    }

    @Test
    fun importedDictionaryWinsOverTheBuiltInSample() {
        val entries = listOf(
            entry(1, "THE", "the", dictionaryId = 1, builtIn = true, dictionaryName = "Sample"),
            entry(2, "THE", "thee", dictionaryId = 2, dictionaryName = "Personal"),
        )

        assertEquals("thee", translationFor("THE", entries))
        assertEquals("Personal", entryFor("THE", entries)?.dictionaryName)
    }

    @Test
    fun importKeepsTheFirstStrokeAcrossBatches() {
        val seen = HashSet<String>()
        val first = unseenStrokes(
            listOf(
                DictionaryEntryDraft("S-T", "is it"),
                DictionaryEntryDraft("S*T", "is the"),
                DictionaryEntryDraft("ST", "is it"),
            ),
            seen,
        )
        val second = unseenStrokes(
            listOf(
                DictionaryEntryDraft("S-T", "is the"),
                DictionaryEntryDraft("s - t", "was"),
                DictionaryEntryDraft("W-R", "were"),
            ),
            seen,
        )

        assertEquals(listOf("is it", "is the", "is it"), first.map { it.translation })
        assertEquals(listOf("were"), second.map { it.translation })
        assertTrue(second.none { normalizeOutline(it.outline) == "S-T" })
    }

    private fun outlinesFor(translation: String, entries: List<DictEntry>): List<String> {
        return visible(entries.filter { it.translation == translation }, entries).map { it.outline }
    }

    private fun translationFor(outline: String, entries: List<DictEntry>): String? {
        return entryFor(outline, entries)?.translation
    }

    private fun entryFor(outline: String, entries: List<DictEntry>): DictEntry? {
        val key = normalizeOutline(outline)
        return visible(entries.filter { it.outlineKey == key }, entries).singleOrNull()
    }

    private fun visible(matches: List<DictEntry>, definitions: List<DictEntry>): List<DictEntry> {
        val winners = winningEntryIds(definitions.map { it.candidate })
        val seen = HashSet<Long>()
        return matches.filter { it.id in winners && seen.add(it.id) }
    }

    private fun entry(
        id: Long,
        outline: String,
        translation: String,
        dictionaryId: Long = 1,
        builtIn: Boolean = false,
        dictionaryName: String = "Dictionary $dictionaryId",
    ): DictEntry {
        return DictEntry(id, outline, translation, dictionaryId, builtIn, dictionaryName)
    }

    private data class DictEntry(
        val id: Long,
        val outline: String,
        val translation: String,
        val dictionaryId: Long,
        val builtIn: Boolean,
        val dictionaryName: String,
    ) {
        val outlineKey: String = normalizeOutline(outline)
        val candidate = StrokeCandidate(id, outlineKey, translation, builtIn, dictionaryId)
    }
}
