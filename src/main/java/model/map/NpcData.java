package model.map;

import lombok.Data;

@Data
public class NpcData {
    private int id;
    private String name;
    private String dialogName;
    private String dialogText;
    private int imageId;
    private int bigAvatar;
    private int totalFrame;
    private int wBlock;
    private int hBlock;
    private boolean isPerson;
    private boolean isShowHp;

}
