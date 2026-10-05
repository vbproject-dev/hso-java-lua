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

    public static Item47 create(int id, int quantity, int category) {
        Item47 item = new Item47();
        item.id = (short) id;
        item.quantity = (short) quantity;
        item.category = (byte) category;
        return item;
    }

    public static boolean isExist(short id, byte category) {
        return category == 4 ? ItemTemplate4.item.get(id) != null : ItemTemplate7.item.get(id) != null;
    }

    public static ItemTemplate4 getPotion(short id) {
        return isExist(id, (byte) 4) ? ItemTemplate4.item.get(id) : null;
    }
    public static ItemTemplate7 getMaterial(short id) {
        return isExist(id, (byte) 4) ? ItemTemplate7.item.get(id) : null;
    }


}
