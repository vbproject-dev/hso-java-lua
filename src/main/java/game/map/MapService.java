package game.map;

import ai.MobAi;
import ai.Clone;
import ai.PlayerClone;

import java.io.IOException;
import java.util.ArrayList;

import game.ai.AiManager;
import game.ai.PlayerBot;
import game.event.GameEventManager;
import game.event.btf.BTF;
import game.event.btf.Team;
import game.guild.Guild;
import game.pvp.PvpManager;
import client.Pet;
import client.Player;
import core.Manager;
import core.MenuController;
import core.Service;
import core.Util;
import event_daily.CastleSiegeManager;
import event_daily.KingCup;
import event_daily.KingCupManager;
import event_daily.Battlefield;
//import event_daily.LoiDai;
import client.io.Message;
import client.io.Session;

import java.util.List;

import feature.teleport.PremiumTeleportManager;
import lombok.extern.slf4j.Slf4j;
import model.map.Vgo;
import template.EffTemplate;
import template.Eff_TextFire;
import template.Item3;
import template.LvSkill;
import template.MainObject;
import template.MobTemplate;
import template.Mob_Dungeon;
import template.Crystal;
import template.Pet_di_buon;
import template.Pet_di_buon_manager;
import template.StrucEff;

@Slf4j
public class MapService {

    public static void enter(GameMap gameMap, Player p) {
        // synchronized (map) {
        // if (!map.players.contains(p)) {
        // map.players.add(p);
        // }
        // }
        if (!gameMap.players.contains(p)) {
            gameMap.players.add(p);
        }
        p.change_new_date();
        //
        try {
            // if (map.map_id != 48) {
            if (gameMap.zoneId == gameMap.maxzone && !gameMap.isMapLoiDai() && !gameMap.isCastleSiegeMap()) {
                MapService.changeFlag(gameMap, p, -1);
            }
            gameMap.sendMapData(p);
            Service.sendMainCharInfo(p);
            Service.send_combo(p.conn);
            Service.send_point_pk(p);
            Service.send_health(p);
            Service.sendPlayerWear(p);

            List<Integer> defaul = new ArrayList<>(java.util.Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
            List<Integer> pks = new ArrayList<>();
            if (GameMap.isSummonMap(gameMap, true) && Manager.gI().mine.isRunning()) {
                int typepk = -1;
                for (Player pl : gameMap.players) {
                    if (pl.objectId == p.objectId || pl.typepk == -1 || pl.is_nhanban) {
                        continue;
                    }
                    if (p.myclan != null && pl.myclan != null) {
                        if (pl.myclan.name.equals(p.myclan.name)) {
                            typepk = pl.typepk;
                            break;
                        }
                    }
                    pks.add((int) pl.typepk);
                }
                if (typepk == -1) {
                    Integer t = Util.random(defaul, pks);
                    if (t != null) {
                        typepk = t;
                    } else {
                        typepk = Util.random(1, 10);
                    }
                }
                Message m = new Message(42);
                m.writer().writeShort(p.objectId);
                m.writer().writeByte(typepk);
                p.typepk = (byte) typepk;
                MapService.sendMsgPlayerInside(gameMap, p, m, true);
                m.cleanup();
            }
            // }
            if (p.party != null) {
                p.party.sendin4();
            }

            if (gameMap.isCastleSiegeMap()) {
                CastleSiegeManager.SenDataTime(p.conn);
            }

            // Kirim countdown sesi premium saat player masuk map premium 111-114
            if (PremiumTeleportManager.isPremiumMap(gameMap.mapId)
                    && !PremiumTeleportManager.isGatewayMap(gameMap.mapId)) {
                PremiumTeleportManager.gI().sendSessionCountdown(p);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        p.other_player_inside.clear();
        p.other_mob_inside.clear();
        p.other_mob_inside_update.clear();
    }

    public static void leave(GameMap gameMap, Player p) {
        // Mode AFK: jangan hapus player dari map, biarkan bot tetap berjalan
        if (p.modeBot) return;
        // synchronized (map) {
        // map.players.remove(p);
        // }
        if (gameMap.players.contains(p)) {
            gameMap.players.remove(p);
        }
        try {
            if (gameMap.mapId == 87) {
                CastleSiegeManager.PlayerDie(p);
            }

            // Bug fix (PVP/King Cup): kotak timer/countdown (-104) yang ditampilkan
            // PvpSession.sendCountdownMessage() saat duel PVP tidak pernah ditutup
            // lagi saat pemain keluar dari map arena (map 100/102) — baik lewat
            // kickPlayers() KingCup, releasePlayer() PvpSession, changeMap() biasa,
            // maupun disconnect (Session.disconnect() -> leave() ini juga). Dulu ada
            // method leave_by_loidai() yang isinya persis fix ini, tapi tidak pernah
            // dipanggil dari mana pun sehingga tidak berefek sama sekali. Samakan
            // dengan referensi (map.kingCupMap != null -> send_time_box(p, 0, ...)
            // di leave()): kirim paket "tutup box" ke SEMUA yang keluar dari map
            // lôi đài (arena maupun lobi map 100), baik peserta pertandingan maupun
            // penonton, supaya UI timer tidak nyangkut sampai relog.
            if (gameMap.isMapLoiDai() && p.conn != null && p.conn.connected) {
                Message clearBox = new Message(-104);
                clearBox.writer().writeByte(1);
                clearBox.writer().writeByte(0);
                p.conn.addmsg(clearBox);
                clearBox.cleanup();
            }

            Message m = new Message(8);
            m.writer().writeShort(p.objectId);
            sendMsgPlayerInside(gameMap, p, m, false);
            m.cleanup();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendMsgPlayerInside(GameMap gameMap, MainObject mainObj, Message m, boolean included) {
        for (int i = 0; i < gameMap.players.size(); i++) {
            Player p0 = gameMap.players.get(i);
            // Skip bot AFK (conn null/terputus) dan player di luar radius
            if (p0 == null || p0.conn == null || !p0.conn.connected) continue;
            if (((Math.abs(p0.x - mainObj.x) < 200 && Math.abs(p0.y - mainObj.y) < 200)
                    || GameMap.is_map__load_board_player(gameMap.mapId)) && (included || (mainObj.objectId != p0.objectId))) {
                p0.conn.addmsg(m);
            }
        }
    }


    public static void broadcastMainCharInfo(GameMap gameMap, Player p) throws IOException {
        long _time = System.currentTimeMillis();
        if (p.getEffectMedal(StrucEff.TangHinh) != null) {
            return;
        }
        for (int i = 0; i < gameMap.players.size(); i++) {
            Player p0 = gameMap.players.get(i);
            if (p0.objectId != p.objectId && ((Math.abs(p0.x - p.x) < 200 && Math.abs(p0.y - p.y) < 200)
                    || GameMap.is_map__load_board_player(gameMap.mapId))) {
                MapService.send_in4_other_char(gameMap, p0, p);
            }
        }
    }

    /**
     * Resolve Vgo yang toMap-nya 0 (tidak diset dari DB).
     * Jika player ada di premium map (111-114) dan portal tidak punya tujuan,
     * arahkan kembali ke gateway map 115.
     */
    private static Vgo resolveVgo(Vgo vgo, int currentMapId) {
        if (vgo.toMap != 0) {
            return vgo; // toMap sudah diset dengan benar, pakai langsung
        }
        // toMap = 0 berarti data portal tidak lengkap dari DB
        // Jika sedang di premium map, portal keluar = kembali ke gateway 115
        if (PremiumTeleportManager.isPremiumMap(currentMapId)) {
            Vgo fixed = new Vgo();
            fixed.x     = vgo.x;
            fixed.y     = vgo.y;
            fixed.name  = vgo.name;
            fixed.toMap = (byte) PremiumTeleportManager.RETURN_MAP_ID;
            fixed.toX   = (short) PremiumTeleportManager.RETURN_X;
            fixed.toY   = (short) PremiumTeleportManager.RETURN_Y;
            return fixed;
        }
        // Map lain: biarkan apa adanya (akan gagal di changeMap, bukan tanggung jawab di sini)
        return vgo;
    }

    public static void send_move(GameMap gameMap, Player p, Message m2) throws IOException {
        short mx = m2.reader().readShort();
        short my = m2.reader().readShort();
        boolean changeee = false;
        if (p.isChangemap && (!gameMap.isCastleSiegeMap() || CastleSiegeManager.isChangeMap(gameMap))) {
            for (Vgo vgo : gameMap.vgos) {
                if (Math.abs(vgo.x - p.x) < 40 && Math.abs(vgo.y - p.y) < 40) {
                    boolean ch = true;
                    if (GameMap.isBattlefieldMap(gameMap.mapId)) {
                        switch (gameMap.mapId) {
                            case 54: {
                                if (vgo.toMap == 53 && p.typepk != 5) {
                                    ch = false;
                                }
                                break;
                            }
                            case 56: {
                                if (vgo.toMap == 55 && p.typepk != 2) {
                                    ch = false;
                                }
                                break;
                            }
                            case 58: {
                                if (vgo.toMap == 57 && p.typepk != 4) {
                                    ch = false;
                                }
                                break;
                            }
                            case 60: {
                                if (vgo.toMap == 59 && p.typepk != 1) {
                                    ch = false;
                                }
                                break;
                            }
                        }
                    }
                    if (ch) {
                        Vgo actualVgo = resolveVgo(vgo, gameMap.mapId);
                        p.changeMap(p, actualVgo);
                        changeee = true;
                    }
                    return;
                }
            }
        }
        long _time = System.currentTimeMillis();
        if (Math.abs(p.x - mx) > (300) || Math.abs(p.y - my) > 300) {
            Message m = new Message(4);
            m.writer().writeByte(0);
            m.writer().writeShort(0);
            m.writer().writeShort(p.objectId);
            m.writer().writeShort(p.x);
            m.writer().writeShort(p.y);
            m.writer().writeByte(-1);
            p.conn.addmsg(m);
            m.cleanup();
            return;
        }
        p.time_move = _time;
        p.x = mx;
        p.y = my;
        if (p.isChangemap && p.timeCantChangeMap < _time && (gameMap.mapId < 83 || gameMap.mapId > 86)) {
            for (Vgo vgo : gameMap.vgos) {
                if (Math.abs(vgo.x - p.x) < 40 && Math.abs(vgo.y - p.y) < 40) {
                    Vgo actualVgo = resolveVgo(vgo, gameMap.mapId);
                    p.changeMap(p, actualVgo);
                    return;
                }
            }
        } else if (!(Math.abs(p.xOld - p.x) < 45 && Math.abs(p.yOld - p.y) < 45)) {
            p.isChangemap = true;
        }
        //
        if (gameMap.mapId != 50) {
            Message m = new Message(4);
            m.writer().writeByte(0);
            m.writer().writeShort(0);
            m.writer().writeShort(p.objectId);
            m.writer().writeShort(p.x);
            m.writer().writeShort(p.y);
            m.writer().writeByte(-1);
            //
            MapService.update_inside_player(gameMap, m, p);
            //
            m.cleanup();
        }
        if (p.pet_di_buon != null && p.pet_di_buon.id_map == p.map.mapId && p.map.zoneId == p.map.maxzone) {
            if (p.pet_di_buon.time_move < System.currentTimeMillis()) {
                p.pet_di_buon.time_move = System.currentTimeMillis() + 1000L;
                if (Math.abs(p.pet_di_buon.x - p.x) < (85 * p.pet_di_buon.speed)
                        && Math.abs(p.pet_di_buon.y - p.y) < (85 * p.pet_di_buon.speed)) {
                    p.pet_di_buon.x = p.x;
                    p.pet_di_buon.y = p.y;
                    if (p.pet_di_buon.speed != 1 && p.pet_di_buon.time_skill < System.currentTimeMillis()) {
                        p.pet_di_buon.speed = 1;
                        //
                        System.out.println("map.MapService.send_move()111");
                        Message mm = new Message(7);
                        mm.writer().writeShort(p.pet_di_buon.objectId);
                        mm.writer().writeByte((byte) 120);
                        mm.writer().writeShort(p.pet_di_buon.x);
                        mm.writer().writeShort(p.pet_di_buon.y);
                        mm.writer().writeInt(p.pet_di_buon.hp);
                        mm.writer().writeInt(p.pet_di_buon.getMaxHP());
                        mm.writer().writeByte(0);
                        mm.writer().writeInt(-1);
                        mm.writer().writeShort(-1);
                        mm.writer().writeByte(1);
                        mm.writer().writeByte(p.pet_di_buon.speed);
                        mm.writer().writeByte(0);
                        mm.writer().writeUTF(p.pet_di_buon.name);
                        mm.writer().writeLong(-11111);
                        mm.writer().writeByte(4);
                        for (int i = 0; i < gameMap.players.size(); i++) {
                            Player p0 = gameMap.players.get(i);
                            if (p0 != null) {
                                p0.conn.addmsg(mm);
                            }
                        }
                        mm.cleanup();
                    }
                }
                Message m22 = new Message(4);
                m22.writer().writeByte(1);
                m22.writer().writeShort(131);
                m22.writer().writeShort(p.pet_di_buon.objectId);
                m22.writer().writeShort(p.pet_di_buon.x);
                m22.writer().writeShort(p.pet_di_buon.y);
                m22.writer().writeByte(-1);
                for (int i = 0; i < gameMap.players.size(); i++) {
                    Player p0 = gameMap.players.get(i);
                    if (p0 != null) {
                        p0.conn.addmsg(m22);
                    }
                }
                m22.cleanup();
            }
        }
    }

    public static void update_inside_player(GameMap gameMap, Message m, Player p) throws IOException {
        Message m4 = new Message(4);
        for (ev_he.MobCay temp : gameMap.mobEvens) {
            if ((Math.abs(temp.x - p.x) < 300 && Math.abs(temp.y - p.y) < 300)
                    || GameMap.is_map__load_board_player(gameMap.mapId)) {

                if (!p.other_mob_inside.containsKey((int) temp.index)) {
                    p.other_mob_inside.put((int) temp.index, true);
                }
                if (!p.other_mob_inside_update.containsKey((int) temp.index)) {
                    p.other_mob_inside_update.put((int) temp.index, false);
                }
                if (p.other_mob_inside.get((int) temp.index)) {
                    temp.SendMob(p.conn);
                    p.other_mob_inside.replace((int) temp.index, true, false);
                }
            }
        }
        for (MobInMap temp : gameMap.Boss_entrys) {
            if (temp.isdie) {
                continue;
            }
            if ((Math.abs(temp.x - p.x) < 200 && Math.abs(temp.y - p.y) < 200)
                    || GameMap.is_map__load_board_player(gameMap.mapId)) {
                if (!temp.list_fight.contains(p)
                        && ((temp.template.mob_id == 151 || temp.template.mob_id == 152 || temp.template.mob_id == 154)
                        || temp.isBoss()
                        || ((Math.abs(temp.x - p.x) < 50) && (Math.abs(temp.y - p.y) < 50)))) {
                    temp.list_fight.add(p);
                }
                if (!p.other_mob_inside.containsKey(temp.objectId)) {
                    p.other_mob_inside.put(temp.objectId, true);
                }
                if (!p.other_mob_inside_update.containsKey(temp.objectId)) {
                    p.other_mob_inside_update.put(temp.objectId, false);
                }
                if (p.other_mob_inside.get(temp.objectId)) {
                    m4.writer().writeByte(1);
                    m4.writer().writeShort(temp.template.mob_id);
                    m4.writer().writeShort(temp.objectId);
                    m4.writer().writeShort(temp.x);
                    m4.writer().writeShort(temp.y);
                    m4.writer().writeByte(-1);
                    p.other_mob_inside.replace(temp.objectId, true, false);
                } else if (p.other_mob_inside_update.get(temp.objectId)) {
                    //
                    gameMap.BossIn4(p.conn, temp.objectId);
                    // Service.mob_in4(p, temp.index);
                    p.other_mob_inside_update.replace(temp.objectId, true, false);
                }
            } else if (p.other_mob_inside_update.containsKey(temp.objectId)
                    && !p.other_mob_inside_update.get(temp.objectId)) {
                p.other_mob_inside_update.replace(temp.objectId, false, true);
            }
        }

        for (MobInMap temp : gameMap.mobs) {
            if (temp.isdie && !(temp.template.mob_id >= 89 && temp.template.mob_id <= 92)) {
                continue;
            }
            if ((Math.abs(temp.x - p.x) < 200 && Math.abs(temp.y - p.y) < 200)
                    || GameMap.is_map__load_board_player(gameMap.mapId)) {
                if (!temp.list_fight.contains(p)
                        && ((temp.template.mob_id == 151 || temp.template.mob_id == 152 || temp.template.mob_id == 154)
                        || temp.isBoss()
                        || ((Math.abs(temp.x - p.x) < 50) && (Math.abs(temp.y - p.y) < 50)))) {
                    temp.list_fight.add(p);
                }
                if (!p.other_mob_inside.containsKey(temp.objectId)) {
                    p.other_mob_inside.put(temp.objectId, true);
                }
                if (!p.other_mob_inside_update.containsKey(temp.objectId)) {
                    p.other_mob_inside_update.put(temp.objectId, false);
                }
                if (p.other_mob_inside.get(temp.objectId)) {
                    m4.writer().writeByte(1);
                    m4.writer().writeShort(temp.template.mob_id);
                    m4.writer().writeShort(temp.objectId);
                    m4.writer().writeShort(temp.x);
                    m4.writer().writeShort(temp.y);
                    m4.writer().writeByte(-1);
                    p.other_mob_inside.replace(temp.objectId, true, false);
                } else if (p.other_mob_inside_update.get(temp.objectId)) {
                    //
                    Service.mob_in4(p, temp.objectId);
                    p.other_mob_inside_update.replace(temp.objectId, true, false);
                }
            } else if (p.other_mob_inside_update.containsKey(temp.objectId)
                    && !p.other_mob_inside_update.get(temp.objectId)) {
                p.other_mob_inside_update.replace(temp.objectId, false, true);
            }
        }
        //
        long _time = System.currentTimeMillis();
        boolean isth = p.getEffectMedal(StrucEff.TangHinh) != null;
        for (int i = 0; i < gameMap.players.size() && !isth; i++) {
            Player p0 = gameMap.players.get(i);
            if (p0.objectId == p.objectId) {
                continue;
            }
            if ((Math.abs(p0.x - p.x) < 200 && Math.abs(p0.y - p.y) < 200)
                    || GameMap.is_map__load_board_player(gameMap.mapId)) {
                if (!p.other_player_inside.containsKey(p0.objectId)) {
                    p.other_player_inside.put(p0.objectId, true);
                }
                p0.conn.addmsg(m);
                if (p.other_player_inside.get(p0.objectId)) {
                    m4.writer().writeByte(0);
                    m4.writer().writeShort(0);
                    m4.writer().writeShort(p0.objectId);
                    m4.writer().writeShort(p0.x);
                    m4.writer().writeShort(p0.y);
                    m4.writer().writeByte(-1);
                    //
                    p.other_player_inside.replace(p0.objectId, true, false);
                }
            } else if (p.other_player_inside.containsKey(p0.objectId)) {
                Message m3 = new Message(8);
                m3.writer().writeShort(p.objectId);
                p0.conn.addmsg(m3);
                m3.cleanup();
                m3 = new Message(8);
                m3.writer().writeShort(p0.objectId);
                p.conn.addmsg(m3);
                m3.cleanup();
                p.other_player_inside.remove(p0.objectId);
            }
        }
        if (m4.writer().size() > 0) {
            p.conn.addmsg(m4);
        }
        m4.cleanup();
    }

    public static MobInMap get_mob_by_index(GameMap gameMap, int n) {
        if (gameMap != null) {
            for (MobInMap m : gameMap.mobs) {
                if (m.objectId == n) {
                    return m;
                }
            }
        }
        return null;
    }


    public static void mob_fire(GameMap gameMap, MobInMap mob, Player p_target, int dame) throws IOException {
        mob_fire(gameMap, mob, p_target, dame, null);
    }

    /**
     * Kirim efek visual "meteor jatuh" (skill id 6, sama seperti serangan mob
     * ke objek Thiên thạch di Dungeon.mob_act()) ke target di map manapun.
     *
     * FIX: versi lama menembak PLAYER sebagai target sambil menulis byte-tipe
     * "0" (konvensi target-player biasa, lihat mob_fire()). Padahal di
     * Dungeon.mob_act() — satu-satunya tempat animasi skill id 6 ini terbukti
     * jalan — byte-tipe itu SELALU "2" dan targetnya SELALU objek struktur
     * (index tetap 32001, "Thiên thạch"), bukan player. Kombinasi byte-tipe=0
     * + target=player + skill=6 itu kombinasi yang tidak pernah dikirim di
     * kode asli, jadi klien diam-diam menolaknya (tidak ada animasi sama
     * sekali). Perbaikannya: ikuti persis pola aslinya — spawn objek struktur
     * dekoratif dulu di posisi target, baru serang STRUKTUR itu (bukan
     * player) dengan byte-tipe=2, persis seperti Dungeon.mob_act().
     *
     * Catatan: animasi ini butuh ATTACKER yang valid & benar-benar ada di map
     * (client menghitung origin animasi dari posisi attacker). Map peaceful/kota
     * (kayak map 1) biasanya tidak punya mob sama sekali, jadi kita spawn mob
     * dummy sementara sebagai sumber animasi, lalu despawn lagi setelahnya.
     * Murni visual untuk testing GM — tidak memotong HP siapa pun sungguhan.
     */
    public static void send_meteor_test_effect(GameMap gameMap, Player target, int dame) throws IOException {
        if (MobTemplate.entrys.isEmpty()) {
            return;
        }
        MobInMap dummy = new MobInMap();
        dummy.template = MobTemplate.entrys.get(0);
        dummy.objectId = 9000 + Util.random(0, 999); // range aman, jauh dari objectId asli
        dummy.x = target.x;
        dummy.y = (short) (target.y - 100); // taruh agak di atas target biar kerasa "jatuh dari langit"
        dummy.map_id = gameMap.mapId;
        dummy.zone_id = gameMap.zoneId;
        dummy.maxHp = 1;
        dummy.hp = 1;

        // ID objek struktur dekoratif yang jadi "sasaran" meteor — beda range
        // dari index 32001 yang dipakai Dungeon 48 asli, biar tidak bentrok
        // kalau kebetulan ada player yang lagi di dungeon itu juga.
        int structId = 32_500 + Util.random(0, 499);

        // 1. Spawn dummy ke client (sumber animasi / attacker)
        Message spawn = new Message(4);
        spawn.writer().writeByte(1);
        spawn.writer().writeShort(dummy.template.mob_id);
        spawn.writer().writeShort(dummy.objectId);
        spawn.writer().writeShort(dummy.x);
        spawn.writer().writeShort(dummy.y);
        spawn.writer().writeByte(-1);
        sendMsgPlayerInside(gameMap, target, spawn, true);
        spawn.cleanup();

        // 2. Spawn objek struktur dekoratif di posisi target — inilah yang akan
        //    "ditabrak" meteor (format sama seperti spawn "index thien thach"
        //    di Dungeon.send_map_data: type=2).
        Message spawnStruct = new Message(4);
        spawnStruct.writer().writeByte(2);
        spawnStruct.writer().writeShort(0);
        spawnStruct.writer().writeShort(structId);
        spawnStruct.writer().writeShort(target.x);
        spawnStruct.writer().writeShort(target.y);
        spawnStruct.writer().writeByte(-1);
        sendMsgPlayerInside(gameMap, target, spawnStruct, true);
        spawnStruct.cleanup();

        // 3. Kirim animasi serang (id skill 6 -> meteor jatuh) dari dummy ke
        //    STRUKTUR (bukan player), byte-tipe=2 — persis pola Dungeon.mob_act().
        Message atk = new Message(10);
        atk.writer().writeByte(1);
        atk.writer().writeShort(dummy.objectId);
        atk.writer().writeInt(dummy.hp);
        atk.writer().writeByte(2);
        atk.writer().writeByte(1);
        atk.writer().writeShort(structId);
        atk.writer().writeInt(dame); // dame yang ditampilkan (visual saja)
        atk.writer().writeInt(Math.max(0, 999_999 - dame)); // HP struktur (visual saja, tidak nyata)
        atk.writer().writeByte(6); // id skill mob -> animasi meteor jatuh
        atk.writer().writeByte(0);
        sendMsgPlayerInside(gameMap, target, atk, true);
        atk.cleanup();

        // 4. Despawn dummy & struktur setelah animasi kelar (delay singkat,
        //    tidak blocking thread map)
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                Message despawnMob = new Message(8);
                despawnMob.writer().writeShort(dummy.objectId);
                sendMsgPlayerInside(gameMap, target, despawnMob, true);
                despawnMob.cleanup();

                Message despawnStruct = new Message(8);
                despawnStruct.writer().writeShort(structId);
                sendMsgPlayerInside(gameMap, target, despawnStruct, true);
                despawnStruct.cleanup();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Efek perayaan untuk SEMUA player yang online di SEMUA map server (bukan
     * cuma map tempat kejadian). Dipakai saat kristal/meteor kemerdekaan
     * berhasil diselamatkan di Persimpangan Kematian, supaya semua player —
     * di manapun mereka berada, termasuk di desa (map 1) — ikut lihat momen
     * kemenangannya.
     *
     * Pakai model "Meteor Kemerdekaan" yang sama seperti yang sudah dipakai
     * KristalEvent di Persimpangan (graphic id sama, lihat
     * KristalEvent.METEOR_GRAPHIC_ID) — bukan animasi tabrakan mob — supaya
     * player langsung mengenali objeknya, dan biar tidak bergantung ke
     * MobTemplate/dummy mob yang dipakai efek testing GM.
     *
     * Murni visual, tidak menyentuh HP siapa pun. Kalau pengiriman ke satu
     * player gagal (mis. koneksi putus di tengah jalan), player lain tetap
     * lanjut menerima efeknya.
     */
    public static void broadcastMeteorCelebrationAllMaps() {
        for (GameMap[] zone : GameMap.entrys) {
            if (zone == null) continue;
            for (GameMap gm : zone) {
                if (gm == null) continue;
                for (int i = 0; i < gm.players.size(); i++) {
                    Player p = gm.players.get(i);
                    if (p == null || p.conn == null || !p.conn.connected) continue;
                    sendCelebrationMeteorVisual(p);
                }
            }
        }
    }

    /**
     * Penyimpanan posisi objek visual "Meteor Kemerdekaan" perayaan yang lagi
     * aktif, key = objectId (visualId). Dibutuhkan karena client TIDAK
     * langsung render objek dari paket spawn (Message 4) doang — begitu lihat
     * objek "type 2" yang belum dikenal, client selalu balik nanya info-nya
     * lewat request Message(-44) berisi objectId. Server harus siap jawab
     * balik (lihat MessageHandler case -44), makanya posisi tiap objek yang
     * lagi aktif perlu disimpan sementara di sini.
     */
    private static final java.util.concurrent.ConcurrentHashMap<Integer, short[]> celebrationMeteorPos = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Kirim satu objek visual dekoratif "Meteor Kemerdekaan" (graphic id 25,
     * sama seperti KristalEvent.METEOR_GRAPHIC_ID) ke SATU player, muncul
     * sebentar di posisinya sendiri lalu hilang. Dipakai oleh
     * broadcastMeteorCelebrationAllMaps(). Gagal untuk 1 player tidak boleh
     * melempar exception ke pemanggil (di-catch di dalam).
     *
     * FIX (root cause "efek gak nongol di map selain Persimpangan/Dungeon"):
     * paket spawn (Message 4, type 2) doang TIDAK CUKUP. Begitu client lihat
     * objek type=2 yang belum ia kenal, ia SELALU kirim balik request
     * Message(-44) berisi objectId minta info (nama/graphic/hp). Selama ini
     * balasan -44 di MessageHandler CUMA di-hardcode untuk map 48 (Dungeon)
     * dan map 117 (Persimpangan) — di map lain (termasuk map 1) request itu
     * tidak dijawab sama sekali sehingga client gak pernah render objeknya,
     * walau paket spawn-nya sendiri sukses terkirim tanpa error apapun. Fix-nya
     * ada 2 bagian: (1) simpan posisi objek di celebrationMeteorPos di sini,
     * (2) MessageHandler case -44 sekarang mengenali range id perayaan ini di
     * MAP MANAPUN dan menjawabnya lewat sendCelebrationMeteorInfo() di bawah.
     */
    private static void sendCelebrationMeteorVisual(Player p) {
        try {
            // Range id khusus perayaan — beda dari KristalEvent.METEOR_VISUAL_ID
            // (31000, dipakai kristal asli di Persimpangan) biar tidak pernah bentrok.
            int visualId = 31_500 + Util.random(0, 499);
            celebrationMeteorPos.put(visualId, new short[]{p.x, p.y});

            // 1. Spawn objek (type=2, sama seperti KristalEvent.sendMeteorVisual)
            Message spawn = new Message(4);
            spawn.writer().writeByte(2);
            spawn.writer().writeShort(0);
            spawn.writer().writeShort(visualId);
            spawn.writer().writeShort(p.x);
            spawn.writer().writeShort(p.y);
            spawn.writer().writeByte(-1);
            p.conn.addmsg(spawn);
            spawn.cleanup();

            // 2. Kirim info duluan juga (proaktif) — supaya kalau kebetulan
            //    render tanpa perlu nunggu request client, tetap langsung ada.
            //    Tapi yang benar-benar nentuin objeknya nongol adalah balasan
            //    ke request client di MessageHandler (lihat catatan di atas).
            sendCelebrationMeteorInfo(p.conn, visualId);

            // 3. Despawn setelah 2 detik
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    Message despawn = new Message(8);
                    despawn.writer().writeShort(visualId);
                    p.conn.addmsg(despawn);
                    despawn.cleanup();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    celebrationMeteorPos.remove(visualId);
                }
            }).start();
        } catch (Exception e) {
            // 1 player gagal tidak boleh menghentikan broadcast ke player lain
        }
    }

    /**
     * Jawab request info (Message -44) dari client untuk objek visual
     * "Meteor Kemerdekaan" perayaan. Dipanggil dari MessageHandler case -44,
     * berlaku di MAP MANAPUN (tidak di-hardcode ke map tertentu seperti
     * Dungeon/Persimpangan). Kalau objectId-nya bukan/sudah bukan objek
     * perayaan yang aktif (mis. sudah despawn), diam saja (tidak ada objek
     * untuk dijawab).
     */
    public static void sendCelebrationMeteorInfo(Session conn, int visualId) throws IOException {
        short[] pos = celebrationMeteorPos.get(visualId);
        if (pos == null) return;

        Message info = new Message(-44);
        info.writer().writeShort(visualId);
        info.writer().writeUTF("Meteor Kemerdekaan");
        info.writer().writeInt(1);
        info.writer().writeInt(1);
        info.writer().writeShort(0);
        info.writer().writeShort(pos[0]);
        info.writer().writeShort(pos[1]);
        info.writer().writeByte(2);
        info.writer().writeByte(2);
        info.writer().writeUTF("");
        info.writer().writeShort((short) 25); // KristalEvent.METEOR_GRAPHIC_ID
        info.writer().writeByte(1);
        info.writer().writeShort(17);
        conn.addmsg(info);
        info.cleanup();
    }

    /** Range objectId yang dipakai objek visual perayaan "Meteor Kemerdekaan" (lihat sendCelebrationMeteorVisual). */
    public static boolean isCelebrationMeteorId(int objectId) {
        return objectId >= 31_500 && objectId < 32_000;
    }

    public static void mob_fire(GameMap gameMap, MobInMap mob, Player p_target, int dame, List<Eff_TextFire> listEf) throws IOException {
        if (mob.template.mob_id >= 89 && mob.template.mob_id <= 92) {
            return;
        }
        Message m = new Message(10);
        m.writer().writeByte(1);
        m.writer().writeShort(mob.objectId);
        m.writer().writeInt(mob.hp);
        m.writer().writeByte(0);
        m.writer().writeByte(1);
        m.writer().writeShort(p_target.objectId);
        m.writer().writeInt(dame); // dame mob
        m.writer().writeInt(p_target.hp);
        m.writer().writeByte(2); // id skill mob
        // Kirim efek tambahan (misal refleksi) jika ada
        if (listEf != null && !listEf.isEmpty()) {
            m.writer().writeByte(listEf.size());
            for (Eff_TextFire ef : listEf) {
                if (ef != null) {
                    m.writer().writeByte(ef.type);
                    m.writer().writeInt(ef.dame);
                }
            }
        } else {
            m.writer().writeByte(0);
        }
        MapService.sendMsgPlayerInside(gameMap, p_target, m, true);
        m.cleanup();
        // Jika dame=0 berarti player dodge → tidak ada stun/efek lanjutan
        if (dame <= 0) {
            return;
        }
        // boss skill
        int percent_stun = 0;
        int time_stun = 0;
        if (mob.isBoss()) {
            int sizelv = p_target.level - mob.level;
            if (mob.template.mob_id == 174 || mob.level >= 120) {
                sizelv = 0;
            } else if (GameMap.is_map_cant_save_site(mob.map_id)) {
                sizelv = 0;
            }
            switch (sizelv) {
                case 5:
                case 4:
                case 3:
                case 2:
                case 1:
                case 0: {
                    percent_stun = 15;
                    time_stun = 1;
                    break;
                }
                default: {
                    dame *= 50;
                    percent_stun = 20;
                    time_stun = 5;
                    break;
                }
            }
        }
        if (p_target.getEffectMedal(StrucEff.NgocKhaiHoan) != null) {
            return;
        }

        if (mob.isBoss() && (percent_stun > Util.random(0, 100))) {
            if (!p_target.isStunes(false)) {
                MapService.add_eff_stun(gameMap, mob, p_target, time_stun, Util.nextInt(4, 7));
            }
        }
    }
    // public static void mob_fire(Map map, Mob_in_map mob, Player p_target) throws
    // IOException {
    // long _time = System.currentTimeMillis();
    // if (!mob.isdie && p_target != null) {
    // // player tàng hình
    // if (p_target.time_TangHinh > _time) {
    // return;
    // }
    // short[] pr_Kham = p_target.body.get_param_kham();
    // if (mob.is_boss) {
    // if (p_target.kham.idAtk_KH == mob.index && p_target.kham.time_KhaiHoan <
    // _time) {
    // p_target.kham.CountAtk_KH++;
    // } else {
    // p_target.kham.idAtk_KH = mob.index;
    // p_target.kham.CountAtk_KH = 1;
    // }
    // }
    // if (pr_Kham[1] > 0 && p_target.kham.CountAtk_KH >= pr_Kham[1]) {
    // p_target.kham.idAtk_KH = 0;
    // p_target.kham.CountAtk_KH = 0;
    // p_target.kham.time_KhaiHoan = System.currentTimeMillis() + 3000;
    // Eff_special_skill.send_eff_kham(p_target, 19, 3000);
    // }
    // // dame
    // int dmob = Util.random((int) (mob.dame * 0.95), (int) (mob.dame * 1.05));

    /// / System.out.println("map.MapService.mob_fire()"+dmob);
    // if (mob.level > 30 && mob.level <= 50) {
    // dmob = (dmob * 13) / 10;
    // } else if (mob.level > 50 && mob.level <= 70) {
    // dmob = (dmob * 16) / 10;
    // } else if (mob.level > 70 && mob.level <= 100) {
    // dmob = (dmob * 19) / 10;
    // } else if (mob.level > 100 && mob.level <= 600) {
    // dmob = (dmob * 21) / 10;
    // }
    // if (mob.is_boss) {
    // dmob = (int) (dmob * mob.level * 0.03);
    // }
    // if (mob.color_name != 0 && (mob.template.mob_id < 89 || mob.template.mob_id >
    // 92)) {
    // dmob *= 2;
    // }
    // if (map.time_BuffHouse > _time && mob.template.mob_id >= 89 &&
    // mob.template.mob_id <= 92) {
    // dmob *= 2;
    // }
    // // def enemy
    // int def = p_target.body.get_def();
    // EffTemplate ef = p_target.get_eff(15);
    // if (ef != null) {
    // def += (int) (def * (ef.param * 0.0001));
    // }
    // dmob -= def;
    // if (dmob <= 0) {
    // dmob = 1;
    // }
    //
    // if (p_target.getlevelpercent() < 0) {
    // dmob *= 2;
    // }
    // int percent_stun = 0;
    // int time_stun = 0;
    // if (mob.is_boss) {
    // int sizelv = p_target.level - mob.level;
    // if (mob.template.mob_id == 174 || mob.level >= 120) {
    // sizelv = 0;
    // } else if (Map.is_map_cant_save_site(mob.map_id)) {
    // sizelv = 0;
    // }
    // switch (sizelv) {
    // case 5:
    // case 4:
    // case 3:
    // case 2:
    // case 1:
    // case 0: {
    // percent_stun = 15;
    // time_stun = 2;
    // break;
    // }
    // default: {
    // dmob *= 50;
    // percent_stun = 50;
    // time_stun = 5;
    // break;
    // }
    // }
    // }
    // if (p_target.kham.time_KhaiHoan > _time) {
    // percent_stun = 0;
    // }
    //
    // if (p_target.time_KhienMaThuat > _time && dmob > (int) (0.1 * p_target.hp))
    // //player đang có khiên ma thuật
    // {
    // dmob /= 2;
    // }
    // p_target.hp -= dmob;
    // if (p_target.hp <= 0) {
    // MapService.die_by_mob(map, p_target, mob);
    // }
    // Message m = new Message(10);
    // m.writer().writeByte(1);
    // m.writer().writeShort(mob.index);
    // m.writer().writeInt(mob.hp);
    // m.writer().writeByte(0);
    // m.writer().writeByte(1);
    // m.writer().writeShort(p_target.id);
    // m.writer().writeInt(dmob); // dame mob
    // m.writer().writeInt(p_target.hp);
    // m.writer().writeByte(2); // id skill mob
    // m.writer().writeByte(0);
    // MapService.send_msg_player_inside(map, p_target, m, true);
    // m.cleanup();
    // // boss skill
    // if (mob.is_boss && (percent_stun > Util.random(0, 100))) {
    // int[] list = new int[]{7, 4, 5, 6};
    // EffTemplate ef1 = p_target.get_eff(-121);
    // EffTemplate ef2 = p_target.get_eff(-122);
    // EffTemplate ef3 = p_target.get_eff(-123);
    // EffTemplate ef4 = p_target.get_eff(-124);
    // if (ef1 == null && ef2 == null && ef3 == null && ef4 == null) {
    // add_eff_stun(map, null, p_target, time_stun, list[Util.random(0,
    // list.length)], mob.index);
    // }
    // }
    // }
    // }
    public static void add_eff_stun(GameMap gameMap, MainObject mainObj, MainObject focus, int time, int type)
            throws IOException {
        if (focus == null) {
            return;
        }
        Message m = new Message(75);
        m.writer().writeByte(type);
        m.writer().writeByte(focus.getTypeObject());
        m.writer().writeShort(focus.objectId);
        m.writer().writeShort(time);
        m.writer().writeByte(0);
        m.writer().writeShort(mainObj.objectId);
        MapService.sendMsgPlayerInside(gameMap, focus, m, true);
        m.cleanup();
        //
        int time_ = 1000 * time;
        switch (type) {
            case 7: {
                focus.addEffectDefault(-124, 1000, time_);
                break;
            }
            case 4: {
                focus.addEffectDefault(-123, 1000, time_);
                break;
            }
            case 5: {
                focus.addEffectDefault(-122, 1000, time_);
                break;
            }
            case 6: {
                focus.addEffectDefault(-121, 1000, time_);
                break;
            }
        }
    }

    public static void changeFlag(GameMap gameMap, Player p_target, int type) throws IOException {
        if ((gameMap.zoneId == gameMap.maxzone && type != -1 && p_target.item.wear[11] != null
                && p_target.item.wear[11].id != 3593)
                || gameMap.isMapLoiDai() || gameMap.isCastleSiegeMap()) {
            Service.send_notice_box(p_target.conn, "Tidak bisa dilakukan!");
            return;
        }
        if (GameMap.isBattlefieldMap(gameMap.mapId)) {
            return;
        }
        if ((GameMap.isSummonMap(gameMap, true) && Manager.gI().mine.isRunning())) {
            Service.send_notice_box(p_target.conn, "Anda tidak bisa melakukan tindakan ini!");
            return;
        }
        if (gameMap.mapId != 52 && gameMap.zoneId == gameMap.maxzone) {
            if (p_target.item.wear[11] != null && p_target.item.wear[11].id != 3593) {
                type = 11;
            } else if (p_target.item.wear[11] != null && p_target.item.wear[11].id != 3599) {
                type = 12;
            }
        }
        Message m = new Message(42);
        m.writer().writeShort(p_target.objectId);
        m.writer().writeByte(type);
        p_target.typepk = (byte) type;
        MapService.sendMsgPlayerInside(gameMap, p_target, m, true);
        m.cleanup();
    }

    @SuppressWarnings("unused")
    public static void buff_skill(GameMap gameMap, Session conn, Message m2) throws IOException {
        if (m2.reader().available() < 3) {
            return; // silently drop malformed packet
        } // code baru
        byte type = m2.reader().readByte();
        byte tem = m2.reader().readByte();
        byte size_buff = m2.reader().readByte();
        if (m2.reader().available() < size_buff * 2) {
            return;
        } // code baru
        // System.out.println(type);
        // System.out.println(tem);
        // System.out.println(size_buff);
        MapService.add_eff_skill(gameMap, conn.p, null, type);
        for (int i = 0; i < size_buff; i++) {
            int id = Short.toUnsignedInt(m2.reader().readShort());
            // System.out.println(id);
        }
    }

    public static void add_eff_skill(GameMap gameMap, Player p, Player p2, byte index_skill) throws IOException {
        int sk_point = p.body.getSkillPoint(index_skill);
        if (sk_point < 1) {
            return;
        }
        int time_buff = p.skills[index_skill].mLvSkill[sk_point - 1].timeBuff;
        int range = p.skills[index_skill].mLvSkill[sk_point - 1].range_lan;
        int n_target = p.skills[index_skill].mLvSkill[sk_point - 1].nTarget - 1;
        switch (p.clazz) {
            case 0: {
                if (index_skill == 18) {
                    p.add_EffDefault(23, 1000, 60_000);
                    Message m = new Message(75);
                    m.writer().writeByte(12);
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    m.writer().writeShort(60);
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    if (p.conn != null) p.conn.addmsg(m);
                    m.cleanup();
                    MapService.send_eff_other(p.map, p, 23);
                    if (!p.modeBot) Service.sendMainCharInfo(p);
                } else if (index_skill == 13) {
                    byte[] id_sk = new byte[]{15, 35};
                    int[] param_sk = new int[]{p.get_pramskill_byid(index_skill, id_sk[0]),
                            p.get_pramskill_byid(index_skill, id_sk[1])};
                    for (int i = 0; i < p.map.players.size(); i++) {
                        if (n_target < 1) {
                            continue;
                        }
                        Player p0 = p.map.players.get(i);
                        if (p0 != null && p0.objectId != p.objectId && !p0.isdie && Math.abs(p0.x - p.x) < range
                                && Math.abs(p0.y - p.y) < range && p0.typepk != 0 && p.typepk == p0.typepk) {
                            for (int j = 0; j < id_sk.length; j++) {
                                p0.add_EffDefault(id_sk[j], param_sk[j], time_buff);
                            }
                            MapService.add_eff_skill_msg(gameMap, p, p0, index_skill, time_buff, id_sk, param_sk);
                            n_target--;
                        }
                    }
                    for (int j = 0; j < id_sk.length; j++) {
                        p.add_EffDefault(id_sk[j], param_sk[j], time_buff);
                    }
                    MapService.add_eff_skill_msg(gameMap, p, p, index_skill, time_buff, id_sk, param_sk);
                } else if (index_skill == 14) {
                    byte[] id_sk = new byte[]{33, 9, 7};
                    int[] param_sk = new int[]{p.get_pramskill_byid(index_skill, id_sk[0]),
                            p.get_pramskill_byid(index_skill, id_sk[1]), p.get_pramskill_byid(index_skill, id_sk[2])};
                    for (int i = 0; i < p.map.players.size(); i++) {
                        if (n_target < 1) {
                            continue;
                        }
                        Player p0 = p.map.players.get(i);
                        if (p0 != null && p0.objectId != p.objectId && !p0.isdie && Math.abs(p0.x - p.x) < range
                                && Math.abs(p0.y - p.y) < range && p0.typepk != 0 && p.typepk == p0.typepk) {
                            for (int j = 0; j < id_sk.length; j++) {
                                p0.add_EffDefault(id_sk[j], param_sk[j], time_buff);
                            }
                            MapService.add_eff_skill_msg(gameMap, p, p0, index_skill, time_buff, id_sk, param_sk);
                            n_target--;
                        }
                    }
                    for (int j = 0; j < id_sk.length; j++) {
                        p.add_EffDefault(id_sk[j], param_sk[j], time_buff);
                    }
                    MapService.add_eff_skill_msg(gameMap, p, p, index_skill, time_buff, id_sk, param_sk);
                } else if (index_skill == 17 && p2 != null) {
                    MapService.add_eff_stun(gameMap, p, p2, 5, 7);
                }
                break;
            }
            case 1: {
                if (index_skill == 18) {
                    p.add_EffDefault(24, 1000, 60_000);
                    Message m = new Message(75);
                    m.writer().writeByte(12);
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    m.writer().writeShort(60);
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    if (p.conn != null) p.conn.addmsg(m);
                    m.cleanup();
                    MapService.send_eff_other(gameMap, p, 24);
                    if (!p.modeBot) Service.sendMainCharInfo(p);
                } else if (index_skill == 13) {
                    byte[] id_sk = new byte[]{15, 34};
                    int[] param_sk = new int[]{p.get_pramskill_byid(index_skill, id_sk[0]),
                            p.get_pramskill_byid(index_skill, id_sk[1])};
                    for (int i = 0; i < p.map.players.size(); i++) {
                        if (n_target < 1) {
                            continue;
                        }
                        Player p0 = p.map.players.get(i);
                        if (p0 != null && p0.objectId != p.objectId && !p0.isdie && Math.abs(p0.x - p.x) < range
                                && Math.abs(p0.y - p.y) < range && p0.typepk != 0 && p.typepk == p0.typepk) {
                            for (int j = 0; j < id_sk.length; j++) {
                                p0.add_EffDefault(id_sk[j], param_sk[j], time_buff);
                            }
                            MapService.add_eff_skill_msg(gameMap, p, p0, index_skill, time_buff, id_sk, param_sk);
                            n_target--;
                        }
                    }
                    for (int j = 0; j < id_sk.length; j++) {
                        p.add_EffDefault(id_sk[j], param_sk[j], time_buff);
                    }
                    MapService.add_eff_skill_msg(gameMap, p, p, index_skill, time_buff, id_sk, param_sk);
                } else if (index_skill == 14) {
                    byte[] id_sk = new byte[]{33, 11, 7};
                    int[] param_sk = new int[]{p.get_pramskill_byid(index_skill, id_sk[0]),
                            p.get_pramskill_byid(index_skill, id_sk[1]), p.get_pramskill_byid(index_skill, id_sk[2])};
                    for (int i = 0; i < p.map.players.size(); i++) {
                        if (n_target < 1) {
                            continue;
                        }
                        Player p0 = p.map.players.get(i);
                        if (p0 != null && p0.objectId != p.objectId && !p0.isdie && Math.abs(p0.x - p.x) < range
                                && Math.abs(p0.y - p.y) < range && p0.typepk != 0 && p.typepk == p0.typepk) {
                            for (int j = 0; j < id_sk.length; j++) {
                                p0.add_EffDefault(id_sk[j], param_sk[j], time_buff);
                            }
                            MapService.add_eff_skill_msg(gameMap, p, p0, index_skill, time_buff, id_sk, param_sk);
                            n_target--;
                        }
                    }
                    for (int j = 0; j < id_sk.length; j++) {
                        p.add_EffDefault(id_sk[j], param_sk[j], time_buff);
                    }
                    MapService.add_eff_skill_msg(gameMap, p, p, index_skill, time_buff, id_sk, param_sk);
                } else if (index_skill == 17 && p2 != null) {
                    MapService.add_eff_stun(gameMap, p, p2, 10, 4);
                }
                break;
            }
            case 2: {
                if (index_skill == 18) {
                    p.add_EffDefault(52, 1000, 60_000);
                    Message m = new Message(75);
                    m.writer().writeByte(12);
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    m.writer().writeShort(60);
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    if (p.conn != null) p.conn.addmsg(m);
                    m.cleanup();
                    MapService.send_eff_other(gameMap, p, 52);
                } else if (index_skill == 13) {
                    byte[] id_sk = new byte[]{15, 35};
                    int[] param_sk = new int[]{p.get_pramskill_byid(index_skill, id_sk[0]),
                            p.get_pramskill_byid(index_skill, id_sk[1])};
                    for (int i = 0; i < p.map.players.size(); i++) {
                        if (n_target < 1) {
                            continue;
                        }
                        Player p0 = p.map.players.get(i);
                        if (p0 != null && p0.objectId != p.objectId && !p0.isdie && Math.abs(p0.x - p.x) < range
                                && Math.abs(p0.y - p.y) < range && p0.typepk != 0 && p.typepk == p0.typepk) {
                            for (int j = 0; j < id_sk.length; j++) {
                                p0.add_EffDefault(id_sk[j], param_sk[j], time_buff);
                            }
                            MapService.add_eff_skill_msg(gameMap, p, p0, index_skill, time_buff, id_sk, param_sk);
                            n_target--;
                        }
                    }
                    for (int j = 0; j < id_sk.length; j++) {
                        p.add_EffDefault(id_sk[j], param_sk[j], time_buff);
                    }
                    MapService.add_eff_skill_msg(gameMap, p, p, index_skill, time_buff, id_sk, param_sk);
                } else if (index_skill == 14) {
                    byte[] id_sk = new byte[]{36, 8, 7};
                    int[] param_sk = new int[]{p.get_pramskill_byid(index_skill, id_sk[0]),
                            p.get_pramskill_byid(index_skill, id_sk[1]), p.get_pramskill_byid(index_skill, id_sk[2])};
                    for (int i = 0; i < p.map.players.size(); i++) {
                        if (n_target < 1) {
                            continue;
                        }
                        Player p0 = p.map.players.get(i);
                        if (p0 != null && p0.objectId != p.objectId && !p0.isdie && Math.abs(p0.x - p.x) < range
                                && Math.abs(p0.y - p.y) < range && p0.typepk != 0 && p.typepk == p0.typepk) {
                            for (int j = 0; j < id_sk.length; j++) {
                                p0.add_EffDefault(id_sk[j], param_sk[j], time_buff);
                            }
                            MapService.add_eff_skill_msg(gameMap, p, p0, index_skill, time_buff, id_sk, param_sk);
                            n_target--;
                        }
                    }
                    for (int j = 0; j < id_sk.length; j++) {
                        p.add_EffDefault(id_sk[j], param_sk[j], time_buff);
                    }
                    MapService.add_eff_skill_msg(gameMap, p, p, index_skill, time_buff, id_sk, param_sk);
                } else if (index_skill == 17 && p2 != null) {
                    MapService.add_eff_stun(gameMap, p, p2, 10, 5);
                }
                break;
            }
            case 3: {
                if (index_skill == 18) {
                    p.add_EffDefault(53, 1000, 60_000);
                    Message m = new Message(75);
                    m.writer().writeByte(12);
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    m.writer().writeShort(60);
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    if (p.conn != null) p.conn.addmsg(m);
                    m.cleanup();
                    MapService.send_eff_other(gameMap, p, 53);
                } else if (index_skill == 13) {
                    byte[] id_sk = new byte[]{15, 34};
                    int[] param_sk = new int[]{p.get_pramskill_byid(index_skill, id_sk[0]),
                            p.get_pramskill_byid(index_skill, id_sk[1])};
                    for (int i = 0; i < p.map.players.size(); i++) {
                        if (n_target < 1) {
                            continue;
                        }
                        Player p0 = p.map.players.get(i);
                        if (p0 != null && p0.objectId != p.objectId && !p0.isdie && Math.abs(p0.x - p.x) < range
                                && Math.abs(p0.y - p.y) < range && p0.typepk != 0 && p.typepk == p0.typepk) {
                            for (int j = 0; j < id_sk.length; j++) {
                                p0.add_EffDefault(id_sk[j], param_sk[j], time_buff);
                            }
                            MapService.add_eff_skill_msg(gameMap, p, p0, index_skill, time_buff, id_sk, param_sk);
                            n_target--;
                        }
                    }
                    for (int j = 0; j < id_sk.length; j++) {
                        p.add_EffDefault(id_sk[j], param_sk[j], time_buff);
                    }
                    MapService.add_eff_skill_msg(gameMap, p, p, index_skill, time_buff, id_sk, param_sk);
                } else if (index_skill == 14) {
                    byte[] id_sk = new byte[]{36, 10, 7};
                    int[] param_sk = new int[]{p.get_pramskill_byid(index_skill, id_sk[0]),
                            p.get_pramskill_byid(index_skill, id_sk[1]), p.get_pramskill_byid(index_skill, id_sk[2])};
                    for (int i = 0; i < p.map.players.size(); i++) {
                        if (n_target < 1) {
                            continue;
                        }
                        Player p0 = p.map.players.get(i);
                        if (p0 != null && p0.objectId != p.objectId && !p0.isdie && Math.abs(p0.x - p.x) < range
                                && Math.abs(p0.y - p.y) < range && p0.typepk != 0 && p.typepk == p0.typepk) {
                            for (int j = 0; j < id_sk.length; j++) {
                                p0.add_EffDefault(id_sk[j], param_sk[j], time_buff);
                            }
                            MapService.add_eff_skill_msg(gameMap, p, p0, index_skill, time_buff, id_sk, param_sk);
                            n_target--;
                        }
                    }
                    for (int j = 0; j < id_sk.length; j++) {
                        p.add_EffDefault(id_sk[j], param_sk[j], time_buff);
                    }
                    MapService.add_eff_skill_msg(gameMap, p, p, index_skill, time_buff, id_sk, param_sk);
                } else if (index_skill == 17 && p2 != null) {
                    MapService.add_eff_stun(gameMap, p, p2, 10, 6);
                }
                break;
            }
        }
    }

    private static void add_eff_skill_msg(GameMap gameMap, Player p, Player p0, byte index_skill, int time_buff, byte[] id_sk,
                                          int[] param_sk) throws IOException {
        int index_skill2 = 0;
        switch (p.clazz) {
            case 0: {
                index_skill2 = index_skill;
                break;
            }
            case 1: {
                if (index_skill == 13) {
                    index_skill2 = 30;
                } else {
                    index_skill2 = 31;
                }
                break;
            }
            case 2: {
                index_skill2 = index_skill;
                break;
            }
            case 3: {
                if (index_skill == 13) {
                    index_skill2 = 30;
                } else {
                    index_skill2 = 31;
                }
                break;
            }
        }
        Message m = new Message(40);
        m.writer().writeByte(1);
        m.writer().writeByte(1);
        m.writer().writeShort(p.objectId);
        m.writer().writeByte(index_skill);
        m.writer().writeInt(time_buff);
        m.writer().writeShort(p0.objectId);
        m.writer().writeByte(0);
        m.writer().writeByte(index_skill2);
        if (index_skill == 13) {
            int index = -1;
            m.writer().writeByte(id_sk.length + 1);
            for (int i = 0; i < id_sk.length; i++) {
                m.writer().writeByte(id_sk[i]);
                m.writer().writeInt(param_sk[i]);
                if (id_sk[i] == 15) {
                    index = i;
                }
            }
            int param;
            if (index == -1) {
                param = 0;
            } else {
                param = param_sk[index];
            }
            m.writer().writeByte(14);
            m.writer().writeInt(p0.body.getDefBase() * (param / 100) / 100);
        } else if (index_skill == 14) {
            int index = -1;
            int index1 = -1;
            int index2 = -1;
            int index3 = -1;
            int index4 = -1;
            m.writer().writeByte(id_sk.length + 5);
            for (int i = 0; i < id_sk.length; i++) {
                m.writer().writeByte(id_sk[i]);
                m.writer().writeInt(param_sk[i]);
                if (id_sk[i] == 7) {
                    index = i;
                }
                if (id_sk[i] == 8) {
                    index1 = i;
                }
                if (id_sk[i] == 9) {
                    index2 = i;
                }
                if (id_sk[i] == 10) {
                    index3 = i;
                }
                if (id_sk[i] == 11) {
                    index4 = i;
                }
            }
            int pr0, pr1, pr2, pr3, pr4;
            if (index == -1) {
                pr0 = 0;
            } else {
                pr0 = param_sk[index];
            }
            if (index1 == -1) {
                pr1 = 0;
            } else {
                pr1 = param_sk[index1];
            }
            if (index2 == -1) {
                pr2 = 0;
            } else {
                pr2 = param_sk[index2];
            }
            if (index3 == -1) {
                pr3 = 0;
            } else {
                pr3 = param_sk[index3];
            }
            if (index4 == -1) {
                pr4 = 0;
            } else {
                pr4 = param_sk[index4];
            }
            m.writer().writeByte(0);
            m.writer().writeInt((p0.body.getDameProp(0) * (pr0 / 100)) / 100);
            m.writer().writeByte(1);
            m.writer().writeInt((p0.body.getDameProp(1) * (pr1 / 100)) / 100);
            m.writer().writeByte(2);
            m.writer().writeInt((p0.body.getDameProp(2) * (pr2 / 100)) / 100);
            m.writer().writeByte(3);
            m.writer().writeInt((p0.body.getDameProp(3) * (pr3 / 100)) / 100);
            m.writer().writeByte(4);
            m.writer().writeInt((p0.body.getDameProp(4) * (pr4 / 100)) / 100);
        } else {
            m.writer().writeByte(0);
        }
        p0.conn.addmsg(m);
        m.cleanup();
        if (p0.objectId != p.objectId) {
            m = new Message(40);
            m.writer().writeByte(1);
            m.writer().writeByte(1);
            m.writer().writeShort(p.objectId);
            m.writer().writeByte(index_skill);
            m.writer().writeInt(time_buff);
            m.writer().writeShort(p0.objectId);
            m.writer().writeByte(0);
            m.writer().writeByte(index_skill2);
            m.writer().writeByte(0);
            p.conn.addmsg(m);
            m.cleanup();
        }
        //
        m = new Message(40);
        m.writer().writeByte(0);
        m.writer().writeByte(1);
        m.writer().writeShort(p.objectId);
        m.writer().writeByte(index_skill);
        m.writer().writeInt(time_buff);
        m.writer().writeShort(p0.objectId);
        m.writer().writeByte(0);
        m.writer().writeByte(index_skill2);
        if (index_skill == 13) {
            int index = -1;
            m.writer().writeByte(id_sk.length + 1);
            for (int i = 0; i < id_sk.length; i++) {
                m.writer().writeByte(id_sk[i]);
                m.writer().writeInt(param_sk[i]);
                if (id_sk[i] == 15) {
                    index = i;
                }
            }
            int param;
            if (index == -1) {
                param = 0;
            } else {
                param = param_sk[index];
            }
            m.writer().writeByte(14);
            m.writer().writeInt(p0.body.getDefBase() * (param / 100) / 100);
        } else if (index_skill == 14) {
            int index = -1;
            int index1 = -1;
            int index2 = -1;
            int index3 = -1;
            int index4 = -1;
            m.writer().writeByte(id_sk.length + 5);
            for (int i = 0; i < id_sk.length; i++) {
                m.writer().writeByte(id_sk[i]);
                m.writer().writeInt(param_sk[i]);
                if (id_sk[i] == 7) {
                    index = i;
                }
                if (id_sk[i] == 8) {
                    index1 = i;
                }
                if (id_sk[i] == 9) {
                    index2 = i;
                }
                if (id_sk[i] == 10) {
                    index3 = i;
                }
                if (id_sk[i] == 11) {
                    index4 = i;
                }
            }
            int pr0, pr1, pr2, pr3, pr4;
            if (index == -1) {
                pr0 = 0;
            } else {
                pr0 = param_sk[index];
            }
            if (index1 == -1) {
                pr1 = 0;
            } else {
                pr1 = param_sk[index1];
            }
            if (index2 == -1) {
                pr2 = 0;
            } else {
                pr2 = param_sk[index2];
            }
            if (index3 == -1) {
                pr3 = 0;
            } else {
                pr3 = param_sk[index3];
            }
            if (index4 == -1) {
                pr4 = 0;
            } else {
                pr4 = param_sk[index4];
            }
            m.writer().writeByte(0);
            m.writer().writeInt((p0.body.getDameProp(0) * (pr0 / 100)) / 100);
            m.writer().writeByte(1);
            m.writer().writeInt((p0.body.getDameProp(1) * (pr1 / 100)) / 100);
            m.writer().writeByte(2);
            m.writer().writeInt((p0.body.getDameProp(2) * (pr2 / 100)) / 100);
            m.writer().writeByte(3);
            m.writer().writeInt((p0.body.getDameProp(3) * (pr3 / 100)) / 100);
            m.writer().writeByte(4);
            m.writer().writeInt((p0.body.getDameProp(4) * (pr4 / 100)) / 100);
        } else {
            m.writer().writeByte(0);
        }
        p0.conn.addmsg(m);
        m.cleanup();
    }

    private static void send_eff_other(GameMap gameMap, Player p, int id) throws IOException {
        EffTemplate temp = p.getEffectDefault(id);
        if (temp != null) {
            switch (id) {
                case -121: {
                    Message m = new Message(75);
                    m.writer().writeByte(6);
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    long time_exist = temp.time - System.currentTimeMillis();
                    if (time_exist < 1000) {
                        return;
                    }
                    m.writer().writeShort((short) (time_exist / 1000));
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    MapService.sendMsgPlayerInside(gameMap, p, m, false);
                    m.cleanup();
                    break;
                }
                case -122: {
                    Message m = new Message(75);
                    m.writer().writeByte(5);
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    long time_exist = temp.time - System.currentTimeMillis();
                    if (time_exist < 1000) {
                        return;
                    }
                    m.writer().writeShort((short) (time_exist / 1000));
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    MapService.sendMsgPlayerInside(gameMap, p, m, false);
                    m.cleanup();
                    break;
                }
                case -123: {
                    Message m = new Message(75);
                    m.writer().writeByte(4);
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    long time_exist = temp.time - System.currentTimeMillis();
                    if (time_exist < 1000) {
                        return;
                    }
                    m.writer().writeShort((short) (time_exist / 1000));
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    MapService.sendMsgPlayerInside(gameMap, p, m, false);
                    m.cleanup();
                    break;
                }
                case -124: {
                    Message m = new Message(75);
                    m.writer().writeByte(7);
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    long time_exist = temp.time - System.currentTimeMillis();
                    if (time_exist < 1000) {
                        return;
                    }
                    m.writer().writeShort((short) (time_exist / 1000));
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    MapService.sendMsgPlayerInside(gameMap, p, m, false);
                    m.cleanup();
                    break;
                }
                case 23:
                case 24:
                case 52:
                case 53: {
                    Message m = new Message(75);
                    m.writer().writeByte(12);
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    long time_exist = temp.time - System.currentTimeMillis();
                    if (time_exist < 1000) {
                        return;
                    }
                    m.writer().writeShort((short) (time_exist / 1000));
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.objectId);
                    MapService.sendMsgPlayerInside(gameMap, p, m, false);
                    m.cleanup();
                    break;
                }
            }
        }
    }

    public static void send_chat(GameMap gameMap, Session conn, Message m2) throws IOException {
        String chat = m2.reader().readUTF();
        // if(conn.ac_admin >0 && chat.indexOf("t")==0)
        // {
        // int t =Integer.parseInt(chat.substring(1));
        // Service.idxDame = t;
        //// send_in4_other_char(map, conn.p, conn.p, t);
        //// ChienTruongManager.SendEffBienHinh(map, conn.p);
        // Message m = new Message(50);
        // m.writer().writeByte(0);
        // m.writer().writeShort(conn.p.index);
        // m.writer().writeByte(t);
        // m.writer().writeByte(1);
        // conn.addmsg(m);
        // m.cleanup();
        // //Service.send_char_main_in4(conn.p, t);
        //// Message m = new Message(75);
        //// m.writer().writeByte(t);
        //// m.writer().writeByte(0);
        //// m.writer().writeShort(conn.p.id);
        //// m.writer().writeShort(3);
        //// m.writer().writeByte(0);
        //// m.writer().writeShort(conn.p.id);
        //// MapService.send_msg_player_inside(map, conn.p, m, true);
        //// m.cleanup();
        //// Message m = new Message(-94);
        //// m.writer().writeByte(t);
        //// m.writer().writeByte(100);
        //// m.writer().writeShort(1000);
        //// m.writer().writeByte(0);
        //// m.writer().writeLong(0);
        //// conn.addmsg(m);
        //// m.cleanup();
        // return;
        // }
        //////
        //// if(conn.ac_admin >0 && chat.indexOf("go")==0)
        //// {
        //// int t =Integer.parseInt(chat.substring(2));
        //// Vgo v = new Vgo();
        //// v.id_map_go = (byte)t;
        //// v.x_new = 300;
        //// v.y_new = 200;
        //// conn.p.change_map(conn.p, v);
        //// return;
        //// }
        // if(conn.ac_admin >0 && chat.equals("end")){
        //// synchronized (map) {
        //// while (true) {
        //// try{
        //// Thread.sleep(100);
        //// }catch(Exception e){}
        ////
        //// }
        //// }
        // Message m = new Message(-49);
        // m.writer().writeByte(0);
        // m.writer().writeShort(Manager.gI().msg_eff_105.length);
        // m.writer().write(Manager.gI().msg_eff_105);
        //
        // m.writer().writeByte(0);
        // m.writer().writeByte(1);
        // m.writer().writeByte(105);
        //
        // m.writer().writeShort(conn.p.index);
        // m.writer().writeByte(0);//tem mob
        // m.writer().writeByte(0);
        // m.writer().writeShort(8);
        // m.writer().writeByte(0);
        // conn.addmsg(m);
        // m.cleanup();
        // return;
        // }
        // if (conn.ac_admin > 3 && chat.equals("gg")){
        // try{
        // List<String[]> abc = new ArrayList<>();
        // abc.add(new String[]{"dhaj",null,"8888"});
        // abc.add(null);
        // for(String[] mm : abc){
        // for(String mr : mm){
        //
        // System.out.println("map.MapService.send_chat()"+mr);
        // }
        // }
        // }catch(Exception e){
        // e.printStackTrace();
        // }
        // }
        if (conn.ac_admin > 3 && chat.equals("admin")) {
            Message m = new Message(7);
            m.writer().writeShort(30109);
            m.writer().writeShort(40);
            m.writer().writeShort(conn.p.x);
            m.writer().writeShort(conn.p.y);
            m.writer().writeInt(1000);
            m.writer().writeInt(1000);
            m.writer().writeByte(0);
            m.writer().writeInt(1);
            m.writer().writeShort(-1);
            m.writer().writeByte(1);
            m.writer().writeByte(1);
            m.writer().writeByte(0);
            m.writer().writeLong(-11111);
            m.writer().writeByte(0);
            conn.addmsg(m);
            m.cleanup();

            MenuController.send_menu_select(conn, 126, new String[]{
                    "Hentikan Server",                                                                     // 0
                    "Tambah emas x1.000.000.000",                                                          // 1
                    "Tambah permata x1.000.000",                                                           // 2
                    "Perbarui data",                                                                       // 3
                    "Ambil item",                                                                          // 4
                    "Naik level",                                                                          // 5
                    "Set XP",                                                                              // 6
                    "Bisukan Pemain",                                                                      // 7
                    "Buka Pemain terbisukan",                                                              // 8
                    "Kunci putaran",                                                                       // 9
                    "Kunci perdagangan",                                                                   // 10
                    "Kunci Transaksi",                                                                     // 11
                    "Menetaskan telur cepat",                                                              // 12
                    "Buff Admin",                                                                          // 13
                    "Buff Bahan",                                                                          // 14
                    "Buka penaklukan tambang",                                                             // 15
                    "Tutup penaklukan tambang",                                                            // 16
                    (KingCup.running ? "[TUTUP] King Cup" : "[BUKA] King Cup")                            // 17
                            + " [Turn " + KingCupManager.TURN_KING_CUP + "/" + KingCupManager.MAX_TURN + "]",
                    "Reset event mob",                                                                     // 18
                    (CastleSiegeManager.isRegister ? "Tutup" : "Buka") + " pendaftaran pengepungan kastil", // 19
                    "Buka pendaftaran medan perang",                                                       // 20
                    "Pindah map",                                                                          // 21
                    "Loadconfig",                                                                          // 22
                    (Manager.logErrorLogin ? "Matikan" : "Nyalakan") + " log bug",                        // 23
                    "Putuskan client",                                                                     // 24
                    "Cek bug",                                                                             // 25
                    "Perbaiki bug",                                                                        // 26
                    "Nap",                                                                                 // 27
                    "Reload Quest",                                                                        // 28
                    "Broadcast Server Wide",                                                               // 29
                    "Info King Cup",                                                                       // 30
                    "Naikan Turn King Cup"                                                                 // 31
            });

        } // else if (conn.ac_admin >=10 && chat.equals("xoa")){
        // //tools.loadacc();
        // }
        else if (conn.ac_admin >= 2 && conn.ac_admin < 10 && chat.equals("smod")) {
            // Menu SMOD (staff junior) — dipisah dari "admin" (ac_admin>3, penuh)
            // biar staff tier rendah cuma pegang tool moderasi ringan, bukan
            // seluruh power server (stop server, reload config, dsb).
            MenuController.send_menu_select(conn, 127, new String[]{
                    "Cek Info Player",     // 0
                    "Teleport ke Player",  // 1
                    "Panggil Player",      // 2
                    "Kirim Peringatan",    // 3
                    "Bisukan Pemain",      // 4
                    "Buka Pemain terbisukan", // 5
                    "Broadcast Server Wide" // 6
            }); }
        
        else if (conn.ac_admin >= 1 && conn.ac_admin < 10 && chat.equals("mod")) {
            // Menu MOD (staff junior) — dipisah dari "admin" (ac_admin>3, penuh)
            // biar staff tier rendah cuma pegang tool moderasi ringan, bukan
            // seluruh power server (stop server, reload config, dsb).
            MenuController.send_menu_select(conn, 128, new String[]{
                    "Cek Info Player",     // 0
                    "Broadcast Server Wide" // 6
            });

        } else if (conn.ac_admin > 3 && chat.equals("info")) {
            int num = 0;
            int count = 0;
            for (GameMap[] mm : GameMap.entrys) {
                for (GameMap gameMap0 : mm) {
                    if (gameMap0.mobEvens != null) {
                        for (int i = 0; i < gameMap0.mobEvens.size(); i++) {
                            count++;
                        }
                    }
                }
            }
            for (GameMap[] gameMaps : GameMap.entrys) {
                for (GameMap gameMap_ : gameMaps) {
                    num += gameMap_.players.size();
                }
            }
            Service.send_notice_box(conn,
                    "Posisi " + conn.p.x + " - " + conn.p.y + "\n ID Map : " + gameMap.mapId + "\n Zona : " + gameMap.zoneId
                            + "\n Jumlah pemain terhubung : " + Session.SESSION_LIST.size() + "\n Jumlah pemain online : " + num
                            + "\nEvent mob: " + ev_he.Event_2.entrys.size() + " / " + count);
        } else if (conn.ac_admin > 3 && chat.equals("nap")) {
            Service.sendBoxInputText(conn, 99, "Masukkan informasi",
                    new String[]{"Nama karakter", "Jumlah uang", "Koin"});

        } else if (conn.ac_admin > 3 && chat.equals("meteor")) {
            MapService.send_meteor_test_effect(gameMap, conn.p, 666666);
            Service.send_notice_nobox_white(conn, "[GM] Efek meteor jatuh ditest di posisi kamu.");

        } else {
            SendChat(gameMap, conn.p, chat, false);

        }
    }

    public static void SendChat(GameMap gameMap, Player p, String chat, boolean include) throws IOException {
        Message m = new Message(27);
        m.writer().writeShort(p.objectId);
        m.writer().writeByte(0);
        m.writer().writeUTF(chat);
        MapService.sendMsgPlayerInside(gameMap, p, m, include);
        m.cleanup();
    }

    public static void send_in4_other_char(GameMap gameMap, Player p, Player p0) throws IOException {
        int dem = 0;
        for (int i = 0; i < p0.item.wear.length; i++) {
            // REVERT: slot 22 (amulet/cincin) dibalik jadi TIDAK dibroadcast ke
            // pemain lain - client belum bisa render type-nya, hasilnya body jadi bolong.
            if (i != 0 && i != 1 && i != 6 && i != 7 && i != 10) {
                continue;
            }
            if (p0.item.wear[i] != null) {
                dem++;
            }
        }
        Message m = new Message(5);
        m.writer().writeShort(p0.objectId);
        m.writer().writeUTF(p0.getDisplayName());
        m.writer().writeShort(p0.x);
        m.writer().writeShort(p0.y);
        m.writer().writeByte(p0.clazz);
        m.writer().writeByte(-1);
        m.writer().writeByte(p0.head);
        m.writer().writeByte(p0.eye);
        m.writer().writeByte(p0.hair);
        m.writer().writeShort(p0.level);
        m.writer().writeInt(p0.hp);
        m.writer().writeInt(p0.body.getMaxHP());
        m.writer().writeByte(p0.typepk);
        m.writer().writeShort(p0.pointpk);
        m.writer().writeByte(dem);
        //
        for (int i = 0; i < p0.item.wear.length; i++) {
            // REVERT: lihat catatan di loop hitung dem di atas.
            if (i != 0 && i != 1 && i != 6 && i != 7 && i != 10) {
                continue;
            }
            Item3 temp = p0.item.wear[i];
            if (temp != null) {
                m.writer().writeByte(temp.type);

                if (i == 10 && p0.item.wear[14] != null
                        && (p0.item.wear[14].id >= 4638 && p0.item.wear[14].id <= 4648)) {
                    m.writer().writeByte(p0.item.wear[14].part);
                } else {
                    m.writer().writeByte(temp.part);
                }
                m.writer().writeByte(3);
                m.writer().writeShort(-1);
                m.writer().writeShort(-1);
                m.writer().writeShort(-1);
                m.writer().writeShort(-1); // eff
            }
        }
        //
        if (p0.myclan != null) {
            m.writer().writeShort(p0.myclan.icon);
            m.writer().writeInt(Guild.get_id_clan(p0.myclan));
            m.writer().writeUTF(p0.myclan.shortName);
            m.writer().writeByte(p0.myclan.get_mem_type(p0.name));
        } else {
            m.writer().writeShort(-1); // clan
        }
        if (p0.pet_follow != -1) {
            for (Pet temp : p0.mypet) {
                if (temp.is_follow) {
                    m.writer().writeByte(temp.type); // type
                    m.writer().writeByte(temp.spriteImage); // icon
                    m.writer().writeByte(temp.nframe); // nframe
                    break;
                }
            }
        } else {
            m.writer().writeByte(-1); // pet
        }
        m.writer().writeByte(p0.fashion.length);
        for (int i = 0; i < p0.fashion.length; i++) {
            if (p.conn.version < 280) {
                m.writer().writeByte(p0.fashion[i]);
            } else {
                m.writer().writeShort(p0.fashion[i]);
            }
        }
        //
        m.writer().writeShort(-1);
        m.writer().writeByte(p0.mount != null ? p0.mount.getType() : -1);
        m.writer().writeBoolean(false);
        m.writer().writeByte(1);
        if (gameMap.isCastleSiegeMap() && p.myclan != null && p0.myclan != null && !p.myclan.equals(p0.myclan)) {
            m.writer().writeByte(1);
        } else {
            m.writer().writeByte(0);
        }
        m.writer().writeShort(Service.getMaskId(p0)); // mat na
        m.writer().writeByte(1); // paint mat na trc sau
        m.writer().writeShort(Service.getCloakId(p0)); // phi phong
        m.writer().writeShort(Service.getWeaponId(p0)); // weapon
        m.writer().writeShort(p0.mount != null ? p0.mount.getPart() : -1);
        m.writer().writeShort(Service.getHairId(p0)); // hair
        m.writer().writeShort(Service.getWingId(p0)); // wing
        m.writer().writeShort(Service.getTitleId(p0));
        m.writer().writeShort(p0.id_name);         // name title id
        m.writer().writeShort(p0.bodyEffectId);    // body effect id (112_xx.png)
        m.writer().writeShort(p0.legEffectId);     // leg effect id  (112_xx.png)
        p.conn.addmsg(m);
        m.cleanup();
        if (p.objectId != p0.objectId && !p0.my_store_name.isEmpty()) {
            m = new Message(-102);
            m.writer().writeByte(1);
            m.writer().writeShort(p0.objectId);
            m.writer().writeUTF(p0.my_store_name);
            p.conn.addmsg(m);
            m.cleanup();
        }
    }

    public static void send_in4_other_char(GameMap gameMap, Player p, Player p0, int bienhinh) throws IOException {
        int dem = 0;
        for (int i = 0; i < p0.item.wear.length; i++) {
            // REVERT: slot 22 (amulet/cincin) dibalik jadi TIDAK dibroadcast ke
            // pemain lain - client belum bisa render type-nya, hasilnya body jadi bolong.
            if (i != 0 && i != 1 && i != 6 && i != 7 && i != 10) {
                continue;
            }
            if (p0.item.wear[i] != null) {
                dem++;
            }
        }
        Message m = new Message(5);
        m.writer().writeShort(p0.objectId);
        m.writer().writeUTF(p0.getDisplayName());
        m.writer().writeShort(p0.x);
        m.writer().writeShort(p0.y);
        m.writer().writeByte(p0.clazz);
        m.writer().writeByte(-1);
        m.writer().writeByte(p0.head);
        m.writer().writeByte(p0.eye);
        m.writer().writeByte(p0.hair);
        m.writer().writeShort(p0.level);
        m.writer().writeInt(p0.hp);
        m.writer().writeInt(p0.body.getMaxHP());
        m.writer().writeByte(p0.typepk);
        m.writer().writeShort(p0.pointpk);
        m.writer().writeByte(dem);
        //
        for (int i = 0; i < p0.item.wear.length; i++) {
            // REVERT: lihat catatan di loop hitung dem di atas.
            if (i != 0 && i != 1 && i != 6 && i != 7 && i != 10) {
                continue;
            }
            Item3 temp = p0.item.wear[i];
            if (temp != null) {
                m.writer().writeByte(temp.type);

                if (i == 10 && p0.item.wear[14] != null
                        && (p0.item.wear[14].id >= 4638 && p0.item.wear[14].id <= 4648)) {
                    m.writer().writeByte(p0.item.wear[14].part);
                } else {
                    m.writer().writeByte(temp.part);
                }
                m.writer().writeByte(3);
                m.writer().writeShort(-1);
                m.writer().writeShort(-1);
                m.writer().writeShort(-1);
                m.writer().writeShort(-1); // eff
            }
        }
        //
        if (p0.myclan != null) {
            m.writer().writeShort(p0.myclan.icon);
            m.writer().writeInt(Guild.get_id_clan(p0.myclan));
            m.writer().writeUTF(p0.myclan.shortName);
            m.writer().writeByte(p0.myclan.get_mem_type(p0.name));
        } else {
            m.writer().writeShort(-1); // clan
        }
        if (p0.pet_follow != -1) {
            for (Pet temp : p0.mypet) {
                if (temp.is_follow) {
                    m.writer().writeByte(temp.type); // type
                    m.writer().writeByte(temp.spriteImage); // icon
                    m.writer().writeByte(temp.nframe); // nframe
                    break;
                }
            }
        } else {
            m.writer().writeByte(-1); // pet
        }
        m.writer().writeByte(p0.fashion.length);
        for (int i = 0; i < p0.fashion.length; i++) {
            if (p.conn.version < 280) {
                m.writer().writeByte(p0.fashion[i]);
            } else {
                m.writer().writeShort(p0.fashion[i]);
            }
        }
        //
        m.writer().writeShort(110);
        m.writer().writeByte(p0.mount != null ? p0.mount.getType() : -1);
        m.writer().writeBoolean(false);
        m.writer().writeByte(1);
        m.writer().writeByte(gameMap.mapId >= 83 && gameMap.mapId <= 87 ? 1 : 0);
        m.writer().writeShort(Service.getMaskId(p0)); // mat na
        m.writer().writeByte(1); // paint mat na trc sau
        m.writer().writeShort(Service.getCloakId(p0)); // phi phong
        m.writer().writeShort(Service.getWeaponId(p0)); // weapon
        m.writer().writeShort(p0.mount != null ? p0.mount.getPart() : -1);
        m.writer().writeShort(Service.getHairId(p0)); // hair
        m.writer().writeShort(Service.getWingId(p0)); // wing
        m.writer().writeShort(Service.getTitleId(p0)); // phi phong
        m.writer().writeShort(p0.id_name);          // name title id
        m.writer().writeShort(p0.bodyEffectId);     // body effect id (112_xx.png)
        m.writer().writeShort(bienhinh);             // bienhinh
        p.conn.addmsg(m);
        m.cleanup();
        if (p.objectId != p0.objectId && !p0.my_store_name.isEmpty()) {
            m = new Message(-102);
            m.writer().writeByte(1);
            m.writer().writeShort(p0.objectId);
            m.writer().writeUTF(p0.my_store_name);
            p.conn.addmsg(m);
            m.cleanup();
        }
    }

    public static void removeNPC(Session s, int objectId) throws IOException {
        Message m = new Message(90);
        m.writer().writeByte(2);
        m.writer().writeShort(objectId);
        m.writer().writeByte(2);

        s.addmsg(m);
        m.cleanup();

    }

    public static void request_livefromdie(GameMap gameMap, Session conn, Message m) throws IOException {
        byte type = m.reader().readByte();

        if (gameMap.isCastleSiegeMap()) {
            CastleSiegeManager.request_livefromdie(gameMap, conn, type);
        } else {
            if (GameMap.isBattlefieldMap(gameMap.mapId)) {
                return;
            }
            // FIX: map 100/102 = arena lôi đài (duel PvpSession & King Cup).
            // Sebelumnya pemain yang mati di arena masih bisa menekan "kembali ke
            // desa" dan langsung ter-changeMap ke map 1 di tengah pertandingan.
            // PvpSession.isActive() cuma mengecek koneksi, jadi sesi tidak pernah
            // tahu pemainnya sudah kabur: lawannya ditinggal sendirian di arena
            // tanpa target sampai ronde habis (10 menit x 2 ronde). Kebangkitan di
            // arena sepenuhnya diurus oleh sesi/event, bukan oleh permintaan client.
            //
            // FIX "PVP nyangkut pas kalah": return diam-diam di sini membuat client
            // yang kalah menampilkan dialog "Harap tunggu" (menunggu balasan server)
            // selamanya — tiap tombol respawn yang ditekan menambah satu lapis teks
            // ("Harap tunggHarap tunggHarap tunggu"). Request client HARUS selalu
            // dijawab. Untuk pemain yang benar-benar sedang di duel, jawab dengan
            // paket yang sama dengan jalur revive normal (3 + -108 + 32) supaya
            // client keluar dari state mati & menutup dialog tunggu, TANPA mengubah
            // HP/isdie di server (kalau tidak, request palsu = heal gratis di duel).
            // Pemain yang tertinggal di arena tanpa sesi/pertandingan aktif tidak
            // boleh terkunci: dibiarkan lanjut ke alur normal (pulang ke desa).
            if (gameMap.isMapLoiDai()) {
                Player pl = conn.p;
                boolean inDuel = pl != null
                        && (PvpManager.gI().isInSession(pl) || gameMap.kingCupRef != null);
                if (inDuel) {
                    try {
                        Service.sendMainCharInfo(pl);
                        Service.send_combo(conn);
                        if (!pl.isdie) {
                            Service.usepotion(pl, 0, 0);
                            Service.usepotion(pl, 1, 0);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                // tidak ada duel aktif -> jatuh ke alur normal di bawah
            }
            if (type == 1) { // hsl
                Service.send_box_input_yesno(conn, 9, "Diperlukan 5 permata untuk hidup kembali di tempat. Lanjutkan?");
            } else if (type == 0) { // ve lang
                conn.p.isdie = false;
                conn.p.hp = conn.p.body.getMaxHP();
                conn.p.mp = conn.p.body.getMaxMP();
                Vgo vgo = new Vgo();
                if (GameMap.isBattlefieldMap(gameMap.mapId)) {
                    switch (conn.p.typepk) {
                        case 1: {
                            vgo.toMap = 59;
                            vgo.toX = 240;
                            vgo.toY = 224;
                            break;
                        }
                        case 2: {
                            vgo.toMap = 55;
                            vgo.toX = 224;
                            vgo.toY = 256;
                            break;
                        }
                        case 4: {
                            vgo.toMap = 57;
                            vgo.toX = 264;
                            vgo.toY = 272;
                            break;
                        }
                        case 5: {
                            vgo.toMap = 53;
                            vgo.toX = 276;
                            vgo.toY = 246;
                            break;
                        }
                    }
                } else {
                    vgo.toMap = 1;
                    vgo.toX = (short) 528;
                    vgo.toY = (short) 480;
                }
                conn.p.changeMap(conn.p, vgo);
                Service.usepotion(conn.p, 0, conn.p.body.getMaxHP());
                Service.usepotion(conn.p, 1, conn.p.body.getMaxMP());
            }
        }
    }

    private static Player get_player_by_id(GameMap gameMap, int n2) {
        for (Player p0 : gameMap.players) {
            if (p0.objectId == n2) {
                return p0;
            }
        }
        return null;
    }

    public static void Player_Die(GameMap gameMap, MainObject p, MainObject Obj, boolean include) throws IOException {

        Message m = new Message(41);
        m.writer().writeShort(p.objectId);
        m.writer().writeShort(Obj.objectId);
        m.writer().writeShort(Obj.typepk); // point pk
        m.writer().writeByte(Obj.getTypeObject()); // type main object
        if (!include) {
            ((Player) p).conn.addmsg(m);
        } else {
            MapService.sendMsgPlayerInside(gameMap, Obj, m, true);
        }
        m.cleanup();
    }

    public static void MainObj_Die(GameMap gameMap, Session conn, MainObject mob, boolean include) throws IOException {
        Message m2 = null;
        if (mob.getTypeObject() == 0 || mob.isMobDiBuon()) {
            m2 = new Message(8);
            m2.writer().writeShort(mob.objectId);
        } else {
            m2 = new Message(17);
            m2.writer().writeShort(conn != null ? conn.p.objectId : mob.objectId);
            m2.writer().writeShort(mob.objectId);
        }
        if (!include && conn != null) {
            conn.addmsg(m2);
        } else {
            MapService.sendMsgPlayerInside(gameMap, mob, m2, true);
        }
        m2.cleanup();
    }

    // public static void Mob_Fire(Map map, Player pTaget, MainObject mainAttack,
    // int indexskill, int dame, List<Eff_TextFire> ListFire) throws IOException {
    //
    // }
    public static void MainObj_Fire_Player(GameMap gameMap, Player pTaget, MainObject mainAttack, int indexskill, int dame,
                                           List<Eff_TextFire> ListFire) throws IOException {
        Message m = new Message(6);
        m.writer().writeShort(mainAttack.objectId);
        m.writer().writeByte(indexskill);
        m.writer().writeByte(1);
        m.writer().writeShort(pTaget.objectId);
        m.writer().writeInt(dame); // dame
        m.writer().writeInt(pTaget.hp); // hp after
        m.writer().writeByte(ListFire.size());
        for (int i = 0; i < ListFire.size(); i++) {
            Eff_TextFire ef = ListFire.get(i);
            if (ef == null) {
                continue;
            }
            m.writer().writeByte(ef.type); // 1: xuyen giap, 2:hut hp, 3: hut mp, 4: chi mang, 5: phan don
            m.writer().writeInt(ef.dame); // par
        }
        m.writer().writeInt(mainAttack.hp);
        m.writer().writeInt(mainAttack.mp);
        m.writer().writeByte(11);
        m.writer().writeInt(0);
        MapService.sendMsgPlayerInside(gameMap, pTaget, m, true);
        m.cleanup();
    }

    public static void Fire_Mob(GameMap gameMap, Session conn, int indexskill, int idPTaget, int dame, int hpPtaget,
                                List<Eff_TextFire> ListFire, int mobid) throws IOException {

        Message m = new Message(9);
        m.writer().writeShort(conn.p.objectId);
        m.writer().writeByte(indexskill);
        m.writer().writeByte(1);
        m.writer().writeShort(idPTaget);
        m.writer().writeInt((int) dame); // dame
        m.writer().writeInt(hpPtaget); // hp mob after
        if (ListFire == null || ListFire.isEmpty()) {
            m.writer().writeByte(1);
            m.writer().writeByte(0); // 1: armor penetration, 2: life steal, 3: mana steal, 4: critical hit, 5: counterattack
            m.writer().writeInt(dame);
        } else {
            m.writer().writeByte(ListFire.size());
            for (int i = 0; i < ListFire.size(); i++) {
                Eff_TextFire ef = ListFire.get(i);
                if (ef == null) {
                    m.writer().writeByte(0); // 1: armor penetration, 2: life steal, 3: mana steal, 4: critical hit, 5: counterattack
                    m.writer().writeInt(dame);
                } else {
                    m.writer().writeByte(ef.type); // 1: armor penetration, 2: life steal, 3: mana steal, 4: critical hit, 5: counterattack
                    m.writer().writeInt(ef.dame); // parameter
                }
            }

            if (Battlefield.gI().list_ai != null && !Battlefield.gI().list_ai.isEmpty()) {
                for (int i = 0; i < Battlefield.gI().list_ai.size(); i++) {
                    PlayerClone temp = Battlefield.gI().list_ai.get(i);
                    if (!temp.isdie && temp.gameMap.equals(gameMap) && temp.time_change_target < System.currentTimeMillis()) {
                        temp.time_change_target = System.currentTimeMillis() + 5000L;
                        temp.target = conn.p.objectId;
                    }
                }
            }

            if (gameMap.isMapChienTruong()) {
                switch (mobid) {
                    case 89: {
                        break;
                    }
                    case 90: {
                        break;
                    }
                    case 91: {
                        break;
                    }
                    case 92: {
                        break;
                    }
                }
            }

        }
        m.writer().writeInt(conn.p.hp);
        m.writer().writeInt(conn.p.mp);
        m.writer().writeByte(11);
        m.writer().writeInt(0);
        MapService.sendMsgPlayerInside(gameMap, conn.p, m, true);
        m.cleanup();
    }

    /**
     * firePlayer versi AFK bot — tidak butuh Session conn.
     * Packet dikirim ke semua player di sekitar via sendMsgPlayerInside.
     */
    public static void firePlayerBot(GameMap gameMap, Player player, int indexskill, int idPTaget, int dame, int hpPtaget,
                                     List<Eff_TextFire> ListFire) throws IOException {
        // FIX: sebelumnya poin diberikan tiap skill dipakai TANPA cek damage sama
        // sekali, beda dengan firePlayer() (versi non-bot) yang mensyaratkan
        // dame > 0. Akibatnya bot AFK bisa dapat poin arena terus-menerus walau
        // serangannya tidak kena/tidak ada damage. Disamakan syaratnya.
        if (dame > 0 && GameMap.isBattlefieldMap(gameMap.mapId)) {
            player.updatePointArena(1);
        }
        Message m = new Message(6);
        m.writer().writeShort(player.objectId);
        m.writer().writeByte(indexskill);
        m.writer().writeByte(1);
        m.writer().writeShort(idPTaget);
        m.writer().writeInt(dame);
        m.writer().writeInt(hpPtaget);
        m.writer().writeByte(ListFire.size());
        for (int i = 0; i < ListFire.size(); i++) {
            Eff_TextFire ef = ListFire.get(i);
            if (ef == null) continue;
            m.writer().writeByte(ef.type);
            m.writer().writeInt(ef.dame);
        }
        m.writer().writeInt(player.hp);
        m.writer().writeInt(player.mp);
        m.writer().writeByte(11);
        m.writer().writeInt(0);
        // Broadcast ke semua player di sekitar (termasuk bot sendiri agar hp update)
        MapService.sendMsgPlayerInsideBot(gameMap, player, m);
        m.cleanup();
    }

    /**
     * Fire_Mob versi AFK bot — tidak butuh Session conn.
     * Packet dikirim ke semua player di sekitar via sendMsgPlayerInside.
     */
    public static void Fire_Mob_Bot(GameMap gameMap, Player player, int indexskill, int idPTaget, int dame, int hpPtaget,
                                    List<Eff_TextFire> ListFire, int mobid) throws IOException {
        Message m = new Message(9);
        m.writer().writeShort(player.objectId);
        m.writer().writeByte(indexskill);
        m.writer().writeByte(1);
        m.writer().writeShort(idPTaget);
        m.writer().writeInt(dame);
        m.writer().writeInt(hpPtaget);
        if (ListFire == null || ListFire.isEmpty()) {
            m.writer().writeByte(1);
            m.writer().writeByte(0);
            m.writer().writeInt(dame);
        } else {
            m.writer().writeByte(ListFire.size());
            for (int i = 0; i < ListFire.size(); i++) {
                Eff_TextFire ef = ListFire.get(i);
                if (ef == null) {
                    m.writer().writeByte(0);
                    m.writer().writeInt(dame);
                } else {
                    m.writer().writeByte(ef.type);
                    m.writer().writeInt(ef.dame);
                }
            }
        }
        m.writer().writeInt(player.hp);
        m.writer().writeInt(player.mp);
        m.writer().writeByte(11);
        m.writer().writeInt(0);
        MapService.sendMsgPlayerInsideBot(gameMap, player, m);
        m.cleanup();
    }

    /**
     * Broadcast packet ke semua player di map yang berada dalam radius 200px dari bot.
     * Skip player yang connnya null/terputus (termasuk bot AFK itu sendiri).
     */
    public static void sendMsgPlayerInsideBot(GameMap gameMap, Player botPlayer, Message m) {
        for (int i = 0; i < gameMap.players.size(); i++) {
            Player p0 = gameMap.players.get(i);
            if (p0 == null || p0.conn == null || !p0.conn.connected) continue;
            if ((Math.abs(p0.x - botPlayer.x) < 200 && Math.abs(p0.y - botPlayer.y) < 200)
                    || GameMap.is_map__load_board_player(gameMap.mapId)) {
                p0.conn.addmsg(m);
            }
        }
    }

        public static void firePlayer(GameMap gameMap, Session conn, int indexskill, int idPTaget, int dame, int hpPtaget,
                                  List<Eff_TextFire> ListFire) throws IOException {
        if (dame > 0 && GameMap.isBattlefieldMap(gameMap.mapId)) {
            conn.p.updatePointArena(1);
        }
        Message m = new Message(6);
        m.writer().writeShort(conn.p.objectId);
        m.writer().writeByte(indexskill);
        m.writer().writeByte(1);
        m.writer().writeShort(idPTaget);
        m.writer().writeInt(dame); // dame
        m.writer().writeInt(hpPtaget); // hp after
        m.writer().writeByte(ListFire.size());
        for (int i = 0; i < ListFire.size(); i++) {
            Eff_TextFire ef = ListFire.get(i);
            if (ef == null) {
                continue;
            }
            m.writer().writeByte(ef.type); // 1: xuyen giap, 2:hut hp, 3: hut mp, 4: chi mang, 5: phan don
            m.writer().writeInt(ef.dame); // par
        }
        m.writer().writeInt(conn.p.hp);
        m.writer().writeInt(conn.p.mp);
        m.writer().writeByte(11);
        m.writer().writeInt(0);
        MapService.sendMsgPlayerInside(gameMap, conn.p, m, true);
        m.cleanup();
    }

    public static void use_skill(GameMap gameMap, Session conn, Message m, int type_atk) {
        try {
            long time_ = System.currentTimeMillis();
            byte index_skill = m.reader().readByte();
            int n = m.reader().readByte();

            // int sk_point = conn.p.body.get_skill_point(index_skill);
            int sk_point1 = conn.p.skillPoint[index_skill];
            if (sk_point1 < 1) {
                return;
            }
            if (conn.p.item.wear[0] == null) {
                Service.send_notice_nobox_white(conn, "No weapon equipped");
                return;
            }
            LvSkill _skill = conn.p.skills[index_skill].mLvSkill[sk_point1 - 1];
            while (sk_point1 > 1 && _skill.LvRe > conn.p.level) {
                sk_point1--;
                _skill = conn.p.skills[index_skill].mLvSkill[sk_point1 - 1];
            }
            if (_skill.LvRe > conn.p.level) {
                Service.send_notice_nobox_white(conn, "Required level: " + _skill.LvRe);

                return;
            }
            int sk_pointPlus = conn.p.getSkillPointPlus(index_skill);
            if (sk_point1 + sk_pointPlus <= 15) {
                _skill = conn.p.skills[index_skill].mLvSkill[(sk_point1 + sk_pointPlus) - 1];
            } else {
                _skill = conn.p.skills[index_skill].mLvSkill[14];
            }
            if (conn.p.mp - _skill.mpLost < 0) {
                Service.send_notice_nobox_white(conn, "Not enough MP");
                return;
            }
            if (conn.p.isStunes(true)) {
                return;
            }
            if (conn.p.time_delay_skill[index_skill] > time_) {
                if (++conn.p.enough_time_disconnect > 5) {
                    conn.close();
                }
                return;
            }
            // begin damage calculation
            conn.p.mp -= _skill.mpLost;
            n = (_skill.nTarget < n) ? _skill.nTarget : n;
            conn.p.time_delay_skill[index_skill] = (long) (time_ + _skill.delay * 0.97);
            conn.p.enough_time_disconnect = 0;
            byte type = 0;
            if (index_skill == 2 || index_skill == 4 || index_skill == 6 || index_skill == 8 || index_skill == 19
                    || index_skill == 20) {
                type = index_skill == 20 && (conn.p.clazz == 2 || conn.p.clazz == 1)
                        || index_skill == 19 && (conn.p.clazz == 0 || conn.p.clazz == 3) ? (byte) 0 : 1;
            }
            if (index_skill == 0) {
                type = 2;
            }
            List<Integer> ListATK = new ArrayList<>();
            if (type_atk == 0) {

                for (int i = 0; i < n; ++i) {
                    int ObjAtk = Short.toUnsignedInt(m.reader().readShort());
                    MobInMap mob_target = MapService.get_mob_by_index(gameMap, ObjAtk);

                    if (mob_target == null) {
                        mob_target = gameMap.GetBoss(ObjAtk);
                    }

                    if (GameMap.isBattlefieldMap(gameMap.mapId)) {
                        BTF btf = GameEventManager.gI().getEvent(BTF.class);
                        Team team;
                        // FIX: mob_target can legitimately be null here (e.g. ObjAtk
                        // refers to a mob index that no longer exists and isn't a boss
                        // either), but the code accessed mob_target.template.mob_id
                        // unconditionally, causing
                        // "NullPointerException: Cannot read field \"template\" because
                        // \"mob_target\" is null". Skip the tower check when there's no
                        // target instead of crashing.
                        if (mob_target != null && btf != null && (team = btf.getPlayerTeam(conn.p.objectId)) != null) {
                            if (team.getTowerId() == mob_target.template.mob_id) {
                                Service.send_notice_nobox_white(conn, "Can't attack");
                                continue;
                            }
                        }
                    }

                    if (gameMap.zoneId == gameMap.maxzone && !gameMap.isCastleSiegeMap() && !gameMap.isMapLoiDai()) {
                        Pet_di_buon pet_di_buon = Pet_di_buon_manager.check(ObjAtk);
                        if (pet_di_buon != null) {
                            if (!pet_di_buon.equals(conn.p.pet_di_buon)) {
                                MainObject.attack(gameMap, conn.p, pet_di_buon, index_skill, _skill, type);
                            }
                        }
                    } else if (mob_target != null) {
                        MainObject.attack(gameMap, conn.p, mob_target, index_skill, _skill, type);
                    } else if (ObjAtk > 10000 && ObjAtk < 11000) {// mob boss
                        Message m2 = new Message(17);
                        m2.writer().writeShort(-1);
                        m2.writer().writeShort(ObjAtk);
                        conn.addmsg(m2);
                        m2.cleanup();
                    } else if (conn.p.map.mapId == 48) {
                        Dungeon d = DungeonManager.get_list(conn.p.name);
                        if (d != null) {
                            Mob_Dungeon mod_target_dungeon = d.get_mob(ObjAtk);
                            if (mod_target_dungeon != null) {
                                MainObject.attack(gameMap, conn.p, mod_target_dungeon, index_skill, _skill, type);
                            }
                        }
                    } else if (GameMap.isSummonMap(conn.p.map, true) && conn.p.myclan != null) {

                        Crystal temp_mob = conn.p.myclan.get_mo_tai_nguyen(ObjAtk);
                        if (temp_mob == null) {
                            temp_mob = Manager.gI().mine.get_mob_in_map(gameMap);
                            MainObject.attack(gameMap, conn.p, temp_mob, index_skill, _skill, type);
                        } else if (conn.p.myclan.gems >= 100
                                && temp_mob.guardSave != null
                                && (temp_mob.guard == null || temp_mob.guard.isdie)) {
                            // BUGFIX: kondisi lama syaratnya `temp_mob.guard == null`, padahal
                            // guard TIDAK PERNAH di-null-kan waktu mati (cuma isdie=true &
                            // hp=0 lewat MainObject.setDie() default) -> cabang ini sebelumnya
                            // gak pernah kepanggil sama sekali waktu guard beneran mati.
                            // Sekarang dicek juga .isdie, dan reset di tempat (in-place) kalau
                            // objectnya masih ada, biar objectId-nya konsisten (gak perlu
                            // swap ke guardSave kalau gak perlu).
                            conn.p.myclan.gems -= 100;
                            Clone revived = (temp_mob.guard != null) ? temp_mob.guard : temp_mob.guardSave;
                            revived.isdie = false;
                            revived.hp = revived.getMaxHP();
                            revived.target = null;
                            revived.x = temp_mob.x;
                            revived.y = temp_mob.y;
                            temp_mob.guard = revived;
                            temp_mob.guardSave = revived;
                            Manager.gI().addClone(temp_mob.guard);

                            //
                            Message m12 = new Message(4);
                            m12.writer().writeByte(0);
                            m12.writer().writeShort(0);
                            m12.writer().writeShort(temp_mob.guard.objectId);
                            m12.writer().writeShort(temp_mob.guard.x);
                            m12.writer().writeShort(temp_mob.guard.y);
                            m12.writer().writeByte(-1);
                            MapService.sendMsgPlayerInside(gameMap, conn.p, m12, true);
                            m12.cleanup();

                            // Sama kayak 2 titik lain: Message(4) doang nggak cukup buat
                            // nge-render karakter clone-nya, wajib nyusul Message(5) via
                            // send_in4() ke semua player yg ada di map ini.
                            for (int gi = 0; gi < gameMap.players.size(); gi++) {
                                Player gp = gameMap.players.get(gi);
                                if (gp != null && gp.conn != null && gp.conn.connected) {
                                    temp_mob.guard.send_in4(gp);
                                }
                            }
                        } else if (conn.p.myclan.gems >= 100
                                && temp_mob.findDeadExtraGuard() != null) {
                            // Revive clone TAMBAHAN yang mati - prioritas sebelum boleh beli
                            // clone baru, jadi lebih murah (100 gems) & masuk akal daripada
                            // langsung suruh beli baru 1000 gems padahal slot-nya masih ada.
                            conn.p.myclan.gems -= 100;
                            Clone revived = temp_mob.findDeadExtraGuard();
                            revived.isdie = false;
                            revived.hp = revived.getMaxHP();
                            revived.target = null;
                            revived.x = temp_mob.x;
                            revived.y = temp_mob.y;
                            Manager.gI().addClone(revived);

                            Message m12r = new Message(4);
                            m12r.writer().writeByte(0);
                            m12r.writer().writeShort(0);
                            m12r.writer().writeShort(revived.objectId);
                            m12r.writer().writeShort(revived.x);
                            m12r.writer().writeShort(revived.y);
                            m12r.writer().writeByte(-1);
                            MapService.sendMsgPlayerInside(gameMap, conn.p, m12r, true);
                            m12r.cleanup();

                            for (int gi = 0; gi < gameMap.players.size(); gi++) {
                                Player gp = gameMap.players.get(gi);
                                if (gp != null && gp.conn != null && gp.conn.connected) {
                                    revived.send_in4(gp);
                                }
                            }
                        } else if (conn.p.myclan.gems >= Crystal.ADD_GUARD_GEM_COST
                                && temp_mob.activeGuardCount() < Crystal.MAX_GUARDS) {
                            // ── Menu "Tambah Clone Penjaga Tambang" ──────────────────────
                            // Diakses lewat klik/serang tambang milik guild sendiri (sama
                            // seperti mekanisme revive guard di atas). Semua anggota guild
                            // boleh (gak cuma leader), maksimal MAX_GUARDS (3) clone aktif
                            // sekaligus, tiap tambahan potong Crystal.ADD_GUARD_GEM_COST
                            // (1000) gems guild.
                            conn.p.myclan.gems -= Crystal.ADD_GUARD_GEM_COST;

                            Clone newGuard = new Clone();
                            newGuard.create(conn.p);
                            newGuard.skillId = 1;
                            newGuard.x = temp_mob.x;
                            newGuard.y = temp_mob.y;
                            temp_mob.extraGuards.add(newGuard);
                            Manager.gI().addClone(newGuard);

                            try {
                                Manager.gI().chatKTGprocess("@" + conn.p.name + " dari guild "
                                        + conn.p.myclan.shortName.toUpperCase()
                                        + " menambah clone penjaga di " + temp_mob.name + " ("
                                        + temp_mob.activeGuardCount() + "/" + Crystal.MAX_GUARDS + ")");
                            } catch (IOException ignore) {
                            }

                            Message m12b = new Message(4);
                            m12b.writer().writeByte(0);
                            m12b.writer().writeShort(0);
                            m12b.writer().writeShort(newGuard.objectId);
                            m12b.writer().writeShort(newGuard.x);
                            m12b.writer().writeShort(newGuard.y);
                            m12b.writer().writeByte(-1);
                            MapService.sendMsgPlayerInside(gameMap, conn.p, m12b, true);
                            m12b.cleanup();

                            for (int gi = 0; gi < gameMap.players.size(); gi++) {
                                Player gp = gameMap.players.get(gi);
                                if (gp != null && gp.conn != null && gp.conn.connected) {
                                    newGuard.send_in4(gp);
                                }
                            }
                        } else {
                            // Gak ada aksi yang jalan (bukan revive, bukan tambah clone).
                            // Sebelumnya di titik ini server DIAM AJA tanpa kasih tau apa-apa,
                            // jadi dari sisi player kelihatan kayak "menu"-nya gak muncul /
                            // gak ngefek. Sekarang dikasih notice biar jelas sebabnya.
                            try {
                                if (temp_mob.activeGuardCount() >= Crystal.MAX_GUARDS
                                        && temp_mob.findDeadExtraGuard() == null
                                        && !(temp_mob.guard != null && temp_mob.guard.isdie)) {
                                    Service.send_notice_box(conn,
                                            "Clone penjaga tambang sudah maksimal (" + Crystal.MAX_GUARDS
                                                    + "/" + Crystal.MAX_GUARDS + ")!");
                                } else if (conn.p.myclan.gems < Crystal.ADD_GUARD_GEM_COST) {
                                    Service.send_notice_box(conn,
                                            "Gems guild tidak cukup! Butuh " + Crystal.ADD_GUARD_GEM_COST
                                                    + " gems untuk menambah clone penjaga baru (revive clone yang mati cuma 100 gems).");
                                }
                            } catch (IOException ignore) {
                            }
                        }
                    }
                    if (mob_target != null && mob_target.isBoss()) {
                        ListATK.add(mob_target.objectId);
                    }
                }
            } else if (type_atk == 1) {
                for (int i = 0; i < n; ++i) {
                    short n3 = m.reader().readShort();
                    int n2 = Short.toUnsignedInt(n3);

                    if (n3 >= 10_000) {
                        PlayerBot bot = AiManager.getInstance().get(n2);
                        if (bot != null) {
                           bot.takeDamage(gameMap, conn.p, index_skill, _skill, type);
                            continue;
                        }
                    }

                    Player p_target;
                    if ((p_target = MapService.get_player_by_id(gameMap, n2)) != null) {
                        // attack player
                        MainObject.attack(gameMap, conn.p, p_target, index_skill, _skill, type);
                        ListATK.add(p_target.objectId);
                    } else if (GameMap.isSummonMap(conn.p.map, true) && conn.p.myclan != null) {
                        // attack clone
                        Crystal temp_mob = conn.p.myclan.get_mo_tai_nguyen(n2);
                        if (temp_mob == null) {
                            temp_mob = Manager.gI().mine.get_mob_in_map(conn.p.map);
                            if (temp_mob.guard != null && temp_mob.guard.objectId == n2) {
                                ListATK.add(temp_mob.guard.objectId);
                                MainObject.attack(gameMap, conn.p, temp_mob.guard, index_skill, _skill, type);
                            } else {
                                // Clone TAMBAHAN (hasil menu tambah clone) juga harus bisa
                                // jadi target attack langsung, sama seperti guard utama di atas.
                                for (Clone extra : temp_mob.extraGuards) {
                                    if (extra != null && extra.objectId == n2) {
                                        ListATK.add(extra.objectId);
                                        MainObject.attack(gameMap, conn.p, extra, index_skill, _skill, type);
                                        break;
                                    }
                                }
                            }
                        }
                    } else {


                        if (n3 >= -1000 && n3 < 0) {
                            for (MobAi ai : gameMap.Ai_entrys) {
                                if (ai != null && ai.objectId == n3) {
                                    try {
                                        MainObject.attack(gameMap, conn.p, ai, index_skill, _skill, type);
                                        ListATK.add(ai.objectId);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                }
                            }
                        } else {
                            PlayerClone.atk(gameMap, conn.p, n2, index_skill, (int) 3000);


                        }
                    }
                }
            }
            if (!ListATK.isEmpty()) {
                int prKham = 0;
                if ((prKham = conn.p.getTotalItemParam(100)) > 0) {
                    if (ListATK.contains(conn.p.kham.idAtk_HN)) {
                        conn.p.kham.CountAtk_HN++;
                    } else {
                        conn.p.kham.idAtk_HN = ListATK.get(Util.nextInt(ListATK.size())); // FIX: nextInt(n) = 0..n-1, random(n) = 0..n (out of bounds!)
                        conn.p.kham.CountAtk_HN = 1;
                    }
                    if (prKham > 0 && conn.p.kham.CountAtk_HN >= prKham) {
                        conn.p.kham.idAtk_HN = 0;
                        conn.p.kham.CountAtk_HN = 0;
                        conn.p.addEffectMedal(StrucEff.NgocHonNguyen, 0, System.currentTimeMillis() + 3000);
                        Eff_special_skill.send_eff_kham(conn.p, StrucEff.NgocHonNguyen, 3000);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void Fire_Mob_DiBuon(GameMap gameMap, Session conn, Pet_di_buon pet_di_buon, int index_skill, int dameBase)
            throws IOException {
        if (dameBase < 0) {
            dameBase = 0;
        }
        if (pet_di_buon != null) {
            pet_di_buon.hp -= dameBase;
            if (pet_di_buon.hp <= 0) {
                pet_di_buon.hp = 0;
                Message mout = new Message(8);
                mout.writer().writeShort(pet_di_buon.objectId);
                for (int i1 = 0; i1 < gameMap.players.size(); i1++) {
                    Player p0 = gameMap.players.get(i1);
                    if (p0 != null) {
                        p0.conn.addmsg(mout);
                    }
                }
                mout.cleanup();
                Pet_di_buon_manager.remove(pet_di_buon.name);
                pet_di_buon.p.pet_di_buon = null;
                for (int j = 0; j < pet_di_buon.item.size(); j++) {
                    ItemMap it_leave = new ItemMap();
                    it_leave.id_item = (short) pet_di_buon.item.get(j);
                    it_leave.color = (byte) 0;
                    it_leave.quantity = 1;
                    it_leave.category = 3;
                    it_leave.idmaster = (short) pet_di_buon.p.objectId;
                    it_leave.op = new ArrayList<>();
                    it_leave.time_exist = System.currentTimeMillis() + 60_000L;
                    it_leave.time_pick = System.currentTimeMillis() + 1_500L;
                    gameMap.add_item_map_leave(gameMap, conn.p, it_leave, pet_di_buon.objectId);
                }
            }
            Message m_atk = new Message(9);
            m_atk.writer().writeShort(conn.p.objectId);
            m_atk.writer().writeByte(index_skill);
            m_atk.writer().writeByte(1);
            m_atk.writer().writeShort(pet_di_buon.objectId);
            m_atk.writer().writeInt((int) dameBase); // dame
            m_atk.writer().writeInt(pet_di_buon.hp); // hp mob after
            m_atk.writer().writeByte(0);
            m_atk.writer().writeInt(conn.p.hp);
            m_atk.writer().writeInt(conn.p.mp);
            m_atk.writer().writeByte(11); // 1: green, 5: small white 9: big white, 10: st dien, 11: st bang
            m_atk.writer().writeInt(0); // dame plus
            MapService.sendMsgPlayerInside(gameMap, conn.p, m_atk, true);
            m_atk.cleanup();
            pet_di_buon.update_all(conn.p);
        }
    }

    public static void use_item_arena(GameMap gameMap, Player p, short id) throws IOException {
        // FIX: sebelumnya kalau pemain tidak berada persis di map markas
        // sendiri (ch == false, lihat switch(gameMap.mapId) di bawah) atau
        // event battlefield sedang tidak aktif, method ini diam-diam tidak
        // melakukan apa-apa - item TIDAK dipakai/dikonsumsi, tapi pemain juga
        // TIDAK dapat notifikasi apapun. Dari sudut pandang pemain ini persis
        // seperti "item tidak bisa dipakai" tanpa penjelasan. Sekarang tiap
        // jalur gagal dikasih notice yang jelas.
        if (!GameMap.isBattlefieldMap(gameMap.mapId)) {
            Service.send_notice_box(p.conn, "Item ini hanya bisa dipakai di area BTF/battlefield");
            return;
        }
        if (p.time_use_item_arena < System.currentTimeMillis()) {
            if (gameMap.time_use_item_arena < System.currentTimeMillis()) {
                    boolean ch = false;
                    switch (gameMap.mapId) {
                        case 54: {
                            if (p.typepk == 5) {
                                ch = true;
                            }
                            break;
                        }
                        case 56: {
                            if (p.typepk == 2) {
                                ch = true;
                            }
                            break;
                        }
                        case 58: {
                            if (p.typepk == 4) {
                                ch = true;
                            }
                            break;
                        }
                        case 60: {
                            if (p.typepk == 1) {
                                ch = true;
                            }
                            break;
                        }
                    }
                    if (!ch) {
                        Service.send_notice_box(p.conn,
                                "Kamu harus berada di markas milikmu sendiri untuk memakai item ini");
                        return;
                    }
                    {
                        switch (id) {
                            case 57: {
                                // TAMBAHAN: "Kemarahan markas" - buff kecepatan serangan
                                // markas utama (mob_id 89-92) selama 1 menit. Markas milik
                                // sendiri ada di gameMap ini juga (ch di atas sudah
                                // memastikan p.typepk cocok dengan pemilik map ini).
                                for (MobInMap mob : gameMap.mobs) {
                                    if (mob.isMobCTruongHouse()) {
                                        mob.atkSpeedBuffUntil = System.currentTimeMillis() + 60_000L;
                                    }
                                }
                                break;
                            }
                            case 58: {
                                // FIX: kalau tidak ada satupun musuh (typepk beda) di map
                                // ini saat item dipakai, loop di bawah tidak mengubah
                                // apapun - item tetap dianggap "berhasil" dipakai (kena
                                // cooldown 250 detik & item ke-consume) padahal efeknya
                                // nihil sama sekali. Ini kelihatan persis seperti "item
                                // tidak bisa dipakai" buat pemain. Beri tahu kalau memang
                                // tidak ada target yang kena efek.
                                boolean anyTransformed = false;
                                for (int i2 = 0; i2 < gameMap.players.size(); i2++) {
                                    Player p0 = gameMap.players.get(i2);
                                    if (p0.typepk != p.typepk) {
                                        anyTransformed = true;
                                        // Message m = new Message(6);
                                        // m.writer().writeByte(1);
                                        // m.writer().writeShort(102);
                                        // m.writer().writeShort(p0.id);
                                        // for (int i = 0; i < map.players.size(); i++) {
                                        // Player p01 = map.players.get(i);
                                        // p01.conn.addmsg(m);
                                        // }
                                        // m.cleanup();
                                        p0.id_henshin = 102;
                                        // FIX: durasi sebelumnya 6 detik, padahal deskripsi
                                        // item ("Mengubah lawan menjadi Babi selama 1 menit")
                                        // menyebutkan 1 menit (60 detik).
                                        p0.time_henshin = System.currentTimeMillis() + 60_000L;
                                        for (int i = 0; i < gameMap.players.size(); i++) {
                                            Player p01 = gameMap.players.get(i);
                                            // FIX: sebelumnya panggil overload 3-argumen yang
                                            // TIDAK PERNAH mengirim field bienhinh ke client sama
                                            // sekali. Status id_henshin sudah di-set di server tapi
                                            // efek transform tidak pernah tampil di client manapun.
                                            // Pakai overload 4-argumen supaya id_henshin benar-benar
                                            // dikirim (lihat juga baris 1633: kode debug admin lama
                                            // yang memanggil overload ini dengan pola yang sama).
                                            MapService.send_in4_other_char(gameMap, p01, p0, p0.id_henshin);
                                        }
                                        // TAMBAHAN: time_henshin di-set tapi sebelumnya tidak
                                        // pernah dicek di manapun untuk mengembalikan tampilan ke
                                        // normal - id_henshin cuma direset di set_in4() (login).
                                        // Tanpa ini transform jadi PERMANEN sampai pemain relog,
                                        // bukan 1 menit seperti seharusnya. Jadwalkan revert +
                                        // broadcast ulang tampilan normal setelah waktunya habis.
                                        Player target = p0;
                                        long henshinAtSchedule = target.time_henshin;
                                        utils.Timer.schedule(() -> {
                                            if (target.time_henshin != henshinAtSchedule || target.id_henshin != 102) {
                                                return; // sudah ditimpa transform lain / sudah direset duluan
                                            }
                                            target.id_henshin = -1;
                                            try {
                                                for (Player viewer : gameMap.players) {
                                                    MapService.send_in4_other_char(gameMap, viewer, target, -1);
                                                }
                                            } catch (IOException ignore) {
                                            }
                                        }, 60_000L);
                                    }
                                }
                                if (!anyTransformed) {
                                    Service.send_notice_box(p.conn,
                                            "Tidak ada musuh di markas ini untuk diubah jadi babi");
                                }
                                break;
                            }
                            case 59: {
                                // TAMBAHAN: "Kemarahan prajurit" - buff kekuatan penjaga
                                // markas ("Penjaga"/linh canh, ai.PlayerClone) selama 1
                                // menit. Cukup penjaga yang ada di gameMap (map) ini.
                                for (PlayerClone clone : event_daily.Battlefield.gI().list_ai) {
                                    if (clone.gameMap.equals(gameMap)) {
                                        clone.powerBuffUntil = System.currentTimeMillis() + 60_000L;
                                    }
                                }
                                break;
                            }
                            case 60: {
                                // TAMBAHAN: "Kemarahan monster" - buff kekuatan monster
                                // (bos "Ular Ratu") tim sendiri selama 1 menit. Bos yang
                                // relevan ada di gameMap (map) ini juga, bukan bos musuh
                                // di map lain.
                                for (MobInMap mob : gameMap.mobs) {
                                    if (mob.isBoss() && !mob.isMobCTruongHouse()) {
                                        mob.powerBuffUntil = System.currentTimeMillis() + 60_000L;
                                    }
                                }
                                break;
                            }
                        }
                        p.item.remove(4, id, 1);
                        p.updatePointArena(10);
                        gameMap.time_use_item_arena = System.currentTimeMillis() + 250_000L;
                        p.time_use_item_arena = System.currentTimeMillis() + 250_000L;
                    }
                } else {
                    Service.send_notice_box(p.conn,
                            "Gunakan nanti " + (gameMap.time_use_item_arena - System.currentTimeMillis()) / 1000 + " s");
                }
            } else {
                Service.send_notice_box(p.conn,
                        "Gunakan nanti " + (p.time_use_item_arena - System.currentTimeMillis()) / 1000 + " s");
            }
    }
}