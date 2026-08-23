package io.github.kotlinmania.vt100

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ParserTest {
    @Test
    fun synopsisDocTest() {
        val parser = Parser.create(24, 80, 0)

        val screen = parser.screen().copy()
        parser.process("this text is \u001b[31mRED\u001b[m")
        assertEquals(
            Color.Idx(1),
            parser.screen().cell(0, 13)?.fgcolor(),
        )

        val screen2 = parser.screen().copy()
        parser.process("\u001b[3D\u001b[32mGREEN")
        assertEquals(
            "\u001b[?25h\u001b[m\u001b[H\u001b[Jthis text is \u001b[32mGREEN",
            parser.screen().contentsFormatted().decodeToString(),
        )
        assertEquals(
            "\u001b[1;14H\u001b[32mGREEN",
            parser.screen().contentsDiff(screen2).decodeToString(),
        )
    }

    @Test
    fun parserHandlesCsiMoveAndColors() {
        val parser = Parser.create(10, 20, 0)
        parser.process("Hello\r\n\u001b[34mWorld\u001b[0m")

        assertEquals("Hello", parser.screen().rows(0, 5).first())
        val worldRow = parser.screen().rows(0, 5).drop(1).first()
        assertEquals("World", worldRow)

        val cell = parser.screen().cell(1, 0)
        assertNotNull(cell)
        assertEquals(Color.Idx(4), cell.fgcolor())
    }

    @Test
    fun parserHandlesOscWindowTitleCallbacks() {
        var recordedTitle: String? = null
        val callbacks = object : Callbacks {
            override fun setWindowTitle(screen: Screen, title: ByteArray) {
                recordedTitle = title.decodeToString()
            }
        }
        val parser = Parser(24, 80, 0, callbacks)
        parser.process("\u001b]2;My Terminal Window\u0007")

        assertEquals("My Terminal Window", recordedTitle)
    }

    @Test
    fun parserHandlesBellCallback() {
        var bellCount = 0
        val callbacks = object : Callbacks {
            override fun audibleBell(screen: Screen) {
                bellCount++
            }
        }
        val parser = Parser(24, 80, 0, callbacks)
        parser.process("Alert\u0007\u0007")

        assertEquals(2, bellCount)
    }
}
