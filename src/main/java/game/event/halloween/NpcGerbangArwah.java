package game.event.halloween;

import client.io.Session;
import game.event.GameEventManager;
import model.map.Vgo;

import java.io.IOException;

public class NpcGerbangArwah {

    public static void onReceivedMenu(Session s, int index) throws IOException {
        HalloweenEvent halloween =
            GameEventManager.gI().getEvent(HalloweenEvent.class);
        GerbangArwah wave =
            GameEventManager.gI().getEvent(GerbangArwah.class);

        // Cek event Halloween aktif
        if (halloween == null || halloween.shouldBeRemoved()) {
            s.p.sendNoticeBox(
                "Gerbang Arwah hanya tersedia selama\n" +
                "Event Halloween \"Malam Suro\" (25 Okt - 25 Nov)."
            );
            return;
        }

        switch (index) {

            // ── Menu 0: Masuk ke arena ────────────────────────────────────────
            case 0 -> {
                if (wave == null || !wave.canEnter()) {
                    String hasil = (wave != null) ? wave.getRingkasanHasil() : null;
                    if (hasil != null) {
                        // Event malam ini sudah selesai (menang/kalah/waktu habis)
                        s.p.sendNoticeBox(
                            "Gerbang Arwah malam ini sudah SELESAI.\n" +
                            "Hasil: " + hasil + "\n\n" +
                            "Dibuka lagi " + wave.getJadwalBerikutnyaText() + "."
                        );
                    } else {
                        s.p.sendNoticeBox(
                            "Gerbang Arwah belum aktif.\n" +
                            "Event dibuka setiap hari pukul " +
                            (wave != null ? wave.getJadwalText() : "20:00 - 21:00") + " WIB\n" +
                            "selama Event Malam Suro berlangsung!"
                        );
                    }
                    return;
                }
                // Teleport langsung ke arena — tidak perlu daftar
                s.p.changeMap(s.p, Vgo.create(
                    s.p,
                    GerbangArwah.MAP_ID,
                    (short)(10 * 24),
                    (short)(10 * 24)
                ));
                Service_notice(s,
                    "[Gerbang Arwah] Kamu telah masuk ke arena!\n" +
                    "Lindungi Api Ritual dari serangan gerombolan Demit Jahat!\n" +
                    "Jika Api Ritual hancur, SEMUA player tidak bisa klaim Jimat Jelangkung sebagai pelindung!"
                );
            }

            // ── Menu default: Info event ──────────────────────────────────────
            default -> {
                StringBuilder info = new StringBuilder();
                info.append("=== GERBANG ARWAH ===\n\n");
                info.append("Bagian dari Event \"Malam Suro\".\n");
                info.append("Jadwal: Setiap hari pukul ")
                    .append(wave != null ? wave.getJadwalText() : "20:00 - 21:00")
                    .append(" WIB\n\n");

                String hasilHariIni = (wave != null) ? wave.getRingkasanHasil() : null;

                if (hasilHariIni != null && !wave.canEnter()) {
                    // Event sudah selesai (bisa lebih cepat dari jadwal) -> tampilkan status akhir
                    info.append("Status: SELESAI malam ini\n");
                    info.append("Hasil: ").append(hasilHariIni).append("\n");
                    info.append(halloween.isHadiahDikunci() ? "Hadiah: DIKUNCI hari ini\n"
                                : halloween.isWaveSudahMenang() ? "Hadiah: TERBUKA\n" : "");
                    info.append("Dibuka lagi ").append(wave.getJadwalBerikutnyaText()).append(".\n\n");
                } else if (wave == null || !wave.canEnter()) {
                    // Tampilkan status hadiah (TODO: kaitkan ke NPC reward Halloween begitu ada)
                    if (halloween.isHadiahDikunci()) {
                        info.append("Status Api Ritual: HANCUR\n");
                        info.append("Hadiah: DIKUNCI hari ini\n\n");
                    } else if (halloween.isWaveSudahMenang()) {
                        info.append("Status Api Ritual: SELAMAT\n");
                        info.append("Hadiah: TERBUKA\n\n");
                    } else {
                        info.append("Status: Menunggu wave malam ini (20:00)\n\n");
                    }
                } else {
                    switch (wave.getState()) {
                        case COUNTDOWN ->
                            info.append("Status: Bersiap! Wave segera dimulai...\n");
                        case WAVE ->
                            info.append("Status: WAVE ").append(wave.getCurrentWave())
                                .append("/").append(wave.getTotalWaves())
                                .append(" SEDANG BERJALAN!\n");
                        case REST ->
                            info.append("Status: Jeda antar wave...\n");
                        case VICTORY ->
                            info.append("Status: MENANG! Api Ritual selamat!\n");
                        case DEFEAT ->
                            info.append("Status: KALAH. Api Ritual hancur.\n");
                        default ->
                            info.append("Status: Tidak aktif\n");
                    }

                    if (wave.getKristal() != null) {
                        int hp    = wave.getKristal().hp;
                        int maxHp = wave.getKristal().maxHp;
                        int pct   = maxHp > 0 ? (hp * 100 / maxHp) : 0;
                        info.append("HP Api Ritual: ").append(pct).append("%\n");
                    }
                    info.append("\n");
                }

                info.append("Aturan:\n");
                info.append("- Semua player bisa masuk tanpa daftar\n");
                info.append("- Bunuh monster sebelum mencapai api ritual\n");
                info.append("- Jika api ritual hancur, hadiah DIKUNCI\n");
                info.append("- Jika api ritual selamat, semua bisa klaim Jimat Jelangkung!\n\n");
                info.append("Total Wave: ").append(wave != null ? wave.getTotalWaves() : WAVES_TOTAL).append("\n");
                info.append("(Wave terakhir adalah BOSS WAVE!)");

                s.p.sendNoticeBox(info.toString());
            }
        }
    }

    private static final int WAVES_TOTAL = 5; // sama dengan WAVES.size() di GerbangArwah

    private static void Service_notice(Session s, String msg) throws IOException {
        s.p.sendNoticeBox(msg);
    }
}