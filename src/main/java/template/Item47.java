package template;

public class Item47 {
    public short id;
    public short quantity;
    public byte category;

    public Item47() {
    }

    public Item47(Item47 Origin) {
        this.id = Origin.id;
        this.quantity = Origin.quantity;
        this.category = Origin.category;
    }

    public static boolean isExist(short id, byte category) {
        return category == 4 ? ItemTemplate4.item.get(id) != null : ItemTemplate7.item.get(id) != null;
    }
}
