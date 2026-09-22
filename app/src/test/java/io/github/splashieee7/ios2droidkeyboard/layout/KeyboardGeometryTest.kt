package io.github.splashieee7.ios2droidkeyboard.layout

import io.github.splashieee7.ios2droidkeyboard.metrics.KeyboardMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A Pixel-class screen width in dp. */
private const val PIXEL_WIDTH = 412f

class KeyboardGeometryTest {

    private val metrics = KeyboardMetrics.forScreenWidth(PIXEL_WIDTH)
    private val letters = buildGeometry(Layers.letters, metrics, PIXEL_WIDTH)

    @Test
    fun `letters layer has the expected key counts per row`() {
        // 10 + 9 + (shift + 7 + delete) + 4
        assertEquals(10 + 9 + 9 + 4, letters.keys.size)
    }

    @Test
    fun `every row spans the full width`() {
        val rows = letters.keys.groupBy { it.bounds.top }
        assertEquals(4, rows.size)
        rows.values.forEach { row ->
            val leftmost = row.minOf { it.hitBounds.left }
            val rightmost = row.maxOf { it.hitBounds.right }
            assertEquals(0f, leftmost, 0.01f)
            assertEquals(PIXEL_WIDTH, rightmost, 0.01f)
        }
    }

    @Test
    fun `drawn keys stay inside the screen`() {
        letters.keys.forEach { placed ->
            assertTrue("${placed.key.label} starts off-screen", placed.bounds.left >= -0.01f)
            assertTrue(
                "${placed.key.label} runs past the right edge",
                placed.bounds.right <= PIXEL_WIDTH + 0.01f,
            )
        }
    }

    @Test
    fun `there are no dead zones anywhere on the keyboard`() {
        // Sample densely, including the gaps between keys, which is where a stock Android
        // keyboard loses touches.
        var x = 0.5f
        while (x < letters.widthDp) {
            var y = 0.5f
            while (y < letters.heightDp) {
                assertNotNull("no key at ($x, $y)", letters.keyAt(x, y))
                y += 1f
            }
            x += 1f
        }
    }

    @Test
    fun `hit areas split the gap between neighbours`() {
        val topRow = letters.keys.filter { it.bounds.top == 0f }.sortedBy { it.bounds.left }
        val q = topRow[0]
        val w = topRow[1]

        // The boundary sits exactly halfway across the gap.
        val expectedBoundary = (q.bounds.right + w.bounds.left) / 2f
        assertEquals(expectedBoundary, q.hitBounds.right, 0.001f)
        assertEquals(expectedBoundary, w.hitBounds.left, 0.001f)

        // A touch in the gap resolves to the nearer key rather than to nothing.
        val justLeftOfBoundary = expectedBoundary - 0.5f
        assertEquals("q", letters.keyAt(justLeftOfBoundary, q.bounds.centerY)?.key?.label)
        val justRight = expectedBoundary + 0.5f
        assertEquals("w", letters.keyAt(justRight, q.bounds.centerY)?.key?.label)
    }

    @Test
    fun `hit areas reach the screen edges and the keyboard top and bottom`() {
        assertEquals("q", letters.keyAt(0f, 0f)?.key?.label)
        assertEquals("p", letters.keyAt(PIXEL_WIDTH - 0.5f, 0f)?.key?.label)
        assertNotNull(letters.keyAt(1f, letters.heightDp - 0.5f))
    }

    @Test
    fun `ASDF row is indented half a key pitch relative to the top row`() {
        val topRow = letters.keys.filter { it.bounds.top == 0f }.minByOrNull { it.bounds.left }!!
        val secondRowTop = metrics.rowPitch
        val secondRow = letters.keys
            .filter { it.bounds.top == secondRowTop }
            .minByOrNull { it.bounds.left }!!

        val indent = secondRow.bounds.left - topRow.bounds.left
        assertEquals(metrics.keyPitch / 2f, indent, 0.5f)
    }

    @Test
    fun `numbers and symbols layers also tile completely`() {
        listOf(Layers.numbers, Layers.symbols).forEach { layer ->
            val geometry = buildGeometry(layer, metrics, PIXEL_WIDTH)
            var x = 0.5f
            while (x < geometry.widthDp) {
                var y = 0.5f
                while (y < geometry.heightDp) {
                    assertNotNull("${layer.id}: no key at ($x, $y)", geometry.keyAt(x, y))
                    y += 2f
                }
                x += 2f
            }
        }
    }
}

class KeyboardMetricsTest {

    @Test
    fun `key aspect ratio matches the reference iPhone`() {
        val metrics = KeyboardMetrics.forScreenWidth(PIXEL_WIDTH)
        assertEquals(KeyboardMetrics.KEY_ASPECT, metrics.keyHeight / metrics.keyWidth, 0.01f)
    }

    @Test
    fun `reference width reproduces the measured iPhone point sizes`() {
        val metrics = KeyboardMetrics.forScreenWidth(KeyboardMetrics.REFERENCE_SCREEN_WIDTH_DP)
        // iphone-metrics.md: 30.8pt wide, 43pt tall, 6.2pt gap, 6.7pt margin.
        assertEquals(30.8f, metrics.keyWidth, 0.3f)
        assertEquals(43f, metrics.keyHeight, 0.3f)
        assertEquals(6.2f, metrics.horizontalGap, 0.3f)
        assertEquals(6.7f, metrics.edgeMargin, 0.3f)
    }

    @Test
    fun `metrics scale linearly with screen width`() {
        val small = KeyboardMetrics.forScreenWidth(360f)
        val large = KeyboardMetrics.forScreenWidth(720f)
        assertEquals(small.keyWidth * 2f, large.keyWidth, 0.001f)
        assertEquals(small.keyHeight * 2f, large.keyHeight, 0.001f)
    }
}
