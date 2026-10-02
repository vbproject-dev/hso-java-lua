package game.event.natal;

import game.event.GameEventManager;
import client.io.Session;
import game.items.ExchangeService;
import game.items.models.ExchangeItem;
import game.items.models.RequiredItem;
import template.ItemTemplate4;

import java.io.IOException;

public class NpcSanta {

    public static void onReceivedMenu(Session s, int index) throws IOException {
        ChristmasEvent event = GameEventManager.gI().getEvent(ChristmasEvent.class);
        if (event == null) {
            s.p.sendNoticeBox("Tidak dalam waktu event");
            return;
        }

        if (event.shouldBeRemoved()) {
            s.p.sendNoticeBox("Even belum dimulai atau sudah berakhir");
            return;
        }
        switch (index) {
            case 0 -> {
                ExchangeItem item = ExchangeService.gI().getItem(-120, "natal");
                if (item == null) {
                    s.p.sendNoticeBox("Item tidak ditemukan atau event sudah berakhir");
                    return;
                }
                ExchangeService.gI().exchange(s.p, item);
            }
            default -> {
                StringBuilder bahan = new StringBuilder();
                ExchangeItem item = ExchangeService.gI().getItem(-120, "natal");
                for (RequiredItem it : item.getRequirements()) {

                    bahan.append(" - ").append(ItemTemplate4.item.get(it.getItemId()).getName()).append(" ").append(it.getQuantity()).append(" pcs \n");
                }

                s.p.sendNoticeBox("Bahan yang di perlukan untuk menerima kotak hadiah \n" + bahan.toString());
            }
        }
    }
}
