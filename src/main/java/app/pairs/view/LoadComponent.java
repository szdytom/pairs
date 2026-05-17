package app.pairs.view;

import static app.pairs.utils.Colors.*;

import app.pairs.save.SaveEntry;
import app.pairs.user.UserSession;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class LoadComponent extends FlexLayout {
	private static final DateTimeFormatter
		TIME_FMT = DateTimeFormatter.ofPattern("MM/dd HH:mm");

	public LoadComponent(Runnable onBack, Consumer<SaveEntry> onSelect) {
		super(Direction.COLUMN, 8, 16);

		var header = buildHeader(onBack);
		var saveList = buildSaveList(onSelect);
		saveList.setProp("flex-grow", 1);

		addChild(header);
		addChild(saveList);
	}

	private static AlignLayout buildHeader(Runnable onBack) {
		var backBtn = Button.fromIcon("home", onBack);
		backBtn.setProp("h-align", AlignLayout.HAlign.LEFT);
		backBtn.setProp("v-align", AlignLayout.VAlign.CENTER);

		var title = new TextComponent("Load Save", 3, rgb(30, 30, 30));
		title.setProp("h-align", AlignLayout.HAlign.CENTER);
		title.setProp("v-align", AlignLayout.VAlign.CENTER);

		var header = new AlignLayout();
		header.addChild(backBtn);
		header.addChild(title);
		return header;
	}

	private static ScrollListLayout buildSaveList(
		Consumer<SaveEntry> onSelect
	) {
		var scrollList = new ScrollListLayout(
			4, 8, ScrollListLayout.HeightStrategy.FILL
		);
		List<SaveEntry> saves = UserSession.instance().getUser().listSaves();
		if (saves.isEmpty()) {
			var emptyAlign = new AlignLayout();
			var empty = new TextComponent(
				"No saves found", 2, rgb(120, 120, 120)
			);
			empty.setProp("h-align", AlignLayout.HAlign.CENTER);
			emptyAlign.addChild(empty);
			scrollList.addChild(emptyAlign);
		} else {
			for (SaveEntry entry : saves) {
				scrollList.addChild(makeRow(entry, onSelect));
			}
		}
		return scrollList;
	}

	private static Button makeRow(
		SaveEntry entry, Consumer<SaveEntry> onSelect
	) {
		var idText = new TextComponent("#" + entry.id(), 1, rgb(80, 80, 80));
		var typeText = new TextComponent(
			entry.type().name(), 1, rgb(30, 30, 30)
		);
		var timeText = new TextComponent(
			formatTime(entry.updatedAt()), 1, rgb(100, 100, 100)
		);

		var spacer1 = new GlueWidget();
		spacer1.setProp("flex-grow", 1);
		var spacer2 = new GlueWidget();
		spacer2.setProp("flex-grow", 1);

		var row = new FlexLayout(Direction.ROW, 8, 6);
		row.addChild(idText);
		row.addChild(spacer1);
		row.addChild(typeText);
		row.addChild(spacer2);
		row.addChild(timeText);

		return makeButton(row, () -> onSelect.accept(entry));
	}

	private static Button makeButton(Widget content, Runnable onClick) {
		var btn = new Button(
			onClick, rgba(220, 220, 220, 255), rgba(180, 180, 180, 255)
		);
		btn.addChild(content);
		return btn;
	}

	private static String formatTime(long epochMs) {
		LocalDateTime dt = LocalDateTime.ofInstant(
			Instant.ofEpochMilli(epochMs), ZoneId.systemDefault()
		);
		return TIME_FMT.format(dt);
	}
}
