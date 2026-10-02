package game.mount;

import lombok.Getter;

@Getter
public class Mount {
    public short part;
    public byte type;

    public Mount(short part, byte type) {
        this.part = part;
        this.type = type;
    }

}
