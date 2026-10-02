package game.items.box;

import java.util.List;

public class Box {
    public int id;
    public BoxType type;
    public List<BoxReward> rewards;

    public Box(int id, BoxType type, List<BoxReward> rewards) {
        this.id = id;
        this.type = type;
        this.rewards = rewards;
    }

    public int getId() {
        return id;
    }

    public BoxType getType() {
        return type;
    }

    public List<BoxReward> getRewards() {
        return rewards;
    }

}
