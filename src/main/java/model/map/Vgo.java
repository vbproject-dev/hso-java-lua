package model.map;

import client.Player;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class Vgo {
	public byte toMap;
	public String name;
	public short x;
	public short y;
	public short toX;
	public short toY;

    public Vgo(int toMap, String name, int toX, int toY){
        this.toMap = (byte)toMap;
        this.name = name;
        this.toX = (short) toX;
        this.toY = (short) toY;
    }

    public static Vgo create(Player player, int mapId, int x, int y) {
        Vgo vgo = new Vgo();
        vgo.x = player.x;
        vgo.y = player.y;
        vgo.toMap = (byte) mapId;
        vgo.toX = (short) x;
        vgo.toY = (short) y;
        return vgo;
    }

    public static Vgo create(int mapId, int x, int y) {
        Vgo vgo = new Vgo();
        vgo.toMap = (byte) mapId;
        vgo.toX = (short) x;
        vgo.toY = (short) y;
        return vgo;
    }
}
