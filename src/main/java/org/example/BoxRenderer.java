package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;

public class BoxRenderer {

    public static final int BOX_WIDTH    = 140;
    public static final int BOX_HEIGHT   = 40;
    public static final int VERTICAL_GAP = 20;
    public static final int TOP_MARGIN   = 60;

    private static final Color BOX_COLOR_DEFAULT = new Color(224, 224, 224);
    private static final Color BOX_COLOR_MATCHED = new Color(179, 216, 255);
    private static final Color BOX_BORDER         = new Color(70, 70, 70);
    private static final Color TEXT_COLOR         = new Color(20, 20, 20);

    public static int centerY(int index) {
        return TOP_MARGIN + index * (BOX_HEIGHT + VERTICAL_GAP) + BOX_HEIGHT / 2;
    }

    public static int panelHeight(int longerListSize) {
        return TOP_MARGIN + longerListSize * (BOX_HEIGHT + VERTICAL_GAP) + 40;
    }

    public void drawList(Graphics2D g2, List<String> items, int x, String header, Matcher matcher) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.setColor(TEXT_COLOR);
        g2.drawString(header, x, TOP_MARGIN - 25);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));

        for (int i = 0; i < items.size(); i++) {
            boolean matched = matcher.isCommon(items.get(i));
            drawBox(g2, x, TOP_MARGIN + i * (BOX_HEIGHT + VERTICAL_GAP), items.get(i), matched);
        }
    }

    private void drawBox(Graphics2D g2, int x, int y, String label, boolean matched) {
        RoundRectangle2D box = new RoundRectangle2D.Float(x, y, BOX_WIDTH,

@'
package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;

public class BoxRenderer {

    public static final int BOX_WIDTH    = 140;
    public static final int BOX_HEIGHT   = 40;
    public static final int VERTICAL_GAP = 20;
    public static final int TOP_MARGIN   = 60;

    private static final Color BOX_COLOR_DEFAULT = new Color(224, 224, 224);
    private static final Color BOX_COLOR_MATCHED = new Color(179, 216, 255);
    private static final Color BOX_BORDER         = new Color(70, 70, 70);
    private static final Color TEXT_COLOR         = new Color(20, 20, 20);

    public static int centerY(int index) {
        return TOP_MARGIN + index * (BOX_HEIGHT + VERTICAL_GAP) + BOX_HEIGHT / 2;
    }

    public static int panelHeight(int longerListSize) {
        return TOP_MARGIN + longerListSize * (BOX_HEIGHT + VERTICAL_GAP) + 40;
    }

    public void drawList(Graphics2D g2, List<String> items, int x, String header, Matcher matcher) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.setColor(TEXT_COLOR);
        g2.drawString(header, x, TOP_MARGIN - 25);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));

        for (int i = 0; i < items.size(); i++) {
            boolean matched = matcher.isCommon(items.get(i));
            drawBox(g2, x, TOP_MARGIN + i * (BOX_HEIGHT + VERTICAL_GAP), items.get(i), matched);
        }
    }

    private void drawBox(Graphics2D g2, int x, int y, String label, boolean matched) {
        RoundRectangle2D box = new RoundRectangle2D.Float(x, y, BOX_WIDTH, BOX_HEIGHT, 12, 12);
        g2.setColor(matched ? BOX_COLOR_MATCHED : BOX_COLOR_DEFAULT);
        g2.fill(box);
        g2.setColor(BOX_BORDER);
        g2.draw(box);

        g2.setColor(TEXT_COLOR);
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        int textX = x + (BOX_WIDTH - textWidth) / 2;
        int textY = y + (BOX_HEIGHT + fm.getAscent()) / 2 - 2;
        g2.drawString(label, textX, textY);
    }

    public static void main(String[] args) {
        java.util.List<String> demoA = java.util.Arrays.asList("Apple", "Banana", "Cherry", "Date");
        java.util.List<String> demoB = java.util.Arrays.asList("Banana", "Kiwi", "Cherry", "Mango");
        Matcher demoMatcher = new Matcher(demoA, demoB);
        BoxRenderer renderer = new BoxRenderer();

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                renderer.drawList(g2, demoA, 80, "List A", demoMatcher);
                renderer.drawList(g2, demoB, 400, "List B", demoMatcher);
            }
        };
        panel.setPreferredSize(new Dimension(600, panelHeight(Math.max(demoA.size(), demoB.size()))));
        panel.setBackground(Color.WHITE);

        JFrame frame = new JFrame("BoxRenderer preview (boxes only, no arrows)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
