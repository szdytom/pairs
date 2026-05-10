package app.pairs.view;

import static app.pairs.utils.Colors.*;

import static io.github.libsdl4j.api.blendmode.SDL_BlendMode.*;
import static io.github.libsdl4j.api.render.SdlRender.*;

import io.github.libsdl4j.api.rect.SDL_Rect;
import io.github.libsdl4j.api.render.*;

public class PairCounter extends Container {
	private static final int H_PAD = 8;
	private static final int V_PAD = 4;

	private final int total;
	private final char[] buf;
	private final int digits;
	private final TextComponent label;
	private float progress;
	private int last = -1;
	private final int textColor = rgb(200, 200, 200);
	private final int bgColor = rgba(180, 190, 210, 30);
	private final int fillColor = rgba(180, 190, 210, 65);
	private final SDL_Rect bgRect = new SDL_Rect();
	private final int[] measuredSize = new int[2];

	public PairCounter(int total) {
		this.total = total;
		String totalStr = String.valueOf(total);
		this.digits = totalStr.length();
		this.buf = new char[digits * 2 + 3];
		buf[digits] = ' ';
		buf[digits + 1] = '/';
		buf[digits + 2] = ' ';
		totalStr.getChars(0, digits, buf, digits + 3);
		for (int i = 0; i < digits; i++) {
			buf[i] = '0';
		}

		this.label = new TextComponent(new String(buf), 1, textColor);
		addChild(label);
	}

	public void setProgress(int eliminated) {
		if (eliminated == last) {
			return;
		}
		last = eliminated;
		int n = eliminated;
		for (int i = digits - 1; i >= 0; i--) {
			buf[i] = (char)('0' + n % 10);
			n /= 10;
		}
		label.setText(new String(buf));
		this.progress = (float)eliminated / total;
	}

	@Override
	public int[] measure() {
		int[] textSize = label.measure();
		measuredSize[0] = textSize[0] + H_PAD * 2;
		measuredSize[1] = textSize[1] + V_PAD * 2;
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);
		label.layout(H_PAD, V_PAD, 0, 0);
	}

	@Override
	public void render(
		SDL_Renderer renderer, int parentX, int parentY, int scale
	) {
		int globalX = parentX + layoutX;
		int globalY = parentY + layoutY;

		bgRect.x = globalX * scale;
		bgRect.y = globalY * scale;
		bgRect.w = layoutW * scale;
		bgRect.h = layoutH * scale;

		SDL_SetRenderDrawBlendMode(renderer, SDL_BLENDMODE_BLEND);
		setRenderDrawColor(renderer, bgColor);
		SDL_RenderFillRect(renderer, bgRect);

		int fillW = (int)(progress * layoutW) * scale;
		if (fillW > 0) {
			bgRect.w = fillW;
			setRenderDrawColor(renderer, fillColor);
			SDL_RenderFillRect(renderer, bgRect);
		}
		SDL_SetRenderDrawBlendMode(renderer, SDL_BLENDMODE_NONE);

		super.render(renderer, parentX, parentY, scale);
	}
}
