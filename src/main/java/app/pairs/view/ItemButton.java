package app.pairs.view;

import static app.pairs.utils.Colors.*;

import io.github.libsdl4j.api.render.SDL_Texture;

public class ItemButton extends Button {
	private final String name;
	private final TextComponent label;

	public ItemButton(
		SDL_Texture icon, String name, int count, Runnable onClick
	) {
		super(onClick);
		this.name = name;
		this.label = new TextComponent(
			name + "(" + count + ")", 1,
			count > 0 ? rgb(0, 0, 0) : rgb(180, 180, 180)
		);
		this.setProp("no-sound", true);
		var align = new AlignLayout();
		align.addChild(label);
		label.setProp("v-align", AlignLayout.VAlign.CENTER);

		var flex = new FlexLayout(FlexLayout.Direction.ROW, 4, 4);
		flex.addChild(new ImageComponent(icon, 16, 16));
		flex.addChild(align);
		addChild(flex);
	}

	public void setCount(int count) {
		label.setText(name + "(" + count + ")");
		label.setColor(count > 0 ? rgb(0, 0, 0) : rgb(180, 180, 180));
	}
}
