package game.event.pvp;

import lombok.Getter;

@Getter
public enum Lane {
    TOP(
            new int[][] {{676, 1756}, {408, 1496}, {52, 1033}},
            new int[][] {{692, 196}, {436, 436}, {52, 836}}
    ),
    MID(
            new int[][] {{964, 1576}, {964, 1356}, {964, 1187}},
            new int[][] {{964, 361}, {964, 580}, {964, 777}}
    ),
    BOT(
            new int[][] {{1240, 1752}, {1504, 1504}, {1868, 1033}},
            new int[][] {{1244, 196}, {1492, 436}, {1868, 836}}
    ),
    BASE(
            new int[][] {{964, 1804}},
            new int[][] {{964, 129}}
    );


    private final int[][] blueTurrets;
    private final int[][] redTurrets;

    Lane(int[][] blueTurrets, int[][] redTurrets) {
        this.blueTurrets = blueTurrets;
        this.redTurrets = redTurrets;
    }

    public int[][] getTurrets(Type type) {
        return type == Type.BLUE ? blueTurrets : redTurrets;
    }


}