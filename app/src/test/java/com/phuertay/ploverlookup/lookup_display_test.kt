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

    @Test
    fun theBriefComesBeforeTheCapitalAndPhrases() {
        val groups = groupTranslations(
            listOf(
                TranslationRow("#-T/HO/PWEUT", "The Hobbit", 3, "Lapwing"),
                TranslationRow("# - T", "The", 1, "Lapwing"),
                TranslationRow("-T", "the", 1, "Lapwing"),
            ),
            "the",
        )
        assertEquals(listOf("the", "The", "The Hobbit"), groups.map { it.translation })
        assertEquals(listOf("-T"), groups[0].outlines.map { it.outline })
    }

    @Test
    fun capitalizedQueryListsCapitalizedPhrasesBeforeLowercase() {
        val groups = groupTranslations(
            listOf(
                TranslationRow("THEPL", "them", 1, "Lapwing"),
                TranslationRow("THEPL", "Them", 1, "Lapwing"),
                TranslationRow("#-T/HO/PWEUT", "The Hobbit", 3, "Lapwing"),
                TranslationRow("-T", "the", 1, "Lapwing"),
                TranslationRow("#-T", "The", 1, "Lapwing"),
                TranslationRow("THR", "there", 1, "Lapwing"),
            ),
            "The",
        )
        assertEquals(
            listOf("The", "the", "Them", "The Hobbit", "them", "there"),
            groups.map { it.translation },
        )
    }
}
