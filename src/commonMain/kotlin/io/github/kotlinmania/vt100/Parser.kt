// port-lint: source parser.rs
package io.github.kotlinmania.vt100

/**
 * A parser for terminal output which produces an in-memory representation of
 * the terminal contents.
 */
public class Parser<CB : Callbacks> {
    private val vte: VteParser = VteParser()
    private val wrappedScreen: WrappedScreen<CB>

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
        wrappedScreen = WrappedScreen(rows, cols, scrollbackLen, callbacks)
    }

    /**
     * Processes the contents of the given byte array, and updates the
     * in-memory terminal state.
     */
    public fun process(bytes: ByteArray) {
        vte.advance(wrappedScreen, bytes)
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
    public fun screen(): Screen = wrappedScreen.screen

    /**
     * Returns the [Callbacks] state object passed into the constructor.
     */
    public fun callbacks(): CB = wrappedScreen.callbacks

    public companion object {
        /**
         * Creates a new terminal parser of the given size and with the given
         * amount of scrollback using [DefaultCallbacks].
         */
        public fun create(
            rows: Int = 24,
            cols: Int = 80,
            scrollbackLen: Int = 0,
        ): Parser<DefaultCallbacks> = Parser(rows, cols, scrollbackLen, DefaultCallbacks)
    }
}

private enum class VteState {
    Ground,
    Escape,
    EscapeIntermediate,
    CsiEntry,
    CsiParam,
    CsiIntermediate,
    CsiIgnore,
    DcsEntry,
    DcsParam,
    DcsIntermediate,
    DcsPassthrough,
    DcsIgnore,
    OscString,
    SosPmApcString,
}

internal class VteParser {
    private var state: VteState = VteState.Ground
    private val intermediates = mutableListOf<Byte>()
    private val params = mutableListOf<IntArray>()
    private val currentSubparams = mutableListOf<Int>()
    private var currentParam: Int = 0
    private var hasCurrentParam: Boolean = false
    private val oscBuffer = mutableListOf<Byte>()
    private var utf8CodePoint: Int = 0
    private var utf8BytesNeeded: Int = 0

    internal fun <CB : Callbacks> advance(perform: WrappedScreen<CB>, bytes: ByteArray) {
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i]
            val u = b.toInt() and 0xFF

            // Anywhere transitions
            if (u == 0x18 || u == 0x1A) {
                // CAN or SUB: cancels escape sequence, returns to ground and executes
                state = VteState.Ground
                perform.execute(b)
                i++
                continue
            }

            when (state) {
                VteState.Ground -> {
                    when {
                        u == 0x1B -> {
                            state = VteState.Escape
                            intermediates.clear()
                        }
                        u == 0x9B -> {
                            state = VteState.CsiEntry
                            intermediates.clear()
                            params.clear()
                            currentSubparams.clear()
                            currentParam = 0
                            hasCurrentParam = false
                        }
                        u == 0x9D -> {
                            state = VteState.OscString
                            oscBuffer.clear()
                        }
                        u < 0x20 || u == 0x7F -> {
                            perform.execute(b)
                        }
                        u < 0x80 -> {
                            perform.print(u.toChar())
                        }
                        else -> {
                            // UTF-8 multi-byte decoding
                            when {
                                u in 0xC2..0xDF -> {
                                    utf8CodePoint = u and 0x1F
                                    utf8BytesNeeded = 1
                                }
                                u in 0xE0..0xEF -> {
                                    utf8CodePoint = u and 0x0F
                                    utf8BytesNeeded = 2
                                }
                                u in 0xF0..0xF4 -> {
                                    utf8CodePoint = u and 0x07
                                    utf8BytesNeeded = 3
                                }
                                u in 0x80..0xBF && utf8BytesNeeded > 0 -> {
                                    utf8CodePoint = (utf8CodePoint shl 6) or (u and 0x3F)
                                    utf8BytesNeeded--
                                    if (utf8BytesNeeded == 0) {
                                        emitCodePoint(perform, utf8CodePoint)
                                    }
                                }
                                else -> {
                                    utf8BytesNeeded = 0
                                    perform.print('\uFFFD')
                                }
                            }
                        }
                    }
                }
                VteState.Escape -> {
                    when (u) {
                        0x1B -> {
                            intermediates.clear()
                        }
                        0x5B -> {
                            // Bracket opens CSI
                            state = VteState.CsiEntry
                            intermediates.clear()
                            params.clear()
                            currentSubparams.clear()
                            currentParam = 0
                            hasCurrentParam = false
                        }
                        0x5D -> {
                            // Bracket opens OSC
                            state = VteState.OscString
                            oscBuffer.clear()
                        }
                        0x50 -> {
                            // P opens DCS
                            state = VteState.DcsEntry
                            intermediates.clear()
                            params.clear()
                            currentSubparams.clear()
                            currentParam = 0
                            hasCurrentParam = false
                        }
                        0x58, 0x5E, 0x5F -> {
                            // Controls open SOS PM APC
                            state = VteState.SosPmApcString
                        }
                        in 0x20..0x2F -> {
                            intermediates.add(b)
                            state = VteState.EscapeIntermediate
                        }
                        in 0x30..0x4F, in 0x51..0x57, 0x59, 0x5A, 0x5C, in 0x60..0x7E -> {
                            perform.escDispatch(intermediates.toByteArray(), false, b)
                            state = VteState.Ground
                        }
                        0x7F -> { /* ignore */ }
                        in 0x00..0x1F -> {
                            perform.execute(b)
                        }
                        else -> {
                            state = VteState.Ground
                        }
                    }
                }
                VteState.EscapeIntermediate -> {
                    when (u) {
                        0x1B -> {
                            state = VteState.Escape
                            intermediates.clear()
                        }
                        in 0x20..0x2F -> {
                            intermediates.add(b)
                        }
                        in 0x30..0x7E -> {
                            perform.escDispatch(intermediates.toByteArray(), false, b)
                            state = VteState.Ground
                        }
                        0x7F -> { /* ignore */ }
                        in 0x00..0x1F -> {
                            perform.execute(b)
                        }
                        else -> {
                            state = VteState.Ground
                        }
                    }
                }
                VteState.CsiEntry -> {
                    when (u) {
                        0x1B -> {
                            state = VteState.Escape
                            intermediates.clear()
                        }
                        in 0x30..0x39 -> {
                            currentParam = u - 0x30
                            hasCurrentParam = true
                            state = VteState.CsiParam
                        }
                        0x3B -> {
                            currentSubparams.add(0)
                            params.add(currentSubparams.toIntArray())
                            currentSubparams.clear()
                            currentParam = 0
                            hasCurrentParam = false
                            state = VteState.CsiParam
                        }
                        0x3A -> {
                            currentSubparams.add(0)
                            currentParam = 0
                            hasCurrentParam = false
                            state = VteState.CsiParam
                        }
                        in 0x3C..0x3F -> {
                            intermediates.add(b)
                            state = VteState.CsiParam
                        }
                        in 0x20..0x2F -> {
                            intermediates.add(b)
                            state = VteState.CsiIntermediate
                        }
                        in 0x40..0x7E -> {
                            perform.csiDispatch(emptyList(), intermediates.toByteArray(), false, u.toChar())
                            state = VteState.Ground
                        }
                        0x7F -> { /* ignore */ }
                        in 0x00..0x1F -> {
                            perform.execute(b)
                        }
                        else -> {
                            state = VteState.Ground
                        }
                    }
                }
                VteState.CsiParam -> {
                    when (u) {
                        0x1B -> {
                            state = VteState.Escape
                            intermediates.clear()
                        }
                        in 0x30..0x39 -> {
                            if (!hasCurrentParam) {
                                currentParam = 0
                                hasCurrentParam = true
                            }
                            currentParam = currentParam * 10 + (u - 0x30)
                        }
                        0x3B -> {
                            currentSubparams.add(if (hasCurrentParam) currentParam else 0)
                            params.add(currentSubparams.toIntArray())
                            currentSubparams.clear()
                            currentParam = 0
                            hasCurrentParam = false
                        }
                        0x3A -> {
                            currentSubparams.add(if (hasCurrentParam) currentParam else 0)
                            currentParam = 0
                            hasCurrentParam = false
                        }
                        in 0x20..0x2F -> {
                            flushCsiParams()
                            intermediates.add(b)
                            state = VteState.CsiIntermediate
                        }
                        in 0x3C..0x3F -> {
                            state = VteState.CsiIgnore
                        }
                        in 0x40..0x7E -> {
                            flushCsiParams()
                            perform.csiDispatch(params, intermediates.toByteArray(), false, u.toChar())
                            state = VteState.Ground
                        }
                        0x7F -> { /* ignore */ }
                        in 0x00..0x1F -> {
                            perform.execute(b)
                        }
                        else -> {
                            state = VteState.Ground
                        }
                    }
                }
                VteState.CsiIntermediate -> {
                    when (u) {
                        0x1B -> {
                            state = VteState.Escape
                            intermediates.clear()
                        }
                        in 0x20..0x2F -> {
                            intermediates.add(b)
                        }
                        in 0x40..0x7E -> {
                            perform.csiDispatch(params, intermediates.toByteArray(), false, u.toChar())
                            state = VteState.Ground
                        }
                        in 0x30..0x3F -> {
                            state = VteState.CsiIgnore
                        }
                        0x7F -> { /* ignore */ }
                        in 0x00..0x1F -> {
                            perform.execute(b)
                        }
                        else -> {
                            state = VteState.Ground
                        }
                    }
                }
                VteState.CsiIgnore -> {
                    when (u) {
                        0x1B -> {
                            state = VteState.Escape
                            intermediates.clear()
                        }
                        in 0x40..0x7E -> {
                            state = VteState.Ground
                        }
                        0x7F -> { /* ignore */ }
                        in 0x00..0x1F -> {
                            perform.execute(b)
                        }
                        else -> {}
                    }
                }
                VteState.OscString -> {
                    when {
                        u == 0x07 -> {
                            dispatchOsc(perform, belTerminated = true)
                            state = VteState.Ground
                        }
                        u == 0x1B -> {
                            if (i + 1 < bytes.size && bytes[i + 1] == 0x5C.toByte()) {
                                i++
                                dispatchOsc(perform, belTerminated = false)
                                state = VteState.Ground
                            } else {
                                state = VteState.Escape
                                intermediates.clear()
                            }
                        }
                        u == 0x9C -> {
                            dispatchOsc(perform, belTerminated = false)
                            state = VteState.Ground
                        }
                        u < 0x20 -> {
                            // C0 controls ignored in OSC
                        }
                        else -> {
                            oscBuffer.add(b)
                        }
                    }
                }
                VteState.DcsEntry, VteState.DcsParam, VteState.DcsIntermediate,
                VteState.DcsPassthrough, VteState.DcsIgnore, VteState.SosPmApcString,
                -> {
                    when (u) {
                        0x07, 0x9C -> {
                            state = VteState.Ground
                        }
                        0x1B -> {
                            if (i + 1 < bytes.size && bytes[i + 1] == 0x5C.toByte()) {
                                i++
                                state = VteState.Ground
                            } else {
                                state = VteState.Escape
                                intermediates.clear()
                            }
                        }
                        else -> {}
                    }
                }
            }
            i++
        }
    }

    private fun flushCsiParams() {
        if (hasCurrentParam || currentSubparams.isNotEmpty()) {
            currentSubparams.add(if (hasCurrentParam) currentParam else 0)
            params.add(currentSubparams.toIntArray())
            currentSubparams.clear()
            currentParam = 0
            hasCurrentParam = false
        }
    }

    private fun <CB : Callbacks> dispatchOsc(perform: WrappedScreen<CB>, belTerminated: Boolean) {
        val oscBytes = oscBuffer.toByteArray()
        val result = mutableListOf<ByteArray>()
        var start = 0
        for (j in oscBytes.indices) {
            if (oscBytes[j] == ';'.code.toByte()) {
                result.add(oscBytes.copyOfRange(start, j))
                start = j + 1
            }
        }
        result.add(oscBytes.copyOfRange(start, oscBytes.size))
        perform.oscDispatch(result, belTerminated)
        oscBuffer.clear()
    }

    private fun <CB : Callbacks> emitCodePoint(perform: WrappedScreen<CB>, codePoint: Int) {
        if (codePoint <= 0xFFFF) {
            perform.print(codePoint.toChar())
        } else {
            val cp = codePoint - 0x10000
            val high = ((cp ushr 10) + 0xD800).toChar()
            val low = ((cp and 0x3FF) + 0xDC00).toChar()
            perform.print(high)
            perform.print(low)
        }
    }
}
