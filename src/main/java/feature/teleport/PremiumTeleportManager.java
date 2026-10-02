package feature.teleport;

import client.Player;
import client.io.Message;
import client.io.Session;
import core.SQL;
import core.Service;
import game.map.GameMap;
import model.map.Vgo;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.*;

/**
 * PremiumTeleportManager
 *
 * Mengelola sesi teleport premium player.
 *
 * ────────────────────────────────────────────────
 * Skema Map Premium (rantai linear):
 *
 *   [Luar] ──► 115 Desa Ajaib  ──► 111 Elan Pletau
 *                                        │
 *                               112 Queer Wood ◄──┘
 *                                        │
 *                               113 Emerald Cave ◄──┘
 *                                        │
 *                               114 Dagnues Nest ◄──┘
 *          
 *
 *
 * Semua map 111-115 adalah area premium.
 * Player masuk dari luar ke map 115 (entry point).
 * Warp antar map tersedia di setiap ujung map.
 * Saat sesi habis → player dikick ke Map 115 (RETURN_MAP).
 * ────────────────────────────────────────────────
 *
 * Harga Gems  : 1 jam = 3000, 2 jam = 6000, 3 jam = 9000
 * Item4 ID    : 319 (Tiket Teleport Premium) — 1 tiket = 1 jam
 *
 * Persistensi sesi ke DB:
 *   Tabel `premium_session` menyimpan sisa durasi saat server mati.
 *   Saat server naik kembali, sisa durasi di-load otomatis.
 *   DDL (jalankan sekali di DB):
 *
 *   CREATE TABLE IF NOT EXISTS `premium_session` (
 *     `player_id`   INT          NOT NULL PRIMARY KEY,
 *     `remaining_ms` BIGINT      NOT NULL,
 *     `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
 *                                ON UPDATE CURRENT_TIMESTAMP
 *   ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 */
@Slf4j
public class PremiumTeleportManager {

    // ---------------------------------------------------------------
    // Konstanta Map
    // ---------------------------------------------------------------
    /** Semua map premium eksklusif — butuh sesi aktif untuk masuk */
    public static final int[] PREMIUM_MAP_IDS = {115, 111, 112, 113, 114, 2}; // tambahan baru map area tambang map id 2

    /** Map entry point — semua player boleh masuk, NPC jual paket ada di sini */
    public static final int GATEWAY_MAP_ID = 115;

    /** Map tujuan kickback saat sesi habis */
    public static final int RETURN_MAP_ID = 115;

    // Koordinat spawn di Map 115 (Desa Ajaib) — NPC -102 ada di (288,288)
    // Spawn player sedikit di sebelah kiri NPC
    public static final int RETURN_X = 288;
    public static final int RETURN_Y = 288;

    // ---------------------------------------------------------------
    // Konstanta Harga
    // ---------------------------------------------------------------
    public static final int GEMS_PER_HOUR   = 3000;
    public static final int ITEM4_TICKET_ID = 319;  // Tiket Teleport Premium

    // ---------------------------------------------------------------
    // Singleton
    // ---------------------------------------------------------------
    private static PremiumTeleportManager instance;

    public static PremiumTeleportManager gI() {
        if (instance == null) {
            instance = new PremiumTeleportManager();
        }
        return instance;
    }

    // ---------------------------------------------------------------
    // State
    // activeSession : objectId -> expiry timestamp (ms)  — player ONLINE
    // pausedSession : objectId -> sisa durasi (ms)       — player OFFLINE
    // ---------------------------------------------------------------
    private final ConcurrentHashMap<Integer, Long> activeSession = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> pausedSession = new ConcurrentHashMap<>();

    /**
     * Throttle: waktu terakhir countdown dikirim per player (ms).
     * Countdown hanya dikirim tiap ~1 detik.
     */
    private final ConcurrentHashMap<Integer, Long> lastCountdownTick = new ConcurrentHashMap<>();

    /**
     * Tracker: apakah notifikasi "15 menit tersisa" sudah dikirim ke player.
     * Direset saat sesi baru dimulai.
     */
    private final ConcurrentHashMap<Integer, Boolean> notified15Min = new ConcurrentHashMap<>();

    /**
     * Threshold auto-extend: saat sisa waktu sesi ≤ ini, sistem SUDAH coba
     * pakai tiket duluan (proaktif) — SEBELUM sesi benar-benar habis.
     * Dengan ini player tidak pernah sempat ke-kick/keluar map premium
     * selama tiket masih ada, karena sesi diperpanjang sebelum angkanya
     * sempat menyentuh 0.
     * Nilainya sengaja > interval checker background (30 detik) supaya
     * checkExpiredSessions() pasti sempat menangkapnya minimal sekali
     * sebelum expiry walau onPlayerMapUpdate() (hook real-time) kebetulan
     * tidak terpanggil (mis. player idle/AFK tanpa aksi apa pun).
     */
    private static final long AUTO_EXTEND_THRESHOLD_MS = 60 * 1000L;

    /** Threshold notifikasi sisa waktu: 15 menit dalam ms */
    private static final long NOTIFY_15MIN_MS = 15 * 60 * 1000L;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "PremiumTeleport-Checker");
                t.setDaemon(true);
                return t;
            });

    // FIX (defense-in-depth alongside the Session.disconnect() idempotency
    // fix): pauseSession() is called synchronously from Session.disconnect()
    // on the player's own receiv thread every time a player logs out, and it
    // used to do a blocking JDBC upsert right there. Under DB/pool pressure
    // this could make disconnect() sit in a blocking wait like the
    // Wedding.saveItem() incident did on the combat path. shutdown() still
    // saves synchronously below (see shutdown()) because the process may
    // exit right after — that save must complete before returning.
    private final ExecutorService saveExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "PremiumTeleport-Save");
                t.setDaemon(true);
                return t;
            });

    // ---------------------------------------------------------------
    // Init — dipanggil dari Manager/Start saat server boot
    // ---------------------------------------------------------------
    public void init() {
        ensureTableExists();
        loadSessionsFromDB();
        scheduler.scheduleAtFixedRate(this::checkExpiredSessions, 30, 30, TimeUnit.SECONDS);
        log.info("[PremiumTeleport] Manager started — checker every 30s");
        log.info("[PremiumTeleport] Premium maps: 115(gateway) → 111 → 112 → 113 → 114");
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Cek apakah player sedang punya sesi aktif.
     */
    public boolean hasActiveSession(Player p) {
        Long expiry = activeSession.get(p.objectId);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            activeSession.remove(p.objectId);
            removeSessionFromDB(p.objectId);
            return false;
        }
        return true;
    }

    /**
     * Sisa waktu sesi dalam detik.
     * Mengembalikan sisa aktif (online) atau sisa paused (offline). 0 jika tidak ada sesi.
     */
    public long getRemainingSeconds(Player p) {
        Long expiry = activeSession.get(p.objectId);
        if (expiry != null) {
            long diff = expiry - System.currentTimeMillis();
            return diff > 0 ? diff / 1000 : 0;
        }
        Long paused = pausedSession.get(p.objectId);
        return (paused != null && paused > 0) ? paused / 1000 : 0;
    }

    /**
     * Mulai / perpanjang sesi premium untuk player.
     * Jika ada sisa durasi dari sesi yang di-pause (offline), lanjutkan dari sana.
     *
     * @param p     player
     * @param hours jumlah jam (1, 2, atau 3)
     */
    public void startSession(Player p, int hours) {
        long now = System.currentTimeMillis();

        Long paused = pausedSession.remove(p.objectId);
        Long expiry = activeSession.get(p.objectId);

        long base;
        if (paused != null && paused > 0) {
            base = now + paused;
        } else {
            base = (expiry != null && expiry > now) ? expiry : now;
        }

        long newExpiry = base + TimeUnit.HOURS.toMillis(hours);
        activeSession.put(p.objectId, newExpiry);
        notified15Min.remove(p.objectId);   // reset flag notifikasi 15 menit

        // FIX: simpan newExpiry sebagai timestamp ABSOLUT (bukan remaining_ms
        // relatif). Timestamp absolut tetap valid dibaca kapan saja, tidak
        // basi walau server crash tanpa sempat memanggil shutdown().
        saveActiveExpiryToDB(p.objectId, newExpiry);

        log.info("[PremiumTeleport] Player {} started {}h session, expiry={}", p.name, hours, newExpiry);
        sendSessionCountdown(p);
    }

    /**
     * Dipanggil saat player OFFLINE (disconnect/logout).
     * Timer di-pause: sisa waktu disimpan ke DB, activeSession dihapus.
     */
    public void pauseSession(Player p) {
        Long expiry = activeSession.remove(p.objectId);
        if (expiry == null) return;

        long remaining = expiry - System.currentTimeMillis();
        int playerId = p.objectId;
        String playerName = p.name;
        if (remaining > 0) {
            pausedSession.put(p.objectId, remaining);
            // Player offline → waktu berhenti berjalan, jadi remaining_ms
            // relatif AMAN disimpan di sini (tidak akan basi selama masih paused).
            // FIX: dispatch ke background executor supaya thread yang lagi
            // menjalankan Session.disconnect() (mis. thread receiv player)
            // tidak pernah menunggu I/O database di sini.
            saveExecutor.execute(() -> savePausedRemainingToDB(playerId, remaining));
            log.info("[PremiumTeleport] Player {} went offline, timer paused — {}s remaining (saved to DB)",
                    playerName, remaining / 1000);
        } else {
            pausedSession.remove(p.objectId);
            saveExecutor.execute(() -> removeSessionFromDB(playerId));
        }
    }

    /**
     * Dipanggil saat player ONLINE kembali (login).
     * Timer dilanjutkan dari sisa durasi yang tersimpan (memory atau DB).
     */
    public void resumeSession(Player p) {
        long now = System.currentTimeMillis();
        Long remaining = pausedSession.remove(p.objectId);

        // Jika tidak ada di memory, coba ambil dari DB (case: server restart normal,
        // player memang sempat logout dengan benar → row berstatus 'paused').
        if (remaining == null || remaining <= 0) {
            remaining = loadPausedRemainingFromDB(p.objectId);
        }

        // FIX bug refill: kalau tidak ada row 'paused' tapi ADA row 'active',
        // berarti server sebelumnya mati mendadak (crash/kill) saat player masih
        // online — jangan pakai remaining_ms lama (basi), hitung ulang dari
        // expire_at absolut dikurangi waktu sekarang. Kalau sudah lewat, sesi
        // memang sudah habis dan TIDAK di-refill.
        if (remaining == null || remaining <= 0) {
            Long expireAt = loadActiveExpiryFromDB(p.objectId);
            if (expireAt != null) {
                long real = expireAt - now;
                remaining = real > 0 ? real : 0L;
                if (remaining <= 0) {
                    log.info("[PremiumTeleport] Player {} sesi sudah habis saat server mati (crash-safe check), tidak di-refill.", p.name);
                    removeSessionFromDB(p.objectId);
                }
            }
        }

        if (remaining == null || remaining <= 0) return;

        long newExpiry = now + remaining;
        activeSession.put(p.objectId, newExpiry);

        // Simpan kembali sebagai expire_at absolut, state='active'
        saveActiveExpiryToDB(p.objectId, newExpiry);

        log.info("[PremiumTeleport] Player {} back online, timer resumed — {}s remaining",
                p.name, remaining / 1000);
        sendSessionCountdown(p);
    }

    /**
     * Coba perpanjang sesi player SECARA OTOMATIS memakai Tiket (item4 id
     * {@link #ITEM4_TICKET_ID}) yang ada di tas, tanpa perlu buka menu NPC.
     *
     * Dipanggil tepat sebelum player di-kick karena sesi habis (baik dari
     * checker background {@link #checkExpiredSessions()} maupun hook
     * real-time {@link #onPlayerMapUpdate(Player)}). Kalau player punya
     * minimal 1 tiket, 1 tiket langsung dipakai (dikurangi dari tas) dan
     * sesi diperpanjang 1 jam lewat {@link #startSession(Player, int)} —
     * sama persis efeknya dengan pilih menu "Pakai Tiket" manual.
     *
     * Dipanggil ulang tiap kali sesi habis lagi, jadi selama tiket masih
     * ada di tas, player terus otomatis diperpanjang 1 jam demi 1 jam
     * tanpa pernah ke-kick — begitu tiket habis, baru kick berjalan normal.
     *
     * @param p player yang sesinya mau/sudah habis
     * @return true jika berhasil diperpanjang otomatis (tiket ditemukan &
     *         terpakai), false jika tidak ada tiket (proses kick lanjut normal)
     */
    public boolean tryAutoExtendWithTicket(Player p) {
        if (p == null || p.item == null) return false;

        int owned = p.item.total_item_by_id(4, ITEM4_TICKET_ID);
        if (owned < 1) return false;

        p.item.remove(4, ITEM4_TICKET_ID, 1);
        p.item.updateBag();

        startSession(p, 1); // 1 tiket = 1 jam, sama seperti pembelian manual

        try {
            if (p.conn != null) {
                Service.send_notice_box(p.conn,
                        "🎫 Tiket Teleport Premium otomatis terpakai!\n" +
                        "Sesi kamu diperpanjang +1 jam.");
            }
        } catch (Exception ignore) {}

        log.info("[PremiumTeleport] Player {} auto-extended +1h using ticket (item4 id {}), sisa tiket: {}",
                p.name, ITEM4_TICKET_ID, owned - 1);
        return true;
    }

    /**
     * Hapus sesi sepenuhnya (admin reset / expired total).
     */
    public void removeSession(Player p) {
        activeSession.remove(p.objectId);
        pausedSession.remove(p.objectId);
        lastCountdownTick.remove(p.objectId);
        notified15Min.remove(p.objectId);
        removeSessionFromDB(p.objectId);
    }

    /**
     * Cek apakah mapId termasuk area premium (111-115).
     */
    public static boolean isPremiumMap(int mapId) {
        for (int id : PREMIUM_MAP_IDS) {
            if (id == mapId) return true;
        }
        return false;
    }

    /**
     * Cek apakah mapId adalah gateway premium (115).
     */
    public static boolean isGatewayMap(int mapId) {
        return mapId == GATEWAY_MAP_ID;
    }

    /**
     * Cek apakah player boleh masuk ke area premium.
     *
     * Aturan:
     * - Map 115 (gateway) = BEBAS, siapapun boleh masuk
     * - Map 111-114 = butuh sesi aktif (tidak perlu cek asal map)
     *
     * Warp antar-map sudah tersedia di SQL sehingga perpindahan
     * 115→111→112→113→114 (dan balik) mengikuti alur normal game.
     *
     * @param p         player yang mencoba masuk
     * @param toMapId   map tujuan
     * @return true jika diizinkan masuk
     */
    public boolean canEnterPremiumMap(Player p, int toMapId) {
        if (!isPremiumMap(toMapId)) return true;      // bukan premium → bebas
        if (isGatewayMap(toMapId)) return true;       // map 115 → bebas masuk
        return hasActiveSession(p);                    // map 111-114 → butuh sesi
    }

    // ---------------------------------------------------------------
    // Background checker — tiap 30 detik
    // ---------------------------------------------------------------
    private void checkExpiredSessions() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, Long> entry : activeSession.entrySet()) {
            int playerId = entry.getKey();
            long expiry  = entry.getValue();

            // ── Waktu mau/sudah habis (≤ threshold) → coba auto-extend PROAKTIF
            //    pakai tiket dulu, sebelum sempat benar-benar 0 & kick ──────────
            long remainingCheck = expiry - now;
            if (remainingCheck <= AUTO_EXTEND_THRESHOLD_MS) {
                Player onlinePlayer = null;
                for (Session session : Session.SESSION_LIST) {
                    if (session != null && session.p != null && session.p.objectId == playerId) {
                        onlinePlayer = session.p;
                        break;
                    }
                }

                if (onlinePlayer != null && tryAutoExtendWithTicket(onlinePlayer)) {
                    // Berhasil diperpanjang otomatis pakai tiket — skip, sesi
                    // ini sudah aman sampai next check, tidak pernah sampai 0
                    continue;
                }
            }

            // ── Sesi benar-benar sudah habis (tiket juga tidak ada) → kick ──
            if (now >= expiry) {
                activeSession.remove(playerId);
                removeSessionFromDB(playerId);

                for (Session session : Session.SESSION_LIST) {
                    try {
                        if (session == null || session.p == null) continue;
                        Player p = session.p;
                        if (p.objectId != playerId) continue;

                        if (!isPremiumMap(p.map.mapId) || isGatewayMap(p.map.mapId)) continue;

                        Vgo vgo = Vgo.create(RETURN_MAP_ID, RETURN_X, RETURN_Y);
                        p.changeMap(p, vgo);
                        Service.send_notice_box(session,
                                "⏰ Waktu Premium Teleport kamu telah habis!\nKamu telah dipindahkan ke Desa Azure.");
                        notified15Min.remove(playerId);
                        log.info("[PremiumTeleport] Kicked player {} from map {} (time expired)",
                                p.name, p.map.mapId);
                    } catch (Exception e) {
                        log.error("[PremiumTeleport] Error kicking player {}: {}", playerId, e.getMessage());
                    }
                }
                continue;
            }

            // ── Sesi aktif — cek apakah sisa waktu ≤ 15 menit ──────────
            long remaining = expiry - now;
            if (remaining <= NOTIFY_15MIN_MS && !Boolean.TRUE.equals(notified15Min.get(playerId))) {
                notified15Min.put(playerId, Boolean.TRUE);

                for (Session session : Session.SESSION_LIST) {
                    try {
                        if (session == null || session.p == null) continue;
                        Player p = session.p;
                        if (p.objectId != playerId) continue;

                        long minsLeft = remaining / 60_000;
                        Service.send_notice_box(session,
                                "⚠️ Perhatian! Sesi Premium Teleport kamu tersisa " + minsLeft + " menit lagi!\nSegera perpanjang agar tidak dipindahkan.");
                        log.info("[PremiumTeleport] Sent 15-min warning to player {} ({}min left)",
                                p.name, minsLeft);
                    } catch (Exception e) {
                        log.error("[PremiumTeleport] Error sending 15-min warning to player {}: {}",
                                playerId, e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Hook dari GameMap.update() / Player.update() sebagai backup checker.
     */
    public void onPlayerMapUpdate(Player p) {
        // Hanya berlaku di map 111-114 (bukan gateway 115)
        if (!isPremiumMap(p.map.mapId) || isGatewayMap(p.map.mapId)) return;

        // Kirim countdown waktu sesi tiap 1 detik ke client (contek pola BTF sendUpdateTime)
        if (hasActiveSession(p)) {
            long now = System.currentTimeMillis();
            Long lastTick = lastCountdownTick.get(p.objectId);
            if (lastTick == null || now - lastTick >= 1_000L) {
                lastCountdownTick.put(p.objectId, now);
                sendSessionCountdown(p);
            }

            // Notifikasi 15 menit tersisa (backup — jika checker 30 detik terlewat)
            Long expiry = activeSession.get(p.objectId);
            if (expiry != null) {
                long remaining = expiry - now;

                // Waktu mau habis (≤ threshold) → auto-extend PROAKTIF pakai
                // tiket SEBELUM sempat menyentuh 0, jadi player tidak pernah
                // benar-benar keluar/ke-kick dari map premium selama tiket ada.
                if (remaining <= AUTO_EXTEND_THRESHOLD_MS) {
                    tryAutoExtendWithTicket(p);
                    // berhasil atau tidak, lanjut normal — kalau berhasil,
                    // hasActiveSession(p) di panggilan berikutnya sudah pakai
                    // expiry baru; kalau gagal (tiket habis), checker 30 detik
                    // atau pengecekan berikut yang akan proses kick begitu
                    // sesi benar-benar habis.
                }

                if (remaining <= NOTIFY_15MIN_MS && !Boolean.TRUE.equals(notified15Min.get(p.objectId))) {
                    notified15Min.put(p.objectId, Boolean.TRUE);
                    try {
                        long minsLeft = remaining / 60_000;
                        Service.send_notice_box(p.conn,
                                "⚠️ Perhatian! Sesi Premium Teleport kamu tersisa " + minsLeft + " menit lagi!\nSegera perpanjang agar tidak dipindahkan.");
                        log.info("[PremiumTeleport] (update hook) Sent 15-min warning to player {} ({}min left)",
                                p.name, minsLeft);
                    } catch (Exception e) {
                        log.error("[PremiumTeleport] onPlayerMapUpdate 15-min warning error: {}", e.getMessage());
                    }
                }
            }
            return;
        }

        // Sesi habis — coba auto-extend pakai tiket dulu, baru kick
        if (hasActiveSession(p)) return;

        if (tryAutoExtendWithTicket(p)) return;

        try {
            Vgo vgo = Vgo.create(RETURN_MAP_ID, RETURN_X, RETURN_Y);
            p.changeMap(p, vgo);
            Service.send_notice_box(p.conn,
                    "⏰ Waktu Premium Teleport kamu telah habis!\nKamu telah dipindahkan ke Desa Azure.");
            log.info("[PremiumTeleport] (update hook) Kicked player {} from map {}", p.name, p.map.mapId);
        } catch (Exception e) {
            log.error("[PremiumTeleport] onPlayerMapUpdate error: {}", e.getMessage());
        }
    }

    /**
     * Kirim packet countdown sesi premium ke client.
     *
     * Menggunakan opcode -94 (sama dengan BTF sendUpdateTime).
     * Format packet:
     *   writeByte(-1)
     *   writeByte(0)
     *   writeShort(0)
     *   writeByte(0)
     *   writeLong(startTimestamp)  ← currentTimeMillis - elapsed
     *
     * startTimestamp dihitung dari expiry dikurangi total durasi sesi,
     * sehingga client dapat menghitung sisa waktu yang tersisa.
     */
    public void sendSessionCountdown(Player p) {
        if (p == null || p.conn == null) return;
        Long expiry = activeSession.get(p.objectId);
        if (expiry == null) return;

        long now       = System.currentTimeMillis();
        long remaining = expiry - now;            // sisa sesi dalam ms
        if (remaining <= 0) return;

        // startTimestamp = waktu "kapan sesi dimulai" jika durasi total = 1 jam
        // Sama persis dengan logika BTF: currentTimeMillis - (3600 - timeCount) * 1000
        // Di sini timeCount (detik) = remaining / 1000
        long totalSec   = TimeUnit.HOURS.toSeconds(1);   // 3600 — tampilkan max 1 jam
        long remaining_sec = remaining / 1000;
        long elapsed_sec   = totalSec - Math.min(remaining_sec, totalSec);
        long startTimestamp = now - elapsed_sec * 1000L;

        try {
            Message m = new Message(-94);
            m.writer().writeByte(-1);
            m.writer().writeByte(0);
            m.writer().writeShort(0);
            m.writer().writeByte(0);
            m.writer().writeLong(startTimestamp);
            p.conn.addmsg(m);
            m.cleanup();
        } catch (Exception ignore) {}
    }

    public void shutdown() {
        // Sebelum shutdown, pastikan semua sesi aktif online di-persist ke DB
        // sebagai expire_at absolut (tetap akurat walau proses baru sungguhan
        // dimatikan lama setelah baris terakhir ini dieksekusi).
        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, Long> entry : activeSession.entrySet()) {
            if (entry.getValue() > now) {
                saveActiveExpiryToDB(entry.getKey(), entry.getValue());
            } else {
                removeSessionFromDB(entry.getKey());
            }
        }
        for (Map.Entry<Integer, Long> entry : pausedSession.entrySet()) {
            if (entry.getValue() > 0) {
                savePausedRemainingToDB(entry.getKey(), entry.getValue());
            }
        }
        // FIX: pauseSession() now dispatches its DB save to saveExecutor
        // instead of writing synchronously. Drain it here (with a short
        // bound) so a save from a player who disconnected right before
        // shutdown isn't lost when the JVM exits.
        saveExecutor.shutdown();
        try {
            saveExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        scheduler.shutdownNow();
        log.info("[PremiumTeleport] Shutdown — semua sesi disimpan ke DB.");
    }

    // ---------------------------------------------------------------
    // DB Persistence — tabel premium_session
    // ---------------------------------------------------------------

    /**
     * Buat tabel jika belum ada. Dipanggil sekali saat init().
     * DDL lengkap ada di Javadoc class ini.
     */
    /**
     * FIX (bug: paket premium "refill" sendiri setelah server crash/restart paksa):
     *
     * Skema lama menyimpan `remaining_ms` sebagai snapshot RELATIF yang cuma
     * di-update ke DB saat startSession/pauseSession/shutdown(). Selama player
     * online, activeSession (sumber kebenaran) hanya hidup di memory dan terus
     * berkurang — tapi angka di DB tetap beku di nilai lama. Kalau proses mati
     * TANPA sempat memanggil shutdown() (crash, kill -9, OOM, restart paksa —
     * dan close() di ServerManager hanya dipanggil manual lewat command GM,
     * BUKAN lewat shutdown hook), maka saat boot lagi server memuat angka basi
     * itu dan player mendapat sisa waktu penuh lagi meski seharusnya sudah
     * nyaris/benar-benar habis. Ini match dengan data premium_session.sql:
     * beberapa row punya remaining_ms hingga belasan HARI, padahal paket
     * terbesar yang dijual cuma 3 jam per pembelian.
     *
     * Perbaikan: tambah kolom `state` ('active'/'paused').
     *  - state='paused' → `value_ms` = sisa waktu relatif (aman, karena waktu
     *    memang berhenti saat player offline, jadi tidak pernah basi).
     *  - state='active' → `value_ms` = expire_at TIMESTAMP ABSOLUT. Timestamp
     *    absolut tidak pernah basi — dibaca kapan pun, `expire_at - now()`
     *    selalu memberi sisa waktu yang benar, termasuk setelah crash.
     */
    private void ensureTableExists() {
        String ddl =
            "CREATE TABLE IF NOT EXISTS `premium_session` (" +
            "  `player_id`  INT NOT NULL PRIMARY KEY," +
            "  `value_ms`   BIGINT NOT NULL," +
            "  `state`      ENUM('active','paused') NOT NULL DEFAULT 'paused'," +
            "  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
            "                        ON UPDATE CURRENT_TIMESTAMP" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(ddl)) {
            ps.executeUpdate();
            if (!conn.getAutoCommit()) conn.commit();
            log.info("[PremiumTeleport] Tabel premium_session siap.");
        } catch (Exception e) {
            log.error("[PremiumTeleport] Gagal membuat tabel premium_session: {}", e.getMessage());
        }
    }

    /**
     * Load semua sesi yang tersimpan di DB ke pausedSession (memory).
     * Dipanggil saat server boot.
     *  - Row 'paused'  → langsung dipakai apa adanya (sisa waktu relatif, valid).
     *  - Row 'active'  → berarti server mati mendadak saat player online.
     *    Hitung ulang sisa waktu = expire_at - now (crash-safe, tidak di-refill).
     * Sesi yang ternyata sudah habis dihapus langsung dari DB.
     */
    private void loadSessionsFromDB() {
        int loaded = 0, expired = 0;
        long now = System.currentTimeMillis();
        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT `player_id`, `value_ms`, `state` FROM `premium_session`");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int    playerId = rs.getInt("player_id");
                long   valueMs  = rs.getLong("value_ms");
                String state    = rs.getString("state");

                long remainingMs = "active".equals(state) ? (valueMs - now) : valueMs;

                if (remainingMs > 0) {
                    pausedSession.put(playerId, remainingMs);
                    loaded++;
                } else {
                    // Sudah expired (baik saat offline maupun saat server mati) — hapus dari DB
                    removeSessionFromDB(playerId);
                    expired++;
                }
            }
        } catch (Exception e) {
            log.error("[PremiumTeleport] Gagal load sesi dari DB: {}", e.getMessage());
        }
        log.info("[PremiumTeleport] Load sesi dari DB: {} aktif, {} sudah expired (dihapus).", loaded, expired);
    }

    /** Simpan sesi player yang OFFLINE (paused) — value relatif, aman disimpan apa adanya. */
    private void savePausedRemainingToDB(int playerId, long remainingMs) {
        if (remainingMs <= 0) {
            removeSessionFromDB(playerId);
            return;
        }
        upsertSession(playerId, remainingMs, "paused");
    }

    /** Simpan sesi player yang ONLINE (active) — value = expire_at absolut. */
    private void saveActiveExpiryToDB(int playerId, long expireAtMs) {
        if (expireAtMs <= System.currentTimeMillis()) {
            removeSessionFromDB(playerId);
            return;
        }
        upsertSession(playerId, expireAtMs, "active");
    }

    private void upsertSession(int playerId, long valueMs, String state) {
        String sql =
            "INSERT INTO `premium_session` (`player_id`, `value_ms`, `state`) VALUES (?, ?, ?)" +
            " ON DUPLICATE KEY UPDATE `value_ms` = VALUES(`value_ms`), `state` = VALUES(`state`)";
        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            ps.setLong(2, valueMs);
            ps.setString(3, state);
            ps.executeUpdate();
            if (!conn.getAutoCommit()) conn.commit();
        } catch (Exception e) {
            log.error("[PremiumTeleport] Gagal menyimpan sesi player {} ke DB: {}", playerId, e.getMessage());
        }
    }

    /** Ambil sisa waktu 'paused' dari DB (row dengan state='paused' saja). */
    private Long loadPausedRemainingFromDB(int playerId) {
        return loadValueFromDB(playerId, "paused");
    }

    /** Ambil expire_at absolut dari DB (row dengan state='active' saja) — untuk kasus crash-recovery. */
    private Long loadActiveExpiryFromDB(int playerId) {
        return loadValueFromDB(playerId, "active");
    }

    private Long loadValueFromDB(int playerId, String state) {
        String sql = "SELECT `value_ms` FROM `premium_session` WHERE `player_id` = ? AND `state` = ? LIMIT 1";
        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            ps.setString(2, state);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("value_ms");
                }
            }
        } catch (Exception e) {
            log.error("[PremiumTeleport] Gagal load sesi ({}) player {} dari DB: {}", state, playerId, e.getMessage());
        }
        return null;
    }

    /**
     * Hapus sesi player dari DB (sesi expired atau di-reset admin).
     */
    private void removeSessionFromDB(int playerId) {
        String sql = "DELETE FROM `premium_session` WHERE `player_id` = ?";
        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            ps.executeUpdate();
            if (!conn.getAutoCommit()) conn.commit();
        } catch (Exception e) {
            log.error("[PremiumTeleport] Gagal hapus sesi player {} dari DB: {}", playerId, e.getMessage());
        }
    }
}