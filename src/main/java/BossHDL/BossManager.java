package BossHDL;

import client.Player;
import core.Manager;
import core.Util;
import java.io.IOException;
import java.util.OptionalInt;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import game.map.LeaveItemMap;
import game.map.MobInMap;
import template.MobTemplate;
import game.map.GameMap;


public class BossManager {
    public static final CopyOnWriteArrayList<MobInMap> entrys = new CopyOnWriteArrayList<>();
    
    private static byte GetIdMap(int idboss){
        return switch (idboss) {
            //case = mod - Return = map

            case 103 -> 7;
            case 104 -> 15;
            case 101 -> 25;
            case 84 -> 37;
            case 105 -> 45;
            case 83 -> 51;
            case 106 -> 62;
            case 149 -> 76;
            case 155 -> 79;
            case 174 -> 26;
            case 173 -> 113;
            case 195 -> 112;
            case 196 -> 115;
            case 197 -> 114;
            case 186 -> 109;
            case 187 -> 110;
            case 188 -> 111;
            default -> -1;
        };
    }
    private static short[] getSite(int idboss){
        return switch (idboss) {
            //mod + vị trí

            case 103 -> new short[]{432, 512};
            case 104 -> new short[]{530, 213};
            case 101 -> new short[]{204, 284};
            case 84 -> new short[]{160, 224};
            case 105 -> new short[]{816, 1064};
            case 83 -> new short[]{320, 1520};
            case 106 -> new short[]{468, 498};
            case 149 -> new short[]{204, 762};
            case 155 -> new short[]{534, 732};
            case 174 -> new short[]{550, 250};
            case 173, 195, 196, 197, 186, 187, 188 -> new short[]{450, 432};
            case 221 -> new short[]{49 * 24, 15 * 24};
            case 222 -> new short[]{30 * 24, 7 * 24};
            default -> new short[]{500, 284};
        };
    }


    public static void init(){
        int idx = 10_000;
        int[] ids = new int[]{101 , 84 , 83 ,103 ,104 ,105 , 106, 149 , 155, 174, 173, 195, 196, 197, 186, 187, 188};
        for(int id : ids){
            for(int i=0; i<5;i++){
                if(id == 174){
                    if(i == 1 || i == 4 || Manager.gI().event != 2) continue;
                }


                    MobTemplate m = MobTemplate.getMob(id);
                    if (m != null) {
                        MobInMap temp = new MobInMap();
                        temp.template = m;
                        if (temp.x == 0 && temp.y == 0) {
                            temp.x = getSite(id)[0];
                            temp.y = getSite(id)[1];
                        }
                        temp.level = id == 174 ? 150 : m.level;
                        temp.setBoss(true);
                        temp.setMaxHP((m.hpmax * m.level) / 2);
                        // FIX: set base damage boss dari level agar getBaseDamage() tidak return 0
                        temp.setBaseDamage(temp.level * 500);
                        if (id == 174)
                            temp.timeBossRecive = 1000 * 60 * 60;
                        else
                            temp.timeBossRecive = 1000 * 60 * 60 * 3;
                        temp.map_id = GetIdMap(id);
                        temp.zone_id = (byte) i;
                        temp.objectId = idx++;
                        entrys.add(temp);

                    }

                //map[i].Boss_entrys.add(temp);
            }
        }
    }

    public static String getInfoBoss(int id) {
        StringBuilder sb = new StringBuilder();
        for(MobInMap mob: entrys){
           if (mob.template.mob_id == id) {
               GameMap[] m = GameMap.getMapById(mob.map_id);
               if (m == null) {
                   continue;
               }

               sb.append("Map: ").append(m[0].name).append(" Area: ").append(mob.zone_id + 1);
               sb.append(mob.isdie ? " Respawn in: " + utils._Time.getTimeLeft(mob.time_back)+ "\n" : " (Hidup) \n");

           }
        }

        if (sb.isEmpty()) {
            sb.append("Tidak ada informasi");
        }

        return sb.toString();
    }


    public static void Update(){
        try{
            long time = System.currentTimeMillis();
            for(MobInMap mob : entrys){
                if((!mob.is_boss_active || mob.isdie ) && time > mob.time_back){
                    GameMap[] gameMap = GameMap.getMapById(mob.map_id);
                    if(gameMap == null || gameMap.length <= mob.zone_id)continue;
                    mob.isdie = false;
                    mob.is_boss_active =true;
                    mob.hp = mob.getMaxHP();
                    mob.objectId = 4000 + Util.nextInt(100);
                    mob.setMaxHP(mob.getMaxHP());
                    gameMap[mob.zone_id].Boss_entrys.remove(mob);
                    gameMap[mob.zone_id].Boss_entrys.add(mob);
                    Manager.gI().chatKTGprocess(""+mob.template.name+" Telah muncul di "+ gameMap[mob.zone_id].name);

                }
            }
        }catch(Exception e){e.printStackTrace();}
    }
    
    
    
    
    public static void DropItemBossEvent(GameMap gameMap, MobInMap mob, Player p)throws IOException{
        if(Manager.gI().event == 2){
            int[] it4 = new int[]{48,49,50,51,52,5,26,131,132,24,10};
            int[] it7 = new int[]{346,349,33,34,12,13};
            
            for(int i=0; i< 15; i++){
                LeaveItemMap.leave_vang(gameMap,mob, -1);
            }

            for(int i=0; i<40; i++){
                int ran = Util.random(100);
                if(ran < 10)
                    LeaveItemMap.leave_item_by_type7(gameMap, (short)Util.random(it7),p, mob.objectId, p.objectId);
                if(ran < 40)
                    LeaveItemMap.leave_item_by_type4(gameMap, (short)Util.random(it4),p, mob.objectId, -1);
                else
                    LeaveItemMap.leave_item_by_type7(gameMap, (short)Util.random(417,464),p, mob.objectId, -1);
            }
        }
    }

    public static boolean loadBossEvent() {
        // ── Load premium boss dari tabel boss_premium ──────────────────
        // boss_event TIDAK di-load di sini — sudah ditangani WorldBoss.loadBoss()
        // agar tidak terjadi duplikasi boss di BossManager.entrys
        PremiumBossManager.load();
        // ── Load cache point kill boss premium ─────────────────────────
        BossKillPoint.load();
        return true;
    }

    public static <T> void replaceById(CopyOnWriteArrayList<T> list, T newItem, java.util.function.Function<T, Integer> getId) {
        OptionalInt indexOpt = IntStream.range(0, list.size())
                .filter(i -> getId.apply(list.get(i)).equals(getId.apply(newItem)))
                .findFirst();

        if (indexOpt.isPresent()) {
            // Replace existing object
            list.set(indexOpt.getAsInt(), newItem);
        } else {
            // Add new object
            list.add(newItem);
        }
    }
    
}