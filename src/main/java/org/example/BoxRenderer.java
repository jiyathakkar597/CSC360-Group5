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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the layout and rendering of the two list columns.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>compute where every box sits on the panel (layout math)</li>
 *   <li>shrink box height to fit the available space, down to a readable
 *       minimum, then wrap into extra columns if it still does not fit</li>
 *   <li>draw the rounded rectangles and labels with Graphics2D</li>
 *   <li>highlight boxes whose value appears in both lists</li>
 *   <li>draw a placeholder box for runs of hidden non-matching items</li>
 *   <li>expose the resulting boxes so ArrowRenderer can anchor its curves to them</li>
 * </ul>
 *
 * <p><b>Interface change:</b> boxes are now built from {@link Matcher#buildRowsA()}
 * and {@link Matcher#buildRowsB()} instead of the raw lists, because a run of
 * non-matching items can collapse into a single gap box. This means box
 * position in {@link #getLeftBoxes()} is <b>no longer the same as the index
 * in {@code Matcher.getListA()}</b>. Code that needs to find the box for an
 * original list index (for example, ArrowRenderer using
 * {@link Matcher#findMatchingIndexPairs()}) must use
 * {@link #getLeftBoxForOriginalIndex(int)} or
 * {@link #getRightBoxForOriginalIndex(int)} instead.</p>
 *
 * @author Dhriti Sarkar
 */
public class BoxRenderer {

    /** A single drawn box: its label, whether it matched, and its on-screen bounds. */
    public static class Box {
        private final String label;
        private final boolean matched;
        private final boolean gap;
        private final int gapCount;
        private final RoundRectangle2D.Double shape;

        Box(String label, boolean matched, boolean gap, int gapCount, RoundRectangle2D.Double shape) {
            this.label = label;
            this.matched = matched;
            this.gap = gap;
            this.gapCount = gapCount;
            this.shape = shape;
        }

        public String getLabel() {
            return label;
        }

        public boolean isMatched() {
            return matched;
        }

        /** True if this box is a placeholder standing in for a run of hidden non-matching items. */
        public boolean isGap() {
            return gap;
        }

        /** Number of items this box hides; 0 for a normal item box. */
        public int getGapCount() {
            return gapCount;
        }

        public RoundRectangle2D.Double getShape() {
            return shape;
        }

        /** Middle of the right edge of this box. */
        public Point2D.Double rightAnchor() {
            return new Point2D.Double(shape.x + shape.width, shape.y + shape.height / 2.0);
        }

        /** Middle of the left edge of this box. */
        public Point2D.Double leftAnchor() {
            return new Point2D.Double(shape.x, shape.y + shape.height / 2.0);
        }

        public Point2D.Double center() {
            return new Point2D.Double(shape.x + shape.width / 2.0, shape.y + shape.height / 2.0);
        }
    }

    // ---- layout constants ---------------------------------------------------
    private static final int BOX_WIDTH = 170;
    private static final int MAX_BOX_HEIGHT = 38;   // box height for short lists
    private static final int MIN_BOX_HEIGHT = 16;   // smallest we shrink to before adding columns
    private static final int SPACING = 4;            // vertical space between boxes in a column
    private static final int COLUMN_GAP = 24;         // horizontal space between columns on the same side
    private static final int MAX_COLUMNS = 4;         // soft cap; the last column absorbs any overflow
    private static final int TOP_MARGIN = 70;         // leaves room for the column titles
    private static final int BOTTOM_MARGIN = 20;
    private static final int SIDE_MARGIN = 60;
    private static final int CORNER_ARC = 16;
    private static final int TEXT_PADDING = 10;
    private static final int CENTER_GAP = 140;        // space reserved between the two lists for arrows

    // ---- colours --------------------------------------------------------------
    private static final Color MATCHED_FILL = new Color(0xD6, 0xEC, 0xFF);
    private static final Color MATCHED_BORDER = new Color(0x1E, 0x6F, 0xD9);
    private static final Color PLAIN_FILL = new Color(0xF2, 0xF2, 0xF2);
    private static final Color PLAIN_BORDER = new Color(0xA0, 0xA0, 0xA0);
    private static final Color GAP_FILL = new Color(0xEA, 0xEA, 0xEA);
    private static final Color GAP_BORDER = new Color(0xB5, 0xB5, 0xB5);
    private static final Color GAP_LABEL_COLOR = new Color(0x70, 0x70, 0x70);
    private static final Color LABEL_COLOR = new Color(0x21, 0x21, 0x21);
    private static final Color TITLE_COLOR = new Color(0x44, 0x44, 0x44);

    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font MATCHED_LABEL_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font GAP_LABEL_FONT = new Font("SansSerif", Font.ITALIC, 12);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 16);

    private static final float PLAIN_BORDER_WIDTH = 1.2f;
    private static final float MATCHED_BORDER_WIDTH = 2.2f;
    private static final float GAP_BORDER_WIDTH = 1.0f;
    private static final float[] GAP_DASH_PATTERN = { 4f, 4f };

    // ---- state ------------------------------------------------------------------
    private final List<Box> leftBoxes = new ArrayList<>();
    private final List<Box> rightBoxes = new ArrayList<>();
    private final Map<Integer, Box> leftIndexToBox = new HashMap<>();
    private final Map<Integer, Box> rightIndexToBox = new HashMap<>();

    private String leftTitle = "List A";
    private String rightTitle = "List B";
    private int leftColumnBaseX;
    private int rightColumnBaseX;

    // remembered from the last layout() call, used by getPreferredWidth/Height
    private int currentBoxHeight = MAX_BOX_HEIGHT;
    private int currentWidth = minWidthForColumns(1, 1);
    private int rowsPerColumnLeft;
    private int rowsPerColumnRight;

    /**
     * Computes the position of every box from the Matcher's data, shrinking
     * box height to fit {@code panelHeight} and wrapping into extra columns
     * if shrinking alone is not enough. Call this before {@link #draw(Graphics2D)},
     * and again whenever the input lists or the panel size change.
     *
     * @param matcher     the Matcher holding the two lists
     * @param panelWidth  current width of the drawing panel
     * @param panelHeight current visible height of the drawing panel; pass 0
     *                    (or any value 0 or less) for unlimited height, which
     *                    reproduces the old fixed-height, single-column layout
     */
    public void layout(Matcher matcher, int panelWidth, int panelHeight) {
        leftBoxes.clear();
        rightBoxes.clear();
        leftIndexToBox.clear();
        rightIndexToBox.clear();

        if (matcher == null) {
            return;
        }

        List<Matcher.Row> rowsA = matcher.buildRowsA();
        List<Matcher.Row> rowsB = matcher.buildRowsB();

        int availableHeight = panelHeight - TOP_MARGIN - BOTTOM_MARGIN;

        int heightA = computeAutoFitHeight(rowsA.size(), availableHeight);
        int heightB = computeAutoFitHeight(rowsB.size(), availableHeight);
        currentBoxHeight = Math.min(heightA, heightB);

        rowsPerColumnLeft = computeRowsPerColumn(rowsA.size(), currentBoxHeight, availableHeight);
        rowsPerColumnRight = computeRowsPerColumn(rowsB.size(), currentBoxHeight, availableHeight);

        int columnsLeft = columnsNeeded(rowsA.size(), rowsPerColumnLeft);
        int columnsRight = columnsNeeded(rowsB.size(), rowsPerColumnRight);

        int minWidth = minWidthForColumns(columnsLeft, columnsRight);
        currentWidth = Math.max(panelWidth, minWidth);

        leftColumnBaseX = SIDE_MARGIN;
        rightColumnBaseX = currentWidth - SIDE_MARGIN - BOX_WIDTH;

        placeRows(rowsA, rowsPerColumnLeft, columnsLeft, leftColumnBaseX, true,
                leftBoxes, leftIndexToBox);
        placeRows(rowsB, rowsPerColumnRight, columnsRight, rightColumnBaseX, false,
                rightBoxes, rightIndexToBox);
    }

    /**
     * Convenience overload kept for callers not yet updated to pass a height.
     * Equivalent to the old fixed-height, single-column layout.
     *
     * @param matcher    the Matcher holding the two lists
     * @param panelWidth current width of the drawing panel
     * @deprecated use {@link #layout(Matcher, int, int)} so box height can
     *             auto-fit to the panel's visible height
     */
    @Deprecated
    public void layout(Matcher matcher, int panelWidth) {
        layout(matcher, panelWidth, 0);
    }

    /**
     * Works out the tallest a box can be while still fitting every row into
     * the available height in one column, clamped between
     * {@link #MIN_BOX_HEIGHT} and {@link #MAX_BOX_HEIGHT}.
     *
     * @param rowCount        number of rows to fit
     * @param availableHeight visible height left for boxes after margins;
     *                        0 or less is treated as unlimited
     * @return the box height to use
     */
    private int computeAutoFitHeight(int rowCount, int availableHeight) {
        if (rowCount <= 0 || availableHeight <= 0) {
            return MAX_BOX_HEIGHT;
        }
        int raw = (availableHeight - SPACING * (rowCount - 1)) / rowCount;
        return Math.max(MIN_BOX_HEIGHT, Math.min(MAX_BOX_HEIGHT, raw));
    }

    /**
     * Works out how many rows of the given height fit in one column within
     * the available height.
     *
     * @param rowCount        total rows that need to be placed
     * @param boxHeight       height chosen by {@link #computeAutoFitHeight}
     * @param availableHeight visible height left for boxes after margins;
     *                        0 or less is treated as unlimited
     * @return rows per column; never more than {@code rowCount}, never less than 1
     */
    private int computeRowsPerColumn(int rowCount, int boxHeight, int availableHeight) {
        if (rowCount <= 0) {
            return 1;
        }
        if (availableHeight <= 0) {
            return rowCount;
        }
        int perColumn = (availableHeight + SPACING) / (boxHeight + SPACING);
        return Math.max(1, Math.min(perColumn, rowCount));
    }

    /**
     * Number of columns needed to hold every row, capped at {@link #MAX_COLUMNS}.
     * If the cap is reached, the last column simply grows taller than the
     * others, which is the scrolling fallback of last resort.
     */
    private int columnsNeeded(int rowCount, int rowsPerColumn) {
        if (rowCount <= 0) {
            return 1;
        }
        int needed = (int) Math.ceil(rowCount / (double) rowsPerColumn);
        return Math.max(1, Math.min(needed, MAX_COLUMNS));
    }

    private int minWidthForColumns(int columnsLeft, int columnsRight) {
        int leftBlock = columnsLeft * BOX_WIDTH + (columnsLeft - 1) * COLUMN_GAP;
        int rightBlock = columnsRight * BOX_WIDTH + (columnsRight - 1) * COLUMN_GAP;
        return 2 * SIDE_MARGIN + leftBlock + CENTER_GAP + rightBlock;
    }

    /**
     * Places one side's rows into boxes, column by column, and records each
     * box under its original list index(es) in {@code indexToBox}.
     *
     * @param rows           the rows to place, in original list order
     * @param rowsPerColumn  rows per column, from {@link #computeRowsPerColumn}
     * @param columns        number of columns, from {@link #columnsNeeded}
     * @param baseX          x position of column 0
     * @param growRightward  true for the left list (columns move toward the
     *                       centre), false for the right list (columns move
     *                       toward the centre from the other side)
     * @param outBoxes       list to append the created boxes to
     * @param indexToBox     map to record original-index-to-box lookups in
     */
    private void placeRows(List<Matcher.Row> rows, int rowsPerColumn, int columns, int baseX,
                           boolean growRightward, List<Box> outBoxes, Map<Integer, Box> indexToBox) {
        for (int i = 0; i < rows.size(); i++) {
            int columnIndex = Math.min(i / rowsPerColumn, columns - 1);
            int positionInColumn = i - columnIndex * rowsPerColumn;

            int x = growRightward
                    ? baseX + columnIndex * (BOX_WIDTH + COLUMN_GAP)
                    : baseX - columnIndex * (BOX_WIDTH + COLUMN_GAP);
            int y = TOP_MARGIN + positionInColumn * (currentBoxHeight + SPACING);

            Matcher.Row row = rows.get(i);
            Box box = (row instanceof Matcher.GapRow gapRow)
                    ? makeGapBox(gapRow, x, y)
                    : makeItemBox((Matcher.ItemRow) row, x, y);
            outBoxes.add(box);

            if (row instanceof Matcher.ItemRow itemRow) {
                indexToBox.put(itemRow.index(), box);
            } else {
                Matcher.GapRow gapRow = (Matcher.GapRow) row;
                for (int k = 0; k < gapRow.count(); k++) {
                    // Hidden items map to the gap marker box, so a match that
                    // falls inside a collapsed run still has somewhere to
                    // anchor an arrow, rather than nothing at all.
                    indexToBox.put(gapRow.startIndex() + k, box);
                }
            }
        }
    }

    private Box makeItemBox(Matcher.ItemRow row, int x, int y) {
        RoundRectangle2D.Double shape = new RoundRectangle2D.Double(
                x, y, BOX_WIDTH, currentBoxHeight, CORNER_ARC, CORNER_ARC);
        return new Box(row.text(), row.matched(), false, 0, shape);
    }

    private Box makeGapBox(Matcher.GapRow row, int x, int y) {
        RoundRectangle2D.Double shape = new RoundRectangle2D.Double(
                x, y, BOX_WIDTH, currentBoxHeight, CORNER_ARC, CORNER_ARC);
        String label = "-- " + row.count() + " hidden --";
        return new Box(label, false, true, row.count(), shape);
    }

    /** Draws the column titles and every box. Arrows are drawn separately by ArrowRenderer. */
    public void draw(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawTitle(g, leftTitle, leftColumnBaseX);
        drawTitle(g, rightTitle, rightColumnBaseX);

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
        if (box.isGap()) {
            drawGapBox(g, box);
            return;
        }

        g.setColor(box.isMatched() ? MATCHED_FILL : PLAIN_FILL);
        g.fill(box.getShape());

        g.setColor(box.isMatched() ? MATCHED_BORDER : PLAIN_BORDER);
        g.setStroke(new BasicStroke(box.isMatched() ? MATCHED_BORDER_WIDTH : PLAIN_BORDER_WIDTH));
        g.draw(box.getShape());

        g.setFont(box.isMatched() ? MATCHED_LABEL_FONT : LABEL_FONT);
        g.setColor(LABEL_COLOR);
        drawCenteredLabel(g, box);
    }

    private void drawGapBox(Graphics2D g, Box box) {
        g.setColor(GAP_FILL);
        g.fill(box.getShape());

        g.setColor(GAP_BORDER);
        g.setStroke(new BasicStroke(GAP_BORDER_WIDTH, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0f, GAP_DASH_PATTERN, 0f));
        g.draw(box.getShape());

        g.setFont(GAP_LABEL_FONT);
        g.setColor(GAP_LABEL_COLOR);
        drawCenteredLabel(g, box);
    }

    private void drawCenteredLabel(Graphics2D g, Box box) {
        FontMetrics fm = g.getFontMetrics();
        String text = fitToWidth(box.getLabel(), fm, BOX_WIDTH - 2 * TEXT_PADDING);

        double textX = box.getShape().x + (BOX_WIDTH - fm.stringWidth(text)) / 2.0;
        double baselineY = box.getShape().y
                + (box.getShape().height - fm.getHeight()) / 2.0 + fm.getAscent();
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

    // ---- accessors used by ArrowRenderer and Main ------------------------------

    /** All boxes for List A (items and gap markers), in rendered row order. */
    public List<Box> getLeftBoxes() {
        return Collections.unmodifiableList(leftBoxes);
    }

    /** All boxes for List B (items and gap markers), in rendered row order. */
    public List<Box> getRightBoxes() {
        return Collections.unmodifiableList(rightBoxes);
    }

    /**
     * Box at position i of the rendered List A rows. This is a position in
     * {@link #getLeftBoxes()}, not an index into {@code Matcher.getListA()} —
     * use {@link #getLeftBoxForOriginalIndex(int)} for that.
     *
     * @param i position in the rendered row list
     * @return the box at that position, or null if out of range
     */
    public Box getLeftBox(int i) {
        return (i >= 0 && i < leftBoxes.size()) ? leftBoxes.get(i) : null;
    }

    /**
     * Box at position j of the rendered List B rows. This is a position in
     * {@link #getRightBoxes()}, not an index into {@code Matcher.getListB()} —
     * use {@link #getRightBoxForOriginalIndex(int)} for that.
     *
     * @param j position in the rendered row list
     * @return the box at that position, or null if out of range
     */
    public Box getRightBox(int j) {
        return (j >= 0 && j < rightBoxes.size()) ? rightBoxes.get(j) : null;
    }

    /**
     * Finds the box that represents a given position in the original List A,
     * as returned by {@link Matcher#findMatchingIndexPairs()}. If that
     * position was collapsed into a hidden run, this returns the gap marker
     * box standing in for it, not null.
     *
     * @param originalIndex index into {@code Matcher.getListA()}
     * @return the box for that index, or null if the index was never laid out
     */
    public Box getLeftBoxForOriginalIndex(int originalIndex) {
        return leftIndexToBox.get(originalIndex);
    }

    /**
     * Finds the box that represents a given position in the original List B,
     * as returned by {@link Matcher#findMatchingIndexPairs()}. If that
     * position was collapsed into a hidden run, this returns the gap marker
     * box standing in for it, not null.
     *
     * @param originalIndex index into {@code Matcher.getListB()}
     * @return the box for that index, or null if the index was never laid out
     */
    public Box getRightBoxForOriginalIndex(int originalIndex) {
        return rightIndexToBox.get(originalIndex);
    }

    /** Height the panel needs so the tallest column fits. Call after {@link #layout}. */
    public int getPreferredHeight() {
        int rows = Math.max(rowsPerColumnLeft, rowsPerColumnRight);
        if (rows == 0) {
            return TOP_MARGIN + BOTTOM_MARGIN;
        }
        return TOP_MARGIN + rows * (currentBoxHeight + SPACING) + BOTTOM_MARGIN;
    }

    /** Width the panel needs so every column fits. Call after {@link #layout}. */
    public int getPreferredWidth() {
        return currentWidth;
    }

    public void setTitles(String leftTitle, String rightTitle) {
        this.leftTitle = leftTitle;
        this.rightTitle = rightTitle;
    }
}