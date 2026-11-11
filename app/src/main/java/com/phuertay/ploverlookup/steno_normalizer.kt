package com.phuertay.ploverlookup

import java.util.Locale

/**
 * Folds a Plover translation the way the lim.au dictionary lookup does:
 * drop spaces and a few spacing metas, unwrap a single brace affix, then
 * lowercase. An empty fold falls back to the newline-escaped original.
 */
fun normalizeTranslation(translation: String): String {
    val escaped = escapeNewlines(translation)
    var text = escaped.replace(" ", "")
    text = text.replace("{>}", "")
    text = text.replace("{-|}", "")
    text = text.replace("{.}", ".")
    text = text.replace("{!}", "!")
    text = text.replace("{?}", "?")
    text = text.replace("{;}", ";")
    text = text.replace("{:}", ":")
    text = text.replace("{,}", ",")
    if (text.startsWith("{") && text.endsWith("}")) {
        text = unwrapBraceAffix(text)
    }
    if (text.length > 2 && text[text.length - 1] == '.' && text[text.length - 2] != '.') {
        text = text.substring(0, text.length - 1)
    }
    if (text.length > 2 && text[0] == '-' && text[1] != '-') {
        text = text.substring(1)
    }
    if (text.length > 2 && text[text.length - 1] == '-' && text[text.length - 2] != '-') {
        text = text.substring(0, text.length - 1)
    }
    if (text.isEmpty()) {
        text = escaped
    }
    return text.lowercase(Locale.US)
}

fun translationTrim(translation: String): String {
    return escapeNewlines(translation).trim()
}

/**
 * Prefix matches keep spaces. `they` matches `they're`, and it does not match
 * `the year`: folding that to `theyear` is only for an exact match.
 */
fun translationStartsWithQuery(translation: String, rawQuery: String): Boolean {
    val query = rawQuery.trim().lowercase(Locale.US)
    if (query.isEmpty()) return false
    return translation.trim().lowercase(Locale.US).startsWith(query)
}

/** Trim, drop spaces, and uppercase. `kat` and `K A T` both become `KAT`. */
fun normalizeOutline(outline: String): String {
    val trimmed = outline.trim()
    val compact = buildString(trimmed.length) {
        for (ch in trimmed) {
            if (ch != ' ') append(ch)
        }
    }
    return compact.uppercase(Locale.US)
}

fun strokeCount(outline: String): Int {
    if (outline.isEmpty()) return 0
    var strokes = 1
    for (ch in outline) {
        if (ch == '/') strokes += 1
    }
    return strokes
}

/**
 * Extra exact keys for the lim.au lookup: plural, past, and -ing forms of
 * the already-normalized query. Order matches that lookup, without duplicates.
 */
fun inflectionVariants(normalized: String): List<String> {
    val variants = LinkedHashSet<String>()
    variants.add(normalized)
    if (normalized.endsWith("s")) variants.add(normalized.dropLast(1))
    if (normalized.endsWith("es")) variants.add(normalized.dropLast(2))
    if (normalized.endsWith("ies") || normalized.endsWith("ied")) {
        variants.add(normalized.dropLast(3) + "y")
    }
    if (normalized.endsWith("d")) variants.add(normalized.dropLast(1))
    if (normalized.endsWith("ed")) {
        variants.add(normalized.dropLast(2))
        if (normalized.length > 3 &&
            normalized[normalized.length - 3] == normalized[normalized.length - 4]
        ) {
            variants.add(normalized.dropLast(3))
        }
    }
    if (normalized.endsWith("ing")) {
        val stem = normalized.dropLast(3)
        variants.add(stem)
        if (normalized.length > 4 &&
            normalized[normalized.length - 4] == normalized[normalized.length - 5]
        ) {
            variants.add(normalized.dropLast(4))
        } else {
            variants.add(stem + "e")
        }
    }
    return variants.toList()
}

/** Exclusive upper bound for a case-sensitive prefix range. Null if [prefix] is empty. */
fun prefixUpperBound(prefix: String): String? {
    if (prefix.isEmpty()) return null
    val chars = prefix.toCharArray()
    val last = chars[chars.lastIndex]
    if (last == Char.MAX_VALUE) return null
    chars[chars.lastIndex] = last + 1
    return String(chars)
}

data class TranslationMatch(
    val rawTrimmed: String,
    val normalized: String,
    val variants: List<String>,
    val prefixEnd: String?,
    val outlineKey: String,
)

fun translationMatch(rawQuery: String): TranslationMatch? {
    val trimmed = rawQuery.trim()
    if (trimmed.isEmpty()) return null
    val normalized = normalizeTranslation(trimmed)
    return TranslationMatch(
        rawTrimmed = trimmed,
        normalized = normalized,
        variants = inflectionVariants(normalized),
        prefixEnd = if (normalized.isEmpty()) null else prefixUpperBound(normalized),
        outlineKey = normalizeOutline(trimmed),
    )
}

private fun escapeNewlines(translation: String): String {
    return translation.replace("\n", "\\n")
}

private fun unwrapBraceAffix(text: String): String {
    return when {
        text.startsWith("{^") -> {
            val rest = text.substring(2)
            if (rest.endsWith("^}")) {
                if (rest.length >= 2) rest.substring(0, rest.length - 2) else ""
            } else if (rest.isNotEmpty()) {
                rest.substring(0, rest.length - 1)
            } else {
                ""
            }
        }
        text.startsWith("{&") -> {
            if (text.length >= 3) text.substring(2, text.length - 1) else ""
        }
        text.endsWith("^}") -> {
            if (text.length >= 3) text.substring(1, text.length - 2) else ""
        }
        else -> text
    }
}
