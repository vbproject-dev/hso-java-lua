package game.event.kemerdekaan;

import game.event.GameEventManager;
import client.io.Session;

import java.io.IOException;
import java.util.Random;

/**
 * NpcNenekMerdeka — NPC Pencerita Sejarah Kemerdekaan (id: -123)
 *
 * Dipanggil dari MenuController.java:
 *   case -123:
 *       NpcNenekMerdeka.onReceivedMenu(conn, index);
 *       break;
 *
 * Menu:
 *   0 → Dengar kisah perjuangan (dialog acak dari 5 pilihan)
 *   default → Sapa pemain & jelaskan menu
 */
public class NpcNenekMerdeka {

    private static final Random RAND = new Random();

    private static final String[] KISAH = {
        "Dulu, para pahlawan kita berjuang tanpa kenal takut demi tanah air ini.\n" +
        "Mereka tidak punya senjata canggih, hanya semangat yang membara.\n" +
        "Ingatlah pengorbanan mereka, Ksatria!",

        "Bung Karno pernah berkata:\n'Bangsa yang besar adalah bangsa yang menghormati " +
        "jasa para pahlawannya.'\nJadilah ksatria yang membanggakan negeri ini!",

        "Pada 17 Agustus 1945, kemerdekaan Indonesia diproklamasikan.\n" +
        "Tetesan darah para pejuang tidak boleh sia-sia.\n" +
        "Teruslah berjuang untuk kebaikan, Ksatria!",

        "Para pemuda pejuang dulu rela mengorbankan segalanya demi satu kata: Merdeka!\n" +
        "Semangat itu harus terus hidup di dalam hatimu, Ksatria.",

        "Nenek ingat cerita tentang pertempuran Surabaya 10 November 1945.\n" +
        "Para pahlawan berteriak 'Merdeka atau Mati!' dengan penuh keberanian.\n" +
        "Jadikan semangat itu kekuatanmu!",
    };

    public static void onReceivedMenu(Session s, int index) throws IOException {
        KemerdekaanEvent event = GameEventManager.gI().getEvent(KemerdekaanEvent.class);

        if (event == null || event.shouldBeRemoved()) {
            s.p.sendNoticeBox(
                "Hai cucuku... Nenek sedang beristirahat.\n" +
                "Datanglah kembali saat perayaan 17 Agustus tiba (10 Agustus - 2 September)!"
            );
            return;
        }

        switch (index) {
            case 0 -> {
                String kisah = KISAH[RAND.nextInt(KISAH.length)];
                s.p.sendNoticeBox("Kisah Nenek Merdeka:\n\n" + kisah);
            }
            default -> {
                s.p.sendNoticeBox(
                    "Hai cucuku! Nenek Merdeka namaku.\n\n" +
                    "Di bulan kemerdekaan ini, aku ingin berbagi kisah perjuangan " +
                    "para pahlawan bangsa kita.\n\n" +
                    "Mau mendengar cerita Nenek?\nMerdeka!"
                );
            }
        }
    }
}
