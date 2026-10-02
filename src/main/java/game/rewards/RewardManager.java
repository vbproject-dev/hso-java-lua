package game.rewards;

import core.SQL;
import core.Service;
import game.event.spin.models.BetType;
import game.event.spin.models.SpinType;
import client.io.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import template.Item3;
import template.Item47;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RewardManager {


    public static List<UserReward> getUnclaimedUserReward(String playerName) {
        List<UserReward> unclaimedRewards = new ArrayList<>();

        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM user_reward")) {

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UserReward rw = new UserReward();
                    rw.id = rs.getShort("id");
                    rw.name = rs.getString("name");

                    int itemId = rs.getInt("item_id");
                    rw.item = DonationPack.getItem(itemId);
                    if (rw.item == null) {
                        continue;
                    }

                    // Parse claimed_by
                    String claimedByData = rs.getString("claimed_by");
                    boolean alreadyClaimed = false;

                    if (claimedByData != null && !claimedByData.isEmpty()) {
                        JSONArray jsar = (JSONArray) JSONValue.parse(claimedByData);
                        if (jsar != null) {
                            for (Object o : jsar) {
                                String claimedName = o.toString();
                                rw.claimedBy.add(claimedName);
                                if (claimedName.equalsIgnoreCase(playerName)) {
                                    alreadyClaimed = true;
                                }
                            }
                            jsar.clear();
                        }
                    }


                    if (rw.name.equalsIgnoreCase("daily_login")) {
                        // Check if player already claimed today
                        if (!alreadyClaimed) {
                            unclaimedRewards.add(rw);
                        }
                    } else if (rw.name.equalsIgnoreCase(playerName)) {
                        // Player-specific reward (only for their name)
                        if (!alreadyClaimed) {
                            unclaimedRewards.add(rw);
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return unclaimedRewards;
    }

    public static List<GambleReward> getUnclaimedSpinReward(String playerName) {
        List<GambleReward> unclaimedRewards = new ArrayList<>();

        String sql = "SELECT * FROM gamble_winner WHERE name = ? AND status = 'PENDING'";

        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, playerName); // set the parameter

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    GambleReward rw = new GambleReward();
                    rw.id = rs.getInt("id");
                    rw.type = SpinType.fromId(rs.getInt("spin_type")); // assuming spin_type is INT
                    rw.name = rs.getString("name");
                    rw.betType = BetType.fromName(rs.getString("bet_type"));
                    rw.amount = rs.getLong("amount");
                    rw.status = rs.getString("status");
                    unclaimedRewards.add(rw);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return unclaimedRewards;
    }


    public static void sendReward(Session s) {

        List<UserReward> rewards = new ArrayList<>(getUnclaimedUserReward(s.p.name));

        List<Short> items = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        List<Short> categories = new ArrayList<>();

        String title = "Kamu Mendapatkan";
        if (!rewards.isEmpty()) {

            for (UserReward rw : rewards) {
                DonationPack donationPack = rw.item;
                if (!donationPack.item3.isEmpty()) {
                    for (Item3 item : donationPack.item3) {
                        if (item.duration > 0) {
                            item.expiry_date = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(item.duration);
                        }
                        item.tier = 15;
                        items.add(item.id);
                        quantities.add(1);
                        categories.add((short) 3);
                        s.p.item.add_item_bag3(item);
                    }
                }

                if (!donationPack.item4.isEmpty()) {
                    for (Item47 item : donationPack.item4) {
                        items.add(item.id);
                        quantities.add((int) item.quantity);
                        categories.add((short) 4);
                        s.p.item.add_item_bag47(4, item);
                    }
                }

                if (!donationPack.item7.isEmpty()) {
                    for (Item47 item : donationPack.item7) {
                        items.add(item.id);
                        quantities.add((int) item.quantity);
                        categories.add((short) 7);
                        s.p.item.add_item_bag47(7, item);
                    }
                }

                if (donationPack.gold > 0) {
                    items.add((short) -1);
                    quantities.add((int) donationPack.gold);
                    categories.add((short) 4);
                    s.p.updateGold(donationPack.gold);
                }

                if (donationPack.gems > 0) {
                    items.add((short) -2);
                    quantities.add((int) donationPack.gems);
                    categories.add((short) 4);
                    s.p.updateGem(donationPack.gems);
                }

                rw.markAsClaimed(s.p.name);

            }

        }

        List<GambleReward> gambleRewards = getUnclaimedSpinReward(s.p.name);
        if (!gambleRewards.isEmpty()) {
            for (GambleReward gmb : gambleRewards) {
                if (gmb.betType == BetType.DIAMOND) {
                    items.add((short) -2);
                    quantities.add((int) gmb.amount);
                    categories.add((short) 4);
                    s.p.updateGem(gmb.amount);
                }else {
                    items.add((short) -1);
                    quantities.add((int) gmb.amount);
                    categories.add((short) 4);
                    s.p.updateGold(gmb.amount);
                }
                gmb.delete();
            }
            title = "Hadiah Roda Putar";
        }

        if (rewards.isEmpty() && gambleRewards.isEmpty()) {
            return;
        }

        short[] ar_id = new short[items.size()];
        int[] ar_quant = new int[quantities.size()];
        short[] ar_type = new short[categories.size()];
        for (int i = 0; i < ar_id.length; i++) {
            ar_id[i] = items.get(i);
            ar_quant[i] = quantities.get(i);
            ar_type[i] = categories.get(i);
        }

        try {
            s.p.item.charInventory(7);
            s.p.item.charInventory(4);
            s.p.item.charInventory(3);
            Service.Show_open_box_notice_item(s.p, title, ar_id, ar_quant, ar_type);
        } catch (IOException ignore) {
        }
    }

}
