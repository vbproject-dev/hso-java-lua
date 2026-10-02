package core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * SessionLog — Lacak sesi player yang sedang aktif di database.
 *
 * Prinsip: INSERT saat login, DELETE saat logout/crash.
 * Tabel ini HANYA berisi sesi yang sedang berjalan — bukan riwayat.
 * Saat server restart, baris yang tersisa = player yang sedang online
 * ketika server mati (crash / restart darurat).
 *
 * DDL tabel:
 * ──────────────────────────────────────────────────────────────
 * CREATE TABLE IF NOT EXISTS `session_active` (
 *   `id`           INT         NOT NULL AUTO_INCREMENT,
 *   `account_id`   INT         NOT NULL,
 *   `player_id`    INT         NOT NULL,
 *   `player_name`  VARCHAR(50) NOT NULL,
 *   `ip`           VARCHAR(45) NOT NULL DEFAULT '',
 *   `map_id`       TINYINT     NOT NULL DEFAULT 0,
 *   `x`            SMALLINT    NOT NULL DEFAULT 0,
 *   `y`            SMALLINT    NOT NULL DEFAULT 0,
 *   `hp`           INT         NOT NULL DEFAULT 0,
 *   `login_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
 *   PRIMARY KEY (`id`),
 *   UNIQUE KEY `uq_player` (`player_id`),
 *   KEY `idx_account`      (`account_id`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
 *   COMMENT='Hanya sesi aktif — dihapus saat logout/crash';
 * ──────────────────────────────────────────────────────────────
 */
@Slf4j
public class SessionLog {

    // ── Singleton ───────────────────────────────────────────────────────────
    private static SessionLog instance;
    public static SessionLog gI() {
        if (instance == null) instance = new SessionLog();
        return instance;
    }
    private SessionLog() {}

    // ───────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ───────────────────────────────────────────────────────────────────────

    /**
     * Dipanggil saat player berhasil masuk ke game (karakter sudah dipilih).
     * INSERT baris baru. Gunakan INSERT … ON DUPLICATE KEY UPDATE
     * untuk menangani kasus player login ulang sebelum baris lama terhapus.
     *
     * @return id baris yang baru dibuat (session_active.id), atau -1 jika gagal
     */
    public int onLogin(int accountId, int playerId, String playerName,
                       String ip, byte mapId, short x, short y, int hp) {
        String sql =
            "INSERT INTO session_active " +
            "(account_id, player_id, player_name, ip, map_id, x, y, hp, login_at) " +
            "VALUES (?,?,?,?,?,?,?,?,NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "  account_id=VALUES(account_id), player_name=VALUES(player_name), " +
            "  ip=VALUES(ip), map_id=VALUES(map_id), x=VALUES(x), y=VALUES(y), " +
            "  hp=VALUES(hp), login_at=NOW()";
        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql,
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, accountId);
            ps.setInt   (2, playerId);
            ps.setString(3, playerName);
            ps.setString(4, ip != null ? ip : "");
            ps.setByte  (5, mapId);
            ps.setShort (6, x);
            ps.setShort (7, y);
            ps.setInt   (8, hp);
            ps.executeUpdate();
            conn.commit();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int newId = rs.getInt(1);
                    log.info("[Session] LOGIN  player={} id={}", playerName, newId);
                    return newId;
                }
            }
            // ON DUPLICATE UPDATE: getGeneratedKeys bisa kosong — cari id manual
            return getIdByPlayer(playerId);
        } catch (SQLException e) {
            log.error("[Session] onLogin gagal player={}: {}", playerName, e.getMessage());
        }
        return -1;
    }

    /**
     * Dipanggil saat player logout normal ATAU masuk AFK.
     * Hapus baris dari tabel — sesi dianggap selesai.
     */
    public void onLogout(int sessionId, String playerName) {
        deleteById(sessionId);
        log.info("[Session] LOGOUT player={} sessionId={}", playerName, sessionId);
    }

    /**
     * Saat server restart/shutdown: HAPUS semua baris (semua sesi dianggap selesai).
     * Dipanggil dari SaveData.process() sebelum proses save berjalan.
     *
     * @return jumlah baris yang dihapus
     */
    //public int clearAll() {
    //    String sql = "DELETE FROM session_active";
    //    try (Connection conn = SQL.gI().getConnection();
    //         PreparedStatement ps = conn.prepareStatement(sql)) {
    //        int deleted = ps.executeUpdate();
    //        conn.commit();
    //        log.warn("[Session] clearAll: {} sesi dihapus (restart/shutdown)", deleted);
    //        return deleted;
    //    } catch (SQLException e) {
    //        log.error("[Session] clearAll gagal: {}", e.getMessage());
    //    }
    //    return 0;
    //}

    /**
     * Ambil semua sesi yang tersisa di tabel saat server baru nyala.
     * Sisa baris ini adalah player yang sedang online ketika server mati.
     * Berguna untuk recovery / notifikasi admin.
     */
    public List<ActiveSession> getResidual() {
        List<ActiveSession> result = new ArrayList<>();
        String sql = "SELECT * FROM session_active ORDER BY login_at DESC";
        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ActiveSession s = new ActiveSession();
                s.id         = rs.getInt("id");
                s.accountId  = rs.getInt("account_id");
                s.playerId   = rs.getInt("player_id");
                s.playerName = rs.getString("player_name");
                s.ip         = rs.getString("ip");
                s.mapId      = rs.getByte("map_id");
                s.x          = rs.getShort("x");
                s.y          = rs.getShort("y");
                s.hp         = rs.getInt("hp");
                s.loginAt    = rs.getString("login_at");
                result.add(s);
            }
        } catch (SQLException e) {
            log.error("[Session] getResidual gagal: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Jumlah sesi aktif saat ini (baris di tabel).
     */
    public int countActive() {
        String sql = "SELECT COUNT(*) FROM session_active";
        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log.error("[Session] countActive gagal: {}", e.getMessage());
        }
        return 0;
    }

    // ───────────────────────────────────────────────────────────────────────
    // PRIVATE helpers
    // ───────────────────────────────────────────────────────────────────────

    private void deleteById(int sessionId) {
        if (sessionId < 0) return;
        boolean wasInterrupted = Thread.interrupted(); // clear so HikariCP can acquire connection
        String sql = "DELETE FROM session_active WHERE id=?";
        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            log.error("[Session] deleteById gagal id={}: {}", sessionId, e.getMessage());
        } finally {
            if (wasInterrupted) Thread.currentThread().interrupt();
        }
    }

    private int getIdByPlayer(int playerId) {
        String sql = "SELECT id FROM session_active WHERE player_id=? LIMIT 1";
        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("[Session] getIdByPlayer gagal: {}", e.getMessage());
        }
        return -1;
    }

    // ───────────────────────────────────────────────────────────────────────
    // DTO
    // ───────────────────────────────────────────────────────────────────────

    /** Data satu sesi aktif. */
    public static class ActiveSession {
        public int    id;
        public int    accountId;
        public int    playerId;
        public String playerName;
        public String ip;
        public byte   mapId;
        public short  x;
        public short  y;
        public int    hp;
        public String loginAt;

        @Override
        public String toString() {
            return String.format("Session{id=%d, player=%s, map=%d, loginAt=%s}",
                    id, playerName, mapId, loginAt);
        }
    }
}
