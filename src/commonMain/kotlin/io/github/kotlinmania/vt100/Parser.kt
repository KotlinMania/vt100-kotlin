// port-lint: source vt100/src/parser.rs
package io.github.kotlinmania.vt100

/**
 * A parser for terminal output which produces an in-memory representation of
 * the terminal contents.
 */
public class Parser<CB : Callbacks> {
    private val vteParser: VteParser = VteParser()
    private val screen: WrappedScreen<CB>

    /**
     * Creates a new terminal parser of the given size and with the given
     * amount of scrollback.
     */
    public constructor(
        rows: Int = 24,
        cols: Int = 80,
        scrollbackLen: Int = 0,
        callbacks: CB,
    ) {
        screen = WrappedScreen.newWithCallbacks(rows, cols, scrollbackLen, callbacks)
    }

    /**
     * Processes the contents of the given byte array, and updates the
     * in-memory terminal state.
     */
    public fun process(bytes: ByteArray) {
        vteParser.advance(screen, bytes)
    }

    /**
     * Processes the contents of the given string, encoded as UTF-8, and updates
     * the in-memory terminal state.
     */
    public fun process(string: String) {
        process(string.encodeToByteArray())
    }

    /**
     * Returns a reference to the [Screen] object containing the terminal state.
     */
    public fun screen(): Screen = screen.screen

    /**
     * Returns a mutable reference to the [Screen] object containing the terminal state.
     */
    public fun screenMut(): Screen = screen.screen

    /**
     * Returns the [Callbacks] state object passed into the constructor.
     */
    public fun callbacks(): CB = screen.callbacks

    /**
     * Returns a mutable reference to the [Callbacks] state object passed into the constructor.
     */
    public fun callbacksMut(): CB = screen.callbacks

    /**
     * Writes the given buffer to the parser, advancing terminal state.
     */
    public fun write(buf: ByteArray): Int {
        process(buf)
        return buf.size
    }

    /**
     * Flushes the parser.
     */
    public fun flush() {
    }

    public companion object {
        /**
         * Creates a new terminal parser of the given size and with the given
         * amount of scrollback using [DefaultCallbacks].
         */
        public fun new(
            rows: Int = 24,
            cols: Int = 80,
            scrollbackLen: Int = 0,
        ): Parser<DefaultCallbacks> = newWithCallbacks(rows, cols, scrollbackLen, DefaultCallbacks)

        /**
         * Creates a new terminal parser of the given size and with the given
         * amount of scrollback using [DefaultCallbacks].
         */
        public fun create(
            rows: Int = 24,
            cols: Int = 80,
            scrollbackLen: Int = 0,
        ): Parser<DefaultCallbacks> = new(rows, cols, scrollbackLen)

        /**
         * Creates a new terminal parser of the given size and with the given
         * amount of scrollback. Terminal events will be reported via method
         * calls on the provided [Callbacks] implementation.
         */
        public fun <CB : Callbacks> newWithCallbacks(
            rows: Int,
            cols: Int,
            scrollbackLen: Int,
            callbacks: CB,
        ): Parser<CB> = Parser(rows, cols, scrollbackLen, callbacks)

        /**
         * Returns a parser with dimensions 80x24 and no scrollback.
         */
        public fun default(): Parser<DefaultCallbacks> = new(24, 80, 0)
    }
}
