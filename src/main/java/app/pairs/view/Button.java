package app.pairs.view;
import static app.pairs.utils.Colors.*;

import app.pairs.audio.AudioManager;

public class Button extends Background {
	private final Runnable onClick;

	public Button(Runnable onClick) {
		this(onClick, rgba(0, 0, 0, 0), rgba(0, 0, 0, 20));
	}

	public Button(Runnable onClick, int color, int hoverColor) {
		super(color, hoverColor);
		this.onClick = onClick;
	}

	@Override
	public boolean onEvent(Event event) {
		if (event instanceof MouseEvent me) {
			if (me.type() == Event.Type.MOUSE_PRESSED && me.button() == 1) {
				if (onClick != null) {
					onClick.run();
					Boolean mute = getProp("no-sound");
					if (mute == null || !mute) {
						AudioManager.instance().play("click", "click");
					}
				}
				return true;
			}
		}
		return super.onEvent(event);
	}
}
