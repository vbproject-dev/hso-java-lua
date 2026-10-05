package core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import game.items.ItemManager;
import game.map.*;
import game.pet.PetManager;
import lombok.extern.slf4j.Slf4j;
import game.quest.QuestManager;
import game.shop.TokenShop;
import ai.Clone;
import game.event.Event_1;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import game.event.GameEventManager;
import event_daily.*;
import client.io.Session;
import model.ServerSetting;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import game.guild.Guild;
import client.Player;
import ev_he.Event_2;
import ev_he.Event_3;

import game.gamble.VXKC2;
import game.gamble.GoldSpine;
import client.io.Message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.simple.JSONObject;
import game.rewards.DonationPack;
import template.*;
import utils.SQLHelper;

@Slf4j
public class Manager {

    public int size_mob;
    private static Manager instance;
    public final HashMap<String, Integer> ip_create_char = new HashMap<>();
    public final HashMap<String, Long> time_login_client = new HashMap<>();

    public final byte[] msg_29_chienbinh;
    public final byte[] msg_29_satthu;
    public final byte[] msg_29_phapsu;
    public final byte[] msg_29_xathu;
    public final byte[] msg_1;
    // public final byte[] msg_61;

    public final byte[] msg_eff_70;
    public final byte[] msg_eff_71;
    public final byte[] msg_eff_109;
    public final byte[] msg_eff_105;
    public boolean debug;
    public String mysql_host;
    public String mysql_database;
    public String mysql_user;
    public String mysql_pass;
    public int mysql_port;

    public int web_port;

    public boolean isServerAdmin;
    public int event;

    // hso.conf sett event
    public boolean event_world_boss = true;
    public boolean event_tambang = true;
    public boolean event_premium_boss = true;
    public boolean event_btf = true;
    public boolean event_gold_spin = true;
    public boolean event_gem_spin = true;
    public boolean event_christmas = true;
    public boolean event_halloween = true;
    public boolean event_malam_suro_ambush = true;
    public boolean event_gerbang_arwah = true;
    public boolean event_kemerdekaan = true;
    public boolean event_persimpangan_kematian = true;
    public boolean event_kemerdekaan_gems_pagi = true;

    public int server_port;
    public byte indexRes;
    public int indexCharPar;
    public int exp;
    public int lvmax;
    public int allow_ip_client;
    public int time_login;
    public List<ItemSell3[]> itemsellTB;
    public short[] itempoitionsell;
    public short[] item7sell;
    public short[] itemMisteriBoxSell; // item4 ids sold at NPC menu_top "Misteri Box" shop, editable via table `itemsell` (id=19)
    public GoldSpine vxmm;
    public VXKC2 vxkc;
    public int size_mob_now = -20;
    public DailyMine mine;
    public List<Clone> cloneList = new ArrayList<>();
    public static boolean isLockVX = false;
    public static boolean isTrade = true;

    public static boolean isKmb = true;
    public static boolean isServerTest;
    public static boolean BuffAdmin = true;
    public static boolean BuffAdminMaterial = true;

    /** Set false untuk menonaktifkan sementara fitur pembuatan & upgrade item bintang (stary) */
    public static boolean isCreateItemStarEnabled = false;
    public static int timeRemoveClient = 1000 * 60;
    public static boolean logErrorLogin = false;


    public static byte thue = 5;
    public static String nameClanThue;
    public static Guild guildThue;
    public static final List<String> PlayersWinCThanh = new ArrayList<>();
    public static final List<Notification> notifications = new ArrayList<>();
    public static SvConfig svConfig;
    public ServerSetting setting;

    public List<Skill> skillList = new ArrayList<>();

    public static void setClanThue() {
        if (nameClanThue == null || nameClanThue.isEmpty()) {
            ResetCThanh();
            return;
        }
        for (Guild c : Guild.entrys) {
            if (c.name.equals(nameClanThue)) {
                guildThue = c;
                return;
            }
        }

        if (guildThue == null) {
            ResetCThanh();
        }
    }

    public static void ResetCThanh() {
        PlayersWinCThanh.clear();
        nameClanThue = null;
        thue = 5;
        guildThue = null;
    }


    public static float ratio_hp = 1;

    // Configurable drop rates
    public static int drop_rate_equipment = 80; // out of 300 (10%)
    public static int drop_rate_potions = 80; // out of 300 (16.67%)
    public static int drop_rate_materials = 80; // out of 300 (16.67%)
    public static int drop_rate_gold = 80; // out of 300 (26.67%)
    public static int drop_rate_event = 80; // out of 100 (30%)
    public static int drop_level_difference = 20; // max level difference for drops
    public static int exp_level_difference = 20; // max level difference for full EXP
    // NOTE: sudah nggak dipakai buat ngitung exp (dulu dobel-kali sama
    // Manager.gI().exp di Player.updateExp() -> exp kill mob jadi kegedean
    // waktu admin ubah rate lewat menu "Setting Exp"). Rate exp sekarang
    // cuma dikontrol via satu tempat: Manager.gI().exp (skala persen — 100 =
    // x1 normal, 200 = x2, dst — di-set lewat menu admin "Setting Exp" atau
    // command teks). Field ini dibiarin ada biar config lama yang masih
    // nyantumin "exp_multiplier" nggak error pas di-parse, tapi nilainya
    // sekarang nggak dipakai.
    public static int exp_multiplier = 100; // EXP rate multiplier (100 = normal, 200 = 2x) — DEPRECATED, unused
    public static int exp_bonus_per_level = 10; // bonus EXP % per level higher (10 = 10% per level)
    public static int exp_penalty_per_level = 5; // penalty EXP % per level lower (5 = 5% per level)
    public static int exp_boost_potion = 5000; // EXP boost from potions (5000 = 5000%)


    public Manager() throws IOException {

        this.msg_29_chienbinh = Util.loadfile("data/msg/msg_29_0");
        this.msg_29_satthu = Util.loadfile("data/msg/msg_29_1");
        this.msg_29_phapsu = Util.loadfile("data/msg/msg_29_2");
        this.msg_29_xathu = Util.loadfile("data/msg/msg_29_3");
        this.msg_1 = Util.loadfile("data/msg/item_map");
        // this.msg_61 = Util.loadfile("data/msg/msg_61");
        // this.msg_26 = Util.loadfile("data/msg/msg_26");

        this.msg_eff_70 = Util.loadfile("data/msg_eff/70");
        this.msg_eff_71 = Util.loadfile("data/msg_eff/71");
        this.msg_eff_109 = Util.loadfile("data/msg_eff/109");
        this.msg_eff_105 = Util.loadfile("data/msg_eff/105");


        // PartDataLoader.loadAllVariants();
        // IconDataLoader.loadAllIcons();

    }

    public static Manager gI() {
        if (instance == null) {
            try {
                instance = new Manager();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("create cache fail!");
                System.exit(0);
            }
        }
        return instance;
    }

    public byte[] load_msg26() throws IOException {

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        java.io.DataOutputStream ou = new DataOutputStream(os);
        ou.writeShort(MobTemplate.entrys.size());
        for (MobTemplate mob : MobTemplate.entrys) {
            ou.writeShort(mob.mob_id);
            ou.writeUTF(mob.name);
            ou.writeByte(mob.level);
            ou.writeInt(mob.hpmax);
            ou.writeByte(mob.typemove);
        }

        SvConfig sv = Manager.svConfig;
        if (sv != null) {

            // PREVIOUS FRAME
            byte[][] prevFrame = sv.getPreviousFrame();
            ou.writeByte(prevFrame.length);
            for (byte[] bytes : prevFrame) {
                ou.writeByte(bytes.length);
                for (byte aByte : bytes) {
                    ou.writeByte(aByte);
                }
            }

            // CHAR STAND FRAME
            byte[][] dyCharStand = sv.getDyCharStand();
            ou.writeByte(dyCharStand.length);
            for (byte[] bytes : dyCharStand) {
                ou.writeByte(bytes.length);
                for (byte aByte : bytes) {
                    ou.writeByte(aByte);
                }
            }

            // CHAR MOVE FRAME
            byte[][] dyCharMove = sv.getDyCharMove();
            ou.writeByte(dyCharMove.length);
            for (byte[] bytes : dyCharMove) {
                ou.writeByte(bytes.length);
                for (byte aByte : bytes) {
                    ou.writeByte(aByte);
                }
            }

            // DX
            byte[][] dx = sv.getDx();
            ou.writeByte(dx.length);
            for (byte[] bytes : dx) {
                ou.writeByte(bytes.length);
                for (byte aByte : bytes) {
                    ou.writeByte(aByte);
                }
            }

            // DY
            byte[][] dy = sv.getDy();
            ou.writeByte(dy.length);
            for (byte[] bytes : dy) {
                ou.writeByte(bytes.length);
                for (byte aByte : bytes) {
                    ou.writeByte(aByte);
                }
            }

            // MOVE LEFT AND RIGHT
            byte[][] moveLr = sv.getMoveFramesLr();
            ou.writeByte(moveLr.length);
            for (byte[] bytes : moveLr) {
                ou.writeByte(bytes.length);
                for (byte aByte : bytes) {
                    ou.writeByte(aByte);
                }
            }

            // MOVE DOWN
            byte[][] moveDown = sv.getMoveFramesDown();
            ou.writeByte(moveDown.length);
            for (byte[] bytes : moveDown) {
                ou.writeByte(bytes.length);
                for (byte aByte : bytes) {
                    ou.writeByte(aByte);
                }
            }

            // MOVE UP
            byte[][] moveUp = sv.getMoveFramesUp();
            ou.writeByte(moveUp.length);
            for (byte[] bytes : moveUp) {
                ou.writeByte(bytes.length);
                for (byte aByte : bytes) {
                    ou.writeByte(aByte);
                }
            }

            // SPEED
            byte[] speed = sv.getSpeed();
            ou.writeByte(speed.length);
            for (byte spd : speed) {
                ou.writeByte(spd);
            }

            // ATB CANNOT PAINT
            byte[] atb = sv.getAtbCantPaint();
            ou.writeByte(atb.length);
            for (byte at : atb) {
                ou.writeByte(at);
            }

            // TYPE MOVE
            byte[] typeMove = sv.getPetTypeMove();
            ou.writeByte(typeMove.length);
            for (byte val : typeMove) {
                ou.writeByte(val);
            }

            // EQUIP TEM
            byte[] itemEquip = sv.getItemEquip();
            ou.writeByte(itemEquip.length);
            for (byte val : itemEquip) {
                ou.writeByte(val);
            }

            // EQUIP TEM ROTATE
            byte[] itemEquipRotate = sv.getItemEquipRotate();
            ou.writeByte(itemEquipRotate.length);
            for (byte val : itemEquipRotate) {
                ou.writeByte(val);
            }

            // ID NEW BOSS
            short[] idBoss = sv.getIdBoss();
            ou.writeByte(idBoss.length);
            for (short val : idBoss) {
                ou.writeShort(val);
            }

            // HAIR NO HAT
            byte[] hairNoHat = sv.getHairNoHat();
            ou.writeByte(hairNoHat.length);
            for (byte val : hairNoHat) {
                ou.writeByte(val);
            }

            // DATA EFFECT
            List<DataEffect> dataEffects = sv.getDataEffects();
            ou.writeByte(dataEffects.size());
            for (DataEffect eff : dataEffects) {
                ou.writeByte(eff.getId());
                byte[] data = eff.getData();
                ou.writeShort(data.length);
                for (byte val : data) {
                    ou.writeByte(val);
                }
            }

            // EFFECT SKILL
            byte[][] effectSkill = sv.getEffectSkill();
            ou.writeShort(effectSkill.length);
            for (byte[] bytes : effectSkill) {
                ou.writeByte(bytes.length);
                for (byte aByte : bytes) {
                    ou.writeByte(aByte);
                }
            }

            // FLY MOUNT
            byte[] flyMount = sv.getFlyMount();
            ou.writeByte(flyMount.length);
            for (byte val : flyMount) {
                ou.writeByte(val);
            }
        }

        return os.toByteArray();
    }

    public void init() {
        try {
            load_config();
            if (!load_database()) {
                System.err.println("load database err");
                System.exit(0);
                return;
            }
            MapManager.getInstance().loadMapData();
            (mine = new DailyMine()).init();



            GameEventManager.gI().loadGlobalEvent();
            GameEventManager.gI().createEvent();
            // FIX: load premium boss dari tabel boss_premium
            BossHDL.BossManager.loadBossEvent();
            if (!TokenShop.loadTokenShop()) {
                System.err.println("load tokenshop err");
                System.exit(0);
                return;
            }

            if (!DonationPack.loadData()) {

                System.err.println("load rewards failed");
                return;
            }
            loadNotif();
            loadSkillData();

            System.out.println("cache loaded!");
            this.vxmm = new GoldSpine();
            this.vxkc = new VXKC2();


            for (GameMap[] maps : GameMap.entrys) {
                for (GameMap map : maps) {
                    if (map == null) continue;
                    map.start_map();
                }
            }

            Log.gI().start_log();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("load database err");
            System.exit(0);
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("load database err");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("load database err");
            System.exit(0);
        }
    }

    private boolean load_database() throws SQLException {
        // load item3
        Connection conn = SQL.gI().getConnection();
        Statement ps = conn.createStatement();
        ResultSet rs;
        String query = "SELECT * FROM `equipment`;";
        rs = ps.executeQuery(query);
        while (rs.next()) {
            ItemTemplate3 temp = new ItemTemplate3();
            temp.setId(rs.getShort("id"));
            temp.setName(rs.getString("name"));
            temp.setType(rs.getByte("type"));
            temp.setPart(rs.getInt("part"));
            temp.setClazz(rs.getByte("clazz"));
            temp.setIcon(rs.getShort("iconid"));
            temp.setLevel(rs.getShort("level"));
            temp.setColor(rs.getByte("color"));
            JSONArray jsar = (JSONArray) JSONValue.parse(rs.getString("data"));
            if (jsar == null) {
                return false;
            }
            List<Option> buffer = new ArrayList<>();
            for (int i = 0; i < jsar.size(); i++) {
                JSONArray jsar2 = (JSONArray) JSONValue.parse(jsar.get(i).toString());
                if (jsar2 == null) {
                    return false;
                }
                int id = Integer.parseInt(jsar2.get(0).toString());
                int val = Integer.parseInt(jsar2.get(1).toString());
                if (id > Byte.MAX_VALUE) {
                    System.out.println("SKIP ID " + temp.getId() + " OPID " + id);
                    continue;
                }

                buffer.add(new Option(Byte.parseByte(jsar2.get(0).toString()),
                        Integer.parseInt(jsar2.get(1).toString()), (short) 0));
            }
            temp.setOp(buffer);
            // set type leave
            if (temp.getType() < 12 && temp.getType() != 7) {
                // load item leave
                switch (temp.getLevel()) {
                    case 1: {
                        LeaveItemMap.item0x.add(temp.getId());
                        break;
                    }
                    case 10: {
                        LeaveItemMap.item1x.add(temp.getId());
                        break;
                    }
                    case 20: {
                        LeaveItemMap.item2x.add(temp.getId());
                        break;
                    }
                    case 30: {
                        LeaveItemMap.item3x.add(temp.getId());
                        break;
                    }
                    case 40: {
                        LeaveItemMap.item4x.add(temp.getId());
                        break;
                    }
                    case 50: {
                        LeaveItemMap.item5x.add(temp.getId());
                        break;
                    }
                    case 60: {
                        LeaveItemMap.item6x.add(temp.getId());
                        break;
                    }
                    case 70: {
                        LeaveItemMap.item7x.add(temp.getId());
                        break;
                    }
                    case 80: {
                        LeaveItemMap.item8x.add(temp.getId());
                        break;
                    }
                    case 90: {
                        LeaveItemMap.item9x.add(temp.getId());
                        break;
                    }
                    case 100: {
                        LeaveItemMap.item10x.add(temp.getId());
                        break;
                    }
                    case 110: {
                        LeaveItemMap.item11x.add(temp.getId());
                        break;
                    }
                    case 120: {
                        LeaveItemMap.item12x.add(temp.getId());
                        break;
                    }
                    case 130: {
                        LeaveItemMap.item13x.add(temp.getId());
                        break;
                    }
                }
            }
            if (temp.getColor() == 2) {
                temp.setLevel((short) (temp.getLevel() + 2));
            } else if (temp.getColor() == 3) {
                temp.setLevel((short) (temp.getLevel() + 4));
            } else if (temp.getColor() == 4) {
                temp.setLevel((short) (temp.getLevel() + 5));
            } else if (temp.getColor() == 5) {
                temp.setLevel((short) (temp.getLevel() + 8));
            }
            ItemTemplate3.item.add(temp);
        }
        rs.close();

        // load item4
        query = "SELECT * FROM `item4`;";
        rs = ps.executeQuery(query);
        while (rs.next()) {
            ItemTemplate4 temp = new ItemTemplate4();
            temp.setId(rs.getShort("id"));
            temp.setIcon(rs.getShort("icon"));
            if (temp.getId() >= 113 && temp.getId() <= 116) {
                temp.setPrice(50);
            } else {
                temp.setPrice(rs.getLong("price"));
            }

            temp.setName(rs.getString("name"));
            temp.setContent(rs.getString("content"));
            temp.setType(rs.getByte("typepotion"));
            temp.setPricetype(rs.getByte("moneytype"));
            temp.setSell(rs.getByte("sell"));
            temp.setValue(rs.getInt("value"));
            temp.setTrade(rs.getByte("canTrade"));
            ItemTemplate4.item.add(temp);
        }
        rs.close();

        query = "SELECT * FROM `mconfig`;";
        rs = ps.executeQuery(query);
        while (rs.next()) {
            if (rs.getString("name").equals("name_server")) {
                core.infoServer.NameServer = rs.getString("data");
            }
            if (rs.getString("name").equals("site_server")) {
                core.infoServer.Website = rs.getString("data");
            }
        }
        rs.close();
        // load item7
        query = "SELECT * FROM `item7`;";
        rs = ps.executeQuery(query);
        while (rs.next()) {
            ItemTemplate7 temp = new ItemTemplate7();
            temp.setId(rs.getShort("id"));
            temp.setIcon(rs.getShort("imgid"));
            temp.setPrice(rs.getLong("price"));
            temp.setName(rs.getString("name"));
            temp.setContent(rs.getString("content"));
            temp.setType(rs.getByte("type"));
            temp.setPricetype(rs.getByte("pricetype"));
            temp.setSell(rs.getByte("sell"));
            temp.setValue(rs.getShort("value"));
            temp.setTrade(rs.getByte("trade"));
            temp.setColor(rs.getByte("setcolorname"));
            ItemTemplate7.item.add(temp);
        }
        // load item medal
        for (int i = 0; i < 10; i++) {
            Medal_Material.m_blue[i] = (short) (i + 116);
            Medal_Material.m_yellow[i] = (short) (i + 126);
            Medal_Material.m_violet[i] = (short) (i + 136);
        }
        Medal_Material.m_white = new short[7][];
        int dem = 46;
        for (int i = 0; i < 7; i++) {
            Medal_Material.m_white[i] = new short[10];
            for (int j = 0; j < 10; j++) {
                Medal_Material.m_white[i][j] = (short) (dem++);
            }
        }
        //
        rs.close();
        // load item option
        query = "SELECT * FROM `itemoption`;";
        rs = ps.executeQuery(query);
        while (rs.next()) {
            OptionItem temp = new OptionItem();
            temp.setName(rs.getString("name"));
            temp.setColor(rs.getByte("colorInfoItem"));
            temp.setIspercent(rs.getByte("isPercentInfoItem"));
            OptionItem.entrys.add(temp);
        }
        rs.close();
        // load item sell
        query = "SELECT * FROM `itemsell`;";
        rs = ps.executeQuery(query);
        itemsellTB = new ArrayList<>();
        while (rs.next()) {
            byte type = rs.getByte("id");
            JSONArray jsar = (JSONArray) JSONValue.parse(rs.getString("data"));
            if (jsar == null) {
                return false;
            }
            switch (type) {
                case 0: {
                    switch (this.event) {
                        case 1: {
                            itempoitionsell = new short[jsar.size() + 4];
                            for (int i = 0; i < jsar.size(); i++) {
                                itempoitionsell[i] = Short.parseShort(jsar.get(i).toString());
                            }
                            itempoitionsell[itempoitionsell.length - 4] = 113;
                            itempoitionsell[itempoitionsell.length - 3] = 114;
                            itempoitionsell[itempoitionsell.length - 2] = 115;
                            itempoitionsell[itempoitionsell.length - 1] = 116;
                            break;
                        }
                        case 2: {
                            itempoitionsell = new short[jsar.size() + 3];
                            for (int i = 0; i < jsar.size(); i++) {
                                itempoitionsell[i] = Short.parseShort(jsar.get(i).toString());
                            }
                            itempoitionsell[itempoitionsell.length - 3] = 253;
                            itempoitionsell[itempoitionsell.length - 2] = 252;
                            itempoitionsell[itempoitionsell.length - 1] = 141;
                            break;
                        }
                        case 3: {
                            itempoitionsell = new short[jsar.size() + 1];
                            for (int i = 0; i < jsar.size(); i++) {
                                itempoitionsell[i] = Short.parseShort(jsar.get(i).toString());
                            }
                            itempoitionsell[itempoitionsell.length - 1] = 303;
                            break;
                        }
                        default: {
                            itempoitionsell = new short[jsar.size()];
                            for (int i = 0; i < itempoitionsell.length; i++) {
                                itempoitionsell[i] = Short.parseShort(jsar.get(i).toString());
                            }
                            break;
                        }
                    }

                    break;
                }
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16: {
                    int size = jsar.size();
                    if (Manager.gI().event == 2 && type >= 1 && type <= 4) {
                        size += 6;
                    } else if (Manager.gI().event == 2 && type >= 5 && type <= 8) {
                        size += 4;
                    }
                    ItemSell3[] itemsell3 = new ItemSell3[size];
                    for (int i = 0; i < jsar.size(); i++) {
                        itemsell3[i] = new ItemSell3();
                        JSONArray jsar2 = (JSONArray) JSONValue.parse(jsar.get(i).toString());
                        itemsell3[i].id = Short.parseShort(jsar2.get(0).toString());
                        itemsell3[i].clazz = Byte.parseByte(jsar2.get(1).toString());
                        itemsell3[i].type = Byte.parseByte(jsar2.get(2).toString());
                        itemsell3[i].price = Long.parseLong(jsar2.get(3).toString());
                        itemsell3[i].level = Short.parseShort(jsar2.get(4).toString());
                        itemsell3[i].color = Byte.parseByte(jsar2.get(5).toString());
                        itemsell3[i].option = new ArrayList<>();
                        JSONArray jsar3 = (JSONArray) JSONValue.parse(jsar2.get(6).toString());
                        for (int j = 0; j < jsar3.size(); j++) {
                            JSONArray jsar4 = (JSONArray) JSONValue.parse(jsar3.get(j).toString());
                            itemsell3[i].option.add(new Option(Byte.parseByte(jsar4.get(0).toString()),
                                    Integer.parseInt(jsar4.get(1).toString()), itemsell3[i].id));
                        }
                        itemsell3[i].pricetype = Byte.parseByte(jsar2.get(7).toString());
                    }
                    if (Manager.gI().event == 2 && type >= 1 && type <= 4) {
                        short[] id_ = new short[]{4714, 4715, 4769, 4770, 4771, 4772};
                        for (int i = 0; i < id_.length; i++) {
                            ItemTemplate3 temp = ItemTemplate3.item.get(id_[i]);
                            itemsell3[i + jsar.size()] = new ItemSell3();
                            itemsell3[i + jsar.size()].id = id_[i];
                            itemsell3[i + jsar.size()].clazz = temp.getClazz();
                            itemsell3[i + jsar.size()].type = temp.getType();
                            itemsell3[i + jsar.size()].price = 50;
                            itemsell3[i + jsar.size()].level = temp.getLevel();
                            itemsell3[i + jsar.size()].color = temp.getColor();
                            itemsell3[i + jsar.size()].option = new ArrayList<>();
                            itemsell3[i + jsar.size()].option.addAll(temp.getOp());
                            itemsell3[i + jsar.size()].pricetype = 1;
                        }
                    } else if (Manager.gI().event == 2 && type >= 5 && type <= 8) {
                        short[] id_ = new short[]{4716, 4717, 4718, 4719};
                        for (int i = 0; i < id_.length; i++) {
                            ItemTemplate3 temp = ItemTemplate3.item.get(id_[i]);
                            itemsell3[i + jsar.size()] = new ItemSell3();
                            itemsell3[i + jsar.size()].id = id_[i];
                            itemsell3[i + jsar.size()].clazz = temp.getClazz();
                            itemsell3[i + jsar.size()].type = temp.getType();
                            itemsell3[i + jsar.size()].price = 50;
                            itemsell3[i + jsar.size()].level = temp.getLevel();
                            itemsell3[i + jsar.size()].color = temp.getColor();
                            itemsell3[i + jsar.size()].option = new ArrayList<>();
                            itemsell3[i + jsar.size()].option.addAll(temp.getOp());
                            itemsell3[i + jsar.size()].pricetype = 1;
                        }
                    }

                    itemsellTB.add(itemsell3);
                    break;
                }
                case 17: {
                    item7sell = new short[jsar.size()];
                    for (int i = 0; i < item7sell.length; i++) {
                        item7sell[i] = Short.parseShort(jsar.get(i).toString());
                    }
                    break;
                }
                case 19: { // Misteri Box shop (item4), NPC menu_top -> "Misteri Box"
                    itemMisteriBoxSell = new short[jsar.size()];
                    for (int i = 0; i < itemMisteriBoxSell.length; i++) {
                        itemMisteriBoxSell[i] = Short.parseShort(jsar.get(i).toString());
                    }
                    break;
                }
            }
        }
        if (itemMisteriBoxSell == null) {
            itemMisteriBoxSell = new short[0];
        }
        rs.close();
        //
        System.out.println("item loaded!");
        // load mob temp
        query = "SELECT * FROM `monster_template`;";
        rs = ps.executeQuery(query);
        while (rs.next()) {
            MobTemplate temp = new MobTemplate();
            temp.mob_id = Short.parseShort(rs.getString("id"));
            temp.name = rs.getString("name");
            temp.level = Short.parseShort(rs.getString("level"));
            temp.hpmax = Integer.parseInt(rs.getString("hp"));
            // Baca kolom dame dari DB — jika 0 atau null, fallback ke level*500
            try {
                int dameDB = rs.getInt("dame");
                temp.damage = (dameDB > 0) ? dameDB : temp.level * 500;
            } catch (Exception e) {
                temp.damage = temp.level * 500; // kolom dame belum ada di DB
            }
            temp.typemove = Byte.parseByte(rs.getString("typeMove"));
            // Baca kolom is_boss dari DB (fallback ke false kalau kolomnya belum ada,
            // supaya tidak bikin load monster_template gagal / server crash saat start).
            try {
                temp.is_boss = rs.getBoolean("is_boss");
            } catch (Exception e) {
                temp.is_boss = false; // kolom is_boss belum ada di DB
            }
            MobTemplate.entrys.add(temp);
        }
        rs.close();

        // load map drop config (per-map drop dari database)
        query = "SELECT * FROM `map_drop_config` WHERE `active` = 1;";
        rs = ps.executeQuery(query);
        while (rs.next()) {
            model.map.MapDropConfig cfg = new model.map.MapDropConfig();
            cfg.setId(rs.getInt("id"));
            cfg.setMapId(rs.getByte("map_id"));
            cfg.setItemId(rs.getInt("item_id"));
            cfg.setItemType(rs.getByte("item_type"));
            cfg.setDropChance(rs.getInt("drop_chance"));
            cfg.setMinQuantity(rs.getInt("min_quantity"));
            cfg.setMaxQuantity(rs.getInt("max_quantity"));
            cfg.setColor(rs.getByte("color"));
            game.map.LeaveItemMap.mapDropConfig
                    .computeIfAbsent(cfg.getMapId(), k -> new java.util.ArrayList<>())
                    .add(cfg);
        }
        rs.close();
        System.out.println("Map drop config loaded: " + game.map.LeaveItemMap.mapDropConfig.size() + " maps configured");

        // load effmap config (efek visual map dari database, dulu hardcoded di GameMap.sendMapData)
        query = "SELECT * FROM `effmap_config` WHERE `active` = 1;";
        rs = ps.executeQuery(query);
        while (rs.next()) {
            model.map.EffMapConfig cfg = new model.map.EffMapConfig();
            cfg.setId(rs.getInt("id"));
            cfg.setMapId(rs.getInt("map_id"));
            cfg.setIdnpc(rs.getInt("idnpc"));
            cfg.setIdEff(rs.getInt("id_eff"));
            cfg.setX(rs.getInt("x"));
            cfg.setY(rs.getInt("y"));
            cfg.setB3(rs.getInt("b3"));
            cfg.setB4(rs.getInt("b4"));
            cfg.setB7(rs.getInt("b7"));
            game.map.GameMap.effMapConfig
                    .computeIfAbsent(cfg.getMapId(), k -> new java.util.ArrayList<>())
                    .add(cfg);
        }
        rs.close();
        System.out.println("Effmap config loaded: " + game.map.GameMap.effMapConfig.size() + " map groups configured");

        // load map
//        query = "SELECT * FROM `maps`;";
//        rs = ps.executeQuery(query);
//        int index_mob = 0;
//        while (rs.next()) {
//            byte maxzone = rs.getByte("maxzone");
//            GameMap[] temp_all_zone = new GameMap[maxzone + 1];
//            byte map_id = rs.getByte("id");
//            String name = rs.getString("name");
//            //
//            List<Vgo> vgo_temp = new ArrayList<>();
//            JSONArray jsar = (JSONArray) JSONValue.parse(rs.getString("vgos"));
//            if (jsar == null) {
//                return false;
//            }
//            for (Object object : jsar) {
//                JSONArray jsar2 = (JSONArray) JSONValue.parse(object.toString());
//                Vgo vgo = new Vgo();
//                vgo.toMap = Byte.parseByte(jsar2.get(0).toString());
//                vgo.x = Short.parseShort(jsar2.get(1).toString());
//                vgo.y = Short.parseShort(jsar2.get(2).toString());
//                vgo.name = jsar2.get(3).toString();
//                vgo.toX = Short.parseShort(jsar2.get(4).toString());
//                vgo.toY = Short.parseShort(jsar2.get(5).toString());
//                vgo_temp.add(vgo);
//            }
//            jsar.clear();
//            //
//            jsar = (JSONArray) JSONValue.parse(rs.getString("npcs"));
//            if (jsar == null) {
//                return false;
//            }
//            String[] name_npc = new String[jsar.size()];
//            for (int i = 0; i < jsar.size(); i++) {
//                name_npc[i] = jsar.get(i).toString();
//            }
//            jsar.clear();
//            //
//            jsar = (JSONArray) JSONValue.parse(rs.getString("mobs"));
//            if (jsar == null) {
//                return false;
//            }
//            List<MobInMap> mob_in_map = new ArrayList<>();
//            for (Object o : jsar) {
//                JSONArray jsar2 = (JSONArray) JSONValue.parse(o.toString());
//                MobInMap mob = new MobInMap();
//                short id = Short.parseShort(jsar2.get(0).toString());
//                if (id == 101 || id == 84 || id == 83 || id == 103 || id == 104 || id == 105 || id == 106 || id == 149
//                        || id == 155 || id == 173 || id == 195 || id == 196 || id == 197
//                        || id == 186 || id == 187 || id == 188) {
//                    continue;
//                }
//                mob.template = MobTemplate.entrys.get(id);
//                mob.x = Short.parseShort(jsar2.get(1).toString());
//                mob.y = Short.parseShort(jsar2.get(2).toString());
//                mob.level = mob.template.level;
//                mob.map_id = map_id;
//                mob.isdie = false;
//                mob.time_back = System.currentTimeMillis() + 4_000L;
//                mob.color_name = 0;
//                mob.setBoss(false);
//                mob.setHpMax(MobTemplate.entrys.get(id).hpmax);
//                mob.hp = mob.getMaxHP();
//                // mob.dame = mob.level * 75;
//                mob_in_map.add(mob);
//            }
//            jsar.clear();
//            //
//            byte typemap = rs.getByte("type");
//            byte maxplayer = rs.getByte("maxplayer");
//            boolean ismaplang = rs.getByte("ismaplang") == 1;
//            boolean showhs = rs.getByte("showhs") == 1;
//            for (int i = 0; i < maxzone + 1; i++) {
//                GameMap m = null;
//                try {
//                    m = new GameMap(map_id, i, name_npc, name, typemap, ismaplang, showhs, maxplayer, maxzone, vgo_temp);
//                } catch (IOException e) {
//                    System.err.println("load data map err " + map_id);
//                    e.printStackTrace();
//                    System.exit(0);
//                }
//                //
//                if (i < maxzone) {
//                    m.mobs = new MobInMap[mob_in_map.size()];
//                    int idxmob = 1;
//                    for (int i1 = 0; i1 < mob_in_map.size(); i1++) {
//                        MobInMap mob = new MobInMap();
//                        mob.index = idxmob++;
//                        index_mob++;
//                        mob.template = mob_in_map.get(i1).template;
//                        mob.x = mob_in_map.get(i1).x;
//                        mob.y = mob_in_map.get(i1).y;
//                        mob.setHpMax(mob_in_map.get(i1).hp);
//                        mob.hp = mob.getMaxHP();
//                        mob.level = mob_in_map.get(i1).level;
//                        mob.map_id = map_id;
//                        mob.zone_id = (byte) i;
//                        mob.isdie = mob_in_map.get(i1).isdie;
//                        mob.color_name = mob_in_map.get(i1).color_name;
//                        mob.setBoss(mob_in_map.get(i1).isBoss());
//                        mob.time_back = mob_in_map.get(i1).time_back;
//                        mob.is_boss_active = false;
//                        m.mobs[i1] = mob;
//                        if (mob.isBoss()) {
//                            if (GameMap.isBattlefieldMap(m.mapId)) {
//                                Battlefield.gI().boss.add(mob);
//                                mob.level = 5;
//                            }
//                        }
//                    }
//                } else {
//                    m.mobs = new MobInMap[0];
//                }
//                //
//                temp_all_zone[i] = m;
//            }
//            GameMap.entrys.add(temp_all_zone);
//        }
//        rs.close();
//        this.size_mob = index_mob;
//        //
//        System.out.println("map loaded, mob size " + (this.size_mob));
//        // load level

        query = "SELECT * FROM `level`;";
        rs = ps.executeQuery(query);
        while (rs.next()) {
            Level temp2 = new Level();
            temp2.level = rs.getShort("level");
            temp2.exp = rs.getLong("exp");
            temp2.tiemnang = rs.getShort("tiemnang");
            temp2.kynang = rs.getShort("kynang");
            Level.entrys.add(temp2);
        }
        if (lvmax > Level.entrys.size()) {
            for (int i = Level.entrys.size() + 1; i < lvmax + 2; i++) {
                Level temp2 = new Level();
                temp2.level = (short) i;
                temp2.exp = Level.entrys.get(i - 2).exp;
                temp2.tiemnang = 5;
                temp2.kynang = 1;
                Level.entrys.add(temp2);
            }
        }
        rs.close();
        // load part fashion temp
        query = "SELECT * FROM `fashiontemplate`;";
        rs = ps.executeQuery(query);
        while (rs.next()) {
            PartFashion temp = new PartFashion();
            temp.id = (short) rs.getInt("id");
            JSONArray jsar = (JSONArray) JSONValue.parse(rs.getString("part"));
            if (jsar == null) {
                return false;
            }
            temp.part = new int[jsar.size()];
            for (int i = 0; i < temp.part.length; i++) {
                temp.part[i] = Integer.parseInt(jsar.get(i).toString());
            }
            PartFashion.fashions.add(temp.id);
            PartFashion.entrys.add(temp);
        }
        System.out.println("part_fashion loaded!");
        rs.close();
        // load clan
        query = "SELECT * FROM `clan`;";
        rs = ps.executeQuery(query);
        List<Guild> guild_list = new ArrayList<>();
        while (rs.next()) {
            Guild temp = new Guild();
            temp.name = rs.getString("name");
            temp.shortName = rs.getString("name_short");
            temp.icon = rs.getShort("icon");
            temp.level = rs.getShort("level");
            temp.exp = rs.getLong("exp");
            temp.slogan = rs.getString("slogan");
            temp.rule = rs.getString("rule");
            temp.notice = rs.getString("notice");
            temp.setGold(rs.getLong("vang"));
            temp.setGems(rs.getInt("kimcuong"));
            temp.maxMember = rs.getShort("max_mem");
            temp.maxMember = Guild.get_mem_by_level(temp.level);
            //
            temp.clanItems = new ArrayList<>();
            JSONArray jsar = (JSONArray) JSONValue.parse(rs.getString("item"));
            if (jsar == null) {
                return false;
            }
            for (int i = 0; i < jsar.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(jsar.get(i).toString());
                Item47 item = new Item47();
                item.id = Short.parseShort(js2.get(0).toString());
                item.quantity = Short.parseShort(js2.get(1).toString());

                temp.clanItems.add(item);
            }

            //
            jsar.clear();
            jsar = (JSONArray) JSONValue.parse(rs.getString("mems"));
            if (jsar == null) {
                return false;
            }
            temp.members = new ArrayList<>();
            for (int i = 0; i < jsar.size(); i++) {
                JSONArray jsar2 = (JSONArray) JSONValue.parse(jsar.get(i).toString());
                ClanMember mem = new ClanMember();
                mem.name = jsar2.get(0).toString();
                mem.memberType = Byte.parseByte(jsar2.get(1).toString());
                mem.gem = Integer.parseInt(jsar2.get(2).toString());
                mem.gold = Long.parseLong(jsar2.get(3).toString());
                mem.head = Byte.parseByte(jsar2.get(4).toString());
                mem.eye = Byte.parseByte(jsar2.get(5).toString());
                mem.hair = Byte.parseByte(jsar2.get(6).toString());
                mem.level = Short.parseShort(jsar2.get(7).toString());
                mem.wearing = new ArrayList<>();
                JSONArray jsar3 = (JSONArray) JSONValue.parse(jsar2.get(8).toString());
                for (int j = 0; j < jsar3.size(); j++) {
                    JSONArray jsar4 = (JSONArray) JSONValue.parse(jsar3.get(j).toString());
                    PlayerPart part = new PlayerPart();
                    part.part = Byte.parseByte(jsar4.get(0).toString());
                    part.type = Byte.parseByte(jsar4.get(1).toString());
                    mem.wearing.add(part);
                }
                temp.members.add(mem);
            }
            temp.crystals = new ArrayList<>();
            guild_list.add(temp);
        }
        Guild.set_clan(guild_list);
        System.out.println("clan loaded!");
        rs.close();
        // load event
        if (this.event == 1) {
            query = "SELECT * FROM `event` WHERE `id` = 0;";
            rs = ps.executeQuery(query);
            long t_ = System.currentTimeMillis();
            while (rs.next()) {
                JSONObject jsob = (JSONObject) JSONValue.parse(rs.getString("data"));
                Event_1.LoadDB(jsob);
            }
        } else if (this.event == 2) {
            query = "SELECT * FROM `event` WHERE `id` = 1;";
            rs = ps.executeQuery(query);
            long t_ = System.currentTimeMillis();
            while (rs.next()) {
                JSONObject jsob = (JSONObject) JSONValue.parse(rs.getString("data"));
                Event_2.LoadDB(jsob);
            }
        } else if (this.event == 3) {
            query = "SELECT * FROM `event` WHERE `id` = 2;";
            rs = ps.executeQuery(query);
            long t_ = System.currentTimeMillis();
            while (rs.next()) {
                JSONObject jsob = (JSONObject) JSONValue.parse(rs.getString("data"));
                Event_3.LoadDB(jsob);
            }
        }

        query = "SELECT * FROM `config_server`;";
        rs = ps.executeQuery(query);
        CastleSiegeManager.LoadData(rs);
        rs.close();
        System.out.println("event loaded!");
        query = "SELECT * FROM `wedding`;";
        rs = ps.executeQuery(query);
        while (rs.next()) {
            Wedding temp_wed = new Wedding();
            temp_wed.id = rs.getInt("id"); // load id untuk gudang bersama
            JSONArray js_w = (JSONArray) JSONValue.parse(rs.getString("name"));
            temp_wed.name_1 = js_w.get(0).toString();
            temp_wed.name_2 = js_w.get(1).toString();
            js_w.clear();
            js_w = (JSONArray) JSONValue.parse(rs.getString("item"));
            temp_wed.exp = Long.parseLong(js_w.get(0).toString());
            temp_wed.it = new Item3();
            temp_wed.it.id = 0;
            temp_wed.it.name = "Cincin Pasangan " + temp_wed.name_2 + " dan " + temp_wed.name_1;
            temp_wed.it.clazz = 4;
            temp_wed.it.type = 103;
            temp_wed.it.level = 60;
            temp_wed.it.icon = 14101; // FIX BUG 1: icon cincin yang benar (bukan 13165)
            temp_wed.it.color = Byte.parseByte(js_w.get(1).toString());
            temp_wed.it.part = 0;
            temp_wed.it.tier = Byte.parseByte(js_w.get(2).toString());
            temp_wed.it.islock = true;
            temp_wed.it.op = new ArrayList<>();
            JSONArray js_op = (JSONArray) JSONValue.parse(js_w.get(3).toString());
            for (Object o : js_op) {
                JSONArray js_op_2 = (JSONArray) JSONValue.parse(o.toString());
                temp_wed.it.op.add(
                        new Option(Byte.parseByte(js_op_2.get(0).toString()),
                                Integer.parseInt(js_op_2.get(1).toString())));
            }
            temp_wed.it.time_use = 0;
            // Load gudang bersama
            String itemWedding = rs.getString("item_wedding");
            if (itemWedding != null && !itemWedding.isEmpty()) {
                temp_wed.loadChest(itemWedding);
            }
            Wedding.list.add(temp_wed);
        }

        // close all
        rs.close();
        ps.close();
        conn.close();


        // quest
        QuestManager.getInstance().loadQuestTemplate();
        DailyQuest.load(); // load daily quest templates dari DB
        ItemManager.getInstance().loadAll();
        PetManager.getInstance().load();

        svConfig = SQLHelper
                .selectFrom("sv_config")
                .where("version", "V1")
                .firstAsModel(SvConfig.class);

        setting = SQLHelper
                .selectFrom("server_setting")
                .where("version", "V1")
                .firstAsModel(ServerSetting.class);

        return true;
    }

    public void load_config() throws IOException {
        final byte[] ab = Util.loadfile("hso.conf");
        if (ab == null) {
            System.out.println("Config file not found!");
            System.exit(0);
        }
        final String data = new String(ab);
        final HashMap<String, String> configMap = new HashMap<String, String>();
        final StringBuilder sbd = new StringBuilder();
        boolean bo = false;
        for (int i = 0; i <= data.length(); ++i) {
            final char es;
            if (i == data.length() || (es = data.charAt(i)) == '\n') {
                bo = false;
                final String sbf = sbd.toString().trim();
                if (sbf != null && !sbf.equals("") && sbf.charAt(0) != '#') {
                    final int j = sbf.indexOf(58);
                    if (j > 0) {
                        final String key = sbf.substring(0, j).trim();
                        final String value = sbf.substring(j + 1).trim();
                        configMap.put(key, value);
                        System.out.println("config: " + key + ": " + value);
                    }
                }
                sbd.setLength(0);
            } else {
                if (es == '#') {
                    bo = true;
                }
                if (!bo) {
                    sbd.append(es);
                }
            }
        }

        if (configMap.containsKey("timeRemoveClient")) {
            timeRemoveClient = Integer.parseInt(configMap.get("timeRemoveClient"));
        }
        if (configMap.containsKey("port")) {
            this.server_port = Integer.parseInt(configMap.get("port"));
        } else {
            this.server_port = 19129;
        }
        if (configMap.containsKey("server_admin")) {
            this.isServerAdmin = Boolean.parseBoolean(configMap.get("server_admin"));
        } else {
            this.isServerAdmin = false;
        }

        if (configMap.containsKey("debug")) {
            this.debug = Boolean.parseBoolean(configMap.get("debug"));
        } else {
            this.debug = false;
        }
        
        this.mysql_host = configMap.getOrDefault("mysql-host", "127.0.0.1");
        this.mysql_user = configMap.getOrDefault("mysql-user", "root");
        this.mysql_pass = configMap.getOrDefault("mysql-password", "12345678");
        this.mysql_database = configMap.getOrDefault("mysql-database", "knightage");
        this.mysql_port = Integer.parseInt(configMap.getOrDefault("mysql-port", String.valueOf(3306)));
        this.web_port = Integer.parseInt(configMap.getOrDefault("web-port", String.valueOf(8081)));
        if (configMap.containsKey("indexRes")) {
            this.indexRes = Byte.parseByte(configMap.get("indexRes"));
        } else {
            this.indexRes = 60;
        }
        if (configMap.containsKey("indexCharPar")) {
            this.indexCharPar = Short.parseShort(configMap.get("indexCharPar"));
        } else {
            this.indexCharPar = -22031;
        }
        if (configMap.containsKey("exp")) {
            this.exp = Integer.parseInt(configMap.get("exp"));
        } else {
            // REVERT: exp sekarang skala persen (100 = x1 normal), bukan raw
            // multiplier lagi. Default fallback harus 100, bukan 1 — kalau 1,
            // artinya server baru tanpa config bakal jalan di rate 0.01x
            // (hampir tidak dapat exp sama sekali).
            this.exp = 100;
        }
        if (configMap.containsKey("lvmax")) {
            this.lvmax = Short.parseShort(configMap.get("lvmax"));
        } else {
            this.lvmax = 150;
        }
        if (configMap.containsKey("allow_ip_client")) {
            this.allow_ip_client = Integer.parseInt(configMap.get("allow_ip_client"));
        } else {
            this.allow_ip_client = 3;
        }
        if (configMap.containsKey("time_login")) {
            this.time_login = Integer.parseInt(configMap.get("time_login"));
        } else {
            this.time_login = 3000;
        }
        if (configMap.containsKey("event")) {
            this.event = Integer.parseInt(configMap.get("event"));
        } else {
            this.event = 0;
        }
        if (configMap.containsKey("server_test")) {
            isServerTest = Boolean.parseBoolean(configMap.get("server_test"));
        }
        if (configMap.containsKey("ratio_hp")) {
            ratio_hp = Float.parseFloat(configMap.get("ratio_hp"));
        }
        // Load configurable drop rates
        if (configMap.containsKey("drop_rate_equipment")) {
            drop_rate_equipment = Integer.parseInt(configMap.get("drop_rate_equipment"));
        }
        if (configMap.containsKey("drop_rate_potions")) {
            drop_rate_potions = Integer.parseInt(configMap.get("drop_rate_potions"));
        }
        if (configMap.containsKey("drop_rate_materials")) {
            drop_rate_materials = Integer.parseInt(configMap.get("drop_rate_materials"));
        }
        if (configMap.containsKey("drop_rate_gold")) {
            drop_rate_gold = Integer.parseInt(configMap.get("drop_rate_gold"));
        }
        if (configMap.containsKey("drop_rate_event")) {
            drop_rate_event = Integer.parseInt(configMap.get("drop_rate_event"));
        }
        if (configMap.containsKey("drop_level_difference")) {
            drop_level_difference = Integer.parseInt(configMap.get("drop_level_difference"));
        }
        if (configMap.containsKey("exp_level_difference")) {
            exp_level_difference = Integer.parseInt(configMap.get("exp_level_difference"));
        }
        if (configMap.containsKey("exp_multiplier")) {
            exp_multiplier = Integer.parseInt(configMap.get("exp_multiplier"));
        }
        if (configMap.containsKey("exp_bonus_per_level")) {
            exp_bonus_per_level = Integer.parseInt(configMap.get("exp_bonus_per_level"));
        }
        if (configMap.containsKey("exp_penalty_per_level")) {
            exp_penalty_per_level = Integer.parseInt(configMap.get("exp_penalty_per_level"));
        }
        if (configMap.containsKey("exp_boost_potion")) {
            exp_boost_potion = Integer.parseInt(configMap.get("exp_boost_potion"));
        }

        // ── FIX: key event_* di hso.conf sebelumnya tidak pernah dibaca ──
        // sehingga togel on/off tidak berpengaruh (semua event tetap true,
        // GameEventManager.createEvent() jadi selalu meregistrasikan semua
        // event terlepas dari isi hso.conf).
        if (configMap.containsKey("event_world_boss")) {
            event_world_boss = Boolean.parseBoolean(configMap.get("event_world_boss"));
        }
        if (configMap.containsKey("event_tambang")) {
            event_tambang = Boolean.parseBoolean(configMap.get("event_tambang"));
        }
        if (configMap.containsKey("event_premium_boss")) {
            event_premium_boss = Boolean.parseBoolean(configMap.get("event_premium_boss"));
        }
        if (configMap.containsKey("event_btf")) {
            event_btf = Boolean.parseBoolean(configMap.get("event_btf"));
        }
        if (configMap.containsKey("event_gold_spin")) {
            event_gold_spin = Boolean.parseBoolean(configMap.get("event_gold_spin"));
        }
        if (configMap.containsKey("event_gem_spin")) {
            event_gem_spin = Boolean.parseBoolean(configMap.get("event_gem_spin"));
        }
        if (configMap.containsKey("event_christmas")) {
            event_christmas = Boolean.parseBoolean(configMap.get("event_christmas"));
        }
        if (configMap.containsKey("event_halloween")) {
            event_halloween = Boolean.parseBoolean(configMap.get("event_halloween"));
        }
        if (configMap.containsKey("event_malam_suro_ambush")) {
            event_malam_suro_ambush = Boolean.parseBoolean(configMap.get("event_malam_suro_ambush"));
        }
        if (configMap.containsKey("event_gerbang_arwah")) {
            event_gerbang_arwah = Boolean.parseBoolean(configMap.get("event_gerbang_arwah"));
        }
        if (configMap.containsKey("event_kemerdekaan")) {
            event_kemerdekaan = Boolean.parseBoolean(configMap.get("event_kemerdekaan"));
        }
        if (configMap.containsKey("event_persimpangan_kematian")) {
            event_persimpangan_kematian = Boolean.parseBoolean(configMap.get("event_persimpangan_kematian"));
        }
        if (configMap.containsKey("event_kemerdekaan_gems_pagi")) {
            event_kemerdekaan_gems_pagi = Boolean.parseBoolean(configMap.get("event_kemerdekaan_gems_pagi"));
        }
    }

    public void chatKTGprocess(String s) throws IOException {
        Message m = new Message(53);
        m.writer().writeUTF(s);
        m.writer().writeByte(1);
        for (GameMap[] gameMap : GameMap.entrys) {
            for (GameMap gameMap0 : gameMap) {
                for (int i = 0; i < gameMap0.players.size(); i++) {
                    gameMap0.players.get(i).conn.addmsg(m);
                }
            }
        }

        m.cleanup();
    }

    public void close() {
        vxmm.close();
        Log.gI().close_log();
        //
        for (int i = 0; i < GameMap.entrys.size(); i++) {
            for (int j = 0; j < GameMap.entrys.get(i).length; j++) {
                GameMap.entrys.get(i)[j].stop_map();
            }
        }
        // instance = null;
    }

    public synchronized int get_index_mob_new() {
        if (this.size_mob_now < -5000) {
            this.size_mob_now = -20;
        }
        return (--this.size_mob_now);
    }

    public synchronized void addClone(Clone nhanban) {
        this.cloneList.add(nhanban);
    }

    public synchronized void removeClone(Clone nhanban) {
        this.cloneList.remove(nhanban);
    }

    public void reloadResources() {
        PartDataLoader.clearAll();

    }

    public static Player getPlayerByName(String name) {
        // Validate input
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String searchName = name.trim();

        // Search for player
        // FIX: SESSION_LIST adalah LinkedList yang ditulis (add/remove) di dalam
        // synchronized(SESSION_LIST) oleh thread lain. Iterasi tanpa lock bisa melempar
        // ConcurrentModificationException. Ambil salinan dulu di dalam lock.
        for (Session session : snapshotSessions()) {
            if (session == null || session.p == null) {
                continue;
            }

            Player player = session.p;

            if (player.isOnline && player.name != null &&
                    player.name.equalsIgnoreCase(searchName)) {
                return player;
            }
        }

        return null;
    }

    /** Salinan aman (thread-safe) dari Session.SESSION_LIST untuk diiterasi. */
    private static Session[] snapshotSessions() {
        synchronized (Session.SESSION_LIST) {
            return Session.SESSION_LIST.toArray(new Session[0]);
        }
    }
    public static Player getPlayerById(int id) {
        for (Session session : snapshotSessions()) {
            if (session == null || session.p == null) {
                continue;
            }

            Player player = session.p;

            if (player.isOnline && player.objectId == id) {
                return player;
            }
        }

        return null;
    }

    public boolean reloadItemTemplate3() {
        AtomicBoolean success = new AtomicBoolean(true);

        // Thread for DonationPack
        Thread donationThread = new Thread(() -> {
            try {
                DonationPack.loadData();
            } catch (Exception e) {
                e.printStackTrace();
                success.set(false);
            }
        });

        // Thread for ExchangeService
        Thread itemManagerThread = new Thread(() -> {
            try {
                ItemManager.getInstance().loadAll();
            } catch (Exception e) {
                e.printStackTrace();
                success.set(false);
            }
        });


        // Thread for loading ItemTemplate3
        Thread itemTemplateThread = new Thread(() -> {
            List<ItemTemplate3> tempItems = new ArrayList<>();
            try (Connection conn = SQL.gI().getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM `equipment`;")) {

                while (rs.next()) {
                    ItemTemplate3 temp = new ItemTemplate3();
                    temp.setId(rs.getShort("id"));
                    temp.setName(rs.getString("name"));
                    temp.setType(rs.getByte("type"));
                    temp.setPart(rs.getInt("part"));
                    temp.setClazz(rs.getByte("clazz"));
                    temp.setIcon(rs.getShort("iconid"));
                    temp.setLevel(rs.getShort("level"));
                    temp.setColor(rs.getByte("color"));

                    JSONArray jsar = (JSONArray) JSONValue.parse(rs.getString("data"));
                    if (jsar == null) {
                        success.set(false);
                        continue;
                    }

                    List<Option> buffer = new ArrayList<>();
                    for (Object o : jsar) {
                        JSONArray jsar2 = (JSONArray) JSONValue.parse(o.toString());
                        if (jsar2 == null || jsar2.size() < 2) {
                            success.set(false);
                            continue;
                        }
                        buffer.add(new Option(Byte.parseByte(jsar2.get(0).toString()),
                                Integer.parseInt(jsar2.get(1).toString()), (short) 0));
                    }
                    temp.setOp(buffer);

                    switch (temp.getColor()) {
                        case 2 -> temp.setLevel((short) (temp.getLevel() + 2));
                        case 3 -> temp.setLevel((short) (temp.getLevel() + 4));
                        case 4 -> temp.setLevel((short) (temp.getLevel() + 5));
                        case 5 -> temp.setLevel((short) (temp.getLevel() + 8));
                    }

                    tempItems.add(temp);
                }

                synchronized (ItemTemplate3.item) {
                    ItemTemplate3.item.clear();
                    ItemTemplate3.item.addAll(tempItems);
                }

            } catch (Exception e) {
                e.printStackTrace();
                success.set(false);
            }
        });

        // Start all threads
        donationThread.start();
        itemManagerThread.start();
        itemTemplateThread.start();

        // Wait for all threads to finish
        try {
            donationThread.join();
            itemManagerThread.join();
            itemTemplateThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            success.set(false);
        }

        return success.get();
    }

    private void loadNotif() {
        List<Notification> notifs = SQLHelper.selectFrom("messager")
                .get(rs -> {
                    Notification notif = new Notification();
                    notif.setId(rs.getInt("id"));
                    notif.setType(rs.getInt("type"));
                    notif.setDelay(rs.getInt("delay"));
                    notif.setContent(rs.getString("content"));
                    return notif;
                });

        notifications.clear();
        notifications.addAll(notifs);
    }

    /**
     * Hot-reload tabel `messager` (notifikasi/announcement global chat, lihat pemakaiannya
     * di ServerManager.serverTick()) dari database tanpa perlu restart server.
     * Dipanggil dari menu GM "Reload Tabel Messager" (MenuManager.adminUtilityMenu()).
     * Return jumlah baris yang berhasil dimuat, atau -1 kalau gagal (mis. koneksi DB error) -
     * pada kasus gagal, isi `notifications` yang lama TIDAK disentuh (loadNotif() sendiri baru
     * clear() list lama setelah query-nya sukses), jadi announcement lama tetap jalan seperti
     * biasa, bukan kosong/hilang.
     */
    public int reloadNotif() {
        try {
            loadNotif();
            return notifications.size();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }



    public void loadSkillData() {

        skillList = SQLHelper.selectFrom("skill").get(rs -> {
            Skill skill = new Skill();

            skill.id = rs.getByte("sid");
            skill.iconid = rs.getByte("icon_id");
            skill.name = rs.getString("name");
            skill.type = rs.getByte("type");
            skill.range = rs.getShort("attack_range");
            skill.detail = rs.getString("description");
            skill.typeBuff = rs.getByte("buff_type");
            skill.subEff = rs.getByte("sub_effect_type");
            skill.performDur = rs.getShort("perform_duration");
            skill.typePaint = rs.getByte("paint_type");
            skill.role = rs.getByte("role");
            skill.mLvSkill = parseLvSkills(rs.getString("levels"));
            return skill;
        });

        System.out.printf("Skill list %d%n", skillList.size());

    }

    private LvSkill[] parseLvSkills(String json) {
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        LvSkill[] levels = new LvSkill[array.size()];

        for (int i = 0; i < array.size(); i++) {
            JsonObject data = array.get(i).getAsJsonObject();
            LvSkill level = new LvSkill();

            level.mpLost = data.get("mpCost").getAsShort();
            level.LvRe = data.get("requiredLevel").getAsShort();
            level.delay = data.get("cooldown").getAsInt();
            level.timeBuff = data.get("buffDuration").getAsInt();
            level.per_Sub_Eff = data.get("subEffectPercent").getAsByte();
            level.time_Sub_Eff = data.get("subEffectDuration").getAsShort();
            level.plus_Hp = data.get("bonusHp").getAsShort();
            level.plus_Mp = data.get("bonusMp").getAsShort();
            level.nTarget = data.get("targetCount").getAsByte();
            level.range_lan = data.get("castRange").getAsShort();

            JsonArray options = data.getAsJsonArray("options");
            level.minfo = new Option[options.size()];

            for (int j = 0; j < options.size(); j++) {
                JsonObject option = options.get(j).getAsJsonObject();
                level.minfo[j] = new Option(option.get("id").getAsInt(), option.get("value").getAsInt());
            }

            levels[i] = level;
        }

        return levels;
    }

    public Skill[] getSkillByClazz(byte clazz) {
        // FIX: tabel `skill` di-insert dengan urutan sid menurun (20 -> 0), padahal
        // skillPoint[i] pada Player mengasumsikan index i == sid. Tanpa sorting ini,
        // skills[i] bisa merujuk ke skill yang salah (mis. skills[20] jadi basic attack
        // yang cuma punya 1 level), menyebabkan ArrayIndexOutOfBoundsException saat
        // CheckSkillPoint() dipanggil di set_in4() (login) untuk akun yang skill
        // point-nya sudah lebih dari 1 di slot tsb.
        return skillList.stream()
                .filter(skill -> skill.role == clazz)
                .sorted(Comparator.comparingInt(s -> s.id))
                .toArray(Skill[]::new);
    }
}