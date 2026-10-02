package game.items.box;

import java.util.ArrayList;
import java.util.List;

public class ItemBox {
    private int id;
    private String name;
    private BoxType type;
    private int maxRewards;
    private final List<Reward> items = new ArrayList<>();

    public ItemBox(int id, String name, BoxType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public void addReward(Reward reward) {
        this.items.add(reward);
    }

    public int getId() {
        return id;
    }

    public BoxType getType() {
        return type;
    }

    public List<Reward> getItems() {
        return items;
    }

    public int getMaxRewards() {
        return maxRewards;
    }
}
