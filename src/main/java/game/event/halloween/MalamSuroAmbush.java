package game.event.halloween;

import client.Player;
import core.Manager;
import core.Service;
import core.Util;
import game.event.GameEvent;
import game.event.GameEventManager;
import game.map.GameMap;
import game.map.MobInMap;
import lombok.extern.slf4j.Slf4j;
import template.MobTemplate;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class MalamSuroAmbush extends GameEvent {

    // ── Konstanta timing ──────────────────────────────────────────────────────
    private static final int SPAWN_INTERVAL_MIN_SEC = 7200; // 3 menit
    private static final int SPAWN_INTERVAL_MAX_SEC = 7200; // 8 menit
    private static final int AMBUSH_LIFETIME_SEC     = 300; // 5 menit: kalau gak ada yang bunuh, hantu hilang begitu saja

    // set map_id yang boleh kedatangan hantu ambush. Kosong = semua map boleh.
    private static final Set<Integer> AMBUSH_MAP_IDS = Set.of(
        118, 119, 120  
    );

    // Kalau spawn gagal (mis. map di daftar belum ter-load / template mob
    // tidak ada), coba lagi lebih cepat daripada nunggu interval penuh.
    private static final int RETRY_FAIL_SEC = 30;

    // ── Konstanta mob ────────────────────────────────────────────────────────
    private static final short AMBUSH_MOB_TEMPLATE_ID = 223; 
    private static final int   AMBUSH_HP     = 400_000;
    private static final int   AMBUSH_DAMAGE = 25_000;
    private static final int   AMBUSH_DEF    = 6_000;
    private static final int   AMBUSH_OBJECT_ID = 29_999; // cuma 1 ambush aktif -> aman pakai id tetap

    // ── Hadiah ───────────────────────────────────────────────────────────────
    public static final short JIMAT_ID           = 475;
    public static final byte  ITEM_CAT_MATERIAL  = 7;
    private static final int  JIMAT_DROP_MIN     = 5;
    private static final int  JIMAT_DROP_MAX     = 10;

    // Hadiah Gems (permata) — tiap penyerang dapat jumlah acak di rentang ini.
    // Ubah angkanya sesuai ekonomi server kamu. Set keduanya 0 kalau mau
    // mematikan hadiah gems.
    private static final int  GEM_DROP_MIN       = 100;
    private static final int  GEM_DROP_MAX       = 200;
    // id khusus "Permata" di Service.Show_open_box_notice_item (type 4).
    private static final short GEM_NOTICE_ID     = -2;
    private static final short GEM_NOTICE_TYPE   = 4;

    // ── State ────────────────────────────────────────────────────────────────
    private MobInMap activeGhost;
    private GameMap  ghostMap;
    private long     spawnedAt;
    private int      nextSpawnCountdown = SPAWN_INTERVAL_MIN_SEC;

    public MalamSuroAmbush() {
        super(
            "Malam Suro - Teror Dadakan",
            EnumSet.allOf(DayOfWeek.class),
            List.of(new TimeEvent(LocalTime.of(18, 0), LocalTime.of(22, 0)))
        );
    }

    // ── GameEvent lifecycle ───────────────────────────────────────────────────

    @Override
    protected boolean isEnabled() {
        return Manager.gI().event_malam_suro_ambush;
    }

    @Override
    protected void onStart() {
        nextSpawnCountdown = randomIntervalSec();
        log.info("[MalamSuroAmbush] Dimulai, ambush pertama dalam {} detik.", nextSpawnCountdown);
    }

    @Override
    protected void onEnd() {
        if (activeGhost != null) {
            broadcastAll("Kabut mistis mereda... teror malam ini berakhir.");
        }
        despawnGhost();
    }

    @Override
    protected void onUpdate() {
        // Sub-event ini cuma jalan selama HalloweenEvent memang lagi musimnya.
        HalloweenEvent halloween = GameEventManager.gI().getEvent(HalloweenEvent.class);
        if (halloween == null || halloween.shouldBeRemoved()) return;

        if (activeGhost == null) {
            nextSpawnCountdown--;
            if (nextSpawnCountdown <= 0) {
                boolean spawned = trySpawnGhost();
                nextSpawnCountdown = spawned ? randomIntervalSec() : RETRY_FAIL_SEC;
            }
            return;
        }

        // Ada hantu aktif -> cek sudah mati atau sudah kelamaan (timeout).
        if (activeGhost.isdie) {
            rewardKillers(activeGhost, ghostMap);
            despawnGhost();
            return;
        }

        if (System.currentTimeMillis() - spawnedAt > AMBUSH_LIFETIME_SEC * 1000L) {
            broadcastAll("Sosok di " + ghostMap.name + " menghilang begitu saja ke dalam kabut...");
            despawnGhost();
        }
    }

    @Override
    public boolean shouldBeRemoved() {
        return false; // on/off murni lewat flag hso.conf (didaftarkan/tidak di GameEventManager)
    }

    // ── Getter publik ────────────────────────────────────────────────────────
    public boolean isGhostActive() {
        return activeGhost != null && !activeGhost.isdie;
    }

    /** Nama map yang sedang kedatangan hantu ambush, atau null kalau tidak ada. */
    public String getGhostMapName() {
        return (ghostMap != null && isGhostActive()) ? ghostMap.name : null;
    }

    // ── Spawn logic ────────────────────────────────────────────────────────────

    /** @return true kalau hantu berhasil di-spawn, false kalau gagal (dicoba lagi sebentar lagi). */
    private boolean trySpawnGhost() {
        GameMap targetMap = pickRandomAmbushMap();
        if (targetMap == null) {
            log.warn("[MalamSuroAmbush] Tidak ada map valid di AMBUSH_MAP_IDS yang ter-load.");
            return false;
        }

        MobTemplate tpl = MobTemplate.getMob(AMBUSH_MOB_TEMPLATE_ID);
        if (tpl == null) {
            log.error("[MalamSuroAmbush] mob_template_id={} tidak ditemukan di monster_template.", AMBUSH_MOB_TEMPLATE_ID);
            return false;
        }

        short[] pos = pickRandomSpawnPoint(targetMap);

        MobInMap ghost = new MobInMap();
        ghost.objectId       = AMBUSH_OBJECT_ID;
        ghost.template       = tpl;
        ghost.name           = "??? " + tpl.name;
        ghost.level          = tpl.level;
        ghost.hp             = AMBUSH_HP;
        ghost.maxHp          = AMBUSH_HP;
        ghost.x              = pos[0];
        ghost.y              = pos[1];
        ghost.map_id         = targetMap.mapId;
        ghost.zone_id        = targetMap.zoneId;
        ghost.setBaseDamage(AMBUSH_DAMAGE);
        ghost.setBaseDefense(AMBUSH_DEF);
        ghost.setBoss(true);
        ghost.is_boss_active = true;
        ghost.isdie          = false;
        ghost.isATK          = true;

        targetMap.Boss_entrys.add(ghost);

        activeGhost = ghost;
        ghostMap    = targetMap;
        spawnedAt   = System.currentTimeMillis();

        // Notice ke SELURUH server (semua pemain online), sebut nama map-nya.
        broadcastAll("Hantu Malam Suro muncul di " + targetMap.name
                + "! Kalahkan sebelum " + (AMBUSH_LIFETIME_SEC / 60) + " menit, atau dia menghilang.");
        log.info("[MalamSuroAmbush] Hantu muncul di map {} (id={}, zone={}) x={} y={}.",
                targetMap.name, targetMap.mapId, targetMap.zoneId, pos[0], pos[1]);
        return true;
    }

    private void despawnGhost() {
        if (activeGhost != null && ghostMap != null) {
            activeGhost.isdie          = true;
            activeGhost.is_boss_active = false;
            ghostMap.Boss_entrys.remove(activeGhost);
        }
        activeGhost = null;
        ghostMap    = null;
    }

    private void rewardKillers(MobInMap mob, GameMap map) {
        if (mob.top_dame.isEmpty() || map == null) return;

        for (Player p : new CopyOnWriteArrayList<>(map.players)) {
            if (!mob.top_dame.containsKey(p.name)) continue;

            short qty = (short) Util.nextInt(JIMAT_DROP_MIN, JIMAT_DROP_MAX);
            p.item.add_item_bag47(JIMAT_ID, qty, ITEM_CAT_MATERIAL);

            // Hadiah gems: updateGem() sekaligus mengirim update saldo ke client.
            int gemQty = (GEM_DROP_MAX > 0) ? Util.nextInt(GEM_DROP_MIN, GEM_DROP_MAX) : 0;
            if (gemQty > 0) p.updateGem(gemQty);

            try {
                // add_item_bag47() cuma update data server, perlu charInventory(7)
                // biar item baru langsung muncul di UI inventory pemain (pola
                // sama seperti PersimpanganKematian.rewardKillers()).
                p.item.charInventory(7);
                if (gemQty > 0) {
                    Service.Show_open_box_notice_item(
                        p, "Kamu Mendapatkan",
                        new short[]{JIMAT_ID, GEM_NOTICE_ID},
                        new int[]{qty, gemQty},
                        new short[]{ITEM_CAT_MATERIAL, GEM_NOTICE_TYPE}
                    );
                } else {
                    Service.Show_open_box_notice_item(
                        p, "Kamu Mendapatkan",
                        new short[]{JIMAT_ID},
                        new int[]{qty},
                        new short[]{ITEM_CAT_MATERIAL}
                    );
                }
            } catch (IOException ignore) {}
        }

        broadcastAll("Hantu Malam Suro di " + map.name + " berhasil ditumpas! Jimat & Gems dibagikan ke penyerangnya.");
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private GameMap pickRandomAmbushMap() {
        java.util.Map<Integer, List<GameMap>> byMap = new java.util.LinkedHashMap<>();
        for (GameMap[] zones : GameMap.entrys) {
            if (zones == null) continue;
            for (GameMap zone : zones) {
                if (zone == null) continue;
                int id = zone.mapId;
                if (!AMBUSH_MAP_IDS.isEmpty() && !AMBUSH_MAP_IDS.contains(id)) continue;
                byMap.computeIfAbsent(id, k -> new ArrayList<>()).add(zone);
            }
        }
        if (byMap.isEmpty()) return null;

        List<Integer> ids = new ArrayList<>(byMap.keySet());
        int pickedId = ids.get(Util.nextInt(ids.size()));
        List<GameMap> zones = byMap.get(pickedId);
        log.info("[MalamSuroAmbush] Map ter-load dari whitelist {} -> terpilih {}", ids, pickedId);
        return zones.get(Util.nextInt(zones.size()));
    }

    /**
     * Titik spawn acak di map: pakai koordinat salah satu titik spawn mob
     * bawaan map (pasti berada di area yang bisa diinjak). Kalau map tidak
     * punya mob sama sekali, fallback ke tengah-bawah map.
     */
    private short[] pickRandomSpawnPoint(GameMap map) {
        if (map.mobs != null && map.mobs.length > 0) {
            MobInMap m = map.mobs[Util.random(map.mobs.length - 1)];
            return new short[]{m.x, m.y};
        }
        return new short[]{(short) (map.mapW * 24 / 2), (short) (map.mapH * 24 - 48)};
    }

    private int randomIntervalSec() {
        return Util.nextInt(SPAWN_INTERVAL_MIN_SEC, SPAWN_INTERVAL_MAX_SEC);
    }

    /** Notice ke SEMUA pemain online di semua map (pola sama seperti broadcastMeteorCelebrationAllMaps). */
    private void broadcastAll(String msg) {
        for (GameMap[] zones : GameMap.entrys) {
            if (zones == null) continue;
            for (GameMap gm : zones) {
                if (gm == null) continue;
                for (Player p : new ArrayList<>(gm.players)) {
                    if (p == null || p.conn == null || !p.conn.connected) continue;
                    try {
                        Service.send_notice_nobox_yellow(p.conn, msg);
                    } catch (IOException ignore) {}
                }
            }
        }
    }

    private void broadcastToMap(GameMap map, String msg) {
        if (map == null) return;
        for (Player p : map.players) {
            try {
                Service.send_notice_nobox_white(p.conn, msg);
            } catch (IOException ignore) {}
        }
    }
}