package menu;


import client.Player;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Builder
@Getter
public class Dialog {
    private byte id;
    private String text;
    private Map<String, Object> args;
    private TriConsumer<Player, Boolean, Map<String, Object>> onRespond;

    public void perform(Player player, boolean yesOrNo, Map<String, Object> args) {
        if (onRespond != null) {
            onRespond.accept(player, yesOrNo, args);
        }
    }
}