package game.items.models;

import lombok.Data;

import java.util.List;
import java.util.Objects;

@Data
public class ExchangeItem {
    private final int npcId;
    private final String name;
    private final List<RequiredItem> requirements;
    private final ItemReward itemReward;

    public ExchangeItem(int npcId, String name, List<RequiredItem> requirements, ItemReward itemReward) {
        this.npcId = npcId;
        this.name = name;
        this.requirements = requirements;
        this.itemReward = itemReward;
    }



}
