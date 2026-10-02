package game.quest;

import lombok.Data;

@Data
public class Progress {
    private int targetId;
    private int current;
    private int total;
    public boolean isComplete() {return current >= total;}

}
