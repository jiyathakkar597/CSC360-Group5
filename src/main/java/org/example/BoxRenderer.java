package org.example;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Handles the layout and rendering of the two list columns.
 *
 * Responsibilities:
 *  - compute where every box sits on the panel (layout math)
 *  - draw the rounded rectangles + labels with Graphics2D
 *  - highlight boxes whose value appears in both lists
 *  - expose the resulting boxes so ArrowRenderer can anchor its curves to them
 *
 * The box lists produced here are index-aligned with Matcher.getListA() and
 * Matcher.getListB(), so the index pairs returned by
 * Matcher.findMatchingIndexPairs() can be used directly to look up boxes.
 *
 * Author: Dhriti Sarkar
 */
public class BoxRenderer {

    /** A single drawn box: its label, whether it matched, and its on-screen bounds. */
    public static class Box {
        private final String label;
        private final boolean matched;
        private final RoundRectangle2D.Double shape;

        Box(String label, boolean matched, RoundRectangle2D.Double shape) {
            this.label = label;
            this.matched = matched;
            this.shape = shape;
        }

        public String getLabel() {
            return label;
        }

        public boolean isMatched() {
            return matched;
        }

        public RoundRectangle2D.Double getShape() {
            return shape;
        }

        /** Middle of the right edge — where an outgoing arrow starts. */
        public Point2D.Double rightAnchor() {
            return new Point2D.Double(shape.x + shape.width, shape.y + shape.height / 2.0);
        }

        /** Middle of the left edge — where an incoming arrow ends. */
        public Point2D.Double leftAnchor() {
            return new Point2D.Double(shape.x, shape.y + shape.height / 2.0);
        }

        public Point2D.Double center() {
            return new Point2D.Double(shape.x + shape.width / 2.0, shape.y + shape.height / 2.0);
        }
    }

    // ---- layout constants -------------------------------------------------
    private static final int BOX_WIDTH = 170;
    private static final int BOX_HEIGHT = 38;
    private static final int V_GAP = 16;          // vertical space between boxes
    private static final int TOP_MARGIN = 70;     // leaves room for the column titles
    private static final int SIDE_MARGIN = 60;
    private static final int CORNER_ARC = 16;
    private static final int TEXT_PADDING = 10;
    private static final int MIN_COLUMN_GAP = 140; // keeps room for the curves

    // ---- colours ----------------------------------------------------------
    private static final Color MATCHED_FILL = new Color(0xD6, 0xEC, 0xFF);
    private static final Color MATCHED_BORDER = new Color(0x1E, 0x6F, 0xD9);
    private static final Color PLAIN_FILL = new Color(0xF2, 0xF2, 0xF2);
    private static final Color PLAIN_BORDER = new Color(0xA0, 0xA0, 0xA0);
    private static final Color LABEL_COLOR = new Color(0x21, 0x21, 0x21);
    private static final Color TITLE_COLOR = new Color(0x44, 0x44, 0x44);

    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font MATCHED_LABEL_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 16);

    // ---- state ------------------------------------------------------------
    private final List<Box> leftBoxes = new ArrayList<>();
    private final List<Box> rightBoxes = new ArrayList<>();

    private String leftTitle = "List A";
    private String rightTitle = "List B";
    private int leftColumnX;
    private int rightColumnX;

    /**
     * Computes the position of every box from the Matcher's data.
     * Call this before draw(), and again whenever the input lists or the
     * panel width change.
     *
     * @param matcher    the Matcher holding the two lists
     * @param panelWidth current width of the drawing panel
     */
    public void layout(Matcher matcher, int panelWidth) {
        leftBoxes.clear();
        rightBoxes.clear();

        if (matcher == null) {
            return;
        }

        List<String> listA = matcher.getListA();
        List<String> listB = matcher.getListB();
        if (listA == null) listA = Collections.emptyList();
        if (listB == null) listB = Collections.emptyList();

        // Computed once here instead of calling matcher.isCommon() per box,
        // which would rebuild the intersection for every single item.
        Set<String> common = matcher.findCommonElements();

        // Keep the two columns apart even if the window is narrow.
        int minWidth = 2 * SIDE_MARGIN + 2 * BOX_WIDTH + MIN_COLUMN_GAP;
        int width = Math.max(panelWidth, minWidth);

        leftColumnX = SIDE_MARGIN;
        rightColumnX = width - SIDE_MARGIN - BOX_WIDTH;

        for (int i = 0; i < listA.size(); i++) {
            String item = listA.get(i);
            leftBoxes.add(makeBox(item, common.contains(item), leftColumnX, yForRow(i)));
        }
        for (int j = 0; j < listB.size(); j++) {
            String item = listB.get(j);
            rightBoxes.add(makeBox(item, common.contains(item), rightColumnX, yForRow(j)));
        }
    }

    private Box makeBox(String label, boolean matched, int x, int y) {
        RoundRectangle2D.Double shape = new RoundRectangle2D.Double(
                x, y, BOX_WIDTH, BOX_HEIGHT, CORNER_ARC, CORNER_ARC);
        return new Box(label, matched, shape);
    }

    private int yForRow(int index) {
        return TOP_MARGIN + index * (BOX_HEIGHT + V_GAP);
    }

    /** Draws the column titles and every box. Arrows are drawn separately by ArrowRenderer. */
    public void draw(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawTitle(g, leftTitle, leftColumnX);
        drawTitle(g, rightTitle, rightColumnX);

        for (Box box : leftBoxes) {
            drawBox(g, box);
        }
        for (Box box : rightBoxes) {
            drawBox(g, box);
        }
    }

    private void drawTitle(Graphics2D g, String title, int columnX) {
        g.setFont(TITLE_FONT);
        g.setColor(TITLE_COLOR);
        FontMetrics fm = g.getFontMetrics();
        int textX = columnX + (BOX_WIDTH - fm.stringWidth(title)) / 2;
        g.drawString(title, textX, TOP_MARGIN - 24);
    }

    private void drawBox(Graphics2D g, Box box) {
        // fill
        g.setColor(box.isMatched() ? MATCHED_FILL : PLAIN_FILL);
        g.fill(box.getShape());

        // border — thicker for matched items so they stand out
        g.setColor(box.isMatched() ? MATCHED_BORDER : PLAIN_BORDER);
        g.setStroke(new BasicStroke(box.isMatched() ? 2.2f : 1.2f));
        g.draw(box.getShape());

        // label, centred inside the box
        g.setFont(box.isMatched() ? MATCHED_LABEL_FONT : LABEL_FONT);
        g.setColor(LABEL_COLOR);
        FontMetrics fm = g.getFontMetrics();
        String text = fitToWidth(box.getLabel(), fm, BOX_WIDTH - 2 * TEXT_PADDING);

        double textX = box.getShape().x + (BOX_WIDTH - fm.stringWidth(text)) / 2.0;
        double baselineY = box.getShape().y + (BOX_HEIGHT - fm.getHeight()) / 2.0 + fm.getAscent();
        g.drawString(text, (float) textX, (float) baselineY);
    }

    /** Shortens a label with an ellipsis when it is too wide for the box. */
    private String fitToWidth(String text, FontMetrics fm, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && fm.stringWidth(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    // ---- accessors used by ArrowRenderer and Main --------------------------

    /** Boxes for List A, in the same order (and index) as Matcher.getListA(). */
    public List<Box> getLeftBoxes() {
        return Collections.unmodifiableList(leftBoxes);
    }

    /** Boxes for List B, in the same order (and index) as Matcher.getListB(). */
    public List<Box> getRightBoxes() {
        return Collections.unmodifiableList(rightBoxes);
    }

    /** Box at index i of List A, or null if the index is out of range. */
    public Box getLeftBox(int i) {
        return (i >= 0 && i < leftBoxes.size()) ? leftBoxes.get(i) : null;
    }

    /** Box at index j of List B, or null if the index is out of range. */
    public Box getRightBox(int j) {
        return (j >= 0 && j < rightBoxes.size()) ? rightBoxes.get(j) : null;
    }

    /** Height the panel needs so the taller column fits. Useful for setPreferredSize(). */
    public int getPreferredHeight() {
        int rows = Math.max(leftBoxes.size(), rightBoxes.size());
        if (rows == 0) {
            return TOP_MARGIN;
        }
        return TOP_MARGIN + rows * (BOX_HEIGHT + V_GAP) + 20;
    }

    public int getPreferredWidth() {
        return 2 * SIDE_MARGIN + 2 * BOX_WIDTH + MIN_COLUMN_GAP;
    }

    public void setTitles(String leftTitle, String rightTitle) {
        this.leftTitle = leftTitle;
        this.rightTitle = rightTitle;
    }
}
