package game.shop;

import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
public class Shop {
    protected int id;
    protected String name;
    protected int npcId;
    protected int category;
    protected List<ShopItem> items;


    public Optional<ShopItem> find(int itemId) {
        return find(itemId, null);
    }


    public Optional<ShopItem> find(int itemId, Integer priceTypeFilter) {
        return items.stream()
                .filter(item -> item.getItemId() == itemId)
                .filter(item -> priceTypeFilter == null || item.getPriceType() == priceTypeFilter)
                .findFirst();
    }
}