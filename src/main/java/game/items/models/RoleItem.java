package game.items.models;


import template.Item3;
import java.util.Collections;
import java.util.List;

public class RoleItem {

    private int role;
    private List<Item3> physical;
    private List<Item3> magical;

    public int getRole() {
        return role;
    }

    public List<Item3> get(RoleType type) {
        return switch (type) {
            case PHYSICAL -> physical == null ? Collections.emptyList() : physical;
            case ELEMENTAL  -> magical == null ? Collections.emptyList() : magical;
        };
    }

    void setRole(int role) { this.role = role; }
    void setPhysical(List<Item3> physical) { this.physical = physical; }
    void setMagical(List<Item3> magical) { this.magical = magical; }
}