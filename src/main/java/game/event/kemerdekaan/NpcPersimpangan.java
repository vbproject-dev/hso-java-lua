package game.event.kemerdekaan;

import client.io.Session;
import game.event.GameEventManager;
import model.map.Vgo;

import java.io.IOException;

/**
 * NpcPersimpangan — NPC info & teleport ke arena Persimpangan Kematian (id: -126)
 *
 * Tidak ada pendaftaran — player langsung masuk ke map event.
 * Siapapun yang ada di map berkontribusi melindungi kristal.
 *
 * ─── Tambahkan di MenuController ────────────────────────────────────────────
 * request_menu():
 *   case -126: {
 *       menu = new String[]{"Masuk ke Arena", "Informasi Event"};
 *       break;
 *   }
 * handleDynamicMenu():
 *   case -126:
 *       NpcPersimpangan.onReceivedMenu(conn, index);
 *       break;
 *
 * Menu:
 *   0 → Langsung teleport ke map event (MAP_ID 120)
 *   default → Info event & status kristal
 */
public class NpcPersimpangan {

    public static void onReceivedMenu(Session s, int index) throws IOException {
        KemerdekaanEvent kemerdekaan =
            GameEventManager.gI().getEvent(KemerdekaanEvent.class);
        PersimpanganKematian wave =
            GameEventManager.gI().getEvent(PersimpanganKematian.class);

        // Cek event kemerdekaan aktif
        if (kemerdekaan == null || kemerdekaan.shouldBeRemoved()) {
            s.p.sendNoticeBox(
                "Persimpangan Kematian hanya tersedia selama\n" +
                "Event Kemerdekaan Indonesia (10 Agustus - 2 September)."
            );
            return;
        }

        switch (index) {

            // ── Menu 0: Masuk ke arena ────────────────────────────────────────
            case 0 -> {
                if (wave == null || !wave.isActive()) {
                    s.p.sendNoticeBox(
                        "Persimpangan Kematian belum aktif.\n" +
                        "Event dibuka setiap hari pukul 20:00 - 21:00\n" +
                        "selama Event Kemerdekaan berlangsung!"
                    );
                    return;
                }
                // Teleport langsung ke arena — tidak perlu daftar
                s.p.changeMap(s.p, Vgo.create(
                    s.p,
                    PersimpanganKematian.MAP_ID,
                    (short)(10 * 24),
                    (short)(10 * 24)
                ));
                Service_notice(s,
                    "[Persimpangan] Kamu telah masuk ke arena!\n" +
                    "Lindungi Kristal Kehidupan dari serangan monster!\n" +
                    "Jika kristal hancur, SEMUA player tidak bisa klaim hadiah Pak Lurah!"
                );
            }

            // ── Menu default: Info event ──────────────────────────────────────
            default -> {
                StringBuilder info = new StringBuilder();
                info.append("=== PERSIMPANGAN KEMATIAN ===\n\n");
                info.append("Bagian dari Event Kemerdekaan Indonesia.\n");
                info.append("Jadwal: Setiap hari pukul 20:00 - 21:00\n\n");

                if (wave == null || !wave.isActive()) {
                    // Tampilkan status hadiah Pak Lurah
                    if (kemerdekaan.isHadiahDikunci()) {
                        info.append("Status Kristal: HANCUR\n");
                        info.append("Hadiah Pak Lurah: DIKUNCI hari ini\n\n");
                    } else if (kemerdekaan.isWaveSudahMenang()) {
                        info.append("Status Kristal: SELAMAT\n");
                        info.append("Hadiah Pak Lurah: TERBUKA\n\n");
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
                            info.append("Status: MENANG! Kristal selamat!\n");
                        case DEFEAT ->
                            info.append("Status: KALAH. Kristal hancur.\n");
                        default ->
                            info.append("Status: Tidak aktif\n");
                    }

                    if (wave.getKristal() != null) {
                        int hp    = wave.getKristal().hp;
                        int maxHp = wave.getKristal().maxHp;
                        int pct   = maxHp > 0 ? (hp * 100 / maxHp) : 0;
                        info.append("HP Kristal: ").append(pct).append("%\n");
                    }
                    info.append("\n");
                }

                info.append("Aturan:\n");
                info.append("- Semua player bisa masuk tanpa daftar\n");
                info.append("- Bunuh monster sebelum mencapai kristal\n");
                info.append("- Jika kristal hancur, hadiah Pak Lurah DIKUNCI\n");
                info.append("- Jika kristal selamat, semua bisa klaim hadiah!\n\n");
                info.append("Total Wave: ").append(wave != null ? wave.getTotalWaves() : WAVES_TOTAL).append("\n");
                info.append("(Wave terakhir adalah BOSS WAVE!)");

                s.p.sendNoticeBox(info.toString());
            }
        }
    }

    private static final int WAVES_TOTAL = 5; // sama dengan WAVES.size() di PersimpanganKematian

    private static void Service_notice(Session s, String msg) throws IOException {
        s.p.sendNoticeBox(msg);
    }
}
