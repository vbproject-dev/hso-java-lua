package client;

import ai.Clone;
import ai.MobAi;
import core.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static core.Service.send_notice_nobox_white;

import core.lua.JavaToLua;
import core.lua.LuaToJava;
import core.lua.NativeLua;
import event_daily.MoLy;
import event_daily.Battlefield;
import game.ai.AiManager;
import game.ai.PlayerBot;
import game.event.GameEventManager;
import game.event.kemerdekaan.KemerdekaanEvent;
import game.event.kemerdekaan.PersimpanganKematian;
import game.event.halloween.GerbangArwah;
import game.event.halloween.KristalArwahEvent;
import game.guild.GuildService;
import game.pvp.PvpManager;
import client.io.Message;
import client.io.Session;
import lombok.extern.slf4j.Slf4j;
import game.map.Dungeon;
import game.map.DungeonManager;
import game.map.GameMap;
import game.map.MapService;
import menu.MenuManager;
import menu.NPCHandler;
import game.quest.QuestManager;
import game.rewards.RewardManager;
import model.event.GlobalEvent;
import template.PartData;
import template.PartDataLoader;
import utils.SQLHelper;

@Slf4j
public class MessageHandler {

    private final Session conn;

    public MessageHandler(Session conn) {
        this.conn = conn;
    }

    public void process_msg(Message m) throws IOException {


        boolean isHandledByLua = JavaToLua.call("core.LuaBridge.onMessage", new Object[]{conn, m});
        if (isHandledByLua) {
            return;
        }

        switch (m.cmd) {
            case -100: {
                GameSrc.jadeInlay(conn.p, m);
                break;
            }
            case -102: {
                GameSrc.playerStore(conn, m);
                break;
            }
            case -91: {
                int available = m.reader().available();
                switch (available) {
                    case 1 -> {
                        if (conn.ac_admin >= 10) {
                            MenuManager.openAdminMenu(conn.p);
                        } else if (conn.ac_admin >= 1) {
                            // SMOD (staff junior) — menu terbatas, bukan full GM Menu
                            MenuManager.openModMenu(conn.p);
                        } else if (conn.ac_admin >= 2) {
                            MenuManager.openSmodMenu(conn.p);
                        } else {
//                            String[] menu = new String[]{"Buka peti", "Roda putar", "Poin PK", "Ke Kota",
//                                    "Drop material medal", "Lihat turnamen"};
//                            MenuController.send_menu_select(conn, -91, menu);
                            MenuManager.openUserMenu(conn.p);
                        }
                    }

                    case 2 -> {
                        MoLy.Lottery_process(conn.p, m);
                    }
                    case 4 -> {
                        //Service.remove_time_use_item(conn, m);
                        //AdminMenuController.handleOtherMenu(conn, m);
                        MenuManager.openOtherMenu(conn.p, m);
                    }
                }

                break;
            }
            case 77: {
                GameSrc.Wings_Process(conn, m);
                break;
            }
            case -105: {

                if (conn.p.isCreateItemStar) {
                    GameSrc.ActionsItemStar(conn, m);
                } else {
                    GameSrc.Create_Medal(conn, m);
                }
                break;
            }
            case 69: {
                byte type = m.reader().readByte();
                if (type == 11) {
                    Player p0 = GameMap.get_player_by_name(m.reader().readUTF());
                    if (p0 != null && p0.myclan != null) {
                        p0.myclan.accept_mem(conn, p0);
                    }

                } else if (type == 13 || type == 15) {
                    GuildService.getInstance().onGuildMessage(conn, type, m);
                } else if (conn.p.myclan != null) {
                    conn.p.myclan.clan_process(conn, m, type);
                } else {
                    GuildService.getInstance().onGuildMessage(conn, type, m);
                }
                break;
            }
            case 73: {
                GameSrc.replace_item_process(conn.p, m);
                break;
            }
            case 36: {
                if (conn.p.level < 100) {
                    Service.send_notice_box(conn, "Level 100 diperlukan untuk melakukan trade");
                    return;
                }
                GameSrc.trade_process(conn, m);
                break;
            }
            case 48: {
                conn.p.map.create_party(conn, m);
                break;
            }
            case 67: {
                GameSrc.rebuild_item(conn, m);
                break;
            }
            case 68: {
                PvpManager.gI().handlePVP(conn, m);
                break;
            }
            case 9: {
                // if (conn.p.map.map_id == 48) {
                // conn.p.dungeon.use_skill(conn, m);
                // } else {
                // conn.p.map.use_skill(;
                // }
                MapService.use_skill(conn.p.map, conn, m, 0);
                break;
            }
            case 6: {
                // if (conn.p.map.map_id == 48) {
                // } else {
                // conn.p.map.use_skill(conn, m, 1);
                // }
                MapService.use_skill(conn.p.map, conn, m, 1);
                break;
            }
            case 40: {
                // if (conn.p.map.map_id == 48) {
                // } else {
                // conn.p.map.buff_skill(conn, m);
                // }
                MapService.buff_skill(conn.p.map, conn, m);
                break;
            }
            case 20: {
                conn.p.map.pick_item(conn, m);
                // if (conn.p.map.map_id == 48) {
                // } else {
                // conn.p.map.pick_item(conn, m);
                // }
                break;
            }
            case 11: {
                if (conn.p.time_speed_rebuild > System.currentTimeMillis()) {
                    if (++conn.p.enough_time_disconnect > 2) {
                        conn.close();
                    }
                    return;
                }
                conn.p.time_speed_rebuild = System.currentTimeMillis() + 500L;
                conn.p.enough_time_disconnect = 0;
                UseItem.ProcessItem3(conn, m);
                break;
            }
            case -107: {
                if (conn.p.time_speed_rebuild > System.currentTimeMillis()) {
                    if (++conn.p.enough_time_disconnect > 2) {
                        conn.close();
                    }
                    return;
                }
                conn.p.time_speed_rebuild = System.currentTimeMillis() + 500L;
                conn.p.enough_time_disconnect = 0;
                UseItem.ProcessItem7(conn, m);
                break;
            }
            case 32: {
                if (conn.p.time_speed_rebuild > System.currentTimeMillis()) {
                    if (++conn.p.enough_time_disconnect > 2) {
                        conn.close();
                    }
                    return;
                }
                conn.p.time_speed_rebuild = System.currentTimeMillis() + 500L;
                conn.p.enough_time_disconnect = 0;
                UseItem.ProcessItem4(conn, m);
                break;
            }
            case 24: {
                Service.buy_item(conn.p, m);
                break;
            }
            case 18: {
                Service.sell_item(conn, m);
                break;
            }
            case 37: {
                // arena
                break;
            }
            case 65: {
                if (conn.p.wedding_chest_open) {
                    byte type = m.reader().readByte();
                    short id  = m.reader().readShort();
                    byte tem  = m.reader().readByte();
                    short num = m.reader().readShort();

                    // type == -1 adalah refresh (num bisa 0, tidak masalah)
                    if (type == -1) {
                        event_daily.Wedding wed = event_daily.Wedding.get_obj(conn.p.name);
                        if (wed != null) wed.sendChestTo(conn.p);
                        break;
                    }
                    // untuk deposit/withdraw, num harus > 0
                    if (num <= 0) break;

                    event_daily.Wedding wed = event_daily.Wedding.get_obj(conn.p.name);
                    if (wed == null) {
                        conn.p.wedding_chest_open = false;
                        break;
                    }
                    if (tem != 3) {
                        // wedding chest hanya support item3 (equipment)
                        Service.send_notice_box(conn, "Gudang pasangan hanya untuk item equipment!");
                        break;
                    }
                    if (type == 1) {
                        // deposit: id = index slot bag player
                        wed.depositItem(conn.p, id);
                    } else {
                        // withdraw: type=0 atau selain 1 = ambil dari gudang (sama seperti box_process)
                        wed.withdrawItem(conn.p, id);
                    }
                } else {
                    conn.p.item.box_process(m);
                }
                break;
            }
            case 44: {
                Service.pet_process(conn, m);
                break;
            }
            case 45: {
                Service.handlePetEat(conn, m);
                break;
            }
            case 35: {
                conn.p.friend_process(m);
                break;
            }
            case 34: {
                Service.chatTab(conn, m);
                break;
            }
            case 22: {
                conn.p.plus_point(m);
                break;
            }
            case -32: {
                Process_Yes_no_box.process(conn, m);
                break;
            }
            case -106: {
                Service.send_item7_template(conn.p, m);
                break;
            }
            case -97: {
                conn.p.down_mount(m);
                break;
            }
            case 28: {
                Service.send_in4_item(conn, m);
                break;
            }
            case 31: {
                // if (conn.p.map.map_id == 48) {
                // conn.p.dungeon.request_livefromdie(conn, m);
                // } else {
                // conn.p.map.request_livefromdie(conn, m);
                // }
                MapService.request_livefromdie(conn.p.map, conn, m);
                break;
            }
            case -31: {
                isHandledByLua = JavaToLua.call("core.LuaBridge.onInput", new Object[]{conn, m});
                if (!isHandledByLua) {
                    TextFromClient.process(conn, m);
                }
                break;
            }
            case -53: {
                TextFromClient_2.process(conn, m);
                break;
            }
            case 21: {
                Service.send_param_item_wear(conn, m);
                break;
            }
            case 51: {
                conn.p.change_zone(conn, m);
                break;
            }
            case 52: {
                QuestManager.getInstance().onReceived(conn, m);
                break;
            }
            case 42: {
                MapService.changeFlag(conn.p.map, conn.p, m.reader().readByte());
                break;
            }
            case 49: {
                Service.send_view_other_player_in4(conn, m);
                break;
            }
            case 71: {

                Service.chat_KTG(conn, m);
                break;
            }
            case -30: {
                MenuController.handleDynamicMenu(conn, m);
                break;
            }
            case 23: {

                if (conn.p.getCurrentShop() != null) {
                    conn.p.setCurrentShop(null);
                    conn.p.setCurrentShopPriceType(null);
                }


                int npcId = m.reader().readByte();
                Boolean handled = JavaToLua.call("core.LuaBridge.onTalk", new Object[]{conn, npcId});
                isHandledByLua = Boolean.TRUE.equals(handled);
                if (isHandledByLua) {
                    return;
                }

                // FIX Halloween: id NPC dikirim ke client sebagai BYTE (-128..127).
                // NPC -131..-134 ter-wrap jadi 125..122 di wire; kembalikan ke id asli.
                if (npcId >= 122 && npcId <= 125) {
                    npcId -= 256;
                }


                GlobalEvent globalEvent = GameEventManager.gI().getGlobalEvent(npcId);
                if (globalEvent != null) {
                    MenuManager.openGlobalMenu(npcId, conn.p);
                    return;
                }
                if (NPCHandler.isHandled((byte) npcId)) {
                    NPCHandler.handle(conn.p, (byte) npcId);
                    return;
                }
                MenuController.request_menu(conn, npcId);
                break;
            }
            case 27: {
                MapService.send_chat(conn.p.map, conn, m);
                break;
            }
            case 12: {
                conn.p.isChangemap = false;
                conn.p.wedding_chest_open = false; // reset flag gudang saat pindah map
//                if (GameMap.isBattlefieldMap(conn.p.map.mapId)) {
//                    BTFManager.getInstance().sendInfoMap(conn.p);

//                    Message m22 = new Message(4);
//                    for (int i = 0; i < Battlefield.gI().list_ai.size(); i++) {
//                        PlayerClone p0 = Battlefield.gI().list_ai.get(i);
//                        if (!p0.isdie && p0.map.equals(conn.p.map)) {
//                            m22.writer().writeByte(0);
//                            m22.writer().writeShort(0);
//                            m22.writer().writeShort(p0.id);
//                            m22.writer().writeShort(p0.x);
//                            m22.writer().writeShort(p0.y);
//                            m22.writer().writeByte(-1);
//                        }
//                    }
//                    if (m22.writer().size() > 0) {
//                        for (int i = 0; i < conn.p.map.players.size(); i++) {
//                            Player p0 = conn.p.map.players.get(i);
//                            p0.conn.addmsg(m22);
//                        }
//                    }
//                    m22.cleanup();
//                }
                if (conn.p.map.mapId == 48) {
                    // weather map dungeon
                    Message mw = new Message(76);
                    mw.writer().writeByte(4);
                    mw.writer().writeShort(-1);
                    mw.writer().writeShort(-1);
                    conn.addmsg(mw);
                    mw.cleanup();
                } else if (conn.p.map.mapId == PersimpanganKematian.MAP_ID) {
                    // Efek cuaca ambient selama event Persimpangan Kematian
                    // berlangsung — dikirim ulang tiap kali player masuk/pindah
                    // zona map 117, sama seperti pola map 48. Cuma nyala kalau
                    // event-nya memang sedang aktif (bukan cuma karena player
                    // kebetulan ada di map 117 di luar jadwal event).
                    PersimpanganKematian ev = GameEventManager.gI().getEvent(PersimpanganKematian.class);
                    if (ev != null && ev.isRunning()) {
                        Message mw = new Message(76);
                        mw.writer().writeByte(4); // TODO: cek ke tim client, mau reuse tipe cuaca yang sama dgn map 48 atau beda
                        mw.writer().writeShort(-1);
                        mw.writer().writeShort(-1);
                        conn.addmsg(mw);
                        mw.cleanup();
                    }
                } else if (conn.p.map.mapId == GerbangArwah.MAP_ID) {
                    // Sama seperti map 117 di atas, tapi untuk event Halloween
                    // "Malam Suro" (Gerbang Arwah) — efek cuaca ambient dikirim
                    // ulang tiap kali player masuk/pindah zona map event ini,
                    // cuma nyala kalau event-nya sedang aktif.
                    GerbangArwah ev = GameEventManager.gI().getEvent(GerbangArwah.class);
                    if (ev != null && ev.isRunning()) {
                        Message mw = new Message(76);
                        mw.writer().writeByte(4); // TODO: sama seperti map 117, cek ke tim client apakah tipe cuaca ini yang dipakai
                        mw.writer().writeShort(-1);
                        mw.writer().writeShort(-1);
                        conn.addmsg(mw);
                        mw.cleanup();
                    }
                }
                break;
            }
            case -44: {
                short reqId = m.reader().readShort();
                if (conn.p.map.mapId == 48) {
                    Dungeon d = DungeonManager.get_list(conn.p.name);
                    if (d != null && reqId == 32001) {
                        d.send_in4_npc(conn, reqId);
                    } else if (MapService.isCelebrationMeteorId(reqId)) {
                        MapService.sendCelebrationMeteorInfo(conn, reqId);
                    }
                } else if (conn.p.map.mapId == PersimpanganKematian.MAP_ID) {
                    PersimpanganKematian ev = GameEventManager.gI().getEvent(PersimpanganKematian.class);
                    if (ev != null && ev.getKristal() != null && reqId == game.event.kemerdekaan.KristalEvent.METEOR_VISUAL_ID) {
                        ev.getKristal().sendMeteorInfo(conn);
                    } else if (MapService.isCelebrationMeteorId(reqId)) {
                        MapService.sendCelebrationMeteorInfo(conn, reqId);
                    }
                } else if (conn.p.map.mapId == GerbangArwah.MAP_ID) {
                    // Sama seperti map 117 di atas, tapi untuk visual lilin
                    // "Lilin Malam Suro" (Gerbang Arwah) — kalau client minta
                    // info objek ini sendiri (misal setelah reconnect atau
                    // pindah zona), jawab manual sama seperti push awal di
                    // KristalArwahEvent.sendLilinFall()/sendLilinInfo().
                    GerbangArwah ev = GameEventManager.gI().getEvent(GerbangArwah.class);
                    if (ev != null && ev.getKristal() != null && reqId == KristalArwahEvent.LILIN_VISUAL_ID) {
                        ev.getKristal().sendLilinInfo(conn);
                    } else if (MapService.isCelebrationMeteorId(reqId)) {
                        MapService.sendCelebrationMeteorInfo(conn, reqId);
                    }
                } else {
                    // FIX: dulu request -44 di map manapun selain 48/117 tidak
                    // dijawab sama sekali, sehingga objek visual perayaan
                    // "Meteor Kemerdekaan" (broadcastMeteorCelebrationAllMaps)
                    // tidak pernah nongol di map lain (mis. map 1) walau paket
                    // spawn-nya sendiri terkirim sukses tanpa error.
                    if (MapService.isCelebrationMeteorId(reqId)) {
                        MapService.sendCelebrationMeteorInfo(conn, reqId);
                    }
                }
                break;
            }
            case 5: {
                // int id = Short.toUnsignedInt(m.reader().readShort());
                int id = m.reader().readShort();
                if (id < 0) {
                    // Dulu cuma id >= -1000 yg dicek di sini. Padahal counter mob/clone
                    // (Manager.get_index_mob_new()) turun bebas dari -20 sampai -5000
                    // sebelum wrap balik ke -20. Jadi objectId clone/mob yg kebetulan
                    // < -1000 (gampang kejadi di server yg udah lama nyala) gagal
                    // dikonversi ke unsigned int di bawah, dan gak pernah ketemu waktu
                    // dicocokkan ke cloneList (yg objectId-nya disimpan dlm bentuk
                    // unsigned) — makanya clone tambang jadi tidak pernah dikirim
                    // data visualnya ke client (invisible), walau tetap hidup di server.
                    for (MobAi temp : conn.p.map.Ai_entrys) {
                        if (temp != null && temp.objectId == id) {
                            temp.send_in4(conn.p);
                            return;
                        }
                    }
                    id = Short.toUnsignedInt((short) id);
                }

                if (id >= 10_000) {
                    PlayerBot bot = AiManager.getInstance().get(id);
                    if (bot != null) {
                        bot.sendBotInfo(conn.p);
                        return;
                    }
                }

                Player p0 = null;
                for (int i = 0; i < conn.p.map.players.size(); i++) {
                    Player p01 = conn.p.map.players.get(i);
                    if (p01.objectId == id) {
                        p0 = p01;
                        break;
                    }
                }
                if (p0 != null) {
                    MapService.send_in4_other_char(conn.p.map, conn.p, p0);
                } else if (GameMap.isSummonMap(conn.p.map, true)) {

                    Clone temp = null;
                    for (int i = 0; i < Manager.gI().cloneList.size(); i++) {
                        Clone temp2 = Manager.gI().cloneList.get(i);
                        if (temp2.objectId == id) {
                            temp = temp2;
                            break;
                        }
                    }
                    if (temp != null) {
                        try {
                            temp.send_in4(conn.p);
                        } catch (Exception e) {
                            // Sebelumnya ditelan diam-diam (catch Exception ignored) -
                            // itu yang bikin bug "clone tambang tidak ada visual" susah
                            // ketauan, karena gagal render nggak pernah muncul di log.
                            log.error("Gagal kirim info clone (objectId={}) ke player {}",
                                    temp.objectId, conn.user, e);
                        }
                    }

                } else if (GameMap.isBattlefieldMap(conn.p.map.mapId)) {
                    Battlefield.gI().get_ai(conn.p, id);
                } else {
                    Message m3 = new Message(8);
                    m3.writer().writeShort(id);
                    conn.addmsg(m3);
                    m3.cleanup();
                }
                break;
            }
            case 7: {
                int n = Short.toUnsignedInt(m.reader().readShort());
                if (n >= 30_000 && n < 31_000)// mob event
                {
                    return;
                }
                if (n > 10_000 && n < 11_000) {// mob boss
                    conn.p.map.BossIn4(conn, n);
                    return;
                }
                Dungeon d = DungeonManager.get_list(conn.p.name);
                if (d != null) {
                    d.send_mob_in4(conn, n);
                } else {
                    Service.mob_in4(conn.p, n);
                }
                break;
            }
            case 4: {
                // if (conn.p.map.map_id == 48) {
                // conn.p.dungeon.send_move(conn.p, m);
                // } else {
                // conn.p.map.send_move(conn.p, m);
                // }
                MapService.send_move(conn.p.map, conn.p, m);
                break;
            }
            case -51: {
                Service.send_icon(conn, m);
                break;
            }
            case -52: {
                try {
                    byte type = m.reader().readByte();
                    short id = m.reader().readShort();
                    // Debug log ini dimatikan — kepanggil tiap kali client minta
                    // part/icon (zoom, render karakter, dll), jadi spam terus-terusan
                    // di console. Aktifkan lagi cuma kalau lagi debug part data yang
                    // hilang (pakai log.debug, bukan println, biar bisa di-toggle
                    // lewat level logging tanpa perlu ubah kode lagi).
                    // log.debug("[PART REQUEST] type={} id={} zoomlv={}", type, id, conn.zoomlv);
                    PartData part = PartDataLoader.getByZoom(conn.zoomlv, type, id);
                    if (part == null) {
                        log.warn("Part data tidak ditemukan: type={} id={} zoomlv={}", type, id, conn.zoomlv);
                        return;
                    }
                    Message m2 = new Message(-52);
                    m2.writer().writeByte(part.type);
                    m2.writer().writeShort(part.id);

                    m2.writer().writeInt(part.image.length);
                    m2.writer().write(part.image);
                    m2.writer().write(part.imageData);
                    conn.addmsg(m2);
                    m2.cleanup();
                } catch (IOException e) {

                }
                break;
            }
            case 55: {
                Service.save_rms(conn, m);
                break;
            }
            case 59: {
                Service.send_health(conn.p);
                break;
            }
            case 13: {
                try {
                    login(m);
                } catch (Exception e) {
                    if (Manager.logErrorLogin) {
                        e.printStackTrace();
                    }
                    conn.close();
                }
                break;
            }
            case 14: {
                conn.charCreate(m);
                break;
            }
            case 1: {
                if (!conn.get_in4) {
                    conn.getClientInfo(m);
                }
                break;
            }
            case 61: {
                // Service.send_msg_data(conn, 61, Manager.gI().msg_61);
                Service.sendNameServer(conn);
                Service.send_item_template(conn);
                Service.send_msg_data(conn, 26, Manager.gI().load_msg26());
                break;
            }
            case -103: {// click mob minuong
                byte b = m.reader().readByte();
                if (b != 0) {
                    break;
                }
                short id = (short) (m.reader().readShort() - 1000);
                MenuController.send_menu_select(conn, id, new String[]{"Petik buah"}, (byte) Manager.gI().event);
                break;
            }
            default: {
                System.out.println("default onRecieveMsg : " + m.cmd);
                break;
            }
        }
    }

    private void login(Message m) throws IOException {
        if (conn.p == null) {
            m.reader().readByte(); // type login
            int id_player_login = m.reader().readInt();

            if (!SQLHelper.selectFrom("player").
                    where("id", id_player_login).
                    where("uid", conn.id)
                    .exists()) {
                conn.close();
                return;

            }

            Player p0 = new Player(conn, id_player_login);

            if (p0.setup()) {
                // synchronized (Session.client_entrys) {
                //
                // }
                Session[] sessionSnapshot = Session.snapshotList();
                for (int i = sessionSnapshot.length - 1; i >= 0; i--) {
                    Session s = sessionSnapshot[i];
                    if (s == null || s.equals(conn) || s.user == null) {
                        continue;
                    }
                    if (s.get_in4 && s.id == conn.id && s.connected) {
                        try {
                            if (conn.socket.isConnected() && s.socket.isConnected()) {
                                System.out.println("-----errorLogin ----conn: " + conn.socket.getInetAddress()
                                        + "-----lastConnect: " + s.socket.getInetAddress());
                            } else {
                                System.out.println("+---- errorLogin ----+");
                            }
                        } catch (Exception e) {
                        }
                        conn.close();
                        s.close();
                        // synchronized (Session.client_entrys) {
                        // Session.client_entrys.remove(conn);
                        // if(Session.client_entrys.get(i).id == conn.id)
                        // Session.client_entrys.remove(i);
                        // }
                        return;
                    }
                }
                // Jika ada player AFK dengan ID yang sama masih di map, hentikan botnya
                stopAfkBotIfActive(p0.objectId, p0.map);
                conn.p = p0;
                conn.p.set_in4();
                conn.p.setOnline();
                conn.SaveIP();

                // Claim kimcuong harian membership
                feature.membership.MembershipService.claimDailyGem(p0);

                MessageHandler.dataloginmap(conn);
            }
        }
    }

    /**
     * Hentikan AFK bot jika player dengan objectId yang sama masih aktif di map.
     * Dipanggil saat player login kembali setelah mode AFK.
     */
    private static void stopAfkBotIfActive(int objectId, game.map.GameMap playerMap) {
        try {
            // Cari di semua map — tidak pakai map_id karena field itu tidak diset dari DB
            for (game.map.GameMap[] zones : game.map.GameMap.entrys) {
                if (zones == null) continue;
                for (game.map.GameMap gameMap : zones) {
                    if (gameMap == null) continue;
                    for (int i = gameMap.players.size() - 1; i >= 0; i--) {
                        client.Player old = gameMap.players.get(i);
                        if (old != null && old.objectId == objectId && old.modeBot) {
                            // ── Tutup record DB sebelum matikan bot (fix: cegah is_active numpuk)
                            // Cek apakah panel sudah stop duluan (is_active=0) — kalau sudah,
                            // jangan panggil AfkLogger.stop() lagi (double write)
                            boolean panelAlreadyStopped = false;
                            if (old.afkBot != null) {
                                try (java.sql.Connection c = core.SQL.gI().getConnection();
                                     java.sql.PreparedStatement ps = c.prepareStatement(
                                         "SELECT `is_active` FROM `afk_log` WHERE `player_name` = ? " +
                                         "ORDER BY `id` DESC LIMIT 1")) {
                                    ps.setString(1, old.name);
                                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                                        if (rs.next()) panelAlreadyStopped = rs.getInt("is_active") == 0;
                                    }
                                } catch (Exception ignored) {}
                            }
                            if (!panelAlreadyStopped && old.afkBot != null) {
                                old.afkBot.stopBotManual();
                            } else {
                                old.modeBot = false;
                                old.afkBot  = null;
                            }
                            gameMap.players.remove(i);
                            log.info("[AFK] Bot dihentikan untuk player id={}, login kembali.", objectId);
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[AFK] Gagal menghentikan bot lama: {}", e.getMessage());
        }
    }

    private static void dataloginmap(Session conn) throws IOException {
        Service.send_quest(conn);
        Service.send_auto_atk(conn);
        Service.sendMainCharInfo(conn.p);
        Service.send_msg_data(conn, 1, Manager.gI().msg_1);
        Service.send_skill(conn.p);
        Service.send_login_rms(conn);

        // ── Notif Event Kemerdekaan Indonesia 🇮🇩 ────────────────────────────
        KemerdekaanEvent kemerdekaanEvent = GameEventManager.gI().getEvent(KemerdekaanEvent.class);
        if (kemerdekaanEvent != null && !kemerdekaanEvent.shouldBeRemoved()) {
            Service.send_notice_nobox_yellow(conn,
                "🇮🇩 Event Kemerdekaan Indonesia sedang aktif! Temui Paskibraka, Pak Lurah, " +
                "dan Nenek Merdeka di desa untuk hadiah spesial 17 Agustus!");
        }
        // ──────────────────────────────────────────────────────────────────────

        send_notice_nobox_white(conn, ("Guild " + Manager.nameClanThue
                + " Sedang Menguasai Hak Pajak di Area Perdagangan. " + "(" + " Pajak " + Manager.thue + " % " + ")"));
        game.guild.Guild topMineGuild = Manager.gI().mine.getTopGuildByMineCount();
        if (topMineGuild != null) {
            Service.send_notice_nobox_yellow(conn,
                    ("Guild " + topMineGuild.name + " Sedang Menguasai Tambang Terbanyak Saat Ini. " + "(" + " Total " + Manager.gI().mine.getMineCount(topMineGuild) + " Tambang " + ")"));
        }
        game.guild.Guild topGoldGuild = game.guild.Guild.getTopGuildByGold();
        if (topGoldGuild != null) {
            Service.send_notice_nobox_yellow(conn,
                    ("Guild " + topGoldGuild.name + " Adalah Guild Dengan Gold Terbanyak Saat Ini. " ));
        }
        Service.send_notice_nobox_yellow(conn,
                ("Knight " + conn.p.name + " Saat Ini Adalah Pahlawan Terkuat di Dunia Knight."));
        // add x2 xp
        conn.p.set_x2_xp(1);
        // Lanjutkan timer premium teleport jika ada sisa sesi sebelum offline
        feature.teleport.PremiumTeleportManager.gI().resumeSession(conn.p);
        MapService.enter(conn.p.map, conn.p);

        if (conn.p.mount != null) {
            conn.p.map.sendUseMount(conn.p);
        }

        if (conn.p.myclan != null) {
            conn.p.myclan.sendNotice(conn.p);
        }

        //
        if (GameMap.name_mo.equals(conn.p.name)) {
            Manager.gI().chatKTGprocess("Selamat Datang Grand Knight " + conn.p.name + " Telah Login ke Game");
        }
        if (conn.p.myclan != null && BXH.BXH___GUILD.indexOf(conn.p.myclan) < 1) {
            Manager.gI().chatKTGprocess("Selamat Datang Pemimpin Guild " + (BXH.BXH___GUILD.indexOf(conn.p.myclan) + 1)
                    + conn.p.name + " Login ke Game");
        }
        if (conn.ac_admin == 10) {
            Manager.gI().chatKTGprocess("@System : GM [" + conn.p.name + "] baru saja login ke game!");
        }

        RewardManager.sendReward(conn);
        QuestManager.getInstance().sendQuest(conn.p);

    }

}