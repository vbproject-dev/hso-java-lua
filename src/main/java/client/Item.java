package client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import core.Service;
import client.io.Message;
import template.Item3;
import template.Item47;
import template.ItemTemplate3;

public class Item {

    private Player p;
    public Item3[] bag3;
    public Item3[] box3;
    public Item3[] wear;
    public List<Item47> bag47;
    public List<Item47> box47;

    public Item(Player player) {
        this.p = player;
    }

    public void updateBag() {
        try {
            charInventory(4);
            charInventory(7);
            charInventory(3);
        } catch (Exception ignore) {

        }
    }
    public void charInventory(int type) throws IOException {
        switch (type) {
            case 3 -> {
                Message m = new Message(16);
                m.writer().writeByte(0);
                m.writer().writeByte(3);
                m.writer().writeLong(p.getGold());
                m.writer().writeInt(p.getGem());
                m.writer().writeByte(3);
                m.writer().writeByte(total_item_by_type(3));
                for (int i = 0; i < bag3.length; i++) {
                    Item3 temp = bag3[i];
                    if (temp != null) {
                        m.writer().writeUTF(temp.name);
                        m.writer().writeByte(temp.clazz); // item clazz
                        m.writer().writeShort(i); // id : index
                        m.writer().writeByte(temp.type); // type only
                        m.writer().writeShort(temp.icon); // idicon
                        m.writer().writeByte(temp.tier); // tier
                        m.writer().writeShort(temp.level); // level
                        m.writer().writeByte(temp.color); // color name
                        m.writer().writeByte(1); // can sell
                        m.writer().writeByte(temp.islock ? 0 : 1); // can trade
                        m.writer().writeByte(temp.op.size()); // size
                        for (int j = 0; j < temp.op.size(); j++) {
                            m.writer().writeByte(temp.op.get(j).id);
                            m.writer().writeInt(temp.op.get(j).getParam(temp.tier));

                        }
                        //
                        if (temp.time_use != 0) {
                            long time_use = temp.time_use - System.currentTimeMillis();
                            time_use /= 60_000;
                            m.writer().writeInt((int) ((time_use > 0) ? time_use : 1)); // time use
                        } else {
                            m.writer().writeInt(0); // time use
                        }
                        m.writer().writeByte(temp.islock ? (byte) 1 : (byte) 0); // islock
                        if (temp.expiry_date <= 0) {
                            m.writer().writeByte(0); // b10
                         }else {
                            m.writer().writeByte(1);
//                            m.writer().writeInt(43200);
//                            m.writer().writeUTF(""+temp.expiry_date);
                            m.writer().writeInt(0);
                            m.writer().writeUTF("" + temp.expiry_date);
                        }
                        m.writer().writeByte(0); // canShell_notCanTrade
                    }
                }
                p.conn.addmsg(m);
                m.cleanup();
            }
            case 5 -> {
                Message m = new Message(16);
                m.writer().writeByte(0);
                m.writer().writeByte(5);
                m.writer().writeLong(p.getGold());
                m.writer().writeInt(p.getGem());
                m.writer().writeByte(5);
                m.writer().writeByte(0); // size item quest
                p.conn.addmsg(m);
                m.cleanup();
            }
            case 4, 7 -> {
                Message m = new Message(16);
                m.writer().writeByte(0);
                m.writer().writeByte(type);
                m.writer().writeLong(p.getGold());
                m.writer().writeInt(p.getGem());
                m.writer().writeByte(type);
                m.writer().writeByte(total_item_by_type(type));
                for (int i = 0; i < bag47.size(); i++) {
                    Item47 temp = bag47.get(i);
                    if (temp == null) {
                        continue;
                    }
                    if (temp.category == type) {
                        m.writer().writeShort(temp.id);
                        m.writer().writeShort(temp.quantity);
                        m.writer().writeByte(1);
                        m.writer().writeByte(0);
                    }
                }
                p.conn.addmsg(m);
                m.cleanup();
            }
        }
    }

    public int countItem(short id, int category) {
        Item47 item;
        if ((item = category == 7 ? getMaterial(id) : getPotion(id)) != null) {
            return item.quantity;
        }
        return 0;
    }

    public Item47 getPotion(short id) {
        for (Item47 item : bag47) {
            if (item != null && item.category == 4 && item.id == id) {
                return item;
            }
        }
        return null;
    }

    public Item47 getMaterial(short id) {
        for (Item47 item : bag47) {
            if (item != null && item.category == 7 && item.id == id) {
                return item;
            }
        }
        return null;
    }


    public void char_chest(int type) throws IOException {
        switch (type) {
            case 3: {
                Message m = new Message(65);
                m.writer().writeByte(p.maxbox);
                m.writer().writeByte(0);
                m.writer().writeByte(3);
                m.writer().writeByte(3);
                m.writer().writeByte(total_item_by_type_box(3));
                for (int i = 0; i < box3.length; i++) {
                    Item3 temp = box3[i];
                    if (temp != null) {
                        m.writer().writeUTF(temp.name);
                        m.writer().writeByte(temp.clazz); // item clazz
                        m.writer().writeShort(i); // id : index
                        m.writer().writeByte(temp.type); // type only
                        m.writer().writeShort(temp.icon); // idicon
                        m.writer().writeByte(temp.tier); // tier
                        m.writer().writeShort(temp.level); // level
                        m.writer().writeByte(temp.color); // color name
                        m.writer().writeByte(1); // can sell
                        m.writer().writeByte(temp.islock ? 0 : 1); // can trade
                        m.writer().writeByte(temp.op.size()); // size
                        for (int j = 0; j < temp.op.size(); j++) {
                            m.writer().writeByte(temp.op.get(j).id);
                            m.writer().writeInt(temp.op.get(j).getParam(temp.tier));
                        }
                        if (temp.time_use != 0) {
                            long time_use = temp.time_use - System.currentTimeMillis();
                            time_use /= 3_600_000;
                            m.writer().writeInt((int) ((time_use > 0) ? time_use : 1)); // time use
                        } else {
                            m.writer().writeInt(0); // time use
                        }
                        m.writer().writeByte(temp.islock ? (byte) 1 : (byte) 0); // islock
                        if (temp.expiry_date <= 0) {
                            m.writer().writeByte(0); // no expiry
                        } else {
                            m.writer().writeByte(1); // has expiry
                            m.writer().writeInt(0);
                            m.writer().writeUTF("" + temp.expiry_date);
                        }
                        m.writer().writeByte(0); // canShell_notCanTrade
                    }
                }
                p.conn.addmsg(m);
                m.cleanup();
                break;
            }
            case 5: {
                Message m = new Message(65);
                m.writer().writeByte(p.maxbox);
                m.writer().writeByte(0);
                m.writer().writeByte(5);
                m.writer().writeByte(5);
                m.writer().writeByte(0); // size item quest
                p.conn.addmsg(m);
                m.cleanup();
                break;
            }
            case 4:
            case 7: {
                Message m = new Message(65);
                m.writer().writeByte(p.maxbox);
                m.writer().writeByte(0);
                m.writer().writeByte(type);
                m.writer().writeByte(type);
                m.writer().writeByte(total_item_by_type_box(type));
                for (int i = 0; i < box47.size(); i++) {
                    Item47 temp = box47.get(i);
                    if (temp == null) {
                        continue;
                    }
                    if (temp.category == type) {
                        m.writer().writeShort(temp.id);
                        m.writer().writeShort(temp.quantity);
                        m.writer().writeByte(1);
                        m.writer().writeByte(0);
                    }
                }
                p.conn.addmsg(m);
                m.cleanup();
                break;
            }
        }
    }

    private int total_item_by_type(int type) {
        int quantity = 0;
        switch (type) {
            case 3: {
                for (int i = 0; i < bag3.length; i++) {
                    if (bag3[i] != null) {
                        quantity++;
                    }
                }
                break;
            }
            case 4:
            case 7: {
                for (int i = 0; i < bag47.size(); i++) {
                    Item47 temp = bag47.get(i);
                    if (temp.category == type) {
                        quantity++;
                    }
                }
                break;
            }
        }
        return quantity;
    }

    private int total_item_by_type_box(int type) {
        int quantity = 0;
        switch (type) {
            case 3: {
                for (int i = 0; i < box3.length; i++) {
                    if (box3[i] != null) {
                        quantity++;
                    }
                }
                break;
            }
            case 4:
            case 7: {
                for (int i = 0; i < box47.size(); i++) {
                    Item47 temp = box47.get(i);
                    if (temp.category == type) {
                        quantity++;
                    }
                }
                break;
            }
        }
        return quantity;
    }

    public int get_bag_able() {
        return (p.maxbag - (total_item_by_type(3) + total_item_by_type(4) + total_item_by_type(7)));
    }

    public int get_box_able() {
        return (p.maxbox - (total_item_by_type_box(3) + total_item_by_type_box(4) + total_item_by_type_box(7)));
    }

    public void add_item_bag47(int type, Item47 item) {
        Item47 temp = new Item47(item);
        temp.category = (byte) type;
        switch (type) {
            case 4:
            case 7: {
                int size = temp.quantity;
                if (size > 0) {
                    // FIX: sebelumnya stack lama yang match di-remove() lalu
                    // stack baru di-add() ke PALING BELAKANG list — jadi
                    // posisi item itu (dan otomatis posisi tampil semua item
                    // lain) berubah/"ngacak" tiap kali dapat drop item yang
                    // SUDAH ada di tas. charInventory() ngirim item persis
                    // sesuai urutan list ini ke client, jadi urutan list =
                    // urutan slot yang kelihatan di game.
                    // SEKARANG: kalau stack sudah ada, quantity-nya di-update
                    // LANGSUNG DI TEMPAT (posisi index-nya di list tetap sama).
                    // Cuma item yang BENER-BENER baru yang ditaruh di akhir list.
                    boolean merged = false;
                    for (int i = 0; i < bag47.size(); i++) {
                        Item47 t = bag47.get(i);
                        if (t == null) {
                            continue;
                        }
                        if (t.category == type && t.id == temp.id) {
                            size += t.quantity;
                            t.quantity = (short) Math.min(size, 32000);
                            merged = true;
                            break;
                        }
                    }
                    if (!merged) {
                        temp.quantity = (short) Math.min(size, 32000);
                        bag47.add(temp);
                    }
                }
                break;
            }
        }
    }

    public void add_item_bag47(short id, short qt, byte cat) {
        Item47 temp = new Item47();
        temp.category = cat;
        temp.quantity = qt;
        temp.id = id;
        switch (cat) {
            case 4:
            case 7: {
                int size = temp.quantity;
                if (size > 0) {
                    // FIX: sama seperti overload di atas — update di tempat,
                    // jangan remove+add-di-belakang, supaya urutan slot
                    // inventory tidak berubah/ngacak tiap dapat drop item
                    // yang sudah ada di tas.
                    boolean merged = false;
                    for (int i = 0; i < bag47.size(); i++) {
                        Item47 t = bag47.get(i);
                        if (t == null) {
                            continue;
                        }
                        if (t.category == cat && t.id == id) {
                            size += t.quantity;
                            t.quantity = (short) Math.min(size, 32000);
                            merged = true;
                            break;
                        }
                    }
                    if (!merged) {
                        temp.quantity = (short) Math.min(size, 32000);
                        bag47.add(temp);
                    }
                }
                break;
            }
        }
    }

    public int total_item_by_id(int type, int id) {
        int quantity = 0;
        switch (type) {
            case 3: {
                for (Item3 it : bag3) {
                    if (it == null) {
                        continue;
                    }
                    if (it != null && it.id == id) {
                        quantity += 1;
                    }
                }
                break;
            }
            case 4:
            case 7: {
                for (Item47 it : bag47) {
                    if (it == null) {
                        continue;
                    }
                    if (it.category == type && it.id == id) {
                        quantity += it.quantity;
                    }
                }
                break;
            }
        }
        return quantity;
    }

    public int total_item_by_id_box(int type, short id) {
        int quantity = 0;
        switch (type) {
            case 3: {
                for (Item3 it : box3) {
                    if (it == null) {
                        continue;
                    }
                    if (it != null && it.id == id) {
                        quantity += 1;
                    }
                }
                break;
            }
            case 4:
            case 7: {
                for (Item47 it : box47) {
                    if (it == null) {
                        continue;
                    }
                    if (it.category == type && it.id == id) {
                        quantity += it.quantity;
                    }
                }
                break;
            }
        }
        return quantity;
    }

    public void add_item_box47(int type, Item47 item) {
        Item47 temp = new Item47(item);
        temp.category = (byte) type;
        switch (type) {
            case 4:
            case 7: {
                int size = temp.quantity;
                if (size > 0) {
                    for (int i = box47.size() - 1; i >= 0; i--) {
                        //for (int i = 0; i < box47.size(); i++) {
                        Item47 t = box47.get(i);
                        if (t == null) {
                            continue;
                        }
                        if (t.category == type && t.id == temp.id) {
                            size += t.quantity;
                            box47.remove(i);
                            //break;
                        }
                    }
                    if (size > 32000) {
                        temp.quantity = 32000;
                    } else {
                        temp.quantity = (short) size;
                    }
                    box47.add(temp);
                }
                break;
            }
        }
    }

    public void add_item_box47(short id, short qt, byte cat) {
        Item47 temp = new Item47();
        temp.category = cat;
        temp.quantity = qt;
        temp.id = id;
        switch (cat) {
            case 4:
            case 7: {
                int size = temp.quantity;
                if (size > 0) {
                    for (int i = box47.size() - 1; i >= 0; i--) {
                        Item47 t = box47.get(i);
                        if (t == null) {
                            continue;
                        }
                        if (t.category == cat && t.id == id) {
                            size += t.quantity;
                            box47.remove(i);
                        }
                    }
                    if (size > 32000) {
                        temp.quantity = 32000;
                    } else {
                        temp.quantity = (short) size;
                    }
                    box47.add(temp);
                }
                break;
            }
        }
    }

    public void add_item_bag3(Item3 buffer) {
        for (int j = 0; j < bag3.length; j++) {
            if (bag3[j] == null) {
                bag3[j] = new Item3(buffer);
                break;
            }
        }
    }

    public void add_item_bag3_default(short it, int date, boolean isLock) {
        Item3 itbag = new Item3();
        itbag.id = it;
        itbag.name = ItemTemplate3.item.get(it).getName();
        itbag.clazz = ItemTemplate3.item.get(it).getClazz();
        itbag.type = ItemTemplate3.item.get(it).getType();
        itbag.level = ItemTemplate3.item.get(it).getLevel();
        itbag.icon = ItemTemplate3.item.get(it).getIcon();
        itbag.op = new ArrayList<>();
        itbag.op.addAll(ItemTemplate3.item.get(it).getOp());
        itbag.color = ItemTemplate3.item.get(it).getColor();
        itbag.part = ItemTemplate3.item.get(it).getPart();
        itbag.tier = 0;
        itbag.time_use = 0;
        itbag.islock = isLock;
        if (date > 0) {
            itbag.expiry_date = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * date;
        }
        add_item_bag3(itbag);
    }

    public void add_item_box3(Item3 buffer) {
        for (int j = 0; j < box3.length; j++) {
            if (box3[j] == null) {
                box3[j] = new Item3(buffer);
                break;
            }
        }
    }

    public void remove(int type, int id, int quantity) {
        switch (type) {
            case 3: {
                bag3[id] = null;
                break;
            }
            case 4:
            case 7: {
                for (int j = bag47.size() - 1; j >= 0; j--) {
                    Item47 it = bag47.get(j);
                    if (it == null) {
                        continue;
                    }
                    if (it.category == type && it.id == id && quantity > 0) {
                        if (it.quantity > quantity) {
                            bag47.get(j).quantity -= (short) quantity;
                            break;
                        } else {
                            quantity -= bag47.get(j).quantity;
                            bag47.remove(j);
                            continue;
                        }
                    }
                    if (bag47.get(j).quantity <= 0) {
                        bag47.remove(j);
                    }
                }
                break;
            }
        }
    }

    public void remove_box(int type, short id, int quantity) {
        switch (type) {
            case 3: {
                box3[id] = null;
                break;
            }
            case 4:
            case 7: {
                for (int j = box47.size() - 1; j >= 0; j--) {
                    Item47 it = box47.get(j);
                    if (it == null) {
                        continue;
                    }
                    if (it.category == type && it.id == id && quantity > 0) {
                        if (it.quantity > quantity) {
                            box47.get(j).quantity -= quantity;
                            break;
                        } else {
                            quantity -= box47.get(j).quantity;
                            box47.remove(j);
                            continue;
                        }
                    }
                    if (box47.get(j).quantity <= 0) {
                        box47.remove(j);
                    }
                }
                break;
            }
        }
    }

    public void box_process(Message m2) throws IOException {
        byte type = m2.reader().readByte();
        short id = m2.reader().readShort();
        byte tem = m2.reader().readByte();
        short num = m2.reader().readShort();
        if (num <= 0) {
            return;
        }
        if (type == -1) {
            char_chest(3);
            char_chest(4);
            char_chest(7);
            return;
        }
        if (type == 1) { // cat vao
            if ((get_box_able() < 1 && tem == 3) || (get_box_able() < 1 && tem == 4 && total_item_by_id_box(4, id) < 1)
                    || (get_box_able() < 1 && tem == 7 && total_item_by_id_box(7, id) < 1)) {
                Service.send_notice_box(p.conn, "Rương đầy!");
                return;
            }
            switch (tem) {
                case 3: {
                    if (bag3[id] == null) {
                        Service.send_notice_box(p.conn, "Item tidak ditemukan di tas!");
                        return;
                    }
                    add_item_box3(bag3[id]);
                    remove(3, id, 1);
                    break;
                }
                case 4:
                case 7: {
                    if ((total_item_by_id(tem, id) + total_item_by_id_box(tem, id)) > 32000) {
                        Service.send_notice_box(p.conn, "Jumlah total lebih dari 32k, tidak dapat dilakukan!");
                        return;
                    }
                    if (num > total_item_by_id(tem, id)) {
                        num = (short) total_item_by_id(tem, id);
                        Item47 itbuffer = new Item47();
                        itbuffer.id = id;
                        itbuffer.quantity = num;
                        itbuffer.category = tem;
                        add_item_box47(tem, itbuffer);
                    } else {
                        Item47 itbuffer = new Item47();
                        itbuffer.id = id;
                        itbuffer.quantity = num;
                        itbuffer.category = tem;
                        add_item_box47(tem, itbuffer);
                    }
                    remove(tem, id, num);
                    break;
                }
            }
            charInventory(tem);
            char_chest(tem);
        } else { // lay ra
            if ((get_bag_able() < 1 && tem == 3) || (get_bag_able() < 1 && tem == 4 && total_item_by_id(4, id) < 1)
                    || (get_bag_able() < 1 && tem == 7 && total_item_by_id(7, id) < 1)) {
                Service.send_notice_box(p.conn, "Inventory penuh!");
                return;
            }
            switch (tem) {
                case 3: {
                    if (box3[id] == null) {
                        Service.send_notice_box(p.conn, "Item tidak ditemukan di gudang!");
                        return;
                    }
                    add_item_bag3(box3[id]);
                    remove_box(3, id, 1);
                    break;
                }
                case 4:
                case 7: {
                    if ((total_item_by_id(tem, id) + total_item_by_id_box(tem, id)) > 32000) {
                        Service.send_notice_box(p.conn, "Jumlah total lebih dari 32k, tidak dapat dilakukan!");
                        return;
                    }
                    if (num > total_item_by_id_box(tem, id)) {
                        num = (short) total_item_by_id_box(tem, id);
                        Item47 itbuffer = new Item47();
                        itbuffer.id = id;
                        itbuffer.quantity = num;
                        itbuffer.category = tem;
                        add_item_bag47(tem, itbuffer);
                    } else {
                        Item47 itbuffer = new Item47();
                        itbuffer.id = id;
                        itbuffer.quantity = num;
                        itbuffer.category = tem;
                        add_item_bag47(tem, itbuffer);
                    }
                    remove_box(tem, id, num);
                    break;
                }
            }
            charInventory(tem);
            char_chest(tem);
        }
    }

    public short[] checkSocket(Item3 it3) {
        short[] result = new short[]{-2, -2, -2};
        for (int i = 0; i < it3.op.size(); i++) {
            if (it3.op.get(i).id == 58) {
                result[0] = (short) it3.op.get(i).getParam(0);
            } else if (it3.op.get(i).id == 59) {
                result[1] = (short) it3.op.get(i).getParam(0);
            } else if (it3.op.get(i).id == 60) {
                result[2] = (short) it3.op.get(i).getParam(0);
            }
        }
        return result;
    }

    public void clearBag() throws IOException {
        Arrays.fill(bag3, null);
        bag47.clear();
        charInventory(3);
        charInventory(4);
        charInventory(7);
    }
}