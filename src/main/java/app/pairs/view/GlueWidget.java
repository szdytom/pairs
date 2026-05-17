package app.pairs.view;

import io.github.libsdl4j.api.render.*;

public class GlueWidget extends Widget {
	private final int[] sz;

	public GlueWidget() {
		this(0, 0);
	}

	public GlueWidget(int w, int h) {
		sz = new int[] {w, h};
	}

	static GlueWidget flexible() {
		var g = new GlueWidget();
		g.setProp("flex-grow", 1);
		return g;
	}

	@Override
	public int[] measure() {
		return sz;
	}

	@Override
	public void render(SDL_Renderer r, int px, int py, int s) {}
}
