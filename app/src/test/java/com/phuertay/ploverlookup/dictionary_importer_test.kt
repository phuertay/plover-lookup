package com.phuertay.ploverlookup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

class DictionaryImporterTest {
    @Test
    fun parsesEscapesAndSkipsNonStrings() {
        val json = """
            {
              "K\"AT": "c\"at",
              "SKIP": 12,
              "FLAG": false,
              "NIL": null,
              "NEST": {"A": 1},
              "LIST": [1, "x", {"B": true}],
              "HRO": "caf\u00e9",
              "NL": "a\nb",
              "SLASH": "a\/b"
            }
        """.trimIndent()
        val entries = mutableListOf<Pair<String, String>>()
        PloverJsonParser(charBufferSize = 8).parse(json.byteInputStream()) { outline, translation ->
            entries.add(outline to translation)
        }
        assertEquals(
            listOf(
                "K\"AT" to "c\"at",
                "HRO" to "café",
                "NL" to "a\nb",
                "SLASH" to "a/b",
            ),
            entries,
        )
    }

    @Test
    fun parsesEmptyObjectAndBom() {
        val entries = mutableListOf<Pair<String, String>>()
        PloverJsonParser().parse("{\n}".byteInputStream()) { outline, translation ->
            entries.add(outline to translation)
        }
        assertTrue(entries.isEmpty())
        val bom = "\uFEFF{\"KAT\":\"cat\"}"
        val bomEntries = mutableListOf<Pair<String, String>>()
        PloverJsonParser(charBufferSize = 1).parse(bom.byteInputStream()) { outline, translation ->
            bomEntries.add(outline to translation)
        }
        assertEquals(listOf("KAT" to "cat"), bomEntries)
    }

    @Test
    fun importYieldsBatchesBeforeReadingTheWholeFile() {
        val json = largeDictionary(entryCount = 4000)
        assertTrue(json.length > 200_000)
        val stream = CountingStream(json.toByteArray(StandardCharsets.UTF_8))
        var positionAtFirstInsert = -1
        val batches = mutableListOf<Int>()
        var largestPiece = 0
        var seen = 0
        var firstOutline = ""
        var lastTranslation = ""
        val sink = RecordingSink(
            onInsert = { entries ->
                if (positionAtFirstInsert < 0) positionAtFirstInsert = stream.position
                batches.add(entries.size)
                assertTrue(entries.size <= 50)
                for (entry in entries) {
                    if (seen == 0) firstOutline = entry.outline
                    lastTranslation = entry.translation
                    seen += 1
                    largestPiece = maxOf(largestPiece, entry.outline.length, entry.translation.length)
                    assertTrue(entry.outline.length < 100)
                    assertFalse(entry.translation.length == json.length)
                }
            },
        )
        val count = DictionaryImporter(parser = PloverJsonParser(charBufferSize = 64), batchSize = 50)
            .import(stream, sink)
        assertEquals(4000, count)
        assertEquals("K0", firstOutline)
        assertTrue(lastTranslation.startsWith("word-3999"))
        assertTrue(batches.size > 1)
        assertTrue(batches.all { it <= 50 })
        assertTrue(positionAtFirstInsert in 1 until json.length)
        assertTrue(positionAtFirstInsert < json.length / 2)
        assertTrue(largestPiece < json.length / 10)
        assertTrue(sink.committed)
        assertFalse(sink.rolledBack)
        assertTrue(stream.reads > 1)
    }

    @Test
    fun importRollsBackWhenTheFileEndsEarly() {
        val json = """{"KAT": "cat", "HRO": "hel"""
        val sink = RecordingSink()
        assertThrows(PloverJsonException::class.java) {
            DictionaryImporter(batchSize = 1).import(json.byteInputStream(), sink)
        }
        assertFalse(sink.committed)
        assertTrue(sink.rolledBack)
        assertEquals(1, sink.insertedEntries)
    }

    @Test
    fun sampleDictionaryParses() {
        val file = listOf(
            File("src/main/assets/sample_dictionary.json"),
            File("app/src/main/assets/sample_dictionary.json"),
        ).first { it.isFile }
        val entries = mutableListOf<Pair<String, String>>()
        file.inputStream().use { input ->
            PloverJsonParser().parse(input) { outline, translation ->
                entries.add(outline to translation)
            }
        }
        assertTrue(entries.contains("KAT" to "cat"))
        assertTrue(entries.contains("TP-PL" to "{.}"))
        assertTrue(entries.contains("KAFE" to "café"))
        assertTrue(entries.size < 40)
    }

    @Test
    fun lapwingDictionaryHasBriefsForItAndThe() {
        val file = listOf(
            File("src/main/assets/lapwing.json.gz"),
            File("app/src/main/assets/lapwing.json.gz"),
        ).first { it.isFile }
        val wanted = setOf("T", "-T", "EUT", "THE")
        val found = HashMap<String, String>()
        GZIPInputStream(FileInputStream(file)).use { input ->
            PloverJsonParser().parse(input) { outline, translation ->
                if (outline in wanted) found[outline] = translation
            }
        }
        assertEquals("it", found["T"])
        assertEquals("it", found["EUT"])
        assertEquals("the", found["-T"])
        assertEquals("they", found["THE"])
    }

    private fun largeDictionary(entryCount: Int): String {
        val builder = StringBuilder(entryCount * 80)
        builder.append('{')
        for (i in 0 until entryCount) {
            if (i > 0) builder.append(',')
            builder.append("\"K").append(i).append("\":\"word-").append(i).append('-')
            repeat(40) { builder.append('x') }
            builder.append('"')
        }
        builder.append('}')
        return builder.toString()
    }

    private class CountingStream(private val data: ByteArray) : InputStream() {
        var position: Int = 0
            private set
        var reads: Int = 0
            private set

        override fun read(): Int {
            if (position >= data.size) return -1
            reads += 1
            return data[position++].toInt() and 0xff
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (position >= data.size) return -1
            reads += 1
            val count = minOf(length, data.size - position)
            data.copyInto(buffer, offset, position, position + count)
            position += count
            return count
        }
    }

    private class RecordingSink(
        private val onInsert: (List<DictionaryEntryDraft>) -> Unit = {},
    ) : EntryBatchSink {
        var committed = false
        var rolledBack = false
        var insertedEntries = 0

        override fun begin() {}

        override fun insert(entries: List<DictionaryEntryDraft>) {
            insertedEntries += entries.size
            onInsert(entries)
        }

        override fun commit() {
            committed = true
        }

        override fun rollback() {
            rolledBack = true
        }
    }
}
