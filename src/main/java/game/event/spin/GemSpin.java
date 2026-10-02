package game.event.spin;

import client.Player;
import core.Manager;
import core.Service;
import game.event.spin.models.BetType;
import game.event.spin.models.SpinType;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;

public class GemSpin extends BaseSpin {

    public GemSpin() {
        super("[Epic Gem Spin]", List.of(
                new TimeEvent(LocalTime.of(10, 1), LocalTime.of(22, 0)),
                new TimeEvent(LocalTime.of(22, 1), LocalTime.of(10, 0))
        ));
    }

    @Override
    protected boolean isEnabled() {
        return Manager.gI().event_gem_spin;
    }

    @Override
    protected SpinType getType() {
        return SpinType.EPIC_GEM_SPIN;
    }

    @Override
    public long getMinBet() {
        return 1000L;
    }

    @Override
    public long getMaxBet() {
        return 1000_000L;
    }
    @Override
    protected BetType getBetType() {
        return BetType.DIAMOND;
    }

    @Override
    protected boolean hasEnoughCurrency(Player player, long amount) {
        return player.getGem() >= amount;
    }

    @Override
    protected void deductCurrency(Player player, long amount) {
        player.updateGem(-amount);
    }

    @Override
    protected void rewardCurrency(String playerName, long amount) {
        Player player =  Manager.getPlayerByName(playerName);
        if (player != null) {
            player.updateGem(amount);
            try {
                player.item.charInventory(4);
                Service.Show_open_box_notice_item(player, getName(), new short[]{-2}, new int[]{(int)amount}, new short[]{4});
            } catch (IOException ignore) {}
        }else{
            saveWinner(playerName, amount);
        }
    }

}
