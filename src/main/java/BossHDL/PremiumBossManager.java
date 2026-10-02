package BossHDL;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import client.Player;
import client.io.Message;
import core.Manager;
import core.SQL;
import core.Service;
import core.Util;
import feature.teleport.PremiumTeleportManager;
import game.event.GameEvent;
import game.map.Eff_special_skill;
import game.map.GameMap;
import game.map.LeaveItemMap;
import game.map.MapService;
import game.map.MobInMap;
import template.Item3;
import template.Item47;
import template.ItemTemplate3;
import template.MobTemplate;
import utils._Time;

/**
 * PremiumBossManager — World Boss eksklusif untuk map premium (111-114).
 * Dibangun ulang mengikuti pola WorldBoss + GameEvent agar masuk
 * ke game-loop yang sudah berjalan via GameEventManager.
 */
public class PremiumBossManager extends GameEvent {

    private static final String EVENT_NAME = "PREMIUM_BOSS";

    // List boss yang dimuat dari tabel boss_premium
    public static final CopyOnWriteArrayList<MobInMap> entrys = new CopyOnWriteArrayList<>();

    public PremiumBossManager() {
        // ── Jadwal: setiap hari, hanya jam 21:00 - 22:00 ─────────────────
        super(EVENT_NAME,
                EnumSet.allOf(DayOfWeek.class),
                List.of(new TimeEvent(
                        java.time.LocalTime.of(21, 0),
                        java.time.LocalTime.of(22, 0)
                ))
        );
    }

    // ---------------------------------------------------------------
    // GameEvent lifecycle
    // ---------------------------------------------------------------

    @Override
    protected boolean isEnabled() {
        return Manager.gI().event_premium_boss;
    }

    @Override
    protected void onStart() {
        if (!load()) {
            System.err.println("[PremiumBoss] Gagal load dari DB.");
            return;
        }
        System.out.println("[PremiumBoss] Event dimulai — " + entrys.size() + " boss premium dimuat.");
        broadcast("⚔ [PREMIUM BOSS] Event boss premium telah dimulai! Serang sekarang hingga pukul 22:00!");

        // Spawn semua boss langsung saat event mulai (1x per event window)
        for (MobInMap mob : entrys) {
            try {
                GameMap[] maps = GameMap.getMapById(mob.map_id);
                if (maps == null || maps.length <= mob.zone_id) continue;
                GameMap gm = maps[mob.zone_id];

                mob.isdie          = false;
                mob.is_boss_active = true;
                mob.hp             = mob.getMaxHP();
                mob.objectId       = 5000 + Util.nextInt(500);
                mob.top_dame.clear();
                mob.time_regen_hp  = 0; // reset timer regen tiap kali boss spawn baru
                mob.lastSpawnedHour = java.time.LocalTime.now().getHour(); // tandai sudah spawn jam ini

                gm.Boss_entrys.remove(mob);
                gm.Boss_entrys.add(mob);

                System.out.println("[PremiumBoss] " + mob.template.name + " muncul di " + gm.name);
                if (mob.announceKill) {
                    broadcast("⚔ [PREMIUM] " + mob.template.name + " telah muncul di " + gm.name + "!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onEnd() {
        // Jam 22:00 — keluarkan semua boss dari map, bersihkan list
        for (MobInMap mob : entrys) {
            try {
                GameMap[] maps = GameMap.getMapById(mob.map_id);
                if (maps == null || maps.length <= mob.zone_id) continue;
                GameMap gm = maps[mob.zone_id];
                gm.Boss_entrys.remove(mob);
                mob.is_boss_active    = false;
                mob.isdie             = true;
                mob.lastSpawnedHour   = -1; // reset untuk event besok
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        entrys.clear();
        broadcast("🌙 [PREMIUM BOSS] Event boss premium telah berakhir. Sampai jumpa besok pukul 21:00!");
        System.out.println("[PremiumBoss] Event berakhir — semua boss dikeluarkan dari map.");
    }

    @Override
    protected void onUpdate() {
        // Boss hanya spawn 1x per event window (di onStart).
        // onUpdate tidak melakukan respawn — boss yang mati dalam window ini
        // tidak akan muncul lagi, baru spawn besok saat onStart berikutnya.
    }

    // ---------------------------------------------------------------
    // Load dari DB
    // ---------------------------------------------------------------

    public static boolean load() {
        entrys.clear();
        // FIX RESOURCE LEAK: conn tidak pernah di-close() (cuma rs/st yang manual
        // close di akhir), dan karena manual, exception di tengah parsing baris
        // manapun bikin rs/st ikut bocor juga. try-with-resources menutup
        // ketiganya selalu, sukses maupun gagal.
        try (Connection conn = SQL.gI().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM `boss_premium` WHERE `active` = 1")) {

            while (rs.next()) {
                int mobId = rs.getInt("id");
                MobTemplate tmpl = MobTemplate.getMob(mobId);
                if (tmpl == null) {
                    System.err.println("[PremiumBoss] MobTemplate tidak ditemukan: id=" + mobId);
                    continue;
                }

                int mapId = rs.getInt("map");
                if (!PremiumTeleportManager.isPremiumMap(mapId)
                        || PremiumTeleportManager.isGatewayMap(mapId)) {
                    System.err.println("[PremiumBoss] map=" + mapId
                            + " bukan 111-114, boss id=" + mobId + " dilewati.");
                    continue;
                }

                MobInMap mob = new MobInMap();
                mob.template  = tmpl;
                mob.name      = tmpl.name;
                mob.level     = tmpl.level;
                mob.objectId  = 5000 + rs.getInt("uid");
                mob.map_id    = (byte) mapId;
                mob.zone_id   = rs.getByte("area");
                mob.x         = (short) (rs.getInt("x") * 24);
                mob.y         = (short) (rs.getInt("y") * 24);
                mob.setBoss(true);
                mob.setPremiumBoss(true);

                long hpDb = rs.getLong("hp");
                int hp = (int) Math.min(hpDb, Integer.MAX_VALUE);
                mob.maxHp = hp > 0 ? hp : (tmpl.hpmax * tmpl.level) / 2;
                mob.hp    = mob.maxHp;

                int dbDamage  = rs.getInt("damage");
                int dbDefense = rs.getInt("defense");
                mob.setBaseDamage(dbDamage   > 0 ? dbDamage   : tmpl.level * 600);
                mob.setBaseDefense(dbDefense > 0 ? dbDefense  : 0);

                // respawn_min menit → millisecond
                int respawnMs = rs.getInt("respawn_min") * 60 * 1000;
                mob.timeBossRecive = respawnMs > 0 ? respawnMs : 60 * 60 * 1000; // default 1 jam

                mob.announceKill = rs.getByte("announce") == 1;
                mob.regenHpPct = rs.getInt("regen_hp_pct");
                mob.regenIntervalSec = rs.getInt("regen_interval_sec");

                // Boss langsung siap spawn saat server start
                mob.isdie          = true;
                mob.is_boss_active = false;
                mob.time_back      = System.currentTimeMillis(); // spawn segera

                parseItem3(mob, rs.getString("item3"));
                parseItem4or7(mob, rs.getString("item4"), false);
                parseItem4or7(mob, rs.getString("item7"), true);

                entrys.add(mob);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // ---------------------------------------------------------------
    // Drop handling — dipanggil dari MobInMap.setDie()
    // ---------------------------------------------------------------

    public static void onPremiumBossDie(GameMap gameMap, MobInMap mob, Player killer)
            throws IOException {

        // Cari player dengan top-damage dari mob.top_dame
        String topDamagePlayer = killer.name; // fallback ke killer jika top_dame kosong
        long topDamage = 0;
        for (java.util.Map.Entry<String, Long> en : mob.top_dame.entrySet()) {
            if (en.getValue() > topDamage) {
                topDamage = en.getValue();
                topDamagePlayer = en.getKey();
            }
        }

        if (mob.announceKill) {
            try {
                Manager.gI().chatKTGprocess(
                        "💀 [PREMIUM] " + topDamagePlayer
                                + " telah mengalahkan " + mob.template.name + "!");
            } catch (Exception ignore) {}
        }

        // ── DIMATIKAN (jangan dihapus) ──────────────────────────────────
        // Point kill boss premium berdasarkan top-damage sudah tidak dipakai lagi.
        // Sekarang point kill (1 point untuk setiap player yang ikut hit boss) sudah
        // ditangani terpusat di MobInMap.setDie(), jadi tidak perlu diberikan lagi di sini
        // (kalau ditambahkan lagi di sini akan jadi dobel point).
        // +2 point kill untuk player top-damage boss premium
        // BossKillPoint.addPoint(topDamagePlayer, 2);

        // +2 point kill juga untuk player last-hit (killer), kalau bukan player yang sama
        // dengan top-damage (biar tidak dobel point untuk orang yang sama)
        // if (killer != null && killer.name != null && !killer.name.equals(topDamagePlayer)) {
        //     BossKillPoint.addPoint(killer.name, 2);
        // }

        // Set respawn timer
        mob.time_back = System.currentTimeMillis() + mob.timeBossRecive;

        GameMap[] maps = GameMap.getMapById(mob.map_id);
        if (maps == null || maps.length <= mob.zone_id) return;
        GameMap gm = maps[mob.zone_id];

        // Drop emas ke map
        for (int i = 0; i < 20; i++) {
            LeaveItemMap.leave_vang(gameMap, mob, -1);
        }

        // Distribusi item ke player yang ikut fight & punya sesi premium
        PremiumTeleportManager ptm = PremiumTeleportManager.gI();

        for (Player p : new ArrayList<>(gm.players)) {
            if (!mob.top_dame.containsKey(p.name)) continue;

            if (!ptm.hasActiveSession(p)) {
                Service.send_notice_box(p.conn,
                        "⚠ Kamu tidak memiliki akses premium aktif.\nDrop eksklusif hanya untuk member premium.");
                continue;
            }

            List<Short>   items      = new ArrayList<>();
            List<Integer> quantities = new ArrayList<>();
            List<Short>   categories = new ArrayList<>();

            // Drop item3 (equipment) — 15% chance
            if (!mob.item3.isEmpty()) {
                for (short itemId : mob.item3.values()) {
                    if (Util.nextInt(100) < 15) {
                        ItemTemplate3 tmpl = ItemTemplate3.item.get(itemId);
                        if (tmpl == null) continue;
                        Item3 equip = buildItem3(tmpl);
                        p.item.add_item_bag3(equip);
                        items.add(itemId);
                        quantities.add(1);
                        categories.add((short) 3);
                    }
                }
            }

            // Drop item4 (consumable/gold/gem)
            if (!mob.item4.isEmpty()) {
                for (java.util.Map.Entry<Short, Short> entry : mob.item4.entrySet()) {
                    Item47 pot = new Item47();
                    pot.id       = entry.getKey();
                    pot.quantity = entry.getValue();
                    pot.category = 4;

                    if (pot.id == -1) {
                        long vang = Util.random(500_000, 1_000_000);
                        items.add(pot.id);
                        quantities.add((int) vang);
                        categories.add((short) 4);
                        p.updateGold(vang);
                    } else if (pot.id == -2) {
                        long gem = Util.random(1, 100);
                        items.add(pot.id);
                        quantities.add((int) gem);
                        categories.add((short) 4);
                        p.updateGem(gem);
                    } else {
                        items.add(pot.id);
                        quantities.add((int) pot.quantity);
                        categories.add((short) 4);
                        p.item.add_item_bag47(4, pot);
                    }
                }
            }

            // Drop item7
            if (!mob.item7.isEmpty()) {
                for (java.util.Map.Entry<Short, Short> entry : mob.item7.entrySet()) {
                    Item47 pot = new Item47();
                    pot.id       = entry.getKey();
                    pot.quantity = entry.getValue();
                    pot.category = 7;

                    int randomQty = Util.random(3, 12);
                    if (pot.id == 472) {
                        pot.quantity = (short) randomQty;
                        Item47 tokenBoster = p.item.getMaterial((short) 473);
                        if (tokenBoster != null) {
                            randomQty = Util.random(5, 20);
                            pot.quantity = (short) (randomQty * 2);
                            p.item.remove(7, tokenBoster.id, 1);
                        }
                    }
                    items.add(pot.id);
                    quantities.add((int) pot.quantity);
                    categories.add((short) 7);
                    p.item.add_item_bag47(7, pot);
                }
            }

            p.item.charInventory(7);
            p.item.charInventory(4);
            p.item.charInventory(3);

            if (!items.isEmpty()) {
                short[] ar_id    = new short[items.size()];
                int[]   ar_quant = new int[items.size()];
                short[] ar_type  = new short[items.size()];
                for (int i = 0; i < ar_id.length; i++) {
                    ar_id[i]    = items.get(i);
                    ar_quant[i] = quantities.get(i);
                    ar_type[i]  = categories.get(i);
                }
                Service.Show_open_box_notice_item(p, "Anda mendapatkan", ar_id, ar_quant, ar_type);
            }
        }
    }

    // ---------------------------------------------------------------
    // Area Skill Boss Premium — menyerang semua job/class player di map
    // Dipanggil dari MobInMap.update() setiap 12 detik
    // ---------------------------------------------------------------

    /**
     * Boss premium menggunakan skill area yang menyerang semua player di map.
     * Paket dikirim dengan format seperti serangan player (opcode 6),
     * menggunakan objectId boss sebagai attacker.
     * Efek visual + debuff disesuaikan per job (clazz) masing-masing player:
     * <ul>
     *   <li>clazz 0 (Warrior)  → skill index 16 — hantaman tanah, stun fisik</li>
     *   <li>clazz 1 (Mage)     → skill index 19 — ledakan api</li>
     *   <li>clazz 2 (Archer)   → skill index 20 — panah beku</li>
     *   <li>clazz 3 (Cleric)   → skill index 17 — aura racun</li>
     *   <li>default             → skill index 15 — petir area</li>
     * </ul>
     */
    public static void bossAreaSkill(GameMap gameMap, MobInMap mob) throws IOException {
        if (gameMap == null || mob == null || mob.isdie) return;

        // Damage area = 40% base damage boss, minimum 1000
        int areaDmg = Math.max(1000, (int) (mob.getBaseDamage() * 0.40));

        // Kumpulkan semua player valid di zone ini dulu
        List<Player> targets = new ArrayList<>();
        for (Player p : new ArrayList<>(gameMap.players)) {
            if (p == null || p.isdie || p.conn == null) continue;
            if (p.map.mapId != mob.map_id || p.map.zoneId != mob.zone_id) continue;
            if (p.getEffectMedal(template.StrucEff.NgocKhaiHoan) != null) continue;
            targets.add(p);
        }
        if (targets.isEmpty()) return;

        // Index 14 = skill area attack yang dipakai semua job player di client
        // Ini satu-satunya index yang render animasi area penuh (bukan sparkle biasa)
        final int SKILL_INDEX = 14;

        for (Player p : targets) {
            try {
                int defReduction = Math.min(p.body.getDefBase() / 8, areaDmg / 2);
                int finalDmg = Math.max(1, areaDmg - defReduction);

                p.hp -= finalDmg;
                if (p.hp < 0) p.hp = 0;

                // Opcode 9: format Fire_Mob (player pakai skill ke target)
                // objectId boss sebagai caster + skill index 14 (area attack)
                // Visual = animasi skill area dari posisi boss ke player
                Message m = new Message(9);
                m.writer().writeShort(mob.objectId);  // objectId boss sebagai caster
                m.writer().writeByte(SKILL_INDEX);     // index 14 = skill area attack
                m.writer().writeByte(1);               // jumlah target
                m.writer().writeShort(p.objectId);     // objectId player sebagai target
                m.writer().writeInt(finalDmg);         // damage
                m.writer().writeInt(p.hp);             // HP player setelah kena
                m.writer().writeByte(1);
                m.writer().writeByte(0);
                m.writer().writeInt(finalDmg);
                m.writer().writeInt(mob.hp);           // HP boss
                m.writer().writeInt(0);                // MP boss
                m.writer().writeByte(11);
                m.writer().writeInt(0);
                // Broadcast dari posisi boss
                MapService.sendMsgPlayerInside(gameMap, mob, m, true);
                m.cleanup();

                // Debuff visual sesuai job
                applyAreaSkillEffect(gameMap, mob, p);

                if (p.hp <= 0) {
                    p.setDie(gameMap, mob);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Kembalikan index skill area sesuai job (clazz) player.
     * Dipakai oleh applyAreaSkillEffect untuk debuff visual.
     */
    private static int getAreaSkillIndex(int clazz) {
        switch (clazz) {
            case 0: return 14; // Warrior
            case 1: return 14; // Mage
            case 2: return 14; // Archer
            case 3: return 14; // Cleric
            default: return 15; // lainnya — petir area  (thunder)
        }
    }

    private static void applyAreaSkillEffect(GameMap gameMap, MobInMap mob, Player p)
            throws IOException {
        final int EFF_DURATION = 3000;

        switch (p.clazz) {
            case 0: {
                // Warrior → stun fisik (tipe 4-6)
                if (!p.isStunes(false)) {
                    MapService.add_eff_stun(gameMap, mob, p, 3, Util.nextInt(4, 6));
                }
                Eff_special_skill.send_eff_Vip(p, 17, EFF_DURATION, false);
                break;
            }
            case 1: {
                // Mage → terbakar api
                Eff_special_skill.send_eff_Vip(p, 13, EFF_DURATION, false);
                p.addEffectMedal(template.StrucEff.BongLua, 0,
                        System.currentTimeMillis() + EFF_DURATION);
                break;
            }
            case 2: {
                // Archer → beku + stun ringan
                Eff_special_skill.send_eff_Vip(p, 12, EFF_DURATION, false);
                if (!p.isStunes(true)) {
                    MapService.add_eff_stun(gameMap, mob, p, 2, 5);
                }
                break;
            }
            case 3: {
                // Cleric → racun
                Eff_special_skill.send_eff_Vip(p, 9, EFF_DURATION, false);
                p.addEffectMedal(template.StrucEff.BongDoc, 0,
                        System.currentTimeMillis() + EFF_DURATION);
                break;
            }
            default: {
                // Lainnya → listrik + stun singkat
                Eff_special_skill.send_eff_Vip(p, 6, EFF_DURATION, false);
                if (!p.isStunes(false)) {
                    MapService.add_eff_stun(gameMap, mob, p, 1, Util.nextInt(4, 7));
                }
                break;
            }
        }
    }

    // ---------------------------------------------------------------
    // Info helper (untuk admin command)
    // ---------------------------------------------------------------

    public static String getInfoAll() {
        if (entrys.isEmpty()) return "Tidak ada Premium Boss yang terdaftar.";
        StringBuilder sb = new StringBuilder();
        for (MobInMap mob : entrys) {
            GameMap[] maps = GameMap.getMapById(mob.map_id);
            String mapName = (maps != null) ? maps[0].name : "Map-" + mob.map_id;

            sb.append("• ").append(mob.template.name)
              .append(" | ").append(mapName)
              .append(" Area ").append(mob.zone_id + 1);

            if (!mob.isdie && mob.is_boss_active) {
                sb.append(" | ✅ HIDUP HP: ").append(_Time.formatNumber(mob.hp));
            } else {
                sb.append(" | ⏳ Respawn: ").append(_Time.getTimeLeft(mob.time_back));
            }
            if (mob.regenHpPct > 0 && mob.regenIntervalSec > 0) {
                sb.append(" | Regen: ").append(mob.regenHpPct).append("%/").append(mob.regenIntervalSec).append("s");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private static void parseItem3(MobInMap mob, String json) {
        JSONArray arr = (JSONArray) JSONValue.parse(json);
        if (arr == null) return;
        for (int i = 0; i < arr.size(); i++) {
            mob.item3.putIfAbsent(i, Short.parseShort(arr.get(i).toString()));
        }
    }

    private static void parseItem4or7(MobInMap mob, String json, boolean isItem7) {
        JSONArray arr = (JSONArray) JSONValue.parse(json);
        if (arr == null) return;
        for (Object o : arr) {
            JSONArray inner = (JSONArray) o;
            if (inner.size() < 2) continue;
            short key = Short.parseShort(inner.get(0).toString());
            short val = Short.parseShort(inner.get(1).toString());
            if (isItem7) mob.item7.putIfAbsent(key, val);
            else         mob.item4.putIfAbsent(key, val);
        }
    }

    private static Item3 buildItem3(ItemTemplate3 tmpl) {
        Item3 eq    = new Item3();
        eq.id       = tmpl.getId();
        eq.name     = tmpl.getName();
        eq.icon     = tmpl.getIcon();
        eq.color    = tmpl.getColor();
        eq.part     = tmpl.getPart();
        eq.type     = tmpl.getType();
        eq.level    = tmpl.getLevel();
        eq.clazz    = tmpl.getClazz();
        eq.tier     = 0;
        eq.tierStar = 0;
        eq.islock   = false;
        eq.op       = tmpl.getOp();
        return eq;
    }
}