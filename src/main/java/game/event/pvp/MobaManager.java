package game.event.pvp;

import client.Player;
import core.Service;
import game.map.GameMap;
import game.map.MapManager;
import game.map.MobInMap;
import lombok.Getter;
import lombok.Setter;
import model.map.MapData;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class MobaManager {
    private static final int MATCH_TIMEOUT = 60;
    private static final int TEAM_SIZE = 1;
    private static final int MATCH_SIZE = TEAM_SIZE * 2;

    private static final MobaManager HOLDER = new MobaManager();

    private final Queue<Player> queue = new ArrayDeque<>();
    private final Map<Integer, Long> queueTimes = new HashMap<>();
    private final Map<Integer, Match> matches = new HashMap<>();

    @Setter
    @Getter
    private boolean isOpen = false;

    private int matchId;

    private MobaManager() {}

    public static MobaManager getInstance() {
        return HOLDER;
    }


    public synchronized boolean join(Player player) {


        if (queueTimes.containsKey(player.objectId)) return false;

        queue.add(player);
        queueTimes.put(player.objectId, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        Service.sendTime(player, "Menunggu lawan", MATCH_TIMEOUT);
        createMatch();
        return true;
    }

    public synchronized boolean leave(Player player) {
        if (queueTimes.remove(player.objectId) == null) return false;
        return queue.remove(player);
    }

    public synchronized void removeMatch(int id) {
        matches.remove(id);
    }

    public synchronized void update() {
        long now = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

        Iterator<Player> iterator = queue.iterator();

        while (iterator.hasNext()) {
            Player player = iterator.next();
            Long joinedAt = queueTimes.get(player.objectId);

            if (joinedAt != null && now - joinedAt >= MATCH_TIMEOUT) {
                iterator.remove();
                queueTimes.remove(player.objectId);
                player.sendNoticeBox("Lawan tidak ditemukan");
                Service.sendTime(player, "", 0);
            }
        }

        createMatch();

        for (Match match : matches.values()) {
            match.update();
        }
    }

    private void createMatch() {
        if (queue.size() < MATCH_SIZE) return;

        MapData mapData = MapManager.getInstance().getMapData(122);
        if (mapData == null) return;

        List<Player> players = new ArrayList<>(MATCH_SIZE);

        for (int i = 0; i < MATCH_SIZE; i++) {
            Player player = queue.poll();
            queueTimes.remove(player.objectId);
            players.add(player);
        }

        GameMap map = GameMap.getEmptyZone(122);
        Match match = new Match(++matchId, players, map, TEAM_SIZE);

        matches.put(match.getId(), match);
        match.start();
    }

    public void onEnter(Player player) {
        Match match = getMatch(player);
        if (match != null) {
            match.onEnter(player);
        }
    }

    public synchronized Match getMatch(Player player) {
        for (Match match : matches.values()) {
            for (Team team : match.getTeams().values()) {
                if (team.getPlayers().containsKey(player.objectId)) {
                    return match;
                }
            }
        }
        return null;
    }

}