package game.items.box;

import lombok.Getter;

@Getter
public enum BoxType {

    RANDOM_SINGLE(0),
    RANDOM_MULTIPLE(1),
    COMPENSATION(2),
    UNKNOW(-1);

    private final int id;

    BoxType(int id) {
        this.id = id;
    }

    public static BoxType fromId(int id) {
        for (BoxType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return UNKNOW;
    }

}
