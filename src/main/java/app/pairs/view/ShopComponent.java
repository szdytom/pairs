package app.pairs.view;

import static app.pairs.utils.Colors.*;

import app.pairs.asset.AssetManager;
import app.pairs.asset.IconManager;
import app.pairs.model.ItemType;
import app.pairs.model.RogueSession;
import app.pairs.model.ShopItemConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import io.github.libsdl4j.api.render.SDL_Texture;

public class ShopComponent extends AlignLayout {
	private static final int CARD_WIDTH = 90;
	private static final int CARD_BG = rgba(230, 230, 230, 255);
	private static final int CARD_BG_HOVER = rgba(215, 215, 215, 255);
	private static final int PADDING = 6;
	private static final String[] SHOP_ITEM_IDS = {
		"shop/auto", "shop/tnt", "shop/time"
	};

	private final RogueSession session;
	private final TextComponent scoreText;
	private final TextComponent timeText;
	private final List<ShopRow> rows = new ArrayList<>();
	private int lastScore = -1;
	private long lastTimeMs = -1;

	private static class ShopRow {
		final ShopItemConfig config;
		final TextComponent costText;
		final TextComponent titleText;
		int lastCost = -1;
		int lastCount = -1;

		ShopRow(
			ShopItemConfig config, TextComponent costText,
			TextComponent titleText
		) {
			this.config = config;
			this.costText = costText;
			this.titleText = titleText;
		}
	}

	public ShopComponent(RogueSession session, Runnable onContinue) {
		this.session = session;

		var title = new TextComponent("Shop", 2, rgb(30, 30, 30));
		title.setProp("h-align", AlignLayout.HAlign.CENTER);

		this.scoreText = new TextComponent("Score: 0", 1, rgb(140, 140, 140));
		this.timeText = new TextComponent("Time: 00:00", 1, rgb(140, 140, 140));
		timeText.setProp("h-align", AlignLayout.HAlign.RIGHT);

		var infoRow = new AlignLayout();
		infoRow.addChild(scoreText);
		infoRow.addChild(timeText);

		var cardsRow = new FlexLayout(FlexLayout.Direction.ROW, 8);

		for (String id : SHOP_ITEM_IDS) {
			ShopItemConfig config = AssetManager.instance().get(id);
			var costText = new TextComponent("Cost: 0", 1, rgb(80, 80, 80));
			var titleText = new TextComponent(
				config.title(), 1, rgb(30, 30, 30)
			);
			if ("item".equals(config.kind()) && config.itemType() != null) {
				int count = session.items.get(
					ItemType.valueOf(config.itemType())
				);
				titleText.setText(config.title() + " (" + count + ")");
			}
			boolean hasBought = hasBoughtAny(config);
			cardsRow.addChild(makeCard(config, costText, titleText, hasBought));
			rows.add(new ShopRow(config, costText, titleText));
		}

		cardsRow.setProp("h-align", AlignLayout.HAlign.CENTER);

		var continueBtn = makeContinueButton("Continue", onContinue);

		var column = new FlexLayout(FlexLayout.Direction.COLUMN, 12);
		column.addChild(title);
		column.addChild(infoRow);
		column.addChild(cardsRow);
		column.addChild(continueBtn);
		column.setProp("h-align", AlignLayout.HAlign.CENTER);
		column.setProp("v-align", AlignLayout.VAlign.CENTER);
		addChild(column);
	}

	private boolean hasBoughtAny(ShopItemConfig config) {
		if ("time".equals(config.kind())) {
			return session.timePurchases > 0;
		}
		if ("item".equals(config.kind()) && config.itemType() != null) {
			int count = session.purchaseCounts.get(
				ItemType.valueOf(config.itemType())
			);
			return count > 0;
		}
		return false;
	}

	private Widget makeCard(
		ShopItemConfig config, TextComponent costText, TextComponent titleText,
		boolean hasBought
	) {
		var col = new FlexLayout(FlexLayout.Direction.COLUMN, 2);

		var headerRow = new FlexLayout(FlexLayout.Direction.ROW, 4);
		SDL_Texture iconTex = config.icon() != null
			? IconManager.instance().getTexture(config.icon())
			: null;
		if (iconTex != null) {
			headerRow.addChild(new ImageComponent(iconTex, 16, 16));
		}
		var titleAlign = new AlignLayout();
		titleText.setProp("v-align", AlignLayout.VAlign.CENTER);
		titleAlign.addChild(titleText);
		headerRow.addChild(titleAlign);
		col.addChild(headerRow);

		List<String> lines;
		if (hasBought && !config.taglines().isEmpty()) {
			int idx = new Random().nextInt(config.taglines().size());
			lines = config.taglines().get(idx);
		} else {
			lines = config.description();
		}
		for (String line : lines) {
			col.addChild(new TextComponent(line, 1, rgb(120, 120, 120)));
		}

		col.addChild(costText);
		col.addChild(buyButton(config, this::purchase));

		var card = new Card(
			CARD_WIDTH, CARD_BG, CARD_BG_HOVER, PADDING, PADDING
		);
		card.addChild(col);
		return card;
	}

	private void purchase(ShopItemConfig config) {
		int cost = currentCost(config);
		if ("time".equals(config.kind())) {
			session.buyTime(cost);
		} else if ("item".equals(config.kind()) && config.itemType() != null) {
			session.buyItem(ItemType.valueOf(config.itemType()), cost);
		}
	}

	private int currentCost(ShopItemConfig config) {
		int count = "time".equals(config.kind())
			? session.timePurchases
			: session.purchaseCounts.get(ItemType.valueOf(config.itemType()));
		return config.getCost(count);
	}

	private static Button buyButton(
		ShopItemConfig config, Consumer<ShopItemConfig> onBuy
	) {
		var btn = new Button(
			()
				-> onBuy.accept(config),
			rgba(180, 200, 180, 255), rgba(140, 160, 140, 255)
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
			scoreText.setText("Score: " + ScoreFormat.format(score));
			lastScore = score;
			dirty = true;
		}

		long remaining = session.remainingMs;
		if (remaining != lastTimeMs) {
			timeText.setText("Time: " + TimeFormat.format(remaining));
			lastTimeMs = remaining;
			dirty = true;
		}

		for (ShopRow row : rows) {
			int cost = currentCost(row.config);
			if (cost != row.lastCost) {
				row.costText.setText("Cost: " + ScoreFormat.format(cost));
				row.lastCost = cost;
				dirty = true;
			}

			if ("item".equals(row.config.kind())
			    && row.config.itemType() != null) {
				int count = session.items.get(
					ItemType.valueOf(row.config.itemType())
				);
				if (count != row.lastCount) {
					row.titleText.setText(
						row.config.title() + " (" + count + ")"
					);
					row.lastCount = count;
					dirty = true;
				}
			}
		}

		if (dirty) {
			var bb = blackboard();
			if (bb != null) {
				bb.layoutDirty = true;
			}
		}
	}
}
