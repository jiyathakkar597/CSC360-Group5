package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class ArrowRendererPreview {
    public static void main(String[] args) {
        List<String> listA = new ArrayList<>();
        List<String> listB = new ArrayList<>();

        // Test case: mostly matching, some gaps, a few unmatched items.
        // Change the loop below to try the other scenarios
        // (empty lists, no matches, everything matching, 900+ items, etc.)
        for (int i = 0; i < 60; i++) {
            listA.add("item" + i);
            boolean unmatched = (i >= 20 && i < 26) || i % 15 == 0;
            listB.add(unmatched ? "different" + i : "item" + i);
        }
        java.util.Collections.reverse(listB);

        Matcher matcher = new Matcher(listA, listB);
        BoxRenderer boxRenderer = new BoxRenderer();
        ArrowRenderer arrowRenderer = new ArrowRenderer();

        int assumedVisibleHeight = 700;
        boxRenderer.layout(matcher, 1000, assumedVisibleHeight);

        int fullWidth = boxRenderer.getPreferredWidth();
        int fullHeight = boxRenderer.getPreferredHeight();

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                boxRenderer.draw(g2);
                arrowRenderer.draw(g2, matcher, boxRenderer);
            }
        };
        MouseAdapter hoverTracker = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (arrowRenderer.setHoverPoint(e.getPoint())) {
                    panel.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (arrowRenderer.setHoverPoint(null)) {
                    panel.repaint();
                }
            }
        };
        panel.addMouseListener(hoverTracker);
        panel.addMouseMotionListener(hoverTracker);
        panel.setPreferredSize(new Dimension(fullWidth, fullHeight));

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(1000, 700));

        JFrame frame = new JFrame("ArrowRenderer Preview");
        frame.add(scrollPane);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}