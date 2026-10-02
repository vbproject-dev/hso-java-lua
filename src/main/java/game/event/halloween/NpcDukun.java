package game.event.halloween;

import client.io.Session;
import game.event.GameEventManager;
import game.items.ExchangeService;
import game.items.models.ExchangeItem;
import game.items.models.RequiredItem;

import java.io.IOException;

/**
 * NpcDukun — NPC Ritual Sesajen Malam Suro (id: -124)
 *
 * Pemain kumpulkan bahan sesajen (item drop biasa selama Halloween, diatur
 * lewat quest/drop rate seperti biasa) lalu tukarkan ke Dukun untuk ritual
 * ruwatan, dan menerima Jimat Malam Suro (item7, lihat MalamSuroAmbush.JIMAT_ID)
 * sebagai currency event.
 *
 * Bahan & jumlah Jimat yang didapat diatur lewat tabel `exchange_item`
 * (key = KEY_RITUAL) — SAMA PERSIS pola NpcSanta/NpcPakLurah, tidak perlu
 * edit kode Java lagi kalau GM mau ganti bahan/reward.
 *
 * Menu:
 *   0       → Lakukan Ritual (tukar bahan sesajen -> Jimat Malam Suro)
 *   default → Informasi bahan yang dibutuhkan
 */
public class NpcDukun {

    private static final String KEY_RITUAL = "halloween_dukun";

    public static void onReceivedMenu(Session s, int index) throws IOException {
        HalloweenEvent event = GameEventManager.gI().getEvent(HalloweenEvent.class);

        if (event == null || event.shouldBeRemoved()) {
            s.p.sendNoticeBox(
                "Hmm... gerbang alam gaib belum terbuka, Nak.\n" +
                "Datang kembali saat Malam Suro tiba."
            );
            return;
        }

        switch (index) {

            // ── Menu 0: Lakukan ritual (tukar sesajen -> Jimat) ──────────────
            case 0 -> {
                ExchangeItem item = ExchangeService.gI().getItem(-124, KEY_RITUAL);
                if (item == null) {
                    s.p.sendNoticeBox("Ritual belum bisa dilakukan sekarang. Hubungi Admin!");
                    return;
                }
                ExchangeService.gI().exchange(s.p, item);
            }

            // ── Menu default: Info bahan ──────────────────────────────────────
            default -> {
                StringBuilder info = new StringBuilder();
                info.append("Aku Dukun penjaga desa.\n\n");
                info.append("Malam Suro adalah malam gerbang alam gaib terbuka.\n");
                info.append("Bantu aku melakukan ritual ruwatan supaya desa selamat!\n\n");

                ExchangeItem ritual = ExchangeService.gI().getItem(-124, KEY_RITUAL);
                if (ritual != null && ritual.getRequirements() != null) {
                    info.append("Bahan sesajen yang dibutuhkan:\n");
                    for (RequiredItem it : ritual.getRequirements()) {
                        info.append(" - ")
                            .append(ExchangeService.resolveItemName(it))
                            .append(" ").append(it.getQuantity()).append(" pcs\n");
                    }
                } else {
                    info.append("(Bahan ritual belum diatur Admin.)\n");
                }

                info.append("\nSetiap ritual berhasil, kamu akan mendapat Jimat Malam Suro.");
                s.p.sendNoticeBox(info.toString());
            }
        }
    }
}
