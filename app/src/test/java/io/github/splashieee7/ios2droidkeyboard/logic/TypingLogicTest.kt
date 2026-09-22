package io.github.splashieee7.ios2droidkeyboard.logic

import io.github.splashieee7.ios2droidkeyboard.layout.LayerId
import io.github.splashieee7.ios2droidkeyboard.layout.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCapitalizeTest {

    @Test
    fun `capitalises at the start of an empty field`() {
        assertTrue(TypingLogic.shouldAutoCapitalize(""))
    }

    @Test
    fun `capitalises after a sentence ender and a space`() {
        assertTrue(TypingLogic.shouldAutoCapitalize("Hello there. "))
        assertTrue(TypingLogic.shouldAutoCapitalize("Really! "))
        assertTrue(TypingLogic.shouldAutoCapitalize("What? "))
    }

    @Test
    fun `capitalises after a newline`() {
        assertTrue(TypingLogic.shouldAutoCapitalize("first line\n"))
    }

    @Test
    fun `does not capitalise mid sentence`() {
        assertFalse(TypingLogic.shouldAutoCapitalize("hello "))
        assertFalse(TypingLogic.shouldAutoCapitalize("hello"))
        assertFalse(TypingLogic.shouldAutoCapitalize("e.g"))
    }

    @Test
    fun `does not capitalise straight after the period with no space`() {
        assertFalse(TypingLogic.shouldAutoCapitalize("Hello."))
    }
}

class DoubleSpaceTest {

    @Test
    fun `space after a word plus space becomes a period`() {
        assertTrue(TypingLogic.shouldInsertPeriodForDoubleSpace("hello "))
        assertTrue(TypingLogic.shouldInsertPeriodForDoubleSpace("item 2 "))
    }

    @Test
    fun `does not fire on two spaces already`() {
        assertFalse(TypingLogic.shouldInsertPeriodForDoubleSpace("hello  "))
    }

    @Test
    fun `does not fire after punctuation or at the start`() {
        assertFalse(TypingLogic.shouldInsertPeriodForDoubleSpace("hello. "))
        assertFalse(TypingLogic.shouldInsertPeriodForDoubleSpace(" "))
        assertFalse(TypingLogic.shouldInsertPeriodForDoubleSpace(""))
    }
}

class ShiftTest {

    @Test
    fun `a single tap turns shift on, another turns it off`() {
        val on = TypingLogic.shiftAfterTap(ShiftState.OFF, isDoubleTap = false)
        assertEquals(ShiftState.ON, on)
        assertEquals(ShiftState.OFF, TypingLogic.shiftAfterTap(on, isDoubleTap = false))
    }

    @Test
    fun `a double tap locks caps`() {
        assertEquals(ShiftState.LOCKED, TypingLogic.shiftAfterTap(ShiftState.ON, isDoubleTap = true))
    }

    @Test
    fun `one-shot shift falls away after a character but caps lock does not`() {
        assertEquals(ShiftState.OFF, TypingLogic.shiftAfterTyping(ShiftState.ON))
        assertEquals(ShiftState.LOCKED, TypingLogic.shiftAfterTyping(ShiftState.LOCKED))
        assertEquals(ShiftState.OFF, TypingLogic.shiftAfterTyping(ShiftState.OFF))
    }

    @Test
    fun `applyShift uppercases only when shift is engaged`() {
        assertEquals("a", TypingLogic.applyShift("a", ShiftState.OFF))
        assertEquals("A", TypingLogic.applyShift("a", ShiftState.ON))
        assertEquals("A", TypingLogic.applyShift("a", ShiftState.LOCKED))
    }
}

class LayerSwitchTest {

    @Test
    fun `punctuation on the numbers layer returns to letters`() {
        listOf(".", ",", "?", "!", "'").forEach { char ->
            assertEquals(
                "typing $char should return to letters",
                LayerId.LETTERS,
                TypingLogic.layerAfterTyping(LayerId.NUMBERS, char),
            )
        }
    }

    @Test
    fun `space returns to letters from the symbols layer`() {
        assertEquals(LayerId.LETTERS, TypingLogic.layerAfterTyping(LayerId.SYMBOLS, " "))
    }

    @Test
    fun `digits keep you on the numbers layer`() {
        assertEquals(LayerId.NUMBERS, TypingLogic.layerAfterTyping(LayerId.NUMBERS, "5"))
    }

    @Test
    fun `the letters layer never switches away on its own`() {
        assertEquals(LayerId.LETTERS, TypingLogic.layerAfterTyping(LayerId.LETTERS, "."))
    }
}

class WordDeleteTest {

    @Test
    fun `deletes the word before the cursor`() {
        assertEquals(5, TypingLogic.charsInPreviousWord("hello world"))
    }

    @Test
    fun `swallows trailing spaces along with the word`() {
        assertEquals(6, TypingLogic.charsInPreviousWord("hello world "))
    }

    @Test
    fun `returns zero on empty text`() {
        assertEquals(0, TypingLogic.charsInPreviousWord(""))
    }

    @Test
    fun `handles a single word`() {
        assertEquals(5, TypingLogic.charsInPreviousWord("hello"))
    }
}
