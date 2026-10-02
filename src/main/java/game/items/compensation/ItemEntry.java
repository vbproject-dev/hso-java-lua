package game.items.compensation;


import lombok.Getter;
import lombok.Setter;
import template.Item47;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ItemEntry {
    private int id;
    private Map<String, Boolean> players;
    private Map<String, RoleItems> equip;
    private long gold;
    private long gems;
    private List<Item47> materials;
    private List<Item47> potions;

    public RoleItems getByClazz(int clazz) {
        if (equip == null || equip.isEmpty()) return null;

        switch (clazz) {
            case 1 -> {
                return equip.get("assasin");
            }
            case 2 -> {
                return equip.get("mage");
            }
            case 3 -> {
                return equip.get("gunner");
            }
            default -> {
                return equip.get("warrior");
            }
        }
    }

}
