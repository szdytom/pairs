package app.pairs.router;

import app.pairs.view.Blackboard;
import app.pairs.view.Event;
import app.pairs.view.Widget;

import io.github.libsdl4j.api.render.*;

public interface Page {
	default void onEnter() {}

	default void onExit() {}

	void update(long deltaTimeMs);

	void render(SDL_Renderer renderer, int scale);

	default boolean onEvent(Event event) {
		return false;
	}

	default void destroy() {}

	Widget getRoot();

	Blackboard getBlackboard();
}
