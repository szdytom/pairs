package app.pairs.view;

import static app.pairs.utils.Colors.*;

import static io.github.libsdl4j.api.blendmode.SDL_BlendMode.*;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.*;
import static io.github.libsdl4j.api.render.SdlRender.*;

import app.pairs.asset.AssetManager;
import app.pairs.asset.BitmapFont;

import io.github.libsdl4j.api.rect.SDL_Rect;
import io.github.libsdl4j.api.render.*;

public class TextField extends Widget {
	public enum InputType { TEXT, PASSWORD }

	private static final long BLINK_INTERVAL = 530;
	private static final int PAD = 2;

	private final BitmapFont font;
	private final int size;
	private final InputType inputType;
	private final int textColor;
	private final int bgColor;
	private final int cursorColor;
	private final int focusColor;
	private final StringBuilder text = new StringBuilder();
	private int maxLength;
	private int cursorPos;
	private long blinkTimer;
	private boolean cursorVisible;

	private final int[] measuredSize = new int[2];
	private final SDL_Rect bgRect = new SDL_Rect();
	private final SDL_Rect cursorRect = new SDL_Rect();
	private String cachedDisplay;
	private boolean displayDirty = true;

	public TextField(
		int size, int textColor, int bgColor, int cursorColor, int focusColor
	) {
		this(size, textColor, bgColor, cursorColor, focusColor, InputType.TEXT);
	}

	public TextField(
		int size, int textColor, int bgColor, int cursorColor, int focusColor,
		InputType inputType
	) {
		this.font = AssetManager.instance().get("monogram/font");
		this.size = size;
		this.textColor = textColor;
		this.bgColor = bgColor;
		this.cursorColor = cursorColor;
		this.focusColor = focusColor;
		this.inputType = inputType;
		setFocusable(true);
	}

	public void setMaxLength(int n) {
		this.maxLength = n;
	}

	public void setText(String s) {
		text.setLength(0);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == ' ') {
				text.append(c);
			}
		}
		if (maxLength > 0 && text.length() > maxLength) {
			text.setLength(maxLength);
		}
		cursorPos = text.length();
		displayDirty = true;
	}

	public String text() {
		return text.toString();
	}

	private String displayText() {
		if (!displayDirty) {
			return cachedDisplay;
		}
		if (inputType == InputType.PASSWORD) {
			cachedDisplay = "*".repeat(text.length());
		} else {
			cachedDisplay = text.toString();
		}
		displayDirty = false;
		return cachedDisplay;
	}

	private void markDisplayDirty() {
		displayDirty = true;
	}

	@Override
	public int[] measure() {
		int charW = maxLength > 0
			? maxLength * BitmapFont.DEFAULT_ADVANCE * size
			: 0;
		int tw = BitmapFontRenderer.measureText(font, displayText(), size);
		if (tw < 0) {
			tw = 0;
		}
		measuredSize[0] = Math.max(charW, tw) + 2 * PAD * size;
		measuredSize[1] = BitmapFont.GLYPH_HEIGHT * size + 2 * PAD * size;
		return measuredSize;
	}

	@Override
	public void update(long deltaTimeMs) {
		if (isActive()) {
			blinkTimer += deltaTimeMs;
			if (blinkTimer >= BLINK_INTERVAL) {
				cursorVisible = !cursorVisible;
				blinkTimer -= BLINK_INTERVAL;
			}
		} else {
			cursorVisible = false;
			blinkTimer = 0;
		}
	}

	@Override
	public void render(
		SDL_Renderer renderer, int parentX, int parentY, int scale
	) {
		int gx = (parentX + layoutX) * scale;
		int gy = (parentY + layoutY) * scale;
		int gw = layoutW * scale;
		int gh = layoutH * scale;

		SDL_SetRenderDrawBlendMode(renderer, SDL_BLENDMODE_BLEND);

		setRenderDrawColor(renderer, isActive() ? focusColor : bgColor);
		bgRect.x = gx;
		bgRect.y = gy;
		bgRect.w = gw;
		bgRect.h = gh;
		SDL_RenderFillRect(renderer, bgRect);

		int tx = parentX + layoutX + PAD * size;
		int ty = parentY + layoutY + PAD * size;

		BitmapFontRenderer.renderText(
			renderer, font, displayText(), tx, ty, size, scale, textColor
		);

		if (cursorVisible) {
			String prefix = text.substring(0, cursorPos);
			int cx = tx + BitmapFontRenderer.measureText(font, prefix, size);
			int cw = 2;

			cursorRect.x = cx * scale;
			cursorRect.y = (ty)*scale;
			cursorRect.w = cw * scale;
			cursorRect.h = BitmapFont.GLYPH_HEIGHT * size * scale;

			setRenderDrawColor(renderer, cursorColor);
			SDL_RenderFillRect(renderer, cursorRect);
		}

		SDL_SetRenderDrawBlendMode(renderer, SDL_BLENDMODE_NONE);
	}

	@Override
	public boolean onEvent(Event event) {
		if (event instanceof MouseEvent me) {
			if (me.type() == Event.Type.MOUSE_PRESSED && me.button() == 1) {
				requestFocus();
				cursorPos = xToCursor(me.x());
				return true;
			}
		}
		if (event instanceof KeyEvent ke && ke.type() == Event.Type.KEY_PRESSED
		    && isActive()) {
			int kc = ke.keycode();
			if (kc == SDLK_LEFT) {
				if (cursorPos > 0) {
					cursorPos--;
				}
				return true;
			}
			if (kc == SDLK_RIGHT) {
				if (cursorPos < text.length()) {
					cursorPos++;
				}
				return true;
			}
			if (kc == SDLK_HOME) {
				cursorPos = 0;
				return true;
			}
			if (kc == SDLK_END) {
				cursorPos = text.length();
				return true;
			}
			if (kc == SDLK_BACKSPACE) {
				if (cursorPos > 0) {
					text.deleteCharAt(cursorPos - 1);
					cursorPos--;
					markDisplayDirty();
				}
				return true;
			}
			if (kc == SDLK_DELETE) {
				if (cursorPos < text.length()) {
					text.deleteCharAt(cursorPos);
					markDisplayDirty();
				}
				return true;
			}
			if (maxLength > 0 && text.length() >= maxLength) {
				return false;
			}
			if (kc >= SDLK_A && kc <= SDLK_Z) {
				char c = (char)('A' + (kc - SDLK_A));
				text.insert(cursorPos, c);
				cursorPos++;
				markDisplayDirty();
				return true;
			}
			if (kc >= SDLK_0 && kc <= SDLK_9) {
				text.insert(cursorPos, (char)kc);
				cursorPos++;
				markDisplayDirty();
				return true;
			}
			if (kc == SDLK_SPACE) {
				text.insert(cursorPos, ' ');
				cursorPos++;
				markDisplayDirty();
				return true;
			}
			return false;
		}
		return super.onEvent(event);
	}

	private int xToCursor(int globalX) {
		String d = displayText();
		int tx = globalX() + PAD * size;
		if (globalX <= tx) {
			return 0;
		}
		int rel = globalX - tx;
		int acc = 0;
		for (int i = 0; i < d.length(); i++) {
			int w = font.getGlyphWidth(d.charAt(i)) * size;
			if (rel < acc + w / 2) {
				return i;
			}
			acc += w;
		}
		return d.length();
	}

	private int globalX() {
		int x = layoutX;
		Widget p = parent;
		while (p != null) {
			x += p.layoutX;
			p = p.parent;
		}
		return x;
	}

	@Override
	protected void onActiveChanged() {
		blinkTimer = 0;
		cursorVisible = isActive();
	}
}
