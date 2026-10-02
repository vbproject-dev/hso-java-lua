package game.event;

import core.Manager;
import lombok.Getter;

import java.time.*;
import java.util.*;

public abstract class GameEvent {
    @Getter
    private final String name;
    private final Set<DayOfWeek> days;
    private final List<TimeEvent> times;
    @Getter
    private boolean active = false;

    /**
     * TRUE selama event ini sedang di-paksa aktif oleh GM (di luar jadwal
     * days/times normal). Selama flag ini TRUE, update() tidak lagi
     * mengecek jadwal otomatis (shouldBeActive) supaya event GM-triggered
     * tidak tiba-tiba di-onEnd() oleh scheduler di tick berikutnya.
     */
    private boolean manualOverride = false;

    public GameEvent(String name, Set<DayOfWeek> days, List<TimeEvent> times) {
        this.name = name;
        this.days = days;
        this.times = times;
    }

    // Override these in subclasses
    protected abstract void onStart();

    protected abstract void onEnd();

    protected abstract void onUpdate();

    public boolean shouldBeRemoved() {
        return false; // default: never remove
    }

    /**
     * FIX (hso.conf toggle, lapisan kedua): override ini di tiap subclass
     * event supaya terhubung ke flag on/off di hso.conf, contoh:
     *   protected boolean isEnabled() { return Manager.gI().event_btf; }
     * GameEventManager.createEvent() sudah mencegah event dibuat kalau
     * flag-nya false SAAT SERVER START, tapi kalau admin reload config
     * (case 22 di menu) sambil server jalan, event yang sudah ter-registrasi
     * tidak otomatis berhenti tanpa restart — pengecekan di update() ini
     * yang menutup celah itu. Default true supaya event yang belum
     * di-override tetap jalan seperti biasa.
     */
    protected boolean isEnabled() {
        return true;
    }

    public void update() {
        // FIX: kalau toggle di hso.conf dimatikan (lewat reload config saat
        // event sudah terlanjur jalan), paksa berhenti sekarang juga,
        // terlepas dari manualOverride atau jadwal days/times.
        if (!isEnabled()) {
            if (active) {
                active = false;
                manualOverride = false;
                System.out.println(name + " DISABLED via hso.conf, force ENDED at " + LocalTime.now());
                onEnd();
            }
            return;
        }

        // Selama di-paksa aktif manual oleh GM, jadwal days/times normal
        // diabaikan sepenuhnya -> onEnd() otomatis TIDAK akan terpanggil
        // hanya karena kita sedang berada di luar jendela waktu terjadwal.
        if (manualOverride) {
            if (active) {
                onUpdate();
            }
            return;
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        boolean shouldBeActive = days.contains(today.getDayOfWeek())
                && times.stream().anyMatch(window -> window.isInTime(now));

        if (shouldBeActive && !active) {
            active = true;
            System.out.println(name + " STARTED at " + now.toString());
            onStart();
        } else if (!shouldBeActive && active) {
            active = false;
            System.out.println(name + " ENDED at " + now.toString());
            onEnd();
        }

        if (active) {
            onUpdate();
        }
    }

    /**
     * Dipanggil GM untuk mengaktifkan event ini secara manual, kapan pun,
     * di luar jadwal days/times normal. Tidak melakukan apa-apa kalau
     * event sedang aktif (baik lewat jadwal otomatis maupun manual).
     *
     * @return true kalau event berhasil di-start manual sekarang.
     */
    public boolean forceStart() {
        if (active) return false;
        manualOverride = true;
        active = true;
        System.out.println(name + " FORCE STARTED (manual GM) at " + LocalTime.now());
        onStart();
        return true;
    }

    /**
     * Dipanggil GM untuk menghentikan paksa event yang sedang berjalan
     * secara manual (forceStart()). Kalau event ini sedang aktif lewat
     * jadwal otomatis biasa (bukan manual), method ini tidak melakukan
     * apa-apa — gunakan jadwal/GameEvent normal untuk itu.
     *
     * @return true kalau event berhasil dihentikan manual sekarang.
     */
    public boolean forceEnd() {
        if (!manualOverride || !active) return false;
        manualOverride = false;
        active = false;
        System.out.println(name + " FORCE ENDED (manual GM) at " + LocalTime.now());
        onEnd();
        return true;
    }

    /** Apakah event ini sedang aktif karena di-paksa manual oleh GM (bukan jadwal). */
    public boolean isManualOverride() {
        return manualOverride;
    }

    public record TimeEvent(LocalTime start, LocalTime end) {

        public boolean isInTime(LocalTime now) {
            // Handle normal time ranges (start < end)
            if (start.isBefore(end)) {
                return (now.equals(start) || now.isAfter(start))
                        && now.isBefore(end);
            }
            // Handle time ranges that cross midnight (start > end)
            // e.g., 23:00 - 01:00
            else {
                return now.equals(start) || now.isAfter(start) || now.isBefore(end);
            }
        }
    }

    public List<TimeEvent> getTimeEvents() {
        return times;
    }

    protected void broadcast(String message) {
        try {
            Manager.gI().chatKTGprocess(message);
        } catch (Exception ignore) {
            // Consider logging this exception
        }
    }
}