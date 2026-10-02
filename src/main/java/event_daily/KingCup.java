package event_daily;

import client.Player;
import core.Manager;
import core.SaveData;
import core.Service;
import core.Util;
import game.map.GameMap;
import game.map.MapService;
import model.map.Vgo;
import client.io.Message;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class KingCup implements Runnable {

    // -------------------------------------------------------------------------
    // Konstanta batas peserta per grup per ronde
    // -------------------------------------------------------------------------
    public static final int MAX_PAIRS_PER_ROUND = 60;

    public short id;
    public List<Player> players_attack;
    public String name1;
    public String name2;
    public int point1;
    public int point2;
    public final short X1_FIXED = 225;
    public final short X2_FIXED = 485;
    public final short Y_FIXED  = 221;

    public static final long TIME_BETWEEN_MATCH = 2 * 60 * 1000L; // 2 menit
    public static final long TIME_WAR           = 5 * 60 * 1000L; // 5 menit
    public static final long TIME_TOTAL         = 10 * (TIME_WAR + TIME_BETWEEN_MATCH); // 10 ronde
    public static final long REGISTRATION_TIME  = 10 * 60 * 1000L; // 10 menit fase pendaftaran

    // Waktu broadcast reminder sebelum event (30 menit & 5 menit)
    public static final long REMINDER_30_MIN = 30 * 60 * 1000L;
    public static final long REMINDER_5_MIN  =  5 * 60 * 1000L;

    public static final int MAX_ROUNDS = 10; // jumlah ronde per event

    // -------------------------------------------------------------------------
    // Flag PK arena. MainObject.attack(): typepk != 0 && typepk SAMA -> tidak bisa
    // saling serang. PK_P1/PK_P2 dipakai juga oleh Body.setDie() untuk menentukan
    // pemenang ronde, jadi nilai netral HARUS di luar 12/13 supaya kematian saat
    // jeda tidak salah dihitung sebagai kemenangan.
    // -------------------------------------------------------------------------
    public static final int PK_P1     = 12;
    public static final int PK_P2     = 13;
    private static final int PK_NEUTRAL = 11;
    // FIX: nilai "tanpa flag" di server ini -1, bukan 0 (Player.set_in4() dan
    // MapService.enter -> changeFlag(map, p, -1)). Dengan 0, gerbang PK
    // (typepk != 0) berhenti memblokir sehingga pemain yang baru keluar arena
    // bisa memukul siapa saja di map non-kota, dan semua skill buff (yang
    // menuntut typepk != 0 && sama) berhenti bekerja untuknya sampai relog.
    private static final int PK_NORMAL  = -1;

    /** true selama jeda antar ronde (kedua pemain diberi flag sama). */
    private boolean neutralized;

    public long timeWar;
    public long timeWait;
    public int  countMatch;
    public GameMap maps;
    public boolean finish;

    // -------------------------------------------------------------------------
    // State statis — volatile/atomic untuk thread safety
    // -------------------------------------------------------------------------
    public static CopyOnWriteArrayList<KingCup> kingCups;
    public static volatile long totalTime;
    public static KingCup kingCup;
    // Fix #6: volatile agar perubahan dari close() langsung terlihat di run()
    public static volatile boolean running;
    public static volatile int  count;
    public static Thread threadKingCup;
    public static volatile long NEXT_MATCHES;

    // Flag reminder agar broadcast tidak dikirim berulang
    private static volatile boolean reminder30Sent = false;
    private static volatile boolean reminder5Sent  = false;
    // Flag fase pendaftaran
    public static volatile boolean registrationOpen = false;

    // -------------------------------------------------------------------------
    // Constructor untuk instance pertandingan (p1 vs p2)
    // -------------------------------------------------------------------------
    public KingCup(final Player p1, final Player p2) {
        this.countMatch = 0;
        this.name1 = p1.name;
        this.name2 = p2.name;
        this.point1 = p1.point_king_cup;
        this.point2 = p2.point_king_cup;
        players_attack = new ArrayList<>();
        players_attack.add(p1);
        players_attack.add(p2);
        finish = false;
        this.timeWait = System.currentTimeMillis() + 7000L;
        this.timeWar  = System.currentTimeMillis() + TIME_WAR;

        // Bug fix: map 102 juga dipakai sistem duel terpisah (game.pvp.PvpManager /
        // PvpSession) yang menomori zone-nya sendiri (sessions.size()+1), independen
        // dari idBase di sini. Sebelumnya id arena King Cup diambil murni dari counter
        // idBase yang di-cast ke byte (overflow ke negatif setelah >127) DAN tidak
        // pernah dicek apakah zone itu sedang dipakai pertandingan/sesi lain — jadi dua
        // pertandingan yang sama sekali tidak berhubungan bisa nyasar ke instance map
        // yang sama persis. Sekarang cari zone yang benar-benar kosong (tidak ada
        // pemain & tidak sedang dipakai King Cup lain) sebelum dipakai.
        GameMap[] temp = GameMap.getMapById(102);
        if (temp == null) {
            throw new IllegalStateException("[KingCup] Map 102 tidak tersedia.");
        }
        // Bug fix: pencarian zone dan penandaannya (kingCupRef = this) tidak atomik,
        // padahal pool map 102 dipakai bersama PvpManager dari thread lain. Dua
        // pencari bisa sama-sama melihat zone yang sama masih kosong dan memakainya
        // berbarengan. Kedua sistem sekarang memakai lock yang sama (ARENA_LOCK).
        synchronized (ARENA_LOCK) {
            short freeZone = -1;
            for (short z = 0; z < temp.length; z++) {
                GameMap zoneMap = temp[z];
                // FIX: sebelumnya cuma cek kingCupRef == null && players.isEmpty(),
                // tidak pernah cek apakah zone ini sedang direservasi sesi duel
                // PvpManager (game.pvp.PvpManager.isZoneUsed()). Karena
                // PvpManager.startSession() mendaftarkan sesi ke `sessions` DULU baru
                // memindahkan pemain (players masih kosong sesaat), KingCup bisa
                // "mencuri" zone yang sebenarnya sudah dipesan duel lain kalau
                // timing-nya pas persis di jendela itu -> dua pertandingan beda
                // numpuk di satu instance map 102 yang sama, typepk kedua sistem pun
                // sama-sama pakai 11/12/13 sehingga saling tumpang tindih.
                if (zoneMap != null && zoneMap.kingCupRef == null && zoneMap.players.isEmpty()
                        && !game.pvp.PvpManager.gI().isZoneUsed(z)) {
                    freeZone = z;
                    break;
                }
            }
            if (freeZone < 0) {
                throw new IllegalStateException("[KingCup] Semua instance arena map 102 sedang penuh, tidak bisa membuat pertandingan baru.");
            }
            this.id = freeZone;
            maps = temp[this.id];
            maps.kingCupRef = this;
        }
    }

    // -------------------------------------------------------------------------
    // Constructor untuk inisialisasi sesi (controller)
    // -------------------------------------------------------------------------
    public KingCup() {
        count            = 0;
        kingCups         = new CopyOnWriteArrayList<>();
        // Tambahkan REGISTRATION_TIME ke total durasi event
        totalTime        = System.currentTimeMillis() + REGISTRATION_TIME + TIME_TOTAL;
        reminder30Sent   = false;
        reminder5Sent    = false;
        // Ronde pertama mulai setelah fase pendaftaran selesai
        NEXT_MATCHES     = System.currentTimeMillis() + REGISTRATION_TIME;
        registrationOpen = true;
        regReminder5Sent = false;
        regReminder1Sent = false;
        threadKingCup    = new Thread(this);
        threadKingCup.setName("KingCup-Thread");
    }

    // -------------------------------------------------------------------------
    // Start & Close
    // -------------------------------------------------------------------------
    /** Lock bersama untuk pemilihan instance/zone map 102 (King Cup & duel PVP). */
    public static final Object ARENA_LOCK = new Object();

    public static void start() {
        // Bug fix: tidak ada guard sama sekali — start() dua kali (mis. dua GM
        // menekan menu hampir bersamaan) membuat thread King Cup kedua berjalan
        // paralel dengan state statis yang sama: ronde dobel, pemain ditarik ke
        // dua pertandingan sekaligus, dan thread lama tidak pernah bisa dimatikan
        // lagi karena referensi threadKingCup sudah ditimpa.
        if (running || threadKingCup != null) {
            return;
        }
        kingCup = new KingCup();
        running = true;
        threadKingCup.start();
        // Fix #11: broadcast pengumuman event dimulai ke seluruh server
        try {
            Manager.gI().chatKTGprocess("[King Cup] Arena King Cup telah dibuka! Pendaftaran terbuka 10 menit. Daftar ke Mrs. Oda sekarang!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void close() {
        running          = false;
        registrationOpen = false;
        kingCup          = null;
        threadKingCup    = null;

        // Bug fix: pertandingan yang masih berjalan saat event ditutup (GM menutup
        // manual atau waktu event habis) tidak pernah dibubarkan. Pemain tertinggal
        // di map 102 sampai relog, dan yang lebih parah maps.kingCupRef tidak pernah
        // di-null-kan sehingga instance arena itu dianggap "terpakai" selamanya —
        // King Cup berikutnya maupun duel PVP biasa kehilangan zone tersebut.
        forceCloseAllMatches();

        try {
            Manager.gI().chatKTGprocess("[King Cup] Arena King Cup Selesai.");
            if (KingCupManager.TURN_KING_CUP >= KingCupManager.MAX_TURN) {
                SaveData.process();
                Manager.gI().chatKTGprocess("[King Cup] Musim selesai! Temui Mrs. Oda untuk mengambil hadiahmu.");
                for (int g = 1; g <= KingCupManager.NUM_GROUPS; g++) {
                    KingCupManager.endSeason(g);
                }
            }
        } catch (IOException | SQLException e) {
            System.err.println("[KingCup] Error saat close/endSeason: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Thread utama King Cup
    // -------------------------------------------------------------------------
    @Override
    public void run() {
        while (running) {
            long now = System.currentTimeMillis();

            // Bug fix: sebelumnya tidak ada yang pernah memanggil update()/kickPlayers()
            // pada match yang sedang berjalan, jadi match tidak pernah selesai lewat
            // waktu habis maupun kombat, dan pemain tidak pernah otomatis di-kick balik
            // ke kota. Proses semua match aktif di setiap tick thread ini.
            // Bug fix: dulu hanya IOException yang ditangkap. Satu RuntimeException
            // (mis. NPE dari refresh() saat ada bot AFK ber-conn null di arena)
            // langsung mematikan thread King Cup — seluruh event berhenti, tidak ada
            // ronde baru, dan pemain yang sedang bertanding terkunci di arena.
            if (kingCups != null) {
                for (KingCup ld : kingCups) {
                    try {
                        ld.update();
                        ld.kickPlayers();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if (now < totalTime) {
                // Fix #11: kirim reminder broadcast terjadwal
                sendScheduledReminders(now);

                if (count < MAX_ROUNDS && NEXT_MATCHES < now) {
                    count  += 1;
                    NEXT_MATCHES = now + TIME_WAR + TIME_BETWEEN_MATCH;

                    // Tutup pendaftaran saat ronde pertama dimulai
                    if (count == 1 && registrationOpen) {
                        registrationOpen = false;
                    }

                    // Fix #11: umumkan ronde baru
                    try {
                        Manager.gI().chatKTGprocess(
                            String.format("[King Cup] Ronde %d dimulai! Peserta harap bersiap.", count));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Fix #14: jemput paksa pemain terdaftar yang belum berada di Map
                    // Tunggu sebelum pengundian, supaya mereka tidak diam-diam terlewat
                    // (lihat KingCupManager.summonRegisteredToLobby()).
                    KingCupManager.summonRegisteredToLobby();

                    ArrayList<Player> gr_60_100  = KingCupManager.setGroup(KingCupManager.group_60_100);
                    ArrayList<Player> gr_101_160 = KingCupManager.setGroup(KingCupManager.group_101_160);
                    ArrayList<Player> gr_161_200 = KingCupManager.setGroup(KingCupManager.group_161_200);
                    ArrayList<Player> gr_201_230 = KingCupManager.setGroup(KingCupManager.group_201_230);
                    ArrayList<Player> gr_231_250 = KingCupManager.setGroup(KingCupManager.group_231_250);
                    ArrayList<Player> gr_251_300 = KingCupManager.setGroup(KingCupManager.group_251_300);
                    

                    randomPk(gr_60_100);
                    randomPk(gr_101_160);
                    randomPk(gr_161_200);
                    randomPk(gr_201_230);
                    randomPk(gr_231_250);
                    randomPk(gr_251_300);

                }
            } else {
                close();
            }

            try {
                Thread.sleep(200L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Fix #11: broadcast reminder 30 menit dan 5 menit sebelum event selesai
    // Flag countdown pendaftaran
    private static volatile boolean regReminder5Sent = false;
    private static volatile boolean regReminder1Sent = false;

    private static void sendScheduledReminders(long now) {
        long remaining = totalTime - now;
        try {
            // Countdown akhir event
            if (!reminder30Sent && remaining <= REMINDER_30_MIN) {
                reminder30Sent = true;
                Manager.gI().chatKTGprocess("[King Cup] King Cup akan berakhir dalam 30 menit!");
            }
            if (!reminder5Sent && remaining <= REMINDER_5_MIN) {
                reminder5Sent = true;
                Manager.gI().chatKTGprocess("[King Cup] King Cup akan berakhir dalam 5 menit!");
            }
            // Countdown fase pendaftaran (hanya ronde pertama belum mulai)
            if (registrationOpen) {
                long regRemaining = NEXT_MATCHES - now;
                if (!regReminder5Sent && regRemaining <= 5 * 60 * 1000L) {
                    regReminder5Sent = true;
                    Manager.gI().chatKTGprocess("[King Cup] Pendaftaran ditutup dalam 5 menit! Segera daftar ke Mrs. Oda.");
                }
                if (!regReminder1Sent && regRemaining <= 60 * 1000L) {
                    regReminder1Sent = true;
                    Manager.gI().chatKTGprocess("[King Cup] Pendaftaran ditutup dalam 1 menit! Pertandingan segera dimulai.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Update state pertandingan — dipanggil dari game loop map
    // -------------------------------------------------------------------------
    public synchronized void update() throws IOException {
        if (finish) return;

        final Player p1 = GameMap.get_player_by_name(name1);
        final Player p2 = GameMap.get_player_by_name(name2);

        // Jeda antar ronde sudah lewat -> kembalikan flag lawan supaya ronde
        // berikutnya bisa saling serang lagi.
        if (neutralized && timeWait < System.currentTimeMillis()) {
            applyFlag(p1, PK_P1);
            applyFlag(p2, PK_P2);
            neutralized = false;
        }

        if (p1 == null && p2 == null) {
            // Kedua pemain disconnect — akhiri tanpa poin
            end_match();

        } else if (timeWar - System.currentTimeMillis() > 0) {
            // Masih dalam waktu pertandingan
            if (p1 == null && p2 != null) {
                p2.point_king_cup += 30;
                end_match();
                KingCupManager.savePointToDb(p2); // Fix #5
                send_notify(String.format("Pertandingan selesai, %s menang melawan %s dengan skor 3 - 0",
                        p2.name, name1));
            } else if (p1 != null && p2 == null) {
                p1.point_king_cup += 30;
                end_match();
                KingCupManager.savePointToDb(p1); // Fix #5
                send_notify(String.format("Pertandingan selesai, %s menang melawan %s dengan skor 3 - 0",
                        p1.name, name2));
            } else {
                if (p1.countWin >= 2) {
                    p1.point_king_cup += 30;
                    end_match();
                    KingCupManager.savePointToDb(p1); // Fix #5
                    KingCupManager.logMatch(name1, name2, p1.countWin, p2.countWin, name1); // Fix #12
                    send_notify(String.format("Pertandingan selesai, %s menang melawan %s dengan skor %d - %d",
                            p1.name, p2.name, p1.countWin, p2.countWin));
                } else if (p2.countWin >= 2) {
                    p2.point_king_cup += 30;
                    end_match();
                    KingCupManager.savePointToDb(p2); // Fix #5
                    KingCupManager.logMatch(name1, name2, p1.countWin, p2.countWin, name2); // Fix #12
                    send_notify(String.format("Pertandingan selesai, %s menang melawan %s dengan skor %d - %d",
                            p2.name, p1.name, p2.countWin, p1.countWin));
                }
            }

        } else {
            // Waktu habis
            if (p1 == null && p2 != null) {
                p2.point_king_cup += 30;
                end_match();
                KingCupManager.savePointToDb(p2); // Fix #5
                send_notify(String.format("Waktu habis, %s menang karena %s disconnect", p2.name, name1));
                return;
            } else if (p1 != null && p2 == null) {
                p1.point_king_cup += 30;
                end_match();
                KingCupManager.savePointToDb(p1); // Fix #5
                send_notify(String.format("Waktu habis, %s menang karena %s disconnect", p1.name, name2));
                return;
            }
            if (p1 != null && p2 != null) {
                if (p1.countWin == p2.countWin) {
                    // Fix #3 (seri): notifikasi eksplisit, tidak ada poin
                    end_match();
                    KingCupManager.logMatch(name1, name2, p1.countWin, p2.countWin, null); // Fix #12
                    send_notify(String.format("Pertandingan seri, tidak ada poin diberikan (%s %d - %d %s)",
                            p1.name, p1.countWin, p2.countWin, p2.name));
                } else if (p1.countWin > p2.countWin) {
                    p1.point_king_cup += 30;
                    end_match();
                    KingCupManager.savePointToDb(p1); // Fix #5
                    KingCupManager.logMatch(name1, name2, p1.countWin, p2.countWin, name1); // Fix #12
                    send_notify(String.format("Waktu habis, %s menang melawan %s dengan skor %d - %d",
                            p1.name, p2.name, p1.countWin, p2.countWin));
                } else {
                    p2.point_king_cup += 30;
                    end_match();
                    KingCupManager.savePointToDb(p2); // Fix #5
                    KingCupManager.logMatch(name1, name2, p1.countWin, p2.countWin, name2); // Fix #12
                    send_notify(String.format("Waktu habis, %s menang melawan %s dengan skor %d - %d",
                            p2.name, p1.name, p2.countWin, p1.countWin));
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Akhir ronde & akhir pertandingan
    // -------------------------------------------------------------------------
    public synchronized void end_round() throws IOException {
        // Bug fix: end_round() dipanggil dari Body.setDie() di thread map, tanpa
        // sinkronisasi dan tanpa cek finish. Kematian yang terjadi setelah
        // pertandingan selesai (atau dua kematian hampir bersamaan) tetap menambah
        // countMatch dan memanggil refresh() lagi — skor & posisi jadi kacau.
        if (finish) return;

        countMatch++;
        // Bug fix: selama jeda 7 detik antar ronde, typepk kedua pemain tetap 12/13
        // sehingga pemenang bisa langsung menyerang lagi begitu lawannya di-refresh
        // (spawn kill) dan countWin bertambah tanpa ronde benar-benar dimulai.
        // Samakan flag dulu selama jeda; dikembalikan ke 12/13 oleh update().
        setNeutralFlag();
        refresh();
        timeWait = System.currentTimeMillis() + 7000L;
        sendTile();
    }

    /**
     * FIX (ditemukan lewat baca client APK): urutan di MainObject.attack() adalah
     *   1) focus.setDie() -> Body.setDie() -> end_round() -> refresh()  [pemain "hidup" lagi:
     *      server kirim paket 32 (HP penuh) ke semua client, client set Action=0]
     *   2) MapService.Player_Die() -> paket 41 ke semua client
     * Client (ReadMessenge.diePlayer) menanggapi paket 41 dengan hp=0 & Action=4 (mati),
     * jadi SETELAH revive dari refresh() client malah menandai pemain itu mati lagi,
     * padahal di server dia hidup. Di client, Action==4 membuat pemain itu tidak bisa
     * menyerang (Player.java: this.Action==4 -> return) dan lawannya tidak bisa
     * menembak targetnya (setFirePlayer butuh target.Action != 4) -> ronde berikutnya
     * "ga bisa saling serang". Dipanggil dari MainObject.attack() tepat SETELAH
     * Player_Die() supaya paket revive (32 + info karakter) tiba paling akhir.
     */
    public synchronized void resyncAliveState() {
        for (Player p : players_attack) {
            if (p == null || p.isdie || p.hp <= 0) continue; // benar-benar mati di server
            try {
                if (p.conn != null && p.conn.connected) {
                    Service.sendMainCharInfo(p);
                    Service.send_combo(p.conn);
                }
                // param 0 = tanpa teks "+HP", cuma memaksa client keluar dari state mati
                Service.usepotion(p, 0, 0);
                Service.usepotion(p, 1, 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void end_match() throws IOException {
        if (finish) return;
        finish = true;
        // Fix #3: reset countWin agar tidak terbawa ke pertandingan berikutnya
        for (Player p : players_attack) {
            if (p != null) {
                p.countWin = 0;
                // Fix #9: reset typepk agar tidak stuck jika pemain login ulang.
                // Bug fix: dulu field-nya di-set diam-diam tanpa paket 42, jadi
                // client masih menampilkan pemain sebagai lawan yang bisa diserang.
                applyFlag(p, PK_NORMAL);
            }
        }
        neutralized = false;
        refresh();
        timeWait = System.currentTimeMillis() + 7000L;
        sendTile();
    }

    // -------------------------------------------------------------------------
    // Utilitas notifikasi
    // -------------------------------------------------------------------------
    public void send_notify(String txt) throws IOException {
        // Bug fix: bot AFK punya conn null dan pemain yang baru DC punya conn
        // tertutup — keduanya bikin NPE/exception di sini, dan karena send_notify()
        // dipanggil tepat setelah end_match(), exception-nya membatalkan sisa alur
        // penutupan pertandingan.
        for (Player viewer : maps.players) {
            if (viewer == null || viewer.conn == null || !viewer.conn.connected) continue;
            Service.send_notice_nobox_white(viewer.conn, txt);
        }
    }

    // -------------------------------------------------------------------------
    // Refresh posisi & HP/MP kedua pemain setelah ronde
    // -------------------------------------------------------------------------
    public synchronized void refresh() throws IOException {
        for (Player p : players_attack) {
            if (p != null) {
                p.isdie = false;
                p.hp    = p.body.getMaxHP();
                p.mp    = p.body.getMaxMP();

                // Bug fix: Service.send_combo(p.conn) & usepotion() menembak conn
                // langsung. Kalau pemain sudah DC (conn null/tertutup) seluruh
                // refresh() gagal di tengah jalan — pemain SATUNYA tidak pernah
                // dipulihkan HP/posisinya, dan end_round()/end_match() ikut batal.
                if (p.conn != null && p.conn.connected) {
                    Service.sendMainCharInfo(p);
                    Service.send_combo(p.conn);
                    Service.usepotion(p, 0, p.body.getMaxHP());
                    Service.usepotion(p, 1, p.body.getMaxMP());
                }

                p.x = name1.equals(p.name) ? X1_FIXED : X2_FIXED;
                p.y = Y_FIXED;
                p.xOld = p.x;
                p.yOld = p.y;

                Message m = new Message(4);
                m.writer().writeByte(0);
                m.writer().writeShort(0);
                m.writer().writeShort(p.objectId);
                m.writer().writeShort(p.x);
                m.writer().writeShort(p.y);
                m.writer().writeByte(-1);
                for (int i = 0; i < maps.players.size(); i++) {
                    Player p_map = maps.players.get(i);
                    if (p_map != null && p_map.conn != null && p_map.conn.connected) {
                        p_map.conn.addmsg(m);
                    }
                }
                m.cleanup();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Kick semua pemain keluar setelah pertandingan selesai
    // -------------------------------------------------------------------------
    public synchronized void kickPlayers() throws IOException {
        if (finish && timeWait < System.currentTimeMillis()) {
            for (int i = maps.players.size() - 1; i >= 0; i--) {
                Player p = maps.players.get(i);
                if (p == null) continue;
                // Bug fix: satu changeMap() yang melempar IOException (mis. pemain
                // sudah DC) menghentikan seluruh loop, jadi pemain berikutnya TIDAK
                // pernah dikeluarkan dari arena dan zone ini tidak pernah dilepas.
                try {
                    // Fix #9: pastikan typepk bersih sebelum kembalikan ke map normal
                    applyFlag(p, PK_NORMAL);
                    if (p.isdie || p.hp <= 0) {
                        p.isdie = false;
                        p.hp = p.body.getMaxHP();
                    }
                    if (p.conn != null && p.conn.connected) {
                        Vgo vgo = new Vgo();
                        vgo.toMap = 1;
                        vgo.toX = (short) 528;
                        vgo.toY = (short) 480;
                        p.changeMap(p, vgo);
                    } else {
                        // Pemain offline: cukup keluarkan dari daftar pemain map
                        // supaya zone bisa dipakai pertandingan berikutnya.
                        MapService.leave(maps, p);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Bug fix: dulu zone hanya dilepas kalau maps.players benar-benar kosong.
            // Kalau ada satu saja pemain yang tersangkut (gagal changeMap, penonton,
            // bot AFK), maps.kingCupRef tidak pernah di-null-kan sehingga instance
            // arena itu hilang selamanya dari pool — untuk King Cup MAUPUN duel PVP.
            // Sekarang tetap dilepas setelah masa tunggu tambahan 30 detik.
            boolean expired = timeWait + 30_000L < System.currentTimeMillis();
            if (maps.players.isEmpty() || expired) {
                releaseArena();
            }
        }
    }

    /** Lepas instance arena ini kembali ke pool dan buang match dari daftar aktif. */
    private void releaseArena() {
        synchronized (ARENA_LOCK) {
            if (maps != null && maps.kingCupRef == this) {
                maps.kingCupRef = null;
            }
        }
        if (kingCups != null) {
            kingCups.remove(this);
        }
    }

    /**
     * Bubarkan semua pertandingan yang masih berjalan — dipakai saat event ditutup.
     */
    private static void forceCloseAllMatches() {
        if (kingCups == null) return;
        for (KingCup ld : kingCups) {
            try {
                ld.finish   = true;
                ld.timeWait = 0;
                ld.kickPlayers();
            } catch (Exception e) {
                e.printStackTrace();
            }
            ld.releaseArena();
        }
        kingCups.clear();
    }

    /**
     * Set flag PK langsung + broadcast paket 42.
     *
     * Bug fix: MapService.changeFlag() tidak bisa dipakai di arena karena
     * langsung return saat gameMap.isMapLoiDai() (map 100/102), dan penulisan
     * p.typepk secara diam-diam membuat client tidak pernah tahu perubahan flag.
     */
    private void applyFlag(Player p, int type) {
        if (p == null) return;
        p.typepk = (byte) type;
        try {
            Message m = new Message(42);
            m.writer().writeShort(p.objectId);
            m.writer().writeByte(type);
            if (p.map != null) {
                MapService.sendMsgPlayerInside(p.map, p, m, true);
            } else if (p.conn != null && p.conn.connected) {
                p.conn.addmsg(m);
            }
            m.cleanup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void notifyArenaFull(Player p) {
        if (p == null || p.conn == null || !p.conn.connected) return;
        try {
            Service.send_notice_box(p.conn, "Semua arena King Cup sedang penuh. Kamu ikut di ronde berikutnya.");
        } catch (IOException ignored) {
        }
    }

    /** Samakan flag kedua peserta: selama jeda mereka tidak bisa saling serang. */
    private void setNeutralFlag() {
        for (Player p : players_attack) {
            applyFlag(p, PK_NEUTRAL);
        }
        neutralized = true;
    }

    // -------------------------------------------------------------------------
    // Pindahkan kedua pemain ke map arena
    // -------------------------------------------------------------------------
    public void getMapPk(final Player p1, final Player p2) throws IOException {
        if (p1 != null && p2 != null) {
            p1.countWin = 0;
            p2.countWin = 0;
            Vgo vgo = new Vgo();
            vgo.toMap = 102;
            vgo.toX   = X1_FIXED;
            vgo.toY   = Y_FIXED;
            p1.typepk = (byte) PK_P1;
            goToLD(p1, vgo, this.id);
            vgo.toX   = X2_FIXED;
            vgo.toY   = Y_FIXED;
            p2.typepk = (byte) PK_P2;
            goToLD(p2, vgo, this.id);

            // Bug fix: timeWait di constructor memberi jeda 7 detik sebelum
            // pertandingan "dimulai", tapi flag 12/13 sudah aktif sejak detik
            // pertama — pemain bisa saling pukul selama hitung mundur, bahkan
            // menghabisi lawan sebelum dia sempat memuat map. Netralkan dulu;
            // update() yang mengembalikan flag 12/13 begitu jeda selesai.
            setNeutralFlag();
        } else {
            System.out.println("[KingCup] Error getMapPk: salah satu atau kedua pemain null (p1="
                    + p1 + ", p2=" + p2 + ")");
        }
    }

    // -------------------------------------------------------------------------
    // Matchmaking acak per grup
    // Fix #8: batasi MAX_PAIRS_PER_ROUND agar tidak membanjiri semua instance arena map 102 sekaligus
    // Fix #13: tracking bye agar pemain yang sama tidak selalu dapat bye
    // -------------------------------------------------------------------------
    public static void randomPk(ArrayList<Player> group) {
        try {
            if (group == null || group.isEmpty()) return;
            Collections.shuffle(group);

            int pairsThisRound = 0;

            while (!group.isEmpty()) {
                // Fix #8: hentikan jika sudah mencapai batas pasangan per ronde
                if (pairsThisRound >= MAX_PAIRS_PER_ROUND) {
                    System.out.println("[KingCup] Batas pasangan per ronde tercapai (" + MAX_PAIRS_PER_ROUND + "), sisa pemain ditunda.");
                    break;
                }

                if (group.size() == 1) {
                    Player p = group.remove(0);
                    // Fix #13: beri poin bye, catat hitungan bye
                    p.point_king_cup += 30;
                    p.bye_count_king_cup = (p.bye_count_king_cup == 0) ? 1 : p.bye_count_king_cup + 1;
                    KingCupManager.savePointToDb(p);
                    if (p.conn != null && p.conn.connected) {
                        Service.send_notice_box(p.conn, "Tidak ada lawan yang ditemukan, kamu mendapat 30 poin. (Bye ke-" + p.bye_count_king_cup + ")");
                    }
                    break;
                }

                // Fix #13: utamakan pemain dengan bye lebih banyak agar dapat lawan duluan
                group.sort((a, b) -> Integer.compare(b.bye_count_king_cup, a.bye_count_king_cup));

                final Player p1 = group.remove(0);
                int c2 = Util.random(group.size());
                final Player p2 = group.remove(c2);

                // Bug fix: constructor melempar IllegalStateException (unchecked)
                // kalau semua instance map 102 penuh. try di bawah cuma menangkap
                // IOException, jadi exception ini lolos keluar dari randomPk() dan
                // run() — THREAD KING CUP MATI: tidak ada ronde baru, pertandingan
                // yang sedang jalan tidak pernah di-update, dan pesertanya terkunci
                // di arena. Sekarang pasangan yang tidak kebagian arena hanya
                // ditunda, event tetap jalan.
                KingCup ld;
                try {
                    ld = new KingCup(p1, p2);
                } catch (RuntimeException ex) {
                    System.err.println("[KingCup] Tidak bisa membuat pertandingan: " + ex.getMessage());
                    notifyArenaFull(p1);
                    notifyArenaFull(p2);
                    break;
                }
                KingCup.kingCups.add(ld);
                ld.getMapPk(p1, p2);
                pairsThisRound++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Teleport pemain ke zone arena tanpa animasi normal changeMap
    // -------------------------------------------------------------------------
    public static void goToLD(Player p, Vgo vgo, short zoneId) {
        p.isChangemap = false;
        GameMap map   = GameMap.getMapByIdAndZone(vgo.toMap, zoneId);
        MapService.leave(p.map, p);
        p.map  = map;
        p.x    = vgo.toX;
        p.y    = vgo.toY;
        p.xOld = p.x;
        p.yOld = p.y;
        MapService.enter(p.map, p);
    }

    // -------------------------------------------------------------------------
    // Kirim info waktu ke pemain di arena
    // Fix #6 (sendTile): countMatch >= 2 bukan >= 3 (best-of-2 sudah selesai)
    // -------------------------------------------------------------------------
    public void sendTile() throws IOException {
        // Bug fix: dulu waktunya dibaca lewat this.maps.kingCupRef — referensi yang
        // di-null-kan saat arena dilepas, jadi pemanggilan sesudah itu NPE. Data ini
        // milik object ini sendiri, baca langsung.
        short timeWarSec  = (short) ((this.timeWar  - System.currentTimeMillis()) / 1000);
        short timeWaitSec = (short) ((this.timeWait - System.currentTimeMillis()) / 1000);
        if (timeWarSec  < 0) timeWarSec  = 0;
        if (timeWaitSec < 0) timeWaitSec = 0;
        // Pertandingan selesai setelah salah satu pemain mencapai 2 kemenangan (best of 2 ronde)
        String title = (finish || countMatch >= 2) ? "Keluar setelah King Cup " : "Pertandingan akan dimulai dalam ";
        for (Player p : players_attack) {
            if (p == null || p.conn == null || !p.conn.connected) continue;
            Service.send_notice_nobox_white(p.conn,
                    title + timeWaitSec + "s | Waktu pertandingan: " + timeWarSec + "s");
        }
    }

    // -------------------------------------------------------------------------
    // Leaderboard — Fix #10
    // -------------------------------------------------------------------------
    public static void sendLeaderboard(Player p) throws IOException {
        KingCupManager.sendLeaderboard(p);
    }
}