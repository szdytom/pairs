package app.pairs.model;

public class Tilemap {
	private final int width;
	private final int height;
	private final int[][] id;
	

	public Tilemap(int[][] id) {
		this.id = id;
		this.height = id.length;
		this.width = id[0].length;
	}

	public int getTile(int row, int col) {
		return id[row][col];
	}

	public void setTile(int row, int col, int newId) {
		id[row][col] = newId;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
}
