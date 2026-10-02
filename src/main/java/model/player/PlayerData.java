package model.player;


import lombok.Data;

import java.util.List;

@Data
public class PlayerData {
    private int id;
    private int uid;
    private String name;
    private boolean active;
    private byte[] body;
    private int level;
    private int clazz;
    private long exp;
    private List<PlayerWear> itemwear;

    public short point1;
    public short point2;
    public short point3;
    public short point4;
    public byte[] skill;
    private int dicuop;
    private int pointarena;
    private long goldRampok;
    private long goldDagang;

}
