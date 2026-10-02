package event_daily;

import client.Player;
import core.Manager;
import core.Service;
import core.Util;
import client.io.Message;
import client.io.Session;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import game.map.LeaveItemMap;
import game.map.GameMap;
import game.map.MapManager;
import game.map.MobInMap;
import model.map.MapData;
import model.map.Vgo;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import template.ItemTemplate3;
import template.MainObject;
import template.MobTemplate;
import template.box_item_template;

public class CastleSiegeManager {

    public static final ConcurrentHashMap<String, List<String>> Clan_entrys = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Integer, Byte> player_HoiSinh = new ConcurrentHashMap<>();
    private static final List<String>[] subMaps = new ArrayList[4];
    private static int Vang;
    public static boolean isRegister;
    public static long timeAttack;
    private static final MobInMap truChinh = new MobInMap();

    public static void request_livefromdie(GameMap gameMap, Session conn, int type) throws IOException {
        if (type == 1) { // hsl
            Service.send_box_input_yesno(conn, 9,
                    "Butuh " + Util.number_format(GetVangHoiSinh(conn.p)) + " emas untuk bangkit di tempat, ok?");
        } else if (type == 0) { // ve lang
            conn.p.isdie = false;
            conn.p.hp = conn.p.body.getMaxHP();
            conn.p.mp = conn.p.body.getMaxMP();
            if (!CastleSiegeManager.joinMap(conn.p)) {
                Vgo vgo = new Vgo();
                vgo.toMap = 1;
                vgo.toX = (short) 528;
                vgo.toY = (short) 480;
                conn.p.changeMap(conn.p, vgo);
            }
            Service.usepotion(conn.p, 0, conn.p.body.getMaxHP());
            Service.usepotion(conn.p, 1, conn.p.body.getMaxMP());
        }
    }

    public static void ActionHoiSinh(GameMap gameMap, Player p) throws IOException {
        if (!gameMap.isCastleSiegeMap()) {
            return;
        }
        int vang = GetVangHoiSinh(p);
        if (p.getGold() >= vang) {
            p.isdie = false;
            p.hp = p.body.getMaxHP();
            p.mp = p.body.getMaxMP();
            p.updateGold(-vang);
            UpdateVang(vang);
            int counths = player_HoiSinh.getOrDefault(p.objectId, (byte) 1);
            if (counths < 100) {
                player_HoiSinh.replace(p.objectId, (byte) (counths + 1));
            }
            p.item.charInventory(5);
            Service.sendMainCharInfo(p);
            // chest in4
            Service.send_combo(p.conn);
            Service.usepotion(p, 0, p.body.getMaxHP());
            Service.usepotion(p, 1, p.body.getMaxMP());
        } else {
            Service.send_notice_box(p.conn, "Tidak cukup " + Util.number_format(vang) + " emas untuk melakukan");
        }
    }

    public static int GetVangHoiSinh(Player p) {
        int counths = player_HoiSinh.getOrDefault(p.objectId, (byte) 1);
        if (counths < 100) {
            return counths * 5_000;
        } else {
            return 100 * 5_000;
        }
    }

    public static int GetVang() {
        return Vang;
    }

    public static synchronized void UpdateVang(int vangjoin) {
        if (Vang + vangjoin > 2_000_000_000) {
            Vang = 2_000_000_000;
        } else {
            Vang += vangjoin;
        }
    }

    public static void init() {

        for (int i = 83; i <= 86; i++) {
            String name = "Gerbang Timur";
            switch (i) {
                case 84 -> name = "Gerbang Barat";
                case 85 -> name = "Gerbang Selatan";
                case 86 -> name = "Gerbang Utara";
                default -> {
                }
            }
            List<Vgo> vgos = new ArrayList<>();
            vgos.add(getVgo(i));
            MapData mapData = MapManager.getInstance().getMapData(i);
            GameMap m = new GameMap(i, 0, mapData);
            MobInMap mob = new MobInMap();
            mob.template = MobTemplate.entrys.get(153);
            mob.objectId = 1;
            mob.ishs = false;
            mob.isATK = false;
            mob.setMaxHP(1_000_000);
            mob.x = 324;
            mob.y = 216;
            mob.map_id = (byte) i;
            mob.zone_id = 0;
            mob.level = 1;
            mob.time_refresh = 1000000;

            MobInMap mob1 = new MobInMap();
            mob1.template = MobTemplate.entrys.get(154);
            mob1.objectId = 2;
            mob1.isATK = true;
            mob1.setMaxHP(5_000_000);
            mob1.x = 187;
            mob1.y = 164;
            mob1.map_id = (byte) i;
            mob1.zone_id = 0;
            mob1.level = 1;
            mob1.setBaseDamage(10_000);
            MobInMap mob2 = new MobInMap();
            mob2.template = MobTemplate.entrys.get(154);
            mob2.objectId = 3;
            mob2.isATK = true;
            mob2.setMaxHP(1_000_000);
            mob2.x = 451;
            mob2.y = 164;
            mob2.map_id = (byte) i;
            mob2.zone_id = 0;
            mob2.level = 1;
            mob2.setBaseDamage(10_000);

            m.mobs = new MobInMap[]{mob, mob1, mob2};
            m.start_map();
            GameMap.entrys.add(new GameMap[]{m});
        }
        MapData mapData = MapManager.getInstance().getMapData(87);
        GameMap m = new GameMap(87, 0, mapData);
        m.mobs = new MobInMap[5];
        for (int i = 0; i < 5; i++) {
            m.mobs[i] = new MobInMap();
            if (i == 4) {
                m.mobs[i] = truChinh;
                m.mobs[i].setBaseDamage(100_000);
            } else {
                m.mobs[i].setBaseDamage(50_000);
            }
            m.mobs[i].template = MobTemplate.entrys.get(i < 4 ? 151 : 152);
            m.mobs[i].objectId = i + 10;
            m.mobs[i].ishs = false;
            m.mobs[i].isATK = true;
            m.mobs[i].setMaxHP(i < 4 ? 200_000_000 : 700_000_000);
            // if (i < 4) {
            // m.mobs[i].time_refresh = 1000000;
            // }
            m.mobs[i].time_refresh = 1000000;
            switch (i) {
                case 0 -> {
                    m.mobs[i].x = 234;
                    m.mobs[i].y = 864;
                }
                case 1 -> {
                    m.mobs[i].x = 870;
                    m.mobs[i].y = 900;
                }
                case 2 -> {
                    m.mobs[i].x = 882;
                    m.mobs[i].y = 312;
                }
                case 3 -> {
                    m.mobs[i].x = 240;
                    m.mobs[i].y = 336;
                }
                case 4 -> {
                    m.mobs[i].x = 613;
                    m.mobs[i].y = 630;
                }
                default -> {
                }
            }

            m.mobs[i].map_id = (byte) 87;
            m.mobs[i].zone_id = 0;
            m.mobs[i].level = 1;
        }
        m.start_map();
        GameMap.entrys.add(new GameMap[]{m});
    }

    public static void ResetMap() {
        NamePlayerOwner = null;
        idIconClan = -1;
        NameClan = null;
        timeDa = 0;
        truChinh.isdie = false;
        truChinh.hp = truChinh.getMaxHP();
        for (GameMap[] gameMap : GameMap.entrys) {
            if (gameMap == null || gameMap.length == 0 || !gameMap[0].isCastleSiegeMap()) {
                continue;
            }
            for (GameMap m : gameMap) {
                for (MobInMap mob : m.mobs) {
                    mob.isdie = false;
                    mob.hp = mob.getMaxHP();
                }
            }
        }
    }

    private static Vgo getVgo(int mapid) {
        Vgo v = new Vgo();
        v.toMap = 87;
        v.x = 324;
        v.y = 216;
        if (mapid == 83) {
            v.toX = 610;
            v.toY = 110;
        } else if (mapid == 84) {
            v.toX = 1120;
            v.toY = 576;
        } else if (mapid == 85) {
            v.toX = 610;
            v.toY = 1120;
        } else {
            v.toX = 80;
            v.toY = 576;
        }
        return v;
    }

    public static void StartRegister() {
        if (isRegister) {
            return;
        }
        ResetMap();
        Manager.ResetCThanh();
        isRegister = true;
        try {
            Manager.gI().chatKTGprocess(
                    "Waktu pendaftaran perebutan kastil telah tiba, para pemimpin segera bawa tim untuk mendaftar berpartisipasi");
        } catch (Exception e) {
        }
    }

    public static void EndRegister() {
        if (!isRegister) {
            return;
        }
        isRegister = false;
        timeAttack = System.currentTimeMillis() + 1000 * 60 * 90;
        SetupMap();
        try {
            Manager.gI()
                    .chatKTGprocess("Waktu perebutan kastil telah tiba, bertempur dengan sepenuh hati para pahlawan");
        } catch (Exception e) {
        }
    }

    public static void SetupMap() {
        synchronized (subMaps) {
            int k = 0;
            for (java.util.Map.Entry<String, List<String>> entry : Clan_entrys.entrySet()) {
                if (subMaps[k] == null) {
                    subMaps[k] = new ArrayList<>();
                }
                String key = entry.getKey();
                subMaps[k].add(key);
                k = (k + 1) % 4;
            }
            for (int i = Session.SESSION_LIST.size() - 1; i >= 0; i--) {
                Session s = Session.SESSION_LIST.get(i);
                if (s.connected && s.get_in4 && s.p != null && s.p.map != null) {
                    try {
                        joinMap(s.p);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    public static void CloseMap() {
        for (GameMap[] gameMap : GameMap.entrys) {
            if (gameMap == null || gameMap.length == 0 || !gameMap[0].isCastleSiegeMap()) {
                continue;
            }
            for (GameMap m : gameMap) {
                for (int i = m.players.size() - 1; i >= 0; i--) {
                    Player p = m.players.get(i);
                    if (p == null) {
                        continue;
                    }
                    try {
                        Vgo v = new Vgo();
                        v.toMap = 1;
                        v.toX = 350;
                        v.toY = 350;
                        p.changeMap(p, v);
                    } catch (Exception e) {
                        try {
                            p.conn.close();
                        } catch (Exception ee) {
                        }
                    }
                }
            }
        }
    }

    public static boolean joinMap(Player p) throws IOException {
        if (subMaps == null || p == null || p.myclan == null || !Clan_entrys.containsKey(p.myclan.name)
                || !Clan_entrys.get(p.myclan.name).contains(p.name) || timeAttack < System.currentTimeMillis()) {
            return false;
        }
        for (int i = 0; i < subMaps.length; i++) {
            if (!subMaps[i].contains(p.myclan.name)) {
                continue;
            }
            Vgo v = new Vgo();
            v.toMap = (byte) (i + 83);
            v.toX = 300;
            v.toY = 300;
            p.changeMap(p, v);
            return true;
        }
        return false;
    }

    public static void ClanRegister(Player p) throws IOException {
        if (!isRegister) {
            Service.send_notice_box(p.conn, "Tidak dalam waktu pendaftaran.");
        } else if (p.myclan == null) {
            Service.send_notice_box(p.conn, "Fitur hanya untuk clan.");
        } else if (!p.myclan.members.get(0).name.equals(p.name)) {
            Service.send_notice_box(p.conn, "Anda bukan pemimpin.");
        } else if (Clan_entrys.containsKey(p.myclan.name)) {
            Service.send_notice_box(p.conn, "Clan Anda sudah ada dalam daftar.");
        } else if (p.party == null || p.party.get_mems().size() < 5) {
            Service.send_notice_box(p.conn,
                    "Perlu membuat grup 5 anggota clan dengan level dari 60 untuk berpartisipasi.");
        } else if (p.myclan.get_vang() < 10_000_000) {
            Service.send_notice_box(p.conn, "Perlu minimal 10 juta dana guild untuk mendaftar.");
        } else {
            List<String> nameP = new ArrayList<>();
            for (int i = 0; i < p.party.get_mems().size(); i++) {
                Player p2 = p.party.get_mems().get(i);
                if (p2 == null || p2.conn == null || !p2.conn.connected) {
                    Service.send_notice_box(p.conn, "Terjadi kesalahan, silakan buat ulang grup dan coba lagi.");
                    return;
                }
                if (p2.myclan == null || !p2.myclan.name.equals(p.myclan.name) || p2.level < 60) {
                    Service.send_notice_box(p.conn,
                            "Perlu membuat grup 5 anggota clan dengan level dari 60 untuk berpartisipasi.");
                    return;
                }
                nameP.add(p2.name);
            }
            p.myclan.updateGold(-10_000_000);
            UpdateVang(10_000_000);
            Clan_entrys.put(p.myclan.name, nameP);
            String chat = "Pemimpin telah mendaftar berpartisipasi dalam perebutan kastil bersama pemain berikut: ";
            for (int i = 0; i < nameP.size(); i++) {
                chat += "\n" + nameP.get(i);
            }

            Service.chatClan(p.myclan, chat);
            Service.send_notice_box(p.conn, chat);
        }
    }

    public static boolean isChangeMap(GameMap gameMap) {
        if (gameMap.mapId < 83 || gameMap.mapId > 86) {
            return true;
        }
        for (MobInMap mob : gameMap.mobs) {
            if (mob.template.mob_id == 153 && !mob.isdie) {
                return false;
            }
        }
        return true;
    }

    public static boolean isDameTruChinh(GameMap gameMap) {
        for (MobInMap mob : gameMap.mobs) {
            if (mob.template.mob_id != 152 && !mob.isdie) {
                return false;
            }
        }
        return true;
    }

    private static String NamePlayerOwner;
    private static int idIconClan;
    private static String NameClan;
    private static long timeDa;

    public static synchronized void SetOwner(Player p) {
        if (timeAttack < System.currentTimeMillis() || p.map.mapId != 87) {
            return;
        }
        List<String> l = new ArrayList<>();
        for (Player p0 : p.map.players) {
            if (p.myclan.equals(p0.myclan) && !p0.isdie) {
                l.add(p0.name);
            }
        }
        timeDa = System.currentTimeMillis() + 1000 * 60 * 10;
        NamePlayerOwner = l.get(Util.random(l.size()));
        idIconClan = p.myclan.icon;
        NameClan = p.myclan.name;

        System.out.println("Pemegang batu: " + NamePlayerOwner);
        for (Player p0 : p.map.players) {
            try {
                SenDataTime(p0.conn);
            } catch (IOException e) {
            }
        }
    }

    public static void PlayerDie(Player p) {
        if (timeAttack < System.currentTimeMillis() || !p.name.equals(NamePlayerOwner) || p.map.mapId != 87) {
            return;
        }

        NamePlayerOwner = null;
        idIconClan = -1;
        NameClan = null;
        timeDa = 0;
        truChinh.isdie = false;
        truChinh.hp = truChinh.getMaxHP();
        for (Player p0 : p.map.players) {
            try {
                SenDataTime(p0.conn);
            } catch (IOException e) {
            }
        }
    }

    public static boolean isOwner(Player p) {
        return p.map.mapId == 87 && p.name != null && p.name.equals(NamePlayerOwner);
    }

    public static void SenDataTime(Session conn) throws IOException {
        long _time = System.currentTimeMillis();
        if (NamePlayerOwner == null || NamePlayerOwner.isEmpty()) {
            Message m = new Message(-104);
            m.writer().writeByte(0);
            m.writer().writeInt(-1);
            m.writer().writeUTF("");
            conn.addmsg(m);
            m.cleanup();
            m = new Message(-104);
            m.writer().writeByte(1);
            m.writer().writeByte(1);
            m.writer().writeShort((int) ((timeAttack - _time) / 1000));
            m.writer().writeUTF("Perebutan Kastil");
            conn.addmsg(m);
            m.cleanup();
        } else {
            Message m = new Message(-104);
            m.writer().writeByte(0);
            m.writer().writeInt(idIconClan);
            m.writer().writeUTF(NameClan);
            conn.addmsg(m);
            m.cleanup();
            m = new Message(-104);
            m.writer().writeByte(1);
            m.writer().writeByte(2);
            m.writer().writeShort((int) ((timeAttack - _time) / 1000));
            m.writer().writeUTF("Perebutan Kastil");
            m.writer().writeShort((int) ((timeDa - _time) / 1000));
            m.writer().writeUTF("Waktu tersisa");
            conn.addmsg(m);
            m.cleanup();
        }
    }

    public static void EndChiemThanh() {
        System.out.println("event_daily.ChiemThanhManager.EndChiemThanh()");
        Manager.ResetCThanh();
        long time = System.currentTimeMillis();
        if (timeDa > 0 && timeDa < time && NameClan != null && !NameClan.isEmpty()) {
            Manager.nameClanThue = NameClan;
            Manager.setClanThue();
            List<String> ss = Clan_entrys.get(Manager.nameClanThue);
            if (ss != null) {
                for (String s : ss) {
                    Manager.PlayersWinCThanh.add(s);
                }
            }
        } else if (NameClan != null && !NameClan.isEmpty()) {
            Manager.nameClanThue = NameClan;
            Manager.setClanThue();
            List<String> ss = Clan_entrys.get(Manager.nameClanThue);
            if (ss != null) {
                for (String s : ss) {
                    Manager.PlayersWinCThanh.add(s);
                }
            }
        } else {
            Manager.nameClanThue = null;
        }
        if (Manager.guildThue != null) {
            Manager.guildThue.updateGold(GetVang());
            Vang = 0;
        }

        timeAttack = 0;
        Manager.thue = 5;
        CloseMap();
        try {
            Manager.gI()
                    .chatKTGprocess("Perebutan kastil telah berakhir "
                            + (NameClan != null && !NameClan.isEmpty() ? " guild " + NameClan : " tidak ada guild")
                            + " berhasil merebut kastil");
        } catch (IOException e) {
        }
    }

    public static void update() {
        if (timeAttack == 0) {
            return;
        }

        long time = System.currentTimeMillis();
        if (timeDa > 0 && timeDa < time && NameClan != null && !NameClan.isEmpty()) {
            EndChiemThanh();
        } else if (timeAttack < time) {
            EndChiemThanh();
        }
    }

    public static void SaveData(PreparedStatement ps) {
        try {
            ps.clearParameters();
            JSONArray jsarct = new JSONArray();
            try {
                for (String i : Manager.PlayersWinCThanh) {
                    jsarct.add(i);
                }
            } catch (Exception e) {
            }
            ps.setString(1, jsarct.toJSONString());
            ps.setString(2, String.valueOf(Manager.nameClanThue));
            ps.setString(3, String.valueOf("chiem_thanh"));
            ps.executeUpdate();
            ps.clearParameters();
            ps.setString(1, String.valueOf(Manager.thue));
            ps.setString(2, String.valueOf(Manager.thue));
            ps.setString(3, String.valueOf("thue"));
            ps.executeUpdate();
            ps.clearParameters();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void LoadData(ResultSet rs) {
        try {
            Manager.ResetCThanh();
            while (rs.next()) {
                String name = rs.getString("name");
                if (name.equals("thue")) {
                    if (rs.getString("data1") != null) {
                        Manager.thue = Byte.parseByte(rs.getString("data1"));
                    }
                } else if (name.equals("chiem_thanh")) {
                    if (rs.getString("data1") != null) {

                        JSONArray jsar = (JSONArray) JSONValue.parse(rs.getString("data1"));
                        if (jsar != null) {
                            for (int i = 0; i < jsar.size(); i++) {
                                try {
                                    Manager.PlayersWinCThanh.add(jsar.get(i).toString());
                                } catch (Exception ee) {
                                    ee.printStackTrace();
                                }
                            }
                        }
                    }
                    if (rs.getString("data2") != null) {
                        Manager.nameClanThue = rs.getString("data2");
                        Manager.setClanThue();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static void Obj_Die(GameMap gameMap, MainObject mainAtk, MainObject focus) throws IOException {
        if (!mainAtk.isPlayer() || !focus.isMob()) {
            return;
        }
        Player p = (Player) mainAtk;
        MobInMap mob = (MobInMap) focus;
        if (mob != null) {
            // roi do boss co dinh
            short[] id_item_leave3 = new short[]{};
            short[] id_item_leave4 = new short[]{};
            short[] id_item_leave7 = new short[]{};
            // short id_medal_material = -1;
            short sizeRandomMedal = 0;
            switch (mob.template.mob_id) {
                case 151: {
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1, 54, 53, 18};
                    id_item_leave7 = new short[]{11, 13, 2, 3, 2, 3, 14};
                    if (Util.random(100) < 10) {
                        id_item_leave3 = new short[]{(short) Util.random(4577, 4585)};
                    }
                    sizeRandomMedal = (short) (50);
                    break;
                }
                case 152: {
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1, 54, 53, 18};
                    id_item_leave7 = new short[]{11, 13, 2, 3, 2, 3, 14};
                    if (Util.random(100) < 20) {
                        id_item_leave3 = new short[]{(short) Util.random(4577, 4585)};
                    }
                    sizeRandomMedal = (short) (60);
                    break;
                }
            }
            for (short id : id_item_leave3) {
                ItemTemplate3 temp = ItemTemplate3.item.get(id);
                LeaveItemMap.leave_item_by_type3(gameMap, id, temp.getColor(), p, temp.getName(), mob.objectId);
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
        }
    }

    public static void NhanQua(Player p) throws IOException {
        if (!Manager.PlayersWinCThanh.contains(p.name)) {
            Service.send_notice_box(p.conn,
                    "Anda bukan peserta perebutan kastil, atau sudah menerima hadiah sebelumnya.");
            return;
        }
        List<box_item_template> ids = new ArrayList<>();
        String text = "Hadiah Anda: ";

        if (Manager.PlayersWinCThanh.contains(p.name)) {
            int size = Util.random(5, 15);
            for (int i = 0; i < size; i++) {
                short id = (short) Util.random(126, 146);
                ids.add(new box_item_template(id, (short) 1, (byte) 7));
            }
            size = Util.random(5);
            for (int i = 0; i < size; i++) {
                ids.add(new box_item_template((short) Util.random(205, 208), (short) Util.random(5), (byte) 4));
            }
            size = Util.random(5);
            for (int i = 0; i < size; i++) {
                ids.add(new box_item_template((short) Util.random(8, 11), (short) Util.random(5, 20), (byte) 7));
            }
        }
        if (ids.isEmpty()) {
            Service.send_notice_box(p.conn,
                    "Anda bukan peserta perebutan kastil, atau sudah menerima hadiah sebelumnya.");
        } else if (p.item.get_bag_able() < ids.size()) {
            Service.send_notice_box(p.conn,
                    "Tidak cukup " + ids.size() + " di inventory. jumlah hadiah acak dari 1 hingga 26 item.");
        } else {
            if (Manager.PlayersWinCThanh.contains(p.name)) {
                Manager.PlayersWinCThanh.remove(p.name);
            }
            for (box_item_template it : ids) {
                if (it.catagory == 4 && it.id == -2) {
                    p.updateGem(it.quantity);
                } else if (it.catagory == 4 && it.id == -1) {
                    p.updateGold(it.quantity);
                } else {
                    p.item.add_item_bag47(it.id, it.quantity, it.catagory);
                }
            }
            Service.Show_open_box_notice_item(p, text, ids);
        }
    }
}
