package app.pairs.router;

import static app.pairs.utils.Colors.*;

import app.pairs.audio.AudioManager;
import app.pairs.logic.GameState;
import app.pairs.view.*;

import io.github.libsdl4j.api.render.*;

public class DifficultyPage implements Page {
	private static final String[] PRESETS = {
		"easy", "medium", "hard", "extreme"
	};
	private static final String[] LABELS = {
		"Easy", "Medium", "Hard", "Extreme"
	};

	private final Blackboard blackboard;
	private final Widget root;
	private final CarouselSelector selector;

	public DifficultyPage() {
		this.blackboard = new Blackboard();

		int maxOptionW = 0;
		for (String label : LABELS) {
			var t = new TextComponent(label, 3, rgb(30, 30, 30));
			int[] sz = t.measure();
			maxOptionW = Math.max(maxOptionW, sz[0]);
		}
		this.selector = new CarouselSelector(LABELS.length, i -> {
			var text = new TextComponent(LABELS[i], 3, rgb(30, 30, 30));
			text.setProp("h-align", AlignLayout.HAlign.CENTER);
			text.setProp("v-align", AlignLayout.VAlign.CENTER);
			return text;
		}, i -> {}, maxOptionW + 60);

		var startBtn = makeButton("Start", this::startGame);
		var customBtn = makeButton("Customize...", this::goCustom);
		var backBtn = new Button(
			this::goBack, rgba(180, 180, 180, 255), rgba(140, 140, 140, 255)
		);
		var backAlign = new AlignLayout();
		var backText = new TextComponent("Back", 2, rgb(50, 50, 50));
		backText.setProp("h-align", AlignLayout.HAlign.CENTER);
		backText.setProp("v-align", AlignLayout.VAlign.CENTER);
		backAlign.addChild(backText);
		backBtn.addChild(backAlign);

		var column = new FlexLayout(FlexLayout.Direction.COLUMN, 12);
		column.addChild(selector);
		column.addChild(startBtn);
		column.addChild(customBtn);
		column.addChild(backBtn);

		var rootAlign = new AlignLayout();
		column.setProp("h-align", AlignLayout.HAlign.CENTER);
		column.setProp("v-align", AlignLayout.VAlign.CENTER);
		rootAlign.addChild(column);
		rootAlign.setBlackboard(blackboard);
		this.root = rootAlign;
	}

	private void startGame() {
		var gameState = GameState.fromPreset(
			"tilemap/" + PRESETS[selector.getIndex()]
		);
		Router.instance().navigateTo(
			new LevelPage(gameState, LevelPage.DEFAULT_COUNTDOWN_MS)
		);
	}

	private void goCustom() {
		Router.instance().navigateTo(new CustomDifficultyPage());
	}

	private void goBack() {
		Router.instance().navigateTo(new MainMenuPage());
	}

	private static Button makeButton(String label, Runnable onClick) {
		var btn = new Button(
			onClick, rgba(200, 200, 200, 255), rgba(160, 160, 160, 255)
		);
		var align = new AlignLayout();
		var text = new TextComponent(label, 2, rgb(50, 50, 50));
		text.setProp("h-align", AlignLayout.HAlign.CENTER);
		text.setProp("v-align", AlignLayout.VAlign.CENTER);
		align.addChild(text);
		btn.addChild(align);
		return btn;
	}

	@Override
	public void update(long deltaTimeMs) {
		root.update(deltaTimeMs);
	}

	@Override
	public void render(SDL_Renderer renderer, int scale) {
		root.render(renderer, 0, 0, scale);
	}

	@Override
	public boolean onEvent(Event event) {
		return root.dispatchEvent(event, 0, 0);
	}

	@Override
	public void destroy() {
		root.destroy();
	}

	@Override
	public Widget getRoot() {
		return root;
	}

	@Override
	public Blackboard getBlackboard() {
		return blackboard;
	}
}
