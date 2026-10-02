package game.items.compensation;


import lombok.Getter;
import lombok.Setter;
import template.Item3;

import java.util.List;

@Setter
@Getter
public class RoleItems {
    private List<Item3> physical;
    private List<Item3> elemental;
}
