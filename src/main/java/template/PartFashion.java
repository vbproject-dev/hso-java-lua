package template;

import java.util.ArrayList;
import java.util.List;
import client.Player;

public class PartFashion {
    public static final List<Short> fashions = new ArrayList<>();
    public static final List<PartFashion> entrys = new ArrayList<>();
    public short id;
    public int[] part;

    // {0:Body, 1:Leg, 2:Hat, 3:IDK, 4:Wing, 5:Hair, 6:Head}
    public static int[] getPart(Player p) {
        if (p.item.wear[11] != null) {
            for (PartFashion temp : entrys) {
                if (temp.id == p.item.wear[11].id) {
                    return temp.part;
                }
            }
        }
        if (p.item.wear[20] != null) {
            for (PartFashion temp : entrys) {
                if (temp.id == p.item.wear[20].id) {
                    return temp.part;
                }
            }
        }

        return new int[]{-1, -1, -1, -1, -1, -1, -1};
    }
}
