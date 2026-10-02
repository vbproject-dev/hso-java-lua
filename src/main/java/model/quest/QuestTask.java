package model.quest;

import lombok.Data;

@Data
public class QuestTask {
    private int id;
    private int quantity;

    public QuestTask(int id, int quantity) {
        this.id = id;
        this.quantity = quantity;
    }
}
