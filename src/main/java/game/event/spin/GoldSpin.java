package game.event.spin;

import client.Player;
import core.Manager;
import core.Service;
import game.event.spin.models.BetType;
import game.event.spin.models.SpinType;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;

public class GoldSpin extends BaseSpin {

    public GoldSpin() {
        super("[Epic Gold Spin]",

                List.of(
                new TimeEvent(LocalTime.of(10, 1), LocalTime.of(22, 0)),
                new TimeEvent(LocalTime.of(22, 1), LocalTime.of(10, 0))
        ));
    }

    @Override
    protected boolean isEnabled() {
        return Manager.gI().event_gold_spin;
    }

    @Override
    public long getMinBet() {
        return 10_000_000L; //
    }

    @Override
    public long getMaxBet() {
        return 1000_000_000L;
    }

    @Override
    protected SpinType getType() {
        return SpinType.EPIC_GOLD_SPIN;
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
