package game.ai;

import java.util.HashMap;
import java.util.Map;

public class AiManager {
    private Map<Integer, PlayerBot> bots = new HashMap<>();
    private AiManager(){}

    private static final class Holder {
        private static final AiManager INSTANCE = new AiManager();
    }
    public static AiManager getInstance() {
        return Holder.INSTANCE;
    }

    public void addBot(PlayerBot bot) {
        bots.put(bot.objectId, bot);
    }

    public PlayerBot get(int id) { return bots.get(id);}


}
