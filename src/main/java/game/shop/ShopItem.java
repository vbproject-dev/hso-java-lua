package game.shop;

import lombok.Data;


@Data
public class ShopItem {
    private int itemId;
    private int price;
    private int priceType;
    private int duration;

}
