// port-lint: source screen.rs
package io.github.kotlinmania.vt100

/** The xterm mouse handling mode currently in use. */
public enum class MouseProtocolMode {
    /** Mouse handling is disabled. */
    None,

    /**
     * Mouse button events should be reported on button press. Also known as
     * X10 mouse mode.
     */
    Press,

    /**
     * Mouse button events should be reported on button press and release.
     * Also known as VT200 mouse mode.
     */
    PressRelease,

    // Highlight,
    /**
     * Mouse button events should be reported on button press and release, as
     * well as when the mouse moves between cells while a button is held
     * down.
     */
    ButtonMotion,

    /**
     * Mouse button events should be reported on button press and release,
     * and mouse motion events should be reported when the mouse moves
     * between cells regardless of whether a button is held down or not.
     */
    AnyMotion,
    // DecLocator,
}

/** The encoding to use for the enabled [MouseProtocolMode]. */
public enum class MouseProtocolEncoding {
    /** Default single-printable-byte encoding. */
    Default,

    /** UTF-8-based encoding. */
    Utf8,

    /** SGR-like encoding. */
    Sgr,
    // Urxvt,
}
