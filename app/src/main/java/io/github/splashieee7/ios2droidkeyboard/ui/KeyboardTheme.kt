package io.github.splashieee7.ios2droidkeyboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

/**
 * Keyboard colours.
 *
 * The dark values are sampled from the reference screenshot: keys `#3F3F3F` on a `#1A1A1A`
 * background, and notably letter and function keys are the *same* colour in this iOS
 * version, which is why [functionKey] equals [letterKey] there. The light values follow the
 * iOS light keyboard, where function keys are visibly darker than letter keys.
 */
@Immutable
data class KeyboardColors(
    val background: Color,
    val letterKey: Color,
    val functionKey: Color,
    val activeKey: Color,
    val pressedKey: Color,
    val label: Color,
    val activeLabel: Color,
    val secondaryLabel: Color,
    val popupBackground: Color,
    val popupLabel: Color,
)

private val DarkColors = KeyboardColors(
    background = Color(0xFF1A1A1A),
    letterKey = Color(0xFF3F3F3F),
    functionKey = Color(0xFF3F3F3F),
    activeKey = Color(0xFFD1D1D1),
    pressedKey = Color(0xFF565656),
    label = Color.White,
    activeLabel = Color(0xFF1A1A1A),
    secondaryLabel = Color(0xFFB0B0B0),
    popupBackground = Color(0xFF6E6E6E),
    popupLabel = Color.White,
)

private val LightColors = KeyboardColors(
    background = Color(0xFFD1D3D9),
    letterKey = Color(0xFFFFFFFF),
    functionKey = Color(0xFFABB0BA),
    activeKey = Color(0xFFFFFFFF),
    pressedKey = Color(0xFFE3E5E9),
    label = Color(0xFF000000),
    activeLabel = Color(0xFF000000),
    secondaryLabel = Color(0xFF4A4A4A),
    popupBackground = Color(0xFFFFFFFF),
    popupLabel = Color(0xFF000000),
)

@Composable
fun keyboardColors(): KeyboardColors =
    if (isSystemInDarkTheme()) DarkColors else LightColors
