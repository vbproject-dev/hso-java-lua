package model;



import lombok.Data;


@Data
public class UpgradeLevel {
    private int level;
    private int gold;
    private int gem;
    private byte[] value;
}
