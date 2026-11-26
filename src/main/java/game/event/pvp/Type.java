package game.event.pvp;

import lombok.Getter;

public enum Type {
    BLUE(964, 1896, (byte)1),
    RED(964, 60, (byte)2);

    @Getter
    private final int spawnX;
    @Getter
    private final int spawnY;

    @Getter
    private final byte flag;

    Type(int spawnX, int spawnY, byte flag) {
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.flag = flag;
    }


}