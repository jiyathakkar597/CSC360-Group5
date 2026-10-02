
package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * Application entry point for the List Match Visualizer.
 * Connects the Matcher, BoxRenderer, and ArrowRenderer components
 * and displays the result in a scrollable window.
 *
 * @author Heer Patel
 */
public class Main {

    private static final int PANEL_WIDTH = 1200;
    private static final int ASSUMED_VISIBLE_HEIGHT = 700;
    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 700;

    /**
     * Starts the List Match Visualizer application.
     *
     * @param args unused command-line arguments
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            List<String> listA = buildSampleListA();
            List<String> listB = buildSampleListB();
            Matcher matcher = new Matcher(listA, listB);

            BoxRenderer boxRenderer = new BoxRenderer();
            ArrowRenderer arrowRenderer = new ArrowRenderer();

            boxRenderer.layout(
                    matcher,
                    PANEL_WIDTH,
                    ASSUMED_VISIBLE_HEIGHT
            );

            boxRenderer.setTitles("List A", "List B");

            int fullWidth = boxRenderer.getPreferredWidth();
            int fullHeight = boxRenderer.getPreferredHeight();

            JPanel drawingPanel = createDrawingPanel(
                    matcher,
                    boxRenderer,
                    arrowRenderer,
                    fullWidth,
                    fullHeight
            );

            JScrollPane scrollPane = new JScrollPane(drawingPanel);
            scrollPane.setPreferredSize(
                    new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT)
            );

            JFrame frame = new JFrame("List Match Visualizer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(scrollPane);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /**
     * Creates the panel that paints boxes and connecting arrows.
     *
     * @param matcher       the matcher containing the two lists
     * @param boxRenderer   the renderer that draws the boxes
     * @param arrowRenderer the renderer that draws matching arrows
     * @param width         the preferred panel width
     * @param height        the preferred panel height
     * @return the configured drawing panel
     */
    private static JPanel createDrawingPanel(
            Matcher matcher,
            BoxRenderer boxRenderer,
            ArrowRenderer arrowRenderer,
            int width,
            int height) {

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    boxRenderer.draw(g2);
                    arrowRenderer.draw(g2, matcher, boxRenderer);
                } finally {
                    g2.dispose();
                }
            }
        };

        panel.setPreferredSize(new Dimension(width, height));
        return panel;
    }

    /**
     * Builds sample data for List A.
     *
     * @return a list of sample strings
     */
    private static List<String> buildSampleListA() {
        List<String> list = new ArrayList<>();

        for (int i = 0; i < 60; i++) {
            list.add("item" + i);
        }

        return list;
    }

    private static List<String> buildSampleListB() {
        List<String> list = new ArrayList<>();

        for (int i = 0; i < 60; i++) {
            if (i % 10 == 0) {
                list.add("different" + i);
            } else {
                list.add("item" + i);
            }
        }

        return list;
    }
}