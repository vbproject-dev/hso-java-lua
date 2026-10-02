package game.items.box;


public class BoxReward {
    private final int itemId;
    private final int quantity;
    private final int category;
    private final double chance;
    private final int duration;


    public BoxReward(int itemId, int category, int quantity, int duration, double chance) {
        this.itemId = itemId;
        this.category = category;
        this.duration = duration;
        this.chance = chance;
        this.quantity = quantity;
    }

    public int getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getCategory() {
        return category;
    }
    public double getChance() {
        return chance;
    }

    public int getDuration() {
        return duration;
    }
}