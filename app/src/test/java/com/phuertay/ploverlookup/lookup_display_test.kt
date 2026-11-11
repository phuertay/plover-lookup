package com.phuertay.ploverlookup

import org.junit.Assert.assertEquals
import org.junit.Test

class LookupDisplayTest {
    @Test
    fun dictionaryNameStaysWithEachStrokeUnderOneTranslation() {
        val lines = translationLines(
            listOf(
                TranslationGroup(
                    translation = "is it",
                    dictionaryName = "Personal",
                    outlines = listOf(OutlineRow("S-T", strokeCount = 1, extraStrokes = 0)),
                ),
                TranslationGroup(
                    translation = "is it",
                    dictionaryName = "main",
                    outlines = listOf(OutlineRow("ST", strokeCount = 1, extraStrokes = 0)),
                ),
                TranslationGroup(
                    translation = "is the",
                    dictionaryName = "Personal",
                    outlines = listOf(OutlineRow("S*T", strokeCount = 1, extraStrokes = 0)),
                ),
            ),
        )

        assertEquals(listOf("is it", "is the"), lines.map { it.translation })
        assertEquals(
            listOf("S-T" to "Personal", "ST" to "main"),
            lines[0].outlines.map { it.outline to it.dictionaryName },
        )
        assertEquals(listOf("S*T" to "Personal"), lines[1].outlines.map { it.outline to it.dictionaryName })
    }
}
