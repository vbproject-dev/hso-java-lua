package game.pet;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import template.PetData;
import utils.SQLHelper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter

public class PetManager {
    private final Map<Integer, PetData> petDataMap = new ConcurrentHashMap<>();
    private final Map<Integer, PetData> petByImageId = new ConcurrentHashMap<>();

    private PetManager() {
    }

    private static class Holder {
        private static final PetManager INSTANCE = new PetManager();
    }

    public static PetManager getInstance() {
        return Holder.INSTANCE;
    }

    public void load() {
        List<PetData> pets = SQLHelper.selectFrom("pet_data").getAsModel(PetData.class);
        for (PetData pet : pets) {
            petDataMap.put(pet.getId(), pet);

            for (int imageId : pet.getImage()) {
                petByImageId.put(imageId, pet);
            }
        }

        log.debug("{} PET DATA LOADED", pets.size());
    }

    public List<PetData> getAll() {
        return petDataMap.values().stream().toList();
    }
    public PetData getPetById(int id) {
        return petDataMap.get(id);
    }

    public PetData getPetByImageId(int imageId) {
        return petByImageId.get(imageId);
    }



}
