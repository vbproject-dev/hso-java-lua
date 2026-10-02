package core;

import utils.ItemInlay;
import utils.CheckItem;
import history.His_KMB;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import client.Player;
import equipment.StatGenerator;
import client.io.Message;
import client.io.Session;
import game.items.UpgradeSystem;
import game.map.GameMap;
import game.map.MapService;
import template.Item3;
import template.Item47;
import template.ItemTemplate3;
import template.ItemTemplate7;
import template.Medal_Material;
import template.Option;
import template.PartFashion;
import template.Player_store;

public class GameSrc {

    public static byte[] percent = new byte[]{95, 90, 80, 70, 60, 50, 45, 35, 25, 15, 10, 5, 4, 3, 2, 1};
    public static short[] wing_upgrade_material_long_khuc_xuong = new short[]{10, 12, 15, 19, 24, 30, 37, 45, 54, 64,
            75, 87, 100, 114, 129, 145, 162, 180, 199, 219, 240, 262, 285, 309, 334, 360, 387, 415, 444, 474};
    public static short[] wing_upgrade_material_kim_loai = new short[]{20, 22, 25, 29, 34, 40, 47, 55, 64, 74, 85, 97,
            110, 124, 139, 155, 172, 190, 209, 229, 250, 272, 295, 319, 344, 370, 397, 425, 454, 484};
    public static short[] wing_upgrade_material_da_cuong_hoa = new short[]{50, 54, 60, 68, 78, 90, 104, 120, 138, 158,
            180, 204, 230, 258, 288, 320, 354, 390, 428, 468, 510, 554, 600, 648, 698, 750, 804, 860, 918, 978};
    public static int[] wing_upgrade_material_gold = new int[]{50000, 52000, 55000, 59000, 64000, 70000, 77000, 85000,
            94000, 104000, 115000, 127000, 140000, 154000, 169000, 185000, 202000, 220000, 239000, 259000, 280000, 302000,
            325000, 349000, 374000, 400000, 427000, 455000, 484000, 514000};
    public static int[] wing_upgrade_material_time = new int[]{2, 4, 6, 8, 12, 15, 20, 30, 45, 60, 100, 130, 150,
            180, 220, 242, 286, 322, 340, 380, 422, 456, 502, 550, 600, 652, 706, 742, 770, 830};

    public static void rebuild_item(Session conn, Message m2) throws IOException {
        byte type = m2.reader().readByte();
        short id = m2.reader().readShort();
        byte tem = m2.reader().readByte(); // type item insert
        // System.out.println(type);
        // System.out.println(id);
        // System.out.println(tem);
        if (tem == 7 && conn.p.id_item_rebuild == -1) {
            Service.send_notice_nobox_white(conn, "Masukkan item yang akan di upgrade terlebih dahulu");
            return;
        }
        if (tem == 7) {
            if (id != 13 && id != 14 && id != 4) {
                Service.send_notice_box(conn, "Sebaiknya hanya gunakan semanggi 4 daun atau batu 3 warna");
                return;
            }
            Message m = new Message(67);
            m.writer().writeByte(type);
            int tier = conn.p.item.bag3[conn.p.id_item_rebuild].tier;
            switch (type) {
                case 0: {
                    m.writer().writeShort(id);
                    conn.p.id_use_mayman = id;
                    m.writer().writeByte(7);
                    m.writer().writeUTF(GameSrc.percent[tier] + "% + 30%");
                    conn.p.is_use_mayman = true;
                    break;
                }
                case 1: {
                    m.writer().writeByte(7);
                    m.writer().writeUTF(GameSrc.percent[tier] + "%");
                    conn.p.is_use_mayman = false;
                    conn.p.id_use_mayman = -1;
                    break;
                }
                default: {
                    Service.send_notice_box(conn, "Fitur belum tersedia");
                    return;
                }
            }
            conn.addmsg(m);
            m.cleanup();
        }
        else {
            Message m = new Message(67);
            Message m12 = null;
            boolean next = false;
            if (conn.ac_admin > 3 && Manager.BuffAdminMaterial) {
                next = true;
            }
            if (type == 2 && !checkmaterial(conn, conn.p.id_item_rebuild, tem) && !next) {
                m.writer().writeByte(3);
                if (conn.p.getGold() < 10_000 || conn.p.getGem() < 10) {
                    m.writer().writeUTF("Dibutuhkan lebih dari 10k emas dan 10 permata untuk melanjutkan ");
                } else {
                    m.writer().writeUTF("Kamu masih kekurangan material...");
                }
            } else {
                m.writer().writeByte(type);
                switch (type) {
                    case 0: {
                        Item3 checkit = conn.p.item.bag3[id];
                        if ((checkit.id >= 4587 && checkit.id <= 4590) || checkit.id == 5288) {
                            Service.send_notice_box(conn, "Tidak valid!");
                            return;
                        }
                        Item3 it = conn.p.item.bag3[id];
                        if (it == null || !check_item_can_rebuild(it.type)) {
                            Service.send_notice_box(conn, "Tidak valid!");
                            return;
                        }
                        if (PartFashion.fashions.contains(checkit.id) || PartFashion.fashions.contains(it.id)) {
                            Service.send_notice_box(conn, "Perlengkapan tidak cocok!");
                            return;
                        }
                        m.writer().writeShort(id);
                        conn.p.id_item_rebuild = id;
                        m.writer().writeByte(3);
                        m.writer().writeUTF(GameSrc.percent[it.tier] + "%");
                        // remove item may man
                        Message m1212 = new Message(67);
                        m1212.writer().writeByte(1);
                        m1212.writer().writeByte(7);
                        m1212.writer().writeUTF(GameSrc.percent[it.tier] + "%");
                        conn.addmsg(m1212);
                        m1212.cleanup();
                        conn.p.is_use_mayman = false;
                        conn.p.id_use_mayman = -1;
                        //
                        break;
                    }
                    case 1: {
                        m.writer().writeByte(3);
                        m.writer().writeUTF("HSO"); // 0,01%
                        conn.p.id_item_rebuild = -1;
                        break;
                    }
                    case 2: {

                        if (conn.p.time_speed_rebuild > System.currentTimeMillis()) {
                            if (++conn.p.enough_time_disconnect > 2) {
                                conn.close();
                            }
                            return;
                        }
                        conn.p.time_speed_rebuild = System.currentTimeMillis() + 4500L;
                        conn.p.enough_time_disconnect = 0;
                        Item3 it_upgrade = conn.p.item.bag3[conn.p.id_item_rebuild];
                        if (it_upgrade == null) {
                            return;
                        }
                        if (PartFashion.fashions.contains(it_upgrade.id)) {
                            Service.send_notice_box(conn, "Perlengkapan tidak cocok!");
                            return;
                        }

                        if (it_upgrade.tier > 14) {
                            return;
                        }
                        boolean percent;
                        int get_percent = GameSrc.percent[it_upgrade.tier];
                        if (it_upgrade.tier < 7) {
                            get_percent += 30;
                        }
                        if (conn.ac_admin > 3 && Manager.BuffAdmin) {
                            percent = true;
                        } else if (conn.p.is_use_mayman && (conn.p.id_use_mayman == 14 || conn.p.id_use_mayman == 13)) {
                            percent = (get_percent + 15) > Util.random(0, 106); //cũ là 120
                        } else {
                            percent = get_percent > Util.random(0, 106);
                        }
                        if (percent) {
                            conn.p.item.bag3[conn.p.id_item_rebuild].tier++;
                            if (it_upgrade.tier >= 9 && conn.p.item.bag3[conn.p.id_item_rebuild].type == 5) {
                                for (int i = 0; i < conn.p.item.bag3[conn.p.id_item_rebuild].op.size(); i++) {
                                    if (conn.p.item.bag3[conn.p.id_item_rebuild].op.get(i).id == 37
                                            || conn.p.item.bag3[conn.p.id_item_rebuild].op.get(i).id == 38) {
                                        conn.p.item.bag3[conn.p.id_item_rebuild].op.get(i).setParam(2);
                                    }
                                }
                            }
                        } else {
                            if (conn.p.is_use_mayman && (conn.p.id_use_mayman == 14 || conn.p.id_use_mayman == 13)) {
                                if (conn.p.id_use_mayman == 13) {
                                    if (it_upgrade.tier > 6 && it_upgrade.tier != 11) {
                                        it_upgrade.tier -= 1;
                                    }
                                }
                            } else {
                                if (it_upgrade.tier >= 11) {
                                    it_upgrade.tier = 11;
                                } else if (it_upgrade.tier >= 6) {
                                    it_upgrade.tier = 6;
                                } else if (it_upgrade.tier >= 1) {
                                    it_upgrade.tier = 1;
                                }
                            }
                        }
                        if (conn.p.is_use_mayman
                                && (conn.p.id_use_mayman == 14 || conn.p.id_use_mayman == 13 || conn.p.id_use_mayman == 4)) {
                            conn.p.item.remove(7, conn.p.id_use_mayman, 1);
                            conn.p.item.charInventory(7);
                            if (conn.p.item.total_item_by_id(7, conn.p.id_use_mayman) < 1) {
                                conn.p.is_use_mayman = false;
                                conn.p.id_use_mayman = -1;
                            }
                        }
                        conn.p.item.charInventory(4);
                        conn.p.item.charInventory(7);
                        conn.p.item.charInventory(3);
                        String per = GameSrc.percent[it_upgrade.tier] + "%";
                        if (tem == 0) { // vang
                            if (!percent) {
                                m.writer().writeByte(4);
                                m.writer().writeUTF("Aku turut menyesal, proses peningkatan telah gagal.");
                            } else {
                                m.writer().writeByte(3);
                                m.writer().writeUTF("Kamu telah berhasil meningkatkan " + it_upgrade.name + "!");
                            }
                        } else if (tem == 1) { // ngoc
                            if (!percent) {
                                m.writer().writeByte(4);
                                m.writer().writeUTF("Aku turut menyesal, proses peningkatan telah gagal.");
                            } else {
                                m.writer().writeByte(3);
                                m.writer().writeUTF("Kamu telah berhasil meningkatkan " + it_upgrade.name + "!");
                            }
                        }
                        if (it_upgrade.tier < 15) {
                            m12 = new Message(67);
                            m12.writer().writeByte(0);
                            m12.writer().writeShort(conn.p.id_item_rebuild);
                            m12.writer().writeByte(3);
                            m12.writer().writeUTF(per);
                        }
                        break;
                    }
                    default: {
                        Service.send_notice_box(conn, "Fitur belum tersedia");
                        return;
                    }
                }
            }
            conn.addmsg(m);
            m.cleanup();
            if (m12 != null) {
                conn.addmsg(m12);
                m12.cleanup();
            }
        }
    }

    private static boolean check_item_can_rebuild(byte type) {
        switch (type) {
            case 0: // coat
            case 1: // pant
            case 2: // crown
            case 3: // grove
            case 4: // ring
            case 5: // chain
            case 6: // shoes
                // case 7: // wing
                //case 15: // fashion
            case 8:
            case 9:
            case 10:
            case 16:
                //case 21:
                //case 22:
                //case 23:
                //case 24:
                //case 25:
                //case 26:
            case 11: {
                return true;
            }
        }
        return false;
    }

    private static boolean checkmaterial(Session conn, short id, byte tem) throws IOException {
        if (conn.p.getGold() < 10_000 || conn.p.getGem() < 25) {
            return false;
        }
        if (!conn.p.item.bag3[id].islock) {
            conn.p.item.bag3[id].islock = true;
            conn.p.item.bag3[id].name = ItemTemplate3.item.get(conn.p.item.bag3[id].id).getName() + " [Terkunci]";
        }
        Item3 it3 = conn.p.item.bag3[id];
        switch (it3.tier) {
            case 0:
            case 1:
            case 2: {
                if (tem == 0) {
                    conn.p.updateGold(-3000);
                } else {
                    conn.p.updateGem(-4);
                }
                if (conn.p.item.total_item_by_id(7, 0) > 0 && conn.p.item.total_item_by_id(7, 1) > 0) {
                    conn.p.item.remove(7, 0, 1);
                    conn.p.item.remove(7, 1, 1);
                    updatematerial(conn, 7, new short[]{0, 1});
                    return true;
                }
                break;
            }
            case 3:
            case 4:
            case 5: {
                if (tem == 0) {
                    conn.p.updateGold(-4000);
                } else {
                    conn.p.updateGem(-5);
                }
                if (conn.p.item.total_item_by_id(7, 0) > 0 && conn.p.item.total_item_by_id(7, 1) > 0) {
                    conn.p.item.remove(7, 0, 1);
                    conn.p.item.remove(7, 1, 1);
                    updatematerial(conn, 7, new short[]{0, 1});
                    return true;
                }
                break;
            }
            case 6:
            case 7:
            case 8: {
                if (tem == 0) {
                    conn.p.updateGold(-6000);
                } else {
                    conn.p.updateGem(-6);
                }
                if (conn.p.item.total_item_by_id(7, 0) > 0 && conn.p.item.total_item_by_id(7, 1) > 0
                        && conn.p.item.total_item_by_id(7, 2) > 0) {
                    conn.p.item.remove(7, 0, 1);
                    conn.p.item.remove(7, 1, 1);
                    conn.p.item.remove(7, 2, 1);
                    updatematerial(conn, 7, new short[]{0, 1, 2});
                    return true;
                }
                break;
            }
            case 9:
            case 10: {
                if (tem == 0) {
                    conn.p.updateGold(-8000);
                } else {
                    conn.p.updateGem(-7);
                }
                if (conn.p.item.total_item_by_id(7, 0) > 0 && conn.p.item.total_item_by_id(7, 1) > 0
                        && conn.p.item.total_item_by_id(7, 2) > 0) {
                    conn.p.item.remove(7, 0, 1);
                    conn.p.item.remove(7, 1, 1);
                    conn.p.item.remove(7, 2, 1);
                    updatematerial(conn, 7, new short[]{0, 1, 2});
                    return true;
                }
                break;
            }
            case 11:
            case 12:
            case 13:
            case 14: {
                if (tem == 0) {
                    conn.p.updateGold(-8000);
                } else {
                    conn.p.updateGem(-7);
                }
                if (conn.p.item.total_item_by_id(7, 0) > 0 && conn.p.item.total_item_by_id(7, 1) > 0
                        && conn.p.item.total_item_by_id(7, 2) > 0 && conn.p.item.total_item_by_id(7, 3) > 0) {
                    conn.p.item.remove(7, 0, 1);
                    conn.p.item.remove(7, 1, 1);
                    conn.p.item.remove(7, 2, 1);
                    conn.p.item.remove(7, 3, 1);
                    updatematerial(conn, 7, new short[]{0, 1, 2, 3});
                    return true;
                }
                break;
            }
        }
        return false;
    }

    private static void updatematerial(Session conn, int b, short[] id) throws IOException {
        conn.p.item.charInventory(7);
        Message m = new Message(16);
        for (int i = 0; i < id.length; i++) {
            m.writer().writeByte(1);
            m.writer().writeByte(b);
            m.writer().writeLong(conn.p.getGold());
            m.writer().writeInt(conn.p.getGem());
            m.writer().writeByte(b);
            m.writer().writeShort(id[i]);
            m.writer().writeShort(conn.p.item.total_item_by_id(b, id[i]));
            m.writer().writeByte(1);
            m.writer().writeByte(0);
            conn.addmsg(m);
            m.cleanup();
        }
    }

    public synchronized static void trade_process(Session conn, Message m2) throws IOException {
        if (!Manager.isTrade) {
            Service.send_notice_box(conn, "Tidak bisa bertransaksi");
            return;
        }
        byte type = m2.reader().readByte();
        if (type == 0) {
            // if (conn.p.time_trade > System.currentTimeMillis()) {
            // Service.send_notice_nobox_white(conn,
            // "Chờ " + (conn.p.time_trade - System.currentTimeMillis()) / 1000 + "s nữa");
            // return;
            // }
            // conn.p.time_trade = System.currentTimeMillis() + 1000L * 15;
        }
        switch (type) {
            case 0: {
                Player p0 = GameMap.get_player_by_name(m2.reader().readUTF());
                if (p0 != null) {
                    if (p0.list_item_trade == null) {
                        Message m = new Message(36);
                        m.writer().writeByte(0);
                        m.writer().writeUTF(conn.p.name);
                        p0.conn.addmsg(m);
                        m.cleanup();
                    } else {
                        Service.send_notice_box(conn, "Pihak lain sedang dalam transaksi");
                    }
                } else {
                    Service.send_notice_box(conn, "Terjadi kesalahan");
                }
                break;
            }
            case 1: {
                Message m = new Message(36);
                m.writer().writeByte(1);
                Player p0 = GameMap.get_player_by_name(m2.reader().readUTF());
                if (p0 == null) {
                    return;
                }
                m.writer().writeUTF(p0.name);
                conn.p.name_trade = p0.name;
                p0.name_trade = conn.p.name;
                conn.addmsg(m);
                m.cleanup();
                //
                m = new Message(36);
                m.writer().writeByte(1);
                m.writer().writeUTF(conn.p.name);
                p0.conn.addmsg(m);
                m.cleanup();
                p0.list_item_trade = new short[9];
                conn.p.list_item_trade = new short[9];
                for (int i = 0; i < conn.p.list_item_trade.length; i++) {
                    conn.p.list_item_trade[i] = -1;
                    p0.list_item_trade[i] = -1;
                }
                break;
            }
            case 2: {
                Player p0 = GameMap.get_player_by_name(conn.p.name_trade);
                if (p0 == null) {
                    return;
                }
                int index_item_trade = -1;
                for (int i = 0; i < conn.p.list_item_trade.length; i++) {
                    if (conn.p.list_item_trade[i] == -1) {
                        index_item_trade = i;
                        break;
                    }
                }
                if (index_item_trade == -1) {
                    Service.send_notice_box(conn, "Tidak bisa menambahkan item");
                    return;
                }
                byte typeit = m2.reader().readByte();
                short idit = m2.reader().readShort();
                switch (typeit) {
                    case 3: {
                        // FIX: cegah item duplikat - tolak jika idit sudah ada di list_item_trade
                        boolean already_in_trade = false;
                        for (short existing : conn.p.list_item_trade) {
                            if (existing == idit) {
                                already_in_trade = true;
                                break;
                            }
                        }
                        if (already_in_trade) {
                            Service.send_notice_box(conn, "Item sudah ada di slot transaksi");
                            return;
                        }
                        conn.p.list_item_trade[index_item_trade] = idit;
                        Message m = new Message(36);
                        m.writer().writeByte(2);
                        m.writer().writeByte(1); // index
                        m.writer().writeByte(typeit);
                        m.writer().writeShort(-1);
                        Item3 it3 = conn.p.item.bag3[idit];
                        m.writer().writeShort(it3.icon);
                        m.writer().writeUTF(it3.name);
                        m.writer().writeByte(it3.color);
                        m.writer().writeByte(conn.p.clazz);
                        m.writer().writeShort(it3.level);
                        m.writer().writeByte(it3.tier); // tier
                        m.writer().writeByte(it3.op.size());
                        for (int i = 0; i < it3.op.size(); i++) {
                            m.writer().writeByte(it3.op.get(i).id);
                            m.writer().writeInt(it3.op.get(i).getParam(it3.tier));
                        }
                        p0.conn.addmsg(m);
                        m.cleanup();
                        break;
                    }
                }
                break;
            }
            case 3: {
                byte typeit = m2.reader().readByte();
                short idit = m2.reader().readShort();
                switch (typeit) {
                    case 3: {
                        for (Short itbuffer : conn.p.list_item_trade) {
                            if (itbuffer > -1) {
                                Message m = new Message(36);
                                m.writer().writeByte(3);
                                m.writer().writeByte(1); // index
                                m.writer().writeByte(typeit);
                                m.writer().writeShort(-1);
                                Item3 it3 = conn.p.item.bag3[itbuffer];
                                m.writer().writeShort(it3.icon);
                                m.writer().writeUTF(it3.name);
                                m.writer().writeByte(it3.color);
                                m.writer().writeByte(conn.p.clazz);
                                m.writer().writeShort(it3.level);
                                m.writer().writeByte(it3.tier); // tier
                                m.writer().writeByte(it3.op.size());
                                for (int i = 0; i < it3.op.size(); i++) {
                                    m.writer().writeByte(it3.op.get(i).id);
                                    m.writer().writeInt(it3.op.get(i).getParam(it3.tier));
                                }
                                Player p0 = GameMap.get_player_by_name(conn.p.name_trade);
                                if (p0 == null) {
                                    m.cleanup();
                                    return;
                                }
                                p0.conn.addmsg(m);
                                m.cleanup();
                            }
                        }
                        for (int i = 0; i < conn.p.list_item_trade.length; i++) {
                            if (conn.p.list_item_trade[i] == idit) {
                                conn.p.list_item_trade[i] = -1;
                                break;
                            }
                        }
                        for (Short itbuffer : conn.p.list_item_trade) {
                            if (itbuffer > -1) {
                                Message m = new Message(36);
                                m.writer().writeByte(2);
                                m.writer().writeByte(1); // index
                                m.writer().writeByte(typeit);
                                m.writer().writeShort(-1);
                                Item3 it3 = conn.p.item.bag3[itbuffer];
                                m.writer().writeShort(it3.icon);
                                m.writer().writeUTF(it3.name);
                                m.writer().writeByte(it3.color);
                                m.writer().writeByte(conn.p.clazz);
                                m.writer().writeShort(it3.level);
                                m.writer().writeByte(it3.tier); // tier
                                m.writer().writeByte(it3.op.size());
                                for (int i = 0; i < it3.op.size(); i++) {
                                    m.writer().writeByte(it3.op.get(i).id);
                                    m.writer().writeInt(it3.op.get(i).getParam(it3.tier));
                                }
                                Player p0 = GameMap.get_player_by_name(conn.p.name_trade);
                                if (p0 == null) {
                                    m.cleanup();
                                    return;
                                }
                                p0.conn.addmsg(m);
                                m.cleanup();
                            }
                        }
                        break;
                    }
                }
                break;
            }
            case 4: {
                conn.p.lock_trade = true;
                Player p0 = GameMap.get_player_by_name(conn.p.name_trade);
                if (p0 == null) {
                    return;
                }
                Service.send_notice_nobox_white(p0.conn, (conn.p.name + " mengunci transaksi!"));
                if (conn.p.lock_trade && p0.lock_trade) {
                    Message m = new Message(36);
                    m.writer().writeByte(4);
                    m.writer().writeByte(2);
                    p0.conn.addmsg(m);
                    conn.addmsg(m);
                    m.cleanup();
                }
                break;
            }
            case 5: {
                conn.p.accept_trade = true;
                Player p0 = GameMap.get_player_by_name(conn.p.name_trade);
                if (p0 == null) {
                    return;
                }
                Service.send_notice_nobox_white(p0.conn, (conn.p.name + " mengonfirmasi transaksi!"));
                if (conn.p.accept_trade && p0.accept_trade) {
                    //
                    if (conn.p.money_trade > 0) {
                        if (conn.p.getGold() < conn.p.money_trade) {
                            Service.send_notice_box(conn, "Emas tidak cukup");
                            return;
                        }
                        conn.p.updateGold(-conn.p.money_trade);
                        p0.updateGold(conn.p.money_trade);
                        Log.gI().add_log(p0.name,
                                "bertransaksi dengan " + conn.p.name + " menerima " + Util.number_format(conn.p.money_trade) + " emas");
                        conn.p.money_trade = 0;
                    }
                    if (p0.money_trade > 0) {
                        if (p0.getGold() < p0.money_trade) {
                            Service.send_notice_box(p0.conn, "Emas tidak cukup");
                            return;
                        }
                        conn.p.updateGold(p0.money_trade);
                        p0.updateGold(-p0.money_trade);
                        Log.gI().add_log(conn.p.name,
                                "bertransaksi dengan " + p0.name + " menerima " + Util.number_format(p0.money_trade) + " emas");
                        p0.money_trade = 0;
                    }
                    // trade item p1
                    List<Item3> itembag3buffer = new ArrayList<>();
                    java.util.Set<Short> visited_idx_p1 = new java.util.HashSet<>();
                    for (short itbuffer : conn.p.list_item_trade) {
                        // FIX: skip index yang udah pernah diproses (cegah dupe kalau list korup/duplikat)
                        if (itbuffer > -1 && visited_idx_p1.add(itbuffer)) {
                            itembag3buffer.add(conn.p.item.bag3[itbuffer]);
                        }
                    }
                    for (Item3 it : itembag3buffer) {
                        for (int i = 0; i < p0.item.bag3.length; i++) {
                            if (p0.item.bag3[i] == null) {
                                p0.item.bag3[i] = it;
                                break;
                            }
                        }
                    }
                    for (short itbuffer : conn.p.list_item_trade) {
                        if (itbuffer > -1) {
                            conn.p.item.bag3[itbuffer] = null;
                        }
                    }
                    // trade item p2
                    itembag3buffer.clear();
                    itembag3buffer = new ArrayList<>();
                    java.util.Set<Short> visited_idx_p2 = new java.util.HashSet<>();
                    for (short itbuffer : p0.list_item_trade) {
                        // FIX: skip index yang udah pernah diproses (cegah dupe kalau list korup/duplikat)
                        if (itbuffer > -1 && visited_idx_p2.add(itbuffer)) {
                            itembag3buffer.add(p0.item.bag3[itbuffer]);
                        }
                    }
                    for (Item3 it : itembag3buffer) {
                        for (int i = 0; i < conn.p.item.bag3.length; i++) {
                            if (conn.p.item.bag3[i] == null) {
                                conn.p.item.bag3[i] = it;
                                break;
                            }
                        }
                    }
                    for (short itbuffer : p0.list_item_trade) {
                        if (itbuffer > -1) {
                            p0.item.bag3[itbuffer] = null;
                        }
                    }
                    p0.item.charInventory(4);
                    conn.p.item.charInventory(4);
                    p0.item.charInventory(7);
                    conn.p.item.charInventory(7);
                    p0.item.charInventory(3);
                    conn.p.item.charInventory(3);
                    //
                    Message m = new Message(36);
                    m.writer().writeByte(5);
                    m.writer().writeByte(2);
                    p0.conn.addmsg(m);
                    conn.addmsg(m);
                    m.cleanup();
                    //
                    conn.p.name_trade = "";
                    p0.name_trade = "";
                    conn.p.lock_trade = false;
                    p0.lock_trade = false;
                    conn.p.money_trade = 0;
                    p0.money_trade = 0;
                    conn.p.accept_trade = false;
                    p0.accept_trade = false;
                    conn.p.list_item_trade = null;
                    p0.list_item_trade = null;
                } else {
                    Message m = new Message(36);
                    m.writer().writeByte(5);
                    m.writer().writeByte(1);
                    p0.conn.addmsg(m);
                    m.cleanup();
                    m = new Message(36);
                    m.writer().writeByte(5);
                    m.writer().writeByte(0);
                    conn.addmsg(m);
                    m.cleanup();
                }
                break;
            }
            case 6: {
                Message m = new Message(36);
                m.writer().writeByte(6);
                conn.addmsg(m);
                Player p0 = GameMap.get_player_by_name(conn.p.name_trade);
                if (p0 == null) {
                    return;
                }
                p0.conn.addmsg(m);
                m.cleanup();
                conn.p.name_trade = "";
                p0.name_trade = "";
                conn.p.lock_trade = false;
                p0.lock_trade = false;
                conn.p.money_trade = 0;
                p0.money_trade = 0;
                conn.p.accept_trade = false;
                p0.accept_trade = false;
                conn.p.list_item_trade = null;
                p0.list_item_trade = null;
                break;
            }
            case 7: {
                int money = m2.reader().readInt();
                if (money < 0) {
                    money = 0;
                }
                if (money > conn.p.getGold()) {
                    money = (int) conn.p.getGold();
                }
                if (money > 2_000_000_000) {
                    money = 2_000_000_000;
                }
                conn.p.money_trade = money;
                Message m = new Message(36);
                m.writer().writeByte(7);
                m.writer().writeInt(money);
                Player p0 = GameMap.get_player_by_name(conn.p.name_trade);
                if (p0 == null) {
                    return;
                }
                p0.conn.addmsg(m);
                m.cleanup();
                break;
            }
            case 9: {
                String text = m2.reader().readUTF();
                Message m = new Message(36);
                m.writer().writeByte(9);
                m.writer().writeUTF(text);
                Player p0 = GameMap.get_player_by_name(conn.p.name_trade);
                if (p0 == null) {
                    return;
                }
                p0.conn.addmsg(m);
                m.cleanup();
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur belum tersedia");
                break;
            }
        }
    }

    public static void replace_item_process(Player p, Message m2) throws IOException {
        byte type = m2.reader().readByte();
        short id = m2.reader().readShort();
        if (type == 0) {
            if (id < 0 || id >= p.item.bag3.length) return;
            if (p.item.bag3[id] != null && !check_item_can_rebuild(p.item.bag3[id].type)) {
                Service.send_notice_box(p.conn, "Tidak valid!");
                return;
            }
            if (p.item.bag3[id] != null && PartFashion.fashions.contains(p.item.bag3[id].id)) {
                Service.send_notice_box(p.conn, "Perlengkapan tidak cocok!");
                return;
            }
            if (p.item_replace == -1) {
                if (p.item.bag3[id] != null && utils.CheckItem.isMedal(p.item.bag3[id].id)) {
                    Service.send_notice_box(p.conn, "Perlengkapan tidak cocok!");
                    return;
                }
                if (p.item.bag3[id] != null && p.item.bag3[id].tier < 6) {
                    Service.send_notice_box(p.conn, "Masukkan item di atas +6 terlebih dahulu");
                    return;
                }
                p.item_replace = id;
            } else if (p.item_replace2 == -1) {
                if (id < 0 || id >= p.item.bag3.length || p.item.bag3[id] == null) {
                    Service.send_notice_box(p.conn, "Item tidak valid");
                    return;
                }
                if (p.item_replace < 0 || p.item_replace >= p.item.bag3.length || p.item.bag3[p.item_replace] == null) {
                    p.item_replace = -1;
                    Service.send_notice_box(p.conn, "Error, silakan coba lagi");
                    return;
                }
                if (p.item.bag3[id].tier > 0) {
                    Service.send_notice_box(p.conn, "Hanya bisa memasukkan item +0");
                    return;
                }
                if (p.item.bag3[p.item_replace].type != p.item.bag3[id].type) {
                    Service.send_notice_box(p.conn, "Hanya bisa mentransfer perlengkapan dengan tipe yang sama");
                    return;
                }
                if (p.item.bag3[p.item_replace].level > p.item.bag3[id].level) {
                    Service.send_notice_box(p.conn, "Hanya bisa mentransfer ke perlengkapan level lebih tinggi");
                    return;
                }
                p.item_replace2 = id;
            } else {
                Service.send_notice_box(p.conn, "Error, silakan coba lagi");
                return;
            }
            Message m = new Message(73);
            m.writer().writeByte(0);
            m.writer().writeShort(id);
            if (p.item_replace2 == -1) {
                m.writer().writeByte(1);
            } else {
                m.writer().writeByte(0);
            }
            p.conn.addmsg(m);
            m.cleanup();
        } else if (type == 1) {
            if (p.item.bag3[p.item_replace].tier < p.item.bag3[p.item_replace2].tier) {
                Service.send_notice_box(p.conn, "Proses transfer sudah selesai!!");
                return;
            }
            if (!p.item.bag3[p.item_replace2].islock) {
                p.item.bag3[p.item_replace2].islock = true;
                p.item.bag3[p.item_replace2].name
                        = ItemTemplate3.item.get(p.item.bag3[p.item_replace2].id).getName() + " [Terkunci]";
                p.item.bag3[p.item_replace2].updateName();
                p.item.charInventory(4);
                p.item.charInventory(7);
                p.item.charInventory(3);
            }
            int fee = 100 * p.item.bag3[p.item_replace].tier;
            Service.send_box_input_yesno(p.conn, 122,
                    "Proses transfer akan memakan biaya " + fee + " permata, apakah Anda yakin ingin melanjutkan?");
        }
    }

    private static int get_ngoc_medal_upgrade(byte tier) {
        int[] result = new int[]{1, 2, 5, 10, 15, 20, 25, 50, 75, 100, 150, 230, 280, 360, 480};
        return result[tier];
    }

    private static int get_quant_material_medal_upgrade(byte tier) {
        return (2 + tier);
    }

    /**
     * Dapatkan index medal berdasarkan item ID.
     * 4587=0 (Ksatria), 4588=1 (Penyihir), 4589=2 (Assasin), 4590=3 (Penembak), 5288=4 (MBG)
     */
    public static int getMedalIndex(short id) {
        if (id >= 4587 && id <= 4590) return id - 4587;
        if (id == 5288) return 4;
        return -1;
    }

    /**
     * Dapatkan item ID medal berdasarkan index.
     * 0=4587, 1=4588, 2=4589, 3=4590, 4=5288
     */
    public static short getMedalItemIdFromIndex(int index) {
        if (index >= 0 && index <= 3) return (short)(index + 4587);
        if (index == 4) return (short)5288;
        return -1;
    }

    /**
     * Inisialisasi material untuk pembuatan medal jika belum pernah di-generate.
     * Dipanggil saat player membuka menu create medal.
     */
    public static void initMedalMaterial(client.Player p, int medalIndex) {
        initMedalMaterial(p, medalIndex, (byte) 0);
    }

    public static void initMedalMaterial(client.Player p, int medalIndex, byte tier) {
        if (p.medal_create_material == null || p.medal_create_material.length < 25) {
            short[] newArr = new short[25];
            if (p.medal_create_material != null) {
                System.arraycopy(p.medal_create_material, 0, newArr, 0, Math.min(p.medal_create_material.length, 25));
            }
            p.medal_create_material = newArr;
        }
        int base = 5 * medalIndex;
        // Cek semua 5 slot - jika ada yang 0, generate ulang semua
        boolean allValid = true;
        for (int i = base; i < base + 5; i++) {
            if (p.medal_create_material[i] == 0) {
                allValid = false;
                break;
            }
        }
        if (allValid) return;

        // Level material berdasarkan tier medal:
        // Selalu pakai level 3 (+200) → ID 246-345, karena item lv4/5/6 bahan medali tidak ada di DB
        int levelOffset = 200;

        // Pilih 2 row m_white berbeda agar nama material tidak duplikat
        int row1 = Util.random(0, 6);
        int row2 = (row1 + 1 + Util.random(0, 5)) % 7; // dijamin berbeda

        p.medal_create_material[base]     = (short) (Medal_Material.m_white[row1][Util.random(0, 9)] + levelOffset);
        p.medal_create_material[base + 1] = (short) (Medal_Material.m_white[row2][Util.random(0, 9)] + levelOffset);
        p.medal_create_material[base + 2] = (short) (Medal_Material.m_blue[Util.random(0, 9)] + levelOffset);
        p.medal_create_material[base + 3] = (short) (Medal_Material.m_yellow[Util.random(0, 9)] + levelOffset);
        p.medal_create_material[base + 4] = (short) (Medal_Material.m_violet[Util.random(0, 9)] + levelOffset);
    }

    /**
     * Medal V2 — versi terpisah dari initMedalMaterial(), pakai
     * template.Medal_Material_V2 (ID sendiri, tidak bentrok dengan Medal V1).
     * Tidak ada offset tier: material V2 adalah item khusus, bukan varian level.
     */
    public static void initMedalMaterialV2(client.Player p, int medalIndex) {
        if (p.medal_create_material_v2 == null || p.medal_create_material_v2.length < 20) {
            short[] newArr = new short[20];
            if (p.medal_create_material_v2 != null) {
                System.arraycopy(p.medal_create_material_v2, 0, newArr, 0, Math.min(p.medal_create_material_v2.length, 20));
            }
            p.medal_create_material_v2 = newArr;
        }
        int base = 5 * medalIndex;
        boolean allValid = true;
        for (int i = base; i < base + 5; i++) {
            if (p.medal_create_material_v2[i] == 0) {
                allValid = false;
                break;
            }
        }
        if (allValid) return;

        int row1 = Util.random(0, template.Medal_Material_V2.m_white.length - 1);
        int row2;
        do {
            row2 = Util.random(0, template.Medal_Material_V2.m_white.length - 1);
        } while (row2 == row1 && template.Medal_Material_V2.m_white.length > 1);

        p.medal_create_material_v2[base]     = template.Medal_Material_V2.m_white[row1][Util.random(0, 9)];
        p.medal_create_material_v2[base + 1] = template.Medal_Material_V2.m_white[row2][Util.random(0, 9)];
        p.medal_create_material_v2[base + 2] = template.Medal_Material_V2.m_blue[Util.random(0, 9)];
        p.medal_create_material_v2[base + 3] = template.Medal_Material_V2.m_yellow[Util.random(0, 9)];
        p.medal_create_material_v2[base + 4] = template.Medal_Material_V2.m_violet[Util.random(0, 9)];
    }

    public static void Create_Medal(Session conn, Message m2) throws IOException {

        byte type = m2.reader().readByte();
        short id = -1;
        byte tem = -1;
        try {
            id = m2.reader().readShort();
            tem = m2.reader().readByte();
        } catch (IOException e) {
        }
        System.out.println("[MEDAL_DEBUG] Create_Medal: type=" + type + " id=" + id + " tem=" + tem + " savedSlot=" + conn.p.id_Upgrade_Medal_Star);
        if (id != -1 && tem == 7) {
            byte type_item = ItemTemplate7.item.get(id).getType();
            if (type_item != 54) {
                Service.send_notice_box(conn, "Item tidak valid!");
                return;
            }
        }
        switch (type) {
            case 0: { // fusion
                if (tem == 7) {
                    if (id >= 246 && id < 346) {
                        Service.send_notice_box(conn, "Level item telah mencapai maksimal!");
                        return;
                    }
                    int quant = conn.p.item.total_item_by_id(7, id);
                    if (quant < 5) {
                        Service.send_notice_box(conn, "Material tidak cukup untuk menggabungkan minimal 1 item!");
                    } else {
                        Message m = new Message(-105);
                        m.writer().writeByte(0);
                        m.writer().writeByte(5);
                        m.writer().writeShort(id);
                        conn.addmsg(m);
                        m.cleanup();
                    }
                } else if (tem == 3) {
                    Item3 it_temp = conn.p.item.bag3[id];
                    boolean isMedalV1 = it_temp != null
                            && (it_temp.id == 4587 || it_temp.id == 4588 || it_temp.id == 4589 || it_temp.id == 4590 || it_temp.id == 5288);
                    boolean isMedalV2 = it_temp != null && CheckItem.isMedalV2(it_temp.id);
                    if (it_temp != null && (isMedalV1 || isMedalV2) && it_temp.tier < 15) {
                        // Simpan slot medal agar UpgradeMedal pakai slot yang sama
                        conn.p.id_Upgrade_Medal_Star = id;
                        // Reset id_medal_is_created agar tidak bentrok dengan flow Create Medal
                        conn.p.id_medal_is_created = -1;
                        conn.p.id_medal_is_created_v2 = -1;
                        int medalIndex = isMedalV2 ? template.MedalV2Config.getMedalIndexV2(it_temp.id) : getMedalIndex(it_temp.id);
                        short[] materialArr;
                        if (isMedalV2) {
                            initMedalMaterialV2(conn.p, medalIndex);
                            materialArr = conn.p.medal_create_material_v2;
                        } else {
                            // Pastikan material sudah di-generate
                            initMedalMaterial(conn.p, medalIndex, it_temp.tier);
                            materialArr = conn.p.medal_create_material;
                        }
                        int quant_upgr = get_quant_material_medal_upgrade(it_temp.tier);
                        // Gunakan Message(23) + writeByte(19) sama seperti send_box_UI case 25-28
                        Message m_send = new Message(23);
                        m_send.writer().writeUTF("Upgrade Medal");
                        m_send.writer().writeByte(19);
                        m_send.writer().writeShort(0);
                        m_send.writer().writeByte(5);
                        for (int i = 5 * medalIndex; i < 5 * medalIndex + 5; i++) {
                            m_send.writer().writeShort(materialArr[i]);
                            if (conn.version > 250) {
                                m_send.writer().writeShort(quant_upgr);
                            } else {
                                m_send.writer().writeByte(quant_upgr);
                            }
                        }
                        conn.addmsg(m_send);
                        m_send.cleanup();
                    } else {
                        Service.send_notice_box(conn, "Error, silakan coba lagi!");
                    }
                }
                break;
            }
            case 2: {
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tas penuh!");
                    return;
                }
                if (id >= 246 && id < 346) {
                    Service.send_notice_box(conn, "Level item telah mencapai maksimal!");
                    return;
                }
                String name = ItemTemplate7.item.get(id).getName();
                Service.sendBoxInputText(conn, 7, name, new String[]{"Jumlah (5000 emas/1)"});
                if (conn.p.fusion_material_medal_id == -1) {
                    conn.p.fusion_material_medal_id = id;
                }
                break;
            }
            case 3: { // create
                // Jika id==-1 && tem==-1 artinya client kirim dari menu Upgrade Medal (bukan Create)
                // Redirect ke flow upgrade seperti case 4
                // TAPI: jika id_medal_is_created sudah di-set, berarti ini request Create Medal biasa
                if (id == -1 && tem == -1 && conn.p.id_medal_is_created == -1 && conn.p.id_medal_is_created_v2 == -1) {
                    int savedId = conn.p.id_Upgrade_Medal_Star;
                    if (savedId < 0 || savedId >= conn.p.item.bag3.length) {
                        Service.send_notice_box(conn, "Silakan drag medal ke slot upgrade terlebih dahulu!");
                        return;
                    }
                    Item3 it_temp = conn.p.item.bag3[savedId];
                    if (it_temp != null && (CheckItem.isMedal(it_temp.id) || CheckItem.isMedalV2(it_temp.id)) && it_temp.tier < 15) {
                        if (it_temp.tier < 6) {
                            UpgradeMedal(conn, (byte) 0);
                        } else {
                            MenuController.send_menu_select(conn, -101, new String[]{
                                "Tanpa item = " + (Ratio_Upgrade_Medal[it_temp.tier] / 100) + "%",
                                "Batu krypton level 1 = " + ((Ratio_Upgrade_Medal[it_temp.tier] + Ratio_Upgrade_Medal[it_temp.tier] * 0.05) / 100) + "%",
                                "Batu krypton level 2 = " + ((Ratio_Upgrade_Medal[it_temp.tier] + Ratio_Upgrade_Medal[it_temp.tier] * 0.1) / 100) + "%",
                                "Batu krypton level 3 = " + ((Ratio_Upgrade_Medal[it_temp.tier] + Ratio_Upgrade_Medal[it_temp.tier] * 0.3) / 100) + "%"
                            }, (byte) 0);
                        }
                    } else {
                        Service.send_notice_box(conn, "Perlengkapan tidak cocok atau telah mencapai level maksimal!");
                    }
                    break;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Inventaris penuh!");
                    return;
                }
                if (System.currentTimeMillis() < conn.p.time_speed_rebuild) {
                    Service.send_notice_box(conn, "Jangan terlalu cepat");
                    return;
                }
                boolean creatingV2 = conn.p.id_medal_is_created == -1 && conn.p.id_medal_is_created_v2 != -1;
                if (conn.p.id_medal_is_created != -1 || conn.p.id_medal_is_created_v2 != -1) {
                    int activeIndex = creatingV2 ? conn.p.id_medal_is_created_v2 : conn.p.id_medal_is_created;
                    short[] materialArr = creatingV2 ? conn.p.medal_create_material_v2 : conn.p.medal_create_material;
                    if (Manager.BuffAdminMaterial && conn.ac_admin > 3) {
                    } else if (conn.p.item.total_item_by_id(7,
                            materialArr[0 + 5 * activeIndex]) < 1) {
                        Service.send_notice_box(conn, "Material tidak cukup!");
                        return;
                    } else if (conn.p.item.total_item_by_id(7,
                            materialArr[1 + 5 * activeIndex]) < 1) {
                        Service.send_notice_box(conn, "Material tidak cukup!");
                        return;
                    } else if (conn.p.item.total_item_by_id(7,
                            materialArr[2 + 5 * activeIndex]) < 1) {
                        Service.send_notice_box(conn, "Material tidak cukup!");
                        return;
                    } else if (conn.p.item.total_item_by_id(7,
                            materialArr[3 + 5 * activeIndex]) < 1) {
                        Service.send_notice_box(conn, "Material tidak cukup!");
                        return;
                    } else if (conn.p.item.total_item_by_id(7,
                            materialArr[4 + 5 * activeIndex]) < 1) {
                        Service.send_notice_box(conn, "Material tidak cukup!");
                        return;
                    }
                    int bound1 = 0 + 5 * activeIndex;
                    int bound2 = 5 + 5 * activeIndex;
                    for (int i = bound1; i < bound2; i++) {
                        conn.p.item.remove(7, materialArr[i], 1);
                    }
                    // Reset dan generate material baru untuk create berikutnya
                    for (int k = bound1; k < bound2; k++) materialArr[k] = 0;
                    if (creatingV2) {
                        initMedalMaterialV2(conn.p, activeIndex);
                    } else {
                        initMedalMaterial(conn.p, activeIndex, (byte) 0);
                    }
                    Message m = new Message(-105);
                    m.writer().writeByte(3);
                    m.writer().writeByte(3);
                    ItemTemplate3 temp = creatingV2
                            ? ItemTemplate3.item.get((int) template.MedalV2Config.getMedalItemIdFromIndexV2(activeIndex))
                            : ItemTemplate3.item.get((int) getMedalItemIdFromIndex(activeIndex));
                    //byte color_ = (byte) Util.random(0, 5);
                    int ran_ = Util.random(0, 101);
                    byte color_ = 0;
                    if (creatingV2 && conn.ac_admin > 3
                            && conn.p.admin_selected_color_v2 >= 0 && conn.p.admin_selected_color_v2 <= 5) {
                        // Admin sudah pilih warna manual lewat menu "Pilih Warna (Admin)" —
                        // pakai persis warna itu, skip random roll & skip force-orange di bawah.
                        // Reset setelah 1x dipakai supaya create berikutnya balik ke behavior normal.
                        color_ = conn.p.admin_selected_color_v2;
                        conn.p.admin_selected_color_v2 = -1;
                    } else if (conn.ac_admin > 3 && Manager.BuffAdmin) {
                        color_ = 4;
                    } else if (creatingV2 || conn.p.id_medal_is_created == 4) {
                        // Medal V2 (semua index) & Medal MBG (V1 index 4): peluang color 5 (hijau) tersedia
                        // Hijau  (5): 1%  (ran 0-1)
                        // Orange (4): 7%  (ran 2-8)
                        // Ungu   (3): 15% (ran 9-23)
                        // Kuning (2): 20% (ran 24-43)
                        // Biru   (1): 27% (ran 44-70)
                        // Putih  (0): 30% (ran 71-101)
                        if (ran_ <= 1) {
                            color_ = 5;
                        } else if (ran_ <= 8) {
                            color_ = 4;
                        } else if (ran_ <= 23) {
                            color_ = 3;
                        } else if (ran_ <= 43) {
                            color_ = 2;
                        } else if (ran_ <= 70) {
                            color_ = 1;
                        } else {
                            color_ = 0;
                        }
                    } else if (ran_ <= 10) {
                        color_ = 4;
                    } else if (ran_ <= 25) {
                        color_ = 3;
                    } else if (ran_ <= 45) {
                        color_ = 2;
                    } else if (ran_ <= 70) {
                        color_ = 1;
                    } else {
                        color_ = 0;
                    }
                    //color_ = (byte) (conn.ac_admin>0?4:Util.random(0, 5));
                    conn.p.time_speed_rebuild = System.currentTimeMillis() + 2000;

                    // Buat item dulu agar stat sudah tersedia sebelum dikirim ke client
                    Item3 itbag = new Item3();
                    itbag.id = temp.getId();
                    itbag.clazz = temp.getClazz();
                    itbag.type = temp.getType();
                    itbag.level = 60; // level required
                    itbag.icon = temp.getIcon();
                    itbag.color = color_;
                    itbag.part = temp.getPart();
                    itbag.islock = false;
                    itbag.name = temp.getName();
                    if (conn.ac_admin >= 10) {
                        itbag.tier = 15;
                    } else {
                        itbag.tier = 0;
                    }
                    itbag.op = new ArrayList<>();
                    StatGenerator gen = new StatGenerator();
                    itbag.opMedal = List.of();
                    // FIX: pass idItem agar formula getParamMD digunakan saat scaling
                    // Medal V2 pakai sistem stat sendiri (createMedalStatV2), terpisah
                    // total dari Medal V1 (createMedalStat) — lihat StatGenerator.java.
                    itbag.op.addAll(creatingV2
                            ? gen.createMedalStatV2(activeIndex, color_, itbag.id)
                            : gen.createMedalStat(activeIndex, color_, itbag.id));
                    itbag.time_use = 0;

                    // FIX: kirim packet menggunakan stat item yang sudah di-generate, bukan template
                    m.writer().writeUTF("Selamat Anda mendapatkan " + temp.getName());
                    m.writer().writeByte(3);
                    m.writer().writeUTF(itbag.name);
                    m.writer().writeByte(itbag.clazz);
                    m.writer().writeShort(itbag.id);
                    m.writer().writeByte(itbag.type);
                    m.writer().writeShort(itbag.icon);
                    m.writer().writeByte(itbag.tier); // tier
                    m.writer().writeShort(itbag.level); // level required
                    m.writer().writeByte(itbag.color); // color
                    m.writer().writeByte(0); // can sell
                    m.writer().writeByte(0); // can trade
                    m.writer().writeByte(itbag.op.size()); // FIX: jumlah op dari item nyata
                    for (int i = 0; i < itbag.op.size(); i++) {
                        m.writer().writeByte(itbag.op.get(i).id);
                        m.writer().writeInt(itbag.op.get(i).getParam(itbag.tier)); // FIX: getParam dengan tier
                    }
                    m.writer().writeInt(0); // time use
                    m.writer().writeByte(0);
                    m.writer().writeByte(0);
                    m.writer().writeByte(0);
                    conn.addmsg(m);
                    m.cleanup();

                    conn.p.item.add_item_bag3(itbag);
                    conn.p.item.charInventory(7); // FIX: hapus charInventory(4) yang tidak perlu
                    conn.p.item.charInventory(3);
                    if (creatingV2) {
                        conn.p.id_medal_is_created_v2 = -1;
                    } else {
                        conn.p.id_medal_is_created = -1;
                    }
                } else {
                    Service.send_notice_box(conn, "Silakan pilih kembali item yang ingin dibuat!");
                }
                break;
            }
            case 4: {
                // Gunakan slot yang disimpan saat case 0 tem==3 (drag medal ke slot upgrade)
                // bukan id dari packet, karena client bisa kirim id yang berbeda
                int savedId = conn.p.id_Upgrade_Medal_Star;
                if (savedId < 0 || savedId >= conn.p.item.bag3.length) {
                    Service.send_notice_box(conn, "Silakan drag medal ke slot upgrade terlebih dahulu!");
                    return;
                }
                Item3 it_temp = conn.p.item.bag3[savedId];
                if (it_temp != null && (CheckItem.isMedal(it_temp.id) || CheckItem.isMedalV2(it_temp.id)) && it_temp.tier < 15) {
                    if (it_temp.tier < 6) {
                        UpgradeMedal(conn, (byte) 0);
                    } else {
                        MenuController.send_menu_select(conn, -101, new String[]{"Tanpa item = " + (Ratio_Upgrade_Medal[it_temp.tier] / 100) + "%",
                                "Batu krypton level 1 = " + ((Ratio_Upgrade_Medal[it_temp.tier] + Ratio_Upgrade_Medal[it_temp.tier] * 0.05) / 100) + "%",
                                "Batu krypton level 2 = " + ((Ratio_Upgrade_Medal[it_temp.tier] + Ratio_Upgrade_Medal[it_temp.tier] * 0.1) / 100) + "%",
                                "Batu krypton level 3 = " + ((Ratio_Upgrade_Medal[it_temp.tier] + Ratio_Upgrade_Medal[it_temp.tier] * 0.3) / 100) + "%"}, (byte) 0);
                    }
                } else {
                    Service.send_notice_box(conn, "Perlengkapan tidak cocok atau telah mencapai level maksimal!");
                }
                break;
            }
        }
    }

    public static short[] Ratio_Upgrade_Medal = new short[]{10000, 7600, 6200, 5200, 4500, 4000, 3500, 3200, 2900, 2700, 2500, 2300, 2100, 2000, 1900, 1800};

    public static void UpgradeMedal(Session conn, byte index) throws IOException {
        if (index > 3) {
            Service.send_notice_box(conn, "Terdeteksi adanya bug!");
            return;
        }
        int id = conn.p.id_Upgrade_Medal_Star;
        if (id < 0) {
            Service.send_notice_box(conn, "Silakan pilih kembali item yang ingin di-upgrade!");
            return;
        }
        if (System.currentTimeMillis() < conn.p.time_speed_rebuild) {
            Service.send_notice_box(conn, "Pelan-pelan!");
            return;
        }
        if (id >= conn.p.item.bag3.length) {
            return;
        }
        Item3 it_temp = conn.p.item.bag3[id];
        boolean isV2 = it_temp != null && CheckItem.isMedalV2(it_temp.id);
        if (it_temp != null && (CheckItem.isMedal(it_temp.id) || isV2) && it_temp.tier < 15) {
            int medalIndex = isV2 ? template.MedalV2Config.getMedalIndexV2(it_temp.id) : getMedalIndex(it_temp.id);
            short[] materialArr = isV2 ? conn.p.medal_create_material_v2 : conn.p.medal_create_material;
            for (int i = 5 * medalIndex; i < 5 * medalIndex + 5; i++) {
                short id_item_upgr = materialArr[i];
                int quant_item_upgr = get_quant_material_medal_upgrade(it_temp.tier);
                if (conn.p.item.total_item_by_id(7, id_item_upgr) < quant_item_upgr && !(Manager.BuffAdminMaterial && conn.ac_admin > 3)) {
                    Service.send_notice_box(conn, "Tidak cukup " + ItemTemplate7.item.get(id_item_upgr).getName());
                    return;
                }
            }
            int ngoc_req = get_ngoc_medal_upgrade(it_temp.tier);
            if (conn.p.getGem() < ngoc_req) {
                Service.send_notice_box(conn, "Tidak cukup " + ngoc_req + " permata");
                return;
            }
            if (index > 0 && conn.p.item.total_item_by_id(7, 348 + index) < 1) {
                Service.send_notice_box(conn, "Anda tidak memiliki cukup Batu krypton level " + index);
                return;
            }
            conn.p.updateGem(-ngoc_req);
            for (int i = 5 * medalIndex; i < 5 * medalIndex + 5; i++) {
                short id_item_upgr = materialArr[i];
                int quant_item_upgr = get_quant_material_medal_upgrade(it_temp.tier);
                conn.p.item.remove(7, id_item_upgr, quant_item_upgr);
            }
            conn.p.time_speed_rebuild = System.currentTimeMillis() + 2000;
            if (index > 0) {
                conn.p.item.remove(7, 348 + index, 1);
            }

            float[] plus_rate = new float[]{(float) 0, (float) 0.05, (float) 0.1, (float) 0.3};
//            boolean suc = (it_temp.tier < 6) ? true : (100 > Util.random(100 + it_temp.tier * 28));//cũ là 25
            boolean suc = (it_temp.tier < 6) ? true : ((Ratio_Upgrade_Medal[it_temp.tier] + Ratio_Upgrade_Medal[it_temp.tier] * plus_rate[index]) > Util.random(10000));
            if (conn.ac_admin > 3 && Manager.BuffAdmin) {
                suc = true;
            }

            if (suc) {
                it_temp.tier++;
                // Tambah 1 stat baru ke medal setelah tier naik
                // Medal V2 pakai sistem stat sendiri (upgradeMedalStatV2), terpisah
                // total dari Medal V1 (upgradeMedalStat) — lihat StatGenerator.java.
                StatGenerator gen = new StatGenerator();
                if (isV2) {
                    gen.upgradeMedalStatV2(it_temp);
                } else {
                    gen.upgradeMedalStat(it_temp);
                }
                // FIX: reset slot ke 0 agar material di-generate ulang dengan tier baru
                int id_material = 5 * medalIndex;
                for (int k = id_material; k < id_material + 5; k++) {
                    materialArr[k] = 0;
                }
                if (isV2) {
                    initMedalMaterialV2(conn.p, medalIndex);
                } else {
                    initMedalMaterial(conn.p, medalIndex, it_temp.tier);
                }

            }
            //
            Message m = new Message(-105);
            m.writer().writeByte(3);
            if (suc) {
                m.writer().writeByte(3);
            } else {
                m.writer().writeByte(4);
            }
            ItemTemplate3 temp = ItemTemplate3.item.get(it_temp.id);
            if (suc) {
                m.writer().writeUTF("Berhasil!");
            } else {
                m.writer().writeUTF("Gagal!");
            }
            m.writer().writeByte(3);
            m.writer().writeUTF(it_temp.name);
            m.writer().writeByte(temp.getClazz());
            m.writer().writeShort(temp.getId());
            m.writer().writeByte(temp.getType());
            m.writer().writeShort(temp.getIcon());
            m.writer().writeByte(it_temp.tier); // tier
            m.writer().writeShort(1); // level required
            m.writer().writeByte(it_temp.color); // color
            m.writer().writeByte(0); // can sell
            m.writer().writeByte(0); // can trade
            m.writer().writeByte(it_temp.op.size());
            for (int i = 0; i < it_temp.op.size(); i++) {
                m.writer().writeByte(it_temp.op.get(i).id);
                if (it_temp.op.get(i).id == 96) {
                    m.writer().writeInt(it_temp.op.get(i).getParam(0));
                } else {
                    m.writer().writeInt(it_temp.op.get(i).getParam(it_temp.tier));
                }
            }
            m.writer().writeInt(0); // time use
            m.writer().writeByte(0);
            m.writer().writeByte(0);
            m.writer().writeByte(0);
            conn.addmsg(m);
            m.cleanup();
            //


            conn.p.item.charInventory(3);
            conn.p.item.charInventory(7);
            conn.p.item.charInventory(7);
            //
            if (it_temp.tier < 15) {
                m = new Message(-105);
                m.writer().writeByte(5);
                if (suc) {
                    m.writer().writeByte(3);
                } else {
                    m.writer().writeByte(4);
                }
                if (suc) {
                    m.writer().writeUTF("Berhasil!");
                } else {
                    m.writer().writeUTF("Gagal!");
                }
                m.writer().writeShort(id);
                conn.addmsg(m);
                m.cleanup();
            }


            //
        } else {
            Service.send_notice_box(conn, "Kesalahan, coba lagi!");
        }
    }

    public static void ChangeCS_Medal(Session conn, int actions) throws IOException {
        if (System.currentTimeMillis() < conn.p.time_speed_rebuild) {
            Service.send_notice_box(conn, "Pelan-pelan!");
            return;
        }
        if (conn.p.item.wear == null || conn.p.item.wear.length < 13 || conn.p.item.wear[12] == null || !utils.CheckItem.isMedal(conn.p.item.wear[12].id) || (actions != 94 && actions != 98)) {
            Service.send_notice_box(conn, "Tidak menemukan item!");
            return;
        }
        conn.p.time_speed_rebuild = System.currentTimeMillis() + 1000;
        if (conn.p.getGem() < 1000) {
            Service.send_notice_box(conn, "Tidak cukup permata untuk melakukan ini!");
            return;
        }
        //System.out.println("core.GameSrc.ChangeCS_Medal(): "+actions);
        conn.p.updateGem(-1000);
        Item3 it_temp = conn.p.item.wear[12];

        if (actions == 94) {
            if (it_temp.op == null || it_temp.op.size() <= 0) {
                Service.send_notice_box(conn, "Perlengkapan rusak!");
                return;
            }
            for (int i = 0; i < it_temp.op.size(); i++) {
                Option op = it_temp.op.get(i);
                if (op != null && op.id >= 0 && op.id <= 4) {
                    int _st = 0;
                    byte color_ = it_temp.color;
                    if (color_ == 0) {
                        _st = Util.random(300, 400);
                    } else if (color_ == 1) {
                        _st = Util.random(300, 400);
                    } else if (color_ == 2) {
                        _st = Util.random(300, 500);
                    } else if (color_ == 3) {
                        _st = Util.random(400, 600);
                    } else if (color_ == 4) {
                        _st = Util.random(600, 800);
                    } else if (color_ == 5) {
                        _st = Util.random(800, 1000);
                    }
                    it_temp.op.set(i, new Option(Util.random(0, 5), _st, it_temp.id));
                    Service.sendPlayerWear(conn.p);
                    Service.send_notice_box(conn, "Berhasil");
                    return;
                }
            }
            Service.send_notice_box(conn, "Không tìm thấy chỉ số phù hợp!");
        }
        if (actions == 98) {
            if (it_temp.op == null || it_temp.op.size() <= 0) {
                Service.send_notice_box(conn, "Perlengkapan rusak!");
                return;
            }
            int lastpr = -1;
            List<Integer> id_PTST = new ArrayList<>(java.util.Arrays.asList(7, 8, 9, 10, 11));
            List<Integer> ops = new ArrayList<>();
            for (int i = 0; i < it_temp.op.size(); i++) {
                Option op = it_temp.op.get(i);
                if (op == null || !(op.id >= 7 && op.id <= 11)) {
                    continue;
                }
                //int ran = Util.random(0, 101);
                Integer id_add = Util.random(id_PTST, ops);
                if (id_add == null) {
                    continue;
                }
                int param_add = 0;
                if (id_add >= 7 && id_add <= 11) {
                    if (it_temp.color == 0) {
                        param_add = Util.randomNext(114, 160, lastpr);
                    } else if (it_temp.color == 1) {
                        param_add = Util.randomNext(160, 207, lastpr);
                    } else if (it_temp.color == 2) {
                        param_add = Util.randomNext(207, 254, lastpr);
                    } else if (it_temp.color == 3) {
                        param_add = Util.randomNext(253, 300, lastpr);
                    } else if (it_temp.color == 4) {
                        param_add = Util.randomNext(300, 347, lastpr);
                    }
                }
                ops.add(id_add);
                it_temp.op.set(i, new Option(id_add, param_add, it_temp.id));
            }
            Service.sendPlayerWear(conn.p);
            Service.send_notice_box(conn, "Berhasil");
            //Service.send_notice_box(conn, "Không tìm thấy chỉ số phù hợp!");
        }
    }

    public static void Wings_Process(Session conn, Message m2) throws IOException {
        // Service.send_notice_box(conn, "Đang phát triển");
        byte type = m2.reader().readByte();
        int wing = m2.reader().readInt();
        short id = m2.reader().readShort();
        // System.out.println(type);
        // System.out.println(wing);
        // System.out.println(id);
        if (type == 1) { // create
            if (conn.p.item.get_bag_able() < 1) {
                Service.send_notice_box(conn, "Tas tidak punya cukup ruang!");
                return;
            }
            if (conn.ac_admin < 10) {
                if (conn.p.item.total_item_by_id(7, 8) < 80) {
                    Service.send_notice_box(conn, "Tidak cukup " + ItemTemplate7.item.get(8).getName() + "!");
                    return;
                } else if (conn.p.item.total_item_by_id(7, 9) < 60) {
                    Service.send_notice_box(conn, "Tidak cukup " + ItemTemplate7.item.get(9).getName() + "!");
                    return;
                } else if (conn.p.item.total_item_by_id(7, 10) < 40) {
                    Service.send_notice_box(conn, "Tidak cukup " + ItemTemplate7.item.get(10).getName() + "!");
                    return;
                } else if (conn.p.item.total_item_by_id(7, 11) < 20) {
                    Service.send_notice_box(conn, "Tidak cukup " + ItemTemplate7.item.get(11).getName() + "!");
                    return;
                } else if (conn.p.item.total_item_by_id(7, 0) < 100) {
                    Service.send_notice_box(conn, "Tidak cukup " + ItemTemplate7.item.get(0).getName() + "!");
                    return;
                } else if (conn.p.item.total_item_by_id(7, 3) < 20) {
                    Service.send_notice_box(conn, "Tidak cukup " + ItemTemplate7.item.get(3).getName() + "!");
                    return;
                }
            }
            if (conn.p.getGold() < 200_000) {
                Service.send_notice_box(conn, "Tidak cukup emas!");
                return;
            }
            //
            conn.p.updateGold(-200_000);
            conn.p.item.remove(7, 8, 80);
            conn.p.item.remove(7, 9, 60);
            conn.p.item.remove(7, 10, 40);
            conn.p.item.remove(7, 11, 20);
            conn.p.item.remove(7, 0, 100);
            conn.p.item.remove(7, 3, 20);
            conn.p.item.charInventory(7);
            //
            ItemTemplate3 temp = ItemTemplate3.item.get(wing);
            Item3 itbag = new Item3();
            itbag.id = (short) wing;
            itbag.name = temp.getName();
            itbag.clazz = temp.getClazz();
            itbag.type = temp.getType();
            itbag.level = 60;
            itbag.icon = temp.getIcon();
            //
            itbag.op = new ArrayList<>();
            List<Option> op_new = new ArrayList<>();
            switch (wing) {
                case 2880: { // canh chien than
                    op_new.add(new Option(25, Util.random(1, 7), itbag.id));
                    break;
                }
                case 280: { // canh chien than
                    op_new.add(new Option(Util.random(23, 27), Util.random(1, 7), itbag.id));
                    break;
                }
                case 2887: { // canh rong
                    op_new.add(new Option(23, Util.random(1, 6), itbag.id));
                    break;
                }
                case 2894: { // canh quai thu
                    op_new.add(new Option(23, Util.random(1, 6), itbag.id));
                    break;
                }
                case 2901: { // canh yeu tinh
                    op_new.add(new Option(24, Util.random(1, 7), itbag.id));
                    break;
                }
                case 2908: { // canh phuong hoang
                    op_new.add(new Option(26, Util.random(1, 6), itbag.id));
                    break;
                }
                case 2915: { // canh bang tuyet
                    op_new.add(new Option(25, Util.random(1, 7), itbag.id));
                    break;
                }
                case 2922: { // canh hong hac
                    op_new.add(new Option(24, Util.random(1, 7), itbag.id));
                    break;
                }
                case 2929: { // canh chuon chuon
                    op_new.add(new Option(26, Util.random(1, 6), itbag.id));
                    break;
                }
            }
            op_new.add(new Option(41,
                    ((3 > Util.random(0, 150)) ? 4 : ((10 > Util.random(0, 150)) ? 3 : ((45 > Util.random(0, 120)) ? 2 : 1)))
                            * 100, itbag.id));
            op_new.add(new Option(42,
                    ((3 > Util.random(0, 150)) ? 8 : ((10 > Util.random(0, 150)) ? 7 : ((45 > Util.random(0, 120)) ? 6 : 5)))
                            * 1000, itbag.id));
            //
            itbag.op.addAll(op_new);
            //
            itbag.color = 5;
            itbag.part = temp.getPart();
            itbag.tier = 0;
            itbag.islock = true;
            itbag.time_use = 0;
            conn.p.item.add_item_bag3(itbag);
            conn.p.item.charInventory(4);
            conn.p.item.charInventory(7);
            conn.p.item.charInventory(3);
            for (int i = (conn.p.item.bag3.length - 1); i >= 0; i--) {
                if (conn.p.item.bag3[i] != null && conn.p.item.bag3[i].id == wing) {
                    Message m = new Message(77);
                    m.writer().writeByte(5);
                    m.writer().writeUTF("Selamat, Anda mendapatkan " + temp.getName());
                    m.writer().writeShort(i);
                    conn.addmsg(m);
                    m.cleanup();
                    break;
                }
            }
            Log.gI().add_log(conn.p.name, "Tạo cánh " + temp.getName());
        } else if (type == 2) {
            if (!conn.p.is_create_wing) {
                if (id < 0 || id >= conn.p.item.bag3.length) return;
                Item3 it = conn.p.item.bag3[id];
                if (it != null) {
                    if (it.tier > 29) {
                        Service.send_notice_box(conn, "Telah ditingkatkan ke level maksimum!");
                        return;
                    }
                    if (it.time_use > 0) {
                        long time_ = it.time_use - System.currentTimeMillis();
                        time_ /= 60_000;
                        Service.send_notice_nobox_white(conn, "Gunakan dalam " + ((time_ > 0) ? time_ : 1) + " menit lagi");
                        return;
                    }
                    Message m = new Message(77);
                    m.writer().writeByte(2);
                    m.writer().writeShort(id);
                    conn.addmsg(m);
                    m.cleanup();
                    //
                    m = new Message(77);
                    m.writer().writeByte(0);
                    m.writer().writeInt(it.id);
                    m.writer().writeUTF((it.name + " +" + (it.tier + 1)));
                    m.writer().writeInt(GameSrc.wing_upgrade_material_gold[it.tier]);
                    m.writer().writeShort(60);
                    m.writer().writeInt(GameSrc.wing_upgrade_material_time[it.tier] * 60);
                    m.writer().writeByte(6);
                    m.writer().writeShort(8);
                    m.writer().writeShort(GameSrc.wing_upgrade_material_long_khuc_xuong[it.tier]);
                    m.writer().writeShort(9);
                    m.writer().writeShort(GameSrc.wing_upgrade_material_long_khuc_xuong[it.tier]);
                    m.writer().writeShort(10);
                    m.writer().writeShort(GameSrc.wing_upgrade_material_long_khuc_xuong[it.tier]);
                    m.writer().writeShort(11);
                    m.writer().writeShort(GameSrc.wing_upgrade_material_long_khuc_xuong[it.tier]);
                    m.writer().writeShort(0);
                    m.writer().writeShort(GameSrc.wing_upgrade_material_da_cuong_hoa[it.tier]);
                    m.writer().writeShort(3);
                    m.writer().writeShort(GameSrc.wing_upgrade_material_kim_loai[it.tier]);
                    conn.addmsg(m);
                    m.cleanup();
                }
            } else {
                Service.send_notice_box(conn, "Anda berencana membuat sayap baru, apakah Anda lupa?");
            }
        } else if (type == 3) {
            if (!conn.p.is_create_wing) {
                Item3 it = conn.p.item.bag3[id];
                if (it != null) {
                    if (it.time_use != 0) {
                        return;
                    }
                    if (conn.ac_admin < 10) {
                        // check material
                        if (conn.p.item.total_item_by_id(7, 8) < GameSrc.wing_upgrade_material_long_khuc_xuong[it.tier]) {
                            Service.send_notice_box(conn, "Tidak cukup " + ItemTemplate7.item.get(8).getName() + "!");
                            return;
                        } else if (conn.p.item.total_item_by_id(7, 9) < GameSrc.wing_upgrade_material_long_khuc_xuong[it.tier]) {
                            Service.send_notice_box(conn, "Tidak cukup " + ItemTemplate7.item.get(9).getName() + "!");
                            return;
                        } else if (conn.p.item.total_item_by_id(7,
                                10) < GameSrc.wing_upgrade_material_long_khuc_xuong[it.tier]) {
                            Service.send_notice_box(conn, "Tidak cukup " + ItemTemplate7.item.get(10).getName() + "!");
                            return;
                        } else if (conn.p.item.total_item_by_id(7,
                                11) < GameSrc.wing_upgrade_material_long_khuc_xuong[it.tier]) {
                            Service.send_notice_box(conn, "Tidak cukup " + ItemTemplate7.item.get(11).getName() + "!");
                            return;
                        } else if (conn.p.item.total_item_by_id(7, 0) < GameSrc.wing_upgrade_material_da_cuong_hoa[it.tier]) {
                            Service.send_notice_box(conn, "Tidak cukup " + ItemTemplate7.item.get(0).getName() + "!");
                            return;
                        } else if (conn.p.item.total_item_by_id(7, 3) < GameSrc.wing_upgrade_material_kim_loai[it.tier]) {
                            Service.send_notice_box(conn, "Tidak cukup " + ItemTemplate7.item.get(3).getName() + "!");
                            return;
                        }
                    }
                    if (conn.p.getGold() < GameSrc.wing_upgrade_material_gold[it.tier]) {
                        Service.send_notice_box(conn, "Tidak cukup emas!");
                        return;
                    }

                    //
                    conn.p.updateGold(-GameSrc.wing_upgrade_material_gold[it.tier]);
                    conn.p.item.remove(7, 8, GameSrc.wing_upgrade_material_long_khuc_xuong[it.tier]);
                    conn.p.item.remove(7, 9, GameSrc.wing_upgrade_material_long_khuc_xuong[it.tier]);
                    conn.p.item.remove(7, 10, GameSrc.wing_upgrade_material_long_khuc_xuong[it.tier]);
                    conn.p.item.remove(7, 11, GameSrc.wing_upgrade_material_long_khuc_xuong[it.tier]);
                    conn.p.item.remove(7, 0, GameSrc.wing_upgrade_material_da_cuong_hoa[it.tier]);
                    conn.p.item.remove(7, 3, GameSrc.wing_upgrade_material_kim_loai[it.tier]);
                    conn.p.item.charInventory(7);
                    if (conn.ac_admin < 10) {
                        UpgradeSystem.updateWingUpgradeOption(it);
                        it.time_use = System.currentTimeMillis()
                                + (((long) GameSrc.wing_upgrade_material_time[it.tier - 1]) * 3_600_000L);
                    } else {
                        for (int i = it.tier; i < 30; i++) {
                            UpgradeSystem.updateWingUpgradeOption(it);
                        }
                    }

                    it.convertWing();

                    conn.p.item.charInventory(4);
                    conn.p.item.charInventory(7);
                    conn.p.item.charInventory(3);
                    Message m = new Message(77);
                    m.writer().writeByte(5);
                    m.writer().writeUTF(("Berhasil meningkatkan " + it.name + " ke +" + it.tier));
                    m.writer().writeShort(id);
                    conn.addmsg(m);
                    m.cleanup();
                }
            } else {
                Service.send_notice_box(conn, "Anda berencana membuat sayap baru, apakah Anda lupa?");
            }
        }
    }


    public static void playerStore(Session conn, Message m2) throws IOException {
        if (!Manager.isKmb) {
            // "Khu mua bán đang tạm khóa!" -> "Area perdagangan saat ini ditutup sementara!"
            Service.send_notice_box(conn, "Area perdagangan saat ini ditutup sementara!");
            return;
        }
        //  if (conn.p.map.map_id != 82) {
        //      return;
//}
        byte cmd = m2.reader().readByte();
        switch (cmd) {
            case 0 -> { // bán -> jual
                if (conn.p.item.total_item_by_id(4, 135) > 0) {
                    int size = m2.reader().readByte();
                    if (size > 12) {
                        // "Tối đa 12 món!" -> "Maksimal 12 item!"
                        Service.send_notice_box(conn, "Maksimal 12 item!");
                        return;
                    }
                    conn.p.my_store.clear();
                    for (int i = 0; i < size; i++) {
                        Player_store p_store = new Player_store();
                        p_store.it_id = m2.reader().readShort();
                        p_store.it_price = m2.reader().readInt();
                        if (p_store.it_price > 2_000_000_000 || p_store.it_price < 10_000) {
                            // "Giá tiền không phù hợp!" -> "Harga tidak sesuai!"
                            Service.send_notice_box(conn, "Harga tidak sesuai!");
                            return;
                        }
                        p_store.it_quant = m2.reader().readShort();
                        p_store.it_type = m2.reader().readByte();
                        if (p_store.it_quant <= 0 || p_store.it_quant > 32_000) {
                            // "Số lượng không hợp lệ!" -> "Jumlah tidak valid!"
                            Service.send_notice_box(conn, "Jumlah tidak valid!");
                            return;
                        }

                        if (p_store.it_type == 3 || p_store.it_type == 7){
                            Service.send_notice_nobox_white(conn, "Transaksi untuk jenis item ini telah di nonaktifkan");
                            return;
                        }

                        if (p_store.it_type == 3) {
                            if (conn.p.item.bag3[p_store.it_id] == null
                                    || (conn.p.item.bag3[p_store.it_id] != null && conn.p.item.bag3[p_store.it_id].islock)) {
                                // "Không thể bán vật phẩm khóa!" -> "Tidak bisa menjual item yang terkunci!"
                                Service.send_notice_box(conn, "Tidak bisa menjual item yang terkunci!");
                                return;
                            }
                            if (conn.p.item.bag3[p_store.it_id] != null && conn.p.item.bag3[p_store.it_id].expiry_date > 0) {
                                // "Không thể bán vật phẩm có hạn sử dụng!" -> "Tidak bisa menjual item dengan tanggal kedaluwarsa!"
                                Service.send_notice_box(conn, "Tidak bisa menjual item dengan tanggal kedaluwarsa!");
                                return;
                            }
                        } else if (p_store.it_type == 4 || p_store.it_type == 7) {
                            if (conn.p.item.total_item_by_id(p_store.it_type, p_store.it_id) < p_store.it_quant) {

                                Service.send_notice_box(conn, "Jumlah tidak valid!");
                                return;
                            }
                            if (p_store.it_type == 4 && (utils.CheckItem.item4CanTrade(p_store.it_id) || p_store.it_id == 135 || p_store.it_id == 52 || p_store.it_id == 56 || p_store.it_id == 143 || p_store.it_id == 226)) {
                                // "Đồ bán không hợp lệ!" -> "Item yang dijual tidak valid!"
                                Service.send_notice_box(conn, "Item yang dijual tidak valid!");
                                return;
                            }
                            if (p_store.it_type == 7 && template.ItemTemplate7.item.get(p_store.it_id).getTrade() == 1) {
                                // "Đồ bán không hợp lệ!" -> "Item yang dijual tidak valid!"
                                Service.send_notice_box(conn, "Item yang dijual tidak valid!");
                                return;
                            }
                        }
                        conn.p.my_store.add(p_store);
                    }
                    if (!conn.p.my_store_name.isEmpty()) {
                        // "Đang bán rồi!" -> "Sudah berjualan!"
                        Service.send_notice_box(conn, "Sudah berjualan!");
                        return;
                    }
                    for (int i = 0; i < conn.p.map.players.size(); i++) {
                        Player p0 = conn.p.map.players.get(i);
                        if (p0 != null && !p0.my_store_name.isEmpty() && p0.objectId != conn.p.objectId
                                && (Math.abs(p0.x - conn.p.x) < 75) && (Math.abs(p0.y - conn.p.y) < 75)) {
                            // "Quá gần một gian hàng khác, hãy chọn chỗ khác!" -> "Terlalu dekat dengan toko lain, silakan pilih tempat lain!"
                            Service.send_notice_box(conn, "Terlalu dekat dengan toko lain, silakan pilih tempat lain!");
                            return;
                        }
                    }
                    conn.p.my_store_name = m2.reader().readUTF();
                    if (conn.p.my_store_name.isEmpty()) {
                        // "Không để trống tên gian hàng!" -> "Nama toko tidak boleh kosong!"
                        Service.send_notice_box(conn, "Nama toko tidak boleh kosong!");
                        return;
                    }
                    Message ms = new Message(-102);
                    ms.writer().writeByte(1);
                    ms.writer().writeShort(conn.p.objectId);
                    ms.writer().writeUTF(conn.p.my_store_name);
                    MapService.sendMsgPlayerInside(conn.p.map, conn.p, ms, true);
                    ms.cleanup();
                    conn.p.item.remove(4, 135, 1);
                    conn.p.item.charInventory(4);
                    conn.p.item.charInventory(7);
                    conn.p.item.charInventory(3);
                    // "Bạn có muốn bán riêng vật phẩm cho ai không?" -> "Apakah Anda ingin menjual item secara pribadi kepada seseorang?"
                    Service.send_box_input_yesno(conn, 97, "Apakah Anda ingin menjual item secara pribadi kepada seseorang?");
                    //Service.send_box_input_text(conn, 20, "Bán riêng cho nhân vật", new String[]{"Tên nhân vật"}); -> "Jual secara pribadi kepada karakter", new String[]{"Nama karakter"});
                } else {
                    // "Không đủ vé!" -> "Tidak cukup tiket!"
                    Service.send_notice_box(conn, "Tidak cukup tiket!");
                }
            }
            case 4 -> {
                Message ms = new Message(-102);
                ms.writer().writeByte(2);
                ms.writer().writeShort(conn.p.objectId);
                MapService.sendMsgPlayerInside(conn.p.map, conn.p, ms, true);
                ms.cleanup();
                conn.p.my_store_name = "";
                conn.p.Store_Sell_ToPL = "";
                // "Hủy bán thành công!" -> "Berhasil membatalkan penjualan!"
                Service.send_notice_box(conn, "Berhasil membatalkan penjualan!");
            }
            case 1 -> {//xem cử hàng -> melihat toko
                short id_p = m2.reader().readShort();
                Player p0 = GameMap.get_player_by_id(id_p);
                if (p0 != null && !p0.my_store_name.isEmpty()) {
                    GameSrc.update_store_player_to_other(p0, conn);
                }
            }
            case 2 -> {// mua -> beli
                short iditem = m2.reader().readShort();
                short idChar = m2.reader().readShort();
                byte idType = m2.reader().readByte();
                Player p0 = GameMap.get_player_by_id(idChar);
                if (p0 != null && !p0.my_store_name.isEmpty()) {
                    if (p0.Store_Sell_ToPL != null && !p0.Store_Sell_ToPL.isEmpty() && !p0.Store_Sell_ToPL.equals(conn.p.name)) {
                        // "Người bán không muốn bán vật phẩm cho bạn!" -> "Penjual tidak ingin menjual item ini kepada Anda!"
                        Service.send_notice_box(conn, "Penjual tidak ingin menjual item ini kepada Anda!");
                        return;
                    }
                    His_KMB hist = new His_KMB(p0.name, conn.p.name, p0.getGold(), conn.p.getGold());
                    for (int i = 0; i < p0.my_store.size(); i++) {
                        if (p0.my_store.get(i).it_type == 3 && p0.item.bag3[p0.my_store.get(i).it_id] != null
                                && p0.item.bag3[p0.my_store.get(i).it_id].id == iditem) {
                            long vang_trade = p0.my_store.get(i).it_price;
                            if (conn.p.getGold() < vang_trade) {
                                // "Không đủ " + vang_trade + " vàng!" -> "Tidak cukup " + vang_trade + " emas!"
                                Service.send_notice_box(conn, "Tidak cukup " + vang_trade + " emas!");
                                return;
                            }
                            if (conn.p.item.get_bag_able() < 1) {
                                // "Hành trang đầy!" -> "Tas penuh!"
                                Service.send_notice_box(conn, "Tas penuh!");
                                return;
                            }
                            Item3 itTrade = p0.item.bag3[p0.my_store.get(i).it_id];
                            conn.p.updateGold(-vang_trade);
                            long thue = (vang_trade / 100) * Manager.thue;
                            if (Manager.guildThue != null) {
                                Manager.guildThue.updateGold(thue);
                            }
//                                Manager.ClanThue.update_vang((vang_trade * Manager.thue) / 100);
                            vang_trade -= thue;

                            p0.updateGold(vang_trade);

                            hist.tem3 = itTrade;
                            hist.UpdateGold(p0.getGold(), conn.p.getGold());
                            hist.Flus();

                            conn.p.item.add_item_bag3(itTrade);
                            p0.item.bag3[p0.my_store.get(i).it_id] = null;
                            p0.item.charInventory(4);
                            p0.item.charInventory(7);
                            p0.item.charInventory(3);
                            conn.p.item.charInventory(4);
                            conn.p.item.charInventory(7);
                            conn.p.item.charInventory(3);
                            p0.my_store.remove(p0.my_store.get(i));
                            GameSrc.update_store_player(p0);
                            GameSrc.update_store_player_to_other(p0, conn);
                            // "Mua thành công" -> "Pembelian berhasil"
                            Service.send_notice_box(conn, "Pembelian berhasil");
                            // "Bán thành công, nhận được " + vang_trade + "vàng" -> "Penjualan berhasil, Anda menerima " + vang_trade + " emas"
                            Service.send_notice_box(p0.conn, "Penjualan berhasil, Anda menerima " + vang_trade + " emas");
                            break;
                        } else if (p0.my_store.get(i).it_type == 4 || p0.my_store.get(i).it_type == 7) {
                            if (p0.my_store.get(i).it_id == iditem && p0.my_store.get(i).it_type == idType) {
                                if (p0.item.total_item_by_id(idType, iditem) >= p0.my_store.get(i).it_quant) {
                                    int vang_trade = p0.my_store.get(i).it_price;
                                    if (conn.p.getGold() < vang_trade) {
                                        // "Không đủ " + vang_trade + " vàng!" -> "Tidak cukup " + vang_trade + " emas!"
                                        Service.send_notice_box(conn, "Tidak cukup " + vang_trade + " emas!");
                                        return;
                                    }
                                    if (conn.p.item.total_item_by_id(idType, iditem) + p0.my_store.get(i).it_quant > 30_000
                                            || (conn.p.item.total_item_by_id(idType, iditem) == 0 && conn.p.item.get_bag_able() < 1)) {
                                        // "Hành trang đầy!" -> "Tas penuh!"
                                        Service.send_notice_box(conn, "Tas penuh!");
                                        return;
                                    }
                                    conn.p.updateGold(-vang_trade);
                                    long thue = (vang_trade / 100) * Manager.thue;
                                    if (Manager.guildThue != null) {
                                        Manager.guildThue.updateGold(thue);
                                    }
                                    // Manager.ClanThue.update_vang((vang_trade * Manager.thue) / 100);
                                    vang_trade -= thue;
                                    p0.updateGold(vang_trade);
                                    Item47 it_b_add = new Item47();
                                    it_b_add.category = idType;
                                    it_b_add.id = iditem;
                                    it_b_add.quantity = p0.my_store.get(i).it_quant;

                                    hist.tem47 = it_b_add;
                                    hist.UpdateGold(p0.getGold(), conn.p.getGold());
                                    hist.Flus();

                                    conn.p.item.add_item_bag47(idType, it_b_add);
                                    p0.item.remove(idType, iditem, p0.my_store.get(i).it_quant);
                                    p0.item.charInventory(4);
                                    p0.item.charInventory(7);
                                    p0.item.charInventory(3);
                                    conn.p.item.charInventory(4);
                                    conn.p.item.charInventory(7);
                                    conn.p.item.charInventory(3);
                                    p0.my_store.remove(p0.my_store.get(i));
                                    GameSrc.update_store_player(p0);
                                    GameSrc.update_store_player_to_other(p0, conn);
                                    // "Mua thành công" -> "Pembelian berhasil"
                                    Service.send_notice_box(conn, "Pembelian berhasil");
                                    // "Bán thành công, nhận được " + vang_trade + "vàng" -> "Penjualan berhasil, Anda menerima " + vang_trade + " emas"
                                    Service.send_notice_box(p0.conn, "Penjualan berhasil, Anda menerima " + vang_trade + " emas");
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            default -> {
                //System.out.println("cmd " + cmd);

            }
        }
    }

    private static void update_store_player_to_other(Player p0, Session conn) throws IOException {
        Message m = new Message(23);
        m.writer().writeUTF("Toko " + p0.name);
        m.writer().writeByte(17);
        m.writer().writeShort(p0.my_store.size());
        for (int i = 0; i < p0.my_store.size(); i++) {
            Player_store temp = p0.my_store.get(i);
            m.writer().writeByte(temp.it_type);
            switch (temp.it_type) {
                case 3: {
                    Item3 it_b = p0.item.bag3[temp.it_id];
                    if (it_b != null) {
                        m.writer().writeShort(it_b.id);
                        m.writer().writeUTF(it_b.name);
                        m.writer().writeByte(it_b.clazz);
                        m.writer().writeByte(it_b.type);
                        m.writer().writeShort(it_b.icon);
                        m.writer().writeByte(it_b.tier);
                        m.writer().writeShort(it_b.level);
                        m.writer().writeByte(it_b.color);
                        m.writer().writeByte(it_b.op.size() + 1);
                        for (int j = 0; j < it_b.op.size(); j++) {
                            m.writer().writeByte(it_b.op.get(j).id);
                            m.writer().writeInt(it_b.op.get(j).getParam(it_b.tier));
                        }
                        m.writer().writeByte(70);
                        m.writer().writeInt(temp.it_price);
                        m.writer().writeByte(0);
                    } else {
                        Service.send_notice_box(conn, "Toko bermasalah, silakan coba lagi nanti!");
                        return;
                    }
                    break;
                }
                case 4: {
                    m.writer().writeShort(temp.it_id);
                    m.writer().writeShort(temp.it_quant);
                    m.writer().writeLong(temp.it_price);
                    break;
                }
                case 7: {
                    m.writer().writeShort(temp.it_id);
                    m.writer().writeShort(temp.it_quant);
                    m.writer().writeByte(1); // can sell
                    m.writer().writeByte(1); // can trade
                    m.writer().writeLong(temp.it_price);
                    break;
                }
            }
        }
        m.writer().writeShort(p0.objectId);
        conn.addmsg(m);
        m.cleanup();
    }

    private static void update_store_player(Player p0) throws IOException {
        Message m = new Message(-102);
        m.writer().writeByte(3);
        m.writer().writeByte(p0.my_store.size());
        for (int i = 0; i < p0.my_store.size(); i++) {
            Player_store temp = p0.my_store.get(i);
            m.writer().writeByte(temp.it_type);
            switch (temp.it_type) {
                case 3: {
                    Item3 it_in_bag = p0.item.bag3[temp.it_id];
                    if (it_in_bag != null) {
                        m.writer().writeShort(it_in_bag.id);
                        m.writer().writeUTF(it_in_bag.name);
                        m.writer().writeByte(it_in_bag.clazz);
                        m.writer().writeByte(it_in_bag.type);
                        m.writer().writeShort(it_in_bag.icon);
                        m.writer().writeByte(it_in_bag.tier);
                        m.writer().writeShort(it_in_bag.level);
                        m.writer().writeByte(it_in_bag.color);
                        m.writer().writeByte(it_in_bag.op.size() + 1);
                        for (int j = 0; j < it_in_bag.op.size(); j++) {
                            m.writer().writeByte(it_in_bag.op.get(j).id);
                            m.writer().writeInt(it_in_bag.op.get(j).getParam(it_in_bag.tier));
                        }
                        m.writer().writeByte(70);
                        m.writer().writeInt(temp.it_price);
                        m.writer().writeByte(0);
                    } else {
                        return;
                    }
                    break;
                }
                case 4: {
                    m.writer().writeShort(temp.it_id);
                    m.writer().writeShort(temp.it_quant);
                    m.writer().writeInt(temp.it_price);
                    break;
                }
                case 7: {
                    m.writer().writeShort(temp.it_id);
                    m.writer().writeShort(temp.it_quant);
                    m.writer().writeByte(1); // can sell
                    m.writer().writeByte(1); // can trade
                    m.writer().writeLong(temp.it_price);
                    break;
                }
            }
        }
        p0.conn.addmsg(m);
        m.cleanup();
    }

    public static void jadeInlay(Player p, Message m2) throws IOException {
        byte type = m2.reader().readByte();
        short itemId = m2.reader().readShort();

//        System.out.println("core.GameSrc.Hop_Ngoc_Kham()");
//        System.out.println(type);
//        System.out.println(id_item);
        switch (type) {
            case 0: {
                if (itemId >= p.item.bag3.length) {
                    // "Trang bị không phù hợp!" -> "Perlengkapan tidak cocok!"
                    Service.send_notice_box(p.conn, "Perlengkapan tidak cocok!");
                    return;
                }
                Item3 it_check = p.item.bag3[itemId];
                if (it_check == null || !check_item_can_rebuild(it_check.type) || PartFashion.fashions.contains(it_check.id)) {
                    Service.send_notice_box(p.conn, "Perlengkapan tidak cocok!");
                    return;
                }
                short id_g1 = m2.reader().readShort();
                short id_g2 = m2.reader().readShort();
                short id_g3 = m2.reader().readShort();
//                System.out.println(id_g1);
//                System.out.println(id_g2);
//                System.out.println(id_g3);
                if (id_g2 != -1 || id_g3 != -1) {
                    // "Mỗi lần hãy khảm 1 viên ngọc thôi!" -> "Setiap kali, bertatah hanya dengan satu permata!"
                    Service.send_notice_box(p.conn, "Setiap kali, bertatah hanya dengan satu permata!");
                    return;
                }
                if (id_g1 == -1 || p.item.total_item_by_id(7, id_g1) < 1) {
                    // "Lỗi hãy thử lại!" -> "Kesalahan, silakan coba lagi!"
                    Service.send_notice_box(p.conn, "Kesalahan, silakan coba lagi!");
                    return;
                }
                if (!ItemInlay.isInlayGem(id_g1)) {
                    // "Chỉ có thể khảm ngọc tinh luyện!" -> "Hanya permata yang sudah dimurnikan yang bisa ditatah!"
                    Service.send_notice_box(p.conn, "Hanya permata yang sudah dimurnikan yang bisa ditatah!");
                    return;
                }
                Item3 it3 = p.item.bag3[itemId];
                if (it3 == null) {
                    break;
                }
                if (!GameSrc.checkInlayByType(id_g1, it3)) {
                    // "Không thể khảm ngọc này lên vật phẩm này!" -> "Tidak bisa menatah permata ini pada item ini!"
                    Service.send_notice_box(p.conn, "Tidak bisa menatah permata ini pada item ini!");
                    return;
                }
                short[] ngoc_in4 = p.item.checkSocket(it3);
                int index_ngoc_kham_vao = -1;
                for (int i = 0; i < ngoc_in4.length; i++) {
                    if (ngoc_in4[i] == -1) {
                        index_ngoc_kham_vao = i;
                        break;
                    }
                }
                if (index_ngoc_kham_vao == -1) {
                    // "Vật phẩm chưa có lỗ đục hoặc không có lỗ dư!" -> "Item tidak memiliki lubang atau tidak ada lubang yang tersisa!"
                    Service.send_notice_box(p.conn, "Item tidak memiliki lubang atau tidak ada lubang yang tersisa!");
                    return;
                }
                int totalGold = (GameSrc.getGoldPriceForGem(id_g1) / 50_000) * 1_000_000;
                if (p.getGold() < totalGold) {
                    // "Không đủ " + (vang_total) + " vàng!" -> "Tidak cukup " + (vang_total) + " emas!"
                    Service.send_notice_box(p.conn, "Tidak cukup " + (totalGold) + " emas!");
                    return;
                }
                Message m = new Message(-100);
                if (35 > Util.random(100) || (p.conn.ac_admin > 3 && Manager.BuffAdmin)) {
                    if (!ItemInlay.inlayGem(id_g1, it3)) {
                        // "Không thể khảm!" -> "Tidak bisa ditatah!"
                        Service.send_notice_box(p.conn, "Tidak bisa ditatah!");
                        return;
                    }
                    for (int i = 0; i < it3.op.size(); i++) {
                        if (it3.op.get(i).id == (index_ngoc_kham_vao + 58)) {
                            it3.op.get(i).setParam(id_g1);
                            break;
                        }
                    }
                    m.writer().writeByte(3);
                    // "Thành công, chúc mừng" -> "Berhasil, selamat"
                    m.writer().writeUTF("Berhasil, selamat");
                } else {
                    m.writer().writeByte(4);
                    // "Chúc con may mắn lần sau!" -> "Semoga beruntung lain kali!"
                    m.writer().writeUTF("Semoga beruntung lain kali!");
                }
                m.writer().writeShort(it3.id);
                m.writer().writeByte(3);
                p.conn.addmsg(m);
                m.cleanup();
                //
                p.updateGold(-totalGold);
                p.item.remove(7, id_g1, 1);
                p.item.charInventory(4);
                p.item.charInventory(7);
                p.item.charInventory(3);

                break;
            }
            case 1: {
                if (p.item.get_bag_able() < 1) // "Hành trang đầy!" -> "Tas penuh!"
                {
                    Service.send_notice_box(p.conn, "Tas penuh!");
                } else if (GameSrc.getGoldPriceForGem(itemId) <= 200_000) {
                    p.id_hop_ngoc = itemId;
                    // "Nhập số lượng", new String[]{"Nhập số lượng"} -> "Masukkan jumlah", new String[]{"Masukkan jumlah"}
                    Service.sendBoxInputText(p.conn, 15, "Masukkan jumlah", new String[]{"Masukkan jumlah"});
                } else {
                    // "Đã hợp tối đa!" -> "Sudah digabung hingga batas maksimum!"
                    Service.send_notice_box(p.conn, "Sudah digabung hingga batas maksimum!");
                }
                break;
            }
            case 2: {
                short id_g1 = m2.reader().readShort();
                Item3 it3 = p.item.bag3[itemId];
                if (it3 == null || !check_item_can_rebuild(it3.type) || PartFashion.fashions.contains(it3.id)) {
                    // "Trang bị không phù hợp!" -> "Perlengkapan tidak cocok!"
                    Service.send_notice_box(p.conn, "Perlengkapan tidak cocok!");
                    return;
                }
                if (it3 != null) {
                    short[] ngoc_in4 = p.item.checkSocket(it3);
                    if (ngoc_in4[0] != -2 && ngoc_in4[1] != -2 && ngoc_in4[2] != -2) {
                        // "Không thể đục thêm với vật phẩm này!" -> "Tidak bisa melubangi item ini lagi!"
                        Service.send_notice_box(p.conn, "Tidak bisa melubangi item ini lagi!");
                        return;
                    }
                    int index_ngoc_kham_vao = -1;
                    for (int i = 0; i < ngoc_in4.length; i++) {
                        if (ngoc_in4[i] == -2) {
                            index_ngoc_kham_vao = i;
                            break;
                        }
                    }
                    if (index_ngoc_kham_vao == -1) {
                        // "Lỗi hãy thử lại!" -> "Kesalahan, silakan coba lagi!"
                        Service.send_notice_box(p.conn, "Kesalahan, silakan coba lagi!");
                        return;
                    }
                    if (p.getGold() < ((index_ngoc_kham_vao + 1) * 1_000_000)) {
                        // "Không đủ " + ((index_ngoc_kham_vao + 1) * 1_000_000) + " vàng!" -> "Tidak cukup " + ((index_ngoc_kham_vao + 1) * 1_000_000) + " emas!"
                        Service.send_notice_box(p.conn, "Tidak cukup " + ((index_ngoc_kham_vao + 1) * 1_000_000) + " emas!");
                        return;
                    }
                    if (p.item.total_item_by_id(7, id_g1) < 1) {
                        // "Không thể đủ nguyên liệu đục!" -> "Tidak cukup bahan untuk melubangi!"
                        Service.send_notice_box(p.conn, "Tidak cukup bahan untuk melubangi!");
                        return;
                    }
                    boolean suc = false;
                    if (id_g1 == 33) {
                        suc = 35 > Util.random(100 + index_ngoc_kham_vao * 35);
                    } else if (id_g1 == 44) {
                        suc = 55 > Util.random(100 + index_ngoc_kham_vao * 35);
                    } else if (id_g1 == 45) {
                        suc = 65 > Util.random(100 + index_ngoc_kham_vao * 35);
                    } else {
                        // "Nguyên liệu đục không hợp lệ!" -> "Bahan untuk melubangi tidak valid!"
                        Service.send_notice_box(p.conn, "Bahan untuk melubangi tidak valid!");
                        return;
                    }
                    p.updateGold(-((index_ngoc_kham_vao + 1) * 1_000_000));
                    p.item.remove(7, id_g1, 1);
                    if (p.conn.ac_admin > 3 && Manager.BuffAdmin) {
                        suc = true;
                    }
                    Message m = new Message(-100);
                    if (suc) {
                        it3.op.add(new Option((58 + index_ngoc_kham_vao), -1, it3.id));
                        m.writer().writeByte(3);
                        // "Thành công, chúc mừng" -> "Berhasil, selamat"
                        m.writer().writeUTF("Berhasil, selamat");
                    } else {
                        m.writer().writeByte(4);
                        // "Chúc con may mắn lần sau!!" -> "Semoga beruntung lain kali!!"
                        m.writer().writeUTF("Semoga beruntung lain kali!!");
                    }
                    m.writer().writeShort(it3.id);
                    m.writer().writeByte(3);
                    p.conn.addmsg(m);
                    m.cleanup();
                    p.item.charInventory(4);
                    p.item.charInventory(7);
                    p.item.charInventory(3);
                }
                break;
            }
        }
    }

    private static boolean checkInlayByType(short gemId, Item3 eq) {
        if (isGemMatch(eq.type, gemId)) {
            return true;
        }

        // Try second set (gemId - 30)
        return isGemMatch(eq.type, (short) (gemId - 30));
    }

    private static boolean isGemMatch(int eqType, short gemId) {

        if (eqType >= 8 && eqType <= 11) {
            return (gemId >= 352 && gemId <= 361) || (gemId >= 23 && gemId <= 32);
        }

        if (eqType == 0 || eqType == 1 || eqType == 2 || eqType == 3 || eqType == 6) {
            return (gemId >= 362 && gemId <= 371) || (gemId >= 35 && gemId <= 39) || (gemId >= 40 && gemId <= 44);
        }

        if (eqType == 4 || eqType == 5) {
            return gemId >= 372 && gemId <= 381;
        }
        // Tambahkan di checkInlayByType atau isGemMatch
        if (eqType == 16) {  // ganti TYPE_MEDAL sesuai type medal
            return (gemId >= 412 && gemId <= 416);  // Permata Primitif
        }

        return false;
    }

    public static int getGoldPriceForGem(int id) {
        if (id >= 23 && id <= 43) {
            return (((id - 23) % 5) + 1) * 50_000;
        } else {
            return (((id - 352) % 5) + 1) * 50_000;
        }
    }


    public static short[] Ratio_UpgradeItemStar = new short[]{4000, 3500, 3000, 2500, 2000, 1500, 1200, 900, 600, 500, 400, 300};

    public static void ActionsItemStar(Session conn, Message m) throws IOException {
        if (conn.p.time_speed_rebuild > System.currentTimeMillis()) {
            Service.send_notice_box(conn, "Pelan-pelan!");
            return;
        }
        conn.p.time_speed_rebuild = System.currentTimeMillis() + 2000;
        byte type = m.reader().readByte();
        short id = -1;
        byte tem = -1;
        try {
            id = m.reader().readShort();
            tem = m.reader().readByte();
        } catch (IOException e) {
        }
//        System.out.println(type);
//        System.out.println(id);
//        System.out.println(tem);

        System.out.println("type: " + type + " id: " + id + " tem: " + tem);
        switch (type) {
            case 0: {
                if (tem != 3) {
                    // "Trang bị không phù hợp!" -> "Perlengkapan tidak cocok!"
                    Service.send_notice_box(conn, "Perlengkapan tidak cocok!");
                    return;
                }
                if (id >= conn.p.item.bag3.length) {
                    return;
                }
                Item3 temp = conn.p.item.bag3[id];
                if (!(temp.id >= 4656 && temp.id <= 4675)) {
                    // "Trang bị không phù hợp!" -> "Perlengkapan tidak cocok!"
                    Service.send_notice_box(conn, "Perlengkapan tidak cocok!");
                    return;
                }
                if (temp.color > 4 && temp.tierStar < 9) {
                    conn.p.TypeItemStarCreate = utils.ItemStar.ConvertType(temp.type);
                    Message m_send = new Message(-105);
                    m_send.writer().writeByte(4);
                    m_send.writer().writeByte(5);
                    if (conn.p.MaterialItemStar == null || conn.p.MaterialItemStar.length < 5) {
                        conn.p.SetMaterialItemStar();
                    }
                    for (int i = conn.p.TypeItemStarCreate * 5; i < conn.p.TypeItemStarCreate * 5 + 5; i++) {
                        m_send.writer().writeShort(conn.p.MaterialItemStar[i]);
                        m_send.writer().writeByte(1);
                    }
                    conn.addmsg(m_send);
                    m_send.cleanup();
                } else {
                    // "Lỗi hãy thử lại!" -> "Kesalahan, silakan coba lagi!"
                    Service.send_notice_box(conn, "Equipment sudah mencapai level max");
                }
                conn.p.time_speed_rebuild = 0;
                break;
            }
            case 3://tạo trang bị tinh tú -> membuat perlengkapan bintang
            {
                for (int i = conn.p.TypeItemStarCreate * 5; i < conn.p.TypeItemStarCreate * 5 + 5; i++) {
                    if (conn.p.item.total_item_by_id(7, conn.p.MaterialItemStar[i]) < 1 && (conn.ac_admin < 4 || !Manager.BuffAdminMaterial)) {
                        // "Thiếu nguyên liệu!" -> "Bahan tidak cukup!"
                        Service.send_notice_box(conn, "Bahan tidak cukup!");
                        return;
                    }
                }
                for (int i = conn.p.TypeItemStarCreate * 5; i < conn.p.TypeItemStarCreate * 5 + 5; i++) {
                    conn.p.item.remove(7, conn.p.MaterialItemStar[i], 1);
                }
                short type_item = utils.ItemStar.ConvertType(conn.p.TypeItemStarCreate, conn.p.ClazzItemStar);
                short id_item = utils.ItemStar.GetIDItem(conn.p.TypeItemStarCreate, conn.p.ClazzItemStar);
                if (type_item == -1 || id_item == -1) {
                    // "Lỗi hãy thử lại sau!" -> "Kesalahan, silakan coba lagi nanti!"
                    Service.send_notice_box(conn, "Kesalahan, silakan coba lagi nanti!");
                    return;
                }
                List<Option> ops = utils.ItemStar.GetOpsItemStar(conn.p.ClazzItemStar, (byte) type_item, 0);
                if (ops != null && ops.size() > 0) {
                    conn.p.ChangeMaterialItemStar(conn.p.TypeItemStarCreate);
                    int ran = Util.random(100);
                    byte color = 1;
                    if ((conn.ac_admin > 4 && Manager.BuffAdmin)) {
                        color = 5; // Admin buff guaranteed highest item
                    } else if (ran < 10) {       // 10%
                        color = 5; // Level 5
                    } else if (ran < 25) {       // 15%
                        color = 4; // Level 4
                    } else if (ran < 45) {       // 20%
                        color = 3; // Level 3
                    } else if (ran < 70) {       // 25%
                        color = 2; // Level 2
                    }
                    Item3 itbag = new Item3();
                    itbag.id = id_item;
                    itbag.name = ItemTemplate3.item.get(id_item).getName();
                    itbag.clazz = ItemTemplate3.item.get(id_item).getClazz();
                    itbag.type = ItemTemplate3.item.get(id_item).getType();
                    itbag.level = 60;
                    itbag.icon = ItemTemplate3.item.get(id_item).getIcon();
                    itbag.op = new ArrayList<>();
                    for (int i = 0; i < ops.size(); i++) {
                        Option o = ops.get(i);
                        int pr = (int) o.getParam(0);
                        int pr1 = (int) (pr + (int) (pr * (color) * 0.167));
                        int pr2 = (int) (pr + (int) (pr * (color + 1) * 0.167));
                        if ((o.id >= 58 && o.id <= 60) || (o.id >= 100 && o.id <= 107)) {
                            itbag.op.add(new Option(o.id, pr, itbag.id));
                        } else if (o.id == 37 || o.id == 38 && itbag.tierStar < 7) {
                            itbag.op.add(new Option(o.id, 2, itbag.id));
                        } else if (o.id == 37 || o.id == 38 && itbag.tierStar > 7) {
                            itbag.op.add(new Option(o.id, 3, itbag.id));
                        } else {
                            itbag.op.add(new Option(o.id, Util.random(pr1, pr2), itbag.id));
                        }
//                        if(o.id == 37 || o.id == 38)
//                            itbag.op.add(new Option(o.id, 1+Util.random(pr1, pr2) , itbag.id));
//                        else
//                            itbag.op.add(new Option(o.id, Util.random(pr1, pr2), itbag.id));
                    }
                    int[] opAo = {-111, -110, -109, -108, -107};
                    int[] opNon = {-102, -113, -105};
                    int[] opVK = {-101, -113, -86, -84, -82, -80};
                    int[] opNhan = {-91, -87, -104, -86, -84, -82, -80};
                    int[] opDayChuyen = {-87, -105, -103, -91};
                    ;
                    int[] opGang = {-89, -103, -91};
                    int[] opGiay = {-104, -103, -91};


                    if (color == 4) {
                        if (itbag.type == 0 || itbag.type == 1) {
                            int percent = Util.nextInt(0, 100);
                            if (percent > 85) {
                                int opid1 = opAo[Util.nextInt(opAo.length)];
                                int opid2 = opAo[Util.nextInt(opAo.length)];
                                while (opid1 == opid2) {
                                    opid1 = opAo[Util.nextInt(opAo.length)];
                                }
                                itbag.op.add(new Option(opid1, Util.random(10, 100), itbag.id));
                                itbag.op.add(new Option(opid2, Util.random(10, 100), itbag.id));
                            } else {
                                int opid = opAo[Util.nextInt(opAo.length)];
                                itbag.op.add(new Option(opid, Util.random(10, 100), itbag.id));
                            }
                        } else if (itbag.type == 2) {

                            int percent = Util.nextInt(0, 100);
                            if (percent > 85) {
                                int opid1 = opNon[Util.nextInt(opNon.length)];
                                int opid2 = opNon[Util.nextInt(opNon.length)];
                                while (opid1 == opid2) {
                                    opid1 = opNon[Util.nextInt(opNon.length)];
                                }
                                itbag.op.add(new Option(opid1, Util.random(10, 100), itbag.id));
                                itbag.op.add(new Option(opid2, Util.random(10, 100), itbag.id));
                            } else {
                                int opid = opNon[Util.nextInt(opNon.length)];
                                itbag.op.add(new Option(opid, Util.random(10, 100), itbag.id));
                            }
                        } else if (itbag.type == 3) {
                            int percent = Util.nextInt(0, 100);
                            if (percent > 85) {
                                int opid1 = opGang[Util.nextInt(opGang.length)];
                                int opid2 = opGang[Util.nextInt(opGang.length)];
                                while (opid1 == opid2) {
                                    opid1 = opGang[Util.nextInt(opGang.length)];
                                }
                                itbag.op.add(new Option(opid1, Util.random(10, 100), itbag.id));
                                itbag.op.add(new Option(opid2, Util.random(10, 100), itbag.id));
                            } else {
                                int opid = opGang[Util.nextInt(opGang.length)];
                                itbag.op.add(new Option(opid, Util.random(10, 100), itbag.id));
                            }
                        } else if (itbag.type == 4) {
                            int percent = Util.nextInt(0, 100);
                            if (percent > 85) {
                                int opid1 = opNhan[Util.nextInt(opNhan.length)];
                                int opid2 = opNhan[Util.nextInt(opNhan.length)];
                                while (opid1 == opid2) {
                                    opid1 = opNhan[Util.nextInt(opNhan.length)];
                                }
                                itbag.op.add(new Option(opid1, Util.random(10, 100), itbag.id));
                                itbag.op.add(new Option(opid2, Util.random(10, 100), itbag.id));
                            } else {
                                int opid = opNhan[Util.nextInt(opNhan.length)];
                                itbag.op.add(new Option(opid, Util.random(10, 100), itbag.id));
                            }
                        } else if (itbag.type == 5) {
                            int percent = Util.nextInt(0, 100);
                            if (percent > 85) {
                                int opid1 = opDayChuyen[Util.nextInt(opDayChuyen.length)];
                                int opid2 = opDayChuyen[Util.nextInt(opDayChuyen.length)];
                                while (opid1 == opid2) {
                                    opid1 = opDayChuyen[Util.nextInt(opDayChuyen.length)];
                                }
                                itbag.op.add(new Option(opid1, Util.random(10, 100), itbag.id));
                                itbag.op.add(new Option(opid2, Util.random(10, 100), itbag.id));
                            } else {
                                int opid = opDayChuyen[Util.nextInt(opDayChuyen.length)];
                                itbag.op.add(new Option(opid, Util.random(10, 100), itbag.id));
                            }
                        } else if (itbag.type == 6) {
                            int percent = Util.nextInt(0, 100);
                            if (percent > 85) {
                                int opid1 = opGiay[Util.nextInt(opGiay.length)];
                                int opid2 = opGiay[Util.nextInt(opGiay.length)];
                                while (opid1 == opid2) {
                                    opid1 = opGiay[Util.nextInt(opGiay.length)];
                                }
                                itbag.op.add(new Option(opid1, Util.random(10, 100), itbag.id));
                                itbag.op.add(new Option(opid2, Util.random(10, 100), itbag.id));
                            } else {
                                int opid = opGiay[Util.nextInt(opGiay.length)];
                                itbag.op.add(new Option(opid, Util.random(10, 100), itbag.id));
                            }
                        } else if (itbag.type > 6) {
                            int percent = Util.nextInt(0, 100);
                            if (percent > 85) {
                                int opid1 = opVK[Util.nextInt(opVK.length)];
                                int opid2 = opVK[Util.nextInt(opVK.length)];
                                while (opid1 == opid2) {
                                    opid1 = opVK[Util.nextInt(opVK.length)];
                                }
                                itbag.op.add(new Option(opid1, Util.random(10, 100), itbag.id));
                                itbag.op.add(new Option(opid2, Util.random(10, 100), itbag.id));
                            } else {
                                int opid = opVK[Util.nextInt(opVK.length)];
                                itbag.op.add(new Option(opid, Util.random(10, 100), itbag.id));
                            }
                        }
                    } else if (color == 5) {
                        if (itbag.type == 0 || itbag.type == 1) {
                            int percent = Util.nextInt(0, 100);
                            if (percent > 85) {
                                int opid1 = opAo[Util.nextInt(opAo.length)];
                                int opid2 = opAo[Util.nextInt(opAo.length)];
                                int opid3 = opAo[Util.nextInt(opAo.length)];
                                while ((opid1 == opid2) || (opid1 == opid3)) {
                                    opid1 = opAo[Util.nextInt(opAo.length)];
                                }
                                while ((opid2 == opid1) || (opid2 == opid3)) {
                                    opid2 = opAo[Util.nextInt(opAo.length)];
                                }
                                while ((opid3 == opid2) || (opid1 == opid3)) {
                                    opid3 = opAo[Util.nextInt(opAo.length)];
                                }
                                if (percent > 87) {
                                    itbag.op.add(new Option(opid3, Util.random(10, 100), itbag.id));
                                }
                                itbag.op.add(new Option(opid1, Util.random(10, 100), itbag.id));
                                itbag.op.add(new Option(opid2, Util.random(10, 100), itbag.id));
                            } else {
                                int opid = opAo[Util.nextInt(opAo.length)];
                                itbag.op.add(new Option(opid, Util.random(10, 100), itbag.id));
                            }
                        } else if (itbag.type == 2) {
                            int percent = Util.nextInt(0, 100);
                            if (percent > 72) {
                                int opid1 = opNon[Util.nextInt(opNon.length)];
                                int opid2 = opNon[Util.nextInt(opNon.length)];
                                int opid3 = opNon[Util.nextInt(opNon.length)];
                                while ((opid1 == opid2) || (opid1 == opid3)) {
                                    opid1 = opNon[Util.nextInt(opNon.length)];
                                }
                                while ((opid2 == opid1) || (opid2 == opid3)) {
                                    opid2 = opNon[Util.nextInt(opNon.length)];
                                }
                                while ((opid3 == opid2) || (opid1 == opid3)) {
                                    opid3 = opNon[Util.nextInt(opNon.length)];
                                }
                                if (percent > 87) {
                                    itbag.op.add(new Option(opid3, Util.random(10, 100), itbag.id));
                                }
                                itbag.op.add(new Option(opid1, Util.random(10, 100), itbag.id));
                                itbag.op.add(new Option(opid2, Util.random(10, 100), itbag.id));
                            } else {
                                int opid = opNon[Util.nextInt(opNon.length)];
                                itbag.op.add(new Option(opid, Util.random(10, 100), itbag.id));
                            }
                        } else if (itbag.type == 3) {
                            int percent = Util.nextInt(0, 100);
                            if (percent > 72) {
                                int opid1 = opGang[Util.nextInt(opGang.length)];
                                int opid2 = opGang[Util.nextInt(opGang.length)];
                                int opid3 = opGang[Util.nextInt(opGang.length)];
                                while ((opid1 == opid2) || (opid1 == opid3)) {
                                    opid1 = opGang[Util.nextInt(opGang.length)];
                                }
                                while ((opid2 == opid1) || (opid2 == opid3)) {
                                    opid2 = opGang[Util.nextInt(opGang.length)];
                                }
                                while ((opid3 == opid2) || (opid1 == opid3)) {
                                    opid3 = opGang[Util.nextInt(opGang.length)];
                                }
                                if (percent > 87) {
                                    itbag.op.add(new Option(opid3, Util.random(10, 100), itbag.id));
                                }
                            } else {
                                int opid = opGang[Util.nextInt(opGang.length)];
                                itbag.op.add(new Option(opid, Util.random(10, 100), itbag.id));
                            }
                        } else if (itbag.type == 4) {
                            int percent = Util.nextInt(0, 100);
                            if (percent > 72) {
                                int opid1 = opNhan[Util.nextInt(opNhan.length)];
                                int opid2 = opNhan[Util.nextInt(opNhan.length)];
                                int opid3 = opNhan[Util.nextInt(opNhan.length)];
                                while ((opid1 == opid2) || (opid1 == opid3)) {
                                    opid1 = opNhan[Util.nextInt(opNhan.length)];
                                }
                                while ((opid2 == opid1) || (opid2 == opid3)) {
                                    opid2 = opNhan[Util.nextInt(opNhan.length)];
                                }
                                while ((opid3 == opid2) || (opid1 == opid3)) {
                                    opid3 = opNhan[Util.nextInt(opNhan.length)];
                                }
                                if (percent > 87) {
                                    itbag.op.add(new Option(opid3, Util.random(10, 100), itbag.id));
                                }
                                itbag.op.add(new Option(opid1, Util.random(10, 100), itbag.id));
                                itbag.op.add(new Option(opid2, Util.random(10, 100), itbag.id));
                            } else {
                                int opid = opNhan[Util.nextInt(opNhan.length)];
                                itbag.op.add(new Option(opid, Util.random(10, 100), itbag.id));
                            }
                        } else if (itbag.type == 5) {
                            int percent = Util.nextInt(0, 100);
                            if (percent > 72) {
                                int opid1 = opDayChuyen[Util.nextInt(opDayChuyen.length)];
                                int opid2 = opDayChuyen[Util.nextInt(opDayChuyen.length)];
                                int opid3 = opDayChuyen[Util.nextInt(opDayChuyen.length)];
                                while ((opid1 == opid2) || (opid1 == opid3)) {
                                    opid1 = opDayChuyen[Util.nextInt(opDayChuyen.length)];
                                }
                                while ((opid2 == opid1) || (opid2 == opid3)) {
                                    opid2 = opDayChuyen[Util.nextInt(opDayChuyen.length)];
                                }
                                while ((opid3 == opid2) || (opid1 == opid3)) {
                                    opid3 = opDayChuyen[Util.nextInt(opDayChuyen.length)];
                                }
                                if (percent > 87) {
                                    itbag.op.add(new Option(opid3, Util.random(10, 100), itbag.id));
                                }
                                itbag.op.add(new Option(opid1, Util.random(10, 100), itbag.id));
                                itbag.op.add(new Option(opid2, Util.random(10, 100), itbag.id));
                            } else {
                                int opid = opDayChuyen[Util.nextInt(opDayChuyen.length)];
                                itbag.op.add(new Option(opid, Util.random(10, 100), itbag.id));
                            }
                        } else if (itbag.type == 6) {
                            int percent = Util.nextInt(0, 100);
                            if (percent > 72) {
                                int opid1 = opGiay[Util.nextInt(opGiay.length)];
                                int opid2 = opGiay[Util.nextInt(opGiay.length)];
                                int opid3 = opGiay[Util.nextInt(opGiay.length)];
                                while ((opid1 == opid2) || (opid1 == opid3)) {
                                    opid1 = opGiay[Util.nextInt(opGiay.length)];
                                }
                                while ((opid2 == opid1) || (opid2 == opid3)) {
                                    opid2 = opGiay[Util.nextInt(opGiay.length)];
                                }
                                while ((opid3 == opid2) || (opid1 == opid3)) {
                                    opid3 = opGiay[Util.nextInt(opGiay.length)];
                                }
                                if (percent > 87) {
                                    itbag.op.add(new Option(opid3, Util.random(10, 100), itbag.id));
                                }
                                itbag.op.add(new Option(opid1, Util.random(10, 100), itbag.id));
                                itbag.op.add(new Option(opid2, Util.random(10, 100), itbag.id));
                            } else {
                                int opid = opGiay[Util.nextInt(opGiay.length)];
                                itbag.op.add(new Option(opid, Util.random(10, 100), itbag.id));
                            }
                        } else if (itbag.type > 7) {
                            int percent = Util.nextInt(0, 100);
                            if (percent > 72) {
                                int opid1 = opVK[Util.nextInt(opVK.length)];
                                int opid2 = opVK[Util.nextInt(opVK.length)];
                                int opid3 = opVK[Util.nextInt(opVK.length)];
                                while ((opid1 == opid2) || (opid1 == opid3)) {
                                    opid1 = opVK[Util.nextInt(opVK.length)];
                                }
                                while ((opid2 == opid1) || (opid2 == opid3)) {
                                    opid2 = opVK[Util.nextInt(opVK.length)];
                                }
                                while ((opid3 == opid2) || (opid1 == opid3)) {
                                    opid3 = opVK[Util.nextInt(opVK.length)];
                                }
                                if (percent > 87) {
                                    itbag.op.add(new Option(opid3, Util.random(10, 100), itbag.id));
                                }
                                itbag.op.add(new Option(opid1, Util.random(10, 100), itbag.id));
                                itbag.op.add(new Option(opid2, Util.random(10, 100), itbag.id));
                            } else {
                                int opid = opVK[Util.nextInt(opVK.length)];
                                itbag.op.add(new Option(opid, Util.random(10, 100), itbag.id));
                            }
                        }
                    }
                    itbag.color = color;
                    itbag.part = ItemTemplate3.item.get(id_item).getPart();
                    itbag.tier = 0;
                    itbag.time_use = 0;
                    itbag.islock = false;
                    conn.p.item.add_item_bag3(itbag);
                    conn.p.item.charInventory(3);
                    conn.p.item.charInventory(4);
                    conn.p.item.charInventory(7);
                    m.cleanup();
                    m = new Message(-105);
                    m.writer().writeByte(3);
                    m.writer().writeByte(3);
                    m.writer().writeUTF("Selamat, Anda mendapatkan " + itbag.name);
                    m.writer().writeByte(3);
                    m.writer().writeUTF(itbag.name);
                    m.writer().writeByte(itbag.clazz);
                    m.writer().writeShort(itbag.id);
                    m.writer().writeByte(itbag.type);
                    m.writer().writeShort(itbag.icon);
                    m.writer().writeByte(0); // tier
                    m.writer().writeShort(1); // level required
                    m.writer().writeByte(itbag.color); // color
                    m.writer().writeByte(0); // can sell
                    m.writer().writeByte(0); // can trade
                    m.writer().writeByte(itbag.op.size());
                    for (int i = 0; i < itbag.op.size(); i++) {
                        m.writer().writeByte(itbag.op.get(i).id);
                        m.writer().writeInt(itbag.op.get(i).getParam(0));
                    }
                    m.writer().writeInt(0); // time use
                    m.writer().writeByte(0);
                    m.writer().writeByte(0);
                    m.writer().writeByte(0);
                    conn.addmsg(m);
                    m.cleanup();
                    conn.p.ResetCreateItemStar();
                    Manager.gI().chatKTGprocess("Selamat " + conn.p.name + " berhasil membuat item rare  " + itbag.name);
                } else {
                    Service.send_notice_box(conn, "Tidak menemukan statistik perlengkapan yang cocok!");
                    return;
                }
                break;
            }
            case 4: {//nâng cấp tbtt -> meningkatkan item bintang
                if (id >= conn.p.item.bag3.length || id < 0) {
                    return;
                }
                Item3 temp = conn.p.item.bag3[id];
                if (temp != null && temp.color >= 5 && temp.tierStar < 9) {
                    conn.p.id_Upgrade_Medal_Star = id;
                    MenuController.send_menu_select(conn, -101, new String[]{"Tanpa = " + (Ratio_UpgradeItemStar[temp.tierStar] / 100) + "%",
                            "Batu mars = " + ((Ratio_UpgradeItemStar[temp.tierStar] + 500) / 100) + "%"}, (byte) 1);
                } else {
                    Service.send_notice_box(conn, "Perlengkapan tidak cocok atau sudah mencapai level maksimal!");
                    return;
                }
//                if(temp == null || !(temp.id >= 4656 && temp.id <= 4675)|| tem != 3)
//                {
//                    Service.send_notice_box(conn, "Trang bị không phù hợp!");
//                    return;
//                }

//                for(int i = conn.p.TypeItemStarCreate *5; i < conn.p.TypeItemStarCreate *5+5; i++)
//                {
//                    if(conn.p.item.total_item_by_id(7,conn.p.MaterialItemStar[i]) < 1 && (conn.ac_admin < 4 || !Manager.BuffAdminMaterial))
//                    {
//                        Service.send_notice_box(conn, "Thiếu nguyên liệu!");
//                        return;
//                    }
//                }
//
//                if(temp.tierStar >= Ratio_UpgradeItemStar.length)
//                {
//                    Service.send_notice_box(conn, "Trang bị đã đạt cấp tối đa!");
//                    return;
//                }
//                boolean suc = Ratio_UpgradeItemStar[temp.tierStar] > Util.random(1,101) || (conn.ac_admin >3  && Manager.BuffAdmin);
//                if(suc)
//                {
//                    List<Option> ops = Helps.ItemStar.GetOpsItemStarUpgrade(temp.clazz, temp.type, temp.tierStar+1,temp.op);
//                    if(ops == null || ops.size() <1)
//                    {
//                        Service.send_notice_box(conn, "không tìm thấy chỉ số trang bị phù hợp!");
//                        return;
//                    }
//                    if((temp.tierStar+1) % 3 == 0)
//                        conn.p.ChangeMaterialItemStar(conn.p.TypeItemStarCreate);
//
//                    temp.tierStar++;
//                    temp.level = Helps.ItemStar.GetLevelItemStar(temp.tierStar);
//                    temp.op.clear();
//                    temp.UpdateName();
//                    for(int i = 0; i <ops.size(); i++)
//                    {
//                        Option o = ops.get(i);
//                        int pr = (int)o.getParam(0);
//                        int pr1 = (int)(pr + (int)(pr * (temp.color) * 0.167));
//                        int pr2 = (int)(pr + (int)(pr * (temp.color + 1) * 0.167));
//                        if(o.id == 37 || o.id == 38)
//                            temp.op.add(new Option(o.id, 1+Util.random(pr1, pr2) , temp.id));
//                        else
//                            temp.op.add(new Option(o.id, Util.random(pr1, pr2), temp.id));
//                    }
//                }
//                for(int i = conn.p.TypeItemStarCreate *5; i< conn.p.TypeItemStarCreate *5+5; i++)
//                    conn.p.item.remove(7, conn.p.MaterialItemStar[i], 1);
//                conn.p.item.char_inventory(3);
//                m = new Message(-105);
//                m.writer().writeByte(3);
//                if (suc) {
//                    m.writer().writeByte(3);
//                    m.writer().writeUTF("Thành công!");
//                } else {
//                    m.writer().writeByte(4);
//                    m.writer().writeUTF("Thất bại!");
//                }
//                m.writer().writeByte(3);
//                m.writer().writeUTF(temp.name);
//                m.writer().writeByte(temp.clazz);
//                m.writer().writeShort(temp.id);
//                m.writer().writeByte(temp.type);
//                m.writer().writeShort(temp.icon);
//                m.writer().writeByte(temp.tier); // tier
//                m.writer().writeShort(1); // level required
//                m.writer().writeByte(temp.color); // color
//                m.writer().writeByte(0); // can sell
//                m.writer().writeByte(0); // can trade
//                m.writer().writeByte(temp.op.size());
//                for (int i = 0; i < temp.op.size(); i++) {
//                    m.writer().writeByte(temp.op.get(i).id);
//                    m.writer().writeInt(temp.op.get(i).getParam(0));
//                    
//                }
//                m.writer().writeInt(0); // time use
//                m.writer().writeByte(0);
//                m.writer().writeByte(0);
//                m.writer().writeByte(0);
//                conn.addmsg(m);
//                m.cleanup();
//                if(temp.tierStar <9){
//                    m = new Message(-105);
//                    m.writer().writeByte(5);
//                    if (suc) {
//                        m.writer().writeByte(3);
//                        m.writer().writeUTF("Thành công, xin chúc mừng :)");
//                    } else {
//                        m.writer().writeByte(4);
//                        m.writer().writeUTF("Thất bại rồi :(");
//                    }
//                    m.writer().writeShort(id);
//                    conn.addmsg(m);
//                    m.cleanup();
//                }
                break;
            }
            default: {
                Service.send_notice_box(conn, "Fitur ini belum tersedia!");
                return;
            }
        }
    }

    public static void UpgradeItemStar(Session conn, byte index) throws IOException {
//        try{
        int id = conn.p.id_Upgrade_Medal_Star;
        if (id >= conn.p.item.bag3.length || id < 0) {
            return;
        }
        Item3 temp = conn.p.item.bag3[id];
        if (temp == null || !(temp.id >= 4656 && temp.id <= 4675)) {
            // "Trang bị không phù hợp!" -> "Perlengkapan tidak cocok!"
            Service.send_notice_box(conn, "Perlengkapan tidak cocok!");
            return;
        }
        for (int i = conn.p.TypeItemStarCreate * 5; i < conn.p.TypeItemStarCreate * 5 + 5; i++) {
            if (conn.p.item.total_item_by_id(7, conn.p.MaterialItemStar[i]) < 1 && (conn.ac_admin < 4 || !Manager.BuffAdminMaterial)) {
                // "Thiếu nguyên liệu!" -> "Bahan tidak cukup!"
                Service.send_notice_box(conn, "Bahan tidak cukup!");
                return;
            }
        }

        if (temp.tierStar >= Ratio_UpgradeItemStar.length) {
            // "Trang bị đã đạt cấp tối đa!" -> "Perlengkapan sudah mencapai level maksimal!"
            Service.send_notice_box(conn, "Perlengkapan sudah mencapai level maksimal!");
            return;
        }
        if (index == 1 && conn.p.item.total_item_by_id(7, 471) < 1) {
            // "Bạn không đủ đá hỏa tinh!" -> "Anda tidak punya cukup batu mars!"
            Service.send_notice_box(conn, "Anda tidak punya cukup batu mars!");
            return;
        }
        boolean suc = Ratio_UpgradeItemStar[temp.tierStar] + (index == 1 ? 500 : 0) > Util.random(10000) || (conn.ac_admin > 3 && Manager.BuffAdmin);
        if (suc) {
            List<Option> ops = utils.ItemStar.GetOpsItemStarUpgrade(temp.clazz, temp.type, temp.id, temp.tierStar + 1, temp.op);
            if (ops == null || ops.isEmpty()) {
                // "Lỗi không tìm thấy chỉ số, hãy chụp lại chỉ số và báo ngay cho ad \"Nhắn riêng\"" -> "Kesalahan, tidak menemukan statistik. Ambil tangkapan layar statistik dan segera laporkan ke admin dengan pesan pribadi"
                Service.send_notice_box(conn, "Kesalahan, tidak menemukan statistik. Ambil tangkapan layar statistik dan segera laporkan ke admin dengan pesan pribadi.");
                return;
            }

            temp.tierStar++;
            temp.level = utils.ItemStar.GetLevelItemStar(temp.tierStar);
            temp.op.clear();
            temp.updateName();
            for (Option o : ops) {
                int pr = (int) o.getParam(0);
                int pr1 = (int) (pr + (int) (pr * (temp.color) * 0.167));
                int pr2 = (int) (pr + (int) (pr * (temp.color + 1) * 0.167));
                if ((o.id >= 58 && o.id <= 60) || (o.id >= 100 && o.id <= 107)) {
                    temp.op.add(new Option(o.id, pr, temp.id));
                } else if (o.id == 37 || o.id == 38 && temp.tierStar < 7) {
                    temp.op.add(new Option(o.id, 2, temp.id));
                } else if (o.id == 38 && temp.tierStar > 7) {
                    temp.op.add(new Option(o.id, 3, temp.id));
                } else {
                    temp.op.add(new Option(o.id, Util.random(pr1, pr2), temp.id));
                }
            }
        }
        // xóa nl -> hapus bahan
        for (int i = conn.p.TypeItemStarCreate * 5; i < conn.p.TypeItemStarCreate * 5 + 5; i++) {
            conn.p.item.remove(7, conn.p.MaterialItemStar[i], 1);
        }
        if (index == 1) {
            conn.p.item.remove(7, 471, 1);
        }
        if (suc && (temp.tierStar + 1) % 3 == 0) {
            conn.p.ChangeMaterialItemStar(conn.p.TypeItemStarCreate);
        }
        conn.p.item.charInventory(4);
        conn.p.item.charInventory(7);
        conn.p.item.charInventory(3);
        Message m = new Message(-105);
        m.writer().writeByte(3);
        if (suc) {
            m.writer().writeByte(3);
            // "Thành công!" -> "Berhasil!"
            m.writer().writeUTF("Berhasil!");
        } else {
            m.writer().writeByte(4);
            // "Thất bại!" -> "Gagal!"
            m.writer().writeUTF("Gagal!");
        }
        m.writer().writeByte(3);
        m.writer().writeUTF(temp.name);
        m.writer().writeByte(temp.clazz);
        m.writer().writeShort(temp.id);
        m.writer().writeByte(temp.type);
        m.writer().writeShort(temp.icon);
        m.writer().writeByte(temp.tier); // tier
        m.writer().writeShort(1); // level required
        m.writer().writeByte(temp.color); // color
        m.writer().writeByte(0); // can sell
        m.writer().writeByte(0); // can trade
        m.writer().writeByte(temp.op.size());
        for (int i = 0; i < temp.op.size(); i++) {
            m.writer().writeByte(temp.op.get(i).id);
            m.writer().writeInt(temp.op.get(i).getParam(0));

        }
        m.writer().writeInt(0); // time use
        m.writer().writeByte(0);
        m.writer().writeByte(0);
        m.writer().writeByte(0);
        conn.addmsg(m);
        m.cleanup();
        if (temp.tierStar < 10) {
            m = new Message(-105);
            m.writer().writeByte(5);
            if (suc) {
                m.writer().writeByte(3);
                // "Thành công, xin chúc mừng :)" -> "Berhasil, selamat :)"
                m.writer().writeUTF("Berhasil, selamat :)");
            } else {
                m.writer().writeByte(4);
                // "Thất bại rồi :(" -> "Gagal :("
                m.writer().writeUTF("Gagal :(");
            }
            m.writer().writeShort(id);
            conn.addmsg(m);
            m.cleanup();
        }
//        }catch(Exception eee){
//            eee.printStackTrace();
//        }
    }

}