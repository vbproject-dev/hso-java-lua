package client;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.Date;
import java.util.Map.Entry;

import core.*;
import game.guild.Guild;
import game.items.ItemManager;
import game.quest.PlayerQuest;
import game.quest.Quest;
import game.shop.Shop;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import menu.Dialog;
import menu.InputDialog;
import menu.Menu;
import game.mount.Mount;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import event_daily.ArenaTemplate;
import event_daily.Wedding;
import client.io.Message;
import client.io.Session;
import game.map.GameMap;
import game.map.MapService;
import model.map.Vgo;
import game.quest.QuestManager;
import template.EffTemplate;
import template.Item3;
import template.Item47;
import template.ItemTemplate3;
import template.Kham_template;
import template.Level;
import template.LvSkill;
import template.Option;
import template.PartFashion;
import template.PlayerPart;
import template.Pet_di_buon;
import template.Pet_di_buon_manager;
import template.Player_store;
import template.Skill;
import utils.SQLHelper;

@Slf4j
public class Player extends Body {

    /**
     * Escape tanda kutip tunggal & backslash sebelum menyisipkan sebuah string ke
     * literal SQL yang dibangun manual lewat concatenation (bukan
     * PreparedStatement).
     * Dipakai khusus untuk kolom yang isinya bisa memuat teks dari player (kode
     * giftcode yang diketik sendiri, nama player lain di daftar musuh) -- tanpa
     * ini,
     * satu tanda kutip di dalam teks itu bisa keluar dari string literal dan
     * menyisipkan SQL sembarangan (SQL injection) ke query flush() ini.
     * Query ini sendiri sengaja tidak dirombak total jadi PreparedStatement karena
     * ukurannya sangat besar (puluhan kolom dinamis) dan berisiko salah hitung
     * placeholder tanpa bisa dikompilasi/diuji di sini; escaping titik-sisip ini
     * adalah perbaikan minimal yang menutup celahnya tanpa menyentuh sisa kolom
     * lain.
     */
    private static String sqlEscape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }

    public List<EffTemplate> list_eff = new ArrayList<>();
    public boolean isCreateOptions;
    public boolean is_nhanban;
    public final Session conn;
    // public final int index;
    public boolean already_setup;
    // public String name;
    public GameMap map;
    public boolean isChangemap;
    public long timeCantChangeMap;

    // public short x;
    // public short y;
    // public short x_old;
    // public short y_old;
    public byte head;
    public byte eye;
    public byte hair;
    // public List<EffTemplate> list_eff;
    public Date date;
    public byte diemdanh;
    public byte chucphuc;
    // public int hieuchien;
    public int dicuop;
    public long gold_dagang; // total gold diterima dari dagang
    public byte type_exp;
    // public byte clazz;
    // public short level;
    // public long exp;
    private long gold;
    private int kimcuong;
    // public boolean isdie;
    public short tiemnang;
    public short kynang;
    public short point1;
    public short point2;
    public short point3;
    public short point4;
    public int suckhoe;
    public int pointarena;
    // Poin akumulasi top up (terpisah antara emas & permata), diisi dari tabel
    // user_topup
    // lihat topup.TopupController#find
    public long poinIsiUlangEmas;
    public long poinIsiUlangPermata;
    // KingCup (Lôi Đài)
    public int point_king_cup;
    public int group_king_cup = -1;
    public byte type_reward_king_cup;
    public int bye_count_king_cup; // jumlah bye (tidak ada lawan) di King Cup
    public int countWin;
    // public byte typepk;
    public int pointpk;
    public byte[] skillPoint;
    public long[] time_delay_skill;
    public Body body;
    public byte maxbag;
    public byte maxbox;
    public Item item;
    public List<String> giftcode;
    public byte[][] rms_save;
    public List<Pet> mypet;
    public short pet_follow = -1;
    public List<Friend> list_friend;
    public List<String> list_enemies;
    // public int hp;
    // public int mp;
    public int[] fashion;
    public Skill[] skills;
    // === PART CHAR EFFECT (data/part_char/x4/img) ===
    public short bodyEffectId = -1; // effect aura tubuh (112_xx.png)
    public short legEffectId = -1; // effect aura kaki (112_xx.png)
    public byte item_color_can_pick;
    public byte hp_mp_can_pick;
    public HashMap<Integer, Boolean> other_player_inside;
    public HashMap<Integer, Boolean> other_mob_inside;
    public HashMap<Integer, Boolean> other_mob_inside_update;

    public short id_item_rebuild;
    public boolean is_use_mayman;
    public short id_use_mayman;
    public short item_replace;
    public short item_replace2;
    public short id_buffer_126;
    public byte id_index_126;
    public Party party;
    public long time_use_poition_hp;
    public long time_use_poition_mp;
    public long time_use_box;
    public byte enough_time_disconnect;
    public int dame_affect_special_sk;
    public int hp_restore;
    public long time_buff_hp;
    public long time_buff_mp;
    public long time_affect_special_sk;
    public long time_speed_rebuild;
    public String name_trade;
    public short[] list_item_trade;
    public boolean lock_trade;
    public boolean accept_trade;
    public int money_trade;
    // public Dungeon dungeon;
    public Guild myclan;
    public byte id_medal_is_created;
    public short[] medal_create_material;
    // Medal V2 — sistem duplikat terpisah dari Medal V1 di atas (id item, material,
    // dan slot sendiri)
    public byte id_medal_is_created_v2 = -1;
    public short[] medal_create_material_v2;
    // Admin-only: kalau di-set (0-5), warna Medal V2 berikutnya yang dibuat akan
    // MENGIKUTI
    // nilai ini persis (skip random roll & skip force-orange dari
    // Manager.BuffAdmin).
    // Di-reset ke -1 otomatis setelah 1x dipakai. Lihat
    // MenuManager.wizardCreateMedalV2Admin()
    // dan GameSrc case 3 (create medal).
    public byte admin_selected_color_v2 = -1;
    public short fusion_material_medal_id;
    public long pet_atk_speed;
    public long time_eff_medal;
    public long time_eff_wear;
    public long time_eff_21;
    public long time_eff_22;
    // Eff wear[22] pas nyerang (MainObject.attack): SEMUA serangan yang nyentuh
    // mob dalam 1 "batch" (window singkat, misal buat nangkep AOE) dapet eff,
    // baru abis itu cooldown GLOBAL berlaku sampai batch berikutnya boleh mulai.
    public long eff_hit_wear22_batchStart; // kapan batch yg lagi aktif dimulai
    public long eff_hit_wear22_nextAllowed; // kapan batch BERIKUTNYA boleh mulai

    // Eff mount pas nyerang (MainObject.attack): dipicu tiap kali player yang
    // sedang naik mount berhasil nyerang musuh/mob, dibatasi cooldown per-player
    // supaya cuma muncul sekali tiap MOUNT_ATTACK_EFF_INTERVAL_MS (lihat
    // MainObject).
    public long mountAttackEff_nextAllowed; // kapan trigger berikutnya boleh muncul
    public int[] point_active;

    public boolean is_create_wing;
    public short id_remove_time_use;
    public byte id_wing_split;
    public byte[] in4_auto;
    public List<Player_store> my_store;
    public String my_store_name;
    public String Store_Sell_ToPL = "";
    public Pet_di_buon pet_di_buon;
    public short id_name;
    public String name_mem_clan_to_appoint = "";
    public byte id_select_mo_ly;
    public short id_hop_ngoc;
    public List<Item3> list_thao_kham_ngoc;
    public int time_atk_ngoc_hon_nguyen = 0;
    public short id_ngoc_tinh_luyen = -1;
    public long timeBlockCTG;
    public Wedding it_wedding;
    public String[] in4_wedding;
    public boolean wedding_chest_open = false; // sedang membuka gudang pasangan
    public int[] quest_daily;
    public int chuyencan;
    public int jointx;
    public boolean tai;
    public boolean xiu;
    public String taixiu = "Anda belum memasang taruhan.";
    public boolean isOnline;
    /** ID baris session_log yang aktif. -1 jika belum login / tidak ditrack. */
    public int sessionLogId = -1;
    /** True jika karakter sedang dalam mode AFK (bot aktif, socket terputus). */
    public boolean modeBot;
    /** Kontroler bot AFK — aktif saat modeBot == true. */
    public game.afk.AfkBotController afkBot;
    public Mount mount;
    public PlayerQuest quest;
    public FashionSetting fashionSetting;

    // kĩ năng mề
    public boolean isTangHinh;
    public long time_move;

    public boolean isDropMaterialMedal = true;
    public boolean isDropMaterialUpgrade = true;
    public boolean isDropMaterialWing = true;
    public boolean isDropEquipment = true;
    public boolean isShowMobEvents = true;
    public ArenaTemplate PointArena;

    // kĩ năng khảm
    // public Kham_template kham;
    // create item star
    public boolean isCreateItemStar = false;
    public byte ClazzItemStar = -1;
    public byte TypeItemStarCreate = -1;
    public short[] MaterialItemStar;
    public int id_Upgrade_Medal_Star = -1;

    // biến heo chiến trường
    public long timeBienHeo;
    public long time_use_item_arena;
    public long time_henshin;
    public int id_henshin;

    // === PREMIUM TELEPORT ===
    public long premiumTeleExpireTime = 0; // waktu habis akses premium (epoch ms)
    public utils.Timer.TimerHandle premiumTeleTimer = null; // handle timer kick otomatis

    // UI
    @Getter
    @Setter
    private Dialog currentDialog;
    @Getter
    @Setter
    private InputDialog currentInputDialog;
    @Getter
    private Menu currentMenu;
    private Stack<Menu> menuHistory = new Stack<>();

    @Getter
    @Setter
    private Shop currentShop;

    // Tab priceType yang lagi aktif dipakai pemain di shop saat ini (mis. 4 = poin
    // gems,
    // 5 = poin gold, di menu "Poin Shop"). Null kalau shop biasa (bukan
    // multi-currency
    // tab kayak Poin Shop) — lihat ShopManager.sendEquipmentShop & doPurchase.
    @Getter
    @Setter
    private Integer currentShopPriceType;

    public void ResetCreateItemStar() {
        isCreateItemStar = false;
        ClazzItemStar = -1;
        TypeItemStarCreate = -1;
    }

    public EffTemplate get_eff(int id) {
        for (int i = 0; i < list_eff.size(); i++) {
            EffTemplate temp = list_eff.get(i);
            if (temp.id == id) {
                return temp;
            }
        }
        return null;
    }

    public void updatePointArena(int i) throws IOException {

        this.pointarena += i;
        Service.send_health(this);
        Message m = new Message(-95);
        m.writer().writeByte(0);
        m.writer().writeShort(this.objectId);
        // FIX: pointarena adalah int (poin arena seumur hidup, tidak pernah
        // di-reset - lihat kolom DB point_arena) dan gampang lewat 32767.
        // writeShort di sini memotongnya jadi 16-bit, jadi begitu total poin
        // pemain lewat 32767 angka yang tampil di client jadi salah/minus,
        // padahal core.Service#send_health/send_point_pk di atas maupun nilai
        // yang tersimpan di DB sudah benar (keduanya pakai writeInt). Samakan
        // jadi writeInt supaya konsisten dan tidak overflow.
        m.writer().writeInt(this.pointarena);
        this.conn.addmsg(m);
        m.cleanup();

    }

    public void SetMaterialItemStar() {
        MaterialItemStar = new short[] {
                (short) Util.random(417, 437), (short) Util.random(437, 457), (short) Util.random(326, 336),
                (short) Util.random(336, 346), (short) Util.random(457, 464),
                (short) Util.random(417, 437), (short) Util.random(437, 457), (short) Util.random(326, 336),
                (short) Util.random(336, 346), (short) Util.random(457, 464),
                (short) Util.random(417, 437), (short) Util.random(437, 457), (short) Util.random(326, 336),
                (short) Util.random(336, 346), (short) Util.random(457, 464),
                (short) Util.random(417, 437), (short) Util.random(437, 457), (short) Util.random(326, 336),
                (short) Util.random(336, 346), (short) Util.random(457, 464),
                (short) Util.random(417, 437), (short) Util.random(437, 457), (short) Util.random(326, 336),
                (short) Util.random(336, 346), (short) Util.random(457, 464),
                (short) Util.random(417, 437), (short) Util.random(437, 457), (short) Util.random(326, 336),
                (short) Util.random(336, 346), (short) Util.random(457, 464),
                (short) Util.random(417, 437), (short) Util.random(437, 457), (short) Util.random(326, 336),
                (short) Util.random(336, 346), (short) Util.random(457, 464),
                (short) Util.random(417, 437), (short) Util.random(437, 457), (short) Util.random(326, 336),
                (short) Util.random(336, 346), (short) Util.random(457, 464), };
    }

    public void ChangeMaterialItemStar(byte type) {
        if (type >= 8) {
            return;
        }
        MaterialItemStar[type * 5] = (short) Util.random(417, 437);
        MaterialItemStar[type * 5 + 1] = (short) Util.random(437, 457);

        MaterialItemStar[type * 5 + 2] = (short) Util.random(326, 336);
        MaterialItemStar[type * 5 + 3] = (short) Util.random(336, 346);

        MaterialItemStar[type * 5 + 4] = (short) Util.random(457, 464);
    }

    public Player(Session conn, int id) {
        this.conn = conn;
        this.objectId = id;
        body = this;
        SetPlayer(this);
        quest = new PlayerQuest();
        List<Quest> quests = QuestManager.getInstance().loadPlayerQuest(id);
        for (Quest q : quests) {
            quest.addQuest(q);
        }
    }

    public void CheckSkillPoint() {
        for (int i = 0; i < skillPoint.length; i++) {
            if (skillPoint[i] <= 0) {
                continue;
            }
            // FIX (defensive): kalau data skill di DB belum lengkap/konsisten
            // (mis. jumlah skill per role < 21, atau mLvSkill lebih pendek dari
            // skillPoint yang tersimpan), jangan sampai crash total pas login.
            // Akar masalah tetap harus dibenerin di data/urutan skill-nya
            // (lihat Manager.getSkillByClazz), ini cuma jaring pengaman.
            if (i >= skills.length || skills[i] == null || skills[i].mLvSkill == null
                    || skills[i].mLvSkill.length == 0) {
                continue;
            }
            if (skillPoint[i] > skills[i].mLvSkill.length) {
                skillPoint[i] = (byte) skills[i].mLvSkill.length;
            }
            LvSkill temp = skills[i].mLvSkill[skillPoint[i] - 1];
            while (skillPoint[i] > 0 && temp.LvRe > level) {
                temp = skills[i].mLvSkill[(--skillPoint[i]) - 1];
                kynang++;
            }
        }
    }

    public boolean setup() throws IOException {
        long _time = System.currentTimeMillis();
        String query = "SELECT * FROM `player` WHERE `id` = '" + this.objectId + "' LIMIT 1;";
        try (Connection connection = SQL.gI().getConnection();
                Statement ps = connection.createStatement();
                ResultSet rs = ps.executeQuery(query)) {
            if (!rs.next()) {
                return false;
            }
            //
            this.kham = new Kham_template();
            this.name = rs.getString("name");
            this.timeBlockCTG = rs.getLong("time_block_ctg");
            JSONArray jsar = (JSONArray) JSONValue.parse(rs.getString("body"));
            if (jsar == null) {
                return false;
            }
            head = Byte.parseByte(jsar.get(0).toString());
            eye = Byte.parseByte(jsar.get(1).toString());
            hair = Byte.parseByte(jsar.get(2).toString());
            jsar.clear();
            jsar = (JSONArray) JSONValue.parse(rs.getString("site"));
            if (jsar == null) {
                return false;
            }
            GameMap[] gameMap_enter = GameMap.getMapById(Byte.parseByte(jsar.get(0).toString()));
            if (gameMap_enter != null) {
                x = Short.parseShort(jsar.get(1).toString());
                y = Short.parseShort(jsar.get(2).toString());
            } else {
                gameMap_enter = GameMap.entrys.get(1);
                x = 432;
                y = 354;
            }
            map = gameMap_enter[0];
            other_player_inside = new HashMap<>();
            other_mob_inside = new HashMap<>();
            other_mob_inside_update = new HashMap<>();
            jsar.clear();
            jsar = (JSONArray) JSONValue.parse(rs.getString("eff"));
            if (jsar == null) {
                return false;
            }
            // list_eff = new ArrayList<>();
            for (int i = 0; i < jsar.size(); i++) {
                JSONArray jsar2 = (JSONArray) JSONValue.parse(jsar.get(i).toString());
                if (jsar2 == null) {
                    return false;
                }
                this.body.addEffectDefault(Integer.parseInt(jsar2.get(0).toString()),
                        Integer.parseInt(jsar2.get(1).toString()),
                        (System.currentTimeMillis() + Long.parseLong(jsar2.get(2).toString())));
                // list_eff.add(
                // new EffTemplate(Integer.parseInt(jsar2.get(0).toString()),
                // Integer.parseInt(jsar2.get(1).toString()),
                // (System.currentTimeMillis() + Long.parseLong(jsar2.get(2).toString()))));
            }
            jsar.clear();
            date = Util.getDate(rs.getString("date"));
            diemdanh = rs.getByte("diemdanh");
            chucphuc = rs.getByte("chucphuc");
            hieuchien = rs.getInt("hieuchien");
            try {
                dicuop = rs.getInt("dicuop");
            } catch (SQLException ignoreDicuop) {
                dicuop = 0;
            }
            try {
                gold_dagang = rs.getLong("gold_dagang");
            } catch (SQLException ignoreGoldDagang) {
                gold_dagang = 0;
            }
            chuyencan = rs.getInt("chuyencan");
            type_exp = rs.getByte("typeexp");
            clazz = rs.getByte("clazz");
            level = rs.getShort("level");
            exp = rs.getLong("exp");
            //
            if (level > Manager.gI().lvmax) {
                level = (short) Manager.gI().lvmax;
                if (exp >= Level.entrys.get(level - 1).exp) {
                    exp = Level.entrys.get(level - 1).exp - 1;
                }
            }
            //
            gold = rs.getLong("vang");
            kimcuong = rs.getInt("kimcuong");
            isdie = false;
            tiemnang = rs.getShort("tiemnang");
            kynang = rs.getShort("kynang");
            point1 = rs.getShort("point1");
            point2 = rs.getShort("point2");
            point3 = rs.getShort("point3");
            point4 = rs.getShort("point4");
            pointarena = rs.getInt("point_arena");
            poinIsiUlangEmas = rs.getLong("poin_isi_ulang_emas");
            poinIsiUlangPermata = rs.getLong("poin_isi_ulang_permata");
            point_king_cup = rs.getInt("point_king_cup");
            group_king_cup = rs.getInt("group_king_cup");
            type_reward_king_cup = rs.getByte("type_reward_king_cup");
            bye_count_king_cup = rs.getInt("bye_count_king_cup");
            short it_name_ = rs.getShort("id_name");
            if (it_name_ != -1) {
                id_name = (short) (ItemTemplate3.item.get(it_name_).getPart() + 41);
                id_name = (short) (((it_name_ >= 4720 && it_name_ <= 4727) || (it_name_ >= 4765 && it_name_ <= 4767))
                        ? id_name
                        : 78);
            } else {
                id_name = -1;
            }
            skillPoint = new byte[21];
            time_delay_skill = new long[21];
            jsar = (JSONArray) JSONValue.parse(rs.getString("skill"));
            if (jsar == null) {
                return false;
            }
            for (int i = 0; i < 21; i++) {
                skillPoint[i] = Byte.parseByte(jsar.get(i).toString());
                time_delay_skill[i] = 0;
            }
            jsar.clear();
            // load item

            maxbag = rs.getByte("maxbag");
            // FIX: kalau kolom `maxbag` di DB NULL (akun lama / data korup), rs.getByte()
            // balikin 0,
            // yang bikin bag3/box3 jadi array 0 slot (tas kelihatan kosong total) SEKALIGUS
            // bikin
            // get_bag_able() selalu 0 (<1), jadi setiap beli/klaim item selalu kena "Tas
            // penuh!!"
            // walau tasnya sebenarnya bukan penuh — cuma gak punya slot sama sekali.
            // Fallback ke
            // default 126 kalau nilainya gak masuk akal (<=0), dan langsung disimpan balik
            // ke DB
            // supaya permanen — kolom `maxbag` ini TIDAK ikut ke-save di
            // SaveData.process()/flush(),
            // jadi tanpa ini nilainya bakal balik NULL/0 lagi terus tiap kali dicek dari
            // DB.
            if (maxbag <= 0) {
                log.warn("Player {} (id={}) punya maxbag={} di DB (kemungkinan NULL/korup), fallback ke 126",
                        rs.getString("name"), this.objectId, maxbag);
                maxbag = 126;
                try (Connection fixConn = SQL.gI().getConnection();
                        Statement fixSt = fixConn.createStatement()) {
                    fixSt.executeUpdate("UPDATE `player` SET `maxbag` = 126 WHERE `id` = " + this.objectId + ";");
                    // FIX: SQL.java set autoCommit(false) untuk SELURUH connection pool.
                    // Tanpa commit() eksplisit di sini, UPDATE ini cuma hidup di transaksi
                    // lokal lalu di-ROLLBACK otomatis oleh HikariCP begitu fixConn ditutup
                    // (kembali ke pool) -- tanpa exception apapun. Efeknya perbaikan maxbag
                    // ini kelihatan jalan (tidak ada error di log) tapi TIDAK PERNAH benar2
                    // tersimpan, jadi warning log yang sama muncul lagi tiap kali player ini
                    // login ulang, selamanya.
                    if (!fixConn.getAutoCommit()) {
                        fixConn.commit();
                    }
                } catch (SQLException fixEx) {
                    log.error("Gagal perbaiki maxbag untuk player id={} di DB: {}", this.objectId, fixEx.getMessage());
                }
            }
            maxbox = 126;
            item = new Item(this);
            item.bag3 = new Item3[maxbag];
            item.box3 = new Item3[maxbag];
            item.wear = new Item3[24];
            item.bag47 = new ArrayList<>();
            item.box47 = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                item.wear[i] = null;
            }
            for (int i = 0; i < maxbag; i++) {
                item.bag3[i] = null;
                item.box3[i] = null;
            }
            jsar = (JSONArray) JSONValue.parse(rs.getString("item4"));
            if (jsar == null) {
                return false;
            }
            for (int i = 0; i < jsar.size(); i++) {
                JSONArray jsar2 = (JSONArray) JSONValue.parse(jsar.get(i).toString());
                Item47 temp = new Item47();
                temp.id = Short.parseShort(jsar2.get(0).toString());
                temp.quantity = Short.parseShort(jsar2.get(1).toString());
                temp.category = 4;
                if (temp.quantity > 0) {
                    item.bag47.add(temp);
                }
                jsar2.clear();
            }
            jsar.clear();
            jsar = (JSONArray) JSONValue.parse(rs.getString("item7"));
            if (jsar == null) {
                return false;
            }
            for (int i = 0; i < jsar.size(); i++) {
                JSONArray jsar2 = (JSONArray) JSONValue.parse(jsar.get(i).toString());
                Item47 temp = new Item47();
                temp.id = Short.parseShort(jsar2.get(0).toString());
                temp.quantity = Short.parseShort(jsar2.get(1).toString());
                temp.category = 7;
                if (temp.quantity > 0) {
                    item.bag47.add(temp);
                }
                jsar2.clear();
            }
            jsar.clear();
            jsar = (JSONArray) JSONValue.parse(rs.getString("item3"));
            if (jsar == null) {
                return false;
            }
            for (int i = 0; i < jsar.size(); i++) {
                JSONArray jsar2 = (JSONArray) JSONValue.parse(jsar.get(i).toString());
                Item3 temp = new Item3();
                temp.id = Short.parseShort(jsar2.get(0).toString());
                temp.clazz = Byte.parseByte(jsar2.get(1).toString());
                temp.type = Byte.parseByte(jsar2.get(2).toString());
                temp.level = Short.parseShort(jsar2.get(3).toString());
                temp.icon = Short.parseShort(jsar2.get(4).toString());
                temp.color = Byte.parseByte(jsar2.get(5).toString());
                temp.part = Byte.parseByte(jsar2.get(6).toString());
                temp.islock = Byte.parseByte(jsar2.get(7).toString()) == 1;
                temp.name = ItemTemplate3.item.get(temp.id).getName();
                if (temp.islock) {
                    temp.name += " [Terkunci]";
                }
                temp.tier = Byte.parseByte(jsar2.get(8).toString());
                // if (temp.type == 15) {
                // temp.tier = 0;
                // }
                JSONArray jsar3 = (JSONArray) JSONValue.parse(jsar2.get(9).toString());
                temp.op = new ArrayList<>();
                for (int j = 0; j < jsar3.size(); j++) {
                    JSONArray jsar4 = (JSONArray) JSONValue.parse(jsar3.get(j).toString());
                    temp.op.add(
                            new Option(Byte.parseByte(jsar4.get(0).toString()),
                                    Integer.parseInt(jsar4.get(1).toString()), temp.id));
                }
                temp.time_use = 0;
                if (jsar2.size() >= 11) {
                    temp.time_use = Long.parseLong(jsar2.get(10).toString());
                }
                if (jsar2.size() >= 12) {
                    temp.tierStar = Byte.parseByte(jsar2.get(11).toString());
                }
                if (jsar2.size() >= 13) {
                    temp.expiry_date = Long.parseLong(jsar2.get(12).toString());
                }
                temp.updateName();
                if (temp.expiry_date == 0 || temp.expiry_date > _time) {
                    item.bag3[i] = temp;
                }

            }

            jsar.clear();
            jsar = (JSONArray) JSONValue.parse(rs.getString("itemwear"));
            if (jsar == null) {
                return false;
            }
            for (int i = 0; i < jsar.size(); i++) {
                JSONArray jsar2 = (JSONArray) JSONValue.parse(jsar.get(i).toString());
                if (jsar2 == null) {
                    return false;
                }
                Item3 temp = new Item3();
                temp.id = Short.parseShort(jsar2.get(0).toString());
                temp.clazz = Byte.parseByte(jsar2.get(1).toString());
                temp.type = Byte.parseByte(jsar2.get(2).toString());

                // FIX: cincin wedding (type=103) jangan lookup equipment template
                // id=0 bukan item equipment biasa, langsung ambil dari Wedding object
                if (temp.type == 103) {
                    event_daily.Wedding wed = event_daily.Wedding.get_obj(this.name);
                    if (wed != null) {
                        this.it_wedding = wed;
                        item.wear[23] = wed.it;
                    }
                    continue;
                }

                temp.name = ItemTemplate3.item.get(temp.id).getName() + " [Terkunci]";
                temp.level = Short.parseShort(jsar2.get(3).toString());
                temp.icon = Short.parseShort(jsar2.get(4).toString());
                temp.color = Byte.parseByte(jsar2.get(5).toString());
                temp.part = Short.parseShort(jsar2.get(6).toString());
                temp.tier = Byte.parseByte(jsar2.get(7).toString());
                // if (temp.type == 15) {
                // temp.tier = 0;
                // }
                temp.islock = true;

                JSONArray jsar3 = (JSONArray) JSONValue.parse(jsar2.get(8).toString());
                temp.op = new ArrayList<>();
                for (Object o : jsar3) {
                    JSONArray jsar4 = (JSONArray) JSONValue.parse(o.toString());
                    if (jsar4 == null) {
                        return false;
                    }
                    temp.op.add(
                            new Option(Byte.parseByte(jsar4.get(0).toString()),
                                    Integer.parseInt(jsar4.get(1).toString()), temp.id));
                }

                Byte indexWear = Byte.valueOf(jsar2.get(9).toString());
                if (jsar2.size() >= 11) {
                    temp.tierStar = Byte.parseByte(jsar2.get(10).toString());
                }
                if (jsar2.size() >= 12) {
                    temp.expiry_date = Long.parseLong(jsar2.get(11).toString());
                }
                temp.time_use = 0;
                temp.updateName();
                temp.convertWing();
                if (temp.expiry_date == 0 || temp.expiry_date > _time) {
                    item.wear[indexWear] = temp;
                    if (indexWear == 21) {
                        setupMount(temp);
                    }
                }

            }

            jsar.clear();
            jsar = (JSONArray) JSONValue.parse(rs.getString("giftcode"));
            if (jsar == null) {
                return false;
            }
            giftcode = new ArrayList<>();
            for (int i = 0; i < jsar.size(); i++) {
                giftcode.add(jsar.get(i).toString());
            }
            jsar.clear();
            // box
            jsar = (JSONArray) JSONValue.parse(rs.getString("itembox4"));
            if (jsar == null) {
                return false;
            }
            for (int i = 0; i < jsar.size(); i++) {
                JSONArray jsar2 = (JSONArray) JSONValue.parse(jsar.get(i).toString());
                if (jsar2 == null) {
                    return false;
                }
                Item47 temp = new Item47();
                temp.id = Short.parseShort(jsar2.get(0).toString());
                temp.quantity = Short.parseShort(jsar2.get(1).toString());
                temp.category = 4;
                if (temp.quantity > 0) {
                    item.box47.add(temp);
                }
                jsar2.clear();
            }
            jsar.clear();
            jsar = (JSONArray) JSONValue.parse(rs.getString("itembox7"));
            if (jsar == null) {
                return false;
            }
            for (int i = 0; i < jsar.size(); i++) {
                JSONArray jsar2 = (JSONArray) JSONValue.parse(jsar.get(i).toString());
                if (jsar2 == null) {
                    return false;
                }
                Item47 temp = new Item47();
                temp.id = Short.parseShort(jsar2.get(0).toString());
                temp.quantity = Short.parseShort(jsar2.get(1).toString());
                temp.category = 7;
                if (temp.quantity > 0) {
                    item.box47.add(temp);
                }
                jsar2.clear();
            }
            jsar.clear();
            jsar = (JSONArray) JSONValue.parse(rs.getString("itembox3"));
            if (jsar == null) {
                return false;
            }
            for (int i = 0; i < jsar.size(); i++) {
                JSONArray jsar2 = (JSONArray) JSONValue.parse(jsar.get(i).toString());
                if (jsar2 == null) {
                    return false;
                }
                Item3 temp = new Item3();
                temp.id = Short.parseShort(jsar2.get(0).toString());
                temp.clazz = Byte.parseByte(jsar2.get(1).toString());
                temp.type = Byte.parseByte(jsar2.get(2).toString());
                temp.level = Short.parseShort(jsar2.get(3).toString());
                temp.icon = Short.parseShort(jsar2.get(4).toString());
                temp.color = Byte.parseByte(jsar2.get(5).toString());
                temp.part = Byte.parseByte(jsar2.get(6).toString());
                temp.islock = Byte.parseByte(jsar2.get(7).toString()) == 1;
                temp.name = ItemTemplate3.item.get(temp.id).getName();
                if (temp.islock) {
                    temp.name += " [Terkunci]";
                }
                temp.tier = Byte.parseByte(jsar2.get(8).toString());
                // if (temp.type == 15) {
                // temp.tier = 0;
                // }
                JSONArray jsar3 = (JSONArray) JSONValue.parse(jsar2.get(9).toString());
                temp.op = new ArrayList<>();
                for (int j = 0; j < jsar3.size(); j++) {
                    JSONArray jsar4 = (JSONArray) JSONValue.parse(jsar3.get(j).toString());
                    temp.op.add(
                            new Option(Byte.parseByte(jsar4.get(0).toString()),
                                    Integer.parseInt(jsar4.get(1).toString()), temp.id));
                }
                temp.time_use = 0;
                if (jsar2.size() >= 11) {
                    temp.time_use = Long.parseLong(jsar2.get(10).toString());
                }
                if (jsar2.size() >= 12) {
                    temp.tierStar = Byte.parseByte(jsar2.get(11).toString());
                }
                if (jsar2.size() >= 13) {
                    temp.expiry_date = Long.parseLong(jsar2.get(12).toString());
                }
                temp.updateName();
                if (temp.expiry_date == 0 || temp.expiry_date > _time) {
                    item.box3[i] = temp;
                }
            }
            jsar.clear();
            jsar = (JSONArray) JSONValue.parse(rs.getString("rms_save"));
            if (jsar == null) {
                return false;
            }
            rms_save = new byte[jsar.size()][];
            for (int i = 0; i < rms_save.length; i++) {
                JSONArray js = (JSONArray) JSONValue.parse(jsar.get(i).toString());
                rms_save[i] = new byte[js.size()];
                for (int j = 0; j < rms_save[i].length; j++) {
                    rms_save[i][j] = Byte.parseByte(js.get(j).toString());
                }
            }
            jsar.clear();
            //
            mypet = new ArrayList<>();
            pet_follow = -1;
            jsar = (JSONArray) JSONValue.parse(rs.getString("pet"));
            long t_off = 0;
            if (jsar == null) {
                return false;
            }
            for (int i = 0; i < jsar.size(); i++) {
                JSONArray js = (JSONArray) JSONValue.parse(jsar.get(i).toString());
                Pet temp = new Pet();
                temp.setup(js);
                temp.update_grown(t_off);
                if (temp.is_follow) {
                    pet_follow = temp.getId();
                }
                if (temp.expiry_date == 0 || _time < temp.expiry_date) {
                    mypet.add(temp);
                }
            }
            jsar.clear();
            list_friend = new ArrayList<>();
            jsar = (JSONArray) JSONValue.parse(rs.getString("friend"));
            if (jsar == null) {
                return false;
            }
            for (int i = 0; i < jsar.size(); i++) {
                JSONArray js12 = (JSONArray) JSONValue.parse(jsar.get(i).toString());
                Friend temp = new Friend();
                temp.name = js12.get(0).toString();
                temp.level = Short.parseShort(js12.get(1).toString());
                temp.head = Byte.parseByte(js12.get(2).toString());
                temp.hair = Byte.parseByte(js12.get(3).toString());
                temp.eye = Byte.parseByte(js12.get(4).toString());
                temp.itemwear = new ArrayList<>();
                JSONArray js2 = (JSONArray) JSONValue.parse(js12.get(5).toString());
                for (int j = 0; j < js2.size(); j++) {
                    JSONArray js3 = (JSONArray) JSONValue.parse(js2.get(j).toString());
                    PlayerPart part = new PlayerPart();
                    part.type = Byte.parseByte(js3.get(0).toString());
                    part.part = Byte.parseByte(js3.get(1).toString());
                    temp.itemwear.add(part);
                }
                list_friend.add(temp);
            }
            jsar.clear();
            list_enemies = new ArrayList<>();
            jsar = (JSONArray) JSONValue.parse(rs.getString("enemies"));
            if (jsar == null) {
                return false;
            }
            for (int i = 0; i < jsar.size(); i++) {
                String n = jsar.get(i).toString();
                if (!list_enemies.contains(n)) {
                    list_enemies.add(n);
                }
            }
            jsar.clear();
            jsar = (JSONArray) JSONValue.parse(rs.getString("medal_create_material"));
            if (jsar == null) {
                return false;
            }
            medal_create_material = new short[jsar.size()];
            for (int i = 0; i < jsar.size(); i++) {
                medal_create_material[i] = Short.parseShort(jsar.get(i).toString());
            }
            jsar.clear();

            // Medal V2 — kolom baru, boleh kosong/null untuk player lama (belum migrasi)
            try {
                String medalV2Raw = rs.getString("medal_create_material_v2");
                JSONArray jsarV2 = (medalV2Raw == null) ? null : (JSONArray) JSONValue.parse(medalV2Raw);
                if (jsarV2 == null) {
                    medal_create_material_v2 = new short[0];
                } else {
                    medal_create_material_v2 = new short[jsarV2.size()];
                    for (int i = 0; i < jsarV2.size(); i++) {
                        medal_create_material_v2[i] = Short.parseShort(jsarV2.get(i).toString());
                    }
                    jsarV2.clear();
                }
            } catch (Exception exV2) {
                // Kolom belum ada di DB (belum jalankan migration) — pakai default kosong,
                // akan di-generate ulang otomatis oleh initMedalMaterialV2().
                medal_create_material_v2 = new short[0];
            }

            jsar = (JSONArray) JSONValue.parse(rs.getString("item_star_material"));
            if (jsar == null) {
                return false;
            }
            MaterialItemStar = new short[jsar.size()];
            for (int i = 0; i < jsar.size(); i++) {
                MaterialItemStar[i] = Short.parseShort(jsar.get(i).toString());
            }
            if (MaterialItemStar == null || MaterialItemStar.length < 40) {
                SetMaterialItemStar();
            }
            jsar.clear();

            jsar = (JSONArray) JSONValue.parse(rs.getString("point_active"));
            if (jsar == null) {
                return false;
            }
            point_active = new int[jsar.size()];
            for (int i = 0; i < jsar.size(); i++) {
                point_active[i] = Integer.parseInt(jsar.get(i).toString());
            }

            jsar.clear();

            // Load quest_daily: [-1,-1,0,0,20,-1] = [target_id, difficulty, progress,
            // target, sisa_ambil, quest_type]
            try {
                String qdStr = rs.getString("quest_daily");
                if (qdStr != null && !qdStr.isEmpty()) {
                    JSONArray jqd = (JSONArray) JSONValue.parse(qdStr);
                    quest_daily = new int[6];
                    for (int i = 0; i < Math.min(jqd.size(), 6); i++) {
                        quest_daily[i] = Integer.parseInt(jqd.get(i).toString());
                    }
                    // Pastikan panjang 6, isi sisanya jika kurang (migrasi dari array [5])
                    if (jqd.size() < 6) {
                        quest_daily[5] = -1;
                    }
                } else {
                    quest_daily = new int[] { -1, -1, 0, 0, 5, -1 };
                }
            } catch (Exception e) {
                quest_daily = new int[] { -1, -1, 0, 0, 5, -1 };
            }

            // Load body/leg effect (112_xx.png)
            try {
                this.bodyEffectId = rs.getShort("body_effect_id");
            } catch (Exception e) {
                this.bodyEffectId = -1;
            }
            try {
                this.legEffectId = rs.getShort("leg_effect_id");
            } catch (Exception e) {
                this.legEffectId = -1;
            }

            myclan = Guild.getPlayerGuild(this.name);
            //
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        fashionSetting = SQLHelper
                .selectFrom("fashion_setting")
                .where("player_id", objectId)
                .firstAsModel(FashionSetting.class);

        if (fashionSetting == null) {

            fashionSetting = new FashionSetting();
            fashionSetting.setPlayerId(objectId);
            SQLHelper
                    .insert("fashion_setting")
                    .values(ModelMapper.toMap(fashionSetting))
                    .execute();

        }

        already_setup = true;
        return true;
    }

    public void setupMount(Item3 itemMount) {
        mount = ItemManager.getInstance().getMountData(itemMount.id);
    }

    private void load_skill() throws IOException {
        ByteArrayInputStream bais = null;
        DataInputStream dis = null;
        try {
            switch (clazz) {
                case 0 -> { // chien binh
                    bais = new ByteArrayInputStream(Manager.gI().msg_29_chienbinh);
                }
                case 1 -> { // sat thu
                    bais = new ByteArrayInputStream(Manager.gI().msg_29_satthu);
                }
                case 2 -> { // phap su
                    bais = new ByteArrayInputStream(Manager.gI().msg_29_phapsu);
                }
                case 3 -> { // xa thu
                    bais = new ByteArrayInputStream(Manager.gI().msg_29_xathu);
                }
            }
            dis = new DataInputStream(bais);
            int size = dis.readByte();
            skills = new Skill[size];
            for (int i = 0; i < size; i++) {
                Skill skill = new Skill();
                skill.id = dis.readByte();
                skill.iconid = dis.readByte();
                skill.name = dis.readUTF();
                skill.type = dis.readByte();
                skill.range = dis.readShort();
                skill.detail = dis.readUTF();
                skill.typeBuff = dis.readByte();
                skill.subEff = dis.readByte();
                byte b2 = dis.readByte();
                skill.mLvSkill = new LvSkill[(int) b2];
                for (int j = 0; j < (int) b2; j++) {
                    skill.mLvSkill[j] = new LvSkill();
                    skill.mLvSkill[j].mpLost = dis.readShort();
                    skill.mLvSkill[j].LvRe = dis.readShort();
                    skill.mLvSkill[j].delay = dis.readInt();
                    skill.mLvSkill[j].timeBuff = dis.readInt();
                    skill.mLvSkill[j].per_Sub_Eff = dis.readByte();
                    skill.mLvSkill[j].time_Sub_Eff = dis.readShort();
                    skill.mLvSkill[j].plus_Hp = dis.readShort();
                    skill.mLvSkill[j].plus_Mp = dis.readShort();
                    byte b3 = dis.readByte();
                    skill.mLvSkill[j].minfo = new Option[(int) b3];
                    for (int k = 0; k < (int) b3; k++) {
                        skill.mLvSkill[j].minfo[k] = new Option(dis.readUnsignedByte(), dis.readInt(), (short) 0);
                    }
                    skill.mLvSkill[j].nTarget = dis.readByte();
                    skill.mLvSkill[j].range_lan = dis.readShort();
                }
                skill.performDur = dis.readShort();
                skill.typePaint = dis.readByte();
                skills[skill.id] = skill;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (dis != null) {
                dis.close();
            }
            if (bais != null) {
                bais.close();
            }
        }
    }

    // public EffTemplate get_EffDefault(int id) {
    // for (int i = 0; i < list_eff.size(); i++) {
    // EffTemplate temp = list_eff.get(i);
    // if (temp.id == id) {
    // return temp;
    // }
    // }
    // return null;
    // }
    public synchronized long getGold() {
        return this.gold;
    }

    public synchronized int getGem() {
        return this.kimcuong;
    }

    public synchronized void updateGold(long i) {
        if ((i + gold) > 2__000_000_000_000_000L) {
            gold = 2__000_000_000_000_000L;
        } else {
            gold += i;
        }
        try {
            Message m = new Message(16);
            m.writer().writeByte(0);
            m.writer().writeByte(5);
            m.writer().writeLong(this.getGold());
            m.writer().writeInt(this.getGem());
            m.writer().writeByte(5);
            m.writer().writeByte(0); // size item quest
            conn.addmsg(m);
            m.cleanup();
        } catch (Exception ignored) {
        }
    }

    public synchronized void updateGem(long i) {
        if ((i + kimcuong) > 2_000_000_000L) {
            kimcuong = 2_000_000_000;
        } else {
            kimcuong += i;
        }
        try {
            Message m = new Message(16);
            m.writer().writeByte(0);
            m.writer().writeByte(5);
            m.writer().writeLong(this.getGold());
            m.writer().writeInt(this.getGem());
            m.writer().writeByte(5);
            m.writer().writeByte(0); // size item quest
            conn.addmsg(m);
            m.cleanup();
        } catch (Exception e) {
        }
    }

    /**
     * Potong gems tanpa kirim packet ke client — aman dipanggil saat bot aktif
     * (conn == null).
     * Langsung modifikasi field kimcuong dan flush ke DB.
     */
    public synchronized void deductGemSilent(int amount) {
        kimcuong = Math.max(0, kimcuong - amount);
        flush(); // simpan ke DB langsung
    }

    @SuppressWarnings("unchecked")
    public void flush() {
        if (!already_setup) {
            return;
        }
        boolean wasInterrupted = Thread.interrupted(); // prevent HikariCP InterruptedException on shutdown
        try (Connection connection = SQL.gI().getConnection(); Statement ps = connection.createStatement()) {
            String a = "`level` = " + level;
            a += ",`exp` = " + exp;
            JSONArray jsar = new JSONArray();
            if (isdie || GameMap.is_map_cant_save_site(map.mapId)) {
                jsar.add(1);
                jsar.add(432);
                jsar.add(354);
            } else {
                jsar.add(map.mapId);
                jsar.add(x);
                jsar.add(y);
            }
            a += ",`site` = '" + jsar.toJSONString() + "'";
            jsar.clear();
            jsar.add(head);
            jsar.add(eye);
            jsar.add(hair);
            a += ",`body` = '" + jsar.toJSONString() + "'";
            jsar.clear();
            for (int i = 0; i < effectDefault.size(); i++) {
                EffTemplate temp = effectDefault.get(i);
                if (temp.id != -126 && temp.id != -125) {
                    continue;
                }
                JSONArray jsar21 = new JSONArray();
                jsar21.add(temp.id);
                jsar21.add(temp.param);
                long time = temp.time - System.currentTimeMillis();
                jsar21.add(time);
                jsar.add(jsar21);
            }
            a += ",`eff` = '" + jsar.toJSONString() + "'";
            jsar.clear();
            for (int i = 0; i < list_friend.size(); i++) {
                JSONArray js12 = new JSONArray();
                Friend temp = list_friend.get(i);
                js12.add(temp.name);
                js12.add(temp.level);
                js12.add(temp.head);
                js12.add(temp.hair);
                js12.add(temp.eye);
                JSONArray js = new JSONArray();
                for (PlayerPart part : temp.itemwear) {
                    JSONArray js2 = new JSONArray();
                    js2.add(part.type);
                    js2.add(part.part);
                    js.add(js2);
                }
                js12.add(js);
                jsar.add(js12);
            }
            a += ",`friend` = '" + sqlEscape(jsar.toJSONString()) + "'";
            jsar.clear();
            for (int i = 0; i < 21; i++) {
                jsar.add(skillPoint[i]);
            }
            a += ",`skill` = '" + jsar.toJSONString() + "'";
            jsar.clear();
            //
            for (Item47 it : item.bag47) {
                if (it.category == 4) {
                    JSONArray jsar2 = new JSONArray();
                    jsar2.add(it.id);
                    jsar2.add(it.quantity);
                    jsar.add(jsar2);
                }
            }
            a += ",`item4` = '" + jsar.toJSONString() + "'";
            jsar.clear();
            //
            for (Item47 it : item.bag47) {
                if (it.category == 7) {
                    JSONArray jsar2 = new JSONArray();
                    jsar2.add(it.id);
                    jsar2.add(it.quantity);
                    jsar.add(jsar2);
                }
            }
            a += ",`item7` = '" + jsar.toJSONString() + "'";
            jsar.clear();
            //
            long _time = System.currentTimeMillis();
            for (int i = 0; i < item.bag3.length; i++) {
                Item3 temp = item.bag3[i];
                if (temp != null) {
                    if (temp.expiry_date != 0 && _time > temp.expiry_date) {
                        item.bag3[i] = null;
                        try {
                            conn.p.item.charInventory(3);
                        } catch (IOException eee) {
                        }
                        continue;
                    }
                    JSONArray jsar2 = new JSONArray();
                    jsar2.add(temp.id);
                    jsar2.add(temp.clazz);
                    jsar2.add(temp.type);
                    jsar2.add(temp.level);
                    jsar2.add(temp.icon);
                    jsar2.add(temp.color);
                    jsar2.add(temp.part);
                    jsar2.add(temp.islock ? 1 : 0);
                    jsar2.add(temp.tier);
                    JSONArray jsar3 = new JSONArray();
                    for (int j = 0; j < temp.op.size(); j++) {
                        JSONArray jsar4 = new JSONArray();
                        jsar4.add(temp.op.get(j).id);
                        jsar4.add(temp.op.get(j).getParam(0));
                        jsar3.add(jsar4);
                    }
                    jsar2.add(jsar3);
                    jsar2.add(temp.time_use);
                    jsar2.add(temp.tierStar);
                    jsar2.add(temp.expiry_date);
                    jsar.add(jsar2);
                }
            }
            a += ",`item3` = '" + jsar.toJSONString() + "'";
            jsar.clear();
            //
            for (int i = 0; i < item.wear.length; i++) {
                Item3 temp = item.wear[i];
                if (temp != null) {
                    if (temp.expiry_date != 0 && _time > temp.expiry_date) {
                        item.wear[i] = null;
                        try {
                            item.charInventory(3);
                            fashion = PartFashion.getPart(this);
                            Service.sendPlayerWear(this);
                            Service.sendMainCharInfo(this);
                            MapService.broadcastMainCharInfo(this.map, this);
                        } catch (IOException eee) {
                        }
                        continue;
                    }
                    JSONArray jsar2 = new JSONArray();
                    jsar2.add(temp.id);
                    jsar2.add(temp.clazz);
                    jsar2.add(temp.type);
                    jsar2.add(temp.level);
                    jsar2.add(temp.icon);
                    jsar2.add(temp.color);
                    jsar2.add(temp.part);
                    jsar2.add(temp.tier);
                    JSONArray jsar3 = new JSONArray();
                    for (int j = 0; j < temp.op.size(); j++) {
                        JSONArray jsar4 = new JSONArray();
                        jsar4.add(temp.op.get(j).id);
                        jsar4.add(temp.op.get(j).getParam(0));
                        jsar3.add(jsar4);
                    }
                    jsar2.add(jsar3);
                    jsar2.add(i);
                    jsar2.add(temp.tierStar);
                    jsar2.add(temp.expiry_date);
                    jsar.add(jsar2);
                }
            }
            a += ",`itemwear` = '" + jsar.toJSONString() + "'";
            jsar.clear();
            //
            for (int i = 0; i < giftcode.size(); i++) {
                jsar.add(giftcode.get(i));
            }
            a += ",`giftcode` = '" + sqlEscape(jsar.toJSONString()) + "'";
            jsar.clear();
            for (int i = 0; i < list_enemies.size(); i++) {
                jsar.add(list_enemies.get(i));
            }
            a += ",`enemies` = '" + sqlEscape(jsar.toJSONString()) + "'";
            jsar.clear();
            for (int i = 0; i < rms_save.length; i++) {
                JSONArray js = new JSONArray();
                for (int i1 = 0; i1 < rms_save[i].length; i1++) {
                    js.add(rms_save[i][i1]);
                }
                jsar.add(js);
            }
            a += ",`rms_save` = '" + jsar.toJSONString() + "'";
            jsar.clear();
            //
            for (int i = 0; i < item.box47.size(); i++) {
                if (item.box47.get(i).category == 4) {
                    JSONArray jsar2 = new JSONArray();
                    jsar2.add(item.box47.get(i).id);
                    jsar2.add(item.box47.get(i).quantity);
                    jsar.add(jsar2);
                }
            }
            a += ",`itembox4` = '" + jsar.toJSONString() + "'";
            jsar.clear();
            //
            for (int i = 0; i < item.box47.size(); i++) {
                if (item.box47.get(i).category == 7) {
                    JSONArray jsar2 = new JSONArray();
                    jsar2.add(item.box47.get(i).id);
                    jsar2.add(item.box47.get(i).quantity);
                    jsar.add(jsar2);
                }
            }
            a += ",`itembox7` = '" + jsar.toJSONString() + "'";
            jsar.clear();
            //
            for (int i = 0; i < item.box3.length; i++) {
                Item3 temp = item.box3[i];
                if (temp != null) {
                    if (temp.expiry_date != 0 && _time > temp.expiry_date) {
                        item.box3[i] = null;
                        try {
                            conn.p.item.char_chest(3);
                        } catch (IOException eee) {
                        }
                        continue;
                    }
                    JSONArray jsar2 = new JSONArray();
                    jsar2.add(temp.id);
                    jsar2.add(temp.clazz);
                    jsar2.add(temp.type);
                    jsar2.add(temp.level);
                    jsar2.add(temp.icon);
                    jsar2.add(temp.color);
                    jsar2.add(temp.part);
                    jsar2.add(temp.islock ? 1 : 0);
                    jsar2.add(temp.tier);
                    JSONArray jsar3 = new JSONArray();
                    for (int j = 0; j < temp.op.size(); j++) {
                        JSONArray jsar4 = new JSONArray();
                        jsar4.add(temp.op.get(j).id);
                        jsar4.add(temp.op.get(j).getParam(0));
                        jsar3.add(jsar4);
                    }
                    jsar2.add(jsar3);
                    jsar2.add(temp.time_use);
                    jsar2.add(temp.tierStar);
                    jsar2.add(temp.expiry_date);
                    jsar.add(jsar2);
                }
            }
            a += ",`itembox3` = '" + jsar.toJSONString() + "'";
            jsar.clear();
            //
            for (int i = 0; i < mypet.size(); i++) {
                JSONArray js1 = new JSONArray();
                js1.add(mypet.get(i).level);
                js1.add(mypet.get(i).type);
                js1.add(mypet.get(i).spriteImage);
                js1.add(mypet.get(i).nframe);
                js1.add(mypet.get(i).color);
                js1.add(mypet.get(i).grown);
                js1.add(mypet.get(i).maxgrown);
                js1.add(mypet.get(i).point1);
                js1.add(mypet.get(i).point2);
                js1.add(mypet.get(i).point3);
                js1.add(mypet.get(i).point4);
                js1.add(mypet.get(i).maxpoint);
                js1.add(mypet.get(i).exp);
                js1.add(mypet.get(i).is_follow ? 1 : 0);
                js1.add(mypet.get(i).is_hatch ? 1 : 0);
                js1.add(mypet.get(i).time_born);
                JSONArray js2 = new JSONArray();
                for (int i2 = 0; i2 < mypet.get(i).op.size(); i2++) {
                    JSONArray js3 = new JSONArray();
                    js3.add(mypet.get(i).op.get(i2).id);
                    js3.add(mypet.get(i).op.get(i2).value);
                    js3.add(mypet.get(i).op.get(i2).maxValue);
                    js2.add(js3);
                }
                js1.add(js2);
                js1.add(mypet.get(i).expiry_date);
                jsar.add(js1);
            }
            a += ",`pet` = '" + jsar.toJSONString() + "'";
            jsar.clear();
            //
            for (int i = 0; i < medal_create_material.length; i++) {
                jsar.add(medal_create_material[i]);
            }
            a += ",`medal_create_material` = '" + jsar.toJSONString() + "'";
            jsar.clear();
            //
            if (medal_create_material_v2 != null) {
                for (int i = 0; i < medal_create_material_v2.length; i++) {
                    jsar.add(medal_create_material_v2[i]);
                }
            }
            a += ",`medal_create_material_v2` = '" + jsar.toJSONString() + "'";
            jsar.clear();

            //
            for (int i = 0; i < MaterialItemStar.length; i++) {
                jsar.add(MaterialItemStar[i]);
            }
            a += ",`item_star_material` = '" + jsar.toJSONString() + "'";
            jsar.clear();

            for (int i = 0; i < point_active.length; i++) {
                jsar.add(point_active[i]);
            }
            a += ",`point_active` = '" + jsar.toJSONString() + "'";
            jsar.clear();

            // Save quest_daily
            if (quest_daily != null) {
                JSONArray jqd = new JSONArray();
                for (int v : quest_daily)
                    jqd.add(v);
                a += ",`quest_daily` = '" + jqd.toJSONString() + "'";
            }
            //

            a += ",`vang` = " + gold;
            a += ",`kimcuong` = " + kimcuong;
            a += ",`tiemnang` = " + tiemnang;
            a += ",`kynang` = " + kynang;
            a += ",`diemdanh` = " + diemdanh;
            a += ",`chucphuc` = " + chucphuc;
            a += ",`hieuchien` = " + hieuchien;
            a += ",`dicuop` = " + dicuop;
            a += ",`gold_dagang` = " + gold_dagang;
            a += ",`chuyencan` = " + chuyencan;
            a += ",`typeexp` = " + type_exp;
            a += ",`date` = '" + date.toString() + "'";
            a += ",`point1` = " + point1;
            a += ",`point2` = " + point2;
            a += ",`point3` = " + point3;
            a += ",`point4` = " + point4;
            a += ",`point_arena` = " + pointarena;
            a += ",`poin_isi_ulang_emas` = " + poinIsiUlangEmas;
            a += ",`poin_isi_ulang_permata` = " + poinIsiUlangPermata;
            a += ",`point_king_cup` = " + point_king_cup;
            a += ",`group_king_cup` = " + group_king_cup;
            a += ",`type_reward_king_cup` = " + type_reward_king_cup;
            a += ",`bye_count_king_cup` = " + bye_count_king_cup;
            // a += ",`body_effect_id` = " + bodyEffectId; // kolom belum ada di DB
            // a += ",`leg_effect_id` = " + legEffectId; // kolom belum ada di DB
            if (ps.executeUpdate("UPDATE `player` SET " + a + " WHERE `id` = " + this.objectId + ";") > 0) {
                connection.commit();
            }
            ps.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Save Fashion
        // NOTE: harus dijalankan SEBELUM restore flag interrupt di bawah.
        // Sebelumnya restore-nya ada di finally block di atas, jadi query
        // fashion_setting ini jalan dengan interrupt flag yang sudah
        // di-set lagi -> HikariCP langsung lempar InterruptedException
        // ("Interrupted during connection acquisition") setiap kali ada
        // disconnect normal, karena Session.disconnect() men-interrupt
        // thread-nya sendiri sebelum manggil flush().
        SQLHelper
                .update("fashion_setting")
                .where("player_id", objectId)
                .set(ModelMapper.toMap(fashionSetting)).execute();

        if (wasInterrupted)
            Thread.currentThread().interrupt();
    }

    public void change_new_date() {
        if (!Util.is_same_day(Date.from(Instant.now()), date)) {
            // diem danh
            diemdanh = 1;
            chucphuc = 1;
            point_active[0] = 10;
            point_active[1] = 0;
            //
            date = Date.from(Instant.now());
        }
    }

    public void set_x2_xp(int type) throws IOException {
        switch (type) {
            case 0: {
                Message m = new Message(62);
                m.writer().writeByte(0);
                m.writer().writeShort(0);
                conn.addmsg(m);
                m.cleanup();
                break;
            }
            case 1: {
                EffTemplate tempp = conn.p.getEffectDefault(-125);
                if (tempp != null) {
                    long time_eff = tempp.time - System.currentTimeMillis();
                    Message m = new Message(62);
                    m.writer().writeByte(1);
                    m.writer().writeShort((short) (time_eff / 60000L));
                    conn.addmsg(m);
                    m.cleanup();
                    // add_EffDefault(-125, 5000, (int) time_eff);
                }
                break;
            }
        }
    }

    public void add_EffDefault(int id, int param, int time) {
        this.body.addEffectDefault(id, param, System.currentTimeMillis() + time);
        // synchronized (list_eff) {
        // if (param == 0) {
        // return;
        // }
        // EffTemplate temp_test = get_EffDefault(id);
        // while (temp_test != null) {
        // list_eff.remove(temp_test);
        // temp_test = get_EffDefault(id);
        // }
        // EffTemplate temp = new EffTemplate(id, param, (System.currentTimeMillis() +
        // time));
        // list_eff.add(temp);
        // }
    }

    public int getlevelpercent() {
        return (int) ((exp * 1000) / Level.entrys.get(level - 1).exp);
    }

    public void load_in4_autoplayer(byte[] num) {
        this.in4_auto = num;
        // System.out.println(hp_mp_can_pick);
        // num[0]; on off auto use poition (0 = off)
        // num[1]; %hp use poition
        // num[2]; %mp use poition
        // num[3]; on off pick item (0 = off)
        // num[4];(0 = all, 1 ->)
        // num[5]; (0 = all, 1 = non)
        // num[6]; (0 = all, 1 = hp, 2 = mp, 3 = non)
    }

    public void changeMap(Player p, Vgo vgo) throws IOException {
        // ── Validasi akses map premium (111-114) ──────────────────────────
        // Hanya bisa masuk dari map 115 (gateway) dan harus punya sesi aktif
        if (!feature.teleport.PremiumTeleportManager.gI().canEnterPremiumMap(p, vgo.toMap)) {
            if (feature.teleport.PremiumTeleportManager.isPremiumMap(vgo.toMap)) {
                if (!feature.teleport.PremiumTeleportManager.isGatewayMap(p.map.mapId)) {
                    Service.send_notice_box(p.conn,
                            "❌ Akses ditolak!\nKamu harus berada di Area Gateway (map 115)\nuntuk masuk ke area premium.");
                } else {
                    Service.send_notice_box(p.conn,
                            "❌ Akses ditolak!\nKamu tidak memiliki sesi premium aktif.\nBeli paket di Batu Teleport Premium terlebih dahulu.");
                }
            }
            return;
        }
        // ─────────────────────────────────────────────────────────────────
        if (map.mapId == 0) {
            Message m = new Message(55);
            m.writer().writeByte(1);
            m.writer().writeShort(2);
            m.writer().writeByte(-1);
            m.writer().writeByte(0);
            conn.addmsg(m);
            m.cleanup();
        }
        p.isChangemap = false;
        p.xOld = vgo.x;
        p.yOld = vgo.y;
        GameMap[] gameMaps = GameMap.getMapById(vgo.toMap);
        if (gameMaps != null) {
            GameMap mbuffer2 = null;
            if (party != null) {
                for (int i = 0; i < party.get_mems().size(); i++) {
                    Player p0 = party.get_mems().get(i);
                    if (p0.map.mapId == gameMaps[0].mapId) {
                        mbuffer2 = p0.map;
                    }
                }
            }
            if (conn.p.item.wear[11] != null && (conn.p.item.wear[11].id == 3599 || conn.p.item.wear[11].id == 3593
                    || conn.p.item.wear[11].id == 3596)) {
                mbuffer2 = gameMaps[gameMaps[0].maxzone];
            } else {
                if (mbuffer2 == null) {
                    for (GameMap mapp : gameMaps) {
                        if (mapp.players.size() < mapp.maxplayer) {
                            mbuffer2 = mapp;
                            break;
                        }
                    }
                }
            }
            if (mbuffer2 == null) {
                Service.send_notice_box(p.conn,
                        "Terjadi kesalahan saat berpindah map atau map sudah penuh, silakan coba lagi nanti");
                return;
            }
            // di buon
            boolean tele = true;
            for (Vgo item : p.map.vgos) {
                if (item.toMap == mbuffer2.mapId) {
                    tele = false;
                    break;
                }
            }
            boolean adminTeleportBringPet = conn.ac_admin >= 10;
            if (p.pet_di_buon != null && (adminTeleportBringPet
                    || (!tele && (Math.abs(p.pet_di_buon.x - p.x) < 125 && Math.abs(p.pet_di_buon.y - p.y) < 125)))) {
                Message mout = new Message(8);
                mout.writer().writeShort(p.pet_di_buon.objectId);
                for (int i = 0; i < map.players.size(); i++) {
                    Player p0 = map.players.get(i);
                    if (p0 != null) {
                        p0.conn.addmsg(mout);
                    }
                }
                mout.cleanup();
                p.pet_di_buon.x = vgo.toX;
                p.pet_di_buon.y = vgo.toY;
                p.pet_di_buon.id_map = mbuffer2.mapId;
                Message m22 = new Message(4);
                m22.writer().writeByte(1);
                m22.writer().writeShort(131);
                m22.writer().writeShort(conn.p.pet_di_buon.objectId);
                m22.writer().writeShort(conn.p.pet_di_buon.x);
                m22.writer().writeShort(conn.p.pet_di_buon.y);
                m22.writer().writeByte(-1);
                conn.addmsg(m22);
                m22.cleanup();
            }
            //
            MapService.leave(p.map, p);
            p.map = mbuffer2;
            p.x = vgo.toX;
            p.y = vgo.toY;
            p.xOld = p.x;
            p.yOld = p.y;
            MapService.enter(p.map, p);
        } else {
            Service.send_notice_box(p.conn, "Terjadi kesalahan saat berpindah map");
        }
    }

    public void updateExp(long expup, boolean expmulti) throws IOException {
        long dame_exp = expup;
        if (expmulti && this.getlevelpercent() >= 0) {
            // REVERT: Manager.gI().exp balik ke skala persen (100 = x1 normal,
            // 200 = x2, dst) — bukan raw multiplier langsung lagi.
            dame_exp = (dame_exp * Manager.gI().exp) / 100;
        }
        if (mount != null && mount.getType() == 4) {
            dame_exp += ((dame_exp * 5) / 100);
        }
        if ((type_exp == 0 && this.typepk != 0) || this.getlevelpercent() < (-500)) {
            return;
        }
        if (level >= Manager.gI().lvmax || type_exp == 0) {
            return;
        }
        Message m;
        if (this.getlevelpercent() < 0) {
            if (dame_exp > 0) {
                dame_exp /= 5;
                // exp kecil (<5) sebelum dibagi kepotong jadi 0 walau
                // sebenarnya player tetap dapet exp — kasih minimal 1
                if (dame_exp <= 0) {
                    dame_exp = 1;
                }
            } else {
                dame_exp *= 2;
            }
        }
        exp += dame_exp;
        if (this.getlevelpercent() < (-500)) {
            exp = -(Level.entrys.get(level - 1).exp * 15) / 10;
        }
        int exp_as_int = 0;
        if (dame_exp > 2_000_000_000L) {

        } else {
            exp_as_int = (int) dame_exp;
        }
        if (exp >= Level.entrys.get(level - 1).exp) {
            while (exp >= Level.entrys.get(level - 1).exp && level < Manager.gI().lvmax) {
                exp -= Level.entrys.get(level - 1).exp;
                level++;
                if ((tiemnang + point1 + point2 + point3 + point4) < 32000) {
                    point1++;
                    point2++;
                    point3++;
                    point4++;
                    if (kynang < 10000) {
                        kynang += Level.entrys.get(level - 1).kynang;
                    }
                    tiemnang += Level.entrys.get(level - 1).tiemnang;
                }
            }
            if (level == Manager.gI().lvmax && exp >= Level.entrys.get(level - 1).exp) {
                exp = Level.entrys.get(level - 1).exp - 1;
            }
            hp = body.getMaxHP();
            mp = body.getMaxMP();
            m = new Message(33);
            m.writer().writeShort(objectId);
            m.writer().writeByte(level);
            MapService.sendMsgPlayerInside(map, this, m, true);
            m.cleanup();
            // Mode AFK: conn sudah ditutup, skip packet ke client
            if (!modeBot) {
                Service.sendMainCharInfo(this);
            }
            MapService.broadcastMainCharInfo(map, this);
            if (party != null) {
                party.sendin4();
            }
        }
        // Mode AFK: hanya simpan exp ke memory, tidak kirim packet ke client
        if (!modeBot && conn != null && conn.connected) {
            m = new Message(30);
            m.writer().writeShort(objectId);
            m.writer().writeShort(getlevelpercent());
            m.writer().writeInt(exp_as_int);
            conn.addmsg(m);
            m.cleanup();
        }
    }

    public void change_zone(Session conn2, Message m2) throws IOException {
        if (this.map.mapId == 0) {
            Message m = new Message(55);
            m.writer().writeByte(1);
            m.writer().writeShort(2);
            m.writer().writeByte(-1);
            m.writer().writeByte(0);
            conn.addmsg(m);
            m.cleanup();
        }
        byte zone = m2.reader().readByte();
        if (zone < this.map.maxzone || (conn.p.item.wear[11] != null && (conn.p.item.wear[11].id == 3599
                || conn.p.item.wear[11].id == 3593 || conn.p.item.wear[11].id == 3596))) {
            if (zone != this.map.zoneId) {
                GameMap gameMap = GameMap.getMapById(this.map.mapId)[zone];

                if (gameMap.players.size() >= gameMap.maxplayer) {
                    // "Có lỗi xảy ra khi chuyển map hoặc đã đầy, hãy thử lại sau" -> "Terjadi
                    // kesalahan saat berpindah peta atau peta sudah penuh, silakan coba lagi nanti"
                    Service.send_notice_box(conn,
                            "Terjadi kesalahan saat berpindah peta atau peta sudah penuh, silakan coba lagi nanti.");
                    return;
                }
                MapService.leave(this.map, this);
                this.map = gameMap;
                MapService.enter(this.map, this);
            } else {
                // "Bạn đang ở khu vực này!" -> "Anda sudah berada di area ini!"
                Service.send_notice_box(conn, "Anda sudah berada di area ini!");
            }
        }
    }

    public synchronized boolean update_coin(int coin_exchange) throws IOException {
        String query = "SELECT `coin` FROM `account` WHERE `user` = '" + conn.user + "' LIMIT 1;";
        int coin_old = 0;
        try (Connection connection = SQL.gI().getConnection();
                Statement ps = connection.createStatement();
                ResultSet rs = ps.executeQuery(query)) {
            rs.next();
            coin_old = rs.getInt("coin");
            if (coin_old + coin_exchange < 0) {
                Service.send_notice_box(conn, "Koin tidak cukup");
                return false;
            }
            coin_old += coin_exchange;
            if (ps.executeUpdate(
                    "UPDATE `account` SET `coin` = " + coin_old + " WHERE `user` = '" + conn.user + "'") == 1) {
                connection.commit();
            }
        } catch (SQLException e) {
            Service.send_notice_box(conn, "Terjadi sebuah kesalahan");
        }
        return true;
    }

    public synchronized boolean history_coin(int coin_exchange, String log) throws IOException {
        String query = "INSERT INTO `history_coin` (`user_id`, `user_name`, `name_player` , `coin_change`, `logger`) VALUES ('"
                + this.conn.id + "', '" + this.conn.user + "', '" + this.name + "', '" + coin_exchange + "', '" + log
                + "')";
        try (Connection connection = SQL.gI().getConnection(); Statement statement = connection.createStatement();) {
            if (statement.executeUpdate(query) > 0) {
                connection.commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void down_mount(Message m2) throws IOException {
        byte type = m2.reader().readByte();
        if (type == -1) {
            Message m = new Message(-97);
            m.writer().writeByte(0);
            m.writer().writeByte(-1);
            m.writer().writeShort(this.objectId);
            MapService.sendMsgPlayerInside(this.map, this, m, true);
            m.cleanup();
            mount = null;
            MapService.broadcastMainCharInfo(this.map, this);
            Service.sendMainCharInfo(this);
        }
    }

    public void rest_skill_point() throws IOException {
        // for (int i = 0; i < skill_point.length - 2; i++) {
        // if (skill_point[i] > 0) {
        // kynang += skill_point[i];
        // }
        // }
        short sk_110 = 0;
        for (int i = 0; i < skillPoint.length; i++) {
            if (skillPoint.length - i <= 2) {
                sk_110 += skillPoint[i];
            } else if (skillPoint[i] > 0) {
                skillPoint[i] = 0;
            }
        }
        skillPoint[0] = 1;
        // kynang -= 1;
        kynang = (short) (1 + Level.get_kynang_by_level(level - 1));
        kynang -= sk_110;
        hp = body.getMaxHP();
        mp = body.getMaxMP();
        Service.sendMainCharInfo(this);
        for (int i = 0; i < map.players.size(); i++) {
            Player p0 = map.players.get(i);
            if (p0.objectId != this.objectId && ((Math.abs(p0.x - this.x) < 200 && Math.abs(p0.y - this.y) < 200)
                    || GameMap.is_map__load_board_player(map.mapId))) {
                MapService.send_in4_other_char(p0.map, p0, this);
            }
        }
    }

    public void rest_potential_point() throws IOException {
        point1 = (short) (4 + level);
        point2 = (short) (4 + level);
        point3 = (short) (4 + level);
        point4 = (short) (4 + level);
        // Sesuai attribute_scheme.xlsx: tiemnang = 5 x level (mis. level 160 -> 800)
        tiemnang = (short) (5 * level);
        hp = body.getMaxHP();
        mp = body.getMaxMP();
        Service.sendMainCharInfo(this);
        for (int i = 0; i < map.players.size(); i++) {
            Player p0 = map.players.get(i);
            if (p0.objectId != this.objectId && ((Math.abs(p0.x - this.x) < 200 && Math.abs(p0.y - this.y) < 200)
                    || GameMap.is_map__load_board_player(map.mapId))) {
                MapService.send_in4_other_char(p0.map, p0, this);
            }
        }
    }

    public void checkFullSetTT() {
        if (item.wear[1] != null && item.wear[7] != null && item.wear[6] != null) {
            if (item.wear[1].isTT() && item.wear[7].isTT() && item.wear[6].isTT()) {
                item.wear[1].UpdateOption();
                item.wear[7].UpdateOption();
                item.wear[6].UpdateOption();
            }

        }
        if (item.wear[0] != null && item.wear[3] != null && item.wear[9] != null) {
            if (item.wear[0].isTT() && item.wear[3].isTT() && item.wear[9].isTT()) {
                item.wear[0].UpdateOption();
                item.wear[3].UpdateOption();
                item.wear[9].UpdateOption();
            }

        }
        if (item.wear[8] != null && item.wear[4] != null && item.wear[2] != null && item.wear[8].isTT()
                && item.wear[4].isTT() && item.wear[2].isTT()) {
            if (item.wear[8].isTT() && item.wear[4].isTT() && item.wear[2].isTT()) {
                item.wear[8].UpdateOption();
                item.wear[4].UpdateOption();
                item.wear[2].UpdateOption();
            }
        }
        if (item.wear[1] != null && item.wear[7] != null && item.wear[6] != null) {
            if (!item.wear[1].isTT() || !item.wear[7].isTT() || !item.wear[6].isTT()) {
                item.wear[1].ReUpdateOption();
                item.wear[7].ReUpdateOption();
                item.wear[6].ReUpdateOption();
            }
        }
        if (item.wear[0] != null && item.wear[3] != null && item.wear[9] != null) {
            if (!item.wear[0].isTT() || !item.wear[3].isTT() || !item.wear[9].isTT()) {
                item.wear[0].ReUpdateOption();
                item.wear[3].ReUpdateOption();
                item.wear[9].ReUpdateOption();
            }
        }
        if (item.wear[8] != null && item.wear[4] != null && item.wear[2] != null) {
            if (!item.wear[8].isTT() || !item.wear[4].isTT() || !item.wear[2].isTT()) {
                item.wear[8].ReUpdateOption();
                item.wear[4].ReUpdateOption();
                item.wear[2].ReUpdateOption();
            }
        }
    }

    public void playerWear(Item3 temp3, int index_bag, byte index_wear) throws IOException {
        byte b = -1;
        switch (temp3.type) {
            case 0: {// coat
                b = 1;
                break;
            }
            case 1: {// pant
                b = 7;
                break;
            }
            case 2: {// crown
                b = 6;
                break;
            }
            case 3: {// grove
                b = 2;
                break;
            }
            case 4: {// ring
                if (index_wear == 3 || index_wear == 9) {
                    b = index_wear;
                } else {
                    b = 3;
                }
                break;
            }
            case 5: {// chain
                b = 4;
                break;
            }
            case 6: {// shoes
                b = 8;
                break;
            }
            case 7: {// wing
                b = 10;
                break;
            }
            case 12: {// amulet naga
                b = 22;
                break;
            }
            case 8:
            case 9:
            case 10:
            case 11: { // weapon
                b = 0;
                break;
            }
            case 15: {
                b = 11;
                break;
            }
            case 16: {
                b = 12;
                break;
            }
            case 21: {
                b = 13;
                break;
            }
            case 22: {
                b = 14; // fashion
                break;
            }
            case 23: {
                b = 15;
                break;
            }
            case 24: {
                b = 17;
                break;
            }
            case 25: {
                b = 16;
                break;
            }
            case 26: {
                b = 18;
                break;
            }
            case 27: {
                b = 19;
                break;
            }
            case 28: {
                b = 20;
                break;
            }
            case 29: {
                b = 21;
                break;
            }
        }
        if (b == -1) {
            Service.send_notice_box(conn, "Terjadi kesalahan, silakan coba lagi");
            return;
        }

        if (!ItemManager.getInstance().canUse(this, temp3.id)) {
            Service.send_notice_box(conn, "Item ini hanya untuk guid tertentu");
            return;
        }

        if (item.wear[b] == null) {
            temp3.name = ItemTemplate3.item.get(temp3.id).getName() + " [Terkunci]";
            temp3.updateName();
            item.wear[b] = temp3;
            checkFullSetTT();
            item.remove(3, index_bag, 1);
        } else {
            Item3 buffer = item.wear[b];
            temp3.name = ItemTemplate3.item.get(temp3.id).getName() + " [Terkunci]";
            temp3.updateName();
            item.wear[b] = temp3;
            checkFullSetTT();
            item.remove(3, index_bag, 1);
            if (buffer.id != 3593 && buffer.id != 3599 && buffer.id != 3596) {
                buffer.ReUpdateOption();
                item.add_item_bag3(buffer);
            }
        }
        if (b == 11) {
            fashion = PartFashion.getPart(this);
        }
        if (b == 21) {
            setupMount(item.wear[b]);
            conn.p.map.sendUseMount(this);
            // map.broadcastMount(this);
        }

        item.charInventory(4);
        item.charInventory(7);
        item.charInventory(3);
        Service.sendPlayerWear(this);
        Service.sendMainCharInfo(conn.p);
        MapService.broadcastMainCharInfo(this.map, this);
    }

    public void takeOffMount() throws IOException {
        if (item.wear[21] != null) {

            if (item.get_bag_able() > 0) {
                Item3 buffer = item.wear[21];
                item.wear[21] = null;
                item.add_item_bag3(buffer);
                this.mount = null;
                map.sendUseMount(this);
                Service.sendPlayerWear(this);
                MapService.broadcastMainCharInfo(map, this);
                item.charInventory(3);
            } else {
                Service.send_notice_box(conn, "Tas penuh!");
            }
        } else {
            this.mount = null;
            Service.sendMainCharInfo(this);
            MapService.broadcastMainCharInfo(map, this);
        }
    }

    public void plus_point(Message m) throws IOException {
        byte type = m.reader().readByte();
        byte index = m.reader().readByte();
        short value = 1;
        try {
            value = m.reader().readShort();
        } catch (IOException e) {
        }
        if (isdie || value <= 0) {
            return;
        }
        if (type == 1 || type == 2) {
            if (kynang >= value) {
                // if (skill_point[index] == 0 && skills[index].mLvSkill[0].LvRe > this.level) {
                // Service.send_notice_box(conn, "Level quá thấp!");
                // return;
                // }
                if (skillPoint[index] >= 15) {
                    Service.send_notice_box(conn, "Skill sudah mencapai level maksimal");
                    return;
                }
                if (skills[index].mLvSkill[skillPoint[index]].LvRe > this.level) {
                    Service.send_notice_box(conn, "Minimal level yang dibutuhkan adalah "
                            + skills[index].mLvSkill[skillPoint[index]].LvRe + " untuk bisa meningkatkan skill ini");
                    return;
                }
                if (skillPoint[index] == 0 && (index == 19 || index == 20)) {
                    boolean dont_have_book_skill_110 = true;
                    switch (clazz) {
                        case 0: {
                            if (item.total_item_by_id(3, 4577) > 0 && index == 19) {
                                dont_have_book_skill_110 = false;
                                for (int i = 0; i < item.bag3.length; i++) {
                                    if (item.bag3[i] != null && item.bag3[i].id == 4577) {
                                        item.bag3[i] = null;
                                    }
                                }
                            } else if (item.total_item_by_id(3, 4578) > 0 && index == 20) {
                                dont_have_book_skill_110 = false;
                                for (int i = 0; i < item.bag3.length; i++) {
                                    if (item.bag3[i] != null && item.bag3[i].id == 4578) {
                                        item.bag3[i] = null;
                                    }
                                }
                            }
                            break;
                        }
                        case 1: {
                            if (item.total_item_by_id(3, 4579) > 0 && index == 19) {
                                dont_have_book_skill_110 = false;
                                for (int i = 0; i < item.bag3.length; i++) {
                                    if (item.bag3[i] != null && item.bag3[i].id == 4579) {
                                        item.bag3[i] = null;
                                    }
                                }
                            } else if (item.total_item_by_id(3, 4580) > 0 && index == 20) {
                                dont_have_book_skill_110 = false;
                                for (int i = 0; i < item.bag3.length; i++) {
                                    if (item.bag3[i] != null && item.bag3[i].id == 4580) {
                                        item.bag3[i] = null;
                                    }
                                }
                            }
                            break;
                        }
                        case 2: {
                            if (item.total_item_by_id(3, 4581) > 0 && index == 19) {
                                dont_have_book_skill_110 = false;
                                for (int i = 0; i < item.bag3.length; i++) {
                                    if (item.bag3[i] != null && item.bag3[i].id == 4581) {
                                        item.bag3[i] = null;
                                    }
                                }
                            } else if (item.total_item_by_id(3, 4582) > 0 && index == 20) {
                                dont_have_book_skill_110 = false;
                                for (int i = 0; i < item.bag3.length; i++) {
                                    if (item.bag3[i] != null && item.bag3[i].id == 4582) {
                                        item.bag3[i] = null;
                                    }
                                }
                            }
                            break;
                        }
                        case 3: {
                            if (item.total_item_by_id(3, 4583) > 0 && index == 19) {
                                dont_have_book_skill_110 = false;
                                for (int i = 0; i < item.bag3.length; i++) {
                                    if (item.bag3[i] != null && item.bag3[i].id == 4583) {
                                        item.bag3[i] = null;
                                    }
                                }
                            } else if (item.total_item_by_id(3, 4584) > 0 && index == 20) {
                                dont_have_book_skill_110 = false;
                                for (int i = 0; i < item.bag3.length; i++) {
                                    if (item.bag3[i] != null && item.bag3[i].id == 4584) {
                                        item.bag3[i] = null;
                                    }
                                }
                            }
                            break;
                        }
                    }
                    if (dont_have_book_skill_110 && conn.ac_admin < 4) {
                        Service.send_notice_box(conn, "Belum ada buku skill untuk dipelajari!");
                        return;
                    }
                    item.charInventory(3);
                }
                if (skillPoint[index] + value > skills[index].mLvSkill.length - 5) {
                    value = (short) (skills[index].mLvSkill.length - 5 - skillPoint[index]);
                    kynang -= value;
                    skillPoint[index] = (byte) (skills[index].mLvSkill.length - 5);
                } else {
                    kynang -= value;
                    skillPoint[index] += value;
                }
                while (skillPoint[index] > 0 && this.level < (skills[index].mLvSkill[skillPoint[index]].LvRe)) {
                    kynang += 1;
                    skillPoint[index] -= 1;
                }
                CheckSkillPoint();
                MapService.broadcastMainCharInfo(this.map, this);
                Service.sendMainCharInfo(this);
            }
        } else if (type == 0) {
            if (tiemnang >= value) {
                switch (index) {
                    case 0: {
                        if ((point1 + value) <= 32000) {
                            point1 += value;
                            tiemnang -= value;
                        }
                        break;
                    }
                    case 1: {
                        if ((point2 + value) <= 32000) {
                            point2 += value;
                            tiemnang -= value;
                        }
                        break;
                    }
                    case 2: {
                        if ((point3 + value) <= 32000) {
                            point3 += value;
                            tiemnang -= value;
                        }
                        break;
                    }
                    case 3: {
                        if ((point4 + value) <= 32000) {
                            point4 += value;
                            tiemnang -= value;
                        }
                        break;
                    }
                }
                MapService.broadcastMainCharInfo(this.map, this);
                Service.sendMainCharInfo(this);
            }
        }
    }

    public void friend_process(Message m2) throws IOException {
        byte type = m2.reader().readByte();
        String name = m2.reader().readUTF();
        switch (type) {
            case 0: { // request friend -> permintaan pertemanan
                for (Friend name0 : list_friend) {
                    if (name0.name.equals(name)) {
                        // (name + " đã có trong danh sách bạn bè!") -> (name + " sudah ada di daftar
                        // teman!")
                        Service.send_notice_box(conn, (name + " sudah ada di daftar teman!"));
                        return;
                    }
                }
                Player p0 = GameMap.get_player_by_name(name);
                if (p0 == null) {
                    // "Có lỗi xảy ra, hãy thử lại!" -> "Terjadi kesalahan, silakan coba lagi!"
                    Service.send_notice_box(conn, "Terjadi kesalahan, silakan coba lagi!");
                } else {
                    Message m = new Message(35);
                    m.writer().writeByte(0);
                    m.writer().writeUTF(this.name);
                    p0.conn.addmsg(m);
                    m.cleanup();
                }
                break;
            }
            case 1: { // accept -> terima
                Player p0 = GameMap.get_player_by_name(name);
                if (p0 == null) {
                    // "Có lỗi xảy ra, hãy thử lại!" -> "Terjadi kesalahan, silakan coba lagi!"
                    Service.send_notice_box(conn, "Terjadi kesalahan, silakan coba lagi!");
                } else {
                    boolean is_fr = false;
                    for (int i = 0; i < list_friend.size(); i++) {
                        if (list_friend.get(i).name.equals(name)) {
                            is_fr = true;
                            break;
                        }
                    }
                    if (!is_fr) {
                        Friend temp = new Friend();
                        temp.name = p0.name;
                        temp.level = p0.level;
                        temp.head = p0.head;
                        temp.hair = p0.hair;
                        temp.eye = p0.eye;
                        temp.itemwear = new ArrayList<>();
                        for (int i = 0; i < p0.item.wear.length; i++) {
                            Item3 it = p0.item.wear[i];
                            if (it != null && (i == 0 || i == 1 || i == 6 || i == 7 || i == 10)) {
                                PlayerPart part = new PlayerPart();
                                part.type = it.type;
                                part.part = it.part;
                                temp.itemwear.add(part);
                            }
                        }
                        list_friend.add(temp);
                        //
                        Message m = new Message(35);
                        m.writer().writeByte(1);
                        m.writer().writeUTF(temp.name);
                        m.writer().writeByte(temp.head);
                        m.writer().writeByte(temp.eye);
                        m.writer().writeByte(temp.hair);
                        m.writer().writeShort(temp.level);
                        m.writer().writeByte(temp.itemwear.size()); // part
                        for (PlayerPart part : temp.itemwear) {
                            m.writer().writeByte(part.part);
                            m.writer().writeByte(part.type);
                        }
                        m.writer().writeByte(1); // type onl
                        if (p0.myclan != null) {
                            m.writer().writeShort(p0.myclan.icon);
                            m.writer().writeUTF(p0.myclan.shortName);
                            m.writer().writeByte(p0.myclan.get_mem_type(p0.name));
                        } else {
                            m.writer().writeShort(-1); // clan
                        }
                        conn.addmsg(m);
                        m.cleanup();
                        // //
                        temp = new Friend();
                        temp.name = this.name;
                        temp.level = level;
                        temp.head = head;
                        temp.hair = hair;
                        temp.eye = eye;
                        temp.itemwear = new ArrayList<>();
                        for (int i = 0; i < item.wear.length; i++) {
                            Item3 it = item.wear[i];
                            if (it != null && (i == 0 || i == 1 || i == 6 || i == 7 || i == 10)) {
                                PlayerPart part = new PlayerPart();
                                part.type = it.type;
                                part.part = it.part;
                                temp.itemwear.add(part);
                            }
                        }
                        //
                        p0.list_friend.add(temp);
                        //
                        m = new Message(35);
                        m.writer().writeByte(1);
                        m.writer().writeUTF(temp.name);
                        m.writer().writeByte(temp.head);
                        m.writer().writeByte(temp.eye);
                        m.writer().writeByte(temp.hair);
                        m.writer().writeShort(temp.level);
                        m.writer().writeByte(temp.itemwear.size()); // part
                        for (PlayerPart part : temp.itemwear) {
                            m.writer().writeByte(part.part);
                            m.writer().writeByte(part.type);
                        }
                        m.writer().writeByte(1); // type onl
                        if (this.myclan != null) {
                            m.writer().writeShort(this.myclan.icon);
                            m.writer().writeUTF(this.myclan.shortName);
                            m.writer().writeByte(this.myclan.get_mem_type(this.name));
                        } else {
                            m.writer().writeShort(-1); // clan
                        }
                        p0.conn.addmsg(m);
                        m.cleanup();
                    } else {
                        Service.send_notice_box(conn, name + " sudah menjadi teman");
                    }
                }
                break;
            }
            case 2: {
                Player p0 = GameMap.get_player_by_name(name);
                if (p0 == null) {
                    Service.send_notice_box(conn, "Terjadi kesalahan, silakan coba lagi!");
                } else {
                    Service.send_notice_box(p0.conn, (conn.p.name + " menolak permintaan pertemanan Anda haha!"));
                }
                break;
            }
            case 3: { // remove friend
                for (int i = 0; i < list_friend.size(); i++) {
                    Friend temp = list_friend.get(i);
                    if (temp.name.equals(name)) {
                        list_friend.remove(temp);
                        break;
                    }
                }
                break;
            }
            case 4: {
                Friend.send_list_friend(this);
                break;
            }
        }
    }

    public int get_pramskill_byid(byte index_skill, byte id_param) {
        int param = 0;
        for (Option temp : skills[index_skill].mLvSkill[body.getSkillPoint(index_skill) - 1].minfo) {
            if (temp.id == id_param) {
                param += temp.getParam(0);
            }
        }
        return param;
    }

    public void set_in4() throws IOException {
        id_henshin = -1;
        this.already_setup = true;
        time_use_item_arena = System.currentTimeMillis() + 250_000L;
        skills =  Manager.gI().getSkillByClazz(clazz);
        //load_skill();
        // try{
        // CheckSkillPoint();
        // }catch(Exception e){
        // e.printStackTrace();
        // }
        CheckSkillPoint();
        suckhoe = 30000;
        typepk = -1;
        pointpk = 0;
        hp = body.getMaxHP();
        mp = body.getMaxMP();
        fashion = PartFashion.getPart(this);
        id_item_rebuild = -1;
        is_use_mayman = false;
        id_use_mayman = -1;
        item_replace = -1;
        item_replace2 = -1;
        id_buffer_126 = -1;
        id_index_126 = -1;
        id_medal_is_created = -1;
        id_medal_is_created_v2 = -1;
        fusion_material_medal_id = -1;
        id_remove_time_use = -1;
        is_create_wing = false;
        id_wing_split = -1;
        in4_auto = new byte[] { 0, 50, 50, 0, 0, 0, 0, 0, 0, 0, 0 };
        my_store = new ArrayList<>();
        my_store_name = "";
        id_select_mo_ly = -1;
        id_hop_ngoc = -1;
        list_thao_kham_ngoc = new ArrayList<>();
        this.it_wedding = Wedding.get_obj(this.name);
        if (this.it_wedding != null) {
            this.item.wear[23] = this.it_wedding.it;
        }

        //
        GameMap[] gameMap_enter = GameMap.getMapById(map.mapId);
        int d = 0;
        while ((d < (gameMap_enter[d].maxzone - 1)) && gameMap_enter[d].players.size() >= gameMap_enter[d].maxplayer) {
            d++;
        }
        map = gameMap_enter[d];
        //
        this.isChangemap = false;
        this.xOld = this.x;
        this.yOld = this.y;
        //
        HashMap<Short, Integer> hm = new HashMap<>();
        for (Item47 it : item.bag47) {
            if (it.category == 7) {
                if (!hm.containsKey(it.id)) {
                    hm.put(it.id, (int) it.quantity);
                } else {
                    int quant = hm.get(it.id);
                    hm.replace(it.id, quant, quant + it.quantity);
                }
            }
        }
        HashMap<Short, Integer> hm2 = new HashMap<>();
        for (Item47 it : item.bag47) {
            if (it.category == 4) {
                if (!hm2.containsKey(it.id)) {
                    hm2.put(it.id, (int) it.quantity);
                } else {
                    int quant = hm2.get(it.id);
                    hm2.replace(it.id, quant, quant + it.quantity);
                }
            }
        }
        item.bag47.clear();
        for (Entry<Short, Integer> entry : hm.entrySet()) {
            Item47 temp = new Item47();
            temp.category = 7;
            temp.id = entry.getKey();
            int quant_ = entry.getValue();
            temp.quantity = (short) quant_;
            item.bag47.add(temp);
        }
        for (Entry<Short, Integer> entry : hm2.entrySet()) {
            Item47 temp = new Item47();
            temp.category = 4;
            temp.id = entry.getKey();
            int quant_ = entry.getValue();
            temp.quantity = (short) quant_;
            item.bag47.add(temp);
        }
        //
        item.charInventory(4);
        item.char_chest(4);
        item.charInventory(7);
        item.char_chest(7);
        item.charInventory(3);
        item.char_chest(3);
        Log.gI().add_log(this.name,
                "Login : [Vàng] : " + Util.number_format(this.gold) + " : [Ngọc] : "
                        + Util.number_format(this.kimcuong));
    }

    public void update_wings_time() throws IOException {
        boolean check = false;
        for (int i = 0; i < item.bag3.length; i++) {
            Item3 it = item.bag3[i];
            if (it != null && it.type == 7 && it.time_use != 0) {
                if ((it.time_use - System.currentTimeMillis()) <= 0) {
                    it.time_use = 0;
                    check = true;
                }
            }
        }
        if (check) {
            item.charInventory(4);
            item.charInventory(7);
            item.charInventory(3);
        }
    }

    public void change_map_di_buon(Player p) throws IOException {
        p.isChangemap = false;
        GameMap[] mbuffer = GameMap.getMapById(p.map.mapId);
        if (mbuffer != null) {
            MapService.leave(p.map, p);
            p.map = mbuffer[p.map.maxzone];
            MapService.enter(p.map, p);
            if (p.pet_di_buon != null) {
                Message mout = new Message(8);
                mout.writer().writeShort(p.pet_di_buon.objectId);
                for (int i = 0; i < p.map.players.size(); i++) {
                    Player p0 = p.map.players.get(i);
                    if (p0 != null) {
                        p0.conn.addmsg(mout);
                    }
                }
                mout.cleanup();
                //
                Pet_di_buon_manager.remove(p.pet_di_buon.name);
                p.pet_di_buon = null;
            }
        } else {
            Service.send_notice_box(p.conn, "Terjadi kesalahan saat berpindah peta");
        }
    }

    public void show_eff_p(int id_eff, int time) throws IOException {
        Message m = new Message(-49);
        m.writer().writeByte(2);
        m.writer().writeShort(0);
        m.writer().writeByte(0);
        m.writer().writeByte(0);
        m.writer().writeByte(id_eff);
        m.writer().writeShort(this.objectId);
        m.writer().writeByte(0);
        m.writer().writeByte(0);
        m.writer().writeInt(time);
        MapService.sendMsgPlayerInside(this.map, this, m, true);
        m.cleanup();
    }

    /**
     * Tampilkan visual effect combo (id 112) di atas karakter penyerang.
     * Dipanggil saat skill combo (index 19/20) berhasil mengenai target.
     * Wrapper try-catch agar bisa dipanggil dari method non-IOException.
     */
    public void showComboEffect() {
        try {
            show_eff_p(112, 1500);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendNoticeBox(String message) {
        try {
            Service.send_notice_box(conn, message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Nama yang ditampilkan ke pemain lain (nametag di atas karakter, info karakter
     * sendiri).
     * Kalau sedang Top 1-3 Level, otomatis dapat prefix "#1 - ", "#2 - ", "#3 - ".
     * Hilang otomatis begitu keselip (lihat BXH.updateTopLevelBuffItem()).
     * JANGAN dipakai untuk login/lookup DB (where("name", ...), guild member
     * lookup, dll) -
     * di situ tetap pakai field `name` yang asli.
     */
    public String getDisplayName() {
        Integer rank = BXH.getTopLevelRank(objectId);
        if (rank != null) {
            return "#" + (rank + 1) + " - " + name;
        }
        return name;
    }

    /**
     * BUG FIX: nametag pemain Top 1-3 Level dikirim ke client lain lewat
     * getDisplayName()
     * (mis. di MapService.send_in4_other_char), yang menambahkan prefix "#1 - ",
     * "#2 - ", dst.
     * Masalahnya: client TIDAK punya field terpisah untuk "nama asli" vs "nama
     * tampilan" - begitu
     * pemain lain klik nametag itu untuk lihat info / ajak trade / whisper, client
     * mengirim balik
     * PERSIS string yang dia lihat di layar (termasuk prefix "#N - ") sebagai
     * target lookup.
     * Server lalu mencocokkan string itu dengan field `name` asli di DB/objek
     * Player (yang TIDAK
     * ada prefixnya) pakai exact match (name.equals(...)), jadi selalu gagal ketemu
     * -> pemain Top
     * 1-3 kelihatan seperti offline/tidak ditemukan buat pemain lain (persis
     * seperti behaviour
     * "sengaja" yang dipakai buat nyembunyiin info GM), padahal sebenarnya online.
     * <p>
     * Fix: pangkas prefix "#<angka> - " ini di titik-titik server yang menerima
     * nama TARGET dari
     * client (view info pemain lain, trade, whisper/chat tab, dll) SEBELUM dipakai
     * buat lookup,
     * supaya balik ke nama asli. Aman dipanggil untuk nama biasa (tanpa prefix) -
     * akan dikembalikan
     * apa adanya.
     */
    public static String stripDisplayNamePrefix(String rawName) {
        if (rawName == null) {
            return null;
        }
        return rawName.replaceFirst("^#\\d+ - ", "");
    }

    public void setOnline() {
        isOnline = true;
        SQLHelper.update("player")

                .set("active", true)
                .where("name", name)
                .execute();
        // ── Catat sesi login ke DB ───────────────────────────────────────
        this.sessionLogId = SessionLog.gI().onLogin(
                conn.id, objectId, name,
                conn.ip != null ? conn.ip : "",
                map != null ? map.mapId : 0,
                x, y, hp);
        // ── Sinkronkan item buff Top 3 Level (kalau status berubah saat offline) ──
        BXH.syncTopLevelBuffOnLogin(this);
    }

    public void setOffline() {
        // Jika mode AFK aktif, karakter tetap "online" (bot masih berjalan)
        isOnline = modeBot;
        boolean wasInterrupted = Thread.interrupted();
        try {
            SQLHelper.update("player")
                    .set("active", modeBot)
                    .where("name", name)
                    .execute();
            // ── Catat logout / AFK ke session_active ────────────────────────
            if (modeBot) {
                // AFK: bot masih jalan, hapus sesi DB agar tidak dianggap crash
                SessionLog.gI().onLogout(this.sessionLogId, name);
                BXH.loadTopLevel();
                BXH.loadTopBossKill();
                BXH.loadTopRampok();
            } else {
                SessionLog.gI().onLogout(this.sessionLogId, name);
                this.sessionLogId = -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (wasInterrupted)
                Thread.currentThread().interrupt();
        }
    }

    public void openInput(InputDialog input) {
        this.currentInputDialog = input;
        Service.sendInputDialog(conn, input);
    }

    public void openDialog(Dialog confirmDialog) {
        this.currentDialog = confirmDialog;
        Service.sendConfirmDialog(conn, confirmDialog);
    }

    // Set menu with automatic parent tracking
    public void setCurrentMenu(Menu menu) {
        if (menu != null && currentMenu != null) {
            // Only add to history if moving to a different menu
            if (currentMenu.getId() != menu.getId()) {
                menuHistory.push(currentMenu);
            }
        }
        this.currentMenu = menu;
    }

    // Set menu without tracking (for root menus)
    public void setMenuDialogRoot(Menu menu) {
        clearMenuHistory();
        this.currentMenu = menu;
    }

    // Get parent menu (pops from history)
    public Menu getParentMenu() {
        return menuHistory.isEmpty() ? null : menuHistory.peek();
    }

    // Clear all menu state
    public void clearMenuHistory() {
        menuHistory.clear();
        currentMenu = null;
    }

    // Send menu to client
    public void sendMenuDialog(Menu menu) {
        if (menu == null)
            return;
        Service.sendMenu(conn, menu);
    }

    // Close menu dialog
    public void closeMenuDialog() {
        clearMenuHistory();
    }

    // Check if has parent
    public boolean hasParentMenu() {
        return !menuHistory.isEmpty();
    }

    // Navigate back to parent
    public void navigateToParent() {
        if (menuHistory.isEmpty()) {
            closeMenuDialog();
            return;
        }

        // Pop the parent from history (removes it from stack)
        Menu parent = menuHistory.pop();

        // Set current menu directly without adding to history
        this.currentMenu = parent;
        sendMenuDialog(parent);
    }

    public short getLevel() {
        return level;
    }

}