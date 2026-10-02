package model.map;


import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.List;

@Slf4j
@Getter
@Setter
public class MapData {
    private int id;
    private String name;
    private byte type;
    private byte isCity;
    private byte showHs;
    private int miniMap;
    private int maxZone;
    private int maxPlayer;
    private List<Vgo> warpPoint;
    private List<String> npcData;
    private List<Point> mobData;
    private List<Point> itemMap;
    private List<Point> itemMapIcon; // dari kolom item_map_icon: id part_char tipe 111, dikirim per-player saat join map
    private TileData tileData;
    private List<Point> npc;
    private int bgType;
    private int bgHeight;

    public synchronized void updateFrom(MapData src) {
        if (src == null) return;

        this.name = src.name;
        this.type = src.type;
        this.isCity = src.isCity;
        this.showHs = src.showHs;
        this.miniMap = src.miniMap;
        this.maxZone = src.maxZone;
        this.maxPlayer = src.maxPlayer;
        this.bgType = src.bgType;
        this.bgHeight = src.bgHeight;
        this.warpPoint = src.warpPoint;
        this.npcData = src.npcData;
        this.mobData = src.mobData;
        this.itemMap = src.itemMap;
        this.itemMapIcon = src.itemMapIcon;
        this.npc = src.npc;
        this.tileData = src.tileData;
    }

    public byte[] toByteArray() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {

            dos.writeShort(miniMap);
            dos.writeUTF(name);

            byte[] tiles = tileData.toBytes();
            dos.writeShort(tiles.length);
            if (tiles.length > 0) {
                dos.write(tiles);
            }

            // Background
            dos.writeByte(bgType);
            if (bgType >= 0) {
                dos.writeShort(bgHeight);
            }

            // Item Map
            byte[] itemMapData = Point.toBytes(itemMap);
            dos.writeShort(itemMapData.length);
            if (itemMapData.length > 0) {
                dos.write(itemMapData);
            }

            // Warp Point
            dos.writeByte(warpPoint.size());
            for (Vgo point : warpPoint) {
                dos.writeShort(point.getX());
                dos.writeShort(point.getY());
                dos.writeUTF(point.getName());
            }

            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Unhandled Exception", e);
        }

        return null;
    }

    public int getWidth() {
        return tileData != null ? tileData.getWidth() : 0;
    }

    public int getHeight() {
        return tileData != null ? tileData.getHeight() : 0;
    }

    @Override
    public String toString() {
        return name;
    }

}