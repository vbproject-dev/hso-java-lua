package game.event.spin;

import client.Player;
import core.Manager;
import core.Service;
import game.event.spin.models.BetType;
import game.event.spin.models.SpinType;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;

public class QuickGoldSpin extends BaseSpin {
    private static final int BETTING_DURATION_MINUTES = 10;
    private static final int WAIT_AFTER_SPIN_SECONDS = 30;  // 10 seconds before reopen
    private long lastPhaseTime = 0;
    private boolean bettingPhase = true;

    public QuickGoldSpin() {
        super("[Quick Gold Spin]",
                List.of(new TimeEvent(LocalTime.MIDNIGHT, LocalTime.MAX))
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        startBettingPhase();
    }

    @Override
    protected void onUpdate() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastPhaseTime;

        if (bettingPhase && elapsed >= BETTING_DURATION_MINUTES * 60_000L) {
            // End betting → spin
            bettingPhase = false;
            lastPhaseTime = now;
            onEnd();
        }
        else if (!bettingPhase && elapsed >= WAIT_AFTER_SPIN_SECONDS * 1000L) {
            // Wait 10s then reopen betting
            startBettingPhase();
        }
    }

    private void startBettingPhase() {
        bettingPhase = true;
        lastPhaseTime = System.currentTimeMillis();
        broadcast(getName() +" "+ getBetType().name() + " telah dimulai! Pasang taruhan kalian sekarang!");
    }

    @Override
    protected SpinType getType() {
        return SpinType.QUICK_GOLD_SPIN;
    }

    @Override
    public long getMinBet() {
        return 10_000_000L;
    }

    @Override
    public long getMaxBet() {
        return 1_000_000_000L;
    }

    @Override
    protected BetType getBetType() {
        return BetType.GOLD;
    }

    @Override
    protected boolean hasEnoughCurrency(Player player, long amount) {
        return player.getGold() >= amount;
    }

    @Override
    protected void deductCurrency(Player player, long amount) {
        player.updateGold(-amount);
    }

    @Override
    protected void rewardCurrency(String playerName, long amount) {
        Player player =  Manager.getPlayerByName(playerName);
        if (player != null) {
            player.updateGold(amount);
            try {
                player.item.charInventory(4);
                Service.Show_open_box_notice_item(player, getName(), new short[]{-1}, new int[]{(int)amount}, new short[]{4});
            } catch (IOException ignore) {}
        }else{
            saveWinner(playerName, amount);
        }
    }
}
