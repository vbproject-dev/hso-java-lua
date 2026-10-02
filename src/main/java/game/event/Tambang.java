package game.event;


import game.guild.Guild;
import core.Manager;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;


public class Tambang extends GameEvent {
    public static final String EVENT_NAME = "TAMBANG";

    public long harvestTime;
    public Tambang() {
        super(
                EVENT_NAME,
                EnumSet.allOf(DayOfWeek.class),
                List.of(
                        new TimeEvent( LocalTime.of(9, 0), LocalTime.of(10, 0)),
                        new TimeEvent( LocalTime.of(20, 0), LocalTime.of(21, 0))
                ));


    }

    @Override
    protected boolean isEnabled() {
        return Manager.gI().event_tambang;
    }

    @Override
    protected void onStart() {
        Manager.gI().mine.openAttack();
        try {
            Manager.gI().chatKTGprocess("Waktu perebutan tambang telah tiba");
        } catch (IOException ignore) {}

    }

    @Override
    protected void onEnd() {
        // Tiap langkah punya try/catch sendiri. GameEvent.update() sudah set active=false SEBELUM
        // onEnd() dipanggil, jadi kalau closeAttack() error, harvest() dan reward member tidak boleh
        // ikut terlewat (dan event ini tidak akan diulang).
        try {
            Manager.gI().mine.closeAttack();
        } catch (Exception e) {
            System.out.println("[Tambang] closeAttack gagal: " + e);
            e.printStackTrace();
        }
        try {
            Manager.gI().mine.harvest();
        } catch (Exception e) {
            System.out.println("[Tambang] harvest gagal: " + e);
            e.printStackTrace();
        }

        // FIX: try/catch per guild. GameEvent.update() sudah set active=false SEBELUM onEnd(),
        // jadi kalau satu guild melempar exception, guild lain tidak akan pernah dapat reward
        // dan event tidak akan diulang.
        for (Guild guild : new ArrayList<>(Guild.entrys)) {
            try {
                guild.sendMemberRewards();
            } catch (Exception e) {
                System.out.println("[Tambang] sendMemberRewards gagal untuk guild " + guild.name + ": " + e);
                e.printStackTrace();
            }
        }

        try {
            Manager.gI().chatKTGprocess("Waktu perebutan tambang telah berakhir rewards akan diberikan pada masing2 guild");
        } catch (IOException ignore) {}
    }

    @Override
    protected void onUpdate() {

    }


}