package game.npc;

import client.io.Session;
import game.map.MapManager;
import model.map.NpcData;

import java.io.IOException;

/**
 * NpcCredit — NPC Credit Game (id: -96)
 *
 * Dipanggil dari MenuController.java:
 *   case -96:
 *       NpcCredit.onReceivedMenu(conn, index);
 *       break;
 *
 * Teks credit diambil dari kolom `dialog_text` tabel `npc` (id -96),
 * jadi bisa diedit langsung lewat DB tanpa rebuild server.
 *
 * Menu:
 *   0 / default → Tampilkan box teks credit
 */
public class NpcCredit {

    private static final int NPC_ID = -96;

    public static void onReceivedMenu(Session s, int index) throws IOException {
        NpcData npcData = MapManager.getInstance().getNpcData(NPC_ID);

        String creditText = (npcData != null && npcData.getDialogText() != null && !npcData.getDialogText().isEmpty())
                ? npcData.getDialogText()
                : "Credit belum diatur.\nSilakan isi kolom dialog_text NPC -96 di DB.";

        // Kalau teks di DB diisi manual lewat grid (bukan query SQL), literal "\n"
        // (2 karakter: backslash + n) tidak otomatis jadi newline beneran.
        // Baris ini menormalkan itu supaya tetap tampil sebagai baris baru.
        creditText = creditText.replace("\\n", "\n");

        switch (index) {
            default -> s.p.sendNoticeBox(creditText);
        }
    }
}