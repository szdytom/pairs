package app.pairs.view;

import static app.pairs.utils.Colors.*;

import static io.github.libsdl4j.api.blendmode.SDL_BlendMode.*;
import static io.github.libsdl4j.api.render.SdlRender.*;

import io.github.libsdl4j.api.rect.SDL_Rect;
import io.github.libsdl4j.api.render.*;

public class PairCounter extends Container {
	private static final int V_PAD = 4;
	private static final int FIXED_WIDTH = 80;

	private final int total;
	private final char[] buf;
	private final int digits;
	private float progress;
	private int last = -1;

	private final TextComponent progressLabel;
	private final TextComponent stallLabel;
	private final TextComponent comboLabel;
	private final Widget progressWrap;
	private final Widget stallWrap;
	private final Widget comboWrap;
	private final FlexLayout row;

	private enum Mode { PROGRESS, STALL, COMBO }
	private Mode mode = Mode.PROGRESS;

	private final int textColor = rgb(200, 200, 200);
	private final int bgColor = rgba(180, 190, 210, 30);
	private final int fillColor = rgba(180, 190, 210, 65);
	private final int stallTextColor = rgb(255, 255, 255);
	private final int stallBgColor = rgb(200, 40, 40);
	private final int comboTextColor = rgb(255, 255, 255);
	private final int comboBgColor = rgb(180, 230, 180);

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

		this.progressLabel = new TextComponent(new String(buf), 1, textColor);
		this.stallLabel = new TextComponent("STALL", 1, stallTextColor);
		this.comboLabel = new TextComponent("Combo 2!", 1, comboTextColor);

		this.progressWrap = wrapCentered(progressLabel);
		this.stallWrap = wrapCentered(stallLabel);
		this.comboWrap = wrapCentered(comboLabel);

		this.row = new FlexLayout(FlexLayout.Direction.ROW, 0);
		row.addChild(progressWrap);
		row.addChild(stallWrap);
		row.addChild(comboWrap);
		addChild(row);
	}

	private static Widget wrapCentered(TextComponent label) {
		var align = new AlignLayout();
		align.setProp("flex-grow", 1);
		label.setProp("h-align", AlignLayout.HAlign.CENTER);
		label.setProp("v-align", AlignLayout.VAlign.CENTER);
		align.addChild(label);
		return align;
	}

	public void setProgress(int eliminated) {
		if (mode == Mode.PROGRESS && eliminated == last) {
			return;
		}
		mode = Mode.PROGRESS;
		last = eliminated;
		progressWrap.setVisible(true);
		stallWrap.setVisible(false);
		comboWrap.setVisible(false);
		int n = eliminated;
		for (int i = digits - 1; i >= 0; i--) {
			buf[i] = (char)('0' + n % 10);
			n /= 10;
		}
		progressLabel.setText(new String(buf));
		this.progress = (float)eliminated / total;
	}

	public void showStall() {
		if (mode == Mode.STALL) {
			return;
		}
		mode = Mode.STALL;
		progressWrap.setVisible(false);
		stallWrap.setVisible(true);
		comboWrap.setVisible(false);
		progress = 0;
	}

	public void showCombo(int combo) {
		mode = Mode.COMBO;
		progressWrap.setVisible(false);
		stallWrap.setVisible(false);
		comboWrap.setVisible(true);
		comboLabel.setText("Combo " + combo + "!");
		progress = 0;
	}

	@Override
	public int[] measure() {
		measuredSize[0] = FIXED_WIDTH;
		measuredSize[1] = progressLabel.measure()[1] + V_PAD * 2;
		return measuredSize;
	}

	@Override
	public void layout(int x, int y, int w, int h) {
		super.layout(x, y, w, h);
		row.layout(0, 0, layoutW, layoutH);
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

		switch (mode) {
		case STALL:
			setRenderDrawColor(renderer, stallBgColor);
			SDL_RenderFillRect(renderer, bgRect);
			break;
		case COMBO:
			setRenderDrawColor(renderer, comboBgColor);
			SDL_RenderFillRect(renderer, bgRect);
			break;
		case PROGRESS:
			setRenderDrawColor(renderer, bgColor);
			SDL_RenderFillRect(renderer, bgRect);
			int fillW = (int)(progress * layoutW) * scale;
			if (fillW > 0) {
				bgRect.w = fillW;
				setRenderDrawColor(renderer, fillColor);
				SDL_RenderFillRect(renderer, bgRect);
			}
			break;
		}

		SDL_SetRenderDrawBlendMode(renderer, SDL_BLENDMODE_NONE);

		super.render(renderer, parentX, parentY, scale);
	}
}
