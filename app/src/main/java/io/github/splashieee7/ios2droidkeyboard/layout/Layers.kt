package io.github.splashieee7.ios2droidkeyboard.layout

import io.github.splashieee7.ios2droidkeyboard.metrics.KeyboardMetrics

/**
 * The three key layers, laid out with the widths measured from the reference iPhone.
 *
 * Everything is a share of screen width. The rows are built to add up to 1.0, and
 * [io.github.splashieee7.ios2droidkeyboard.layout.buildGeometry] normalises away any
 * rounding drift so each row still reaches both edges exactly.
 */
object Layers {

    private const val LETTER = KeyboardMetrics.KEY_WIDTH_RATIO      // 0.0818
    private const val GAP = KeyboardMetrics.HORIZONTAL_GAP_RATIO    // 0.0164
    private const val MARGIN = KeyboardMetrics.EDGE_MARGIN_RATIO    // 0.0180
    private const val HALF_PITCH = KeyboardMetrics.KEY_PITCH_RATIO / 2f

    /** Shift and delete: 41.7pt on a 375pt screen. */
    private const val WIDE = 0.1112f

    /** iOS puts a noticeably wider gap either side of shift and delete. */
    private const val WIDE_GAP = 0.0365f

    private const val LAYER_KEY = 0.1067f
    private const val EMOJI_KEY = 0.1067f
    private const val SPACE = 0.4747f
    private const val RETURN = 0.2293f
    private const val BOTTOM_GAP = 0.0168f

    /** Punctuation row on the numbers and symbols layers: 5 keys between #+= and delete. */
    private const val PUNCT = 0.1206f

    private val accents = mapOf(
        "a" to listOf("à", "á", "â", "ä", "æ", "ã", "å", "ā"),
        "c" to listOf("ç", "ć", "č"),
        "e" to listOf("è", "é", "ê", "ë", "ē", "ė", "ę"),
        "i" to listOf("î", "ï", "í", "ī", "į", "ì"),
        "l" to listOf("ł"),
        "n" to listOf("ñ", "ń"),
        "o" to listOf("ô", "ö", "ò", "ó", "œ", "ø", "ō", "õ"),
        "s" to listOf("ß", "ś", "š"),
        "u" to listOf("û", "ü", "ù", "ú", "ū"),
        "y" to listOf("ÿ"),
        "z" to listOf("ž", "ź", "ż"),
    )

    private fun letter(char: String, gapAfter: Float = GAP) = Key(
        action = KeyAction.Character(char),
        label = char,
        widthRatio = LETTER,
        gapAfterRatio = gapAfter,
        accents = accents[char].orEmpty(),
        showsPopup = true,
    )

    private fun symbol(
        char: String,
        width: Float = LETTER,
        gapAfter: Float = GAP,
        alts: List<String> = emptyList(),
    ) = Key(
        action = KeyAction.Character(char),
        label = char,
        widthRatio = width,
        gapAfterRatio = gapAfter,
        accents = alts,
        showsPopup = true,
    )

    /** Row of evenly spaced, letter-width keys that spans edge to edge. */
    private fun evenRow(chars: List<String>, alts: Map<String, List<String>> = emptyMap()) =
        KeyRow(
            leadingRatio = MARGIN,
            keys = chars.mapIndexed { index, char ->
                symbol(
                    char = char,
                    gapAfter = if (index == chars.lastIndex) 0f else GAP,
                    alts = alts[char].orEmpty(),
                )
            },
            trailingRatio = MARGIN,
        )

    /** Shift · letters · delete. Shared shape between rows 3 of every layer. */
    private fun thirdRow(middle: List<Key>, leftKey: Key, rightKey: Key) = KeyRow(
        leadingRatio = MARGIN,
        keys = buildList {
            add(leftKey)
            addAll(middle)
            add(rightKey)
        },
        trailingRatio = MARGIN,
    )

    private val shiftKey = Key(
        action = KeyAction.Shift,
        label = "⇧",
        widthRatio = WIDE,
        gapAfterRatio = WIDE_GAP,
        isFunctionKey = true,
    )

    private val deleteKey = Key(
        action = KeyAction.Backspace,
        label = "⌫",
        widthRatio = WIDE,
        isFunctionKey = true,
    )

    /** The bottom row is identical on every layer apart from which layer the left key goes to. */
    private fun bottomRow(layerLabel: String, target: LayerId) = KeyRow(
        leadingRatio = MARGIN,
        keys = listOf(
            Key(
                action = KeyAction.SwitchLayer(target),
                label = layerLabel,
                widthRatio = LAYER_KEY,
                gapAfterRatio = BOTTOM_GAP,
                isFunctionKey = true,
            ),
            Key(
                action = KeyAction.Emoji,
                label = "☺",
                widthRatio = EMOJI_KEY,
                gapAfterRatio = BOTTOM_GAP,
                isFunctionKey = true,
            ),
            Key(
                action = KeyAction.Space,
                label = "space",
                widthRatio = SPACE,
                gapAfterRatio = BOTTOM_GAP,
            ),
            Key(
                action = KeyAction.Return,
                label = "return",
                widthRatio = RETURN,
                isFunctionKey = true,
            ),
        ),
        trailingRatio = MARGIN,
    )

    val letters = Layer(
        id = LayerId.LETTERS,
        rows = listOf(
            KeyRow(
                leadingRatio = MARGIN,
                keys = "qwertyuiop".map { letter(it.toString()) }
                    .mapIndexed { i, k -> if (i == 9) k.copy(gapAfterRatio = 0f) else k },
                trailingRatio = MARGIN,
            ),
            // The ASDF row is indented exactly half a key pitch on each side.
            KeyRow(
                leadingRatio = MARGIN + HALF_PITCH,
                keys = "asdfghjkl".map { letter(it.toString()) }
                    .mapIndexed { i, k -> if (i == 8) k.copy(gapAfterRatio = 0f) else k },
                trailingRatio = MARGIN + HALF_PITCH,
            ),
            thirdRow(
                middle = "zxcvbnm".map { letter(it.toString()) }
                    .mapIndexed { i, k -> if (i == 6) k.copy(gapAfterRatio = WIDE_GAP) else k },
                leftKey = shiftKey,
                rightKey = deleteKey,
            ),
            bottomRow(layerLabel = "123", target = LayerId.NUMBERS),
        ),
    )

    val numbers = Layer(
        id = LayerId.NUMBERS,
        rows = listOf(
            evenRow(
                chars = "1234567890".map { it.toString() },
                alts = mapOf("0" to listOf("°")),
            ),
            evenRow(
                chars = listOf("-", "/", ":", ";", "(", ")", "$", "&", "@", "\""),
                alts = mapOf(
                    "-" to listOf("–", "—", "•"),
                    "/" to listOf("\\"),
                    "$" to listOf("€", "£", "¥", "₩", "₱"),
                    "&" to listOf("§"),
                    "\"" to listOf("“", "”", "„", "«", "»"),
                ),
            ),
            thirdRow(
                middle = listOf(".", ",", "?", "!", "'").mapIndexed { index, char ->
                    symbol(
                        char = char,
                        width = PUNCT,
                        gapAfter = if (index == 4) WIDE_GAP else GAP,
                        alts = when (char) {
                            "'" -> listOf("‘", "’", "`")
                            "?" -> listOf("¿")
                            "!" -> listOf("¡")
                            else -> emptyList()
                        },
                    )
                },
                leftKey = Key(
                    action = KeyAction.SwitchLayer(LayerId.SYMBOLS),
                    label = "#+=",
                    widthRatio = WIDE,
                    gapAfterRatio = WIDE_GAP,
                    isFunctionKey = true,
                ),
                rightKey = deleteKey,
            ),
            bottomRow(layerLabel = "ABC", target = LayerId.LETTERS),
        ),
    )

    val symbols = Layer(
        id = LayerId.SYMBOLS,
        rows = listOf(
            evenRow(listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "=")),
            evenRow(
                listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•"),
            ),
            thirdRow(
                middle = listOf(".", ",", "?", "!", "'").mapIndexed { index, char ->
                    symbol(
                        char = char,
                        width = PUNCT,
                        gapAfter = if (index == 4) WIDE_GAP else GAP,
                    )
                },
                leftKey = Key(
                    action = KeyAction.SwitchLayer(LayerId.NUMBERS),
                    label = "123",
                    widthRatio = WIDE,
                    gapAfterRatio = WIDE_GAP,
                    isFunctionKey = true,
                ),
                rightKey = deleteKey,
            ),
            bottomRow(layerLabel = "ABC", target = LayerId.LETTERS),
        ),
    )

    fun of(id: LayerId): Layer = when (id) {
        LayerId.LETTERS -> letters
        LayerId.NUMBERS -> numbers
        LayerId.SYMBOLS -> symbols
    }
}
