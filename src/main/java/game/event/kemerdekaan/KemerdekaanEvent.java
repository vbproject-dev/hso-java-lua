package game.event.kemerdekaan;

import core.Manager;
import game.event.GameEvent;
import game.items.ExchangeService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * KemerdekaanEvent — Event Hari Kemerdekaan Indonesia
 *
 * Aktif setiap tahun: 10 – 21 Agustus, 24 jam penuh.
 *
 * ── Integrasi Persimpangan Kematian ──────────────────────────────────────────
 * Event ini memiliki flag kristalHancur.
 * Jika kristal di Persimpangan Kematian hancur, flag ini menjadi TRUE
 * dan semua player TIDAK BISA klaim hadiah di NpcPakLurah sampai event berakhir.
 *
 * Flag direset otomatis setiap kali event mulai (awal hari baru / server restart).
 *
 * NPC terkait:
 *   -121  Paskibraka       (tukar item utama)
 *   -122  Pak Lurah        (klaim hadiah lomba — dikunci jika kristal hancur)
 *   -123  Nenek Merdeka    (kisah perjuangan)
 *   -126  Penjaga Persimpangan  (daftar & info wave)
 */
public class KemerdekaanEvent extends GameEvent {

    private static final int START_MONTH = 8;
    private static final int START_DAY   = 10;
    private static final int END_MONTH   = 9;
    private static final int END_DAY     = 30;

    /**
     * TRUE = kristal di Persimpangan Kematian hancur hari ini.
     * Selama flag ini TRUE, NpcPakLurah menolak klaim hadiah.
     * Di-reset otomatis tiap pergantian hari selama periode event (lihat onUpdate()).
     */
    private boolean kristalHancur = false;

    /** Status wave Persimpangan hari ini — untuk info ke player */
    private boolean wavePernahDimenangkan = false;

    /**
     * Tanggal terakhir flag di atas di-reset. Dipakai untuk mendeteksi
     * pergantian hari.
     *
     * CATATAN PENTING: schedule event ini pakai SEMUA hari + TimeEvent(MIN, MAX),
     * artinya di GameEvent.update() kondisi shouldBeActive akan SELALU true
     * 24/7 sepanjang tahun. Akibatnya field `active` di base class cuma
     * berubah false→true SEKALI (saat server pertama kali tick event ini) dan
     * TIDAK PERNAH balik false lagi — jadi onStart()/onEnd() cuma jalan sekali
     * seumur uptime server, BUKAN tiap hari baru / tiap tanggal 10 Agustus.
     * Karena itu, reset harian & broadcast "event dimulai" TIDAK BOLEH
     * ditaruh di onStart(), tapi harus dideteksi manual di onUpdate() (yang
     * jalan tiap tick selama active) dengan mengecek tanggal aktual.
     */
    private LocalDate lastResetDate = null;

    public KemerdekaanEvent() {
        super(
            "Event Kemerdekaan Indonesia",
            Set.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
            ),
            List.of(new TimeEvent(LocalTime.MIN, LocalTime.MAX))
        );
    }

    @Override
    protected boolean isEnabled() {
        return Manager.gI().event_kemerdekaan;
    }

    @Override
    protected void onStart() {
        // Sengaja kosong: karena 'active' base class praktis permanen true
        // (lihat catatan di atas lastResetDate), onStart() cuma kepanggil
        // sekali seumur uptime server dan TIDAK BISA diandalkan untuk reset
        // harian. Semua inisialisasi & reset per-hari ditangani di onUpdate().
    }

    @Override
    protected void onEnd() {
        // Praktis tidak pernah terpanggil otomatis (lihat catatan lastResetDate),
        // tapi tetap disediakan untuk jaga-jaga kalau schedule di atas berubah
        // suatu saat jadi punya jendela waktu yang benar-benar tertutup.
        broadcast(
            "Event Kemerdekaan Indonesia telah berakhir. " +
            "Terima kasih sudah berpartisipasi! Merdeka!"
        );
    }

    @Override
    protected void onUpdate() {
        // Deteksi manual: apakah kita sedang di dalam periode 10-21 Agustus,
        // dan apakah hari ini beda dari hari terakhir kali flag di-reset.
        boolean inPeriod = isActive(START_MONTH, START_DAY, END_MONTH, END_DAY);
        LocalDate today  = LocalDate.now();

        if (inPeriod) {
            if (lastResetDate == null || !lastResetDate.equals(today)) {
                // Hari baru dalam periode event (termasuk hari pertama, 10 Agustus)
                // -> reset flag kunci hadiah & umumkan mulainya event hari ini.
                kristalHancur         = false;
                wavePernahDimenangkan = false;
                lastResetDate         = today;

                ExchangeService.gI().loadDatabase();
                broadcast(
                    "Selamat Hari Kemerdekaan Indonesia! " +
                    "Event spesial 17 Agustus telah dimulai! " +
                    "Temui NPC Paskibraka di desa untuk hadiah spesial!"
                );
            }
        } else {
            // Di luar periode event -> pastikan begitu tanggal 10 Agustus tiba
            // lagi (tahun depan / kalau tanggal server diubah manual), blok
            // di atas langsung dianggap "hari baru" dan reset ulang.
            lastResetDate = null;
        }
    }

    @Override
    public boolean shouldBeRemoved() {
        return !isActive(START_MONTH, START_DAY, END_MONTH, END_DAY);
    }

    private boolean isActive(int sm, int sd, int em, int ed) {
        LocalDate today = LocalDate.now();
        LocalDate start = LocalDate.of(today.getYear(), sm, sd);
        LocalDate end   = LocalDate.of(today.getYear(), em, ed);
        return !today.isBefore(start) && !today.isAfter(end);
    }

    // ── Flag Persimpangan ─────────────────────────────────────────────────────

    /**
     * Dipanggil oleh PersimpanganKematian saat kristal hancur.
     * Mengunci klaim hadiah di NpcPakLurah.
     */
    public void onKristalHancur() {
        this.kristalHancur = true;
        broadcast(
            "[Persimpangan Kematian] Kristal Kehidupan HANCUR! " +
            "Hadiah Pak Lurah dikunci hari ini. " +
            "Lindungi kristal lebih baik besok!"
        );
    }

    /**
     * Dipanggil oleh PersimpanganKematian saat semua wave berhasil dipertahankan.
     */
    public void onKristalSelamat() {
        this.wavePernahDimenangkan = true;
        broadcast(
            "[Persimpangan Kematian] Kristal Kehidupan SELAMAT! " +
            "Semua player bisa klaim hadiah Pak Lurah. Merdeka!"
        );
        // Efek meteor perayaan untuk SEMUA player di SEMUA map server,
        // sebagai penanda visual bahwa meteor kemerdekaan berhasil diselamatkan.
        game.map.MapService.broadcastMeteorCelebrationAllMaps();
    }

    /** Apakah klaim hadiah Pak Lurah saat ini dikunci? */
    public boolean isHadiahDikunci() {
        return kristalHancur;
    }

    /** Apakah wave hari ini sudah pernah dimenangkan? */
    public boolean isWaveSudahMenang() {
        return wavePernahDimenangkan;
    }
}
