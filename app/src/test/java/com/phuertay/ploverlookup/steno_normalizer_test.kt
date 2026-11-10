package com.phuertay.ploverlookup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StenoNormalizerTest {
    @Test
    fun normalizesOutlines() {
        assertEquals("KAT", normalizeOutline("kat"))
        assertEquals("KAT", normalizeOutline("  KAT  "))
        assertEquals("KAT", normalizeOutline("k a t"))
        assertEquals("KAT/KATS", normalizeOutline("kat/kats"))
        assertEquals("#12", normalizeOutline("#12"))
        assertEquals("I", normalizeOutline("i"))
    }

    @Test
    fun foldsTranslationsLikeTheLookupSite() {
        assertEquals("helloworld", normalizeTranslation("Hello World"))
        assertEquals("ing", normalizeTranslation("{^ing}"))
        assertEquals("ing", normalizeTranslation("{^ing^}"))
        assertEquals("cat", normalizeTranslation("{&cat}"))
        assertEquals("prefix", normalizeTranslation("{prefix^}"))
        assertEquals("cat", normalizeTranslation("Cat."))
        assertEquals("a.", normalizeTranslation("a."))
        assertEquals("{>}", normalizeTranslation("{>}"))
        assertEquals("{^ ^}", normalizeTranslation("{^ ^}"))
        assertEquals(".", normalizeTranslation("{.}"))
        assertEquals("end", normalizeTranslation("end{.}"))
        assertEquals("hello{^ing}", normalizeTranslation("hello {^ing}"))
        assertEquals("cat", normalizeTranslation("-cat"))
        assertEquals("--cat", normalizeTranslation("--cat"))
        assertEquals("cat", normalizeTranslation("cat-"))
        assertEquals("cat--", normalizeTranslation("cat--"))
        assertEquals("...", normalizeTranslation("..."))
    }

    @Test
    fun countsStrokes() {
        assertEquals(1, strokeCount("KAT"))
        assertEquals(2, strokeCount("KAT/KATS"))
        assertEquals(0, strokeCount(""))
    }

    @Test
    fun buildsInflectionVariants() {
        assertEquals(listOf("cats", "cat"), inflectionVariants("cats"))
        assertEquals(listOf("running", "runn", "run"), inflectionVariants("running"))
        assertEquals(listOf("making", "mak", "make"), inflectionVariants("making"))
        assertEquals(listOf("carried", "carry", "carrie", "carri"), inflectionVariants("carried"))
        assertEquals(listOf("studies", "studie", "studi", "study"), inflectionVariants("studies"))
        assertEquals(listOf("stopped", "stoppe", "stopp", "stop"), inflectionVariants("stopped"))
    }

    @Test
    fun prefixBoundMatchesSpacedTranslationsAndSkipsDifferentCase() {
        val prefix = normalizeTranslation("hello")
        val end = checkNotNull(prefixUpperBound(prefix))
        assertEquals("hello", prefix)
        assertEquals("hellp", end)
        val spaced = translationTrim("hello world")
        assertTrue(spaced >= prefix && spaced < end)
        val titled = translationTrim("Hello")
        assertFalse(titled >= prefix && titled < end)
        assertEquals("cau", prefixUpperBound("cat"))
        assertNull(prefixUpperBound(""))
    }

    @Test
    fun queryFormsCarryOutlineAndInflection() {
        val match = translationMatch("  Cats ")
        requireNotNull(match)
        assertEquals("CATS", match.outlineKey)
        assertEquals("Cats", match.rawTrimmed)
        assertEquals("cats", match.normalized)
        assertTrue(match.variants.contains("cat"))
        assertNull(translationMatch("   "))
    }
}
