package game.items.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class DisabledItem {
    private int id;
    private int category;
    private String reason;


    public boolean match(int id, int category) {
        return this.id == id && this.category == category;
    }

}
