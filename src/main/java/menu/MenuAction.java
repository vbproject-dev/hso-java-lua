package menu;


import client.Player;

import java.io.IOException;

@FunctionalInterface
public interface MenuAction {
    void execute(Player p) throws IOException;
}
