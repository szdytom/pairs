package app.pairs.model;

public class Item {
	private int swap = 0;
	private int repromute = 0;
	private int autosolve = 0;
	private int timefrozer = 0;
	public ItemType canRevive() {
		if (swap > 0) {
			return ItemType.SWAP;
		}
		if (repromute > 0) {
			return ItemType.REPROMUTE;
		}
		return null;
	}
}
