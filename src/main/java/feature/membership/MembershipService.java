package feature.membership;

import client.Player;
import client.io.Session;
import core.Service;
import lombok.extern.slf4j.Slf4j;
import utils.SQLHelper;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * MembershipService — handle semua logika membership 7/30 hari.
 *
 * CARA INTEGRASI:
 * 1. Panggil MembershipService.onLogin(session) di Session.loadAccount() setelah data account di-load.
 * 2. Panggil MembershipService.claimDailyGem(player) setiap kali player login / masuk map pertama
 *    (setelah p0.setup() berhasil).
 *
 * Pastikan sudah menjalankan SQL migration: 01_membership_schema.sql
 */
@Slf4j
public class MembershipService {

    // ── KONFIGURASI REWARD ────────────────────────────────────────────────────
    /** Kimcuong harian untuk member 7 hari */
    private static final int GEM_DAILY_7DAY = 5;
    /** Kimcuong harian untuk member 30 hari */
    private static final int GEM_DAILY_30DAY = 15;

    /**
     * Item reward saat aktivasi membership 7 hari.
     * Format: {bag, item_id, quantity, upgrade}
     * Contoh: bag=7 (item7/equipment), bag=3 (item3/consumable), bag=4 (item4/misc)
     */
    private static final int[][] ACTIVATION_ITEMS_7DAY = {
        // {bag, item_id, quantity, upgrade}
        {3, 501, 3, 0},  // Contoh: 3x Activation Box 7Day (bag3)
        {4, 101, 1, 0},  // Contoh: 1x Special Scroll (bag4)
    };

    /**
     * Item reward saat aktivasi membership 30 hari.
     */
    private static final int[][] ACTIVATION_ITEMS_30DAY = {
        // {bag, item_id, quantity, upgrade}
        {3, 502, 5, 0},  // Contoh: 5x Activation Box 30Day (bag3)
        {4, 102, 2, 0},  // Contoh: 2x Premium Scroll (bag4)
        {7, 600, 1, 5},  // Contoh: 1x Premium Weapon +5 (bag7)
    };
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Dipanggil saat player login (di Session.loadAccount()).
     * Load data membership dari DB ke Session.
     *
     * DIMATIKAN SEMENTARA: kolom membership_type / membership_expire belum ada
     * di tabel `account`, jadi query di bawah selalu gagal (SQLException: Column
     * 'membership_type' not found) dan bikin login error / disconnect.
     * Method ini langsung return supaya fitur membership tidak aktif dulu.
     * Kalau nanti migration SQL-nya sudah dijalankan, hapus baris "if (true) return;"
     * di bawah untuk mengaktifkan kembali.
     */
    public static void onLogin(Session session) {
        if (true) return; // fitur membership dimatikan — lihat catatan di atas

        if (session == null || session.user == null) return;

        try {
            Boolean ok = SQLHelper.selectFrom("account")
                    .where("user", session.user)
                    .firstAs((ResultSet rs) -> {
                        session.membership_type = rs.getByte("membership_type");
                        session.membership_expire = rs.getLong("membership_expire");
                        return true;
                    });

            if (ok == null) return;

            // Cek expired — kalau sudah lewat, reset ke 0
            long now = System.currentTimeMillis();
            if (session.membership_expire > 0 && now > session.membership_expire) {
                expireMembership(session.user);
                session.membership_type = 0;
                session.membership_expire = 0;
                log.info("[Membership] {} expired, reset ke None.", session.user);
            }
        } catch (Exception e) {
            log.error("[Membership] onLogin error untuk user {}: {}", session.user, e.getMessage());
        }
    }

    /**
     * Cek & berikan kimcuong harian untuk member aktif.
     * Dipanggil saat player login / masuk map pertama (setelah setup() berhasil).
     * Hanya memberi sekali per hari (berdasarkan kolom member_claimed_date di DB).
     */
    public static void claimDailyGem(Player player) {
        if (player == null || player.conn == null) return;
        Session session = player.conn;

        if (session.membership_type == 0) return; // bukan member
        if (System.currentTimeMillis() > session.membership_expire) return; // expired

        int gemAmount = (session.membership_type == 2) ? GEM_DAILY_30DAY : GEM_DAILY_7DAY;
        String today = LocalDate.now(ZoneId.of("Asia/Jakarta")).toString(); // "2026-06-15"

        try {
            // Cek apakah sudah claim hari ini
            String lastClaimed = SQLHelper.selectFrom("account")
                    .where("user", session.user)
                    .firstAs(rs -> rs.getString("member_claimed_date"));

            if (today.equals(lastClaimed)) {
                return; // Sudah claim hari ini
            }

            // Update tanggal claim
            SQLHelper.update("account")
                    .set("member_claimed_date", today)
                    .where("user", session.user)
                    .execute();

            // Beri kimcuong ke player (in-game real-time)
            player.updateGem(gemAmount);

            // Log reward
            logReward(session.user, "daily_gem", gemAmount, 1);

            log.info("[Membership] {} mendapat {} kimcuong harian (type={}).",
                    session.user, gemAmount, session.membership_type);

            // Notifikasi ke player
            String memberName = session.membership_type == 2 ? "30 Hari" : "7 Hari";
            try {
                Service.send_notice_box(session, "Membership " + memberName + ": +" + gemAmount + " Kimcuong harian!");
            } catch (Exception ignored) {
            }

        } catch (Exception e) {
            log.error("[Membership] claimDailyGem error untuk {}: {}", session.user, e.getMessage());
        }
    }

    /**
     * Aktivasi membership untuk player — dipanggil saat admin approve order di web.
     * Web PHP UPDATE langsung ke DB, method ini untuk aktivasi in-game jika player sedang online.
     *
     * Catatan: Web PHP sudah handle UPDATE account secara langsung ke DB.
     * Method ini opsional — untuk trigger in-game jika server punya command socket ke web.
     */
    public static void activateMembership(String username, int type, int durationDays) {
        long now = System.currentTimeMillis();
        long expire = now + (long) durationDays * 24 * 60 * 60 * 1000L;

        try {
            // Kalau sudah punya membership aktif, extend dari expire yang ada
            Long currentExpire = SQLHelper.selectFrom("account")
                    .where("user", username)
                    .firstAs(rs -> rs.getLong("membership_expire"));

            if (currentExpire != null && currentExpire > now) {
                // Extend dari waktu expire yang ada
                expire = currentExpire + (long) durationDays * 24 * 60 * 60 * 1000L;
            }

            // Update membership di DB
            SQLHelper.update("account")
                    .set("membership_type", (byte) type)
                    .set("membership_expire", expire)
                    .where("user", username)
                    .execute();

            // Beri item reward aktivasi
            giveActivationItems(username, type);

            log.info("[Membership] {} aktif type={} selama {} hari, expire={}.", username, type, durationDays, expire);

        } catch (Exception e) {
            log.error("[Membership] activateMembership error untuk {}: {}", username, e.getMessage());
        }
    }

    /**
     * Beri item reward aktivasi ke player.
     * Jika player sedang online: tambah ke bag langsung (lihat PATCH_INSTRUCTIONS.md pilihan B).
     * Jika offline: log dulu — sesuaikan dengan sistem gift/item-box server kamu.
     *
     * NOTE: Sesuaikan dengan sistem item server kamu (itembox / inject langsung ke GameMap).
     */
    private static void giveActivationItems(String username, int type) {
        int[][] items = (type == 2) ? ACTIVATION_ITEMS_30DAY : ACTIVATION_ITEMS_7DAY;

        for (int[] item : items) {
            int bag = item[0];
            int itemId = item[1];
            int quantity = item[2];
            logReward(username, "activation_item_bag" + bag, itemId, quantity);
            log.info("[Membership] Reward aktivasi untuk {}: bag={} id={} qty={}",
                    username, bag, itemId, quantity);
        }
    }

    /**
     * Reset membership saat expire.
     */
    private static void expireMembership(String username) {
        try {
            SQLHelper.update("account")
                    .set("membership_type", (byte) 0)
                    .set("membership_expire", 0L)
                    .where("user", username)
                    .execute();
        } catch (Exception e) {
            log.error("[Membership] expireMembership error: {}", e.getMessage());
        }
    }

    /** Catat log pemberian reward ke DB. */
    private static void logReward(String username, String type, int value, int quantity) {
        try {
            SQLHelper.insert("membership_reward_log")
                    .value("username", username)
                    .value("type", type)
                    .value("value", value)
                    .value("quantity", quantity)
                    .execute();
        } catch (Exception e) {
            log.warn("[Membership] logReward gagal: {}", e.getMessage());
        }
    }

    /** Cek apakah player saat ini punya membership aktif. */
    public static boolean isActiveMember(Session session) {
        if (session == null || session.membership_type == 0) return false;
        return System.currentTimeMillis() < session.membership_expire;
    }

    /** Ambil sisa waktu membership dalam jam. */
    public static long getRemainingHours(Session session) {
        if (!isActiveMember(session)) return 0;
        return (session.membership_expire - System.currentTimeMillis()) / (1000 * 60 * 60);
    }
}