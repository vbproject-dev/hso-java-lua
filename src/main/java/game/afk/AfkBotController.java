package game.afk;

import client.Player;
import client.io.Message;
import core.Util;
import game.map.GameMap;
import game.map.ItemMap;
import game.map.MapService;
import game.map.MobInMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import template.LvSkill;
import template.MainObject;
import game.afk.AfkLogger;

import java.io.IOException;

/**
 * AFK Bot Controller untuk arsitektur src2 (client.Player).
 *
 * Navigasi menggunakan gerakan langsung (tanpa pathfinding A*),
 * mengikuti pola PlayerClone dari ada_bot.
 *
 * State machine:
 * IDLE   → enemy detected          → CHASE
 * IDLE   → tidak ada musuh (wander)→ bergerak random di area spawn
 * IDLE   → setelah PATROL_DELAY    → PATROL
 * PATROL → enemy terdeteksi        → CHASE
 * PATROL → semua waypoint selesai  → IDLE
 * CHASE  → dalam jangkauan         → ATTACK
 * CHASE  → target hilang/jauh      → RETURN
 * ATTACK → target keluar jangkauan → CHASE
 * ATTACK → target mati/pergi       → RETURN
 * RETURN → sampai spawn            → IDLE
 */
@Slf4j
@Getter
public class AfkBotController {

    // ─── Konstanta ──────────────────────────────────────────────────────────
    private static final long IDLE_SCAN_DELAY     = 1500L;
    private static final int  GEM_COST_ACTIVATE   = 500;       // biaya aktivasi awal mode AFK
    private static final int  GEM_COST_PER_HOUR   = 500;        // gems dipotong tiap 1 jam
    private static final long GEM_DEDUCT_INTERVAL = 3_600_000L; // 1 jam dalam ms
    private static final long PATROL_DELAY      = 8000L;  // tunggu 8 detik idle sebelum patrol
    private static final long WANDER_DELAY_MIN  = 2000L;  // jeda minimum wander (sama seperti PlayerClone)
    private static final long WANDER_DELAY_MAX  = 5000L;  // jeda maksimum wander
    private static final int  WANDER_RADIUS     = 150;    // radius area wander saat idle (~deteksi)
    private static final int  DETECTION_RANGE   = 200;    // pixel — jarak deteksi musuh
    private static final int  ATTACK_RANGE      = 120;    // pixel — jarak serang
    private static final int  PATROL_RADIUS     = 100;    // radius patrol dari spawn
    private static final long CHASE_MOVE_DELAY  = 600L;   // jeda gerak saat mengejar
    private static final long RETURN_MOVE_DELAY = 500L;   // jeda gerak saat kembali
    private static final int  ARRIVE_THRESHOLD  = 30;     // dianggap sampai jika dalam 30px
    private static final long ADMIN_CHECK_INTERVAL = 10_000L; // cek is_active DB tiap 10 detik

    // ─── Referensi ──────────────────────────────────────────────────────────
    private final Player player;
    private final short  spawnX;
    private final short  spawnY;

    // Waypoint patrol kotak (NE → SE → SW → NW)
    private final short[][] patrolWaypoints;
    private int patrolIndex   = 0;
    private int patrolVisited = 0;

    // ─── State runtime ──────────────────────────────────────────────────────
    private AfkBotState state         = AfkBotState.IDLE;
    private MobInMap    target;
    private long        lastScanTime;
    private long        idleSinceTime;
    private long        lastWanderTime;   // seperti time_move di PlayerClone
    private long        lastChaseMove;
    private long        lastReturnMove;
    private long        reviveTime;       // timestamp kapan bot harus di-revive (0 = belum dijadwal)
    private long        lastPickupTime;   // throttle pickup agar tidak loop setiap tick
    private long        nextGemDeductTime; // waktu berikutnya gems dipotong (0 = belum dijadwal)
    private long        nextAdminCheckTime; // waktu berikutnya cek is_active dari DB (panel stop)
    private final long  afkStartTime;     // waktu bot pertama kali diaktifkan
    private long totalExpGained = 0L;   // akumulasi exp selama AFK
    private long expSnapshot    = 0L;   // nilai player.exp saat snapshot terakhir
    private short levelSnapshot = 0;    // level saat snapshot terakhir

    // ─── Konstruktor ────────────────────────────────────────────────────────
    public AfkBotController(Player player) {
        this.player        = player;
        this.spawnX        = player.x;
        this.spawnY        = player.y;
        this.idleSinceTime    = System.currentTimeMillis();
        this.nextGemDeductTime = System.currentTimeMillis() + GEM_DEDUCT_INTERVAL; // potong pertama setelah 1 jam
        this.nextAdminCheckTime = System.currentTimeMillis() + ADMIN_CHECK_INTERVAL;
        this.afkStartTime      = System.currentTimeMillis();
        this.expSnapshot   = player.exp;
        this.levelSnapshot = player.level;

        // ── Potong biaya aktivasi 5000 gems di awal ──────────────────────
        player.deductGemSilent(GEM_COST_ACTIVATE);
        log.info("[AfkBot] Aktivasi AFK: player={} potong {} gems, sisa={}",
                player.name, GEM_COST_ACTIVATE, player.getGem());

        AfkLogger.start(player);

        int r = PATROL_RADIUS;
        this.patrolWaypoints = new short[][]{
            {(short)(spawnX + r), (short)(spawnY - r)}, // [0] NE
            {(short)(spawnX + r), (short)(spawnY + r)}, // [1] SE
            {(short)(spawnX - r), (short)(spawnY + r)}, // [2] SW
            {(short)(spawnX - r), (short)(spawnY - r)}, // [3] NW
        };
    }

    // ─── Main tick (dipanggil dari GameMap.update()) ─────────────────────
    public void update() {
        try {
            if (player.isdie) return;

            // ── Cek tagihan gems setiap 1 jam ──────────────────────────────
            checkGemDeduction();
            // Jika checkGemDeduction() menghentikan bot (gems habis), hentikan tick
            if (!player.modeBot) return;

            // ── Cek apakah di-stop dari panel admin (setiap 10 detik) ──────
            if (checkAdminStop()) return;

            snapshotExp();

            switch (state) {
                case IDLE   -> onIdle();
                case PATROL -> onPatrol();
                case CHASE  -> onChase();
                case ATTACK -> onAttack();
                case RETURN -> onReturn();
            }
        } catch (Exception e) {
            log.warn("[AfkBot] Error update player {}: {}", player.name, e.getMessage());
        }
    }

    // ─── State: IDLE ────────────────────────────────────────────────────────
    private void onIdle() throws IOException {
        if (distanceFromSpawn(player.x, player.y) > DETECTION_RANGE) {
            transitionTo(AfkBotState.RETURN);
            return;
        }

        long now = System.currentTimeMillis();

        // Scan musuh setiap IDLE_SCAN_DELAY ms
        if (now - lastScanTime >= IDLE_SCAN_DELAY) {
            lastScanTime = now;
            target = findNearestMonster();
            if (target != null) {
                transitionTo(AfkBotState.CHASE);
                return;
            }
        }

        // Mulai patrol setelah cukup lama idle
        if (now - idleSinceTime >= PATROL_DELAY) {
            resetPatrol();
            transitionTo(AfkBotState.PATROL);
            return;
        }

        // ── Wander random (meniru PlayerClone.update()) ──────────────────
        // Bergerak acak tiap 2–5 detik agar karakter tidak diam di tempat
        if (now >= lastWanderTime) {
            lastWanderTime = now + Util.random((int) WANDER_DELAY_MIN, (int) WANDER_DELAY_MAX);
            player.x = (short) Util.random(spawnX - WANDER_RADIUS, spawnX + WANDER_RADIUS);
            player.y = (short) Util.random(spawnY - WANDER_RADIUS, spawnY + WANDER_RADIUS);
            broadcastMove();
        }
    }

    // ─── State: PATROL ──────────────────────────────────────────────────────
    private void onPatrol() throws IOException {
        long now = System.currentTimeMillis();

        // Tetap scan musuh saat patrol
        if (now - lastScanTime >= IDLE_SCAN_DELAY) {
            lastScanTime = now;
            target = findNearestMonster();
            if (target != null) {
                transitionTo(AfkBotState.CHASE);
                return;
            }
        }

        // Patrol selesai → kembali IDLE
        if (patrolVisited >= patrolWaypoints.length) {
            idleSinceTime = System.currentTimeMillis();
            transitionTo(AfkBotState.IDLE);
            return;
        }

        // Jalan ke waypoint saat ini tiap WANDER_DELAY_MIN ms
        if (now - lastWanderTime >= WANDER_DELAY_MIN) {
            lastWanderTime = now;
            short wx = patrolWaypoints[patrolIndex][0];
            short wy = patrolWaypoints[patrolIndex][1];
            stepToward(wx, wy, 0);

            // Kalau sudah dekat waypoint, maju ke berikutnya
            if (distance(player.x, player.y, wx, wy) <= ARRIVE_THRESHOLD) {
                patrolVisited++;
                patrolIndex = (patrolIndex + 1) % patrolWaypoints.length;
            }
        }
    }

    // ─── State: CHASE ───────────────────────────────────────────────────────
    private void onChase() throws IOException {
        // Jika target tidak valid, coba cari target baru dulu sebelum return ke spawn
        if (!isValidTarget()) {
            target = findNearestMonster();
            if (target == null) {
                returnToSpawn();
                return;
            }
        }
        if (distanceFromSpawn(player.x, player.y) > DETECTION_RANGE * 2) {
            returnToSpawn();
            return;
        }

        int dist = distance(player.x, player.y, target.x, target.y);
        if (dist <= ATTACK_RANGE) {
            transitionTo(AfkBotState.ATTACK);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastChaseMove >= CHASE_MOVE_DELAY) {
            lastChaseMove = now;
            stepToward(target.x, target.y, ATTACK_RANGE - 10);
        }
    }

    // ─── State: ATTACK ──────────────────────────────────────────────────────
    private void onAttack() throws IOException {
        // Jika target tidak valid (mati/pindah map), cari target baru di sekitar
        if (!isValidTarget()) {
            target = findNearestMonster();
            if (target == null) {
                // Tidak ada target baru → kembali IDLE (bukan return to spawn)
                reset();
                return;
            }
            // Ada target baru → langsung chase
            transitionTo(AfkBotState.CHASE);
            return;
        }

        int dist = distance(player.x, player.y, target.x, target.y);
        if (dist > ATTACK_RANGE) {
            transitionTo(AfkBotState.CHASE);
            return;
        }

        performBestSkill();

        // Auto-pickup: cek item drop setiap 2 detik (tidak setiap tick)
        long nowPick = System.currentTimeMillis();
        if (nowPick - lastPickupTime >= 2000L) {
            lastPickupTime = nowPick;
            pickupNearbyItems();
        }
    }

    // ─── State: RETURN ──────────────────────────────────────────────────────
    private void onReturn() throws IOException {
        if (distanceFromSpawn(player.x, player.y) <= ARRIVE_THRESHOLD) {
            player.x = spawnX;
            player.y = spawnY;
            broadcastMove();
            reset();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastReturnMove >= RETURN_MOVE_DELAY) {
            lastReturnMove = now;
            stepToward(spawnX, spawnY, 0);
        }
    }

    // ─── Gerakan langsung (tanpa pathfinding A*) ─────────────────────────
    /**
     * Bergerak selangkah ke arah (tx, ty). Berhenti jika dalam stopRadius pixel.
     * Meniru pola PlayerClone: langsung update x,y lalu broadcast.
     */
    private void stepToward(short tx, short ty, int stopRadius) throws IOException {
        int dist = distance(player.x, player.y, tx, ty);
        if (dist <= stopRadius) return;

        int step = Math.min(48, dist - stopRadius);
        double angle = Math.atan2(ty - player.y, tx - player.x);

        player.x = (short)(player.x + (int)(Math.cos(angle) * step));
        player.y = (short)(player.y + (int)(Math.sin(angle) * step));
        broadcastMove();
    }

    private void broadcastMove() throws IOException {
        GameMap map = player.map;
        if (map == null) return;

        Message m = new Message(4);
        m.writer().writeByte(0);
        m.writer().writeShort(0);
        m.writer().writeShort(player.objectId);
        m.writer().writeShort(player.x);
        m.writer().writeShort(player.y);
        m.writer().writeByte(-1);
        // Gunakan sendMsgPlayerInsideBot — skip player tanpa koneksi aktif (lebih efisien untuk bot)
        MapService.sendMsgPlayerInsideBot(map, player, m);
        m.cleanup();
    }

    // ─── Target finding ─────────────────────────────────────────────────────
    private MobInMap findNearestMonster() {
        GameMap map = player.map;
        if (map == null || map.mobs == null) {
            log.debug("[AfkBot] findNearestMonster: map={}, mobs={}",
                map == null ? "null" : map.mapId,
                map == null || map.mobs == null ? "null" : map.mobs.length);
            return null;
        }

        log.debug("[AfkBot] Scan — player={} map={} mobs={} spawnX={} spawnY={}",
            player.name, map.mapId, map.mobs.length, spawnX, spawnY);

        MobInMap nearest = null;
        int minDist = Integer.MAX_VALUE;

        // Scan mob biasa
        for (MobInMap mob : map.mobs) {
            if (mob == null || mob.isdie || mob.hp <= 0) continue;
            int dist = distance(player.x, player.y, mob.x, mob.y);
            if (dist > DETECTION_RANGE) continue;
            if (dist < minDist) {
                minDist = dist;
                nearest = mob;
            }
        }

        // Scan boss juga — selalu, bukan hanya jika nearest == null
        if (map.Boss_entrys != null) {
            for (MobInMap boss : map.Boss_entrys) {
                if (boss == null || boss.isdie || boss.hp <= 0) continue;
                int dist = distance(player.x, player.y, boss.x, boss.y);
                if (dist > DETECTION_RANGE) continue;
                if (dist < minDist) {
                    minDist = dist;
                    nearest = boss;
                }
            }
        }

        if (nearest != null) {
            log.debug("[AfkBot] Target found: {} (objectId={}) dist={}", nearest.name, nearest.objectId, minDist);
        }

        return nearest;
    }

    /**
     * Gunakan semua skill yang cooldown-nya sudah selesai — mirip player aktif hunt.
     *
     * Urutan validasi sama dengan MapService.use_skill():
     *   weapon → level → MP → stun → cooldown → eksekusi
     *
     * Skill type=0 (damage) → MainObject.attack() per target
     * Skill type=1 (buff)   → MapService.add_eff_skill() dengan null-safe conn guard
     */
    private void performBestSkill() throws IOException {
        if (player.skills == null || player.skillPoint == null || target == null) return;

        // Cek weapon (sama seperti use_skill) — bot harus punya senjata
        if (player.item == null || player.item.wear == null || player.item.wear[0] == null) return;

        // Cek stun — bot tidak bisa attack jika sedang di-stun
        if (player.isStunes(true)) return;

        // Auto-refill MP jika hampir habis
        if (player.mp < player.getMaxMP() * 0.2) {
            player.mp = player.getMaxMP();
        }

        GameMap map = player.map;
        if (map == null) return;

        long now = System.currentTimeMillis();

        // ── Loop semua skill, langsung pakai yang sudah ready ─────────────
        for (int idx = 0; idx < player.skills.length; idx++) {
            if (player.skills[idx] == null) continue;
            if (idx >= player.skillPoint.length) break;

            int lvPoint = player.skillPoint[idx] & 0xFF;
            if (lvPoint <= 0) continue;

            int lvPlus      = player.getSkillPointPlus(idx);
            int effectiveLv = Math.min(Math.max(lvPoint + lvPlus, 1), player.skills[idx].mLvSkill.length);
            LvSkill lv      = player.skills[idx].mLvSkill[effectiveLv - 1];
            if (lv == null) continue;
            if (lv.LvRe > player.level) continue;             // level belum cukup
            if (player.time_delay_skill[idx] > now) continue; // masih cooldown
            if (player.mp < lv.mpLost) continue;              // MP tidak cukup

            // ── Kurangi MP dan set cooldown (identik MapService.use_skill) ──
            player.mp -= lv.mpLost;
            if (player.mp < 0) player.mp = 0;
            player.time_delay_skill[idx] = (long)(now + lv.delay * 0.97);

            byte skillType = player.skills[idx].type;

            if (skillType == 1) {
                // ── Buff skill: panggil add_eff_skill (null-safe untuk bot) ──
                try {
                    MapService.add_eff_skill(map, player, null, (byte) idx);
                    log.debug("[AfkBot] buff skill={}", idx);
                } catch (Exception e) {
                    log.debug("[AfkBot] buff skill={} error: {}", idx, e.getMessage());
                }
                continue; // buff tidak perlu attack target
            }

            // ── Damage skill: tentukan tipe (identik MapService.use_skill) ──
            byte type = 0;
            if (idx == 2 || idx == 4 || idx == 6 || idx == 8 || idx == 19 || idx == 20) {
                type = (byte)((idx == 20 && (player.clazz == 2 || player.clazz == 1))
                        || (idx == 19 && (player.clazz == 0 || player.clazz == 3)) ? 0 : 1);
            }
            if (idx == 0) type = 2;

            // ── Eksekusi: single atau area ─────────────────────────────────
            int maxTargets = (lv.nTarget & 0xFF);
            if (maxTargets < 1) maxTargets = 1;

            try {
                if (maxTargets == 1) {
                    // Single target
                    MainObject.attack(map, player, target, idx, lv, type);
                    log.debug("[AfkBot] skill={} single target={}", idx, target.name);

                } else {
                    // Area: kumpul mob dalam jangkauan skill
                    // Prioritas: range_lan → skill.range → ATTACK_RANGE fallback
                    int skillRange = (lv.range_lan > 0) ? lv.range_lan
                            : (player.skills[idx].range > 0) ? player.skills[idx].range
                            : ATTACK_RANGE;

                    java.util.List<MobInMap> targets = new java.util.ArrayList<>();
                    targets.add(target);

                    if (map.mobs != null) {
                        for (MobInMap mob : map.mobs) {
                            if (targets.size() >= maxTargets) break;
                            if (mob == null || mob == target || mob.isdie || mob.hp <= 0) continue;
                            if (distance(player.x, player.y, mob.x, mob.y) > skillRange) continue;
                            targets.add(mob);
                        }
                    }
                    if (targets.size() < maxTargets && map.Boss_entrys != null) {
                        for (MobInMap boss : map.Boss_entrys) {
                            if (targets.size() >= maxTargets) break;
                            if (boss == null || boss == target || boss.isdie || boss.hp <= 0) continue;
                            if (distance(player.x, player.y, boss.x, boss.y) > skillRange) continue;
                            targets.add(boss);
                        }
                    }
                    for (MobInMap t : targets) {
                        MainObject.attack(map, player, t, idx, lv, type);
                    }
                    log.debug("[AfkBot] skill={} area range={} hit={}/{}", idx, skillRange, targets.size(), maxTargets);
                }
            } catch (Exception e) {
                log.warn("[AfkBot] skill={} error: {}", idx, e.getMessage());
            }
        } // end for each skill
    }

    // ─── Auto-pickup item drop ───────────────────────────────────────────────
    /**
     * Ambil semua item di map yang boleh diambil bot.
     * ItemMap tidak menyimpan koordinat, jadi semua item eligible langsung diambil.
     * Dipanggil setiap kali bot menyerang.
     */
    private void pickupNearbyItems() {
        GameMap map = player.map;
        if (map == null || map.itemDrop == null) return;

        for (int i = 0; i < map.itemDrop.length; i++) {
            ItemMap item = map.itemDrop[i];
            if (item == null) continue;
            // Skip item milik player lain
            if (item.idmaster != -1 && item.idmaster != player.objectId) continue;
            try {
                map.pickItemForBot(player, i);
                log.debug("[AfkBot] Pickup item idx={} player={}", i, player.name);
            } catch (Exception e) {
                log.debug("[AfkBot] Pickup gagal idx={}: {}", i, e.getMessage());
            }
        }
    }

    // ─── Gem deduction ──────────────────────────────────────────────────────
    /**
     * Potong 3000 gems setiap 1 jam selama bot aktif.
     * Jika gems tidak cukup → matikan bot (kembalikan ke state offline normal).
     */
    private void checkGemDeduction() {
        long now = System.currentTimeMillis();
        if (now < nextGemDeductTime) return;

        nextGemDeductTime = now + GEM_DEDUCT_INTERVAL;

        if (player.getGem() < GEM_COST_PER_HOUR) {
            // Gems habis → matikan bot
            log.info("[AfkBot] Gems habis untuk player={}, bot dimatikan.", player.name);
            stopBot("Gems tidak cukup! Mode AFK dimatikan. Isi ulang gems dan aktifkan kembali.");
            return;
        }

        // Potong gems langsung ke field (bypass conn karena bot offline)
        snapshotExp();   // update akumulasi exp sebelum deduct — satu-satunya snapshot di tick ini
        player.deductGemSilent(GEM_COST_PER_HOUR);
        AfkLogger.updateDeduct(player, GEM_COST_PER_HOUR, totalExpGained);
    }

    /**
     * Hentikan bot: matikan modeBot, lepas AfkBotController, restore posisi.
     * Player akan tetap di map tapi tidak lagi auto-hunt.
     */
    private void stopBot(String reason) {
        snapshotExp();
        AfkLogger.stop(player, reason, totalExpGained);
        player.modeBot = false;
        player.afkBot  = null;
        // Keluarkan karakter dari map agar tidak ghost di game
        if (player.map != null) {
            MapService.leave(player.map, player);
        }
        player.flush();
        log.info("[AfkBot] Bot stopped: player={} reason={}", player.name, reason);
    }

    /**
     * Hentikan bot secara manual oleh player via panel menu.
     * Dipanggil dari MenuManager saat player klik "Hentikan AFK".
     * Berbeda dengan stopBot() (private), method ini public dan
     * menggunakan reason "manual" agar tercatat di DB.
     */
    public void stopBotManual() {
        stopBot("manual");
    }

    /**
     * Cek setiap ADMIN_CHECK_INTERVAL ms apakah is_active di DB sudah di-set 0
     * oleh panel admin. Jika iya, hentikan bot di Java tanpa update DB lagi
     * (DB sudah diupdate panel).
     *
     * @return true jika bot dihentikan (caller harus return dari update())
     */
    private boolean checkAdminStop() {
        long now = System.currentTimeMillis();
        if (now < nextAdminCheckTime) return false;
        nextAdminCheckTime = now + ADMIN_CHECK_INTERVAL;

        try (java.sql.Connection c = core.SQL.gI().getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(
                 "SELECT `is_active` FROM `afk_log` WHERE `player_name` = ? AND `id` = " +
                 "(SELECT MAX(`id`) FROM `afk_log` WHERE `player_name` = ?)");
             ) {
            ps.setString(1, player.name);
            ps.setString(2, player.name);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt("is_active") == 0) {
                    log.info("[AfkBot] Dihentikan via panel admin: player={}", player.name);
                    snapshotExp();
                    player.modeBot = false;
                    player.afkBot  = null;
                    // Keluarkan karakter dari map agar tidak ghost di game
                    if (player.map != null) {
                        MapService.leave(player.map, player);
                    }
                    player.flush();
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("[AfkBot] checkAdminStop error: {}", e.getMessage());
        }
        return false;
    }

    // ─── Guards & utils ─────────────────────────────────────────────────────
    private boolean isValidTarget() {
        if (target == null) return false;
        if (target.isdie || target.hp <= 0) return false;
        // Cukup cek isdie/hp — server sudah set isdie=true saat mob mati.
        // Tidak perlu loop seluruh array mobs (O(n) per tick) untuk validasi.
        return player.map != null;
    }

    private int distanceFromSpawn(short px, short py) {
        return distance(px, py, spawnX, spawnY);
    }

    private static int distance(short x1, short y1, short x2, short y2) {
        int dx = x1 - x2, dy = y1 - y2;
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    private static int distance(short x1, short y1, int x2, int y2) {
        int dx = x1 - x2, dy = y1 - y2;
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    // ─── Transitions ────────────────────────────────────────────────────────
    private void returnToSpawn() {
        target = null;
        transitionTo(AfkBotState.RETURN);
    }

    private void reset() {
        target         = null;
        lastWanderTime = 0;
        lastChaseMove  = 0;
        lastReturnMove = 0;
        lastPickupTime = 0;
        idleSinceTime  = System.currentTimeMillis();
        transitionTo(AfkBotState.IDLE);
    }

    private void resetPatrol() {
        patrolIndex   = 0;
        patrolVisited = 0;
    }

    private void transitionTo(AfkBotState next) {
        log.debug("[AfkBot] {} → {}", state, next);
        this.state = next;
    }

    /**
     * Reset bot ke state IDLE — dipanggil setelah auto-revive.
     */
    public void resetToIdle() {
        target          = null;
        lastScanTime    = 0;
        lastWanderTime  = 0;
        lastChaseMove   = 0;
        lastReturnMove  = 0;
        reviveTime      = 0;
        idleSinceTime   = System.currentTimeMillis();
        resetPatrol();
        state = AfkBotState.IDLE;
        log.debug("[AfkBot] resetToIdle untuk player {}", player.name);
    }

    /** Jadwalkan revive pada timestamp tertentu. */
    public void scheduleRevive(long timestamp) {
        this.reviveTime = timestamp;
    }

    /** Waktu revive yang dijadwalkan (0 = belum dijadwal). */
    public long getReviveTime() {
        return reviveTime;
    }

    // ─── Info waktu AFK ─────────────────────────────────────────────────────

    /**
     * Durasi AFK yang sudah berjalan sejak bot diaktifkan.
     * Format: "2 jam 15 menit" / "45 menit" / "< 1 menit"
     */
    public String getElapsedFormatted() {
        long elapsed      = System.currentTimeMillis() - afkStartTime;
        long totalMinutes = elapsed / 60_000L;
        long hours        = totalMinutes / 60;
        long minutes      = totalMinutes % 60;
        if (totalMinutes < 1)  return "< 1 menit";
        if (hours > 0)         return hours + " jam " + minutes + " menit";
        return minutes + " menit";
    }

    /**
     * Sisa menit sebelum gems dipotong berikutnya.
     */
    public long getMinutesUntilNextDeduct() {
        long remaining = nextGemDeductTime - System.currentTimeMillis();
        return Math.max(0, remaining / 60_000L);
    }
    /**
     * Update snapshot exp — harus dipanggil tiap kali bot menyerang
     * agar delta exp terakumulasi dengan benar meski player naik level
     * (karena exp direset ke 0 saat level up).
     *
     * Dipanggil dari update() setiap tick atau minimal tiap checkGemDeduction().
     */
    private void snapshotExp() {
        // Jika level belum naik: delta = exp_sekarang - exp_snapshot
        if (player.level == levelSnapshot) {
            long delta = player.exp - expSnapshot;
            if (delta > 0) {
                totalExpGained += delta;
            }
        } else {
            // Level naik satu atau lebih:
            // exp_snapshot sudah di level lama yang direset → anggap gained = exp sekarang
            // (ini estimasi konservatif, exp dari kill terakhir di level lama tidak terhitung)
            totalExpGained += player.exp;
            levelSnapshot = player.level;
        }
        expSnapshot = player.exp;
    }
 
    /** Getter untuk panel / AfkLogger. */
    public long getTotalExpGained() {
        return totalExpGained;
    }
}