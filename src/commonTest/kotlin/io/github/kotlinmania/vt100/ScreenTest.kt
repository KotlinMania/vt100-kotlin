// port-lint: tests vt100/src/screen.rs
package io.github.kotlinmania.vt100

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScreenTest {
    @Test
    fun screenInitializationHasGivenDimensions() {
        val screen = Screen(rows = 24, cols = 80, scrollbackLen = 100)
        assertEquals(Pair(24, 80), screen.size())
        assertEquals(0, screen.scrollback())
        assertEquals(100, screen.scrollbackLen())
        assertEquals(Pair(0, 0), screen.cursorPosition())
        assertFalse(screen.hideCursor())
        assertFalse(screen.alternateScreen())
        assertEquals(MouseProtocolMode.None, screen.mouseProtocolMode())
        assertEquals(MouseProtocolEncoding.Default, screen.mouseProtocolEncoding())
    }

    @Test
    fun screenTextAndContents() {
        val screen = Screen(rows = 3, cols = 10)
        "Hello".forEach { screen.text(it) }
        assertEquals(Pair(0, 5), screen.cursorPosition())
        assertEquals("Hello", screen.contents().trim())
        assertEquals("Hello", screen.rows(0, 5).first())
    }

    @Test
    fun screenCursorMovements() {
        val screen = Screen(rows = 10, cols = 10)
        screen.cup(5, 5)
        assertEquals(Pair(4, 4), screen.cursorPosition())
        screen.cuu(2)
        assertEquals(Pair(2, 4), screen.cursorPosition())
        screen.cud(3)
        assertEquals(Pair(5, 4), screen.cursorPosition())
        screen.cuf(2)
        assertEquals(Pair(5, 6), screen.cursorPosition())
        screen.cub(4)
        assertEquals(Pair(5, 2), screen.cursorPosition())
        screen.cnl(2)
        assertEquals(Pair(7, 0), screen.cursorPosition())
        screen.cpl(1)
        assertEquals(Pair(6, 0), screen.cursorPosition())
        screen.cha(8)
        assertEquals(Pair(6, 7), screen.cursorPosition())
        screen.vpa(3)
        assertEquals(Pair(2, 7), screen.cursorPosition())
    }

    @Test
    fun screenEraseOperations() {
        val screen = Screen(rows = 3, cols = 5)
        "ABCDE".forEach { screen.text(it) }
        screen.cr()
        screen.lf()
        "FGHIJ".forEach { screen.text(it) }
        screen.cup(1, 3)
        screen.el(0) {}
        assertEquals("AB", screen.rows(0, 5).first().trim())

        screen.cup(2, 3)
        screen.el(1) {}
        val row1 = screen.rows(0, 5).drop(1).first()
        assertEquals("   IJ", row1)

        screen.cup(2, 1)
        screen.el(2) {}
        val row1After = screen.rows(0, 5).drop(1).first()
        assertEquals("", row1After.trim())
    }

    @Test
    fun screenSgrAttributes() {
        val screen = Screen(rows = 2, cols = 10)
        screen.sgr(listOf(intArrayOf(1), intArrayOf(31), intArrayOf(42))) {}
        assertTrue(screen.bold())
        assertEquals(Color.Idx(1), screen.fgcolor())
        assertEquals(Color.Idx(2), screen.bgcolor())

        screen.text('X')
        val cell = screen.cell(0, 0)
        assertNotNull(cell)
        assertTrue(cell.bold())
        assertEquals(Color.Idx(1), cell.fgcolor())
        assertEquals(Color.Idx(2), cell.bgcolor())

        screen.sgr(listOf(intArrayOf(0))) {}
        assertFalse(screen.bold())
        assertEquals(Color.Default, screen.fgcolor())
        assertEquals(Color.Default, screen.bgcolor())
    }

    @Test
    fun screenAlternateScreen() {
        val screen = Screen(rows = 5, cols = 10)
        assertFalse(screen.alternateScreen())

        screen.decset(listOf(intArrayOf(1049))) {}
        assertTrue(screen.alternateScreen())

        screen.decrst(listOf(intArrayOf(1049))) {}
        assertFalse(screen.alternateScreen())
    }

    @Test
    fun screenContentsBetween() {
        val screen = Screen(rows = 3, cols = 5)
        "12345".forEach { screen.text(it) }
        screen.cr()
        screen.lf()
        "67890".forEach { screen.text(it) }
        assertEquals("234", screen.contentsBetween(0, 1, 0, 4))
    }

    @Test
    fun testU16ToU8() {
        assertEquals(0, u16ToU8(0))
        assertEquals(255, u16ToU8(255))
        kotlin.test.assertNull(u16ToU8(256))
        kotlin.test.assertNull(u16ToU8(-1))
    }
}
