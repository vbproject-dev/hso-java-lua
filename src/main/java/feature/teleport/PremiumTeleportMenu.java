package feature.teleport;

import client.Item;
import client.Player;
import menu.Dialog;
import menu.Menu;
import menu.MenuHelper;
import menu.MenuManager;
import model.map.Vgo;
import template.ItemTemplate4;

import java.io.IOException;
import java.util.Map;

/**
 * PremiumTeleportMenu
 *
 * Menu yang muncul ketika player mengklik NPC Teleport Premium.
 *
 * ────────────────────────────────────────────────
 * NPC yang terlibat:
 *   NPC -100  = Batu Teleport di desa luar
 *               → Teleport GRATIS ke Map 115 (Desa Ajaib)
 *   NPC -102  = NPC di Map 115 (Desa Ajaib)
 *               → Jual paket, pilih map tujuan 111-114
 *
 * Alur pembelian:
 *   1. Pilih map tujuan (111-114)
 *   2. Pilih durasi (1/2/3 jam)
 *   3. Pilih metode bayar (Gems / Tiket)
 *   4. Konfirmasi (Yes/No)
 *   5. Sesi aktif → teleport langsung ke map tujuan
 *
 * Jika sesi sudah aktif → bisa langsung pilih map tanpa bayar (gratis)
 * ────────────────────────────────────────────────
 *
 * Map Premium (rantai linear):
 *   115 Desa Ajaib   → entry point, gateway
 *   111 Elan Pletau  → area 1
 *   112 Queer Wood   → area 2
 *   113 Emerald Cave → area 3
 *   114 Dagnues Nest → area 4 (ujung)
 */
public class PremiumTeleportMenu {

    private static final int NPC_LOBBY = -100;   // Batu Teleport di desa luar
    private static final int NPC_ID    = -102;   // NPC penjual paket di Map 115

    // ---------------------------------------------------------------
    // Data map premium (tidak termasuk 115 sebagai pilihan tujuan)
    // Player sudah ada di 115 saat buka menu ini
    // ---------------------------------------------------------------

    /**
     * Spawn point di tiap map premium.
     * Berdasarkan ukuran map dan posisi warp yang sudah ada di SQL:
     *
     * Map 111 (41x31 tiles = 984x744px):
     *   Warp masuk dari 115 ada di x=336,y=0 → spawn di pintu masuk atas
     *
     * Map 112 (50x70 tiles = 1200x1680px):
     *   Warp masuk dari 111 ada di x=120,y=720 → spawn di sisi kiri tengah
     *
     * Map 113 (44x37 tiles = 1056x888px):
     *   Warp masuk dari 112 ada di x=0,y=1656 → spawn di bawah kiri
     *   (warp baru yang ditambah di SQL)
     *
     * Map 114 (40x30 tiles = 960x720ps
     * x):
     *   Warp baru — spawn di tengah atas
     */
    private static final int[][] MAP_SPAWN = {
            {111, 72,  696},   // Elan Pletau   — dekat warp masuk dari 115
            //{112, 120, 720},   // Queer Wood    — dekat warp masuk dari 111
            //{113, 24,  816},   // Emerald Cave  — dekat warp masuk dari 112
            //{114, 480, 48},    // Dagnues Nest  — tengah atas (map baru)
    };

    private static final String[] MAP_NAMES = {
            "Elan Pletau",
            // "Queer Wood",
            // "Emerald Cave",
            // "Dagnues Nest",
    };

    // ---------------------------------------------------------------
    // Entry point NPC -100 — Batu Teleport di desa luar
    // Teleport GRATIS ke Map 115 (Desa Ajaib)
    // ---------------------------------------------------------------
    public static void teleportToGateway(Player player) {
        if (PremiumTeleportManager.isPremiumMap(player.map.mapId)) {
            player.sendNoticeBox(
                "ℹ️ Kamu sudah berada di Area Premium.\n" +
                "Cari NPC Teleport Premium di Desa Ajaib\nuntuk berpindah ke area lain."
            );
            return;
        }

        try {
            Vgo vgo = Vgo.create(
                    PremiumTeleportManager.GATEWAY_MAP_ID,
                    PremiumTeleportManager.RETURN_X,
                    PremiumTeleportManager.RETURN_Y
            );
            player.changeMap(player, vgo);
            player.sendNoticeBox(
                "✅ Selamat datang di Desa Azure!\n\n" +
                "Cari Batu Ratapan\n" +
                "untuk masuk ke area berbayar."
            );
        } catch (IOException e) {
            player.sendNoticeBox("❌ Gagal teleport ke gateway. Silakan coba lagi.");
        }
    }

    // ---------------------------------------------------------------
    // Entry point NPC -102 — NPC di Map 115
    // ---------------------------------------------------------------
    public static void open(Player player) {
        // Guard: hanya bisa dibuka dari Map 115
        if (!PremiumTeleportManager.isGatewayMap(player.map.mapId)) {
            player.sendNoticeBox("❌ NPC ini hanya bisa diakses dari Desa Ajaib (Map 115).");
            return;
        }

        PremiumTeleportManager mgr = PremiumTeleportManager.gI();
        String sessionInfo = buildSessionInfo(player);
        MenuHelper helper = MenuHelper.forNpc(NPC_ID);

        if (mgr.hasActiveSession(player)) {
            // ── Punya sesi aktif → masuk GRATIS ke tiap area ──
            for (int i = 0; i < MAP_SPAWN.length; i++) {
                final int mapIdx = i;
                helper.menu(
                        "🚀 " + MAP_NAMES[i] + " (Gratis)",
                        p -> teleportFree(p, mapIdx)
                );
            }
            // Opsi tambah waktu
            helper.submenu("⏱ Tambah Waktu", buildExtendSubMenu());

        } else {
            // ── Belum punya sesi → wajib beli paket ──
            for (int i = 0; i < MAP_SPAWN.length; i++) {
                final int mapIdx = i;
                helper.submenu("🗺 Masuk ke " + MAP_NAMES[i], buildPackageSubMenu(mapIdx));
            }
        }

        // Status sesi
        helper.menu("📊 Status Sesi", p -> p.sendNoticeBox(buildSessionInfo(p)));

        // Panduan
        helper.menu("📖 Panduan", p -> {
            String tiketName = getTicketName();
            p.sendNoticeBox(
                "━━━ TELEPORT PREMIUM ━━━\n\n" +
                "🗺  Area: 111-114 (rantai linear)\n" +
                "    115 → 111 → 112 → 113 → 114 → 2\n\n" +
                "💎 Harga Gems:\n" +
                "  • 1 Jam  = 3.000 Gems\n" +
                "  • 2 Jam  = 6.000 Gems\n" +
                "  • 3 Jam  = 9.000 Gems\n\n" +
                "🎫 Harga Item (" + tiketName + "):\n" +
                "  • 1 Jam  = 1 Tiket\n" +
                "  • 2 Jam  = 2 Tiket\n" +
                "  • 3 Jam  = 3 Tiket\n\n" +
                "⚠️  Saat waktu habis kamu akan\n" +
                "   dipindah ke Desa Ajaib.\n\n" +
                "💡 Paket bisa ditumpuk (extend)!\n" +
                "💡 Warp antar area tersedia di\n" +
                "   ujung setiap map."
            );
        });

        helper.menu("Close", "Tutup", Player::closeMenuDialog);

        Menu root = Menu.builder()
                .npc(NPC_ID)
                .title("Teleport Premium")
                .name(sessionInfo)
                .menus(helper.build())
                .build();

        MenuManager.openMenu(player, root);
    }

    // ---------------------------------------------------------------
    // Sub-menu pilih paket jam untuk map tertentu
    // ---------------------------------------------------------------
    private static java.util.List<Menu> buildPackageSubMenu(int mapIdx) {
        MenuHelper sub = MenuHelper.forNpc(NPC_ID);

        for (int[] pkg : new int[][]{
                {1, PremiumTeleportManager.GEMS_PER_HOUR},
                {2, PremiumTeleportManager.GEMS_PER_HOUR * 2},
                {3, PremiumTeleportManager.GEMS_PER_HOUR * 3}}) {
            final int hours   = pkg[0];
            final int gemCost = pkg[1];
            sub.submenu(
                    "⏱ " + hours + " Jam",
                    buildPaymentSubMenu(mapIdx, hours, gemCost)
            );
        }

        sub.menu("← Kembali", MenuManager::navigateBack);
        return sub.build();
    }

    // ---------------------------------------------------------------
    // Sub-menu pilih metode bayar
    // mapIdx == -1 berarti mode extend saja (tidak pindah map)
    // ---------------------------------------------------------------
    private static java.util.List<Menu> buildPaymentSubMenu(int mapIdx, int hours, int gemCost) {
        int ticketCount = hours;
        String tiketName = getTicketName();
        MenuHelper pay = MenuHelper.forNpc(NPC_ID);

        // ── Bayar Gems ────────────────────────────────────────────
        pay.menu(
                "💎 Bayar " + gemCost + " Gems",
                Map.of("mapIdx", mapIdx, "hours", hours, "gemCost", gemCost),
                (p, args) -> {
                    int mi = (int) args.get("mapIdx");
                    int h  = (int) args.get("hours");
                    int gc = (int) args.get("gemCost");

                    if (p.getGem() < gc) {
                        p.sendNoticeBox("❌ Gems tidak cukup!\nKamu butuh " + gc + " Gems.\nGems kamu: " + p.getGem());
                        return;
                    }

                    String dest = (mi >= 0) ? MAP_NAMES[mi] : "perpanjang waktu";
                    Dialog confirm = Dialog.builder()
                            .text("Bayar " + gc + " Gems untuk " + h + " jam?\nTujuan: " + dest +
                                  "\nGems kamu: " + p.getGem())
                            .args(Map.of("mapIdx", mi, "hours", h, "gemCost", gc))
                            .onRespond((px, yes, a) -> {
                                if (!yes) return;
                                int gc2 = (int) a.get("gemCost");
                                int h2  = (int) a.get("hours");
                                int mi2 = (int) a.get("mapIdx");

                                if (px.getGem() < gc2) {
                                    px.sendNoticeBox("❌ Gems tidak cukup!");
                                    return;
                                }
                                px.updateGem(-gc2);
                                activateAndTeleport(px, mi2, h2);
                            })
                            .build();
                    p.openDialog(confirm);
                }
        );

        // ── Bayar Tiket Item ──────────────────────────────────────
        pay.menu(
                "🎫 Pakai " + ticketCount + "x " + tiketName,
                Map.of("mapIdx", mapIdx, "hours", hours, "ticketCount", ticketCount),
                (p, args) -> {
                    int mi = (int) args.get("mapIdx");
                    int h  = (int) args.get("hours");
                    int tc = (int) args.get("ticketCount");

                    int owned = p.item.total_item_by_id(4, PremiumTeleportManager.ITEM4_TICKET_ID);
                    if (owned < tc) {
                        p.sendNoticeBox("❌ Tiket tidak cukup!\nKamu butuh " + tc + "x " + tiketName +
                                        ".\nMilik kamu: " + owned + " buah.");
                        return;
                    }

                    String dest = (mi >= 0) ? MAP_NAMES[mi] : "perpanjang waktu";
                    Dialog confirm = Dialog.builder()
                            .text("Pakai " + tc + "x " + tiketName + " untuk " + h + " jam?\nTujuan: " + dest +
                                  "\nMilik kamu: " + owned + " buah.")
                            .args(Map.of("mapIdx", mi, "hours", h, "ticketCount", tc))
                            .onRespond((px, yes, a) -> {
                                if (!yes) return;
                                int tc2 = (int) a.get("ticketCount");
                                int h2  = (int) a.get("hours");
                                int mi2 = (int) a.get("mapIdx");

                                int owned2 = px.item.total_item_by_id(4, PremiumTeleportManager.ITEM4_TICKET_ID);
                                if (owned2 < tc2) {
                                    px.sendNoticeBox("❌ Tiket tidak cukup!");
                                    return;
                                }
                                px.item.remove(4, PremiumTeleportManager.ITEM4_TICKET_ID, tc2);
                                px.item.updateBag();
                                activateAndTeleport(px, mi2, h2);
                            })
                            .build();
                    p.openDialog(confirm);
                }
        );

        pay.menu("← Kembali", MenuManager::navigateBack);
        return pay.build();
    }

    // ---------------------------------------------------------------
    // Teleport gratis — hanya jika sesi masih aktif
    // ---------------------------------------------------------------
    private static void teleportFree(Player p, int mapIdx) {
        PremiumTeleportManager mgr = PremiumTeleportManager.gI();
        if (!mgr.hasActiveSession(p)) {
            p.sendNoticeBox("❌ Sesi kamu sudah habis!\nSilakan beli paket terlebih dahulu.");
            return;
        }
        int[] spawn = MAP_SPAWN[mapIdx];
        try {
            Vgo vgo = Vgo.create(spawn[0], spawn[1], spawn[2]);
            p.changeMap(p, vgo);

            long remaining = mgr.getRemainingSeconds(p);
            long jam       = remaining / 3600;
            long sisaMenit = (remaining % 3600) / 60;

            p.sendNoticeBox(
                "✅ Selamat datang di " + MAP_NAMES[mapIdx] + "!\n\n" +
                "⏱  Sisa waktu: " + jam + " jam " + sisaMenit + " menit\n\n" +
                "💡 Gunakan warp di ujung map\n" +
                "   untuk pindah ke area berikutnya."
            );
        } catch (IOException e) {
            p.sendNoticeBox("❌ Terjadi kesalahan saat teleport. Silakan coba lagi.");
        }
    }

    // ---------------------------------------------------------------
    // Sub-menu tambah waktu (extend) — muncul saat sesi masih aktif
    // ---------------------------------------------------------------
    private static java.util.List<Menu> buildExtendSubMenu() {
        MenuHelper ext = MenuHelper.forNpc(NPC_ID);

        for (int[] pkg : new int[][]{
                {1, PremiumTeleportManager.GEMS_PER_HOUR},
                {2, PremiumTeleportManager.GEMS_PER_HOUR * 2},
                {3, PremiumTeleportManager.GEMS_PER_HOUR * 3}}) {
            final int hours   = pkg[0];
            final int gemCost = pkg[1];
            ext.submenu(
                    "⏱ Tambah " + hours + " Jam",
                    buildPaymentSubMenu(-1, hours, gemCost)  // mapIdx -1 = extend only
            );
        }

        ext.menu("← Kembali", MenuManager::navigateBack);
        return ext.build();
    }

    // ---------------------------------------------------------------
    // Aktivasi sesi dan teleport player ke map premium
    // ---------------------------------------------------------------
    private static void activateAndTeleport(Player p, int mapIdx, int hours) {
        PremiumTeleportManager mgr = PremiumTeleportManager.gI();
        mgr.startSession(p, hours);

        long remaining = mgr.getRemainingSeconds(p);
        long jam       = remaining / 3600;
        long sisaMenit = (remaining % 3600) / 60;

        // mapIdx == -1 → hanya extend, tidak pindah map
        if (mapIdx < 0) {
            p.sendNoticeBox(
                "✅ Waktu berhasil ditambahkan!\n\n" +
                "⏱  Total sisa waktu: " + jam + " jam " + sisaMenit + " menit"
            );
            return;
        }

        int[] spawn = MAP_SPAWN[mapIdx];
        try {
            Vgo vgo = Vgo.create(spawn[0], spawn[1], spawn[2]);
            p.changeMap(p, vgo);

            p.sendNoticeBox(
                "✅ Selamat datang di " + MAP_NAMES[mapIdx] + "!\n\n" +
                "⏱  Sisa waktu: " + jam + " jam " + sisaMenit + " menit\n\n" +
                "⚠️  Jika waktu habis kamu akan\n" +
                "   dipindah ke Desa Ajaib.\n\n" +
                "💡 Gunakan warp di ujung map\n" +
                "   untuk pindah ke area berikutnya."
            );
        } catch (IOException e) {
            p.sendNoticeBox("❌ Terjadi kesalahan saat teleport. Silakan coba lagi.");
        }
    }

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------
    private static String buildSessionInfo(Player p) {
        PremiumTeleportManager mgr = PremiumTeleportManager.gI();
        if (!mgr.hasActiveSession(p)) {
            return "Tidak ada sesi aktif";
        }
        long secs  = mgr.getRemainingSeconds(p);
        long jam   = secs / 3600;
        long menit = (secs % 3600) / 60;
        long detik = secs % 60;
        return "✅ Sesi aktif — Sisa: " + jam + "j " + menit + "m " + detik + "d";
    }

    private static String getTicketName() {
        try {
            ItemTemplate4 tpl = ItemTemplate4.item.get(PremiumTeleportManager.ITEM4_TICKET_ID);
            return tpl != null ? tpl.getName() : "Tiket Teleport Premium";
        } catch (Exception e) {
            return "Tiket Teleport Premium";
        }
    }

    public static void teleportToLobby(Player player) {

        teleportToGateway(player);
    }
}