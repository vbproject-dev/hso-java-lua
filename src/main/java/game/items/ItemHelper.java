package game.items;

import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import template.Item3;
import template.Item47;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ItemHelper {



    public static List<Item3> item3FromJArray(JSONArray arr) {
        List<Item3> list = new ArrayList<>();

        if (arr == null) return list;

        for (Object o : arr) {
            JSONObject item = (JSONObject) o;
            long id = (Long) item.get("id");
            long duration = (Long) item.get("duration");
            Item3 equip = Item3.fromTemplate((short) id);
            if (equip == null) {
                continue;
            }


            equip.duration = (int)duration;
            equip.expiry_date = duration > 0 ? System.currentTimeMillis() + TimeUnit.HOURS.toMillis(duration) : 0;
            list.add(equip);
        }
        return list;
    }
    public static List<Item3> parseItem3(String jsonStr) {

        List<Item3> list = new ArrayList<>();
        if (jsonStr == null) return list;
        JSONObject obj = (JSONObject) JSONValue.parse(jsonStr);
        JSONArray arr = (JSONArray) obj.get("equip");

        for (Object o : arr) {
            JSONObject item = (JSONObject) o;
            long id = (Long) item.get("id");
            long duration = (Long) item.get("duration");
            Item3 equip = Item3.fromTemplate((short) id);
            if (equip == null) {
                continue;
            }
            equip.duration = (int)duration;
            equip.expiry_date = duration > 0 ? System.currentTimeMillis() + TimeUnit.HOURS.toMillis(duration) : 0;
            list.add(equip);
        }
        return list;
    }

    public static List<Item47> parseItem47(String jsonStr, int category) {

        List<Item47> list = new ArrayList<>();
        if (jsonStr == null) return list;
        JSONObject obj = (JSONObject) JSONValue.parse(jsonStr);
        JSONArray arr = (JSONArray) obj.get(category == 4?  "potion" : "material");
        for (Object o : arr) {
            JSONObject item = (JSONObject) o;
            long id = (Long) item.get("id");
            long quantity = (Long) item.get("quantity");
            if (quantity > 32000) {
                quantity = 32000;
            }

            Item47 item47 = new Item47();
            item47.id = (short) id;
            item47.quantity = (short)quantity;
            item47.category = (byte) category;
            list.add(item47);
        }
        return list;
    }

    public static List<int[]> parseIdQtyList(String str) {
        List<int[]> list = new ArrayList<>();

        if (str == null || str.isEmpty()) return list;

        // remove outer spaces
        str = str.trim();

        // find all [id,qty]
        String[] parts = str.split("],");

        for (String part : parts) {
            part = part.replace("[", "").replace("]", "").trim();
            if (part.isEmpty()) continue;

            String[] vals = part.split(",");
            if (vals.length != 2) continue;

            int id  = Integer.parseInt(vals[0].trim());
            int qty = Integer.parseInt(vals[1].trim());

            list.add(new int[]{id, qty});
        }

        return list;
    }


}
