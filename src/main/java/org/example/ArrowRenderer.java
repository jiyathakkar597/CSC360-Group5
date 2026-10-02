package org.example;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws arrows (connecting lines) between matched boxes produced by a
 * {@link BoxRenderer}, based on the index pairs reported by a {@link Matcher}.
 *
 * <p>Each matched pair gets a colour from a small colour-blind-safe palette,
 * used for its line and for the outlines of both of its boxes, so a pair can
 * often be spotted by colour alone. Neighbouring lines never share a colour.
 * When a point is set with {@link #setHoverPoint(Point2D)} (for example, the
 * mouse position), the pair under that point is drawn bold and every other
 * line is faded.</p>
 *
 * <p>This class does not perform any layout itself — it assumes the given
 * {@code BoxRenderer} has already had {@code layout(...)} called on it, so
 * that box positions are available.</p>
 *
 * @author Aangi Shah
 */
public class ArrowRenderer {

    /**
     * Colours cycled through for match pairs. Based on the Okabe-Ito
     * colour-blind-safe palette (its yellow swapped for a dark wine, which
     * shows up better on the light background).
     */
    private static final Color[] PALETTE = {
            new Color(0x00, 0x72, 0xB2), // blue
            new Color(0xD5, 0x5E, 0x00), // vermillion
            new Color(0x00, 0x9E, 0x73), // bluish green
            new Color(0xCC, 0x79, 0xA7), // reddish purple
            new Color(0xE6, 0x9F, 0x00), // orange
            new Color(0x56, 0xB4, 0xE9), // sky blue
            new Color(0x88, 0x22, 0x55), // wine
    };

    /** Stroke width used for an arrow line in its normal state. */
    private static final float ARROW_STROKE_WIDTH = 1.4f;

    /** Stroke width used for the line of the hovered pair. */
    private static final float HIGHLIGHT_STROKE_WIDTH = 3.0f;

    /** Stroke width used to outline a pair's boxes in the pair's colour. */
    private static final float BOX_OUTLINE_WIDTH = 2.2f;

    /** Stroke width used to outline the hovered pair's boxes. */
    private static final float HIGHLIGHT_BOX_OUTLINE_WIDTH = 3.5f;

    /** Alpha (0-255) for arrow lines in their normal state. */
    private static final int ARROW_ALPHA = 170;

    /** Alpha used instead when there are too many arrows to give them tracks. */
    private static final int DENSE_ARROW_ALPHA = 90;

    /** Alpha for the lines of every pair other than the hovered one. */
    private static final int FADED_ALPHA = 30;

    /** Outline colour for the boxes of every pair other than the hovered one. */
    private static final Color FADED_BOX_OUTLINE = new Color(0xB0, 0xB0, 0xB0);

    /** How close (in pixels) the hover point must be to a line to pick it. */
    private static final double HOVER_TOLERANCE = 4.0;

    /** Narrowest spacing between centre-gap tracks before falling back to diagonals. */
    private static final double MIN_TRACK_PITCH = 2.0;

    /** Height differences smaller than this are drawn as a plain horizontal line. */
    private static final double STRAIGHT_TOLERANCE = 0.5;

    /**
     * One arrow to draw: its two boxes, plus the heights at which it enters
     * and leaves the centre gap (after any lane routing on either side).
     */
    private record Route(BoxRenderer.Box leftBox, BoxRenderer.Box rightBox, double leftY, double rightY) {

        boolean isStraight() {
            return Math.abs(rightY - leftY) < STRAIGHT_TOLERANCE;
        }

        boolean goesDown() {
            return rightY > leftY;
        }
    }

    /** A routed arrow ready to draw: the polyline it follows and its colour. */
    private record Arrow(Route route, List<Point2D.Double> points, Color color) {

        Path2D.Double toPath() {
            Path2D.Double path = new Path2D.Double();
            path.moveTo(points.get(0).x, points.get(0).y);
            for (int i = 1; i < points.size(); i++) {
                path.lineTo(points.get(i).x, points.get(i).y);
            }
            return path;
        }

        double distanceTo(Point2D p) {
            double best = Double.MAX_VALUE;
            for (int i = 1; i < points.size(); i++) {
                best = Math.min(best, Line2D.ptSegDist(
                        points.get(i - 1).x, points.get(i - 1).y,
                        points.get(i).x, points.get(i).y,
                        p.getX(), p.getY()));
            }
            return best;
        }
    }

    /** Arrows from the most recent {@link #draw}, used for hover hit-testing. */
    private List<Arrow> lastArrows = List.of();

    /** Current hover point, or {@code null} when nothing is hovered. */
    private Point2D hoverPoint;

    /** Arrows picked out by {@link #hoverPoint} among {@link #lastArrows}. */
    private List<Arrow> highlighted = List.of();

    /**
     * Draws one line for every matching pair reported by the given matcher,
     * connecting the corresponding boxes in the given box renderer, and
     * outlines both boxes of each pair in the pair's colour.
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

        lastArrows = buildArrows(matcher, boxes);
        highlighted = findHighlighted(lastArrows, hoverPoint);
        boolean hovering = !highlighted.isEmpty();

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setStroke(new BasicStroke(ARROW_STROKE_WIDTH));
            for (Arrow arrow : lastArrows) {
                g2.setColor(hovering ? withAlpha(arrow.color(), FADED_ALPHA) : arrow.color());
                g2.draw(arrow.toPath());
            }

            g2.setStroke(new BasicStroke(BOX_OUTLINE_WIDTH));
            for (Arrow arrow : lastArrows) {
                g2.setColor(hovering ? FADED_BOX_OUTLINE : opaque(arrow.color()));
                g2.draw(arrow.route().leftBox().getShape());
                g2.draw(arrow.route().rightBox().getShape());
            }

            // The hovered pair goes on top, bold and fully opaque.
            for (Arrow arrow : highlighted) {
                g2.setColor(opaque(arrow.color()));
                g2.setStroke(new BasicStroke(HIGHLIGHT_STROKE_WIDTH,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(arrow.toPath());
                g2.setStroke(new BasicStroke(HIGHLIGHT_BOX_OUTLINE_WIDTH));
                g2.draw(arrow.route().leftBox().getShape());
                g2.draw(arrow.route().rightBox().getShape());
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Sets the point (in the same coordinates the boxes are drawn in) used to
     * pick a pair to highlight. Hovering a box highlights every pair that box
     * belongs to; hovering near a line highlights that line's pair. Pass
     * {@code null} to clear the highlight.
     *
     * @param point the hover point, or {@code null}
     * @return {@code true} if the highlighted pair changed and the caller
     *         should repaint
     */
    public boolean setHoverPoint(Point2D point) {
        hoverPoint = point;
        List<Arrow> next = findHighlighted(lastArrows, point);
        if (next.equals(highlighted)) {
            return false;
        }
        highlighted = next;
        return true;
    }

    /**
     * Picks the arrows to highlight for a hover point: every arrow attached
     * to the box under the point, or else the single nearest arrow within
     * {@link #HOVER_TOLERANCE}.
     */
    private List<Arrow> findHighlighted(List<Arrow> arrows, Point2D point) {
        if (point == null) {
            return List.of();
        }

        List<Arrow> onBox = new ArrayList<>();
        for (Arrow arrow : arrows) {
            if (arrow.route().leftBox().getShape().contains(point)
                    || arrow.route().rightBox().getShape().contains(point)) {
                onBox.add(arrow);
            }
        }
        if (!onBox.isEmpty()) {
            return onBox;
        }

        Arrow nearest = null;
        double nearestDistance = HOVER_TOLERANCE;
        for (Arrow arrow : arrows) {
            double distance = arrow.distanceTo(point);
            if (distance <= nearestDistance) {
                nearest = arrow;
                nearestDistance = distance;
            }
        }
        return nearest == null ? List.of() : List.of(nearest);
    }

    /**
     * Works out every arrow's route, centre-gap track and colour.
     *
     * <p>Colours are handed out in the order arrows sit next to each other,
     * so neighbours always differ: sloped arrows in track order, and level
     * arrows from top to bottom (a level arrow's neighbours are the ones
     * directly above and below it).</p>
     */
    private List<Arrow> buildArrows(Matcher matcher, BoxRenderer boxes) {
        List<Route> routes = new ArrayList<>();
        for (int[] pair : matcher.findMatchingIndexPairs()) {
            BoxRenderer.Box leftBox = boxes.getLeftBoxForOriginalIndex(pair[0]);
            BoxRenderer.Box rightBox = boxes.getRightBoxForOriginalIndex(pair[1]);

            if (!isDrawable(leftBox) || !isDrawable(rightBox)) {
                continue;
            }

            routes.add(new Route(leftBox, rightBox,
                    gapEntryY(leftBox, leftLane(leftBox, boxes), boxes),
                    gapEntryY(rightBox, rightLane(rightBox, boxes), boxes)));
        }

        List<Route> sloped = orderSloped(routes);
        Map<Route, Double> trackX = assignTracks(sloped, boxes);
        boolean dense = !sloped.isEmpty() && trackX.isEmpty();
        int alpha = dense ? DENSE_ARROW_ALPHA : ARROW_ALPHA;

        List<Route> level = new ArrayList<>();
        for (Route route : routes) {
            if (route.isStraight()) {
                level.add(route);
            }
        }
        if (dense) {
            // Without tracks, sloped arrows are plain diagonals; colour them
            // by height along with the level ones.
            level.addAll(sloped);
            sloped = List.of();
        }
        level.sort(Comparator.comparingDouble(Route::leftY));

        List<Arrow> arrows = new ArrayList<>();
        for (int i = 0; i < sloped.size(); i++) {
            Route route = sloped.get(i);
            arrows.add(new Arrow(route, buildPoints(route, trackX.get(route), boxes), paletteColour(i, alpha)));
        }
        for (int i = 0; i < level.size(); i++) {
            Route route = level.get(i);
            arrows.add(new Arrow(route, buildPoints(route, null, boxes), paletteColour(i, alpha)));
        }
        return arrows;
    }

    /**
     * Puts the sloped arrows into track order, left to right.
     *
     * <p>Drawn as straight diagonals, arrows can all funnel through the same
     * spot: when List B is List A reversed, for instance, every line passes
     * through the exact centre of the gap. Instead, each arrow crosses the gap
     * horizontally at its own height, turns onto a vertical track that no
     * other arrow uses, and turns again at its destination height.</p>
     *
     * <p>Track order keeps crossings down. Downward arrows sit to the left
     * of upward ones, and within each group the arrow that starts lowest
     * turns first (for downward arrows) or last (for upward arrows). Two
     * arrows that are staggered rather than nested then never cross.</p>
     *
     * @param routes every arrow to be drawn
     * @return the sloped arrows in track order; level arrows are left out
     */
    private List<Route> orderSloped(List<Route> routes) {
        List<Route> down = new ArrayList<>();
        List<Route> up = new ArrayList<>();
        for (Route route : routes) {
            if (!route.isStraight()) {
                (route.goesDown() ? down : up).add(route);
            }
        }
        down.sort(Comparator.comparingDouble(Route::leftY).reversed());
        up.sort(Comparator.comparingDouble(Route::leftY));

        List<Route> ordered = new ArrayList<>(down);
        ordered.addAll(up);
        return ordered;
    }

    /**
     * Spreads the sloped arrows' tracks evenly across the centre gap. If
     * there are too many arrows to give each a visibly separate track, no
     * tracks are assigned and arrows fall back to straight diagonals.
     *
     * @param ordered sloped arrows in track order, from {@link #orderSloped}
     * @param boxes   the laid-out box renderer, for the centre gap's edges
     * @return the track x for each sloped arrow, or an empty map
     */
    private Map<Route, Double> assignTracks(List<Route> ordered, BoxRenderer boxes) {
        Map<Route, Double> trackX = new IdentityHashMap<>();
        double leftEdgeX = boxes.getLeftInnerEdgeX();
        double pitch = (boxes.getRightInnerEdgeX() - leftEdgeX) / (ordered.size() + 1);
        if (pitch < MIN_TRACK_PITCH) {
            return trackX;
        }
        for (int i = 0; i < ordered.size(); i++) {
            trackX.put(ordered.get(i), leftEdgeX + (i + 1) * pitch);
        }
        return trackX;
    }

    /**
     * Builds the polyline for one arrow so that it never crosses another box.
     *
     * <p>A box in the innermost column on its side connects straight to the
     * centre gap. A box in an outer column first steps into the gutter beside
     * its own column, drops into the gap below its row, and follows that gap
     * horizontally past the inner columns to the centre. Each outer column
     * gets its own lane inside the gap (the column nearest the centre takes
     * the lane closest to the row), so arrows from different columns never
     * merge and never cross each other's turns. Inside the centre gap the
     * arrow follows its own vertical track (see {@link #orderSloped}).</p>
     *
     * @param route  the arrow to draw
     * @param trackX x of this arrow's track in the centre gap, or {@code null}
     *               to cross the gap in a single straight segment
     * @param boxes  the laid-out box renderer, for column and lane geometry
     * @return the points the arrow passes through, from List A to List B
     */
    private List<Point2D.Double> buildPoints(Route route, Double trackX, BoxRenderer boxes) {
        double halfGap = boxes.getColumnGap() / 2.0;
        double leftEdgeX = boxes.getLeftInnerEdgeX();
        double rightEdgeX = boxes.getRightInnerEdgeX();

        List<Point2D.Double> points = new ArrayList<>();

        // List A side: from the box out to the centre gap.
        Point2D.Double start = route.leftBox().rightAnchor();
        points.add(start);
        if (leftLane(route.leftBox(), boxes) >= 0) {
            double gutterX = start.x + halfGap;
            points.add(new Point2D.Double(gutterX, start.y));
            points.add(new Point2D.Double(gutterX, route.leftY()));
            points.add(new Point2D.Double(leftEdgeX, route.leftY()));
        }

        // Centre gap: across, along this arrow's own track, and across again.
        if (trackX != null) {
            points.add(new Point2D.Double(trackX, route.leftY()));
            points.add(new Point2D.Double(trackX, route.rightY()));
        }

        // List B side, from the centre gap into the box.
        Point2D.Double end = route.rightBox().leftAnchor();
        if (rightLane(route.rightBox(), boxes) >= 0) {
            double gutterX = end.x - halfGap;
            points.add(new Point2D.Double(rightEdgeX, route.rightY()));
            points.add(new Point2D.Double(gutterX, route.rightY()));
            points.add(new Point2D.Double(gutterX, end.y));
        }
        points.add(end);

        return points;
    }

    /**
     * Lane an arrow from this List A box uses to reach the centre, or -1 if
     * the box is in the innermost column and connects directly.
     */
    private int leftLane(BoxRenderer.Box box, BoxRenderer boxes) {
        return boxes.getLeftColumnCount() - 2 - box.getColumnIndex();
    }

    /**
     * Lane an arrow into this List B box uses from the centre, or -1 if the
     * box is in the innermost column and connects directly.
     */
    private int rightLane(BoxRenderer.Box box, BoxRenderer boxes) {
        return boxes.getRightColumnCount() - 2 - box.getColumnIndex();
    }

    /**
     * Height at which an arrow for this box meets the centre gap: the box's
     * own middle if it connects directly, otherwise its lane below the row.
     */
    private double gapEntryY(BoxRenderer.Box box, int lane, BoxRenderer boxes) {
        return lane >= 0 ? boxes.getLaneY(box, lane) : box.center().y;
    }

    /** The i-th palette colour (cycling) with the given alpha. */
    private static Color paletteColour(int i, int alpha) {
        return withAlpha(PALETTE[i % PALETTE.length], alpha);
    }

    private static Color withAlpha(Color colour, int alpha) {
        return new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), alpha);
    }

    private static Color opaque(Color colour) {
        return withAlpha(colour, 255);
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
