// port-lint: source attrs.rs
package io.github.kotlinmania.vt100

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AttrsTest {
    @Test
    fun togglesTextModes() {
        val attrs = Attrs()

        attrs.setBold()
        assertTrue(attrs.bold())
        assertFalse(attrs.dim())

        attrs.setDim()
        assertFalse(attrs.bold())
        assertTrue(attrs.dim())

        attrs.setNormalIntensity()
        assertFalse(attrs.bold())
        assertFalse(attrs.dim())

        attrs.setItalic(true)
        attrs.setUnderline(true)
        attrs.setInverse(true)
        assertTrue(attrs.italic())
        assertTrue(attrs.underline())
        assertTrue(attrs.inverse())

        attrs.setItalic(false)
        attrs.setUnderline(false)
        attrs.setInverse(false)
        assertFalse(attrs.italic())
        assertFalse(attrs.underline())
        assertFalse(attrs.inverse())
    }

    @Test
    fun writesResetWhenReturningToDefaultAttributes() {
        val previous = Attrs()
        previous.setBold()

        val bytes = mutableListOf<Byte>()
        Attrs().writeEscapeCodeDiff(bytes, previous)

        assertEquals("\u001b[m", bytes.toByteArray().decodeToString())
    }

    @Test
    fun writesChangedColorAndModeParametersInOrder() {
        val attrs =
            Attrs(
                fgColor = Color.Idx(9),
                bgColor = Color.Rgb(1, 2, 3),
            )
        attrs.setBold()
        attrs.setItalic(true)
        attrs.setUnderline(true)
        attrs.setInverse(true)

        val bytes = mutableListOf<Byte>()
        attrs.writeEscapeCodeDiff(bytes, Attrs())

        assertEquals("\u001b[91;48;2;1;2;3;1;3;4;7m", bytes.toByteArray().decodeToString())
    }
}
