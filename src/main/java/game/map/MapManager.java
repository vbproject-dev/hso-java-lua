package game.map;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import model.UpgradeData;
import model.map.MapData;
import model.map.MapName;
import model.map.NpcData;
import model.map.Point;
import template.MobTemplate;
import utils.SQLHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
public class MapManager {
    private final Map<Integer, MapData> maps = new ConcurrentHashMap<>();
    private final Map<Integer, NpcData> npcs = new ConcurrentHashMap<>();
    private final Map<Integer, MapName> mapName = new ConcurrentHashMap<>();
    private UpgradeData upgradeData;

    private static final Set<Integer> BOSS_MOBS = Set.of(
            101, 84, 83, 103, 104, 105, 106, 149,
            155, 173, 195, 196, 197, 186, 187, 188
    );

    private MapManager() {
    }

    private static class Holder {
        private static final MapManager INSTANCE = new MapManager();
    }

    public static MapManager getInstance() {
        return Holder.INSTANCE;
    }

    public void loadMapData() {
        // Load MapData
        List<MapData> mapDataList = SQLHelper.selectFrom("map_data").getAsModel(MapData.class);
        mapDataList.forEach(mapData -> maps.put(mapData.getId(), mapData));

        // Load NPC
        List<NpcData> npcData = SQLHelper.selectFrom("npc").getAsModel(NpcData.class);
        npcData.forEach(npc -> npcs.put(npc.getId(), npc));

        List<MapName> names = SQLHelper.selectFrom("map_name").getAsModel(MapName.class);
        names.forEach(name -> mapName.put(name.getId(), name));

        upgradeData = SQLHelper.selectFrom("upgrade_data").firstAsModel(UpgradeData.class);


        // Initial Map
        for (MapData mapData : mapDataList) {
            GameMap[] zones = new GameMap[mapData.getMaxZone() + 1];

            for (int i = 0; i < zones.length; i++) {
                zones[i] = new GameMap(mapData.getId(), i, mapData);
                List<MobInMap> mobList = new ArrayList<>();

                // Area Perdagangan (mapId 52) harus bersih dari monster reguler.
                // Hanya "Clone" (ai/Clone.java, berdasarkan lokasi kematian player) yang boleh menyerang di sini.
                if (mapData.getId() != 52) {
                    for (Point point : mapData.getMobData()) {

                        MobTemplate template = MobTemplate.getMob(point.getId());
                        if (template == null || BOSS_MOBS.contains(point.getId()))
                            continue;

                        // Set gameMap pada template agar info nama map tersedia (hanya zone 0)
                        if (i == 0 && template.gameMap == null) {
                            template.gameMap = zones[0];
                        }

                        MobInMap mob = new MobInMap();
                        mob.template = template;
                        mob.objectId = mobList.size() + 1;
                        mob.x = (short) point.getX();
                        mob.y = (short) point.getY();
                        mob.setMaxHP(template.hpmax);
                        mob.hp = mob.getMaxHP();
                        mob.level = template.level;
                        mob.setBaseDamage(template.damage > 0 ? template.damage : template.level * 500);
                        mob.map_id = (byte) mapData.getId();
                        mob.zone_id = (byte) i;
                        mob.isdie = false;
                        mob.color_name = 0;
                        mob.is_boss_active = false;
                        // Sebelumnya dipaksa false untuk semua mob — sekarang ambil dari
                        // flag is_boss di database (monster_template), supaya mob yang
                        // memang ditandai boss (mis. "Ular Ratu" battlefield) dikenali benar.
                        mob.setBoss(template.is_boss);
                        if (template.mob_id == 89 || template.mob_id == 90 || template.mob_id == 91 || template.mob_id == 92) {
                            mob.time_back = System.currentTimeMillis() + 60_000L * 120;
                            mob.time_refresh = 60 * 120;
                            mob.color_name = 4;
                        }else {

                            mob.time_back = System.currentTimeMillis() + 4_000L;

                        }

                        mobList.add(mob);
                    }
                }

                zones[i].mobs = mobList.toArray(MobInMap[]::new);

            }

            GameMap.entrys.add(zones);
        }



    }

    public MapData getMapData(int mapId) {
        return maps.get(mapId);
    }

    public NpcData getNpcData(int npcId) {
        return npcs.get(npcId);
    }
}