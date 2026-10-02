package game.event.btf;

import client.Player;
import client.io.Message;
import core.Manager;
import core.Service;
import core.Util;
import event_daily.Battlefield;
import game.event.GameEvent;
import game.map.GameMap;
import game.map.LeaveItemMap;
import game.map.MapService;
import game.map.MobInMap;
import lombok.Getter;
import lombok.Setter;
import model.map.Vgo;
import template.ItemTemplate3;
import template.MemberBattlefield;
import utils.Timer;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class BTF extends GameEvent {
    // FIX: registerPlayer() dipanggil dari thread NPC/koneksi pemain, sedangkan
    // distributeTeams()/onUpdate() jalan di thread scheduler event — HashMap biasa
    // tidak aman dipakai lintas thread begitu. Pakai ConcurrentHashMap, konsisten
    // dengan Team#players yang sudah begitu.
    private final Map<Integer, Player> participants = new ConcurrentHashMap<>();
    // FIX: sama alasannya kayak participants di atas - teams di-clear()/add()
    // ulang dari thread scheduler tiap match baru (distributeTeams()), tapi
    // dibaca (getPlayerTeam/destroyTower/sendBattlefieldInfo/dst) dari thread
    // map/pemain manapun kapan saja. ArrayList biasa gampang kena
    // ConcurrentModificationException kalau kebetulan match baru mulai pas
    // masih ada pembacaan dari match sebelumnya yang belum selesai.
    private final List<Team> teams = new CopyOnWriteArrayList<>();
    // Bos "Ular Ratu" (Snake Queen) yang muncul di tengah pertempuran,
    // di-restore dari sistem event_daily.Battlefield versi awal.
    private final List<MobInMap> bosses = new ArrayList<>();
    // Leaderboard BXH (top ranking pertempuran terakhir), di-restore dari
    // event_daily.Battlefield#BXH (asalnya event_daily.ChienTruong). Poin
    // per-pemain diambil dari player.pointarena (poin arena global yang
    // sudah ada), bukan poin baru khusus BTF.
    // FIX: bxh.add() dipanggil dari captureBxh(), yang bisa dipicu dari
    // destroyTower() di thread map manapun (rumah tim beda hancur nyaris
    // bersamaan = 2 thread nulis ke ArrayList yang sama bersamaan -> korup).
    private final List<MemberBattlefield> bxh = new CopyOnWriteArrayList<>();
    private BTFState state = BTFState.CLOSED;
    private int timeCount;

    public BTF() {
        super("BTF_DAILY_EVENT", EnumSet.allOf(DayOfWeek.class),
                List.of(
                        new TimeEvent(LocalTime.of(20, 0), LocalTime.of(22, 0))
                )
        );
    }

    public void registerPlayer(Player player) {

        if (state != BTFState.REGISTRATION) {
            player.sendNoticeBox("Pendaftaran belum dibuka");
            return;
        }
        if (player.level < 40) {
            player.sendNoticeBox("Level belum cukup");
            return;
        }

        if (participants.containsKey(player.objectId)) {
            player.sendNoticeBox("Kamu sudah terdaftar");
            return;
        }
        if (player.getGold() < 100_000L) {
            player.sendNoticeBox("Tidak cukup emas");
            return;
        }
        player.updateGold(-100_000L);
        participants.put(player.objectId, player);
        player.sendNoticeBox("Pendaftaran berhasil!");
    }


    @Override
    protected boolean isEnabled() {
        return Manager.gI().event_btf;
    }

    @Override
    protected void onStart() {
        state = BTFState.REGISTRATION;
        timeCount = (int) TimeUnit.MINUTES.toSeconds(15);
        broadcast("Waktu pendaftaran [BTF] telah tiba silakan pergi ke NPC MR.Ballard untuk mendaftar");
    }

    @Override
    protected void onEnd() {
      state = BTFState.CLOSED;
      // Safety net: pastikan status Battlefield lama ikut mati kalau event
      // berakhir lewat jadwal (onEnd) tanpa sempat lewat finishBattle().
      Battlefield.gI().setStatus(0);
    }

    @Override
    protected void onUpdate() {
        if (!isActive()) {
            return;
        }
        timeCount--;
        switch (state) {
            case REGISTRATION:
                if (timeCount <= 0) {
                    closeRegistrationAndPrepare();
                    startBattle();
                }
                break;
            case FIGHTING:
                updateBattle();
                break;
        }
    }

    private void closeRegistrationAndPrepare() {
        distributeTeams();
        teleportPlayersToArena();
    }

    private void startBattle() {
        timeCount = 3600;
        state = BTFState.FIGHTING;
        // Sinkronkan status singleton Battlefield lama (0 sleep, 1 register, 2 start):
        // GameMap#run() masih cek `Battlefield.gI().getStatus() == 2` untuk
        // memanggil PlayerClone.update() (gerak & serang clone "Penjaga"/linh
        // canh di map 54/56/58/60). Tanpa ini, clone penjaga tidak akan pernah
        // bergerak/terlihat karena BTF (event baru) tidak lagi memakai status
        // milik Battlefield lama.
        Battlefield.gI().setStatus(2);
        // BXH pertandingan sebelumnya masih bisa dilihat sampai pertempuran
        // baru benar-benar mulai, persis seperti ChienTruong#start().
        bxh.clear();
        prepareBosses();
        broadcast("[BTF] telah di mulai");
    }

    private void updateBattle() {
        // Munculnya bos "Ular Ratu" di tengah pertempuran, seperti versi awal
        // (event_daily.Battlefield): muncul di menit ke-10 dan menguat lagi di menit ke-20.
        if (timeCount == 60 * 50) {
            spawnBoss(10);
        } else if (timeCount == 60 * 40) {
            spawnBoss(20);
        }

        // TAMBAHAN: dulu HUD battlefield (jumlah rumah/pemain per desa) di-refresh
        // tiap 5 detik lewat hook lama di GameMap#run() (Battlefield.gI().send_info()).
        // Hook itu sudah dihapus karena selalu NullPointerException (info_house
        // singleton lama tidak pernah diisi lagi). Supaya jumlah pemain per desa
        // tetap ter-update meski tidak ada rumah yang hancur (misal pemain mati/
        // keluar/gabung), refresh berkala yang sama sekarang dilakukan dari sini.
        if (timeCount % 5 == 0) {
            broadcastBattlefieldInfo();
        }

        long aliveTeams = teams.stream().filter(t -> !t.isDestroyed()).count();

        if (aliveTeams <= 1 || timeCount <= 0) {
            finishBattle();
        }
    }

    /**
     * Mengumpulkan mob bos yang ada di peta "rumah/boss" battlefield lama
     * (54, 56, 58, 60), dan mereset seluruh mob di peta tersebut supaya siap
     * dipakai lagi di pertempuran baru.
     * Ported apa adanya dari event_daily.Battlefield#start() (byte[] id_map).
     */
    private void prepareBosses() {
        bosses.clear();
        byte[] idMap = new byte[]{54, 56, 58, 60};
        for (byte id : idMap) {
            GameMap[] mapp = GameMap.getMapById(id);
            if (mapp == null) continue;
            for (GameMap gameMap : mapp) {
                for (MobInMap mobb : gameMap.mobs) {
                    mobb.isdie = false;
                    if (mobb.isBoss() && !mobb.isMobCTruongHouse()) {
                        mobb.level = 10;
                        mobb.is_boss_active = false;
                        bosses.add(mobb);
                    }
                }
            }
        }
    }

    /**
     * Ported apa adanya dari event_daily.Battlefield#create_boss(int).
     */
    private void spawnBoss(int i) {
        if (bosses.isEmpty()) {
            return;
        }
        if (i == 20) {
            MobInMap m = null;
            for (MobInMap mobInMap : bosses) {
                if (mobInMap.is_boss_active) {
                    m = mobInMap;
                    break;
                }
            }
            if (m == null) {
                int index = Util.random(bosses.size());
                if (!bosses.get(index).is_boss_active && bosses.get(index).level == 10) {
                    bosses.get(index).level = 100;
                    bosses.get(index).is_boss_active = true;
                }
            } else {
                // FIX: sebelumnya boss `m` yang sudah aktif tidak pernah dinaikkan
                // levelnya — kode malah mencari boss LAIN di zona yang sama untuk
                // diaktifkan, sehingga "menguat lagi di menit ke-20" (lihat komentar
                // prepareBosses/spawnBoss) tidak pernah benar-benar terjadi pada boss
                // yang sedang tampil. Sekarang boss yang sudah aktif itu sendiri yang
                // dinaikkan levelnya.
                m.level = 100;
            }
            broadcast("Ular Ratu telah muncul di medan pertempuran.");
        } else {
            int index = Util.random(bosses.size());
            if (!bosses.get(index).is_boss_active && bosses.get(index).level == 10) {
                bosses.get(index).level = 50;
                bosses.get(index).is_boss_active = true;
            }
            broadcast("Ular Ratu telah muncul di medan pertempuran.");
        }
    }

    /**
     * Loot (gold + item) saat sebuah rumah desa (mob_id 89-92) dihancurkan.
     * Ported apa adanya dari event_daily.Battlefield#Obj_Die(...).
     */
    public static void grantHouseLoot(GameMap gameMap, Player p, MobInMap mob) {
        if (mob == null) return;
        try {
            short[] id_item_leave3 = new short[]{};
            short[] id_item_leave4 = new short[]{};
            short[] id_item_leave7 = new short[]{};
            short sizeRandomMedal = 0;
            switch (mob.template.mob_id) {
                case 89: {
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1, 54, 53, 18};
                    id_item_leave7 = new short[]{11, 13, 2, 3, 2, 3, 14};
                    if (Util.random(100) < 10)
                        id_item_leave3 = new short[]{(short) Util.random(4577, 4585)};
                    sizeRandomMedal = 50;
                    break;
                }
                case 90:
                case 91:
                case 92: {
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1, 54, 53, 18};
                    id_item_leave7 = new short[]{11, 13, 2, 3, 2, 3, 14};
                    if (Util.random(100) < 20)
                        id_item_leave3 = new short[]{(short) Util.random(4577, 4585)};
                    sizeRandomMedal = 60;
                    break;
                }
                default:
                    return;
            }
            for (short id : id_item_leave3) {
                ItemTemplate3 t = ItemTemplate3.item.get(id);
                LeaveItemMap.leave_item_by_type3(gameMap, id, t.getColor(), p, t.getName(), mob.objectId);
            }
            for (int i = 0; i < 3; i++) {
                for (short id : id_item_leave4) {
                    if (id == -1) {
                        LeaveItemMap.leave_gold(gameMap, mob, p);
                    } else {
                        LeaveItemMap.leave_item_by_type4(gameMap, id, p, mob.objectId, p.objectId);
                    }
                }
            }
            for (int i = 0; i < 3; i++) {
                for (short id : id_item_leave7) {
                    LeaveItemMap.leave_item_by_type7(gameMap, id, p, mob.objectId, p.objectId);
                }
            }
            for (int l = 0; l < sizeRandomMedal; l++) {
                LeaveItemMap.leave_item_by_type7(gameMap, (short) Util.random(136, 146), p, mob.objectId, p.objectId);
            }
        } catch (IOException ignore) {
        }
    }

    private void finishBattle() {
        state = BTFState.CLOSED;
        // Matikan lagi status Battlefield lama supaya PlayerClone.update()
        // berhenti dipanggil di luar sesi BTF yang sedang berjalan.
        Battlefield.gI().setStatus(0);
        teams.stream().filter(t -> !t.isDestroyed()).findFirst().ifPresent(winner ->
        {
            try {
                winner.addPoint(100);
            } catch (IOException ignore) {}

            broadcast(String.format("[BTF] telah berakhir, Selamat %s telah memenangkan pertarungan", winner.getName()));

        });

        // Tim yang gugur di tengah jalan sudah dicatat ke BXH lewat
        // destroyTower() (lihat captureBxh()); di sini tinggal catat tim yang
        // masih bertahan sampai waktu habis (termasuk pemenang), lalu urutkan
        // seluruh BXH dari poin tertinggi. Ported dari ChienTruong#finish()
        // (BXH.sort(...)).
        teams.stream().filter(t -> !t.isDestroyed()).forEach(this::captureBxh);
        bxh.sort((a, b) -> Integer.compare(b.point, a.point));

        // Reset bos, seperti event_daily.Battlefield#finish()
        for (MobInMap boss : bosses) {
            boss.level = 10;
            boss.hp = 0;
            boss.is_boss_active = false;
            boss.isdie = true;
        }
        bosses.clear();

        teleportPlayersToVillage();
    }

    private void distributeTeams() {

        List<Player> players = new ArrayList<>(participants.values());

        players.sort(Comparator.comparingInt(Player::getLevel).reversed());

        // Map/rumah/flag persis sama seperti event_daily.Battlefield#start():
        // village 2 -> map 55 (lang gio),  rumah id 90, flag 2
        // village 3 -> map 59 (lang lua),  rumah id 92, flag 1
        // village 4 -> map 57 (lang set),  rumah id 89, flag 4
        // village 5 -> map 53 (lang anh sang), rumah id 91, flag 5
        // FIX: nama tim sebelumnya generik ("DESA BARAT/UTARA/SELATAN/TIMUR")
        // dan tidak cocok dengan nama desa yang benar-benar ditampilkan client
        // (dicek dari string UI di client Android: "Desa Angin", "Desa Api",
        // "Desa Halilintar", "Desa Cahaya"). Nama ini dipakai di pesan
        // broadcast pemenang, jadi harus sama persis dengan yang pemain lihat.
        teams.clear();
        teams.add(new Team("Desa Angin", 90, Vgo.create(55, 224, 256)));
        teams.add(new Team("Desa Api", 92, Vgo.create(59, 240, 224)));
        teams.add(new Team("Desa Halilintar", 89, Vgo.create(57, 264, 272)));
        teams.add(new Team("Desa Cahaya", 91, Vgo.create(53, 276, 246)));
        teams.get(0).setFlag((byte) 2);
        teams.get(1).setFlag((byte) 1);
        teams.get(2).setFlag((byte) 4);
        teams.get(3).setFlag((byte) 5);

        int index = 0;
        for (Player player : players) {
            Player p = Manager.getPlayerById(player.objectId);
            if (p == null) continue;

            Team team = teams.get(index % 4);
            team.addPlayer(p);
            index++;
        }
    }

    private void teleportPlayersToArena() {
        for (Team team : teams) {
            for (String name : team.getPlayers().values()) {
                Player p = Manager.getPlayerByName(name);
                if (p == null)
                    continue;

                p.typepk = team.getFlag();
                // FIX: jangan mutasi team.getLocation() langsung — objek itu di-share
                // oleh semua anggota tim (dan dibaca lagi secara async di onPlayerDie()).
                // Selalu bikin Vgo baru per pemain supaya tidak ada race condition.
                Vgo arena = team.getLocation();
                Vgo vgo = Vgo.create(p, arena.getToMap(), arena.getToX(), arena.getToY());
                try {
                    // Sama seperti versi awal: set flag PK dulu (broadcast ke sekitar)
                    // sebelum pindah map, karena changeFlag() no-op kalau sudah di battlefield map.
                    MapService.changeFlag(p.map, p, team.getFlag());
                    p.changeMap(p, vgo);
                    // FIX: sebelumnya cuma sendUpdateTime() (paket waktu doang) yang
                    // dikirim; paket info rumah/jumlah pemain per desa (yang dipakai
                    // client untuk HUD battlefield) tidak pernah dikirim sama sekali
                    // saat pemain baru masuk arena.
                    sendBattlefieldInfo(p);
                } catch (IOException ignore) {
                }
            }
        }
    }

    private void teleportPlayersToVillage() {
        for (Team team : teams) {
            for (String name : team.getPlayers().values()) {
                Player p = Manager.getPlayerByName(name);
                if (p == null)
                    continue;

                try {
                    // Titik kembali sama seperti event_daily.Battlefield#finish()
                    p.changeMap(p, Vgo.create(p, 1, 432, 354));
                } catch (IOException ignore) {
                }
            }
        }
    }

    public Team getPlayerTeam(int playerId) {
        for (Team team : teams) {
            if (team.isMyTeam(playerId)) {
                return team;
            }
        }
        return null;
    }

    public void destroyTower(int towerId) {
        for (Team team : teams) {
            if (team.getTowerId() == towerId) {
                // Catat pemain tim ini ke BXH DULU sebelum team.destroy() —
                // destroy() mengosongkan (clear) daftar pemain tim, jadi kalau
                // dicatat belakangan (misal pas finishBattle()), nama-nama tim
                // yang gugur duluan sudah keburu hilang.
                captureBxh(team);
                team.destroy();
                // TAMBAHAN: legacy event_daily.Battlefield#update_house_die() selalu
                // mem-broadcast ulang info rumah/jumlah pemain tiap desa ke semua
                // pemain begitu ada rumah yang hancur. Port BTF sebelumnya tidak
                // pernah melakukan ini sama sekali — HUD battlefield di client jadi
                // tidak pernah ter-update.
                broadcastBattlefieldInfo();
                break;
            }
        }
    }

    /**
     * TAMBAHAN: port dari event_daily.Battlefield#send_info(Player).
     * Mengirim paket opcode -94 per desa (village 2-5) berisi status rumah
     * (1 = masih berdiri, 0 = sudah hancur — di sistem baru satu tim cuma
     * punya satu tower/rumah, beda dengan sistem lama yang punya banyak rumah
     * per desa) dan jumlah pemain tim itu yang sedang ada di map battlefield.
     * Client memakai paket ini untuk menampilkan info di layar battlefield;
     * tanpa ini info tersebut tidak pernah ter-update.
     */
    /**
     * TAMBAHAN: wrapper publik dari sendBattlefieldInfo(), dipakai tempat lain
     * (misal menu NPC "Masuk Arena" untuk masuk ulang secara manual) yang perlu
     * menyegarkan HUD battlefield pemain tanpa harus lewat teleportPlayersToArena().
     */
    public void refreshBattlefieldInfoFor(Player p) {
        sendBattlefieldInfo(p);
    }

    private void sendBattlefieldInfo(Player p) {
        try {
            sendUpdateTime(p);
            for (Team team : teams) {
                int village = villageFromTowerId(team.getTowerId());
                if (village == 0) continue;
                Message m = new Message(-94);
                m.writer().writeByte(village);
                m.writer().writeByte(team.isDestroyed() ? 0 : 1);
                m.writer().writeShort(countAlivePlayersInArena(team));
                m.writer().writeByte(1);
                p.conn.addmsg(m);
                m.cleanup();
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * TAMBAHAN: port dari event_daily.Battlefield#total_p_of_house(int).
     */
    private int countAlivePlayersInArena(Team team) {
        int result = 0;
        for (String name : team.getPlayers().values()) {
            Player p0 = Manager.getPlayerByName(name);
            if (p0 != null && GameMap.isBattlefieldMap(p0.map.mapId)) {
                result++;
            }
        }
        return result;
    }

    /**
     * TAMBAHAN: kirim info battlefield terbaru ke seluruh pemain yang masih
     * berada di sebuah tim (dipanggil setiap kali status rumah berubah).
     */
    private void broadcastBattlefieldInfo() {
        for (Team team : teams) {
            for (String name : team.getPlayers().values()) {
                Player p = Manager.getPlayerByName(name);
                if (p != null) {
                    sendBattlefieldInfo(p);
                }
            }
        }
    }

    /**
     * Snapshot poin (player.pointarena) tiap pemain di sebuah tim ke daftar
     * BXH. Dipanggil baik saat tim gugur (destroyTower) maupun saat
     * pertempuran berakhir untuk tim yang masih bertahan (finishBattle).
     * Ported semangatnya dari ChienTruong#finish() (BXH.add(...)), poin
     * per-sesi diganti pakai player.pointarena yang sudah ada.
     */
    private void captureBxh(Team team) {
        int village = villageFromTowerId(team.getTowerId());
        for (String name : team.getPlayers().values()) {
            Player p = Manager.getPlayerByName(name);
            MemberBattlefield entry = new MemberBattlefield();
            entry.name = name;
            entry.village = village;
            entry.received = false;
            entry.point = p != null ? p.pointarena : 0;
            bxh.add(entry);
        }
    }

    /**
     * Sama seperti mapping village di distributeTeams(): 90->2 (lang gio),
     * 92->3 (lang lua), 89->4 (lang set), 91->5 (lang anh sang).
     */
    private int villageFromTowerId(int towerId) {
        switch (towerId) {
            case 90: return 2;
            case 92: return 3;
            case 89: return 4;
            case 91: return 5;
            default: return 0;
        }
    }

    /**
     * Ported dari ChienTruong#get_bxh(String) / event_daily.Battlefield#get_bxh(String).
     * Cuma mengembalikan entry kalau posisinya di top 10.
     */
    public MemberBattlefield getBxhEntry(String name) {
        for (int i = 0; i < bxh.size(); i++) {
            if (bxh.get(i).name.equals(name)) {
                if (i < 10) {
                    return bxh.get(i);
                }
            }
        }
        return null;
    }

    /**
     * Ported dari ChienTruong#get_index_bxh(...) / event_daily.Battlefield#get_index_bxh(...).
     */
    public int getBxhRank(MemberBattlefield entry) {
        return bxh.indexOf(entry);
    }

    public void onPlayerDie(Player player) {
        Team team;
        if ((team = getPlayerTeam(player.objectId)) != null) {
            Timer.countdown(TimeUnit.SECONDS.toMillis(10), tick -> {
                try {
                    Service.send_notice_nobox_white(player.conn, String.format("Cooldown %d second", tick));
                } catch (IOException ignore) {
                }
            }, () -> {
                player.isdie = false;
                player.hp = player.body.getMaxHP();
                player.mp = player.body.getMaxMP();

                // FIX: dulu kode ini mutasi team.getLocation() (objek Vgo yang di-share
                // seluruh anggota tim) langsung dari callback Timer.countdown (async,
                // bisa dipanggil dari thread map manapun). Kalau dua pemain satu tim
                // mati berdekatan waktu, satu bisa menimpa koordinat/tujuan milik yang
                // lain sebelum sempat dipakai changeMap(), dan kalau tim sudah
                // destroyed, toMap/toX/toY-nya ketiban permanen ke titik desa —
                // merusak Vgo arena tim untuk pemakaian berikutnya.
                // Sekarang selalu bikin Vgo baru, khusus untuk pemain ini saja.
                Vgo vgo;
                if (team.isDestroyed()) {
                    vgo = Vgo.create(player, 1, 432, 354);
                } else {
                    Vgo arena = team.getLocation();
                    vgo = Vgo.create(player, arena.getToMap(), arena.getToX(), arena.getToY());
                }

                try {

                    player.changeMap(player, vgo);
                } catch (IOException ignore) {}

            });
        }
    }

    private void sendUpdateTime(Player p) {
        try {
            Message m = new Message(-94);
            m.writer().writeByte(-1); //
            m.writer().writeByte(0);
            int point = Math.max(0, Math.min(p.pointarena, Short.MAX_VALUE));
            m.writer().writeShort(point);
            m.writer().writeByte(0);
            m.writer().writeLong(System.currentTimeMillis() - (60 * 60 - timeCount) * 1000L);
            p.conn.addmsg(m);
            m.cleanup();
        } catch (Exception ignore) {}
    }

    public String getTime() {
        // FIX: sebelumnya detik ditampilkan mentah (timeCount), bukan sisa detik
        // setelah dikurangi menit -> misal 359 detik tampil "5:359 s" alih-alih "5:59 s".
        long minutes = TimeUnit.SECONDS.toMinutes(timeCount);
        long seconds = timeCount - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format("%d:%02d s", minutes, seconds);
    }
}