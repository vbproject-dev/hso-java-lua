package game.map;


import game.ai.Position;
import io.ytcode.pathfinding.astar.AStar;
import io.ytcode.pathfinding.astar.Grid;
import io.ytcode.pathfinding.astar.Path;
import io.ytcode.pathfinding.astar.Reachability;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import model.map.TileData;

import java.util.*;

@Slf4j
@Getter
public class PathFinder {

    private static final int[] BLOCK_LIST = {39, 35, 19, 33, 20, 9, 35, 35, 17, 31, 25, 38, 45, 45, 45, 45, -1};
    private final int tileId;
    private final int mapWidth;
    private final int mapHeight;
    private final byte[] tiles;
    private Grid grid;
    private AStar aStar;

    public PathFinder(TileData tileData) {
        this.tileId = tileData.getImageId();
        this.mapWidth = tileData.getWidth();
        this.mapHeight = tileData.getHeight();
        this.tiles = tileData.getData();
        setup();
    }

    private void setup() {
        grid = new Grid(mapWidth, mapHeight);
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                grid.setWalkable(x, y, isWalkable(x, y));
            }
        }
        aStar = new AStar();
    }

    private int getIndex(int x, int y) {
        return y * mapWidth + x;
    }

    private boolean isWalkable(int x, int y) {
        if (x < 0 || x >= mapWidth || y < 0 || y >= mapHeight) return false;
        return !(tiles[getIndex(x, y)] >= getBlockTile() || tiles[getIndex(x, y)] == 0);
    }

    public boolean isReachable(Position start, Position target) {
        return Reachability.isReachable(start.getTileX(), start.getTileY(), target.getTileX(), target.getTileY(), grid);
    }

    public Path find(Position start, Position target) {
        return aStar.search(start.getTileX(), start.getTileY(), target.getTileX(), target.getTileY(), grid, true);
    }

    private int getBlockTile() {
        if (tileId < 0 || tileId > BLOCK_LIST.length) return -1;
        return BLOCK_LIST[tileId];
    }


}