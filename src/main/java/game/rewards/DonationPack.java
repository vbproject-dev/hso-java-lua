package game.rewards;

import game.items.ItemHelper;
import lombok.extern.slf4j.Slf4j;
import template.Item3;
import template.Item47;
import utils.SQLHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class DonationPack {
    public int id;
    public String name;
    public List<Item3> item3 = new ArrayList<>();
    public List<Item47> item4 = new ArrayList<>();
    public List<Item47> item7 = new ArrayList<>();
    public long gold;
    public long gems;

    public static HashMap<Integer, DonationPack> items = new HashMap<>();

    public static DonationPack getItem(int itemId) {
        try {
            return items.get(itemId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean loadData() {
        items.clear();

        try {
            List<DonationPack> packs = SQLHelper.selectFrom("item_donation")
                    .get(rs -> {
                        DonationPack rw = new DonationPack();
                        rw.id = rs.getShort("id");
                        rw.name = rs.getString("name");
                        rw.gold = rs.getLong("gold");
                        rw.gems = rs.getLong("gem");

                        String itemData = rs.getString("items");
                        if (itemData != null && !itemData.isEmpty()) {
                            rw.item3.addAll(ItemHelper.parseItem3(itemData));
                            rw.item4.addAll(ItemHelper.parseItem47(itemData, 4));
                            rw.item7.addAll(ItemHelper.parseItem47(itemData, 7));
                        }

                        return rw;
                    });

            // Populate map
            packs.forEach(pack -> items.putIfAbsent(pack.id, pack));

            log.info("Loaded {} donation packs", packs.size());
            return true;
        } catch (Exception e) {
            log.error("Error loading donation data", e);
            return false;
        }
    }

}
