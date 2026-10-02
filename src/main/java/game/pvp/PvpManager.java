package game.pvp;

import client.Player;
import core.Manager;
import client.io.Message;
import client.io.Session;
import game.map.GameMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PvpManager {
    private static final int PVP_MAP_ID = 102;

    // -------------------------------------------------------------------------
    // Syarat masuk arena. Semua ini adalah kondisi yang bikin MainObject.attack()
    // menolak serangan DIAM-DIAM (return tanpa pesan apa pun), jadi kalau tidak
    // divalidasi di sini pemain tetap ditarik ke arena lalu bengong 10 menit per
    // ronde tanpa tahu kenapa serangannya tidak masuk.
    //   - level < 11         -> gerbang PK memblokir KEDUA arah
    //   - hieuchien > 32000  -> pemain itu tidak bisa menyerang pemain mana pun
    //   - isdie              -> ObjAtk.isdie di baris pertama attack()
    // -------------------------------------------------------------------------
    private static final int MIN_LEVEL = 11;
    private static final int MAX_HIEUCHIEN = 32_000;

    /** Masa berlaku tantangan PVP yang belum dijawab. */
    private static final long REQUEST_TTL_MS = 60_000;

    private static PvpManager instance;

    private final List<PvpSession> sessions = new CopyOnWriteArrayList<>();
    private final List<Player> queue = new ArrayList<>();

    /** Tantangan yang menunggu jawaban. Key = nama pemain yang ditantang. */
    private final Map<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    private static final class PendingRequest {
        final String challenger;
        final long expireAt;

        PendingRequest(String challenger, long expireAt) {
            this.challenger = challenger;
            this.expireAt = expireAt;
        }
    }

    public static synchronized PvpManager gI() {
        if (instance == null) {
            instance = new PvpManager();
        }
        return instance;
    }

    // ============================================================
    // QUEUE MANAGEMENT
    // ============================================================

    public synchronized void joinQueue(Player player) {
        if (player == null || isInQueue(player) || isInSession(player)) return;
        if (rejectIfCannotFight(player, player)) return;
        queue.add(player);
        tryMatchmaking();
    }

    public synchronized void leaveQueue(Player player) {
        if (player == null) return;
        queue.removeIf(p -> p.name.equals(player.name));
    }

    public synchronized boolean isInQueue(Player player) {
        return player != null && queue.stream().anyMatch(p -> p.name.equals(player.name));
    }

    public boolean isInSession(Player player) {
        return player != null && sessions.stream().anyMatch(s -> s.contains(player.name));
    }

    public synchronized int getQueueSize() {
        return queue.size();
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Dipakai KingCup untuk mengecek apakah sebuah zone map 102 sedang dipakai
     * sesi duel PvpManager, supaya KingCup tidak ikut memakai zone yang sama
     * (lihat catatan bug di KingCup constructor: zone-picker KingCup sebelumnya
     * cuma cek kingCupRef & players.isEmpty(), tidak pernah cek daftar sessions
     * di sini — jadi dua pertandingan beda bisa nyasar ke instance map yang sama
     * persis kalau timing-nya pas).
     */
    public boolean isZoneUsed(short zone) {
        return sessions.stream().anyMatch(s -> !s.isFinished() && s.getZone() == zone);
    }

    private void tryMatchmaking() {
        while (queue.size() >= 2) {
            Player p1 = queue.get(0);
            Player p2 = queue.get(1);
            if (!isAvailable(p1)) { queue.remove(0); continue; }
            if (!isAvailable(p2)) { queue.remove(1); continue; }
            // Kondisi bisa berubah selama menunggu di antrean (mati, kena PK).
            if (rejectIfCannotFight(p1, p1)) { queue.remove(0); continue; }
            if (rejectIfCannotFight(p2, p2)) { queue.remove(1); continue; }

            PvpSession session = reserveSession(p1, p2);
            if (session == null) {
                // Bug fix: sebelumnya zone selalu diambil dari sessions.size()+1 tanpa
                // cek ketersediaan, jadi dua pemain tetap dimasukkan ke arena walau
                // semua instance map 102 penuh (atau malah bentrok dengan pertandingan
                // lain). Sekarang kalau memang tidak ada zone kosong, tunda dulu di
                // antrean daripada memaksa masuk.
                break;
            }
            queue.remove(0);
            queue.remove(0);

            startSession(session);
        }
    }

    /**
     * Bug fix: dulu sesi langsung ditambahkan ke daftar sebelum begin() dicoba.
     * Kalau begin() gagal (map arena tidak ada), sesi mati itu tetap nyangkut di
     * daftar dan kedua pemain selamanya dianggap "sedang bertarung" sehingga
     * tidak pernah bisa PVP lagi sampai server restart.
     */
    private boolean startSession(PvpSession session) {
        Player p1 = session.getPlayer1();
        Player p2 = session.getPlayer2();
        if (!session.begin()) {
            sessions.remove(session);
            p1.sendNoticeBox("Gagal membuka arena PVP, coba lagi sebentar.");
            p2.sendNoticeBox("Gagal membuka arena PVP, coba lagi sebentar.");
            return false;
        }
        p1.sendNoticeBox("Lawan ditemukan: " + p2.name + "\nKamu dipindah ke arena PVP!");
        p2.sendNoticeBox("Lawan ditemukan: " + p1.name + "\nKamu dipindah ke arena PVP!");
        return true;
    }

    private boolean isAvailable(Player p) {
        return p != null && p.isOnline && p.conn != null && p.conn.connected;
    }

    /**
     * Alasan pemain tidak bisa bertarung di arena, atau null kalau boleh.
     * {@code self} menentukan sudut pandang pesan (diri sendiri vs lawan).
     */
    private String cannotFightReason(Player p, boolean self) {
        if (p == null) return self ? "Kamu tidak bisa bertanding sekarang." : "Lawan tidak bisa bertanding sekarang.";
        String who = self ? "Kamu" : p.name;
        if (p.level < MIN_LEVEL) {
            return who + " belum level " + MIN_LEVEL + ", belum bisa masuk arena PVP.";
        }
        if (p.isdie || p.hp <= 0) {
            return who + " sedang dalam keadaan mati.";
        }
        if (p.hieuchien > MAX_HIEUCHIEN) {
            return who + " punya poin hieuchien terlalu tinggi (" + p.hieuchien
                    + ") sehingga tidak bisa menyerang pemain lain. Bersihkan dulu lewat NPC.";
        }
        return null;
    }

    /**
     * Kirim alasan penolakan ke {@code notify} kalau {@code p} tidak layak
     * bertanding. Mengembalikan true kalau permintaan harus dibatalkan.
     */
    private boolean rejectIfCannotFight(Player p, Player notify) {
        String reason = cannotFightReason(p, p == notify);
        if (reason == null) return false;
        if (notify != null) {
            try {
                notify.sendNoticeBox(reason);
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Bug fix: sebelumnya zone dipilih dari sessions.size()+1 dan King Cup dari
    // counter idBase-nya sendiri — dua penomoran independen di pool map 102 yang
    // sama, jadi pertandingan King Cup dan sesi duel PVP biasa bisa nyasar ke
    // instance map yang sama persis. Sekarang cari zone yang benar-benar kosong:
    // tidak ada pemain, tidak dipakai King Cup, dan tidak dipakai sesi PVP lain.
    //
    // Bug fix #2: pencarian zone (dulu findFreeZone()) dan pendaftaran sesi ke
    // `sessions` (dulu di startSession()) sebelumnya dua langkah terpisah, masing-
    // masing dengan blok synchronized(ARENA_LOCK) SENDIRI. Ada jeda di antara
    // keduanya yang TIDAK terkunci — thread KingCup bisa menyelinap di jeda itu,
    // melihat zone yang sama masih "kosong" (karena sesi PVP ini belum sempat
    // terdaftar), lalu memakainya juga. Sekarang scan + reservasi (sessions.add)
    // digabung jadi SATU operasi atomik di dalam satu blok synchronized yang sama.
    // -------------------------------------------------------------------------
    private PvpSession reserveSession(Player p1, Player p2) {
        GameMap[] temp = GameMap.getMapById(PVP_MAP_ID);
        if (temp == null) return null;
        synchronized (event_daily.KingCup.ARENA_LOCK) {
            for (short z = 0; z < temp.length; z++) {
                GameMap zoneMap = temp[z];
                if (zoneMap == null || zoneMap.kingCupRef != null || !zoneMap.players.isEmpty()) {
                    continue;
                }
                final short zz = z;
                boolean usedByOtherSession = sessions.stream().anyMatch(s -> !s.isFinished() && s.getZone() == zz);
                if (usedByOtherSession) {
                    continue;
                }
                PvpSession session = new PvpSession(p1, p2, (byte) zz);
                sessions.add(session);
                return session;
            }
        }
        return null;
    }

    public void handlePVP(Session session, Message msg) throws IOException {
        try {
            if (session == null || session.p == null) return;

            byte type = msg.reader().readByte();
            String targetName = msg.reader().readUTF();

            Player sender = session.p;
            Player target = Manager.getPlayerByName(targetName);

            switch (type) {

                // Type 0: SEND REQUEST
                case 0 -> handleRequest(sender, target);

                // Type 1: ACCEPT REQUEST
                case 1 -> handleAccept(sender, target);

            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("[PvpManager] Error handling PVP: " + e.getMessage());
        }
    }

    private void handleRequest(Player sender, Player target) throws IOException {
        if (sender == null) return;

        if (target == null || !isAvailable(target)) {
            sender.sendNoticeBox("Player tidak ditemukan atau offline");
            return;
        }

        // Bug fix: tidak ada validasi sama sekali sebelumnya — pemain bisa
        // menantang dirinya sendiri (sesi dengan p1 == p2, arena langsung kacau)
        // atau menantang orang yang sedang duel.
        if (sender.name.equals(target.name)) {
            sender.sendNoticeBox("Kamu tidak bisa menantang dirimu sendiri!");
            return;
        }
        if (isInSession(sender)) {
            sender.sendNoticeBox("Kamu sedang dalam pertandingan PVP!");
            return;
        }
        if (isInSession(target)) {
            sender.sendNoticeBox(target.name + " sedang bertarung dengan pemain lain");
            return;
        }
        if (rejectIfCannotFight(sender, sender)) return;
        if (rejectIfCannotFight(target, sender)) return;

        // Catat tantangan supaya hanya pemain yang benar-benar ditantang yang bisa
        // menerimanya (lihat handleAccept).
        pendingRequests.put(target.name, new PendingRequest(sender.name, System.currentTimeMillis() + REQUEST_TTL_MS));

        // Send PVP challenge to target
        Message m = new Message(68); // same command ID
        m.writer().writeByte(0); // type = request
        m.writer().writeUTF(sender.name); // challenger name
        m.writer().writeShort(sender.objectId); // challenger id/index
        target.conn.addmsg(m);
        m.cleanup();
    }

    private synchronized void handleAccept(Player acceptor, Player challenger) throws IOException {
        if (acceptor == null) return;

        if (challenger == null || !isAvailable(challenger)) {
            acceptor.sendNoticeBox("Penantang tidak online!");
            return;
        }

        // Bug fix (exploit): dulu tidak ada pengecekan bahwa tantangan itu memang
        // pernah dikirim. Siapa pun bisa mengirim paket 68 tipe 1 dengan nama
        // pemain mana saja, dan korban langsung ditarik ke arena PVP tanpa pernah
        // menantang atau menyetujui apa pun.
        PendingRequest req = pendingRequests.get(acceptor.name);
        if (req == null || req.expireAt < System.currentTimeMillis() || !req.challenger.equals(challenger.name)) {
            pendingRequests.remove(acceptor.name);
            acceptor.sendNoticeBox("Tidak ada tantangan PVP dari pemain ini (atau sudah kedaluwarsa).");
            return;
        }
        pendingRequests.remove(acceptor.name);

        if (acceptor.name.equals(challenger.name)) {
            acceptor.sendNoticeBox("Kamu tidak bisa bertarung melawan dirimu sendiri!");
            return;
        }

        if (isInSession(acceptor) || isInSession(challenger)) {
            acceptor.sendNoticeBox("Penantang sedang bertarung dengan pemain lain");
            return;
        }

        // Divalidasi ulang di sini: kondisi bisa berubah antara tantangan dikirim
        // dan diterima (naik/turun level tidak, tapi mati & hieuchien bisa).
        if (rejectIfCannotFight(acceptor, acceptor)) return;
        if (rejectIfCannotFight(challenger, acceptor)) return;

        PvpSession session = reserveSession(acceptor, challenger);
        if (session == null) {
            acceptor.sendNoticeBox("Semua arena PVP sedang penuh, coba lagi sebentar.");
            return;
        }

        // Bug fix: kedua pemain juga harus dikeluarkan dari antrean matchmaking,
        // kalau tidak mereka bisa ditarik ke duel kedua saat masih bertanding.
        leaveQueue(acceptor);
        leaveQueue(challenger);

        startSession(session);
    }
    public void update() {
        Iterator<PvpSession> it = sessions.iterator();

        while (it.hasNext()) {
            PvpSession s = it.next();
            // Bug fix: satu exception dari satu sesi dulu menghentikan seluruh loop
            // update (dan ServerManager tidak menangkapnya), jadi sesi-sesi lain
            // ikut beku di tengah pertandingan.
            try {
                s.update();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (s.isFinished()) sessions.remove(s);
        }

        // Bersihkan tantangan yang sudah kedaluwarsa.
        long now = System.currentTimeMillis();
        pendingRequests.entrySet().removeIf(en -> en.getValue().expireAt < now);

        // Bersihkan antrean dari pemain yang sudah offline.
        synchronized (this) {
            queue.removeIf(p -> !isAvailable(p));
        }
    }

    public void onPlayerDie(Player deadPlayer) {
        if (deadPlayer == null) return;
        for (PvpSession session : sessions) {
            if (session.contains(deadPlayer.name)) {
                try {
                    session.onPlayerDied(deadPlayer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }


}