package game.event.pvp;

import client.Player;
import game.map.GameMap;
import lombok.Getter;
import lombok.Setter;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class Team {
    private Match match;
    private Type type;
    private final HashMap<Integer, Player> players = new HashMap<>();
    private final List<Building> buildings = new ArrayList<>();


    public Team(Match match, Type type) {
        this.match = match;
        this.type = type;
    }

    public void init(GameMap map) {
        buildings.clear();

        int id = type == Type.BLUE ? 1000 : 2000;

        for (Lane lane : Lane.values()) {
            int[][] positions = lane.getTurrets(type);

            for (int[] position : positions) {
                Building building = new Building(id++, position[0], position[1], this, lane);
                building.setMap(map);
                buildings.add(building);
            }
        }
    }



    public void clear() {
        players.clear();
        buildings.clear();
    }
}