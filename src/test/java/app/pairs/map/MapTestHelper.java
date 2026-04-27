package app.pairs.map;

import app.pairs.model.Tilemap;

final class MapTestHelper {
    private MapTestHelper() {
    }

    static void printTilemap(Tilemap tilemap) {
        for (int row = 0; row < tilemap.getHeight(); row++) {
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < tilemap.getWidth(); col++) {
                line.append(String.format("%4d", tilemap.getTile(row, col)));
            }
            System.out.println(line);
        }
    }
}
