package app.pairs.view;

import static app.pairs.utils.Colors.*;

import app.pairs.model.GameType;
import app.pairs.save.Database;
import app.pairs.save.LeaderBoardEntry;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RankComponent extends FlexLayout {
	private static final DateTimeFormatter
		TIME_FMT = DateTimeFormatter.ofPattern("MM/dd HH:mm");

	private static final GameType[] TABS = {
		GameType.EASY, GameType.HARD, GameType.EXTREME, GameType.NORMAL,
		GameType.ROGUE
	};

	private static final int TAB_NORMAL = rgba(200, 200, 200, 255);
	private static final int TAB_SELECTED = rgba(100, 150, 220, 255);
	private static final int TAB_HOVER = rgba(160, 160, 160, 255);
	private static final int TAB_SELECTED_HOVER = rgba(80, 120, 180, 255);

	private final List<LeaderBoardEntry> allEntries;
	private final Button[] tabButtons = new Button[TABS.length];
	private final ScrollListLayout scrollList;
	private GameType selectedType = TABS[0];

	public RankComponent(Runnable onBack) {
		super(Direction.COLUMN, 8, 16);
		allEntries = Database.instance().saves().listLeaderBoard();

		addChild(buildHeader(onBack));
		addChild(buildTabs());

		scrollList = new ScrollListLayout(
			4, 8, ScrollListLayout.HeightStrategy.FILL
		);
		scrollList.setProp("flex-grow", 1);
		populateList();
		addChild(scrollList);
	}

	private AlignLayout buildHeader(Runnable onBack) {
		var backBtn = Button.fromIcon("home", onBack);
		backBtn.setProp("h-align", AlignLayout.HAlign.LEFT);
		backBtn.setProp("v-align", AlignLayout.VAlign.CENTER);

		var title = new TextComponent("Leaderboard", 3, rgb(30, 30, 30));
		title.setProp("h-align", AlignLayout.HAlign.CENTER);
		title.setProp("v-align", AlignLayout.VAlign.CENTER);

		var header = new AlignLayout();
		header.addChild(backBtn);
		header.addChild(title);
		return header;
	}

	private AlignLayout buildTabs() {
		var row = new FlexLayout(Direction.ROW, 4);
		for (int i = 0; i < TABS.length; i++) {
			GameType type = TABS[i];
			boolean selected = type == selectedType;
			tabButtons[i] = makeTabButton(
				type.name(), selected, () -> selectTab(type)
			);
			row.addChild(tabButtons[i]);
		}
		row.setProp("h-align", AlignLayout.HAlign.CENTER);
		var wrapper = new AlignLayout();
		wrapper.addChild(row);
		return wrapper;
	}

	private void selectTab(GameType type) {
		if (type == selectedType) {
			return;
		}
		selectedType = type;
		for (int i = 0; i < TABS.length; i++) {
			boolean selected = TABS[i] == selectedType;
			tabButtons[i].setColor(selected ? TAB_SELECTED : TAB_NORMAL);
			tabButtons[i].setHoverColor(
				selected ? TAB_SELECTED_HOVER : TAB_HOVER
			);
		}
		scrollList.removeAllChildren();
		populateList();
		blackboard().layoutDirty = true;
	}

	private void populateList() {
		List<LeaderBoardEntry>
			filtered = allEntries.stream()
						   .filter(e -> e.difficulty() == selectedType)
						   .toList();
		if (filtered.isEmpty()) {
			var align = new AlignLayout();
			var empty = new TextComponent(
				"No scores yet", 2, rgb(120, 120, 120)
			);
			empty.setProp("h-align", AlignLayout.HAlign.CENTER);
			align.addChild(empty);
			scrollList.addChild(align);
		} else {
			for (LeaderBoardEntry entry : filtered) {
				scrollList.addChild(makeRow(entry));
			}
		}
	}

	// A column that always reports natural width = 0, so flex-grow distributes
	// the total row width equally among all columns regardless of content.
	private static Widget makeEqualColumn(
		boolean glueLeft, Widget content, boolean glueRight
	) {
		var col = new FlexLayout(Direction.ROW, 0) {
			private final int[] sz = new int[2];
			@Override
			public int[] measure() {
				sz[0] = 0;
				sz[1] = super.measure()[1];
				return sz;
			}
		};
		col.setProp("flex-grow", 1);
		if (glueLeft)
			col.addChild(GlueWidget.flexible());
		col.addChild(content);
		if (glueRight)
			col.addChild(GlueWidget.flexible());
		return col;
	}

	private static Widget makeRow(LeaderBoardEntry entry) {
		var nameText = new TextComponent(entry.username(), 1, rgb(30, 30, 30));
		var scoreText = new TextComponent(
			String.valueOf(entry.score()), 1, rgb(50, 80, 150)
		);
		var timeText = new TextComponent(
			formatTime(entry.updatedAt()), 1, rgb(100, 100, 100)
		);

		var row = new FlexLayout(Direction.ROW, 0, 6);
		row.addChild(makeEqualColumn(false, nameText, true)); // left in col
		row.addChild(
			makeEqualColumn(false, scoreText, true)
		); // left in center col
		row.addChild(makeEqualColumn(true, timeText, false)); // right in col
		return row;
	}

	private static Button makeTabButton(
		String label, boolean selected, Runnable onClick
	) {
		int bg = selected ? TAB_SELECTED : TAB_NORMAL;
		int hover = selected ? TAB_SELECTED_HOVER : TAB_HOVER;
		var btn = new Button(onClick, bg, hover);
		var text = new TextComponent(label, 1, rgb(30, 30, 30));
		text.setProp("h-align", AlignLayout.HAlign.CENTER);
		text.setProp("v-align", AlignLayout.VAlign.CENTER);
		var align = new AlignLayout();
		align.addChild(text);
		btn.addChild(align);
		return btn;
	}

	private static String formatTime(long epochMs) {
		LocalDateTime dt = LocalDateTime.ofInstant(
			Instant.ofEpochMilli(epochMs), ZoneId.systemDefault()
		);
		return TIME_FMT.format(dt);
	}
}
