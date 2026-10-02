package feature.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import equipment.StatGenerator;
import utils.StatCalculator;
import game.shop.TokenShop;
import core.Manager;
import core.Service;
import core.Util;
import client.io.Message;
import client.io.Session;
import template.Item3;
import template.Option;

import static core.Service.send_notice_box;

public class AdminMenuController {

    public static final short OTHER_MENU = -501;
    // selectedIndex dipindahkan ke Session agar tidak terjadi race condition antar pemain
    // Gunakan: conn.adminSelectedIndex (tambahkan field ini di Session.java)

    public static void processShopToken(Session c, byte index) throws IOException {
        switch (index) {
            case 0 -> {
                TokenShop.sendItemShop(c, "Warrior");
            }
            case 1 -> {
                TokenShop.sendItemShop(c, "Sorcerer");
            }
            case 2 -> {
                TokenShop.sendItemShop(c, "Assasin");
            }
            case 3 -> {
                TokenShop.sendItemShop(c, "Gunner");
            }
        }
    }

    public static void processAdminMenu(Session conn, byte index) throws IOException {

        if (conn.ac_admin < 10) {
            send_notice_box(conn, "You don't have permission to access this menu.");
            return;
        }
        switch (index) {
            case 0 -> {
                Service.sendBoxInputText(conn, 4, "Atur Xp(x1 = 100)", new String[]{"Masukkan pengganda x :"});
            }
            case 1 -> {
                Service.sendBoxInputText(conn, 1, "Get Item",
                        new String[]{"Jenis Item (3,4,7)", "ID", "Jumlah"});
            }
            case 2 -> {
                if (Manager.gI().reloadItemTemplate3()) {
                    send_notice_box(conn, "Templates Loaded");
                } else {
                    send_notice_box(conn, "Terjadi kesalahan");
                }
            }
            case 3 -> {
                Manager.gI().reloadResources();
                send_notice_box(conn, "Resources loaded");
            }
            case 4 -> {
                if (TokenShop.loadTokenShop()) {
                    send_notice_box(conn, "Berhasil Diperbaharui");
                }
            }
            case 5 -> {
                Service.sendBoxInputText(conn, 29, "Summon Boss",
                        new String[]{"Monster ID"});
            }
            case 6 -> Service.sendBoxInputText(conn, 31, "Goto", new String[]{"Map ID"});
            case 7 -> {
                conn.p.item.clearBag();
                send_notice_box(conn, "Tas berhasil dikosongkan");
            }
            case 8 -> {
                Item3 item = Item3.fromTemplate((short) 5252);
                if (item == null) {
                    send_notice_box(conn, "Item tidak ditemukan");
                    return;
                }
                StatGenerator gen = new StatGenerator();
                item.op = gen.createCloakStat(conn.p.clazz, 0);
                conn.p.item.add_item_bag3(item);
                conn.p.item.charInventory(3);
            }

            default -> send_notice_box(conn, "Invalid menu option selected.");
        }
    }

    public static void processOtherMenu(Session s, byte index) throws IOException {
        Item3 item;

        switch (index) {
            case 0 -> {
                item = s.p.item.bag3[s.adminSelectedIndex];
                if (item != null && item.islock) {

                    if (item.expiry_date > 0) {
                        send_notice_box(s, "Item ini tidak dapat di unlock");
                        return;
                    }

                    if (s.p.getGem() < 20_000) {
                        send_notice_box(s, "Permata tidak cukup dibutuhkan 20.000 Permata");
                        return;
                    }

                    switch (item.type) {
                        case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 22, 24, 21, 29 -> {
                            s.p.updateGem(-20_000);
                            item.islock = false;
                            s.p.item.charInventory(4);
                            s.p.item.charInventory(7);
                            s.p.item.charInventory(3);
                            send_notice_box(s, "Kunci berhasil dibuka");
                            s.adminSelectedIndex = -1;
                        }
                        default -> send_notice_box(s, "Item ini tidak dapat di unlock");
                    }

                } else {
                    send_notice_box(s, "Kunci sudah terbuka");
                }
            }
            case 1 -> {
            }
            case 2 -> {
            }
        }


    }



    public static void handleBoxUI(Session conn, int type, Message m) throws IOException {

        switch (type) {
            case 100 -> {

                m.writer().writeUTF("Murnikan equipments");
                m.writer().writeByte(20);
                m.writer().writeShort(0);

            }
            default -> {
                send_notice_box(conn, "Invalid menu option selected.");
            }
        }
    }

    public static void createOption(Session s, Message m) throws IOException {

        byte type = m.reader().readByte();
        short id = m.reader().readShort();
        byte tem = m.reader().readByte();
        if (type == 0) {
            if (tem != 3) {
                send_notice_box(s, "Item yang dipilih bukan equipment");

                return;
            }


            Item3 temp = s.p.item.bag3[id];
            if (temp == null) {
                send_notice_box(s, "Equipment tidak ditemukan");
                return;
            }

            boolean exists = temp.op.stream().anyMatch(o -> o.id == 122);
            if (exists) {
                send_notice_box(s, "Equipment ini sudah dimurnikan");
                return;
            }

            Message m_send = new Message(-105);
            m_send.writer().writeByte(4);
            m_send.writer().writeByte(0);
            s.addmsg(m_send);
            m_send.cleanup();

        } else if (type == 4) {
            Item3 temp = s.p.item.bag3[id];
            if (temp == null) {
                send_notice_box(s, "Equipment tidak ditemukan");
                return;
            }

            boolean exists = temp.op.stream().anyMatch(o -> o.id == 122);
            if (exists) {
                send_notice_box(s, "Equipment ini sudah dimurnikan");
                return;
            }

            List<Option> options = getRandomOptions(temp);
            if (options.isEmpty()) {
                send_notice_box(s, "Belum ada fungsi");
                return;
            }

            temp.op.clear();
            temp.op.addAll(options);


            Message sc = new Message(-105);
            sc.writer().writeByte(3);
            sc.writer().writeByte(3);
            sc.writer().writeUTF("Berhasil Dimurnikan");

            sc.writer().writeByte(3);
            sc.writer().writeUTF(temp.name);
            sc.writer().writeByte(temp.clazz);
            sc.writer().writeShort(temp.id);
            sc.writer().writeByte(temp.type);
            sc.writer().writeShort(temp.icon);
            sc.writer().writeByte(temp.tier); // tier
            sc.writer().writeShort(temp.level); // level required
            sc.writer().writeByte(temp.color); // color
            sc.writer().writeByte(0); // can sell
            sc.writer().writeByte(0); // can trade
            sc.writer().writeByte(temp.op.size());
            for (int i = 0; i < temp.op.size(); i++) {
                sc.writer().writeByte(temp.op.get(i).id);
                sc.writer().writeInt(temp.op.get(i).getParam(temp.tier));
            }
            sc.writer().writeInt(0); // time use
            sc.writer().writeByte(0);
            sc.writer().writeByte(0);
            sc.writer().writeByte(0);
            s.addmsg(sc);
            sc.cleanup();


            s.p.item.charInventory(4);
            s.p.item.charInventory(7);
            s.p.item.charInventory(3);
        }


    }


    private static List<Option> getRandomOptions(Item3 item) {
        List<Option> options = new ArrayList<>();


        int percent = 0;
        switch (item.type) {
            // Weapon
            case 8, 9, 10, 11 -> {


                int value = 0;
                switch (item.clazz) {
                    case 0 -> {
                        // Base Elemental ATK
                        value = StatCalculator.generateFlat(item.color, item.level).value;
                        options.add(new Option(ThreadLocalRandom.current().nextBoolean() ? 0 : 2, value));

                        // Base ATK
                        value = StatCalculator.generateFlat(item.color, item.level).value;
                        options.add(new Option(40, value - 500));

                        percent = Util.nextInt(0, 100);
                        if (percent < 50 && options.stream().anyMatch(option -> option.id == 0)) {
                            value = StatCalculator.generatePercent(item.color, item.level).value;
                            options.add(new Option(7, value));
                        }
                        percent = Util.nextInt(0, 100);
                        if (percent < 50 && options.stream().anyMatch(option -> option.id == 2)) {
                            value = StatCalculator.generatePercent(item.color, item.level).value;
                            options.add(new Option(9, value));
                        }

                    }
                    case 1 -> {

                        // Base Elemental ATK
                        value = StatCalculator.generateFlat(item.color, item.level).value;
                        options.add(new Option(ThreadLocalRandom.current().nextBoolean() ? 0 : 4, value));

                        // Base ATK
                        value = StatCalculator.generateFlat(item.color, item.level).value;
                        options.add(new Option(40, value - 500));

                        percent = Util.nextInt(0, 100);
                        if (percent < 50 && options.stream().anyMatch(option -> option.id == 0)) {
                            value = StatCalculator.generatePercent(item.color, item.level).value;
                            options.add(new Option(7, value));
                        }
                        percent = Util.nextInt(0, 100);
                        if (percent < 50 && options.stream().anyMatch(option -> option.id == 2)) {
                            value = StatCalculator.generatePercent(item.color, item.level).value;
                            options.add(new Option(11, value));
                        }

                    }
                    case 2 -> {
                        // Base Elemental ATK
                        value = StatCalculator.generateFlat(item.color, item.level).value;
                        options.add(new Option(ThreadLocalRandom.current().nextBoolean() ? 0 : 1, value));

                        // Base ATK
                        value = StatCalculator.generateFlat(item.color, item.level).value;
                        options.add(new Option(40, value - 500));

                        // PHYSIC %
                        percent = Util.nextInt(0, 100);
                        if (percent < 50 && options.stream().anyMatch(option -> option.id == 0)) {
                            value = StatCalculator.generatePercent(item.color, item.level).value;
                            options.add(new Option(7, value));
                        }

                        // ICE %
                        percent = Util.nextInt(0, 100);
                        if (percent < 50 && options.stream().anyMatch(option -> option.id == 2)) {
                            value = StatCalculator.generatePercent(item.color, item.level).value;
                            options.add(new Option(8, value));
                        }

                    }
                    case 3 -> {
                        // Base Elemental ATK
                        value = StatCalculator.generateFlat(item.color, item.level).value;
                        options.add(new Option(ThreadLocalRandom.current().nextBoolean() ? 0 : 3, value));

                        // Base ATK
                        value = StatCalculator.generateFlat(item.color, item.level).value;
                        options.add(new Option(40, value - 500));

                        percent = Util.nextInt(0, 100);
                        if (percent < 50 && options.stream().anyMatch(option -> option.id == 0)) {
                            value = StatCalculator.generatePercent(item.color, item.level).value;
                            options.add(new Option(7, value));
                        }

                        percent = Util.nextInt(0, 100);
                        if (percent < 50 && options.stream().anyMatch(option -> option.id == 3)) {
                            value = StatCalculator.generatePercent(item.color, item.level).value;
                            options.add(new Option(10, value));
                        }

                    }
                }
                options.addAll(getWeaponStat(item));

            }
            // Baju
            case 0 -> {
                // TODO: Implementasikan stat purifikasi untuk Baju
            }
            // Helm
            case 2 -> {
                // TODO: Implementasikan stat purifikasi untuk Helm
            }
            // Pant
            case 1 -> {
                // TODO: Implementasikan stat purifikasi untuk Celana
            }
            // Boot
            case 6 -> {
                // TODO: Implementasikan stat purifikasi untuk Sepatu
            }
            //Glove Ring Necklace
            case 3, 4, 5 -> {
                // TODO: Implementasikan stat purifikasi untuk Sarung Tangan/Cincin/Kalung
            }

        }

        return options;
    }

    public static List<Option> getWeaponStat(Item3 item) {
        List<Option> op = new ArrayList<>();
        int percent;
        int value;


        // PEN
        percent = Util.nextInt(100);
        if (percent < 50) {
            value = Util.nextInt(1200, 1700);
            op.add(new Option(36, value));
        }
        // CRIT
        percent = Util.nextInt(100);
        if (percent < 50) {
            value = Util.nextInt(1200, 1700);
            op.add(new Option(33, value));
        }

        // DODGE
        percent = Util.nextInt(100);
        if (percent < 50) {
            value = Util.nextInt(1200, 1700);
            op.add(new Option(34, value));
        }


        return op;
    }
}