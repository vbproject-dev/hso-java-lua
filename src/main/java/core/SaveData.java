package core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import game.guild.Guild;
import client.Player;
import core.BXH.Memin4;
import game.map.GameMap;
import template.Level;
import template.PlayerPart;
import game.event.Event_1;
import event_daily.CastleSiegeManager;
import core.SessionLog;

import event_daily.Wedding;


import game.map.MapService;
import utils.JsonUtil;
import utils.SQLHelper;

public class SaveData {

    @SuppressWarnings({"unchecked"})
    public synchronized static void process() {
        if(Manager.isServerTest)
            return;

        long time_check = System.currentTimeMillis();
        Manager.gI().mine.update();

        try (Connection conn = SQL.gI().getConnection()) {
            conn.setAutoCommit(false);

            // clan
            BXH.BXH___GUILD.clear();
            BXH.BXH___GUILD.addAll(Guild.get_all_clan());
            BXH.BXH___GUILD.sort(Comparator
                    .comparingInt((Guild g) -> g.level).reversed()
                    .thenComparing(Comparator.comparingLong((Guild g) -> g.exp).reversed()));

            // Save Guild Data

            Guild.entrys.forEach(guild -> {
                SQLHelper.update("clan").where("name", guild.name)
                        .set("name", guild.name)
                        .set("level", guild.level)
                        .set("exp", guild.exp)
                        .set("slogan", guild.slogan)
                        .set("rule", guild.rule)
                        .set("mems",  Guild.flush_mem_json(guild.members))
                        .set("item",   Guild.flush_item_json(guild.clanItems))
                        .set("notice", guild.notice)
                        .set("vang", guild.gold)
                        .set("kimcuong", guild.gems)
                        .set("icon", guild.icon)
                        .set("max_mem", guild.maxMember);
            });

            // Save Wedding
            Wedding.list.forEach(w -> SQLHelper.update("wedding").where("id", w.id)
                    .set("item", JsonUtil.array(
                            w.exp,
                            w.it.color,
                            w.it.tier,
                            w.it.op.stream()
                                    .map(op -> JsonUtil.array(op.id, op.getParam(0)))
                                    .collect(com.google.gson.JsonArray::new,
                                            com.google.gson.JsonArray::add,
                                            (a, b) -> b.forEach(a::add))
                    ).toString()));

            var ps = conn.prepareStatement("UPDATE `wedding` SET `item` = ? WHERE `id` = ?;");
            for (int i = 0; i < Wedding.list.size(); i++) {
                Wedding temp = Wedding.list.get(i);
                ps.clearParameters();
                ps.setString(1, Wedding.serializeRingItem(temp.exp, temp.it));
                ps.setInt(2, temp.id);
                ps.executeUpdate();
            }
            ps.close();
            conn.commit();

            // flush player
            String query
                    = "UPDATE `player` SET `level` = ?, `exp` = ?, `site` = ?, `body` = ?, `eff` = ?, `friend` = ?, `skill` = ?, `item4` = ?, "
                    + "`item7` = ?, `item3` = ?, `itemwear` = ?, `giftcode` = ?, `enemies` = ?, `rms_save` = ?, `itembox4` = ?, "
                    + "`itembox7` = ?, `itembox3` = ?, `pet` = ?, `medal_create_material` = ?, `point_active` = ?, `vang` = ?, "
                    + "`kimcuong` = ?, `tiemnang` = ?, `kynang` = ?, `diemdanh` = ?, `chucphuc` = ?, `hieuchien` = ?, `typeexp` = ?, "
                    + "`date` = ?, `point1` = ?, `point2` = ?, `point3` = ?, `point4` = ?  WHERE `id` = ?;";
            ps = conn.prepareStatement(query);
            for (GameMap[] gameMap : GameMap.entrys) {
                if(gameMap == null)continue;
                for (GameMap gameMap0 : gameMap) {
                    if(gameMap0 == null || gameMap0.players == null )continue;
                    for (int i1 = 0; i1 < gameMap0.players.size(); i1++) {
                        // for (int i1 = 0; i1 < ServerManager.gI().t1.list_p.size(); i1++) {
                        try
                        {
                            ps.clearParameters();
                            Player p0 = gameMap0.players.get(i1);

                            if(p0.conn == null || p0.conn.socket == null || p0.conn.socket.isClosed() || !p0.conn.connected)
                            {
                                MapService.leave(gameMap0, p0);
                                continue;
                            }
                            p0.flush();
                        }catch(Exception ee){
                            Log.gI().addLogServer("save_data", ee.getMessage());
                        }
                    }
                }
            }
            
            
//            try
//            {
//                SessionManager.checkBugAccount();
//            }catch(Exception eee){}
            // }
//            ps.executeBatch();
            ps.close();
            // event
            if (Manager.gI().event == 1) {
                ps = conn.prepareStatement("UPDATE `event` SET `data` = ? WHERE `id` = ?;");
                ps.clearParameters();
                //
                ps.setNString(1, Event_1.SaveData().toJSONString());
                ps.setInt(2, 0);
                ps.executeUpdate();
                ps.close();
            }
            else if (Manager.gI().event == 2) {
                ps = conn.prepareStatement("UPDATE `event` SET `data` = ? WHERE `id` = ?;");
                ps.clearParameters();
                //
                ps.setNString(1, ev_he.Event_2.SaveData().toJSONString());
                ps.setInt(2, 1);
                ps.executeUpdate();
                ps.close();
            }
            else if (Manager.gI().event == 3) {
                ps = conn.prepareStatement("UPDATE `event` SET `data` = ? WHERE `id` = ?;");
                ps.clearParameters();
                //
                ps.setNString(1, ev_he.Event_3.SaveData().toJSONString());
                ps.setInt(2, 2);
                ps.executeUpdate();
                ps.close();
            }
            // bxh
            BXH.BXH_level.clear();
            //ps = conn.prepareStatement(
            //
            //       "SELECT `id`, `level`, `exp`, `name`, `body`, `itemwear` FROM `player` WHERE `level` > 10 ORDER BY `level` DESC, exp DESC LIMIT 99;");
            ps = conn.prepareStatement(
                    "SELECT p.`id`, p.`level`, p.`exp`, p.`name`, p.`body`, p.`itemwear` " +
                            "FROM `player` p " +
                            "JOIN `account` a ON p.`uid` = a.`id` " +
                            "WHERE a.`ac_admin` = 0 AND p.`level` > 10 " +
                            "ORDER BY p.`level` DESC, p.`exp` DESC " +
                            "LIMIT 99;"
            );
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                Memin4 temp = new Memin4();
                temp.level = rs.getShort("level");
                temp.exp = rs.getLong("exp");
                temp.name = rs.getString("name");
                JSONArray jsar = (JSONArray) JSONValue.parse(rs.getString("body"));
                if (jsar == null) {
                    continue; // ✅ skip player ini, jangan return (akan bocorkan koneksi)
                }
                temp.head = Byte.parseByte(jsar.get(0).toString());
                temp.hair = Byte.parseByte(jsar.get(2).toString());
                temp.eye = Byte.parseByte(jsar.get(1).toString());
                jsar.clear();
                jsar = (JSONArray) JSONValue.parse(rs.getString("itemwear"));
                if (jsar == null) {
                    continue; // ✅ skip player ini
                }
                temp.itemwear = new ArrayList<>();
                for (int i3 = 0; i3 < jsar.size(); i3++) {
                    JSONArray jsar2 = (JSONArray) JSONValue.parse(jsar.get(i3).toString());
                    byte index_wear = Byte.parseByte(jsar2.get(9).toString());
                    if (index_wear != 0 && index_wear != 1 && index_wear != 6 && index_wear != 7 && index_wear != 10) {
                        continue;
                    }
                    PlayerPart temp2 = new PlayerPart();
                    temp2.type = Byte.parseByte(jsar2.get(2).toString());
                    temp2.part = Byte.parseByte(jsar2.get(6).toString());
                    temp.itemwear.add(temp2);
                }
                temp.guild = Guild.getPlayerGuild(temp.name);
                String percent
                        = String.format("%.1f", (((float) temp.exp * 1000) / Level.entrys.get(temp.level - 1).exp) / 10f);
                temp.info = "Level : " + (temp.level) + "\t-\t" + percent + "%";
                BXH.BXH_level.add(temp);
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

            try (PreparedStatement ps2 = conn.prepareStatement(
                "SELECT * FROM `player` p JOIN `account` a ON p.`uid` = a.`id` " +
                "WHERE a.`ac_admin` = 0 ORDER BY p.`level` DESC LIMIT 1;")) 
            {
                rs = ps2.executeQuery(); // perubahan 23.06.2024: pakai ps2 agar ps tetap bisa dipakai untuk update clan dll
                


            // rs = ps.executeQuery("SELECT * FROM `player` ORDER BY `level` DESC, `id` LIMIT 1");
   //         rs = ps.executeQuery("SELECT * FROM `player` ORDER BY `hieuchien` DESC, `id` LIMIT 1");
  //          rs = ps.executeQuery("SELECT * FROM `player` WHERE `name` = '"+Manager.VuaChienTruong+"' LIMIT 1");
            if (rs.next()) {
                GameMap.name_mo = rs.getString("name");
                JSONArray js = (JSONArray) JSONValue.parse(rs.getString("body"));
                GameMap.head = Short.parseShort(js.get(0).toString());
                GameMap.eye = Short.parseShort(js.get(1).toString());
                GameMap.hair = Short.parseShort(js.get(2).toString());
                js.clear();
                js = (JSONArray) JSONValue.parse(rs.getString("itemwear"));
                for (int i3 = 0; i3 < js.size(); i3++) {
                    JSONArray jsar2 = (JSONArray) JSONValue.parse(js.get(i3).toString());
                    if (jsar2 == null) {
                        continue; // ✅ skip item ini, jangan return
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

            }
            
            //
            conn.commit(); // ✅ commit semua perubahan
            // rs, ps, conn ditutup otomatis oleh try-with-resources
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[" + Util.get_now_by_time() + "] save data fail!");
            return;
        }
        System.out.println("[" + Util.get_now_by_time() + "] save data ok " + (System.currentTimeMillis() - time_check));
        //ServerManager.gI().time_l = System.currentTimeMillis()+60_000L;

        BXH.loadTopLevel();
        BXH.loadTopBossKill();
    }
}