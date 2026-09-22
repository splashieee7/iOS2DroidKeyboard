package io.github.splashieee7.ios2droidkeyboard.ime

/**
 * Everything the keyboard UI is allowed to do to the text field it is attached to.
 *
 * The UI talks to this instead of touching an InputConnection directly, so the Compose layer
 * stays free of Android input plumbing and can be previewed and tested with a fake. It is
 * also the only surface in the app through which typed text passes, which is what makes the
 * "nothing typed is logged" rule checkable rather than aspirational.
 */
interface KeyboardActions {
    fun commitText(text: String)

    /** Deletes [count] characters before the cursor. */
    fun deleteBackward(count: Int = 1)

    /** Text immediately before the cursor, used by the auto-capitalise and double-space rules. */
    fun textBeforeCursor(maxChars: Int = 64): String

    /** Moves the cursor by [steps] characters; negative is left. */
    fun moveCursor(steps: Int)

    /** Enter / send, respecting whatever action the field asked for. */
    fun performReturn()

    /** Hands over to the next installed keyboard (the globe key). */
    fun switchToNextKeyboard()
}
