package game.event.kemerdekaan;

import game.event.GameEventManager;
import client.io.Session;
import game.items.ExchangeService;
import game.items.models.ExchangeItem;
import game.items.models.RequiredItem;

import java.io.IOException;

/**
 * NpcPaskibraka — NPC utama Event Kemerdekaan (id: -121)
 *
 * Dipanggil dari MenuController.java:
 *   case -121:
 *       NpcPaskibraka.onReceivedMenu(conn, index);
 *       break;
 *
 * Menu:
 *   0 → Tukar Koin Kemerdekaan → Bendera Merah Putih
 *   1 → Tukar Koin Kemerdekaan → Kotak Hadiah 17 Agustus
 *   default → Info bahan yang dibutuhkan
 */
public class NpcPaskibraka {

    private static final String KEY_BENDERA = "kemerdekaan_bendera";
    private static final String KEY_HADIAH  = "kemerdekaan_hadiah";

    public static void onReceivedMenu(Session s, int index) throws IOException {
        KemerdekaanEvent event = GameEventManager.gI().getEvent(KemerdekaanEvent.class);

        if (event == null || event.shouldBeRemoved()) {
            s.p.sendNoticeBox(
                "Maaf Ksatria, event Kemerdekaan belum dimulai atau sudah berakhir.\n" +
                "Kembali lagi pada 10-21 Agustus!"
            );
            return;
        }

        switch (index) {
            case 0 -> {
                ExchangeItem item = ExchangeService.gI().getItem(-121, KEY_BENDERA);
                if (item == null) {
                    s.p.sendNoticeBox("Item Bendera Merah Putih tidak ditemukan.\nHubungi Admin!");
                    return;
                }
                ExchangeService.gI().exchange(s.p, item);
            }
            case 1 -> {
                ExchangeItem item = ExchangeService.gI().getItem(-121, KEY_HADIAH);
                if (item == null) {
                    s.p.sendNoticeBox("Kotak Hadiah 17 Agustus tidak ditemukan.\nHubungi Admin!");
                    return;
                }
                ExchangeService.gI().exchange(s.p, item);
            }
            default -> {
                StringBuilder info = new StringBuilder();
                info.append("Selamat Hari Kemerdekaan Indonesia!\n\n");

                ExchangeItem bendera = ExchangeService.gI().getItem(-121, KEY_BENDERA);
                if (bendera != null && bendera.getRequirements() != null) {
                    info.append("Bahan Tukar Bendera Merah Putih:\n");
                    for (RequiredItem it : bendera.getRequirements()) {
                        info.append(" - ")
                            .append(ExchangeService.resolveItemName(it))
                            .append(" ").append(it.getQuantity()).append(" pcs\n");
                    }
                    info.append("\n");
                }

                ExchangeItem hadiah = ExchangeService.gI().getItem(-121, KEY_HADIAH);
                if (hadiah != null && hadiah.getRequirements() != null) {
                    info.append("Bahan Kotak Hadiah 17 Agustus:\n");
                    for (RequiredItem it : hadiah.getRequirements()) {
                        info.append(" - ")
                            .append(ExchangeService.resolveItemName(it))
                            .append(" ").append(it.getQuantity()).append(" pcs\n");
                    }
                }

                s.p.sendNoticeBox(info.toString());
            }
        }
    }
}
