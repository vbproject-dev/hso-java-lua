package menu;

import client.Player;
import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.function.BiConsumer;

@Builder
@Getter
public class InputDialog {

    private int npcId;
    private String title;
    private List<String> fields;
    private BiConsumer<Player, String[]> action;
}
