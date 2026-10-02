package event_daily;

import client.Player;
import core.Service;
import core.Util;
import game.map.GameMap;
import template.*;
import core.SQL;
import client.io.Message;
import model.map.Vgo;
import org.json.simple.JSONArray;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KingCupManager {

    public static int  TURN_KING_CUP = -1;
    public static int  MAX_TURN      = 7;  // jumlah musim/akumulasi per season (bukan jumlah grup)
    public static int  DAY_OFF       = 2;
    public static final int NUM_GROUPS   = 6;  // jumlah grup level (60-100, 101-160, 161-200, 201-230, 231-250, 251-300)

    // Batas peserta per grup — Fix #3 (cegah terlalu banyak pasangan sekaligus)
    public static final int MAX_PARTICIPANTS_PER_GROUP = 120;

    // -------------------------------------------------------------------------
    // Fix #14: koordinat Map Tunggu (Map 100). setGroup() cuma memilih pemain
    // yang SEDANG BERDIRI di map ini; dulu tidak ada apa pun yang memindahkan
    // pemain ke sana, jadi pemain yang sudah daftar tapi lupa/​tidak tahu harus
    // pindah map sendiri bisa terlewat setiap ronde sampai King Cup selesai
    // tanpa pernah ikut bertanding sama sekali (dan tanpa notifikasi apa pun).
    // TODO: sesuaikan LOBBY_X/LOBBY_Y dengan titik spawn aman di peta Map 100.
    // -------------------------------------------------------------------------
    public static final byte  LOBBY_MAP = 100;
    public static final short LOBBY_X   = 432;
    public static final short LOBBY_Y   = 354;

    public static List<String> group_60_100   = new ArrayList<>();
    public static List<String> group_101_160  = new ArrayList<>();
    public static List<String> group_161_200  = new ArrayList<>();
    public static List<String> group_201_230  = new ArrayList<>();
    public static List<String> group_231_250  = new ArrayList<>();
    public static List<String> group_251_300  = new ArrayList<>();
    public static List<String> list_name      = new ArrayList<>();

    // Fix #7: whitelist kolom untuk cegah SQL injection di updateData()
    private static final Set<String> VALID_COLUMNS = new HashSet<>(Arrays.asList(
            "group_60_100", "group_101_160", "group_161_200",
            "group_201_230", "group_231_250", "group_251_300"
    ));

    // -------------------------------------------------------------------------
    // Registrasi peserta
    // Fix #1: cegah duplikat registrasi
    // Fix #2: cegah daftar saat event sudah berjalan
    // Fix #3: batasi jumlah peserta per grup
    // -------------------------------------------------------------------------
    public static void register(final Player p) throws IOException {
        int level = p.level;

        // Pendaftaran hanya boleh saat fase registrasi terbuka
        if (KingCup.running && !KingCup.registrationOpen) {
            Service.send_notice_box(p.conn, "Pendaftaran sudah ditutup! Ronde sudah berjalan.\nDaftar di season berikutnya.");
            return;
        }
        if (!KingCup.running && KingCupManager.TURN_KING_CUP < 0) {
            Service.send_notice_box(p.conn, "King Cup belum dibuka oleh GM. Tunggu pengumuman!");
            return;
        }

        if (level < 60) {
            Service.send_notice_box(p.conn, "Syarat level minimum 60 baru bisa mendaftar.");
            return;
        }

        // Bug fix: biaya pendaftaran ditarik dengan updateGem(-1000) (permata),
        // tapi yang dicek justru getGold() (emas). Pemain dengan emas banyak dan
        // permata 0 tetap bisa mendaftar, dan updateGem() tidak punya penjaga nilai
        // minimum — saldo permatanya jadi MINUS 1000.
        if (p.getGem() < 1000) {
            Service.send_notice_box(p.conn, "Permata-mu tidak cukup 1000.");
            return;
        }

        // Fix #1: cegah daftar ganda
        if (list_name.contains(p.name)) {
            Service.send_notice_box(p.conn, "Kamu sudah terdaftar di King Cup.");
            return;
        }

        // Tentukan grup berdasarkan level
        List<String> targetGroup;
        String       columnName;
        int          groupId;
        String       groupLabel;

        if (level <= 100) {
            targetGroup = group_60_100;  columnName = "group_60_100";  groupId = 1; groupLabel = "60-100";
        } else if (level <= 160) {
            targetGroup = group_101_160; columnName = "group_101_160"; groupId = 2; groupLabel = "101-160";
        } else if (level <= 200) {
            targetGroup = group_161_200; columnName = "group_161_200"; groupId = 3; groupLabel = "161-200";
        } else if (level <= 230) {
            targetGroup = group_201_230; columnName = "group_201_230"; groupId = 4; groupLabel = "201-230";
        } else if (level <= 250) {
            targetGroup = group_231_250; columnName = "group_231_250"; groupId = 5; groupLabel = "231-250";
        } else {
            targetGroup = group_251_300; columnName = "group_251_300"; groupId = 6; groupLabel = "251-300";
        }

        // Fix #3: batasi peserta per grup
        if (targetGroup.size() >= MAX_PARTICIPANTS_PER_GROUP) {
            Service.send_notice_box(p.conn,
                    "Grup level-mu sudah penuh (" + MAX_PARTICIPANTS_PER_GROUP + " peserta). Coba lagi musim depan.");
            return;
        }

        targetGroup.add(p.name);
        updateData(targetGroup, columnName);
        p.group_king_cup  = groupId;
        p.point_king_cup  = 0;
        p.bye_count_king_cup = 0;
        list_name.add(p.name);
        p.updateGem(-1000);
        Service.send_notice_box(p.conn, "Pendaftaran berhasil! Kamu masuk grup level " + groupLabel + ".");

        // Fix #14: langsung antar ke Map Tunggu supaya pemain tidak perlu tahu/​
        // ingat sendiri bahwa dia harus berada di Map 100 saat ronde dimulai —
        // sebelumnya register() tidak memindahkan pemain sama sekali, jadi kalau
        // dia tetap di map lain, setGroup() akan selalu melewatinya diam-diam.
        try {
            if (p.map == null || p.map.mapId != LOBBY_MAP) {
                Vgo vgo = new Vgo();
                vgo.toMap = LOBBY_MAP;
                vgo.toX   = LOBBY_X;
                vgo.toY   = LOBBY_Y;
                p.changeMap(p, vgo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Batalkan pendaftaran & refund permata — Fix #4
    // -------------------------------------------------------------------------
    public static void unregister(final Player p) throws IOException {
        // Tidak bisa unregister jika ronde sudah berjalan
        if (KingCup.running && !KingCup.registrationOpen) {
            Service.send_notice_box(p.conn, "Tidak bisa batalkan pendaftaran, pertandingan sudah dimulai.");
            return;
        }

        if (!list_name.contains(p.name)) {
            Service.send_notice_box(p.conn, "Kamu belum terdaftar di King Cup.");
            return;
        }

        List<String> targetGroup = getGroupByGroupId(p.group_king_cup);
        String       columnName  = getGroupColumnName(p.group_king_cup);

        if (targetGroup != null) {
            targetGroup.remove(p.name);
            updateData(targetGroup, columnName);
        }
        list_name.remove(p.name);

        p.group_king_cup  = 0;
        p.point_king_cup  = 0;
        p.bye_count_king_cup = 0;
        // Fix #4: kembalikan biaya pendaftaran
        p.updateGem(1000);
        Service.send_notice_box(p.conn, "Pendaftaran dibatalkan. 1000 permata telah dikembalikan.");
    }

    // -------------------------------------------------------------------------
    // Ambil daftar pemain online di lobby yang siap bertanding
    // -------------------------------------------------------------------------
    public static ArrayList<Player> setGroup(List<String> group) {
        ArrayList<Player> players = new ArrayList<>();
        for (String name : group) {
            Player p = GameMap.get_player_by_name(name);
            // map_id 100 = lobby/ruang tunggu arena King Cup
            // Bug fix: p.map bisa null saat pemain sedang pindah map -> NPE di sini
            // mematikan seluruh proses pengundian ronde untuk grup tersebut.
            if (p != null && p.map != null && p.map.mapId == 100
                    && p.conn != null && p.conn.connected && !p.isdie) {
                // FIX: syaratnya dulu typepk == 0, padahal nilai "tanpa flag" di
                // server ini adalah -1 (Player.set_in4() saat login, dan
                // changeFlag(map, p, -1) saat masuk map biasa). Map lobby 100 juga
                // isMapLoiDai() sehingga enter() TIDAK mereset flag — jadi pemain
                // normal di lobby selalu -1 dan tidak pernah lolos filter ini,
                // bikin pengundian ronde sering menghasilkan grup kosong.
                // Yang perlu ditolak cuma flag arena aktif (11/12/13).
                if (p.typepk <= 0) {
                    players.add(p);
                }
            }
        }
        return players;
    }

    // -------------------------------------------------------------------------
    // Fix #14: tarik semua pemain terdaftar (yang online & tidak sedang
    // bertanding) ke Map Tunggu sebelum pengundian ronde. Ini jaring pengaman
    // kalau pemain sempat pindah map lagi setelah daftar (mis. belanja,
    // farming) — tanpa ini mereka bisa terlewat pengundian selamanya karena
    // setGroup() hanya melihat siapa yang KEBETULAN sedang berdiri di map 100.
    // -------------------------------------------------------------------------
    public static void summonRegisteredToLobby() {
        for (String name : list_name) {
            Player p = GameMap.get_player_by_name(name);
            if (p == null || p.conn == null || !p.conn.connected || p.isdie) continue;
            if (p.map == null) continue;
            // Sedang bertanding (flag arena aktif) atau sudah di lobby/arena -> lewati
            if (p.typepk == KingCup.PK_P1 || p.typepk == KingCup.PK_P2) continue;
            if (p.map.mapId == LOBBY_MAP || p.map.mapId == 102) continue;

            try {
                Vgo vgo = new Vgo();
                vgo.toMap = LOBBY_MAP;
                vgo.toX   = LOBBY_X;
                vgo.toY   = LOBBY_Y;
                p.changeMap(p, vgo);
                Service.send_notice_nobox_white(p.conn,
                        "Kamu ditarik ke Map Tunggu King Cup, bersiaplah untuk ronde berikutnya!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Simpan poin ke database — Fix #5
    // -------------------------------------------------------------------------
    public static void savePointToDb(Player p) {
        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE `player` SET `point_king_cup` = ? WHERE `name` = ?;")) {
            ps.setInt(1, p.point_king_cup);
            ps.setString(2, p.name);
            if (ps.executeUpdate() > 0) {
                conn.commit();
            }
        } catch (SQLException e) {
            System.err.println("[KingCupManager] Gagal simpan point untuk " + p.name);
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Update data grup ke database
    // Fix #7: validasi whitelist kolom sebelum query
    // -------------------------------------------------------------------------
    public static void updateData(List<String> group, String column) {
        if (!VALID_COLUMNS.contains(column)) {
            System.err.println("[KingCupManager] updateData: nama kolom tidak valid -> " + column);
            return;
        }
        try (Connection connection = SQL.gI().getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE `king_cup` SET `" + column + "` = ?;")) {
            JSONArray array = new JSONArray();
            for (String name : group) {
                array.add(name);
            }
            ps.setString(1, array.toJSONString());
            if (ps.executeUpdate() > 0) {
                connection.commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Update turn ke database
    // -------------------------------------------------------------------------
    public static void updateTurn() {
        try (Connection connection = SQL.gI().getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE `king_cup` SET `turn_king_cup` = ?;")) {
            ps.setInt(1, KingCupManager.TURN_KING_CUP);
            if (ps.executeUpdate() > 0) {
                connection.commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Log hasil pertandingan ke database — Fix #12
    // Butuh tabel: CREATE TABLE king_cup_log (
    //   id INT AUTO_INCREMENT PRIMARY KEY,
    //   turn INT, name1 VARCHAR(50), name2 VARCHAR(50),
    //   score1 INT, score2 INT, winner VARCHAR(50), match_time DATETIME DEFAULT NOW()
    // );
    // -------------------------------------------------------------------------
    public static void logMatch(String name1, String name2, int score1, int score2, String winner) {
        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO `king_cup_log` (`turn`,`name1`,`name2`,`score1`,`score2`,`winner`) VALUES (?,?,?,?,?,?);")) {
            ps.setInt(1, TURN_KING_CUP);
            ps.setString(2, name1);
            ps.setString(3, name2);
            ps.setInt(4, score1);
            ps.setInt(5, score2);
            ps.setString(6, winner != null ? winner : "SERI");
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            // Log tidak kritis — jangan sampai menghentikan flow pertandingan
            System.err.println("[KingCupManager] logMatch gagal: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Leaderboard real-time — Fix #10
    // Tampilkan top 10 per grup pemain tersebut
    // -------------------------------------------------------------------------
    public static void sendLeaderboard(Player p) throws IOException {
        int groupId = p.group_king_cup;
        if (groupId <= 0) {
            Service.send_notice_box(p.conn, "Kamu belum terdaftar di King Cup.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Leaderboard King Cup (Grupmu) ===\n");

        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT `name`, `point_king_cup` FROM `player` " +
                     "WHERE `group_king_cup` = ? AND `point_king_cup` > 0 " +
                     "ORDER BY `point_king_cup` DESC LIMIT 10;")) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    sb.append(String.format("%d. %s — %d poin\n",
                            rank++, rs.getString("name"), rs.getInt("point_king_cup")));
                }
            }
        } catch (SQLException e) {
            sb.append("(Gagal memuat leaderboard)");
            e.printStackTrace();
        }

        sb.append("Poinmu saat ini: ").append(p.point_king_cup);
        Service.send_notice_box(p.conn, sb.toString());
    }

    // -------------------------------------------------------------------------
    // Akhir musim — beri ranking & bersihkan data grup
    // -------------------------------------------------------------------------
    public static void endSeason(int group) throws SQLException {
        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement psSelect = conn.prepareStatement(
                     "SELECT `name`,`point_king_cup` FROM `player` " +
                     "WHERE `point_king_cup` > 0 AND `group_king_cup` = ? " +
                     "ORDER BY `point_king_cup` DESC;");
             PreparedStatement psUpdate = conn.prepareStatement(
                     "UPDATE `player` SET `type_reward_king_cup` = ? WHERE `name` = ?;")) {

            psSelect.setInt(1, group);
            try (ResultSet rs = psSelect.executeQuery()) {
                short i = 0;
                while (rs.next()) {
                    // Hanya top 4 per grup yang mendapat hadiah
                    if (i >= 4) break;
                    String name = rs.getString("name");
                    short type  = (short) (i + 4 * (group - 1) + 1);
                    psUpdate.setShort(1, type);
                    psUpdate.setString(2, name);
                    Player player = GameMap.get_player_by_name(name);
                    if (player != null) {
                        player.type_reward_king_cup = (byte) type;
                    }
                    psUpdate.addBatch();
                    i++;
                }
            }
            psUpdate.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        List<String> target = getGroupByGroupId(group);
        if (target != null) {
            List<String> names = new ArrayList<>(target);
            // Bersihkan bye_count di memori untuk semua pemain yang online
            for (String name : names) {
                Player p = GameMap.get_player_by_name(name);
                if (p != null) p.bye_count_king_cup = 0;
            }
            // Bug fix: list_name.clear() menghapus SELURUH peserta padahal
            // endSeason() dipanggil per grup (g = 1..6). Grup 1 selesai -> nama
            // peserta grup 2..6 ikut terhapus, sehingga mereka bisa mendaftar
            // ganda di musim yang sama dan pengecekan duplikat jadi tidak berguna.
            // Hapus hanya nama-nama grup yang sedang diproses.
            list_name.removeAll(names);
            target.clear();
            updateData(target, getGroupColumnName(group));
        }
    }

    // -------------------------------------------------------------------------
    // Klaim hadiah akhir musim
    // -------------------------------------------------------------------------
    public static void rewardKingCup(Player p) throws IOException {
        if (KingCupManager.TURN_KING_CUP < MAX_TURN) {
            Service.send_notice_box(p.conn, "Musim ini belum selesai.");
            return;
        }
        if (p.type_reward_king_cup == 0 || p.point_king_cup <= 0) {
            Service.send_notice_box(p.conn, "Kamu tidak ada dalam daftar atau sudah menerima hadiah.");
            return;
        }

        short[] id_reward_7;
        short[] quantity_reward_7;
        short[] id_reward_4;
        short[] quantity_reward_4;
        boolean isHaveBook = false;

        // type_reward_king_cup = i + 4*(group-1) + 1
        // group 1=Lv60-100, 2=Lv101-160, 3=Lv161-200, 4=Lv201-230, 5=Lv231-250, 6=Lv251-300
        // Rank 1: type 1,5,9,13,17,21  → hadiah terbaik
        // Rank 2: type 2,6,10,14,18,22 → hadiah kedua
        // Rank 3: type 3,7,11,15,19,23 → hadiah ketiga
        // Rank 4: type 4,8,12,16,20,24 → hadiah keempat (partisipasi)
        switch (p.type_reward_king_cup) {
            // ── Rank 1 semua grup ──────────────────────────────────────────
            case 1, 5, 9, 13 -> {           // grup 1-4 (Lv 60-230)
                id_reward_7       = new short[]{14, (short) Util.random(8, 10), 11, 349};
                quantity_reward_7 = new short[]{3, 10, 10, 1};
                id_reward_4       = new short[]{53, 54};
                quantity_reward_4 = new short[]{1, 1};
            }
            case 17, 21 -> {                // grup 5-6 (Lv 231-300) — dapat skill book
                id_reward_7       = new short[]{14, (short) Util.random(8, 10), 11, 349};
                quantity_reward_7 = new short[]{3, 10, 10, 1};
                id_reward_4       = new short[]{10, 53, 54};
                quantity_reward_4 = new short[]{1, 1, 1};
                isHaveBook = true;
            }
            // ── Rank 2 semua grup ──────────────────────────────────────────
            case 2, 6, 10, 14 -> {          // grup 1-4 (Lv 60-230)
                id_reward_7       = new short[]{(short) Util.random(8, 10), 11};
                quantity_reward_7 = new short[]{5, 5};
                id_reward_4       = new short[]{53, 54};
                quantity_reward_4 = new short[]{1, 1};
            }
            case 18, 22 -> {                // grup 5-6 (Lv 231-300)
                id_reward_7       = new short[]{14};
                quantity_reward_7 = new short[]{2};
                id_reward_4       = new short[]{10, 53, 54};
                quantity_reward_4 = new short[]{1, 1, 1};
            }
            // ── Rank 3 semua grup ──────────────────────────────────────────
            case 3, 7, 11, 15 -> {          // grup 1-4 (Lv 60-230)
                id_reward_7       = new short[]{(short) Util.random(8, 10), 11};
                quantity_reward_7 = new short[]{3, 3};
                id_reward_4       = new short[]{53};
                quantity_reward_4 = new short[]{1};
            }
            case 19, 23 -> {                // grup 5-6 (Lv 231-300)
                id_reward_7       = new short[]{14};
                quantity_reward_7 = new short[]{1};
                id_reward_4       = new short[]{10, 53, 54};
                quantity_reward_4 = new short[]{1, 1, 1};
            }
            // ── Rank 4 semua grup (hadiah partisipasi) ─────────────────────
            case 4, 8, 12, 16, 20, 24 -> {
                id_reward_7       = new short[]{(short) Util.random(8, 10)};
                quantity_reward_7 = new short[]{2};
                id_reward_4       = new short[]{53};
                quantity_reward_4 = new short[]{1};
            }
            // ── Default: type tidak dikenali ───────────────────────────────
            default -> {
                Service.send_notice_box(p.conn, "Tipe reward tidak dikenali (type=" + p.type_reward_king_cup + "). Hubungi GM.");
                return;
            }
        }

        // Bug fix: saat isHaveBook = true ada satu item tambahan (skill book), tapi
        // kebutuhan slot dihitung tanpa memperhitungkannya. Tas yang kurang persis
        // satu slot tetap lolos pengecekan dan salah satu hadiah hilang.
        int slotNeeded = id_reward_7.length + id_reward_4.length + 1 + (isHaveBook ? 1 : 0);
        if (p.item.get_bag_able() < slotNeeded) {
            Service.send_notice_nobox_white(p.conn, "Inventory penuh! Kosongkan dulu sebelum ambil hadiah.");
            return;
        }

        int gold = calculateGold(p.point_king_cup);

        Message m = new Message(78);
        m.writer().writeUTF("Kamu mendapatkan hadiah King Cup!");
        if (isHaveBook) {
            m.writer().writeByte(id_reward_7.length + id_reward_4.length + 2);
            addBookSkill(m, p, 1);
        } else {
            m.writer().writeByte(id_reward_7.length + id_reward_4.length + 1);
        }
        writeRewardsToMessage(p, m, id_reward_7, quantity_reward_7, (byte) 7);
        writeRewardsToMessage(p, m, id_reward_4, quantity_reward_4, (byte) 4);

        m.writer().writeUTF("Gold");
        m.writer().writeShort(0);
        m.writer().writeInt(gold);
        m.writer().writeByte(4);
        m.writer().writeByte(0);
        m.writer().writeByte(0);
        m.writer().writeUTF("");
        m.writer().writeByte(1);
        m.writer().writeByte(0);
        p.conn.addmsg(m);
        m.cleanup();

        p.updateGold(gold);
        p.point_king_cup        = 0;
        p.group_king_cup        = -1;
        p.type_reward_king_cup  = 0;
        p.bye_count_king_cup    = 0;
    }

    // -------------------------------------------------------------------------
    // Helper: tulis reward ke Message
    // -------------------------------------------------------------------------
    private static void writeRewardsToMessage(Player p, Message m, short[] ids, short[] quantities, byte type)
            throws IOException {
        for (int i = 0; i < ids.length; i++) {
            if (type == 7) {
                ItemTemplate7 item = ItemTemplate7.item.get(ids[i]);
                m.writer().writeUTF(item.getName());
                m.writer().writeShort(item.getIcon());
                Item47 item47 = new Item47();
                item47.id       = item.getId();
                item47.quantity = quantities[i];
                p.item.add_item_bag47(7, item47);
            } else if (type == 4) {
                ItemTemplate4 item = ItemTemplate4.item.get(ids[i]);
                m.writer().writeUTF(item.getName());
                m.writer().writeShort(item.getIcon());
                Item47 item47 = new Item47();
                item47.id       = item.getId();
                item47.quantity = quantities[i];
                p.item.add_item_bag47(4, item47);
            }
            m.writer().writeInt(quantities[i]);
            m.writer().writeByte(type);
            m.writer().writeByte(0); // tier
            m.writer().writeByte(0); // color
        }
    }

    // -------------------------------------------------------------------------
    // Helper: tambah skill book ke reward
    // -------------------------------------------------------------------------
    private static void addBookSkill(Message m, Player p, int quantity) throws IOException {
        for (int i = 0; i < quantity; i++) {
            ItemTemplate3 temp3 = ItemTemplate3.item.get(Util.random(4577, 4585));
            Item3 it = new Item3();
            it.id    = temp3.getId();
            it.name  = temp3.getName();
            it.clazz = temp3.getClazz();
            it.type  = temp3.getType();
            it.level = temp3.getLevel();
            it.icon  = temp3.getIcon();
            it.op    = temp3.getOp();
            it.color = 5;
            it.part  = temp3.getPart();
            p.item.add_item_bag3(it);

            m.writer().writeUTF(temp3.getName());
            m.writer().writeShort(temp3.getIcon());
            m.writer().writeInt(1);
            m.writer().writeByte(3);
            m.writer().writeByte(0);
            m.writer().writeByte(4);
        }
    }

    // -------------------------------------------------------------------------
    // Helper: hitung gold reward dari total poin
    // -------------------------------------------------------------------------
    private static int calculateGold(int point) {
        if      (point >= 2520) return point * 5000;
        else if (point >= 1680) return point * 4000;
        else if (point >= 1260) return point * 1000;
        else if (point >= 840)  return point * 800;
        else                    return point * 500;
    }

    // -------------------------------------------------------------------------
    // Helper: ambil List grup berdasarkan groupId
    // -------------------------------------------------------------------------
    private static List<String> getGroupByGroupId(int groupId) {
        return switch (groupId) {
            case 1 -> group_60_100;
            case 2 -> group_101_160;
            case 3 -> group_161_200;
            case 4 -> group_201_230;
            case 5 -> group_231_250;
            case 6 -> group_251_300;
            default -> null;
        };
    }

    // -------------------------------------------------------------------------
    // Helper: nama kolom DB berdasarkan groupId
    // -------------------------------------------------------------------------
    public static String getGroupColumnName(int group) {
        return switch (group) {
            case 1 -> "group_60_100";
            case 2 -> "group_101_160";
            case 3 -> "group_161_200";
            case 4 -> "group_201_230";
            case 5 -> "group_231_250";
            case 6 -> "group_251_300";
            default -> "group_60_100";
        };
    }
}