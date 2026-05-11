package app.pairs.asset;

import app.pairs.map.Shape;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;

import com.google.gson.JsonObject;

public class ShapeOperation implements AssetOperation {
	private String file;

	@Override
	public String type() {
		return "shape";
	}

	@Override
	public void configure(JsonObject item) {
		file = item.get("file").getAsString();
	}

	@Override
	public void process(Context ctx) throws Exception {
		System.out.println("  Loading shape: " + file);
		BufferedImage img;
		try (InputStream is = ctx.loader().load(file)) {
			img = ImageIO.read(is);
		}

		if (img == null) {
			throw new RuntimeException("Failed to load shape image: " + file);
		}

		int width = img.getWidth();
		int height = img.getHeight();
		int[] argb = img.getRGB(0, 0, width, height, null, 0, width);

		int[][] grid = new int[height][width];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int alpha = (argb[y * width + x] >> 24) & 0xFF;
				if (alpha != 0 && alpha != 255) {
					throw new IllegalArgumentException(
						"Pixel (" + x + "," + y + ") in \"" + file
						+ "\" has alpha=" + alpha
						+ "; shape PNGs must use only fully-transparent"
						+ " (alpha=0) or fully-opaque (alpha=255) pixels"
					);
				}
				grid[y][x] = (alpha == 0) ? -1 : 0;
			}
		}

		System.out.println(
			"  Loaded shape: " + ctx.id() + " (" + width + "x" + height + ")"
		);
		ctx.put(ctx.id(), new Shape(grid));
	}
}
