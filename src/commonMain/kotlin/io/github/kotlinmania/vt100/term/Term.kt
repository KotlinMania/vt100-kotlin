// port-lint: source term.rs
package io.github.kotlinmania.vt100.term

import io.github.kotlinmania.vt100.Color
import io.github.kotlinmania.vt100.MouseProtocolEncoding
import io.github.kotlinmania.vt100.MouseProtocolMode
import io.github.kotlinmania.vt100.grid.Pos

// upstream: read all of this from terminfo

/** Writes a control-byte sequence into a caller-provided buffer. */
internal interface BufWrite {
    fun writeBuf(buf: MutableList<Byte>)
}

/** Clears the entire screen and moves the cursor home. */
internal class ClearScreen : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        buf.appendAscii("\u001b[H\u001b[J")
    }
}

/** Clears from the cursor to the end of the current row. */
internal class ClearRowForward : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        buf.appendAscii("\u001b[K")
    }
}

/** Writes a CR/LF newline pair. */
internal class Crlf : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        buf.appendAscii("\r\n")
    }
}

/** Writes a single backspace control byte. */
internal class Backspace : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        buf.add(0x08)
    }
}

/** Saves the current cursor position. */
internal class SaveCursor : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        buf.appendAscii("\u001b7")
    }
}

/** Restores the cursor to the previously saved position. */
internal class RestoreCursor : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        buf.appendAscii("\u001b8")
    }
}

/** Moves the cursor to a row/column position. */
internal class MoveTo(
    private val row: Int,
    private val col: Int,
) : BufWrite {
    internal constructor(pos: Pos) : this(pos.row, pos.col)

    override fun writeBuf(buf: MutableList<Byte>) {
        if (row == 0 && col == 0) {
            buf.appendAscii("\u001b[H")
        } else {
            buf.appendAscii("\u001b[")
            buf.appendItoa(row + 1)
            buf.add(';'.code.toByte())
            buf.appendItoa(col + 1)
            buf.add('H'.code.toByte())
        }
    }
}

/** Clears any active SGR text attributes. */
internal class ClearAttrs : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        buf.appendAscii("\u001b[m")
    }
}

/** Per-cell text intensity. */
internal enum class Intensity {
    Normal,
    Bold,
    Dim,
}

/**
 * A builder for writing a partial SGR attribute sequence.
 *
 * Only the fields explicitly set on the builder are emitted; cleared (null)
 * fields keep their previously written value.
 */
internal class Attrs : BufWrite {
    private var fgcolor: Color? = null
    private var bgcolor: Color? = null
    private var intensity: Intensity? = null
    private var italic: Boolean? = null
    private var underline: Boolean? = null
    private var inverse: Boolean? = null

    internal fun fgcolor(fgcolor: Color): Attrs {
        this.fgcolor = fgcolor
        return this
    }

    internal fun bgcolor(bgcolor: Color): Attrs {
        this.bgcolor = bgcolor
        return this
    }

    internal fun intensity(intensity: Intensity): Attrs {
        this.intensity = intensity
        return this
    }

    internal fun italic(italic: Boolean): Attrs {
        this.italic = italic
        return this
    }

    internal fun underline(underline: Boolean): Attrs {
        this.underline = underline
        return this
    }

    internal fun inverse(inverse: Boolean): Attrs {
        this.inverse = inverse
        return this
    }

    override fun writeBuf(buf: MutableList<Byte>) {
        if (fgcolor == null &&
            bgcolor == null &&
            intensity == null &&
            italic == null &&
            underline == null &&
            inverse == null
        ) {
            return
        }

        buf.appendAscii("\u001b[")
        var first = true

        fun writeParam(i: Int) {
            if (first) {
                first = false
            } else {
                buf.add(';'.code.toByte())
            }
            buf.appendItoa(i)
        }

        when (val fg = fgcolor) {
            null -> { /* not set */ }
            Color.Default -> writeParam(39)
            is Color.Idx -> {
                val i = fg.value
                when {
                    i < 8 -> writeParam(i + 30)
                    i < 16 -> writeParam(i + 82)
                    else -> {
                        writeParam(38)
                        writeParam(5)
                        writeParam(i)
                    }
                }
            }
            is Color.Rgb -> {
                writeParam(38)
                writeParam(2)
                writeParam(fg.red)
                writeParam(fg.green)
                writeParam(fg.blue)
            }
        }

        when (val bg = bgcolor) {
            null -> { /* not set */ }
            Color.Default -> writeParam(49)
            is Color.Idx -> {
                val i = bg.value
                when {
                    i < 8 -> writeParam(i + 40)
                    i < 16 -> writeParam(i + 92)
                    else -> {
                        writeParam(48)
                        writeParam(5)
                        writeParam(i)
                    }
                }
            }
            is Color.Rgb -> {
                writeParam(48)
                writeParam(2)
                writeParam(bg.red)
                writeParam(bg.green)
                writeParam(bg.blue)
            }
        }

        when (intensity) {
            null -> { /* not set */ }
            Intensity.Normal -> writeParam(22)
            Intensity.Bold -> writeParam(1)
            Intensity.Dim -> writeParam(2)
        }

        italic?.let { writeParam(if (it) 3 else 23) }
        underline?.let { writeParam(if (it) 4 else 24) }
        inverse?.let { writeParam(if (it) 7 else 27) }

        buf.add('m'.code.toByte())
    }
}

/** Moves the cursor right by [count] columns. */
internal class MoveRight(
    private val count: Int = 1,
) : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        when (count) {
            0 -> { /* no-op */ }
            1 -> buf.appendAscii("\u001b[C")
            else -> {
                buf.appendAscii("\u001b[")
                buf.appendItoa(count)
                buf.add('C'.code.toByte())
            }
        }
    }
}

/** Erases [count] cells starting at the cursor. */
internal class EraseChar(
    private val count: Int = 1,
) : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        when (count) {
            0 -> { /* no-op */ }
            1 -> buf.appendAscii("\u001b[X")
            else -> {
                buf.appendAscii("\u001b[")
                buf.appendItoa(count)
                buf.add('X'.code.toByte())
            }
        }
    }
}

/** Hides or shows the cursor. */
internal class HideCursor(
    private val state: Boolean = false,
) : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        if (state) {
            buf.appendAscii("\u001b[?25l")
        } else {
            buf.appendAscii("\u001b[?25h")
        }
    }
}

/** Moves the cursor from [from] to [to], choosing the cheapest control sequence. */
internal class MoveFromTo(
    private val from: Pos,
    private val to: Pos,
) : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        when {
            to.row == from.row + 1 && to.col == 0 -> Crlf().writeBuf(buf)
            from.row == to.row && from.col < to.col ->
                MoveRight(to.col - from.col).writeBuf(buf)
            to != from -> MoveTo(to).writeBuf(buf)
        }
    }
}

/** Switches the terminal into or out of application-keypad mode. */
internal class ApplicationKeypad(
    private val state: Boolean = false,
) : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        if (state) {
            buf.appendAscii("\u001b=")
        } else {
            buf.appendAscii("\u001b>")
        }
    }
}

/** Switches the terminal into or out of application-cursor mode. */
internal class ApplicationCursor(
    private val state: Boolean = false,
) : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        if (state) {
            buf.appendAscii("\u001b[?1h")
        } else {
            buf.appendAscii("\u001b[?1l")
        }
    }
}

/** Enables or disables bracketed-paste mode. */
internal class BracketedPaste(
    private val state: Boolean = false,
) : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        if (state) {
            buf.appendAscii("\u001b[?2004h")
        } else {
            buf.appendAscii("\u001b[?2004l")
        }
    }
}

/**
 * Writes the control sequence that transitions the mouse protocol mode from
 * [prev] to [mode]. Writing nothing when the two are equal preserves the
 * upstream invariant.
 */
internal class MouseProtocolModeWriter(
    private val mode: MouseProtocolMode = MouseProtocolMode.None,
    private val prev: MouseProtocolMode = MouseProtocolMode.None,
) : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        if (mode == prev) {
            return
        }

        when (mode) {
            MouseProtocolMode.None ->
                when (prev) {
                    MouseProtocolMode.None -> { /* unreachable */ }
                    MouseProtocolMode.Press -> buf.appendAscii("\u001b[?9l")
                    MouseProtocolMode.PressRelease -> buf.appendAscii("\u001b[?1000l")
                    MouseProtocolMode.ButtonMotion -> buf.appendAscii("\u001b[?1002l")
                    MouseProtocolMode.AnyMotion -> buf.appendAscii("\u001b[?1003l")
                }
            MouseProtocolMode.Press -> buf.appendAscii("\u001b[?9h")
            MouseProtocolMode.PressRelease -> buf.appendAscii("\u001b[?1000h")
            MouseProtocolMode.ButtonMotion -> buf.appendAscii("\u001b[?1002h")
            MouseProtocolMode.AnyMotion -> buf.appendAscii("\u001b[?1003h")
        }
    }
}

/**
 * Writes the control sequence that transitions the mouse protocol encoding
 * from [prev] to [encoding]. Writing nothing when the two are equal preserves
 * the upstream invariant.
 */
internal class MouseProtocolEncodingWriter(
    private val encoding: MouseProtocolEncoding = MouseProtocolEncoding.Default,
    private val prev: MouseProtocolEncoding = MouseProtocolEncoding.Default,
) : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        if (encoding == prev) {
            return
        }

        when (encoding) {
            MouseProtocolEncoding.Default ->
                when (prev) {
                    MouseProtocolEncoding.Default -> { /* unreachable */ }
                    MouseProtocolEncoding.Utf8 -> buf.appendAscii("\u001b[?1005l")
                    MouseProtocolEncoding.Sgr -> buf.appendAscii("\u001b[?1006l")
                }
            MouseProtocolEncoding.Utf8 -> buf.appendAscii("\u001b[?1005h")
            MouseProtocolEncoding.Sgr -> buf.appendAscii("\u001b[?1006h")
        }
    }
}

private fun MutableList<Byte>.appendAscii(value: String) {
    for (byte in value.encodeToByteArray()) {
        add(byte)
    }
}

private fun MutableList<Byte>.appendItoa(value: Int) {
    appendAscii(value.toString())
}
