package game.event.natal;

import core.Manager;
import game.event.GameEvent;
import game.items.ExchangeService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public class ChristmasEvent extends GameEvent {

    public ChristmasEvent() {
        super(
                "Christmas Event",
                Set.of(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY,
                        DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY
                ),
                List.of(
                        new TimeEvent(LocalTime.MIN, LocalTime.MAX))
        );
    }


    @Override
    protected boolean isEnabled() {
        return Manager.gI().event_christmas;
    }

    @Override
    protected void onStart() {
        ExchangeService.gI().loadDatabase();
    }

    @Override
    protected void onEnd() {
    }

    @Override
    protected void onUpdate() {

    }

    @Override
    public boolean shouldBeRemoved() {
         return !isActive(2, 2, 3, 1);
    }

    private boolean isActive(int startMonth, int startDay, int endMonth, int endDay) {
        LocalDate today = LocalDate.now();
        LocalDate start = LocalDate.of(today.getYear(), startMonth, startDay);
        LocalDate end = LocalDate.of(today.getYear(), endMonth, endDay);

        // If event crosses new year (Dec → Jan)
        if (end.isBefore(start)) {
            // If we are in Jan → event end is next year
            if (today.getMonthValue() == 1) {
                start = start.minusYears(1);
            }
            // If we are in Dec → event end is next year
            else {
                end = end.plusYears(1);
            }
        }

        return !today.isBefore(start) && !today.isAfter(end);
    }
}
