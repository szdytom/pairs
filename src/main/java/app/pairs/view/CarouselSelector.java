package app.pairs.view;

import static app.pairs.utils.Colors.*;

import java.util.function.Consumer;
import java.util.function.IntFunction;

public class CarouselSelector extends FlexLayout {
	private int currentIndex;
	private final Widget[] options;
	private final Consumer<Integer> onChange;
	private final boolean wrap;

	public CarouselSelector(
		int count, IntFunction<Widget> optionFactory, Consumer<Integer> onChange
	) {
		this(
			count, optionFactory, onChange, 0, true, arrow("<", 2),
			arrow(">", 2)
		);
	}

	public CarouselSelector(
		int count, IntFunction<Widget> optionFactory,
		Consumer<Integer> onChange, int fixedWidth
	) {
		this(
			count, optionFactory, onChange, fixedWidth, true, arrow("<", 2),
			arrow(">", 2)
		);
	}

	public CarouselSelector(
		int count, IntFunction<Widget> optionFactory,
		Consumer<Integer> onChange, int fixedWidth, boolean wrap
	) {
		this(
			count, optionFactory, onChange, fixedWidth, wrap, arrow("<", 2),
			arrow(">", 2)
		);
	}

	public CarouselSelector(
		int count, IntFunction<Widget> optionFactory,
		Consumer<Integer> onChange, int fixedWidth, boolean wrap,
		Widget prevContent, Widget nextContent
	) {
		super(Direction.ROW, 8);
		if (count <= 0)
			throw new IllegalArgumentException("count must be positive");
		this.options = new Widget[count];
		for (int i = 0; i < count; i++)
			options[i] = optionFactory.apply(i);
		this.currentIndex = 0;
		this.onChange = onChange != null ? onChange : i -> {};
		this.wrap = wrap;

		var prevBtn = new Button(this::prev);
		prevBtn.addChild(prevContent);
		addChild(prevBtn);

		var wrapper = new AlignLayout();
		for (int i = 0; i < options.length; i++) {
			options[i].setVisible(i == currentIndex);
			wrapper.addChild(options[i]);
		}
		wrapper.setProp("flex-grow", 1);
		addChild(wrapper);

		var nextBtn = new Button(this::next);
		nextBtn.addChild(nextContent);
		addChild(nextBtn);

		if (fixedWidth > 0) {
			int[] ps = prevBtn.measure();
			int[] ns = nextBtn.measure();
			int target = fixedWidth - ps[0] - ns[0] - 16;
			if (target > 0)
				wrapper.addChild(new GlueWidget(target, 0));
		}
	}

	public static Widget arrow(String label, int size) {
		var align = new AlignLayout();
		var text = new TextComponent(label, size, rgb(50, 50, 50));
		text.setProp("h-align", AlignLayout.HAlign.CENTER);
		text.setProp("v-align", AlignLayout.VAlign.CENTER);
		align.addChild(text);
		return align;
	}

	public int getIndex() {
		return currentIndex;
	}

	private void prev() {
		int next;
		if (currentIndex == 0) {
			if (!wrap)
				return;
			next = options.length - 1;
		} else {
			next = currentIndex - 1;
		}
		options[currentIndex].setVisible(false);
		currentIndex = next;
		options[currentIndex].setVisible(true);
		onChange.accept(currentIndex);
	}

	private void next() {
		int next;
		if (currentIndex == options.length - 1) {
			if (!wrap)
				return;
			next = 0;
		} else {
			next = currentIndex + 1;
		}
		options[currentIndex].setVisible(false);
		currentIndex = next;
		options[currentIndex].setVisible(true);
		onChange.accept(currentIndex);
	}
}
