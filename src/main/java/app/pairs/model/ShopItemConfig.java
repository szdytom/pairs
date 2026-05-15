package app.pairs.model;

import java.util.List;

public record ShopItemConfig(
	String title, String icon, int k, double alpha, String kind,
	String itemType, List<String> description, List<List<String>> taglines
) {
	public ShopItemConfig {
		if (description == null) {
			description = List.of();
		}
		if (taglines == null) {
			taglines = List.of();
		}
	}

	public int getCost(int purchaseCount) {
		return (int)(k * Math.pow(alpha, purchaseCount));
	}
}
