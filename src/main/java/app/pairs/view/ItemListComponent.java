package app.pairs.view;

import static app.pairs.utils.Colors.*;

import app.pairs.asset.IconManager;
import app.pairs.logic.GameState;
import app.pairs.model.ItemType;

import java.util.function.Consumer;

public class ItemListComponent extends FlexLayout {
	private final ItemButton autoSolverBtn;
	private final ItemButton tntBtn;
	private final TextComponent abortText;
	private int lastSolverCount;
	private int lastTntCount;

	public ItemListComponent(
		int solverCount, int tntCount, Consumer<ItemType> onItemUse
	) {
		super(Direction.ROW, 4, 4);

		this.autoSolverBtn = new ItemButton(
			IconManager.instance().getTexture(ItemType.AUTO_SOLVER.iconId()),
			ItemType.AUTO_SOLVER.displayName(), solverCount,
			() -> onItemUse.accept(ItemType.AUTO_SOLVER)
		);
		this.tntBtn = new ItemButton(
			IconManager.instance().getTexture(ItemType.TNT.iconId()),
			ItemType.TNT.displayName(), tntCount,
			() -> onItemUse.accept(ItemType.TNT)
		);
		addChild(autoSolverBtn);
		addChild(tntBtn);

		this.abortText = new TextComponent("Select tile", 1, rgb(160, 0, 0));

		this.lastSolverCount = solverCount;
		this.lastTntCount = tntCount;
	}

	public void showAbort() {
		removeChild(autoSolverBtn);
		removeChild(tntBtn);
		addChild(abortText);
		var bb = blackboard();
		if (bb != null) {
			bb.layoutDirty = true;
		}
	}

	public void hideAbort() {
		removeChild(abortText);
		addChild(autoSolverBtn);
		addChild(tntBtn);
		var bb = blackboard();
		if (bb != null) {
			bb.layoutDirty = true;
		}
	}

	@Override
	public void update(long deltaTimeMs) {
		super.update(deltaTimeMs);
		GameState gs = blackboard().get(GameState.class);
		boolean dirty = false;

		int solverCount = gs.gameStatus.getCount(ItemType.AUTO_SOLVER);
		if (solverCount != lastSolverCount) {
			autoSolverBtn.setCount(solverCount);
			lastSolverCount = solverCount;
			dirty = true;
		}

		int tntCount = gs.gameStatus.getCount(ItemType.TNT);
		if (tntCount != lastTntCount) {
			tntBtn.setCount(tntCount);
			lastTntCount = tntCount;
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
