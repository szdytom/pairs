package app.pairs.view;

import app.pairs.asset.IconManager;
import app.pairs.logic.GameState;
import app.pairs.model.ItemType;

import java.util.function.Consumer;

public class ItemListComponent extends GridLayout {
	private final ItemButton autoSolverBtn;
	private int lastCount;

	public ItemListComponent(int initialCount, Consumer<ItemType> onItemUse) {
		super(1, 0, 0);
		var icon = IconManager.instance().getTexture(
			ItemType.AUTO_SOLVER.iconId()
		);
		this.autoSolverBtn = new ItemButton(
			icon, ItemType.AUTO_SOLVER.displayName(), initialCount,
			() -> onItemUse.accept(ItemType.AUTO_SOLVER)
		);
		addChild(autoSolverBtn);
		this.lastCount = initialCount;
	}

	@Override
	public void update(long deltaTimeMs) {
		super.update(deltaTimeMs);

		GameState gs = blackboard().get(GameState.class);
		int count = gs.gameStatus.getCount(ItemType.AUTO_SOLVER);
		if (count != lastCount) {
			autoSolverBtn.setCount(count);
			lastCount = count;
			var bb = blackboard();
			if (bb != null) {
				bb.layoutDirty = true;
			}
		}
	}
}
