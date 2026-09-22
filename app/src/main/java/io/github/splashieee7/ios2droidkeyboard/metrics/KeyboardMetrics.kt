package io.github.splashieee7.ios2droidkeyboard.metrics

/**
 * Every size the keyboard draws with, in dp. Nothing else in the codebase is allowed to
 * invent a dimension - if a number matters, it lives here.
 *
 * The ratios below were measured off `reference/iphone-letters-dark.webp` and independently
 * re-measured from the pixels, agreeing with `iphone-metrics.md` to within 0.03%. They are
 * stored as a share of screen width rather than as absolute points on purpose: the reference
 * phone and the target phone are different widths, and a ratio survives that where a fixed
 * point value would not.
 */
data class KeyboardMetrics(
    /** Drawn width of a letter key. */
    val keyWidth: Float,
    /** Drawn height of any key. */
    val keyHeight: Float,
    /** Horizontal distance between the left edges of two adjacent keys. */
    val keyPitch: Float,
    /** Vertical distance between the top edges of two adjacent rows. */
    val rowPitch: Float,
    /** Visible gap between two adjacent keys. */
    val horizontalGap: Float,
    /** Blank margin between the outermost keys and the screen edge. */
    val edgeMargin: Float,
    /** Key corner radius. */
    val cornerRadius: Float,
) {
    /** Height of the whole four-row key grid, excluding the globe/mic strip. */
    val keyGridHeight: Float get() = rowPitch * 3 + keyHeight

    companion object {
        // --- Measured ratios, as a share of screen width ---

        /** Letter key width. Measured 8.18%. */
        const val KEY_WIDTH_RATIO = 0.0818f

        /** Key pitch: key + gap. Measured 9.81%. */
        const val KEY_PITCH_RATIO = 0.0981f

        /** Visible gap between keys. Measured 1.64%. */
        const val HORIZONTAL_GAP_RATIO = 0.0164f

        /** Margin from the outermost key to the screen edge. Measured 1.80%. */
        const val EDGE_MARGIN_RATIO = 0.0180f

        /** Key height. Measured 11.46%. */
        const val KEY_HEIGHT_RATIO = 0.1146f

        /** Row pitch: key height + vertical gap. Measured 14.36%. */
        const val ROW_PITCH_RATIO = 0.1436f

        /** Corner radius, ~7.5pt on a 375pt screen. */
        const val CORNER_RADIUS_RATIO = 0.0200f

        /**
         * Key height divided by key width on the reference iPhone: keys are 1.40x taller
         * than they are wide. Arguably the single most recognisable number here - a thumb
         * learns a key's shape long before it learns its size.
         */
        const val KEY_ASPECT = 1.402f

        /** Key height in dp on the reference iPhone, for anchoring a non-proportional policy. */
        const val REFERENCE_KEY_HEIGHT_DP = 43f

        /** Width in dp of the phone the reference screenshot came from. */
        const val REFERENCE_SCREEN_WIDTH_DP = 375f

        /**
         * Builds the metrics for a real screen.
         *
         * Horizontal sizing is settled: scale straight off the measured ratios, so the keys
         * span the screen exactly as they do on the iPhone. Vertical sizing is the open
         * question - see the TODO below.
         */
        fun forScreenWidth(screenWidthDp: Float): KeyboardMetrics {
            val keyWidth = screenWidthDp * KEY_WIDTH_RATIO
            val keyPitch = screenWidthDp * KEY_PITCH_RATIO
            val horizontalGap = screenWidthDp * HORIZONTAL_GAP_RATIO
            val edgeMargin = screenWidthDp * EDGE_MARGIN_RATIO
            val cornerRadius = screenWidthDp * CORNER_RADIUS_RATIO

            // Height scales proportionally too, so every measured ratio survives intact and
            // a key keeps its 1.40 aspect on any screen. On a ~412dp Pixel that gives 47dp
            // keys against the iPhone's 43dp - slightly larger, in proportion to the larger
            // screen, which is the faithful reading of the reference.
            val keyHeight = screenWidthDp * KEY_HEIGHT_RATIO
            val rowPitch = screenWidthDp * ROW_PITCH_RATIO

            return KeyboardMetrics(
                keyWidth = keyWidth,
                keyHeight = keyHeight,
                keyPitch = keyPitch,
                rowPitch = rowPitch,
                horizontalGap = horizontalGap,
                edgeMargin = edgeMargin,
                cornerRadius = cornerRadius,
            )
        }
    }
}
