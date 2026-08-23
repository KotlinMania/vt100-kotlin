// port-lint: source perform.rs
package io.github.kotlinmania.vt100

import io.github.kotlinmania.vt100.grid.Size

private fun isBase64(b: Byte): Boolean {
    val c = b.toInt() and 0xFF
    return (c in 0x41..0x5A) || (c in 0x61..0x7A) || (c in 0x30..0x39) || c == 0x2B || c == 0x2F || c == 0x3D
}

private fun isClipboardSelector(b: Byte): Boolean {
    val c = b.toInt() and 0xFF
    return c == 0x63 || c == 0x70 || c == 0x71 || c == 0x73 || (c in 0x30..0x37)
}

internal class WrappedScreen<CB : Callbacks>(
    internal val screen: Screen,
    internal val callbacks: CB,
) {
    internal constructor(rows: Int, cols: Int, scrollbackLen: Int, callbacks: CB) :
        this(Screen(Size(rows, cols), scrollbackLen), callbacks)

    internal fun print(c: Char) {
        if (c == '\uFFFD' || c in '\u0080'..'\u009F') {
            callbacks.unhandledChar(screen, c)
        } else {
            screen.text(c)
        }
    }

    internal fun execute(b: Byte) {
        val code = b.toInt() and 0xFF
        when (code) {
            7 -> callbacks.audibleBell(screen)
            8 -> screen.bs()
            9 -> screen.tab()
            10 -> screen.lf()
            11 -> screen.vt()
            12 -> screen.ff()
            13 -> screen.cr()
            14, 15 -> {
                // shift in/out alternate character sets ignored
            }
            else -> callbacks.unhandledControl(screen, b)
        }
    }

    internal fun escDispatch(intermediates: ByteArray, ignore: Boolean, b: Byte) {
        if (intermediates.isNotEmpty()) {
            val i1 = intermediates[0]
            val i2 = if (intermediates.size > 1) intermediates[1] else null
            callbacks.unhandledEscape(screen, i1, i2, b)
        } else {
            when (b.toInt().toChar()) {
                '7' -> screen.decsc()
                '8' -> screen.decrc()
                '=' -> screen.deckpam()
                '>' -> screen.deckpnm()
                'M' -> screen.ri()
                'c' -> screen.ris()
                'g' -> callbacks.visualBell(screen)
                else -> callbacks.unhandledEscape(screen, null, null, b)
            }
        }
    }

    internal fun csiDispatch(
        params: List<IntArray>,
        intermediates: ByteArray,
        ignore: Boolean,
        c: Char,
    ) {
        val unhandled: (Screen) -> Unit = { s ->
            val i1 = intermediates.firstOrNull()
            val i2 = if (intermediates.size > 1) intermediates[1] else null
            callbacks.unhandledCsi(s, i1, i2, params, c)
        }

        val firstInter = intermediates.firstOrNull()?.toInt()?.toChar()
        when (firstInter) {
            null ->
                when (c) {
                    '@' -> screen.ich(canonicalizeParams1(params, 1))
                    'A' -> screen.cuu(canonicalizeParams1(params, 1))
                    'B' -> screen.cud(canonicalizeParams1(params, 1))
                    'C' -> screen.cuf(canonicalizeParams1(params, 1))
                    'D' -> screen.cub(canonicalizeParams1(params, 1))
                    'E' -> screen.cnl(canonicalizeParams1(params, 1))
                    'F' -> screen.cpl(canonicalizeParams1(params, 1))
                    'G' -> screen.cha(canonicalizeParams1(params, 1))
                    'H' -> screen.cup(canonicalizeParams2(params, 1, 1))
                    'J' -> screen.ed(canonicalizeParams1(params, 0), unhandled)
                    'K' -> screen.el(canonicalizeParams1(params, 0), unhandled)
                    'L' -> screen.il(canonicalizeParams1(params, 1))
                    'M' -> screen.dl(canonicalizeParams1(params, 1))
                    'P' -> screen.dch(canonicalizeParams1(params, 1))
                    'S' -> screen.su(canonicalizeParams1(params, 1))
                    'T' -> screen.sd(canonicalizeParams1(params, 1))
                    'X' -> screen.ech(canonicalizeParams1(params, 1))
                    'd' -> screen.vpa(canonicalizeParams1(params, 1))
                    'm' -> screen.sgr(params, unhandled)
                    'r' -> {
                        val s = screen.grid().size()
                        screen.decstbm(canonicalizeParamsDecstbm(params, s))
                    }
                    't' -> {
                        val op = params.firstOrNull()?.firstOrNull()
                        if (op == 8) {
                            val (screenRows, screenCols) = screen.size()
                            val rows = params.getOrNull(1)?.firstOrNull() ?: screenRows
                            val cols = params.getOrNull(2)?.firstOrNull() ?: screenCols
                            callbacks.resize(screen, Pair(rows, cols))
                        } else {
                            unhandled(screen)
                        }
                    }
                    else -> unhandled(screen)
                }
            '?' ->
                when (c) {
                    'J' -> screen.decsed(canonicalizeParams1(params, 0), unhandled)
                    'K' -> screen.decsel(canonicalizeParams1(params, 0), unhandled)
                    'h' -> screen.decset(params, unhandled)
                    'l' -> screen.decrst(params, unhandled)
                    else -> unhandled(screen)
                }
            else -> unhandled(screen)
        }
    }

    internal fun oscDispatch(params: List<ByteArray>, belTerminated: Boolean) {
        val p0 = params.firstOrNull()?.decodeToString()
        when {
            params.size == 2 && p0 == "0" -> {
                callbacks.setWindowIconName(screen, params[1])
                callbacks.setWindowTitle(screen, params[1])
            }
            params.size == 2 && p0 == "1" -> {
                callbacks.setWindowIconName(screen, params[1])
            }
            params.size == 2 && p0 == "2" -> {
                callbacks.setWindowTitle(screen, params[1])
            }
            params.size == 3 && p0 == "52" -> {
                val ty = params[1]
                val data = params[2]
                val validTy = ty.all { isClipboardSelector(it) }
                if (validTy && data.size == 1 && data[0] == '?'.code.toByte()) {
                    callbacks.pasteFromClipboard(screen, ty)
                } else if (validTy && data.all { isBase64(it) }) {
                    callbacks.copyToClipboard(screen, ty, data)
                } else {
                    callbacks.unhandledOsc(screen, params)
                }
            }
            else -> callbacks.unhandledOsc(screen, params)
        }
    }
}

internal fun canonicalizeParams1(params: List<IntArray>, default: Int): Int {
    val first = params.firstOrNull()?.firstOrNull() ?: 0
    return if (first == 0) default else first
}

internal fun canonicalizeParams2(params: List<IntArray>, default1: Int, default2: Int): Pair<Int, Int> {
    val p1 = params.getOrNull(0)?.firstOrNull() ?: 0
    val p2 = params.getOrNull(1)?.firstOrNull() ?: 0
    val first = if (p1 == 0) default1 else p1
    val second = if (p2 == 0) default2 else p2
    return Pair(first, second)
}

internal fun canonicalizeParamsDecstbm(params: List<IntArray>, size: Size): Pair<Int, Int> {
    val p1 = params.getOrNull(0)?.firstOrNull() ?: 0
    val p2 = params.getOrNull(1)?.firstOrNull() ?: 0
    val top = if (p1 == 0) 1 else p1
    val bottom = if (p2 == 0) size.rows else p2
    return Pair(top, bottom)
}
