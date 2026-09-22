package io.github.splashieee7.ios2droidkeyboard.logic

import io.github.splashieee7.ios2droidkeyboard.layout.LayerId
import io.github.splashieee7.ios2droidkeyboard.layout.ShiftState

/**
 * The iOS typing behaviours, as pure functions over the text before the cursor.
 *
 * Deliberately free of Android types so every rule can be unit tested on the JVM. Nothing
 * here logs, stores or inspects text beyond the few characters each rule needs.
 */
object TypingLogic {

    private val SENTENCE_ENDERS = setOf('.', '!', '?')

    /** Typing one of these on the numbers layer drops back to letters, as iOS does. */
    private val RETURNS_TO_LETTERS = setOf('.', ',', '?', '!', '\'', '"')

    /**
     * iOS capitalises at the start of a field, after a newline, and after a sentence
     * ender followed by a space.
     */
    fun shouldAutoCapitalize(textBefore: String): Boolean {
        if (textBefore.isBlank()) return true
        if (textBefore.last() == '\n') return true
        if (textBefore.length >= 2 &&
            textBefore.last() == ' ' &&
            textBefore[textBefore.length - 2] in SENTENCE_ENDERS
        ) {
            return true
        }
        return false
    }

    /**
     * True when a space should become ". " instead - that is, the text already ends in a
     * single space directly after a word. Guards against turning "a  " into "a . ".
     */
    fun shouldInsertPeriodForDoubleSpace(textBefore: String): Boolean {
        if (textBefore.length < 2) return false
        if (textBefore.last() != ' ') return false
        return textBefore[textBefore.length - 2].isLetterOrDigit()
    }

    /** The layer to show after committing [text]. Letters layer never switches away. */
    fun layerAfterTyping(current: LayerId, text: String): LayerId = when {
        current == LayerId.LETTERS -> current
        text == " " -> LayerId.LETTERS
        text.length == 1 && text[0] in RETURNS_TO_LETTERS -> LayerId.LETTERS
        else -> current
    }

    /** A one-shot shift falls away after a character; caps lock does not. */
    fun shiftAfterTyping(shift: ShiftState): ShiftState =
        if (shift == ShiftState.ON) ShiftState.OFF else shift

    /** Tapping shift cycles OFF -> ON -> OFF; a second tap inside the window locks it. */
    fun shiftAfterTap(current: ShiftState, isDoubleTap: Boolean): ShiftState = when {
        isDoubleTap -> ShiftState.LOCKED
        current == ShiftState.OFF -> ShiftState.ON
        else -> ShiftState.OFF
    }

    fun applyShift(text: String, shift: ShiftState): String =
        if (shift == ShiftState.OFF) text else text.uppercase()

    /**
     * How many characters a word-delete should remove: any trailing spaces, then the word
     * before them. Matches what a held backspace does on iOS once it speeds up.
     */
    fun charsInPreviousWord(textBefore: String): Int {
        if (textBefore.isEmpty()) return 0
        var index = textBefore.length
        while (index > 0 && textBefore[index - 1].isWhitespace()) index--
        while (index > 0 && !textBefore[index - 1].isWhitespace()) index--
        return textBefore.length - index
    }
}

/** Timings for held backspace, pulled out so they are easy to tune against a real phone. */
object BackspaceTiming {
    const val INITIAL_DELAY_MS = 400L
    const val CHAR_REPEAT_MS = 55L

    /** After this many single characters, deletion switches to whole words. */
    const val CHARS_BEFORE_WORDS = 14
    const val WORD_REPEAT_MS = 130L
}

/** Window in which a second shift tap counts as a double tap and locks caps. */
const val DOUBLE_TAP_WINDOW_MS = 300L

/** How long space must be held before it becomes a cursor trackpad. */
const val SPACE_DRAG_HOLD_MS = 280L
