// port-lint: source term.rs
package io.github.kotlinmania.vt100.term

import io.github.kotlinmania.vt100.Color
import io.github.kotlinmania.vt100.MouseProtocolEncoding
import io.github.kotlinmania.vt100.MouseProtocolMode
import io.github.kotlinmania.vt100.grid.Pos

// upstream: read all of this from terminfo

/** Writes a control-byte sequence into a caller-provided buffer. */
public interface BufWrite {
    public fun writeBuf(buf: MutableList<Byte>)
}

/** Clears the entire screen and moves the cursor home. */
public class ClearScreen : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        buf.appendAscii("[H[J")
    }
}

/** Clears from the cursor to the end of the current row. */
public class ClearRowForward : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        buf.appendAscii("[K")
    }
}

/** Writes a CR/LF newline pair. */
public class Crlf : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        buf.appendAscii("\r\n")
    }
}

/** Writes a single backspace control byte. */
public class Backspace : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        buf.add(0x08)
    }
}

/** Saves the current cursor position. */
public class SaveCursor : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        buf.appendAscii("7")
    }
}

/** Restores the cursor to the previously saved position. */
public class RestoreCursor : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        buf.appendAscii("8")
    }
}

/** Moves the cursor to a row/column position. */
public class MoveTo(private val row: Int, private val col: Int) : BufWrite {
    public constructor(pos: Pos) : this(pos.row, pos.col)

    override fun writeBuf(buf: MutableList<Byte>) {
        if (row == 0 && col == 0) {
            buf.appendAscii("[H")
        } else {
            buf.appendAscii("[")
            buf.appendItoa(row + 1)
            buf.add(';'.code.toByte())
            buf.appendItoa(col + 1)
            buf.add('H'.code.toByte())
        }
    }
}

/** Clears any active SGR text attributes. */
public class ClearAttrs : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        buf.appendAscii("[m")
    }
}

/** Per-cell text intensity. */
public enum class Intensity {
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
public class Attrs : BufWrite {
    private var fgcolor: Color? = null
    private var bgcolor: Color? = null
    private var intensity: Intensity? = null
    private var italic: Boolean? = null
    private var underline: Boolean? = null
    private var inverse: Boolean? = null

    public fun fgcolor(fgcolor: Color): Attrs {
        this.fgcolor = fgcolor
        return this
    }

    public fun bgcolor(bgcolor: Color): Attrs {
        this.bgcolor = bgcolor
        return this
    }

    public fun intensity(intensity: Intensity): Attrs {
        this.intensity = intensity
        return this
    }

    public fun italic(italic: Boolean): Attrs {
        this.italic = italic
        return this
    }

    public fun underline(underline: Boolean): Attrs {
        this.underline = underline
        return this
    }

    public fun inverse(inverse: Boolean): Attrs {
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

        buf.appendAscii("[")
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
public class MoveRight(private val count: Int = 1) : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        when (count) {
            0 -> { /* no-op */ }
            1 -> buf.appendAscii("[C")
            else -> {
                buf.appendAscii("[")
                buf.appendItoa(count)
                buf.add('C'.code.toByte())
            }
        }
    }
}

/** Erases [count] cells starting at the cursor. */
public class EraseChar(private val count: Int = 1) : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        when (count) {
            0 -> { /* no-op */ }
            1 -> buf.appendAscii("[X")
            else -> {
                buf.appendAscii("[")
                buf.appendItoa(count)
                buf.add('X'.code.toByte())
            }
        }
    }
}

/** Hides or shows the cursor. */
public class HideCursor(private val state: Boolean = false) : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        if (state) {
            buf.appendAscii("[?25l")
        } else {
            buf.appendAscii("[?25h")
        }
    }
}

/** Moves the cursor from [from] to [to], choosing the cheapest control sequence. */
public class MoveFromTo(private val from: Pos, private val to: Pos) : BufWrite {
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
public class ApplicationKeypad(private val state: Boolean = false) : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        if (state) {
            buf.appendAscii("=")
        } else {
            buf.appendAscii(">")
        }
    }
}

/** Switches the terminal into or out of application-cursor mode. */
public class ApplicationCursor(private val state: Boolean = false) : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        if (state) {
            buf.appendAscii("[?1h")
        } else {
            buf.appendAscii("[?1l")
        }
    }
}

/** Enables or disables bracketed-paste mode. */
public class BracketedPaste(private val state: Boolean = false) : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        if (state) {
            buf.appendAscii("[?2004h")
        } else {
            buf.appendAscii("[?2004l")
        }
    }
}

/**
 * Writes the control sequence that transitions the mouse protocol mode from
 * [prev] to [mode]. Writing nothing when the two are equal preserves the
 * upstream invariant.
 */
public class MouseProtocolModeWriter(
    private val mode: MouseProtocolMode = MouseProtocolMode.None,
    private val prev: MouseProtocolMode = MouseProtocolMode.None,
) : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        if (mode == prev) {
            return
        }

        when (mode) {
            MouseProtocolMode.None -> when (prev) {
                MouseProtocolMode.None -> { /* unreachable */ }
                MouseProtocolMode.Press -> buf.appendAscii("[?9l")
                MouseProtocolMode.PressRelease -> buf.appendAscii("[?1000l")
                MouseProtocolMode.ButtonMotion -> buf.appendAscii("[?1002l")
                MouseProtocolMode.AnyMotion -> buf.appendAscii("[?1003l")
            }
            MouseProtocolMode.Press -> buf.appendAscii("[?9h")
            MouseProtocolMode.PressRelease -> buf.appendAscii("[?1000h")
            MouseProtocolMode.ButtonMotion -> buf.appendAscii("[?1002h")
            MouseProtocolMode.AnyMotion -> buf.appendAscii("[?1003h")
        }
    }
}

/**
 * Writes the control sequence that transitions the mouse protocol encoding
 * from [prev] to [encoding]. Writing nothing when the two are equal preserves
 * the upstream invariant.
 */
public class MouseProtocolEncodingWriter(
    private val encoding: MouseProtocolEncoding = MouseProtocolEncoding.Default,
    private val prev: MouseProtocolEncoding = MouseProtocolEncoding.Default,
) : BufWrite {
    override fun writeBuf(buf: MutableList<Byte>) {
        if (encoding == prev) {
            return
        }

        when (encoding) {
            MouseProtocolEncoding.Default -> when (prev) {
                MouseProtocolEncoding.Default -> { /* unreachable */ }
                MouseProtocolEncoding.Utf8 -> buf.appendAscii("[?1005l")
                MouseProtocolEncoding.Sgr -> buf.appendAscii("[?1006l")
            }
            MouseProtocolEncoding.Utf8 -> buf.appendAscii("[?1005h")
            MouseProtocolEncoding.Sgr -> buf.appendAscii("[?1006h")
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
