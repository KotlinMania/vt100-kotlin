// port-lint: source row.rs
package io.github.kotlinmania.vt100

import io.github.kotlinmania.vt100.grid.Pos
import io.github.kotlinmania.vt100.term.Backspace
import io.github.kotlinmania.vt100.term.ClearRowForward
import io.github.kotlinmania.vt100.term.EraseChar
import io.github.kotlinmania.vt100.term.MoveFromTo

internal class Row private constructor(
    private val cells: MutableList<Cell>,
    private var wrapped: Boolean,
) {
    internal constructor(cols: Int) : this(
        cells = MutableList(cols) { Cell.new() },
        wrapped = false,
    )

    private fun cols(): Int =
        // The grid size limits column counts to the unsigned 16-bit range.
        cells.size

    internal fun clear(attrs: Attrs) {
        for (cell in cells) {
            cell.clear(attrs.copy())
        }
        wrapped = false
    }

    private fun cells(): List<Cell> = cells

    internal fun get(col: Int): Cell? = cells.getOrNull(col)

    internal fun getMut(col: Int): Cell? = cells.getOrNull(col)

    internal fun insert(i: Int, cell: Cell) {
        cells.add(i, cell)
        wrapped = false
    }

    internal fun remove(i: Int) {
        clearWide(i)
        cells.removeAt(i)
        wrapped = false
    }

    internal fun erase(i: Int, attrs: Attrs) {
        val wide = cells[i].isWide()
        clearWide(i)
        cells[i].clear(attrs.copy())
        if (i == cols() - if (wide) 2 else 1) {
            wrapped = false
        }
    }

    internal fun truncate(len: Int) {
        while (cells.size > len) {
            cells.removeAt(cells.lastIndex)
        }
        wrapped = false
        val lastCell = cells[len - 1]
        if (lastCell.isWide()) {
            lastCell.clear(lastCell.attrs().copy())
        }
    }

    internal fun resize(len: Int, cell: Cell) {
        while (cells.size > len) {
            cells.removeAt(cells.lastIndex)
        }
        while (cells.size < len) {
            cells.add(cell.copy())
        }
        wrapped = false
    }

    internal fun wrap(wrap: Boolean) {
        wrapped = wrap
    }

    internal fun wrapped(): Boolean = wrapped

    internal fun clearWide(col: Int) {
        val cell = cells[col]
        val other =
            when {
                cell.isWide() -> cells[col + 1]
                cell.isWideContinuation() -> cells[col - 1]
                else -> return
            }
        other.clear(other.attrs().copy())
    }

    internal fun writeContents(
        contents: StringBuilder,
        start: Int,
        width: Int,
        wrapping: Boolean,
    ) {
        var previousWasWide = false
        var previousCol = start

        for (
        (col, cell) in cells()
            .withIndex()
            .drop(start)
            .take(width)
        ) {
            if (previousWasWide) {
                previousWasWide = false
                continue
            }
            previousWasWide = cell.isWide()

            // The grid size limits column counts to the unsigned 16-bit range.
            if (cell.hasContents()) {
                repeat(col - previousCol) {
                    contents.append(' ')
                }
                previousCol += col - previousCol

                contents.append(cell.contents())
                previousCol += if (cell.isWide()) 2 else 1
            }
        }
        if (previousCol == start && wrapping) {
            contents.append('\n')
        }
    }

    internal fun writeContentsFormatted(
        contents: MutableList<Byte>,
        start: Int,
        width: Int,
        row: Int,
        wrapping: Boolean,
        prevPos: Pos?,
        prevAttrs: Attrs?,
    ): Pair<Pos, Attrs> {
        var previousWasWide = false
        val defaultCell = Cell.new()

        var previousPos =
            prevPos ?: if (wrapping) {
                Pos(row = row - 1, col = cols())
            } else {
                Pos(row = row, col = start)
            }
        var previousAttrs = prevAttrs ?: Attrs()

        val firstCell = cells[start]
        if (wrapping && firstCell == defaultCell) {
            val defaultAttrs = defaultCell.attrs()
            if (previousAttrs != defaultAttrs) {
                defaultAttrs.writeEscapeCodeDiff(contents, previousAttrs)
                previousAttrs = defaultAttrs.copy()
            }
            contents.add(' '.code.toByte())
            Backspace().writeBuf(contents)
            EraseChar(1).writeBuf(contents)
            previousPos = Pos(row = row, col = 0)
        }

        var erase: Pair<Int, Attrs>? = null
        for (
        (col, cell) in cells()
            .withIndex()
            .drop(start)
            .take(width)
        ) {
            if (previousWasWide) {
                previousWasWide = false
                continue
            }
            previousWasWide = cell.isWide()

            // The grid size limits column counts to the unsigned 16-bit range.
            val pos = Pos(row = row, col = col)

            erase?.let { (previousCol, attrs) ->
                if (cell.hasContents() || cell.attrs() != attrs) {
                    val newPos = Pos(row = row, col = previousCol)
                    if (wrapping && previousPos.row + 1 == newPos.row && previousPos.col >= cols()) {
                        if (newPos.col > 0) {
                            contents.appendSpaces(newPos.col)
                        } else {
                            contents.add(' '.code.toByte())
                            Backspace().writeBuf(contents)
                        }
                    } else {
                        MoveFromTo(previousPos, newPos).writeBuf(contents)
                    }
                    previousPos = newPos
                    if (previousAttrs != attrs) {
                        attrs.writeEscapeCodeDiff(contents, previousAttrs)
                        previousAttrs = attrs.copy()
                    }
                    EraseChar(pos.col - previousCol).writeBuf(contents)
                    erase = null
                }
            }

            if (cell != defaultCell) {
                val attrs = cell.attrs()
                if (cell.hasContents()) {
                    if (pos != previousPos) {
                        if (!wrapping ||
                            previousPos.row + 1 != pos.row ||
                            previousPos.col < cols() - cell.isWide().toInt() ||
                            pos.col != 0
                        ) {
                            MoveFromTo(previousPos, pos).writeBuf(contents)
                        }
                        previousPos = pos
                    }

                    if (previousAttrs != attrs) {
                        attrs.writeEscapeCodeDiff(contents, previousAttrs)
                        previousAttrs = attrs.copy()
                    }

                    previousPos = previousPos.copy(col = previousPos.col + if (cell.isWide()) 2 else 1)
                    contents.appendBytes(cell.contents())
                } else if (erase == null) {
                    erase = pos.col to attrs.copy()
                }
            }
        }
        erase?.let { (previousCol, attrs) ->
            val newPos = Pos(row = row, col = previousCol)
            if (wrapping && previousPos.row + 1 == newPos.row && previousPos.col >= cols()) {
                if (newPos.col > 0) {
                    contents.appendSpaces(newPos.col)
                } else {
                    contents.add(' '.code.toByte())
                    Backspace().writeBuf(contents)
                }
            } else {
                MoveFromTo(previousPos, newPos).writeBuf(contents)
            }
            previousPos = newPos
            if (previousAttrs != attrs) {
                attrs.writeEscapeCodeDiff(contents, previousAttrs)
                previousAttrs = attrs.copy()
            }
            ClearRowForward().writeBuf(contents)
        }

        return previousPos to previousAttrs
    }

    // Although most of this logic matches writeContentsFormatted, the upstream
    // keeps it specialized to avoid making the shared path noticeably slower.
    internal fun writeContentsDiff(
        contents: MutableList<Byte>,
        prev: Row,
        start: Int,
        width: Int,
        row: Int,
        wrapping: Boolean,
        prevWrapping: Boolean,
        prevPos: Pos,
        prevAttrs: Attrs,
    ): Pair<Pos, Attrs> {
        var previousPos = prevPos
        var previousAttrs = prevAttrs
        var previousWasWide = false

        val firstCell = cells[start]
        val prevFirstCell = prev.cells[start]
        if (wrapping &&
            !prevWrapping &&
            firstCell == prevFirstCell &&
            previousPos.row + 1 == row &&
            previousPos.col >= cols() - prevFirstCell.isWide().toInt()
        ) {
            val firstCellAttrs = firstCell.attrs()
            if (previousAttrs != firstCellAttrs) {
                firstCellAttrs.writeEscapeCodeDiff(contents, previousAttrs)
                previousAttrs = firstCellAttrs.copy()
            }
            var cellContents = prevFirstCell.contents()
            val needErase =
                if (cellContents.isEmpty()) {
                    cellContents = " "
                    true
                } else {
                    false
                }
            contents.appendBytes(cellContents)
            Backspace().writeBuf(contents)
            if (prevFirstCell.isWide()) {
                Backspace().writeBuf(contents)
            }
            if (needErase) {
                EraseChar(1).writeBuf(contents)
            }
            previousPos = Pos(row = row, col = 0)
        }

        var erase: Pair<Int, Attrs>? = null
        for (
        (col, cellPair) in cells()
            .zip(prev.cells())
            .withIndex()
            .drop(start)
            .take(width)
        ) {
            val (cell, prevCell) = cellPair
            if (previousWasWide) {
                previousWasWide = false
                continue
            }
            previousWasWide = cell.isWide()

            // The grid size limits column counts to the unsigned 16-bit range.
            val pos = Pos(row = row, col = col)

            erase?.let { (previousCol, attrs) ->
                if (cell.hasContents() || cell.attrs() != attrs) {
                    val newPos = Pos(row = row, col = previousCol)
                    if (wrapping && previousPos.row + 1 == newPos.row && previousPos.col >= cols()) {
                        if (newPos.col > 0) {
                            contents.appendSpaces(newPos.col)
                        } else {
                            contents.add(' '.code.toByte())
                            Backspace().writeBuf(contents)
                        }
                    } else {
                        MoveFromTo(previousPos, newPos).writeBuf(contents)
                    }
                    previousPos = newPos
                    if (previousAttrs != attrs) {
                        attrs.writeEscapeCodeDiff(contents, previousAttrs)
                        previousAttrs = attrs.copy()
                    }
                    EraseChar(pos.col - previousCol).writeBuf(contents)
                    erase = null
                }
            }

            if (cell != prevCell) {
                val attrs = cell.attrs()
                if (cell.hasContents()) {
                    if (pos != previousPos) {
                        if (!wrapping ||
                            previousPos.row + 1 != pos.row ||
                            previousPos.col < cols() - cell.isWide().toInt() ||
                            pos.col != 0
                        ) {
                            MoveFromTo(previousPos, pos).writeBuf(contents)
                        }
                        previousPos = pos
                    }

                    if (previousAttrs != attrs) {
                        attrs.writeEscapeCodeDiff(contents, previousAttrs)
                        previousAttrs = attrs.copy()
                    }

                    previousPos = previousPos.copy(col = previousPos.col + if (cell.isWide()) 2 else 1)
                    contents.appendBytes(cell.contents())
                } else if (erase == null) {
                    erase = pos.col to attrs.copy()
                }
            }
        }
        erase?.let { (previousCol, attrs) ->
            val newPos = Pos(row = row, col = previousCol)
            if (wrapping && previousPos.row + 1 == newPos.row && previousPos.col >= cols()) {
                if (newPos.col > 0) {
                    contents.appendSpaces(newPos.col)
                } else {
                    contents.add(' '.code.toByte())
                    Backspace().writeBuf(contents)
                }
            } else {
                MoveFromTo(previousPos, newPos).writeBuf(contents)
            }
            previousPos = newPos
            if (previousAttrs != attrs) {
                attrs.writeEscapeCodeDiff(contents, previousAttrs)
                previousAttrs = attrs.copy()
            }
            ClearRowForward().writeBuf(contents)
        }

        // If this row goes from wrapped to not wrapped, erase and redraw the
        // last character to break wrapping. If this row is wrapped, redraw the
        // last character without erasing it so the cursor is positioned after
        // the end of the line and drawing the next line can start writing.
        if ((!wrapped && prev.wrapped) || (!prev.wrapped && wrapped)) {
            val endPos =
                if (cells[cols() - 1].isWideContinuation()) {
                    Pos(row = row, col = cols() - 2)
                } else {
                    Pos(row = row, col = cols() - 1)
                }
            MoveFromTo(previousPos, endPos).writeBuf(contents)
            previousPos = endPos
            if (!wrapped) {
                EraseChar(1).writeBuf(contents)
            }
            val endCell = cells[endPos.col]
            if (endCell.hasContents()) {
                val attrs = endCell.attrs()
                if (previousAttrs != attrs) {
                    attrs.writeEscapeCodeDiff(contents, previousAttrs)
                    previousAttrs = attrs.copy()
                }
                contents.appendBytes(endCell.contents())
                previousPos = previousPos.copy(col = previousPos.col + if (endCell.isWide()) 2 else 1)
            }
        }

        return previousPos to previousAttrs
    }

    internal fun copy(): Row =
        Row(
            cells = cells.map { it.copy() }.toMutableList(),
            wrapped = wrapped,
        )

    override fun toString(): String =
        "Row(cells=$cells, wrapped=$wrapped)"

    internal companion object {
        internal fun new(cols: Int): Row = Row(cols)
    }
}

private fun Boolean.toInt(): Int = if (this) 1 else 0

private fun MutableList<Byte>.appendBytes(value: String) {
    addAll(value.encodeToByteArray().asList())
}

private fun MutableList<Byte>.appendSpaces(count: Int) {
    repeat(count) {
        add(' '.code.toByte())
    }
}
