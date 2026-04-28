The pixel font is defined as:

- Each character is an array of 12 integers, index 0 = top row, index 11 = bottom row.
- Each integer encodes a row of pixels: **bit 0 (the least significant bit / LSB) represents the leftmost pixel**, and higher bits extend to the right. A `1` bit lights a pixel, `0` leaves it dark.
- Most ASCII characters use only 5 active pixel columns (values ≤ 31). The standard render width is 6 pixels: the 5 effective bits are **left‑aligned**, and the **rightmost column (6th pixel) is always blank, acting as the inter‑character spacing**.
- A few special glyphs (e.g. `"ď"`, `"đ"`) have values >31, using extra columns for diacritics; these still follow the same LSB‑left, left‑aligned rule, with the right‑side spacing added to reach the final glyph cell width.

**Example decoder (Python)**
```python
def decode_char(rows):
    """Return a list of 12 strings, each a row of '#' (on) and '.' (off)."""
    return [''.join('#' if (val >> i) & 1 else '.'
                    for i in range(5))   # i=0 -> bit 0 (LSB, leftmost)
            for val in rows]
```
For a 6‑pixel wide rendering, simply append a `'.'` to each decoded row to account for the blank spacing column.
