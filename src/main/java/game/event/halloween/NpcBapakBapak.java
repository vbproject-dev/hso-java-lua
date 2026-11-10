package game.event.halloween;

import game.event.GameEventManager;
import client.io.Session;
import java.io.IOException;

public class NpcBapakBapak {

    public static void onReceivedMenu(Session s, int index) throws IOException {
        s.p.sendNoticeBox(
            "Halo Nak, aku Bapak-Bapak penjaga desa.\n\n" +
            "Malam Suro adalah malam gerbang alam gaib terbuka.\n" +
            "Datanglah ke desa saat Malam Suro tiba untuk melihat keajaiban!"
        );
    }
}