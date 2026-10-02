package org.example;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
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

                g2.draw(buildRoute(leftBox, rightBox, boxes));
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Builds the path for one arrow so that it never crosses another box.
     *
     * <p>A box in the innermost column on its side connects straight to the
     * centre gap. A box in an outer column first steps into the gutter beside
     * its own column, drops into the gap below its row, and follows that gap
     * horizontally past the inner columns to the centre. Each outer column
     * gets its own lane inside the gap (the column nearest the centre takes
     * the lane closest to the row), so arrows from different columns never
     * merge and never cross each other's turns. Only the segment across the
     * centre gap is diagonal.</p>
     *
     * @param leftBox  the matched box in List A
     * @param rightBox the matched box in List B
     * @param boxes    the laid-out box renderer, for column and lane geometry
     * @return the path to stroke
     */
    private Path2D.Double buildRoute(BoxRenderer.Box leftBox, BoxRenderer.Box rightBox, BoxRenderer boxes) {
        double halfGap = boxes.getColumnGap() / 2.0;
        double leftEdgeX = boxes.getLeftInnerEdgeX();
        double rightEdgeX = boxes.getRightInnerEdgeX();

        Path2D.Double path = new Path2D.Double();

        // List A side: from the box out to the centre gap.
        Point2D.Double start = leftBox.rightAnchor();
        path.moveTo(start.x, start.y);
        int leftLane = boxes.getLeftColumnCount() - 2 - leftBox.getColumnIndex();
        if (leftLane >= 0) {
            double gutterX = start.x + halfGap;
            double laneY = boxes.getLaneY(leftBox, leftLane);
            path.lineTo(gutterX, start.y);
            path.lineTo(gutterX, laneY);
            path.lineTo(leftEdgeX, laneY);
        }

        // List B side, worked out from the box inward and then joined up.
        Point2D.Double end = rightBox.leftAnchor();
        int rightLane = boxes.getRightColumnCount() - 2 - rightBox.getColumnIndex();
        if (rightLane >= 0) {
            double gutterX = end.x - halfGap;
            double laneY = boxes.getLaneY(rightBox, rightLane);
            path.lineTo(rightEdgeX, laneY);
            path.lineTo(gutterX, laneY);
            path.lineTo(gutterX, end.y);
        }
        path.lineTo(end.x, end.y);

        return path;
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