package org.example;

import java.util.*;

public class Matcher {

    private final List<String> listA;
    private final List<String> listB;

    public Matcher(List<String> listA, List<String> listB) {
        this.listA = listA;
        this.listB = listB;
    }

    public List<String> getListA() {
        return listA;
    }

    public List<String> getListB() {
        return listB;
    }

    public Set<String> findCommonElements() {
        Set<String> setA = new HashSet<>(listA);
        Set<String> setB = new HashSet<>(listB);
        setA.retainAll(setB);
        return setA;
    }

    public boolean isCommon(String value) {
        return findCommonElements().contains(value);
    }

    public List<int[]> findMatchingIndexPairs() {
        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < listA.size(); i++) {
            String value = listA.get(i);
            for (int j = 0; j < listB.size(); j++) {
                if (listB.get(j).equals(value)) {
                    pairs.add(new int[] { i, j });
                }
            }
        }
        return pairs;
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

        runTest("Empty lists",
            Collections.emptyList(),
            Arrays.asList("Kiwi", "Mango"));
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
        System.out.println();
    }
}
