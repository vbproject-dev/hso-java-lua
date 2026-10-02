package model;

import lombok.Data;

import java.util.List;


@Data
public class UpgradeData {
    private short[] materials;
    private List<UpgradeLevel> levels;
}
