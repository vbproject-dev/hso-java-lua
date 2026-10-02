package model;

import lombok.Data;

@Data
public class ServerSetting {
    private int dropRateEquipment;
    private int dropRatePotion;
    private int dropRateMaterial;
    private int dropRateEvent;
    private int dropRateGold;
}
