package game.quest;

import client.Player;
import core.ModelMapper;
import lombok.Data;
import utils.SQLHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map;

@Data
public class PlayerQuest {

    private final Map<Integer, Quest> allQuest = new HashMap<>();

    public void addQuest(Quest quest) {
        if (quest == null) return;

        int questId = quest.getQuestId();

        if (allQuest.containsKey(questId)) {
            return;
        }

        allQuest.put(questId, quest);
    }

    public void updateProgress(Player player, int mobId) {
        for (Quest quest : getOngoingQuest()) {
            quest.updateProgress(player, mobId);
        }
    }

    public Quest getQuest(int questId) {
        return allQuest.get(questId);
    }
    public void remove(int questId) {
      allQuest.remove(questId);
    }

    public List<Quest> getFinishedQuest() {
        return allQuest.values().stream()
                .filter(Quest::isComplete)
                .filter(quest -> quest.getStatus() != QuestStatus.CLAIMED)
                .toList();
    }
    public List<Quest> getOngoingQuest() {
        return allQuest.values().stream()
                .filter(quest -> quest.getStatus() == QuestStatus.ONGOING)
                .filter(quest -> !quest.isComplete())
                .toList();
    }

    public void save(int playerId) {
        if (allQuest.isEmpty()) return;
        boolean wasInterrupted = Thread.interrupted(); // prevent HikariCP InterruptedException on shutdown
        allQuest.forEach((id, quest) -> {
            try {
                Map<String, Object> data = ModelMapper.toMap(quest);
                if (data.isEmpty()) {
                    System.err.println("[PlayerQuest] toMap kosong untuk quest_id=" + quest.getQuestId() + " player_id=" + playerId);
                    return;
                }
                boolean exists = SQLHelper.selectFrom("player_quest")
                        .where("player_id", playerId)
                        .and("quest_id", quest.getQuestId())
                        .exists();
                if (exists) {
                    SQLHelper.update("player_quest")
                            .where("player_id", playerId)
                            .and("quest_id", quest.getQuestId())
                            .set(data).execute();
                } else {
                    SQLHelper.insert("player_quest")
                            .values(data).execute();
                }
            } catch (Exception e) {
                System.err.println("[PlayerQuest] Gagal save quest_id=" + quest.getQuestId()
                        + " player_id=" + playerId + " : " + e.getMessage());
            }
        });
        if (wasInterrupted) Thread.currentThread().interrupt();
    }

}
