// port-lint: source callbacks.rs
package io.github.kotlinmania.vt100

/**
 * A set of callbacks for terminal events.
 *
 * All methods have default empty implementations.
 */
public interface Callbacks {
    /**
     * An audible bell (BEL) was received.
     */
    public fun audibleBell(screen: Screen) {}

    /**
     * A visual bell (ESC g) was received.
     */
    public fun visualBell(screen: Screen) {}

    /**
     * A terminal resize request (CSI 8 ; <rows> ; <cols> t) was received.
     */
    public fun resize(screen: Screen, size: Pair<Int, Int>) {}

    /**
     * A set window icon name request (OSC 0 ; <iconName> BEL, OSC 1 ; <iconName> BEL, etc.) was received.
     */
    public fun setWindowIconName(screen: Screen, iconName: ByteArray) {}

    /**
     * A set window title request (OSC 0 ; <title> BEL, OSC 2 ; <title> BEL, etc.) was received.
     */
    public fun setWindowTitle(screen: Screen, title: ByteArray) {}

    /**
     * A copy to clipboard request (OSC 52 ; <type> ; <data> BEL) was received.
     */
    public fun copyToClipboard(screen: Screen, type: ByteArray, data: ByteArray) {}

    /**
     * A paste from clipboard request (OSC 52 ; <type> ; ? BEL) was received.
     */
    public fun pasteFromClipboard(screen: Screen, type: ByteArray) {}

    /**
     * A printable character was received that was unhandled.
     */
    public fun unhandledChar(screen: Screen, c: Char) {}

    /**
     * A C0 or C1 control code was received that was unhandled.
     */
    public fun unhandledControl(screen: Screen, b: Byte) {}

    /**
     * An escape sequence was received that was unhandled.
     */
    public fun unhandledEscape(screen: Screen, intermediate1: Byte?, intermediate2: Byte?, b: Byte) {}

    /**
     * A CSI sequence was received that was unhandled.
     */
    public fun unhandledCsi(
        screen: Screen,
        intermediate1: Byte?,
        intermediate2: Byte?,
        params: List<IntArray>,
        c: Char,
    ) {}

    /**
     * An OSC sequence was received that was unhandled.
     */
    public fun unhandledOsc(screen: Screen, params: List<ByteArray>) {}
}

/**
 * Default implementation of [Callbacks] which ignores all events.
 */
public object DefaultCallbacks : Callbacks
