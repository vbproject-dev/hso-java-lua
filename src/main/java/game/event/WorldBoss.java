package game.event;

import core.Manager;
import core.SQL;
import game.map.GameMap;
import game.map.MobInMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import template.MobTemplate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WorldBoss extends GameEvent {
    public static final String EVENT_NAME = "WORLDBOSS";
    private final CopyOnWriteArrayList<MobInMap> listBoss = new CopyOnWriteArrayList<>();

    public WorldBoss() {

        super(EVENT_NAME,
                EnumSet.allOf(DayOfWeek.class),
                List.of(
                        new TimeEvent(LocalTime.of(18, 0), LocalTime.of(20, 0))
                        , new TimeEvent(LocalTime.of(5, 0), LocalTime.of(7, 0))
                )
        );
    }

    @Override
    protected boolean isEnabled() {
        return Manager.gI().event_world_boss;
    }

    @Override
    protected void onStart() {

        if (!loadBoss()) {
            System.out.println("Worldboss failed to load");
            return;
        }

        System.out.print(listBoss.size() + " boss loaded ");

        try {
            Manager.gI().chatKTGprocess(" Waktu perburuan boss telah tiba");
        } catch (IOException ignore) {
        }
    }

    @Override
    protected void onEnd() {
        for (MobInMap mob : listBoss) {
            GameMap[] gameMap = GameMap.getMapById(mob.map_id);
            if (gameMap == null || gameMap.length <= mob.zone_id)
                continue;

            mob.isdie = true;
            mob.is_boss_active = false;
            gameMap[mob.zone_id].Boss_entrys.remove(mob);
        }
        listBoss.clear();

        try {
            Manager.gI().chatKTGprocess(" Waktu perburuan boss telah berakhir");
        } catch (IOException ignore) {
        }
    }

    @Override
    protected void onUpdate() {


        try {

            long time = System.currentTimeMillis();

            for (MobInMap mob : listBoss) {
                if ((!mob.is_boss_active || mob.isdie) && time > mob.time_back) {
                    GameMap[] gameMap = GameMap.getMapById(mob.map_id);
                    if (gameMap == null || gameMap.length <= mob.zone_id) {
                        continue;
                    }

                    mob.isdie = false;
                    mob.is_boss_active = true;
                    mob.hp = mob.maxHp;
                    mob.time_regen_hp = 0; // reset timer regen tiap kali boss spawn baru
                    gameMap[mob.zone_id].Boss_entrys.remove(mob);
                    gameMap[mob.zone_id].Boss_entrys.add(mob);

                    Manager.gI().chatKTGprocess("WorldBoss " + mob.template.name + " Telah muncul di " + gameMap[mob.zone_id].name + " area " + (mob.zone_id + 1));


                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public boolean loadBoss() {

        // FIX RESOURCE LEAK: conn/ps/rs di sini tidak pernah di-close() sama
        // sekali, baik sukses maupun gagal -- satu koneksi dari pool HikariCP
        // (maksimal 40) hilang permanen setiap loadBoss() dipanggil.
        // try-with-resources menjamin ketiganya selalu ditutup.
        try (Connection conn = SQL.gI().getConnection();
             Statement ps = conn.createStatement();
             ResultSet rs = ps.executeQuery("SELECT * FROM `boss_event` WHERE `active` = 1;")) {
            int index = 10_000;

            DayOfWeek today = LocalDate.now().getDayOfWeek();
            while (rs.next()) {


                MobTemplate temp = MobTemplate.getMob(rs.getShort("id"));
                if (temp != null) {
                    MobInMap mob = new MobInMap();

                    mob.objectId = index++;
                    mob.template = temp;
                    mob.name = temp.name;
                    mob.level = temp.level;
                    long maxHp = (rs.getLong("hp") * temp.level) / 2;
                    if (maxHp > Integer.MAX_VALUE) {
                        maxHp = Integer.MAX_VALUE;
                    }
                    mob.hp = (int) maxHp;
                    mob.maxHp = (int) maxHp;
                    mob.map_id = rs.getByte("map");
                    mob.x = (short) (rs.getInt("x") * 24);
                    mob.y = (short) (rs.getInt("y") * 24);
                    mob.zone_id = rs.getByte("area");
                    mob.setBaseDamage(rs.getInt("damage"));
                    mob.setBaseDefense(rs.getInt("defense"));
                    mob.timeBossRecive = rs.getInt("time") * 60_000;
                    mob.setBoss(true);
                    mob.regenHpPct = rs.getInt("regen_hp_pct");
                    mob.regenIntervalSec = rs.getInt("regen_interval_sec");

                    JSONArray jsar = (JSONArray) JSONValue.parse(rs.getString("item3"));
                    if (jsar != null) {
                        for (int i = 0; i < jsar.size(); i++) {
                            mob.item3.putIfAbsent(i, Short.parseShort(jsar.get(i).toString()));
                        }
                        jsar.clear();
                    }

                    jsar = (JSONArray) JSONValue.parse(rs.getString("item4"));
                    if (jsar != null) {
                        for (Object o : jsar) {
                            JSONArray innerArray = (JSONArray) o; // each inner array
                            if (innerArray.size() >= 2) {
                                Short key = Short.parseShort(innerArray.get(0).toString());
                                Short value = Short.parseShort(innerArray.get(1).toString());
                                mob.item4.putIfAbsent(key, value);
                            }
                        }
                        jsar.clear();
                    }

                    jsar = (JSONArray) JSONValue.parse(rs.getString("item7"));
                    if (jsar != null) {
                        for (Object o : jsar) {
                            JSONArray innerArray = (JSONArray) o; // each inner array
                            if (innerArray.size() >= 2) {
                                Short key = Short.parseShort(innerArray.get(0).toString());
                                Short value = Short.parseShort(innerArray.get(1).toString());
                                mob.item7.putIfAbsent(key, value);
                            }
                        }
                        jsar.clear();
                    }

                    int day = rs.getInt("day");

                    if (day == -1) {
                        listBoss.add(mob);
                    } else {
                        if (day < 1 || day > 7) {
                            day = 1;
                        }
                        DayOfWeek target = DayOfWeek.of(day);
                        if (target == today) {
                            for (int i = 0; i < 7; i++) {
                                MobInMap mobInMap = new MobInMap();
                                mobInMap.objectId = (mob.objectId + i);
                                mobInMap.template = temp;
                                mobInMap.name = temp.name;
                                mobInMap.level = temp.level;
                                mobInMap.hp = mob.getMaxHP();
                                mobInMap.maxHp = mob.getMaxHP();
                                mobInMap.map_id = mob.map_id;
                                mobInMap.x = mob.x;
                                mobInMap.y = mob.y;
                                mobInMap.setBaseDamage(mob.getBaseDamage());
                                mobInMap.setBaseDefense(mob.getDefBase());
                                mobInMap.zone_id = (byte) (mob.zone_id + i);
                                mobInMap.timeBossRecive = mob.timeBossRecive;
                                mobInMap.item3.putAll(mob.item3);
                                mobInMap.item4.putAll(mob.item4);
                                mobInMap.item7.putAll(mob.item7);
                                mobInMap.regenHpPct = mob.regenHpPct;
                                mobInMap.regenIntervalSec = mob.regenIntervalSec;
                                mobInMap.setBoss(true);
                                listBoss.add(mobInMap);

                                System.out.println("def " + mobInMap.getDefBase() + " dmg " + mob.getBaseDamage());
                            }
                        }
                    }

                }


            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public MobInMap getBoss(int id) {
        for (MobInMap mob : listBoss) {
            if (mob.template.mob_id == id) {
                return mob;
            }
        }

        return null;
    }
}
