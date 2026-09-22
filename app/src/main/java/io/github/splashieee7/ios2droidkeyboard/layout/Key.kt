package io.github.splashieee7.ios2droidkeyboard.layout

/** Which set of keys is on screen. */
enum class LayerId { LETTERS, NUMBERS, SYMBOLS }

/** Shift is a three-state control on iOS, not a toggle. */
enum class ShiftState { OFF, ON, LOCKED }

/** What pressing a key does. */
sealed interface KeyAction {
    /** Types [text], adjusted for shift if the key is a letter. */
    data class Character(val text: String) : KeyAction

    data object Shift : KeyAction

    data object Backspace : KeyAction

    data object Space : KeyAction

    data object Return : KeyAction

    data class SwitchLayer(val target: LayerId) : KeyAction

    data object Emoji : KeyAction

    data object Globe : KeyAction
}

/**
 * One key, sized as a share of screen width so it ports across devices unchanged.
 *
 * [gapAfterRatio] is the visible gap to the next key in the row. It is per-key rather than
 * per-row because iOS uses a wider gap either side of shift and delete than it does between
 * letters, and that asymmetry is visible.
 */
data class Key(
    val action: KeyAction,
    val label: String,
    val widthRatio: Float,
    val gapAfterRatio: Float = 0f,
    /** Long-press alternatives, e.g. a -> à á â. Empty means no long-press menu. */
    val accents: List<String> = emptyList(),
    /** Letter keys get the enlarged bubble on press; function keys do not. */
    val showsPopup: Boolean = false,
    /** Function keys are a different colour in light mode. */
    val isFunctionKey: Boolean = false,
)

/**
 * A single row of keys plus the blank space either side of it.
 *
 * [trailingRatio] has to be declared rather than inferred: the row is normalised to span the
 * screen exactly, so a margin that isn't counted gets stretched away.
 */
data class KeyRow(
    val leadingRatio: Float,
    val keys: List<Key>,
    val trailingRatio: Float,
)

/** A complete set of rows: the letters layer, the numbers layer, or the symbols layer. */
data class Layer(
    val id: LayerId,
    val rows: List<KeyRow>,
)
