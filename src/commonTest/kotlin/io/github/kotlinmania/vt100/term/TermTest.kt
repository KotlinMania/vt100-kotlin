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
    fun clearScreen_writesEscSequence() {
        assertEquals(
            bytes(0x1B, '['.code, 'H'.code, 0x1B, '['.code, 'J'.code).toList(),
            writeOf(ClearScreen()).toList(),
        )
    }

    @Test
    fun clearRowForward_writesEscK() {
        assertEquals(
            bytes(0x1B, '['.code, 'K'.code).toList(),
            writeOf(ClearRowForward()).toList(),
        )
    }

    @Test
    fun crlf_writesCrLf() {
        assertEquals(byteArrayOf(0x0D, 0x0A).toList(), writeOf(Crlf()).toList())
    }

    @Test
    fun backspace_writesSingleByte() {
        assertEquals(byteArrayOf(0x08).toList(), writeOf(Backspace()).toList())
    }

    @Test
    fun saveCursor_writesEsc7() {
        assertEquals(bytes(0x1B, '7'.code).toList(), writeOf(SaveCursor()).toList())
    }

    @Test
    fun restoreCursor_writesEsc8() {
        assertEquals(bytes(0x1B, '8'.code).toList(), writeOf(RestoreCursor()).toList())
    }

    @Test
    fun moveTo_home_writesShortForm() {
        assertEquals(
            bytes(0x1B, '['.code, 'H'.code).toList(),
            writeOf(MoveTo(Pos(0, 0))).toList(),
        )
    }

    @Test
    fun moveTo_nonHome_writesOneBasedCoords() {
        assertEquals(
            bytes(0x1B, '['.code, '4'.code, ';'.code, '6'.code, 'H'.code).toList(),
            writeOf(MoveTo(Pos(3, 5))).toList(),
        )
    }

    @Test
    fun clearAttrs_writesEscM() {
        assertEquals(bytes(0x1B, '['.code, 'm'.code).toList(), writeOf(ClearAttrs()).toList())
    }

    @Test
    fun moveRight_default_writesEscC() {
        assertEquals(
            bytes(0x1B, '['.code, 'C'.code).toList(),
            writeOf(MoveRight()).toList(),
        )
    }

    @Test
    fun moveRight_zero_writesNothing() {
        assertTrue(writeOf(MoveRight(0)).isEmpty())
    }

    @Test
    fun moveRight_multi_writesCount() {
        assertEquals(
            bytes(0x1B, '['.code, '5'.code, 'C'.code).toList(),
            writeOf(MoveRight(5)).toList(),
        )
    }

    @Test
    fun eraseChar_default_writesEscX() {
        assertEquals(
            bytes(0x1B, '['.code, 'X'.code).toList(),
            writeOf(EraseChar()).toList(),
        )
    }

    @Test
    fun eraseChar_zero_writesNothing() {
        assertTrue(writeOf(EraseChar(0)).isEmpty())
    }

    @Test
    fun hideCursor_on_writesL() {
        assertEquals(
            bytes(0x1B, '['.code, '?'.code, '2'.code, '5'.code, 'l'.code).toList(),
            writeOf(HideCursor(true)).toList(),
        )
    }

    @Test
    fun hideCursor_off_writesH() {
        assertEquals(
            bytes(0x1B, '['.code, '?'.code, '2'.code, '5'.code, 'h'.code).toList(),
            writeOf(HideCursor(false)).toList(),
        )
    }

    @Test
    fun moveFromTo_nextRow_writesCrlf() {
        assertEquals(
            byteArrayOf(0x0D, 0x0A).toList(),
            writeOf(MoveFromTo(Pos(2, 5), Pos(3, 0))).toList(),
        )
    }

    @Test
    fun moveFromTo_sameRowRight_writesMoveRight() {
        assertEquals(
            bytes(0x1B, '['.code, '4'.code, 'C'.code).toList(),
            writeOf(MoveFromTo(Pos(2, 1), Pos(2, 5))).toList(),
        )
    }

    @Test
    fun moveFromTo_arbitrary_writesAbsoluteMove() {
        assertEquals(
            bytes(0x1B, '['.code, '4'.code, ';'.code, '6'.code, 'H'.code).toList(),
            writeOf(MoveFromTo(Pos(2, 8), Pos(3, 5))).toList(),
        )
    }

    @Test
    fun moveFromTo_same_writesNothing() {
        assertTrue(writeOf(MoveFromTo(Pos(2, 5), Pos(2, 5))).isEmpty())
    }

    @Test
    fun applicationKeypad_on_writesEscEquals() {
        assertEquals(bytes(0x1B, '='.code).toList(), writeOf(ApplicationKeypad(true)).toList())
    }

    @Test
    fun applicationKeypad_off_writesEscGt() {
        assertEquals(bytes(0x1B, '>'.code).toList(), writeOf(ApplicationKeypad(false)).toList())
    }

    @Test
    fun bracketedPaste_on() {
        val expected = byteArrayOf(0x1B) + "[?2004h".encodeToByteArray()
        assertEquals(expected.toList(), writeOf(BracketedPaste(true)).toList())
    }

    @Test
    fun bracketedPaste_off() {
        val expected = byteArrayOf(0x1B) + "[?2004l".encodeToByteArray()
        assertEquals(expected.toList(), writeOf(BracketedPaste(false)).toList())
    }

    @Test
    fun mouseProtocolMode_sameStateWritesNothing() {
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
    fun mouseProtocolMode_disablePress() {
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
    fun mouseProtocolMode_enablePressRelease() {
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
    fun mouseProtocolEncoding_sameWritesNothing() {
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
    fun mouseProtocolEncoding_enableSgr() {
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
    fun attrs_empty_writesNothing() {
        assertTrue(writeOf(Attrs()).isEmpty())
    }

    @Test
    fun attrs_fgRedBoldItalic_emitsSgr() {
        val out = writeOf(Attrs().fgcolor(Color.Idx(1)).intensity(Intensity.Bold).italic(true))
        val expected = byteArrayOf(0x1B) + "[31;1;3m".encodeToByteArray()
        assertEquals(expected.toList(), out.toList())
    }

    @Test
    fun attrs_indexedHighColor_emits9X() {
        val out = writeOf(Attrs().fgcolor(Color.Idx(10)))
        val expected = byteArrayOf(0x1B) + "[92m".encodeToByteArray()
        assertEquals(expected.toList(), out.toList())
    }

    @Test
    fun attrs_indexed256_emits38_5_n() {
        val out = writeOf(Attrs().bgcolor(Color.Idx(200)))
        val expected = byteArrayOf(0x1B) + "[48;5;200m".encodeToByteArray()
        assertEquals(expected.toList(), out.toList())
    }

    @Test
    fun attrs_rgbForeground_emits38_2() {
        val out = writeOf(Attrs().fgcolor(Color.Rgb(12, 34, 56)))
        val expected = byteArrayOf(0x1B) + "[38;2;12;34;56m".encodeToByteArray()
        assertEquals(expected.toList(), out.toList())
    }
}
