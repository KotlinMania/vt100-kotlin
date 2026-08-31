// port-lint: tests callbacks.rs
package io.github.kotlinmania.vt100

import io.github.kotlinmania.vt100.grid.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CallbacksTest {
    class RecordingCallbacks : Callbacks {
        var audibleBellCalled = false
        var visualBellCalled = false
        var resizedTo: Pair<Int, Int>? = null
        var windowTitle: String? = null
        var iconName: String? = null
        var clipboardData: String? = null
        var pastedType: String? = null
        var unhandledChar: Char? = null
        var unhandledControl: Byte? = null

        override fun audibleBell(screen: Screen) {
            audibleBellCalled = true
        }

        override fun visualBell(screen: Screen) {
            visualBellCalled = true
        }

        override fun resize(screen: Screen, size: Pair<Int, Int>) {
            resizedTo = size
        }

        override fun setWindowTitle(screen: Screen, title: ByteArray) {
            windowTitle = title.decodeToString()
        }

        override fun setWindowIconName(screen: Screen, iconName: ByteArray) {
            this.iconName = iconName.decodeToString()
        }

        override fun copyToClipboard(screen: Screen, type: ByteArray, data: ByteArray) {
            clipboardData = data.decodeToString()
        }

        override fun pasteFromClipboard(screen: Screen, type: ByteArray) {
            pastedType = type.decodeToString()
        }

        override fun unhandledChar(screen: Screen, c: Char) {
            unhandledChar = c
        }

        override fun unhandledControl(screen: Screen, b: Byte) {
            unhandledControl = b
        }
    }

    @Test
    fun testDefaultCallbacks() {
        val screen = Screen(Size(24, 80), 100)
        DefaultCallbacks.audibleBell(screen)
        DefaultCallbacks.visualBell(screen)
        DefaultCallbacks.resize(screen, Pair(30, 100))
        DefaultCallbacks.setWindowIconName(screen, "icon".encodeToByteArray())
        DefaultCallbacks.setWindowTitle(screen, "title".encodeToByteArray())
        DefaultCallbacks.copyToClipboard(screen, "c".encodeToByteArray(), "SGVsbG8=".encodeToByteArray())
        DefaultCallbacks.pasteFromClipboard(screen, "c".encodeToByteArray())
        DefaultCallbacks.unhandledChar(screen, '?')
        DefaultCallbacks.unhandledControl(screen, 0x03.toByte())
        DefaultCallbacks.unhandledEscape(screen, null, null, 'Z'.code.toByte())
        DefaultCallbacks.unhandledCsi(screen, null, null, listOf(intArrayOf(1)), 'Z')
        DefaultCallbacks.unhandledOsc(screen, listOf("99".encodeToByteArray()))
    }

    @Test
    fun testCustomCallbacks() {
        val screen = Screen(Size(24, 80), 100)
        val cb = RecordingCallbacks()

        cb.audibleBell(screen)
        assertTrue(cb.audibleBellCalled)

        cb.visualBell(screen)
        assertTrue(cb.visualBellCalled)

        cb.resize(screen, Pair(40, 120))
        assertEquals(Pair(40, 120), cb.resizedTo)

        cb.setWindowTitle(screen, "Terminal".encodeToByteArray())
        assertEquals("Terminal", cb.windowTitle)

        cb.setWindowIconName(screen, "TermIcon".encodeToByteArray())
        assertEquals("TermIcon", cb.iconName)

        cb.copyToClipboard(screen, "c".encodeToByteArray(), "SGVsbG8=".encodeToByteArray())
        assertEquals("SGVsbG8=", cb.clipboardData)

        cb.pasteFromClipboard(screen, "c".encodeToByteArray())
        assertEquals("c", cb.pastedType)

        cb.unhandledChar(screen, '\uFFFD')
        assertEquals('\uFFFD', cb.unhandledChar)

        cb.unhandledControl(screen, 0x1B.toByte())
        assertEquals(0x1B.toByte(), cb.unhandledControl)
    }
}
