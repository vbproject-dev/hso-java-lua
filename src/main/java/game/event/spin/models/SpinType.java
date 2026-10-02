package game.event.spin.models;

public enum SpinType {
    QUICK_GOLD_SPIN(2),
    QUICK_GEM_SPIN(3),
    EPIC_GOLD_SPIN(0),
    EPIC_GEM_SPIN(1);

    private final int id;

    SpinType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static SpinType fromId(int id) {
        return switch (id) {
            case 0 -> EPIC_GOLD_SPIN;
            case 1 -> EPIC_GEM_SPIN;
            case 2 -> QUICK_GOLD_SPIN;
            case 3 -> QUICK_GEM_SPIN;
            default -> null;
        };
    }
}
