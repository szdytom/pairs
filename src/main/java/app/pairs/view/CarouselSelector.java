package app.pairs.view;

import static app.pairs.utils.Colors.*;

import java.util.function.Consumer;

public class CarouselSelector extends FlexLayout {
	private int currentIndex;
	private final Widget[] options;
	private final Consumer<Integer> onChange;

	public CarouselSelector(Widget[] options, Consumer<Integer> onChange) {
		this(options, onChange, 0);
	}

	public CarouselSelector(
		Widget[] options, Consumer<Integer> onChange, int fixedWidth
	) {
		super(Direction.ROW, 8);
		if (options == null || options.length == 0)
			throw new IllegalArgumentException(
				"options must not be null or empty"
			);
		this.options = options;
		this.currentIndex = 0;
		this.onChange = onChange != null ? onChange : i -> {};

		var prevBtn = new Button(this::prev);
		var prevAlign = new AlignLayout();
		var prevText = new TextComponent("<", 2, rgb(50, 50, 50));
		prevText.setProp("h-align", AlignLayout.HAlign.CENTER);
		prevText.setProp("v-align", AlignLayout.VAlign.CENTER);
		prevAlign.addChild(prevText);
		prevBtn.addChild(prevAlign);
		addChild(prevBtn);

		var wrapper = new AlignLayout();
		for (int i = 0; i < options.length; i++) {
			options[i].setVisible(i == currentIndex);
			wrapper.addChild(options[i]);
		}
		wrapper.setProp("flex-grow", 1);
		addChild(wrapper);

		var nextBtn = new Button(this::next);
		var nextAlign = new AlignLayout();
		var nextText = new TextComponent(">", 2, rgb(50, 50, 50));
		nextText.setProp("h-align", AlignLayout.HAlign.CENTER);
		nextText.setProp("v-align", AlignLayout.VAlign.CENTER);
		nextAlign.addChild(nextText);
		nextBtn.addChild(nextAlign);
		addChild(nextBtn);

		if (fixedWidth > 0) {
			int[] ps = prevBtn.measure();
			int[] ns = nextBtn.measure();
			int target = fixedWidth - ps[0] - ns[0] - 16;
			if (target > 0)
				wrapper.addChild(new GlueWidget(target, 0));
		}
	}

	public int getIndex() {
		return currentIndex;
	}

	private void prev() {
		options[currentIndex].setVisible(false);
		currentIndex = (currentIndex - 1 + options.length) % options.length;
		options[currentIndex].setVisible(true);
		onChange.accept(currentIndex);
	}

	private void next() {
		options[currentIndex].setVisible(false);
		currentIndex = (currentIndex + 1) % options.length;
		options[currentIndex].setVisible(true);
		onChange.accept(currentIndex);
	}
}
