package game.event.spin.models;

public enum BetType {
    GOLD,
    DIAMOND;

    public static BetType fromName(String name) {
        if (name == null) return null;
        return switch (name.toUpperCase()) {
            case "GOLD" -> GOLD;
            case "DIAMOND" -> DIAMOND;
            default -> null;
        };
    }
}
