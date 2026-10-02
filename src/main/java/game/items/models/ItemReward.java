package game.items.models;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import template.Item3;
import template.Item47;
import template.ItemTemplate3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ItemReward {
    private transient final List<Item3> equipments;
    private transient final List<Item47> potions;
    private transient final List<Item47> materials;

    private final long exp;
    private final long gold;
    private final long gem;

    public ItemReward(List<Item3> equipments, List<Item47> potions, List<Item47> materials, long exp, long gold, long gem) {
        this.equipments = equipments;
        this.potions = potions;
        this.materials = materials;
        this.exp = exp;
        this.gold = gold;
        this.gem = gem;
    }


    public static ItemReward fromJson(String json) {
        if (json == null) {
            return new ItemReward(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0, 0, 0);
        }

        JSONObject obj = (JSONObject) JSONValue.parse(json);
        if (obj == null) {
            return new ItemReward(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0, 0, 0);
        }

        String item3Data = obj.get("item3") != null ? obj.get("item3").toString() : null;
        String item4Data = obj.get("item4") != null ? obj.get("item4").toString() : null;
        String item7Data = obj.get("item7") != null ? obj.get("item7").toString() : null;

        long exp = obj.get("exp") != null ? (Long) obj.get("exp") : 0;
        long gold = obj.get("gold") != null ? (Long) obj.get("gold") : 0;
        long gem = obj.get("gem") != null ? (Long) obj.get("gem") : 0;

        return fromJsonArray(item3Data, item4Data, item7Data, exp, gold, gem);
    }

    public static ItemReward fromJsonArray(String item3Data, String item4Data, String item7Data, long exp, long gold, long gem) {
        List<Item3> equipments = new ArrayList<>();
        if (item3Data != null) {
            JSONArray jsonArray = (JSONArray) JSONValue.parse(item3Data);
            if (jsonArray != null) {
                for (Object o : jsonArray) {
                    short itemId = Short.parseShort(o.toString());
                    ItemTemplate3 temp = ItemTemplate3.item.get(itemId);
                    Item3 equip = new Item3();
                    equip.id = temp.getId();
                    equip.name = temp.getName();
                    equip.icon = temp.getIcon();
                    equip.color = temp.getColor();
                    equip.part = temp.getPart();
                    equip.type = temp.getType();
                    equip.level = temp.getLevel();
                    equip.clazz = temp.getClazz();
                    equip.tier = 15;
                    equip.tierStar = 0;
                    equip.islock = true;
                    equip.op = temp.getOp();
                    equipments.add(equip);
                }
            }
        }

        List<Item47> potions = new ArrayList<>();
        if (item4Data != null) {
            JSONArray jsonArray = (JSONArray) JSONValue.parse(item4Data);
            if (jsonArray != null) {
                for (Object o : jsonArray) {
                    JSONArray innerArray = (JSONArray) o; // each inner array
                    if (innerArray.size() >= 2) {
                        Item47 potion = new Item47();
                        potion.id = Short.parseShort(innerArray.get(0).toString());
                        potion.quantity = Short.parseShort(innerArray.get(1).toString());
                        potions.add(potion);
                    }
                }
            }
        }

        List<Item47> materials = new ArrayList<>();
        if (item7Data != null) {
            JSONArray jsonArray = (JSONArray) JSONValue.parse(item7Data);
            for (Object o : jsonArray) {
                JSONArray innerArray = (JSONArray) o; // each inner array
                if (innerArray.size() >= 2) {
                    Item47 material = new Item47();
                    material.id = Short.parseShort(innerArray.get(0).toString());
                    material.quantity = Short.parseShort(innerArray.get(1).toString());
                    materials.add(material);
                }
            }
        }

        return new ItemReward(equipments, potions, materials, exp, gold, gem);

    }

    public List<Item3> equipments() {
        return equipments;
    }

    public List<Item47> potions() {
        return potions;
    }

    public List<Item47> materials() {
        return materials;
    }

    public long exp() {
        return exp;
    }

    public long gold() {
        return gold;
    }

    public long gem() {
        return gem;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ItemReward) obj;
        return Objects.equals(this.equipments, that.equipments) &&
                Objects.equals(this.potions, that.potions) &&
                Objects.equals(this.materials, that.materials) &&
                this.exp == that.exp &&
                this.gold == that.gold &&
                this.gem == that.gem;
    }

    @Override
    public int hashCode() {
        return Objects.hash(equipments, potions, materials, exp, gold, gem);
    }

    @Override
    public String toString() {
        return "ItemReward[" +
                "equipments=" + equipments + ", " +
                "potions=" + potions + ", " +
                "materials=" + materials + ", " +
                "exp=" + exp + ", " +
                "gold=" + gold + ", " +
                "gem=" + gem + ']';
    }


}