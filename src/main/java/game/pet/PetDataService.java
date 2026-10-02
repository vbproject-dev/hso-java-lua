package game.pet;

import com.google.gson.*;
import java.sql.*;
import java.util.*;

public class PetDataService {

    private final Map<Integer, PetData> petDataMap = new HashMap<>();
    private final Gson gson = new Gson();

    public PetDataService(Connection conn) throws Exception {
        load(conn);
    }

    private void load(Connection conn) throws Exception {
        String sql = "SELECT id, max_grow, options FROM pet_data";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                PetData p = new PetData();
                p.setId(rs.getInt("id"));
                p.setMaxGrow(rs.getInt("max_grow"));

                String optionsJson = rs.getString("options");
                p.setOptions(parseOptions(optionsJson));

                petDataMap.put(p.getId(), p);
            }
        }
    }

    private List<PetOption> parseOptions(String json) {
        List<PetOption> list = new ArrayList<>();

        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();

            PetOption op = new PetOption();
            op.setId(o.get("id").getAsInt());
            op.setValue(o.get("value").getAsInt());
            op.setMaxValue(o.get("maxValue").getAsInt());

            list.add(op);
        }
        return list;
    }

    public PetData get(int id) {
        return petDataMap.get(id);
    }
}