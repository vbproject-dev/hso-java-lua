package model.map;

import lombok.Data;

@Data
public class NpcChatData {
    private int id;
    private int mapId;
    private int npcId;
    private String chatText;
    private boolean enabled;
}