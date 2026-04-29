package app.pairs.view;

import io.github.libsdl4j.api.render.*;

/**
 * Common interface for view components.
 *
 * <p>Uses a two-phase layout cycle:
 * <ol>
 *   <li>{@link #measure()} — bottom-up, each component reports its natural
 *       logical-pixel size without knowing its final position.</li>
 *   <li>{@link #layout(int, int, int, int)} — top-down, the parent assigns
 *       each child a rectangle (position relative to the parent, in logical
 *       pixels).</li>
 * </ol>
 *
 * <p>Coordinate systems:
 * <ol>
 *   <li><b>Local logical pixel</b> — position relative to parent, set by
 *       {@code layout()}.</li>
 *   <li><b>Global logical pixel</b> — parent's global position + local
 *       position, computed inside {@code render()}.</li>
 *   <li><b>Screen pixel</b> — global logical × {@code scale}.</li>
 * </ol>
 */
public interface ViewComponent {
	/**
	 * Updates the component state.
	 * @param deltaTimeMs time elapsed since last frame in milliseconds
	 */
	void update(long deltaTimeMs);

	/**
	 * Returns the component's natural logical size {@code {width, height}}.
	 * Called bottom-up before {@link #layout(int, int, int, int)}.
	 * <p>The returned array must not be stored in a field — it may be a
	 * shared singleton. Override this method to return a cached instance.
	 */
	default int[] measure() {
		return ZERO_SIZE;
	}

	/**
	 * Shared zero-size array for the default {@link #measure()}
	 * implementation.
	 */
	int[] ZERO_SIZE = new int[2];

	/**
	 * Assigns this component's rectangle in the parent's local logical-pixel
	 * coordinate space.
	 * @param x position relative to parent
	 * @param y position relative to parent
	 * @param w allocated width
	 * @param h allocated height
	 */
	default void layout(int x, int y, int w, int h) {}

	/**
	 * Renders the component.
	 * @param renderer the SDL_Renderer to draw to
	 * @param parentX  parent's global X in logical pixels (root passes 0)
	 * @param parentY  parent's global Y in logical pixels (root passes 0)
	 * @param scale    integer pixel scale factor
	 */
	void render(SDL_Renderer renderer, int parentX, int parentY, int scale);

	/**
	 * Releases any SDL resources (textures, surfaces) held by this component.
	 */
	default void destroy() {}
}
