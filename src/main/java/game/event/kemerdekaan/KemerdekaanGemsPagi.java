package game.event.kemerdekaan;

import client.Player;
import core.Manager;
import core.Service;
import game.event.GameEvent;
import game.items.ItemManager;
import game.items.models.ItemReward;
import game.map.GameMap;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * KemerdekaanGemsPagi — Hadiah Pagi 17 Agustus
 *
 * ── Konsep ───────────────────────────────────────────────────────────────────
 *  Setiap tanggal 17 Agustus, jam 07:00 - 08:00 (waktu server / WIB), SEMUA
 *  player yang berada di MAP_ID akan otomatis diberi hadiah. Tidak perlu
 *  daftar atau klaim manual — cukup berada di map saat jam tersebut, hadiah
 *  otomatis masuk sekali per player selama jendela waktu ini.
 *  Player yang masuk ke map di tengah jam 07:00-08:00 tetap kebagian hadiah
 *  (dicek tiap detik lewat onUpdate(), bukan cuma sekali di awal jam 07:00).
 *
 * ── Isi hadiah ────────────────────────────────────────────────────────────
 *  Hadiah TIDAK cuma Gems — bisa dicampur equipment, potion (item4), dan
 *  material (item7) sekaligus, pakai format JSON yang SAMA seperti kolom
 *  reward_item3/4/7 di tabel item_exchange (lihat REWARD_* di bawah).
 *  Kosongkan "[]" kalau kategori itu tidak dipakai.
 *
 * ── Kenapa perlu guard tanggal manual? ──────────────────────────────────────
 *  GameEvent cuma bisa dijadwalkan lewat DayOfWeek + jam (lihat konstruktor),
 *  TIDAK BISA jadwal tanggal kalender spesifik (17 Agustus) secara langsung.
 *  Makanya di sini days diisi SEMUA hari (supaya jam 07:00-08:00 selalu
 *  dicek tiap hari), tapi onStart()/onUpdate() menolak melakukan apapun
 *  kecuali tanggal hari ini memang benar-benar 17 Agustus (lihat
 *  isTanggal17Agustus()). Pola ini sama seperti guard tanggal 10-21 Agustus
 *  di KemerdekaanEvent.java.
 *
 * ── Daftarkan di GameEventManager ────────────────────────────────────────────
 *  addEvent(new KemerdekaanGemsPagi());
 */
public class KemerdekaanGemsPagi extends GameEvent {

    // ── Konstanta map ─────────────────────────────────────────────────────────
    public static final byte MAP_ID = 116; // Ganti sesuai map yang mau dikasih hadiah

    // ── Konstanta hadiah ─────────────────────────────────────────────────────
    // Format SAMA seperti kolom reward_item3/reward_item4/reward_item7 di
    // tabel item_exchange:
    //   REWARD_EQUIPMENT_JSON -> array id item3, contoh: "[1001,1002]"
    //   REWARD_POTION_JSON    -> array [id, qty] item4, contoh: "[[201,5],[202,3]]"
    //   REWARD_MATERIAL_JSON  -> array [id, qty] item7, contoh: "[[474,10]]"
    // Kosongkan jadi "[]" kalau kategori itu tidak mau dikasih.
    private static final String REWARD_EQUIPMENT_JSON = "[]";
    private static final String REWARD_POTION_JSON    = "[]";
    private static final String REWARD_MATERIAL_JSON  = "[]";
    private static final long   REWARD_EXP  = 0;
    private static final long   REWARD_GOLD = 10000000;
    private static final long   REWARD_GEM  = 5000;

    // ── State ─────────────────────────────────────────────────────────────────
    // Player yang sudah dapat hadiah selama jendela waktu 07:00-08:00 hari ini
    // -> dicegah dapat berkali-kali tiap tick onUpdate(). Direset tiap kali
    // event mulai lagi (onStart()).
    private final java.util.Set<Player> rewarded = new CopyOnWriteArraySet<>();

    // ── Constructor ───────────────────────────────────────────────────────────
    public KemerdekaanGemsPagi() {
        super(
            "KEMERDEKAAN_GEMS_PAGI",
            EnumSet.allOf(DayOfWeek.class),
            List.of(new TimeEvent(LocalTime.of(7, 0), LocalTime.of(12, 0)))
        );
    }

    // ── GameEvent lifecycle ───────────────────────────────────────────────────

    @Override
    protected boolean isEnabled() {
        return Manager.gI().event_kemerdekaan_gems_pagi;
    }

    @Override
    protected void onStart() {
        // Kecuali GM yang memaksa start manual (buat testing), tolak start
        // kalau hari ini bukan 17 Agustus.
        if (!isManualOverride() && !isTanggal17Agustus()) return;

        rewarded.clear();
        broadcastToMap(
            "[Kedaulatan NKRI] Hadiah Pagi Kemerdekaan telah dibuka! " +
            "Semua Ksatria yang ada di sini dapat hadiah spesial selama 1 jam ke depan!"
        );
    }

    @Override
    protected void onEnd() {
        broadcastToMap("[Kedaulatan NKRI] Waktu hadiah pagi telah berakhir. Sampai jumpa tahun depan!");
        rewarded.clear();
    }

    @Override
    protected void onUpdate() {
        if (!isActive()) return;
        if (!isManualOverride() && !isTanggal17Agustus()) return;

        GameMap[] maps = GameMap.getMapById(MAP_ID);
        if (maps == null) return;

        for (GameMap map : maps) {
            if (map == null) continue;
            for (Player p : new CopyOnWriteArrayList<>(map.players)) {
                if (p == null || rewarded.contains(p)) continue;
                rewarded.add(p);

                // Reward baru dibangun tiap player (bukan di-cache/dipakai
                // ulang) supaya tiap player dapat instance Item3/Item47
                // sendiri, bukan objek yang sama-sama dipakai bareng.
                ItemReward reward = ItemReward.fromJsonArray(
                    REWARD_EQUIPMENT_JSON, REWARD_POTION_JSON, REWARD_MATERIAL_JSON,
                    REWARD_EXP, REWARD_GOLD, REWARD_GEM
                );
                ItemManager.getInstance().sendReward(p.conn, reward);
            }
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** TRUE hanya kalau tanggal server sekarang persis 17 Agustus (tahun berapa pun). */
    private boolean isTanggal17Agustus() {
        LocalDate today = LocalDate.now();
        return today.getMonthValue() == 8 && today.getDayOfMonth() == 17;
    }

    /** Broadcast ke semua player yang sedang ada di MAP_ID (bukan server-wide). */
    private void broadcastToMap(String msg) {
        GameMap[] maps = GameMap.getMapById(MAP_ID);
        if (maps == null) return;

        for (GameMap map : maps) {
            if (map == null) continue;
            for (Player p : map.players) {
                try {
                    Service.send_notice_nobox_yellow(p.conn, msg);
                } catch (java.io.IOException ignore) {}
            }
        }
    }

    // ── Trigger manual GM (buat testing di luar tanggal 17 Agustus) ──────────

    public String adminForceStart() {
        if (isActive()) {
            return "Hadiah Pagi Kemerdekaan sedang berjalan, tidak bisa dimulai lagi.";
        }
        boolean ok = forceStart();
        return ok ? "Hadiah Pagi Kemerdekaan berhasil diaktifkan manual!"
                   : "Gagal mengaktifkan Hadiah Pagi Kemerdekaan.";
    }

    public String adminForceEnd() {
        if (!isManualOverride()) {
            return "Hadiah Pagi Kemerdekaan tidak sedang berjalan secara manual.";
        }
        boolean ok = forceEnd();
        return ok ? "Hadiah Pagi Kemerdekaan telah dihentikan paksa."
                   : "Gagal menghentikan Hadiah Pagi Kemerdekaan.";
    }
}

