// port-lint: source term.rs
package io.github.kotlinmania.vt100.term

import io.github.kotlinmania.vt100.Color
import io.github.kotlinmania.vt100.MouseProtocolEncoding
import io.github.kotlinmania.vt100.MouseProtocolMode
import io.github.kotlinmania.vt100.grid.Pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun writeOf(w: BufWrite): ByteArray {
    val buf = mutableListOf<Byte>()
    w.writeBuf(buf)
    return buf.toByteArray()
}

private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }

class TermTest {
    @Test
    fun clearScreenWritesEscSequence() {
        assertEquals(
            bytes(0x1B, '['.code, 'H'.code, 0x1B, '['.code, 'J'.code).toList(),
            writeOf(ClearScreen()).toList(),
        )
    }

    @Test
    fun clearRowForwardWritesEscK() {
        assertEquals(
            bytes(0x1B, '['.code, 'K'.code).toList(),
            writeOf(ClearRowForward()).toList(),
        )
    }

    @Test
    fun crlfWritesCrLf() {
        assertEquals(byteArrayOf(0x0D, 0x0A).toList(), writeOf(Crlf()).toList())
    }

    @Test
    fun backspaceWritesSingleByte() {
        assertEquals(byteArrayOf(0x08).toList(), writeOf(Backspace()).toList())
    }

    @Test
    fun saveCursorWritesEsc7() {
        assertEquals(bytes(0x1B, '7'.code).toList(), writeOf(SaveCursor()).toList())
    }

    @Test
    fun restoreCursorWritesEsc8() {
        assertEquals(bytes(0x1B, '8'.code).toList(), writeOf(RestoreCursor()).toList())
    }

    @Test
    fun moveToHomeWritesShortForm() {
        assertEquals(
            bytes(0x1B, '['.code, 'H'.code).toList(),
            writeOf(MoveTo(Pos(0, 0))).toList(),
        )
    }

    @Test
    fun moveToNonHomeWritesOneBasedCoords() {
        assertEquals(
            bytes(0x1B, '['.code, '4'.code, ';'.code, '6'.code, 'H'.code).toList(),
            writeOf(MoveTo(Pos(3, 5))).toList(),
        )
    }

    @Test
    fun clearAttrsWritesEscM() {
        assertEquals(bytes(0x1B, '['.code, 'm'.code).toList(), writeOf(ClearAttrs()).toList())
    }

    @Test
    fun moveRightDefaultWritesEscC() {
        assertEquals(
            bytes(0x1B, '['.code, 'C'.code).toList(),
            writeOf(MoveRight()).toList(),
        )
    }

    @Test
    fun moveRightZeroWritesNothing() {
        assertTrue(writeOf(MoveRight(0)).isEmpty())
    }

    @Test
    fun moveRightMultiWritesCount() {
        assertEquals(
            bytes(0x1B, '['.code, '5'.code, 'C'.code).toList(),
            writeOf(MoveRight(5)).toList(),
        )
    }

    @Test
    fun eraseCharDefaultWritesEscX() {
        assertEquals(
            bytes(0x1B, '['.code, 'X'.code).toList(),
            writeOf(EraseChar()).toList(),
        )
    }

    @Test
    fun eraseCharZeroWritesNothing() {
        assertTrue(writeOf(EraseChar(0)).isEmpty())
    }

    @Test
    fun hideCursorOnWritesL() {
        assertEquals(
            bytes(0x1B, '['.code, '?'.code, '2'.code, '5'.code, 'l'.code).toList(),
            writeOf(HideCursor(true)).toList(),
        )
    }

    @Test
    fun hideCursorOffWritesH() {
        assertEquals(
            bytes(0x1B, '['.code, '?'.code, '2'.code, '5'.code, 'h'.code).toList(),
            writeOf(HideCursor(false)).toList(),
        )
    }

    @Test
    fun moveFromToNextRowWritesCrlf() {
        assertEquals(
            byteArrayOf(0x0D, 0x0A).toList(),
            writeOf(MoveFromTo(Pos(2, 5), Pos(3, 0))).toList(),
        )
    }

    @Test
    fun moveFromToSameRowRightWritesMoveRight() {
        assertEquals(
            bytes(0x1B, '['.code, '4'.code, 'C'.code).toList(),
            writeOf(MoveFromTo(Pos(2, 1), Pos(2, 5))).toList(),
        )
    }

    @Test
    fun moveFromToArbitraryWritesAbsoluteMove() {
        assertEquals(
            bytes(0x1B, '['.code, '4'.code, ';'.code, '6'.code, 'H'.code).toList(),
            writeOf(MoveFromTo(Pos(2, 8), Pos(3, 5))).toList(),
        )
    }

    @Test
    fun moveFromToSameWritesNothing() {
        assertTrue(writeOf(MoveFromTo(Pos(2, 5), Pos(2, 5))).isEmpty())
    }

    @Test
    fun applicationKeypadOnWritesEscEquals() {
        assertEquals(bytes(0x1B, '='.code).toList(), writeOf(ApplicationKeypad(true)).toList())
    }

    @Test
    fun applicationKeypadOffWritesEscGt() {
        assertEquals(bytes(0x1B, '>'.code).toList(), writeOf(ApplicationKeypad(false)).toList())
    }

    @Test
    fun bracketedPasteOn() {
        val expected = byteArrayOf(0x1B) + "[?2004h".encodeToByteArray()
        assertEquals(expected.toList(), writeOf(BracketedPaste(true)).toList())
    }

    @Test
    fun bracketedPasteOff() {
        val expected = byteArrayOf(0x1B) + "[?2004l".encodeToByteArray()
        assertEquals(expected.toList(), writeOf(BracketedPaste(false)).toList())
    }

    @Test
    fun mouseProtocolModeSameStateWritesNothing() {
        assertTrue(
            writeOf(
                MouseProtocolModeWriter(
                    mode = MouseProtocolMode.Press,
                    prev = MouseProtocolMode.Press,
                ),
            ).isEmpty(),
        )
    }

    @Test
    fun mouseProtocolModeDisablePress() {
        val expected = byteArrayOf(0x1B) + "[?9l".encodeToByteArray()
        assertEquals(
            expected.toList(),
            writeOf(
                MouseProtocolModeWriter(
                    mode = MouseProtocolMode.None,
                    prev = MouseProtocolMode.Press,
                ),
            ).toList(),
        )
    }

    @Test
    fun mouseProtocolModeEnablePressRelease() {
        val expected = byteArrayOf(0x1B) + "[?1000h".encodeToByteArray()
        assertEquals(
            expected.toList(),
            writeOf(
                MouseProtocolModeWriter(
                    mode = MouseProtocolMode.PressRelease,
                    prev = MouseProtocolMode.None,
                ),
            ).toList(),
        )
    }

    @Test
    fun mouseProtocolEncodingSameWritesNothing() {
        assertTrue(
            writeOf(
                MouseProtocolEncodingWriter(
                    encoding = MouseProtocolEncoding.Utf8,
                    prev = MouseProtocolEncoding.Utf8,
                ),
            ).isEmpty(),
        )
    }

    @Test
    fun mouseProtocolEncodingEnableSgr() {
        val expected = byteArrayOf(0x1B) + "[?1006h".encodeToByteArray()
        assertEquals(
            expected.toList(),
            writeOf(
                MouseProtocolEncodingWriter(
                    encoding = MouseProtocolEncoding.Sgr,
                    prev = MouseProtocolEncoding.Default,
                ),
            ).toList(),
        )
    }

    @Test
    fun attrsEmptyWritesNothing() {
        assertTrue(writeOf(Attrs()).isEmpty())
    }

    @Test
    fun attrsFgRedBoldItalicEmitsSgr() {
        val out = writeOf(Attrs().fgcolor(Color.Idx(1)).intensity(Intensity.Bold).italic(true))
        val expected = byteArrayOf(0x1B) + "[31;1;3m".encodeToByteArray()
        assertEquals(expected.toList(), out.toList())
    }

    @Test
    fun attrsIndexedHighColorEmits9X() {
        val out = writeOf(Attrs().fgcolor(Color.Idx(10)))
        val expected = byteArrayOf(0x1B) + "[92m".encodeToByteArray()
        assertEquals(expected.toList(), out.toList())
    }

    @Test
    fun attrsIndexed256Emits38FiveN() {
        val out = writeOf(Attrs().bgcolor(Color.Idx(200)))
        val expected = byteArrayOf(0x1B) + "[48;5;200m".encodeToByteArray()
        assertEquals(expected.toList(), out.toList())
    }

    @Test
    fun attrsRgbForegroundEmits38Two() {
        val out = writeOf(Attrs().fgcolor(Color.Rgb(12, 34, 56)))
        val expected = byteArrayOf(0x1B) + "[38;2;12;34;56m".encodeToByteArray()
        assertEquals(expected.toList(), out.toList())
    }
}
