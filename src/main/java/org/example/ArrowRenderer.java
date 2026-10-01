package org.example;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Draws arrows (connecting lines) between matched boxes produced by a
 * {@link BoxRenderer}, based on the index pairs reported by a {@link Matcher}.
 *
 * <p>This class does not perform any layout itself — it assumes the given
 * {@code BoxRenderer} has already had {@code layout(...)} called on it, so
 * that box positions are available.</p>
 *
 * @author Aangi Shah
 */
public class ArrowRenderer {

    /** Stroke width used for every arrow line. */
    private static final float ARROW_STROKE_WIDTH = 1.2f;

    /** Alpha (0-255) used for the arrow color, to keep dense arrow sets readable. */
    private static final int ARROW_ALPHA = 90;

    /** Base color for arrows before alpha is applied. */
    private static final Color ARROW_BASE_COLOR = new Color(30, 90, 200);

    /** The fully-configured color (base color + alpha) used to stroke arrows. */
    private static final Color ARROW_COLOR =
            new Color(ARROW_BASE_COLOR.getRed(), ARROW_BASE_COLOR.getGreen(),
                    ARROW_BASE_COLOR.getBlue(), ARROW_ALPHA);

    /**
     * Draws one line for every matching pair reported by the given matcher,
     * connecting the corresponding boxes in the given box renderer.
     *
     * <p>Pairs whose left or right box cannot be found (or resolve to a gap
     * marker box) are silently skipped, since a gap marker has no single
     * matching partner to draw a line to.</p>
     *
     * @param g       the graphics context to draw into; must not be {@code null}
     * @param matcher the matcher supplying original-index match pairs; must not be {@code null}
     * @param boxes   the box renderer that has already been laid out; must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public void draw(Graphics2D g, Matcher matcher, BoxRenderer boxes) {
        if (g == null || matcher == null || boxes == null) {
            throw new NullPointerException("g, matcher, and boxes must not be null");
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(ARROW_STROKE_WIDTH));
            g2.setColor(ARROW_COLOR);

            List<int[]> pairs = matcher.findMatchingIndexPairs();
            for (int[] pair : pairs) {
                int leftIndex = pair[0];
                int rightIndex = pair[1];

                BoxRenderer.Box leftBox = boxes.getLeftBoxForOriginalIndex(leftIndex);
                BoxRenderer.Box rightBox = boxes.getRightBoxForOriginalIndex(rightIndex);

                if (!isDrawable(leftBox) || !isDrawable(rightBox)) {
                    continue;
                }

                Point2D.Double start = leftBox.rightAnchor();
                Point2D.Double end = rightBox.leftAnchor();
                g2.draw(new Line2D.Double(start, end));
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Checks whether a box is a valid arrow endpoint (non-null and not a gap marker).
     *
     * @param box the box to check, may be {@code null}
     * @return {@code true} if the box can be used as an arrow endpoint
     */
    private boolean isDrawable(BoxRenderer.Box box) {
        return box != null && !box.isGap();
    }
}