package game.map;

import game.ai.PlayerBot;
import game.event.GameEventManager;
import history.His_DelItem;
import ai.Clone;
import ai.MobAi;
import ai.PlayerClone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.map.EffMapConfig;

import client.Party;
import client.Pet;
import client.Player;
import core.Manager;
import core.Service;
import core.Util;
import ev_he.MobCay;
import event_daily.UseItemArena;
import event_daily.Battlefield;
import event_daily.DailyQuest;
import client.io.Message;
import client.io.Session;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import model.map.MapData;
import model.map.NpcData;
import model.map.Point;
import model.map.Vgo;
import template.*;

@Slf4j
public class GameMap implements Runnable {

    public static final List<GameMap[]> entrys = new ArrayList<>();

    // Konfigurasi effmap per-map dari database (tabel effmap_config), diisi
    // saat load_database() di Manager. Key 0 = efek global (tampil di SEMUA
    // map) — dulu 5 baris ini hardcoded di sendMapData(), sekarang bisa
    // ditambah/ubah/hapus langsung lewat DB tanpa perlu rebuild server.
    public static Map<Integer, List<EffMapConfig>> effMapConfig = new HashMap<>();
    public final List<Player> players;
    public final List<PlayerBot> bots;
    public long time_use_item_arena = System.currentTimeMillis();
    public byte mapId;
    public byte zoneId;
    // Referensi ke KingCup match yang sedang berjalan di map ini (null jika tidak
    // ada)
    public event_daily.KingCup kingCupRef;
    public final ItemMap[] itemDrop;
    private final Thread mapthread;
    public MobInMap[] mobs;
    public static short head;
    public static short eye;
    public static short hair;
    public static short weapon;
    public static short body;
    public static short leg;
    public static short hat;
    public static short wing;
    public static String name_mo = "";
    public final String[] npcNameData;
    public String name;
    public List<Vgo> vgos;
    public byte typemap;
    public boolean ismaplang;
    public boolean showhs;
    public short maxplayer;
    public byte maxzone;
    private final byte[] map_data;
    private boolean running;
    public int num_mob_super;
    public Dungeon d;
    // public LoiDai ld;
    public short mapW;
    public short mapH;
    public long time_ct;
    public long time_chat;
    public CopyOnWriteArrayList<MobCay> mobEvens = new CopyOnWriteArrayList<>();
    public final CopyOnWriteArrayList<MobInMap> Boss_entrys = new CopyOnWriteArrayList<>();
    public final CopyOnWriteArrayList<MobAi> Ai_entrys;
    public UseItemArena Arena;

    public MapData mapData;

    public GameMap(int id, int zone, MapData mapData) {
        this.mapData = mapData;
        this.mapId = (byte) id;
        this.zoneId = (byte) zone;
        // FIX: sebelumnya NPE kalau kolom npc_data di tabel map_data kosong/null
        // untuk map manapun (mis. map baru yang belum lengkap datanya) — ini bikin
        // SELURUH server gagal start saat MapManager.loadMapData() looping semua map.
        this.npcNameData = mapData.getNpcData() != null
                ? mapData.getNpcData().toArray(String[]::new)
                : new String[0];
        this.name = mapData.getName();
        this.typemap = mapData.getType();
        // FIX: arena bertarung (map 102) TIDAK BOLEH dianggap desa. ismaplang=true
        // bikin
        // MainObject.attack() menolak semua serangan pemain vs pemain (dan client juga
        // menerima flag kota lewat sendMapData), jadi King Cup & duel PVP tidak bisa
        // saling serang walau flag 12/13 sudah benar. Nilai is_city di DB diabaikan.
        this.ismaplang = mapData.getIsCity() == 1 && mapId != 102;
        this.showhs = mapData.getShowHs() == 1;
        this.maxplayer = (short) mapData.getMaxPlayer();
        this.maxzone = (byte) mapData.getMaxZone();
        this.itemDrop = new ItemMap[100];
        this.mapthread = new Thread(this);
        this.players = new ArrayList<>();
        this.bots = new ArrayList<>();
        this.vgos = mapData.getWarpPoint();
        this.running = false;
        this.num_mob_super = 0;
        this.map_data = mapData.toByteArray();
        this.mapW = (short) mapData.getTileData().getWidth();
        this.mapH = (short) mapData.getTileData().getHeight();
        Ai_entrys = new CopyOnWriteArrayList<>();
        if (mapId == 54 || mapId == 56 || mapId == 58 || mapId == 60) {
            Arena = new UseItemArena();
        }
    }

    public void updateData(MapData mapData) {
        this.mapId = (byte) mapData.getId();
        this.name = mapData.getName();
        this.typemap = mapData.getType();
        // FIX: arena bertarung (map 102) TIDAK BOLEH dianggap desa. ismaplang=true
        // bikin
        // MainObject.attack() menolak semua serangan pemain vs pemain (dan client juga
        // menerima flag kota lewat sendMapData), jadi King Cup & duel PVP tidak bisa
        // saling serang walau flag 12/13 sudah benar. Nilai is_city di DB diabaikan.
        this.ismaplang = mapData.getIsCity() == 1 && mapId != 102;
        this.showhs = mapData.getShowHs() == 1;
        this.maxplayer = (short) mapData.getMaxPlayer();
        this.maxzone = (byte) mapData.getMaxZone();
        this.vgos = mapData.getWarpPoint();
        this.mobs = mapData.getMobData()
                .stream()
                .map(point -> {
                    MobInMap mob = new MobInMap();
                    mob.template = MobTemplate.entrys.get(point.getId());
                    mob.x = (short) point.getX();
                    mob.y = (short) point.getY();
                    mob.level = mob.template.level;
                    mob.setBaseDamage(mob.template.damage > 0 ? mob.template.damage : mob.template.level * 500);
                    mob.map_id = mapId;
                    mob.zone_id = zoneId; // FIX: set zone_id agar isValidTarget() akurat
                    mob.isdie = false;
                    mob.time_back = System.currentTimeMillis() + 4_000L;
                    mob.color_name = 0;
                    mob.setBoss(false);
                    mob.setMaxHP(MobTemplate.entrys.get(point.getId()).hpmax);
                    mob.hp = mob.getMaxHP();
                    return mob;
                })
                .toArray(MobInMap[]::new);

        // FIX: Assign objectId unik untuk setiap mob agar attack packet dikirim ke
        // target yang benar.
        // Range 1000+ agar tidak tabrakan dengan objectId player (biasanya < 500).
        for (int i = 0; i < this.mobs.length; i++) {
            this.mobs[i].objectId = 1000 + i;
        }

    }

    @Override
    public void run() {
        this.running = true;
        long time1 = 0;
        long time2 = 0;
        long time3 = 0;
        while (this.running) {
            try {
                time1 = System.currentTimeMillis();
                update();
                update_AI();

                if (this.time_chat < System.currentTimeMillis()) {
                    this.time_chat = System.currentTimeMillis() + 8000L;
                    // auto_chat_npc();
                }
                if (GameMap.isSummonMap(this, true)) {
                    updateClone();
                }
                if (Battlefield.gI().getStatus() == 2 && GameMap.isBattlefieldMap(this.mapId)) {
                    // FIX: baris ini dulu memanggil Battlefield.gI().send_info(p0)
                    // (sistem lama) tiap 5 detik untuk semua pemain di map. Tapi
                    // info_house milik singleton lama itu TIDAK PERNAH diisi lagi
                    // (start() lama sudah jadi dead code sejak diganti BTF baru),
                    // jadi selalu null -> send_info() SELALU NullPointerException.
                    // Exception ini memang ketangkep oleh try-catch di run(), tapi
                    // efeknya serius: PlayerClone.update(this) di bawah ini (AI
                    // "Penjaga" markas) jadi IKUT TIDAK PERNAH JALAN tiap kali NPE
                    // ini kena, karena posisinya sesudah kode yang error dalam
                    // blok if yang sama - jadi penjaga diam/nggak nyerang selama
                    // battle. Sisa tick map (timing/sleep di bawah) juga ke-skip.
                    // HUD info sekarang sudah dikirim benar oleh
                    // BTF#sendBattlefieldInfo()/broadcastBattlefieldInfo(), jadi
                    // panggilan send_info() lama ini dihapus, bukan cuma redundant
                    // tapi memang selalu crash tiap dipanggil.
                    PlayerClone.update(this);
                }
                if (this.mapId == 48 && d != null) {
                    d.update();
                }
                time2 = System.currentTimeMillis();
                time3 = (1_000L - (time2 - time1));
                if (time3 > 0) {
                    if (time3 < 20) {
                        System.err.println("map_id " + this.mapId + " - zone " + (this.zoneId + 1) + " overload...");
                    }

                    try {
                        Thread.sleep(time3);
                    } catch (InterruptedException ignored) {

                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e.toString());
            }
        }
    }

    public boolean isCastleSiegeMap() {
        return mapId >= 83 && mapId <= 87;
    }

    public boolean isMapLoiDai() {
        return mapId == 100 || mapId == 102;
    }

    public void BossDie(MobInMap mob) {
        mob.isdie = true;
        mob.time_back = System.currentTimeMillis() + mob.timeBossRecive;
        synchronized (Boss_entrys) {
            Boss_entrys.remove(mob);
        }
    }

    public void BossIn4(Session conn, int idx) throws IOException {
        for (MobInMap temp : Boss_entrys) {
            if (temp.objectId == idx) {
                Message m = new Message(7);
                m.writer().writeShort(idx);
                m.writer().writeByte((byte) temp.level);
                m.writer().writeShort(temp.x);
                m.writer().writeShort(temp.y);
                m.writer().writeInt(temp.hp);
                m.writer().writeInt(temp.getMaxHP());
                m.writer().writeByte(20); // id skill monster (Spec: 32, ...)
                m.writer().writeInt(temp.timeBossRecive / 1000);
                m.writer().writeShort(-1); // clan monster
                m.writer().writeByte(0);
                m.writer().writeByte(2); // speed
                m.writer().writeByte(0);
                m.writer().writeUTF("");
                m.writer().writeLong(-11111);
                m.writer().writeByte(temp.color_name); // color name 1: blue, 2: yellow
                conn.addmsg(m);
                m.cleanup();
                return;
            }
        }
        Message m2 = new Message(17);
        m2.writer().writeShort(-1);
        m2.writer().writeShort(idx);
        conn.addmsg(m2);
        m2.cleanup();
    }

    public MobInMap GetBoss(int index) {
        for (MobInMap mob : Boss_entrys) {
            if (mob.objectId == index) {
                return mob;
            }
        }
        return null;
    }

    private void update_AI() {

    }

    private synchronized void updateClone() throws IOException {
        // update mo tai nguyen
        Crystal mobtainguyen = Manager.gI().mine.get_mob_in_map(this);
        if (mobtainguyen != null) {
            if (mobtainguyen.hp <= 0) {
                mobtainguyen.setMaxHP((mobtainguyen.getMaxHP() / 10) * 12);
                if (mobtainguyen.getMaxHP() > 20_000_000) {
                    mobtainguyen.setMaxHP(20_000_000);
                }
                mobtainguyen.hp = mobtainguyen.getMaxHP();
                mobtainguyen.isBuffHp = false;
            }
            if (!mobtainguyen.isBuffHp && mobtainguyen.hp < mobtainguyen.getMaxHP() / 2) {
                mobtainguyen.setMaxHP((mobtainguyen.getMaxHP() / 10) * 12);
                if (mobtainguyen.getMaxHP() > 20_000_000) {
                    mobtainguyen.setMaxHP(20_000_000);
                }
                mobtainguyen.isBuffHp = true;
            }
            if (mobtainguyen.isBuffHp && mobtainguyen.timeBuffHp < System.currentTimeMillis()) {
                mobtainguyen.timeBuffHp = System.currentTimeMillis() + 2500L;
                int par = mobtainguyen.getMaxHP() / 20;
                mobtainguyen.hp += par;
                if (mobtainguyen.hp > mobtainguyen.getMaxHP()) {
                    mobtainguyen.hp = mobtainguyen.getMaxHP();
                    mobtainguyen.isBuffHp = false;
                }
                Message m_hp = new Message(32);
                m_hp.writer().writeByte(1);
                m_hp.writer().writeShort(mobtainguyen.objectId);
                m_hp.writer().writeShort(-1); // id potion in bag
                m_hp.writer().writeByte(0);
                m_hp.writer().writeInt(mobtainguyen.getMaxHP()); // max hp
                m_hp.writer().writeInt(mobtainguyen.hp); // hp
                m_hp.writer().writeInt(par); // param use
                for (int i = 0; i < this.players.size(); i++) {
                    this.players.get(i).conn.addmsg(m_hp);
                }
                m_hp.cleanup();
            }
        }

        //
        // FIX: sebelumnya guard/clone dicari dari cloneList cuma dicocokkan lewat
        // map_id, TANPA cek zone. Satu mapId bisa punya banyak instance zone
        // (channel), sementara tambang/crystal cuma ada persis di 1 zone (zone
        // index 4). Akibatnya tiap kali zone LAIN yang mapId-nya sama ikut nge-tick
        // updateClone(), dia bakal "nemu" guard yang sama itu juga, lalu jalanin
        // logic serang balik pakai `this` (GameMap zone yang SALAH) sebagai target
        // broadcast pesan -> paket serangan guard dikirim ke player di zone yang
        // beda, jadi dari sudut pandang player yg beneran ada di zona tambang,
        // clone-nya keliatan diam / gak pernah nyerang balik walau HP-nya kesedot.
        // Sekarang ambil guard langsung dari Crystal (mobtainguyen) yang memang
        // sudah dicocokkan persis ke GameMap (`this`) yang sedang berjalan lewat
        // mine.get_mob_in_map(this) di atas -> guard cuma diproses di zone yang
        // benar-benar jadi rumahnya.
        // Update guard utama + semua clone tambahan (hasil menu tambah clone)
        // pakai logic AI yang sama persis.
        if (mobtainguyen != null) {
            updateGuardAi(mobtainguyen, mobtainguyen.guard);
            for (Clone extra : mobtainguyen.extraGuards) {
                updateGuardAi(mobtainguyen, extra);
            }
        }
    }

    // Radius (dalam pixel) clone penjaga tambang buat cari & tetap ngejar target.
    // Sebelumnya 300, diperlebar biar clone lebih agresif & jangkauannya lebih
    // luas.
    private static final int GUARD_AGGRO_RADIUS = 600;

    /**
     * Logic AI untuk 1 clone penjaga tambang (regen HP + cari & serang
     * target). Dipakai baik untuk guard utama maupun clone tambahan, supaya
     * clone tambahan berperilaku identik dengan guard utama.
     */
    private void updateGuardAi(Crystal mobtainguyen, Clone temp) {
        if (temp == null || temp.isdie) {
            return;
        }
        if (temp.time_hp_buff < System.currentTimeMillis()) {
            temp.time_hp_buff = System.currentTimeMillis() + 2500L;
            if (temp.hp < temp.getMaxHP()) {
                int par = temp.getMaxHP() / 20;
                temp.hp += par;
                if (temp.hp > temp.getMaxHP()) {
                    temp.hp = temp.getMaxHP();
                }
                Message m_hp = new Message(32);
                try {
                    m_hp.writer().writeByte(0);
                    m_hp.writer().writeShort(temp.objectId);
                    m_hp.writer().writeShort(-1); // id potion in bag
                    m_hp.writer().writeByte(0);
                    m_hp.writer().writeInt(temp.getMaxHP()); // max hp
                    m_hp.writer().writeInt(temp.hp); // hp
                    m_hp.writer().writeInt(par); // param use
                    for (int i = 0; i < this.players.size(); i++) {
                        this.players.get(i).conn.addmsg(m_hp);
                    }
                    m_hp.cleanup();
                } catch (Exception ignore) {
                }
            }
        }

        // Cari target baru secara proaktif kalau guard lagi nganggur & tambang
        // sedang dikuasai guild (bukan cuma pas jendela canAttack/event jam
        // rebutan). Target harus dari guild yang beda sama pemilik tambang
        // (guild pemilik dianggap kawan, gak diserang). Player tanpa guild
        // dianggap musuh juga.
        if (temp.target == null && mobtainguyen.guild != null) {
            Player nearestEnemy = null;
            int nearestDist = Integer.MAX_VALUE;
            for (int i = 0; i < this.players.size(); i++) {
                Player pl = this.players.get(i);
                if (pl == null || pl.conn == null || !pl.conn.connected || pl.isdie) {
                    continue;
                }
                if (pl.map.mapId != temp.map_id || pl.map.zoneId != 4) {
                    continue;
                }
                if (mobtainguyen.guild != null && pl.myclan == mobtainguyen.guild) {
                    continue; // guild pemilik tambang == kawan, skip
                }
                int dist = Math.abs(temp.x - pl.x) + Math.abs(temp.y - pl.y);
                if (dist < GUARD_AGGRO_RADIUS && dist < nearestDist) {
                    nearestDist = dist;
                    nearestEnemy = pl;
                }
            }
            if (nearestEnemy != null) {
                temp.target = nearestEnemy;
                temp.isMove = false;
            }
        }

        if (temp.isMove && temp.timeAction < System.currentTimeMillis()) {

        } else if (temp.target != null) {
            if (temp.target.conn.connected && temp.target.map.mapId == temp.map_id
                    && temp.target.map.zoneId == 4
                    && (Math.abs(temp.x - temp.target.x) < GUARD_AGGRO_RADIUS
                            && Math.abs(temp.y - temp.target.y) < GUARD_AGGRO_RADIUS)
                    && !temp.target.isdie) {
                try {
                    MainObject.attack(this, temp, temp.target,
                            Util.random(new int[] { 0, 1, 2, 5, 6, 9, 10, 13, 14, 18 }), null, 2);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                temp.target = null;
                temp.isMove = true;
            }
        }
    }

    private void update() {
        try {
            long _timec = System.currentTimeMillis();
            if (this.mapId >= 53 && this.mapId <= 60 && this.mapId % 2 == 1 && !vgos.isEmpty()) {
                Vgo v = vgos.get(0);
                for (int i1 = players.size() - 1; i1 >= 0 && v != null; i1--) {
                    Player p1 = players.get(i1);
                    if (p1 != null && _timec - p1.timeCantChangeMap > 15_000) {
                        p1.changeMap(p1, v);
                    }
                }
            }

            // <editor-fold defaultstate="collapsed" desc="update Player ...">
            for (int i1 = players.size() - 1; i1 >= 0; i1--) {
                try {
                    Player p = players.get(i1);
                    if (p == null) {
                        players.remove(null);
                        continue;
                    }

                    // ── Mode AFK: socket terputus tapi bot masih aktif ──────
                    if (p.modeBot && p.afkBot != null) {
                        try {
                            p.afkBot.update();
                        } catch (Exception e) {
                            log.warn("[AFK] Error bot update player {}: {}", p.name, e.getMessage());
                        }
                        continue;
                    }
                    // ───────────────────────────────────────────────────────

                    if (p.conn == null || p.conn.socket == null || p.conn.socket.isClosed()
                            || !p.conn.connected) {
                        players.remove(p);
                        if (p != null && p.conn != null) {
                            p.conn.close();
                        }
                        continue;
                    }
                    if (p != null && p.getEffectMedal(StrucEff.TangHinh) != null) {
                        continue;
                    } else if (p != null && p.isTangHinh && p.getEffectMedal(StrucEff.TangHinh) == null) {
                        p.isTangHinh = false;
                        // MapService.update_in4_2_other_inside(p.map, p);
                        Message m6 = new Message(4);
                        m6.writer().writeByte(0);
                        m6.writer().writeShort(0);
                        m6.writer().writeShort(p.objectId);
                        m6.writer().writeShort(p.x);
                        m6.writer().writeShort(p.y);
                        m6.writer().writeByte(-1);
                        MapService.sendMsgPlayerInside(p.map, p, m6, true);
                        m6.cleanup();
                    }

                    if (this.mapId == 50) { // pet_manager
                        long now_time = System.currentTimeMillis();
                        for (Pet temp : p.mypet) {
                            if (temp.expiry_date != 0 && temp.expiry_date < now_time) {
                                if (temp.is_follow) {
                                    p.pet_follow = -1;
                                }
                                p.mypet.remove(temp);
                                Service.sendPlayerWear(p);
                                Service.sendMainCharInfo(p);
                                continue;
                            }
                            if (temp.is_hatch && temp.time_born < now_time) {
                                temp.is_hatch = false;
                                //
                                Message m = new Message(44);
                                //
                                m.writer().writeByte(28);
                                m.writer().writeByte(0);
                                m.writer().writeByte(3);
                                m.writer().writeByte(3);
                                int dem = 0;
                                for (Pet temp2 : p.mypet) {
                                    if (temp.is_hatch && temp2.time_born > now_time) {
                                        dem++;
                                    }
                                }
                                m.writer().writeByte(dem);
                                for (Pet temp2 : p.mypet) {
                                    if (temp.is_hatch && temp2.time_born > now_time) {
                                        int id_ = temp.getId();
                                        m.writer().writeUTF(ItemTemplate3.item.get(id_).getName());
                                        m.writer().writeByte(4); // clazz
                                        m.writer().writeShort(id_);
                                        m.writer().writeByte(14); // type
                                        m.writer().writeShort(ItemTemplate3.item.get(id_).getIcon());
                                        m.writer().writeByte(0); // tier
                                        m.writer().writeShort(10); // level
                                        m.writer().writeByte(0); // color
                                        m.writer().writeByte(1);
                                        m.writer().writeByte(1);
                                        m.writer().writeByte(0); // op size
                                        long time2 = ((temp2.time_born - now_time) / 60000) + 1;
                                        m.writer().writeInt((int) time2);
                                        m.writer().writeByte(0);
                                    }
                                }
                                p.conn.addmsg(m);
                                m.cleanup();
                                //
                                m = new Message(44);
                                m.writer().writeByte(28);
                                m.writer().writeByte(1);
                                m.writer().writeByte(9);
                                m.writer().writeByte(9);
                                m.writer().writeUTF(temp.name);
                                m.writer().writeByte(temp.type);
                                m.writer().writeShort(p.mypet.indexOf(temp)); // id
                                m.writer().writeShort(temp.level);
                                m.writer().writeShort(temp.getLevelPercent()); // exp
                                m.writer().writeByte(temp.type);
                                m.writer().writeByte(temp.spriteImage);
                                m.writer().writeByte(temp.nframe);
                                m.writer().writeByte(temp.color);
                                m.writer().writeInt(temp.get_age());
                                m.writer().writeShort(temp.grown);
                                m.writer().writeShort(temp.maxgrown);
                                m.writer().writeShort(temp.point1);
                                m.writer().writeShort(temp.point2);
                                m.writer().writeShort(temp.point3);
                                m.writer().writeShort(temp.point4);
                                m.writer().writeShort(temp.maxpoint);
                                m.writer().writeByte(temp.op.size());
                                for (int i2 = 0; i2 < temp.op.size(); i2++) {
                                    PetOption temp2 = temp.op.get(i2);
                                    m.writer().writeByte(temp2.id);
                                    m.writer().writeInt(temp2.value);
                                    m.writer().writeInt(temp2.maxValue);
                                }
                                p.conn.addmsg(m);
                                m.cleanup();
                            }
                        }
                    }

                    p.update_wings_time();
                    for (Pet pet : p.mypet) {
                        if (pet.grown > 0 && pet.time_eat < System.currentTimeMillis()) {
                            pet.time_eat = System.currentTimeMillis() + 180_000L;
                            pet.grown -= 1;
                            if (pet.is_follow) {
                                // Service.send_wear(p);
                            }
                        }
                    }
                    p.updateEffect();
                    if (!p.isdie) {
                        // auto +hp,mp
                        p.update(this);

                        feature.teleport.PremiumTeleportManager.gI().onPlayerMapUpdate(p);

                        // auto kurangi hp, mp saat terkena luka bakar api, luka bakar dingin
                        // eff medal
                        Item3 it = p.item.wear[12];
                        if (it != null && it.tier >= 3 && p.time_eff_medal < System.currentTimeMillis()) {
                            p.time_eff_medal = System.currentTimeMillis() + 5_000L;
                            Message m = new Message(-49);
                            m.writer().writeByte(2);
                            m.writer().writeShort(0);
                            m.writer().writeByte(0);
                            m.writer().writeByte(0);
                            switch (it.id) {
                                case 4588: {
                                    byte eff_ = 0;
                                    if (it.tier == 15) {
                                        eff_ = 26;
                                    } else if (it.tier >= 12) {
                                        eff_ = 25;
                                    } else if (it.tier >= 9) {
                                        eff_ = 2;
                                    } else if (it.tier >= 6) {
                                        eff_ = 1;
                                    } else if (it.tier >= 3) {
                                        eff_ = 0;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 4589: {
                                    byte eff_ = 9;
                                    if (it.tier == 15) {
                                        eff_ = 28;
                                    } else if (it.tier >= 12) {
                                        eff_ = 27;
                                    } else if (it.tier >= 9) {
                                        eff_ = 11;
                                    } else if (it.tier >= 6) {
                                        eff_ = 10;
                                    } else if (it.tier >= 3) {
                                        eff_ = 9;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 4590: {
                                    byte eff_ = 6;
                                    if (it.tier == 15) {
                                        eff_ = 32;
                                    } else if (it.tier >= 12) {
                                        eff_ = 31;
                                    } else if (it.tier >= 9) {
                                        eff_ = 8;
                                    } else if (it.tier >= 6) {
                                        eff_ = 7;
                                    } else if (it.tier >= 3) {
                                        eff_ = 6;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 5288: { // calon medal mass
                                    byte eff_ = 6;
                                    if (it.tier == 15) {
                                        eff_ = 72;
                                    } else if (it.tier >= 12) {
                                        eff_ = 72;
                                    } else if (it.tier >= 9) {
                                        eff_ = 72;
                                    } else if (it.tier >= 6) {
                                        eff_ = 72;
                                    } else if (it.tier >= 3) {
                                        eff_ = 72;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 4587: { // 4587
                                    byte eff_ = 3;
                                    if (it.tier == 15) {
                                        eff_ = 30;
                                    } else if (it.tier >= 12) {
                                        eff_ = 29;
                                    } else if (it.tier >= 9) {
                                        eff_ = 5;
                                    } else if (it.tier >= 6) {
                                        eff_ = 4;
                                    } else if (it.tier >= 3) {
                                        eff_ = 3;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 5467: { // 5467 medal evo ksatria
                                    byte eff_ = 3;
                                    if (it.tier == 15) {
                                        eff_ = 73;
                                    } else if (it.tier >= 12) {
                                        eff_ = 73;
                                    } else if (it.tier >= 9) {
                                        eff_ = 73;
                                    } else if (it.tier >= 6) {
                                        eff_ = 73;
                                    } else if (it.tier >= 3) {
                                        eff_ = 73;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 5468: { // 5468 medal evo Penyihir
                                    byte eff_ = 3;
                                    if (it.tier == 15) {
                                        eff_ = 72;
                                    } else if (it.tier >= 12) {
                                        eff_ = 72;
                                    } else if (it.tier >= 9) {
                                        eff_ = 72;
                                    } else if (it.tier >= 6) {
                                        eff_ = 72;
                                    } else if (it.tier >= 3) {
                                        eff_ = 72;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 5469: { // 5469 medal evo assassin
                                    byte eff_ = 3;
                                    if (it.tier == 15) {
                                        eff_ = 74;
                                    } else if (it.tier >= 12) {
                                        eff_ = 74;
                                    } else if (it.tier >= 9) {
                                        eff_ = 74;
                                    } else if (it.tier >= 6) {
                                        eff_ = 74;
                                    } else if (it.tier >= 3) {
                                        eff_ = 74;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 5470: { // 5470 medal evo gunner
                                    byte eff_ = 3;
                                    if (it.tier == 15) {
                                        eff_ = 76;
                                    } else if (it.tier >= 12) {
                                        eff_ = 76;
                                    } else if (it.tier >= 9) {
                                        eff_ = 76;
                                    } else if (it.tier >= 6) {
                                        eff_ = 76;
                                    } else if (it.tier >= 3) {
                                        eff_ = 76;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                            }
                            m.writer().writeShort(p.objectId);
                            m.writer().writeByte(0);
                            m.writer().writeByte(0);
                            m.writer().writeInt(100000);
                            MapService.sendMsgPlayerInside(this, p, m, true);
                            m.cleanup();
                        }
                        it = p.item.wear[20];
                        if (it != null && p.time_eff_wear < System.currentTimeMillis()) {
                            p.time_eff_wear = System.currentTimeMillis() + 5000L;
                            Message m = new Message(-49);
                            m.writer().writeByte(2);
                            m.writer().writeShort(0);
                            m.writer().writeByte(0);
                            m.writer().writeByte(0);
                            switch (it.id) {

                                case 4784: {
                                    byte eff_ = 67;
                                    if (it.tier == 15) {
                                        eff_ = 67;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 4785: {
                                    byte eff_ = 67;
                                    if (it.tier == 15) {
                                        eff_ = 67;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 4786: {
                                    byte eff_ = 67;
                                    if (it.tier == 15) {
                                        eff_ = 67;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 4787: {
                                    byte eff_ = 67;
                                    if (it.tier == 15) {
                                        eff_ = 67;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 5439: { // aura dewan strike
                                    byte eff_ = 57;
                                    if (it.tier == 15) {
                                        eff_ = 57;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 5440: { // aura dewan deff
                                    byte eff_ = 55;
                                    if (it.tier == 15) {
                                        eff_ = 55;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 5477: { // aura dewan support
                                    byte eff_ = 52;
                                    if (it.tier == 15) {
                                        eff_ = 52;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                default: {
                                    byte eff_ = 51;
                                    if (it.tier == 15) {
                                        eff_ = 51;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                            }
                            m.writer().writeShort(p.objectId);
                            m.writer().writeByte(0);
                            m.writer().writeByte(0);
                            m.writer().writeInt(5000);
                            MapService.sendMsgPlayerInside(this, p, m, true);
                            m.cleanup();

                        }
                        it = p.item.wear[22]; // samping ccin
                        if (it != null && p.time_eff_22 < System.currentTimeMillis()) {
                            p.time_eff_22 = System.currentTimeMillis() + 5000L; // 1 detik
                            Message m = new Message(-49);
                            m.writer().writeByte(2); // 1: update effect wear
                            m.writer().writeShort(0); //
                            m.writer().writeByte(0); //
                            m.writer().writeByte(0); //
                            switch (it.id) {
                                case 5494: {
                                    byte eff_ = 84; // 83
                                    if (it.tier == 15) {
                                        eff_ = 84; // 83
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 5495: {
                                    byte eff_ = 85; // 84
                                    if (it.tier == 15) {
                                        eff_ = 85; // 84
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 5496: {
                                    byte eff_ = 86; // 85
                                    if (it.tier == 15) {
                                        eff_ = 86; // 85
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 5497: {
                                    byte eff_ = 87; // 86
                                    if (it.tier == 15) {
                                        eff_ = 87; // 86
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 5500: { // rank 1
                                    byte eff_ = 88; // 86
                                    if (it.tier == 15) {
                                        eff_ = 88; // 86
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 5501: { // rank 2
                                    byte eff_ = 89; // 86
                                    if (it.tier == 15) {
                                        eff_ = 89; // 86
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                case 5502: { // rank 3
                                    byte eff_ = 90; // 86
                                    if (it.tier == 15) {
                                        eff_ = 90; // 86
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                                default: {
                                    byte eff_ = 51;
                                    if (it.tier == 15) {
                                        eff_ = 51;
                                    }
                                    m.writer().writeByte(eff_);
                                    break;
                                }
                            }
                            m.writer().writeShort(p.objectId);
                            m.writer().writeByte(0);
                            m.writer().writeByte(0);
                            m.writer().writeInt(5000);
                            MapService.sendMsgPlayerInside(this, p, m, true);
                            m.cleanup();

                        }
                        // boolean hasKupuKupu = p.item.wear[22] != null && p.item.wear[22].id == 5490;
                        // if (hasKupuKupu && p.bodyEffectId != 80) {
                        // p.bodyEffectId = 80;
                        // Service.sendMainCharInfo(p);
                        // for (Player p0 : this.players) {
                        // if (p0.objectId != p.objectId) {
                        // MapService.send_in4_other_char(this, p0, p);
                        // }
                        // }
                        // } else if (!hasKupuKupu && p.bodyEffectId == 80) {
                        // p.bodyEffectId = -1;
                        // Service.sendMainCharInfo(p);
                        // for (Player p0 : this.players) {
                        // if (p0.objectId != p.objectId) {
                        // MapService.send_in4_other_char(this, p0, p);
                        // }
                        // }
                        // } else if (!hasKupuKupu && p.bodyEffectId == 80) {
                        // p.bodyEffectId = -1;
                        // Service.sendMainCharInfo(p);
                        // for (Player p0 : this.players) {
                        // if (p0.objectId != p.objectId) {
                        // MapService.send_in4_other_char(this, p0, p);
                        // }
                        // }
                        // }
                    }
                } catch (Exception eee) {
                }

            }
            // </editor-fold> update player

            // mob
            // <editor-fold defaultstate="collapsed" desc="update mob, boss ...">
            for (MobInMap mob : this.mobs) {
                mob.update(this);
            }
            for (MobInMap mob : this.Boss_entrys) {
                // mob fire
                mob.update(this);
            }
            // </editor-fold> update mob, boss

        } catch (IOException e) {
            e.printStackTrace();
        }
        // update item map
        for (int i = 0; i < this.itemDrop.length; i++) {
            if (this.itemDrop[i] != null && this.itemDrop[i].idmaster != -1
                    && ((this.itemDrop[i].time_exist - System.currentTimeMillis()) < 15000L)) {
                this.itemDrop[i].idmaster = -1;
            }
            if (this.itemDrop[i] != null && this.itemDrop[i].time_exist < System.currentTimeMillis()) {
                this.itemDrop[i] = null;
            }
        }
    }

    public void start_map() {
        this.mapthread.start();
    }

    public void stop_map() {
        this.running = false;
        this.mapthread.interrupt();
    }

    public static Player get_player_by_name(String name) {
        name = Player.stripDisplayNamePrefix(name);
        for (GameMap[] gameMaps : entrys) {
            for (GameMap gameMap : gameMaps) {
                for (Player p0 : gameMap.players) {
                    if (p0.name.equals(name)) {
                        return p0;
                    }

                }
            }
        }
        return null;
    }

    public static GameMap[] getMapById(int id) {
        for (GameMap[] temp : entrys) {
            if (temp[0].mapId == id) {
                return temp;
            }
        }
        return null;
    }

    public static GameMap getMapByIdAndZone(int id, int zone) {
        for (GameMap[] gameMaps : entrys) {
            if (gameMaps.length > zone && gameMaps[0].mapId == id) {
                return gameMaps[zone];
            }
        }
        return null;
    }

    public void sendMapData(Player p) throws IOException {
        if (p.x / 24 >= mapW || p.y / 24 >= mapH || p.x < 0 || p.y < 0) {
            Vgo vgo = new Vgo();
            vgo.toMap = 1;
            vgo.toX = 432;
            vgo.toY = 354;
            p.changeMap(p, vgo);
            return;
        }

        Message m = new Message(12);
        m.writer().writeShort(this.mapId);
        m.writer().writeShort((short) (p.x / 24));
        m.writer().writeShort((short) (p.y / 24));

        m.writer().write(map_data);

        m.writer().writeByte(0); // Teleport atau Pindah Map ?? 1 = Teleport, 0 = Pindah Map

        m.writer().writeByte(this.zoneId); // zone
        m.writer().writeByte(this.typemap);
        m.writer().writeBoolean(this.ismaplang);
        m.writer().writeBoolean(this.showhs);
        p.conn.addmsg(m);
        m.cleanup();

        sendNpcData(p, mapData);

        // Kirim dekorasi/efek statis map (id part_char tipe 111) dari
        // map_data.item_map_icon.
        // Dikirim per-player di sini (bukan broadcast sekali di awal) supaya tetap
        // muncul buat siapapun yang baru join map, kapan pun mereka masuk.
        //
        // DIMATIKAN SEMENTARA atas permintaan (matikan item_map_icon dulu).
        // Tinggal uncomment blok di bawah buat nyalain lagi.
        /*
         * if (mapData.getItemMapIcon() != null) {
         * log.info("[item_map_icon] map={} count={}", this.mapId,
         * mapData.getItemMapIcon().size());
         * for (Point point : mapData.getItemMapIcon()) {
         * log.info("[item_map_icon] sending id={} x={} y={} to player objectId={}",
         * point.getId(), point.getX(), point.getY(), p.objectId);
         * MapItemEffect.sendTo(p, point.getId(), point.getX(), point.getY());
         * }
         * } else {
         * log.info("[item_map_icon] map={} itemMapIcon NULL", this.mapId);
         * }
         */

        // mob mo tai nguyen
        if (GameMap.isSummonMap(p.map, true)) {
            Crystal crystal = Manager.gI().mine.get_mob_in_map(p.map);
            m = new Message(4);
            m.writer().writeByte(1);
            m.writer().writeShort(64);
            m.writer().writeShort(crystal.objectId);
            m.writer().writeShort(crystal.x);
            m.writer().writeShort(crystal.y);
            m.writer().writeByte(-1);
            if (crystal.guard != null) {
                m.writer().writeByte(0);
                m.writer().writeShort(0);
                m.writer().writeShort(crystal.guard.objectId);
                m.writer().writeShort(crystal.guard.x);
                m.writer().writeShort(crystal.guard.y);
                m.writer().writeByte(-1);
            }
            p.conn.addmsg(m);
            m.cleanup();

            // Sama kayak di Crystal.setDie(): Message(4) di atas cuma marker posisi,
            // bukan paket render karakter. Player yang baru join map ini butuh paket
            // Message(5) (send_in4) juga, kalau nggak clone-nya bakal invisible buat
            // dia meskipun dia masih bisa nyerang si clone.
            if (crystal.guard != null) {
                try {
                    crystal.guard.send_in4(p);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Clone TAMBAHAN (hasil menu tambah clone) juga harus dikirim ke
            // player yang baru masuk map ini, sama seperti guard utama di atas -
            // kalau tidak, clone tambahan invisible buat player yang baru join.
            for (Clone extra : crystal.extraGuards) {
                if (extra == null || extra.isdie)
                    continue;
                Message mExtra = new Message(4);
                mExtra.writer().writeByte(0);
                mExtra.writer().writeShort(0);
                mExtra.writer().writeShort(extra.objectId);
                mExtra.writer().writeShort(extra.x);
                mExtra.writer().writeShort(extra.y);
                mExtra.writer().writeByte(-1);
                p.conn.addmsg(mExtra);
                mExtra.cleanup();
                try {
                    extra.send_in4(p);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        if (GameMap.isBattlefieldMap(this.mapId)) {
            for (PlayerClone clone : Battlefield.gI().list_ai) {
                if (clone.isdie || !clone.gameMap.equals(this))
                    continue;
                Message mClone = new Message(4);
                mClone.writer().writeByte(0);
                mClone.writer().writeShort(0);
                mClone.writer().writeShort(clone.id);
                mClone.writer().writeShort(clone.x);
                mClone.writer().writeShort(clone.y);
                mClone.writer().writeByte(-1);
                p.conn.addmsg(mClone);
                mClone.cleanup();
            }
        }

        List<EffMapConfig> globalEff = effMapConfig.get(0);
        if (globalEff != null) {
            for (EffMapConfig cfg : globalEff) {
                Service.sendEffMap(p, cfg.getIdnpc(), cfg.getIdEff(), cfg.getX(), cfg.getY(), cfg.getB3(), cfg.getB4(),
                        cfg.getB7());
            }
        }
        List<EffMapConfig> mapEff = effMapConfig.get((int) this.mapId);
        if (mapEff != null) {
            for (EffMapConfig cfg : mapEff) {
                Service.sendEffMap(p, cfg.getIdnpc(), cfg.getIdEff(), cfg.getX(), cfg.getY(), cfg.getB3(), cfg.getB4(),
                        cfg.getB7());
            }
        }

        if (this.mapId == 52 && this.zoneId == this.maxzone) {
            m = new Message(-50);
            m.writer().writeByte(1);
            m.writer().writeUTF("Mr Dylan");
            m.writer().writeUTF("Jual Beli");
            m.writer().writeByte(-57); // NPC ID
            m.writer().writeByte(34);
            m.writer().writeShort(384);
            m.writer().writeShort(432);
            m.writer().writeByte(1);
            m.writer().writeByte(1);
            m.writer().writeByte(2);
            m.writer().writeByte(26);
            m.writer().writeUTF(
                    "Saya khusus menjual berbagai jenis barang untuk keperluan berdagang. Dengan senang hati melayani Anda");
            m.writer().writeByte(1);
            m.writer().writeByte(0);
            p.conn.addmsg(m);
            m.cleanup();
        }

        if (mapId == 3) {
            m = new Message(-50);
            m.writer().writeByte(1);
            m.writer().writeUTF("Ibu Mega");
            m.writer().writeUTF("Toko Equipment");
            m.writer().writeByte(-200); // NPC ID
            m.writer().writeByte(50); // ImageID
            m.writer().writeShort(6 * 24); // X
            m.writer().writeShort(15 * 24); // Y
            m.writer().writeByte(1);
            m.writer().writeByte(1);
            m.writer().writeByte(2);
            m.writer().writeByte(42); // ImageBIG
            m.writer().writeUTF(
                    "Hi Ksatria, senang bisa bertemu denganmu. apakah kamu ingin menukarkan token mu?");
            m.writer().writeByte(1); // Is Person
            m.writer().writeByte(0); // ShowHP
            p.conn.addmsg(m);
            m.cleanup();
        }

        // monument
        if (this.mapId == 1 && !GameMap.name_mo.isEmpty()) {
            m = new Message(-96);
            m.writer().writeShort(188); // 288
            m.writer().writeShort(312); // 312
            m.writer().writeShort(0); // 264
            m.writer().writeShort(0); // 288
            m.writer().writeByte(3);
            m.writer().writeByte(1);
            m.writer().writeByte(-1);
            m.writer().writeByte(-25);
            m.writer().writeByte(1);
            m.writer().writeUTF("Top Level");
            m.writer().writeUTF(GameMap.name_mo);
            m.writer().writeByte(-49);
            m.writer().writeByte(15);
            //
            m.writer().writeShort(GameMap.weapon); // weapon
            m.writer().writeShort(GameMap.body); // body
            m.writer().writeShort(-1);
            m.writer().writeShort(-1);
            m.writer().writeShort(-1);
            m.writer().writeShort(3); // pet
            m.writer().writeShort(GameMap.hat); // hat
            m.writer().writeShort(GameMap.leg); // leg
            m.writer().writeShort(-1);
            m.writer().writeShort(-1);
            m.writer().writeShort(GameMap.wing); // wing
            m.writer().writeShort(-1);
            m.writer().writeShort(GameMap.head); // head
            m.writer().writeShort(GameMap.eye); // eye
            m.writer().writeShort(GameMap.hair); // hair
            //
            m.writer().write(Util.loadfile("data/msg/msg_-96_x" + p.conn.zoomlv));
            p.conn.addmsg(m);
            m.cleanup();

        } else if (this.mapId == 50) { // map pet
            m = new Message(44);
            m.writer().writeByte(28);
            m.writer().writeByte(0);
            m.writer().writeByte(3);
            m.writer().writeByte(3);
            int dem = 0;
            long now_time = System.currentTimeMillis();
            for (Pet temp2 : p.mypet) {
                if (temp2.is_hatch && temp2.time_born > now_time) {
                    dem++;
                }
            }
            m.writer().writeByte(dem);
            for (Pet temp2 : p.mypet) {
                if (temp2.is_hatch && temp2.time_born > now_time) {
                    int id_ = temp2.getId();
                    m.writer().writeUTF(ItemTemplate3.item.get(id_).getName());
                    m.writer().writeByte(4); // clazz
                    m.writer().writeShort(id_);
                    m.writer().writeByte(14); // type
                    m.writer().writeShort(ItemTemplate3.item.get(id_).getIcon());
                    m.writer().writeByte(0); // tier
                    m.writer().writeShort(10); // level
                    m.writer().writeByte(0); // color
                    m.writer().writeByte(1);
                    m.writer().writeByte(1);
                    m.writer().writeByte(0); // op size
                    long time2 = ((temp2.time_born - now_time) / 60000) + 1;
                    m.writer().writeInt((int) time2);
                    m.writer().writeByte(0);
                }
            }
            p.conn.addmsg(m);
            m.cleanup();
            //
            m = new Message(44);
            m.writer().writeByte(28);
            m.writer().writeByte(0);
            m.writer().writeByte(9);
            m.writer().writeByte(9);
            m.writer().writeByte(0);
            p.conn.addmsg(m);
            m.cleanup();
            //
            m = new Message(44);
            m.writer().writeByte(28);
            m.writer().writeByte(0);
            m.writer().writeByte(9);
            m.writer().writeByte(9);
            dem = 0;
            for (Pet temp : p.mypet) {
                if (!temp.is_follow && !temp.is_hatch) {
                    dem++;
                }
            }
            m.writer().writeByte(dem); // size pet
            //
            for (int i = 0; i < p.mypet.size(); i++) {
                if (!p.mypet.get(i).is_follow && !p.mypet.get(i).is_hatch) {
                    m.writer().writeUTF(p.mypet.get(i).name);
                    m.writer().writeByte(p.mypet.get(i).type);
                    m.writer().writeShort(i); // id
                    m.writer().writeShort(p.mypet.get(i).level);
                    m.writer().writeShort(p.mypet.get(i).getLevelPercent()); // exp
                    m.writer().writeByte(p.mypet.get(i).type);
                    m.writer().writeByte(p.mypet.get(i).spriteImage);
                    m.writer().writeByte(p.mypet.get(i).nframe);
                    m.writer().writeByte(p.mypet.get(i).color);
                    m.writer().writeInt(p.mypet.get(i).get_age());
                    m.writer().writeShort(p.mypet.get(i).grown);
                    m.writer().writeShort(p.mypet.get(i).maxgrown);
                    m.writer().writeShort(p.mypet.get(i).point1);
                    m.writer().writeShort(p.mypet.get(i).point2);
                    m.writer().writeShort(p.mypet.get(i).point3);
                    m.writer().writeShort(p.mypet.get(i).point4);
                    m.writer().writeShort(p.mypet.get(i).maxpoint);
                    m.writer().writeByte(p.mypet.get(i).op.size());
                    for (int i2 = 0; i2 < p.mypet.get(i).op.size(); i2++) {
                        PetOption temp = p.mypet.get(i).op.get(i2);
                        m.writer().writeByte(temp.id);
                        m.writer().writeInt(temp.value);
                        m.writer().writeInt(temp.maxValue);
                    }
                }
            }
            p.conn.addmsg(m);
            m.cleanup();
        }
    }

    public void sendNpcData(Player p, MapData mapData) {
        Message m = new Message(-50);
        List<Point> points = mapData.getNpc();
        try {

            m.writer().writeByte(points.size());
            for (Point point : points) {
                NpcData npcData = MapManager.getInstance().getNpcData(point.getId());
                if (!GameEventManager.gI().isEventNpcVisible(npcData.getId()))
                    continue;

                // Write NPC data
                m.writer().writeUTF(npcData.getName());
                m.writer().writeUTF(npcData.getDialogName());
                m.writer().writeByte(npcData.getId());
                m.writer().writeByte(npcData.getImageId());
                m.writer().writeShort(point.getX());
                m.writer().writeShort(point.getY());
                m.writer().writeByte(npcData.getWBlock());
                m.writer().writeByte(npcData.getHBlock());
                m.writer().writeByte(npcData.getTotalFrame());
                m.writer().writeByte(npcData.getBigAvatar());
                m.writer().writeUTF(npcData.getDialogText());
                m.writer().writeByte(npcData.isPerson() ? 1 : 0);
                m.writer().writeByte(npcData.isShowHp() ? 1 : 0);

            }
            p.conn.addmsg(m);
        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }

    }

    public static GameMap get_map_dungeon(int id) {
        for (GameMap[] temp : entrys) {
            if (temp[0].mapId == id) {
                return temp[0];
            }
        }
        return null;
    }

    public synchronized void drop_item(Player p, byte type, short id) throws IOException {
        His_DelItem hist = new His_DelItem(p.name);
        hist.Logger = "Buang";
        switch (type) {
            case 3: {
                if (id < 0 || id >= p.item.bag3.length)
                    return;
                Item3 temp = p.item.bag3[id];
                if (temp != null) {
                    if (temp.islock) {
                        Service.send_notice_box(p.conn, "Item telah dikunci");
                        return;
                    }
                    hist.tem3 = temp;
                    hist.Flus();
                    p.item.bag3[id] = null;
                }
                break;
            }
            case 4:
            case 7: {
                hist.tem47 = new Item47();
                hist.tem47.id = id;
                hist.tem47.category = type;
                hist.tem47.quantity = (short) p.item.total_item_by_id(type, id);
                hist.Flus();
                p.item.remove(type, id, p.item.total_item_by_id(type, id));
                break;
            }
        }
        p.item.charInventory(4);
        p.item.charInventory(7);
        p.item.charInventory(3);
    }

    public void sendUseMount(Player p) throws IOException {
        Message m = new Message(-97);
        m.writer().writeByte(0);
        m.writer().writeByte(p.mount != null ? p.mount.getType() : -1);
        m.writer().writeShort(p.objectId);
        MapService.sendMsgPlayerInside(this, p, m, true);
        m.cleanup();
        Service.sendMainCharInfo(p);
    }

    public void broadcastMount(Player mainPlayer) throws IOException {
        Message m = new Message(-97);
        m.writer().writeByte(0);
        m.writer().writeByte(mainPlayer.mount != null ? mainPlayer.mount.getType() : -1);
        m.writer().writeShort(mainPlayer.objectId);
        for (Player player : players) {
            if (player != null && ((Math.abs(player.x - mainPlayer.x) < 200 && Math.abs(player.y - mainPlayer.y) < 200)
                    || GameMap.is_map__load_board_player(mapId)) && (mainPlayer.objectId != player.objectId)) {
                player.conn.addmsg(m);
            }
        }
    }

    public synchronized void pick_item(Session conn, Message m2) throws IOException {
        short id = m2.reader().readShort();
        byte type = m2.reader().readByte();

        if (id < 0 || id >= itemDrop.length)
            return;
        if (itemDrop[id] == null) {
            Message m = new Message(20);
            m.writer().writeByte(type);
            m.writer().writeShort(id);
            m.writer().writeShort(conn.p.objectId);
            MapService.sendMsgPlayerInside(this, conn.p, m, true);
            m.cleanup();
            itemDrop[id] = null;
            return;
        }
        if (type == 3 && itemDrop[id] != null
                && (itemDrop[id].id_item == 3590 || itemDrop[id].id_item == 3591 || itemDrop[id].id_item == 3592)) {
            if (itemDrop[id].idmaster != -1 && conn.p.objectId != itemDrop[id].idmaster) {
                Service.send_notice_nobox_white(conn, "Barang milik orang lain");
                return;
            }
            if (conn.p.pet_di_buon != null && conn.p.pet_di_buon.item.size() < 12) {
                conn.p.pet_di_buon.item.add(itemDrop[id].id_item);
                //
                Message m = new Message(20);
                m.writer().writeByte(type);
                m.writer().writeShort(id);
                m.writer().writeShort(conn.p.objectId);
                MapService.sendMsgPlayerInside(this, conn.p, m, true);
                m.cleanup();
                itemDrop[id] = null;
                //
            } else {
                Service.send_notice_nobox_white(conn, "Tidak bisa diambil!");
            }
            return;
        }
        type = itemDrop[id].category;
        // if (conn.p.isdie || conn.p.in4_auto[3] == 0
        // || (conn.p.in4_auto[4] == -1 && conn.p.in4_auto[5] == 1 && conn.p.in4_auto[6]
        // == 3)) {
        // System.out.println("map.Map.pick_item()"+id+" "+type);
        // return;
        // }
        if (conn.p.isdie) {
            return;
        }
        if (itemDrop[id].idmaster != -1 && conn.p.objectId != itemDrop[id].idmaster) {
            Service.send_notice_nobox_white(conn, "Barang milik orang lain");
            return;
        }
        if (itemDrop[id] != null && (itemDrop[id].idmaster == -1 || conn.p.objectId == itemDrop[id].idmaster)
                && (itemDrop[id].time_pick < System.currentTimeMillis())) {
            if (type == 4 && itemDrop[id].id_item == -1) { // vang
                if (conn.p.in4_auto[5] == 0) {
                    conn.p.updateGold(itemDrop[id].quantity);
                    conn.p.item.charInventory(5);
                    Message m = new Message(20);
                    m.writer().writeByte(type);
                    m.writer().writeShort(id);
                    m.writer().writeShort(conn.p.objectId);
                    MapService.sendMsgPlayerInside(this, conn.p, m, true);
                    m.cleanup();
                    itemDrop[id] = null;
                }
            } else if (itemDrop[id].id_item != -1) {
                if (conn.p.item.get_bag_able() > 0
                        || ((type == 4 || type == 7)
                                && (conn.p.item.total_item_by_id(type, itemDrop[id].id_item) > 0))) {
                    switch (type) {
                        case 3: {
                            if (itemDrop[id].id_item < ItemTemplate3.item.size()) {
                                Short idadd = itemDrop[id].id_item;
                                Item3 itbag = new Item3();
                                itbag.id = idadd;
                                itbag.name = ItemTemplate3.item.get(idadd).getName();
                                itbag.clazz = ItemTemplate3.item.get(idadd).getClazz();
                                itbag.type = ItemTemplate3.item.get(idadd).getType();
                                itbag.level = ItemTemplate3.item.get(idadd).getLevel();
                                itbag.icon = ItemTemplate3.item.get(idadd).getIcon();
                                itbag.op = new ArrayList<>();
                                itbag.op.addAll(itemDrop[id].op);
                                itbag.color = ItemTemplate3.item.get(idadd).getColor();
                                itbag.part = ItemTemplate3.item.get(idadd).getPart();
                                itbag.tier = 0;
                                itbag.islock = false;
                                itbag.time_use = 0;
                                if (conn.p.in4_auto[4] > itbag.color) {
                                    return;
                                }
                                conn.p.item.add_item_bag3(itbag);
                                conn.p.item.charInventory(3);
                            }
                            break;
                        }
                        case 4: {
                            if (itemDrop[id].id_item < ItemTemplate4.item.size()) {
                                Short idadd = itemDrop[id].id_item;
                                if (ItemTemplate4.item.get(idadd).getType() == 1 && conn.p.in4_auto[6] == 1) {
                                    return;
                                } else if (ItemTemplate4.item.get(idadd).getType() == 0 && conn.p.in4_auto[6] == 2) {
                                    return;
                                }
                                Item47 itbag = new Item47();
                                itbag.id = idadd;
                                itbag.quantity = (short) itemDrop[id].quantity;
                                itbag.category = 4;
                                conn.p.item.add_item_bag47(4, itbag);
                                conn.p.item.charInventory(4);
                            }
                            break;
                        }
                        case 7: {
                            if (itemDrop[id].id_item < ItemTemplate7.item.size()) {
                                Short idadd = itemDrop[id].id_item;
                                Item47 itbag = new Item47();
                                itbag.id = idadd;
                                itbag.quantity = (short) itemDrop[id].quantity;
                                itbag.category = 7;
                                conn.p.item.add_item_bag47(7, itbag);
                                conn.p.item.charInventory(7);
                            }
                            break;
                        }
                    }
                    Message m = new Message(20);
                    m.writer().writeByte(type);
                    m.writer().writeShort(id);
                    m.writer().writeShort(conn.p.objectId);
                    MapService.sendMsgPlayerInside(this, conn.p, m, true);
                    m.cleanup();
                    itemDrop[id] = null;
                }
            }
        }
    }

    /**
     * FIX (race condition): cari slot kosong di itemDrop[] DAN langsung
     * "reserve" slot itu (isi placeholder) dalam satu operasi ter-synchronized.
     *
     * SEBELUMNYA: method ini cuma return index kosong, lalu caller baru isi
     * itemDrop[index] di baris berikutnya — ADA JEDA tanpa lock. Kalau 2 mob
     * mati hampir bersamaan (banyak player farming/event rame), 2 thread bisa
     * dapat index yang SAMA dari method ini sebelum salah satunya sempat
     * menulis, sehingga drop yang satu ketimpa drop yang lain dan hilang
     * permanen tanpa pernah sempat di-pick up player (item hilang misterius).
     *
     * SEKARANG: slot langsung ditandai terpakai (placeholder, time_exist jauh
     * di masa depan supaya tidak keburu dibersihkan oleh cleanup tick di
     * update()) sebelum method ini return, jadi thread lain tidak akan
     * pernah melihat slot yang sama sebagai kosong lagi. Caller tetap
     * melakukan `itemDrop[index] = new ItemMap(); ...` seperti biasa —
     * placeholder ini otomatis ketimpa data asli, tidak perlu ubah caller.
     */
    public synchronized int get_item_map_index_able() {
        for (int i = 0; i < itemDrop.length; i++) {
            if (itemDrop[i] == null) {
                ItemMap placeholder = new ItemMap();
                placeholder.time_exist = Long.MAX_VALUE; // cegah kehapus cleanup tick sebelum caller isi data asli
                placeholder.idmaster = -1;
                itemDrop[i] = placeholder;
                return i;
            }
        }
        return -1;
    }

    /**
     * FIX: Pickup item untuk bot AFK — tidak memerlukan Session conn.
     * Hanya item yang boleh diambil bot (bukan milik player lain, dan sudah
     * melewati time_pick).
     *
     * @param player Player bot yang mengambil item
     * @param id     Index itemDrop[]
     */
    public synchronized void pickItemForBot(Player player, int id) throws IOException {
        if (id < 0 || id >= itemDrop.length || itemDrop[id] == null)
            return;
        if (player.isdie)
            return;

        ItemMap item = itemDrop[id];

        // Skip jika milik player lain
        if (item.idmaster != -1 && item.idmaster != player.objectId)
            return;
        // Skip jika masih dalam cooldown
        if (item.time_pick > System.currentTimeMillis())
            return;

        byte type = item.category;

        if (type == 4 && item.id_item == -1) {
            // Gold — hormati toggle "Gold <Loot>" di menu Auto Functions.
            // in4_auto[5] == 0 artinya auto-loot gold DIAKTIFKAN (sama seperti
            // pick_item()).
            if (player.in4_auto == null || player.in4_auto.length <= 5 || player.in4_auto[5] != 0)
                return;

            player.updateGold(item.quantity);
            player.item.charInventory(5);
            itemDrop[id] = null;
            // Broadcast animasi pickup ke pemain lain di map
            Message m = new Message(20);
            m.writer().writeByte(type);
            m.writer().writeShort((short) id);
            m.writer().writeShort(player.objectId);
            MapService.sendMsgPlayerInside(this, player, m, true);
            m.cleanup();

        } else if (item.id_item != -1) {
            if (player.item.get_bag_able() <= 0)
                return; // tas penuh

            switch (type) {
                case 3: {
                    // Equipment — hormati toggle "Items <Loot all>" (filter warna minimum).
                    if (item.id_item < ItemTemplate3.item.size()) {
                        Short idadd = item.id_item;
                        byte itemColor = ItemTemplate3.item.get(idadd).getColor();
                        if (player.in4_auto != null && player.in4_auto.length > 4
                                && player.in4_auto[4] > itemColor) {
                            return; // di bawah warna minimum yang dipilih player → skip
                        }

                        Item3 itbag = new Item3();
                        itbag.id = idadd;
                        itbag.name = ItemTemplate3.item.get(idadd).getName();
                        itbag.clazz = ItemTemplate3.item.get(idadd).getClazz();
                        itbag.type = ItemTemplate3.item.get(idadd).getType();
                        itbag.level = ItemTemplate3.item.get(idadd).getLevel();
                        itbag.icon = ItemTemplate3.item.get(idadd).getIcon();
                        itbag.op = new java.util.ArrayList<>();
                        itbag.op.addAll(item.op);
                        itbag.color = itemColor;
                        itbag.part = ItemTemplate3.item.get(idadd).getPart();
                        itbag.tier = 0;
                        itbag.islock = false;
                        itbag.time_use = 0;
                        player.item.add_item_bag3(itbag);
                        player.item.charInventory(3);
                    }
                    break;
                }
                case 4: {
                    // Potion — hormati toggle "MP,HP <Loot all>".
                    // in4_auto[6] == 1 → skip potion HP (type 1), == 2 → skip potion MP (type 0).
                    if (item.id_item < ItemTemplate4.item.size()) {
                        Short idadd = item.id_item;
                        byte potionType = ItemTemplate4.item.get(idadd).getType();
                        if (player.in4_auto != null && player.in4_auto.length > 6) {
                            if (potionType == 1 && player.in4_auto[6] == 1)
                                return;
                            if (potionType == 0 && player.in4_auto[6] == 2)
                                return;
                        }

                        Item47 itbag = new Item47();
                        itbag.id = idadd;
                        itbag.quantity = (short) item.quantity;
                        itbag.category = 4;
                        player.item.add_item_bag47(4, itbag);
                        player.item.charInventory(4);
                    }
                    break;
                }
                case 7: {
                    if (item.id_item < ItemTemplate7.item.size()) {
                        Short idadd = item.id_item;
                        Item47 itbag = new Item47();
                        itbag.id = idadd;
                        itbag.quantity = (short) item.quantity;
                        itbag.category = 7;
                        player.item.add_item_bag47(7, itbag);
                        player.item.charInventory(7);
                    }
                    break;
                }
                default:
                    return;
            }

            itemDrop[id] = null;
            Message m = new Message(20);
            m.writer().writeByte(type);
            m.writer().writeShort((short) id);
            m.writer().writeShort(player.objectId);
            MapService.sendMsgPlayerInside(this, player, m, true);
            m.cleanup();
        }
    }

    public void create_party(Session conn, Message m2) throws IOException {
        byte type = m2.reader().readByte();
        String name = "";
        Player p0 = null;
        if (type != 0 && type != 5 && type != 4) {
            name = m2.reader().readUTF();
            p0 = GameMap.get_player_by_name(name);
        }
        switch (type) {
            case 1: { // request party other
                if (p0 == null) {
                    Service.send_notice_box(conn, "Terjadi kesalahan, silakan coba lagi");
                    return;
                }
                if (p0.party != null) {
                    if (conn.p.party != null && conn.p.party.get_mems().contains(p0)) {
                        Service.send_notice_box(conn, "Lawan sudah dalam tim");
                    } else {
                        Service.send_notice_box(conn, "Lawan sedang dalam tim lain");
                    }
                    return;
                }
                if (conn.p.party != null) {
                    if (conn.p.party.get_mems().get(0).objectId != conn.p.objectId) {
                        Service.send_notice_box(conn, "Anda bukan ketua tim!");
                        return;
                    }
                    if (conn.p.party.get_mems().size() > 4) {
                        Service.send_notice_box(conn, "tidak bisa mengundang anggota lagi");
                        return;
                    }
                }
                if (conn.p.party == null) {
                    conn.p.party = new Party();
                    conn.p.party.add_mems(conn.p);
                    conn.p.party.sendin4();
                }
                //
                Message m = new Message(48);
                m.writer().writeByte(type);
                m.writer().writeUTF(conn.p.name);
                p0.conn.addmsg(m);
                m.cleanup();
                break;
            }
            case 2: { // accept
                if (conn.p.party != null) {
                    Service.send_notice_box(conn, "Anda sudah dalam grup");
                    return;
                }
                if (p0 == null || (p0 != null && p0.party == null)) {
                    Service.send_notice_box(conn, "Grup tidak ada lagi");
                    return;
                }
                if (p0.party.get_mems().size() > 4) {
                    Service.send_notice_box(conn, "Grup penuh");
                    return;
                } else {
                    conn.p.party = p0.party;
                    p0.party.add_mems(conn.p);
                    p0.party.sendin4();
                    p0.party.send_txt_notice(conn.p.name + " bergabung ke grup");
                }
                break;
            }
            case 3: { // kick
                if (conn.p.party == null) {
                    Service.send_notice_box(conn, "Grup tidak ada");
                    return;
                }
                Player p01 = null;
                for (int i = 0; i < conn.p.party.get_mems().size(); i++) {
                    if (conn.p.party.get_mems().get(i).name.equals(name)) {
                        p01 = conn.p.party.get_mems().get(i);
                        break;
                    }
                }
                if (p01 == null || name.equals("")) {
                    Service.send_notice_box(conn, "Terjadi kesalahan, silakan coba lagi");
                }
                p01.party.remove_mems(p01);
                p01.party.sendin4();
                p01.party = null;
                conn.p.party.send_txt_notice(p01.name + " telah dikeluarkan dari tim");
                Service.send_notice_nobox_white(p01.conn, "Anda telah dikeluarkan dari tim ehehe");
                Message m22 = new Message(48);
                m22.writer().writeByte(5);
                p01.conn.addmsg(m22);
                m22.cleanup();
                break;
            }
            case 4: { // giai tan
                Message m = new Message(48);
                m.writer().writeByte(4);
                for (int i = 1; i < conn.p.party.get_mems().size(); i++) {
                    Player p02 = conn.p.party.get_mems().get(i);
                    p02.conn.addmsg(m);
                    p02.party = null;
                }
                conn.addmsg(m);
                conn.p.party.get_mems().clear();
                conn.p.party = null;
                m.cleanup();
                break;
            }
            case 5: { // leave
                if (conn.p.party.get_mems().get(0).objectId == conn.p.objectId) {
                    Service.send_notice_box(conn, "Sebagai ketua tim tidak boleh meninggalkan grup!");
                    return;
                }
                conn.p.party.remove_mems(conn.p);
                conn.p.party.sendin4();
                conn.p.party.send_txt_notice(conn.p.name + " meninggalkan grup");
                conn.p.party = null;
                //
                Message m = new Message(48);
                m.writer().writeByte(5);
                conn.addmsg(m);
                m.cleanup();
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    public static Player get_player_by_id(int id_player_login) {
        for (GameMap[] gameMaps : entrys) {
            for (GameMap gameMap : gameMaps) {
                for (Player p0 : gameMap.players) {
                    if (p0.objectId == id_player_login) {
                        return p0;
                    }
                }

            }
        }
        return null;
    }

    public static boolean is_map_cant_save_site(byte id) {
        return id == 48 || id == 88 || id == 89 || id == 90 || id == 91 || id == 82 || id == 102 || id == 100
                || (id >= 83 && id <= 87) || (id >= 53 && id <= 61)
                || GameMap.isBattlefieldMap(id);
    }

    public synchronized void add_item_map_leave(GameMap gameMap, Player p_master, ItemMap temp, int mob_index)
            throws IOException {
        for (int i = 0; i < itemDrop.length; i++) {
            if (itemDrop[i] == null) {
                itemDrop[i] = temp;
                Message mi = new Message(19);
                mi.writer().writeByte(temp.category);
                mi.writer().writeShort(mob_index); // index mob die
                switch (temp.category) {
                    case 3: {
                        mi.writer().writeShort(ItemTemplate3.item.get(temp.id_item).getIcon());
                        mi.writer().writeShort(i); //
                        mi.writer().writeUTF(ItemTemplate3.item.get(temp.id_item).getName());
                        break;
                    }
                    case 4: {
                        mi.writer().writeShort(ItemTemplate4.item.get(temp.id_item).getIcon());
                        mi.writer().writeShort(i); //
                        mi.writer().writeUTF(ItemTemplate4.item.get(temp.id_item).getName());
                        break;
                    }
                    case 7: {
                        mi.writer().writeShort(ItemTemplate7.item.get(temp.id_item).getIcon());
                        mi.writer().writeShort(i); //
                        mi.writer().writeUTF(ItemTemplate7.item.get(temp.id_item).getName());
                        break;
                    }
                }
                mi.writer().writeByte(0); // color
                mi.writer().writeShort(-1); // id player
                MapService.sendMsgPlayerInside(gameMap, p_master, mi, true);
                mi.cleanup();
                break;
            }
        }
    }

    public static boolean isSummonMap(GameMap gameMap, boolean is_zone) {
        boolean is_map = false;
        int[] map_ = new int[] { 3, 5, 8, 9, 11, 12, 15, 16, 19, 21, 22, 24, 26, 27, 37, 42 };
        for (int i = 0; i < map_.length; i++) {
            if (map_[i] == gameMap.mapId) {
                is_map = true;
                break;
            }
        }
        return (is_zone) ? (gameMap.zoneId == 4 && is_map) : is_map;
    }

    public static boolean is_map__load_board_player(byte id) {
        // id 117 = map event "Persimpangan Kematian" (Kemerdekaan)
        // id 120 = map event "Gerbang Arwah" (Halloween) — semua objek
        // (mob wave & kristal) harus selalu terlihat oleh player di map ini,
        // tidak dibatasi radius 200 seperti map biasa.
        // FIX: sebelumnya masih 118 di sini padahal GerbangArwah.MAP_ID sudah
        // diganti ke 120 — mismatch ini bikin mob/kristal Halloween cuma
        // kelihatan dalam radius 200 dari player (sama seperti map biasa),
        // bukan selalu terlihat seperti map 117.
        return id == 102 || id == 117 || id == 120;
    }

    public static boolean isBattlefieldMap(byte id) {
        return id >= 53 && id <= 60;
    }

    public boolean isMapChienTruong() {
        return mapId >= 53 && mapId <= 61;
    }

    public MobInMap getMob(int templateId) {
        for (MobInMap mob : mobs) {
            if (mob.template.mob_id == templateId) {
                return mob;
            }
        }
        return null;
    }

    public void broadcast(Consumer<Player> action) {
        if (action == null || players == null || players.isEmpty()) {
            return;
        }

        players.forEach(player -> {
            try {
                action.accept(player);
            } catch (Exception e) {
                log.error("Broadcast error for player {}: {}",
                        player.objectId, e.getMessage(), e);
            }
        });
    }

}