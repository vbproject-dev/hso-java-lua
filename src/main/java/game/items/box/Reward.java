package game.items.box;

import template.Item3;
import template.Item47;

import java.util.ArrayList;
import java.util.List;

public class Reward {
    public List<Item3> equipments = new ArrayList<>();
    public List<Item47> potions = new ArrayList<>();
    public List<Item47> materials = new ArrayList<>();

    public boolean hasEquip() { return !equipments.isEmpty();}
    public boolean hasPotion() { return !potions.isEmpty();}
    public boolean hasMaterials() { return !materials.isEmpty();}

}
