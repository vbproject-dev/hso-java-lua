package game.event.btf;


import client.Player;
import core.Manager;
import lombok.Data;
import model.map.Vgo;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Team {
    private String name;
    private Vgo location;
    private int towerId;
    private boolean destroyed;
    private byte flag;
    private Map<Integer, String> players = new ConcurrentHashMap<>();

    public Team(String name, int towerId, Vgo location) {
        this.name = name;
        this.towerId = towerId;
        this.location = location;
    }

    public void addPlayer(Player player) {
        players.put(player.objectId, player.name);
    }

    public boolean isMyTeam(int playerId) {
        return players.containsKey(playerId);
    }

    public void addPoint(int point) throws IOException {
        for (Map.Entry<Integer, String> entry : players.entrySet()) {
            Player player = Manager.getPlayerById(entry.getKey());
            if (player == null) {
                continue;
            }

            // FIX: sebelumnya kalau updatePointArena() satu pemain gagal (IOException,
            // misal socket lagi bermasalah), exception-nya langsung keluar dari method
            // ini dan MEMOTONG loop - sisa anggota tim lain yang belum diproses jadi
            // ikut tidak dapat poinnya sama sekali. Sekarang tiap pemain ditangani
            // sendiri-sendiri supaya kegagalan satu orang tidak merugikan yang lain.
            try {
                player.updatePointArena(point);
            } catch (IOException ignore) {
            }
        }
    }

    public void destroy() {
        destroyed = true;
        players.values().forEach(name -> {

            try {

                Player p = Manager.getPlayerByName(name);
                if (p == null) {
                    return;
                }
                Vgo vgo = Vgo.create(p, 1, 320, 320);
                p.changeMap(p, vgo);
            } catch (IOException ignore) {
            }
        });
        players.clear();
    }


}
