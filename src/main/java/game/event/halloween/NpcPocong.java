package game.event.halloween;

import client.Player;
import client.io.Session;
import core.Service;
import core.Util;
import game.event.GameEventManager;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NpcPocong — NPC Trick-or-Treat (id: -127)
 *
 * Sekali sehari per pemain, Pocong kasih kejutan random:
 *  - TREAT (70%): dapat beberapa Jimat Malam Suro gratis (item7, lihat
 *    MalamSuroAmbush.JIMAT_ID).
 *  - TRICK (30%): cuma jumpscare/dijahilin, tidak dapat apa-apa — murni
 *    flavor text, TIDAK ada efek/debuff mekanik apapun ke pemain.
 *
 * Cooldown harian disimpan IN-MEMORY (ConcurrentHashMap nama pemain ->
 * tanggal klaim terakhir) — reset otomatis begitu tanggal server berganti.
 * CATATAN: karena in-memory, cooldown ini ke-reset kalau server restart
 * (pemain bisa klaim ulang hari yang sama). Kalau butuh persist lintas
 * restart, pindahkan ke kolom DB player seperti pola daily_login lain di
 * codebase ini (lihat RewardManager).
 *
 * Menu:
 *   0       → Trick or Treat! (klaim kejutan harian)
 *   default → Informasi
 */
public class NpcPocong {

    private static final Map<String, LocalDate> lastClaimDate = new ConcurrentHashMap<>();

    private static final int TREAT_CHANCE_PCT = 70; // sisanya (30%) trick

    private static final int TREAT_JIMAT_MIN = 2;
    private static final int TREAT_JIMAT_MAX = 5;

    public static void onReceivedMenu(Session s, int index) throws IOException {
        HalloweenEvent event = GameEventManager.gI().getEvent(HalloweenEvent.class);

        if (event == null || event.shouldBeRemoved()) {
            s.p.sendNoticeBox("Pocong itu diam saja... sepertinya belum Malam Suro.");
            return;
        }

        switch (index) {

            // ── Menu 0: Trick or Treat ─────────────────────────────────────────
            case 0 -> claimTrickOrTreat(s.p);

            // ── Menu default: Info ─────────────────────────────────────────────
            default -> s.p.sendNoticeBox(
                "*Pocong meloncat-loncat pelan*\n\n" +
                "Ajak aku main \"Trick or Treat\" — sekali sehari!\n" +
                "70% kamu dapat Jimat Malam Suro gratis, 30% cuma dikerjain (gapapa, gak rugi apa-apa kok)."
            );
        }
    }

    private static void claimTrickOrTreat(Player p) throws IOException {
        LocalDate today = LocalDate.now();
        LocalDate last = lastClaimDate.get(p.name);

        if (last != null && last.isEqual(today)) {
            p.sendNoticeBox("Pocong sudah main sama kamu hari ini. Datang lagi besok ya!");
            return;
        }

        lastClaimDate.put(p.name, today);

        if (Util.random(99) < TREAT_CHANCE_PCT) {
            // ── TREAT ──
            short qty = (short) Util.nextInt(TREAT_JIMAT_MIN, TREAT_JIMAT_MAX);
            p.item.add_item_bag47(MalamSuroAmbush.JIMAT_ID, qty, MalamSuroAmbush.ITEM_CAT_MATERIAL);
            p.item.charInventory(7);
            Service.Show_open_box_notice_item(
                p, "Trick or Treat! Kamu Mendapatkan",
                new short[]{MalamSuroAmbush.JIMAT_ID},
                new int[]{qty},
                new short[]{MalamSuroAmbush.ITEM_CAT_MATERIAL}
            );
        } else {
            // ── TRICK ── (murni flavor, tidak ada efek mekanik apapun)
            p.sendNoticeBox(
                "\"BUUU!\" Pocong meloncat kaget-kagetin kamu...\n" +
                "...terus ngakak sendiri. Gak dapet apa-apa kali ini. Coba lagi besok!"
            );
        }
    }
}
