package game.pvp;

import client.Player;
import core.Manager;
import core.Service;
import client.io.Message;
import game.map.GameMap;
import game.map.MapService;
import model.map.Vgo;
import template.StrucEff;

import java.io.IOException;

/**
 * Manages PvP session between two players with round-based combat system
 */
public class PvpSession {

    // ============================================================================
    // CONSTANTS
    // ============================================================================
    private static final int MAP_ID = 102;
    private static final long COUNTDOWN_MS = 10_000;
    private static final long ROUND_TIME_MS = 600_000;
    private static final long LEAVE_DELAY_MS = 5_000;
    private static final int MAX_ROUNDS = 2;
    private static final int WINS_TO_WIN = 2;
    private static final int PLAYER1_SPAWN_X = 228;
    private static final int PLAYER1_SPAWN_Y = 228;
    private static final int PLAYER2_SPAWN_X = 492;
    private static final int PLAYER2_SPAWN_Y = 228;
    private static final int LOBBY_MAP_ID = 1;
    private static final int LOBBY_X = 432;
    private static final int LOBBY_Y = 354;

    // -------------------------------------------------------------------------
    // Bug fix (utama): flag PK arena TIDAK BOLEH lewat MapService.changeFlag().
    // changeFlag() langsung return di pengecekan pertama kalau
    // gameMap.isMapLoiDai(), dan GameMap.isMapLoiDai() bernilai true untuk mapId
    // 100 & 102. Jadi semua pemanggilan changeFlag() dari sesi PVP ini tidak
    // pernah mengubah typepk — yang terkirim cuma popup "Tidak bisa dilakukan!"
    // ke kedua pemain di tiap awal ronde dan saat pertandingan ditutup.
    // Akibatnya typepk kedua pemain tetap seperti sebelum masuk arena; kalau
    // kebetulan sama dan bukan 0 (mis. sama-sama -1, atau sisa flag event lain)
    // MainObject.attack() memblokir serangan -> duel tidak jalan sama sekali.
    // Sekarang flag di-set langsung + broadcast paket 42, persis pola KingCup
    // (lihat KingCup.getMapPk(): p.typepk = 12 / 13).
    //
    // Aturan MainObject.attack(): typepk != 0 && typepk SAMA -> tidak bisa saling
    // serang. Jadi NEUTRAL = kedua pemain diberi nilai sama (fase hitung mundur
    // dan jeda antar ronde), FIGHT = nilai berbeda (fase bertarung).
    // -------------------------------------------------------------------------
    // Nilai netral sengaja BUKAN 12/13: Body.setDie() memakai 12/13 untuk
    // menentukan pemenang ronde King Cup, jadi nilai netral yang berbeda mencegah
    // salah atribusi kalau logika itu kelak dipakai ulang di map yang sama.
    private static final int PK_NEUTRAL = 11;
    private static final int PK_FIGHT_P1 = 12;
    private static final int PK_FIGHT_P2 = 13;
    // Nilai "tanpa flag" di server ini adalah -1, bukan 0: Player.set_in4()
    // memberi typepk = -1 saat login dan MapService.enter() memanggil
    // changeFlag(map, p, -1) untuk map biasa. Dulu di sini dipakai 0, dan 0
    // punya arti khusus: gerbang PK (MainObject.attack) cuma memblokir kalau
    // typepk != 0, jadi pemain yang baru keluar arena bisa memukul siapa saja
    // di map non-kota; dan semua skill buff menuntut p0.typepk != 0 && sama,
    // jadi pemain itu tidak bisa nge-buff / di-buff sampai relog.
    private static final int PK_NORMAL = -1;

    // ============================================================================
    // FIELDS
    // ============================================================================

    private final Player player1;
    private final Player player2;

    private long startTime;
    private long endTime;
    private long countdownStart;
    private long leaveCountdownStart;

    private boolean started;
    private boolean finished;
    private boolean leaving;

    private int round;
    private int player1Wins;
    private int player2Wins;
    private final byte zone;

    // ============================================================================
    // CONSTRUCTOR
    // ============================================================================

    public PvpSession(Player p1, Player p2, byte zone) {
        this.player1 = p1;
        this.player2 = p2;
        this.round = 1;
        this.player1Wins = 0;
        this.player2Wins = 0;
        this.zone = zone;
    }

    // ============================================================================
    // PUBLIC METHODS
    // ============================================================================

    /**
     * Initializes and begins the PvP session.
     *
     * Bug fix: sebelumnya void dan semua error ditelan try-catch, jadi kalau map
     * arena tidak tersedia (getMapByIdAndZone() -> null, lalu NPE di
     * MapService.enter) sesi tetap terdaftar di PvpManager dan kedua pemain
     * dianggap "sedang bertarung" selamanya. Sekarang statusnya dikembalikan
     * supaya pemanggil bisa membatalkan sesi yang gagal dibuat.
     */
    public boolean begin() {
        GameMap arena = GameMap.getMapByIdAndZone(MAP_ID, zone);
        if (arena == null || player1 == null || player2 == null) {
            finished = true;
            return false;
        }

        this.countdownStart = System.currentTimeMillis();
        this.startTime = countdownStart + COUNTDOWN_MS;
        this.endTime = startTime + ROUND_TIME_MS;

        try {
            // Bug fix: begin() dulu langsung memindahkan pemain tanpa revive.
            // Tidak ada satupun cek isdie di PvpManager, jadi pemain yang sedang
            // mati saat menerima tantangan masuk arena dengan isdie = true —
            // MainObject.attack() menolak semua serangannya (ObjAtk.isdie)
            // sepanjang ronde 1, baru terbetulkan di ronde 2 lewat resetPlayer().
            revive(player1);
            revive(player2);

            enterPVPMap(player1, PLAYER1_SPAWN_X, PLAYER1_SPAWN_Y);
            enterPVPMap(player2, PLAYER2_SPAWN_X, PLAYER2_SPAWN_Y);

            clearCombatBlockingEffects(player1);
            clearCombatBlockingEffects(player2);

            // Fase hitung mundur: flag sama -> belum bisa saling serang.
            setPkFlag(player1, PK_NEUTRAL);
            setPkFlag(player2, PK_NEUTRAL);

            int seconds = (int) (COUNTDOWN_MS / 1000);
            sendCountdownMessage(player1, "Pertarungan dimulai dalam ", seconds);
            sendCountdownMessage(player2, "Pertarungan dimulai dalam ", seconds);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            finish();
            return false;
        }
    }

    /**
     * Handles player death event during combat
     */
    public void onPlayerDied(Player deadPlayer) {
        if (finished || leaving || deadPlayer == null) return;
        // Kematian saat masih hitung mundur tidak dihitung sebagai kekalahan.
        if (!started) return;

        // Bug fix: dulu dibandingkan dengan == (identitas objek). Kalau objek
        // Player yang dikirim bukan instance player1, otomatis dianggap player2
        // sehingga pemenangnya bisa salah. Sekarang dicocokkan by name, dan
        // pemain di luar sesi ini diabaikan.
        boolean deadIsP1 = player1 != null && player1.name.equals(deadPlayer.name);
        boolean deadIsP2 = player2 != null && player2.name.equals(deadPlayer.name);
        if (!deadIsP1 && !deadIsP2) return;

        Player dead = deadIsP1 ? player1 : player2;
        Player winner = deadIsP1 ? player2 : player1;

        leaving = true;
        leaveCountdownStart = System.currentTimeMillis();

        if (deadIsP1) {
            player2Wins++;
        } else {
            player1Wins++;
        }

        // Selama jeda 5 detik samakan flag, supaya pemain yang baru dibangkitkan
        // tidak bisa langsung dibunuh lagi oleh lawannya.
        try {
            setPkFlag(player1, PK_NEUTRAL);
            setPkFlag(player2, PK_NEUTRAL);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int seconds = (int) (LEAVE_DELAY_MS / 1000);
        String str = isMatchOver() ? " Menutup pertandingan dalam " : " Lanjut dalam ";
        sendCountdownMessage(winner, "Kamu menang di ronde " + round + str, seconds);
        sendCountdownMessage(dead, "Kamu kalah di ronde " + round + str, seconds);

        revive(dead);
    }

    /**
     * Updates session state - should be called periodically
     */
    public void update() {
        if (finished) return;

        long now = System.currentTimeMillis();

        // Bug fix: sebelumnya pemain DC/logout di tengah duel tidak ditangani.
        // Sesi menggantung sampai timeout 10 menit, zone arena ikut terkunci, dan
        // pemain yang masih online terjebak di map 102 tanpa lawan.
        if (!isActive(player1) || !isActive(player2)) {
            abortByDisconnect();
            return;
        }

        // Bug fix: isActive() cuma mengecek koneksi, bukan posisi. Kalau seorang
        // pemain keluar arena di tengah pertandingan (tombol respawn, teleport,
        // atau dipindah admin), sesi tetap jalan dan pemain yang tersisa berdiri
        // sendirian di map 102 tanpa target sampai timeout ronde.
        if (!isInArena(player1) || !isInArena(player2)) {
            abortByLeavingArena();
            return;
        }

        // Countdown phase
        if (!started && now >= startTime) {
            started = true;
            try {
                setPkFlag(player1, PK_FIGHT_P1);
                setPkFlag(player2, PK_FIGHT_P2);
                int seconds = (int) (ROUND_TIME_MS / 1000);
                sendCountdownMessage(player1, "Bertarung ", seconds);
                sendCountdownMessage(player2, "Bertarung ", seconds);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Leave countdown after death
        if (leaving && now - leaveCountdownStart >= LEAVE_DELAY_MS) {
            handleNextRoundOrFinish();
            // Bug fix: dulu tidak ada return di sini, jadi blok "match timeout" di
            // bawah ikut jalan pada tick yang sama. Kalau ronde ini menutup
            // pertandingan, finish() terpanggil dua kali -> pengumuman ganda di
            // chat dunia dan goVillage() dobel.
            return;
        }

        // Match timeout
        if (started && now >= endTime) {
            handleNextRoundOrFinish();
        }
    }

    /**
     * Checks if session has ended
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Checks if player is part of this session
     */
    public boolean contains(String name) {
        if (name == null) return false;
        return (player1 != null && name.equals(player1.name))
                || (player2 != null && name.equals(player2.name));
    }

    /**
     * Zone/instance map 102 yang dipakai sesi ini — dipakai PvpManager untuk
     * menghindari memberi zone yang sama ke sesi/pertandingan lain.
     */
    public byte getZone() {
        return zone;
    }

    /** Dipakai PvpManager setelah reserveSession() untuk memanggil begin() & notice. */
    public Player getPlayer1() {
        return player1;
    }

    /** Dipakai PvpManager setelah reserveSession() untuk memanggil begin() & notice. */
    public Player getPlayer2() {
        return player2;
    }

    // ============================================================================
    // PRIVATE METHODS
    // ============================================================================

    private boolean isMatchOver() {
        return player1Wins >= WINS_TO_WIN || player2Wins >= WINS_TO_WIN || round >= MAX_ROUNDS;
    }

    private static boolean isActive(Player p) {
        return p != null && p.isOnline && p.conn != null && p.conn.connected;
    }

    /** Pemain masih berada di instance arena milik sesi ini. */
    private boolean isInArena(Player p) {
        return p != null && p.map != null && p.map.mapId == MAP_ID && p.map.zoneId == zone;
    }

    private void abortByLeavingArena() {
        Player left = !isInArena(player1) ? player1 : player2;
        Player stay = (left == player1) ? player2 : player1;

        if (isActive(stay)) {
            String leftName = (left != null ? left.name : "Lawan");
            stay.sendNoticeBox(leftName + " meninggalkan arena. Kamu menang!");
            if (stay == player1) {
                player1Wins++;
            } else {
                player2Wins++;
            }
        }
        finish();
    }

    /**
     * Cabut efek yang membuat pertarungan mustahil: TangHinh (tembus pandang)
     * bikin pemiliknya tidak bisa diserang sama sekali, LuLan bikin pemiliknya
     * tidak bisa menyerang (lihat MainObject.attack). Keduanya sebelumnya
     * terbawa masuk dari luar arena dan tidak pernah dibersihkan.
     */
    private void clearCombatBlockingEffects(Player player) {
        if (player == null) return;
        player.removeEffectMedal(StrucEff.TangHinh);
        player.removeEffectMedal(StrucEff.LuLan);
    }

    /**
     * Pindahkan pemain di dalam arena tanpa leave/enter (lihat catatan di
     * resetPlayer). Paket 4 = teleport objek, disiarkan ke semua pemain di map.
     */
    private void moveInArena(Player player, int x, int y) throws IOException {
        if (player == null) return;
        GameMap gameMap = GameMap.getMapByIdAndZone(MAP_ID, zone);
        if (gameMap == null || player.map != gameMap) {
            // Belum/tidak lagi di arena: jatuh kembali ke jalur masuk normal.
            enterPVPMap(player, x, y);
            return;
        }

        player.isChangemap = false;
        player.x = (short) x;
        player.y = (short) y;
        player.xOld = player.x;
        player.yOld = player.y;

        Message m = new Message(4);
        m.writer().writeByte(0);
        m.writer().writeShort(0);
        m.writer().writeShort(player.objectId);
        m.writer().writeShort(player.x);
        m.writer().writeShort(player.y);
        m.writer().writeByte(-1);
        MapService.sendMsgPlayerInside(gameMap, player, m, true);
        m.cleanup();
    }

    private void abortByDisconnect() {
        Player left = !isActive(player1) ? player1 : player2;
        Player stay = (left == player1) ? player2 : player1;

        if (isActive(stay)) {
            String leftName = (left != null ? left.name : "Lawan");
            stay.sendNoticeBox(leftName + " keluar dari pertandingan. Kamu menang!");
            if (stay == player1) {
                player1Wins++;
            } else {
                player2Wins++;
            }
        }
        finish();
    }

    private void handleNextRoundOrFinish() {
        if (finished) return;

        if (isMatchOver()) {
            finish();
            return;
        }

        // Start next round
        round++;
        started = false;
        leaving = false;
        countdownStart = System.currentTimeMillis();
        startTime = countdownStart + COUNTDOWN_MS;
        endTime = startTime + ROUND_TIME_MS;

        try {
            resetPlayer(player1, PLAYER1_SPAWN_X, PLAYER1_SPAWN_Y);
            resetPlayer(player2, PLAYER2_SPAWN_X, PLAYER2_SPAWN_Y);
            int seconds = (int) (COUNTDOWN_MS / 1000);
            sendCountdownMessage(player1, "Round " + round + " dimulai dalam ", seconds);
            sendCountdownMessage(player2, "Round " + round + " dimulai dalam ", seconds);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void finish() {
        if (finished) return;
        finished = true;

        String result;
        if (player1Wins > player2Wins) {
            result = player1.name + " menang " + player1Wins + " - " + player2Wins + " dari " + player2.name + " dalam PVP";
        } else if (player2Wins > player1Wins) {
            result = player2.name + " menang " + player2Wins + " - " + player1Wins + " dari " + player1.name + " dalam PVP";
        } else {
            result = player1.name + " vs " + player2.name + " berakhir seri dalam PVP";
        }

        try {
            Manager.gI().chatKTGprocess(result);
        } catch (IOException e) {
            e.printStackTrace();
        }

        releasePlayer(player1);
        releasePlayer(player2);
    }

    /**
     * Kembalikan pemain ke kondisi normal: flag PK bersih + keluar dari arena.
     *
     * Bug fix: dulu goVillage() dipanggil tanpa cek pemain masih online — kalau
     * salah satu sudah DC, exception-nya membatalkan pemanggilan untuk pemain
     * SATUNYA juga, jadi dia tertinggal di map 102 sampai relog. Selain itu
     * typepk tidak pernah benar-benar dibersihkan (lihat catatan changeFlag di
     * atas), sehingga flag arena kebawa ke map biasa.
     */
    private void releasePlayer(Player player) {
        if (player == null) return;
        try {
            setPkFlag(player, PK_NORMAL);
        } catch (Exception ignored) {
        }

        if (isActive(player)) {
            try {
                player.isdie = false;
                if (player.hp <= 0) {
                    player.hp = player.body.getMaxHP();
                }
                goVillage(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        // Pemain sudah offline: pastikan dia tidak tertinggal di daftar pemain map
        // arena (bikin zone dianggap terpakai terus oleh findFreeZone) dan tidak
        // ter-respawn di dalam arena saat login lagi.
        try {
            if (player.map != null && player.map.mapId == MAP_ID) {
                MapService.leave(player.map, player);
                GameMap lobby = GameMap.getMapByIdAndZone(LOBBY_MAP_ID, 0);
                if (lobby != null) {
                    player.map = lobby;
                    player.x = (short) LOBBY_X;
                    player.y = (short) LOBBY_Y;
                    player.xOld = player.x;
                    player.yOld = player.y;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetPlayer(Player player, int x, int y) throws IOException {
        revive(player);
        // Bug fix: dulu memanggil enterPVPMap() lagi = leave() + enter() di map
        // yang SAMA. leave() menyiarkan paket 8 (hapus objek) ke lawan, sementara
        // enter() cuma mengosongkan other_player_inside milik pemain ini, bukan
        // milik lawannya. Karena map 102 masuk is_map__load_board_player(),
        // cabang penghapusan di loop proximity MapService tidak pernah jalan, jadi
        // entri lawan tidak pernah dibuang dan server merasa "dia sudah tahu" —
        // paket kemunculan tidak dikirim ulang. Hasilnya lawan hilang dari layar
        // sampai ronde selesai dan tidak bisa di-target. Untuk reset antar ronde
        // cukup pindahkan posisi, persis pola KingCup.refresh().
        moveInArena(player, x, y);
        clearCombatBlockingEffects(player);
        // Jeda sebelum ronde berikutnya: flag sama -> belum bisa saling serang.
        setPkFlag(player, PK_NEUTRAL);
    }

    /**
     * Bug fix: dulu cuma isdie/hp yang di-set di sisi server tanpa mengabari
     * client, jadi pemain yang "hidup lagi" masih tampil mati / HP 0 di layar
     * sampai ganti map. Ikuti pola KingCup.refresh(): sinkronkan HP & MP.
     */
    private void revive(Player player) {
        if (player == null) return;
        player.isdie = false;
        player.hp = player.body.getMaxHP();
        player.mp = player.body.getMaxMP();
        if (!isActive(player)) return;
        try {
            // Urutan sama persis dengan jalur revive yang sudah terbukti jalan
            // (Process_Yes_no_box case 9 & KingCup.refresh): info char -> combo
            // (-108) -> hp -> mp. Dulu send_combo tidak ada dan diganti send_health
            // (paket 59, yang cuma poin suckhoe/arena), jadi client yang kalah tidak
            // pernah keluar dari state mati / dialog respawn-nya.
            Service.sendMainCharInfo(player);
            Service.send_combo(player.conn);
            Service.usepotion(player, 0, player.body.getMaxHP());
            Service.usepotion(player, 1, player.body.getMaxMP());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void enterPVPMap(Player player, int x, int y) {
        if (player == null) return;
        GameMap gameMap = GameMap.getMapByIdAndZone(MAP_ID, zone);
        if (gameMap == null) return;

        if (player.map != null) {
            MapService.leave(player.map, player);
        }
        // Sama seperti KingCup.goToLD(): reset flag pindah map, kalau tidak pemain
        // bisa nyangkut karena server menolak paket gerak berikutnya.
        player.isChangemap = false;
        player.map = gameMap;
        player.x = (short) x;
        player.y = (short) y;
        player.xOld = player.x;
        player.yOld = player.y;
        MapService.enter(gameMap, player);
    }

    private void goVillage(Player player) throws IOException {
        Vgo vgo = new Vgo();
        vgo.toMap = (byte) LOBBY_MAP_ID;
        vgo.toX = (short) LOBBY_X;
        vgo.toY = (short) LOBBY_Y;
        player.changeMap(player, vgo);
    }

    /**
     * Set flag PK langsung (bypass MapService.changeFlag yang selalu ditolak di
     * map lôi đài / arena 102) + broadcast paket 42 ke pemain di sekitar.
     */
    private void setPkFlag(Player player, int type) throws IOException {
        if (player == null) return;
        player.typepk = (byte) type;

        Message m = new Message(42);
        m.writer().writeShort(player.objectId);
        m.writer().writeByte(type);
        if (player.map != null) {
            MapService.sendMsgPlayerInside(player.map, player, m, true);
        } else if (player.conn != null && player.conn.connected) {
            player.conn.addmsg(m);
        }
        m.cleanup();
    }

    private void sendCountdownMessage(Player player, String text, int seconds) {
        if (player == null || player.conn == null || !player.conn.connected) return;
        try {
            Message message = new Message(-104);
            message.writer().writeByte(1);
            message.writer().writeByte(2);
            message.writer().writeShort(seconds);
            message.writer().writeUTF(text);
            message.writer().writeShort(0);
            message.writer().writeUTF("");
            player.conn.addmsg(message);
            message.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}