package com.phuertay.ploverlookup

import java.io.InputStream

data class DictionaryEntryDraft(
    val outline: String,
    val translation: String,
)

interface EntryBatchSink {
    fun begin()
    fun insert(entries: List<DictionaryEntryDraft>)
    fun commit()
    fun rollback()
}

/**
 * Streams a Plover JSON file into [sink] in batches, inside one begin/commit.
 * A failure rolls the sink back and does not commit.
 */
class DictionaryImporter(
    private val parser: PloverJsonParser = PloverJsonParser(),
    private val batchSize: Int = 400,
) {
    init {
        require(batchSize > 0)
    }

    fun import(input: InputStream, sink: EntryBatchSink): Int {
        sink.begin()
        val batch = ArrayList<DictionaryEntryDraft>(batchSize)
        var count = 0
        try {
            parser.parse(input) { outline, translation ->
                batch.add(DictionaryEntryDraft(outline, translation))
                count += 1
                if (batch.size >= batchSize) {
                    sink.insert(batch.toList())
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) {
                sink.insert(batch.toList())
                batch.clear()
            }
            sink.commit()
            return count
        } catch (error: Exception) {
            try {
                sink.rollback()
            } catch (rollback: Exception) {
                error.addSuppressed(rollback)
            }
            throw error
        }
    }
}
