package utils;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@Builder
public class Menu {

    private int id;
    private String name;
    private int npcId;
    private List<Menu> menus;
    private Runnable action;

    public void addMenu(Menu menu) {
        this.menus.add(menu);
    }

    public boolean isMenu() {
        return this.menus != null && !this.menus.isEmpty();
    }

    public void perform() {
        if (action != null) {
            action.run();
        }
    }
}
