// port-lint: tests row.rs
package io.github.kotlinmania.vt100

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RowTest {
    @Test
    fun testRowCreationAndCells() {
        val row = Row.new(80)
        assertFalse(row.wrapped())
        val cell0 = row.get(0)
        assertNotNull(cell0)
        assertFalse(cell0.hasContents())

        row.wrap(true)
        assertTrue(row.wrapped())
    }

    @Test
    fun testRowClearAndErase() {
        val row = Row.new(40)
        val attrs = Attrs()
        row.clear(attrs)
        assertFalse(row.wrapped())

        val cell = Cell.new()
        cell.set('X'.code, attrs)
        row.insert(0, cell)
        row.erase(0, attrs)
        assertFalse(row.get(0)!!.hasContents())
    }

    @Test
    fun testRowTruncateAndResize() {
        val row = Row.new(40)
        row.truncate(20)
        row.resize(30, Cell.new())
        val copied = row.copy()
        assertEquals(row.toString(), copied.toString())
    }

    @Test
    fun testRowWriteContents() {
        val row = Row.new(10)
        val cell = Cell.new()
        cell.set('A'.code, Attrs())
        row.getMut(0)?.set('A'.code, Attrs())
        row.getMut(1)?.set('B'.code, Attrs())

        val sb = StringBuilder()
        row.writeContents(sb, 0, 10, false)
        assertEquals("AB", sb.toString())
    }
}
