package com.phuertay.ploverlookup

import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.StandardCharsets

class PloverJsonException(message: String) : Exception(message)

/**
 * Reads a flat Plover JSON object from a stream.
 * Entries are delivered one at a time. The file is never copied into one string.
 * Values that are not strings are skipped, matching the lim.au lookup.
 */
class PloverJsonParser(
    private val charBufferSize: Int = 2048,
) {
    init {
        require(charBufferSize > 0)
    }

    fun parse(input: InputStream, onEntry: (outline: String, translation: String) -> Unit) {
        parse(InputStreamReader(input, StandardCharsets.UTF_8), onEntry)
    }

    fun parse(reader: Reader, onEntry: (outline: String, translation: String) -> Unit) {
        val source = CharSource(reader, charBufferSize)
        source.skipWhitespace()
        if (source.peek() == '\uFEFF'.code) {
            source.next()
            source.skipWhitespace()
        }
        if (source.peek() < 0) {
            throw PloverJsonException("Expected a JSON object of outlines")
        }
        source.expect('{')
        source.skipWhitespace()
        if (source.peek() == '}'.code) {
            source.next()
            source.finish()
            return
        }
        while (true) {
            source.skipWhitespace()
            val outline = source.parseString()
            source.skipWhitespace()
            source.expect(':')
            source.skipWhitespace()
            if (source.peek() == '"'.code) {
                onEntry(outline, source.parseString())
            } else {
                source.skipValue()
            }
            source.skipWhitespace()
            when (source.peek()) {
                ','.code -> source.next()
                '}'.code -> {
                    source.next()
                    source.finish()
                    return
                }
                else -> throw PloverJsonException(
                    "Expected ',' or '}' in the dictionary JSON at ${source.position}",
                )
            }
        }
    }
}

private class CharSource(
    private val reader: Reader,
    private val buffer: CharArray,
) {
    private var length = 0
    private var index = 0
    var position: Int = 0
        private set

    constructor(reader: Reader, size: Int) : this(reader, CharArray(size))

    fun peek(): Int {
        if (!fill()) return -1
        return buffer[index].code
    }

    fun next(): Int {
        val value = peek()
        if (value < 0) {
            throw PloverJsonException("The dictionary JSON ended early at $position")
        }
        index += 1
        position += 1
        return value
    }

    fun skipWhitespace() {
        while (true) {
            when (peek()) {
                ' '.code, '\n'.code, '\r'.code, '\t'.code -> next()
                else -> return
            }
        }
    }

    fun expect(expected: Char) {
        val got = next()
        if (got != expected.code) {
            throw PloverJsonException("Expected '$expected' in the dictionary JSON at $position")
        }
    }

    fun finish() {
        skipWhitespace()
        if (peek() >= 0) {
            throw PloverJsonException("Unexpected trailing data in the dictionary JSON at $position")
        }
    }

    fun parseString(): String {
        expect('"')
        val out = StringBuilder()
        while (true) {
            when (val value = next()) {
                '"'.code -> return out.toString()
                '\\'.code -> out.append(escaped())
                else -> {
                    if (value < 0x20) {
                        throw PloverJsonException("Invalid character in a dictionary string at $position")
                    }
                    out.append(value.toChar())
                }
            }
        }
    }

    fun skipValue() {
        skipWhitespace()
        when (peek()) {
            '"'.code -> parseString()
            '{'.code -> skipObject()
            '['.code -> skipArray()
            't'.code -> expectLiteral("true")
            'f'.code -> expectLiteral("false")
            'n'.code -> expectLiteral("null")
            '-'.code, in '0'.code..'9'.code -> skipNumber()
            else -> throw PloverJsonException("Expected a value in the dictionary JSON at $position")
        }
    }

    private fun fill(): Boolean {
        if (index < length) return true
        val read = reader.read(buffer)
        if (read == 0) {
            throw PloverJsonException("The dictionary JSON ended early at $position")
        }
        length = read
        index = 0
        return length > 0
    }

    private fun escaped(): Char {
        return when (val value = next()) {
            '"'.code -> '"'
            '\\'.code -> '\\'
            '/'.code -> '/'
            'b'.code -> '\b'
            'f'.code -> '\u000C'
            'n'.code -> '\n'
            'r'.code -> '\r'
            't'.code -> '\t'
            'u'.code -> {
                var code = 0
                repeat(4) {
                    code = (code shl 4) or hex(next())
                }
                code.toChar()
            }
            else -> throw PloverJsonException("Invalid escape in a dictionary string at $position")
        }
    }

    private fun hex(value: Int): Int {
        return when (value) {
            in '0'.code..'9'.code -> value - '0'.code
            in 'a'.code..'f'.code -> value - 'a'.code + 10
            in 'A'.code..'F'.code -> value - 'A'.code + 10
            else -> throw PloverJsonException("Invalid unicode escape at $position")
        }
    }

    private fun expectLiteral(literal: String) {
        for (ch in literal) {
            val got = next()
            if (got != ch.code) {
                throw PloverJsonException("Expected $literal in the dictionary JSON at $position")
            }
        }
    }

    private fun skipObject() {
        expect('{')
        skipWhitespace()
        if (peek() == '}'.code) {
            next()
            return
        }
        while (true) {
            skipWhitespace()
            parseString()
            skipWhitespace()
            expect(':')
            skipValue()
            skipWhitespace()
            when (peek()) {
                ','.code -> next()
                '}'.code -> {
                    next()
                    return
                }
                else -> throw PloverJsonException(
                    "Expected ',' or '}' in the dictionary JSON at $position",
                )
            }
        }
    }

    private fun skipArray() {
        expect('[')
        skipWhitespace()
        if (peek() == ']'.code) {
            next()
            return
        }
        while (true) {
            skipValue()
            skipWhitespace()
            when (peek()) {
                ','.code -> next()
                ']'.code -> {
                    next()
                    return
                }
                else -> throw PloverJsonException(
                    "Expected ',' or ']' in the dictionary JSON at $position",
                )
            }
        }
    }

    private fun skipNumber() {
        if (peek() == '-'.code) next()
        if (peek() !in '0'.code..'9'.code) {
            throw PloverJsonException("Expected a number in the dictionary JSON at $position")
        }
        while (peek() in '0'.code..'9'.code) next()
        if (peek() == '.'.code) {
            next()
            if (peek() !in '0'.code..'9'.code) {
                throw PloverJsonException("Expected a digit in the dictionary JSON at $position")
            }
            while (peek() in '0'.code..'9'.code) next()
        }
        if (peek() == 'e'.code || peek() == 'E'.code) {
            next()
            if (peek() == '+'.code || peek() == '-'.code) next()
            if (peek() !in '0'.code..'9'.code) {
                throw PloverJsonException("Expected a digit in the dictionary JSON at $position")
            }
            while (peek() in '0'.code..'9'.code) next()
        }
    }
}
