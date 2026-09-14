## Group Members

| Name | AU ID     |
|---|-----------|
| Jiya Thakkar | AU2420189 |
| Dhriti Sarkar | AU2420123 |
| Aangi Shah | AU2300055 |
| Heer Patel | AU2420114 |

## What This Project Is About

This project is a Java Swing application built for our Digital Graphics
and Image Processing course. It takes two lists of items as input,
identifies which elements are common to both lists, and visually
represents those relationships by drawing curved arrows connecting the
matching items across two columns.

The goal was to practice 2D graphics rendering (custom drawing with
`Graphics2D`), coordinate/layout math, and basic UI interactivity, while
also applying core data-structure concepts (sets, lists) to solve a
comparison problem.

## Concepts Used

- **Java Swing** — building the GUI window, panels, text fields, and buttons.
- **Custom 2D graphics (`Graphics2D`)** — drawing rounded rectangles,
  curves (`QuadCurve2D`), and polygons (arrowheads) directly onto a panel.
- **Set/List operations** — using `HashSet` intersection to find common
  elements between the two lists.
- **Coordinate geometry & trigonometry** — computing arrow angles and
  arrowhead points using `atan2`, `sin`, and `cos`.
- **Object-oriented design** — splitting the program into separate
  classes with single responsibilities (data, box rendering, arrow
  rendering, integration).
- **Event-driven programming** — responding to button clicks to
  re-render the visualization with new input.

## Who Did What

| Team Member | File | Contribution |
|---|---|---|
| Jiya Thakkar | `Matcher.java` | Designed the data representation for the two lists and implemented the logic to find common elements, including handling duplicate values correctly. |
| Dhriti Sarkar | `BoxRenderer.java` | Implemented the box layout and rendering for both list columns, including highlighting matched items. |
| Aangi Shah | `ArrowRenderer.java` | Implemented the curved arrow and arrowhead drawing logic connecting matched boxes between the two lists. |
| Heer Patel | `Main.java` | Integrated all components into a single application, built the input UI (text fields + Compare button), and set up the project structure/repo. |

## Project Structure

```
src/main/java/org/example/
├── Matcher.java        # data + matching logic
├── BoxRenderer.java    # box drawing + layout
├── ArrowRenderer.java  # arrow drawing
└── Main.java            # integration, UI, entry point
```

## Real-World Use Cases

The core idea behind this program — visually linking matching items
across two separate collections — shows up in many practical scenarios:

- **Database schema mapping**: When migrating data between two systems,
  developers need to see which fields in an old database table
  correspond to fields in a new one. A visual connector like this makes
  mismatches and overlaps immediately obvious.
- **Plagiarism / duplicate detection**: Comparing two documents, code
  submissions, or datasets to highlight shared phrases, functions, or
  entries, so a reviewer can quickly see what overlaps.
- **Inventory reconciliation**: Matching a warehouse's recorded stock
  list against a supplier's shipment list to flag which items were
  successfully received versus which are missing or extra.
- **Bioinformatics / gene matching**: Visualizing shared genes or
  markers between two datasets (e.g. two species or two samples) is a
  common technique in comparative genomics research.
- **Curriculum or syllabus comparison**: Universities comparing two
  course syllabi to identify overlapping topics when deciding on credit
  transfers or avoiding redundant content.
- **UI/UX diagram tools**: The same arrow-and-node visualization pattern
  underlies tools like flowchart builders, mind maps, and org charts,
  where relationships between separate groups of items need to be shown
  clearly at a glance.

In short, any situation where two sets of information need to be
compared — and the connections between them explained visually rather
than just listed in text — is a real-world application of this concept.
