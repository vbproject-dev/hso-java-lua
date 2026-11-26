package game.event.pvp;


import client.Player;
import client.io.Message;
import core.Service;
import game.map.GameMap;
import game.map.MapService;
import lombok.Getter;
import model.map.Vgo;
import template.MainObject;
import utils.Timer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Getter
public class Match {
    private final int id;
    private final Map<Type, Team> teams = new HashMap<>();
    private final GameMap map;
    private MobaState state = MobaState.PLAYING;

    private final Map<Integer, Long> buildingAttackTimes = new HashMap<>();
    private static final long BUILDING_ATTACK_DELAY = 3;
    private static final long MATCH_DURATION = TimeUnit.MINUTES.toSeconds(30);
    private long startTime;

    public Match(int id, List<Player> players, GameMap map, int teamSize) {
        this.id = id;
        this.map = map;

        teams.put(Type.BLUE, new Team(this, Type.BLUE));
        teams.put(Type.RED, new Team(this, Type.RED));

        int index = 0;

        for (Team team : teams.values()) {
            for (int i = 0; i < teamSize && index < players.size(); i++) {
                Player player = players.get(index++);
                team.getPlayers().put(player.objectId, player);
            }
            team.init(map);
        }
    }

    public void start() {
        startTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        for (Team team : teams.values()) {
            Type type = team.getType();

            for (Player player : team.getPlayers().values()) {

                MapService.leave(player.map, player);
                player.map = map;
                player.x = (short) type.getSpawnX();
                player.y = (short) type.getSpawnY();
                player.xOld = player.x;
                player.yOld = player.y;
                player.typepk = team.getType().getFlag();
                MapService.enter(player.map, player);

            }
        }
    }

    public boolean isTeamMateBuilding(Player player, Building building) {
        for (Team team : teams.values()) {
            if (!team.getPlayers().containsKey(player.objectId)) continue;
            return (building != null && building.getTeam().getType() == team.getType());

        }
        return false;
    }

    public boolean canAttackBuilding(Building target) {
        Team team = target.getTeam();
        Lane lane = target.getLane();

        if (lane == Lane.BASE) {
            for (Building building : team.getBuildings()) {
                if (building.getLane() != Lane.BASE && !building.isdie) {
                    return false;
                }
            }
            return true;
        }

        List<Building> laneBuildings = team.getBuildings().stream()
                .filter(building -> building.getLane() == lane)
                .toList();

        int targetIndex = laneBuildings.indexOf(target);

        for (int i = laneBuildings.size() - 1; i > targetIndex; i--) {
            if (!laneBuildings.get(i).isdie) {
                return false;
            }
        }

        return true;
    }

    public void onEnter(Player player) {
        // Send building info when player entered map
        for (Team team : teams.values()) {
            for (Building object : team.getBuildings()) {
                object.sendBuildingInfo(player);
            }
        }

        Service.sendTime(player, "Berakhir Dalam", (int) MATCH_DURATION);
    }

    public void onBaseDestroyed(Building building) {
        Type winner = building.getTeam().getType() == Type.BLUE ? Type.RED : Type.BLUE;
        Team winnerTeam = teams.get(winner);
        end();
    }

    public Building getBuilding(int id) {
        for (Team team : teams.values()) {
            for (Building object : team.getBuildings()) {
                if (object.objectId == id) {
                    return object;
                }
            }
        }

        return null;
    }

    public List<Player> getNearbyEnemies(Building building, int range) {
        Team enemyTeam = teams.get(building.getTeam().getType() == Type.BLUE ? Type.RED : Type.BLUE);
        List<Player> enemies = new ArrayList<>();
        int rangeSq = range * range;

        for (Player player : enemyTeam.getPlayers().values()) {
            if (player.map != map) continue;

            int dx = player.x - building.x;
            int dy = player.y - building.y;

            if (dx * dx + dy * dy <= rangeSq) {
                enemies.add(player);
            }
        }

        return enemies;
    }


    public void broadcast(Message msg) {
        for (Team team : teams.values()) {
            for (Player player : team.getPlayers().values()) {
                player.conn.addmsg(msg);
            }
        }
    }

    public void end() {
        if (state == MobaState.ENDED) return;
        state = MobaState.ENDED;

        for (Team team : teams.values()) {
            for (Player player : team.getPlayers().values()) {
                MapService.leave(player.map, player);
                player.typepk = -1;
                Service.sendTime(player, "", 0);
                MapService.leave(player.map, player);


                try {
                    player.changeMap(player, Vgo.create(player, 1, 432, 354));
                } catch (IOException ignore) {
                }
            }
        }

        MobaManager.getInstance().removeMatch(id);
    }

    public Team getTeam(Player player) {
        for (Team team : teams.values()) {
            if (team.getPlayers().containsKey(player.objectId)) {
                return team;
            }
        }
        return null;
    }

    public void onPlayerDie(Player player, MainObject attacker) {
        var team = getTeam(player);
        if (team != null) {
            Timer.countdown(TimeUnit.SECONDS.toMillis(15), tick -> {
                try {

                    Service.send_notice_nobox_white(player.conn, String.format("Respawn in %d second", tick));

                } catch (IOException ignore) {
                }
            }, () -> {


                try {

                    // Spawn at team base
                    player.x = (short) team.getType().getSpawnX();
                    player.y = (short) team.getType().getSpawnY();
                    Message m = new Message(4);
                    m.writer().writeByte(0);
                    m.writer().writeShort(0);
                    m.writer().writeShort(player.objectId);
                    m.writer().writeShort(player.x);
                    m.writer().writeShort(player.y);
                    m.writer().writeByte(-1);
                    MapService.update_inside_player(map, m, player);

                    player.isdie = false;
                    player.hp = player.body.getMaxHP();
                    player.mp = player.body.getMaxMP();
                    Service.sendMainCharInfo(player);
                    Service.send_combo(player.conn);
                    Service.usepotion(player, 0, player.body.getMaxHP());
                    Service.usepotion(player, 1, player.body.getMaxMP());
                } catch (Exception ignore) {
                }


            });
        }

    }

    public void update() {
        long now = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

        if (now - startTime >= MATCH_DURATION) {
            end();
            return;
        }

        for (Team team : teams.values()) {
            for (Building building : team.getBuildings()) {
                if (building.isdie) continue;

                long lastAttack = buildingAttackTimes.getOrDefault(building.objectId, 0L);
                if (now - lastAttack < BUILDING_ATTACK_DELAY) continue;

                List<Player> enemies = getNearbyEnemies(building, 250);
                if (enemies.isEmpty()) continue;

                for (Player enemy : enemies) {
                    try {
                        MainObject.attack(map, building, enemy, 0, null, 2);
                    } catch (IOException ignore) {
                    }
                }

                buildingAttackTimes.put(building.objectId, now);
            }
        }
    }
}