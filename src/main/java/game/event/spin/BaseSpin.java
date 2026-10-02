package game.event.spin;

import client.Player;
import core.SQL;
import core.Util;
import game.event.GameEvent;
import game.event.spin.models.PlayerBet;
import game.event.spin.models.BetType;
import game.event.spin.models.SpinType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

public abstract class BaseSpin extends GameEvent {

    protected final Map<Integer, PlayerBet> participants = new HashMap<>();
    protected final Random random = new Random();
    protected long carryOverPool = 0;
    private long lastBroadcastTime = 0;
    private TimeEvent currentActiveTime;
    private String lastWinner = "Belum ada";


    // Configurable settings
    private static final int MIN_PARTICIPANTS = 10; // Require at least 2 players
    private static final double HOUSE_EDGE = 0.05; // 10% house edge
    private static final double WINNER_SHARE = 0.50; // Winner gets 50% of total pool
    private static final double WINNER_TAX = 0; // 5% tax on winner's reward

    public BaseSpin(String eventName, List<TimeEvent> times) {
        super(eventName, EnumSet.allOf(DayOfWeek.class), times);
    }

    protected abstract BetType getBetType();
    protected abstract boolean hasEnoughCurrency(Player player, long amount);
    protected abstract void deductCurrency(Player player, long amount);
    protected abstract void rewardCurrency(String playerName, long amount);
    protected abstract long getMinBet();
    protected abstract long getMaxBet();
    protected abstract SpinType getType();

    @Override
    protected void onStart() {
        TimeEvent newActiveTime = getCurrentTimeEvent();

        if (currentActiveTime == null || !currentActiveTime.equals(newActiveTime)) {
            currentActiveTime = newActiveTime;

            participants.clear();
            loadState();
            clearDatabase();

            broadcast(getName() + " telah dimulai! (" +
                    currentActiveTime.start() + " - " + currentActiveTime.end() +
                    ") Minimal " + MIN_PARTICIPANTS + " peserta diperlukan!");
        }
    }

    @Override
    protected void onEnd() {


        if (participants.size() < MIN_PARTICIPANTS) {
            broadcast(getName() + " dibatalkan! peserta tidak mencukupi (" +
                    participants.size() + "/" + MIN_PARTICIPANTS + "). Taruhan dikembalikan.");
            refundAllParticipants();
            participants.clear();
            clearDatabase();

            currentActiveTime = null;
            return;
        }

        performSpin();

    }

    private void performSpin() {
        // Calculate current round total bets
        long currentRoundTotal = participants.values().stream()
                .mapToLong(PlayerBet::getBetAmount)
                .sum();

        // Total pool = carryover + current round (BEFORE any deductions)
        long totalPool = carryOverPool + currentRoundTotal;

        // Calculate house edge (10% of TOTAL POOL)
        long houseAmount = (long) (totalPool * HOUSE_EDGE);

        // Remaining after house edge
        long poolAfterHouse = totalPool - houseAmount;

        // Winner gets 50% of pool after house edge
        long grossReward = (long) (poolAfterHouse * WINNER_SHARE);

        // Apply 5% tax on winner's reward
        long tax = (long) (grossReward * WINNER_TAX);
        long netReward = grossReward - tax;

        // Remaining 50% goes to next jackpot (after house edge, before tax)
        carryOverPool = poolAfterHouse - grossReward;

        PlayerBet winner = getRandomWinner();
        if (winner == null) {
            broadcast(getName() + " gagal menentukan pemenang!");
            return;
        }

        rewardCurrency(winner.getPlayerName(), netReward);
        lastWinner = winner.getPlayerName();

        // Calculate profit/loss for winner (after tax)
        long winnerBet = winner.getBetAmount();
        long profit = netReward - winnerBet;
        String profitText = profit >= 0
                ? "+" + Util.number_format(profit)
                : Util.number_format(profit);

        broadcast(getName() + " selesai! Pemenang: " + winner.getPlayerName() +
                " | Total Pool: " + Util.number_format(totalPool) +
                " | House (10%): " + Util.number_format(houseAmount) +
                " | Hadiah Kotor (50%): " + Util.number_format(grossReward) +
                " | Pajak (5%): " + Util.number_format(tax) +
                " | Hadiah Bersih: " + Util.number_format(netReward) +
                " | Profit: " + profitText +
                " | Jackpot Berikutnya (50%): " + Util.number_format(carryOverPool));

        participants.clear();
        saveState();
        clearDatabase();
        currentActiveTime = null;
    }

    @Override
    protected void onUpdate() {
        int duration = getType() == SpinType.EPIC_GEM_SPIN || getType() == SpinType.EPIC_GOLD_SPIN ? 60_000 * 30 : 60_000 * 2;
        long now = System.currentTimeMillis();
        if (now - lastBroadcastTime >= duration) { // every 30 minutes
            lastBroadcastTime = now;

            long currentPool = carryOverPool + participants.values().stream()
                    .mapToLong(PlayerBet::getBetAmount)
                    .sum();

            broadcast(getName() + " sedang berlangsung!" +
                    " Peserta saat ini: " + participants.size() +
                    " Total Pool: " + Util.number_format(currentPool));
        }
    }

    public boolean join(Player player, long amount) {
        if (!isActive()) {
            player.sendNoticeBox(getName() + " belum dimulai atau sudah berakhir.");
            return false;
        }

        if (participants.containsKey(player.objectId)) {
            player.sendNoticeBox("Kamu sudah bergabung di taruhan ini!");
            return false;
        }

        if (amount < getMinBet() || amount > getMaxBet()) {
            player.sendNoticeBox("Taruhan harus antara " +
                    Util.number_format(getMinBet()) + " - " +
                    Util.number_format(getMaxBet()) + " " +
                    getBetType().name().toLowerCase() + "!");
            return false;
        }

        if (!hasEnoughCurrency(player, amount)) {
            player.sendNoticeBox("Uangmu tidak cukup untuk taruhan " + getName());
            return false;
        }

        deductCurrency(player, amount);
        PlayerBet bet = new PlayerBet(player.objectId, player.name, amount, getBetType());
        participants.put(player.objectId, bet);
        saveToDatabase(player, amount);

        long totalBet = participants.values().stream()
                .mapToLong(PlayerBet::getBetAmount)
                .sum();

        double chance = totalBet > 0 ? (amount * 100.0) / totalBet : 0.0;

        broadcast(getName() + " " + player.name + " bergabung dengan taruhan " +
                Util.number_format(amount) + " " + getBetType().name() +
                " (Peluang: " + String.format("%.2f", chance) + "%)");

        return true;
    }

    private void refundAllParticipants() {
        for (PlayerBet bet : participants.values()) {
            rewardCurrency(bet.getPlayerName(), bet.getBetAmount());
        }
    }

    private PlayerBet getRandomWinner() {
        if (participants.isEmpty()) return null;

        long totalBet = participants.values().stream()
                .mapToLong(PlayerBet::getBetAmount)
                .sum();

        if (totalBet <= 0) {
            List<PlayerBet> list = new ArrayList<>(participants.values());
            return list.get(random.nextInt(list.size()));
        }

        // Weighted random selection based on bet amount
        long rand = (long) (random.nextDouble() * totalBet);
        long cumulative = 0L;

        for (PlayerBet bet : participants.values()) {
            cumulative += bet.getBetAmount();
            if (rand < cumulative) {
                return bet;
            }
        }

        return participants.values().stream()
                .reduce((first, second) -> second)
                .orElse(null);
    }

    public void getChance(Player player) {
        if (!isActive()) {
            player.sendNoticeBox("Event tidak sedang berlangsung.");
            return;
        }

        PlayerBet bet = participants.get(player.objectId);
        if (bet == null) {
            player.sendNoticeBox("Kamu belum bergabung di taruhan ini.");
            return;
        }

        long totalBet = participants.values().stream()
                .mapToLong(PlayerBet::getBetAmount)
                .sum();

        double chance = totalBet > 0 ? (bet.getBetAmount() * 100.0) / totalBet : 0.0;

        long totalPool = carryOverPool + totalBet;
        long houseAmount = (long) (totalPool * HOUSE_EDGE);
        long poolAfterHouse = totalPool - houseAmount;
        long grossPotentialWin = (long) (poolAfterHouse * WINNER_SHARE);
        long tax = (long) (grossPotentialWin * WINNER_TAX);
        long netPotentialWin = grossPotentialWin - tax;
        long potentialProfit = netPotentialWin - bet.getBetAmount();

        player.sendNoticeBox("=== Info Taruhan ===\n" +
                "Peluang menang: " + String.format("%.2f", chance) + "%\n" +
                "Taruhan kamu: " + Util.number_format(bet.getBetAmount()) + "\n" +
                "Total pool: " + Util.number_format(totalPool) + "\n" +
                "House edge (10%): " + Util.number_format(houseAmount) + "\n" +
                "Pool setelah house: " + Util.number_format(poolAfterHouse) + "\n" +
                "Potensi hadiah (kotor): " + Util.number_format(grossPotentialWin) + "\n" +
                "Pajak (5%): " + Util.number_format(tax) + "\n" +
                "Potensi hadiah (bersih): " + Util.number_format(netPotentialWin) + "\n" +
                "Potensi profit: " + Util.number_format(potentialProfit));
    }

    private TimeEvent getCurrentTimeEvent() {
        LocalTime now = LocalTime.now();
        return getTimeEvents().stream()
                .filter(t -> t.isInTime(now))
                .findFirst()
                .orElse(null);
    }

    public String getInformation() {
        long currentRoundTotal = participants.values().stream()
                .mapToLong(PlayerBet::getBetAmount)
                .sum();

        long totalPool = carryOverPool + currentRoundTotal;
        long houseAmount = (long) (totalPool * HOUSE_EDGE);
        long poolAfterHouse = totalPool - houseAmount;
        long grossPotentialWin = (long) (poolAfterHouse * WINNER_SHARE);
        long tax = (long) (grossPotentialWin * WINNER_TAX);
        long netPotentialWin = grossPotentialWin - tax;
        long nextJackpot = poolAfterHouse - grossPotentialWin;

        String time = currentActiveTime != null
                ? currentActiveTime.start() + " - " + currentActiveTime.end()
                : "Tidak aktif";

        String status = isActive() ? "AKTIF" : "TIDAK AKTIF";

        return """
        %s
        Status: %s
        Waktu Event: %s
        Total Peserta: %d
        Minimal Peserta: %d
        Taruhan Saat Ini: %s
        Jackpot Pool: %s
        Total Pool: %s
        House Edge (10%%): %s
        Pool Setelah House: %s
        Potensi Hadiah Kotor (50%%): %s
        Pajak Pemenang (5%%): %s
        Potensi Hadiah Bersih: %s
        Jackpot Berikutnya (50%%): %s
        Pemenang Terakhir: %s
        Min Bet: %s
        Max Bet: %s
        """.formatted(
                getName(),
                status,
                time,
                participants.size(),
                MIN_PARTICIPANTS,
                Util.number_format(currentRoundTotal),
                Util.number_format(carryOverPool),
                Util.number_format(totalPool),
                Util.number_format(houseAmount),
                Util.number_format(poolAfterHouse),
                Util.number_format(grossPotentialWin),
                Util.number_format(tax),
                Util.number_format(netPotentialWin),
                Util.number_format(nextJackpot),
                lastWinner,
                Util.number_format(getMinBet()),
                Util.number_format(getMaxBet())
        );
    }

    private void saveToDatabase(Player player, long amount) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = SQL.gI().getConnection();
            conn.setAutoCommit(false);

            ps = conn.prepareStatement(
                    "INSERT INTO gamble_participants (player_id, player_name, bet_amount, bet_type, spin_type) " +
                            "VALUES (?, ?, ?, ?, ?)");

            ps.setInt(1, player.objectId);
            ps.setString(2, player.name);
            ps.setLong(3, amount);
            ps.setString(4, getBetType().name());
            ps.setInt(5, getType().getId());
            ps.executeUpdate();
            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            player.sendNoticeBox("Terjadi kesalahan saat menyimpan taruhan!");
        } finally {
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void clearDatabase() {
        String sql = "DELETE FROM gamble_participants WHERE spin_type = ?";

        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            ps.setInt(1, getType().getId());
            ps.executeUpdate();
            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
            try (Connection conn = SQL.gI().getConnection()) {
                if (conn != null) conn.rollback();
            } catch (SQLException ignored) {}
        }
    }

    private void saveState() {
        String sql = """
        INSERT INTO gamble_state (spin_type, bet_type, carry_over, last_winner)
        VALUES (?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            bet_type = VALUES(bet_type),
            carry_over = VALUES(carry_over),
            last_winner = VALUES(last_winner)
        """;

        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            ps.setInt(1, getType().getId());
            ps.setString(2, getBetType().name());
            ps.setLong(3, carryOverPool);
            ps.setString(4, lastWinner);
            ps.executeUpdate();
            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
            try (Connection conn = SQL.gI().getConnection()) {
                if (conn != null) conn.rollback();
            } catch (SQLException ignored) {}
        }
    }

    private void loadState() {
        String sql = "SELECT carry_over, last_winner FROM gamble_state WHERE spin_type = ?";

        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, getType().getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    carryOverPool = rs.getLong("carry_over");
                    String winner = rs.getString("last_winner");
                    lastWinner = winner != null ? winner : "Belum ada";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void saveWinner(String playerName, long amount) {
        String query = "INSERT INTO gamble_winner (name, spin_type, bet_type, amount, status) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            conn.setAutoCommit(false);
            ps.setString(1, playerName);
            ps.setInt(2, getType().getId());
            ps.setString(3, getBetType().name());
            ps.setLong(4, amount);
            ps.setString(5, "PENDING");
            ps.executeUpdate();
            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}