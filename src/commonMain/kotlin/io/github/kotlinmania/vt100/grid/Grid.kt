// port-lint: source grid.rs
package io.github.kotlinmania.vt100.grid

/**
 * The dimensions of a [Grid], measured in rows and columns.
 *
 * `cols` is constrained to a `UShort` upstream so the maximum value is 65535;
 * the Kotlin port keeps the value in an `Int` for ergonomic arithmetic and
 * relies on the upstream invariant that callers respect that ceiling.
 */
public data class Size(
    public val rows: Int = 0,
    public val cols: Int = 0,
)

/**
 * A position within a [Grid], measured in rows and columns from the top-left
 * cell.
 */
public data class Pos(
    public val row: Int = 0,
    public val col: Int = 0,
)
