package model.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import template.Option;

import java.util.List;
@Data
@AllArgsConstructor
public class PlayerWear {
    private short id;
    private byte role;
    private byte type;
    private short level;
    private short icon;
    private byte color;
    private byte part;
    private byte tier;
    private List<Option> option;
    private byte index;
    private byte tierStar;
    private long expireDate;

}
