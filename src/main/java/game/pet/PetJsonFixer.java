package game.pet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

class FixPetLegacyOnly {

    private static final boolean DRY_RUN = true; // true dulu, kalau hasil sudah benar baru false

    private static final String JDBC_URL =
            "jdbc:mysql://127.0.0.1:3306/hso_raw?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "asu123";

    static class OptTpl {
        int id;
        int value;
        int maxValue;

        OptTpl(int id, int value, int maxValue) {
            this.id = id;
            this.value = value;
            this.maxValue = maxValue;
        }
    }

    static class PetTpl {
        int id;
        LinkedHashMap<Integer, OptTpl> options = new LinkedHashMap<>();
    }

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS)) {
            conn.setAutoCommit(false);

            System.out.println("Connected DB: " + JDBC_URL);

            Map<Integer, PetTpl> petById = new LinkedHashMap<>();
            Map<Integer, PetTpl> petByImage = new LinkedHashMap<>();
            loadPetTemplates(conn, petById, petByImage);

            System.out.println("Total template byId    : " + petById.size());
            System.out.println("Total template byImage : " + petByImage.size());

            int total = 0;
            int changed = 0;
            int skippedNoTpl = 0;
            int skippedNoOptions = 0;
            int skippedInvalidJson = 0;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, name, pet FROM player WHERE pet IS NOT NULL AND pet <> ''");
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int playerId = rs.getInt("id");
                    String name = rs.getString("name");
                    String petStr = rs.getString("pet");

                    total++;

                    Object parsed = JSONValue.parse(petStr);
                    if (!(parsed instanceof JSONArray)) {
                        skippedInvalidJson++;
                        System.out.println("[SKIP JSON] player=" + playerId + " name=" + name);
                        continue;
                    }

                    JSONArray petList = (JSONArray) parsed;
                    boolean anyChange = false;

                    for (int i = 0; i < petList.size(); i++) {
                        Object obj = petList.get(i);
                        if (!(obj instanceof JSONArray)) {
                            continue;
                        }

                        JSONArray petEntry = (JSONArray) obj;
                        if (petEntry.isEmpty()) {
                            continue;
                        }

                        // Cari index options otomatis
                        int optionIndex = findOptionIndex(petEntry);
                        if (optionIndex == -1) {
                            skippedNoOptions++;
                            System.out.println("[SKIP OPT] player=" + playerId
                                    + ", entry=" + JSONValue.toJSONString(petEntry));
                            continue;
                        }

                        // Lookup template:
                        // 1) format baru -> spriteImage biasanya di index 2
                        // 2) fallback legacy -> pakai petId index 0 bila ada di pet_data
                        PetTpl tpl = null;
                        int spriteImage = -1;
                        int legacyPetId = -1;

                        if (petEntry.size() > 2) {
                            spriteImage = toInt(petEntry.get(2));
                            tpl = petByImage.get(spriteImage);
                        }

                        if (tpl == null && petEntry.size() > 0) {
                            legacyPetId = toInt(petEntry.get(0));
                            tpl = petById.get(legacyPetId);
                        }

                        if (tpl == null) {
                            skippedNoTpl++;
                            System.out.println("[SKIP TPL] player=" + playerId
                                    + ", sprite=" + spriteImage
                                    + ", legacyPetId=" + legacyPetId
                                    + ", entry=" + JSONValue.toJSONString(petEntry));
                            continue;
                        }

                        JSONArray oldOpts = toJsonArray(petEntry.get(optionIndex));
                        JSONArray newOpts = rebuildOptions(oldOpts, tpl.options);

                        if (!jsonEquals(oldOpts, newOpts)) {
                            petEntry.set(optionIndex, newOpts);
                            anyChange = true;

                            System.out.println("[CHANGED] player=" + playerId
                                    + ", name=" + name
                                    + ", sprite=" + spriteImage
                                    + ", legacyPetId=" + legacyPetId
                                    + ", optionIndex=" + optionIndex);
                            System.out.println("  old=" + JSONValue.toJSONString(oldOpts));
                            System.out.println("  new=" + JSONValue.toJSONString(newOpts));
                        }
                    }

                    if (anyChange) {
                        changed++;
                        String newJson = JSONValue.toJSONString(petList);

                        System.out.println("[FIX] " + playerId + " - " + name);

                        if (!DRY_RUN) {
                            try (PreparedStatement ups = conn.prepareStatement(
                                    "UPDATE player SET pet = ? WHERE id = ?")) {
                                ups.setString(1, newJson);
                                ups.setInt(2, playerId);
                                ups.executeUpdate();
                            }
                        }
                    }
                }
            }

            if (DRY_RUN) {
                conn.rollback();
                System.out.println("DRY_RUN aktif, rollback semua.");
            } else {
                conn.commit();
                System.out.println("Commit selesai.");
            }

            System.out.println("==================================");
            System.out.println("Total player dicek     : " + total);
            System.out.println("Total player diubah    : " + changed);
            System.out.println("Skip template null     : " + skippedNoTpl);
            System.out.println("Skip options tidak ada : " + skippedNoOptions);
            System.out.println("Skip JSON invalid      : " + skippedInvalidJson);
            System.out.println("==================================");
        }
    }

    private static void loadPetTemplates(Connection conn,
                                         Map<Integer, PetTpl> byId,
                                         Map<Integer, PetTpl> byImage) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, image, options FROM pet_data");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                PetTpl tpl = new PetTpl();
                tpl.id = rs.getInt("id");

                Object parsedOptions = JSONValue.parse(rs.getString("options"));
                if (parsedOptions instanceof JSONArray) {
                    JSONArray arr = (JSONArray) parsedOptions;
                    for (Object o : arr) {
                        if (!(o instanceof JSONObject)) {
                            continue;
                        }

                        JSONObject jo = (JSONObject) o;
                        int id = toInt(jo.get("id"));
                        int value = toInt(jo.get("value"));
                        int maxValue = toInt(jo.get("maxValue"));

                        tpl.options.put(id, new OptTpl(id, value, maxValue));
                    }
                }

                byId.put(tpl.id, tpl);

                Object parsedImage = JSONValue.parse(rs.getString("image"));
                if (parsedImage instanceof JSONArray) {
                    JSONArray imgArr = (JSONArray) parsedImage;
                    for (Object img : imgArr) {
                        int sprite = toInt(img);
                        byImage.put(sprite, tpl);
                    }
                }
            }
        }
    }

    private static int findOptionIndex(JSONArray petEntry) {
        int[] candidates = {16, 9, 8};

        for (int idx : candidates) {
            if (idx < petEntry.size()) {
                Object obj = petEntry.get(idx);

                if (obj instanceof JSONArray) {
                    return idx;
                }

                if (obj instanceof String) {
                    Object parsed = JSONValue.parse((String) obj);
                    if (parsed instanceof JSONArray) {
                        petEntry.set(idx, parsed);
                        return idx;
                    }
                }
            }
        }

        return -1;
    }

    private static JSONArray rebuildOptions(JSONArray oldOpts,
                                            LinkedHashMap<Integer, OptTpl> tplOpts) {
        Map<Integer, Integer> current = new LinkedHashMap<>();

        for (Object o : oldOpts) {
            if (!(o instanceof JSONArray)) {
                continue;
            }

            JSONArray a = (JSONArray) o;
            if (a.size() < 2) {
                continue;
            }

            int id = toInt(a.get(0));
            int value = toInt(a.get(1));
            current.put(id, value);
        }

        JSONArray result = new JSONArray();

        for (OptTpl tpl : tplOpts.values()) {
            int value = current.containsKey(tpl.id) ? current.get(tpl.id) : tpl.value;

            if (value < 0) {
                value = 0;
            }

            if (tpl.maxValue > 0 && value > tpl.maxValue) {
                value = tpl.maxValue;
            }

            JSONArray row = new JSONArray();
            row.add(tpl.id);
            row.add(value);
            row.add(tpl.maxValue);
            result.add(row);
        }

        return result;
    }

    private static JSONArray toJsonArray(Object o) {
        if (o instanceof JSONArray) {
            return (JSONArray) o;
        }

        if (o instanceof String) {
            Object parsed = JSONValue.parse((String) o);
            if (parsed instanceof JSONArray) {
                return (JSONArray) parsed;
            }
        }

        return new JSONArray();
    }

    private static boolean jsonEquals(Object a, Object b) {
        return JSONValue.toJSONString(a).equals(JSONValue.toJSONString(b));
    }

    private static int toInt(Object o) {
        if (o == null) {
            return 0;
        }

        if (o instanceof Number) {
            return ((Number) o).intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return 0;
        }
    }
}