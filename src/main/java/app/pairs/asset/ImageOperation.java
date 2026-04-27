package app.pairs.asset;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.google.gson.JsonObject;

import static io.github.libsdl4j.api.pixels.SDL_PixelFormatEnum.SDL_PIXELFORMAT_ABGR8888;
import io.github.libsdl4j.api.surface.SDL_Surface;
import io.github.libsdl4j.api.surface.SdlSurface;

public class ImageOperation implements AssetOperation {
	private String id;
	private String file;

	@Override
	public String type() {
		return "image";
	}

	@Override
	public void configure(JsonObject item) {
		id(item.get("id").getAsString());
		file(item.get("file").getAsString());
	}

	@Override
	public void process(Context ctx) throws Exception {
		System.out.println("  Loading image: " + file);
		InputStream is = ctx.loader().load(file);
		BufferedImage img = ImageIO.read(is);
		is.close();

		if (img == null) {
			throw new RuntimeException("Failed to load image: " + file);
		}

		int width = img.getWidth();
		int height = img.getHeight();
		int[] argb = img.getRGB(0, 0, width, height, null, 0, width);

		// Convert ARGB to RGBA
		byte[] rgba = new byte[width * height * 4];
		for (int i = 0; i < argb.length; i++) {
			int pixel = argb[i];
			rgba[i * 4 + 0] = (byte) ((pixel >> 16) & 0xFF); // R
			rgba[i * 4 + 1] = (byte) ((pixel >> 8) & 0xFF); // G
			rgba[i * 4 + 2] = (byte) (pixel & 0xFF); // B
			rgba[i * 4 + 3] = (byte) ((pixel >> 24) & 0xFF); // A
		}

		// Create SDL surface with SDL-owned pixel memory
		SDL_Surface surface = SdlSurface.SDL_CreateRGBSurfaceWithFormat(
				0, width, height, 32, SDL_PIXELFORMAT_ABGR8888);
		if (surface == null) {
			throw new RuntimeException("Failed to create surface for: " + file);
		}
		surface.getPixels().write(0, rgba, 0, rgba.length);

		System.out.println(
				"  Loaded image: " + id + " (" + width + "x" + height + ")");
		ctx.put(id, surface);
	}

	public ImageOperation id(String id) {
		this.id = id;
		return this;
	}

	public ImageOperation file(String file) {
		this.file = file;
		return this;
	}
}
