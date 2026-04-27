package app.pairs.view;

import io.github.libsdl4j.api.render.SDL_Renderer;

/**
 * Common interface for view components.
 */
public interface ViewComponent {
	/**
	 * Updates the component state.
	 * @param deltaTimeMs time elapsed since last frame in milliseconds
	 */
	void update(long deltaTimeMs);

	/**
	 * Renders the component to the given renderer.
	 * @param renderer the SDL_Renderer to draw to
	 * @param scale integer pixel scale factor for pixel-perfect rendering
	 */
	void render(SDL_Renderer renderer, int scale);

	/**
	 * Releases any SDL resources (textures, surfaces) held by this component.
	 */
	default void destroy() {}
}
