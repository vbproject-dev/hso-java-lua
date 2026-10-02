package game.items.compensation;

import game.items.ItemHelper;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utils.SQLHelper;

import java.util.*;

@Slf4j
public class CompensationManager {
    private static CompensationManager INSTANCE;

    public Map<Integer, ItemEntry> templates = new HashMap<>();

    public static CompensationManager gI() {
        if (INSTANCE == null) {
            INSTANCE = new CompensationManager();
        }
        return INSTANCE;
    }

    public List<ItemEntry> claimAllCompensations(String playerName) {
        List<ItemEntry> claimedEntries = new ArrayList<>();
        String key = playerName.toLowerCase();


        for (Map.Entry<Integer, ItemEntry> entry : templates.entrySet()) {
            ItemEntry itemEntry = entry.getValue();
            Map<String, Boolean> players = itemEntry.getPlayers();
            if (players != null && players.containsKey(key)) {
                if (!players.get(key)) { // Only claim if not already claimed
                    players.put(key, true);

                    SQLHelper.update()
                            .table("compensation") // corrected table name
                            .set("players", playersJSON(players))
                            .where("id", itemEntry.getId())
                            .execute();

                    claimedEntries.add(itemEntry);
                }
            }
        }

        return claimedEntries; // Return all ItemEntry objects the player claimed
    }



    public static String playersJSON(Map<String, Boolean> players) {
        JSONArray jsonArray = new JSONArray();

        for (Map.Entry<String, Boolean> entry : players.entrySet()) {
            JSONObject obj = new JSONObject();
            obj.put("name", entry.getKey());
            obj.put("status", entry.getValue());
            jsonArray.add(obj);
        }

        return jsonArray.toJSONString();

    }

    public void loadDatabase() {
        templates.clear();

        List<ItemEntry> entries = SQLHelper.selectFrom("compensation")
                .get(rs -> {
                    ItemEntry itemEntry = new ItemEntry();
                    JSONParser parser = new JSONParser();

                    try {
                        int id = rs.getInt("id");
                        String players = rs.getString("players");
                        Map<String, Boolean> playersMap = new HashMap<>();

                        // Parse players JSON
                        if (players != null && !players.isEmpty()) {
                            JSONArray array = (JSONArray) JSONValue.parse(players);
                            array.forEach(o -> {
                                JSONObject obj = (JSONObject) o;
                                String name = (String) obj.get("name");
                                Boolean status = (Boolean) obj.get("status");
                                playersMap.put(name.toLowerCase(), status);
                            });
                            itemEntry.setPlayers(playersMap);
                        }

                        // Parse items JSON
                        String items = rs.getString("items");
                        if (items != null && !items.isEmpty()) {
                            JSONObject json = (JSONObject) parser.parse(items);

                            itemEntry.setGold((Long) json.get("gold"));
                            itemEntry.setGems((Long) json.get("gems"));
                            itemEntry.setPotions(ItemHelper.parseItem47(items, 4));
                            itemEntry.setMaterials(ItemHelper.parseItem47(items, 7));

                            // Parse equipment
                            Map<String, RoleItems> equipMap = new HashMap<>();
                            JSONObject equip = (JSONObject) json.get("equip");

                            if (equip != null) {
                                for (Object roleKey : equip.keySet()) {
                                    String role = (String) roleKey;
                                    JSONObject roleData = (JSONObject) equip.get(role);

                                    JSONArray physicalItems = (JSONArray) roleData.get("physical");
                                    JSONArray elementalItems = (JSONArray) roleData.get("elemental");

                                    RoleItems roleItems = new RoleItems();
                                    roleItems.setPhysical(ItemHelper.item3FromJArray(physicalItems));
                                    roleItems.setElemental(ItemHelper.item3FromJArray(elementalItems));
                                    equipMap.put(role, roleItems);
                                }
                            }

                            itemEntry.setEquip(equipMap);
                        }

                        itemEntry.setId(id);
                        return itemEntry;

                    } catch (ParseException e) {
                        log.error("Error parsing JSON for item entry", e);
                        return null;
                    }
                });

        // Populate templates map (filter out null entries if any parsing failed)
        entries.stream()
                .filter(Objects::nonNull)
                .forEach(entry -> templates.put(entry.getId(), entry));

        log.info("Loaded {} compensation templates", templates.size());
    }
}
