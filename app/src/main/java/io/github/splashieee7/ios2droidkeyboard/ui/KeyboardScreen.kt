package io.github.splashieee7.ios2droidkeyboard.ui

import android.media.AudioManager
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.splashieee7.ios2droidkeyboard.ime.KeyboardActions
import io.github.splashieee7.ios2droidkeyboard.layout.KeyAction
import io.github.splashieee7.ios2droidkeyboard.layout.LayerId
import io.github.splashieee7.ios2droidkeyboard.layout.Layers
import io.github.splashieee7.ios2droidkeyboard.layout.PlacedKey
import io.github.splashieee7.ios2droidkeyboard.layout.ShiftState
import io.github.splashieee7.ios2droidkeyboard.layout.buildGeometry
import io.github.splashieee7.ios2droidkeyboard.logic.BackspaceTiming
import io.github.splashieee7.ios2droidkeyboard.logic.DOUBLE_TAP_WINDOW_MS
import io.github.splashieee7.ios2droidkeyboard.logic.SPACE_DRAG_HOLD_MS
import io.github.splashieee7.ios2droidkeyboard.logic.TypingLogic
import io.github.splashieee7.ios2droidkeyboard.metrics.KeyboardMetrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** Long-press delay before the accent menu appears. */
private const val ACCENT_HOLD_MS = 300L

/** Strip below the key grid holding the globe key, outside the grid as on iOS. */
private const val BOTTOM_STRIP_RATIO = 0.105f

/** How far the finger travels per character when dragging the space bar as a trackpad. */
private const val CURSOR_STEP_DP = 8f

private data class AccentMenu(val anchor: PlacedKey, val selectedIndex: Int)

@Composable
fun KeyboardScreen(actions: KeyboardActions) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value
        val metrics = remember(widthDp) { KeyboardMetrics.forScreenWidth(widthDp) }
        val stripHeight = widthDp * BOTTOM_STRIP_RATIO
        val colors = keyboardColors()

        var layerId by remember { mutableStateOf(LayerId.LETTERS) }
        var shift by remember { mutableStateOf(ShiftState.ON) }
        var pressed by remember { mutableStateOf<PlacedKey?>(null) }
        var accentMenu by remember { mutableStateOf<AccentMenu?>(null) }
        var spaceDragging by remember { mutableStateOf(false) }
        var lastShiftTapAt by remember { mutableLongStateOf(0L) }

        val geometry = remember(layerId, widthDp) {
            buildGeometry(Layers.of(layerId), metrics, widthDp)
        }

        val view = LocalView.current
        val audioManager = LocalContext.current.getSystemService(AudioManager::class.java)

        // Both respect the user's system settings - no flags forcing them on.
        fun playFeedback() {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
        }

        /** Re-evaluates auto-capitalisation. Caps lock always wins. */
        fun refreshShift() {
            if (shift == ShiftState.LOCKED) return
            shift = if (TypingLogic.shouldAutoCapitalize(actions.textBeforeCursor())) {
                ShiftState.ON
            } else {
                ShiftState.OFF
            }
        }

        fun typeCharacter(raw: String, applyShift: Boolean) {
            val text = if (applyShift) TypingLogic.applyShift(raw, shift) else raw
            actions.commitText(text)
            layerId = TypingLogic.layerAfterTyping(layerId, raw)
            if (shift != ShiftState.LOCKED) {
                shift = TypingLogic.shiftAfterTyping(shift)
                refreshShift()
            }
        }

        fun onTap(placed: PlacedKey) {
            when (val action = placed.key.action) {
                is KeyAction.Character -> typeCharacter(action.text, applyShift = true)

                is KeyAction.Space -> {
                    val before = actions.textBeforeCursor()
                    if (TypingLogic.shouldInsertPeriodForDoubleSpace(before)) {
                        actions.deleteBackward(1)
                        actions.commitText(". ")
                    } else {
                        actions.commitText(" ")
                    }
                    layerId = TypingLogic.layerAfterTyping(layerId, " ")
                    refreshShift()
                }

                is KeyAction.Shift -> {
                    val now = System.currentTimeMillis()
                    val isDoubleTap = now - lastShiftTapAt <= DOUBLE_TAP_WINDOW_MS
                    lastShiftTapAt = now
                    shift = TypingLogic.shiftAfterTap(shift, isDoubleTap)
                }

                is KeyAction.Return -> {
                    actions.performReturn()
                    refreshShift()
                }

                is KeyAction.SwitchLayer -> layerId = action.target

                is KeyAction.Globe -> actions.switchToNextKeyboard()

                // Emoji panel is a later phase; the key is present so the layout is right.
                is KeyAction.Emoji -> Unit

                is KeyAction.Backspace -> Unit // handled by the hold effect below
            }
        }

        // Held backspace: single characters, accelerating to whole words. Runs for as long as
        // the key stays down, and is cancelled automatically when `pressed` changes.
        val pressedKey = pressed
        LaunchedEffect(pressedKey) {
            if (pressedKey?.key?.action !is KeyAction.Backspace) return@LaunchedEffect
            actions.deleteBackward(1)
            delay(BackspaceTiming.INITIAL_DELAY_MS)
            var deletions = 0
            while (true) {
                if (deletions < BackspaceTiming.CHARS_BEFORE_WORDS) {
                    actions.deleteBackward(1)
                    delay(BackspaceTiming.CHAR_REPEAT_MS)
                } else {
                    val count = TypingLogic.charsInPreviousWord(actions.textBeforeCursor())
                    if (count == 0) break
                    actions.deleteBackward(count)
                    delay(BackspaceTiming.WORD_REPEAT_MS)
                }
                deletions++
            }
        }

        val totalHeight = geometry.heightDp + stripHeight

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight.dp)
                .background(colors.background)
                .pointerInput(geometry) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x / density
                        val startY = down.position.y / density
                        val placed = geometry.keyAt(startX, startY) ?: return@awaitEachGesture

                        pressed = placed
                        playFeedback()

                        val key = placed.key

                        // Space doubles as a cursor trackpad once it is held.
                        if (key.action is KeyAction.Space) {
                            val up = withTimeoutOrNull(SPACE_DRAG_HOLD_MS) {
                                waitForUpOrCancellation()
                            }
                            if (up != null) {
                                onTap(placed)
                                pressed = null
                                return@awaitEachGesture
                            }
                            spaceDragging = true
                            var lastX = down.position.x
                            var carried = 0f
                            while (true) {
                                val change = awaitPointerEvent().changes
                                    .firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                carried += (change.position.x - lastX) / density
                                lastX = change.position.x
                                while (carried >= CURSOR_STEP_DP) {
                                    actions.moveCursor(1)
                                    carried -= CURSOR_STEP_DP
                                }
                                while (carried <= -CURSOR_STEP_DP) {
                                    actions.moveCursor(-1)
                                    carried += CURSOR_STEP_DP
                                }
                                change.consume()
                            }
                            spaceDragging = false
                            pressed = null
                            return@awaitEachGesture
                        }

                        // Keys with alternatives open an accent menu on hold, and the finger
                        // slides across it to pick one.
                        if (key.accents.isNotEmpty()) {
                            val up = withTimeoutOrNull(ACCENT_HOLD_MS) {
                                waitForUpOrCancellation()
                            }
                            if (up != null) {
                                onTap(placed)
                                pressed = null
                                return@awaitEachGesture
                            }
                            accentMenu = AccentMenu(placed, 0)
                            playFeedback()
                            var chosen = 0
                            while (true) {
                                val change = awaitPointerEvent().changes
                                    .firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                chosen = accentIndexAt(
                                    x = change.position.x / density,
                                    anchor = placed,
                                    count = key.accents.size,
                                    metrics = metrics,
                                    keyboardWidth = widthDp,
                                )
                                accentMenu = AccentMenu(placed, chosen)
                                change.consume()
                            }
                            key.accents.getOrNull(chosen)?.let { accent ->
                                typeCharacter(accent, applyShift = true)
                            }
                            accentMenu = null
                            pressed = null
                            return@awaitEachGesture
                        }

                        waitForUpOrCancellation()
                        onTap(placed)
                        pressed = null
                    }
                },
        ) {
            geometry.keys.forEach { placed ->
                KeyView(
                    placed = placed,
                    metrics = metrics,
                    colors = colors,
                    shift = shift,
                    isPressed = pressed == placed,
                    // iOS blanks every keycap while the space bar is acting as a trackpad.
                    blankLabel = spaceDragging,
                )
            }

            GlobeStrip(
                widthDp = widthDp,
                topDp = geometry.heightDp,
                heightDp = stripHeight,
                colors = colors,
                onGlobe = { actions.switchToNextKeyboard() },
            )

            pressed?.let { placed ->
                if (placed.key.showsPopup && accentMenu == null && !spaceDragging) {
                    KeyPopup(placed = placed, metrics = metrics, colors = colors, shift = shift)
                }
            }

            accentMenu?.let { menu ->
                AccentMenuView(
                    menu = menu,
                    metrics = metrics,
                    colors = colors,
                    shift = shift,
                    keyboardWidth = widthDp,
                )
            }
        }
    }
}

/** Width of one cell in the accent menu. */
private fun accentItemWidth(metrics: KeyboardMetrics) = metrics.keyWidth * 1.15f

private fun accentMenuLeft(
    anchor: PlacedKey,
    count: Int,
    metrics: KeyboardMetrics,
    keyboardWidth: Float,
): Float {
    val width = accentItemWidth(metrics) * count
    return (anchor.bounds.centerX - width / 2f).coerceIn(0f, (keyboardWidth - width).coerceAtLeast(0f))
}

private fun accentIndexAt(
    x: Float,
    anchor: PlacedKey,
    count: Int,
    metrics: KeyboardMetrics,
    keyboardWidth: Float,
): Int {
    val left = accentMenuLeft(anchor, count, metrics, keyboardWidth)
    val item = accentItemWidth(metrics)
    return (((x - left) / item).toInt()).coerceIn(0, count - 1)
}

@Composable
private fun KeyView(
    placed: PlacedKey,
    metrics: KeyboardMetrics,
    colors: KeyboardColors,
    shift: ShiftState,
    isPressed: Boolean,
    blankLabel: Boolean,
) {
    val key = placed.key
    val isShiftKey = key.action is KeyAction.Shift
    val shiftActive = isShiftKey && shift != ShiftState.OFF

    val background = when {
        shiftActive -> colors.activeKey
        isPressed && !key.isFunctionKey -> colors.pressedKey
        key.isFunctionKey -> colors.functionKey
        else -> colors.letterKey
    }

    val label = when {
        blankLabel -> ""
        key.action is KeyAction.Character && key.showsPopup && key.label.length == 1 &&
            key.label[0].isLetter() -> TypingLogic.applyShift(key.label, shift)
        shift == ShiftState.LOCKED && isShiftKey -> "⇪"
        else -> key.label
    }

    val fontSize = if (key.isFunctionKey || key.label.length > 1) {
        metrics.keyHeight * 0.36f
    } else {
        metrics.keyHeight * 0.52f
    }

    Box(
        modifier = Modifier
            .offset(x = placed.bounds.left.dp, y = placed.bounds.top.dp)
            .size(width = placed.bounds.width.dp, height = placed.bounds.height.dp)
            .clip(RoundedCornerShape(metrics.cornerRadius.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (shiftActive) colors.activeLabel else colors.label,
            style = TextStyle(fontSize = fontSize.sp, textAlign = TextAlign.Center),
        )
    }
}

/** The enlarged bubble above a pressed letter key. */
@Composable
private fun KeyPopup(
    placed: PlacedKey,
    metrics: KeyboardMetrics,
    colors: KeyboardColors,
    shift: ShiftState,
) {
    val width = placed.bounds.width * 1.35f
    val height = placed.bounds.height * 1.15f
    val left = (placed.bounds.centerX - width / 2f)
    val top = placed.bounds.top - height - metrics.keyHeight * 0.12f

    Box(
        modifier = Modifier
            .offset(x = left.dp, y = top.coerceAtLeast(0f).dp)
            .size(width = width.dp, height = height.dp)
            .clip(RoundedCornerShape((metrics.cornerRadius * 1.4f).dp))
            .background(colors.popupBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = TypingLogic.applyShift(placed.key.label, shift),
            color = colors.popupLabel,
            style = TextStyle(fontSize = (metrics.keyHeight * 0.66f).sp),
        )
    }
}

@Composable
private fun AccentMenuView(
    menu: AccentMenu,
    metrics: KeyboardMetrics,
    colors: KeyboardColors,
    shift: ShiftState,
    keyboardWidth: Float,
) {
    val accents = menu.anchor.key.accents
    val item = accentItemWidth(metrics)
    val left = accentMenuLeft(menu.anchor, accents.size, metrics, keyboardWidth)
    val height = menu.anchor.bounds.height * 1.15f
    val top = (menu.anchor.bounds.top - height - metrics.keyHeight * 0.12f).coerceAtLeast(0f)

    Box(
        modifier = Modifier
            .offset(x = left.dp, y = top.dp)
            .size(width = (item * accents.size).dp, height = height.dp)
            .clip(RoundedCornerShape((metrics.cornerRadius * 1.4f).dp))
            .background(colors.popupBackground),
    ) {
        accents.forEachIndexed { index, accent ->
            Box(
                modifier = Modifier
                    .offset(x = (item * index).dp)
                    .size(width = item.dp, height = height.dp)
                    .clip(RoundedCornerShape((metrics.cornerRadius * 1.1f).dp))
                    .background(
                        if (index == menu.selectedIndex) Color(0xFF0A84FF) else Color.Transparent,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = TypingLogic.applyShift(accent, shift),
                    color = if (index == menu.selectedIndex) Color.White else colors.popupLabel,
                    style = TextStyle(fontSize = (metrics.keyHeight * 0.48f).sp),
                )
            }
        }
    }
}

/** Globe key sits below the key grid, outside it, as on iOS. */
@Composable
private fun GlobeStrip(
    widthDp: Float,
    topDp: Float,
    heightDp: Float,
    colors: KeyboardColors,
    onGlobe: () -> Unit,
) {
    Box(
        modifier = Modifier
            .offset(x = (widthDp * 0.02f).dp, y = topDp.dp)
            .size(width = (widthDp * 0.16f).dp, height = heightDp.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    waitForUpOrCancellation()?.let { onGlobe() }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "🌐",
            color = colors.secondaryLabel,
            style = TextStyle(fontSize = (heightDp * 0.42f).sp),
        )
    }
}
