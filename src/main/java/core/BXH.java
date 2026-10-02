package core;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Comparator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import model.player.PlayerData;
import model.player.PlayerWear;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import client.Player;
import template.Item3;
import game.guild.Guild;
import client.io.Message;
import client.io.Session;
import game.map.GameMap;
import game.map.MapService;
import template.Level;
import template.PlayerPart;
import utils.SQLHelper;

@Slf4j
public class BXH {

    public static final List<Memin4> BXH_level = new ArrayList<>();
    public static final List<Guild> BXH___GUILD = new ArrayList<>();
    public static final List<PlayerData> topLevels = new ArrayList<>();
    public static final List<PlayerData> topRampok = new ArrayList<>();
    public static final List<PlayerData> topDagang = new ArrayList<>();

    // ===== Item buff khusus Top 1-3 Level (item BEDA per posisi) =====
    // index 0 = item buff utk Top 1, index 1 = Top 2, index 2 = Top 3.
    // TODO: ganti -1 dengan id template item (tabel `equipment`) masing-masing posisi.
    public static final short[] TOP_LEVEL_BUFF_ITEM_ID = { 5500, 5501, 5502 }; // <-- WAJIB diisi sebelum dipakai
    // playerId -> rank (0/1/2) sesuai state terakhir yang diketahui server
    private static Map<Integer, Integer> currentTopRank = new HashMap<>();

    public static List<Memin4> entry0 = new ArrayList<>();

    public static void loadTopLevel() {
        BXH.topLevels.clear();
        var players = SQLHelper
                .select("p.id", "p.level", "p.exp", "p.name", "p.body", "p.itemwear")
                .from("player p")
                .join("account a", "p.uid = a.id")
                .where("a.ac_admin", 0)
                .and("p.level", ">", 10)
                .orderByRaw("p.level DESC, p.exp DESC")
                .limit(99)
                .getAsModel(PlayerData.class);

        topLevels.addAll(players);
        updateTopLevelBuffItem();
    }

    /**
     * Bandingkan Top 1-3 Level yang baru vs state sebelumnya (per posisi, bukan cuma "member top3").
     * - Player yang KELUAR dari top 3 ("keselip") -> item posisi lamanya dicabut otomatis.
     * - Player yang TUKAR POSISI (mis. Top1 -> Top2) -> item lama dicabut, item posisi baru diberikan.
     * - Player yang BARU MASUK top 3 -> item sesuai posisinya diberikan.
     * Dipanggil otomatis tiap loadTopLevel() (mengikuti jadwal SaveData.process(), ~1 menit).
     */
    private static void updateTopLevelBuffItem() {
        if (TOP_LEVEL_BUFF_ITEM_ID[0] < 0 && TOP_LEVEL_BUFF_ITEM_ID[1] < 0 && TOP_LEVEL_BUFF_ITEM_ID[2] < 0) {
            return; // belum dikonfigurasi, jangan lakukan apa-apa
        }

        Map<Integer, Integer> newRank = new HashMap<>();
        for (int i = 0; i < Math.min(3, topLevels.size()); i++) {
            newRank.put(topLevels.get(i).getId(), i);
        }

        // player lama: keluar top3 sepenuhnya, atau tukar posisi
        for (Map.Entry<Integer, Integer> e : currentTopRank.entrySet()) {
            int playerId = e.getKey();
            int oldRank = e.getValue();
            Integer newR = newRank.get(playerId);
            if (newR == null) {
                revokeTopLevelBuff(playerId, oldRank);
            } else if (!newR.equals(oldRank)) {
                revokeTopLevelBuff(playerId, oldRank);
                grantTopLevelBuff(playerId, newR);
            }
        }

        // player baru masuk top3
        for (Map.Entry<Integer, Integer> e : newRank.entrySet()) {
            if (!currentTopRank.containsKey(e.getKey())) {
                grantTopLevelBuff(e.getKey(), e.getValue());
            }
        }

        currentTopRank = newRank;
    }

    /**
     * Rank Top Level player saat ini (0=Top1, 1=Top2, 2=Top3), atau null kalau tidak di top 3.
     * Dipakai Player.getDisplayName() untuk menambahkan prefix "#1 - ", dst.
     */
    public static Integer getTopLevelRank(int playerId) {
        return currentTopRank.get(playerId);
    }

    private static void grantTopLevelBuff(int playerId, int rank) {
        short itemId = TOP_LEVEL_BUFF_ITEM_ID[rank];
        if (itemId < 0) {
            return; // posisi ini belum dikonfigurasi id item-nya
        }
        Player p = Manager.getPlayerById(playerId);
        if (p == null) {
            return; // player offline, akan disinkronkan otomatis saat dia login (lihat syncTopLevelBuffOnLogin)
        }
        try {
            for (Item3 it : p.item.bag3) {
                if (it != null && it.id == itemId) {
                    return; // sudah ada, jangan dobel
                }
            }
            p.item.add_item_bag3_default(itemId, 0, true); // isLock=true: tidak bisa dijual/ditrade
            p.item.charInventory(3);
            Service.send_notice_box(p.conn, "Selamat! Kamu sekarang Top " + (rank + 1) + " Level dan mendapat item buff spesial!");
        } catch (IOException ex) {
            log.error("grantTopLevelBuff error, playerId=" + playerId + " rank=" + rank, ex);
        }
    }

    private static void revokeTopLevelBuff(int playerId, int rank) {
        short itemId = TOP_LEVEL_BUFF_ITEM_ID[rank];
        if (itemId < 0) {
            return;
        }
        Player p = Manager.getPlayerById(playerId);
        if (p == null) {
            return; // player offline, akan disinkronkan otomatis saat dia login
        }
        try {
            for (int j = 0; j < p.item.bag3.length; j++) {
                if (p.item.bag3[j] != null && p.item.bag3[j].id == itemId) {
                    p.item.remove(3, j, 1);
                }
            }
            p.item.charInventory(3);
            // FIX: item buff ini juga bisa di-"pake"/equip ke slot item.wear[] (bukan cuma
            // duduk di tas bag3). Kalau lagi dipake pas rank-nya keselip, kode di atas nggak
            // pernah nemu itemnya (karena udah pindah dari bag3 ke wear[]) jadi item tetap
            // nempel selamanya di karakter. Cek & copot juga dari wear[].
            if (removeTopLevelBuffFromWear(p, itemId)) {
                p.checkFullSetTT();
                Service.sendPlayerWear(p);
                Service.sendMainCharInfo(p);
                // FIX: sendPlayerWear/sendMainCharInfo cuma update layar SI PEMAIN sendiri
                // (addmsg ke p.conn doang). Pemain LAIN yang lagi berdiri di dekatnya dan
                // sudah kadung nampilin item/aura buff ini di karakternya TIDAK ikut ke-refresh
                // - sistem proximity (MapService, other_player_inside) cuma ngirim full info
                // sekali pas SEORANG PLAYER BARU masuk radius, bukan tiap ada perubahan wear.
                // Tanpa broadcastMainCharInfo, item yg udah kepasang keliatan nempel terus di
                // mata pemain lain sampai salah satu pindah map/jauh lalu balik lagi radius.
                // Semua unequip lain di codebase ini (MenuManager, UseItem, Guild, dst) selalu
                // ikut panggil ini - disamakan di sini.
                MapService.broadcastMainCharInfo(p.map, p);
            }
            Service.send_notice_box(p.conn, "Posisi Top " + (rank + 1) + " Level kamu berubah, item buff spesial otomatis dicabut.");
        } catch (IOException ex) {
            log.error("revokeTopLevelBuff error, playerId=" + playerId + " rank=" + rank, ex);
        }
    }

    /**
     * Cari & copot item buff Top Level dari slot equip (item.wear[]) player, kalau ada.
     * Item cukup dihapus (di-null-kan), TIDAK dikembalikan ke tas - karena begitu keselip
     * dari top3 item ini memang harus hilang total, sama seperti behaviour di bag3.
     * Return true kalau ada perubahan (supaya caller tahu perlu sync ulang ke client).
     */
    private static boolean removeTopLevelBuffFromWear(Player p, short itemId) {
        boolean removed = false;
        for (int i = 0; i < p.item.wear.length; i++) {
            if (p.item.wear[i] != null && p.item.wear[i].id == itemId) {
                p.item.wear[i] = null;
                removed = true;
            }
        }
        return removed;
    }

    /**
     * Dipanggil dari Player.setOnline() supaya player yang statusnya berubah
     * (masuk/keluar/tukar posisi top 3) SAAT DIA OFFLINE tetap tersinkron begitu login lagi.
     */
    public static void syncTopLevelBuffOnLogin(Player p) {
        if (p == null) {
            return;
        }
        Integer expectedRank = currentTopRank.get(p.objectId); // null jika tidak di top3

        try {
            boolean changed = false;
            boolean wearChanged = false;
            // cabut item posisi manapun yang TIDAK sesuai posisi seharusnya sekarang
            for (int rank = 0; rank < TOP_LEVEL_BUFF_ITEM_ID.length; rank++) {
                short itemId = TOP_LEVEL_BUFF_ITEM_ID[rank];
                if (itemId < 0) continue;
                boolean shouldHaveThis = expectedRank != null && expectedRank == rank;
                if (shouldHaveThis) continue;
                for (int j = 0; j < p.item.bag3.length; j++) {
                    if (p.item.bag3[j] != null && p.item.bag3[j].id == itemId) {
                        p.item.remove(3, j, 1);
                        changed = true;
                    }
                }
                // FIX: sama seperti revokeTopLevelBuff() - item ini bisa lagi dipake/equip di
                // item.wear[], bukan cuma di tas. Kalau player login sambil masih pake item ini
                // padahal rank-nya udah keselip pas offline, harus dicopot juga dari wear[].
                if (removeTopLevelBuffFromWear(p, itemId)) {
                    wearChanged = true;
                }
            }
            if (wearChanged) {
                p.checkFullSetTT();
                Service.sendPlayerWear(p);
                Service.sendMainCharInfo(p);
                // FIX: sama seperti di revokeTopLevelBuff() - jaga-jaga kalau p.map sudah
                // terisi & ada pemain lain di radius saat method ini jalan (mis. reconnect
                // tanpa benar-benar keluar dari map), biar wear yg dicopot ikut ke-refresh
                // buat pemain sekitar juga, bukan cuma buat diri sendiri.
                if (p.map != null) {
                    MapService.broadcastMainCharInfo(p.map, p);
                }
            }
            // kasih item sesuai posisi seharusnya, kalau belum ada
            if (expectedRank != null) {
                short itemId = TOP_LEVEL_BUFF_ITEM_ID[expectedRank];
                if (itemId >= 0) {
                    boolean hasItem = false;
                    for (Item3 it : p.item.bag3) {
                        if (it != null && it.id == itemId) {
                            hasItem = true;
                            break;
                        }
                    }
                    if (!hasItem) {
                        p.item.add_item_bag3_default(itemId, 0, true);
                        changed = true;
                    }
                }
            }
            if (changed) {
                p.item.charInventory(3);
            }
        } catch (IOException e) {
            log.error("syncTopLevelBuffOnLogin error, playerId=" + p.objectId, e);
        }
    }

    /**
     * Load Top 20 Perampok (player dengan poin "dicuop" terbanyak dari sistem
     * area dagang / zona 6 - hasil merampok pet_di_buon milik player lain).
     */
    public static void loadTopRampok() {
        BXH.topRampok.clear();
        var players = SQLHelper
                .select("p.id", "p.level", "p.exp", "p.name", "p.body", "p.itemwear", "p.dicuop")
                .from("player p")
                .join("account a", "p.uid = a.id")
                .where("a.ac_admin", 0)
                .and("p.dicuop", ">", 0)
                .orderByRaw("p.dicuop DESC")
                .limit(20)
                .getAsModel(PlayerData.class);

        topRampok.addAll(players);
    }

    public static void loadTopDagang() {
        BXH.topDagang.clear();
        var players = SQLHelper
                .select("p.id", "p.level", "p.exp", "p.name", "p.body", "p.itemwear", "p.gold_dagang AS goldDagang")
                .from("player p")
                .join("account a", "p.uid = a.id")
                .where("a.ac_admin", 0)
                .and("p.gold_dagang", ">", 0)
                .orderByRaw("p.gold_dagang DESC")
                .limit(20)
                .getAsModel(PlayerData.class);

        topDagang.addAll(players);
    }

    public static void send(Session conn, int b) {
        switch (b) {
            case 0 -> sendTopLevel(conn);
            case 1 -> sendTopGuild(conn);
            case 2 -> sendTopCouple(conn);
            case 3 -> sendTopBossKill(conn);
            case 4 -> sendTopRampok(conn);
            case 5 -> sendTopDagang(conn);
            case 6 -> sendTopGuildByGold(conn);
            case 7 -> sendTopGuildByGems(conn);
        }
    }

    /**
     * Kirim papan ranking Top Pasangan ke client.
     * Diurutkan berdasarkan skor keaktifan: exp bersama + bonus tier cincin + bonus online.
     * Format paket: Message(56) type=2, mirip sendTopLevel.
     */
    public static void sendTopCouple(Session s) {
        try {
            List<event_daily.Wedding> wedList = event_daily.Wedding.list;

            if (wedList.isEmpty()) {
                Service.send_notice_box(s, "Belum ada pasangan yang terdaftar!");
                return;
            }

            // Sort by score tertinggi
            List<event_daily.Wedding> sorted = new ArrayList<>(wedList);
            sorted.sort((a, b) -> Long.compare(b.getActivityScore(), a.getActivityScore()));

            int size = Math.min(sorted.size(), 20);

            Message m = new Message(56);
            m.writer().writeByte(1 ); // type
            m.writer().writeUTF("Top Pasangan");
            m.writer().writeByte(99);
            m.writer().writeInt(0);

            // ✅ 1 pasangan = 1 entry
            m.writer().writeByte(size);

            for (int i = 0; i < size; i++) {
                event_daily.Wedding wed = sorted.get(i);

                client.Player p1 = game.map.GameMap.get_player_by_name(wed.name_1);
                client.Player p2 = game.map.GameMap.get_player_by_name(wed.name_2);

                // =========================
                // PILIH PLAYER UNTUK DISPLAY
                // =========================
                client.Player displayPlayer = null;

                if (p1 != null) {
                    displayPlayer = p1;
                } else if (p2 != null) {
                    displayPlayer = p2;
                }

                // =========================
                // AMBIL VISUAL
                // =========================
                byte head = 0, eye = 0, hair = 0;
                List<template.PlayerPart> parts = new ArrayList<>();

                if (displayPlayer != null) {
                    head = displayPlayer.head;
                    eye = displayPlayer.eye;
                    hair = displayPlayer.hair;

                    if (displayPlayer.item != null && displayPlayer.item.wear != null) {
                        for (int wi = 0; wi < displayPlayer.item.wear.length; wi++) {
                            template.Item3 it = displayPlayer.item.wear[wi];
                            if (it != null && it.type >= 0 && it.type < 12 && (wi == 0 || wi == 1 || wi == 6 || wi == 7 || wi == 10)) { // FIX: type>=12 bikin ArrayIndexOutOfBoundsException di client (byte[12] byArray diindeks pakai type) - lihat catatan lengkap di renderTopLevelEntry
                                template.PlayerPart part = new template.PlayerPart();
                                part.type = it.type;
                                part.part = it.part;
                                parts.add(part);
                            }
                        }
                    }
                }

                // =========================
                // STATUS ONLINE
                // =========================
                boolean bothOnline = (p1 != null && p2 != null);
                boolean oneOnline = (p1 != null || p2 != null);

                String status = bothOnline ? "Online Berdua"
                        : (p1 != null ? wed.name_1 + " Online"
                        : (p2 != null ? wed.name_2 + " Online" : "Offline"));

                // =========================
                // INFO
                // =========================
                String ringLabel = "Cincin " + (wed.it.color + 1);
                String ringInfo = ringLabel + " +Lv " + wed.it.tier;

                String coupleName = wed.name_1 + " & " + wed.name_2;

                String info = ringInfo
                        //+ " | EXP: " + wed.exp
                        //+ " | Score: " + wed.getActivityScore()
                        + " | " + status;

                // =========================
                // WRITE DATA
                // =========================
                m.writer().writeUTF(coupleName);

                m.writer().writeByte(head);
                m.writer().writeByte(eye);
                m.writer().writeByte(hair);

                m.writer().writeShort(0);

                m.writer().writeByte(parts.size());
                for (template.PlayerPart pt : parts) {
                    m.writer().writeByte(pt.part);
                    m.writer().writeByte(pt.type);
                }

                m.writer().writeByte(oneOnline ? 1 : 0);

                m.writer().writeUTF(info);

                m.writer().writeShort(-1);
            }

            s.addmsg(m);
            m.cleanup();

        } catch (Exception e) {
            log.error("sendTopCouple error", e);
        }
    }

    // ---------------------------------------------------------------
    // Top Kill Boss Premium
    // ---------------------------------------------------------------

    /** Inner class khusus Top Kill Boss — tidak ganggu PlayerData */
    public static class BossKillEntry {
        public String name;
        public byte   head, eye, hair;
        public int    point;
        public List<PlayerPart> itemwear = new ArrayList<>();
    }

    public static final List<BossKillEntry> topBossKill = new CopyOnWriteArrayList<>();

    public static void loadTopBossKill() {
        topBossKill.clear();
        try (Connection conn = SQL.gI().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT p.name, p.body, p.itemwear, b.point " +
                "FROM `boss_kill_point` b " +
                "JOIN `player` p ON p.name = b.name " +
                "ORDER BY b.point DESC LIMIT 20")) {

            while (rs.next()) {
                BossKillEntry e = new BossKillEntry();
                e.name  = rs.getString("name");
                e.point = rs.getInt("point");

                // parse body
                JSONArray bodyArr = (JSONArray) JSONValue.parse(rs.getString("body"));
                if (bodyArr != null && bodyArr.size() >= 3) {
                    e.head = Byte.parseByte(bodyArr.get(0).toString());
                    e.eye  = Byte.parseByte(bodyArr.get(1).toString());
                    e.hair = Byte.parseByte(bodyArr.get(2).toString());
                }

                // parse itemwear — sama persis pola Memin4
                JSONArray wears = (JSONArray) JSONValue.parse(rs.getString("itemwear"));
                if (wears != null) {
                    for (int i = 0; i < wears.size(); i++) {
                        JSONArray w = (JSONArray) JSONValue.parse(wears.get(i).toString());
                        if (w == null) continue;
                        byte idx = Byte.parseByte(w.get(9).toString());
                        if (idx != 0 && idx != 1 && idx != 6 && idx != 7 && idx != 10) continue;
                        byte typeVal = Byte.parseByte(w.get(2).toString());
                        if (typeVal < 0 || typeVal >= 12) continue; // FIX: type>=12 bikin ArrayIndexOutOfBoundsException di client - lihat catatan lengkap di renderTopLevelEntry
                        PlayerPart pp = new PlayerPart();
                        pp.type = typeVal;
                        pp.part = Byte.parseByte(w.get(6).toString());
                        e.itemwear.add(pp);
                    }
                }
                topBossKill.add(e);
            }
        } catch (Exception ex) {
            log.error("[BXH] loadTopBossKill error: {}", ex.getMessage());
        }
    }

    private static void sendTopBossKill(Session s) {
        try {
            if (topBossKill.isEmpty()) {
                Service.send_notice_box(s, "Belum ada data kill boss.");
                return;
            }

            int myIndex = 0;
            for (int i = 0; i < topBossKill.size(); i++) {
                if (topBossKill.get(i).name.equals(s.p.name)) {
                    myIndex = i + 1;
                    break;
                }
            }

            Message m = new Message(56);
            m.writer().writeByte(1);
            m.writer().writeUTF("Top Kill Boss");
            m.writer().writeByte(99);
            m.writer().writeInt(myIndex);
            m.writer().writeByte(topBossKill.size());

            for (BossKillEntry e : topBossKill) {
                Guild guild = Guild.getPlayerGuild(e.name);
                String info = "Kill Boss : " + e.point;

                m.writer().writeUTF(e.name);
                m.writer().writeByte(e.head);
                m.writer().writeByte(e.eye);
                m.writer().writeByte(e.hair);
                m.writer().writeShort(e.point); // sama dengan writeShort di sendTopLevel

                m.writer().writeByte(e.itemwear.size());
                for (PlayerPart it : e.itemwear) {
                    m.writer().writeByte(it.getPart());
                    m.writer().writeByte(it.getType());
                }

                boolean isOnline = Manager.getPlayerByName(e.name) != null
                        || isBotOnline(e.name);
                m.writer().writeByte(isOnline ? (byte) 1 : (byte) 0);
                m.writer().writeUTF(info);

                if (guild != null) {
                    m.writer().writeShort(guild.icon);
                    m.writer().writeUTF(guild.shortName);
                    m.writer().writeByte(guild.get_mem_type(e.name));
                } else {
                    m.writer().writeShort(-1);
                }
            }
            s.addmsg(m);
            m.cleanup();
        } catch (Exception ex) {
            log.error("[BXH] sendTopBossKill error: {}", ex.getMessage());
        }
    }

    /**
     * Kirim papan ranking Top Perampok ke client (hasil rampok pet_di_buon
     * di area dagang / zona 6, lewat NPC Mr Frank, Graham, Mr Dylan).
     */
    private static void sendTopRampok(Session s) {
        try {
            if (BXH.topRampok.isEmpty()) {
                Service.send_notice_box(s, "Data belum tersedia");
                return;
            }

            // FIX: filter dulu SEBELUM tulis jumlah entry ke packet, supaya jumlah yang dijanjikan
            // ke client selalu sama dengan jumlah entry yang benar-benar ditulis di bawah (kalau
            // ada entry yang di-skip di tengah loop tapi jumlah di header masih jumlah asli,
            // client akan salah parsing packet ini / packet-packet setelahnya).
            List<PlayerData> validTopRampok = BXH.topRampok.stream()
                    .filter(temp -> temp != null && temp.getBody() != null && temp.getBody().length >= 3)
                    .toList();

            Message m = new Message(56);
            m.writer().writeByte(1);
            m.writer().writeUTF("Top Perampok");
            m.writer().writeByte(99); // page
            m.writer().writeInt(0); // my index in bxh
            m.writer().writeByte(validTopRampok.size());
            for (PlayerData temp : validTopRampok) {

                List<PlayerWear> itemwear = temp.getItemwear();
                List<PlayerPart> parts = (itemwear == null ? List.<PlayerWear>of() : itemwear)
                        .stream()
                        .filter(wear -> wear.getIndex() == 0 || wear.getIndex() == 1 || wear.getIndex() == 6 || wear.getIndex() == 7 || wear.getIndex() == 10 || wear.getIndex() == 22) // FIX: sertakan slot 22 (amulet/cincin)
                        .filter(wear -> wear.getType() >= 0 && wear.getType() < 12) // FIX: client (eq.Y) simpen icon ke "byte[12]" yg diindeks LANGSUNG pakai nilai type; type>=12 (mis. amulet=12) bikin ArrayIndexOutOfBoundsException di client & seluruh window ranking gagal kebuka. Item type>=12 di-skip dari icon list (data lain tetap tampil normal).
                        .map(wear -> new PlayerPart(wear.getType(), wear.getPart()))
                        .toList();
                Guild guild = Guild.getPlayerGuild(temp.getName());

                long g = temp.getGoldRampok();
                String goldStr;
                if (g >= 1_000_000_000L) {
                    goldStr = String.format("%.1fb", g / 1_000_000_000.0);
                } else if (g >= 1_000_000L) {
                    goldStr = String.format("%.1fm", g / 1_000_000.0);
                } else if (g >= 1_000L) {
                    goldStr = String.format("%.0fk", g / 1_000.0);
                } else {
                    goldStr = g + "";
                }

                String info = "Level : " + temp.getLevel() + "\t-\tDagang : " + goldStr;

                m.writer().writeUTF(temp.getName());
                m.writer().writeByte(temp.getBody()[0]);
                m.writer().writeByte(temp.getBody()[1]);
                m.writer().writeByte(temp.getBody()[2]);
                m.writer().writeShort(temp.getLevel());

                m.writer().writeByte(parts.size());
                for (PlayerPart it : parts) {
                    m.writer().writeByte(it.getPart());
                    m.writer().writeByte(it.getType());
                }
                boolean isOnline = Manager.getPlayerByName(temp.getName()) != null
                        || isBotOnline(temp.getName());
                m.writer().writeByte(isOnline ? (byte) 1 : (byte) 0);
                m.writer().writeUTF(info);
                if (guild != null) {
                    m.writer().writeShort(guild.icon);
                    m.writer().writeUTF(guild.shortName);
                    m.writer().writeByte(guild.get_mem_type(temp.getName()));
                } else {
                    m.writer().writeShort(-1);
                }
            }
            s.addmsg(m);
            m.cleanup();
        } catch (Exception ex) {
            log.error("[BXH] sendTopRampok error: {}", ex.getMessage());
        }
    }

    private static void sendTopDagang(Session s) {
        try {
            if (BXH.topDagang.isEmpty()) {
                Service.send_notice_box(s, "Data belum tersedia");
                return;
            }

            // FIX: filter dulu sebelum tulis jumlah entry ke packet (lihat komentar di sendTopRampok)
            List<PlayerData> validTopDagang = BXH.topDagang.stream()
                    .filter(temp -> temp != null && temp.getBody() != null && temp.getBody().length >= 3)
                    .toList();

            Message m = new Message(56);
            m.writer().writeByte(1);
            m.writer().writeUTF("Top Pedagang");
            m.writer().writeByte(99); // page
            m.writer().writeInt(0);   // my index
            m.writer().writeByte(validTopDagang.size());
            for (PlayerData temp : validTopDagang) {

                List<PlayerWear> itemwear = temp.getItemwear();
                List<PlayerPart> parts = (itemwear == null ? List.<PlayerWear>of() : itemwear)
                        .stream()
                        .filter(wear -> wear.getIndex() == 0 || wear.getIndex() == 1 || wear.getIndex() == 6 || wear.getIndex() == 7 || wear.getIndex() == 10 || wear.getIndex() == 22) // FIX: sertakan slot 22 (amulet/cincin)
                        .filter(wear -> wear.getType() >= 0 && wear.getType() < 12) // FIX: cegah ArrayIndexOutOfBoundsException di client (lihat sendTopRampok)
                        .map(wear -> new PlayerPart(wear.getType(), wear.getPart()))
                        .toList();

                Guild guild = Guild.getPlayerGuild(temp.getName());

                // Format gold singkat: 2.500.000 → "2,5m", 500.000 → "500k"
                long g = temp.getGoldDagang();
                String goldStr;
                if (g >= 1_000_000_000L) {
                    goldStr = String.format("%.1fb", g / 1_000_000_000.0);
                } else if (g >= 1_000_000L) {
                    goldStr = String.format("%.1fm", g / 1_000_000.0);
                } else if (g >= 1_000L) {
                    goldStr = String.format("%.0fk", g / 1_000.0);
                } else {
                    goldStr = g + "";
                }

                String info = "Level : " + temp.getLevel() + "\t-\tRampok : " + goldStr;

                m.writer().writeUTF(temp.getName());
                m.writer().writeByte(temp.getBody()[0]);
                m.writer().writeByte(temp.getBody()[1]);
                m.writer().writeByte(temp.getBody()[2]);
                m.writer().writeShort(temp.getLevel());

                m.writer().writeByte(parts.size());
                for (PlayerPart it : parts) {
                    m.writer().writeByte(it.getPart());
                    m.writer().writeByte(it.getType());
                }
                boolean isOnline = Manager.getPlayerByName(temp.getName()) != null
                        || isBotOnline(temp.getName());
                m.writer().writeByte(isOnline ? (byte) 1 : (byte) 0);
                m.writer().writeUTF(info);
                if (guild != null) {
                    m.writer().writeShort(guild.icon);
                    m.writer().writeUTF(guild.shortName);
                    m.writer().writeByte(guild.get_mem_type(temp.getName()));
                } else {
                    m.writer().writeShort(-1);
                }
            }
            s.addmsg(m);
            m.cleanup();
        } catch (Exception ex) {
            log.error("[BXH] sendTopDagang error: {}", ex.getMessage());
        }
    }

    private static void sendTopLevel(Session s) {
        try {
            if (BXH.topLevels.isEmpty()) {
                Service.send_notice_box(s, "Data belum tersedia");
                return;
            }

            // FIX: filter dulu sebelum tulis jumlah entry ke packet (lihat komentar di sendTopRampok).
            // Baris hasil query yang gagal di-map (misal itemwear/body korup) bisa jadi null atau
            // punya body pendek — kalau lolos, dulu bikin NPE di tengah loop yang ditelan diam-diam
            // oleh "catch (Exception ignore)" di bawah, jadi packet-nya GAK PERNAH terkirim ke client
            // sama sekali dan menu "Top Level" kelihatan seperti tidak merespon.
            List<PlayerData> validTopLevels = BXH.topLevels.stream()
                    .filter(temp -> temp != null && temp.getBody() != null && temp.getBody().length >= 3)
                    .toList();

            // FIX 2: render tiap entry ke buffer sementara dulu, dibungkus try-catch PER PEMAIN.
            // Sebelumnya satu try-catch besar membungkus SELURUH loop — kalau ada 1 saja entry yang
            // bikin exception saat dirender (mis. Level.entrys.get(level-1) index out of bounds untuk
            // pemain yang levelnya di luar jangkauan tabel `level`, guild data korup, dll), seluruh
            // packet gagal ditulis dan menu "Top Level" jadi error/ga bisa dibuka untuk SEMUA orang,
            // bukan cuma pemain yang datanya bermasalah. Sekarang entry yang gagal di-skip + di-log,
            // sisanya tetap tampil normal.
            Message m = new Message(56);
            m.writer().writeByte(1);
            m.writer().writeUTF("Top Level");
            m.writer().writeByte(99); // page
            m.writer().writeInt(0); // my index in bxh

            List<byte[]> renderedEntries = new ArrayList<>();
            for (PlayerData temp : validTopLevels) {
                try {
                    renderedEntries.add(renderTopLevelEntry(temp));
                } catch (Exception entryEx) {
                    log.error("[BXH] sendTopLevel: skip entry rusak, player={} level={} : {}",
                            temp.getName(), temp.getLevel(), entryEx.getMessage(), entryEx);
                }
            }

            m.writer().writeByte(renderedEntries.size()); // num2 — sesuai jumlah entry yang BERHASIL dirender
            for (byte[] entry : renderedEntries) {
                m.writer().write(entry);
            }
            s.addmsg(m);
            m.cleanup();
            // DIAGNOSTIC SEMENTARA: pasangan dari log di MenuManager. Kalau log ini MUNCUL tapi
            // client tetap ga nampilin apa-apa, berarti server sukses total (klik diterima, packet
            // dikirim) dan masalahnya murni CLIENT gagal render/parse packet-nya. Hapus setelah kelar.
            log.warn("[DEBUG-TOPLEVEL] Packet Top Level terkirim, {} entry", renderedEntries.size());
        } catch (Exception ex) {
            // FIX: sebelumnya exception di sini ditelan diam-diam (catch...ignore), jadi kalau ada
            // 1 saja player di topLevels yang datanya bikin error, packet gak pernah terkirim ke
            // client dan menu "Top Level" kelihatan seperti tidak merespon/tidak bisa dibuka, tanpa
            // jejak log sama sekali. Sekarang di-log spt sendTopRampok/sendTopDagang.
            log.error("[BXH] sendTopLevel error: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Render satu entry "Top Level" ke buffer terpisah (bukan langsung ke Message utama),
     * supaya kalau method ini throw, pemanggil (sendTopLevel) bisa tangkap per-entry dan
     * skip entry itu saja tanpa merusak packet keseluruhan.
     */
    private static byte[] renderTopLevelEntry(PlayerData temp) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream w = new DataOutputStream(buf);

        List<PlayerWear> itemwear = temp.getItemwear();
        List<PlayerPart> parts = (itemwear == null ? List.<PlayerWear>of() : itemwear)
                .stream()
                .filter(wear -> wear.getIndex() == 0 || wear.getIndex() == 1 || wear.getIndex() == 6 || wear.getIndex() == 7 || wear.getIndex() == 10 || wear.getIndex() == 22) // FIX: sertakan slot 22 (amulet/cincin)
                .filter(wear -> wear.getType() >= 0 && wear.getType() < 12) // FIX AKAR MASALAH: client (class eq, method Y) nyimpen icon ke "byte[12] byArray" yg diindeks LANGSUNG pakai nilai type baca dari packet (byArray[type] = part). Array cuma 12 slot (0-11) tapi type bisa sampai 29 (lihat Player.playerWear switch); type=12 (amulet, slot 22) jelas out-of-bounds -> ArrayIndexOutOfBoundsException tertangkap catch(Exception) di client & SELURUH window ranking gagal render, walau server sukses kirim. Skip item type>=12 dari icon list; data lain (nama/level/guild) tetap normal.
                .map(wear -> new PlayerPart(wear.getType(), wear.getPart()))
                .toList();

        Guild guild = Guild.getPlayerGuild(temp.getName());

        // FIX: Level.entrys.get(level - 1) dulu bisa IndexOutOfBoundsException kalau level pemain
        // di luar jangkauan tabel `level` (mis. player level lebih tinggi dari data level yang ke-load,
        // seperti yang bisa terjadi kalau ada item/fitur baru yang mendorong pemain naik level lebih
        // jauh dari sebelumnya). Sekarang di-guard supaya persen cuma fallback ke "0.0", entry-nya
        // TETAP tampil di ranking, bukan bikin seluruh menu Top Level gagal kebuka.
        String percent = "0.0";
        int lvlIdx = temp.getLevel() - 1;
        if (lvlIdx >= 0 && lvlIdx < Level.entrys.size() && Level.entrys.get(lvlIdx).exp > 0) {
            percent = String.format("%.1f", (((float) temp.getExp() * 1000) / Level.entrys.get(lvlIdx).exp) / 10f);
        }
        String info = "Level : " + (temp.getLevel()) + "\t-\t" + percent + "%";

        w.writeUTF(temp.getName());
        w.writeByte(temp.getBody()[0]);
        w.writeByte(temp.getBody()[1]);
        w.writeByte(temp.getBody()[2]);
        w.writeShort(temp.getLevel());

        w.writeByte(parts.size());
        for (PlayerPart it : parts) {
            w.writeByte(it.getPart());
            w.writeByte(it.getType());
        }
        // Cek online: real player (session) ATAU bot player (modeBot)
        boolean isOnline = Manager.getPlayerByName(temp.getName()) != null
                || isBotOnline(temp.getName());
        w.writeByte(isOnline ? (byte) 1 : (byte) 0);
        w.writeUTF(info);
        if (guild != null) {
            w.writeShort(guild.icon);
            w.writeUTF(guild.shortName);
            w.writeByte(guild.get_mem_type(temp.getName()));
        } else {
            w.writeShort(-1);
        }

        w.flush();
        return buf.toByteArray();
    }

    public static void sendTopGuild(Session s) {

        try {

            Message m = new Message(56);
            m.writer().writeByte(3);
            m.writer().writeUTF("Top Guild");
            m.writer().writeByte(0);
            m.writer().writeInt(0);

            List<Guild> guilds = Guild.entrys
                    .stream()
                    .sorted(Comparator.comparingInt(Guild::getLevel).reversed())
                    .toList();

            m.writer().writeByte(guilds.size());
            for (Guild guild : guilds) {

                m.writer().writeUTF(guild.name);
                m.writer().writeInt(Guild.entrys.indexOf(guild));
                m.writer().writeShort(guild.icon);
                m.writer().writeUTF(guild.shortName);
                m.writer().writeUTF(guild.slogan);

            }

            s.addmsg(m);
        } catch (Exception e) {
            log.error("Unhandle Exception", e);
        }
    }

    /**
     * Top Guild diurutkan berdasarkan gold terbanyak ("Top Kaya").
     * Format paket sama persis dgn sendTopGuild(), cuma beda urutan sort,
     * biar client (yg parsing packet type 56) tidak perlu diubah.
     */
    public static void sendTopGuildByGold(Session s) {

        try {

            Message m = new Message(56);
            m.writer().writeByte(3);
            m.writer().writeUTF("Top Kaya");
            m.writer().writeByte(0);
            m.writer().writeInt(0);

            List<Guild> guilds = Guild.entrys
                    .stream()
                    .sorted(Comparator.comparingLong((Guild g) -> g.gold).reversed())
                    .toList();

            m.writer().writeByte(guilds.size());
            for (Guild guild : guilds) {

                m.writer().writeUTF(guild.name);
                m.writer().writeInt(Guild.entrys.indexOf(guild));
                m.writer().writeShort(guild.icon);
                m.writer().writeUTF(guild.shortName);
                m.writer().writeUTF(String.valueOf("Gold: " + guild.gold));

            }

            s.addmsg(m);
        } catch (Exception e) {
            log.error("Unhandle Exception", e);
        }
    }

    /**
     * Top Guild diurutkan berdasarkan gems terbanyak.
     * Format paket sama persis dgn sendTopGuild(), cuma beda urutan sort.
     */
    public static void sendTopGuildByGems(Session s) {

        try {

            Message m = new Message(56);
            m.writer().writeByte(3);
            m.writer().writeUTF("Top Gems Guild");
            m.writer().writeByte(0);
            m.writer().writeInt(0);

            List<Guild> guilds = Guild.entrys
                    .stream()
                    .sorted(Comparator.comparingInt((Guild g) -> g.gems).reversed())
                    .toList();

            m.writer().writeByte(guilds.size());
            for (Guild guild : guilds) {

                m.writer().writeUTF(guild.name);
                m.writer().writeInt(Guild.entrys.indexOf(guild));
                m.writer().writeShort(guild.icon);
                m.writer().writeUTF(guild.shortName);
                m.writer().writeUTF(String.valueOf("Gems: " + guild.gems));

            }

            s.addmsg(m);
        } catch (Exception e) {
            log.error("Unhandle Exception", e);
        }
    }

    public static void init() {
        try {

            Connection conn = SQL.gI().getConnection();
            Statement ps = conn.createStatement();
            ResultSet rs = ps.executeQuery(
                    "SELECT `id`, `level`, `exp`, `name`, `body`, `itemwear` FROM `player` WHERE `level` > 10 ORDER BY `level` DESC, exp DESC LIMIT 99;");

            entry0.clear();
            while (rs.next()) {
                Memin4 temp = new Memin4();
                temp.id = rs.getShort("id");
                temp.level = rs.getShort("level");
                temp.exp = rs.getLong("exp");
                temp.name = rs.getString("name");
                JSONArray jsar = (JSONArray) JSONValue.parse(rs.getString("body"));
                if (jsar == null) {
                    continue;
                }
                temp.head = Byte.parseByte(jsar.get(0).toString());
                temp.hair = Byte.parseByte(jsar.get(2).toString());
                temp.eye = Byte.parseByte(jsar.get(1).toString());
                jsar.clear();
                jsar = (JSONArray) JSONValue.parse(rs.getString("itemwear"));
                temp.itemwear = new ArrayList<>();
                for (int i3 = 0; i3 < jsar.size(); i3++) {
                    JSONArray jsar2 = (JSONArray) JSONValue.parse(jsar.get(i3).toString());
                    if (jsar2 == null) {
                        continue;
                    }
                    byte index_wear = Byte.parseByte(jsar2.get(9).toString());
                    if (index_wear != 0 && index_wear != 1 && index_wear != 6 && index_wear != 7 && index_wear != 10) {
                        continue;
                    }
                    byte typeVal2 = Byte.parseByte(jsar2.get(2).toString());
                    if (typeVal2 < 0 || typeVal2 >= 12) { // FIX: type>=12 bikin ArrayIndexOutOfBoundsException di client - lihat catatan lengkap di renderTopLevelEntry
                        continue;
                    }
                    PlayerPart temp2 = new PlayerPart();
                    temp2.type = typeVal2;
                    temp2.part = Byte.parseByte(jsar2.get(6).toString());
                    temp.itemwear.add(temp2);
                }
                entry0.add(temp);
            }
            rs.close();
            //
            GameMap.head = -1;
            GameMap.eye = -1;
            GameMap.hair = -1;
            GameMap.weapon = -1;
            GameMap.body = -1;
            GameMap.leg = -1;
            GameMap.hat = -1;
            GameMap.wing = -1;
            GameMap.name_mo = "";
            rs = ps.executeQuery("SELECT * FROM `player` ORDER BY `hieuchien` DESC, `id` LIMIT 1");
            if (rs.next()) {
                System.out.println("123123");
                GameMap.name_mo = rs.getString("name");
                JSONArray js = (JSONArray) JSONValue.parse(rs.getString("body"));
                GameMap.head = Short.parseShort(js.get(0).toString());
                GameMap.eye = Short.parseShort(js.get(1).toString());
                GameMap.hair = Short.parseShort(js.get(2).toString());
                js.clear();
                js = (JSONArray) JSONValue.parse(rs.getString("itemwear"));
                for (Object j : js) {
                    JSONArray jsar2 = (JSONArray) JSONValue.parse(j.toString());
                    if (jsar2 == null) {
                        return;
                    }
                    byte index_wear = Byte.parseByte(jsar2.get(9).toString());
                    if (index_wear != 0 && index_wear != 1 && index_wear != 2 && index_wear != 7 && index_wear != 10) {
                        continue;
                    }

                    PlayerPart temp = new PlayerPart();
                    temp.type = Byte.parseByte(jsar2.get(2).toString());
                    temp.part = Byte.parseByte(jsar2.get(6).toString());
                    if (temp.type == 2) {
                        GameMap.hat = (short) temp.part;
                    }
                    if (temp.type == 0) {
                        GameMap.body = (short) temp.part;
                    }
                    if (temp.type == 1) {
                        GameMap.leg = (short) temp.part;
                    }
                    if (temp.type == 7) {
                        GameMap.wing = (short) temp.part;
                    }
                    if (temp.type == 10) {
                        GameMap.weapon = (short) temp.part;
                    }
                }

            }

            rs.close();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cek apakah player dengan nama tersebut sedang online sebagai bot (modeBot=true).
     * Diperlukan karena bot tidak punya Session sehingga tidak muncul di Manager.getPlayerByName().
     */
    private static boolean isBotOnline(String name) {
        if (name == null) return false;
        for (GameMap[] maps : GameMap.entrys) {
            for (GameMap map : maps) {
                if (map == null) continue;
                for (client.Player p : map.players) {
                    if (p != null && p.modeBot && name.equalsIgnoreCase(p.name)) return true;
                }
            }
        }
        return false;
    }

    public static class Memin4 {

        public short level;
        public long exp;
        public String name;
        public byte head;
        public byte eye;
        public byte hair;
        public List<PlayerPart> itemwear;
        public Guild guild;
        public String info;
        public short id;
    }

}