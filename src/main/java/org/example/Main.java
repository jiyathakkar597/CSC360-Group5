
package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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

            DialogResult input = showCustomListDialog();
            if (input != null) {
                List<String> parsedA = parseList(input.listAText());
                List<String> parsedB = parseList(input.listBText());

                if (!parsedA.isEmpty()) {
                    listA = parsedA;
                }
                if (!parsedB.isEmpty()) {
                    listB = parsedB;
                }
            }
            final int minGap = (input != null) ? input.minGap() : Matcher.DEFAULT_MIN_GAP;

            Matcher matcher = new Matcher(listA, listB);

            BoxRenderer boxRenderer = new BoxRenderer();
            ArrowRenderer arrowRenderer = new ArrowRenderer();

            boxRenderer.layout(
                    matcher,
                    PANEL_WIDTH,
                    ASSUMED_VISIBLE_HEIGHT,
                    minGap
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

            // Re-lays out the boxes against the viewport's real size whenever the
            // window is resized, so dragging it wider genuinely adds columns and
            // cuts down vertical scrolling instead of being stuck with whatever
            // column count the initial assumed size produced. The resize events
            // fire rapidly while dragging, so a short debounce timer waits for
            // the dragging to pause before doing the (relatively expensive)
            // relayout, rather than running it on every intermediate event.
            Timer relayoutTimer = new Timer(150, null);
            relayoutTimer.setRepeats(false);
            relayoutTimer.addActionListener(e -> {
                Dimension viewportSize = scrollPane.getViewport().getSize();
                if (viewportSize.width <= 0 || viewportSize.height <= 0) {
                    return;
                }
                boxRenderer.layout(matcher, viewportSize.width, viewportSize.height, minGap);
                drawingPanel.setPreferredSize(
                        new Dimension(boxRenderer.getPreferredWidth(), boxRenderer.getPreferredHeight())
                );
                drawingPanel.revalidate();
                drawingPanel.repaint();
            });
            scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    relayoutTimer.restart();
                }
            });

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
     * Holds the values collected from {@link #showCustomListDialog()}.
     *
     * @param listAText raw text entered for List A
     * @param listBText raw text entered for List B
     * @param minGap    minimum run of consecutive non-matching items to
     *                  collapse into a single placeholder box
     */
    private record DialogResult(String listAText, String listBText, int minGap) {
    }

    /**
     * Prompts the user to enter their own List A and List B values, plus how
     * aggressively runs of non-matching items should be collapsed. Leaving
     * either text area blank falls back to the sample data for that list.
     *
     * @return the entered values, or {@code null} if the user cancelled the dialog
     */
    private static DialogResult showCustomListDialog() {
        JTextArea listAField = new JTextArea(10, 20);
        JTextArea listBField = new JTextArea(10, 20);

        JPanel listsPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        listsPanel.add(createLabeledTextArea("List A:", listAField));
        listsPanel.add(createLabeledTextArea("List B:", listBField));

        JSpinner minGapSpinner = new JSpinner(
                new SpinnerNumberModel(Matcher.DEFAULT_MIN_GAP, 1, 100, 1)
        );
        JPanel minGapPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        minGapPanel.add(new JLabel("Collapse runs of at least this many consecutive non-matches:"));
        minGapPanel.add(minGapSpinner);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(listsPanel, BorderLayout.CENTER);
        panel.add(minGapPanel, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Enter Custom Lists (one item per line or comma-separated; leave blank for sample data)",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        return new DialogResult(
                listAField.getText(),
                listBField.getText(),
                (Integer) minGapSpinner.getValue()
        );
    }

    /**
     * Wraps a text area with a label above it inside a scroll pane.
     *
     * @param label    the label describing the text area
     * @param textArea the text area to wrap
     * @return a panel containing the label and scrollable text area
     */
    private static JPanel createLabeledTextArea(String label, JTextArea textArea) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Parses user-entered text into a list of trimmed, non-empty items.
     * Items may be separated by commas, newlines, or both.
     *
     * @param text the raw text entered by the user
     * @return the parsed list of items, empty if {@code text} has no items
     */
    private static List<String> parseList(String text) {
        List<String> result = new ArrayList<>();

        if (text == null) {
            return result;
        }

        for (String token : text.split("[,\\n]+")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }

        return result;
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