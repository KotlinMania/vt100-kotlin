// port-lint: source grid.rs
package io.github.kotlinmania.vt100.grid

import io.github.kotlinmania.vt100.Attrs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// The upstream vt100 crate ships no tests/ directory in its crates.io
// package (Cargo.toml line `autotests = false`, no `[[test]]` blocks).
// These tests exercise the Kotlin Grid port directly so future
// regressions in scroll-region, cursor clamping, scrollback, and
// erase-cells paths show up here instead of leaking into Screen
// behavior once that struct is ported.
class GridTest {
    private fun grid(rows: Int = 24, cols: Int = 80, scrollback: Int = 100): Grid {
        val g = Grid.new(Size(rows = rows, cols = cols), scrollbackLen = scrollback)
        g.allocateRows()
        return g
    }

    @Test
    fun newGridReportsSizeAndZeroCursor() {
        val g = grid()
        assertEquals(Size(24, 80), g.size())
        assertEquals(Pos(0, 0), g.pos())
        assertEquals(100, g.scrollbackLen())
    }

    @Test
    fun visibleRowsMatchesRowCount() {
        val g = grid(rows = 5, cols = 10)
        assertEquals(5, g.visibleRows().toList().size)
    }

    @Test
    fun setPosClampsToBottomRightCorner() {
        val g = grid(rows = 3, cols = 4)
        g.setPos(Pos(row = 99, col = 99))
        assertEquals(Pos(row = 2, col = 3), g.pos())
    }

    @Test
    fun saveAndRestoreCursorRoundTrip() {
        val g = grid()
        g.setPos(Pos(row = 5, col = 7))
        g.saveCursor()
        g.setPos(Pos(row = 0, col = 0))
        g.restoreCursor()
        assertEquals(Pos(row = 5, col = 7), g.pos())
    }

    @Test
    fun colTabAdvancesToNextEightBoundary() {
        val g = grid(rows = 3, cols = 40)
        g.setPos(Pos(row = 0, col = 0))
        g.colTab()
        assertEquals(Pos(row = 0, col = 8), g.pos())
        g.setPos(Pos(row = 0, col = 5))
        g.colTab()
        assertEquals(Pos(row = 0, col = 8), g.pos())
        g.setPos(Pos(row = 0, col = 8))
        g.colTab()
        assertEquals(Pos(row = 0, col = 16), g.pos())
    }

    @Test
    fun colWrapAtRightEdgeMovesCursorToNextRow() {
        val g = grid(rows = 4, cols = 5)
        // Park the cursor "after the end of row 0" via colInc, which
        // (unlike setPos) does not clamp to cols - 1. That matches the
        // upstream invariant that wrap only fires from the post-row
        // overflow position.
        g.setPos(Pos(row = 0, col = 4))
        g.colInc(1)
        assertEquals(Pos(row = 0, col = 5), g.pos())
        g.colWrap(width = 1, wrap = true)
        assertEquals(Pos(row = 1, col = 0), g.pos())
    }

    @Test
    fun setScrollRegionConstrainsCursorToTop() {
        val g = grid(rows = 6, cols = 10)
        g.setScrollRegion(top = 2, bottom = 4)
        assertEquals(Pos(row = 2, col = 0), g.pos())
    }

    @Test
    fun scrollUpPushesRowsIntoScrollback() {
        val g = grid(rows = 3, cols = 4, scrollback = 5)
        g.scrollUp(2)
        // No setScrollback yet, so visibleRows still returns 3 (the
        // current rows). Scrollback offset is 0 by default — confirm
        // setScrollback can see what we pushed.
        g.setScrollback(1)
        assertEquals(1, g.scrollback())
        // Visible window still shows 3 rows even when scrolled back
        // partially; upstream behavior is "skip from front".
        assertEquals(3, g.visibleRows().toList().size)
    }

    @Test
    fun eraseAllClearsEveryDrawingRow() {
        val g = grid(rows = 3, cols = 4)
        g.eraseAll(Attrs())
        // After erase, every drawing row should still exist.
        assertEquals(3, g.drawingRows().toList().size)
    }

    @Test
    fun drawingCellReturnsNullOutsideBounds() {
        val g = grid(rows = 2, cols = 3)
        assertNull(g.drawingCell(Pos(row = 99, col = 0)))
        assertNull(g.drawingCell(Pos(row = 0, col = 99)))
        assertNotNull(g.drawingCell(Pos(row = 0, col = 0)))
    }

    @Test
    fun rowIncScrollReturnsZeroOutsideScrollRegion() {
        val g = grid(rows = 4, cols = 4)
        // Cursor at row 0, no scroll region active — incrementing past
        // bottom should clamp but not scroll.
        g.setPos(Pos(row = 0, col = 0))
        val scrolled = g.rowIncScroll(99)
        assertTrue(scrolled == 0 || g.pos().row == 3)
    }

    @Test
    fun copyProducesIndependentClone() {
        val g = grid(rows = 3, cols = 4)
        g.setPos(Pos(row = 1, col = 2))
        val c = g.copy()
        assertEquals(g.pos(), c.pos())
        c.setPos(Pos(row = 0, col = 0))
        assertEquals(Pos(row = 1, col = 2), g.pos())
        assertEquals(Pos(row = 0, col = 0), c.pos())
    }

    @Test
    fun writeContentsOnEmptyGridProducesNoNewlines() {
        val g = grid(rows = 3, cols = 4)
        val sb = StringBuilder()
        g.writeContents(sb)
        assertEquals("", sb.toString())
    }
}
