package core;

import BossHDL.BossManager;
import core.lua.JavaToLua;
import equipment.StatGenerator;
import ev_he.Event_2;
import ev_he.Event_3;
import ev_he.MobCay;
import game.event.btf.BTF;
import game.event.btf.BTFState;
import game.event.btf.Team;

import game.event.natal.NpcSanta;
import game.event.spin.GemSpin;
import game.event.Event_1;
import game.event.kemerdekaan.NpcPaskibraka;
import game.event.kemerdekaan.NpcPersimpangan;
import game.event.kemerdekaan.NpcPakLurah;
import game.event.kemerdekaan.NpcNenekMerdeka;
import game.event.halloween.NpcDukun;
import game.event.halloween.NpcBanaspati;
import game.event.halloween.NpcPocong;
import game.event.halloween.NpcGerbangArwah;

import game.npc.NpcCredit;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import game.guild.Guild;
import client.Pet;
import client.Player;
import game.event.GameEventManager;
import game.event.spin.GoldSpin;
import game.event.spin.QuickGemSpin;
import game.event.spin.QuickGoldSpin;
import event_daily.CastleSiegeManager;

import event_daily.DailyQuest;
import event_daily.KingCup;
import event_daily.KingCupManager;
import event_daily.MoLy;
import event_daily.Wedding;
import feature.admin.AdminMenuController;
import client.io.Message;
import client.io.Session;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import game.items.ItemManager;
import game.items.compensation.CompensationManager;
import game.items.compensation.ItemEntry;
import game.items.compensation.RoleItems;
import game.items.models.ItemReward;
import lombok.extern.slf4j.Slf4j;
import game.map.Dungeon;
import game.map.GameMap;
import game.map.MapService;
import model.map.Vgo;

import menu.MenuManager;
import template.Item3;
import template.Item47;
import template.ItemTemplate3;
import template.ItemTemplate4;
import template.ItemTemplate7;
import template.Level;
import template.Medal_Material;
import template.Medal_Material_V2;
import template.MemberBattlefield;
import template.Option;
import template.PetOption;
import template.PartFashion;
import template.Pet_di_buon_manager;
import template.box_item_template;
import utils.CheckItem;

@Slf4j
public class MenuController {

    public static void request_menu(Session conn, int npcId) throws IOException {
        log.debug("MenuController.request_menu() npcID: {}", npcId);
        if (npcId == -43 || npcId == -45 || npcId == -48 || npcId == -46) {
            Menu_ChangeZone(conn);
            return;
        }
        conn.p.isCreateOptions = false;

        String[] menu;
        switch (npcId) {
            case -115: { // NPC Peringkat Kill Boss Premium
                menu = new String[]{"🏆 Top 20 Kill Boss", "⚔ Point Saya"};
                break;
            }
            case -120: {
                menu = new String[]{"Tukarkan Item Natal", "Informasi"};
                break;
            }
            case -121: { // Paskibraka
                menu = new String[]{"Tukar Bendera Merah Putih", "Tukar Kotak Hadiah 17 Agustus", "Informasi"};
                break;
            }
            case -122: { // Pak Lurah
                menu = new String[]{"Klaim Hadiah Lomba 17-an", "Informasi"};
                break;
            }
//            case -123: { // Nenek Merdeka
//                NpcNenekMerdeka.onReceivedMenu(conn, 1);
//                return;
//            }
            case -126: { // Persimpangan Kematian
                menu = new String[]{"Masuk ke Arena", "Informasi Event"};
                break;
            }

            // ── Event Halloween "Malam Suro" 🎃 ─────────────────────────────
            case -132: { // Dukun
                menu = new String[]{"Lakukan Ritual", "Informasi Bahan"};
                break;
            }
            case -131: { // Banaspati
                menu = new String[]{"Rasakan Aura", "Informasi"};
                break;
            }
            case -133: { // Pocong
                menu = new String[]{"Trick or Treat!", "Informasi"};
                break;
            }
            case -134: { // Gerbang Arwah
                menu = new String[]{"Masuk ke Arena", "Informasi Event"};
                break;
            }
            // ─────────────────────────────────────────────────────────────────

            case -96: { // NPC Credit
                menu = new String[]{"Informasi"};
                break;
            } 
            case -110: {
                if (conn.ac_admin <= 0) {
                    Service.send_notice_box(conn, "Tidak ada akses.");
                    return;
                }
                menu = new String[]{
                    "=== BODY EFFECT ===",
                    // Grup: Aura & Swirl (0–8)
                    "Body: Aura Hijau (112_0)",
                    "Body: Aura Hijau Lebat (112_1)",
                    "Body: Spiral Biru (112_2)",
                    "Body: Aura Biru Halus (112_3)",
                    "Body: Api Merah (112_4)",
                    "Body: Api Merah Besar (112_5)",
                    "Body: Sulur Kuning (112_6)",
                    "Body: Sulur Kuning Lebat (112_7)",
                    "Body: Aura Ungu (112_8)",
                    // Grup: Elemental (9–17)
                    "Body: Petir Ungu (112_9)",
                    "Body: Awan Petir (112_10)",
                    "Body: Gelombang Hijau (112_11)",
                    "Body: Pusaran Hijau (112_12)",
                    "Body: Api Putih (112_13)",
                    "Body: Api Oranye (112_14)",
                    "Body: Kristal Merah (112_15)",
                    "Body: Kristal Biru (112_16)",
                    "Body: Partikel Emas (112_17)",
                    // Grup: Cahaya & Bintang (18–26)
                    "Body: Bintang Emas (112_18)",
                    "Body: Bintang Biru (112_19)",
                    "Body: Cahaya Putih (112_20)",
                    "Body: Cahaya Emas (112_21)",
                    "Body: Sinar Merah (112_22)",
                    "Body: Sinar Ungu (112_23)",
                    "Body: Kilau Pink (112_24)",
                    "Body: Kilau Cyan (112_25)",
                    "Body: Aura Pink (112_26)",
                    // Grup: Asap & Debu (27–35)
                    "Body: Asap Hitam (112_27)",
                    "Body: Asap Ungu (112_28)",
                    "Body: Asap Biru (112_29)",
                    "Body: Debu Merah (112_30)",
                    "Body: Debu Emas (112_31)",
                    "Body: Uap Putih (112_32)",
                    "Body: Uap Cyan (112_33)",
                    "Body: Lingkaran Rune (112_34)",
                    "Body: Rune Emas (112_35)",
                    // Grup: Spesial (36–44)
                    "Body: Sayap Kupu (112_36)",
                    "Body: Sayap Kelelawar (112_37)",
                    "Body: Aura Naga (112_38)",
                    "Body: Api Naga (112_39)",
                    "Body: Meteor (112_40)",
                    "Body: Badai Es (112_41)",
                    "Body: Gelombang Laut (112_42)",
                    "Body: Petir Emas (112_43)",
                    "Body: Pusaran Merah (112_44)",
                    // Grup: Premium (45–53)
                    "Body: Aura Sakura (112_45)",
                    "Body: Aura Neon (112_46)",
                    "Body: Aura Perak (112_47)",
                    "Body: Aura Emas Besar (112_48)",
                    "Body: Ice Spirit (112_49)",
                    "Body: Thunder Swirl (112_50)",
                    "Body: Lightning (112_51)",
                    "Body: Cosmic Dust (112_52)",
                    "Body: Galaxy Swirl (112_53)",
                    "=== LEG EFFECT ===",
                    // Grup: Jejak (54–62)
                    "Leg: Jejak Api (112_54)",
                    "Leg: Jejak Es (112_55)",
                    "Leg: Jejak Petir (112_56)",
                    "Leg: Jejak Angin (112_57)",
                    "Leg: Jejak Ungu (112_58)",
                    "Leg: Jejak Emas (112_59)",
                    "Leg: Jejak Pelangi (112_60)",
                    "Leg: Jejak Daun (112_61)",
                    "Leg: Jejak Bintang (112_62)",
                    // Grup: Aura Kaki (63–71)
                    "Leg: Aura Hijau Bawah (112_63)",
                    "Leg: Aura Biru Bawah (112_64)",
                    "Leg: Aura Merah Bawah (112_65)",
                    "Leg: Aura Emas Bawah (112_66)",
                    "Leg: Lingkaran Sihir (112_67)",
                    "Leg: Ice Spirit (112_49)",
                    "Leg: Thunder Swirl (112_50)",
                    "Leg: Lightning (112_51)",
                    "Leg: Galaxy (112_68)",
                    "Leg: Aurora (112_69)",
                    "Leg: Shadow Step (112_70)",
                    "Leg: Cosmic Step (112_71)",
                    "=== RESET ===",
                    "Reset Semua Effect"
                };
                break;
            }
            case 56: {
                menu = new String[]{"Ksatria", "Penyihir", "Assasin", "Penembak"};
                break;
            }
            case -81: { // Mrs. Oda — Lôi Đài (KingCup)
                menu = new String[]{
                    "Pendaftaran King Cup",       // 0
                    "Informasi King Cup",      // 1
                    "Poin King Cup",           // 2
                    "Terima Hadiah Musim",  // 3
                    "Panduan",              // 4
                    "Lepas Permata"           // 5
                };
                break;
            }
            case -3: { // Lisa
                menu = new String[]{"Jual beli", "Buka peti", "Pajak", "Terima hadiah medan perang",
                        "Tutup"};
                break;
            }
            case -20: { // Lisa
                menu = new String[]{"Jual beli", "Buka peti", "Pajak", "Terima hadiah medan perang",
                        "Tutup"};
                break;
            }

            case -5: { // Hammer
                menu = new String[]{"Ksatria", "Assassin", "Penyihir", "Penembak", "Buat equipment bintang",
                        "Upgrade equipment bintang", "Lepas Armor Super ", "Lepas gelar",
                        "Buat Armor Super "};
                break;
            }
            case -77: // Alisama
            case -22: { // Hammer
                menu = new String[]{"Ksatria", "Assassin", "Penyihir", "Penembak"};
                break;
            }
            case -4: {// Doubar
                menu = new String[]{"Ksatria", "Assassin", "Penyihir", "Penembak"};
                break;
            }
            case -33: { // da dich chuyen
                menu = new String[]{"Kota Pelabuhan", "Kota Harta Karun", "Area Perdagangan", "Gurun",
                        "Jurang Tenggelam",
                        "Kuburan Pasir", "Mata Air Hantu", "Makam Lantai 1", "Makam Lantai 3", "Hutan Dataran Tinggi",
                        "Tebing Curam",
                        "Jalan ke Dunia Atas", "Jalan ke Bawah Tanah", "Gerbang Dunia Bawah", "Taman"};
                break;
            }
            case -55: { // da dich chuyen
                menu = new String[]{"Kota Pelabuhan", "Area Perdagangan", "Labirin", "Labirin Lantai 3",
                        "Kota Musim Dingin",
                        "Lembah es", "Kaki gunung salju", "Celah es", "Jurang kabut", "Stasiun gunung salju",
                        "Kota Harta Karun"};
                break;
            }
            case -10: { // da dich chuyen
                menu = new String[]{"Desa Serigala Putih", "Kota Harta Karun", "Mataram Kuno", "Area Perdagangan", "Gua Api",
                        "Hutan Ilusi",
                        "Lembah Misterius", "Danau Kenangan", "Pantai", "Jurang Batu", "Karang Tersembunyi", "Rawa",
                        "Kuil Kuno",
                        "Gua Kelelawar"};
                break;
            }
            case -8: {
                menu = new String[]{"Toko Rambut", "Absen harian", "Tukar coin ke permata", "Tukar coin ke emas",
                        (conn.p.type_exp == 0 ? "Aktifkan" : "Matikan") + " terima exp", "Ganti Password"};
                // if (conn.p.type_exp == 1) {
                // menu = new String[]{"Toko Rambut", "Absen harian", "Tukar coin ke
                // permata", "Tukar coin ke emas",
                // "Daftar afk anti pk", "Waktu tersisa", "Matikan terima exp"};
                // } else {
                // menu = new String[]{"Toko Rambut", "Absen harian", "Tukar coin ke
                // permata", "Tukar coin ke emas",
                // "Daftar afk anti pk", "Waktu tersisa", "Aktifkan terima exp"};
                // }
                break;
            }
            case -36: {

                menu = new String[]{"Perkuat Equipment", "Toko Material", "Transformasi", "Gabung material medal",
                        "Medal Ksatria", "Medal Penyihir", "Medal Assasin", "Medal Penembak",
                        "Upgrade Medal",
                        "Ubah line damage", "Ubah line % damage", "Gabung permata", "Pasang permata", "Buat lubang"};
                break;
            }
            case -44: {
                menu = new String[]{"GiftCode", "Lepas sayap", "Lepas kostum", "Lepas medal",
                        "Lepas topeng",
                        "Lepas sayap fashion", "Lepas jubah", "Lepas rambut fashion", "Lepas senjata fashion",
                        "Lepas headphone fashion", "Lepas Mount (Equipment)", "Terima Kompensasi"};
                // menu = new String[]{"Terima GiftCode", "Lepas sayap", "Lepas kostum", "Lepas
                // rami",
                // "Lepas topeng",
                // "Lepas sayap fashion", "Lepas jubah", "Lepas rambut fashion", "Lepas senjata
                // fashion",
                // "Lepas headphone fashion"};
                break;
            }
            case -32: {
                menu = new String[]{"Lihat Ranking Level", "Lihat Ranking Guild", "Tukar Jubah", "Ranking Lainnya", "🏆 Top Kill Boss"};
                break;
            }
            case -21: { // blackeye
                menu = new String[]{"Ksatria", "Assassin", "Penyihir", "Penembak"};
                break;
            }
            case -90: { // keva
                menu = new String[]{"Shop", "Area Kabut Up", "Map Boss", "Kembali ke Desa Kabut", "Ke Desa "};
                break;
            }
            case -7: {
                if (conn.user.contains("user_")) {
                    menu = new String[]{"Peti penyimpanan", "Buka slot inventory", "Daftar akun"};
                } else {
                    menu = new String[]{"Peti penyimpanan", "Buka slot inventory", "Password peti"};
                }
                break;
            }
            case -34: { // cuop bien
                menu = new String[]{
                        "Epic Gold Spin",
                        "Epic Gem Spin"};
                break;
            }
            case -2: { // zoro
                if (conn.p.myclan != null) {
                    if (conn.p.myclan.members.get(0).name.equals(conn.p.name)) {
                        menu = new String[]{"Kelola guild", "Shop Icon", "Shop Guild", "Buat Jubah", "Buat Title"};
                    } else {
                        menu = new String[]{"Gudang Guild", "Sumbang Emas", "Sumbang Permata", "Keluar guild"};
                    }
                } else {
                    menu = new String[]{"Daftar guild", "Informasi"};
                }
                break;
            }
            case -19: { //
                if (conn.p.myclan != null) {
                    if (conn.p.myclan.members.get(0).name.equals(conn.p.name)) {
                        menu = new String[]{"Kelola guild", "Shop Icon", "Shop Guild"};
                    } else {
                        menu = new String[]{"Gudang Guild", "Sumbang Emas", "Sumbang Permata", "Keluar guild"};
                    }
                } else {
                    menu = new String[]{"Daftar guild", "Informasi"};
                }
                break;
            }
            case -85: { // mr edgar
                menu = new String[]{"Balas Dendam", "Informasi"};
                break;
            }
            case -42: { // pet
                menu = new String[]{"Kandang hewan", "Shop makanan", "Shop telur", "Lepas pet"};
                break;
            }
            case -37: {
                menu = new String[]{"Masuk Persimpangan Kematian", "Pengenalan", "Ranking dungeon", "Daftar siege",
                        "Masuk siege",
                        "Lihat Poin Saat Ini", "Terima hadiah", "Menjadi knight"};
                break;
            }
            case -38:
            case -40: {
                // if (conn.p.dungeon != null && conn.p.dungeon.getWave() == 20) {
                // menu = new String[]{"Lanjutkan penaklukan", "Menyerah", "Panduan"};
                // } else {
                Service.send_notice_box(conn, "Belum ada fitur :(.");
                return;

            }
            case -41: {
                menu = new String[]{"Buat sayap", "Upgrade sayap", "Aktifkan sayap", "Pisah sayap"};
                break;
            }
            case -49: { // menu_top
                menu = new String[]{"Misteri Box", "LIKE", "Fashion Shop", "Starter Pack", "Info Personal ", "Terima hadiah PK", "🏆 Top Kill Boss"};
                break;
            }
            case -82: {
                return;
            }
            // case -81: {
            // menu = new String[]{"Poin Pk"};
            // break;
            // }s
            case -69: {
                switch (Manager.gI().event) {
                    case 1 -> menu = new String[]{"Tukar kotak mainan", "Panduan", "Daftar masak permen",
                            "Masukkan bahan ke panci permen",
                            "Ambil permen yang sudah matang", "Tukar tas permen", "Tukar telur phoenix es",
                            "Tukar telur peri",
                            "Tukar sepatu es", "Tukar topeng es", "Tukar tongkat permen", "Tukar tongkat salju",
                            "Tukar kereta luncur salju",
                            "Tukar telur monyet coklat"};
                    case 2 -> {
                        menu = new String[]{"Nampan buah", "Top event", "Tukar hadiah keberuntungan"};
                        send_menu_select(conn, -69, menu, (byte) Manager.gI().event);
                        return;
                    }
                    case 3 -> {
                        menu = new String[]{"Tukar buket teratai putih", "Tukar bunga teratai merah",
                                "Tukar buket teratai merah", "Lihat top",
                                "Tukar unicorn", "Tukar telur monyet coklat", "Tukar telur peri kecil",
                                "Tukar sayap fashion"};
                        send_menu_select(conn, -69, menu, (byte) Manager.gI().event);
                        return;
                    }
                    default -> {
                        Service.send_notice_box(conn, "Belum ada fitur :(.");
                        return;
                        // menu = new String[]{"Coming soon", infoServer.Website};
                    }
                    // menu = new String[]{"Coming soon", infoServer.Website};
                }
                // menu = new String[]{"Coming soon", infoServer.Website};
                // menu = new String[]{"Coming soon", infoServer.Website};
                break;
            }
            case -62: {
                if (Manager.gI().event == 1) {
                    menu = new String[]{"Percepat memasak", "Panduan", "Informasi", "Top Material"};
                } else {
                    Service.send_notice_box(conn, "Belum ada fitur :(.");
                    return;
                    // menu = new String[]{"Coming soon", infoServer.Website};
                }
                break;
            }
            case -66: {
                if (Manager.gI().event == 1) {
                    menu = new String[]{"Bunga salju", "Bintang", "Bola", "Kartu", "Top dekorasi pohon cemara"};
                } else {
                    Service.send_notice_box(conn, "Belum ada fitur :(.");
                    return;
                }
                break;
            }
            case -57: {
                menu = new String[]{"Jual beli"};
                break;
            }
            case -54: {
                menu = new String[]{"Ke Kota Harta Karun"};
                break;
            }
            case -58: {
                menu = new String[]{"Beli unta", "Jual batu mulia", "Barang pedagang"};
                break;
            }
            case -59: {
                menu = new String[]{"Beli unta", "Jual batu mulia", "Barang perampok"};
                break;
            }
            case -53: {
                menu = new String[]{" Daftar Medan Perang", "Panduan", "Tukar elang", "Masuk Medan Perang"};
                break;
            }
            case 114: { // Anna - Wedding
                menu = new String[]{"Menikah", "Batalkan Pernikahan", "Upgrade Cincin", "Informasi Cincin", "Gudang Pasangan"};
                break;
            }
            case 101: { // Nona Misi - Quest Harian
                menu = new String[]{"Informasi", "Ambil Misi", "Batal Misi", "Setor Misi", "Cek Misi"};
                break;
            }
            default: {
                // System.writer().println("core.MenuController.request_menu()"+idnpc);
                Service.send_notice_box(conn, "Belum ada fitur :(.");
                return;
                // menu = new String[]{"Coming soon", infoServer.Website};
                // break;
            }
        }
        //
        send_menu_select(conn, npcId, menu);
    }

    public static void handleDynamicMenu(Session conn, Message m) throws IOException {
        short npcId = m.reader().readShort();
        byte menuId = m.reader().readByte();
        byte index = m.reader().readByte();

        boolean isHandledByLua = JavaToLua.call("core.LuaBridge.onSelectMenu", new Object[]{conn, npcId, menuId, index});
        if (isHandledByLua) {
            return;
        }

        log.info("DYNAMIC_MENU NPC {} ID {} INDEX {}", npcId, menuId, index);
        if (MenuManager.handleMenuSelection(conn.p, npcId, menuId, index)) {
            return;
        }

        if (npcId == -53) {
            Menu_Mr_Ballard(conn, npcId, menuId, index);
            return;
        }
        if (npcId == -81) {
            Menu_NPC_Turnamen(conn, menuId, index);
            return;
        }
        if (npcId == -56) {
            send_menu_select(conn, 119, new String[]{"Informasi", "Perlindungan", "Pulihkan HP", "Percepat"});
            return;
        }
        // Handler pemilihan difficulty quest harian (menu 999)
        if (npcId == 999) {
            // index: 0=Sangat Mudah, 1=Biasa, 2=Sulit, 3=Sangat Sulit
            try {
                DailyQuest.get_quest(conn.p, index);
            } catch (Exception e) {
                log.error("Error get_quest daily", e);
            }
            return;
        }
        if (npcId >= 30000 && menuId == Manager.gI().event) {
            Menu_MobEvent(conn, npcId, menuId, index);
            return;
        }
        switch (npcId) {
            case -115: // Submenu Top Kill Boss
                if (index == 0) BXH.send(conn, 3);       // Top 20
                else Service.send_notice_box(conn,
                    "Kill Boss kamu: " + BossHDL.BossKillPoint.getPoint(conn.p.name) + " kill");
                break;
            case -120:
                NpcSanta.onReceivedMenu(conn, index);
                break;
            // ── Event Kemerdekaan Indonesia 🇮🇩 ──────────────────────────────
            case -121:
                NpcPaskibraka.onReceivedMenu(conn, index);
                break;
            case -122:
                NpcPakLurah.onReceivedMenu(conn, index);
                break;
            case -123:
                NpcNenekMerdeka.onReceivedMenu(conn, index);
                break;
            case -126:
                NpcPersimpangan.onReceivedMenu(conn, index);
                break;
            // ── Event Halloween "Malam Suro" 🎃 ──────────────────────────────
            case -132:
                NpcDukun.onReceivedMenu(conn, index);
                break;
            case -131:
                NpcBanaspati.onReceivedMenu(conn, index);
                break;
            case -133:
                NpcPocong.onReceivedMenu(conn, index);
                break;
            case -134:
                NpcGerbangArwah.onReceivedMenu(conn, index);
                break;
            // ─────────────────────────────────────────────────────────────────
            case -96:
                NpcCredit.onReceivedMenu(conn, index);
                break;
            // ─────────────────────────────────────────────────────────────────
            case -110: { // Part Char Effect
                if (conn.ac_admin <= 0) return;
                Player p = conn.p;
                // index 0 = header "=== BODY EFFECT ===" → no-op
                // index 1..53  → body effects (112_0..112_52 sesuai urutan menu)
                // index 54 = header "=== LEG EFFECT ===" → no-op
                // index 55..66 → leg effects
                // index 67 = header "=== RESET ===" → no-op
                // index 68 = Reset
                final short[] BODY_IDS = {
                    0,1,2,3,4,5,6,7,8,        // Aura & Swirl
                    9,10,11,12,13,14,15,16,17, // Elemental
                    18,19,20,21,22,23,24,25,26,// Cahaya & Bintang
                    27,28,29,30,31,32,33,34,35,// Asap & Debu
                    36,37,38,39,40,41,42,43,44,// Spesial
                    45,46,47,48,49,50,51,52,53 // Premium
                };
                final short[] LEG_IDS = {
                    54,55,56,57,58,59,60,61,62, // Jejak
                    63,64,65,66,67,49,50,51,68,69,70,71 // Aura Kaki
                };
                if (index == 0 || index == 54 || index == 67) {
                    // header — no-op
                } else if (index >= 1 && index <= 53) {
                    p.bodyEffectId = BODY_IDS[index - 1];
                } else if (index >= 55 && index <= 66) {
                    p.legEffectId = LEG_IDS[index - 55];
                } else if (index == 68) {
                    p.bodyEffectId = -1;
                    p.legEffectId  = -1;
                }
                Service.sendPlayerWear(p);
                p.flush();
                String bodyName = (p.bodyEffectId == -1) ? "NONE" : "112_" + p.bodyEffectId;
                String legName  = (p.legEffectId  == -1) ? "NONE" : "112_" + p.legEffectId;
                Service.send_notice_box(conn, "Body Effect : " + bodyName + "\nLeg Effect  : " + legName);
                break;
            }
            case -501:
                AdminMenuController.processOtherMenu(conn, index);
                break;
            case -500: {
                AdminMenuController.processAdminMenu(conn, index);
                break;
            }
            case 56: {
                AdminMenuController.processShopToken(conn, index);
                break;
            }
            case 4: {
                Menu_DoiDongMeDaySTG(conn, index);
                break;
            }
            case 5: {
                Menu_DoiDongMeDaySTPT(conn, index);
                break;
            }
            case 114: {
                Menu_Wedding(conn, index);
                break;
            }
            case 115: {
                Menu_Thongtincanhan(conn, index);
                break;
            }

            case 117: {
                Menu_ThaoKhamNgoc(conn, index);
                break;
            }
            case -54: {
                Menu_Mr_Haku(conn, index);
                break;
            }
            case -81: {
                // Ditangani via routing di handleDynamicMenu → Menu_NPC_Turnamen
                break;
            }
            case -82: {
                Menu_MissAnwen(conn, index);
                break;
            }
            case 118: {
                Menu_View_LoiDai(conn, index);
                break;
            }

            case 210: {
                Menu_ThayDongCanh_percent(conn, index);
            }
            case 119: {
                Menu_Pet_di_buon(conn, index);
                break;
            }
            case -57: {
                Menu_Mr_Dylan(conn, index);
                break;
            }
            case -58: {
                Menu_Graham(conn, index);
                break;
            }
            case -59: {
                Menu_Mr_Frank(conn, index);
                break;
            }
            case 121: {
                Menu_TachCanh(conn, index);
                break;
            }
            case -3: { // Lisa
                Menu_Lisa(conn, index);
                break;
            }
            case 888: { //
                Menu_BXHCLAN(conn, index);
                break;
            }
            case -20: {
                Menu_Emma(conn, index);
            }
            case -90: { // keva
                Menu_keva(conn, index);
                break;
            }
            case 600: {
                Menu_Langphusuongup(conn, index);
                break;
            }
            case 601: {
                Menu_Langphusuongboss(conn, index);
                break;
            }
            case -4: {
                Menu_Doubar(conn, index, menuId);
                break;
            }
            case 345: {
                Menu_Doiaochoang(conn, index);
                break;
            }
            case 346: {
                Menu_Doiaochoang1(conn, index);
                break;
            }
            case 347: {
                Menu_Doiaochoang2(conn, index);
                break;
            }
            case 348: {
                Menu_Doiaochoang3(conn, index);
                break;
            }
            case 349: {
                Menu_Doiaochoang4(conn, index);
                break;
            }
            case 350: {
                Menu_Doiaochoang5(conn, index);
                break;
            }
            case 351: {
                Menu_Doiaochoang6(conn, index);
                break;
            }
            case 777: {
                Menu_Doiaochoang7(conn, index);
                break;
            }
            case -5: {
                Menu_Hammer(conn, index, menuId);
                break;
            }
            case -22: {
                Menu_Alisama(conn, index, menuId);
                break;
            }
            case -33: {
                Menu_DaDichChuyen33(conn, index);
                break;
            }
            case -55: {
                Menu_DaDichChuyen55(conn, index);
                break;
            }
            case -10: {
                Menu_DaDichChuyen10(conn, index);
                break;
            }
            case 1000: {
                Menu_GiapSieuNhan(conn, index);
                break;
            }

            case 1001: {
                Menu_Quest_Daily(conn, index);
                break;
            }
            case 101: { // Nona Misi — Quest Harian
                Menu_Quest_Daily(conn, index);
                break;
            }
            case -77: {
                Menu_Alisama(conn, index);
                break;
            }
            case -8: {
                Menu_Zulu(conn, index);
                break;
            }
            case 126: {
                Menu_Admin(conn, index);
                break;
            }
            case 127: {
                Menu_Smod(conn, index);
                break;
            }
            case 128 : {
                Menu_Mod(conn, index);
                break;
            }
            case -36: {
                //Menu_PhapSu(conn, index);
                MenuManager.openWizardMenu(conn.p);
                break;
            }
            case -44: {
                menuMissAnna(conn, index);
                break;
            }
            case -32: {
                Menu_BXH(conn, index);
                break;
            }
            case -21: {
                Menu_Black_Eye(conn, index);
                break;
            }

            case -7: {
                Menu_Aman(conn, index);
                break;
            }
            case -34: {
                MenuGamble(conn, index);
                break;
            }
            case 125: {
                menuGambleEmas(conn, index);
                break;
            }
            case 132: {
                menuGamblePermata(conn, index);
                break;
            }
            case 133: {
                menuGambleEmasRegular(conn, index);
                break;
            }
            case 134: {
                menuGamblePermataRegular(conn, index);
                break;
            }
            case -2: {
                Menu_Zoro(conn, index);
                break;
            }
            case -19: {
                Menu_Benjamin(conn, index);
                break;
            }
            case -85: { //
                Menu_Mr_Edgar(conn, index);
                break;
            }
            case 124: {
                Service.revenge(conn, index);
                break;
            }
            case 123: {
                Menu_Dungeon_Mode_Selection(conn, index);
                break;
            }
            case 122: {
                Menu_Clan_Manager(conn, index);
                break;
            }
            case -12: {
                createCloak(conn, index);
                break;
            }
            case -13: {
                createTitle(conn, index);
            }

            case -42: {
                Menu_Pet_Manager(conn, index);
                break;
            }
            case -37: {
                Menu_PhoChiHuy(conn, index);
                break;
            }
            case -38:
            case -40: {
                Menu_LinhCanh(conn, index);
                break;
            }
            case -41: {
                Menu_TienCanh(conn, index);
                break;
            }
            case -49: {
                Menu_top(conn, index);
                break;
            }
            // case -81: {
            // Menu_diempk(conn, index);
            // break;
            // }
            case -69: {
                if (Manager.gI().event == 1) {
                    Menu_Event(conn, index);
                }
                if (Manager.gI().event == 2) {
                    Menu_MissSophia(conn, npcId, menuId, index);
                }
                if (Manager.gI().event == 3) {
                    Menu_MissSophia(conn, npcId, menuId, index);
                }
                break;
            }
            case -62: {
                if (Manager.gI().event == 1) {
                    Menu_NauKeo(conn, index);
                }
                break;
            }
            case -66: {
                if (Manager.gI().event == 1) {
                    Menu_CayThong(conn, index);
                }

                break;
            }
            case 120: {
                break;
            }
            case -91: {

                break;
            }
            case -101: {
                Menu_Krypton(conn, menuId, index);
                break;
            }
            case -103: {

                break;
            }

            default: {
                Service.send_notice_box(conn, "Terjadi kesalahan");
                break;
            }
        }
    }


    private static void Menu_Mr_Ballard(Session conn, int idNPC, byte idmenu, byte index) throws IOException {
        BTF btf = GameEventManager.gI().getEvent(BTF.class);
        if (btf == null) {
            conn.p.sendNoticeBox("Event tidak aktif");
            return;
        }

        switch (idmenu) {
            case 0: {
                switch (index) {
                    case 0: { // dang ky


                        if (btf.getState() == BTFState.REGISTRATION) {
                            btf.registerPlayer(conn.p);
                        } else {
                            Service.send_notice_box(conn, "Tidak dalam waktu pendaftaran");
                        }
                        break;
                    }
                    case 1: {
                        String s = "Daftar di NPC Mr. Ballard di map Hang Lửa 45 menit sebelum waktu mulai";
                        s += "Biaya partisipasi 2 permata atau 500.000 emas";
                        s += "Pemain harus level 40 atau lebih tinggi";
                        s += "Cara bermain :";
                        s += " Pemain akan dibagi rata secara acak menjadi 4 faksi. Saat waktu mendekat, pemain akan dipindahkan ke map konsentrasi.";
                        s += "Tepat pada waktu acara dimulai, map konsentrasi akan membuka pintu untuk pergi ke map lain.";

                        s += "Setiap faksi akan memiliki map konsentrasi sendiri dan setidaknya 1 map markas utama. Ini tergantung pada jumlah pemain yang mendaftar. Maksimal 25 map";

                        s += "Contoh: total pemain yang mendaftar adalah 40 orang - setiap faksi 10 orang - maka setiap faksi akan memiliki 1 markas utama. Jika total pemain yang mendaftar adalah 80 orang - setiap faksi 20 orang - maka setiap faksi akan memiliki 2 markas utama.";

                        s += "\nDi setiap map markas utama, hanya 40 orang yang bisa masuk dalam 1 giliran, dibagi rata menjadi 10 orang per faksi. Artinya, di setiap map markas utama, hanya 10 orang dari faksi yang sama yang bisa masuk.";
                        s += "\nDi setiap map markas utama akan ada 10 penjaga. 10 penjaga ini akan menyerang semua orang dari faksi lain.";
                        s += "\nMap-map markas utama akan terhubung ke 1 map umum. Di map umum, pada menit ke-5 dan menit ke-10, Boss Xà Nữ akan muncul. Jika Boss pertama pada menit ke-5 belum mati hingga menit ke-10, Boss kedua akan tetap muncul. Mengalahkan Boss Xà Nữ akan mendapatkan poin pertempuran.";

                        s += "\n4. Kondisi kemenangan :";
                        s += "Jika belum 60 menit tetapi 3 faksi telah kehilangan semua markas utamanya dan hanya tersisa 1 faksi, maka faksi itu adalah pemenang, dan acara akan berakhir. Hadiahnya adalah total permata pendaftaran dari semua faksi yang dibagi rata di antara semua pemain yang masih hidup di faksi pemenang.";
                        s += "Jika 60 menit telah berakhir dan belum ada faksi yang menang, hadiahnya adalah permata pendaftaran yang dibagi rata di antara semua pemain yang masih hidup di arena.";
                        s += "Pemain harus ingat untuk menemui NPC Lisa atau Emma untuk menerima hadiah.";

                        s += "Poin Pertempuran";
                        s += "Cara mendapatkan poin pertempuran adalah:";
                        s += "Menghancurkan pemain dari faksi lain: 1 poin";
                        s += "Membunuh monster atau penjaga dari faksi lain: 2 poin";
                        s += "Menghancurkan markas lawan: 20 poin";
                        s += "Menggunakan item pertempuran: 10 poin";
                        s += "Mengalahkan Boss Xà Nữ: 30 poin";
                        Service.send_notice_box(conn, s);
                        break;
                    }
                    case 3: {
                        if (btf.getState() == BTFState.FIGHTING) {
                            if (conn.p.time_use_item_arena > System.currentTimeMillis()) {
                                Service.send_notice_box(conn,
                                        "Mohon tunggu setelah "
                                                + (conn.p.time_use_item_arena - System.currentTimeMillis()) / 1000
                                                + " detik");
                                return;
                            }
                            Team team = btf.getPlayerTeam(conn.p.objectId);
                            if (team == null) {
                                conn.p.sendNoticeBox("Kamu tidak terdaftar");
                                return;
                            }

                            conn.p.typepk = team.getFlag();
                            Vgo vgo = team.getLocation();
                            vgo.x = conn.p.x;
                            vgo.y = conn.p.y;
                            conn.p.changeMap(conn.p, vgo);

                        } else {
                            Service.send_notice_box(conn, "Tidak dalam waktu event");
                        }
                        break;
                    }
                    case 2: {
                        if (conn.p.pointarena < 5000) {
                            Service.send_notice_box(conn,
                                    "Perlu minimal 5000 poin akumulasi medan perang untuk bisa tukar telur elang.");
                        } else if (conn.p.item.get_bag_able() < 1) {
                            Service.send_notice_box(conn, "Perlu minimal 1 slot kosong untuk bisa tukar.");
                        } else {
                            try (Connection connection = SQL.gI().getConnection(); Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(
                                    "SELECT * FROM `history_doi_dai_bang` WHERE `user` = '" + conn.user
                                            + "' AND `time` >= DATE_SUB(NOW(), INTERVAL 1 WEEK);")) {
                                if (rs.next()) {
                                    Service.send_notice_box(conn,
                                            "Dalam 1 minggu 1 akun hanya bisa tukar 1 kali.");
                                    return;
                                } else {
                                    int last_point = conn.p.pointarena;
                                    short iditem = 3269;
                                    Item3 itbag = new Item3();
                                    itbag.id = iditem;
                                    itbag.name = ItemTemplate3.item.get(iditem).getName();
                                    itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                                    itbag.type = ItemTemplate3.item.get(iditem).getType();
                                    itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                                    itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                                    itbag.op = new ArrayList<>();
                                    itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                                    itbag.color = ItemTemplate3.item.get(iditem).getColor();
                                    itbag.part = ItemTemplate3.item.get(iditem).getPart();
                                    itbag.tier = 0;
                                    itbag.islock = false;
                                    itbag.time_use = 0;
                                    conn.p.item.add_item_bag3(itbag);
                                    conn.p.pointarena -= 1200;
                                    conn.p.item.charInventory(3);
                                    String query = "INSERT INTO `history_doi_dai_bang` (`user`, `name_player`, `last_point` , `point_arena`) VALUES ('"
                                            + conn.user + "', '" + conn.p.name + "', '" + last_point + "', '"
                                            + conn.p.pointarena + "')";
                                    if (st.executeUpdate(query) > 0) {
                                        connection.commit();
                                    }
                                    List<box_item_template> ids = new ArrayList<>();
                                    ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                                    Service.Show_open_box_notice_item(conn.p, "Anda mendapat", ids);
                                }
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }

                        }
                        break;

                    }
                    default:
                        Service.send_notice_box(conn, "Fitur sedang disempurnakan.");
                        break;
                }
                break;
            }
            case 1: {
                Service.send_notice_box(conn, "Fitur sedang disempurnakan.");
                break;
            }
            // }
        }

    }

    private static void Menu_NPC_Turnamen(Session conn, byte idmenu, byte index) throws IOException {
        if (idmenu != 0) {
            Service.send_notice_box(conn, "Fitur belum tersedia.");
            return;
        }
        switch (index) {
            case 0: { // Đăng ký Lôi Đài
                if (!KingCup.running) {
                    Service.send_notice_box(conn, "King Cup belum di buka.\nKembali lagi nanti!");
                    return;
                }
                if (KingCupManager.list_name.contains(conn.p.name)) {
                    Service.send_notice_box(conn, "Anda sudah mendaftar di King Cup musim ini!");
                    return;
                }
                KingCupManager.register(conn.p);
                break;
            }
            case 1: { // Thông tin Lôi Đài
                String status = KingCup.running ? "Sedang berlangsung" : "Belum buka";
                int turn = KingCupManager.TURN_KING_CUP;
                int maxTurn = KingCupManager.MAX_TURN;
                Service.send_notice_box(conn,
                    "=== King Cup ===\n"
                    + "Status : " + status + "\n"
                    + "Giliran       : " + turn + " / " + maxTurn + "\n"
                    + "Jumlah Grup :\n"
                    + "  Lv 60-100  : " + KingCupManager.group_60_100.size()   + " Player\n"
                    + "  Lv 101-160 : " + KingCupManager.group_101_160.size()   + " Player\n"
                    + "  Lv 161-200 : " + KingCupManager.group_161_200.size()   + " Player\n"
                    + "  Lv 201-230 : " + KingCupManager.group_201_230.size()  + " Player\n"
                    + "  Lv 231-250 : " + KingCupManager.group_231_250.size() + " Player\n"
                    + "  Lv 251-300 : " + KingCupManager.group_251_300.size() + " Player"
                );
                break;
            }
            case 2: { // Điểm của tôi
                Service.send_notice_box(conn,
                    "=== Point King Cup ===\n"
                    + "Akumulasi Point : " + conn.p.point_king_cup + " Point\n"
                    + "Grup   : " + (conn.p.group_king_cup == -1 ? "Belum terdaftar" : "Grup " + conn.p.group_king_cup) + "\n"
                    + "Tingkatan Hadiah   : " + (conn.p.type_reward_king_cup == 0 ? "Belum" : "Tingkatan " + conn.p.type_reward_king_cup)
                );
                break;
            }
            case 3: { // Nhận thưởng mùa giải
                KingCupManager.rewardKingCup(conn.p);
                break;
            }
            case 4: { // Hướng dẫn
                Service.send_notice_box(conn,
                    "=== Panduan King Cup ===\n"
                    + "1. Pendaftaran dibuka saat King Cup berlangsung, biaya 1000 permata, Lv 60+\n"
                    + "2. Setiap musim ada " + KingCupManager.MAX_TURN + " akumulasi\n"
                    + "3. Setiap putaran sistem berpasangan secara acak\n"
                    + "4. Setiap pertandingan: Best of 3 ronde, durasi 5 menit/ronde\n"
                    + "5. Memenangkan pertandingan = +30 poin, tidak ada lawan = +30 poin\n"
                    + "6. Di akhir musim, rangking berdasarkan skor dan terima hadiah di Ny.Oda\n"
                    + "Catatan: Harus berada di Map Tunggu (Map 100) saat ronde dimulai!"
                );
                break;
            }
            case 5: { // Lepas permata
                Service.send_box_UI(conn, 18);
                break;
            }
            default:
                Service.send_notice_box(conn, "Fitur belum tersedia.");
                break;
        }
    }

    private static void Menu_MissSophia(Session conn, int idNPC, byte idmenu, byte index) throws IOException {
        // System.writer().println("core.MenuController.Menu_MissSophia() id: "+idmenu);
        // System.writer().println("core.MenuController.Menu_MissSophia() idx: "+index);
        // System.writer().println("core.MenuController.Menu_MissSophia() ev:
        // "+Manager.gI().event);
        if (idmenu == 2 && Manager.gI().event == 2) {
            switch (index) {
                case 0: {
                    if (conn.p.level < 40) {
                        Service.send_notice_box(conn, "Levelnya terlalu rendah.");
                        return;
                    }
                    if (conn.p.item.get_bag_able() < 4) {
                        Service.send_notice_box(conn, "Inventory penuh");
                        return;
                    }
                    if (conn.p.item.total_item_by_id(4, 141) < 1 && (!Manager.BuffAdminMaterial || conn.ac_admin < 4)) {
                        Service.send_notice_box(conn, "Kekurangan " + ItemTemplate4.item.get(141).getName());
                        return;
                    }
                    for (int i = 254; i <= 258; i++) {
                        if (conn.p.item.total_item_by_id(4, i) < 1
                                && (!Manager.BuffAdminMaterial || conn.ac_admin < 4)) {
                            Service.send_notice_box(conn, "Kekurangan " + ItemTemplate4.item.get(i).getName());
                            return;
                        }
                    }

                    conn.p.item.remove(4, 141, 1);
                    for (int i = 254; i <= 258; i++) {
                        conn.p.item.remove(4, i, 1);
                    }
                    List<box_item_template> ids = new ArrayList<>();

                    List<Integer> it7 = new ArrayList<>(Arrays.asList(0, 1, 4, 8, 9, 10, 11, 12, 13, 14));
                    List<Integer> it7_vip = new ArrayList<>(Arrays.asList(33, 346, 347, 349));
                    List<Integer> it4 = new ArrayList<>(Arrays.asList(2, 5, 61, 67, 269));
                    List<Integer> it4_vip = new ArrayList<>(Arrays.asList(131, 123, 132, 133, 52, 235, 147));
                    for (int i = 0; i < Util.random(1, 5); i++) {
                        int ran = Util.random(100);
                        if (ran < 0) {
                            short id = Util.random(it7, new ArrayList<>()).shortValue();
                            short quant = (short) Util.random(2, 5);
                            ids.add(new box_item_template(id, quant, (byte) 7));
                            conn.p.item.add_item_bag47(id, quant, (byte) 7);
                        } else if (ran < 2) {
                            short idsach = (short) Util.random(4577, 4585);
                            ids.add(new box_item_template(idsach, (short) 1, (byte) 3));
                            conn.p.item.add_item_bag3_default(idsach, 0, false);
                        } else if (ran < 6) {
                            short idsach = (short) 4762;
                            ids.add(new box_item_template(idsach, (short) 1, (byte) 3));
                            conn.p.item.add_item_bag3_default(idsach, Util.random(10, 20), true);
                        } else if (ran < 14) {
                            short id = (short) Util.random(46, 246);
                            short quant = (short) 1;
                            ids.add(new box_item_template(id, quant, (byte) 7));
                            conn.p.item.add_item_bag47(id, quant, (byte) 7);
                        } else if (ran < 24) {
                            short id = (short) Util.random(417, 464);
                            short quant = (short) Util.random(3);
                            ids.add(new box_item_template(id, quant, (byte) 7));
                            conn.p.item.add_item_bag47(id, quant, (byte) 7);
                        } else if (ran < 41) {
                            short id = Util.random(it7_vip, new ArrayList<>()).shortValue();
                            short quant = (short) Util.random(1, 2);
                            ids.add(new box_item_template(id, quant, (byte) 7));
                            conn.p.item.add_item_bag47(id, quant, (byte) 7);
                        } else if (ran < 57) {
                            short id = Util.random(it4_vip, new ArrayList<>()).shortValue();
                            short quant = (short) Util.random(1, 2);
                            ids.add(new box_item_template(id, quant, (byte) 4));
                            conn.p.item.add_item_bag47(id, quant, (byte) 4);
                        } else if (ran < 77) {
                            short id = Util.random(it4, new ArrayList<>()).shortValue();
                            short quant = (short) Util.random(2, 5);
                            ids.add(new box_item_template(id, quant, (byte) 4));
                            conn.p.item.add_item_bag47(id, quant, (byte) 4);
                        } else {
                            short id = Util.random(it7, new ArrayList<>()).shortValue();
                            short quant = (short) Util.random(2, 5);
                            ids.add(new box_item_template(id, quant, (byte) 7));
                            conn.p.item.add_item_bag47(id, quant, (byte) 7);
                        }
                    }
                    Event_2.add_caythong(conn.p.name, 1);
                    Service.Show_open_box_notice_item(conn.p, "Kamu menerima", ids);
                    break;
                }
                case 1: {
                    send_menu_select(conn, 120, Event_2.get_top());
                    break;
                }
                case 2: {
                    if (conn.p.item.get_bag_able() < 1) {
                        Service.send_notice_box(conn, "Inventory penuh");
                        return;
                    }
                    if (conn.p.item.total_item_by_id(4, 123) < 5) {
                        Service.send_notice_box(conn, "Perlu minimal 5 lonceng emas");
                        return;
                    }
                    List<box_item_template> ids = new ArrayList<>();
                    conn.p.item.remove(4, 123, 5);
                    List<Integer> it = new ArrayList<>(Arrays.asList(4612, 4632, 4633, 4634, 4635));
                    List<Integer> it4 = new ArrayList<>(Arrays.asList(299, 205, 207));
                    if (Util.random(100) < 60) {
                        short id = Util.random(it4, new ArrayList<>()).shortValue();
                        short quant = (short) Util.random(1, 3);
                        ids.add(new box_item_template(id, quant, (byte) 4));
                        conn.p.item.add_item_box47(id, quant, (byte) 4);
                    } else {
                        short id = Util.random(it, new ArrayList<>()).shortValue();
                        ids.add(new box_item_template(id, (short) 1, (byte) 3));
                        conn.p.item.add_item_bag3_default(id, Util.random(5, 7), true);
                    }

                    Service.Show_open_box_notice_item(conn.p, "Kamu menerima", ids);
                    break;
                }
                default:
                    Service.send_notice_box(conn, "Belum ada fitur ev2!");
                    break;
            }
        } else if (idmenu == 3 && Manager.gI().event == 3) {
            switch (index) {
                case 0: {
                    Service.sendBoxInputText(conn, 25, "Tukar buket lotus putih",
                            new String[]{"30 lotus putih + 100k emas"});
                    break;
                }
                case 1: {
                    Service.sendBoxInputText(conn, 26, "Tukar bunga lotus merah muda",
                            new String[]{"10 lotus putih + 25k emas"});
                    break;
                }
                case 2: {
                    Service.sendBoxInputText(conn, 27, "Tukar buket lotus merah muda",
                            new String[]{"5 lotus merah muda + 100 permata"});
                    break;
                }
                case 3: {
                    send_menu_select(conn, 120, Event_3.get_top());
                    break;
                }
                case 4: {
                    if (conn.p.getGem() < 100 || conn.p.item.total_item_by_id(4, 304) < 10) {
                        Service.send_notice_box(conn,
                                "Perlu minimal 100 permata dan 10 bunga teratai merah untuk tukar!");
                        return;
                    }
                    if (conn.p.item.get_bag_able() < 1) {
                        Service.send_notice_box(conn, "Không đủ ô trống!");
                        return;
                    }
                    conn.p.updateGem(-100);
                    conn.p.item.remove(4, 304, 10);
                    Item47 itbag = new Item47();
                    itbag.id = 246;
                    itbag.quantity = (short) 100;
                    itbag.category = 4;
                    conn.p.item.add_item_bag47(4, itbag);
                    conn.p.item.charInventory(5);

                    Service.Show_open_box_notice_item(conn.p, "Kamu menerima", new short[]{246}, new int[]{100},
                            new short[]{4});
                    break;
                }
                case 5: {
                    if (conn.p.getGem() < 200 || conn.p.item.total_item_by_id(4, 304) < 50) {
                        Service.send_notice_box(conn,
                                "Perlu minimal 200 permata dan 50 bunga teratai merah untuk tukar!");
                        return;
                    }
                    if (conn.p.item.get_bag_able() < 1) {
                        Service.send_notice_box(conn, "Tidak cukup ruang!");
                        return;
                    }
                    conn.p.updateGem(-200);
                    conn.p.item.remove(4, 304, 50);
                    short iditem = 3616;
                    Item3 itbag = new Item3();
                    itbag.id = iditem;
                    itbag.name = ItemTemplate3.item.get(iditem).getName();
                    itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                    itbag.type = ItemTemplate3.item.get(iditem).getType();
                    itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                    itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                    itbag.op = new ArrayList<>();
                    itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                    itbag.color = ItemTemplate3.item.get(iditem).getColor();
                    itbag.part = ItemTemplate3.item.get(iditem).getPart();
                    itbag.tier = 0;
                    itbag.islock = false;
                    itbag.time_use = 0;
                    itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 15;
                    conn.p.item.add_item_bag3(itbag);
                    conn.p.item.charInventory(5);

                    List<box_item_template> ids = new ArrayList<>();
                    ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                    Service.Show_open_box_notice_item(conn.p, "Kamu menerima", ids);
                    break;
                }
                case 6: {
                    if (conn.p.getGem() < 200 || conn.p.item.total_item_by_id(4, 304) < 50) {
                        Service.send_notice_box(conn,
                                "Perlu minimal 200 permata dan 50 bunga teratai merah untuk tukar!");
                        return;
                    }
                    if (conn.p.item.get_bag_able() < 1) {
                        Service.send_notice_box(conn, "Tidak cukup ruang!");
                        return;
                    }
                    conn.p.updateGem(-200);
                    conn.p.item.remove(4, 304, 50);
                    short iditem = 4761;
                    Item3 itbag = new Item3();
                    itbag.id = iditem;
                    itbag.name = ItemTemplate3.item.get(iditem).getName();
                    itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                    itbag.type = ItemTemplate3.item.get(iditem).getType();
                    itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                    itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                    itbag.op = new ArrayList<>();
                    itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                    itbag.color = ItemTemplate3.item.get(iditem).getColor();
                    itbag.part = ItemTemplate3.item.get(iditem).getPart();
                    itbag.tier = 0;
                    itbag.islock = false;
                    itbag.time_use = 0;
                    itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 15;
                    conn.p.item.add_item_bag3(itbag);
                    conn.p.item.charInventory(5);

                    List<box_item_template> ids = new ArrayList<>();
                    ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                    Service.Show_open_box_notice_item(conn.p, "Kamu menerima", ids);
                    break;
                }
                case 7: {
                    if (conn.p.getGem() < 500 || conn.p.item.total_item_by_id(4, 304) < 50) {
                        Service.send_notice_box(conn,
                                "Perlu minimal 500 permata dan 50 bunga teratai merah untuk tukar!");
                        return;
                    }
                    if (conn.p.item.get_bag_able() < 1) {
                        Service.send_notice_box(conn, "Tidak cukup ruang!");
                        return;
                    }
                    conn.p.updateGem(-500);
                    conn.p.item.remove(4, 304, 50);
                    short iditem = 4642;
                    Item3 itbag = new Item3();
                    itbag.id = iditem;
                    itbag.name = ItemTemplate3.item.get(iditem).getName();
                    itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                    itbag.type = ItemTemplate3.item.get(iditem).getType();
                    itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                    itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                    itbag.op = new ArrayList<>();
                    itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                    itbag.color = ItemTemplate3.item.get(iditem).getColor();
                    itbag.part = ItemTemplate3.item.get(iditem).getPart();
                    itbag.tier = 0;
                    itbag.islock = false;
                    itbag.time_use = 0;
                    itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30;
                    conn.p.item.add_item_bag3(itbag);
                    conn.p.item.charInventory(5);

                    List<box_item_template> ids = new ArrayList<>();
                    ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                    Service.Show_open_box_notice_item(conn.p, "Kamu menerima", ids);
                    break;
                }
                default:
                    Service.send_notice_box(conn, "Belum ada fitur ev3!");
                    break;
            }
        } else {
            Service.send_notice_box(conn, "menu: " + idmenu + "  ev: " + Manager.gI().event);
        }

    }

    private static void Menu_MobEvent(Session conn, int idmob, byte idmenu, byte index) throws IOException {
        if (idmenu == 2) {
            if (index != 0) {
                return;
            }
            if (conn.p.level < 40) {
                Service.send_notice_box(conn, "Level 40 diperlukan untuk memainkan event tersebut.");
                return;
            }
            MobCay mob = Event_2.getMob(idmob);
            if (mob == null || !mob.gameMap.equals(conn.p.map)) {
                Message m2 = new Message(17);
                m2.writer().writeShort(-1);
                m2.writer().writeShort(idmob);
                conn.addmsg(m2);
                m2.cleanup();
                Service.send_notice_box(conn, "Tidak ditemukan");
                return;
            }
            if (!(mob.gameMap.equals(conn.p.map) && Math.abs(mob.x - conn.p.x) < 150 && Math.abs(mob.y - conn.p.y) < 150)) {
                Service.send_notice_box(conn, "Jarak terlalu jauh.\nJika benar-benar dekat coba reload map.");
                return;
            }
            if (mob.Owner != null) {
                Service.send_notice_box(conn, "Đã có người khác hái quả.");
                return;
            }
            if (conn.p.item.get_bag_able() < 1) {
                Service.send_notice_nobox_white(conn, "Inventory penuh.");
                return;
            }
            if (conn.p.item.total_item_by_id(4, 252) < 1) {
                Service.send_notice_nobox_white(conn, "Silakan beli keranjang pemetik buah untuk menyimpan.");
                return;
            }
            conn.p.item.remove(4, 252, 1);
            mob.setOwner(conn.p);
            short id = (short) Util.random(254, 259);
            conn.p.item.add_item_bag47(id, (short) 1, (byte) 4);
            conn.p.item.charInventory(4);
            Service.Show_open_box_notice_item(conn.p, "Kamu menerima", new short[]{id}, new int[]{1},
                    new short[]{4});
            // Service.send_notice_box(conn, "Nhận quả: "+mob.nameOwner);
        }
    }

    private static void Menu_Krypton(Session conn, byte idmenu, byte index) throws IOException {
        if (idmenu == 0)// nâng mề dùng đá Krypton
        {
            GameSrc.UpgradeMedal(conn, index);
        } else if (idmenu == 1) {
            GameSrc.UpgradeItemStar(conn, index);
        }
        conn.p.id_Upgrade_Medal_Star = -1;
    }


    private static void Menu_View_LoiDai(Session conn, byte index) throws IOException {
        Service.send_notice_box(conn, "Fitur belum sempurna");
        // LoiDaiManager.gI().JoinMap(conn.p, index);
    }


    private static void Menu_Pet_di_buon(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                String notice = null;
                if (conn.p.pet_di_buon != null && conn.p.pet_di_buon.item.size() > 0) {
                    notice = "%s " + ItemTemplate3.item.get(3590).getName() + "\n";
                    notice += "%s " + ItemTemplate3.item.get(3591).getName() + "\n";
                    notice += "%s " + ItemTemplate3.item.get(3592).getName() + "\n";
                    int n1 = 0, n2 = 0, n3 = 0;
                    for (int i = 0; i < conn.p.pet_di_buon.item.size(); i++) {
                        if (conn.p.pet_di_buon.item.get(i) == 3590) {
                            n1++;
                        } else if (conn.p.pet_di_buon.item.get(i) == 3591) {
                            n2++;
                        } else {
                            n3++;
                        }
                    }
                    notice = String.format(notice, n1, n2, n3);
                } else {
                    notice = "Tong";
                }
                Service.send_notice_box(conn, notice);
                break;
            }
            case 1: {
                break;
            }
            case 2: {
                if (conn.p.getGem() > 5) {
                    conn.p.pet_di_buon.update_hp(conn.p, 100);
                } else {
                    Service.send_notice_box(conn, "Tidak cukup 5 permata");
                }
                break;
            }
            case 3: {
                if (conn.p.getGem() > 5) {
                    conn.p.pet_di_buon.update_speed(conn.p);
                } else {
                    Service.send_notice_box(conn, "Tidak cukup 5 permata");
                }
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Mr_Frank(Session conn, byte index) throws IOException {
        if (conn.p.map.mapId != 17) {
            Service.send_notice_box(conn, "Salah sayang!");
            return;
        }
        switch (index) {
            case 0: {
                Service.send_box_UI(conn, 39);
                break;
            }
            case 1: {
                if (conn.p.pet_di_buon != null && Math.abs(conn.p.pet_di_buon.x - conn.p.x) < 75
                        && Math.abs(conn.p.pet_di_buon.y - conn.p.y) < 75 && conn.p.item.wear[11] != null
                        && conn.p.item.wear[11].id == 3593) {
                    //
                    int vang_recei = 0;
                    for (int i = 0; i < conn.p.pet_di_buon.item.size(); i++) {
                        if (conn.p.pet_di_buon.item.get(i) == 3590) {
                            vang_recei += 50_000;
                        } else if (conn.p.pet_di_buon.item.get(i) == 3591) {
                            vang_recei += 75_000;
                        } else if (conn.p.pet_di_buon.item.get(i) == 3592) {
                            vang_recei += 100_000;
                        }
                    }
                    if (vang_recei > 0) {
                        conn.p.updateGold(vang_recei);
                        conn.p.item.charInventory(5);
                        conn.p.dicuop += (vang_recei / 1000);
                        conn.p.gold_dagang += vang_recei;
                        BXH.loadTopRampok();
                        BXH.loadTopDagang();

                        int totalBarang = conn.p.pet_di_buon.item.size();
                        int bonusGem = 0;
                        short bonusItemId = -1;
                        short bonusItemQty = 0;
                        String bonusMsg = "";

                        if (totalBarang >= 10) {
                            bonusGem = 500;
                            bonusItemId = Medal_Material_V2.m_yellow[Util.random(0, 9)];
                            bonusItemQty = 3;
                            bonusMsg = "\n+ Bonus Perampok Ulung: " + bonusGem + " permata + " + bonusItemQty + "x Material Medal V2 (Kuning)!";
                        } else if (totalBarang >= 6) {
                            bonusGem = 200;
                            bonusItemId = Medal_Material_V2.m_yellow[Util.random(0, 9)];
                            bonusItemQty = 1;
                            bonusMsg = "\n+ Bonus Perampok Mahir: " + bonusGem + " permata + " + bonusItemQty + "x Material Medal V2 (Kuning)!";
                        } else if (totalBarang >= 3) {
                            bonusGem = 50;
                            bonusMsg = "\n+ Bonus Perampok: " + bonusGem + " permata!";
                        }

                        if (bonusGem > 0) {
                            conn.p.updateGem(bonusGem);
                        }
                        if (bonusItemId > 0) {
                            conn.p.item.add_item_bag47(bonusItemId, bonusItemQty, (byte) 7);
                        }

                        Message m = new Message(8);
                        m.writer().writeShort(conn.p.pet_di_buon.objectId);
                        for (int i = 0; i < conn.p.map.players.size(); i++) {
                            Player p0 = conn.p.map.players.get(i);
                            if (p0 != null) {
                                p0.conn.addmsg(m);
                            }
                        }
                        m.cleanup();
                
                        Pet_di_buon_manager.remove(conn.p.pet_di_buon.name);
                        conn.p.pet_di_buon = null;
                        Service.send_notice_box(conn, "Menerima " + vang_recei + " emas!" + bonusMsg);
                    } else {
                        Service.send_notice_box(conn, "Belum berhasil merampok apa pun, sangat payah!");
                    }
                } else {
                    Service.send_notice_box(conn, "Aku tidak melihat hewan dagangmu");
                }
                break;
            }
            case 2: {
                Item3 itbag = new Item3();
                itbag.id = 3593;
                itbag.clazz = ItemTemplate3.item.get(3593).getClazz();
                itbag.type = ItemTemplate3.item.get(3593).getType();
                itbag.level = ItemTemplate3.item.get(3593).getLevel();
                itbag.icon = ItemTemplate3.item.get(3593).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(3593).getOp());
                itbag.color = 5;
                itbag.part = ItemTemplate3.item.get(3593).getPart();
                itbag.tier = 0;
                itbag.islock = true;
                itbag.time_use = 0;
                // thao do
                if (conn.p.item.wear[11] != null && conn.p.item.wear[11].id != 3593 && conn.p.item.wear[11].id != 3599
                        && conn.p.item.wear[11].id != 3596) {
                    Item3 buffer = conn.p.item.wear[11];
                    conn.p.item.wear[11] = null;
                    conn.p.item.add_item_bag3(buffer);
                }
                itbag.name = ItemTemplate3.item.get(3593).getName() + " [Terkunci]";
                itbag.updateName();
                conn.p.item.wear[11] = itbag;
                conn.p.item.charInventory(4);
                conn.p.item.charInventory(7);
                conn.p.item.charInventory(3);
                conn.p.fashion = PartFashion.getPart(conn.p);
                conn.p.change_map_di_buon(conn.p);
                Service.send_notice_box(conn, "Berhasil diterima");
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Graham(Session conn, byte index) throws IOException {
        if (conn.p.map.mapId != 8) {
            return;
        }
        switch (index) {
            case 0: {
                Service.send_box_UI(conn, 32);
                break;
            }
            case 1: {
                if (conn.p.pet_di_buon != null && Math.abs(conn.p.pet_di_buon.x - conn.p.x) < 75
                        && Math.abs(conn.p.pet_di_buon.y - conn.p.y) < 75 && conn.p.item.wear[11] != null
                        && conn.p.item.wear[11].id == 3599) {
                    //
                    int vang_recei = 0;
                    for (int i = 0; i < conn.p.pet_di_buon.item.size(); i++) {
                        if (conn.p.pet_di_buon.item.get(i) == 3590) {
                            vang_recei += 250_000;
                        } else if (conn.p.pet_di_buon.item.get(i) == 3591) {
                            vang_recei += 350_000;
                        } else if (conn.p.pet_di_buon.item.get(i) == 3592) {
                            vang_recei += 450_000;
                        }
                    }
                    if (vang_recei > 0) {
                        conn.p.updateGold(vang_recei);
                        conn.p.item.charInventory(5);
                        conn.p.dicuop += (vang_recei / 1000);
                        conn.p.gold_dagang += vang_recei;
                        BXH.loadTopRampok();
                        BXH.loadTopDagang();

                        // === HADIAH BONUS BERDASARKAN JUMLAH BARANG DAGANGAN ===
                        int totalBarang = conn.p.pet_di_buon.item.size();
                        int bonusGem = 0;
                        short bonusItemId = -1;
                        short bonusItemQty = 0;
                        String bonusMsg = "";

                        if (totalBarang >= 10) {
                            // 10+ barang: gem besar + item bonus
                            bonusGem = 500;
                            bonusItemId = Medal_Material_V2.m_yellow[Util.random(0, 9)];
                            bonusItemQty = 3;
                            bonusMsg = "\n+ Bonus Pedagang Ulung: " + bonusGem + " permata + " + bonusItemQty + "x Material Medal V2 (Kuning)!";
                        } else if (totalBarang >= 6) {
                            // 6-9 barang: gem sedang + 1 item bonus
                            bonusGem = 200;
                            bonusItemId = Medal_Material_V2.m_yellow[Util.random(0, 9)];
                            bonusItemQty = 1;
                            bonusMsg = "\n+ Bonus Pedagang Mahir: " + bonusGem + " permata + " + bonusItemQty + "x Material Medal V2 (Kuning)!";
                        } else if (totalBarang >= 3) {
                            // 3-5 barang: hanya gem kecil
                            bonusGem = 50;
                            bonusMsg = "\n+ Bonus Pedagang: " + bonusGem + " permata!";
                        }
                        // 1-2 barang: hanya gold, tidak ada bonus tambahan

                        if (bonusGem > 0) {
                            conn.p.updateGem(bonusGem);
                        }
                        if (bonusItemId > 0) {
                            conn.p.item.add_item_bag47(bonusItemId, bonusItemQty, (byte) 7);
                        }
                        // =========================================================

                        //
                        Message m = new Message(8);
                        m.writer().writeShort(conn.p.pet_di_buon.objectId);
                        for (int i = 0; i < conn.p.map.players.size(); i++) {
                            Player p0 = conn.p.map.players.get(i);
                            if (p0 != null) {
                                p0.conn.addmsg(m);
                            }
                        }
                        m.cleanup();
                        //
                        Pet_di_buon_manager.remove(conn.p.pet_di_buon.name);
                        conn.p.pet_di_buon = null;
                        Service.send_notice_box(conn, "Menerima " + vang_recei + " emas!" + bonusMsg);
                    } else {
                        Service.send_notice_box(conn, "Kamu belum punya apa-apa, tapi semua barangmu sering dirampok!");
                    }
                } else {
                    Service.send_notice_box(conn, "Aku tidak melihat hewan dagangmu");
                }
                break;
            }
            case 2: {
                Item3 itbag = new Item3();
                itbag.id = 3599;
                itbag.clazz = ItemTemplate3.item.get(3599).getClazz();
                itbag.type = ItemTemplate3.item.get(3599).getType();
                itbag.level = ItemTemplate3.item.get(3599).getLevel();
                itbag.icon = ItemTemplate3.item.get(3599).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(3599).getOp());
                itbag.color = 5;
                itbag.part = ItemTemplate3.item.get(3599).getPart();
                itbag.tier = 0;
                itbag.islock = true;
                itbag.time_use = 0;
                // thao do
                if (conn.p.item.wear[11] != null && conn.p.item.wear[11].id != 3593 && conn.p.item.wear[11].id != 3599
                        && conn.p.item.wear[11].id != 3596) {
                    Item3 buffer = conn.p.item.wear[11];
                    conn.p.item.wear[11] = null;
                    conn.p.item.add_item_bag3(buffer);
                }
                itbag.name = ItemTemplate3.item.get(3599).getName() + " [Terkunci]";
                itbag.updateName();
                conn.p.item.wear[11] = itbag;
                conn.p.item.charInventory(4);
                conn.p.item.charInventory(7);
                conn.p.item.charInventory(3);
                conn.p.fashion = PartFashion.getPart(conn.p);
                conn.p.change_map_di_buon(conn.p);
                Service.send_notice_box(conn, "Berhasil diterima");
                break;
            }
            case 3: {
                BXH.send(conn, 5); // Top Pedagang
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Mr_Dylan(Session conn, byte index) throws IOException {
        if (conn.p.map.mapId != 52) {
            return;
        }

        if (conn.p.item.wear[11] == null || (conn.p.item.wear[11] != null && conn.p.item.wear[11].id != 3599)) {
            Service.send_notice_box(conn, "Kamu bukan pedagang");
            return;
        }
        if (conn.p.pet_di_buon != null && Math.abs(conn.p.pet_di_buon.x - conn.p.x) < 75
                && Math.abs(conn.p.pet_di_buon.y - conn.p.y) < 75) {
            switch (index) {
                case 0: {
                    Service.send_box_UI(conn, 31);
                    break;
                }
                default: {
                    Service.send_notice_box(conn, "Fitur belum tersedia");
                    break;
                }
            }
        } else {
            Service.send_notice_box(conn, "Aku tidak melihat hewan dagangmu");
        }
    }

    private static void Menu_NauKeo(Session conn, byte index) throws IOException {
        if (Manager.gI().event == 1) {
            switch (index) {
                case 0: {

                    if (conn.p.getGem() < 10) {
                        Service.send_notice_box(conn, "Tidak cukup 10 permata");
                        return;
                    }
                    if (Event_1.naukeo.time <= 30) {
                        Service.send_notice_box(conn, "Tidak dapat mempercepat");
                        return;
                    }
                    conn.p.updateGem(-10);
                    conn.p.item.charInventory(5);
                    Event_1.naukeo.update(1);
                    Service.send_notice_box(conn, "Tăng tốc thành công");
                    break;
                }
                case 1: {
                    Service.send_notice_box(conn,
                            "Bahan yang dibutuhkan untuk membuat permen adalah sebagai berikut: Gula, Susu, Mentega, Vanili\r\n"
                                    + "- Setiap hari server mengizinkan pembuatan permen satu kali pada pukul 17:00, dengan waktu memasak 2 jam.\r\n"
                                    + "- Waktu pendaftaran adalah dari pukul 19:00 hari sebelumnya hingga 16:30 hari berikutnya. Biaya pendaftaran adalah 5 permata\r\n"
                                    + "- Sekali mempercepat membutuhkan 10 permata dan akan mengurangi waktu memasak sebanyak 2 menit\r\n"
                                    + "- Jumlah permen maksimal yang bisa diterima adalah 20 permen. Namun, jika para ksatria menyumbang lebih banyak, itu akan lebih menguntungkan karena 10 pemain dengan sumbangan bahan terbanyak akan mendapatkan tambahan 20 permen\r\n"
                                    + "+ Jumlah permen yang diterima akan dihitung dengan rumus 1 Permen = 1 Gula + 1 Susu + 1 Mentega + 1 Vanili");
                    break;
                }
                case 2: {
                    Service.send_notice_box(conn,
                            "Informasi:\nSudah menyumbang: " + Event_1.get_keo_now(conn.p.name)
                                    + "\nWaktu memasak tersisa: "
                                    + ((Event_1.naukeo.time == 0) ? "Tidak dalam waktu memasak"
                                    : ("Tersisa " + Event_1.naukeo.time + " menit")));
                    break;
                }
                case 3: {
                    send_menu_select(conn, 120, Event_1.get_top_naukeo());
                    break;
                }
                default: {
                    Service.send_notice_box(conn, "Fitur belum tersedia");
                    break;
                }
            }
        }
    }

    private static void Menu_Event(Session conn, byte index) throws IOException {
        if (Manager.gI().event == 1) {
            switch (index) {
                case 0: {
                    Service.sendBoxInputText(conn, 10, "Masukkan jumlah", new String[]{"Jumlah :"});
                    break;
                }
                case 1: {
                    Service.send_notice_box(conn,
                            "Untuk menukar Kotak mainan lengkap dengan resep: 20.000 emas + 50 Patung naga + 50 Pedang mainan + 50 Sepatu kecil + 50 Kostum mini + 50 Topi prajurit timah."
                                    + "\nUntuk menukar Kantong permen lengkap dengan resep: 50.000 emas + 5 Permen.");
                    break;
                }
                case 2: {
                    if (!Event_1.check_time_can_register()) {
                        Service.send_notice_box(conn, "Tidak dalam waktu pendaftaran!");
                        return;
                    }
                    if (conn.p.getGem() < 5) {
                        Service.send_notice_box(conn, "Tidak cukup 5 permata");
                        return;
                    }
                    if (Event_1.check(conn.p.name)) {
                        Service.send_notice_box(conn, "Sudah terdaftar, lupa ya!");
                        return;
                    }
                    conn.p.updateGem(-5);
                    conn.p.item.charInventory(5);
                    Event_1.add_material(conn.p.name, 0);
                    Service.send_notice_box(conn, "Pendaftaran berhasil, sekarang bisa menyumbang bahan");
                    break;
                }
                case 3: {
                    if (!Event_1.check_time_can_register()) {
                        Service.send_notice_box(conn, "Tidak dalam waktu pendaftaran!");
                        return;
                    }
                    if (Event_1.check(conn.p.name)) {
                        Service.sendBoxInputText(conn, 11, "Masukkan jumlah", new String[]{"Jumlah :"});
                    } else {
                        Service.send_notice_box(conn, "Belum mendaftar untuk membuat permen, silakan daftar!");
                    }
                    break;
                }
                case 4: {
                    int quant = Event_1.get_keo(conn.p.name);
                    if (quant > 0) {
                        quant = (quant > 20) ? 20 : quant;
                        if (Event_1.list_bxh_naukeo_name.contains(conn.p.name)) {
                            quant += 20;
                        }
                        quant *= 3;
                        Item47 it = new Item47();
                        it.category = 4;
                        it.id = 162;
                        it.quantity = (short) quant;
                        conn.p.item.add_item_bag47(4, it);
                        conn.p.item.charInventory(4);
                        Service.send_notice_box(conn, "Mendapat " + quant + " permen");
                    } else {
                        Service.send_notice_box(conn, "Sudah diambil atau belum ikut serta!");
                    }
                    break;
                }
                case 5: {
                    Service.sendBoxInputText(conn, 12, "Masukkan jumlah", new String[]{"Jumlah :"});
                    break;
                }
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13: {
                    if (conn.p.item.get_bag_able() < 1) {
                        Service.send_notice_box(conn, "Inventory tidak cukup tempat kosong!");
                        return;
                    }
                    short[] id_receiv = new short[]{4626, 4761, 3610, 4636, 4709, 4710, 281, 3616};
                    short[] tuikeo_required = new short[]{120, 120, 60, 60, 30, 30, 15, 60};
                    short[] hopdochoi_required = new short[]{120, 120, 60, 60, 30, 30, 15, 60};
                    int[] ngoc_required = new int[]{360, 330, 60, 60, 60, 60, 15, 300};
                    if (tuikeo_required[index - 6] > conn.p.item.total_item_by_id(4, 157)) {
                        Service.send_notice_box(conn, "Tidak cukup " + tuikeo_required[index - 6] + " kantong permen!");
                        return;
                    }
                    if (hopdochoi_required[index - 6] > conn.p.item.total_item_by_id(4, 158)) {
                        Service.send_notice_box(conn,
                                "Tidak cukup " + hopdochoi_required[index - 6] + " kotak mainan!");
                        return;
                    }
                    if (ngoc_required[index - 6] > conn.p.getGem()) {
                        Service.send_notice_box(conn, "Tidak cukup " + ngoc_required[index - 6] + " permata!");
                        return;
                    }
                    if (index != 12) {
                        Item3 itbag = new Item3();
                        ItemTemplate3 it_temp = ItemTemplate3.item.get(id_receiv[index - 6]);
                        itbag.id = it_temp.getId();
                        itbag.name = it_temp.getName();
                        itbag.clazz = it_temp.getClazz();
                        itbag.type = it_temp.getType();
                        itbag.level = 10;
                        itbag.icon = it_temp.getIcon();
                        itbag.op = new ArrayList<>();
                        itbag.op.addAll(it_temp.getOp());
                        itbag.color = it_temp.getColor();
                        itbag.part = it_temp.getPart();
                        itbag.tier = 0;
                        itbag.islock = false;
                        itbag.time_use = 0;
                        conn.p.item.add_item_bag3(itbag);
                        Service.send_notice_box(conn, "Menerima " + itbag.name + ".");
                    } else {
                        Item47 itbag = new Item47();
                        itbag.id = id_receiv[index - 6];
                        itbag.quantity = (short) 20;
                        itbag.category = 4;
                        conn.p.item.add_item_bag47(4, itbag);
                        Service.send_notice_box(conn, "Menerima 20 papan seluncur salju.");
                    }
                    conn.p.item.remove(4, 157, tuikeo_required[index - 6]);
                    conn.p.item.remove(4, 158, hopdochoi_required[index - 6]);
                    conn.p.updateGem(-ngoc_required[index - 6]);
                    conn.p.item.charInventory(4);
                    conn.p.item.charInventory(3);
                    break;
                }
                default: {
                    Service.send_notice_box(conn, "Sedang dipersiapkan");
                    break;
                }
            }
        }
    }

    // private static void Menu_diempk(Session conn, byte index) throws IOException
    // {
    // switch (index) {
    // case 0: {
    // Service.send_notice_box(conn, "Bạn đang có " + conn.p.hieuchien + " Điểm
    // Pk.");
    // break;
    // }
    // default: {
    // Service.send_notice_box(conn, "Chưa có chức năng");
    // break;
    // }
    // }
    // }
    private static void Menu_MissAnwen(Session conn, byte index) throws IOException {

        Vgo vgo = null;
        switch (index) {
            case 0: {
                vgo = new Vgo();
                vgo.toMap = 1;
                vgo.toX = 432;
                vgo.toY = 354;
                conn.p.changeMap(conn.p, vgo);
                break;
            }

            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Thongtincanhan(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                Service.send_notice_box(conn,
                        "Informasi:\n Ksatria Shine \n Alamat IP:" + conn.ip + "\nAkun:" + conn.user
                                + "\nKata Sandi Adalah:" + conn.pass + "\n Sisa Koin \n:" + conn.coin
                                + "\n(Catatan 0 = Sudah Aktif, 1 = Belum Aktif) \n :" + conn.status);
                break;

            }
            case 1: {
                Service.send_notice_box(conn,
                        "Informasi: \n" + "\nKerusakan : " + conn.p.body.getBaseDamage() + "\nHP :"
                                + conn.p.body.getMaxHP()
                                + "\nMP :" + conn.p.body.getMaxMP() + "\nKritis :" + conn.p.body.getCrit()
                                + "\nPenetrasi Armor :" + conn.p.body.getPierce() + "\nPertahanan :"
                                + conn.p.body.getDefBase() + "\nMenghindar:" + conn.p.body.getMiss()
                                + "\nRefleksi Kerusakan :" + conn.p.body.getReflectDamage());
                break;
            }

            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_top(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: { // Misteri Box shop, daftar item diatur lewat tabel `itemsell` id=18
                Service.send_box_UI(conn, 50);
                break;
            }
            case 1: {
                if (conn.p.chucphuc == 1) {
                    conn.p.chucphuc = 0;
                    int ngoc_ = Util.random(100, 500);
                    int vang_ = Util.random(10000, 200000);
                    conn.p.updateGem(ngoc_);
                    conn.p.updateGold(vang_);
                    conn.p.item.charInventory(5);
                    Service.send_notice_box(conn,
                            "Terima kasih sudah menyukai saya, sebagai rasa terima kasih saya berikan: " + ngoc_
                                    + " permata, " + vang_
                                    + " gold.");
                } else {
                    Service.send_notice_box(conn,
                            "Hari ini kamu sudah menyukai saya, saya tidak punya banyak uang untuk membagikan hadiah seperti itu!");
                }
                break;
            }
            case 2: {
                Service.send_box_UI(conn, 37);
                break;
            }

            case 3: {
                Service.send_box_UI(conn, 48);

                break;
            }

            case 4: {
                send_menu_select(conn, 115, new String[]{"Chest Informasi Akun", "Informasi Pribadi"});
                break;
            }
            // case 4: {
            // send_menu_select(conn, 1001,
            // new String[]{"Hướng dẫn", "Nhận nhiệm vụ", "Hủy nhiệm vụ", "Trả nhiệm vụ",
            // "Kiểm tra"});
            // break;
            // }
            case 5: {
                if (conn.p.hieuchien < 1000) {
                    Service.send_notice_box(conn, "Belum mencapai 1000 poin pk");
                    return;
                }
                conn.p.hieuchien -= 1000;
                int random = Util.random(100);
                if (random < 10) {
                    short id_ = 4718;
                    Item3 itbag = new Item3();
                    itbag.id = id_;
                    itbag.name = ItemTemplate3.item.get(id_).getName();
                    itbag.clazz = ItemTemplate3.item.get(id_).getClazz();
                    itbag.type = ItemTemplate3.item.get(id_).getType();
                    itbag.level = ItemTemplate3.item.get(id_).getLevel();
                    itbag.icon = ItemTemplate3.item.get(id_).getIcon();
                    itbag.op = ItemTemplate3.item.get(id_).getOp();
                    itbag.color = ItemTemplate3.item.get(id_).getColor();
                    itbag.part = ItemTemplate3.item.get(id_).getPart();
                    itbag.tier = 0;
                    itbag.islock = false;
                    itbag.time_use = 0;
                    conn.p.item.add_item_bag3(itbag);
                    conn.p.item.charInventory(3);
                    Service.send_notice_box(conn, "Menerima " + itbag.name);
                    return;
                } else if (random > 10 && random < 20) {
                    short id_ = 4719;
                    Item3 itbag = new Item3();
                    itbag.id = id_;
                    itbag.name = ItemTemplate3.item.get(id_).getName();
                    itbag.clazz = ItemTemplate3.item.get(id_).getClazz();
                    itbag.type = ItemTemplate3.item.get(id_).getType();
                    itbag.level = ItemTemplate3.item.get(id_).getLevel();
                    itbag.icon = ItemTemplate3.item.get(id_).getIcon();
                    itbag.op = ItemTemplate3.item.get(id_).getOp();
                    itbag.color = ItemTemplate3.item.get(id_).getColor();
                    itbag.part = ItemTemplate3.item.get(id_).getPart();
                    itbag.tier = 0;
                    itbag.islock = false;
                    itbag.time_use = 0;
                    conn.p.item.add_item_bag3(itbag);
                    conn.p.item.charInventory(3);
                    Service.send_notice_box(conn, "Menerima " + itbag.name);
                    return;
                } else if (random > 20 && random < 30) {
                    short id_ = 4709;
                    Item3 itbag = new Item3();
                    itbag.id = id_;
                    itbag.name = ItemTemplate3.item.get(id_).getName();
                    itbag.clazz = ItemTemplate3.item.get(id_).getClazz();
                    itbag.type = ItemTemplate3.item.get(id_).getType();
                    itbag.level = ItemTemplate3.item.get(id_).getLevel();
                    itbag.icon = ItemTemplate3.item.get(id_).getIcon();
                    itbag.op = ItemTemplate3.item.get(id_).getOp();
                    itbag.color = ItemTemplate3.item.get(id_).getColor();
                    itbag.part = ItemTemplate3.item.get(id_).getPart();
                    itbag.tier = 0;
                    itbag.islock = false;
                    itbag.time_use = 0;
                    conn.p.item.add_item_bag3(itbag);
                    conn.p.item.charInventory(3);
                    Service.send_notice_box(conn, "Menerima " + itbag.name);
                    return;
                } else if (random > 30) {
                    int vang = Util.random(5000, 100000);
                    int ngoc = Util.random(200, 400);
                    conn.p.updateGold(vang);
                    conn.p.updateGem(ngoc);
                    conn.p.item.charInventory(5);
                    Service.send_notice_box(conn, "Menerima " + vang + "emas. " + ngoc + "permata");
                }
                break;
            }
            // case 5: {
            // ...
            // }
            case 6: { // Top Kill Boss Premium
                send_menu_select(conn, -115, new String[]{"🏆 Top 20 Kill Boss", "⚔ Point Saya"});
                break;
            }
            case 7: {

                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                send_menu_select(conn, 601,
                        new String[]{"Area Bos Event 0x", "Area Bos Event 1x", "Area Bos Event 2x",
                                "Area Bos Event 7x", "Area Bos Event 8x", "Area Bos Event 11x", "Area Bos Event 13x"});
                break;
            }

            default: {
                Service.send_notice_box(conn, "fitur sedang maintenance!!");
                break;

            }
        }
    }

    private static void Menu_TachCanh(Session conn, byte index) throws IOException {
        Item3 item = null;
        int count = 0;
        for (int i = 0; i < conn.p.item.bag3.length; i++) {
            Item3 it = conn.p.item.bag3[i];
            if (it != null && it.type == 7 && it.tier > 0) {
                if (count == index) {
                    item = it;
                    break;
                }
                count++;
            }
        }
        if (item != null) {
            conn.p.id_wing_split = index;
            int quant1 = 40;
            int quant2 = 10;
            int quant3 = 50;
            for (int i = 0; i < item.tier; i++) {
                quant1 += GameSrc.wing_upgrade_material_long_khuc_xuong[i];
                quant2 += GameSrc.wing_upgrade_material_kim_loai[i];
                quant3 += GameSrc.wing_upgrade_material_da_cuong_hoa[i];
            }
            if (item.tier > 15) {
                quant1 /= 2;
                quant2 /= 2;
                quant3 /= 2;
            } else {
                quant1 /= 3;
                quant2 /= 3;
                quant3 /= 3;
            }
            Service.send_box_input_yesno(conn, 114, "Apakah Anda ingin pisahkan sayap ini dan mendapat: " + quant1
                    + " bulu dan tulang, " + quant2 + " logam, " + quant3 + " batu penguatan?");
        }
    }

    private static void Menu_TienCanh(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                // Service.send_box_UI(conn, index);
                Service.send_msg_data(conn, 23, "create_wings");


                break;
            }
            case 1: {
                Message m2 = new Message(77);
                m2.writer().writeByte(6);
                conn.addmsg(m2);
                m2.cleanup();
                //
                m2 = new Message(77);
                m2.writer().writeByte(1);
                m2.writer().writeUTF("Peningkatan sayap");
                conn.addmsg(m2);
                m2.cleanup();
                conn.p.is_create_wing = false;
                break;
            }
            // case 2: {
            // List<String> list = new ArrayList<>();
            // for (int i = 0; i < conn.p.item.bag3.length; i++) {
            // Item3 it = conn.p.item.bag3[i];
            // if (it != null && it.type == 7) {
            // list.add(it.name + " +" + it.tier);
            // }
            // }
            //
            // String[] list_2 = new String[]{"Trống"};
            // if (list.size() > 0) {
            // list_2 = new String[list.size()];
            // for (int i = 0; i < list_2.length; i++) {
            // list_2[i] = list.get(i);
            // }
            // }
            // MenuController.send_menu_select(conn, 210, list_2);
            //
            // break;
            // }
            case 3: {
                conn.p.id_wing_split = -1;
                List<String> list = new ArrayList<>();
                for (Item3 it : conn.p.item.bag3) {
                    if (it != null && it.type == 7 && it.tier > 0) {
                        list.add((it.name + " +" + it.tier));
                    }
                }
                if (!list.isEmpty()) {
                    String[] list_2 = new String[list.size()];
                    for (int i = 0; i < list_2.length; i++) {
                        list_2[i] = list.get(i);
                    }
                    send_menu_select(conn, 121, list_2);
                } else {
                    Service.send_notice_box(conn, "Tidak punya sayap, kok minta dipisah?");
                }
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur tidak tersedia");
                break;
            }
        }
    }

    private static void Menu_ThayDongCanh_percent(Session conn, byte index) throws IOException {
        if (conn.p.getGem() < 300) {
            Service.send_notice_box(conn, "Tidak cukup 300 permata");
            return;
        }
        conn.p.updateGem(-300);
        Log.gI().add_log(conn.p.name, "used 300 gem");
        Item3 it_process = null;
        for (int i = 0; i < conn.p.item.bag3.length; i++) {
            Item3 it = conn.p.item.bag3[i];
            if (it != null && it.type == 7) {
                if (index == 0) {
                    it_process = it;
                    break;
                }
                index--;
            }
        }
        if (it_process != null) {
            Option[] process = new Option[2];
            for (int i = 0; i < it_process.op.size(); i++) {
                if (it_process.op.get(i).id >= 7 && it_process.op.get(i).id <= 11) {
                    if (process[0] == null) {
                        process[0] = it_process.op.get(i);
                    } else if (process[1] == null) {
                        process[1] = it_process.op.get(i);
                    } else {
                        break;
                    }
                }
            }
            if (process[0] != null) {
                process[0].id = (byte) Util.random(7, 12);
                process[0].setParam(Util.random(1500, 2500));
                // process[0].setParam(process[0].getParam(0) + Util.random(50, 100));
            }
            if (process[1] != null) {
                process[1].id = (byte) Util.random(7, 12);
                process[1].setParam(Util.random(1500, 2500));
                // process[1].setParam(process[1].getParam(0) + Util.random(50, 100));
            }
            Service.send_notice_box(conn, "Berhasil");
            conn.p.item.charInventory(3);
        }
    }

    public static void createCloak(Session conn, byte index) throws IOException {

        if (conn.p.myclan == null) return;

        Item3 item = ItemManager.getInstance().getGuildItem(23, conn.p.myclan);
        if (item == null) {
            Service.send_notice_box(conn, "Guild mu tidak memiliki jubah");
            return;
        }

        Item47 ticket = conn.p.item.getPotion((short) 235);
        if (ticket == null) {
            Service.send_notice_box(conn, "Kamu tidak memiliki ticket");
            return;
        }

        item.islock = false;
        List<Option> configuredStat = ItemManager.getInstance().getGuildItemOptions(23, conn.p.myclan, index);
        if (configuredStat != null) {
            // Stat sudah diatur admin lewat database (tabel guild_item)
            item.op = new ArrayList<>(configuredStat);
        } else {
            // Belum ada konfigurasi di DB, pakai stat random bawaan
            StatGenerator st = new StatGenerator();
            item.op = st.createCloakStat(index, 0);
        }

        if (conn.p.item.get_bag_able() <= 0) {
            Service.send_notice_box(conn, "Tidak dapat di proses dikarnakan inventory penuh");
            return;
        }
        conn.p.item.remove(4, ticket.id, 1);
        conn.p.item.add_item_bag3(item);
        conn.p.item.charInventory(3);
        conn.p.item.charInventory(4);
        Service.send_notice_box(conn, "Jubah berhasil dibuat");
    }

    public static void createTitle(Session conn, byte index) throws IOException {

        if (conn.p.myclan == null) return;

        Item3 item = ItemManager.getInstance().getGuildItem(27, conn.p.myclan);
        if (item == null) {
            Service.send_notice_box(conn, "Guild mu tidak memiliki title");
            return;
        }

        Item47 ticket = conn.p.item.getPotion((short) 235);
        if (ticket == null) {
            Service.send_notice_box(conn, "Kamu tidak memiliki ticket");
            return;
        }

        item.islock = false;
        List<Option> configuredStat = ItemManager.getInstance().getGuildItemOptions(27, conn.p.myclan, index);
        if (configuredStat != null) {
            // Stat sudah diatur admin lewat database (tabel guild_item)
            item.op = new ArrayList<>(configuredStat);
        } else {
            // Belum ada konfigurasi di DB, pakai stat random bawaan
            StatGenerator st = new StatGenerator();
            item.op = st.createTitleStat(index, 0);
        }

        if (conn.p.item.get_bag_able() <= 0) {
            Service.send_notice_box(conn, "Tidak dapat di proses dikarnakan inventory penuh");
            return;
        }
        conn.p.item.remove(4, ticket.id, 1);
        conn.p.item.add_item_bag3(item);
        conn.p.item.charInventory(3);
        conn.p.item.charInventory(4);
        Service.send_notice_box(conn, "Title berhasil dibuat");
    }

    public static void Menu_Clan_Manager(Session conn, byte index) throws IOException {
        if (conn.p.myclan.members.get(0).name.equals(conn.p.name)) {
            switch (index) {
                case 0: {
                    conn.p.myclan.open_box_clan(conn);
                    break;
                }
                case 1: {
                    if (conn.p.myclan.get_percent_level() >= 100) {
                        Service.send_box_input_yesno(conn, 118,
                                "Apakah Anda ingin upgrade guild ke level " + (conn.p.myclan.level + 1) + " dengan "
                                        + (Guild.vang_upgrade[1] * conn.p.myclan.level) + " vàng và "
                                        + (conn.p.myclan.level + 1)
                                        + " với " + (Guild.ngoc_upgrade[1] * conn.p.myclan.level) + " ngọc không?");
                    } else {
                        Service.send_notice_box(conn, "EXP tidak cukup untuk meningkatkan level!");
                    }
                    break;
                }
                case 2: {
                    Service.send_box_input_yesno(conn, 116,
                            "Konfirmasi pembubaran guild. Jangan menyesal dan menangis minta admin untuk mengembalikannya!");
                    break;
                }
                case 3: {
                    Service.sendBoxInputText(conn, 13, "Masukkan nama :", new String[]{"Masukkan nama :"});
                    break;
                }
                default: {

                    Service.send_notice_box(conn, "Belum ada fungsi");
                    break;
                }
            }
        }
    }

    private static void Menu_Dungeon_Mode_Selection(Session conn, byte index) throws IOException {
        // if (conn.p.dungeon != null && conn.p.dungeon.getWave() == 20) {
        // if (index != 2 && conn.p.party != null && conn.p.party.get_mems().get(0).id
        // != conn.p.id) {
        // Service.send_notice_box(conn, "Chỉ có đội trưởng mới có quyền quyết định!");
        // return;
        // }
        // conn.p.dungeon.setMode(index);
        // if (conn.p.dungeon != null) {
        // conn.p.dungeon.setWave(21);
        // conn.p.dungeon.state = 1;
        // } else {
        // Service.send_notice_box(conn, "Có lỗi xảy ra, hãy thử chọn lại!");
        // }
        // }
    }

    private static void Menu_LinhCanh(Session conn, byte index) throws IOException {
        // if (conn.p.dungeon != null && conn.p.dungeon.getWave() == 20) {
        // if (index != 2 && conn.p.party != null && conn.p.party.get_mems().get(0).id
        // != conn.p.id) {
        // Service.send_notice_box(conn, "Chỉ có đội trưởng mới có quyền quyết định!");
        // return;
        // }
        // switch (index) {
        // case 0: {
        // send_menu_select(conn, 123, new String[]{"Easy", "Normal", "Hard",
        // "Nightmare", "Hell"});
        // break;
        // }
        // case 1: {
        // if (conn.p.dungeon != null) {
        // conn.p.dungeon.state = 6;
        // } else {
        // Service.send_notice_box(conn, "Có lỗi xảy ra, hãy thử chọn lại!");
        // }
        // Service.send_notice_box(conn, "gà thật, éo dám đi tiếp à");
        // break;
        // }
        // case 2: {
        // Service.send_notice_box(conn,
        // "Đến được đây quả là có cố gắng, hãy nói chuyện với phó chỉ huy để nhận
        // thưởng hoàn thành phó bản, hoặc chọn tiếp tục chinh phục để có thể nhận được
        // nhiều phần thưởng hơn");
        // break;
        // }
        // default: {
        // Service.send_notice_box(conn, "Chưa có chức năng");
        // break;
        // }
        // }
        // }
    }

    private static void Menu_PhoChiHuy(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                if (conn.p.level < 30) {
                    Service.send_notice_box(conn, "Harus mencapai level 30 untuk masuk dungeon");
                    return;
                }
                if (conn.p.point_active[0] < 1) {
                    Service.send_notice_box(conn, "Sudah kehabisan giliran, silakan kembali besok");
                    return;
                }
                String notice = "Daftar pemain yang masuk dungeon :\n1) " + conn.p.name + " : level " + conn.p.level;
                if (conn.p.party != null) {
                    Service.send_notice_box(conn, "Dungeon saat ini hanya mendukung mode solo");
                    return;
                }
                notice += "\nTingkat kesulitan: ??? \nSilakan konfirmasi."
                        + (conn.p.point_active[0] != 10 ? " biaya masuk 30 permata." : "");
                // conn.p.dungeon = null;
                Service.send_box_input_yesno(conn, 119, notice);
                break;
            }
            case 1: {
                Service.send_notice_box(conn,
                        "Jalan Empat Maut yang ditingkatkan:\nSetelah melewati 20 stage pertama, Anda akan menerima hadiah penyelesaian dungeon. Setelah itu, bicaralah dengan NPC di dalam dungeon untuk memutuskan tingkat kesulitan, lalu lanjutkan menaklukkan dungeon.\nSemakin banyak poin yang Anda kumpulkan, semakin banyak hadiah yang akan Anda dapatkan. Poin dihitung berdasarkan jumlah stage yang dilewati dan jumlah damage yang diberikan.\nCatatan: Maksimal 10 kali masuk per hari.");
                break;
            }
            case 2: {
                synchronized (Dungeon.bxh_time_complete) {
                    String notice;
                    if (Dungeon.bxh_time_complete.size() > 0) {
                        notice = "Peringkat waktu penyelesaian:\n";
                        int dem = 1;
                        for (Dungeon.BXH_Dungeon_Finished set : Dungeon.bxh_time_complete) {
                            notice += (dem++) + ". " + set.name + " : " + set.time + "s\n";
                        }
                    } else {
                        notice = "Belum ada informasi";
                    }
                    Service.send_notice_box(conn, notice);
                }
                break;
            }
            case 3: {
                CastleSiegeManager.ClanRegister(conn.p);
                break;
            }
            case 4: {
                if (CastleSiegeManager.timeAttack < System.currentTimeMillis()) {
                    Service.send_notice_box(conn, "Perang Benteng sudah berakhir.");
                } else if (!CastleSiegeManager.joinMap(conn.p)) {
                    Service.send_notice_box(conn,
                            "Kamu tidak memenuhi syarat untuk berpartisipasi dalam Perang Benteng.");
                }
                break;
            }
            case 5: {
                Service.send_notice_box(conn, "Informasi:\nSisa kesempatan " + conn.p.point_active[0]
                        + " kali\nTotal poin hari ini : " + conn.p.point_active[1]);
                break;
            }
            case 6: {
                if (conn.p.point_active[1] < 1) {
                    Service.send_notice_box(conn,
                            "Hari ini belum melakukan apa-apa, kamu pikir bisa dapat hadiah tanpa usaha?");
                    return;
                }
                if (conn.p.item.get_bag_able() < 3) {
                    Service.send_notice_box(conn, "Ruang inventaris tidak cukup!");
                    return;
                }
                while (conn.p.point_active[1] > 0) {
                    conn.p.point_active[1]--;
                    short id_ = Medal_Material.m_blue[Util.random(0, 9)];
                    if (conn.p.item.get_bag_able() <= 0) {
                        Service.send_notice_box(conn, "Ruang inventaris tidak cukup!");
                        conn.p.item.charInventory(7);
                        conn.p.updateGold(Util.random(10, 50));
                        return;
                    }
                    if (25 > Util.random(0, 100) && ((conn.p.item.get_bag_able() > 0))) {
                        Item47 itbag = new Item47();
                        itbag.id = id_;
                        itbag.quantity = (short) Util.random(0, 2);
                        itbag.category = 7;
                        conn.p.item.add_item_bag47(7, itbag);
                    }
                    //
                    conn.p.updateGold(Util.random(10, 50));
                }
                conn.p.item.charInventory(7);
                Service.send_notice_box(conn, "Berhasil diterima");
                break;
            }
            case 7: {
                Item3 itbag = new Item3();
                itbag.id = 3596;
                itbag.clazz = ItemTemplate3.item.get(3596).getClazz();
                itbag.type = ItemTemplate3.item.get(3596).getType();
                itbag.level = ItemTemplate3.item.get(3596).getLevel();
                itbag.icon = ItemTemplate3.item.get(3596).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(3596).getOp());
                itbag.color = 5;
                itbag.part = ItemTemplate3.item.get(3596).getPart();
                itbag.tier = 0;
                itbag.islock = true;
                itbag.time_use = 0;
                // thao do
                if (conn.p.item.wear[11] != null && conn.p.item.wear[11].id != 3593 && conn.p.item.wear[11].id != 3599
                        && conn.p.item.wear[11].id != 3596) {
                    Item3 buffer = conn.p.item.wear[11];
                    conn.p.item.wear[11] = null;
                    conn.p.item.add_item_bag3(buffer);
                }
                itbag.name = ItemTemplate3.item.get(3596).getName() + " [Terkunci]";
                itbag.updateName();
                conn.p.item.wear[11] = itbag;
                conn.p.item.charInventory(4);
                conn.p.item.charInventory(7);
                conn.p.item.charInventory(3);
                conn.p.fashion = PartFashion.getPart(conn.p);
                conn.p.change_map_di_buon(conn.p);
                Service.send_notice_box(conn, "Berhasil diterima");
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Pet_Manager(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                Service.send_box_UI(conn, 21);
                break;
            }
            case 1: {
                Service.send_box_UI(conn, 22);
                break;
            }
            case 2: {
                Service.send_box_UI(conn, 23);
                break;
            }
            case 3: {
                if (conn.p.pet_follow != -1) {
                    for (Pet temp : conn.p.mypet) {
                        if (temp.is_follow) {
                            temp.is_follow = false;
                            Message m = new Message(44);
                            m.writer().writeByte(28);
                            m.writer().writeByte(1);
                            m.writer().writeByte(9);
                            m.writer().writeByte(9);
                            m.writer().writeUTF(temp.name);
                            m.writer().writeByte(temp.type);
                            m.writer().writeShort(conn.p.mypet.indexOf(temp)); // id
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
                            conn.p.conn.addmsg(m);
                            m.cleanup();
                            break;
                        }
                    }
                    conn.p.pet_follow = -1;
                    Service.sendPlayerWear(conn.p);
                    Service.sendMainCharInfo(conn.p);
                } else {
                    Service.send_notice_box(conn, "Belum memakai pet, kok minta dilepas?");
                }
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur tidak tersedia");
                break;
            }
        }
    }

    private static void Menu_Mr_Edgar(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                if (conn.p.list_enemies.size() > 0) {
                    String[] name = new String[conn.p.list_enemies.size()];
                    for (int i = 0; i < name.length; i++) {
                        name[i] = conn.p.list_enemies.get(name.length - i - 1);
                    }
                    send_menu_select(conn, 124, name);
                } else {
                    Service.send_notice_box(conn, "Daftar masih kosong, silakan cari masalah untuk menambahkannya!");
                }
                break;
            }
            case 1: {
                Service.send_notice_box(conn,
                        "Jika diserang oleh pemain lain, nama mereka akan disimpan ke dalam daftar. "
                                + "Setiap kali balas dendam, kamu akan dibawa ke lokasi musuh dengan biaya hanya 2 permata.\n"
                                + "Setelah dibawa ke lokasi, nama musuh akan dihapus dari daftar");
                break;
            }
            default: {
                Service.send_notice_box(conn, "Belum ada fungsi");
                break;
            }
        }
    }

    private static void Menu_Zoro(Session conn, byte index) throws IOException {
        if (conn.p.myclan != null) {
            if (conn.p.myclan.members.get(0).name.equals(conn.p.name)) {
                switch (index) {
                    case 0: {
                        send_menu_select(conn, 122,
                                new String[]{"Gudang guild", "Tingkatkan guild", "Bubar guild",
                                        "Transfer pemimpin"});
                        break;
                    }
                    case 1: { //
                        Service.send_box_UI(conn, 29);
                        break;
                    }
                    case 2: {
                        Service.send_box_UI(conn, 30);
                        break;
                    }
                    case 3: {
                        send_menu_select(conn, -12,
                                new String[]{"Warrior", "Penyihir", "Assasin", "Penembak"});
                        break;
                    }
                    case 4: {
                        send_menu_select(conn, -13,
                                new String[]{"Warrior", "Penyihir", "Assasin", "Penembak"});
                        break;
                    }
                    default: {
                        Service.send_notice_box(conn, "Fitur belum tersedia");
                        break;
                    }
                }
            } else {
                switch (index) {
                    case 0: {
                        conn.p.myclan.open_box_clan(conn);
                        break;
                    }
                    case 1: {
                        Service.sendBoxInputText(conn, 8, "Sumbang Emas", new String[]{"Jumlah :"});
                        break;
                    }
                    case 2: {
                        Service.sendBoxInputText(conn, 9, "Sumbang Permata", new String[]{"Jumlah :"});
                        break;
                    }
                    case 3: {
                        Service.send_box_input_yesno(conn, 117,
                                "Harap konfirmasi untuk keluar dari guild, Anda mungkin tidak akan bisa kembali lagi.");
                        break;
                    }
                    default: {
                        Service.send_notice_box(conn, "Fitur belum tersedia");
                        break;
                    }
                }
            }
        } else {
            switch (index) {
                case 0: {
                    Service.send_box_input_yesno(conn, 70,
                            "Apakah Anda ingin membuat guild dengan biaya 20.000 permata?");
                    break;
                }
                default: {
                    Service.send_notice_box(conn, "Fitur belum tersedia");
                    break;
                }
            }
        }
    }

    private static void Menu_Benjamin(Session conn, byte index) throws IOException {
        if (conn.p.myclan != null) {
            if (conn.p.myclan.members.get(0).name.equals(conn.p.name)) {
                switch (index) {
                    case 0: {
                        send_menu_select(conn, 122,
                                new String[]{"Gudang guild", "Tingkatkan guild", "Bubar guild",
                                        "Transfer pemimpin"});
                        break;
                    }
                    case 1: { //
                        Service.send_box_UI(conn, 29);
                        break;
                    }
                    case 2: {
                        Service.send_box_UI(conn, 30);
                        break;
                    }
                    default: {
                        Service.send_notice_box(conn, "Fitur belum tersedia");
                        break;
                    }
                }
            } else {
                switch (index) {
                    case 0: {
                        conn.p.myclan.open_box_clan(conn);
                        break;
                    }
                    case 1: {
                        Service.sendBoxInputText(conn, 8, "Sumbang Emas", new String[]{"Jumlah :"});
                        break;
                    }
                    case 2: {
                        Service.sendBoxInputText(conn, 9, "Sumbang Permata", new String[]{"Jumlah :"});
                        break;
                    }
                    case 3: {
                        Service.send_box_input_yesno(conn, 117,
                                "Harap konfirmasi untuk keluar dari guild, Anda mungkin tidak akan bisa kembali lagi.");
                        break;
                    }
                    default: {
                        Service.send_notice_box(conn, "Fitur belum tersedia");
                        break;
                    }
                }
            }
        } else {
            switch (index) {
                case 0: {
                    Service.send_box_input_yesno(conn, 70,
                            "Apakah Anda ingin membuat guild dengan biaya 20.000 permata?");
                    break;
                }
                default: {
                    Service.send_notice_box(conn, "Fitur belum tersedia");
                    break;
                }
            }
        }
    }


    private static void menuGambleEmas(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                // Manager.gI().vxmm.send_in4(conn.p);
                GoldSpin gamble = GameEventManager.gI().getEvent(GoldSpin.class);
                conn.p.sendNoticeBox(gamble.getInformation());
                break;
            }
            case 1: {
                GoldSpin gamble = GameEventManager.gI().getEvent(GoldSpin.class);
                Service.sendBoxInputText(conn, 3,
                        "Roda Putar Emas",
                        new String[]{
                                Util.number_format(gamble.getMinBet()) + "-" + Util.number_format(gamble.getMaxBet())});
                break;
            }
            case 2: {
                GoldSpin gamble = GameEventManager.gI().getEvent(GoldSpin.class);
                gamble.getChance(conn.p);
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void menuGambleEmasRegular(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                // Manager.gI().vxmm.send_in4(conn.p);
                QuickGoldSpin gamble = GameEventManager.gI().getEvent(QuickGoldSpin.class);
                conn.p.sendNoticeBox(gamble.getInformation());
                break;
            }
            case 1: {
                QuickGoldSpin gamble = GameEventManager.gI().getEvent(QuickGoldSpin.class);
                Service.sendBoxInputText(conn, 32,
                        "Roda Putar Emas",
                        new String[]{
                                Util.number_format(gamble.getMinBet()) + "-" + Util.number_format(gamble.getMaxBet())});
                break;
            }
            case 2: {
                QuickGoldSpin gamble = GameEventManager.gI().getEvent(QuickGoldSpin.class);
                gamble.getChance(conn.p);
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void menuGamblePermata(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                //Manager.gI().vxkc.send_in4(conn.p);
                GemSpin gamble = GameEventManager.gI().getEvent(GemSpin.class);
                conn.p.sendNoticeBox(gamble.getInformation());
                break;
            }
            case 1: {
                GemSpin gamble = GameEventManager.gI().getEvent(GemSpin.class);
                Service.sendBoxInputText(conn, 17, "Roda Putar Permata",
                        new String[]{gamble.getMinBet() + "-" + gamble.getMaxBet()});
                break;
            }
            case 2: {
                GemSpin gamble = GameEventManager.gI().getEvent(GemSpin.class);
                gamble.getChance(conn.p);
                break;
            }

            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void menuGamblePermataRegular(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                //Manager.gI().vxkc.send_in4(conn.p);
                QuickGemSpin gamble = GameEventManager.gI().getEvent(QuickGemSpin.class);
                conn.p.sendNoticeBox(gamble.getInformation());
                break;
            }
            case 1: {
                QuickGemSpin gamble = GameEventManager.gI().getEvent(QuickGemSpin.class);
                Service.sendBoxInputText(conn, 33, "Roda Putar Permata",
                        new String[]{gamble.getMinBet() + "-" + gamble.getMaxBet()});
                break;
            }
            case 2: {
                QuickGemSpin gamble = GameEventManager.gI().getEvent(QuickGemSpin.class);
                gamble.getChance(conn.p);
                break;
            }

            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void MenuGamble(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                send_menu_select(conn, 125, new String[]{"Lihat informasi", "Ikut serta", "Peluang Menang"});
                break;
            }
            case 1: {
                send_menu_select(conn, 132, new String[]{"Lihat informasi", "Ikut serta", "Peluang Menang"});
                // Service.send_notice_box(conn, "Sắp ra mắt");
                break;
            }
            case 2: {
                send_menu_select(conn, 133, new String[]{"Lihat informasi", "Ikut serta", "Peluang Menang"});
                break;
            }
            case 3: {
                send_menu_select(conn, 134, new String[]{"Lihat informasi", "Ikut serta", "Peluang Menang"});
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    public static void send_menu_select(Session conn, int idnpc, String[] menu) throws IOException {
        if (!conn.p.isdie) {
            if (menu != null && menu.length > 0) {
                Message m2 = new Message(-30);
                m2.writer().writeShort(idnpc);
                m2.writer().writeByte(0);
                m2.writer().writeByte(menu.length);
                for (int i = 0; i < menu.length; i++) {
                    m2.writer().writeUTF(menu[i]);
                }
                if (conn.ac_admin > 0) {
                    m2.writer().writeUTF("MENU : " + idnpc);
                } else {
                    m2.writer().writeUTF("MENU");
                }
                conn.addmsg(m2);
                m2.cleanup();
            }
        }
    }

    public static void send_menu_select(Session conn, int idnpc, String[] menu, byte idmenu) throws IOException {
        if (!conn.p.isdie) {
            if (menu != null && menu.length > 0) {
                Message m2 = new Message(-30);
                m2.writer().writeShort(idnpc);
                m2.writer().writeByte(idmenu);
                m2.writer().writeByte(menu.length);
                for (int i = 0; i < menu.length; i++) {
                    m2.writer().writeUTF(menu[i]);
                }
                if (conn.ac_admin > 0) {
                    m2.writer().writeUTF("MENU : " + idnpc);
                } else {
                    m2.writer().writeUTF("MENU");
                }
                conn.addmsg(m2);
                m2.cleanup();
            }
        }
    }

    private static void Menu_Aman(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                Message m = new Message(23);
                m.writer().writeUTF("Peti Barang");
                m.writer().writeByte(3);
                m.writer().writeShort(0);
                conn.addmsg(m);
                m.cleanup();
                break;
            }
            case 1: {
                if (conn.p.maxbag >= 126) {
                    Service.send_notice_box(conn, "Penyimpanan sudah terbuka!");
                    return;
                }
                try (Connection connection = SQL.gI().getConnection(); Statement statement = connection.createStatement();) {
                    if (statement.executeUpdate(
                            "UPDATE `player` SET `maxbag` = 126 WHERE `id` = " + conn.p.objectId + ";") > 0) {
                        connection.commit();
                    }
                    Service.send_notice_box(conn, "Berhasil membuka 126 slot, silakan login ulang untuk update!");
                } catch (SQLException e) {
                    e.printStackTrace();
                    // Service.send_notice_box(conn, "Terjadi kesalahan, silakan coba lagi!");
                }
                break;
            }
            case 2: {
                // if (conn.p.update_coin(-25_000)) {
                // if (true) {
                // String js =
                // "[1,%s,%s,%s,%s,10,100,300,1,1,1,1,100,[[0,%s,%s],[23,%s,0],[24,%s,0],[25,%s,0],[26,%s,0]]]";
                // byte[] type = new byte[] {8, 13, 10, 11, 11, 11, 12, 12, 12, 9, 7, 7, 7, 6,
                // 5, 5, 5, 4, 4, 4, 3,
                // 3, 3, 2,
                // 2, 2, 1, 1, 1, 0, 0, 0};
                // byte[] icon = new byte[] {26, 41, 32, 33, 34, 35, 36, 37, 38, 29, 21, 22, 23,
                // 20, 15, 16, 17, 12,
                // 13, 14,
                // 9, 10, 11, 6, 7, 8, 0, 1, 2, 3, 4, 5};
                // int rd = Util.random(0, 32);
                // js = String.format(js, type[rd], icon[rd], 3, 0, Util.random(0, 99),
                // Util.random(99, 999),
                // Util.random(1, 50), Util.random(1, 50), Util.random(1, 50), Util.random(1,
                // 50));
                // if (conn.p.mypet == null) {
                // conn.p.mypet = new Pet(conn.p);
                // }
                // conn.p.mypet.setup((JSONArray) JSONValue.parse(js));
                // Service.send_wear(conn.p);
                // Service.send_char_main_in4(conn.p);
                // conn.p.map.update_in4_inside(conn.p);
                // Service.send_box_notice(conn, "Nhận thành công!");
                // // } else {
                // // Service.send_box_notice(conn, "Éo đủ coin, đi nạp đi!");
                // }
                if (conn.user.contains("user_")) {
                    if (conn.p.level < 10) {
                        Service.send_notice_box(conn, "Capai level 10 untuk bisa daftar akun");
                        return;
                    }
                    Service.sendBoxInputText(conn, 6, "Masukkan informasi",
                            new String[]{"Username baru", "Password baru"});
                } else {
                    Service.send_notice_box(conn, "Fitur sedang dalam pengembangan...");
                }
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Black_Eye(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                Service.send_box_UI(conn, 13);
                break;
            }
            case 1: {
                Service.send_box_UI(conn, 14);
                break;
            }
            case 2: {
                Service.send_box_UI(conn, 15);
                break;
            }
            case 3: {
                Service.send_box_UI(conn, 16);
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_BXH(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                BXH.send(conn, 0);
                break;
            }
            case 1: {
                send_menu_select(conn, 888, new String[]{"Guild Terkaya", "Guild Terkuat",
                        "Guild Permata Terbanyak", "Top Guild Penakluk Tambang"});

                break;
            }
            case 2: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
            }

            send_menu_select(conn, 345,
                    new String[]{"Permata 1 Bintang", "Permata 2 Bintang", "Permata 3 Bintang",
                            "Permata 4 Bintang",
                            "Permata 5 Bintang", "Permata 6 Bintang", "Permata 7 Bintang", "Panduan Event Mini",
                            "Peta Pembasmi Iblis"});
            break;
            case 3: {
                // Ranking Lainnya - submenu
                send_menu_select(conn, -32, new String[]{
                    "Top Pasangan 💕",
                    "Top Dagang  (Segera)",
                    "Top Rampok  (Segera)",
                    "Top Knight  (Segera)"
                });
                break;
            }
            case 4: { // Top Kill Boss Premium
                BXH.send(conn, 3);
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_BXHCLAN(Session conn, byte index) throws IOException {
        switch (index) {
            case 1: {
                String[] list = new String[Math.min(20, BXH.BXH___GUILD.size())];
                for (int i = 0; i < list.length; i++) {
                    list[i] = ("Peringkat " + (i + 1) + " : "
                            + BXH.BXH___GUILD.get(i).name
                            + " - " + BXH.BXH___GUILD.get(i).shortName
                            + "\n Emas : " + BXH.BXH___GUILD.get(i).gold) + " - ";

                }
                if (list.length > 0) {
                    send_menu_select(conn, 120, list);
                } else {
                    Service.send_notice_box(conn, "Belum ada informasi");
                }

                break;
            }

            default: {
                Service.send_notice_box(conn, "Fitur Sedang Dalam Perbaikan");
                break;
            }
        }
    }

    private static void menuMissAnna(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                Service.sendBoxInputText(conn, 0, "Masukkan kode", new String[]{"Kode"});
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
            case 9: {
                if (conn.p.item.wear[index + 9] == null) {
                    Service.send_notice_box(conn, "Tidak ada item untuk dilepas");
                } else if (conn.p.item.get_bag_able() > 0) {
                    Item3 buffer = conn.p.item.wear[index + 9];
                    conn.p.item.wear[index + 9] = null;
                    if (buffer.id != 3599 && buffer.id != 3593 && buffer.id != 3596) {
                        conn.p.item.add_item_bag3(buffer);
                    }
                    conn.p.item.charInventory(3);
                    conn.p.fashion = PartFashion.getPart(conn.p);
                    Service.sendPlayerWear(conn.p);
                    Service.sendMainCharInfo(conn.p);
                    MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                    Service.send_notice_box(conn, "Berhasil dilepas");
                    //
                    if (index == 2 && conn.p.pet_di_buon != null) {
                        Message m = new Message(8);
                        m.writer().writeShort(conn.p.pet_di_buon.objectId);
                        for (int i = 0; i < conn.p.map.players.size(); i++) {
                            Player p0 = conn.p.map.players.get(i);
                            if (p0 != null) {
                                p0.conn.addmsg(m);
                            }
                        }
                        m.cleanup();
                        //
                        Pet_di_buon_manager.remove(conn.p.pet_di_buon.name);
                        conn.p.pet_di_buon = null;
                    }
                } else {
                    Service.send_notice_box(conn, "Tas penuh!");
                }
                break;
            }
            case 10: {
                conn.p.takeOffMount();
                break;
            }
            case 11: {
                List<ItemEntry> entries = CompensationManager.gI().claimAllCompensations(conn.p.name);
                if (entries.isEmpty()) {
                    conn.p.sendNoticeBox("Tidak ada hadiah untukmu.");
                    return;
                }


                List<ItemReward> rewards = new ArrayList<>();
                entries.forEach(itemEntry -> {
                    RoleItems item = itemEntry.getByClazz(conn.p.clazz);
                    List<Item3> equip = item.getElemental();
                    if (equip != null && !equip.isEmpty()) {
                        equip.forEach(item3 -> {
                            item3.islock = true;
                            item3.expiry_date = item3.duration > 0 ? System.currentTimeMillis() + TimeUnit.HOURS.toMillis(item3.duration) : 0;
                        });
                    }

                    rewards.add(new ItemReward(equip, itemEntry.getPotions(), itemEntry.getMaterials(), 0, itemEntry.getGold(), itemEntry.getGems()));
                });

                if (!rewards.isEmpty()) {
                    rewards.forEach(itemReward -> ItemManager.getInstance().sendReward(conn, itemReward));
                }
                break;
            }
        }
    }

    private static void Menu_Doiaochoang(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                send_menu_select(conn, 346, new String[]{"Jubah Baron", "Jubah Viscount ",
                        "Jubah Count ", "Jubah Marquis ", "Jubah Duke"});
                break;
            }
            case 1: {
                send_menu_select(conn, 347, new String[]{"Jubah Baron", "Jubah Viscount ",
                        "Jubah Count ", "Jubah Marquis ", "Jubah Duke"});
                break;
            }
            case 2: {
                send_menu_select(conn, 348, new String[]{"Jubah Baron", "Jubah Viscount ",
                        "Jubah Count ", "Jubah Marquis ", "Jubah Duke"});
                break;
            }
            case 3: {
                send_menu_select(conn, 349, new String[]{"Jubah Baron", "Jubah Viscount ",
                        "Jubah Count ", "Jubah Marquis ", "Jubah Duke"});
                break;
            }
            case 4: {
                send_menu_select(conn, 350, new String[]{"Jubah Baron", "Jubah Viscount ",
                        "Jubah Count ", "Jubah Marquis ", "Jubah Duke"});
                break;
            }
            case 5: {
                send_menu_select(conn, 351, new String[]{"Jubah Baron", "Jubah Viscount ",
                        "Jubah Count ", "Jubah Marquis ", "Jubah Duke"});
                break;
            }
            case 6: {
                send_menu_select(conn, 777, new String[]{"Jubah Baron", "Jubah Viscount ",
                        "Jubah Count ", "Jubah Marquis ", "Jubah Duke"});
                break;
            }
            case 7: {

                String s = "💖Panduan💖";
                s += "\nAda 2 cara untuk berburu bola naga";
                s += "\nPemain harus berada di level 40 ke atas";
                s += "\nCara bermain :";
                s += "\nBola naga akan muncul secara acak di 3 peta {Tepi Hutan, Gua Api, Hutan Ilusi} .";
                s += "\nSelain itu, para ksatria juga bisa berburu bos di peta terpisah melalui NPC peringkat";
                s += "\nSetelah mendapatkan bola naga, para ksatria dapat menukarkannya dengan hadiah di NPC peringkat";
                s += "\nAkhir Event, Terima Kasih Kepada Para Ksatria yang Telah Mendukung Server";
                s += "\nBy VB";
                break;
            }
            case 8: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 116;
                vgo.toX = 1020;
                vgo.toY = 588;
                conn.p.changeMap(conn.p, vgo);

                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Doiaochoang7(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 470) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 7 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 470, 1);
                short iditem = 4676;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 1: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 470) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 7 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 470, 1);
                short iditem = 4679;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 2: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 470) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 1 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 470, 1);
                short iditem = 4682;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 3: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 470) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 7 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 470, 1);
                short iditem = 4685;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 4: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 470) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 7 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 470, 1);
                short iditem = 4688;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }

            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Doiaochoang1(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 464) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 1 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 464, 1);
                short iditem = 4676;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 1;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 1: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 464) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 1 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 464, 1);
                short iditem = 4679;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 1;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 2: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 464) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 1 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 464, 1);
                short iditem = 4682;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 1;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 3: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 464) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 1 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 464, 1);
                short iditem = 4685;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 1;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 4: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 464) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 1 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 464, 1);
                short iditem = 4688;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 1;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Doiaochoang2(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 465) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 2 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 465, 1);
                short iditem = 4676;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 2;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 1: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 465) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 2 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 465, 1);
                short iditem = 4679;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 2;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 2: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 465) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 2 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 465, 1);
                short iditem = 4682;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 2;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 3: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 465) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 2 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 465, 1);
                short iditem = 4685;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 2;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 4: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 465) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 2 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 465, 1);
                short iditem = 4688;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 2;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }

            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Doiaochoang3(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 466) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 3 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 466, 1);
                short iditem = 4676;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 3;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 1: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 466) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 3 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 466, 1);
                short iditem = 4679;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 3;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 2: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 466) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 3 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 466, 1);
                short iditem = 4682;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 3;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 3: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 466) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 3 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 466, 1);
                short iditem = 4685;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 3;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 4: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 466) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 3 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 466, 1);
                short iditem = 4688;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 3;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }

            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Doiaochoang4(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 467) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 4 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 467, 1);
                short iditem = 4676;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 4;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 1: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 467) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 4 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 467, 1);
                short iditem = 4679;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 4;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 2: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 467) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 4 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 467, 1);
                short iditem = 4682;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 4;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 3: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 467) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 4 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 467, 1);
                short iditem = 4685;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 4;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 4: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 467) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 4 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 467, 1);
                short iditem = 4688;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 4;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }

            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Doiaochoang5(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 468) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 5 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 468, 1);
                short iditem = 4676;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 5;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 1: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 468) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 5 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 468, 1);
                short iditem = 4679;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 5;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 2: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 468) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 5 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 468, 1);
                short iditem = 4682;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 5;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 3: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 468) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 5 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 468, 1);
                short iditem = 4685;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 5;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 4: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 468) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 5 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 468, 1);
                short iditem = 4688;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 5;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }

            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Doiaochoang6(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 469) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 6 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 469, 1);
                short iditem = 4676;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 6;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 1: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 469) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 6 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 469, 1);
                short iditem = 4679;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 6;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 2: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 469) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 6 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 469, 1);
                short iditem = 4682;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 6;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 3: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 469) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 6 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 469, 1);
                short iditem = 4685;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 6;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 4: {
                if (conn.p.getGem() < 0 || conn.p.item.total_item_by_id(7, 465) < 1) {
                    Service.send_notice_box(conn, "Kekurangan 1 permata 6 bintang!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-0);
                conn.p.item.remove(7, 469, 1);
                short iditem = 4688;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 6;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }

            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_PhapSu(Session conn, byte index) throws IOException {

        conn.p.ResetCreateItemStar();
        switch (index) {
            case 0: {
                conn.p.id_item_rebuild = -1;
                conn.p.is_use_mayman = false;
                conn.p.id_use_mayman = -1;
                Service.send_box_UI(conn, 18);
                break;
            }
            case 1: {
                Service.send_box_UI(conn, 17);
                break;
            }
            case 2: {
                conn.p.item_replace = -1;
                conn.p.item_replace2 = -1;
                Service.send_box_UI(conn, 19);
                break;
            }
            case 3: {
                Service.send_box_UI(conn, 24);
                break;
            }
            case 4: {
                conn.p.ResetCreateItemStar();
                conn.p.id_medal_is_created = 0;
                GameSrc.initMedalMaterial(conn.p, 0);
                Service.send_box_UI(conn, 25);
                break;
            }
            case 5: {
                conn.p.ResetCreateItemStar();
                conn.p.id_medal_is_created = 1;
                GameSrc.initMedalMaterial(conn.p, 1);
                Service.send_box_UI(conn, 26);
                break;
            }
            case 6: {
                conn.p.ResetCreateItemStar();
                conn.p.id_medal_is_created = 2;
                GameSrc.initMedalMaterial(conn.p, 2);
                Service.send_box_UI(conn, 27);
                break;
            }
            case 7: {
                conn.p.ResetCreateItemStar();
                conn.p.id_medal_is_created = 3;
                GameSrc.initMedalMaterial(conn.p, 3);
                Service.send_box_UI(conn, 28);
                break;
            }
            case 8: {
                conn.p.ResetCreateItemStar();
                Service.send_box_UI(conn, 33);
                break;
            }
            case 9:
                break;
            case 10: {
                if (conn.ac_admin < 10) {
                    Service.send_notice_box(conn, "Fitur dalam perbaikan");
                    return;
                }
                ArrayList<String> myList = new ArrayList<String>();
                // Item3[] item3 = conn.p.item.bag3;
                Item3[] itemw = conn.p.item.wear;

                if (itemw == null || itemw.length < 13) {
                    Service.send_notice_box(conn, "Error pada inventaris atau perlengkapan!");
                    return;
                }
                if (itemw.length > 12 && itemw[12] != null && CheckItem.isMedal(itemw[12].id)) {
                    myList.add(itemw[12].name + " (1000 permata)");
                }
                if (myList.size() <= 0) {
                    Service.send_notice_box(conn, "Tidak ada item yang cocok!");
                    return;
                }

                send_menu_select(conn, index == 9 ? 4 : 5, myList.toArray(new String[0]));


                break;
            }
            case 11: {
                Service.send_box_UI(conn, 34);
                break;
            }
            case 12: {
                Service.send_box_UI(conn, 35);
                break;
            }
            case 13: {
                Service.send_box_UI(conn, 36);
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Admin(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                if (conn.ac_admin < 10) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin untuk melakukan ini!");
                    return;
                }
                Service.send_box_input_yesno(conn, 88, "Apakah kamu yakin ingin melakukan maintenance server?");
                break;
            }
            case 1: {
                if (conn.ac_admin <= 3) {
                    Service.send_notice_box(conn, "Kamu tidak memiliki cukup izin!");
                    return;
                }
                conn.p.updateGold(1_000_000_000);
                // conn.p.item.char_inventory(5);
                Service.send_notice_nobox_white(conn, "+ 1.000.000.000 emas");
                break;
            }
            case 2: {
                if (conn.ac_admin <= 3) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                conn.p.updateGem(1_000_000_000);
                // conn.p.item.char_inventory(5);
                Service.send_notice_nobox_white(conn, "+ 1.000.000.000 permata");
                break;
            }
            case 3: {
                if (conn.ac_admin < 10) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                SaveData.process();
                Service.send_notice_nobox_white(conn, "Data telah diperbarui");
                break;
            }
            case 4: {
                Service.sendBoxInputText(conn, 1, "Ambil Item",
                        new String[]{"Masukkan tipe (3,4,7) item :", "Masukkan ID item", "Masukkan jumlah"});
                break;
            }
            case 5: {
                if (conn.ac_admin <= 3) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                Service.sendBoxInputText(conn, 2, "Tambah Level", new String[]{"Masukkan level :"});
                break;
            }
            case 6: {
                Service.sendBoxInputText(conn, 4, "Atur Xp", new String[]{"Masukkan pengganda x :"});
                break;
            }
            case 7: {
                Service.sendBoxInputText(conn, 18, "Nama Karakter", new String[]{"Masukkan Nama Karakter :"});
                break;
            }
            case 8: {
                Service.sendBoxInputText(conn, 19, "Nama Karakter", new String[]{"Masukkan Nama Karakter :"});
                break;
            }
            case 9: {
                if (conn.ac_admin < 10) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                Manager.isLockVX = !Manager.isLockVX;
                Service.send_notice_box(conn, "Roda keberuntungan telah " + (Manager.isLockVX ? "dikunci" : "dibuka"));
                // Service.send_box_input_text(conn, 19, "Tên nhân vật", new String[]{"Nhập Tên
                // nhân vật :"});
                break;
            }
            case 10: {
                if (conn.ac_admin < 10) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                Manager.isTrade = !Manager.isTrade;
                Service.send_notice_box(conn, "Transaksi telah " + (Manager.isTrade ? "dibuka" : "ditutup"));
                break;
            }
            case 11: {
                if (conn.ac_admin < 10) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                Manager.isKmb = !Manager.isKmb;
                Service.send_notice_box(conn, "Transaksi telah " + (Manager.isKmb ? "dibuka" : "ditutup"));
                break;
            }
            case 12: {
                if (conn.ac_admin < 4) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                for (Pet pet : conn.p.mypet) {
                    if (pet.time_born > 0) {
                        pet.time_born = 3;
                    }
                }
                Service.send_notice_box(conn, "Selesai");
                break;
            }
            case 13: {
                if (conn.ac_admin < 4) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                Manager.BuffAdmin = !Manager.BuffAdmin;
                Service.send_notice_box(conn, "Buff Admin telah: " + (Manager.BuffAdmin ? "Aktif" : "Nonaktif"));
                break;
            }
            case 14: {
                if (conn.ac_admin < 4) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                Manager.BuffAdminMaterial = !Manager.BuffAdminMaterial;
                Service.send_notice_box(conn,
                        "Buff material untuk Admin telah: " + (Manager.BuffAdminMaterial ? "Aktif" : "Nonaktif"));
                break;
            }
            case 15: {
                if (conn.ac_admin < 5) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                Manager.gI().mine.openAttack();
                Manager.gI().chatKTGprocess(" Waktu penaklukan tambang telah tiba!");
                break;
            }
            case 16: {
                if (conn.ac_admin < 10) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                Manager.gI().mine.closeAttack();
                Manager.gI().chatKTGprocess(" Waktu penaklukan tambang telah ditutup!");
                break;
            }
            case 17: {
                // Buka / Tutup King Cup
                if (conn.ac_admin < 5) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                if (KingCup.running) {
                    KingCup.close();
                    Service.send_notice_box(conn, "King Cup telah DITUTUP.");
                } else {
                    KingCupManager.TURN_KING_CUP++;
                    KingCupManager.updateTurn();
                    KingCup.start();
                    Service.send_notice_box(conn,
                        "King Cup telah DIBUKA!\nTurn ke-" + KingCupManager.TURN_KING_CUP
                        + " / " + KingCupManager.MAX_TURN);
                }
                break;
            }
            case 18: {
                if (conn.ac_admin < 10) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                if (Manager.gI().event == 2) {
                    Event_2.ClearMob();
                    Event_2.ResetMob();
                    Service.send_notice_box(conn, "Reset monster event telah dilakukan");
                }
                break;
            }
            case 19: {
                if (conn.ac_admin < 10) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                if (CastleSiegeManager.isRegister) {
                    CastleSiegeManager.EndRegister();
                } else {
                    CastleSiegeManager.StartRegister();
                }
                Service.send_notice_box(conn,
                        "Telah " + (CastleSiegeManager.isRegister ? "membuka" : "menutup")
                                + " pendaftaran penaklukan kastil");
                break;
            }
            case 20: {
                Service.send_notice_box(conn, "Fitur sedang disempurnakan.");
                break;
            }
            case 21: {
                if (conn.ac_admin < 4) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                Service.sendBoxInputText(conn, 21, "Pindah Peta",
                        new String[]{"Masukkan ID Peta", "Masukkan koordinat x", "Masukkan koordinat y"});
                break;
            }
            case 22: {
                if (conn.ac_admin < 10) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                Manager.gI().load_config();
                break;
            }
            case 23: {
                if (conn.ac_admin < 10) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                Manager.logErrorLogin = !Manager.logErrorLogin;
                Service.send_notice_box(conn,
                        "Anda telah " + (Manager.logErrorLogin ? "Mengaktifkan" : "Menonaktifkan") + " log error");
                break;
            }
            case 24: {
                Service.sendBoxInputText(conn, 24, "Putuskan Koneksi",
                        new String[]{"Masukkan Tipe :", "Masukkan Nama :"});
                break;
            }
            case 25: {
                break;
            }
            case 26: {

                break;
            }
            case 27: {
                if (conn.ac_admin < 10) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                Service.sendBoxInputText(conn, 99, "Masukkan informasi",
                        new String[]{"Nama karakter", "Jumlah uang", "Coin"});

                break;
            }
            case 28: {
                if (conn.ac_admin < 10) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                // Hot-reload quest template dari database tanpa restart server
                event_daily.DailyQuest.reload();
                game.quest.QuestManager.getInstance().reloadQuestTemplate();
                Service.send_notice_box(conn, "✅ Reload Quest berhasil!\nDaily Quest & Quest Template telah diperbarui dari database.");
                break;
            }
            case 29: {
                // Broadcast Server Wide
                if (conn.ac_admin < 4) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                Service.sendBoxInputText(conn, 70, "📢 Broadcast Server Wide",
                        new String[]{"Tipe (1=Kotak, 2=Kuning, 3=Putih)", "Pesan Broadcast"});
                break;
            }
            case 30: {
                // Info status King Cup
                if (conn.ac_admin < 4) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                String kcStatus = KingCup.running ? "SEDANG BERLANGSUNG" : "BELUM BUKA";
                Service.send_notice_box(conn,
                    "=== Status King Cup ===\n"
                    + "Status  : " + kcStatus + "\n"
                    + "Turn    : " + KingCupManager.TURN_KING_CUP + " / " + KingCupManager.MAX_TURN + "\n"
                    + "Peserta Lv 60-100   : " + KingCupManager.group_60_100.size() + "\n"
                    + "Peserta Lv 101-160  : " + KingCupManager.group_101_160.size() + "\n"
                    + "Peserta Lv 161-200  : " + KingCupManager.group_161_200.size() + "\n"
                    + "Peserta Lv 201-230  : " + KingCupManager.group_201_230.size() + "\n"
                    + "Peserta Lv 231-250  : " + KingCupManager.group_231_250.size() + "\n"
                    + "Peserta Lv 251-300  : " + KingCupManager.group_251_300.size()
                );
                break;
            }
            case 31: {
                // Naikkan turn King Cup manual tanpa membuka event
                if (conn.ac_admin < 10) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                if (KingCup.running) {
                    Service.send_notice_box(conn, "Tidak bisa ubah turn saat King Cup sedang berjalan!");
                    return;
                }
                KingCupManager.TURN_KING_CUP++;
                KingCupManager.updateTurn();
                Service.send_notice_box(conn,
                    "Turn King Cup dinaikkan menjadi " + KingCupManager.TURN_KING_CUP
                    + " / " + KingCupManager.MAX_TURN);
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    /**
     * Menu SMOD (staff junior, ac_admin 1-9) — dipicu chat "smod". Cuma tool
     * moderasi ringan: cek info, teleport/panggil player, kirim peringatan,
     * mute/unmute. TIDAK ada akses kirim item/gold/gem, setting exp, atau
     * kontrol server (itu tetap khusus "admin" / ac_admin >= 10 via Menu_Admin).
     */
    private static void Menu_Smod(Session conn, byte index) throws IOException {
        // Defense-in-depth: menu ini cuma boleh diakses staff (ac_admin >= 2).
        // Command chat "smod" udah nge-gate ini juga, tapi dicek ulang di sini
        // biar aman kalau suatu saat menu 127 dipanggil dari jalur lain.
        if (conn.ac_admin < 2) {
            Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
            return;
        }
        switch (index) {
            case 0: {
                Service.sendBoxInputText(conn, 71, "Cek Info Player", new String[]{"Nickname"});
                break;
            }
            case 1: {
                Service.sendBoxInputText(conn, 72, "Teleport ke Player", new String[]{"Nickname"});
                break;
            }
            case 2: {
                Service.sendBoxInputText(conn, 73, "Panggil Player", new String[]{"Nickname"});
                break;
            }
            case 3: {
                Service.sendBoxInputText(conn, 74, "Kirim Peringatan", new String[]{"Nickname", "Pesan"});
                break;
            }
            case 4: {
                // Reuse handler mute yang udah ada (box 18) — gate internalnya
                // sekarang ac_admin >= 2 biar smod juga bisa pakai.
                Service.sendBoxInputText(conn, 18, "Nama Karakter", new String[]{"Masukkan Nama Karakter :"});
                break;
            }
            case 5: {
                // Reuse handler unmute yang udah ada (box 19), sama kayak di atas.
                Service.sendBoxInputText(conn, 19, "Nama Karakter", new String[]{"Masukkan Nama Karakter :"});
                break;
            }
            case 6: {
                // Reuse handler broadcast yang udah ada (box 70) — gate
                // internalnya sekarang ac_admin >= 2, dan pesannya otomatis
                // dikasih label "[Moderator]" bukan "[Admin]" buat SMOD.
                Service.sendBoxInputText(conn, 70, "📢 Broadcast Server Wide",
                        new String[]{"Tipe (1=Kotak, 2=Kuning, 3=Putih)", "Pesan Broadcast"});
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    /**
     * Menu SMOD (staff junior, ac_admin 1-9) — dipicu chat "smod". Cuma tool
     * moderasi ringan: cek info, teleport/panggil player, kirim peringatan,
     * mute/unmute. TIDAK ada akses kirim item/gold/gem, setting exp, atau
     * kontrol server (itu tetap khusus "admin" / ac_admin >= 10 via Menu_Admin).
     */

    private static void Menu_Mod(Session conn, byte index) throws IOException {
        // Defense-in-depth: menu ini cuma boleh diakses staff (ac_admin >= 2).
        // Command chat "smod" udah nge-gate ini juga, tapi dicek ulang di sini
        // biar aman kalau suatu saat menu 127 dipanggil dari jalur lain.
        if (conn.ac_admin < 1) {
            Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
            return;
        }
        switch (index) {
            case 0: {
                Service.sendBoxInputText(conn, 71, "Cek Info Player", new String[]{"Nickname"});
                break;
            }
            case 1: {
                // Reuse handler broadcast yang udah ada (box 70) — gate
                // internalnya sekarang ac_admin >= 2, dan pesannya otomatis
                // dikasih label "[Moderator]" bukan "[Admin]" buat SMOD.
                Service.sendBoxInputText(conn, 70, "📢 Broadcast Server Wide",
                        new String[]{"Tipe (1=Kotak, 2=Kuning, 3=Putih)", "Pesan Broadcast"});
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }


    private static void Menu_Zulu(Session conn, byte index) throws IOException {

        switch (index) {
            case 0:
                List<Integer> female = List.of(6, 7, 10, 11, 14, 15, 21, 22, 23, 26, 27, 30, 31, 36, 37, 40, 41, 43, 45, 47, 50, 51, 53, 54);
                List<Integer> exclude = List.of(0, 1, 2, 3, 32, 33, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65);
                List<Integer> items =
                        switch (conn.p.clazz) {
                            case 0, 1 -> IntStream.range(0, 67)
                                    .filter(value -> !exclude.contains(value))
                                    .filter(value -> !female.contains(value))
                                    .boxed()
                                    .toList();
                            default -> female;
                        };

                Message m = new Message(23);
                m.writer().writeUTF("Toko Rambut");
                m.writer().writeByte(2);

                m.writer().writeShort(items.size());
                for (int id : items) {
                    m.writer().writeShort(id);
                    m.writer().writeUTF(String.format("Style %d", id));
                    m.writer().writeShort(id);
                    m.writer().writeLong(500);
                    m.writer().writeByte(1);
                    m.writer().writeByte(0); // Options
                }
                conn.addmsg(m);
                break;
            case 1: {
                if (conn.p.diemdanh == 1) {
                    conn.p.diemdanh = 0;
                    int ngoc_ = Util.random(100, 1000);
                    conn.p.updateGem(ngoc_);
                    Log.gI().add_log(conn.p.name,
                            "Absensi harian mendapatkan " + Util.number_format(ngoc_) + " permata");
                    conn.p.item.charInventory(5);
                    Service.send_notice_box(conn, "Kamu telah berhasil absensi, mendapatkan " + ngoc_ + " permata");
                } else {
                    Service.send_notice_box(conn, "Kamu sudah melakukan absensi hari ini");
                }
                break;
            }
            case 2: {
                Service.sendBoxInputText(conn, 5, "Tukar koin ke permata",
                        new String[]{"Kurs 1000 koin = 500 permata"});
                break;
            }
            case 3: {
                Service.sendBoxInputText(conn, 14, "Tukar koin ke emas",
                        new String[]{"Kurs 1000 koin = 5jt emas"});
                break;
            }

            // case 4: {
            // Service.send_notice_box(conn, "Chức năng không còn tồn tại.");
            // //Service.send_box_input_yesno(conn, 121, "1000 ngọc cho 2h, hãy xác nhận");
            // break;
            // }
            // case 5: {
            // EffTemplate ef = conn.p.get_eff(-126);
            // if (ef != null && ef.time > System.currentTimeMillis()) {
            // Service.send_notice_box(conn,
            // "Thời gian còn lại : " + Util.getTime((int) (ef.time -
            // System.currentTimeMillis()) / 1000));
            // } else {
            // Service.send_notice_box(conn, "Chức năng không còn tồn tại.");
            // //Service.send_notice_box(conn, "Chưa đăng ký kiểm tra cái gì?");
            // }
            // break;
            // }
            case 4: {
                if (conn.p.type_exp == 0) {
                    conn.p.type_exp = 1;
                    Service.send_notice_box(conn, "Penerimaan EXP telah diaktifkan");
                } else {
                    conn.p.type_exp = 0;
                    Service.send_notice_box(conn, "Penerimaan EXP telah dinonaktifkan");
                }
                break;
            }
            case 5: {
                Service.sendBoxInputText(conn, 30, "Ubah Kata Sandi", new String[]{"masukkan kata sandi lama",
                        "masukkan kata sandi baru", "masukkan kembali kata sandi baru"});
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_ChangeZone(Session conn) throws IOException {
        GameMap[] gameMap = GameMap.getMapById(conn.p.map.mapId);
        if (gameMap != null) {
            Message m = new Message(54);
            if (GameMap.is_map_cant_save_site(conn.p.map.mapId)) {
                m.writer().writeByte(conn.p.map.maxzone);
            } else {
                m.writer().writeByte(conn.p.map.maxzone + 1);
            }
            //
            for (int i = 0; i < conn.p.map.maxzone; i++) {
                if (gameMap[i].players.size() > (gameMap[i].maxplayer - 2)) {
                    m.writer().writeByte(2); // redzone
                } else if (gameMap[i].players.size() >= (gameMap[i].maxplayer / 2)) {
                    m.writer().writeByte(1); // yellow zone
                } else {
                    m.writer().writeByte(0); // green zone
                }
                if (i == 4 && GameMap.isSummonMap(conn.p.map, false)) {
                    m.writer().writeByte(4);
                } else {
                    m.writer().writeByte(0);
                }
            }
            if (!GameMap.is_map_cant_save_site(conn.p.map.mapId)) {
                m.writer().writeByte(1);
                m.writer().writeByte(5);
            }
            for (int i = 0; i < conn.p.map.maxzone; i++) {
                m.writer().writeUTF(
                        "Area " + (gameMap[i].zoneId + 1) + " (" + gameMap[i].players.size() + "/" + gameMap[i].maxplayer + ")");
            }
            if (!GameMap.is_map_cant_save_site(conn.p.map.mapId)) {
                m.writer().writeUTF("Area Perdagangan");
            }
            //
            conn.addmsg(m);
            m.cleanup();
        }
    }

    private static void Menu_Alisama(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                Service.send_box_UI(conn, 9);
                break;
            }
            case 1: {
                Service.send_box_UI(conn, 10);
                break;
            }
            case 2: {
                Service.send_box_UI(conn, 11);
                break;
            }
            case 3: {
                Service.send_box_UI(conn, 12);
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    public static void Menu_DaDichChuyen10(Session conn, byte index) throws IOException {
        if (conn.ac_admin < 10 && conn.p.item.wear[11] != null && (conn.p.item.wear[11].id == 3599 || conn.p.item.wear[11].id == 3593
                || conn.p.item.wear[11].id == 3596)) {
            return;
        }
        Vgo vgo = null;
        switch (index) {
            case 0: { // desa serigala
                vgo = new Vgo();
                vgo.toMap = 1;
                vgo.toX = 432;
                vgo.toY = 354;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 1: { // kota harta karun
                vgo = new Vgo();
                vgo.toMap = 33;
                vgo.toX = 432;
                vgo.toY = 480;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 2: { // mataram kuno
                vgo = new Vgo();
                vgo.toMap = 103;
                vgo.toX = 500;
                vgo.toY = 1000;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 3: { // area perdangan
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan, harap aktifkan");
                    return;
                }
                vgo = new Vgo();
                vgo.toMap = 82;
                vgo.toX = 432;
                vgo.toY = 354;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 4: { // gua api
                vgo = new Vgo();
                vgo.toMap = 4;
                vgo.toX = 888;
                vgo.toY = 672;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 5: { // hutan ilusi
                vgo = new Vgo();
                vgo.toMap = 5;
                vgo.toX = 1056;
                vgo.toY = 864;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 6: { // lembah misterius
                vgo = new Vgo();
                vgo.toMap = 8;
                vgo.toX = 576;
                vgo.toY = 222;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 7: { // danau kenangan
                vgo = new Vgo();
                vgo.toMap = 9;
                vgo.toX = 1243;
                vgo.toY = 876;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 8: { // pantai
                vgo = new Vgo();
                vgo.toMap = 11;
                vgo.toX = 286;
                vgo.toY = 708;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 9: { // jurang batu 
                vgo = new Vgo();
                vgo.toMap = 12;
                vgo.toX = 240;
                vgo.toY = 732;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 10: { // karang tersembunyi
                vgo = new Vgo();
                vgo.toMap = 13;
                vgo.toX = 150;
                vgo.toY = 979;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 11: { // rawa
                vgo = new Vgo();
                vgo.toMap = 15;
                vgo.toX = 469;
                vgo.toY = 1099;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 12: { // kuil kuno
                vgo = new Vgo();
                vgo.toMap = 16;
                vgo.toX = 673;
                vgo.toY = 1093;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            
            case 13: { // gua kelelawar
                vgo = new Vgo();
                vgo.toMap = 17;
                vgo.toX = 678;
                vgo.toY = 630;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_DaDichChuyen33(Session conn, byte index) throws IOException {
        if (conn.ac_admin < 10 && conn.p.item.wear[11] != null && (conn.p.item.wear[11].id == 3599 || conn.p.item.wear[11].id == 3593
                || conn.p.item.wear[11].id == 3596)) {
            return;
        }
        Vgo vgo = null;
        switch (index) {
            case 0: { // kota pelabuhan
                vgo = new Vgo();
                vgo.toMap = 67;
                vgo.toX = 576;
                vgo.toY = 222;
                conn.p.changeMap(conn.p, vgo);
                break;
            } 
            case 1: { // kota harta karun
                vgo = new Vgo();
                vgo.toMap = 33;
                vgo.toX = 432;
                vgo.toY = 480;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 2: { // area perdagangan
                // vgo = new Vgo();
                // vgo.idmapgo = 82;
                // vgo.xnew = 432;
                // vgo.ynew = 354;
                // conn.p.changemap(conn.p, vgo);
                Service.send_notice_box(conn, "Area ini sedang dalam perbaikan");
                break;
            }
            case 3: { // gurun
                vgo = new Vgo();
                vgo.toMap = 20;
                vgo.toX = 787;
                vgo.toY = 966;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 4: { // Jurang Tenggelam
                vgo = new Vgo();
                vgo.toMap = 22;
                vgo.toX = 120;
                vgo.toY = 678;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 5: { // kuburan pasir
                vgo = new Vgo();
                vgo.toMap = 24;
                vgo.toX = 576;
                vgo.toY = 222;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 6: { // mata air hantu
                vgo = new Vgo();
                vgo.toMap = 26;
                vgo.toX = 576;
                vgo.toY = 222;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 7: { // makan lantai 1
                vgo = new Vgo();
                vgo.toMap = 29;
                vgo.toX = 576;
                vgo.toY = 222;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 8: { // makan lantai 3
                vgo = new Vgo();
                vgo.toMap = 31;
                vgo.toX = 360;
                vgo.toY = 624;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 9: { // hutan dataran tinggi
                vgo = new Vgo();
                vgo.toMap = 37;
                vgo.toX = 150;
                vgo.toY = 674;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 10: { // tebing curam
                vgo = new Vgo();
                vgo.toMap = 39;
                vgo.toX = 199;
                vgo.toY = 882;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 11: { // jalan ke dunia atas
                vgo = new Vgo();
                vgo.toMap = 41;
                vgo.toX = 187;
                vgo.toY = 462;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 12: { // jalan ke dunia bawah
                vgo = new Vgo();
                vgo.toMap = 43;
                vgo.toX = 228;
                vgo.toY = 43;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 13: { // gerbang dunia bawah
                vgo = new Vgo();
                vgo.toMap = 45;
                vgo.toX = 576;
                vgo.toY = 222;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 14: { // taman
                vgo = new Vgo();
                vgo.toMap = 50;
                vgo.toX = 300;
                vgo.toY = 300;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_DaDichChuyen55(Session conn, byte index) throws IOException {
        if (conn.ac_admin < 10 && conn.p.item.wear[11] != null && (conn.p.item.wear[11].id == 3599 || conn.p.item.wear[11].id == 3593
                || conn.p.item.wear[11].id == 3596)) {
            return;
        }
        Vgo vgo = null;
        switch (index) {
            case 0: { // kota pelabuhan
                vgo = new Vgo();
                vgo.toMap = 67;
                vgo.toX = 576;
                vgo.toY = 222;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 1: { // area perdagangan
                // vgo = new Vgo();
                // vgo.idmapgo = 82;
                // vgo.xnew = 432;
                // vgo.ynew = 354;
                // conn.p.changemap(conn.p, vgo);
                Service.send_notice_box(conn, "Area ini sedang dalam perbaikan");
                break;
            }
            case 2: { // labirin
                vgo = new Vgo();
                vgo.toMap = 74;
                vgo.toX = 258;
                vgo.toY = 354;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 3: { // labirin lantai 3
                vgo = new Vgo();
                vgo.toMap = 77;
                vgo.toX = 576;
                vgo.toY = 222;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 4: { // kota musim dingin
                vgo = new Vgo();
                vgo.toMap = 93;
                vgo.toX = 462;
                vgo.toY = 342;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 5: { // lembah es
                vgo = new Vgo();
                vgo.toMap = 94;
                vgo.toX = 306;
                vgo.toY = 240;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 6: { // kaki gunung salju
                vgo = new Vgo();
                vgo.toMap = 95;
                vgo.toX = 390;
                vgo.toY = 162;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 7: { // celah es
                vgo = new Vgo();
                vgo.toMap = 96;
                vgo.toX = 198;
                vgo.toY = 666;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 8: { // jurang kabut
                vgo = new Vgo();
                vgo.toMap = 97;
                vgo.toX = 432;
                vgo.toY = 168;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 9: { // stasiun gunung salju
                vgo = new Vgo();
                vgo.toMap = 98;
                vgo.toX = 270;
                vgo.toY = 132;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            case 10: { // kota harta karun
                vgo = new Vgo();
                vgo.toMap = 33;
                vgo.toX = 432;
                vgo.toY = 480;
                conn.p.changeMap(conn.p, vgo);
                break;
            }
            default: {
                Service.send_notice_box(conn, "Chưa có chức năng");
                break;
            }
        }
    }

    private static void Menu_Hammer(Session conn, byte index, byte idmenu) throws IOException {
        if (idmenu == 0) {
            switch (index) {
                case 0: {
                    Service.send_box_UI(conn, 5);
                    break;
                }
                case 1: {
                    Service.send_box_UI(conn, 6);
                    break;
                }
                case 2: {
                    Service.send_box_UI(conn, 7);
                    break;
                }
                case 3: {
                    Service.send_box_UI(conn, 8);
                    break;
                }
                case 4: // chế tạo tinh tú
                {
                    if (!Manager.isCreateItemStarEnabled) {
                        Service.send_notice_box(conn, "Fitur pembuatan item bintang sedang dinonaktifkan sementara.");
                        break;
                    }
                    send_menu_select(conn, -5, new String[]{"Ksatria", "Assassin", "Penyihir", "Penembak"}, (byte) 1);
                    break;
                }
                case 5:// nâng cấp tinh tú
                {
                    if (!Manager.isCreateItemStarEnabled) {
                        Service.send_notice_box(conn, "Fitur upgrade item bintang sedang dinonaktifkan sementara.");
                        break;
                    }
                    conn.p.isCreateItemStar = true;
                    Service.send_box_UI(conn, 33);
                    // send_menu_select(conn,-5100,new String[]{"Chiến binh","Sát thủ","Pháp sư","Xạ
                    // thủ"});
                    break;
                }
                case 6: { // giap sieu nhan
                    if (conn.p.item.wear[20] == null) {
                        Service.send_notice_box(conn, "Tidak ada item untuk dilepas");
                    } else {
                        Item3 buffer = conn.p.item.wear[20];
                        conn.p.item.wear[20] = null;
                        conn.p.item.add_item_bag3(buffer);
                        conn.p.item.charInventory(3);
                        conn.p.fashion = PartFashion.getPart(conn.p);
                        Service.sendPlayerWear(conn.p);
                        Service.sendMainCharInfo(conn.p);
                        MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                        Service.send_notice_box(conn, "Berhasil dilepas");
                    }
                    break;
                }
                case 7: { // thao danh hiẹu
                    if (conn.p.item.wear[19] == null) {
                        Service.send_notice_box(conn, "Tidak ada item untuk dilepas");
                    } else {
                        Item3 buffer = conn.p.item.wear[19];
                        conn.p.item.wear[19] = null;
                        conn.p.item.add_item_bag3(buffer);
                        conn.p.item.charInventory(3);
                        conn.p.fashion = PartFashion.getPart(conn.p);
                        Service.sendPlayerWear(conn.p);
                        Service.sendMainCharInfo(conn.p);
                        MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                        Service.send_notice_box(conn, "Berhasil dilepas");
                    }
                    break;
                }
                case 8: {
                    send_menu_select(conn, 1000, new String[]{"Armor Superhero Perak (harian)  ",
                            "Armor Superhero Ungu (harian) ", " Armor Superhero Biru (harian)  ",
                            "Armor Superhero Emas (harian)"});
                    break;
                }
                default: {
                    Service.send_notice_box(conn, "Fitur belum tersedia");
                    break;
                }
            }
        } else if (idmenu == 1) {
            String[] nemu = new String[]{"Helm", "Baju", "Celana", "Sepatu", "Sarung Tangan", "Cincin", "Senjata",
                    "Kalung"};
            send_menu_select(conn, -5, nemu, (byte) (10 + index));
        } else if (idmenu >= 10 && idmenu <= 13) {
            if (!Manager.isCreateItemStarEnabled) {
                Service.send_notice_box(conn, "Fitur pembuatan item bintang sedang dinonaktifkan sementara.");
                return;
            }
            conn.p.isCreateItemStar = true;
            conn.p.ClazzItemStar = (byte) (idmenu - 10);
            conn.p.TypeItemStarCreate = index;
            Service.send_box_UI(conn, 40 + index);
        }
    }

    private static void Menu_Alisama(Session conn, byte index, byte idmenu) throws IOException {
        if (idmenu == 0) {
            switch (index) {
                case 0: {
                    Service.send_box_UI(conn, 9);
                    break;
                }
                case 1: {
                    Service.send_box_UI(conn, 10);
                    break;
                }
                case 2: {
                    Service.send_box_UI(conn, 11);
                    break;
                }
                case 3: {
                    Service.send_box_UI(conn, 12);
                    break;
                }
                default:
                    Service.send_notice_box(conn, "Fitur belum tersedia");
                    break;
            }
        }
    }

    private static void Menu_GiapSieuNhan(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                if (conn.p.getGem() < 20000 || conn.p.item.total_item_by_id(4, 319) < 200) {
                    Service.send_notice_box(conn, "Dibutuhkan minimal 20000 permata dan 200 aura perak untuk menukar!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-20000);
                conn.p.item.remove(4, 319, 200);
                short iditem = 4784;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 3;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 1: {
                if (conn.p.getGem() < 20000 || conn.p.item.total_item_by_id(4, 320) < 200) {
                    Service.send_notice_box(conn, "Dibutuhkan minimal 20000 permata dan 200 aura ungu untuk menukar!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-20000);
                conn.p.item.remove(4, 320, 200);
                short iditem = 4785;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 3;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 2: {
                if (conn.p.getGem() < 20000 || conn.p.item.total_item_by_id(4, 321) < 200) {
                    Service.send_notice_box(conn, "Dibutuhkan minimal 20000 permata dan 200 aura ungu untuk menukar!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-20000);
                conn.p.item.remove(4, 321, 200);
                short iditem = 4786;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 3;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            case 3: {
                if (conn.p.getGem() < 20000 || conn.p.item.total_item_by_id(4, 322) < 200) {
                    Service.send_notice_box(conn, "Dibutuhkan minimal 20000 permata dan 200 aura ungu untuk menukar!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tidak cukup slot kosong!");
                    return;
                }
                conn.p.updateGem(-20000);
                conn.p.item.remove(4, 322, 200);
                short iditem = 4787;
                Item3 itbag = new Item3();
                itbag.id = iditem;
                itbag.name = ItemTemplate3.item.get(iditem).getName();
                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                itbag.type = ItemTemplate3.item.get(iditem).getType();
                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                itbag.op = ItemTemplate3.item.get(iditem).getOp();
                itbag.tier = 0;
                itbag.islock = false;
                itbag.time_use = 0;
                itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 3;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.charInventory(5);

                List<box_item_template> ids = new ArrayList<>();
                ids.add(new box_item_template(iditem, (short) 1, (byte) 3));
                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;

            }
        }
    }

    private static void Menu_Doubar(Session conn, byte index, byte idmenu) throws IOException {
        if (idmenu == 1) {
            String s = "Terjadi kesalahan";
            if (index == 0) {
                s = BossManager.getInfoBoss(83);
            } else if (index == 1) {
                s = BossManager.getInfoBoss(84);
            } else if (index == 2) {
                s = BossManager.getInfoBoss(101);
            } else if (index == 3) {
                s = BossManager.getInfoBoss(103);
            } else if (index == 4) {
                s = BossManager.getInfoBoss(104);
            } else if (index == 5) {
                s = BossManager.getInfoBoss(105);
            } else if (index == 6) {
                s = BossManager.getInfoBoss(106);
            } else if (index == 7) {
                s = BossManager.getInfoBoss(149);
            } else if (index == 8) {
                s = BossManager.getInfoBoss(155);
            } else if (index == 9) {
                s = BossManager.getInfoBoss(195);
            } else if (index == 10) {
                s = BossManager.getInfoBoss(173);
            } else if (index == 11) {
                s = BossManager.getInfoBoss(197);
            } else if (index == 12) {
                s = BossManager.getInfoBoss(196);
            } else if (index == 13) {
                s = BossManager.getInfoBoss(186);
            } else if (index == 14) {
                s = BossManager.getInfoBoss(187);
            } else if (index == 15) {
                s = BossManager.getInfoBoss(188);
            } else if (index == 16) {
                s = BossManager.getInfoBoss(174);
            }
            Service.send_notice_box(conn, s);
            return;

        }
        // cua hang item wear
        switch (index) {
            case 0: {
                Service.send_box_UI(conn, 1);
                break;
            }
            case 1: {
                Service.send_box_UI(conn, 2);
                break;
            }
            case 2: {
                Service.send_box_UI(conn, 3);
                break;
            }
            case 3: {
                Service.send_box_UI(conn, 4);
                break;
            }
            case 4: {
                send_menu_select(conn, -4, new String[]{
                        "Kambing Perak",
                        "Kambing Emas",
                        "Wanita Ular",
                        "Raja Kalajengking",
                        "Iblis Mata Satu",
                        "Iblis Kepala Banteng",
                        "Ksatria Neraka",
                        "Ratu Laba-laba",
                        "Kerangka Raksasa",
                        "Bos Event 7x",
                        "Bos Event 8x",
                        "Bos Event 11x",
                        "Bos Event 13x",
                        "Bos Event 0x",
                        "Bos Event 1x",
                        "Bos Event 2x",
                        "Boss Event"

                }, (byte) 1);
                break;
            }
            case 5: {
                Service.send_box_UI(conn, 37);
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_keva(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: { // cua hang potion
                Service.send_box_UI(conn, 0);
                break;
            }
            case 1: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                send_menu_select(conn, 600, new String[]{"Area Desa 10x", "Area Desa 11x", "Area Desa 12x",
                        "Area Desa 13x", "Area Desa 14x"});
                break;
            }
            case 2: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                send_menu_select(conn, 601,
                        new String[]{"Area Boss Event 0x", "Area Boss Event 1x", "Area Boss Event 2x",
                                "Area Boss Event 7x", "Area Boss Event 8x", "Area Boss Event 11x",
                                "Area Boss Event 13x"});
                break;
            }
            case 3: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 103;
                vgo.toX = 282;
                vgo.toY = 186;
                conn.p.changeMap(conn.p, vgo);

                break;
            }
            case 4: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 1;
                vgo.toX = 282;
                vgo.toY = 186;
                conn.p.changeMap(conn.p, vgo);

                break;
            }

            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Mr_Haku(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGold() < 500) {
                    Service.send_notice_box(conn, "Tidak cukup 500 emas");
                    return;
                }
                conn.p.updateGold(-500);

                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 67;
                vgo.toX = 576;
                vgo.toY = 222;
                conn.p.changeMap(conn.p, vgo);

                break;
            }

            default:
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
        }
    }

    private static void Menu_Langphusuongup(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 104;
                vgo.toX = 282;
                vgo.toY = 186;
                conn.p.changeMap(conn.p, vgo);

                break;
            }
            case 1: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 105;
                vgo.toX = 282;
                vgo.toY = 186;
                conn.p.changeMap(conn.p, vgo);

                break;
            }
            case 2: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 106;
                vgo.toX = 282;
                vgo.toY = 186;
                conn.p.changeMap(conn.p, vgo);

                break;
            }
            case 3: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 107;
                vgo.toX = 282;
                vgo.toY = 186;
                conn.p.changeMap(conn.p, vgo);

                break;
            }
            case 4: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 108;
                vgo.toX = 282;
                vgo.toY = 186;
                conn.p.changeMap(conn.p, vgo);

                break;
            }

            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Langphusuongboss(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 109;
                vgo.toX = 282;
                vgo.toY = 186;
                conn.p.changeMap(conn.p, vgo);

                break;
            }
            case 1: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 110;
                vgo.toX = 282;
                vgo.toY = 186;
                conn.p.changeMap(conn.p, vgo);

                break;
            }
            case 2: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 111;
                vgo.toX = 282;
                vgo.toY = 186;
                conn.p.changeMap(conn.p, vgo);

                break;
            }
            case 3: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 112;
                vgo.toX = 282;
                vgo.toY = 186;
                conn.p.changeMap(conn.p, vgo);

                break;
            }
            case 4: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 113;
                vgo.toX = 282;
                vgo.toY = 186;
                conn.p.changeMap(conn.p, vgo);

                break;
            }
            case 5: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 114;
                vgo.toX = 282;
                vgo.toY = 186;
                conn.p.changeMap(conn.p, vgo);

                break;
            }
            case 6: {
                if (conn.status != 0) {
                    Service.send_notice_box(conn, "Akun belum diaktifkan,");
                    return;
                }
                if (conn.p.getGem() < 100) {
                    Service.send_notice_box(conn, "Tidak cukup 100 permata");
                    return;
                }
                conn.p.updateGem(-100);

                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 115;
                vgo.toX = 282;
                vgo.toY = 186;
                conn.p.changeMap(conn.p, vgo);

                break;
            }

            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Lisa(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: { // cua hang potion
                Service.send_box_UI(conn, 0);
                break;
            }
            case 1: {
                if (conn.p.item.total_item_by_id(4, 52) > 0) {
                    MoLy.show_table_to_choose_item(conn.p);
                } else {
                    Service.send_notice_box(conn, "Tiket pembuka di inventaris tidak cukup");
                }
                break;
            }
            case 2: { // cua hang potion
                Service.sendBoxInputText(conn, 22, "% pajak", new String[]{"Masukkan % pajak 0 - 5%"});
                break;
            }
            case 3: {
                // FIX "hadiah BXH BTF tidak pernah bisa diambil": sebelumnya kode
                // ini baca dari Battlefield.gI().get_bxh(...)/get_index_bxh(...) -
                // BXH milik SISTEM LAMA (event_daily.Battlefield) yang tidak lagi
                // diisi sejak battlefield diganti BTF (event baru punya
                // List<MemberBattlefield> bxh sendiri, lihat
                // BTF#getBxhEntry()/#getBxhRank()). Akibatnya menu ini SELALU
                // bilang "Nama tidak ada dalam daftar" untuk siapa pun, walau
                // pemain itu benar-benar masuk top 10 BTF kemarin.
                // Sekalian tambah pengecekan MemberBattlefield#received yang
                // sebelumnya tidak pernah dipakai, supaya hadiah tidak bisa
                // diambil berkali-kali dengan buka menu ini berulang-ulang.
                BTF btf = GameEventManager.gI().getEvent(BTF.class);
                MemberBattlefield temp = btf != null ? btf.getBxhEntry(conn.p.name) : null;
                if (temp != null) {
                    if (temp.received) {
                        Service.send_notice_box(conn, "Hadiah sudah pernah diambil");
                        break;
                    }
                    switch (btf.getBxhRank(temp)) {
                        case 0: {
                            short[] id_ = new short[]{3, 2, 53, 54, 18};
                            short[] id2_ = new short[]{5, 5, 1, 1, 10};
                            short[] id3_ = new short[]{7, 7, 4, 4, 4};
                            for (int i = 0; i < id_.length; i++) {
                                Item47 it = new Item47();
                                it.id = id_[i];
                                it.quantity = id2_[i];
                                conn.p.item.add_item_bag47(id3_[i], it);
                            }
                            break;
                        }
                        case 1:
                        case 2: {
                            short[] id_ = new short[]{3, 2, 18};
                            short[] id2_ = new short[]{5, 5, 10};
                            short[] id3_ = new short[]{7, 7, 4};
                            for (int i = 0; i < id_.length; i++) {
                                Item47 it = new Item47();
                                it.id = id_[i];
                                it.quantity = id2_[i];
                                conn.p.item.add_item_bag47(id3_[i], it);
                            }
                            break;
                        }
                        case 3:
                        case 4:
                        case 5:
                        case 6:
                        case 7:
                        case 8:
                        case 9: {
                            short[] id_ = new short[]{3, 18};
                            short[] id2_ = new short[]{5, 10};
                            short[] id3_ = new short[]{7, 4};
                            for (int i = 0; i < id_.length; i++) {
                                Item47 it = new Item47();
                                it.id = id_[i];
                                it.quantity = id2_[i];
                                conn.p.item.add_item_bag47(id3_[i], it);
                            }
                            break;
                        }
                    }
                    temp.received = true;
                } else {
                    Service.send_notice_box(conn, "Nama tidak ada dalam daftar");
                }
                break;
            }
            case 5: { // cua hang potion
                CastleSiegeManager.NhanQua(conn.p);
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    private static void Menu_Emma(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: { // cua hang potion
                Service.send_box_UI(conn, 0);
                break;
            }
            case 1: {
                if (conn.p.item.total_item_by_id(4, 52) > 0) {
                    MoLy.show_table_to_choose_item(conn.p);
                } else {
                    Service.send_notice_box(conn, "Tidak ada cukup tiket pembuka di inventaris");
                }
                break;
            }
            case 2: { // cua hang potion
                Service.sendBoxInputText(conn, 22, "% pajak", new String[]{"Masukkan % pajak 5 - 20%"});
                break;
            }
            case 3: {
                // Lihat catatan yang sama di Menu_Lisa case 3: baca dari BTF#bxh
                // (event baru), bukan Battlefield.gI() (sistem lama, tidak
                // pernah diisi lagi), dan cek MemberBattlefield#received supaya
                // hadiah tidak bisa diambil berkali-kali.
                BTF btf = GameEventManager.gI().getEvent(BTF.class);
                MemberBattlefield temp = btf != null ? btf.getBxhEntry(conn.p.name) : null;
                if (temp != null) {
                    if (temp.received) {
                        Service.send_notice_box(conn, "Hadiah sudah pernah diambil");
                        break;
                    }
                    switch (btf.getBxhRank(temp)) {
                        case 0: {
                            short[] id_ = new short[]{3, 2, 53, 54, 18};
                            short[] id2_ = new short[]{5, 5, 1, 1, 10};
                            short[] id3_ = new short[]{7, 7, 4, 4, 4};
                            for (int i = 0; i < id_.length; i++) {
                                Item47 it = new Item47();
                                it.id = id_[i];
                                it.quantity = id2_[i];
                                conn.p.item.add_item_bag47(id3_[i], it);
                            }
                            break;
                        }
                        case 1:
                        case 2: {
                            short[] id_ = new short[]{3, 2, 18};
                            short[] id2_ = new short[]{5, 5, 10};
                            short[] id3_ = new short[]{7, 7, 4};
                            for (int i = 0; i < id_.length; i++) {
                                Item47 it = new Item47();
                                it.id = id_[i];
                                it.quantity = id2_[i];
                                conn.p.item.add_item_bag47(id3_[i], it);
                            }
                            break;
                        }
                        case 3:
                        case 4:
                        case 5:
                        case 6:
                        case 7:
                        case 8:
                        case 9: {
                            short[] id_ = new short[]{3, 18};
                            short[] id2_ = new short[]{5, 10};
                            short[] id3_ = new short[]{7, 4};
                            for (int i = 0; i < id_.length; i++) {
                                Item47 it = new Item47();
                                it.id = id_[i];
                                it.quantity = id2_[i];
                                conn.p.item.add_item_bag47(id3_[i], it);
                            }
                            break;
                        }
                    }
                    temp.received = true;
                } else {
                    Service.send_notice_box(conn, "Nama tidak ada dalam daftar");
                }
                break;
            }
            case 5: { // cua hang potion
                CastleSiegeManager.NhanQua(conn.p);
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur ini belum tersedia");
                break;
            }
        }
    }

    private static void Menu_CayThong(Session conn, byte index) throws IOException {
        if (Manager.gI().event == 1) {
            switch (index) {
                case 0:
                case 1:
                case 2:
                case 3: {
                    int quant = conn.p.item.total_item_by_id(4, (113 + index));
                    if (quant > 0) {
                        //
                        short[] id_4 = new short[]{2, 5, 52, 142, 225, 271};
                        short[] id_7 = new short[]{0, 4, 23, 34, 39, 352, 357, 362, 367, 372, 377, 382, 387, 392, 397,
                                402,
                                407, 412,};
                        HashMap<Short, Short> list_4 = new HashMap<>();
                        HashMap<Short, Short> list_7 = new HashMap<>();
                        for (int i = 0; i < quant; i++) {
                            if (conn.p.item.get_bag_able() > 1) {
                                if (80 > Util.random(100)) {
                                    Item47 it = new Item47();
                                    it.category = 4;
                                    it.id = id_4[Util.random(id_4.length)];
                                    it.quantity = (short) Util.random(1, 3);
                                    if (!list_4.containsKey(it.id)) {
                                        list_4.put(it.id, it.quantity);
                                    } else {
                                        short quant_ = it.quantity;
                                        list_4.put(it.id, (short) (list_4.get(it.id) + quant_));
                                    }
                                    conn.p.item.add_item_bag47(4, it);
                                } else {
                                    Item47 it = new Item47();
                                    it.category = 7;
                                    it.id = id_7[Util.random(id_7.length)];
                                    it.quantity = (short) Util.random(1, 2);
                                    if (!list_7.containsKey(it.id)) {
                                        list_7.put(it.id, it.quantity);
                                    } else {
                                        short quant_ = it.quantity;
                                        list_7.put(it.id, (short) (list_7.get(it.id) + quant_));
                                    }
                                    conn.p.item.add_item_bag47(7, it);
                                }
                            }
                        }
                        //
                        Event_1.add_caythong(conn.p.name, quant);
                        conn.p.item.remove(4, (113 + index), quant);
                        conn.p.item.charInventory(4);
                        conn.p.item.charInventory(7);
                        String item_receiv = "\n";
                        for (Entry<Short, Short> en : list_4.entrySet()) {
                            item_receiv += ItemTemplate4.item.get(en.getKey()).getName() + " " + en.getValue() + "\n";
                        }
                        for (Entry<Short, Short> en : list_7.entrySet()) {
                            item_receiv += ItemTemplate7.item.get(en.getKey()).getName() + " " + en.getValue() + "\n";
                        }
                        Service.send_notice_box(conn,
                                "Berhasil dihias sebanyak " + quant + " kali dan mendapatkan:" + item_receiv);
                    } else {
                        Service.send_notice_box(conn, "Ruang inventory tidak cukup!");
                    }
                    break;
                }
                case 4: {
                    send_menu_select(conn, 120, Event_1.get_top_caythong());
                    break;
                }
                default: {
                    Service.send_notice_box(conn, "Sedang dalam perbaikan");
                    break;
                }
            }
        }
    }

    private static void Menu_ThaoKhamNgoc(Session conn, byte index) throws IOException {
        if (conn.p.list_thao_kham_ngoc.size() > 0) {
            if (conn.p.item.get_bag_able() < 3) {
                Service.send_notice_box(conn, "Ruang inventory tidak cukup!");
                return;
            }
            Item3 it = conn.p.list_thao_kham_ngoc.get(index);
            if (it != null) {
                for (int i = it.op.size() - 1; i >= 0; i--) {
                    byte id = it.op.get(i).id;
                    if (id == 58 || id == 59 || id == 60) {
                        if (it.op.get(i).getParam(0) != -1) {
                            Item47 it_add = new Item47();
                            it_add.id = (short) (it.op.get(i).getParam(0));
                            it_add.quantity = 1;
                            it_add.category = 7;
                            conn.p.item.add_item_bag47(7, it_add);
                        }
                        it.op.get(i).setParam(-1);
                    } else if (id >= 100 && id <= 107) {
                        it.op.remove(i);
                    }
                }
                // for (int i = 0; i < it.op.size(); i++) {
                // if (it.op.get(i).id == 58) {
                // if (it.op.get(i).getParam(0) != -1) {
                // Item47 it_add = new Item47();
                // it_add.id = (short) (it.op.get(i).getParam(0));
                // it_add.quantity = 1;
                // it_add.category = 7;
                // conn.p.item.add_item_bag47(7, it_add);
                // }
                // it.op.get(i).setParam(-1);
                // }
                // if (it.op.get(i).id == 59) {
                // if (it.op.get(i).getParam(0) != -1) {
                // Item47 it_add = new Item47();
                // it_add.id = (short) (it.op.get(i).getParam(0));
                // it_add.quantity = 1;
                // it_add.category = 7;
                // conn.p.item.add_item_bag47(7, it_add);
                // }
                // it.op.get(i).setParam(-1);
                // }
                // if (it.op.get(i).id == 60) {
                // if (it.op.get(i).getParam(0) != -1) {
                // Item47 it_add = new Item47();
                // it_add.id = (short) (it.op.get(i).getParam(0));
                // it_add.quantity = 1;
                // it_add.category = 7;
                // conn.p.item.add_item_bag47(7, it_add);
                // }
                // it.op.get(i).setParam(-1);
                // }
                // }
                conn.p.item.charInventory(4);
                conn.p.item.charInventory(7);
                conn.p.item.charInventory(3);
                Service.sendPlayerWear(conn.p);
                Service.send_notice_box(conn, "Berhasil dilepas");
            }
        }
    }

    private static void Menu_DoiDongMeDaySTG(Session conn, byte index) throws IOException {
        if (conn.p.item.wear != null && conn.p.item.wear.length > 12
                && CheckItem.isMedal(conn.p.item.wear[12].id)) {
            Service.send_box_input_yesno(conn, 94, "Aksi ini akan menghabiskan 1000 permata, apa Anda yakin?");
        } else {
            Service.send_notice_box(conn, "Tidak ada item yang sesuai!");
        }
    }

    private static void Menu_DoiDongMeDaySTPT(Session conn, byte index) throws IOException {
        if (conn.p.item.wear != null && conn.p.item.wear.length > 12
                && CheckItem.isMedal(conn.p.item.wear[12].id)) {
            Service.send_box_input_yesno(conn, 98, "Aksi ini akan menghabiskan 1000 permata, apa Anda yakin?");
        } else {
            Service.send_notice_box(conn, "Tidak ada item yang sesuai!");
        }
    }

    private static void Menu_Quest_Daily(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                String notice = "=== MISI HARIAN ===\n"
                        + "Tersedia 4 tipe misi acak sesuai level:\n"
                        + "  • Bunuh Mob     — kalahkan monster target\n"
                        + "  • Kumpulkan Item — kumpulkan item drop dari mob\n"
                        + "  • Party Grind   — grinding bersama party (min. 2 anggota)\n"
                        + "  • Berburu Boss  — kalahkan boss (tersedia di difficulty Sulit+)\n\n"
                        + "Hadiah per difficulty:\n"
                        + "  Sangat Mudah : Emas + Permata + EXP\n"
                        + "  Biasa        : + Material Medali Biru\n"
                        + "  Sulit        : + Material Medali Kuning\n"
                        + "  Sangat Sulit : + Material Medali Ungu\n\n"
                        + "Bonus permata untuk Party Grind & Boss Hunt!\n"
                        + "Maksimal 5 misi per hari.";
                Service.send_notice_box(conn, notice);
                break;
            }
            case 1: {
                if (conn.p.quest_daily[0] != -1) {
                    Service.send_notice_box(conn, "Anda sudah menerima misi!");
                } else {
                    if (conn.p.quest_daily[4] > 0) {
                        send_menu_select(conn, 999, new String[]{"Sangat Mudah", "Biasa", "Sulit", "Sangat Sulit"});
                    } else {
                        Service.send_notice_box(conn, "Kesempatan hari ini sudah habis, kembali besok");
                    }
                }

            }
            break;
            case 2: {
                DailyQuest.remove_quest(conn.p);
                break;
            }
            case 3: {
                DailyQuest.finish_quest(conn.p);
                break;
            }
            case 4: {
                Service.send_notice_box(conn, DailyQuest.info_quest(conn.p));
                break;
            }
        }
    }

    // private static void Menu_Quest(Session conn, byte index) throws IOException {
    // switch (index) {
    //
    // case 0: {
    // send_menu_select(conn, 1000, new String[] {"Hướng dẫn", "Nhận nhiệm vụ", "Hủy
    // nhiệm vụ", "Trả
    // nhiệm vụ", "Kiểm tra"});
    // break;
    // }
    // }
    // }
    private static void Menu_Wedding(Session conn, byte index) throws IOException {
        switch (index) {
            case 0: {
                if (conn.p.item.wear[23] == null) {
                    Service.sendBoxInputText(conn, 66, "Masukkan informasi",
                            new String[]{"Pilih cincin (1-4) : ", "Nama Pasangan : "});
                } else {
                    Service.send_notice_box(conn,
                            "Kenapa kamu memakai cincin pernikahan, tapi malah meminta untuk menikahi orang lain?");
                }
                break;
            }
            case 1: {
                if (conn.p.item.wear[23] != null) {
                    Wedding temp = Wedding.get_obj(conn.p.name);
                    if (temp != null) {
                        String name_target = "";
                        if (temp.name_1.equals(conn.p.name)) {
                            name_target = temp.name_2;
                        } else {
                            name_target = temp.name_1;
                        }
                        Service.send_box_input_yesno(conn, 111,
                                "Memutuskan untuk membatalkan pertunangan dengan " + name_target);
                    }
                } else {
                    Service.send_notice_box(conn, "Siapa yang kamu nikahi, kamu orang gila?");
                }
                break;
            }
            case 2: {
                Item3 it = conn.p.item.wear[23];
                if (it != null) {
                    float perc = (((float) Wedding.get_obj(conn.p.name).exp) / Level.entrys.get(it.tier).exp) * 100f;
                    String notice = "Exp Saat ini : %s, Peningkatan diperlukan %s juta emas dan %sk Permata";
                    String a = String.format("%.2f", perc) + "%";
                    Service.send_box_input_yesno(conn, 112,
                            String.format(notice, a, (3 * (it.tier + 1)), (3 * (it.tier + 1))));
                } else {
                    Service.send_notice_box(conn, "Kalau begitu, siapa yang kamu nikahi? Kamu bercanda, kan?");
                }
                break;
            }
            case 3: {
                String notice = "Cincin Pernikahan\r\n"
                        + "- 3 miliar emas \"cincin pernikahan 1\"\r\n"
                        + "- 300 ribu permata \"cincin pernikahan 2\"\r\n"
                        + "- 600 ribu permata \"cincin pernikahan 3\"\r\n"
                        + "- 900 ribu permata \"cincin pernikahan 4\"\r\n"
                        + "Peningkatan Cincin:\r\n"
                        + "Ketika sudah menikah, pasangan (istri dan suami) dalam satu grup harus melawan monster atau bos agar cincin pernikahan mendapatkan EXP.\r\n"
                        + "Pergilah ke NPC Anna untuk meningkatkan cincin. Setiap peningkatan membutuhkan 1 ribu x1 permata dan 2 juta emas x2.\r\n"
                        + "Level maksimum cincin adalah 30. \r\n"
                        + "Catatan: Setiap kali ditingkatkan, jumlah emas dan permata akan berlipat ganda \r\n"
                        + "Contoh: Level 1 biayanya 1k Gems dan 2 juta Gold. Untuk naik ke level 2, biayanya akan menjadi 2 ribu Gems dan 4 juta Gold.";
                Service.send_notice_box(conn, notice);
                break;
            }
            case 4: { // Gudang Pasangan - buka
                event_daily.Wedding wed = event_daily.Wedding.get_obj(conn.p.name);
                if (wed == null || conn.p.it_wedding == null) {
                    Service.send_notice_box(conn, "Kamu belum menikah!");
                    break;
                }
                conn.p.wedding_chest_open = true;
                wed.openChestFor(conn.p); // kirim Message(23) + Message(65)
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia.");
                break;
            }
        }
    }
}