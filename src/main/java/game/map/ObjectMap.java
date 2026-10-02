package game.map;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ObjectMap {
    public short id;
    public short x;
    public short y;

    public ObjectMap(int id, int x, int y) {
        this.id = (short) id;
        this.x = (short) x;
        this.y = (short) y;
    }

    public static List<ObjectMap> fromBytes(byte[] data) throws IOException {

        List<ObjectMap> items = new ArrayList<>();
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        try {
            short size = dis.readShort();
            for (int i = 0; i < size; i++) {

                short id = dis.readShort();
                short x = dis.readShort();
                short y = dis.readShort();

                items.add(new ObjectMap(id, x, y));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }

}
