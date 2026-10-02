package game.event.kemerdekaan;

import game.event.GameEventManager;
import client.io.Session;
import game.items.ExchangeService;
import game.items.models.ExchangeItem;
import game.items.models.RequiredItem;

import java.io.IOException;

/**
 * NpcPakLurah — NPC Misi Lomba 17-an (id: -122)
 *
 * Klaim hadiah DIKUNCI jika kristal di Persimpangan Kematian hancur.
 * Semua player tidak bisa klaim sampai event kemerdekaan berakhir atau esok hari.
 *
 * Menu:
 *   0 → Klaim Hadiah Lomba 17-an (tukar Koin Kemerdekaan)
 *   default → Info misi & status kristal
 */
public class NpcPakLurah {

    private static final String KEY_LOMBA = "kemerdekaan_lomba";

    public static void onReceivedMenu(Session s, int index) throws IOException {
        KemerdekaanEvent event = GameEventManager.gI().getEvent(KemerdekaanEvent.class);

        if (event == null || event.shouldBeRemoved()) {
            s.p.sendNoticeBox(
                "Halo Ksatria! Misi Lomba 17-an belum dimulai atau sudah selesai.\n" +
                "Datang kembali pada 10-21 Agustus ya!"
            );
            return;
        }

        switch (index) {

            // ── Menu 0: Klaim hadiah ──────────────────────────────────────────
            case 0 -> {
                // Cek apakah kristal hancur — jika ya, klaim ditolak untuk SEMUA player
                if (event.isHadiahDikunci()) {
                    s.p.sendNoticeBox(
                        "Maaf Ksatria, hadiah Lomba 17-an hari ini DIKUNCI!\n\n" +
                        "Kristal Kehidupan di Persimpangan Kematian telah hancur.\n" +
                        "Seluruh ksatria gagal mempertahankannya.\n\n" +
                        "Ikuti event Persimpangan besok untuk membuka kembali hadiah!\n\n" +
                        "\"Kemerdekaan bukan hadiah, tapi perjuangan!\""
                    );
                    return;
                }

                ExchangeItem item = ExchangeService.gI().getItem(-122, KEY_LOMBA);
                if (item == null) {
                    s.p.sendNoticeBox("Hadiah Lomba 17-an tidak tersedia saat ini.\nHubungi Admin!");
                    return;
                }
                ExchangeService.gI().exchange(s.p, item);
            }

            // ── Menu default: Info ────────────────────────────────────────────
            default -> {
                StringBuilder info = new StringBuilder();
                info.append("Selamat datang Ksatria!\n\n");
                info.append("Aku Pak Lurah, panitia Lomba 17-an!\n\n");

                // Tampilkan status kristal
                if (event.isHadiahDikunci()) {
                    info.append("⚠ STATUS HADIAH: DIKUNCI\n");
                    info.append("Kristal Kehidupan hancur di Persimpangan Kematian.\n");
                    info.append("Hadiah tidak bisa diklaim hari ini.\n\n");
                } else if (event.isWaveSudahMenang()) {
                    info.append("✓ STATUS HADIAH: TERBUKA\n");
                    info.append("Kristal berhasil dijaga! Hadiah bisa diklaim.\n\n");
                } else {
                    info.append("Status Kristal: Belum ada wave hari ini.\n");
                    info.append("Ikuti Persimpangan Kematian jam 20:00 untuk menjaga kristal!\n\n");
                }

                info.append("Cara mendapat hadiah:\n");
                info.append("1. Ikuti event Persimpangan Kematian (jaga kristal)\n");
                info.append("2. Kalahkan monster & kumpulkan Koin Kemerdekaan\n");
                info.append("3. Tukar Koin ke aku!\n\n");

                ExchangeItem lomba = ExchangeService.gI().getItem(-122, KEY_LOMBA);
                if (lomba != null && lomba.getRequirements() != null) {
                    info.append("Bahan yang dibutuhkan:\n");
                    for (RequiredItem it : lomba.getRequirements()) {
                        info.append(" - ")
                            .append(ExchangeService.resolveItemName(it))
                            .append(" ").append(it.getQuantity()).append(" pcs\n");
                    }
                }

                info.append("\nMerdeka!");
                s.p.sendNoticeBox(info.toString());
            }
        }
    }
}
