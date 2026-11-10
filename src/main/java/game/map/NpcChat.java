package game.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import client.Player;
import client.io.Message;
import model.map.NpcChatData;
import utils.SQLHelper;

public class NpcChat {

    private static final Map<Integer, List<NpcChatData>> BY_MAP = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    public static synchronized void reload() {
        Map<Integer, List<NpcChatData>> fresh = new ConcurrentHashMap<>();
        try {
            List<NpcChatData> rows = SQLHelper.selectFrom("npc_chat").getAsModel(NpcChatData.class);
            for (NpcChatData row : rows) {
                if (!row.isEnabled() || row.getChatText() == null || row.getChatText().isEmpty()) {
                    continue;
                }
                fresh.computeIfAbsent(row.getMapId(), k -> new ArrayList<>()).add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        BY_MAP.clear();
        BY_MAP.putAll(fresh);
        loaded = true;
    }

    public static void autoChat(GameMap map) throws IOException {
        if (map == null) {
            return;
        }
        if (!loaded) {
            reload();
        }
        List<NpcChatData> list = BY_MAP.get((int) map.mapId);
        if (list == null) {
            return;
        }
        for (NpcChatData c : list) {
            chat(map, c.getChatText(), c.getNpcId());
        }
    }

    public static void chat(GameMap map, String txt, int id) throws IOException {
        if (map == null || txt == null) {
            return;
        }
        Message m = new Message(23);
        try {
            m.writer().writeUTF(txt);
            m.writer().writeByte(id);
            for (int j = 0; j < map.players.size(); j++) {
                Player p0 = map.players.get(j);
                if (p0 != null && p0.conn != null && p0.map != null && p0.map.equals(map)) {
                    p0.conn.addmsg(m);
                }
            }
        } finally {
            m.cleanup();
        }
    }
}