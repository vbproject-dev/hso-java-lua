package model.quest;

import lombok.Data;
import model.item.Reward;

import java.util.List;

@Data
public class QuestData {
    private int id;
    private String name;
    private boolean isMainQuest;
    private int npcId;
    private String detailTalk;
    private String detailHelp;
    private Reward reward;
    private List<QuestTask> task;
    private byte type;

}
