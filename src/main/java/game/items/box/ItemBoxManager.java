package game.items.box;
import core.SQL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import template.Item3;
import template.Item47;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ItemBoxManager {
    private static ItemBoxManager INSTANCE;
    public HashMap<Integer, Box> boxes = new HashMap<>();

    public static ItemBoxManager gI() {
        if (INSTANCE == null) {
            INSTANCE = new ItemBoxManager();
        }
        return INSTANCE;
    }


    public Box getBoxById(int id) {
       return boxes.get(id);
    }



    public Reward rollSingle(Box itemBox) {
        Reward reward = new Reward();

        // Scale factor untuk mendukung chance desimal (misal 0.1 -> dikali 10 jadi 1)
        // Gunakan 1000 agar mendukung hingga 3 angka desimal (0.001)
        final double SCALE = 1000.0;

        long total = 0;
        for (BoxReward b : itemBox.getRewards()) {
            total += Math.round(b.getChance() * SCALE);
        }

        if (total <= 0) {
            return null;
        }

        long roll = ThreadLocalRandom.current().nextLong(total);
        long current = 0;

        for (BoxReward b : itemBox.getRewards()) {
            current += Math.round(b.getChance() * SCALE);
            if (roll < current) {
                switch (b.getCategory()) {
                    case 3 -> {
                        Item3 item = Item3.fromTemplate((short) b.getItemId());
                        if (item != null) {
                            if (item.type != 14) {
                                item.expiry_date = b.getDuration() == 0 ? 0 : System.currentTimeMillis() + TimeUnit.HOURS.toMillis(b.getDuration());
                            }

                            if (item.expiry_date > 0) {
                                int hours = b.getDuration();         // duration from DB
                                int days = hours / 24;                  // convert hours → days
                                item.name += " (" + days + " Hari)";
                            }

                            reward.equipments.add(item);
                        }
                    }
                    case 4 -> {
                        Item47 item = new Item47();
                        item.id = (short) b.getItemId();
                        item.quantity = (short) b.getQuantity();
                        item.category = 4;
                        reward.potions.add(item);
                    }
                    case 7 -> {
                        Item47 item = new Item47();
                        item.id = (short) b.getItemId();
                        item.quantity = (short) b.getQuantity();
                        item.category = 7;
                        reward.materials.add(item);
                    }
                }
                return reward;
            }

        }
        return null;
    }


    public void loadDatabase() {
        boxes.clear();

        // FIX RESOURCE LEAK: dulu Connection dari SQL.gI().getConnection() (pool
        // HikariCP, maksimal 40 koneksi) tidak pernah di-close() -- baik saat
        // sukses maupun saat SQLException. Setiap kali loadDatabase() ini
        // dipanggil (mis. reload item config lewat admin command / event reset),
        // satu slot koneksi hilang permanen dari pool. Lama-lama pool habis dan
        // SEMUA query lain di server (login, save data, dll) ikut macet nunggu
        // koneksi. try-with-resources menjamin conn/ps/rs selalu ditutup.
        try (Connection conn = SQL.gI().getConnection();
             Statement ps = conn.createStatement();
             ResultSet rs = ps.executeQuery("SELECT * FROM item_box")) {

            while (rs.next()) {

                int boxId = rs.getInt("box_id");
                String items = rs.getString("items");
                BoxType type = BoxType.fromId(rs.getInt("type"));
                if (items == null) {
                    continue;
                }

                switch (type) {
                    case RANDOM_SINGLE -> {

                        List<BoxReward> itemBoxes = new ArrayList<>();
                        JSONArray arr = (JSONArray) JSONValue.parse(items);
                        for (Object o : arr) {
                            JSONObject item = (JSONObject) o;
                            int id = ((Number) item.get("id")).intValue();
                            int category = ((Number) item.get("category")).intValue();
                            int quantity = ((Number) item.get("quantity")).intValue();
                            int duration = ((Number) item.get("duration")).intValue();
                            double chance = ((Number) item.get("chance")).doubleValue();
                            itemBoxes.add(new BoxReward(id,  category,quantity,   duration, chance));
                        }
                        boxes.put(boxId, new Box(boxId, type, itemBoxes));
                    }
                    case RANDOM_MULTIPLE -> {}
                    case COMPENSATION -> {}
                    default -> {}
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



}