UI Layout Guide
===============

This document covers the available layout tools and how to compose them to
build UI.  Read [ui-guidelines.md](ui-guidelines.md) for overall UI conventions,
and [ui-components.md](ui-components.md) for the detailed API of each component.

The Golden Rule: **Custom `measure()` is always the last resort.**  Before
writing a custom `measure()` override, stop and ask whether you can achieve the
layout by composing existing containers (`FlexLayout`, `AlignLayout`,
`GridLayout`) with `GlueWidget` spacers and the `flex-grow` property.  In
practice, nearly every layout can be expressed as a tree of these primitives.

---

## Overview of Layout Tools

### AlignLayout

Positions each child independently within the available space using alignment
properties.  Children can overlap (later children have higher z-index).

**Use when:** you need to position children at specific edges/corners of a
region, or center a single child within its parent.

Supports `h-padding` / `v-padding` props (ignored when alignment is `CENTER`).

```java
var align = new AlignLayout();

var title = new TextComponent("Title", 3, rgb(30, 30, 30));
title.setProp("h-align", AlignLayout.HAlign.CENTER);
title.setProp("v-align", AlignLayout.VAlign.TOP);
align.addChild(title);

var closeBtn = makeButton("×");
closeBtn.setProp("h-align", AlignLayout.HAlign.RIGHT);
closeBtn.setProp("v-align", AlignLayout.VAlign.TOP);
align.addChild(closeBtn);
```

### FlexLayout

Lays out children sequentially in a row or column with uniform gap.
Supports `flex-grow` (int prop on children) to distribute remaining space.

**Use when:** stacking items in one dimension with spacing between them, or
when you need flexible space distribution.

```java
// Column: items stacked vertically
var column = new FlexLayout(FlexLayout.Direction.COLUMN, 8);
column.addChild(title);
column.addChild(description);
column.addChild(actionBtn);

// Row: items arranged horizontally, wrapper fills remaining width
var row = new FlexLayout(FlexLayout.Direction.ROW, 4);
row.addChild(icon);
var wrapper = new AlignLayout();
wrapper.setProp("flex-grow", 1);
wrapper.addChild(label);
row.addChild(wrapper);
row.addChild(deleteBtn);
```

#### flex-grow

Children with `flex-grow` receive a share of the remaining space on the main
axis.  The value is a relative weight (like CSS `flex-grow`):

```java
var row = new FlexLayout(FlexLayout.Direction.ROW, 0);

var spacer = new GlueWidget();    // measures 0×0
spacer.setProp("flex-grow", 1);   // takes all remaining space
row.addChild(spacer);             // → pushes children apart

row.addChild(leftBtn);
row.addChild(rightBtn);
```

Remaining space = allocated size − sum of natural sizes − gaps.  It is
computed during `layout()`, so `measure()` always reports natural sizes.

### GridLayout

Places children left-to-right, top-to-bottom in a fixed-column grid with
uniform horizontal/vertical gaps.  All cells share the same size (the maximum
child size).

**Use when:** displaying tabular data or a grid of uniform items (e.g., key-
value stats, stage selection cards).

```java
var grid = new GridLayout(2, 8, 0, 2); // 2 columns, gapX=8, gapY=0, padding=2
grid.addChild(new TextComponent("Time", 2, COLOR));
grid.addChild(new TextComponent("--:--", 2, COLOR));
grid.addChild(new TextComponent("Score", 2, COLOR));
grid.addChild(new TextComponent("0", 2, COLOR));
```

When the parent allocates `w > 0 && h > 0`, cells are sized to fill the
available space; otherwise they use the natural max child size.

### ScrollListLayout

Lays out children vertically (like `FlexLayout(COLUMN)`) but clips to its
allocated viewport and provides scrolling with a scrollbar overlay.  Children
beyond the viewport are not rendered.

**Use when:** you have a dynamic list that may overflow the available space.

```java
var list = new ScrollListLayout(0, 0); // gap=0, padding=0
for (Entry e : entries) {
    list.addChild(new EntryRow(e));
}
```

By default `measure()` returns the full content height.  When placed inside a
`FlexLayout` with `flex-grow`, this prevents the parent from distributing
remaining space correctly.  Use `HeightStrategy.FILL` to make `measure()`
report only padding on the main axis, allowing the parent to fill the
viewport:

```java
var column = new FlexLayout(FlexLayout.Direction.COLUMN, 0);
column.addChild(title);
var list = new ScrollListLayout(0, 0, ScrollListLayout.HeightStrategy.FILL);
list.setProp("flex-grow", 1);
list.addChild(new TextComponent("entry 1", 2, COLOR));
list.addChild(new TextComponent("entry 2", 2, COLOR));
column.addChild(list);
```

### GlueWidget

An invisible placeholder that takes up space.  Its measured size is `(w, h)`.
With `flex-grow`, it can serve as an elastic spacer.

```java
// Fixed-size spacer
new GlueWidget(16, 0);   // 16px wide, 0px tall — adds horizontal gap

// Elastic spacer: fills all remaining space in a FlexLayout
var spacer = new GlueWidget();
spacer.setProp("flex-grow", 1);
```

---

## Common Composition Patterns

### 1. Full-page layout: centered column

The most common page pattern: an `AlignLayout` root centers a single
`FlexLayout(COLUMN)` child.  Everything stacks vertically.

```
AlignLayout (root)
  └── FlexLayout(COLUMN, gap=12)
        ├── title
        ├── content  (could be GridLayout, FlexLayout, etc.)
        └── buttonRow (FlexLayout ROW)
```

```java
var column = new FlexLayout(FlexLayout.Direction.COLUMN, 12);
column.addChild(title);
column.addChild(content);
column.addChild(buttonRow);
column.setProp("h-align", AlignLayout.HAlign.CENTER);
column.setProp("v-align", AlignLayout.VAlign.CENTER);

var rootAlign = new AlignLayout();
rootAlign.addChild(column);
```

### 2. Button row at the bottom

A `FlexLayout(ROW)` inside the column, centered.

```
FlexLayout(COLUMN)
  └── FlexLayout(ROW, gap=8)   ← buttonRow
        ├── cancelBtn
        └── confirmBtn
```

```java
var buttonRow = new FlexLayout(FlexLayout.Direction.ROW, 8);
buttonRow.addChild(cancelBtn);
buttonRow.addChild(confirmBtn);
buttonRow.setProp("h-align", AlignLayout.HAlign.CENTER);
column.addChild(buttonRow);
```

### 3. Text + icon in a row

Wrap text in `AlignLayout` for vertical centering, then place in a
`FlexLayout(ROW)` alongside an icon.

```
FlexLayout(ROW, gap=4)
  ├── ImageComponent (icon)
  └── AlignLayout
        └── TextComponent (label, v-align=CENTER)
```

```java
var row = new FlexLayout(FlexLayout.Direction.ROW, 4);

var iconImg = new ImageComponent(iconTex, 16, 16);
row.addChild(iconImg);

var labelAlign = new AlignLayout();
label.setProp("v-align", AlignLayout.VAlign.CENTER);
labelAlign.addChild(label);
row.addChild(labelAlign);
```

### 4. Label-value grid (stats panel)

`GridLayout(2, gapX, gapY)` for key-value data, placed inside a column.

```java
var grid = new GridLayout(2, 8, 0, 2);
addRow(grid, "Round", "5");
addRow(grid, "Time", "01:23.45");
addRow(grid, "Score", "1000");

// Helper:
private static void addRow(GridLayout grid, String label, String value) {
    grid.addChild(new TextComponent(label, 1, LABEL_COLOR));
    grid.addChild(new TextComponent(value, 1, VALUE_COLOR));
}
```

### 5. Fixed-width carousel with spacers

Use `GlueWidget` inside an `AlignLayout` wrapper to enforce a minimum measure,
keeping button positions stable.

```java
// Inside CarouselSelector constructor:
var wrapper = new AlignLayout();
wrapper.setProp("flex-grow", 1);

// Add spacer so wrapper always measures at least (target, 0)
if (fixedWidth > 0) {
    int target = fixedWidth - prevBtnWidth - nextBtnWidth - gapTotal;
    if (target > 0)
        wrapper.addChild(new GlueWidget(target, 0));
}
wrapper.addChild(optionWidget);
```

### 6. Elastic spacer pushing content apart

`GlueWidget()` (0×0) with `flex-grow: 1` absorbs all remaining space in a
`FlexLayout`.

```
FlexLayout(ROW)
  ├── AlignLayout (flex-grow=1 = elastic spacer)
  │     └── (no visible children — just takes up space)
  └── Button("Login")
```

```java
var row = new FlexLayout(FlexLayout.Direction.ROW, 0);
var spacer = new AlignLayout();
spacer.setProp("flex-grow", 1);
row.addChild(spacer);
row.addChild(loginBtn);
```

This pushes `loginBtn` to the right edge of whatever width the parent provides.

### 7. Scrollable list

`ScrollListLayout` stacks children vertically and clips to its viewport.
Use it when items may overflow the available height.

```
AlignLayout (root)
  └── FlexLayout(COLUMN)
        ├── title
        └── ScrollListLayout(gap=0, padding=0)
              ├── OpLogEntry (FlexLayout ROW)
              ├── OpLogEntry
              └── OpLogEntry ...
```

```java
var list = new ScrollListLayout(0, 0);
for (Op op : ops) {
    list.addChild(new OpLogEntry(/* ... */));
}
```

### 8. Overlapping elements (toolbar on top of content)

Add multiple children to an `AlignLayout`.  Later children render on top.

```java
var root = new AlignLayout();
root.addChild(contentView);          // full-screen content
root.addChild(topBar);               // toolbar overlay
root.addChild(overlayText);          // centered text overlay
```

Each child can have its own alignment:
```java
topBar.setProp("v-align", AlignLayout.VAlign.TOP);
overlayText.setProp("h-align", AlignLayout.HAlign.CENTER);
overlayText.setProp("v-align", AlignLayout.VAlign.CENTER);
```

---

## Visual Cheat Sheet

```
FlexLayout(ROW, gap)                         FlexLayout(COLUMN, gap)
┌──────────────────────────────┐             ┌──────────────────────────────┐
│ [child]  gap  [child]  gap  [child]        │ [child]                      │
│                                            │                              │
│ cross-axis = max child height              │ gap                          │
│                                            │                              │
│ flex-grow distributes remaining width      │ [child]                      │
│                                            │                              │
│                                            │ cross-axis = max child width │
│                                            │                              │
│                                            │ flex-grow distributes        │
│                                            │ remaining height             │
└──────────────────────────────┘             └──────────────────────────────┘

AlignLayout                               GridLayout(2 cols)
┌──────────────────────────────┐          ┌──────────┬──────────┐
│ [child LEFT, TOP]            │          │ child    │ child    │
│                              │          ├──────────┼──────────┤
│            [child CENTER]    │          │ child    │ child    │
│                              │          └──────────┴──────────┘
│                [child RIGHT, │          cellW = max child width
│                          BOTTOM]        cellH = max child height
└──────────────────────────────┘

ScrollListLayout
┌──────────────────────────────┐
│ visible child                │ ← viewport clips here
│ visible child                │
├──────────────────────────────┤ ← scrollbar overlay
│ (scrolled content...)        │
│ scrollbar thumb              │
└──────────────────────────────┘
```

---

## Choosing the Right Container

| Goal | Tool |
|------|------|
| Stack items vertically | `FlexLayout(COLUMN, gap)` |
| Arrange items horizontally | `FlexLayout(ROW, gap)` |
| Distribute remaining space | `flex-grow` prop on children |
| Center a single child | `AlignLayout` with `HAlign.CENTER` + `VAlign.CENTER` |
| Pin to edges (like a toolbar) | `AlignLayout` with alignment props per child |
| Create elastic whitespace | `GlueWidget()` with `flex-grow: 1` |
| Fixed-size spacer | `GlueWidget(w, h)` |
| Grid of uniform cells | `GridLayout(columns, gapX, gapY)` |
| Label-value data rows | `GridLayout(2, gapX, gapY)` |
| Scrollable list | `ScrollListLayout(gap, padding)` |
| Carousel (cycle options) | `CarouselSelector(count, optionFactory, onChange, width?, wrap?, prevContent, nextContent)` |

---

## When You Really Must Write Custom `measure()`

Custom `measure()` is needed when:

1. **Non-rectangular layout** — children positioned along a curved path,
   radially, or with complex constraints.

2. **Aspect-ratio-aware sizing** — a component that must maintain a specific
   aspect ratio based on the parent's allocated size.  (But consider whether
   the parent can simply allocate the right size.)

3. **Layout primitives that belong in this codebase** — if you find yourself
   writing the same layout logic in three places, extract it as a new
   `Container` subclass with its own `measure()`/`layout()`, write tests,
   document it, and add it to this guide.

In all other cases: **compose, don't custom measure**.  If measuring options
differently is the problem (e.g., a carousel whose width shifts), fix it with
`GlueWidget` spacers or a `fixedWidth` parameter — don't override `measure()`.
