// port-lint: source screen.rs
package io.github.kotlinmania.vt100

import io.github.kotlinmania.unicodewidth.unicodeWidth
import io.github.kotlinmania.vt100.grid.Grid
import io.github.kotlinmania.vt100.grid.Pos
import io.github.kotlinmania.vt100.grid.Size
import io.github.kotlinmania.vt100.term.ApplicationCursor
import io.github.kotlinmania.vt100.term.ApplicationKeypad
import io.github.kotlinmania.vt100.term.BracketedPaste
import io.github.kotlinmania.vt100.term.ClearAttrs
import io.github.kotlinmania.vt100.term.HideCursor
import io.github.kotlinmania.vt100.term.MouseProtocolEncodingWriter
import io.github.kotlinmania.vt100.term.MouseProtocolModeWriter

/** The xterm mouse handling mode currently in use. */
public enum class MouseProtocolMode {
    /** Mouse handling is disabled. */
    None,

    /**
     * Mouse button events should be reported on button press. Also known as
     * X10 mouse mode.
     */
    Press,

    /**
     * Mouse button events should be reported on button press and release.
     * Also known as VT200 mouse mode.
     */
    PressRelease,

    // Highlight,
    /**
     * Mouse button events should be reported on button press and release, as
     * well as when the mouse moves between cells while a button is held
     * down.
     */
    ButtonMotion,

    /**
     * Mouse button events should be reported on button press and release,
     * and mouse motion events should be reported when the mouse moves
     * between cells regardless of whether a button is held down or not.
     */
    AnyMotion,
    // DecLocator,
}

/** The encoding to use for the enabled [MouseProtocolMode]. */
public enum class MouseProtocolEncoding {
    /** Default single-printable-byte encoding. */
    Default,

    /** UTF-8-based encoding. */
    Utf8,

    /** SGR-like encoding. */
    Sgr,
    // Urxvt,
}

private const val MODE_APPLICATION_KEYPAD: Int = 0b0000_0001
private const val MODE_APPLICATION_CURSOR: Int = 0b0000_0010
private const val MODE_HIDE_CURSOR: Int = 0b0000_0100
private const val MODE_ALTERNATE_SCREEN: Int = 0b0000_1000
private const val MODE_BRACKETED_PASTE: Int = 0b0001_0000

/**
 * Represents the state of the terminal screen.
 *
 * This includes the grid containing the cells that are visible or in the
 * scrollback buffer, the cursor position, and any active text attributes.
 */
public class Screen internal constructor(
    size: Size,
    scrollbackLen: Int,
) {
    private var grid: Grid = Grid.new(size, scrollbackLen).also { it.allocateRows() }
    private var alternateGrid: Grid = Grid.new(size, 0)
    private var attrs: Attrs = Attrs()
    private var savedAttrs: Attrs = Attrs()
    private var modes: Int = 0
    private var mouseProtocolMode: MouseProtocolMode = MouseProtocolMode.None
    private var mouseProtocolEncoding: MouseProtocolEncoding = MouseProtocolEncoding.Default

    /**
     * Creates a new terminal screen of the given size and with the given amount of
     * scrollback.
     */
    public constructor(rows: Int, cols: Int, scrollbackLen: Int = 0) : this(Size(rows, cols), scrollbackLen)

    /**
     * Changes the size of the screen.
     *
     * This will not resize the scrollback buffer. If rows is larger than the
     * previous row count, rows will be taken from the bottom of the scrollback
     * buffer and added to the bottom of the screen. If rows is smaller than the
     * previous row count, rows will be removed from the bottom of the screen and
     * added to the bottom of the scrollback buffer.
     */
    public fun setSize(rows: Int, cols: Int) {
        grid.setSize(Size(rows, cols))
        alternateGrid.setSize(Size(rows, cols))
    }

    /** Returns the current size of the screen as (rows, cols). */
    public fun size(): Pair<Int, Int> {
        val s = grid().size()
        return Pair(s.rows, s.cols)
    }

    /**
     * Changes the number of scrollback lines the screen will save.
     *
     * If scrollback is smaller than the current number of scrollback lines, the
     * oldest scrollback lines will be dropped.
     */
    public fun setScrollback(rows: Int) {
        gridMut().setScrollback(rows)
    }

    /** Returns the current position in the scrollback. */
    public fun scrollback(): Int = grid().scrollback()

    /** Returns the maximum number of scrollback lines. */
    public fun scrollbackLen(): Int = grid().scrollbackLen()

    /**
     * Returns the entire contents of the screen (not including scrollback) as a
     * string.
     */
    public fun contents(): String {
        val contents = StringBuilder()
        writeContents(contents)
        return contents.toString()
    }

    /**
     * Writes the entire contents of the screen (not including scrollback) to a
     * [StringBuilder].
     */
    public fun writeContents(contents: StringBuilder) {
        grid().writeContents(contents)
    }

    /**
     * Returns the contents of each row on the screen (not including scrollback)
     * as a sequence of strings, from the column [start] up to the width [width].
     */
    public fun rows(start: Int, width: Int): Sequence<String> =
        grid().visibleRows().map { row ->
            val contents = StringBuilder()
            row.writeContents(contents, start, width, false)
            contents.toString()
        }

    /**
     * Returns the contents of the screen (not including scrollback) between the
     * given coordinates.
     */
    public fun contentsBetween(startRow: Int, startCol: Int, endRow: Int, endCol: Int): String =
        when {
            startRow < endRow -> {
                val (_, cols) = size()
                val contents = StringBuilder()
                val visible = grid().visibleRows().toList()
                val limit = minOf(endRow, visible.size - 1)
                for (i in startRow..limit) {
                    val row = visible[i]
                    when (i) {
                        startRow -> {
                            row.writeContents(contents, startCol, cols - startCol, false)
                            if (!row.wrapped()) {
                                contents.append('\n')
                            }
                        }
                        endRow -> {
                            row.writeContents(contents, 0, endCol, false)
                        }
                        else -> {
                            row.writeContents(contents, 0, cols, false)
                        }
                    }
                    if (i != startRow && i != endRow && !row.wrapped()) {
                        contents.append('\n')
                    }
                }
                contents.toString()
            }
            startRow == endRow -> {
                if (startCol < endCol) {
                    rows(startCol, endCol - startCol)
                        .drop(startRow)
                        .firstOrNull() ?: ""
                } else {
                    ""
                }
            }
            else -> ""
        }

    /**
     * Returns a formatted representation of the entire screen (including cursor
     * positioning and attributes).
     */
    public fun stateFormatted(): ByteArray {
        val contents = mutableListOf<Byte>()
        writeContentsFormatted(contents)
        writeInputModeFormatted(contents)
        return contents.toByteArray()
    }

    /**
     * Returns a formatted representation of the difference between the entire
     * current screen and a previous screen state.
     */
    public fun stateDiff(prev: Screen): ByteArray {
        val contents = mutableListOf<Byte>()
        writeContentsDiff(contents, prev)
        writeInputModeDiff(contents, prev)
        return contents.toByteArray()
    }

    /**
     * Returns a formatted representation of the current screen contents
     * (including cursor positioning and attributes).
     */
    public fun contentsFormatted(): ByteArray {
        val contents = mutableListOf<Byte>()
        writeContentsFormatted(contents)
        return contents.toByteArray()
    }

    internal fun writeContentsFormatted(contents: MutableList<Byte>) {
        HideCursor(hideCursor()).writeBuf(contents)
        val prevAttrs = grid().writeContentsFormatted(contents)
        attrs.writeEscapeCodeDiff(contents, prevAttrs)
    }

    /**
     * Returns a formatted representation of each row on the screen (not
     * including scrollback) from column [start] up to width [width].
     */
    public fun rowsFormatted(start: Int, width: Int): Sequence<ByteArray> =
        sequence {
            var wrapping = false
            val cols = grid.size().cols
            for ((i, row) in grid().visibleRows().withIndex()) {
                val contents = mutableListOf<Byte>()
                row.writeContentsFormatted(
                    contents = contents,
                    start = start,
                    width = width,
                    row = i,
                    wrapping = wrapping,
                    prevPos = null,
                    prevAttrs = null,
                )
                if (start == 0 && width == cols) {
                    wrapping = row.wrapped()
                }
                yield(contents.toByteArray())
            }
        }

    /**
     * Returns a formatted representation of the difference between the screen
     * contents of the current screen and a previous screen.
     */
    public fun contentsDiff(prev: Screen): ByteArray {
        val contents = mutableListOf<Byte>()
        writeContentsDiff(contents, prev)
        return contents.toByteArray()
    }

    internal fun writeContentsDiff(contents: MutableList<Byte>, prev: Screen) {
        if (hideCursor() != prev.hideCursor()) {
            HideCursor(hideCursor()).writeBuf(contents)
        }
        val prevAttrs = grid().writeContentsDiff(contents, prev.grid(), prev.attrs)
        attrs.writeEscapeCodeDiff(contents, prevAttrs)
    }

    /**
     * Returns a formatted representation of the difference between each row on
     * the screen and a previous screen from column [start] up to width [width].
     */
    public fun rowsDiff(prev: Screen, start: Int, width: Int): Sequence<ByteArray> =
        sequence {
            val rows = grid().visibleRows().toList()
            val prevRows = prev.grid().visibleRows().toList()
            val count = minOf(rows.size, prevRows.size)
            var wrapping = false
            var prevWrapping = false
            for (i in 0 until count) {
                val contents = mutableListOf<Byte>()
                rows[i].writeContentsDiff(
                    contents = contents,
                    prev = prevRows[i],
                    start = start,
                    width = width,
                    row = i,
                    wrapping = wrapping,
                    prevWrapping = prevWrapping,
                    prevPos = Pos(row = i, col = start),
                    prevAttrs = Attrs(),
                )
                wrapping = rows[i].wrapped()
                prevWrapping = prevRows[i].wrapped()
                yield(contents.toByteArray())
            }
        }

    /** Returns a formatted representation of the current terminal input mode settings. */
    public fun inputModeFormatted(): ByteArray {
        val contents = mutableListOf<Byte>()
        writeInputModeFormatted(contents)
        return contents.toByteArray()
    }

    internal fun writeInputModeFormatted(contents: MutableList<Byte>) {
        ApplicationKeypad(mode(MODE_APPLICATION_KEYPAD)).writeBuf(contents)
        ApplicationCursor(mode(MODE_APPLICATION_CURSOR)).writeBuf(contents)
        BracketedPaste(mode(MODE_BRACKETED_PASTE)).writeBuf(contents)
        MouseProtocolModeWriter(mouseProtocolMode, MouseProtocolMode.None).writeBuf(contents)
        MouseProtocolEncodingWriter(mouseProtocolEncoding, MouseProtocolEncoding.Default).writeBuf(contents)
    }

    /** Returns a formatted representation of the difference between input modes. */
    public fun inputModeDiff(prev: Screen): ByteArray {
        val contents = mutableListOf<Byte>()
        writeInputModeDiff(contents, prev)
        return contents.toByteArray()
    }

    internal fun writeInputModeDiff(contents: MutableList<Byte>, prev: Screen) {
        if (mode(MODE_APPLICATION_KEYPAD) != prev.mode(MODE_APPLICATION_KEYPAD)) {
            ApplicationKeypad(mode(MODE_APPLICATION_KEYPAD)).writeBuf(contents)
        }
        if (mode(MODE_APPLICATION_CURSOR) != prev.mode(MODE_APPLICATION_CURSOR)) {
            ApplicationCursor(mode(MODE_APPLICATION_CURSOR)).writeBuf(contents)
        }
        if (mode(MODE_BRACKETED_PASTE) != prev.mode(MODE_BRACKETED_PASTE)) {
            BracketedPaste(mode(MODE_BRACKETED_PASTE)).writeBuf(contents)
        }
        MouseProtocolModeWriter(mouseProtocolMode, prev.mouseProtocolMode).writeBuf(contents)
        MouseProtocolEncodingWriter(mouseProtocolEncoding, prev.mouseProtocolEncoding).writeBuf(contents)
    }

    /** Returns a formatted representation of the currently active SGR attributes. */
    public fun attributesFormatted(): ByteArray {
        val contents = mutableListOf<Byte>()
        writeAttributesFormatted(contents)
        return contents.toByteArray()
    }

    internal fun writeAttributesFormatted(contents: MutableList<Byte>) {
        ClearAttrs().writeBuf(contents)
        attrs.writeEscapeCodeDiff(contents, Attrs())
    }

    /** Returns the current cursor position as (row, col). */
    public fun cursorPosition(): Pair<Int, Int> {
        val pos = grid().pos()
        return Pair(pos.row, pos.col)
    }

    /** Returns a formatted representation of the cursor state (position and visibility). */
    public fun cursorStateFormatted(): ByteArray {
        val contents = mutableListOf<Byte>()
        writeCursorStateFormatted(contents)
        return contents.toByteArray()
    }

    internal fun writeCursorStateFormatted(contents: MutableList<Byte>) {
        HideCursor(hideCursor()).writeBuf(contents)
        grid().writeCursorPositionFormatted(contents, null, null)
    }

    /**
     * Returns the cell at the given position, or `null` if the position is out of
     * bounds.
     */
    public fun cell(row: Int, col: Int): Cell? = grid().visibleCell(Pos(row, col))

    /** Returns whether the given row is wrapped onto the next row. */
    public fun rowWrapped(row: Int): Boolean = grid().visibleRow(row)?.wrapped() ?: false

    /** Returns whether the alternate screen buffer is currently active. */
    public fun alternateScreen(): Boolean = mode(MODE_ALTERNATE_SCREEN)

    /** Returns whether the application keypad mode is enabled. */
    public fun applicationKeypad(): Boolean = mode(MODE_APPLICATION_KEYPAD)

    /** Returns whether the application cursor mode is enabled. */
    public fun applicationCursor(): Boolean = mode(MODE_APPLICATION_CURSOR)

    /** Returns whether the cursor is hidden. */
    public fun hideCursor(): Boolean = mode(MODE_HIDE_CURSOR)

    /** Returns whether bracketed paste mode is enabled. */
    public fun bracketedPaste(): Boolean = mode(MODE_BRACKETED_PASTE)

    /** Returns the current mouse protocol mode. */
    public fun mouseProtocolMode(): MouseProtocolMode = mouseProtocolMode

    /** Returns the current mouse protocol encoding. */
    public fun mouseProtocolEncoding(): MouseProtocolEncoding = mouseProtocolEncoding

    /** Returns the currently active foreground color. */
    public fun fgcolor(): Color = attrs.fgColor

    /** Returns the currently active background color. */
    public fun bgcolor(): Color = attrs.bgColor

    /** Returns whether bold text attribute is currently active. */
    public fun bold(): Boolean = attrs.bold()

    /** Returns whether dim text attribute is currently active. */
    public fun dim(): Boolean = attrs.dim()

    /** Returns whether italic text attribute is currently active. */
    public fun italic(): Boolean = attrs.italic()

    /** Returns whether underline text attribute is currently active. */
    public fun underline(): Boolean = attrs.underline()

    /** Returns whether inverse text attribute is currently active. */
    public fun inverse(): Boolean = attrs.inverse()

    public fun copy(): Screen {
        val s = Screen(grid.size(), grid.scrollbackLen())
        s.grid = grid.copy()
        s.alternateGrid = alternateGrid.copy()
        s.attrs = attrs.copy()
        s.savedAttrs = savedAttrs.copy()
        s.modes = modes
        s.mouseProtocolMode = mouseProtocolMode
        s.mouseProtocolEncoding = mouseProtocolEncoding
        return s
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Screen) return false
        return grid == other.grid &&
            alternateGrid == other.alternateGrid &&
            attrs == other.attrs &&
            savedAttrs == other.savedAttrs &&
            modes == other.modes &&
            mouseProtocolMode == other.mouseProtocolMode &&
            mouseProtocolEncoding == other.mouseProtocolEncoding
    }

    override fun hashCode(): Int {
        var result = grid.hashCode()
        result = 31 * result + alternateGrid.hashCode()
        result = 31 * result + attrs.hashCode()
        result = 31 * result + savedAttrs.hashCode()
        result = 31 * result + modes.hashCode()
        result = 31 * result + mouseProtocolMode.hashCode()
        result = 31 * result + mouseProtocolEncoding.hashCode()
        return result
    }

    internal fun grid(): Grid = if (mode(MODE_ALTERNATE_SCREEN)) alternateGrid else grid

    internal fun gridMut(): Grid = if (mode(MODE_ALTERNATE_SCREEN)) alternateGrid else grid

    internal fun enterAlternateGrid() {
        gridMut().setScrollback(0)
        setMode(MODE_ALTERNATE_SCREEN)
        alternateGrid.allocateRows()
    }

    internal fun exitAlternateGrid() {
        clearMode(MODE_ALTERNATE_SCREEN)
    }

    internal fun saveCursor() {
        gridMut().saveCursor()
        savedAttrs = attrs.copy()
    }

    internal fun restoreCursor() {
        gridMut().restoreCursor()
        attrs = savedAttrs.copy()
    }

    internal fun setMode(mode: Int) {
        modes = modes or mode
    }

    internal fun clearMode(mode: Int) {
        modes = modes and mode.inv()
    }

    internal fun mode(mode: Int): Boolean = modes and mode != 0

    internal fun setMouseMode(mode: MouseProtocolMode) {
        mouseProtocolMode = mode
    }

    internal fun clearMouseMode(mode: MouseProtocolMode) {
        if (mouseProtocolMode == mode) {
            mouseProtocolMode = MouseProtocolMode.None
        }
    }

    internal fun setMouseEncoding(encoding: MouseProtocolEncoding) {
        mouseProtocolEncoding = encoding
    }

    internal fun clearMouseEncoding(encoding: MouseProtocolEncoding) {
        if (mouseProtocolEncoding == encoding) {
            mouseProtocolEncoding = MouseProtocolEncoding.Default
        }
    }

    internal fun text(c: Char) {
        val pos = grid().pos()
        val size = grid().size()
        val currentAttrs = attrs.copy()

        val charWidth = c.code.unicodeWidth()
        if (charWidth == null && c.code < 256) {
            return
        }
        val width = charWidth ?: 1

        var wrap = false
        if (pos.col > size.cols - width) {
            val lastCell = grid().drawingCell(Pos(row = pos.row, col = size.cols - 1))
            if (lastCell != null && (lastCell.hasContents() || lastCell.isWideContinuation())) {
                wrap = true
            }
        }
        gridMut().colWrap(width, wrap)
        val currentPos = grid().pos()

        if (width == 0) {
            if (currentPos.col > 0) {
                var prevCell = gridMut().drawingCell(Pos(row = currentPos.row, col = currentPos.col - 1))
                if (prevCell != null && prevCell.isWideContinuation()) {
                    prevCell = gridMut().drawingCell(Pos(row = currentPos.row, col = currentPos.col - 2))
                }
                prevCell?.append(c.code)
            } else if (currentPos.row > 0) {
                val prevRow = grid().drawingRow(currentPos.row - 1)
                if (prevRow != null && prevRow.wrapped()) {
                    var prevCell = gridMut().drawingCell(Pos(row = currentPos.row - 1, col = size.cols - 1))
                    if (prevCell != null && prevCell.isWideContinuation()) {
                        prevCell = gridMut().drawingCell(Pos(row = currentPos.row - 1, col = size.cols - 2))
                    }
                    prevCell?.append(c.code)
                }
            }
        } else {
            val startCell = grid().drawingCell(currentPos)
            if (startCell != null && startCell.isWideContinuation()) {
                val prevCell = gridMut().drawingCell(Pos(row = currentPos.row, col = currentPos.col - 1))
                prevCell?.clear(currentAttrs)
            }

            val checkWideCell = grid().drawingCell(currentPos)
            if (checkWideCell != null && checkWideCell.isWide()) {
                val nextCell = gridMut().drawingCell(Pos(row = currentPos.row, col = currentPos.col + 1))
                nextCell?.set(' '.code, currentAttrs)
            }

            val cell = gridMut().drawingCell(currentPos)
            cell?.set(c.code, currentAttrs)
            gridMut().colInc(1)
            if (width > 1) {
                val nextPos = grid().pos()
                val nextDrawingCell = grid().drawingCell(nextPos)
                if (nextDrawingCell != null && nextDrawingCell.isWide()) {
                    val nextNextPos = Pos(row = nextPos.row, col = nextPos.col + 1)
                    val nextNextCell = gridMut().drawingCell(nextNextPos)
                    nextNextCell?.clear(currentAttrs)
                    if (nextNextPos.col == size.cols - 1) {
                        gridMut().drawingRow(nextPos.row)?.wrap(false)
                    }
                }
                val nextCell = gridMut().drawingCell(nextPos)
                nextCell?.clear(Attrs())
                nextCell?.setWideContinuation(true)
                gridMut().colInc(1)
            }
        }
    }

    internal fun bs() {
        gridMut().colDec(1)
    }

    internal fun tab() {
        gridMut().colTab()
    }

    internal fun lf() {
        gridMut().rowIncScroll(1)
    }

    internal fun vt() {
        lf()
    }

    internal fun ff() {
        lf()
    }

    internal fun cr() {
        gridMut().colSet(0)
    }

    internal fun decsc() {
        saveCursor()
    }

    internal fun decrc() {
        restoreCursor()
    }

    internal fun deckpam() {
        setMode(MODE_APPLICATION_KEYPAD)
    }

    internal fun deckpnm() {
        clearMode(MODE_APPLICATION_KEYPAD)
    }

    internal fun ri() {
        gridMut().rowDecScroll(1)
    }

    internal fun ris() {
        grid = Grid.new(grid.size(), grid.scrollbackLen()).also { it.allocateRows() }
        alternateGrid = Grid.new(grid.size(), 0)
        attrs = Attrs()
        savedAttrs = Attrs()
        modes = 0
        mouseProtocolMode = MouseProtocolMode.None
        mouseProtocolEncoding = MouseProtocolEncoding.Default
    }

    internal fun ich(count: Int) {
        gridMut().insertCells(count)
    }

    internal fun cuu(offset: Int) {
        gridMut().rowDecClamp(offset)
    }

    internal fun cud(offset: Int) {
        gridMut().rowIncClamp(offset)
    }

    internal fun cuf(offset: Int) {
        gridMut().colIncClamp(offset)
    }

    internal fun cub(offset: Int) {
        gridMut().colDec(offset)
    }

    internal fun cnl(offset: Int) {
        gridMut().colSet(0)
        gridMut().rowIncClamp(offset)
    }

    internal fun cpl(offset: Int) {
        gridMut().colSet(0)
        gridMut().rowDecClamp(offset)
    }

    internal fun cha(col: Int) {
        gridMut().colSet(col - 1)
    }

    internal fun cup(pos: Pair<Int, Int>) {
        cup(pos.first, pos.second)
    }

    internal fun cup(row: Int, col: Int) {
        gridMut().setPos(Pos(row = row - 1, col = col - 1))
    }

    internal fun ed(mode: Int, unhandled: (Screen) -> Unit) {
        val currentAttrs = attrs.copy()
        when (mode) {
            0 -> gridMut().eraseAllForward(currentAttrs)
            1 -> gridMut().eraseAllBackward(currentAttrs)
            2 -> gridMut().eraseAll(currentAttrs)
            else -> unhandled(this)
        }
    }

    internal fun decsed(mode: Int, unhandled: (Screen) -> Unit) {
        ed(mode, unhandled)
    }

    internal fun el(mode: Int, unhandled: (Screen) -> Unit) {
        val currentAttrs = attrs.copy()
        when (mode) {
            0 -> gridMut().eraseRowForward(currentAttrs)
            1 -> gridMut().eraseRowBackward(currentAttrs)
            2 -> gridMut().eraseRow(currentAttrs)
            else -> unhandled(this)
        }
    }

    internal fun decsel(mode: Int, unhandled: (Screen) -> Unit) {
        el(mode, unhandled)
    }

    internal fun il(count: Int) {
        gridMut().insertLines(count)
    }

    internal fun dl(count: Int) {
        gridMut().deleteLines(count)
    }

    internal fun dch(count: Int) {
        gridMut().deleteCells(count)
    }

    internal fun su(count: Int) {
        gridMut().scrollUp(count)
    }

    internal fun sd(count: Int) {
        gridMut().scrollDown(count)
    }

    internal fun ech(count: Int) {
        gridMut().eraseCells(count, attrs.copy())
    }

    internal fun vpa(row: Int) {
        gridMut().rowSet(row - 1)
    }

    internal fun decset(params: List<IntArray>, unhandled: (Screen) -> Unit) {
        for (param in params) {
            when {
                param.contentEquals(intArrayOf(1)) -> setMode(MODE_APPLICATION_CURSOR)
                param.contentEquals(intArrayOf(6)) -> gridMut().setOriginMode(true)
                param.contentEquals(intArrayOf(9)) -> setMouseMode(MouseProtocolMode.Press)
                param.contentEquals(intArrayOf(25)) -> clearMode(MODE_HIDE_CURSOR)
                param.contentEquals(intArrayOf(47)) -> enterAlternateGrid()
                param.contentEquals(intArrayOf(1000)) -> setMouseMode(MouseProtocolMode.PressRelease)
                param.contentEquals(intArrayOf(1002)) -> setMouseMode(MouseProtocolMode.ButtonMotion)
                param.contentEquals(intArrayOf(1003)) -> setMouseMode(MouseProtocolMode.AnyMotion)
                param.contentEquals(intArrayOf(1005)) -> setMouseEncoding(MouseProtocolEncoding.Utf8)
                param.contentEquals(intArrayOf(1006)) -> setMouseEncoding(MouseProtocolEncoding.Sgr)
                param.contentEquals(intArrayOf(1049)) -> {
                    decsc()
                    alternateGrid.clear()
                    enterAlternateGrid()
                }
                param.contentEquals(intArrayOf(2004)) -> setMode(MODE_BRACKETED_PASTE)
                else -> unhandled(this)
            }
        }
    }

    internal fun decrst(params: List<IntArray>, unhandled: (Screen) -> Unit) {
        for (param in params) {
            when {
                param.contentEquals(intArrayOf(1)) -> clearMode(MODE_APPLICATION_CURSOR)
                param.contentEquals(intArrayOf(6)) -> gridMut().setOriginMode(false)
                param.contentEquals(intArrayOf(9)) -> clearMouseMode(MouseProtocolMode.Press)
                param.contentEquals(intArrayOf(25)) -> setMode(MODE_HIDE_CURSOR)
                param.contentEquals(intArrayOf(47)) -> exitAlternateGrid()
                param.contentEquals(intArrayOf(1000)) -> clearMouseMode(MouseProtocolMode.PressRelease)
                param.contentEquals(intArrayOf(1002)) -> clearMouseMode(MouseProtocolMode.ButtonMotion)
                param.contentEquals(intArrayOf(1003)) -> clearMouseMode(MouseProtocolMode.AnyMotion)
                param.contentEquals(intArrayOf(1005)) -> clearMouseEncoding(MouseProtocolEncoding.Utf8)
                param.contentEquals(intArrayOf(1006)) -> clearMouseEncoding(MouseProtocolEncoding.Sgr)
                param.contentEquals(intArrayOf(1049)) -> {
                    exitAlternateGrid()
                    decrc()
                }
                param.contentEquals(intArrayOf(2004)) -> clearMode(MODE_BRACKETED_PASTE)
                else -> unhandled(this)
            }
        }
    }

    internal fun sgr(params: List<IntArray>, unhandled: (Screen) -> Unit) {
        if (params.isEmpty()) {
            attrs = Attrs()
            return
        }

        val iter = params.iterator()

        fun nextParam(): IntArray? = if (iter.hasNext()) iter.next() else null

        fun nextParamU8(): Int? {
            val p = nextParam() ?: return null
            if (p.size == 1 && p[0] in 0..255) {
                return p[0]
            }
            return null
        }

        while (true) {
            val param = nextParam() ?: break
            when {
                param.contentEquals(intArrayOf(0)) -> attrs = Attrs()
                param.contentEquals(intArrayOf(1)) -> attrs.setBold()
                param.contentEquals(intArrayOf(2)) -> attrs.setDim()
                param.contentEquals(intArrayOf(3)) -> attrs.setItalic(true)
                param.contentEquals(intArrayOf(4)) -> attrs.setUnderline(true)
                param.contentEquals(intArrayOf(7)) -> attrs.setInverse(true)
                param.contentEquals(intArrayOf(22)) -> attrs.setNormalIntensity()
                param.contentEquals(intArrayOf(23)) -> attrs.setItalic(false)
                param.contentEquals(intArrayOf(24)) -> attrs.setUnderline(false)
                param.contentEquals(intArrayOf(27)) -> attrs.setInverse(false)
                param.size == 1 && param[0] in 30..37 -> attrs.fgColor = Color.Idx(param[0] - 30)
                param.size == 5 && param[0] == 38 && param[1] == 2 -> {
                    if (param[2] in 0..255 && param[3] in 0..255 && param[4] in 0..255) {
                        attrs.fgColor = Color.Rgb(param[2], param[3], param[4])
                    }
                }
                param.size == 3 && param[0] == 38 && param[1] == 5 -> {
                    if (param[2] in 0..255) {
                        attrs.fgColor = Color.Idx(param[2])
                    }
                }
                param.contentEquals(intArrayOf(38)) -> {
                    val sub = nextParam()
                    when {
                        sub != null && sub.contentEquals(intArrayOf(2)) -> {
                            val r = nextParamU8()
                            val g = nextParamU8()
                            val b = nextParamU8()
                            if (r != null && g != null && b != null) {
                                attrs.fgColor = Color.Rgb(r, g, b)
                            } else {
                                unhandled(this)
                                return
                            }
                        }
                        sub != null && sub.contentEquals(intArrayOf(5)) -> {
                            val i = nextParamU8()
                            if (i != null) {
                                attrs.fgColor = Color.Idx(i)
                            } else {
                                unhandled(this)
                                return
                            }
                        }
                        else -> {
                            unhandled(this)
                            return
                        }
                    }
                }
                param.contentEquals(intArrayOf(39)) -> attrs.fgColor = Color.Default
                param.size == 1 && param[0] in 40..47 -> attrs.bgColor = Color.Idx(param[0] - 40)
                param.size == 5 && param[0] == 48 && param[1] == 2 -> {
                    if (param[2] in 0..255 && param[3] in 0..255 && param[4] in 0..255) {
                        attrs.bgColor = Color.Rgb(param[2], param[3], param[4])
                    }
                }
                param.size == 3 && param[0] == 48 && param[1] == 5 -> {
                    if (param[2] in 0..255) {
                        attrs.bgColor = Color.Idx(param[2])
                    }
                }
                param.contentEquals(intArrayOf(48)) -> {
                    val sub = nextParam()
                    when {
                        sub != null && sub.contentEquals(intArrayOf(2)) -> {
                            val r = nextParamU8()
                            val g = nextParamU8()
                            val b = nextParamU8()
                            if (r != null && g != null && b != null) {
                                attrs.bgColor = Color.Rgb(r, g, b)
                            } else {
                                unhandled(this)
                                return
                            }
                        }
                        sub != null && sub.contentEquals(intArrayOf(5)) -> {
                            val i = nextParamU8()
                            if (i != null) {
                                attrs.bgColor = Color.Idx(i)
                            } else {
                                unhandled(this)
                                return
                            }
                        }
                        else -> {
                            unhandled(this)
                            return
                        }
                    }
                }
                param.contentEquals(intArrayOf(49)) -> attrs.bgColor = Color.Default
                param.size == 1 && param[0] in 90..97 -> attrs.fgColor = Color.Idx(param[0] - 82)
                param.size == 1 && param[0] in 100..107 -> attrs.bgColor = Color.Idx(param[0] - 92)
                else -> unhandled(this)
            }
        }
    }

    internal fun decstbm(bounds: Pair<Int, Int>) {
        gridMut().setScrollRegion(bounds.first - 1, bounds.second - 1)
    }
}
