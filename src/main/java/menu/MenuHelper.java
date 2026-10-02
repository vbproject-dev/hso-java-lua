package menu;


import client.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class MenuHelper {
    private final int npc;
    private final List<Menu> menus = new ArrayList<>();
    private int nextId = 1;

    private MenuHelper(int npc) {
        this.npc = npc;
    }

    public static MenuHelper forNpc(int npc) {
        return new MenuHelper(npc);
    }

    public MenuHelper menu(String title, String name) {
        menus.add(Menu.builder()
                .npc(npc)
                .id(nextId++)
                .title(title)
                .name(name)
                .build());
        return this;
    }

    public MenuHelper menu(String title, String name, MenuAction action) {
        menus.add(Menu.builder()
                .npc(npc)
                .id(nextId++)
                .title(title)
                .name(name)
                .action(action)
                .build());
        return this;
    }

    public MenuHelper menu(String name, MenuAction action) {
        menus.add(Menu.builder()
                .npc(npc)
                .id(nextId++)
                .title("")
                .name(name)
                .action(action)
                .build());
        return this;
    }

    public MenuHelper menu(String name,
                           Map<String, Object> args,
                           BiConsumer<Player, Map<String, Object>> action) {

        menus.add(Menu.builder()
                .npc(npc)
                .id(nextId++)
                .title("")
                .name(name)
                .args(args)
                .actionArgs(action)
                .build());
        return this;
    }



    public MenuHelper submenu(String name, List<Menu> submenus) {
        Menu menu = Menu.builder()
                .npc(npc)
                .id(nextId++)
                .title("")
                .name(name)
                .menus(new ArrayList<>(submenus))
                .build();
        menus.add(menu);
        return this;
    }


    public List<Menu> build() {
        return new ArrayList<>(menus);
    }

    // Nested builder for submenus
    public static class SubMenuBuilder {
        private final int npc;
        private final List<Menu> submenus = new ArrayList<>();
        private int nextId = 1;

        private SubMenuBuilder(int npc) {
            this.npc = npc;
        }

        public static SubMenuBuilder create(int npc) {
            return new SubMenuBuilder(npc);
        }

        public SubMenuBuilder item(String title, String name, MenuAction action) {
            submenus.add(Menu.builder()
                    .npc(npc)
                    .id(nextId++)
                    .title(title)
                    .name(name)
                    .action(action)
                    .build());
            return this;
        }

        public SubMenuBuilder item(String title, String name) {
            submenus.add(Menu.builder()
                    .npc(npc)
                    .id(nextId++)
                    .title(title)
                    .name(name)
                    .build());
            return this;
        }

        public SubMenuBuilder item(String name, MenuAction action) {
            submenus.add(Menu.builder()
                    .npc(npc)
                    .id(nextId++)
                    .title("")
                    .name(name)
                    .action(action)
                    .build());
            return this;
        }

        public SubMenuBuilder item(String name,
                                   Map<String, Object> args,
                                   BiConsumer<Player, Map<String, Object>> action) {

            submenus.add(Menu.builder()
                    .npc(npc)
                    .id(nextId++)
                    .title("")
                    .name(name)
                    .args(args)
                    .actionArgs(action)
                    .build());
            return this;
        }

        public SubMenuBuilder submenu(String name, List<Menu> nestedMenus) {
            submenus.add(Menu.builder()
                    .npc(npc)
                    .id(nextId++)
                    .title("")
                    .name(name)
                    .menus(new ArrayList<>(nestedMenus))
                    .build());
            return this;
        }


        public List<Menu> build() {
            return new ArrayList<>(submenus);
        }
    }
}