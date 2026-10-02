package game.afk;

import core.SQL;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Mencatat data AFK (gems + exp) ke tabel `afk_log`.
 *
 * Strategi: SATU row per player (UPSERT via INSERT ... ON DUPLICATE KEY UPDATE).
 * Tidak ada row baru per sesi — row lama di-update setiap kali bot aktif.
 * Ini mencegah spam baris di tabel meski player sering aktifkan/nonaktifkan bot.
 *
 * Syarat: kolom `player_name` harus punya UNIQUE constraint.
 *   ALTER TABLE `afk_log` ADD UNIQUE KEY `uq_player_name` (`player_name`);
 *
 * Cara pakai:
 *   AfkLogger.start(player);                             // saat bot diaktifkan
 *   AfkLogger.updateDeduct(player, deducted, expTotal);  // tiap gems dipotong (per jam)
 *   AfkLogger.stop(player, reason, expTotal);            // saat bot berhenti
 */
@Slf4j
public class AfkLogger {

    // Berapa row riwayat lama (is_active=0) yang boleh tersisa per player.
    // Sisanya dihapus saat start() untuk menjaga tabel tetap bersih.
    private static final int MAX_HISTORY_ROWS = 5;

    // ── Start AFK ────────────────────────────────────────────────────────────
    public static void start(client.Player p) {
        if (p == null) return;
        long now = System.currentTimeMillis();

        // Hapus riwayat lama (is_active=0) jika melebihi MAX_HISTORY_ROWS
        pruneOldHistory(p.name);

        // UPSERT: update row aktif jika sudah ada, insert jika belum.
        // ON DUPLICATE KEY UPDATE memastikan hanya 1 row aktif per player_name.
        String sql =
            "INSERT INTO `afk_log` " +
            "(`player_name`,`player_id`,`map_id`," +
            " `level_start`,`level_current`,`exp_start`,`exp_gained`," +
            " `start_time`,`next_deduct`," +
            " `gems_start`,`gems_current`,`total_deducted`," +
            " `is_active`,`stop_reason`,`updated_at`) " +
            "VALUES (?,?,?, ?,?,?,0, ?,?, ?,?,0, 1,NULL,?) " +
            "ON DUPLICATE KEY UPDATE " +
            "  `player_id`      = VALUES(`player_id`)," +
            "  `map_id`         = VALUES(`map_id`)," +
            "  `level_start`    = VALUES(`level_start`)," +
            "  `level_current`  = VALUES(`level_current`)," +
            "  `exp_start`      = VALUES(`exp_start`)," +
            "  `exp_gained`     = 0," +
            "  `start_time`     = VALUES(`start_time`)," +
            "  `next_deduct`    = VALUES(`next_deduct`)," +
            "  `gems_start`     = VALUES(`gems_start`)," +
            "  `gems_current`   = VALUES(`gems_current`)," +
            "  `total_deducted` = 0," +
            "  `is_active`      = 1," +
            "  `stop_reason`    = NULL," +
            "  `updated_at`     = VALUES(`updated_at`)";

        try (Connection c = SQL.gI().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.name);
            ps.setInt   (2, p.objectId);
            ps.setInt   (3, p.map != null ? p.map.mapId : 0);
            ps.setShort (4, p.level);
            ps.setShort (5, p.level);
            ps.setLong  (6, p.exp);
            ps.setLong  (7, now);
            ps.setLong  (8, now + 3_600_000L);
            ps.setInt   (9, p.getGem());
            ps.setInt   (10, p.getGem());
            ps.setLong  (11, now);
            ps.executeUpdate();
            if (!c.getAutoCommit()) c.commit();
            log.info("[AfkLogger] start (upsert): player={} lv={} gems={}", p.name, p.level, p.getGem());
        } catch (Exception e) {
            log.warn("[AfkLogger] start error: {}", e.getMessage());
        }
    }

    // ── Update tiap potongan gems (setiap 1 jam) ─────────────────────────────
    public static void updateDeduct(client.Player p, int deducted, long expAtStart) {
        if (p == null) return;
        long now       = System.currentTimeMillis();
        long expGained = Math.max(0, expAtStart);

        String sql =
            "UPDATE `afk_log` SET " +
            " `gems_current`   = ?," +
            " `total_deducted` = `total_deducted` + ?," +
            " `next_deduct`    = ?," +
            " `level_current`  = ?," +
            " `exp_gained`     = ?," +
            " `updated_at`     = ? " +
            "WHERE `player_name` = ? AND `is_active` = 1";
        try (Connection c = SQL.gI().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt   (1, p.getGem());
            ps.setInt   (2, deducted);
            ps.setLong  (3, now + 3_600_000L);
            ps.setShort (4, p.level);
            ps.setLong  (5, expGained);
            ps.setLong  (6, now);
            ps.setString(7, p.name);
            ps.executeUpdate();
            if (!c.getAutoCommit()) c.commit();
            log.info("[AfkLogger] deduct: player={} lv={} expGained={} gems={}", p.name, p.level, expGained, p.getGem());
        } catch (Exception e) {
            log.warn("[AfkLogger] updateDeduct error: {}", e.getMessage());
        }
    }

    // ── Stop AFK ─────────────────────────────────────────────────────────────
    public static void stop(client.Player p, String reason, long expAtStart) {
        if (p == null) return;
        long now       = System.currentTimeMillis();
        long expGained = Math.max(0, expAtStart);

        String sql =
            "UPDATE `afk_log` SET " +
            " `is_active`     = 0," +
            " `gems_current`  = ?," +
            " `level_current` = ?," +
            " `exp_gained`    = ?," +
            " `stop_reason`   = ?," +
            " `updated_at`    = ? " +
            "WHERE `player_name` = ? AND `is_active` = 1";
        try (Connection c = SQL.gI().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt   (1, p.getGem());
            ps.setShort (2, p.level);
            ps.setLong  (3, expGained);
            ps.setString(4, reason);
            ps.setLong  (5, now);
            ps.setString(6, p.name);
            ps.executeUpdate();
            if (!c.getAutoCommit()) c.commit();
            log.info("[AfkLogger] stop: player={} reason={} expGained={}", p.name, reason, expGained);
        } catch (Exception e) {
            log.warn("[AfkLogger] stop error: {}", e.getMessage());
        }
    }

    // ── Hapus riwayat lama (is_active=0) jika melebihi MAX_HISTORY_ROWS ──────
    private static void pruneOldHistory(String playerName) {
        // Karena sekarang pakai UNIQUE per player_name, normalnya hanya ada 1 row.
        // Prune ini sebagai safety net jika constraint belum dipasang di DB lama.
        String sql =
            "DELETE FROM `afk_log` " +
            "WHERE `player_name` = ? AND `is_active` = 0 " +
            "ORDER BY `id` DESC " +
            "LIMIT 1000 OFFSET " + MAX_HISTORY_ROWS;
        try (Connection c = SQL.gI().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerName);
            int deleted = ps.executeUpdate();
            if (!c.getAutoCommit()) c.commit();
            if (deleted > 0) {
                log.info("[AfkLogger] pruneOldHistory: hapus {} row lama untuk player={}", deleted, playerName);
            }
        } catch (Exception e) {
            log.debug("[AfkLogger] pruneOldHistory error: {}", e.getMessage());
        }
    }
}
