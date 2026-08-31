// port-lint: source vt100/src/lib.rs
/**
 * This library parses a terminal byte stream and provides an in-memory
 * representation of the rendered contents.
 *
 * ## Overview
 *
 * This is essentially the terminal parser component of a graphical terminal
 * emulator pulled out into a separate multiplatform library. Although you can
 * use this to build a graphical terminal emulator, it also contains
 * functionality necessary for implementing terminal applications that want to
 * run other terminal applications - programs like `screen` or `tmux` for example.
 *
 * ## Synopsis
 *
 * ```kotlin
 * val parser = Parser(24, 80, 0)
 *
 * val screen = parser.screen().copy()
 * parser.process("this text is \u001b[31mRED\u001b[m".encodeToByteArray())
 * check(parser.screen().cell(0, 13)?.fgcolor() == Color.Idx(1))
 *
 * val previousScreen = parser.screen().copy()
 * parser.process("\u001b[3D\u001b[32mGREEN".encodeToByteArray())
 * check(
 *     parser.screen().contentsFormatted().contentEquals(
 *         "\u001b[?25h\u001b[m\u001b[H\u001b[Jthis text is \u001b[32mGREEN".encodeToByteArray(),
 *     ),
 * )
 * check(
 *     parser.screen().contentsDiff(previousScreen).contentEquals(
 *         "\u001b[1;14H\u001b[32mGREEN".encodeToByteArray(),
 *     ),
 * )
 * ```
 */
package io.github.kotlinmania.vt100

/**
 * Marker object and entrypoint for the vt100 library.
 */
public object Lib {
    /**
     * Default number of terminal rows.
     */
    public const val DEFAULT_ROWS: Int = 24

    /**
     * Default number of terminal columns.
     */
    public const val DEFAULT_COLS: Int = 80

    /**
     * Default number of scrollback lines.
     */
    public const val DEFAULT_SCROLLBACK_LEN: Int = 0
}
