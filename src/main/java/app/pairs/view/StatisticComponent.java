package app.pairs.view;

import static app.pairs.utils.Colors.*;

import app.pairs.model.GameType;
import app.pairs.user.UserSession;

import java.util.Map;

public class StatisticComponent extends AlignLayout {
	private static final GameType[] DIFFICULTIES = {
		GameType.EASY, GameType.NORMAL, GameType.HARD, GameType.EXTREME,
		GameType.ROGUE
	};

	public StatisticComponent(Runnable onBack) {
		Map<GameType, Long>
			scores = UserSession.instance().getUser().listBestScores();

		var backBtn = Button.fromIcon("home", onBack);
		backBtn.setProp("h-align", AlignLayout.HAlign.LEFT);
		backBtn.setProp("v-align", AlignLayout.VAlign.TOP);
		backBtn.setProp("h-padding", 8);
		backBtn.setProp("v-padding", 8);
		addChild(backBtn);

		var col = new FlexLayout(FlexLayout.Direction.COLUMN, 8, 16);
		col.addChild(buildTitle());
		col.addChild(buildRows(scores));
		col.setProp("h-align", AlignLayout.HAlign.CENTER);
		col.setProp("v-align", AlignLayout.VAlign.CENTER);
		addChild(col);
	}

	private static Widget buildTitle() {
		return new TextComponent("Statistics", 3, rgb(30, 30, 30));
	}

	private Widget buildRows(Map<GameType, Long> scores) {
		var col = new FlexLayout(FlexLayout.Direction.COLUMN, 0);
		col.addChild(new GlueWidget(280, 0));
		for (GameType type : DIFFICULTIES) {
			col.addChild(makeRow(type, scores.get(type)));
		}
		return col;
	}

	// Equal-width column: natural width reported as 0 so flex-grow distributes
	// space equally; glue widgets control alignment of content within the col.
	private static Widget makeEqualColumn(
		boolean glueLeft, Widget content, boolean glueRight
	) {
		var col = new FlexLayout(FlexLayout.Direction.ROW, 0) {
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

	private static Widget makeRow(GameType type, Long score) {
		var nameText = new TextComponent(type.name(), 1, rgb(30, 30, 30));
		var scoreText = new TextComponent(
			score != null ? String.valueOf(score) : "--", 1,
			score != null ? rgb(50, 80, 150) : rgb(150, 150, 150)
		);

		var row = new FlexLayout(FlexLayout.Direction.ROW, 0, 6);
		row.addChild(makeEqualColumn(false, nameText, true));
		row.addChild(makeEqualColumn(false, scoreText, true));
		return row;
	}
}
