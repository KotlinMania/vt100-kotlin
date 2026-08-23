// port-lint: tests cell.rs
package io.github.kotlinmania.vt100

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CellTest {
    @Test
    fun newCellIsEmpty() {
        val cell = Cell.new()
        assertFalse(cell.hasContents())
        assertEquals("", cell.contents())
        assertFalse(cell.isWide())
        assertFalse(cell.isWideContinuation())
    }

    @Test
    fun setStoresAsciiCharacter() {
        val cell = Cell.new()
        cell.set('a'.code, Attrs())
        assertTrue(cell.hasContents())
        assertEquals("a", cell.contents())
        assertFalse(cell.isWide())
    }

    @Test
    fun setStoresMultiByteCharacter() {
        val cell = Cell.new()
        // U+00E9 LATIN SMALL LETTER E WITH ACUTE is two UTF-8 bytes.
        cell.set(0x00E9, Attrs())
        assertEquals("é", cell.contents())
        assertFalse(cell.isWide())
    }

    @Test
    fun setMarksWideCharacterAsWide() {
        val cell = Cell.new()
        // U+4E2D CJK UNIFIED IDEOGRAPH-4E2D is two columns wide.
        cell.set(0x4E2D, Attrs())
        assertEquals("中", cell.contents())
        assertTrue(cell.isWide())
    }

    @Test
    fun appendStacksCombiningCharacters() {
        val cell = Cell.new()
        cell.set('a'.code, Attrs())
        // U+0301 COMBINING ACUTE ACCENT is a zero-width combining mark.
        cell.append(0x0301)
        assertEquals("á", cell.contents())
    }

    @Test
    fun appendOnEmptyCellInsertsSpaceFirst() {
        val cell = Cell.new()
        cell.append(0x0301)
        assertEquals(" ́", cell.contents())
    }

    @Test
    fun appendStopsWhenContentBufferIsNearlyFull() {
        val cell = Cell.new()
        cell.set('a'.code, Attrs())
        // Each combining acute accent contributes two UTF-8 bytes (0xCC 0x81).
        // The upstream guard refuses an append once the running length would
        // leave fewer than four bytes of headroom in the 22-byte buffer, so
        // we should be able to fit nine extra combining marks but not ten.
        for (i in 0 until 12) {
            cell.append(0x0301)
        }
        val expected =
            buildString {
                append('a')
                repeat(9) { append('́') }
            }
        assertEquals(expected, cell.contents())
    }

    @Test
    fun clearResetsContentsAndKeepsAttrs() {
        val cell = Cell.new()
        cell.set('a'.code, Attrs())
        val attrs = Attrs(fgColor = Color.Idx(2))
        cell.clear(attrs)
        assertFalse(cell.hasContents())
        assertEquals("", cell.contents())
        assertEquals(Color.Idx(2), cell.fgcolor())
    }

    @Test
    fun wideContinuationFlagIsIndependent() {
        val cell = Cell.new()
        cell.setWideContinuation(true)
        assertTrue(cell.isWideContinuation())
        cell.setWideContinuation(false)
        assertFalse(cell.isWideContinuation())
    }

    @Test
    fun equalityComparesContentsLengthAndAttrs() {
        val left = Cell.new()
        val right = Cell.new()
        left.set('a'.code, Attrs())
        right.set('a'.code, Attrs())
        assertEquals(left, right)

        val differentAttrs = Cell.new()
        differentAttrs.set('a'.code, Attrs(fgColor = Color.Idx(1)))
        assertNotEquals(left, differentAttrs)

        val differentChar = Cell.new()
        differentChar.set('b'.code, Attrs())
        assertNotEquals(left, differentChar)
    }

    @Test
    fun copyProducesIndependentCell() {
        val cell = Cell.new()
        cell.set('a'.code, Attrs())
        val clone = cell.copy()
        assertEquals(cell, clone)

        clone.set('b'.code, Attrs())
        assertNotEquals(cell, clone)
        assertEquals("a", cell.contents())
        assertEquals("b", clone.contents())
    }

    @Test
    fun reflectsAttrsFlagsAndColors() {
        val attrs =
            Attrs(
                fgColor = Color.Idx(1),
                bgColor = Color.Rgb(10, 20, 30),
            )
        attrs.setBold()
        attrs.setItalic(true)
        attrs.setUnderline(true)
        attrs.setInverse(true)

        val cell = Cell.new()
        cell.set('a'.code, attrs)

        assertEquals(Color.Idx(1), cell.fgcolor())
        assertEquals(Color.Rgb(10, 20, 30), cell.bgcolor())
        assertTrue(cell.bold())
        assertFalse(cell.dim())
        assertTrue(cell.italic())
        assertTrue(cell.underline())
        assertTrue(cell.inverse())
    }
}
