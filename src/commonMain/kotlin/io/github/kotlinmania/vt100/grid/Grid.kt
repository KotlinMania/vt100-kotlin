// port-lint: source vt100/src/grid.rs
package io.github.kotlinmania.vt100.grid

import io.github.kotlinmania.vt100.Attrs
import io.github.kotlinmania.vt100.Cell
import io.github.kotlinmania.vt100.Row
import io.github.kotlinmania.vt100.term.Backspace
import io.github.kotlinmania.vt100.term.ClearAttrs
import io.github.kotlinmania.vt100.term.ClearScreen
import io.github.kotlinmania.vt100.term.EraseChar
import io.github.kotlinmania.vt100.term.MoveFromTo
import io.github.kotlinmania.vt100.term.MoveTo
import io.github.kotlinmania.vt100.term.RestoreCursor
import io.github.kotlinmania.vt100.term.SaveCursor

/**
 * The dimensions of a [Grid], measured in rows and columns.
 *
 * `cols` is constrained to a `UShort` upstream so the maximum value is 65535;
 * the Kotlin port keeps the value in an `Int` for ergonomic arithmetic and
 * relies on the upstream invariant that callers respect that ceiling.
 */
internal data class Size(
    internal val rows: Int = 0,
    internal val cols: Int = 0,
)

/**
 * A position within a [Grid], measured in rows and columns from the top-left
 * cell.
 */
internal data class Pos(
    internal val row: Int = 0,
    internal val col: Int = 0,
)

internal class Grid private constructor(
    private var size: Size,
    private val scrollbackLen: Int,
) {
    private var pos: Pos = Pos()
    private var savedPos: Pos = Pos()
    private val rows: MutableList<Row> = mutableListOf()
    private var scrollTop: Int = 0
    private var scrollBottom: Int = size.rows - 1
    private var originMode: Boolean = false
    private var savedOriginMode: Boolean = false
    private val scrollback: ArrayDeque<Row> = ArrayDeque()
    private var scrollbackOffset: Int = 0

    internal fun allocateRows() {
        if (rows.isEmpty()) {
            repeat(size.rows) {
                rows.add(Row.new(size.cols))
            }
        }
    }

    private fun newRow(): Row = Row.new(size.cols)

    internal fun clear() {
        pos = Pos()
        savedPos = Pos()
        for (row in rows) {
            row.clear(Attrs())
        }
        scrollTop = 0
        scrollBottom = size.rows - 1
        originMode = false
        savedOriginMode = false
    }

    internal fun size(): Size = size

    internal fun setSize(newSize: Size) {
        if (newSize.cols != size.cols) {
            for (row in rows) {
                row.wrap(false)
            }
        }

        if (scrollBottom == size.rows - 1) {
            scrollBottom = newSize.rows - 1
        }

        size = newSize
        for (row in rows) {
            row.resize(newSize.cols, Cell.new())
        }
        // Grow or shrink the row list to match the new row count.
        while (rows.size < newSize.rows) {
            rows.add(newRow())
        }
        while (rows.size > newSize.rows) {
            rows.removeAt(rows.lastIndex)
        }

        if (scrollBottom >= newSize.rows) {
            scrollBottom = newSize.rows - 1
        }
        if (scrollBottom < scrollTop) {
            scrollTop = 0
        }

        rowClampTop(false)
        rowClampBottom(false)
        colClamp()

        if (savedPos.row > size.rows - 1) {
            savedPos = savedPos.copy(row = size.rows - 1)
        }
        if (savedPos.col > size.cols - 1) {
            savedPos = savedPos.copy(col = size.cols - 1)
        }
    }

    internal fun pos(): Pos = pos

    internal fun setPos(newPos: Pos) {
        var p = newPos
        if (originMode) {
            p = p.copy(row = saturatingAdd(p.row, scrollTop))
        }
        pos = p
        rowClampTop(originMode)
        rowClampBottom(originMode)
        colClamp()
    }

    internal fun saveCursor() {
        savedPos = pos
        savedOriginMode = originMode
    }

    internal fun restoreCursor() {
        pos = savedPos
        originMode = savedOriginMode
    }

    internal fun visibleRows(): Sequence<Row> =
        sequence {
            val scrollbackLen = scrollback.size
            val rowsLen = rows.size
            val skip = scrollbackLen - scrollbackOffset
            // when scrollbackOffset > rowsLen (e.g. rows = 3, scrollbackLen = 10,
            // offset = 9) the skip(10 - 9) will take 9 rows instead of 3. We
            // need to set the upper bound to rowsLen (e.g. 3).
            var emitted = 0
            for ((idx, row) in scrollback.withIndex()) {
                if (idx < skip) continue
                if (emitted >= rowsLen) break
                yield(row)
                emitted++
            }
            // Same for rowsLen - scrollbackOffset (e.g. 3 - 9). It would
            // overflow with unsigned arithmetic upstream, so saturate here.
            val take = (rowsLen - scrollbackOffset).coerceAtLeast(0)
            for ((idx, row) in rows.withIndex()) {
                if (idx >= take) break
                yield(row)
            }
        }

    internal fun drawingRows(): Sequence<Row> = rows.asSequence()

    internal fun drawingRowsMut(): Sequence<Row> = rows.asSequence()

    internal fun visibleRow(row: Int): Row? = visibleRows().elementAtOrNull(row)

    internal fun drawingRow(row: Int): Row? = rows.getOrNull(row)

    internal fun drawingRowMut(row: Int): Row? = rows.getOrNull(row)

    internal fun currentRowMut(): Row =
        // we assume pos.row is always valid
        drawingRowMut(pos.row)!!

    internal fun visibleCell(p: Pos): Cell? = visibleRow(p.row)?.get(p.col)

    internal fun drawingCell(p: Pos): Cell? = drawingRow(p.row)?.get(p.col)

    internal fun drawingCellMut(p: Pos): Cell? = drawingRowMut(p.row)?.getMut(p.col)

    internal fun scrollbackLen(): Int = scrollbackLen

    internal fun scrollback(): Int = scrollbackOffset

    internal fun setScrollback(rows: Int) {
        scrollbackOffset = rows.coerceAtMost(scrollback.size)
    }

    internal fun writeContents(contents: StringBuilder) {
        var wrapping = false
        for (row in visibleRows()) {
            row.writeContents(contents, 0, size.cols, wrapping)
            if (!row.wrapped()) {
                contents.append('\n')
            }
            wrapping = row.wrapped()
        }

        while (contents.isNotEmpty() && contents.last() == '\n') {
            contents.deleteAt(contents.length - 1)
        }
    }

    internal fun writeContentsFormatted(contents: MutableList<Byte>): Attrs {
        ClearAttrs().writeBuf(contents)
        ClearScreen().writeBuf(contents)

        var prevAttrs = Attrs()
        var prevPos = Pos()
        var wrapping = false
        for ((i, row) in visibleRows().withIndex()) {
            // we limit the number of cols to a u16 (see Size), so visibleRows
            // can never return more rows than will fit
            val (newPos, newAttrs) =
                row.writeContentsFormatted(
                    contents,
                    0,
                    size.cols,
                    i,
                    wrapping,
                    prevPos,
                    prevAttrs,
                )
            prevPos = newPos
            prevAttrs = newAttrs
            wrapping = row.wrapped()
        }

        writeCursorPositionFormatted(contents, prevPos, prevAttrs)

        return prevAttrs
    }

    internal fun writeContentsDiff(
        contents: MutableList<Byte>,
        prev: Grid,
        prevAttrsInitial: Attrs,
    ): Attrs {
        var prevAttrs = prevAttrsInitial
        var prevPos = prev.pos
        var wrapping = false
        var prevWrapping = false
        val thisIt = visibleRows().iterator()
        val prevIt = prev.visibleRows().iterator()
        var i = 0
        while (thisIt.hasNext() && prevIt.hasNext()) {
            val row = thisIt.next()
            val prevRow = prevIt.next()
            // we limit the number of cols to a u16 (see Size), so visibleRows
            // can never return more rows than will fit
            val (newPos, newAttrs) =
                row.writeContentsDiff(
                    contents,
                    prevRow,
                    0,
                    size.cols,
                    i,
                    wrapping,
                    prevWrapping,
                    prevPos,
                    prevAttrs,
                )
            prevPos = newPos
            prevAttrs = newAttrs
            wrapping = row.wrapped()
            prevWrapping = prevRow.wrapped()
            i++
        }

        writeCursorPositionFormatted(contents, prevPos, prevAttrs)

        return prevAttrs
    }

    internal fun writeCursorPositionFormatted(
        contents: MutableList<Byte>,
        prevPos: Pos?,
        prevAttrs: Attrs?,
    ) {
        val prevAttrsLocal = prevAttrs ?: Attrs()
        // writing a character to the last column of a row doesn't wrap the
        // cursor immediately - it waits until the next character is actually
        // drawn. it is only possible for the cursor to have this kind of
        // position after drawing a character though, so if we end in this
        // position, we need to redraw the character at the end of the row.
        if (prevPos != pos && pos.col >= size.cols) {
            var p = Pos(row = pos.row, col = size.cols - 1)
            if (
                // we assume pos.row is always valid, and size.cols - 1 is
                // always a valid column
                drawingCell(p)!!.isWideContinuation()
            ) {
                p = p.copy(col = size.cols - 2)
            }
            // we assume pos.row is always valid, and size.cols - 2 must be a
            // valid column because size.cols - 1 is always valid and we just
            // checked that the cell at size.cols - 1 is a wide continuation
            // character, which means that the first half of the wide
            // character must be before it.
            val cell = drawingCell(p)!!
            if (cell.hasContents()) {
                if (prevPos != null) {
                    MoveFromTo(prevPos, p).writeBuf(contents)
                } else {
                    MoveTo(p).writeBuf(contents)
                }
                cell.attrs().writeEscapeCodeDiff(contents, prevAttrsLocal)
                contents.appendBytes(cell.contents())
                prevAttrsLocal.writeEscapeCodeDiff(contents, cell.attrs())
            } else {
                // if the cell doesn't have contents, we can't have gotten here
                // by drawing a character in the last column. this means that
                // as far as i'm aware, we have to have reached here from a
                // newline when we were already after the end of an earlier
                // row. in the case where we are already after the end of an
                // earlier row, we can just write a few newlines, otherwise we
                // also need to do the same as above to get ourselves to after
                // the end of a row.
                var found = false
                var i = pos.row - 1
                while (i >= 0) {
                    var pp = p.copy(row = i, col = size.cols - 1)
                    if (
                        // i is always less than pos.row, which we assume to be
                        // always valid, so it must also be valid. size.cols
                        // - 1 is always a valid col.
                        drawingCell(pp)!!.isWideContinuation()
                    ) {
                        pp = pp.copy(col = size.cols - 2)
                    }
                    // i is always less than pos.row, which we assume to be
                    // always valid, so it must also be valid. size.cols - 2
                    // is valid because size.cols - 1 is always valid, and col
                    // gets set to size.cols - 2 when the cell at size.cols
                    // - 1 is a wide continuation character, meaning that the
                    // first half of the wide character must be before it.
                    val cell2 = drawingCell(pp)!!
                    if (cell2.hasContents()) {
                        if (prevPos != null) {
                            if (prevPos.row != i || prevPos.col < size.cols) {
                                MoveFromTo(prevPos, pp).writeBuf(contents)
                                cell2.attrs().writeEscapeCodeDiff(contents, prevAttrsLocal)
                                contents.appendBytes(cell2.contents())
                                prevAttrsLocal.writeEscapeCodeDiff(contents, cell2.attrs())
                            }
                        } else {
                            MoveTo(pp).writeBuf(contents)
                            cell2.attrs().writeEscapeCodeDiff(contents, prevAttrsLocal)
                            contents.appendBytes(cell2.contents())
                            prevAttrsLocal.writeEscapeCodeDiff(contents, cell2.attrs())
                        }
                        repeat(pos.row - i) {
                            contents.add('\n'.code.toByte())
                        }
                        p = pp
                        found = true
                        break
                    }
                    i--
                }

                // this can happen if you get the cursor off the end of a row,
                // and then do something to clear the end of the current row
                // without moving the cursor (IL, DL, ED, EL, etc). we know
                // there can't be something in the last column because we would
                // have caught that above, so it should be safe to overwrite
                // it.
                if (!found) {
                    p = Pos(row = pos.row, col = size.cols - 1)
                    if (prevPos != null) {
                        MoveFromTo(prevPos, p).writeBuf(contents)
                    } else {
                        MoveTo(p).writeBuf(contents)
                    }
                    contents.add(' '.code.toByte())
                    // we know that the cell has no contents, but it still may
                    // have drawing attributes (background color, etc)
                    // we assume pos.row is always valid, and size.cols - 1 is
                    // always a valid column
                    val endCell = drawingCell(p)!!
                    endCell.attrs().writeEscapeCodeDiff(contents, prevAttrsLocal)
                    SaveCursor().writeBuf(contents)
                    Backspace().writeBuf(contents)
                    EraseChar(1).writeBuf(contents)
                    RestoreCursor().writeBuf(contents)
                    prevAttrsLocal.writeEscapeCodeDiff(contents, endCell.attrs())
                }
            }
        } else if (prevPos != null) {
            MoveFromTo(prevPos, pos).writeBuf(contents)
        } else {
            MoveTo(pos).writeBuf(contents)
        }
    }

    internal fun eraseAll(attrs: Attrs) {
        for (row in rows) {
            row.clear(attrs)
        }
    }

    internal fun eraseAllForward(attrs: Attrs) {
        val p = pos
        for ((idx, row) in rows.withIndex()) {
            if (idx <= p.row) continue
            row.clear(attrs)
        }

        eraseRowForward(attrs)
    }

    internal fun eraseAllBackward(attrs: Attrs) {
        val p = pos
        for ((idx, row) in rows.withIndex()) {
            if (idx >= p.row) break
            row.clear(attrs)
        }

        eraseRowBackward(attrs)
    }

    internal fun eraseRow(attrs: Attrs) {
        currentRowMut().clear(attrs)
    }

    internal fun eraseRowForward(attrs: Attrs) {
        val sz = size
        val p = pos
        val row = currentRowMut()
        var col = p.col
        while (col < sz.cols) {
            row.erase(col, attrs)
            col++
        }
    }

    internal fun eraseRowBackward(attrs: Attrs) {
        val sz = size
        val p = pos
        val row = currentRowMut()
        val limit = p.col.coerceAtMost(sz.cols - 1)
        var col = 0
        while (col <= limit) {
            row.erase(col, attrs)
            col++
        }
    }

    internal fun insertCells(count: Int) {
        val sz = size
        val p = pos
        val wide =
            p.col < sz.cols &&
                // we assume pos.row is always valid, and we know we are not off
                // the end of a row because we just checked p.col < sz.cols
                drawingCell(p)!!.isWideContinuation()
        val row = currentRowMut()
        repeat(count) {
            if (wide) {
                row.getMut(p.col)!!.setWideContinuation(false)
            }
            row.insert(p.col, Cell.new())
            if (wide) {
                row.getMut(p.col)!!.setWideContinuation(true)
            }
        }
        row.truncate(sz.cols)
    }

    internal fun deleteCells(count: Int) {
        val sz = size
        val p = pos
        val row = currentRowMut()
        val n = count.coerceAtMost(sz.cols - p.col)
        repeat(n) {
            row.remove(p.col)
        }
        row.resize(sz.cols, Cell.new())
    }

    internal fun eraseCells(count: Int, attrs: Attrs) {
        val sz = size
        val p = pos
        val row = currentRowMut()
        val end = saturatingAdd(p.col, count).coerceAtMost(sz.cols)
        var col = p.col
        while (col < end) {
            row.erase(col, attrs)
            col++
        }
    }

    internal fun insertLines(count: Int) {
        repeat(count) {
            rows.removeAt(scrollBottom)
            rows.add(pos.row, newRow())
            // scrollBottom is maintained to always be a valid row
            rows[scrollBottom].wrap(false)
        }
    }

    internal fun deleteLines(count: Int) {
        val n = count.coerceAtMost(size.rows - pos.row)
        repeat(n) {
            rows.add(scrollBottom + 1, newRow())
            rows.removeAt(pos.row)
        }
    }

    internal fun scrollUp(count: Int) {
        val n = count.coerceAtMost(size.rows - scrollTop)
        repeat(n) {
            rows.add(scrollBottom + 1, newRow())
            val removed = rows.removeAt(scrollTop)
            if (scrollbackLen > 0 && !scrollRegionActive()) {
                scrollback.addLast(removed)
                while (scrollback.size > scrollbackLen) {
                    scrollback.removeFirst()
                }
                if (scrollbackOffset > 0) {
                    scrollbackOffset = scrollback.size.coerceAtMost(scrollbackOffset + 1)
                }
            }
        }
    }

    internal fun scrollDown(count: Int) {
        repeat(count) {
            rows.removeAt(scrollBottom)
            rows.add(scrollTop, newRow())
            // scrollBottom is maintained to always be a valid row
            rows[scrollBottom].wrap(false)
        }
    }

    internal fun setScrollRegion(top: Int, bottom: Int) {
        val clampedBottom = bottom.coerceAtMost(size().rows - 1)
        if (top < clampedBottom) {
            scrollTop = top
            scrollBottom = clampedBottom
        } else {
            scrollTop = 0
            scrollBottom = size().rows - 1
        }
        pos = Pos(row = scrollTop, col = 0)
    }

    private fun inScrollRegion(): Boolean =
        pos.row >= scrollTop && pos.row <= scrollBottom

    private fun scrollRegionActive(): Boolean =
        scrollTop != 0 || scrollBottom != size.rows - 1

    internal fun setOriginMode(mode: Boolean) {
        originMode = mode
        setPos(Pos(row = 0, col = 0))
    }

    internal fun rowIncClamp(count: Int) {
        val inRegion = inScrollRegion()
        pos = pos.copy(row = saturatingAdd(pos.row, count))
        rowClampBottom(inRegion)
    }

    internal fun rowIncScroll(count: Int): Int {
        val inRegion = inScrollRegion()
        pos = pos.copy(row = saturatingAdd(pos.row, count))
        val lines = rowClampBottom(inRegion)
        return if (inRegion) {
            scrollUp(lines)
            lines
        } else {
            0
        }
    }

    internal fun rowDecClamp(count: Int) {
        val inRegion = inScrollRegion()
        pos = pos.copy(row = saturatingSub(pos.row, count))
        rowClampTop(inRegion)
    }

    internal fun rowDecScroll(count: Int) {
        val inRegion = inScrollRegion()
        // need to account for clamping by both rowClampTop and by saturatingSub
        val extraLines = saturatingSub(count, pos.row)
        pos = pos.copy(row = saturatingSub(pos.row, count))
        val lines = rowClampTop(inRegion)
        scrollDown(lines + extraLines)
    }

    internal fun rowSet(i: Int) {
        pos = pos.copy(row = i)
        rowClamp()
    }

    internal fun colInc(count: Int) {
        pos = pos.copy(col = saturatingAdd(pos.col, count))
    }

    internal fun colIncClamp(count: Int) {
        pos = pos.copy(col = saturatingAdd(pos.col, count))
        colClamp()
    }

    internal fun colDec(count: Int) {
        pos = pos.copy(col = saturatingSub(pos.col, count))
    }

    internal fun colTab() {
        var c = pos.col
        c -= c % 8
        c += 8
        pos = pos.copy(col = c)
        colClamp()
    }

    internal fun colSet(i: Int) {
        pos = pos.copy(col = i)
        colClamp()
    }

    internal fun colWrap(width: Int, wrap: Boolean) {
        if (pos.col > size.cols - width) {
            var prevPos = pos
            pos = pos.copy(col = 0)
            val scrolled = rowIncScroll(1)
            prevPos = prevPos.copy(row = prevPos.row - scrolled)
            val newPos = pos
            // we assume pos.row is always valid, and so prevPos.row must be
            // valid because it is always less than or equal to pos.row
            drawingRowMut(prevPos.row)!!
                .wrap(wrap && prevPos.row + 1 == newPos.row)
        }
    }

    private fun rowClampTop(limitToScrollRegion: Boolean): Int =
        if (limitToScrollRegion && pos.row < scrollTop) {
            val rows = scrollTop - pos.row
            pos = pos.copy(row = scrollTop)
            rows
        } else {
            0
        }

    private fun rowClampBottom(limitToScrollRegion: Boolean): Int {
        val bottom =
            if (limitToScrollRegion) {
                scrollBottom
            } else {
                size.rows - 1
            }
        return if (pos.row > bottom) {
            val rows = pos.row - bottom
            pos = pos.copy(row = bottom)
            rows
        } else {
            0
        }
    }

    private fun rowClamp() {
        if (pos.row > size.rows - 1) {
            pos = pos.copy(row = size.rows - 1)
        }
    }

    private fun colClamp() {
        if (pos.col > size.cols - 1) {
            pos = pos.copy(col = size.cols - 1)
        }
    }

    internal fun copy(): Grid {
        val out = Grid(size, scrollbackLen)
        out.pos = pos
        out.savedPos = savedPos
        for (row in rows) {
            out.rows.add(row.copy())
        }
        out.scrollTop = scrollTop
        out.scrollBottom = scrollBottom
        out.originMode = originMode
        out.savedOriginMode = savedOriginMode
        for (row in scrollback) {
            out.scrollback.addLast(row.copy())
        }
        out.scrollbackOffset = scrollbackOffset
        return out
    }

    internal companion object {
        internal fun new(size: Size, scrollbackLen: Int): Grid = Grid(size, scrollbackLen)
    }
}

private fun saturatingAdd(a: Int, b: Int): Int {
    val s = a + b
    return if (s < a) Int.MAX_VALUE else s
}

private fun saturatingSub(a: Int, b: Int): Int {
    val s = a - b
    return if (s > a) {
        0
    } else if (s < 0) {
        0
    } else {
        s
    }
}

private fun MutableList<Byte>.appendBytes(s: String) {
    for (ch in s) {
        val code = ch.code
        if (code < 0x80) {
            add(code.toByte())
        } else if (code < 0x800) {
            add((0xC0 or (code shr 6)).toByte())
            add((0x80 or (code and 0x3F)).toByte())
        } else {
            add((0xE0 or (code shr 12)).toByte())
            add((0x80 or ((code shr 6) and 0x3F)).toByte())
            add((0x80 or (code and 0x3F)).toByte())
        }
    }
}
