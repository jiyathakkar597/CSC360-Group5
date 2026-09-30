package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the elements shared by two lists of strings and prepares the rows
 * that BoxRenderer draws for each list.
 *
 * <p>The set of common elements is computed once in the constructor, so
 * repeated calls to {@link #isCommon(String)} stay cheap even for lists with
 * thousands of items. Both lists are copied on construction, so later changes
 * to the caller's lists do not affect this object.</p>
 *
 * @author Jiya Thakkar
 */
public class Matcher {

    public static final int DEFAULT_MIN_GAP = 3;
    private static final int MIN_ALLOWED_GAP = 1;
    private static final int LARGE_LIST_SIZE = 1000;
    private static final int MATCH_INTERVAL = 20;
    private static final int LONG_STRING_LENGTH = 500;

    public interface Row {
    }

    public record ItemRow(int index, String text, boolean matched) implements Row {
    }

    public record GapRow(int startIndex, int count) implements Row {
    }

    private final List<String> listA;
    private final List<String> listB;
    private final Set<String> commonElements;

    public Matcher(List<String> listA, List<String> listB) {
        this.listA = new ArrayList<>(listA);
        this.listB = new ArrayList<>(listB);
        Set<String> common = new HashSet<>(this.listA);
        common.retainAll(new HashSet<>(this.listB));
        this.commonElements = Collections.unmodifiableSet(common);
    }

    public List<String> getListA() {
        return listA;
    }

    public List<String> getListB() {
        return listB;
    }

    public Set<String> findCommonElements() {
        return commonElements;
    }

    public boolean isCommon(String value) {
        return commonElements.contains(value);
    }

    public List<int[]> findMatchingIndexPairs() {
        Map<String, List<Integer>> positionsInB = new HashMap<>();
        for (int j = 0; j < listB.size(); j++) {
            positionsInB.computeIfAbsent(listB.get(j), key -> new ArrayList<>()).add(j);
        }
        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < listA.size(); i++) {
            List<Integer> matches = positionsInB.get(listA.get(i));
            if (matches != null) {
                for (int j : matches) {
                    pairs.add(new int[] { i, j });
                }
            }
        }
        return pairs;
    }

    public List<Row> buildRowsA() {
        return buildRows(listA, DEFAULT_MIN_GAP);
    }

    public List<Row> buildRowsB() {
        return buildRows(listB, DEFAULT_MIN_GAP);
    }

    public List<Row> buildRows(List<String> list, int minGap) {
        if (minGap < MIN_ALLOWED_GAP) {
            throw new IllegalArgumentException(
                    "minGap must be at least " + MIN_ALLOWED_GAP + ", was " + minGap);
        }
        List<Row> rows = new ArrayList<>();
        int i = 0;
        while (i < list.size()) {
            if (isCommon(list.get(i))) {
                rows.add(new ItemRow(i, list.get(i), true));
                i++;
            } else {
                int start = i;
                while (i < list.size() && !isCommon(list.get(i))) {
                    i++;
                }
                int run = i - start;
                if (run >= minGap) {
                    rows.add(new GapRow(start, run));
                } else {
                    for (int k = start; k < i; k++) {
                        rows.add(new ItemRow(k, list.get(k), false));
                    }
                }
            }
        }
        return rows;
    }

    public static void main(String[] args) {
        runTest("Basic case",
                Arrays.asList("Apple", "Banana", "Cherry", "Date", "Fig", "Grape"),
                Arrays.asList("Kiwi", "Banana", "Mango", "Fig", "Cherry", "Lemon"));

        runTest("No common elements",
                Arrays.asList("Apple", "Banana"),
                Arrays.asList("Kiwi", "Mango"));

        runTest("All common elements",
                Arrays.asList("Apple", "Banana", "Cherry"),
                Arrays.asList("Cherry", "Banana", "Apple"));

        runTest("Duplicates in List A",
                Arrays.asList("Fig", "Fig", "Grape"),
                Arrays.asList("Fig", "Kiwi"));

        runTest("Duplicates in both lists",
                Arrays.asList("Fig", "Fig"),
                Arrays.asList("Fig", "Fig", "Fig"));

        runTest("Empty List A",
                Collections.<String>emptyList(),
                Arrays.asList("Kiwi", "Mango"));

        runTest("Both lists empty",
                Collections.<String>emptyList(),
                Collections.<String>emptyList());

        runLongStringTest();
        runLargeListTest();
    }

    private static void runTest(String label, List<String> a, List<String> b) {
        System.out.println("---- " + label + " ----");
        Matcher matcher = new Matcher(a, b);
        System.out.println("List A: " + a);
        System.out.println("List B: " + b);
        System.out.println("Common elements: " + matcher.findCommonElements());
        System.out.println("Matching index pairs (A index, B index):");
        for (int[] pair : matcher.findMatchingIndexPairs()) {
            System.out.println("  A[" + pair[0] + "]=" + a.get(pair[0])
                    + "  ->  B[" + pair[1] + "]=" + b.get(pair[1]));
        }
        System.out.println("Rows for List A: " + matcher.buildRowsA());
        System.out.println("Rows for List B: " + matcher.buildRowsB());
        System.out.println();
    }

    private static void runLongStringTest() {
        System.out.println("---- Very long strings ----");
        String longMatch = "x".repeat(LONG_STRING_LENGTH);
        String longOther = "y".repeat(LONG_STRING_LENGTH);
        Matcher matcher = new Matcher(
                Arrays.asList(longMatch, "short", longOther),
                Arrays.asList("other", longMatch));
        System.out.println("Common elements: " + matcher.findCommonElements().size()
                + " (expect 1, length " + LONG_STRING_LENGTH + ")");
        System.out.println("Matching index pairs: " + matcher.findMatchingIndexPairs().size()
                + " (expect 1)");
        System.out.println();
    }

    private static void runLargeListTest() {
        List<String> first = new ArrayList<>();
        List<String> second = new ArrayList<>();
        for (int n = 0; n < LARGE_LIST_SIZE; n++) {
            first.add("item" + n);
            second.add(n % MATCH_INTERVAL == 0 ? "item" + n : "other" + n);
        }
        Matcher matcher = new Matcher(first, second);
        int expectedRows = 2 * (LARGE_LIST_SIZE / MATCH_INTERVAL);
        System.out.println("---- Large list (" + LARGE_LIST_SIZE + " items, "
                + LARGE_LIST_SIZE / MATCH_INTERVAL + " matches) ----");
        System.out.println("Rows for List A: " + matcher.buildRowsA().size()
                + " (expect " + expectedRows + ")");
        System.out.println();
    }
}
