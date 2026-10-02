package ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import client.Player;
import core.Util;
import event_daily.Battlefield;
import client.io.Message;
import game.event.GameEventManager;
import game.event.btf.BTF;
import game.map.GameMap;
import game.map.MapService;
import template.EffTemplate;

public class PlayerClone {

    public static short[][] LOCATION = new short[][]{ //
        new short[]{318, 528, 516, 612}, // 2
        new short[]{416, 552, 120, 200}, // 3
        new short[]{424, 552, 304, 416}, // 4
        new short[]{235, 411, 493, 573} // 5
    };
    public int id;
    public short x, y;
    public GameMap gameMap;
    public boolean isdie;
    public int hp, hp_max;
    public long time_move;
    public int target;
    public int village;
    public int dame;
    // TAMBAHAN: dipakai item4 id 59 "Kemarahan prajurit" (buff kekuatan
    // penjaga markas selama 1 menit). Dicek langsung di titik pakai (atk()),
    // bukan mutasi+revert manual, supaya aman dipakai berkali-kali.
    public long powerBuffUntil = 0;
    public long time_change_target;
    public long time_refresh;

    public static List<PlayerClone> init() {
        // Zona diambil dari peta lama tempat penjaga (linh canh) sebenarnya
        // ditempatkan (54, 56, 58, 60), bukan peta baru (104-108).
        int size2 = GameMap.getMapById(56)[0].maxzone;
        int size3 = GameMap.getMapById(60)[0].maxzone;
        int size4 = GameMap.getMapById(58)[0].maxzone;
        int size5 = GameMap.getMapById(54)[0].maxzone;
        int i = -2;
        List<PlayerClone> result = new ArrayList<>();
        
        int size_linh_canh = 10;
        for (int j = 0; j < size2; j++) {
            for (int j2 = 0; j2 < size_linh_canh; j2++) { // 20 linh canh
                add_linh_canh(result, i--, 56, j, 2);
            }
        }
        for (int j = 0; j < size3; j++) {
            for (int j2 = 0; j2 < size_linh_canh; j2++) { // 20 linh canh
                add_linh_canh(result, i--, 60, j, 3);
            }
        }
        for (int j = 0; j < size4; j++) {
            for (int j2 = 0; j2 < size_linh_canh; j2++) { // 20 linh canh
                add_linh_canh(result, i--, 58, j, 4);
            }
        }
        for (int j = 0; j < size5; j++) {
            for (int j2 = 0; j2 < size_linh_canh; j2++) { // 20 linh canh
                add_linh_canh(result, i--, 54, j, 5);
            }
        }

        return result;
    }

    private static void add_linh_canh(List<PlayerClone> result, int i, int map, int zone, int village) {
        PlayerClone temp = new PlayerClone();
        temp.id = i;
        temp.x = 432;
        temp.y = 520;
        temp.gameMap = GameMap.getMapById(map)[zone];
        temp.isdie = false;
        temp.hp_max = 10_000_000;
        temp.hp = temp.hp_max;
        temp.target = -1;
        temp.village = village;
        temp.dame = 3000;
        result.add(temp);
    }

    public static void update(GameMap gameMap) throws IOException {
        for (int i = 0; i < Battlefield.gI().list_ai.size(); i++) {
            PlayerClone temp = Battlefield.gI().list_ai.get(i);
            if (!temp.isdie && temp.gameMap.equals(gameMap) && temp.time_move < System.currentTimeMillis()) {
                temp.time_move = System.currentTimeMillis() + Util.random(2000, 5000);
                //
                Player p0 = GameMap.get_player_by_id(temp.target);
                if (p0 != null) { // atk
                    if (!p0.isdie && p0.map.equals(temp.gameMap)) {
                        if (Math.abs(p0.x - temp.x) < 150 && Math.abs(p0.y - temp.y) < 150) {
                            temp.x = (short) (p0.x + Util.random(-30, 30));
                            temp.y = (short) (p0.y + Util.random(-30, 30));
                            move(gameMap, temp);
                        }
                        atk(gameMap, temp, p0);
                    } else {
                        temp.target = -1;
                    }
                } else {
                    temp.x = (short) Util.random(PlayerClone.LOCATION[temp.village - 2][0],
                            PlayerClone.LOCATION[temp.village - 2][1]);
                    temp.y = (short) Util.random(PlayerClone.LOCATION[temp.village - 2][2],
                            PlayerClone.LOCATION[temp.village - 2][3]);
                    move(gameMap, temp);
                }
            }
            if (temp.time_refresh < System.currentTimeMillis()) {
                if (temp.isdie) {
                    temp.hp = temp.hp_max;
                    temp.isdie = false;
                } else {
                    temp.hp += 1000;
                    if (temp.hp > temp.hp_max) {
                        temp.hp = temp.hp_max;
                    }
                }
                temp.time_refresh = System.currentTimeMillis() + 60_000L;
            }
        }
    }

    private static void atk(GameMap gameMap, PlayerClone temp, Player p0) throws IOException {
        // TAMBAHAN: buff dari item4 id 59 "Kemarahan prajurit" - damage +50%
        // selama buff aktif (powerBuffUntil > sekarang).
        double powerBuff = (temp.powerBuffUntil > System.currentTimeMillis()) ? 1.5 : 1.0;
        int dame = (int) (temp.dame * powerBuff * Util.random(90, 100)) / 100;
        Message m = new Message(6);
        m.writer().writeShort(temp.id);
        m.writer().writeByte(0); // indexskill
        m.writer().writeByte(1);
        m.writer().writeShort(p0.objectId);
        boolean crit = 10 > Util.random(0, 120);
        // FIX: jika skill refleksi aktif (ef != null) → dijamin 100%; else pakai chance normal
        EffTemplate ef;
        boolean react_dame = p0.get_eff(35) != null || p0.body.getReflectDamage() > Util.random(0, 15000);
        if (crit) {
            dame *= 2;
        }
        int miss = p0.body.getMiss();
        ef = p0.get_eff(34);
        if (ef != null) {
            miss += ef.param;
        }
        if (miss > Util.random(0, 15_000)) {
            dame = 0;
            react_dame = false;
            crit = false;
        }
        p0.hp -= dame;
        if (p0.hp <= 0) {
            p0.hp = 0;
            if (!p0.isdie) {
                p0.dame_affect_special_sk = 0;
                p0.hp = 0;
                p0.isdie = true;
                Message m2 = new Message(41);
                m2.writer().writeShort(p0.objectId);
                m2.writer().writeShort(temp.id);
                m2.writer().writeShort(-1); // point pk
                m2.writer().writeByte(1); // type main object
                MapService.sendMsgPlayerInside(gameMap, p0, m2, true);
                m2.cleanup();

                // FIX: kematian pemain di sini (dibunuh penjaga/"linh canh" markas)
                // TIDAK LEWAT jalur MainObject.attack() sama sekali - ini pipeline
                // serangan terpisah, khusus penjaga menyerang balik pemain. Karena
                // itu BTF#onPlayerDie() (yang menjadwalkan revive 10 detik & pindah
                // pemain kembali ke titik arena/tim) tidak pernah dipanggil untuk
                // kematian jenis ini. Akibatnya pemain yang mati kena serangan
                // penjaga akan macet permanen dalam kondisi mati (isdie=true) di
                // tengah arena - tidak bisa revive maupun kembali sendiri, karena
                // tidak ada timer lain di battlefield yang mem-fasilitasi revive.
                if (GameMap.isBattlefieldMap(gameMap.mapId)) {
                    BTF btf = GameEventManager.gI().getEvent(BTF.class);
                    if (btf != null) {
                        btf.onPlayerDie(p0);
                    }
                }
            }
        }
        //
        m.writer().writeInt(dame); // dame
        m.writer().writeInt(p0.hp); // hp after
        //
        if (dame > 0 && crit) {
            if (react_dame) {
                m.writer().writeByte(2); // size color show
                //
                m.writer().writeByte(4); // 1: xuyen giap, 2:hut hp, 3: hut mp, 4: chi mang, 5: phan don
                m.writer().writeInt((int) dame); // par
                //
                m.writer().writeByte(5);
                m.writer().writeInt((int) dame);
            } else {
                m.writer().writeByte(1); // size color show
                //
                m.writer().writeByte(4); // 1: xuyen giap, 2:hut hp, 3: hut mp, 4: chi mang, 5: phan don
                m.writer().writeInt((int) dame); // par
                //
            }
        } else {
            if (react_dame) {
                m.writer().writeByte(2);
                m.writer().writeByte(0);
                m.writer().writeInt((int) dame);
                m.writer().writeByte(5);
                m.writer().writeInt(dame);
            } else {
                m.writer().writeByte(0);
            }
        }
        if (react_dame && dame > 0) {
            temp.hp -= dame;
            if (temp.hp <= 0) {
                temp.hp = 0;
                temp.isdie = true;
                Message m3 = new Message(8);
                m3.writer().writeShort(temp.id);
                for (int i = 0; i < gameMap.players.size(); i++) {
                    Player pp = gameMap.players.get(i);
                    pp.conn.addmsg(m3);
                }
                m3.cleanup();
            }
        }
        m.writer().writeInt(temp.hp);
        m.writer().writeInt(0);
        m.writer().writeByte(11);
        m.writer().writeInt(0);
        for (int i = 0; i < gameMap.players.size(); i++) {
            Player pp = gameMap.players.get(i);
            pp.conn.addmsg(m);
        }
        m.cleanup();
    }

    private static void move(GameMap gameMap, PlayerClone temp) throws IOException {
        Message m22 = new Message(4);
        m22.writer().writeByte(0);
        m22.writer().writeShort(0);
        m22.writer().writeShort(temp.id);
        m22.writer().writeShort(temp.x);
        m22.writer().writeShort(temp.y);
        m22.writer().writeByte(-1);
        for (int i = 0; i < gameMap.players.size(); i++) {
            Player p0 = gameMap.players.get(i);
            p0.conn.addmsg(m22);
        }
        m22.cleanup();
    }

    public static void atk(GameMap gameMap, Player p, int n2, int indexskill, int dame) throws IOException {

        if ((p.typepk == 2 && gameMap.mapId == 56) || (p.typepk == 1 && gameMap.mapId == 60)
                || (p.typepk == 4 && gameMap.mapId == 58) || (p.typepk == 5 && gameMap.mapId == 54)) {
            return;
        }
        short id = (short) n2;
        for (int i = 0; i < Battlefield.gI().list_ai.size(); i++) {
            PlayerClone temp = Battlefield.gI().list_ai.get(i);
            if (temp.id == id) {
                Message m = new Message(6);
                m.writer().writeShort(p.objectId);
                m.writer().writeByte(indexskill);
                m.writer().writeByte(1);
                m.writer().writeShort(temp.id);
                // if (indexskill == 17) {
                // MapService.add_eff_skill(map, p, p0, indexskill);
                // }
                EffTemplate ef = null;
                int cr = p.body.getCrit();
                ef = p.get_eff(33);
                if (ef != null) {
                    cr += ef.param;
                }
                boolean crit = cr > Util.random(0, 15000);
                if (crit) {
                    dame *= 2;
                }
                if (20 > Util.random(0, 120)) {
                    dame = 0;
                    crit = false;
                }
                if (dame < 0) {
                    dame = 0;
                }
                if (dame > 2_000_000_000) {
                    dame = 2_000_000_000;
                }
                temp.hp -= dame;
                // Tampilkan visual effect combo (img/112) saat skill combo berhasil mengenai
                if (dame > 0 && (indexskill == 19 || indexskill == 20)) {
                    p.showComboEffect();
                }
                if (temp.hp <= 0) {
                    temp.hp = 0;
                    temp.isdie = true;
                    //
                    Message m2 = new Message(41);
                    m2.writer().writeShort(temp.id);
                    m2.writer().writeShort(p.objectId);
                    m2.writer().writeShort(-1); // point pk
                    m2.writer().writeByte(1); // type main object
                    for (int i2 = 0; i2 < gameMap.players.size(); i2++) {
                        Player pp = gameMap.players.get(i2);
                        pp.conn.addmsg(m2);
                    }
                    m2.cleanup();
                    //
                    Message m3 = new Message(8);
                    m3.writer().writeShort(temp.id);
                    for (int i2 = 0; i2 < gameMap.players.size(); i2++) {
                        Player pp = gameMap.players.get(i2);
                        pp.conn.addmsg(m3);
                    }
                    m3.cleanup();
                    //
                    p.updatePointArena(2);
                }
                
                m.writer().writeInt((int) dame); // dame
                m.writer().writeInt(temp.hp); // hp after
                
                if (dame > 0 && crit) {
                    m.writer().writeByte(1); // size color show
                    m.writer().writeByte(4); // 1: xuyen giap, 2:hut hp, 3: hut mp, 4: chi mang, 5: phan don
                    m.writer().writeInt((int) dame); // par
                } else {
                    m.writer().writeByte(0);
                }
                m.writer().writeInt(p.hp);
                m.writer().writeInt(p.mp);
                m.writer().writeByte(11);
                m.writer().writeInt(0);
                MapService.sendMsgPlayerInside(gameMap, p, m, true);
                m.cleanup();
                break;
            }
        }
    }
}