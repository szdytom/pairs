package app.pairs.view;

import static app.pairs.utils.Colors.*;

import app.pairs.model.ItemType;
import app.pairs.model.RogueSession;

public class ShopComponent extends AlignLayout {
	private final RogueSession session;
	private final TextComponent scoreText;
	private final TextComponent autoCost;
	private final TextComponent tntCost;
	private final TextComponent timeCost;
	private int lastScore = -1;
	private int lastAutoCost = -1;
	private int lastTntCost = -1;
	private int lastTimeCost = -1;

	public ShopComponent(RogueSession session, Runnable onContinue) {
		this.session = session;

		var title = new TextComponent("Shop", 3, rgb(30, 30, 30));
		title.setProp("h-align", AlignLayout.HAlign.CENTER);

		this.scoreText = new TextComponent("Score: 0", 2, rgb(60, 60, 60));
		scoreText.setProp("h-align", AlignLayout.HAlign.CENTER);

		this.autoCost = new TextComponent("0", 1, rgb(80, 80, 80));
		var autoBtn = buyButton(() -> {
			session.purchaseItem(ItemType.AUTO_SOLVER);
		});
		var autoRow = new FlexLayout(FlexLayout.Direction.ROW, 6);
		autoRow.addChild(new TextComponent("Auto Solver", 1, rgb(80, 80, 80)));
		autoRow.addChild(autoCost);
		autoRow.addChild(autoBtn);

		this.tntCost = new TextComponent("0", 1, rgb(80, 80, 80));
		var tntBtn = buyButton(() -> { session.purchaseItem(ItemType.TNT); });
		var tntRow = new FlexLayout(FlexLayout.Direction.ROW, 6);
		tntRow.addChild(new TextComponent("TNT", 1, rgb(80, 80, 80)));
		tntRow.addChild(tntCost);
		tntRow.addChild(tntBtn);

		this.timeCost = new TextComponent("0", 1, rgb(80, 80, 80));
		var timeBtn = buyButton(() -> { session.purchaseTime(); });
		var timeRow = new FlexLayout(FlexLayout.Direction.ROW, 6);
		timeRow.addChild(new TextComponent("+30s Time", 1, rgb(80, 80, 80)));
		timeRow.addChild(timeCost);
		timeRow.addChild(timeBtn);

		var continueBtn = makeContinueButton("Continue", onContinue);

		var column = new FlexLayout(FlexLayout.Direction.COLUMN, 10);
		column.addChild(title);
		column.addChild(scoreText);
		column.addChild(autoRow);
		column.addChild(tntRow);
		column.addChild(timeRow);
		column.addChild(continueBtn);
		column.setProp("h-align", AlignLayout.HAlign.CENTER);
		column.setProp("v-align", AlignLayout.VAlign.CENTER);
		addChild(column);
	}

	private static Button buyButton(Runnable onClick) {
		var btn = new Button(
			onClick, rgba(180, 200, 180, 255), rgba(140, 160, 140, 255)
		);
		var align = new AlignLayout();
		var text = new TextComponent("Buy", 1, rgb(30, 60, 30));
		text.setProp("h-align", AlignLayout.HAlign.CENTER);
		text.setProp("v-align", AlignLayout.VAlign.CENTER);
		align.addChild(text);
		btn.addChild(align);
		return btn;
	}

	private static Button makeContinueButton(String label, Runnable onClick) {
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
		super.update(deltaTimeMs);
		boolean dirty = false;

		int score = session.spendableScore;
		if (score != lastScore) {
			scoreText.setText("Score: " + score);
			lastScore = score;
			dirty = true;
		}

		int ac = session.getItemCost(ItemType.AUTO_SOLVER);
		if (ac != lastAutoCost) {
			autoCost.setText(String.valueOf(ac));
			lastAutoCost = ac;
			dirty = true;
		}

		int tc = session.getItemCost(ItemType.TNT);
		if (tc != lastTntCost) {
			tntCost.setText(String.valueOf(tc));
			lastTntCost = tc;
			dirty = true;
		}

		int timec = session.getTimeCost();
		if (timec != lastTimeCost) {
			timeCost.setText(String.valueOf(timec));
			lastTimeCost = timec;
			dirty = true;
		}

		if (dirty) {
			var bb = blackboard();
			if (bb != null) {
				bb.layoutDirty = true;
			}
		}
	}
}
