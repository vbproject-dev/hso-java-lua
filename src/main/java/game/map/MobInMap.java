package game.map;

import BossHDL.BossManager;

import java.util.ArrayList;
import java.util.List;

import lombok.Setter;
import model.ServerSetting;
import utils._Time;
import client.Player;
import core.Manager;
import core.Service;
import core.Util;
import ev_he.Event_3;
import event_daily.CastleSiegeManager;
import client.io.Message;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import template.Item3;
import template.Item47;
import template.ItemTemplate3;
import template.EffTemplate;
import template.MainObject;
import template.StrucEff;
import event_daily.DailyQuest;

public class MobInMap extends MainObject {

    public final static HashMap<Integer, MobInMap> ENTRYS = new HashMap<>();
    public int time_refresh = 3;
    @Setter
    private boolean isBoss;
    /** Tandai boss ini hanya untuk map premium (drop hanya ke player premium). */
    @Setter
    private boolean isPremiumBoss;
    /** Jika true, broadcast global saat boss muncul / mati. */
    /** Jika true, broadcast global saat boss muncul / mati. */
    public boolean announceKill = false;
    /** Jadwal jam spawn WIB, misal {12,18,21}. Null = respawn bebas. */
    public int[] spawnHours = null;
    /** Jam terakhir boss premium spawn (mode jadwal). -1 = belum pernah. */
    public int lastSpawnedHour = -1;
    public long time_back;
    public final List<Player> list_fight = new ArrayList<>();
    public long time_fight;
    public long time_poison_tick; // timer tick DoT racun
    /** Timer untuk area skill boss premium — AOE ke semua player di map. */
    public long timeAreaSkill = 0;
    public boolean is_boss_active;
    public int timeBossRecive = 1000 * 60 * 60 * 3;
    // TAMBAHAN: dipakai item4 id 57 "Kemarahan markas" (buff kecepatan serangan
    // markas utama selama 1 menit) & id 60 "Kemarahan monster" (buff kekuatan
    // monster tim sendiri selama 1 menit). Dicek langsung di titik pakai
    // (bukan mutasi+revert manual) supaya aman kalau item dipakai berkali-kali
    // sebelum buff sebelumnya habis (cukup memperpanjang, tidak numpuk/dobel).
    public long atkSpeedBuffUntil = 0;
    public long powerBuffUntil = 0;
    /**
     * Sistem regen HP World Boss — konsepnya sama seperti player (lihat Body.getHpRegen()):
     * persen dari MaxHP yang dipulihkan tiap 1 tick, interval tick dikonfigurasi dari DB
     * (kolom regen_hp_pct & regen_interval_sec di tabel boss_event / boss_premium).
     * Default 0 = regen nonaktif (boss biasa/legacy BossManager tidak terpengaruh).
     */
    public int regenHpPct = 0;
    public int regenIntervalSec = 0;
    public long time_regen_hp = 0;
    public final ConcurrentHashMap<String, Long> top_dame = new ConcurrentHashMap<>();
    public final HashMap<Integer, Short> item3 = new HashMap<>();
    public final HashMap<Short, Short> item4 = new HashMap<>();
    public final HashMap<Short, Short> item7 = new HashMap<>();


    public void reset() {
        hp = getMaxHP();
        isdie = false;
        synchronized (list_fight) {
            list_fight.clear();
        }
        synchronized (top_dame) {
            top_dame.clear();
        }
    }

    @Override
    public boolean isBoss() {
        return isBoss;
    }

    public boolean isPremiumBoss() {
        return isPremiumBoss;
    }

    @Override
    public boolean isMobCTruongHouse() {
        return template.mob_id >= 89 && template.mob_id <= 92;
    }

    @Override
    public boolean isMob() {
        return true;
    }

    @Override
    public int getBaseDamage() {
        // Kalau boss_event: dame sudah di-set dari DB → gunakan langsung (hanya ±5% random)
        // Kalau boss biasa (dame==0): pakai formula level multiplier
        // TAMBAHAN: buff dari item4 id 60 "Kemarahan monster" - naikkan damage 50%
        // selama buff aktif (powerBuffUntil > sekarang).
        double powerBuff = (powerBuffUntil > System.currentTimeMillis()) ? 1.5 : 1.0;
        if (this.isBoss && this.dame > 0) {
            int dmob = Util.random((int) (this.dame * 0.95), (int) (this.dame * 1.05));
            dmob = (int) (dmob * powerBuff);
            if (this.color_name != 0 && (this.template.mob_id < 89 || this.template.mob_id > 92)) {
                dmob *= 2;
            }
            return dmob;
        }
        int dmob = Util.random((int) (this.dame * 0.95), (int) (this.dame * 1.05));
        if (this.level > 30 && this.level <= 50) {
            dmob = (dmob * 13) / 10;
        } else if (this.level > 50 && this.level <= 70) {
            dmob = (dmob * 16) / 10;
        } else if (this.level > 70 && this.level <= 100) {
            dmob = (dmob * 19) / 10;
        } else if (this.level > 100 && this.level <= 600) {
            dmob = (dmob * 21) / 10;
        }
        if (this.isBoss) {
            dmob = (int) (dmob * this.level * 0.8);
        }
        dmob = (int) (dmob * powerBuff);
        if (this.color_name != 0 && (this.template.mob_id < 89 || this.template.mob_id > 92)) {
            dmob *= 2;
        }
        return dmob;
    }

    @Override
    public int getMiss() {
        return 500;
    }

    @Override
    public int getDefBase() {
        if (template.mob_id >= 89 && template.mob_id <= 92) {
            return 500 * level;
        }

        return def * level;
    }

    @Override
    public void setDie(GameMap gameMap, MainObject mainAtk) throws IOException {
        hp = 0;
        isdie = true;

        MobInMap mob = this;
        if (mainAtk != null && mainAtk.isPlayer()) {
            if (mainAtk.hieuchien > 0 && Math.abs(mainAtk.level - mob.level) <= 5) {
                mainAtk.hieuchien--;
            }
            if (mob.template.mob_id == 152) {
                CastleSiegeManager.SetOwner((Player) mainAtk);
            }
        }
        boolean check_mob_drops_gem_socket = mob.template.mob_id >= 167 && mob.template.mob_id <= 172;
        if (mob.isBoss()) {
            gameMap.BossDie(mob);
            String p_name = "";
            long top_damage = 0;
            for (java.util.Map.Entry<String, Long> en : mob.top_dame.entrySet()) {
                if (en.getValue() > top_damage) {
                    top_damage = en.getValue();
                    p_name = en.getKey();
                }
            }
            mob.is_boss_active = false;
            String killerName = (mainAtk != null) ? mainAtk.name : (!p_name.isEmpty() ? p_name : "Unknown");
            if (!GameMap.is_map_cant_save_site(mob.map_id)) {
                Manager.gI().chatKTGprocess(killerName + " Telah mengalahkan " + mob.template.name + " total damage tertinggi " + _Time.formatNumber(top_damage));
            }


            if (mainAtk != null && mainAtk.isPlayer()) {
                if (mob.isPremiumBoss()) {
                    // ── Premium World Boss: drop hanya ke player premium aktif ──
                    BossHDL.PremiumBossManager.onPremiumBossDie(gameMap, mob, (Player) mainAtk);
                } else if (mob.template.mob_id == 174) {
                    BossManager.DropItemBossEvent(gameMap, mob, (Player) mainAtk);
                } else {
                    LeaveItemMap.leave_item_boss(gameMap, mob, (Player) mainAtk);

                    GameMap m = GameMap.getMapById(mob.map_id)[mob.zone_id];
                    if (m != null) {
                        for (Player p : m.players) {
                            List<Short> items = new ArrayList<>();
                            List<Integer> quantities = new ArrayList<>();
                            List<Short> catagories = new ArrayList<>();

                            if (mob.top_dame.containsKey(p.name)) {

                                if (!mob.item3.isEmpty()) {

                                    for (short s : mob.item3.values()) {
                                        int percent = Util.nextInt(1, 100);
                                        if (percent < 10) {
                                            ItemTemplate3 temp = ItemTemplate3.item.get(s);
                                            Item3 equip = new Item3();
                                            equip.id = temp.getId();
                                            equip.name = temp.getName();
                                            equip.icon = temp.getIcon();
                                            equip.color = temp.getColor();
                                            equip.part = temp.getPart();
                                            equip.type = temp.getType();
                                            equip.level = temp.getLevel();
                                            equip.clazz = temp.getClazz();
                                            equip.tier = 0;
                                            equip.tierStar = 0;
                                            equip.islock = false;
                                            equip.op = temp.getOp();

                                            items.add(s);
                                            quantities.add(1);
                                            catagories.add((short) 3);

                                            p.item.add_item_bag3(equip);
                                        }
                                    }
                                }

                                if (!mob.item4.isEmpty()) {
                                    for (java.util.Map.Entry<Short, Short> entry : mob.item4.entrySet()) {
                                        Item47 potion = new Item47();
                                        potion.id = entry.getKey();
                                        potion.quantity = entry.getValue();
                                        potion.category = 4;

                                        if (potion.id == -1) {
                                            long vang = Util.random(500_000, 1000_000);
                                            if (map_id == 62) {
                                                vang = Util.random(1000_000, 10_000_000);
                                            }
                                            items.add(potion.id);
                                            quantities.add((int) vang);
                                            catagories.add((short) potion.category);
                                            p.updateGold(vang);
                                        } else if (potion.id == -2) {
                                            long gem = Util.random(1, 100);
                                            items.add(potion.id);
                                            quantities.add((int) gem);
                                            catagories.add((short) potion.category);
                                            p.updateGem(gem);
                                        } else {
                                            items.add(potion.id);
                                            quantities.add((int) potion.quantity);
                                            catagories.add((short) potion.category);
                                            p.item.add_item_bag47(4, potion);
                                        }
                                    }
                                }

                                if (!mob.item7.isEmpty()) {
                                    for (java.util.Map.Entry<Short, Short> entry : mob.item7.entrySet()) {
                                        Item47 potion = new Item47();
                                        potion.id = entry.getKey();
                                        potion.quantity = entry.getValue();
                                        potion.category = 7;
                                        int randomQty = Util.random(3, 12);
                                        if (potion.id == 472) {
                                            potion.quantity = (short) randomQty;
                                            Item47 tokenBoster = p.item.getMaterial((short) 473);
                                            if (tokenBoster != null) {
                                                randomQty = Util.random(5, 20);
                                                potion.quantity = (short) (randomQty * 2);
                                                p.item.remove(7, tokenBoster.id, 1);
                                            }
                                        }

                                        items.add(potion.id);
                                        quantities.add((int) potion.quantity);
                                        catagories.add((short) potion.category);

                                        p.item.add_item_bag47(7, potion);
                                    }
                                }

                                short[] ar_id = new short[items.size()];
                                int[] ar_quant = new int[quantities.size()];
                                short[] ar_type = new short[catagories.size()];
                                for (int i = 0; i < ar_id.length; i++) {
                                    ar_id[i] = items.get(i);
                                    ar_quant[i] = quantities.get(i);
                                    ar_type[i] = catagories.get(i);
                                }

                                p.item.charInventory(7);
                                p.item.charInventory(4);
                                p.item.charInventory(3);

                                Service.Show_open_box_notice_item(p, "Anda mendapatkan", ar_id, ar_quant, ar_type);
                            }



                            if (template != null && !p.quest.getAllQuest().isEmpty()) {
                                p.quest.updateProgress(p, template.mob_id);
                            }
                            // DailyQuest hook — boss kill
                            if (template != null) {
                                try {
                                    DailyQuest.on_mob_killed(p, template.mob_id);
                                    DailyQuest.on_party_kill(p, template.mob_id);
                                } catch (Exception ignored) {}
                            }
                        }
                    }


                }
            }

            // FIX DELAY REWARD BOSS: blok pemberian poin kill (di bawah) sengaja
            // dipindah ke SETELAH reward/drop item (bukan sebelum seperti versi lama).
            // Alasannya: BossKillPoint.addPoint() melakukan round-trip DB sinkron
            // (buka koneksi + INSERT + commit + reload seluruh leaderboard top-20
            // lewat query JOIN + parsing JSON) untuk SETIAP player yang ikut hit boss.
            // Kalau blok ini dijalankan duluan (posisi lama), player harus menunggu
            // N kali round-trip DB berurutan itu kelar dulu -- baru item reward-nya
            // di-drop/dikirim. Makin banyak yang ikut nge-hit boss, makin lama delay-nya.
            // Dropping reward duluan bikin player langsung lihat hasilnya; pencatatan
            // poin & leaderboard (yang sifatnya cuma bookkeeping, tidak perlu instan)
            // boleh nyusul beberapa milidetik kemudian tanpa terasa oleh player.
            if (!GameMap.isBattlefieldMap((byte) mob.map_id)) {
                int participationPoint = mob.isPremiumBoss() ? 2 : 1;
                int lastHitBonusPoint  = mob.isPremiumBoss() ? 3 : 2;

                // Semua player yang ikut hit boss dapat point partisipasi.
                // refreshLeaderboard=false: jangan reload top-20 tiap panggilan,
                // cukup sekali di akhir batch (lihat core.BXH.loadTopBossKill() di bawah).
                java.util.Set<String> hitterNames = new java.util.HashSet<>(mob.top_dame.keySet());
                if (mainAtk != null && mainAtk.isPlayer() && mainAtk.name != null) {
                    hitterNames.add(mainAtk.name);
                }
                for (String hitterName : hitterNames) {
                    BossHDL.BossKillPoint.addPoint(hitterName, participationPoint, false);
                }

                // Player last-hit (penentu kill) dapat point bonus tambahan di luar point partisipasi.
                if (mainAtk != null && mainAtk.isPlayer() && mainAtk.name != null) {
                    BossHDL.BossKillPoint.addPoint(mainAtk.name, lastHitBonusPoint, false);
                }

                // Reload leaderboard SEKALI setelah semua poin batch ini masuk,
                // bukan N kali (N = jumlah hitter) seperti versi lama.
                core.BXH.loadTopBossKill();
            }


        } else {
            mob.time_back = System.currentTimeMillis() + (mob.time_refresh * 1000) - 1000L;
            if (mainAtk.isPlayer()) {

                ServerSetting setting = Manager.gI().setting;
                if (LeaveItemMap.mapDropConfig.containsKey(gameMap.mapId)) {
                    // Map premium (111-114): drop 100% dari konfigurasi DB
                    LeaveItemMap.leaveItemByMapConfig(gameMap, mob, (Player) mainAtk);
                } else {
                    // Map biasa: drop normal
                    if (Util.random(0, 300) < setting.getDropRateEquipment()) {
                        LeaveItemMap.leave_item_3(gameMap, mob, (Player) mainAtk);
                    }
                    if (Util.random(0, 300) < setting.getDropRatePotion()) {
                        LeaveItemMap.leave_item_4(gameMap, mob, (Player) mainAtk);
                    }
                    if (Util.random(0, 300) < setting.getDropRateMaterial()) {
                        LeaveItemMap.leaveItemUpgrade(gameMap, mob, (Player) mainAtk);
                    }
                    if (Util.random(0, 300) < setting.getDropRateGold()) {
                        LeaveItemMap.leave_gold(gameMap, mob, (Player) mainAtk);
                    }
                    if (Util.random(0, 300) < setting.getDropRateMaterial()) {
                        LeaveItemMap.leaveCraftMaterial(gameMap, mob, (Player) mainAtk);
                    }
                    if (Util.random(0, 300) < setting.getDropRateEvent()) {
                        LeaveItemMap.leave_item_event(gameMap, mob, (Player) mainAtk);
                    }
                }
            }

            if (check_mob_drops_gem_socket) {
                LeaveItemMap.leave_material_ngockham(gameMap, mob, (Player) mainAtk);
            }


            if (mob.color_name != 0) {
                gameMap.num_mob_super--;
            }

            Player localPlayer = ((Player) mainAtk);
            if (template != null && !localPlayer.quest.getAllQuest().isEmpty()) {
                localPlayer.quest.updateProgress(localPlayer, template.mob_id);
            }
            // DailyQuest hook — mob kill
            if (template != null) {
                try {
                    DailyQuest.on_mob_killed(localPlayer, template.mob_id);
                    DailyQuest.on_party_kill(localPlayer, template.mob_id);
                } catch (Exception ignored) {}
            }

        }


    }

    @Override
    public void update(GameMap gameMap) {
        try {
            if (this.isdie && this.ishs && this.time_back < System.currentTimeMillis()) {
                this.isdie = false;
                this.reset();
                this.hp = this.getMaxHP();
                if (this.isBoss()) {
                    this.color_name = 3;
                } else if (5 > Util.random(200) && gameMap.num_mob_super < 2 && this.level > 50) {
                    this.color_name = (new byte[]{1, 2, 4, 5})[Util.random(4)];
                    gameMap.num_mob_super++;
                } else {
                    this.color_name = 0;
                }
                for (int j = 0; j < gameMap.players.size(); j++) {
                    Player pp = gameMap.players.get(j);
                    if ((Math.abs(pp.x - this.x) < 200) && (Math.abs(pp.y - this.y) < 200)) {
                        if (!pp.other_mob_inside.containsKey(this.objectId)) {
                            pp.other_mob_inside.put(this.objectId, true);
                        }
                        if (pp.other_mob_inside.get(this.objectId)) {
                            Message mm = new Message(4);
                            mm.writer().writeByte(1);
                            mm.writer().writeShort(this.template.mob_id);
                            mm.writer().writeShort(this.objectId);
                            mm.writer().writeShort(this.x);
                            mm.writer().writeShort(this.y);
                            mm.writer().writeByte(-1);
                            pp.conn.addmsg(mm);
                            mm.cleanup();
                            pp.other_mob_inside.replace(this.objectId, true, false);
                        } else {
                            Service.mob_in4(pp, this.objectId);
                        }
                    }
                }
            } else if (!this.isdie && this.isATK && this.time_fight < System.currentTimeMillis()) {
                if ((this.template.mob_id == 151 || this.template.mob_id == 152 || this.template.mob_id == 154)) {
                    for (Player p0 : this.list_fight) {
                        if (p0 != null && !p0.isdie && p0.map.mapId == this.map_id && p0.map.zoneId == this.zone_id
                                && Math.abs(this.x - p0.x) < 200 && Math.abs(this.y - p0.y) < 200) {
                            MainObject.attack(gameMap, this, p0, 0, null, 2);
                        }
                        // MapService.mob_fire(this, mob, p0);
                    }
                    this.time_fight = System.currentTimeMillis() + 3500L;
                } else if (!this.list_fight.isEmpty()) {
                    Player p0 = this.list_fight.get(Util.random(this.list_fight.size()));
                    if (p0 != null && !p0.isdie && p0.map.mapId == this.map_id && p0.map.zoneId == this.zone_id) {
                        if (Math.abs(this.x - p0.x) < 200 && Math.abs(this.y - p0.y) < 200) {
                            if (this.time_fight < System.currentTimeMillis()) {

                                // TAMBAHAN: buff dari item4 id 57 "Kemarahan markas" -
                                // jeda serangan dipercepat jadi setengahnya (600ms,
                                // dari normalnya 1200ms) selama buff aktif.
                                long atkInterval = (atkSpeedBuffUntil > System.currentTimeMillis()) ? 600L : 1200L;
                                this.time_fight = System.currentTimeMillis() + atkInterval;
                                MainObject.attack(gameMap, this, p0, 0, null, 2);
                                // MapService.mob_fire(this, mob, p0);
                            }
                        } else {
                            this.list_fight.remove(p0);
                            //
                            Message m = new Message(10);
                            m.writer().writeByte(0);
                            m.writer().writeShort(this.objectId);
                            MapService.sendMsgPlayerInside(gameMap, p0, m, true);
                            m.cleanup();
                        }
                    }
                    if (p0.isdie) {
                        this.list_fight.remove(p0);
                        //
                        Message m = new Message(10);
                        m.writer().writeByte(0);
                        m.writer().writeShort(this.objectId);
                        MapService.sendMsgPlayerInside(gameMap, p0, m, true);
                        m.cleanup();
                    }
                    if (this.list_fight.contains(p0)
                            && !(p0.map.mapId == this.map_id && p0.map.zoneId == this.zone_id)) {
                        this.list_fight.remove(p0);
                    }
                }
            }
        } catch (Exception e) {
        }

        // FIX: Tick DoT racun (BongDoc) — kurangi HP mob tiap 1 detik selama efek aktif
        try {
            EffTemplate dotEff = getEffectDefault(StrucEff.BongDoc);
            if (dotEff != null && !this.isdie) {
                long now = System.currentTimeMillis();
                if (time_poison_tick < now) {
                    time_poison_tick = now + 1000L; // tick tiap 1 detik
                    int dotDmg = Math.max(1, dotEff.param);
                    this.hp -= dotDmg;
                    if (this.hp <= 0) {
                        try {
                            this.setDie(gameMap, null);
                        } catch (Exception ignored) {
                            this.hp = 1;
                        }
                    }
                }
                // Hapus efek jika sudah expired
                if (dotEff.time <= now) {
                    effectDefault.remove(dotEff);
                }
            }
        } catch (Exception ignored) {}

        // ── World Boss: HP auto-regen — konsepnya sama seperti player ──────
        // (lihat Body.getHpRegen()/update() di client.Body): persen MaxHP yang
        // dipulihkan tiap 1 tick. Bedanya, untuk boss persen & interval tick
        // dikonfigurasi per-baris dari DB (boss_event.regen_hp_pct / regen_interval_sec,
        // boss_premium.regen_hp_pct / regen_interval_sec) bukan dari stat equipment.
        // regenHpPct/regenIntervalSec default 0 → regen nonaktif (aman utk boss lama).
        try {
            if (this.isBoss() && this.is_boss_active && !this.isdie
                    && this.regenHpPct > 0 && this.regenIntervalSec > 0) {
                long now = System.currentTimeMillis();
                if (this.time_regen_hp < now) {
                    this.time_regen_hp = now + (this.regenIntervalSec * 1000L);
                    int maxHp = this.getMaxHP();
                    if (this.hp < maxHp) {
                        long regenAmount = ((long) maxHp * this.regenHpPct) / 100L;
                        if (regenAmount < 1) {
                            regenAmount = 1; // minimal 1 poin walau persen kecil dari MaxHP besar
                        }
                        long newHp = this.hp + regenAmount;
                        this.hp = (int) Math.min(maxHp, newHp);
                    }
                }
            }
        } catch (Exception ignored) {}

        // ── Premium Boss: area skill periodik ke semua player di map ──────
        try {
            if (isPremiumBoss() && !this.isdie && this.is_boss_active) {
                long now = System.currentTimeMillis();
                if (timeAreaSkill < now) {
                    // Interval skill area: 12 detik
                    timeAreaSkill = now + 12_000L;
                    BossHDL.PremiumBossManager.bossAreaSkill(gameMap, this);
                }
            }
        } catch (Exception ignored) {}
    }
}