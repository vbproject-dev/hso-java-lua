package game.quest;

import client.Player;
import client.io.Message;
import client.io.Session;
import core.Service;
import lombok.extern.slf4j.Slf4j;
import model.quest.QuestData;
import utils.SQLHelper;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class QuestManager {
    public static List<QuestData> questTemplate = new ArrayList<>();

    private QuestManager() {
    }

    private static class Holder {
        private static final QuestManager INSTANCE = new QuestManager();
    }

    public static QuestManager getInstance() {
        return Holder.INSTANCE;
    }

    public QuestData getById(int questId) {
        for (QuestData data : questTemplate) {
            if (data.getId() == questId) return data;
        }
        return null;
    }

    public List<Quest> getAvailableQuest(Player player) {
        Map<Integer, Quest> currentQuest = player.quest.getAllQuest();
        Set<Integer> currentQuestIds = currentQuest.values().stream()
                .map(Quest::getQuestId)
                .collect(Collectors.toSet());

        List<Quest> availableQuest = new ArrayList<>();

        for (QuestData questData : questTemplate) {

            // Skip if player already has this quest
            if (currentQuestIds.contains(questData.getId())) {
                continue;
            }

            Quest quest = new Quest();
            quest.setPlayerId(player.objectId);
            quest.setProgress(questData.getTask().stream().map(task -> {
                Progress progress = new Progress();
                progress.setCurrent(0);
                progress.setTargetId(task.getId());
                progress.setTotal(task.getQuantity());
                return progress;
            }).toList());
            quest.setTemplate(questData);
            availableQuest.add(quest);
        }

        return availableQuest;
    }


    public void sendQuest(Player player) {
        sendInfoQuest(player, (byte) 0, getAvailableQuest(player).stream().filter(quest -> quest.getStatus() != QuestStatus.CLAIMED).toList());
        sendInfoQuest(player, (byte) 2, player.quest.getOngoingQuest());
        sendInfoQuest(player, (byte) 1, player.quest.getFinishedQuest());
    }

    private void sendInfoQuest(Player player, byte type, List<Quest> quests) {
        try {

            Message m = new Message(52);

            m.writer().writeByte(type);
            m.writer().writeByte(quests.size());
            System.out.println("Send QuestList Size " + quests.size() + " Type= " + type);
            for (Quest q : quests) {
                QuestData temp = q.getTemplate();
                m.writer().writeShort(temp.getId());
                m.writer().writeBoolean(temp.isMainQuest());
                m.writer().writeUTF(temp.getName());

                switch (type) {

                    // LIST OF AVAILABLE QUESTS
                    case 0 -> {
                        m.writer().writeByte(temp.getNpcId());
                        m.writer().writeUTF(temp.getDetailTalk());
                        m.writer().writeByte(0);
                        m.writer().writeUTF(temp.getDetailHelp());
                    }

                    // FINISHED QUESTS
                    case 1 -> {
                        m.writer().writeByte(temp.getNpcId());
                        m.writer().writeUTF(temp.getDetailTalk());
                        m.writer().writeUTF(temp.getDetailHelp());
                    }

                    // DOING QUESTS (main/sub merged)
                    case 2 -> {
                        m.writer().writeByte(temp.getType());
                        m.writer().writeUTF(temp.getDetailHelp());
                        m.writer().writeUTF(temp.getDetailTalk());
                        m.writer().writeByte(temp.getNpcId());

                        m.writer().writeByte(q.getProgress().size());
                        for (Progress progress : q.getProgress()) {
                            m.writer().writeShort(progress.getTargetId());
                            m.writer().writeShort(progress.getCurrent());
                            m.writer().writeShort(progress.getTotal());
                        }

                    }

                }
            }

            player.conn.addmsg(m);

        } catch (Exception ignored) {

        }
    }


    public void onReceived(Session s, Message m) throws IOException {
        short questId = m.reader().readShort();
        byte type = m.reader().readByte();
        boolean isMain = (m.reader().readByte() == 0);

        log.debug("QuestRequest: type= {} id={} isMain={}", type, questId, isMain);
        switch (type) {
            case 0 -> {
                QuestData q = getById(questId);
                if (q != null) {
                    s.p.quest.addQuest(Quest.fromTemplate(s.p.objectId, q));
                    sendQuest(s.p);
                }
            }
            case 1 -> {
                // Finish
                Quest qFinish = s.p.quest.getQuest(questId);
                if (qFinish != null) {
                    qFinish.setStatus(QuestStatus.CLAIMED);
                    sendQuest(s.p);

                    if (qFinish.getReward() != null) {
                        Service.sendReward(s.p, "Quest Rewards", qFinish.getReward(), 1);
                    }
                }
            }
            case 2 -> {
                // Cancel
                Quest quest = s.p.quest.getQuest(questId);
                if (quest != null) {
                    s.p.quest.remove(questId);
                    sendQuest(s.p);
                }
            }
        }


    }

    public void loadQuestTemplate() {
        questTemplate.clear();
        questTemplate = SQLHelper.selectFrom("quest").getAsModel(QuestData.class);
        System.out.println("[QuestManager] Loaded " + questTemplate.size() + " quest templates from DB.");
    }

    /**
     * Hot-reload quest template tanpa restart server.
     * Panggil lewat admin command setelah edit tabel `quest` di database.
     */
    public void reloadQuestTemplate() {
        int before = questTemplate.size();
        loadQuestTemplate();
        System.out.println("[QuestManager] Hot-reload selesai: " + before + " → " + questTemplate.size() + " quest.");
    }

    public List<Quest> loadPlayerQuest(int playerId) {
        try {
            List<Quest> quests = SQLHelper.selectFrom("player_quest")
                    .where("player_id", playerId)
                    .getAsModel(Quest.class);

            QuestManager questManager = QuestManager.getInstance();

            Iterator<Quest> iterator = quests.iterator();

            while (iterator.hasNext()) {
                Quest quest = iterator.next();

                QuestData template = questManager.getById(quest.getQuestId());

                // Remove broken quests instead of crashing server
                if (template == null) {
                    iterator.remove();
                    continue;
                }

                quest.setTemplate(template);
                quest.setReward(template.getReward());
            }

            return quests;

        } catch (Exception e) {
            System.err.println("Failed to load quests for player " + playerId);
            log.error("Load Player QUEST ", e);
            return Collections.emptyList();
        }
    }


}
