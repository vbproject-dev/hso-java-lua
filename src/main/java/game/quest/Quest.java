package game.quest;

import client.Player;
import lombok.Data;
import model.item.Reward;
import model.quest.QuestData;
import org.checkerframework.checker.units.qual.C;

import java.util.List;
@Data
public class Quest {
    private int playerId;
    private int questId;
    private List<Progress> progress;
    private QuestStatus status;
    private transient Reward reward;
    private transient QuestData template;

    public boolean isComplete() {
        for (Progress p : progress) {
            if (!p.isComplete()) return false;
        }
        return true;
    }

    public void updateProgress(Player player, int mobId) {

        if (status == QuestStatus.COMPLETED || status == QuestStatus.CLAIMED) {
            return;
        }

        boolean changed = false;
        for (Progress pr : progress) {

            if (pr.getTargetId() != mobId) continue;
            if (pr.isComplete()) continue;

            int newValue = Math.min(pr.getCurrent() + 1, pr.getTotal());
            pr.setCurrent(newValue);
            changed = true;
        }

        if (changed && isComplete()) {
            status = QuestStatus.COMPLETED;
            QuestManager.getInstance().sendQuest(player);
        }
    }

    public static Quest fromTemplate(int playerId, QuestData data) {
        Quest quest = new Quest();
        quest.setTemplate(data);
        quest.setQuestId(data.getId());
        quest.setPlayerId(playerId);
        quest.setProgress(data.getTask().stream().map(task -> {
            Progress progress = new Progress();
            progress.setCurrent(0);
            progress.setTargetId(task.getId());
            progress.setTotal(task.getQuantity());
            return progress;
        }).toList());
        quest.setStatus(QuestStatus.ONGOING);
        quest.setReward(data.getReward());
        return quest;
    }
}
