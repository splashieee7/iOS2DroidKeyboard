package io.github.splashieee7.ios2droidkeyboard.layout

import io.github.splashieee7.ios2droidkeyboard.metrics.KeyboardMetrics
import kotlin.math.abs

/** An axis-aligned rectangle in dp. Right and bottom are exclusive. */
data class Rect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun contains(x: Float, y: Float): Boolean =
        x >= left && x < right && y >= top && y < bottom
}

/**
 * A key with its final position.
 *
 * [bounds] is what gets drawn. [hitBounds] is what responds to touch, and it is bigger: it
 * extends halfway into each surrounding gap. That difference is the whole point - see
 * [buildGeometry].
 */
data class PlacedKey(
    val key: Key,
    val bounds: Rect,
    val hitBounds: Rect,
)

/** A laid-out layer, ready to draw and to hit-test. */
data class KeyboardGeometry(
    val widthDp: Float,
    val heightDp: Float,
    val keys: List<PlacedKey>,
) {
    /**
     * The key under a touch. Never returns null for a point inside the keyboard, because the
     * hit rectangles tile it completely; the nearest-centre fallback only covers touches that
     * arrive slightly outside the bounds.
     */
    fun keyAt(x: Float, y: Float): PlacedKey? {
        keys.firstOrNull { it.hitBounds.contains(x, y) }?.let { return it }
        return keys.minByOrNull { placed ->
            val dx = abs(placed.bounds.centerX - x)
            val dy = abs(placed.bounds.centerY - y)
            dx * dx + dy * dy
        }
    }
}

/**
 * Turns a [Layer] of width ratios into real dp positions.
 *
 * Two things happen here that matter:
 *
 * 1. **Normalisation.** Each row's ratios are scaled so the row spans exactly the screen
 *    width. The measured percentages sum to about 100.2% rather than 100%, because they came
 *    from rounded pixel measurements, and left uncorrected that drift would push the last key
 *    of every row off the right edge.
 *
 * 2. **Gap-splitting.** Each key's touch target grows to meet its neighbours halfway across
 *    the gap, and the outer keys extend to the screen edge. There is therefore no point on
 *    the keyboard that belongs to no key. This is the single biggest feel difference against
 *    a stock Android keyboard, where the gaps are dead.
 */
fun buildGeometry(
    layer: Layer,
    metrics: KeyboardMetrics,
    widthDp: Float,
): KeyboardGeometry {
    val keyboardHeight = metrics.keyGridHeight
    val placed = mutableListOf<PlacedKey>()

    // Drawn rectangles first, row by row.
    val rowRects: List<List<Pair<Key, Rect>>> = layer.rows.mapIndexed { rowIndex, row ->
        val rawTotal = row.leadingRatio + row.trailingRatio + row.keys.sumOf {
            (it.widthRatio + it.gapAfterRatio).toDouble()
        }.toFloat()
        val scale = if (rawTotal > 0f) 1f / rawTotal else 1f

        val top = rowIndex * metrics.rowPitch
        var cursor = row.leadingRatio * scale * widthDp

        row.keys.map { key ->
            val width = key.widthRatio * scale * widthDp
            val rect = Rect(
                left = cursor,
                top = top,
                right = cursor + width,
                bottom = top + metrics.keyHeight,
            )
            cursor += width + key.gapAfterRatio * scale * widthDp
            key to rect
        }
    }

    rowRects.forEachIndexed { rowIndex, rowKeys ->
        val rowTop = rowIndex * metrics.rowPitch
        val rowBottom = rowTop + metrics.keyHeight

        // Vertical split: halfway into the gap above and below, edges reach the boundary.
        val hitTop = if (rowIndex == 0) {
            0f
        } else {
            val previousBottom = (rowIndex - 1) * metrics.rowPitch + metrics.keyHeight
            (previousBottom + rowTop) / 2f
        }
        val hitBottom = if (rowIndex == rowRects.lastIndex) {
            keyboardHeight
        } else {
            val nextTop = (rowIndex + 1) * metrics.rowPitch
            (rowBottom + nextTop) / 2f
        }

        rowKeys.forEachIndexed { keyIndex, (key, rect) ->
            val hitLeft = if (keyIndex == 0) {
                0f
            } else {
                (rowKeys[keyIndex - 1].second.right + rect.left) / 2f
            }
            val hitRight = if (keyIndex == rowKeys.lastIndex) {
                widthDp
            } else {
                (rect.right + rowKeys[keyIndex + 1].second.left) / 2f
            }

            placed += PlacedKey(
                key = key,
                bounds = rect,
                hitBounds = Rect(hitLeft, hitTop, hitRight, hitBottom),
            )
        }
    }

    return KeyboardGeometry(widthDp = widthDp, heightDp = keyboardHeight, keys = placed)
}
