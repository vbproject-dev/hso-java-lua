/*
 * DailyQuest.java — Versi database-driven
 *
 * Semua konfigurasi misi (tipe, target, reward, level range, bobot)
 * dibaca dari tabel `daily_quest_template` saat server start.
 * Admin cukup INSERT/UPDATE/DELETE baris di tabel tersebut,
 * lalu panggil DailyQuest.reload() (atau restart server) agar perubahan aktif.
 *
 * Struktur quest_daily[] di Player (panjang 6):
 *   [0] = template_id  (id di daily_quest_template, atau -1 jika kosong)
 *   [1] = difficulty
 *   [2] = progress
 *   [3] = target       (hasil random antara target_min..target_max)
 *   [4] = sisa_ambil   (reset harian)
 *   [5] = quest_type   (0=KILL, 1=COLLECT, 2=PARTY, 3=BOSS)
 *
 * quest_daily[0] menyimpan template_id (bukan mob_id langsung).
 * Gunakan DailyQuest.getTemplate(p.quest_daily[0]) untuk ambil detail.
 */

package event_daily;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import client.Player;
import core.SQL;
import core.Util;
import game.map.GameMap;
import game.map.MobInMap;
import template.Item47;
import template.MobTemplate;

public class DailyQuest {

    // -------------------------------------------------------------------------
    // Konstanta tipe quest
    // -------------------------------------------------------------------------
    public static final int TYPE_KILL_MOB     = 0;
    /** @deprecated Tidak digunakan — misi collect item telah dihapus */
    public static final int TYPE_COLLECT_ITEM = 1;
    public static final int TYPE_PARTY_GRIND  = 2;
    public static final int TYPE_BOSS_HUNT    = 3;

    // -------------------------------------------------------------------------
    // Model template (di-load dari DB)
    // -------------------------------------------------------------------------
    public static class QuestTemplate {
        public int    id;
        public String name;
        public int    questType;
        public int    targetId;      // mob_id atau item_id tergantung questType
        public int    minLevel;
        public int    maxLevel;
        public int    difficulty;
        public int    targetMin;
        public int    targetMax;
        public long   rewardGold;
        public int    rewardGem;
        public long   rewardExp;
        public short  rewardItem7;   // -1 = tidak ada
        public short  rewardItem7Qty;
        public int    weight;
        public String description;
    }

    // -------------------------------------------------------------------------
    // Cache template — key = template id
    // -------------------------------------------------------------------------
    private static final Map<Integer, QuestTemplate> TEMPLATES = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // load() — dipanggil saat server start (dari Manager.load_database)
    // -------------------------------------------------------------------------
    public static void load() {
        TEMPLATES.clear();
        try {
            List<QuestTemplate> list = SQL.gI().selectList(
                "SELECT * FROM `daily_quest_template` WHERE is_active = 1",
                rs -> {
                    QuestTemplate t = new QuestTemplate();
                    t.id             = rs.getInt("id");
                    t.name           = rs.getString("name");
                    t.questType      = rs.getInt("quest_type");
                    t.targetId       = rs.getInt("target_id");
                    t.minLevel       = rs.getInt("min_level");
                    t.maxLevel       = rs.getInt("max_level");
                    t.difficulty     = rs.getInt("difficulty");
                    t.targetMin      = rs.getInt("target_min");
                    t.targetMax      = rs.getInt("target_max");
                    t.rewardGold     = rs.getLong("reward_gold");
                    t.rewardGem      = rs.getInt("reward_gem");
                    t.rewardExp      = rs.getLong("reward_exp");
                    t.rewardItem7    = rs.getShort("reward_item7");
                    t.rewardItem7Qty = rs.getShort("reward_item7_qty");
                    t.weight         = rs.getInt("weight");
                    t.description    = rs.getString("description");
                    return t;
                }
            );
            if (list != null) {
                for (QuestTemplate t : list) {
                    TEMPLATES.put(t.id, t);
                }
            }
            System.out.println("[DailyQuest] Loaded " + TEMPLATES.size() + " quest templates from DB.");
        } catch (Exception e) {
            System.err.println("[DailyQuest] Failed to load templates: " + e.getMessage());
        }
    }

    // reload() untuk hot-reload tanpa restart server — panggil lewat admin command
    public static void reload() {
        int before = TEMPLATES.size();
        load();
        int after = TEMPLATES.size();
        System.out.println("[DailyQuest] Hot-reload selesai: " + before + " → " + after + " template aktif.");
    }

    public static QuestTemplate getTemplate(int templateId) {
        return TEMPLATES.get(templateId);
    }

    // -------------------------------------------------------------------------
    // get_quest — ambil misi baru berdasarkan level dan difficulty player
    // -------------------------------------------------------------------------
    public static void get_quest(Player p, byte difficulty) throws IOException {
        if (p.quest_daily[4] <= 0) {
            core.Service.send_notice_box(p.conn, "Kamu sudah tidak memiliki sisa ambil misi hari ini!");
            return;
        }
        // Cek apakah sudah ada misi aktif (quest_daily[0] != -1)
        if (p.quest_daily[0] != -1) {
            core.Service.send_notice_box(p.conn, "Kamu masih memiliki misi aktif! Selesaikan atau batalkan dulu.");
            return;
        }

        // Kumpulkan template yang cocok dengan level dan difficulty player
        List<QuestTemplate> candidates = new ArrayList<>();
        for (QuestTemplate t : TEMPLATES.values()) {
            if (t.difficulty == difficulty
                    && p.level >= t.minLevel
                    && p.level <= t.maxLevel) {
                candidates.add(t);
            }
        }

        // Fallback: semua difficulty jika tidak ada yang cocok
        if (candidates.isEmpty()) {
            for (QuestTemplate t : TEMPLATES.values()) {
                if (p.level >= t.minLevel && p.level <= t.maxLevel) {
                    candidates.add(t);
                }
            }
        }

        if (candidates.isEmpty()) {
            core.Service.send_notice_box(p.conn, "Tidak ada misi yang tersedia untuk level kamu saat ini!");
            return;
        }

        QuestTemplate chosen = weightedRandom(candidates);
        if (chosen == null) {
            core.Service.send_notice_box(p.conn, "Gagal memilih misi, coba lagi!");
            return;
        }

        assignQuest(p, chosen);
    }

    // -------------------------------------------------------------------------
    // assignQuest — isi slot quest_daily dari template
    // -------------------------------------------------------------------------
    private static void assignQuest(Player p, QuestTemplate t) throws IOException {
        int min = Math.max(1, t.targetMin);
        int max = Math.max(min, t.targetMax);
        int target = (min >= max) ? min : Util.random(min, max);

        p.quest_daily[0] = t.id;
        p.quest_daily[1] = t.difficulty;
        p.quest_daily[2] = 0;
        p.quest_daily[3] = target;
        p.quest_daily[4] -= 1;
        p.quest_daily[5] = t.questType;

        String typeLabel = switch (t.questType) {
            case TYPE_COLLECT_ITEM -> "[Kumpulkan Item]";
            case TYPE_PARTY_GRIND  -> "[Party Grind]";
            case TYPE_BOSS_HUNT    -> "[Berburu Boss]";
            default                -> "[Bunuh Mob]";
        };

        String targetName = resolveTargetNameWithMap(t);
        String msg;
        if (targetName != null) {
            msg = String.format(
                "%s %s\nTarget : %s\nProgress : 0/%d\nHari ini tersisa %d kali.",
                typeLabel, t.name, targetName, target, p.quest_daily[4]);
        } else {
            msg = String.format(
                "%s %s\n%s\nTarget : 0/%d\nHari ini tersisa %d kali.",
                typeLabel, t.name,
                (t.description != null && !t.description.isEmpty()) ? t.description : "",
                target, p.quest_daily[4]);
        }
        core.Service.send_notice_box(p.conn, msg);
    }

    // -------------------------------------------------------------------------
    // info_quest
    // -------------------------------------------------------------------------
    public static String info_quest(Player p) {
        if (p.quest_daily[0] == -1) {
            return String.format(
                "Kamu belum mengambil misi.\nPoin kerajinan : %d\nHari ini tersisa %d kali.",
                p.chuyencan, p.quest_daily[4]);
        }
        QuestTemplate t = getTemplate(p.quest_daily[0]);
        if (t == null) return "Data misi tidak ditemukan.";

        String typeLabel = switch (t.questType) {
            case TYPE_COLLECT_ITEM -> "[Kumpulkan Item]";
            case TYPE_PARTY_GRIND  -> "[Party Grind]";
            case TYPE_BOSS_HUNT    -> "[Berburu Boss]";
            default                -> "[Bunuh Mob]";
        };
        String targetName = resolveTargetNameWithMap(t);
        return String.format(
            "%s %s\nTarget : %s\nProgress : %d/%d\nSisa hari ini : %d kali.",
            typeLabel, t.name,
            targetName != null ? targetName : String.valueOf(t.targetId),
            p.quest_daily[2], p.quest_daily[3], p.quest_daily[4]);
    }

    // -------------------------------------------------------------------------
    // remove_quest
    // -------------------------------------------------------------------------
    public static void remove_quest(Player p) throws IOException {
        if (p.quest_daily[0] == -1) {
            core.Service.send_notice_box(p.conn, "Saat ini tidak ada misi yang diambil!");
        } else {
            resetQuestSlots(p);
            core.Service.send_notice_box(p.conn,
                "Berhasil membatalkan misi, kalau ada waktu kembali lagi untuk mengambil misi!");
        }
    }

    // -------------------------------------------------------------------------
    // finish_quest
    // -------------------------------------------------------------------------
    public static void finish_quest(Player p) throws IOException {
        if (p.quest_daily[0] == -1) {
            core.Service.send_notice_box(p.conn, "Saat ini tidak ada misi yang diambil!");
            return;
        }
        if (p.quest_daily[2] < p.quest_daily[3]) {
            core.Service.send_notice_box(p.conn, String.format(
                "Belum menyelesaikan misi! Progress: %d/%d",
                p.quest_daily[2], p.quest_daily[3]));
            return;
        }

        QuestTemplate t = getTemplate(p.quest_daily[0]);
        if (t == null) {
            core.Service.send_notice_box(p.conn, "Data misi tidak valid, hubungi GM!");
            return;
        }

        int difficulty = p.quest_daily[1];
        int killCount  = p.quest_daily[3];

        long vang = t.rewardGold > 0
            ? t.rewardGold
            : Util.random(50, 100) * (long)(difficulty + 1) * killCount;

        int ngoc = t.rewardGem > 0
            ? t.rewardGem
            : switch (difficulty) {
                case 3  -> Util.random(200, 400);
                case 2  -> Util.random(100, 200);
                case 1  -> Util.random(50, 100);
                default -> Util.random(5, 10);
            };

        long exp = t.rewardExp > 0
            ? t.rewardExp
            : Util.random(50, 100) * (long)(difficulty + 1) * killCount;

        p.updateGold(vang);
        p.updateGem(ngoc);
        p.updateExp(exp, true);
        p.chuyencan++;

        String itemMsg = "";
        if (t.rewardItem7 >= 0) {
            int qty = t.rewardItem7Qty > 0 ? t.rewardItem7Qty : 1;
            if (p.item.get_bag_able() > 0 || p.item.total_item_by_id(7, t.rewardItem7) > 0) {
                Item47 itbag = new Item47();
                itbag.id       = t.rewardItem7;
                itbag.quantity = (short) qty;
                itbag.category = 7;
                p.item.add_item_bag47(7, itbag);
                p.item.charInventory(7);
                itemMsg = "\n+ " + qty + "x item bonus";
            }
        }

        core.Service.send_notice_box(p.conn, String.format(
            "Misi selesai! Kamu mendapat:\n%s emas\n%s permata\n%s EXP%s",
            Util.number_format(vang), ngoc, Util.number_format(exp), itemMsg));

        resetQuestSlots(p);
    }

    // =========================================================================
    // Event hooks
    // =========================================================================

    public static void on_mob_killed(Player p, int mobIndex) throws IOException {
        if (p.quest_daily[0] == -1) return;
        QuestTemplate t = getTemplate(p.quest_daily[0]);
        if (t == null) return;
        switch (t.questType) {
            case TYPE_KILL_MOB -> {
                if (t.targetId == mobIndex) incrementProgress(p);
            }
            case TYPE_BOSS_HUNT -> {
                MobTemplate mob = MobTemplate.entrys.get(mobIndex);
                if (mob != null && mob.is_boss && t.targetId == mobIndex) {
                    incrementProgress(p);
                }
            }
        }
    }


    public static void on_party_kill(Player p, int mobIndex) throws IOException {
        if (p.quest_daily[0] == -1) return;
        QuestTemplate t = getTemplate(p.quest_daily[0]);
        if (t == null || t.questType != TYPE_PARTY_GRIND) return;
        if (t.targetId != mobIndex) return;
        if (p.party == null) return;
        long membersInMap = p.party.get_mems().stream()
            .filter(m -> m.map != null && m.map.mapId == p.map.mapId)
            .count();
        if (membersInMap >= 2) incrementProgress(p);
    }

    // =========================================================================
    // Util internal
    // =========================================================================

    private static void incrementProgress(Player p) throws IOException {
        if (p.quest_daily[2] >= p.quest_daily[3]) return; // already complete, ignore
        p.quest_daily[2]++;
        if (p.quest_daily[2] >= p.quest_daily[3]) {
            p.quest_daily[2] = p.quest_daily[3]; // cap exactly at target
            core.Service.send_notice_nobox_white(p.conn,
                "Misi harian selesai! Temui NPC quest untuk mengambil hadiah.");
        } else {
            // Notice kecil progress: "Bunuh Smiley 3/20"
            QuestTemplate t = getTemplate(p.quest_daily[0]);
            String targetName = (t != null) ? resolveTargetName(t) : null;
            if (targetName == null && t != null) targetName = "Target";
            String label = (t != null && t.questType == TYPE_COLLECT_ITEM) ? "Kumpulkan" : "Bunuh";
            core.Service.send_notice_nobox_white(p.conn,
                label + " " + (targetName != null ? targetName : "") + " " + p.quest_daily[2] + "/" + p.quest_daily[3]);
        }
    }

    private static void resetQuestSlots(Player p) {
        p.quest_daily[0] = -1;
        p.quest_daily[1] = -1;
        p.quest_daily[2] = 0;
        p.quest_daily[3] = 0;
        p.quest_daily[5] = -1;
    }

    private static QuestTemplate weightedRandom(List<QuestTemplate> list) {
        int total = 0;
        for (QuestTemplate t : list) total += Math.max(1, t.weight);
        if (total <= 0) return list.isEmpty() ? null : list.get(0);
        int roll = Util.random(total);
        int cumulative = 0;
        for (QuestTemplate t : list) {
            cumulative += Math.max(1, t.weight);
            if (roll < cumulative) return t;
        }
        return list.get(list.size() - 1);
    }

    private static String resolveTargetName(QuestTemplate t) {
        try {
            if (t.questType == TYPE_COLLECT_ITEM) {
                template.ItemTemplate7 item = template.ItemTemplate7.item.get(t.targetId);
                return item != null ? item.getName() : null;
            } else {
                MobTemplate mob = MobTemplate.entrys.get(t.targetId);
                return mob != null ? mob.name : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String resolveTargetNameWithMap(QuestTemplate t) {
        try {
            if (t.questType == TYPE_COLLECT_ITEM) {
                template.ItemTemplate7 item = template.ItemTemplate7.item.get(t.targetId);
                return item != null ? item.getName() : null;
            } else {
                MobTemplate mob = MobTemplate.entrys.get(t.targetId);
                if (mob == null) return null;
                String mapName = mob.gameMap != null ? mob.gameMap.name : "?";
                return mob.name + " (Map: " + mapName + ")";
            }
        } catch (Exception e) {
            return null;
        }
    }

}