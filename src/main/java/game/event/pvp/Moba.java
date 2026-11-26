package game.event.pvp;

import client.Player;
import game.event.GameEvent;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;

public class Moba extends GameEvent {


    public Moba() {
        super("MOBA", EnumSet.allOf(DayOfWeek.class),
                List.of(new TimeEvent(LocalTime.of(1, 0), LocalTime.of(17, 59)))
        );
    }

    public boolean isOpen() {
        return MobaManager.getInstance().isOpen();
    }

    public void register(Player player) {
        if (!MobaManager.getInstance().join(player)) {
            player.sendNoticeBox("Kamu sudah berada dalam antrian");
            return;
        }
    }

    @Override
    protected void onStart() {
        MobaManager.getInstance().setOpen(true);
        broadcast("Moba telah buka");
    }

    @Override
    protected void onEnd() {
        MobaManager.getInstance().setOpen(false);
        broadcast("Moba telah ditutup ");
    }

    @Override
    protected void onUpdate() {
        MobaManager.getInstance().update();
    }


}
