package game.event.halloween;

import core.Manager;
import game.event.GameEvent;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Kerangka event Halloween. Belum ada logic apa pun di dalamnya —
 * isi onStart()/onEnd()/onUpdate() sesuai kebutuhan (buka toko, spawn NPC,
 * ubah drop rate, dll). Jadwal default: aktif tiap hari, 25 Okt - 1 Nov.
 */
public class HalloweenEvent extends GameEvent {


    // Periode musim Halloween "Malam Suro". SATU-SATUNYA sumber tanggal event
    // ini — dipakai langsung oleh shouldBeRemoved() di bawah. Ganti di sini
    // kalau mau ubah tanggal event, jangan hardcode angka lain di tempat lain.
    private static final int START_MONTH = 9;
    private static final int START_DAY   = 25;
    private static final int END_MONTH   = 11;
    private static final int END_DAY     = 1;
    // ── Flag Gerbang Arwah ───────────────────────────────────────────────────
    // Sama pola seperti kristalHancur/wavePernahDimenangkan di KemerdekaanEvent.
    // Belum dipakai NPC manapun buat kunci/buka hadiah (belum ada NPC "Pak
    // Lurah versi Halloween") — disiapkan duluan supaya GerbangArwah bisa
    // langsung jalan tanpa NullPointer, tinggal dikaitkan ke NPC reward nanti.
    private boolean kristalHancur = false;
    private boolean wavePernahDimenangkan = false;

    public HalloweenEvent() {
        super(
                "Halloween Event",
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
        return Manager.gI().event_halloween;
    }

    @Override
    protected void onStart() {
        // TODO: isi logic saat event mulai (buka toko, aktifkan drop, dll)
    }

    @Override
    protected void onEnd() {
        // TODO: isi logic saat event selesai
    }

    @Override
    protected void onUpdate() {
        // TODO: isi logic yang jalan berulang selama event aktif (opsional)
    }

    @Override
    public boolean shouldBeRemoved() {
        return !isActive(START_MONTH, START_DAY, END_MONTH, END_DAY);
    }

    // ── Hook Gerbang Arwah ───────────────────────────────────────────────────

    /**
     * Dipanggil oleh GerbangArwah saat kristal hancur.
     * TODO: kaitkan ke NPC reward Halloween begitu ada (mengunci klaim,
     * sama seperti KemerdekaanEvent.onKristalHancur() -> NpcPakLurah).
     */
    public void onKristalHancur() {
        this.kristalHancur = true;
        broadcast(
            "[Malam Suro] Api Ritual HANCUR! Gerombolan arwah berhasil menembus pertahanan. " +
            "Lindungi lebih baik lain kali!"
        );
    }

    /**
     * Dipanggil oleh GerbangArwah saat semua wave berhasil dipertahankan.
     * TODO: kaitkan ke NPC reward Halloween begitu ada (membuka klaim).
     */
    public void onKristalSelamat() {
        this.wavePernahDimenangkan = true;
        broadcast(
            "[Malam Suro] Api Ritual SELAMAT! Para pemberani berhasil mengusir gerombolan arwah."
        );
    }

    /** Apakah klaim hadiah gerbang arwah saat ini dikunci? (disiapkan buat NPC reward nanti) */
    public boolean isHadiahDikunci() {
        return kristalHancur;
    }

    /** Apakah wave hari ini sudah pernah dimenangkan? */
    public boolean isWaveSudahMenang() {
        return wavePernahDimenangkan;
    }

    private boolean isActive(int startMonth, int startDay, int endMonth, int endDay) {
        LocalDate today = LocalDate.now();
        LocalDate start = LocalDate.of(today.getYear(), startMonth, startDay);
        LocalDate end = LocalDate.of(today.getYear(), endMonth, endDay);

        // Kalau rentang event melewati pergantian tahun (Des -> Jan)
        if (end.isBefore(start)) {
            if (today.getMonthValue() == 1) {
                start = start.minusYears(1);
            } else {
                end = end.plusYears(1);
            }
        }

        return !today.isBefore(start) && !today.isAfter(end);
    }
}
