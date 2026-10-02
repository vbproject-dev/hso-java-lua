package game.event.spin.models;

public class PlayerBet {
    private final int playerId;
    private final String playerName;
    private final long betAmount;
    private final BetType type;

    public PlayerBet(int playerId, String playerName, long betAmount, BetType type) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.betAmount = betAmount;
        this.type = type;
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getBetAmount() {
        return betAmount;
    }

    public BetType getType() {
        return type;
    }
}
