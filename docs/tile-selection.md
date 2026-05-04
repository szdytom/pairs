# Tile Selection (`TileSelectionPolicy`)

Picks `count` tile types from the registry to populate one game's palette.
Two orthogonal knobs drive every difficulty:

| Knob | Values |
|---|---|
| `includeSlabs` | true / false |
| `spread` | `FREE` · `NO_DUPLICATES` · `PREFER_DUPLICATES` |

The output is a 1-based `int[count + 1]` of registry numeric IDs that
`SubsetTilemapFactory` uses as a remap table.

## How `select()` runs end-to-end

1. **`pickSlabs`** — if `includeSlabs`, gather every tile whose group is a
   slab, shuffle, take up to `MAX_SLABS = 3`. Otherwise empty.
2. **`buildNonSlabPool`** — walk every registered tile, skip slabs, attach
   each remaining tile to its group (or `null` for ungrouped). Result is a
   `List<Candidate>`.
3. **Spread**, applied to the non-slab pool, requesting `count - slabPicks`:
   - `FREE` — `Collections.shuffle`, take prefix.
   - `NO_DUPLICATES` — bucket by group, draw one random representative from
     each bucket (ungrouped each count as their own bucket), shuffle reps,
     take prefix. Cap = number of distinct groups + ungrouped.
   - `PREFER_DUPLICATES` — bucket the same way, sort buckets by size desc
     (with random tie-breaks), drain bucket-by-bucket until full; spill into
     ungrouped if still short.
4. **Combine** — concatenate non-slab picks + slab picks, shuffle, copy into
   the 1-based output array. Fewer than `count` available → throws.

```
selectFor(registry, groups, count, rnd)
  └─ pickSlabs ─┐
                ├─ shuffle + slice → out[1..count]
  └─ buildNonSlabPool ─ pickFree / pickNoDuplicates / pickPreferDuplicates ─┘
```

## Reflection: can it be simpler?

The current implementation carries three parallel data shapes:

- a `Candidate(numericId, TileGroup)` wrapper class,
- a `List<Candidate>` "pool",
- a per-strategy `Map<TileGroup, List<Candidate>>` plus a sibling
  `List<Candidate> ungrouped`.

Two of the three strategies (`NO_DUPLICATES`, `PREFER_DUPLICATES`) rebuild
that map from the pool independently — the same partition computed twice in
two near-identical loops.

A leaner shape would be a single `Map<TileGroup, List<Integer>>` built once
at pool time, with the `null` key holding ungrouped tile IDs. Then:

- `FREE` — flatten `map.values()` into one list, shuffle, take prefix.
- `NO_DUPLICATES` — for each non-null bucket pick one element at random;
  add every entry from the null bucket; shuffle, take prefix.
- `PREFER_DUPLICATES` — sort non-null buckets by size desc, drain; then
  drain the null bucket.

That removes the `Candidate` class entirely (we never need the group label
once the bucket is known) and the duplicated bucketing loop. Behaviour is
preserved: every strategy already treats `Candidate.group` as a partition
key only and never reads it back after bucketing.

The current code is not wrong — just slightly over-structured for what is
really a "partition by group, then sample" job.
