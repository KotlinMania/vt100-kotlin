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
    public fun audibleBell(screen: Screen) {
        screen.hashCode()
    }

    /**
     * A visual bell (ESC g) was received.
     */
    public fun visualBell(screen: Screen) {
        screen.hashCode()
    }

    /**
     * A terminal resize request (CSI 8 ; <rows> ; <cols> t) was received.
     */
    public fun resize(screen: Screen, size: Pair<Int, Int>) {
        screen.hashCode()
        size.hashCode()
    }

    /**
     * A set window icon name request (OSC 0 ; <iconName> BEL, OSC 1 ; <iconName> BEL, etc.) was received.
     */
    public fun setWindowIconName(screen: Screen, iconName: ByteArray) {
        screen.hashCode()
        iconName.hashCode()
    }

    /**
     * A set window title request (OSC 0 ; <title> BEL, OSC 2 ; <title> BEL, etc.) was received.
     */
    public fun setWindowTitle(screen: Screen, title: ByteArray) {
        screen.hashCode()
        title.hashCode()
    }

    /**
     * A copy to clipboard request (OSC 52 ; <type> ; <data> BEL) was received.
     */
    public fun copyToClipboard(screen: Screen, type: ByteArray, data: ByteArray) {
        screen.hashCode()
        type.hashCode()
        data.hashCode()
    }

    /**
     * A paste from clipboard request (OSC 52 ; <type> ; ? BEL) was received.
     */
    public fun pasteFromClipboard(screen: Screen, type: ByteArray) {
        screen.hashCode()
        type.hashCode()
    }

    /**
     * A printable character was received that was unhandled.
     */
    public fun unhandledChar(screen: Screen, c: Char) {
        screen.hashCode()
        c.hashCode()
    }

    /**
     * A C0 or C1 control code was received that was unhandled.
     */
    public fun unhandledControl(screen: Screen, b: Byte) {
        screen.hashCode()
        b.hashCode()
    }

    /**
     * An escape sequence was received that was unhandled.
     */
    public fun unhandledEscape(screen: Screen, intermediate1: Byte?, intermediate2: Byte?, b: Byte) {
        screen.hashCode()
        intermediate1?.hashCode()
        intermediate2?.hashCode()
        b.hashCode()
    }

    /**
     * A CSI sequence was received that was unhandled.
     */
    public fun unhandledCsi(
        screen: Screen,
        intermediate1: Byte?,
        intermediate2: Byte?,
        params: List<IntArray>,
        c: Char,
    ) {
        screen.hashCode()
        intermediate1?.hashCode()
        intermediate2?.hashCode()
        params.hashCode()
        c.hashCode()
    }

    /**
     * An OSC sequence was received that was unhandled.
     */
    public fun unhandledOsc(screen: Screen, params: List<ByteArray>) {
        screen.hashCode()
        params.hashCode()
    }
}

/**
 * Default implementation of [Callbacks] which ignores all events.
 */
public object DefaultCallbacks : Callbacks
