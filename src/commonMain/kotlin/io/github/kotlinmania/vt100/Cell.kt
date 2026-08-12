// port-lint: source cell.rs
package io.github.kotlinmania.vt100

import io.github.kotlinmania.unicodewidth.unicodeWidth

// chosen to make the size of the cell struct 32 bytes
private const val CONTENT_BYTES: Int = 22

private const val IS_WIDE: Int = 0b1000_0000
private const val IS_WIDE_CONTINUATION: Int = 0b0100_0000
private const val LEN_BITS: Int = 0b0001_1111

/**
 * Represents a single terminal cell.
 *
 * The upstream implementation packs UTF-8 bytes, a length+flag byte, and the
 * [Attrs] together so the value occupies exactly 32 bytes. Kotlin does not
 * provide fixed-size inline arrays, so the equivalent layout is approximated
 * with a [ByteArray] sized to [CONTENT_BYTES] plus a packed [Int] length/flag
 * field matching the upstream bit layout.
 */
public class Cell internal constructor() {
    private val contents: ByteArray = ByteArray(CONTENT_BYTES)
    private var lenAndFlags: Int = 0
    private var attrs: Attrs = Attrs()

    private fun len(): Int = lenAndFlags and LEN_BITS

    internal fun set(c: Int, a: Attrs) {
        lenAndFlags = 0
        appendChar(0, c)
        // strings in this context should always be an arbitrary character
        // followed by zero or more zero-width characters, so we should only
        // have to look at the first character
        setWide((c.unicodeWidth() ?: 1) > 1)
        attrs = a
    }

    internal fun append(c: Int) {
        val len = len()
        if (len >= CONTENT_BYTES - 4) {
            return
        }
        if (len == 0) {
            contents[0] = ' '.code.toByte()
            lenAndFlags += 1
        }

        // we already checked that we have space for another codepoint
        appendChar(len(), c)
    }

    // Writes the UTF-8 bytes for c starting at the provided byte offset.
    // The caller verifies there is room for the largest supported code point.
    private fun appendChar(start: Int, c: Int) {
        val encoded = codePointToUtf8(c)
        for (i in encoded.indices) {
            contents[start + i] = encoded[i]
        }
        lenAndFlags += encoded.size
    }

    internal fun clear(attrs: Attrs) {
        lenAndFlags = 0
        this.attrs = attrs
    }

    /**
     * Returns the text contents of the cell.
     *
     * Can include multiple unicode characters if combining characters are
     * used, but will contain at most one character with a non-zero character
     * width.
     */
    // Since contents has been constructed by appending chars encoded as UTF-8 it will be valid UTF-8
    public fun contents(): String =
        contents.decodeToString(endIndex = len())

    /** Returns whether the cell contains any text data. */
    public fun hasContents(): Boolean = len() > 0

    /** Returns whether the text data in the cell represents a wide character. */
    public fun isWide(): Boolean = lenAndFlags and IS_WIDE != 0

    /**
     * Returns whether the cell contains the second half of a wide character
     * (in other words, whether the previous cell in the row contains a wide
     * character)
     */
    public fun isWideContinuation(): Boolean = lenAndFlags and IS_WIDE_CONTINUATION != 0

    private fun setWide(wide: Boolean) {
        lenAndFlags =
            if (wide) {
                lenAndFlags or IS_WIDE
            } else {
                lenAndFlags and IS_WIDE.inv()
            }
    }

    internal fun setWideContinuation(wide: Boolean) {
        lenAndFlags =
            if (wide) {
                lenAndFlags or IS_WIDE_CONTINUATION
            } else {
                lenAndFlags and IS_WIDE_CONTINUATION.inv()
            }
    }

    internal fun attrs(): Attrs = attrs

    /** Returns the foreground color of the cell. */
    public fun fgcolor(): Color = attrs.fgColor

    /** Returns the background color of the cell. */
    public fun bgcolor(): Color = attrs.bgColor

    /** Returns whether the cell should be rendered with the bold text attribute. */
    public fun bold(): Boolean = attrs.bold()

    /** Returns whether the cell should be rendered with the dim text attribute. */
    public fun dim(): Boolean = attrs.dim()

    /** Returns whether the cell should be rendered with the italic text attribute. */
    public fun italic(): Boolean = attrs.italic()

    /** Returns whether the cell should be rendered with the underlined text attribute. */
    public fun underline(): Boolean = attrs.underline()

    /** Returns whether the cell should be rendered with the inverse text attribute. */
    public fun inverse(): Boolean = attrs.inverse()

    public fun copy(): Cell {
        val out = Cell()
        contents.copyInto(out.contents)
        out.lenAndFlags = lenAndFlags
        out.attrs = attrs.copy()
        return out
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Cell) return false
        if (lenAndFlags != other.lenAndFlags) {
            return false
        }
        if (attrs != other.attrs) {
            return false
        }
        val len = len()
        for (i in 0 until len) {
            if (contents[i] != other.contents[i]) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = lenAndFlags
        result = 31 * result + attrs.hashCode()
        val len = len()
        for (i in 0 until len) {
            result = 31 * result + contents[i]
        }
        return result
    }

    override fun toString(): String =
        "Cell(contents=${contents()}, len=${len()}, wide=${isWide()}, wideContinuation=${isWideContinuation()}, attrs=$attrs)"

    internal companion object {
        internal fun new(): Cell = Cell()
    }
}

// Encodes a Unicode scalar value into UTF-8 bytes. The upstream helper writes
// between one and four bytes per code point.
private fun codePointToUtf8(c: Int): ByteArray =
    when {
        c < 0x80 -> byteArrayOf(c.toByte())
        c < 0x800 ->
            byteArrayOf(
                (0xC0 or (c shr 6)).toByte(),
                (0x80 or (c and 0x3F)).toByte(),
            )
        c < 0x10000 ->
            byteArrayOf(
                (0xE0 or (c shr 12)).toByte(),
                (0x80 or ((c shr 6) and 0x3F)).toByte(),
                (0x80 or (c and 0x3F)).toByte(),
            )
        else ->
            byteArrayOf(
                (0xF0 or (c shr 18)).toByte(),
                (0x80 or ((c shr 12) and 0x3F)).toByte(),
                (0x80 or ((c shr 6) and 0x3F)).toByte(),
                (0x80 or (c and 0x3F)).toByte(),
            )
    }
