#!/usr/bin/env python3
"""
Convert an image to ASCII art.
For each pixel:
  - print '#' if the pixel is non-transparent
  - print '.' if the pixel is transparent
Image dimensions are read from the file (no hardcoded size).
Usage: python script.py <image_path>
"""

import sys
from PIL import Image

def image_to_ascii(image_path):
    """
    Load an image, ensure it has an alpha channel,
    and generate a string of ASCII characters where
    non-transparent pixels become '#' and transparent ones become '.'.
    """
    # Open the image
    img = Image.open(image_path)

    # Convert to RGBA to reliably access alpha channel
    img = img.convert("RGBA")
    width, height = img.size
    pixels = img.load()

    # Build the output line by line
    lines = []
    for y in range(height):
        line_chars = []
        for x in range(width):
            r, g, b, a = pixels[x, y]
            # Alpha == 0 -> transparent -> '.'
            # Alpha > 0 -> non-transparent -> '#'
            line_chars.append('.' if a == 0 else '#')
        lines.append(''.join(line_chars))

    return '\n'.join(lines)

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python script.py <image_path>")
        sys.exit(1)

    path = sys.argv[1]
    try:
        ascii_art = image_to_ascii(path)
        print(ascii_art)
    except Exception as e:
        print(f"Error processing image: {e}", file=sys.stderr)
        sys.exit(1)