package game.event.halloween;

import client.io.Session;
import game.event.GameEventManager;

import java.io.IOException;

/**
 * NpcBanaspati — NPC Roh Api Peramal (id: -125)
 *
 * Banaspati (roh api dalam folklor Jawa) berperan sebagai "penunjuk arah"
 * event MalamSuroAmbush: pemain bisa tanya ke dia untuk merasakan apakah
 * ada hantu ambush yang sedang mengintai sekarang, dan di map mana — tanpa
 * kasih tahu koordinat persis/nama pemain yang jadi target, supaya pemain
 * tetap harus cari sendiri begitu sampai di map itu.
 *
 * Menu:
 *   0       → Rasakan Aura (cek status ambush MalamSuroAmbush sekarang)
 *   default → Informasi / lore Malam Suro
 */
public class NpcBanaspati {

    public static void onReceivedMenu(Session s, int index) throws IOException {
        HalloweenEvent event = GameEventManager.gI().getEvent(HalloweenEvent.class);

        if (event == null || event.shouldBeRemoved()) {
            s.p.sendNoticeBox(
                "Api ini tenang... belum ada tanda-tanda Malam Suro, Nak."
            );
            return;
        }

        switch (index) {

            // ── Menu 0: Rasakan aura ambush ────────────────────────────────────
            case 0 -> {
                MalamSuroAmbush ambush = GameEventManager.gI().getEvent(MalamSuroAmbush.class);
                if (ambush != null && ambush.isGhostActive()) {
                    String mapName = ambush.getGhostMapName();
                    s.p.sendNoticeBox(
                        "🔥 Api Banaspati bergejolak liar!\n\n" +
                        "\"Aku mencium amarah arwah... di " + (mapName != null ? mapName : "suatu tempat") + "!\n" +
                        "Cepat ke sana sebelum terlambat, atau biarkan kalau tak berani...\""
                    );
                } else {
                    s.p.sendNoticeBox(
                        "🔥 Api Banaspati tenang.\n\n" +
                        "\"Tidak ada arwah yang mengamuk saat ini. Coba tanya lagi nanti, Nak.\""
                    );
                }
            }

            // ── Menu default: Lore ─────────────────────────────────────────────
            default -> s.p.sendNoticeBox(
                "Aku Banaspati, roh api yang mengembara sejak dulu kala.\n\n" +
                "Konon di Malam Suro, arwah-arwah gentayangan bebas berkeliaran " +
                "mencari mangsa yang lengah.\n\n" +
                "Tanya aku kapan saja kalau mau merasakan ke mana amarah mereka mengarah — " +
                "tapi ingat, keberanianmu sendiri yang menentukan hasilnya."
            );
        }
    }
}
