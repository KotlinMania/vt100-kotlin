// port-lint: tests vt100/src/perform.rs
package io.github.kotlinmania.vt100

import io.github.kotlinmania.vt100.grid.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PerformTest {
    @Test
    fun testCanonicalizeParams() {
        assertEquals(5, canonicalizeParams1(listOf(intArrayOf(5)), 1))
        assertEquals(1, canonicalizeParams1(listOf(intArrayOf(0)), 1))
        assertEquals(1, canonicalizeParams1(emptyList(), 1))

        assertEquals(Pair(3, 4), canonicalizeParams2(listOf(intArrayOf(3), intArrayOf(4)), 1, 1))
        assertEquals(Pair(1, 1), canonicalizeParams2(listOf(intArrayOf(0), intArrayOf(0)), 1, 1))

        assertEquals(Pair(1, 24), canonicalizeParamsDecstbm(listOf(intArrayOf(0), intArrayOf(0)), Size(24, 80)))
        assertEquals(Pair(5, 20), canonicalizeParamsDecstbm(listOf(intArrayOf(5), intArrayOf(20)), Size(24, 80)))
    }

    @Test
    fun testWrappedScreenPrintAndExecute() {
        val ws = WrappedScreen.new(24, 80, 100)
        ws.print('A')
        ws.print('B')
        ws.execute(10.toByte()) // LF
        ws.execute(13.toByte()) // CR
        ws.print('C')
        assertEquals("AB", ws.screen.contentsBetween(0, 0, 0, 2))
    }

    @Test
    fun testWrappedScreenEscAndCsi() {
        val ws = WrappedScreen.new(24, 80, 100)
        ws.escDispatch(byteArrayOf(), false, '7'.code.toByte()) // save cursor
        ws.print('X')
        ws.escDispatch(byteArrayOf(), false, '8'.code.toByte()) // restore cursor
        ws.csiDispatch(listOf(intArrayOf(2)), byteArrayOf(), false, 'J') // clear screen
        assertTrue(ws.screen.size() == Pair(24, 80))
    }

    @Test
    fun testWrappedScreenOsc() {
        val cb = CallbacksTest.RecordingCallbacks()
        val ws = WrappedScreen.newWithCallbacks(24, 80, 100, cb)
        ws.oscDispatch(listOf("0".encodeToByteArray(), "My Window".encodeToByteArray()), true)
        assertEquals("My Window", cb.windowTitle)
        assertEquals("My Window", cb.iconName)
    }
}
