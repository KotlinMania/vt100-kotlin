// port-lint: source lib.rs
/**
 * This library parses a terminal byte stream and provides an in-memory
 * representation of the rendered contents.
 *
 * Overview:
 * This is essentially the terminal parser component of a graphical terminal
 * emulator pulled out into a separate multiplatform library. Although you can use this
 * to build a graphical terminal emulator, it also contains functionality
 * necessary for implementing terminal applications that want to run other
 * terminal applications - programs like screen or tmux for example.
 */
package io.github.kotlinmania.vt100
