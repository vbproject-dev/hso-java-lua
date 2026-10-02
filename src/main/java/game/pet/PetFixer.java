package game.pet;

import com.google.gson.*;
import java.util.*;

public class PetFixer {

    private final PetDataService petDataService;
    private final Gson gson = new Gson();

    public PetFixer(PetDataService service) {
        this.petDataService = service;
    }

    public String fix(String petJson) {
        if (petJson == null || petJson.isEmpty()) return "[]";

        JsonArray pets = JsonParser.parseString(petJson).getAsJsonArray();
        JsonArray result = new JsonArray();

        for (JsonElement e : pets) {
            try {
                JsonArray pet = e.getAsJsonArray();

                int petId = pet.get(1).getAsInt();

                PetData template = petDataService.get(petId);

                // ❌ skip pet invalid
                if (template == null) continue;

                // ✅ maxGrow (index 6)
                pet.set(6, new JsonPrimitive(template.getMaxGrow()));

                // ✅ options (index 16)
                pet.set(16, convertOptions(template.getOptions()));

                // 🔥 reset stat (opsional)
                pet.set(7, new JsonPrimitive(0));
                pet.set(8, new JsonPrimitive(0));
                pet.set(9, new JsonPrimitive(0));
                pet.set(10, new JsonPrimitive(0));

                result.add(pet);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return gson.toJson(result);
    }

    private JsonArray convertOptions(List<PetOption> options) {
        JsonArray arr = new JsonArray();

        for (PetOption op : options) {
            JsonArray o = new JsonArray();
            o.add(op.getId());
            o.add(op.getValue());
            o.add(op.getMaxValue());
            arr.add(o);
        }

        return arr;
    }
}