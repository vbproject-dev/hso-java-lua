package model.map;

import lombok.Data;

@Data
public class EffMapConfig {
    private int id;
    private int mapId;   // 0 = global (semua map), selain itu = mapId spesifik
    private int idnpc;
    private int idEff;
    private int x;
    private int y;
    private int b3;
    private int b4;
    private int b7;
}