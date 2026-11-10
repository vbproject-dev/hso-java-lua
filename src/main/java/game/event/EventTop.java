package game.event;

import core.SQL;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class EventTop {

    // ── Daftar key event (tambah di sini untuk event baru) ───────────────────
    public static final String HALLOWEEN_RITUAL = "halloween_ritual";

    public record Entry(String name, int total) {}

    private static final int TOP_LIMIT = 10;

    private static boolean tableReady = false;
    private static final Map<String, Map<String, Integer>> store = new HashMap<>();

    // Semua akses ke `store` lewat method synchronized di bawah.

    private static void ensureTable(Connection conn) throws SQLException {
        if (tableReady) return;
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `event_top` (" +
                " `event_key` VARCHAR(50) NOT NULL," +
                " `data` LONGTEXT NOT NULL," +
                " PRIMARY KEY (`event_key`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            conn.commit();
        }
        tableReady = true;
    }

    /** Ambil data satu event (muat dari DB sekali, setelah itu dari memori). */
    private static Map<String, Integer> load(String key) {
        Map<String, Integer> map = store.get(key);
        if (map != null) return map;

        map = new HashMap<>();
        try (Connection conn = SQL.gI().getConnection()) {
            ensureTable(conn);
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT `data` FROM `event_top` WHERE `event_key` = ?")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Object parsed = JSONValue.parse(rs.getString(1));
                        if (parsed instanceof Map<?, ?> m) {
                            for (Map.Entry<?, ?> e : m.entrySet()) {
                                map.put(String.valueOf(e.getKey()),
                                        ((Number) e.getValue()).intValue());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Jangan di-cache kalau gagal baca, supaya dicoba lagi & tidak menimpa data DB dengan map kosong
            log.error("[EventTop] Gagal load {}: {}", key, e.getMessage());
            return new HashMap<>();
        }
        store.put(key, map);
        return map;
    }

    private static void save(String key, Map<String, Integer> map) {
        try (Connection conn = SQL.gI().getConnection()) {
            ensureTable(conn);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO `event_top` (`event_key`, `data`) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE `data` = VALUES(`data`)")) {
                ps.setString(1, key);
                ps.setString(2, new JSONObject(map).toJSONString());
                ps.executeUpdate();
                conn.commit();
            }
        } catch (SQLException e) {
            log.error("[EventTop] Gagal save {}: {}", key, e.getMessage());
        }
    }

    /** Tambah `count` ke total player pada event `key`. */
    public static synchronized void add(String key, String playerName, int count) {
        if (key == null || playerName == null || count <= 0) return;
        Map<String, Integer> map = load(key);
        map.merge(playerName, count, Integer::sum);
        save(key, map);
    }

    public static synchronized List<Entry> getTop(String key) {
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<String, Integer> e : load(key).entrySet()) {
            list.add(new Entry(e.getKey(), e.getValue()));
        }
        list.sort((a, b) -> b.total() != a.total()
                ? Integer.compare(b.total(), a.total())
                : a.name().compareTo(b.name()));
        return list.size() > TOP_LIMIT ? new ArrayList<>(list.subList(0, TOP_LIMIT)) : list;
    }

    public static synchronized int getTotal(String key, String playerName) {
        return load(key).getOrDefault(playerName, 0);
    }

    /** Peringkat player (1 = teratas), 0 kalau belum punya data. */
    public static synchronized int getRank(String key, String playerName) {
        int mine = getTotal(key, playerName);
        if (mine <= 0) return 0;
        int rank = 1;
        for (int v : load(key).values()) {
            if (v > mine) rank++;
        }
        return rank;
    }

    /** Hapus semua data satu event (mis. reset musim). */
    public static synchronized void reset(String key) {
        store.put(key, new HashMap<>());
        try (Connection conn = SQL.gI().getConnection()) {
            ensureTable(conn);
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM `event_top` WHERE `event_key` = ?")) {
                ps.setString(1, key);
                ps.executeUpdate();
                conn.commit();
            }
        } catch (SQLException e) {
            log.error("[EventTop] Gagal reset {}: {}", key, e.getMessage());
        }
    }

    /** Teks siap tampil untuk sendNoticeBox. `satuan` mis. "x" atau " poin". */
    public static String buildText(String key, String judul, String satuan, String viewerName) {
        StringBuilder sb = new StringBuilder("=== ").append(judul).append(" ===\n\n");
        List<Entry> top = getTop(key);
        if (top.isEmpty()) {
            sb.append("Belum ada data.\n");
        } else {
            int i = 1;
            for (Entry e : top) {
                sb.append(i++).append(". ").append(e.name())
                  .append(" - ").append(e.total()).append(satuan).append("\n");
            }
        }
        int mine = getTotal(key, viewerName);
        int rank = getRank(key, viewerName);
        sb.append("\nTotal kamu: ").append(mine).append(satuan);
        if (rank > 0) sb.append(" (peringkat ").append(rank).append(")");
        return sb.toString();
    }
}