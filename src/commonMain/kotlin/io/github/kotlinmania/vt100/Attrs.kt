// port-lint: source vt100/src/attrs.rs
package io.github.kotlinmania.vt100

/** Represents a foreground or background color for cells. */
public sealed interface Color {
    /** The default terminal color. */
    public data object Default : Color

    /** An indexed terminal color. */
    public data class Idx(
        public val value: Int,
    ) : Color {
        init {
            require(value in 0..255) { "Indexed terminal color must fit in one byte: $value" }
        }
    }

    /** An RGB terminal color. The parameters are (red, green, blue). */
    public data class Rgb(
        public val red: Int,
        public val green: Int,
        public val blue: Int,
    ) : Color {
        init {
            require(red in 0..255) { "Red terminal color component must fit in one byte: $red" }
            require(green in 0..255) { "Green terminal color component must fit in one byte: $green" }
            require(blue in 0..255) { "Blue terminal color component must fit in one byte: $blue" }
        }
    }
}

private const val TEXT_MODE_INTENSITY: Int = 0b0000_0011
private const val TEXT_MODE_BOLD: Int = 0b0000_0001
private const val TEXT_MODE_DIM: Int = 0b0000_0010
private const val TEXT_MODE_ITALIC: Int = 0b0000_0100
private const val TEXT_MODE_UNDERLINE: Int = 0b0000_1000
private const val TEXT_MODE_INVERSE: Int = 0b0001_0000

/** Represents character drawing attributes such as colors and text modes. */
internal data class Attrs(
    /** The foreground color. */
    internal var fgColor: Color = Color.Default,
    /** The background color. */
    internal var bgColor: Color = Color.Default,
    /** The bitmask of active text modes. */
    internal var mode: Int = 0,
) {
    /** Returns whether the bold text attribute is set. */
    internal fun bold(): Boolean = mode and TEXT_MODE_BOLD != 0

    /** Returns whether the dim text attribute is set. */
    internal fun dim(): Boolean = mode and TEXT_MODE_DIM != 0

    private fun intensity(): Int = mode and TEXT_MODE_INTENSITY

    /** Sets the bold text attribute. */
    internal fun setBold() {
        mode = mode and TEXT_MODE_INTENSITY.inv()
        mode = mode or TEXT_MODE_BOLD
    }

    /** Sets the dim text attribute. */
    internal fun setDim() {
        mode = mode and TEXT_MODE_INTENSITY.inv()
        mode = mode or TEXT_MODE_DIM
    }

    /** Clears bold and dim intensity attributes. */
    internal fun setNormalIntensity() {
        mode = mode and TEXT_MODE_INTENSITY.inv()
    }

    /** Returns whether the italic text attribute is set. */
    internal fun italic(): Boolean = mode and TEXT_MODE_ITALIC != 0

    /** Sets or clears the italic text attribute. */
    internal fun setItalic(italic: Boolean) {
        mode =
            if (italic) {
                mode or TEXT_MODE_ITALIC
            } else {
                mode and TEXT_MODE_ITALIC.inv()
            }
    }

    /** Returns whether the underline text attribute is set. */
    internal fun underline(): Boolean = mode and TEXT_MODE_UNDERLINE != 0

    /** Sets or clears the underline text attribute. */
    internal fun setUnderline(underline: Boolean) {
        mode =
            if (underline) {
                mode or TEXT_MODE_UNDERLINE
            } else {
                mode and TEXT_MODE_UNDERLINE.inv()
            }
    }

    /** Returns whether the inverse text attribute is set. */
    internal fun inverse(): Boolean = mode and TEXT_MODE_INVERSE != 0

    /** Sets or clears the inverse text attribute. */
    internal fun setInverse(inverse: Boolean) {
        mode =
            if (inverse) {
                mode or TEXT_MODE_INVERSE
            } else {
                mode and TEXT_MODE_INVERSE.inv()
            }
    }

    internal fun writeEscapeCodeDiff(contents: MutableList<Byte>, other: Attrs) {
        if (this != other && this == Attrs()) {
            contents.addAscii("\u001b[m")
            return
        }

        val params = mutableListOf<Int>()
        if (fgColor != other.fgColor) {
            params.addColor(fgColor, foreground = true)
        }
        if (bgColor != other.bgColor) {
            params.addColor(bgColor, foreground = false)
        }
        if (intensity() != other.intensity()) {
            params.add(
                when (val value = intensity()) {
                    0 -> 22
                    TEXT_MODE_BOLD -> 1
                    TEXT_MODE_DIM -> 2
                    else -> throw IllegalStateException("Unexpected terminal text intensity bits: $value")
                },
            )
        }
        if (italic() != other.italic()) {
            params.add(if (italic()) 3 else 23)
        }
        if (underline() != other.underline()) {
            params.add(if (underline()) 4 else 24)
        }
        if (inverse() != other.inverse()) {
            params.add(if (inverse()) 7 else 27)
        }

        if (params.isNotEmpty()) {
            contents.addAscii("\u001b[")
            contents.addAscii(params.joinToString(";"))
            contents.add('m'.code.toByte())
        }
    }
}

private fun MutableList<Int>.addColor(color: Color, foreground: Boolean) {
    val default = if (foreground) 39 else 49
    val lowBase = if (foreground) 30 else 40
    val highBase = if (foreground) 90 else 100
    val extended = if (foreground) 38 else 48

    when (color) {
        Color.Default -> add(default)
        is Color.Idx -> {
            val value = color.value
            when {
                value < 8 -> add(value + lowBase)
                value < 16 -> add(value + highBase - 8)
                else -> {
                    add(extended)
                    add(5)
                    add(value)
                }
            }
        }
        is Color.Rgb -> {
            add(extended)
            add(2)
            add(color.red)
            add(color.green)
            add(color.blue)
        }
    }
}

private fun MutableList<Byte>.addAscii(value: String) {
    value.encodeToByteArray().forEach { byte -> add(byte) }
}
