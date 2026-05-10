package app.pairs.model;

public enum ItemType {
	AUTO_SOLVER("Auto", "ai");

	private final String displayName;
	private final String iconId;

	ItemType(String displayName, String iconId) {
		this.displayName = displayName;
		this.iconId = iconId;
	}

	public String displayName() {
		return displayName;
	}

	public String iconId() {
		return iconId;
	}
}
