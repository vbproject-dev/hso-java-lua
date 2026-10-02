package BossHDL;

import core.SQL;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;

/**
 * Sistem point kill boss.
 * Boss biasa = +1 point, Boss premium = +2 point.
 * Point diberikan ke player top-damage DAN player last-hit (mainAtk/killer).
 * Kalau top-damage dan last-hit adalah player yang sama, point cuma ditambahkan sekali (tidak dobel).
 * Data disimpan di tabel `boss_kill_point`.
 * Cache top 20 dikelola oleh BXH.loadTopBossKill().
 */
@Slf4j
public class BossKillPoint {

    // ---------------------------------------------------------------
    // Tambah point untuk player top-damage
    // ---------------------------------------------------------------
    public static void addPoint(String playerName, int points) {
        addPoint(playerName, points, true);
    }

    /**
     * @param refreshLeaderboard kalau false, cache top-20 TIDAK di-reload di sini.
     *   Dipakai saat memanggil addPoint() berkali-kali berturutan (semua hitter
     *   boss dapat poin) supaya reload leaderboard (query JOIN + parsing JSON,
     *   jauh lebih berat dari sekadar INSERT satu baris) cukup dijalankan SEKALI
     *   di akhir batch oleh pemanggil, bukan diulang untuk tiap player.
     *   Lihat catatan panjang di MobInMap.setDie() soal delay reward boss yang
     *   ditimbulkan pola lama (reload leaderboard sinkron x jumlah hitter,
     *   dijalankan SEBELUM item reward di-drop).
     */
    public static void addPoint(String playerName, int points, boolean refreshLeaderboard) {
        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO `boss_kill_point` (`name`, `point`) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE `point` = `point` + ?")) {
            ps.setString(1, playerName);
            ps.setInt(2, points);
            ps.setInt(3, points);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            log.error("[BossKillPoint] Gagal addPoint untuk {}: {}", playerName, e.getMessage());
        }
        if (refreshLeaderboard) {
            // Refresh cache leaderboard setelah point masuk
            core.BXH.loadTopBossKill();
        }
    }

    // ---------------------------------------------------------------
    // Ambil point milik 1 player (untuk info menu)
    // ---------------------------------------------------------------
    public static int getPoint(String playerName) {
        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT `point` FROM `boss_kill_point` WHERE `name` = ?")) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("point");
            }
        } catch (SQLException e) {
            log.error("[BossKillPoint] Gagal getPoint untuk {}: {}", playerName, e.getMessage());
        }
        return 0;
    }

    // ---------------------------------------------------------------
    // Load cache saat server start — delegasi ke BXH
    // ---------------------------------------------------------------
    public static void load() {
        core.BXH.loadTopBossKill();
        log.info("[BossKillPoint] Cache boss kill loaded.");
    }
}