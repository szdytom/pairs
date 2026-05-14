package app.pairs.view;
import static app.pairs.utils.Colors.*;

import static io.github.libsdl4j.api.keycode.SDL_Keycode.*;

import app.pairs.audio.AudioManager;

public class Button extends Background {
	private final Runnable onClick;

	public Button(Runnable onClick) {
		this(onClick, rgba(0, 0, 0, 0), rgba(0, 0, 0, 20));
	}

	public Button(Runnable onClick, int color, int hoverColor) {
		super(color, hoverColor);
		this.onClick = onClick;
		setFocusable(true);
	}

	@Override
	public boolean onEvent(Event event) {
		if (event instanceof MouseEvent me) {
			if (me.type() == Event.Type.MOUSE_PRESSED && me.button() == 1) {
				requestFocus();
				invokeClick();
				return true;
			}
		}
		if (event instanceof KeyEvent ke && ke.type() == Event.Type.KEY_PRESSED
		    && isActive()) {
			int kc = ke.keycode();
			if (kc == SDLK_RETURN || kc == SDLK_SPACE) {
				invokeClick();
				return true;
			}
		}
		return super.onEvent(event);
	}

	private void invokeClick() {
		if (onClick == null) {
			return;
		}
		onClick.run();
		Boolean mute = getProp("no-sound");
		if (mute == null || !mute) {
			AudioManager.instance().play("click", "click");
		}
	}
}
