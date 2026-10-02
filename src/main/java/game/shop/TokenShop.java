package game.shop;

import client.Player;
import core.SQL;
import core.Service;
import client.io.Message;
import client.io.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import template.Item3;
import template.Item47;
import template.ItemTemplate3;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TokenShop {
    public static Map<String, List<Item>> items = new HashMap<>();

    public static boolean loadTokenShop() {
        String query = "SELECT * FROM `token_shop`;";
        try (Connection connection = SQL.gI().getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {

            items.clear();

            while (rs.next()) {
                String shopName = rs.getString("name");
                JSONArray itemArray = (JSONArray) JSONValue.parse(rs.getString("data"));
                if (itemArray == null) {
                    return false;
                }

                List<Item> itemList = new ArrayList<>();

                for (Object element : itemArray) {
                    JSONArray itemData = (JSONArray) JSONValue.parse(element.toString());
                    short itemId = Short.parseShort(itemData.get(0).toString());
                    long itemPrice = Long.parseLong(itemData.get(1).toString());
                    itemList.add(new Item(itemId, itemPrice));
                }

                items.put(shopName, itemList);
            }

            System.out.println("Loaded " + items.size() + " token shop(s) successfully.");
            return true;

        } catch (Exception e) {
            System.err.println("Failed to load token shop: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public static void sendItemShop(Session s, String name) {
        List<TokenShop.Item> shopItems = TokenShop.items.get(name);
        if (shopItems == null) {
            s.p.sendNoticeBox(String.format("%s tidak tersedia", name));
            return;
        }
        try {
            Message m = new Message(23);
            m.writer().writeUTF(name);
            m.writer().writeByte(1);
            m.writer().writeShort(shopItems.size());
            switch (name) {
                case "Starter Pack" -> {
                    for (Item item : shopItems) {
                        ItemTemplate3 temp = ItemTemplate3.item.get(item.id);
                        m.writer().writeShort(temp.getId());
                        m.writer().writeUTF(temp.getName());
                        m.writer().writeByte(temp.getClazz());
                        m.writer().writeByte(temp.getType());
                        m.writer().writeShort(temp.getIcon());
                        m.writer().writeLong(item.price);
                        m.writer().writeShort(temp.getColor() == 5 ? 60 : 1); // Level
                        m.writer().writeByte(temp.getColor());
                        m.writer().writeByte(temp.getOp().size());

                        for (int j = 0; j < temp.getOp().size(); j++) {
                            m.writer().writeByte(temp.getOp().get(j).id);
                            m.writer().writeInt(temp.getOp().get(j).getParam(0));
                        }
                        m.writer().writeByte(0);
                    }
                }
                case "Fashion Shop" -> {
                    for (Item item : shopItems) {
                        ItemTemplate3 temp = ItemTemplate3.item.get(item.id);
                        String localName = temp.getName() +" (30 Hari)";
                        m.writer().writeShort(temp.getId());
                        m.writer().writeUTF(temp.getType() != 14 ?  localName : temp.getName() );
                        m.writer().writeByte(temp.getClazz());
                        m.writer().writeByte(temp.getType());
                        m.writer().writeShort(temp.getIcon());
                        m.writer().writeLong(item.price);
                        m.writer().writeShort(temp.getLevel());
                        m.writer().writeByte(temp.getColor());
                        m.writer().writeByte(temp.getOp().size());

                        for (int j = 0; j < temp.getOp().size(); j++) {
                            m.writer().writeByte(temp.getOp().get(j).id);
                            m.writer().writeInt(temp.getOp().get(j).getParam(0));
                        }
                        m.writer().writeByte(1);
                    }
                }
                default -> {
                    for (Item item : shopItems) {
                        ItemTemplate3 temp = ItemTemplate3.item.get(item.id);
                        m.writer().writeShort(temp.getId());
                        m.writer().writeUTF(temp.getName());
                        m.writer().writeByte(temp.getClazz());
                        m.writer().writeByte(temp.getType());
                        m.writer().writeShort(temp.getIcon());
                        m.writer().writeLong(item.price);
                        m.writer().writeShort(temp.getLevel());
                        m.writer().writeByte(temp.getColor());
                        m.writer().writeByte(temp.getOp().size());

                        for (int j = 0; j < temp.getOp().size(); j++) {
                            m.writer().writeByte(temp.getOp().get(j).id);
                            m.writer().writeInt(temp.getOp().get(j).getParam( 0));
                        }
                        m.writer().writeByte(0);
                    }
                }
            }

            s.addmsg(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void buyStarterPack(Player p, short itemId) throws IOException {

        long price = getPrice(itemId);
        if (price == -1) {
            p.sendNoticeBox("Item tidak ditemukan");
            return;
        }

        if (p.getGold() < price) {
            p.sendNoticeBox("Gem tidak cukup");
            return;
        }

        p.updateGold(-price);

        Item3 itbag = new Item3();
        ItemTemplate3 temp = ItemTemplate3.item.get(itemId);
        itbag.id = itemId;
        itbag.clazz = temp.getClazz();
        itbag.type =temp.getType();
        itbag.icon = temp.getIcon();
        itbag.color = temp.getColor();
        itbag.part = temp.getPart();
        itbag.islock = true;
        itbag.level = (short) (itbag.color == 5 ? 60 : 1);
        itbag.name = temp.getName();
        itbag.tierStar = (byte) (itbag.color == 5 ? 9 : 0);
        itbag.updateName();
        itbag.op = new ArrayList<>();
        itbag.op.addAll(temp.getOp());
        itbag.time_use = 0;

        p.item.add_item_bag3(itbag);
        p.item.charInventory(3);
        p.item.charInventory(4);
        p.item.charInventory(7);
        Service.send_notice_box(p.conn, "Berhasil membeli " + itbag.name);
    }

    public static void buyItemFashion(Player p, short itemId) throws IOException {

        long price = getPrice(itemId);
        if (price == -1) {
            p.sendNoticeBox("Item tidak ditemukan");
            return;
        }

        if (p.getGem() < price) {
            p.sendNoticeBox("Gem tidak cukup");
            return;
        }

        p.updateGem(-price);

        Item3 itbag = new Item3();
        ItemTemplate3 temp = ItemTemplate3.item.get(itemId);
        itbag.id = itemId;
        itbag.clazz = temp.getClazz();
        itbag.type = temp.getType();
        itbag.level = temp.getLevel();
        itbag.icon = temp.getIcon();
        itbag.color = temp.getColor();
        itbag.part = temp.getPart();
        itbag.islock = true;
        itbag.name = temp.getName();
        itbag.tier = 0;
        itbag.op = new ArrayList<>();
        itbag.op.addAll(temp.getOp());
        itbag.time_use = 0;
        long expireDate = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(21 * 30);
        itbag.expiry_date = itbag.type != 14 ? expireDate : 0;

        p.item.add_item_bag3(itbag);
        p.item.charInventory(3);
        p.item.charInventory(4);
        p.item.charInventory(7);
        Service.send_notice_box(p.conn, "Berhasil membeli " + itbag.name);
    }

    public static void buyItemToken(Player p, short itemId) throws IOException  {
        if (p.conn.ac_admin >= 10) {
            Item3 itbag = new Item3();
            ItemTemplate3 temp = ItemTemplate3.item.get(itemId);
            itbag.id = itemId;
            itbag.clazz = temp.getClazz();
            itbag.type =temp.getType();
            itbag.level = temp.getLevel();
            itbag.icon = temp.getIcon();
            itbag.color = temp.getColor();
            itbag.part = temp.getPart();
            itbag.islock = true;
            itbag.name = temp.getName();
            itbag.tier = 15;
            itbag.op = new ArrayList<>();
            itbag.op.addAll(temp.getOp());
            itbag.time_use = 0;
            p.item.add_item_bag3(itbag);
            p.item.charInventory(3);
            Service.send_notice_box(p.conn, "Berhasil membeli peralatan " + itbag.name);
            return;
        }

        long price = getPrice(itemId);
        if (price == -1) {
            Service.send_notice_box(p.conn, "Item tidak ditemukan");
            return;
        }

        Item47 token = p.item.getMaterial((short)472);
        if (token == null) {
            Service.send_notice_box(p.conn, "Kamu tidak memiliki token");
            return;
        }

        if (token.quantity < price) {
            Service.send_notice_box(p.conn, "Kamu tidak memiliki cukup token");
            return;
        }

        p.item.remove(7, token.id, (short) price);

        Item3 itbag = new Item3();
        ItemTemplate3 temp = ItemTemplate3.item.get(itemId);
        itbag.id = itemId;
        itbag.clazz = temp.getClazz();
        itbag.type =temp.getType();
        itbag.level = temp.getLevel();
        itbag.icon = temp.getIcon();
        itbag.color = temp.getColor();
        itbag.part = temp.getPart();
        itbag.islock = true;
        itbag.name = temp.getName();
        itbag.tier = 0;
        itbag.op = new ArrayList<>();
        itbag.op.addAll(temp.getOp());
        itbag.time_use = 0;
        p.item.add_item_bag3(itbag);
        p.item.charInventory(3);
        p.item.charInventory(4);
        p.item.charInventory(7);
        Service.send_notice_box(p.conn, "Berhasil membeli peralatan " + itbag.name);

    }

    public static boolean checkItem(String name, short id) {
        List<TokenShop.Item> shopItems = TokenShop.items.get(name);
        if (shopItems == null) {
            return false;
        }

        return shopItems.stream().anyMatch(item -> item.id == id);
    }


    public static long getPrice(short id) {

        for (List<Item> itemList : items.values()) { // iterate through all shops
            for (Item item : itemList) {
                if (item.id == id) {
                    return item.price; // found it
                }
            }
        }

        return -1; // not found
    }

    public record Item(short id, long price) {

        @Override
        public String toString() {
            return "Item{id=" + id + ", price=" + price + '}';
        }
    }
}
