package game.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Position {
    private short x;
    private short y;


    public int getTileX() {return x * 24;}
    public int getTileY() {return y * 24;}


}
