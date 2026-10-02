package menu;


import client.FashionSetting;
import client.Pet;
import client.Player;
import core.*;
import event_daily.MoLy;
import client.io.Message;
import client.io.Session;
import game.ai.AiManager;
import game.ai.PlayerBot;
import game.event.GameEventManager;
import game.event.btf.BTF;
import game.event.btf.BTFState;
import game.event.btf.Team;

import game.event.kemerdekaan.PersimpanganKematian;
import game.event.halloween.GerbangArwah;
import game.items.ExchangeService;
import game.items.ItemManager;
import game.items.compensation.CompensationManager;
import game.items.compensation.ItemEntry;
import game.items.compensation.RoleItems;
import game.items.models.ExchangeItem;
import game.items.models.ItemReward;
import game.items.models.RequiredItem;
import game.pet.PetManager;
import game.shop.Shop;
import game.shop.ShopManager;
import lombok.extern.slf4j.Slf4j;
import game.map.GameMap;
import game.map.MapService;
import model.event.GlobalEvent;
import model.map.Vgo;
import template.Item3;
import template.Item47;
import template.ItemTemplate4;
import topup.Status;
import topup.TopupType;
import utils.SQLHelper;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Slf4j
public class MenuManager {

    public static boolean handleMenuSelection(Player player, short npcId, byte menuId, byte index) throws IOException {
        Menu current = player.getCurrentMenu();

        if (!isValidMenu(current, npcId, menuId)) {
            return false;
        }

        List<Menu> options = current.getMenus();
        if (!isValidIndex(index, options)) {
            return false;
        }

        Menu selected = options.get(index);
        executeMenuSelection(player, selected);
        return true;
    }

    private static boolean isValidMenu(Menu menu, short npcId, byte menuId) {
        return menu != null && menu.getNpc() == npcId && menu.getId() == menuId;
    }

    private static boolean isValidIndex(byte index, List<Menu> options) {
        return options != null && index >= 0 && index < options.size();
    }

    private static void executeMenuSelection(Player player, Menu selected) throws IOException {
        // Check if it's a navigation action (like Back button)
        boolean isNavigation = selected.getTitle().equals("Back");

        // If menu has submenus, navigate to them
        if (hasSubmenus(selected)) {
            player.setCurrentMenu(selected); // This adds current to history
            player.sendMenuDialog(selected);
            // Execute action after navigation if present
            selected.perform(player);
            return;
        }

        selected.perform(player);

        if (!isNavigation && !hasSubmenus(selected)) {
            player.closeMenuDialog();
        }
    }

    private static boolean hasSubmenus(Menu menu) {
        return menu.getMenus() != null && !menu.getMenus().isEmpty();
    }

    // Helper to open root menu
    public static void openMenu(Player player, Menu rootMenu) {
        player.setMenuDialogRoot(rootMenu);
        player.sendMenuDialog(rootMenu);
    }

    // Helper to navigate back to parent menu (if tracking menu history)
    public static void navigateBack(Player player) {
        player.navigateToParent();
    }

    public static void openUserMenu(Player player) {
        Menu root = Menu.builder()
                .npc(-199)
                .title("Lainya")
                .name("Where would you like to go?")
                .menus(MenuHelper
                        .forNpc(-199)
                        .menu("Buka Peti", p -> {
                            if (p.item.total_item_by_id(4, 52) > 0) {
                                MoLy.show_table_to_choose_item(p);
                            } else {
                                Service.send_notice_box(p.conn, "Tidak cukup tiket terbuka dalam inventory");
                            }
                        })
                        .submenu("Drop Settings", dropSettingMenu(player))
                        .submenu("Lepas Fashion", createFashionMenu(player))
                        .submenu("Hide Fashion", createShowHideFashionMenu(player))
                        .menu("Ke Kota", p -> {
                            if (p.conn.ac_admin < 10 && p.item.wear[11] != null && (p.item.wear[11].id == 3599
                                    || p.item.wear[11].id == 3593 || p.item.wear[11].id == 3596)) {
                                Service.send_notice_box(p.conn,
                                        "Kamu tidak bisa kembali ke desa dengan cepat saat memakai jenis armor yang berhubungan dengan fungsi berdagang");
                                return;
                            }
                            Vgo vgo = new Vgo();
                            vgo.toMap = 1;
                            vgo.toX = 432;
                            vgo.toY = 354;
                            p.changeMap(p, vgo);
                        })
                        .menu("Mode AFK", p -> {
                            if (p.modeBot) {
                                game.afk.AfkBotController bot = p.afkBot;
                                String elapsed    = (bot != null) ? bot.getElapsedFormatted()       : "?";
                                long   nextDeduct = (bot != null) ? bot.getMinutesUntilNextDeduct() : 0;
                                long   expGained  = (bot != null) ? bot.getTotalExpGained()         : 0;

                                Dialog stopDialog = Dialog.builder()
                                        .id((byte) -11)
                                        .text("● Mode AFK Sedang Aktif!\n\n" +
                                              "Durasi AFK   : " + elapsed + "\n" +
                                              "Exp didapat  : " + core.Util.number_format(expGained) + "\n" +
                                              "Gems sisa    : " + p.getGem() + "\n" +
                                              "Potong gems  : " + nextDeduct + " menit lagi\n\n" +
                                              "Apakah kamu ingin MENGHENTIKAN mode AFK sekarang?\n" +
                                              "(Karakter akan tetap di map, bot berhenti berburu)")
                                        .onRespond((px, yesOrNo, args) -> {
                                            if (yesOrNo) {
                                                game.afk.AfkBotController activeBot = px.afkBot;
                                                if (px.modeBot && activeBot != null) {
                                                    activeBot.stopBotManual();
                                                    try {
                                                        core.Service.send_notice_box(px.conn,
                                                                "✓ Mode AFK berhasil dihentikan.\n" +
                                                                "Karakter kamu kembali ke mode manual.");
                                                    } catch (java.io.IOException ioe) {
                                                        log.warn("[AFK] Gagal kirim notice stop: {}", ioe.getMessage());
                                                    }
                                                } else {
                                                    try {
                                                        core.Service.send_notice_box(px.conn, "Mode AFK sudah tidak aktif.");
                                                    } catch (java.io.IOException ioe) {
                                                        log.warn("[AFK] Gagal kirim notice: {}", ioe.getMessage());
                                                    }
                                                }
                                            }
                                        })
                                        .build();
                                p.openDialog(stopDialog);
                                return;
                            }
                            // FIX: Validasi posisi karakter sebelum aktifkan bot
                            if (p.map == null) {
                                Service.send_notice_box(p.conn, "Tidak bisa aktivasi AFK — karakter belum masuk map.");
                                return;
                            }
                            if (p.x == 0 && p.y == 0) {
                                Service.send_notice_box(p.conn, "Tidak bisa aktivasi AFK — posisi karakter tidak valid (0,0). Coba pindah dulu.");
                                return;
                            }
                            if (p.map.ismaplang) {
                                Service.send_notice_box(p.conn, "Tidak bisa aktivasi AFK di kota. Pergi ke map berburu terlebih dahulu.");
                                return;
                            }
                            // Cek gems sebelum tampil dialog
                            final int GEM_COST_ACTIVATE = 5000; // biaya aktivasi sekali bayar
                            final int GEM_COST_PER_HOUR = 500;  // biaya per jam setelahnya
                            String gemWarning = p.getGem() >= GEM_COST_ACTIVATE
                                    ? "Gems kamu: " + p.getGem() + " ✓"
                                    : "⚠ Gems kamu: " + p.getGem() + " (kurang! butuh " + GEM_COST_ACTIVATE + ")";

                            Dialog afkDialog = Dialog.builder()
                                    .id((byte) -10)
                                    .text("Aktifkan Mode AFK?\n\n" +
                                          "Biaya Aktivasi: " + GEM_COST_ACTIVATE + " Gems (langsung dipotong)\n" +
                                          "Biaya Lanjutan: " + GEM_COST_PER_HOUR + " Gems / jam\n" +
                                          gemWarning + "\n\n" +
                                          "Karakter akan tetap ONLINE dan berburu otomatis:\n" +
                                          "- Auto attack monster\n" +
                                          "- Auto pickup Gold\n" +
                                          "- Auto pickup Item & Material\n" +
                                          "- Exp & Level tetap bertambah\n\n" +
                                          "Bot akan berhenti otomatis jika gems habis.\n" +
                                          "Koneksi kamu akan diputus, karakter tetap di dunia game.")
                                    .onRespond((px, yesOrNo, args) -> {
                                        if (yesOrNo) {
                                            // Validasi posisi player sebelum aktifkan AFK
                                            if (px.map == null || (px.x == 0 && px.y == 0)) {
                                                try {
                                                    core.Service.send_notice_box(px.conn, "Aktivasi AFK gagal — posisi tidak valid. Coba gerak dulu.");
                                                } catch (java.io.IOException ioe) {
                                                    log.warn("[AFK] Gagal kirim notice: {}", ioe.getMessage());
                                                }
                                                return;
                                            }
                                            // Cek gems cukup untuk biaya aktivasi
                                            if (px.getGem() < GEM_COST_ACTIVATE) {
                                                try {
                                                    core.Service.send_notice_box(px.conn, "Gems tidak cukup! Butuh minimal " + GEM_COST_ACTIVATE + " Gems untuk aktifkan Mode AFK.");
                                                } catch (java.io.IOException ioe) {
                                                    log.warn("[AFK] Gagal kirim notice: {}", ioe.getMessage());
                                                }
                                                return;
                                            }
                                            px.modeBot = true;
                                            px.afkBot = new game.afk.AfkBotController(px);
                                            log.info("[AFK] Bot aktif untuk player={} map={} x={} y={} zone={}",
                                                px.name, px.map.mapId, px.x, px.y, px.map.zoneId);
                                            try {
                                                px.conn.close();
                                            } catch (Exception e) {
                                                log.warn("[AFK] Gagal menutup koneksi: {}", e.getMessage());
                                            }
                                        }
                                    })
                                    .build();
                            p.openDialog(afkDialog);
                        })
                        .submenu("● Pemain Online", onlinePlayersMenu())
                        .menu("Close", "Tutup", Player::closeMenuDialog)
                        .build())

                .build();

        openMenu(player, root);
    }

// mod menu
    public static void openModMenu(Player player) {
        Menu root = Menu.builder()
                .npc(-199)
                .title("Lainya")
                .name("Where would you like to go?")
                .menus(MenuHelper
                        .forNpc(-199)
                        .menu("Buka Peti", p -> {
                            if (p.item.total_item_by_id(4, 52) > 0) {
                                MoLy.show_table_to_choose_item(p);
                            } else {
                                Service.send_notice_box(p.conn, "Tidak cukup tiket terbuka dalam inventory");
                            }
                        })
                        .submenu("Drop Settings", dropSettingMenu(player))
                        .submenu("Lepas Fashion", createFashionMenu(player))
                        .submenu("Hide Fashion", createShowHideFashionMenu(player))
                        .menu("Ke Kota", p -> {
                            if (p.conn.ac_admin < 10 && p.item.wear[11] != null && (p.item.wear[11].id == 3599
                                    || p.item.wear[11].id == 3593 || p.item.wear[11].id == 3596)) {
                                Service.send_notice_box(p.conn,
                                        "Kamu tidak bisa kembali ke desa dengan cepat saat memakai jenis armor yang berhubungan dengan fungsi berdagang");
                                return;
                            }
                            Vgo vgo = new Vgo();
                            vgo.toMap = 1;
                            vgo.toX = 432;
                            vgo.toY = 354;
                            p.changeMap(p, vgo);
                        })
                        .menu("Mode AFK", p -> {
                            if (p.modeBot) {
                                game.afk.AfkBotController bot = p.afkBot;
                                String elapsed    = (bot != null) ? bot.getElapsedFormatted()       : "?";
                                long   nextDeduct = (bot != null) ? bot.getMinutesUntilNextDeduct() : 0;
                                long   expGained  = (bot != null) ? bot.getTotalExpGained()         : 0;

                                Dialog stopDialog = Dialog.builder()
                                        .id((byte) -11)
                                        .text("● Mode AFK Sedang Aktif!\n\n" +
                                              "Durasi AFK   : " + elapsed + "\n" +
                                              "Exp didapat  : " + core.Util.number_format(expGained) + "\n" +
                                              "Gems sisa    : " + p.getGem() + "\n" +
                                              "Potong gems  : " + nextDeduct + " menit lagi\n\n" +
                                              "Apakah kamu ingin MENGHENTIKAN mode AFK sekarang?\n" +
                                              "(Karakter akan tetap di map, bot berhenti berburu)")
                                        .onRespond((px, yesOrNo, args) -> {
                                            if (yesOrNo) {
                                                game.afk.AfkBotController activeBot = px.afkBot;
                                                if (px.modeBot && activeBot != null) {
                                                    activeBot.stopBotManual();
                                                    try {
                                                        core.Service.send_notice_box(px.conn,
                                                                "✓ Mode AFK berhasil dihentikan.\n" +
                                                                "Karakter kamu kembali ke mode manual.");
                                                    } catch (java.io.IOException ioe) {
                                                        log.warn("[AFK] Gagal kirim notice stop: {}", ioe.getMessage());
                                                    }
                                                } else {
                                                    try {
                                                        core.Service.send_notice_box(px.conn, "Mode AFK sudah tidak aktif.");
                                                    } catch (java.io.IOException ioe) {
                                                        log.warn("[AFK] Gagal kirim notice: {}", ioe.getMessage());
                                                    }
                                                }
                                            }
                                        })
                                        .build();
                                p.openDialog(stopDialog);
                                return;
                            }
                            // FIX: Validasi posisi karakter sebelum aktifkan bot
                            if (p.map == null) {
                                Service.send_notice_box(p.conn, "Tidak bisa aktivasi AFK — karakter belum masuk map.");
                                return;
                            }
                            if (p.x == 0 && p.y == 0) {
                                Service.send_notice_box(p.conn, "Tidak bisa aktivasi AFK — posisi karakter tidak valid (0,0). Coba pindah dulu.");
                                return;
                            }
                            if (p.map.ismaplang) {
                                Service.send_notice_box(p.conn, "Tidak bisa aktivasi AFK di kota. Pergi ke map berburu terlebih dahulu.");
                                return;
                            }
                            // Cek gems sebelum tampil dialog
                            final int GEM_COST_ACTIVATE = 5000; // biaya aktivasi sekali bayar
                            final int GEM_COST_PER_HOUR = 500;  // biaya per jam setelahnya
                            String gemWarning = p.getGem() >= GEM_COST_ACTIVATE
                                    ? "Gems kamu: " + p.getGem() + " ✓"
                                    : "⚠ Gems kamu: " + p.getGem() + " (kurang! butuh " + GEM_COST_ACTIVATE + ")";

                            Dialog afkDialog = Dialog.builder()
                                    .id((byte) -10)
                                    .text("Aktifkan Mode AFK?\n\n" +
                                          "Biaya Aktivasi: " + GEM_COST_ACTIVATE + " Gems (langsung dipotong)\n" +
                                          "Biaya Lanjutan: " + GEM_COST_PER_HOUR + " Gems / jam\n" +
                                          gemWarning + "\n\n" +
                                          "Karakter akan tetap ONLINE dan berburu otomatis:\n" +
                                          "- Auto attack monster\n" +
                                          "- Auto pickup Gold\n" +
                                          "- Auto pickup Item & Material\n" +
                                          "- Exp & Level tetap bertambah\n\n" +
                                          "Bot akan berhenti otomatis jika gems habis.\n" +
                                          "Koneksi kamu akan diputus, karakter tetap di dunia game.")
                                    .onRespond((px, yesOrNo, args) -> {
                                        if (yesOrNo) {
                                            // Validasi posisi player sebelum aktifkan AFK
                                            if (px.map == null || (px.x == 0 && px.y == 0)) {
                                                try {
                                                    core.Service.send_notice_box(px.conn, "Aktivasi AFK gagal — posisi tidak valid. Coba gerak dulu.");
                                                } catch (java.io.IOException ioe) {
                                                    log.warn("[AFK] Gagal kirim notice: {}", ioe.getMessage());
                                                }
                                                return;
                                            }
                                            // Cek gems cukup untuk biaya aktivasi
                                            if (px.getGem() < GEM_COST_ACTIVATE) {
                                                try {
                                                    core.Service.send_notice_box(px.conn, "Gems tidak cukup! Butuh minimal " + GEM_COST_ACTIVATE + " Gems untuk aktifkan Mode AFK.");
                                                } catch (java.io.IOException ioe) {
                                                    log.warn("[AFK] Gagal kirim notice: {}", ioe.getMessage());
                                                }
                                                return;
                                            }
                                            px.modeBot = true;
                                            px.afkBot = new game.afk.AfkBotController(px);
                                            log.info("[AFK] Bot aktif untuk player={} map={} x={} y={} zone={}",
                                                px.name, px.map.mapId, px.x, px.y, px.map.zoneId);
                                            try {
                                                px.conn.close();
                                            } catch (Exception e) {
                                                log.warn("[AFK] Gagal menutup koneksi: {}", e.getMessage());
                                            }
                                        }
                                    })
                                    .build();
                            p.openDialog(afkDialog);
                        })
                        .submenu("● Pemain Online", onlinePlayersMenu())
                        .menu("Close", "Tutup", Player::closeMenuDialog)
                        .build())

                .build();

        openMenu(player, root);
    }

    /**
     * Submenu daftar pemain yang sedang online.
     * Pemain normal  → "● NamaPlayer (Online)"
     * Pemain AFK bot → "◌ NamaPlayer [AFK]"
     */
    private static List<Menu> onlinePlayersMenu() {
        MenuHelper.SubMenuBuilder builder = MenuHelper.SubMenuBuilder.create(-199);
        java.util.Set<Integer> seen = new java.util.HashSet<>(); // cegah duplikasi per objectId
        int count = 0;
        for (GameMap[] zones : GameMap.entrys) {
            if (zones == null) continue;
            for (GameMap zone : zones) {
                if (zone == null || zone.players == null) continue;
                for (Player op : zone.players) {
                    if (op == null || !op.isOnline) continue;
                    if (!seen.add(op.objectId)) continue; // skip jika objectId sudah masuk
                    String indicator = op.modeBot ? "◌ " : "● ";
                    String suffix    = op.modeBot ? " [AFK]" : " (Online)";
                    String label     = indicator + op.name + suffix;
                    builder.item(label, viewer ->
                            Service.send_notice_box(viewer.conn,
                                    "Pemain : " + op.name + "\n" +
                                    "Level  : " + op.level + "\n" +
                                    "Status : " + (op.modeBot ? "● AFK (Bot aktif)" : "● Online") + "\n" +
                                    "Map    : " + op.map.mapId));
                    if (++count >= 30) break;
                }
                if (count >= 30) break;
            }
            if (count >= 30) break;
        }
        if (count == 0) {
            builder.item("Tidak ada pemain online saat ini", p ->
                    Service.send_notice_box(p.conn, "Belum ada pemain yang online."));
        }
        builder.item("Back", "Kembali", MenuManager::navigateBack);
        return builder.build();
    }

    public static void openRankingMenu(Player player) {
        Menu root = Menu.builder()
                .npc(-32)
                .title("Ranking")
                .name("Pilih papan peringkat yang ingin kamu lihat")
                .menus(MenuHelper
                        .forNpc(-32)
                        .menu("Top Level", p -> {
                            // DIAGNOSTIC SEMENTARA: buat mastiin klik "Top Level" di client beneran
                            // nyampe ke server atau enggak. Kalau baris ini ga pernah muncul di log
                            // pas tombol diklik, berarti masalahnya di CLIENT (packet klik ga terkirim
                            // / hit-area tombol salah), bukan di server. Hapus log ini kalau udah kelar
                            // debugging.
                            log.warn("[DEBUG-TOPLEVEL] Player {} menekan menu Top Level", p.name);
                            BXH.send(p.conn, 0);
                        })
                        .submenu("Top Guild", topGuildMenu())
                        .menu("Top Kill Boss", p -> BXH.send(p.conn, 3))
                        .submenu("Ranking Lainnya", rankingOtherMenu())
                        // .submenu("● Pemain Online", onlinePlayersMenu())
                        .menu("Close", "Tutup", Player::closeMenuDialog)
                        .build())
                .build();

        openMenu(player, root);
    }

    private static List<Menu> topGuildMenu() {
        return MenuHelper
                .SubMenuBuilder
                .create(-32)
                .item("Top Kaya", p -> BXH.send(p.conn, 6))
                .item("Top Guild by Level", p -> BXH.send(p.conn, 1)) // FIX: dulu labelnya "Top Level" juga, sama persis dgn menu root Ranking > Top Level. Nama sekarang dibedakan biar ga ada 2 tombol dgn teks identik yg beda tujuan/handler.
                .item("Top Gems Guild", p -> BXH.send(p.conn, 7))
                .item("Back", "Kembali", MenuManager::navigateBack)
                .build();
    }

    private static List<Menu> rankingOtherMenu() {
        return MenuHelper
                .SubMenuBuilder
                .create(-32)
                .item("Top Pasangan", p -> BXH.send(p.conn, 2))
                .item("Top Dagang", p -> BXH.send(p.conn, 5))
                .item("Top Rampok", p -> BXH.send(p.conn, 4))
                .item("Top Knight  (Segera)", p ->
                        Service.send_notice_box(p.conn, "Fitur Top Knight akan hadir di update berikutnya!"))
                .item("Back", "Kembali", MenuManager::navigateBack)
                .build();
    }

    private static List<Menu> createShowHideFashionMenu(Player p) {
        MenuHelper.SubMenuBuilder helper = MenuHelper
                .SubMenuBuilder
                .create(-199);

        FashionSetting setting = p.fashionSetting;
        if (p.item.wear[13] != null) {
            helper.item(String.format("%s %s", p.item.wear[13].name, setting.isMask() ? "[ON]" : "[OFF]"), p1 -> {

                setting.setMask(!setting.isMask());
                Service.sendMainCharInfo(p1);
                MapService.broadcastMainCharInfo(p1.map, p1);
            });

        }
        if (p.item.wear[14] != null) {
            helper.item(String.format("%s %s", p.item.wear[14].name, setting.isWing() ? "[ON]" : "[OFF]"), p1 -> {

                setting.setWing(!setting.isWing());
                Service.sendMainCharInfo(p1);
                MapService.broadcastMainCharInfo(p1.map, p1);
            });

        }
        if (p.item.wear[15] != null) {
            helper.item(String.format("%s %s", p.item.wear[15].name, setting.isCloak() ? "[ON]" : "[OFF]"), p1 -> {


                setting.setCloak(!setting.isCloak());
                Service.sendMainCharInfo(p1);
                MapService.broadcastMainCharInfo(p1.map, p1);
            });

        }
        if (p.item.wear[19] != null) {
            helper.item(String.format("%s %s", p.item.wear[19].name, setting.isTitle() ? "[ON]" : "[OFF]"), p1 -> {

                setting.setTitle(!setting.isTitle());
                Service.sendMainCharInfo(p1);
                MapService.broadcastMainCharInfo(p1.map, p1);
            });

        }
        if (p.item.wear[16] != null) {
            helper.item(String.format("%s %s", p.item.wear[16].name, setting.isHair() ? "[ON]" : "[OFF]"), p1 -> {

                setting.setHair(!setting.isHair());
                Service.sendMainCharInfo(p1);
                MapService.broadcastMainCharInfo(p1.map, p1);
            });

        }
        if (p.item.wear[17] != null) {
            helper.item(String.format("%s %s", p.item.wear[17].name, setting.isWeapon() ? "[ON]" : "[OFF]"), p1 -> {

                setting.setWeapon(!setting.isWeapon());
                Service.sendMainCharInfo(p1);
                MapService.broadcastMainCharInfo(p1.map, p1);
            });

        }

        helper.item("Back", "Kembali", MenuManager::navigateBack);

        return helper.build();

    }

    private static List<Menu> dropSettingMenu(Player player) {
        return MenuHelper
                .SubMenuBuilder
                .create(-199)
                .item(String.format("Material Medal %s", player.isDropMaterialMedal ? "[ON]" : "[OFF]"), p -> {
                    p.isDropMaterialMedal = !p.isDropMaterialMedal;
                    Service.send_notice_box(p.conn,
                            String.format("Drop Material medal %s", p.isDropMaterialMedal ? " telah aktif" : "tidak aktif"));


                })
                .item(String.format("Material Sayap %s", player.isDropMaterialWing ? "[ON]" : "[OFF]"), p -> {
                    p.isDropMaterialWing = !p.isDropMaterialWing;
                    Service.send_notice_box(p.conn,
                            String.format("Drop material sayap %s", p.isDropMaterialWing ? " telah aktif" : "tidak aktif"));


                })
                .item(String.format("Material Upgrade %s", player.isDropMaterialUpgrade ? "[ON]" : "[OFF]"), p -> {
                    p.isDropMaterialUpgrade = !p.isDropMaterialUpgrade;
                    Service.send_notice_box(p.conn,
                            String.format("Drop material upgrade %s", p.isDropMaterialUpgrade ? " telah aktif" : "tidak aktif"));


                })
                .item(String.format("Equipment %s", player.isDropEquipment ? "[ON]" : "[OFF]"), p -> {
                    p.isDropEquipment = !p.isDropEquipment;
                    Service.send_notice_box(p.conn,
                            String.format("Drop equipment %s", p.isDropEquipment ? " telah aktif" : "tidak aktif"));


                })
                .item("Back", "Kembali", MenuManager::navigateBack)
                .build();
    }


    public static void openAdminMenu(Player player) {
        Menu root = Menu.builder()
                .npc(-200)
                .title("GM Menu")
                .name("Where would you like to go?")
                .menus(MenuHelper.forNpc(-200)
                        .submenu("Game Helper", adminGameFunctions())
                        .submenu("Server Helper", adminUtilityMenu())
                        .submenu("Item Helper", adminUserManagement())
                        .submenu("Lepas Fashion", createFashionMenu(player))
                        .submenu("Hide Fashion", createShowHideFashionMenu(player))
                        .menu("Topup", p -> {
                            InputDialog dialog = InputDialog
                                    .builder()
                                    .title("Donasi")
                                    .fields(List.of("Nickname", "Type(1=GOLD 2=GEM", "Jumlah"))
                                    .action((px, arr) -> {
                                        try {
                                            String name = arr[0];
                                            byte type = Byte.parseByte(arr[1]);
                                            long amount = Long.parseLong(arr[2]);

                                            SQLHelper
                                                    .insert("user_topup")
                                                    .value("name", name)
                                                    .value("jumlah", amount)
                                                    .value("type", type == 1 ? TopupType.GOLD.name() : TopupType.GEM.name())
                                                    .value("status", Status.PENDING.name())
                                                    .execute();

                                            px.sendNoticeBox("Berhasil");

                                        } catch (Exception e) {
                                            px.sendNoticeBox("Terjadi kesalahan");
                                        }

                                    }).build();

                            p.openInput(dialog);
                        })
                        .menu("Close", "Tutup", Player::closeMenuDialog)
                        .build())
                .build();

        MenuManager.openMenu(player, root);
    }

    /**
     * Menu terbatas buat SMOD (ac_admin level 1) — cuma tool moderasi ringan
     * yang aman dipegang staff junior: cek info player, teleport ke/panggil
     * player, kirim peringatan. TIDAK ada akses kirim item, ubah rate exp,
     * reload database, atau apapun yang bisa dipake buat cheat/nyuri barang.
     * Full GM Menu (openAdminMenu) tetap khusus ac_admin >= 10.
     */
    public static void openSmodMenu(Player player) {
        Menu root = Menu.builder()
                .npc(-200)
                .title("SMOD Menu")
                .name("Where would you like to go?")
                .menus(MenuHelper.forNpc(-200)
                        .menu("Cek Info Player", p -> {
                            InputDialog dialog = InputDialog
                                    .builder()
                                    .title("Cek Info Player")
                                    .fields(List.of("Nickname"))
                                    .action((px, arr) -> {
                                        String name = arr[0];
                                        Player target = Manager.getPlayerByName(name);
                                        if (target == null || !target.isOnline) {
                                            px.sendNoticeBox("Player tidak ditemukan atau sedang offline");
                                            return;
                                        }
                                        px.sendNoticeBox(String.format(
                                                "Nama: %s\nLevel: %d\nMap: %d\nPosisi: %d, %d",
                                                target.name, target.level,
                                                target.map != null ? target.map.mapId : -1,
                                                target.x, target.y));
                                    }).build();

                            p.openInput(dialog);
                        })
                        .menu("Teleport ke Player", p -> {
                            InputDialog dialog = InputDialog
                                    .builder()
                                    .title("Teleport ke Player")
                                    .fields(List.of("Nickname"))
                                    .action((px, arr) -> {
                                        try {
                                            String name = arr[0];
                                            Player target = Manager.getPlayerByName(name);
                                            if (target == null || !target.isOnline || target.map == null) {
                                                px.sendNoticeBox("Player tidak ditemukan atau sedang offline");
                                                return;
                                            }
                                            px.changeMap(px, Vgo.create(target.map.mapId, target.x, target.y));
                                        } catch (Exception e) {
                                            px.sendNoticeBox("Terjadi kesalahan");
                                        }
                                    }).build();

                            p.openInput(dialog);
                        })
                        .menu("Panggil Player", p -> {
                            InputDialog dialog = InputDialog
                                    .builder()
                                    .title("Panggil Player")
                                    .fields(List.of("Nickname"))
                                    .action((px, arr) -> {
                                        try {
                                            String name = arr[0];
                                            Player target = Manager.getPlayerByName(name);
                                            if (target == null || !target.isOnline || px.map == null) {
                                                px.sendNoticeBox("Player tidak ditemukan atau sedang offline");
                                                return;
                                            }
                                            target.changeMap(target, Vgo.create(px.map.mapId, px.x, px.y));
                                            px.sendNoticeBox("Player berhasil dipanggil");
                                        } catch (Exception e) {
                                            px.sendNoticeBox("Terjadi kesalahan");
                                        }
                                    }).build();

                            p.openInput(dialog);
                        })
                        .menu("Kirim Peringatan", p -> {
                            InputDialog dialog = InputDialog
                                    .builder()
                                    .title("Kirim Peringatan")
                                    .fields(List.of("Nickname", "Pesan"))
                                    .action((px, arr) -> {
                                        String name = arr[0];
                                        String message = arr[1];
                                        Player target = Manager.getPlayerByName(name);
                                        if (target == null || !target.isOnline) {
                                            px.sendNoticeBox("Player tidak ditemukan atau sedang offline");
                                            return;
                                        }
                                        target.sendNoticeBox("[Peringatan dari Moderator]\n" + message);
                                        px.sendNoticeBox("Peringatan berhasil dikirim");
                                    }).build();

                            p.openInput(dialog);
                        })
                        .menu("Close", "Tutup", Player::closeMenuDialog)
                        .build())
                .build();

        MenuManager.openMenu(player, root);
    }

    private static List<Menu> adminGameFunctions() {
        return MenuHelper
                .SubMenuBuilder
                .create(-200)
                .submenu("Ubah Role", changeRoleMenu())
                .item("Goto", p -> {
                    InputDialog dialog = InputDialog
                            .builder()
                            .title("Teleport")
                            .fields(List.of("Map Id", "x", "y"))
                            .action((px, arr) -> {
                                try {
                                    int mapId = Integer.parseInt(arr[0]);
                                    short x = Short.parseShort(arr[1]);
                                    short y = Short.parseShort(arr[2]);

                                    Vgo go = new Vgo();
                                    go.toMap = (byte) mapId;
                                    go.x = px.x;
                                    go.toY = px.y;
                                    go.toX = (short) (x * 24);
                                    go.toY = (short) (y * 24);
                                    px.changeMap(px, go);
                                } catch (Exception e) {
                                    px.sendNoticeBox("Input bukan angka");
                                }

                            }).build();

                    p.openInput(dialog);

                })
                .item("Get Eggs", p -> {
                    PetManager.getInstance().getAll().forEach(petData -> {

                        Item3 tmp = Item3.fromTemplate((short) petData.getId());
                        p.item.add_item_bag3(tmp);
                        p.item.updateBag();

                    });
                })
                .item("Set Pet Level", p -> {
                    InputDialog dialog = InputDialog
                            .builder()
                            .title("Set Pet Level")
                            .fields(List.of("Level tujuan (1-30)"))
                            .action((px, arr) -> {
                                try {
                                    int targetLevel = Integer.parseInt(arr[0].trim());
                                    if (targetLevel < 1 || targetLevel > 30) {
                                        px.sendNoticeBox("Level harus di antara 1-30");
                                        return;
                                    }
                                    if (px.pet_follow == -1 || px.mypet == null) {
                                        px.sendNoticeBox("Belum membawa pet!");
                                        return;
                                    }

                                    Pet pet = null;
                                    for (Pet temp : px.mypet) {
                                        if (temp.is_follow) {
                                            pet = temp;
                                        }
                                    }
                                    if (pet == null) {
                                        px.sendNoticeBox("Belum membawa pet!");
                                        return;
                                    }

                                    while (pet.level < targetLevel) {
                                        pet.UpgradeLevel();
                                        // sama seperti item revolusi (case 69 di UseItem): naik wujud di level 9 & 19
                                        if (pet.level == 9 || pet.level == 19) {
                                            pet.spriteImage++;
                                        }
                                    }
                                    pet.exp = 0;

                                    Service.sendPlayerWear(px);
                                    px.sendNoticeBox("Pet '" + pet.name + "' sekarang level " + pet.level
                                            + " (sprite=" + pet.spriteImage + ")");
                                } catch (NumberFormatException e) {
                                    px.sendNoticeBox("Input level harus berupa angka");
                                } catch (Exception e) {
                                    px.sendNoticeBox("Terjadi kesalahan");
                                }
                            }).build();

                    p.openInput(dialog);
                })
                .item("Create Clone", p -> {
                    PlayerBot bot = PlayerBot.copyFrom(p);
                    bot.getData().setName(p.name + " Clone");
                    AiManager.getInstance().addBot(bot);
                    p.map.broadcast(bot::move);

                    p.sendNoticeBox("Clone Created");
                })
                .item("Clear Inventory", p -> {
                    p.item.clearBag();
                    p.sendNoticeBox("Berhasil Dibersihkan");
                })
                .item("Back", "Kembali", MenuManager::navigateBack)
                .build();
    }

    private static List<Menu> adminUserManagement() {
        return MenuHelper
                .SubMenuBuilder
                .create(-200)
                .item("Kirim Item", p -> {
                    InputDialog dialog = InputDialog
                            .builder()
                            .title("Kirim Item")
                            .fields(List.of("Nickname", "Category(3,4,7)", "Item id", "Jumlah"))
                            .action((px, arr) -> {

                                try {
                                    String name = arr[0];
                                    byte category = Byte.parseByte(arr[1]);
                                    short itemId = Short.parseShort(arr[2]);
                                    short quantity = Short.parseShort(arr[3]);

                                    Player target = Manager.getPlayerByName(name);
                                    if (target == null) {
                                        px.sendNoticeBox("Target tidak ditemukan, pastikan target sedang dalam game");
                                        return;
                                    }
                                    if (!target.isOnline) {
                                        px.sendNoticeBox("Target tidak berada dalam game");
                                        return;
                                    }

                                    List<Short> items = new ArrayList<>();
                                    List<Integer> quantities = new ArrayList<>();
                                    List<Short> categories = new ArrayList<>();

                                    switch (category) {
                                        case 3 -> {

                                            Item3 eq = Item3.fromTemplate(itemId);
                                            if (eq == null) {
                                                px.sendNoticeBox("Item tidak ditemukan");
                                                return;
                                            }

                                            items.add(itemId);
                                            quantities.add((int) quantity);
                                            categories.add((short) category);
                                            target.item.add_item_bag3(eq);
                                        }
                                        case 4, 7 -> {
                                            if (!Item47.isExist(itemId, category)) {
                                                px.sendNoticeBox("Item tidak ditemukan");
                                                return;
                                            }

                                            items.add(itemId);
                                            quantities.add((int) quantity);
                                            categories.add((short) category);
                                            target.item.add_item_bag47(itemId, quantity, category);
                                        }
                                    }

                                    short[] ar_id = new short[items.size()];
                                    int[] ar_quant = new int[quantities.size()];
                                    short[] ar_type = new short[categories.size()];
                                    for (int i = 0; i < ar_id.length; i++) {
                                        ar_id[i] = items.get(i);
                                        ar_quant[i] = quantities.get(i);
                                        ar_type[i] = categories.get(i);
                                    }

                                    Service.Show_open_box_notice_item(target, "Hadiah dari admin", ar_id, ar_quant, ar_type);
                                } catch (Exception e) {
                                    px.sendNoticeBox("Terjadi kesalahan");
                                }

                            }).build();

                    p.openInput(dialog);
                })
                .item("Kirim Item Durasi", p -> {
                    InputDialog dialog = InputDialog
                            .builder()
                            .title("Kirim Item Durasi")
                            .fields(List.of("Nickname", "Item id", "Plus(1-15)", "Durasi (Jam)"))
                            .action((px, arr) -> {

                                try {
                                    String name = arr[0];
                                    short itemId = Short.parseShort(arr[1]);
                                    byte plus = Byte.parseByte(arr[2]);
                                    long duration = Long.parseLong(arr[3]);

                                    Player target = Manager.getPlayerByName(name);
                                    if (target == null) {
                                        px.sendNoticeBox("Target tidak ditemukan, pastikan target sedang dalam game");
                                        return;
                                    }
                                    if (!target.isOnline) {
                                        px.sendNoticeBox("Target tidak berada dalam game");
                                        return;
                                    }

                                    List<Short> items = new ArrayList<>();
                                    List<Integer> quantities = new ArrayList<>();
                                    List<Short> categories = new ArrayList<>();


                                    Item3 eq = Item3.fromTemplate(itemId);
                                    if (eq == null) {
                                        px.sendNoticeBox("Item tidak ditemukan");
                                        return;
                                    }
                                    eq.tier = (byte) Math.max(plus, 15);
                                    eq.expiry_date = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(duration);
                                    items.add(itemId);
                                    quantities.add(1);
                                    categories.add((short) 3);
                                    target.item.add_item_bag3(eq);


                                    short[] ar_id = new short[items.size()];
                                    int[] ar_quant = new int[quantities.size()];
                                    short[] ar_type = new short[categories.size()];
                                    for (int i = 0; i < ar_id.length; i++) {
                                        ar_id[i] = items.get(i);
                                        ar_quant[i] = quantities.get(i);
                                        ar_type[i] = categories.get(i);
                                    }

                                    Service.Show_open_box_notice_item(target, "Hadiah dari admin", ar_id, ar_quant, ar_type);
                                } catch (Exception e) {
                                    px.sendNoticeBox("Terjadi kesalahan");
                                }

                            }).build();

                    p.openInput(dialog);
                })
                .item("Cari Item", p -> {
                    InputDialog dialog = InputDialog
                            .builder()
                            .title("Cari Item")
                            .fields(List.of("Category(3,4,7)", "Item Id", "Quantity(Max 32K)"))
                            .action((px, arr) -> {
                                try {
                                    int category = Integer.parseInt(arr[0]);
                                    short id = Short.parseShort(arr[1]);
                                    short quantity = Short.parseShort(arr[2]);

                                    switch (category) {
                                        case 3 -> {
                                            Item3 item = Item3.fromTemplate(id);
                                            if (item == null) {
                                                Service.send_notice_nobox_white(p.conn, "Item tidak ditemukan");
                                                return;
                                            }

                                            px.item.add_item_bag3(item);
                                            px.item.charInventory(category);
                                            Service.send_notice_nobox_white(p.conn, String.format("%s ditambahkan ke inentory", item.name));
                                        }
                                        case 4, 7 -> {
                                            if (!Item47.isExist(id, (byte) category)) {
                                                px.sendNoticeBox("Item tidak ditemukan");
                                                return;
                                            }

                                            px.item.add_item_bag47(id, quantity, (byte) category);
                                            px.item.charInventory(category);
                                        }
                                    }
                                } catch (Exception e) {
                                    px.sendNoticeBox("Input bukan angka");
                                }

                            }).build();

                    p.openInput(dialog);
                })

                .item("Kurangi Gems", p -> {
                    InputDialog dialog = InputDialog
                            .builder()
                            .title("Kurangi Gems")
                            .fields(List.of("Nickname", "Jumlah"))
                            .action((px, arr) -> {

                                try {
                                    String name = arr[0];
                                    if (!Util.isnumber(arr[1])) {
                                        px.sendNoticeBox("Jumlah harus berupa angka");
                                        return;
                                    }
                                    long amount = Long.parseLong(arr[1]);

                                    if (amount <= 0) {
                                        px.sendNoticeBox("Jumlah harus lebih dari 0");
                                        return;
                                    }

                                    Player target = Manager.getPlayerByName(name);
                                    if (target == null) {
                                        px.sendNoticeBox("Target tidak ditemukan, pastikan target sedang dalam game");
                                        return;
                                    }
                                    if (!target.isOnline) {
                                        px.sendNoticeBox("Target tidak berada dalam game");
                                        return;
                                    }

                                    // Sengaja TIDAK di-clamp ke sisa gems target -- kalau gems-nya
                                    // udah kepake duluan, GM tetap mau motong penuh sesuai jumlah
                                    // yang diminta (boleh jadi minus di database).
                                    long reduceBy = amount;

                                    // updateGem() otomatis kirim paket update gold/gem (opcode 16)
                                    // ke koneksi target, jadi client-nya langsung ke-update real-time
                                    // tanpa perlu target relog atau pindah map.
                                    target.updateGem(-reduceBy);

                                    px.sendNoticeBox(String.format(
                                            "Berhasil mengurangi %d gems milik %s (sisa: %d)",
                                            reduceBy, target.name, target.getGem()));
                                    target.sendNoticeBox(String.format(
                                            "%d gems kamu dikurangi oleh GM. Sisa gems: %d",
                                            reduceBy, target.getGem()));
                                } catch (Exception e) {
                                    px.sendNoticeBox("Terjadi kesalahan");
                                }

                            }).build();

                    p.openInput(dialog);
                })

                .item("Back", "Kembali", MenuManager::navigateBack)
                .build();
    }

    private static List<Menu> adminUtilityMenu() {
        return MenuHelper
                .SubMenuBuilder
                .create(-200)
                .item("Matikan Server", p -> {
                    Dialog dialog = Dialog
                            .builder()
                            .id((byte) -3)
                            .text("Server akan dimatikan setelah 1 menit, lanjutan?")
                            .onRespond((px, yOrN, args) -> {
                                try {
                                    if (yOrN) {

                                        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                                        Service.send_notice_nobox_yellow(p.conn, "Perhatian server akan dimatikan dalam 1 menit");
                                        scheduler.schedule(() -> {
                                            try {

                                                SaveData.process();
                                                ServerManager.gI().close();
                                                for (int k = Session.SESSION_LIST.size() - 1; k >= 0; k--) {

                                                    try {
                                                        Session.SESSION_LIST.get(k).p = null;
                                                        Session.SESSION_LIST.get(k).close();
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                                Manager.gI().close();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }, 1, TimeUnit.MINUTES);
                                        scheduler.shutdown();

                                    }
                                } catch (Exception e) {
                                    log.error("Unhandled excepttion {} ", e.getMessage(), e);
                                }
                            })
                            .build();
                    p.openDialog(dialog);
                })
                .item("Setting Exp", p -> {
                    InputDialog dialog = InputDialog
                            .builder()
                            .title("1 = x1")
                            .fields(List.of("XP"))
                            .action((px, arr) -> {

                                try {
                                    int value = Integer.parseInt(arr[0]);
                                    if (value < 1) {
                                        px.sendNoticeBox("Nilai harus minimal 1");
                                        return;
                                    }
                                    // REVERT: balik ke skala persen seperti semula — admin
                                    // ketik "1" (maksud x1), disimpan sebagai "100" (dibaca
                                    // sebagai persen di Player.updateExp(), 100 = x1 normal).
                                    Manager.gI().exp = value * 100;
                                    px.sendNoticeBox(String.format("Server XP berhasil di ubah menjadi x%s", value));
                                } catch (NumberFormatException e) {
                                    px.sendNoticeBox("Input bukan angka");
                                }

                            }).build();

                    p.openInput(dialog);
                })
                .item("Update Database", p -> {
                    if (Manager.gI().reloadItemTemplate3()) {
                        p.sendNoticeBox("Berhasil Diperbaharui");
                    }
                })
                .item("Reload BTF Shop", p -> {
                    ShopManager.getInstance().load();
                    p.sendNoticeBox("Berhasil Diperbaharui");
                })
                .item("Reload Tabel Messager", p -> {
                    int count = Manager.gI().reloadNotif();
                    if (count >= 0) {
                        p.sendNoticeBox("✅ Reload Tabel Messager berhasil!\nTotal " + count + " notifikasi dimuat dari database.");
                    } else {
                        p.sendNoticeBox("❌ Gagal reload tabel messager. Cek log server, data lama tetap dipakai.");
                    }
                })
                .item("Buka Wave Event", p -> {
                    if (p.conn.ac_admin < 5) {
                        p.sendNoticeBox("Anda tidak memiliki cukup izin!");
                        return;
                    }
                    PersimpanganKematian event = GameEventManager.gI().getEvent(PersimpanganKematian.class);
                    if (event == null) {
                        p.sendNoticeBox("Event Persimpangan Kematian tidak ditemukan.");
                        return;
                    }
                    p.sendNoticeBox(event.adminForceStart());
                })
                .item("Tutup Wave Event", p -> {
                    if (p.conn.ac_admin < 5) {
                        p.sendNoticeBox("Anda tidak memiliki cukup izin!");
                        return;
                    }
                    PersimpanganKematian event = GameEventManager.gI().getEvent(PersimpanganKematian.class);
                    if (event == null) {
                        p.sendNoticeBox("Event Persimpangan Kematian tidak ditemukan.");
                        return;
                    }
                    p.sendNoticeBox(event.adminForceEnd());
                })
                .item("Reload Wave Event (dari DB)", p -> {
                    if (p.conn.ac_admin < 5) {
                        p.sendNoticeBox("Anda tidak memiliki cukup izin!");
                        return;
                    }
                    PersimpanganKematian event = GameEventManager.gI().getEvent(PersimpanganKematian.class);
                    if (event == null) {
                        p.sendNoticeBox("Event Persimpangan Kematian tidak ditemukan.");
                        return;
                    }
                    p.sendNoticeBox(event.adminReloadWaves());
                })
                .item("Buka Gerbang Arwah (Halloween)", p -> {
                    if (p.conn.ac_admin < 5) {
                        p.sendNoticeBox("Anda tidak memiliki cukup izin!");
                        return;
                    }
                    GerbangArwah event = GameEventManager.gI().getEvent(GerbangArwah.class);
                    if (event == null) {
                        p.sendNoticeBox("Event Gerbang Arwah tidak ditemukan.");
                        return;
                    }
                    p.sendNoticeBox(event.adminForceStart());
                })
                .item("Tutup Gerbang Arwah (Halloween)", p -> {
                    if (p.conn.ac_admin < 5) {
                        p.sendNoticeBox("Anda tidak memiliki cukup izin!");
                        return;
                    }
                    GerbangArwah event = GameEventManager.gI().getEvent(GerbangArwah.class);
                    if (event == null) {
                        p.sendNoticeBox("Event Gerbang Arwah tidak ditemukan.");
                        return;
                    }
                    p.sendNoticeBox(event.adminForceEnd());
                })
                .item("Reload Wave Gerbang Arwah (dari DB)", p -> {
                    if (p.conn.ac_admin < 5) {
                        p.sendNoticeBox("Anda tidak memiliki cukup izin!");
                        return;
                    }
                    GerbangArwah event = GameEventManager.gI().getEvent(GerbangArwah.class);
                    if (event == null) {
                        p.sendNoticeBox("Event Gerbang Arwah tidak ditemukan.");
                        return;
                    }
                    p.sendNoticeBox(event.adminReloadWaves());
                })
                .item("Broadcast Server Wide", p -> {
                    if (p.conn.ac_admin < 4) {
                        p.sendNoticeBox("Anda tidak memiliki cukup izin!");
                        return;
                    }
                    InputDialog dialog = InputDialog
                            .builder()
                            .title("📢 Broadcast Server Wide")
                            .fields(List.of("Tipe (1=Kotak, 2=Kuning, 3=Putih)", "Pesan"))
                            .action((px, arr) -> {
                                try {
                                    int tipe = Integer.parseInt(arr[0].trim());
                                    String pesan = arr[1].trim();

                                    if (pesan.isEmpty()) {
                                        px.sendNoticeBox("Pesan tidak boleh kosong!");
                                        return;
                                    }
                                    if (pesan.length() > 200) {
                                        px.sendNoticeBox("Pesan terlalu panjang! Maksimal 200 karakter.");
                                        return;
                                    }
                                    if (tipe < 1 || tipe > 3) {
                                        px.sendNoticeBox("Tipe tidak valid!\n1=Kotak  2=Kuning  3=Putih");
                                        return;
                                    }

                                    String pesanFinal = "[Admin] " + pesan;
                                    int jumlah = 0;
                                    for (int i = 0; i < Session.SESSION_LIST.size(); i++) {
                                        Session target = Session.SESSION_LIST.get(i);
                                        if (target == null || target.p == null) continue;
                                        try {
                                            switch (tipe) {
                                                case 1 -> Service.send_notice_box(target, pesanFinal);
                                                case 2 -> Service.send_notice_nobox_yellow(target, pesanFinal);
                                                case 3 -> Service.send_notice_nobox_white(target, pesanFinal);
                                            }
                                            jumlah++;
                                        } catch (Exception ignored) {}
                                    }
                                    Log.gI().add_log(px.name, "BROADCAST [Tipe=" + tipe + "] ke " + jumlah + " pemain: " + pesan);
                                    px.sendNoticeBox("✅ Broadcast terkirim ke " + jumlah + " pemain online.\nTipe: "
                                            + (tipe == 1 ? "Kotak" : tipe == 2 ? "Kuning" : "Putih")
                                            + "\nPesan: " + pesan);
                                } catch (NumberFormatException e) {
                                    px.sendNoticeBox("Tipe harus angka 1, 2, atau 3!");
                                } catch (Exception e) {
                                    px.sendNoticeBox("Terjadi kesalahan: " + e.getMessage());
                                }
                            }).build();
                    p.openInput(dialog);
                })
                .item("Pesan ke Pemain", p -> {
                    if (p.conn.ac_admin < 4) {
                        p.sendNoticeBox("Anda tidak memiliki cukup izin!");
                        return;
                    }
                    InputDialog dialog = InputDialog
                            .builder()
                            .title("✉ Pesan ke Pemain")
                            .fields(List.of("Nama Karakter", "Tipe (1=Kotak, 2=Kuning, 3=Putih)", "Pesan"))
                            .action((px, arr) -> {
                                try {
                                    String namaTarget = arr[0].trim();
                                    int tipe = Integer.parseInt(arr[1].trim());
                                    String pesan = arr[2].trim();

                                    if (namaTarget.isEmpty()) {
                                        px.sendNoticeBox("Nama karakter tidak boleh kosong!");
                                        return;
                                    }
                                    if (pesan.isEmpty()) {
                                        px.sendNoticeBox("Pesan tidak boleh kosong!");
                                        return;
                                    }
                                    if (pesan.length() > 200) {
                                        px.sendNoticeBox("Pesan terlalu panjang! Maksimal 200 karakter.");
                                        return;
                                    }
                                    if (tipe < 1 || tipe > 3) {
                                        px.sendNoticeBox("Tipe tidak valid!\n1=Kotak  2=Kuning  3=Putih");
                                        return;
                                    }

                                    Player target = Manager.getPlayerByName(namaTarget);
                                    if (target == null || target.conn == null) {
                                        px.sendNoticeBox("❌ Pemain \"" + namaTarget + "\" tidak ditemukan atau sedang offline.");
                                        return;
                                    }

                                    String pesanFinal = "[Admin] " + pesan;
                                    switch (tipe) {
                                        case 1 -> Service.send_notice_box(target.conn, pesanFinal);
                                        case 2 -> Service.send_notice_nobox_yellow(target.conn, pesanFinal);
                                        case 3 -> Service.send_notice_nobox_white(target.conn, pesanFinal);
                                    }
                                    Log.gI().add_log(px.name, "PESAN_PRIBADI [Tipe=" + tipe + "] ke " + namaTarget + ": " + pesan);
                                    px.sendNoticeBox("✅ Pesan berhasil dikirim ke " + namaTarget + ".\nTipe: "
                                            + (tipe == 1 ? "Kotak" : tipe == 2 ? "Kuning" : "Putih")
                                            + "\nPesan: " + pesan);
                                } catch (NumberFormatException e) {
                                    px.sendNoticeBox("Tipe harus angka 1, 2, atau 3!");
                                } catch (Exception e) {
                                    px.sendNoticeBox("Terjadi kesalahan: " + e.getMessage());
                                }
                            }).build();
                    p.openInput(dialog);
                })
                .item("Back", "Kembali", MenuManager::navigateBack)
                .build();
    }

    private static List<Menu> changeRoleMenu() {
        return MenuHelper
                .SubMenuBuilder
                .create(-200)
                .item("Warrior", p -> {
                    p.clazz = 0;
                    Service.send_skill(p);
                    Service.sendMainCharInfo(p);
                    MapService.broadcastMainCharInfo(p.map, p);
                })
                .item("Assasin", p -> {
                    p.clazz = 1;
                    Service.send_skill(p);
                    Service.sendMainCharInfo(p);
                    MapService.broadcastMainCharInfo(p.map, p);
                })
                .item("Mage", p -> {
                    p.clazz = 2;
                    Service.send_skill(p);
                    Service.sendMainCharInfo(p);
                    MapService.broadcastMainCharInfo(p.map, p);
                })
                .item("Gunner", p -> {
                    p.clazz = 3;
                    Service.send_skill(p);
                    Service.sendMainCharInfo(p);
                    MapService.broadcastMainCharInfo(p.map, p);
                })
                .item("Back", "Kembali", MenuManager::navigateBack)
                .build();
    }

    public static void openAnnaMenu(Player player) {
        Menu root = Menu.builder()
                .npc(-200)
                .title("Miss Anna")
                .name("Where would you like to go?")
                .menus(MenuHelper.forNpc(-200)
                        .menu("Giftcode", p -> {
                            Service.sendBoxInputText(p.conn, 0, "Masukkan kode", new String[]{"Kode"});
                        })
                        .menu("Terima Kompensasi", p -> {
                            List<ItemEntry> entries = CompensationManager.gI().claimAllCompensations(p.name);
                            if (entries.isEmpty()) {
                                p.sendNoticeBox("Tidak ada hadiah untukmu.");
                                return;
                            }


                            List<ItemReward> rewards = new ArrayList<>();
                            entries.forEach(itemEntry -> {
                                RoleItems item = itemEntry.getByClazz(p.clazz);
                                List<Item3> equip = item.getElemental();
                                if (equip != null && !equip.isEmpty()) {
                                    equip.forEach(item3 -> {
                                        item3.islock = true;
                                        item3.expiry_date = item3.duration > 0 ? System.currentTimeMillis() + TimeUnit.HOURS.toMillis(item3.duration) : 0;
                                    });
                                }

                                rewards.add(new ItemReward(equip, itemEntry.getPotions(), itemEntry.getMaterials(), 0, itemEntry.getGold(), itemEntry.getGems()));
                            });

                            if (!rewards.isEmpty()) {
                                rewards.forEach(itemReward -> ItemManager.getInstance().sendReward(p.conn, itemReward));
                            }
                        })
                        .menu("Close", "Tutup", Player::closeMenuDialog)
                        .build())
                .build();

        openMenu(player, root);
    }

    public static List<Menu> createFashionMenu(Player player) {
        MenuHelper.SubMenuBuilder helper = MenuHelper
                .SubMenuBuilder
                .create(-199);


        for (int i = 10; i < 24; i++) {
            Item3 wear = player.item.wear[i];
            if (wear == null) continue;

            if (wear.type == 16) continue;

            helper.item(
                    wear.name,
                    Map.of("item", wear, "index", i),
                    (p, args) -> {
                        try {

                            Item3 item = (Item3) args.get("item");
                            int index = (int) args.get("index");

                            p.item.wear[index] = null;
                            p.item.add_item_bag3(item);

                            if (index == 21) {
                                p.mount = null;
                                p.map.sendUseMount(p);
                            }
                            Service.sendPlayerWear(p);
                            Service.sendMainCharInfo(p);
                            MapService.broadcastMainCharInfo(p.map, p);
                            p.item.charInventory(3);
                            Service.send_notice_nobox_white(p.conn, String.format("%s berhasil dilepas", item.name));
                        } catch (Exception ignore) {
                        }
                    }
            );
        }
        helper.item("Back", "Kembali", MenuManager::navigateBack);

        return helper.build();
    }


    public static void openOtherMenu(Player player, Message m) throws IOException {
        byte type = m.reader().readByte();
        byte category = m.reader().readByte();
        short index = m.reader().readShort();

        if (category != 3) {
            return;
        }

        Item3 item = player.item.bag3[index];
        if (item == null) {
            return;
        }

        if (type == 6 && item.time_use > 0) {
            long price = item.time_use - System.currentTimeMillis();
            price /= 3_600_000;
            price = (price > 4) ? (price + 1) : 5;

            long finalPrice = price;
            Dialog dialog = Dialog
                    .builder()
                    .id((byte) -1)
                    .text(String.format("Mempercepat upgrade akan memotong %d gem, lanjutkan?", price))
                    .args(Map.of("item", item))
                    .onRespond((p, yOrN, args) -> {
                        try {
                            if (yOrN) {
                                if (p.getGem() < finalPrice) {
                                    p.sendNoticeBox("Kamu tidak memiliki cukup permata");
                                    return;
                                }

                                Item3 it = (Item3) args.get("item");

                                it.time_use = 0;
                                p.updateGem(-finalPrice);
                                p.item.charInventory(3);
                                p.item.charInventory(4);

                            }
                        } catch (Exception e) {
                            log.error("Unhandled excepttion {} ", e.getMessage(), e);
                        }
                    })
                    .build();

            player.openDialog(dialog);
        } else {

            Menu root = Menu.builder()
                    .npc(-2)
                    .title("Item Utility")
                    .name("")
                    .menus(MenuHelper
                            .forNpc(-2)
                            .menu("Unlock Item", p -> {
                                long price = 20_000;
                                Dialog dialog = Dialog
                                        .builder()
                                        .id((byte) -2)
                                        .text(String.format("Membuka kunci akan memotong %d permata mu, lanjutkan?", price))
                                        .args(Map.of("item", item))
                                        .onRespond((pi, yOrN, args) -> {
                                            try {
                                                if (yOrN) {
                                                    Item3 it = (Item3) args.get("item");
                                                    if (!it.islock) {
                                                        pi.sendNoticeBox("Item tidak terkunci");
                                                        return;
                                                    }

                                                    if (pi.getGem() < price) {
                                                        pi.sendNoticeBox("Kamu tidak memiliki cukup permata");
                                                        return;
                                                    }


                                                    it.islock = false;
                                                    pi.updateGem(-price);
                                                    pi.item.charInventory(3);
                                                    pi.item.charInventory(4);

                                                    log.info("{} Unlocked", it.name);
                                                }
                                            } catch (Exception e) {
                                                log.error("Unhandled excepttion {} ", e.getMessage(), e);
                                            }
                                        })
                                        .build();

                                player.openDialog(dialog);

                            }).build())

                    .build();

            openMenu(player, root);
        }

    }

    public static void openWizardMenu(Player player) throws IOException {
        player.ResetCreateItemStar();

        Menu root = Menu.builder()
                .npc(-36)
                .title("Kakek Penyihir")
                .name("")
                .menus(MenuHelper
                        .forNpc(-36)
                        .menu("Toko Material", p -> Service.send_box_UI(p.conn, 17))
                        .submenu("Upgrade", wizardUpgradeMenu())
                        .submenu("Buat Medal", wizardCreateMedal())
                        .submenu("Lepas Medal", wizardLepasMedal(player))
                        .submenu("Lainya", wizardInlayMenu())
                        .menu("Close", "Tutup", MenuManager::navigateBack)
                        .build())

                .build();

        openMenu(player, root);
    }

    private static List<Menu> wizardCreateMedal() {

        return MenuHelper
                .SubMenuBuilder
                .create(-36)
                .item("Medal Ksatria", p -> {
                    p.id_medal_is_created = 0;
                    sendMedalCreateUI(p.conn, 0);
                })
                .item("Medal Penyihir", p -> {
                    p.id_medal_is_created = 1;
                    sendMedalCreateUI(p.conn, 1);
                })
                .item("Medal Assasin", p -> {
                    p.id_medal_is_created = 2;
                    sendMedalCreateUI(p.conn, 2);
                })
                .item("Medal Penembak", p -> {
                    p.id_medal_is_created = 3;
                    sendMedalCreateUI(p.conn, 3);
                })
                //.item("Medal MBG", p -> {
                //    p.id_medal_is_created = 4;
                //    sendMedalCreateUI(p.conn, 4);
                //})
                .item("Back", "Kembali", MenuManager::navigateBack)
                .build();
    }

    private static void sendMedalCreateUI(Session conn, int medalIndex) {
        try {
            GameSrc.initMedalMaterial(conn.p, medalIndex, (byte) 0);
            String[] medalNames = {"Medal Ksatria", "Medal Penyihir", "Medal Assasin", "Medal Penembak", "Medal MBG"};
            Message m = new Message(23);
            m.writer().writeUTF(medalNames[medalIndex]);
            m.writer().writeByte(19);
            m.writer().writeShort(0);
            m.writer().writeByte(5);
            for (int i = 5 * medalIndex; i < 5 * medalIndex + 5; i++) {
                m.writer().writeShort(conn.p.medal_create_material[i]);
                if (conn.version > 250) {
                    m.writer().writeShort(1);
                } else {
                    m.writer().writeByte(1);
                }
            }
            conn.addmsg(m);
            m.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= MEDAL V2 (NPC -111) =================
    // Duplikat independen dari sistem Medal di atas: item id, material,
    // dan slot penyimpanan (medal_create_material_v2) sendiri, tidak
    // bentrok dengan Medal V1. Lihat template.MedalV2Config &
    // template.Medal_Material_V2 untuk ID yang dipakai.

    public static void openMedalV2Menu(Player player) throws IOException {
        player.ResetCreateItemStar();

        MenuHelper helper = MenuHelper
                .forNpc(-111)
                .menu("Toko Material", p -> Service.send_box_UI(p.conn, 51))
                .submenu("Buat Medal V2", wizardCreateMedalV2())
                .submenu("Lepas Medal", wizardLepasMedalV2(player));

        // Admin-only: pilih warna medal V2 secara manual (Putih s/d Hijau),
        // bypass random roll & bypass force-orange dari Manager.BuffAdmin.
        if (player.conn.ac_admin > 3) {
            helper.submenu("Pilih Warna (Admin)", wizardPilihWarnaMedalV2Admin());
        }

        helper.menu("Upgrade Medal", p -> Service.send_box_UI(p.conn, 33));

        // .menu("Gabung material medal", p -> Service.send_box_UI(p.conn, 24))
        helper.menu("Close", "Tutup", MenuManager::navigateBack);

        Menu root = Menu.builder()
                .npc(-111)
                .title("Master Medal V2")
                .name("")
                .menus(helper.build())
                .build();

        openMenu(player, root);
    }

    /**
     * Submenu admin untuk memilih warna Medal V2 secara manual sebelum create.
     * Setelah dipilih, warna ini akan dipakai PERSIS untuk 1x create berikutnya
     * (lihat GameSrc case 3), lalu otomatis di-reset ke -1.
     */
    private static List<Menu> wizardPilihWarnaMedalV2Admin() {
        return MenuHelper
                .SubMenuBuilder
                .create(-111)
                .item("Putih", p -> {
                    if (p.conn.ac_admin <= 3) return;
                    p.admin_selected_color_v2 = 0;
                    Service.send_notice_box(p.conn, "Warna Medal V2 berikutnya: Putih");
                })
                .item("Biru", p -> {
                    if (p.conn.ac_admin <= 3) return;
                    p.admin_selected_color_v2 = 1;
                    Service.send_notice_box(p.conn, "Warna Medal V2 berikutnya: Biru");
                })
                .item("Kuning", p -> {
                    if (p.conn.ac_admin <= 3) return;
                    p.admin_selected_color_v2 = 2;
                    Service.send_notice_box(p.conn, "Warna Medal V2 berikutnya: Kuning");
                })
                .item("Ungu", p -> {
                    if (p.conn.ac_admin <= 3) return;
                    p.admin_selected_color_v2 = 3;
                    Service.send_notice_box(p.conn, "Warna Medal V2 berikutnya: Ungu");
                })
                .item("Oranye", p -> {
                    if (p.conn.ac_admin <= 3) return;
                    p.admin_selected_color_v2 = 4;
                    Service.send_notice_box(p.conn, "Warna Medal V2 berikutnya: Oranye");
                })
                .item("Hijau", p -> {
                    if (p.conn.ac_admin <= 3) return;
                    p.admin_selected_color_v2 = 5;
                    Service.send_notice_box(p.conn, "Warna Medal V2 berikutnya: Hijau");
                })
                .item("Batal (pakai roll normal)", p -> {
                    p.admin_selected_color_v2 = -1;
                    Service.send_notice_box(p.conn, "Warna manual dibatalkan, kembali ke roll normal.");
                })
                .item("Back", "Kembali", MenuManager::navigateBack)
                .build();
    }

    private static List<Menu> wizardCreateMedalV2() {
        return MenuHelper
                .SubMenuBuilder
                .create(-111)
                .item("Medal Evo Ksatria", p -> {
                    p.id_medal_is_created_v2 = 0;
                    sendMedalCreateUIV2(p.conn, 0);
                })
                .item("Medal Evo Penyihir", p -> {
                    p.id_medal_is_created_v2 = 1;
                    sendMedalCreateUIV2(p.conn, 1);
                })
                .item("Medal Evo Assasin", p -> {
                    p.id_medal_is_created_v2 = 2;
                    sendMedalCreateUIV2(p.conn, 2);
                })
                .item("Medal Evo Penembak", p -> {
                    p.id_medal_is_created_v2 = 3;
                    sendMedalCreateUIV2(p.conn, 3);
                })
                .item("Back", "Kembali", MenuManager::navigateBack)
                .build();
    }

    private static void sendMedalCreateUIV2(Session conn, int medalIndex) {
        try {
            GameSrc.initMedalMaterialV2(conn.p, medalIndex);
            Message m = new Message(23);
            m.writer().writeUTF(template.MedalV2Config.MEDAL_V2_NAME[medalIndex]);
            m.writer().writeByte(19);
            m.writer().writeShort(0);
            m.writer().writeByte(5);
            for (int i = 5 * medalIndex; i < 5 * medalIndex + 5; i++) {
                m.writer().writeShort(conn.p.medal_create_material_v2[i]);
                if (conn.version > 250) {
                    m.writer().writeShort(1);
                } else {
                    m.writer().writeByte(1);
                }
            }
            conn.addmsg(m);
            m.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<Menu> wizardLepasMedalV2(Player player) {
        MenuHelper.SubMenuBuilder helper = MenuHelper
                .SubMenuBuilder
                .create(-111);

        boolean hasMedal = false;
        for (int i = 10; i < 24; i++) {
            Item3 wear = player.item.wear[i];
            if (wear == null) continue;
            if (wear.type != 16) continue;

            hasMedal = true;
            helper.item(
                    wear.name,
                    Map.of("item", wear, "index", i),
                    (p, args) -> {
                        try {
                            Item3 item = (Item3) args.get("item");
                            int index = (int) args.get("index");

                            p.item.wear[index] = null;
                            p.item.add_item_bag3(item);

                            Service.sendPlayerWear(p);
                            Service.sendMainCharInfo(p);
                            MapService.broadcastMainCharInfo(p.map, p);
                            p.item.charInventory(3);
                            Service.send_notice_nobox_white(p.conn, String.format("%s berhasil dilepas", item.name));
                        } catch (Exception ignore) {
                        }
                    }
            );
        }

        if (!hasMedal) {
            helper.item("Tidak ada medal yang dipakai", p ->
                    Service.send_notice_nobox_white(p.conn, "Kamu tidak sedang memakai medal apapun.")
            );
        }

        helper.item("Back", "Kembali", MenuManager::navigateBack);
        return helper.build();
    }

    private static List<Menu> wizardLepasMedal(Player player) {
        MenuHelper.SubMenuBuilder helper = MenuHelper
                .SubMenuBuilder
                .create(-36);

        boolean hasMedal = false;
        for (int i = 10; i < 24; i++) {
            Item3 wear = player.item.wear[i];
            if (wear == null) continue;
            if (wear.type != 16) continue;

            hasMedal = true;
            helper.item(
                    wear.name,
                    Map.of("item", wear, "index", i),
                    (p, args) -> {
                        try {
                            Item3 item = (Item3) args.get("item");
                            int index = (int) args.get("index");

                            p.item.wear[index] = null;
                            p.item.add_item_bag3(item);

                            Service.sendPlayerWear(p);
                            Service.sendMainCharInfo(p);
                            MapService.broadcastMainCharInfo(p.map, p);
                            p.item.charInventory(3);
                            Service.send_notice_nobox_white(p.conn, String.format("%s berhasil dilepas", item.name));
                        } catch (Exception ignore) {
                        }
                    }
            );
        }

        if (!hasMedal) {
            helper.item("Tidak ada medal yang dipakai", p ->
                    Service.send_notice_nobox_white(p.conn, "Kamu tidak sedang memakai medal apapun.")
            );
        }

        helper.item("Back", "Kembali", MenuManager::navigateBack);
        return helper.build();
    }

    private static List<Menu> wizardUpgradeMenu() {
        return MenuHelper
                .SubMenuBuilder
                .create(-36)
                .item("Upgrade Equipment", p -> Service.send_box_UI(p.conn, 18))
                .item("Upgrade Medal", p -> Service.send_box_UI(p.conn, 33))
                .item("Back", "Kembali", MenuManager::navigateBack)
                .build();
    }

    private static List<Menu> wizardInlayMenu() {
        return MenuHelper
                .SubMenuBuilder
                .create(-36)
                .item("Gabung permata", p -> Service.send_box_UI(p.conn, 34))
                .item("Pasang permata", p -> Service.send_box_UI(p.conn, 35))
                .item("Buat lubang", p -> Service.send_box_UI(p.conn, 36))
                .item("Transformasi", p -> Service.send_box_UI(p.conn, 19))
                .item("Gabung material medal", p -> Service.send_box_UI(p.conn, 24))
                .item("Reload Wedding", p -> {
                    event_daily.Wedding.loadAllFromDB();
                    // Refresh wear[23] untuk semua player online yang punya cincin wedding
                    for (client.io.Session s : client.io.Session.SESSION_LIST) {
                        try {
                            if (s == null || s.p == null) continue;
                            event_daily.Wedding wed = event_daily.Wedding.get_obj(s.p.name);
                            if (wed != null) {
                                s.p.it_wedding = wed;
                                s.p.item.wear[23] = wed.it;
                                core.Service.sendPlayerWear(s.p);
                                core.Service.sendMainCharInfo(s.p);
                                game.map.MapService.broadcastMainCharInfo(s.p.map, s.p);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    p.sendNoticeBox("✅ Data wedding berhasil di-reload dari DB!");
                })
                .item("Back", "Kembali", MenuManager::navigateBack)
                .build();
    }

    public static void openMenuBallard(Player player) {
        // TAMBAHAN: BTF#getTime() sebelumnya tidak pernah dipanggil di manapun -
        // sisa waktu pendaftaran/pertempuran tidak pernah ditampilkan ke pemain
        // sama sekali. Sekarang ditampilkan di judul menu, dihitung ulang tiap
        // kali menu ini dibuka.
        BTF btfForTitle = GameEventManager.gI().getEvent(BTF.class);
        String title = "Mr Ballard";
        if (btfForTitle != null && btfForTitle.isActive()) {
            if (btfForTitle.getState() == BTFState.REGISTRATION) {
                title = "Mr Ballard (Pendaftaran ditutup dalam " + btfForTitle.getTime() + ")";
            } else if (btfForTitle.getState() == BTFState.FIGHTING) {
                title = "Mr Ballard (Pertempuran berakhir dalam " + btfForTitle.getTime() + ")";
            }
        }
        Menu root = Menu.builder()
                .npc(-53)
                .title(title)
                .name("")
                .menus(MenuHelper
                        .forNpc(-53)
                        .menu("Daftar BTF", p -> {
                            BTF btf = GameEventManager.gI().getEvent(BTF.class);
                            if (btf == null) {
                                p.sendNoticeBox("Event tidak aktif");
                                return;
                            }

                            if (btf.getState() == BTFState.REGISTRATION) {
                                btf.registerPlayer(player);
                            } else {
                                // TAMBAHAN: kasih tahu kapan pendaftaran berikutnya
                                // terbuka lagi (dihitung dari sisa waktu pertempuran
                                // yang sedang berjalan), bukan cuma bilang "tidak bisa".
                                if (btf.isActive() && btf.getState() == BTFState.FIGHTING) {
                                    p.sendNoticeBox("Tidak dalam waktu pendaftaran. Pertempuran masih berjalan, berakhir dalam " + btf.getTime());
                                } else {
                                    p.sendNoticeBox("Tidak dalam waktu pendaftaran");
                                }
                            }
                        })
                        .menu("Masuk Arena", p -> {
                            BTF btf = GameEventManager.gI().getEvent(BTF.class);
                            if (btf == null) {
                                p.sendNoticeBox("Event tidak aktif");
                                return;
                            }
                            if (btf.getState() == BTFState.FIGHTING) {
                                if (p.time_use_item_arena > System.currentTimeMillis()) {
                                    p.sendNoticeBox(
                                            "Mohon tunggu setelah "
                                                    + (p.time_use_item_arena - System.currentTimeMillis()) / 1000
                                                    + " detik");
                                    return;
                                }

                                Team team = btf.getPlayerTeam(p.objectId);
                                if (team == null) {
                                    p.sendNoticeBox("Kamu tidak terdaftar");
                                    return;
                                }

                                p.typepk = team.getFlag();
                                // FIX: sama seperti bug di BTF#teleportPlayersToArena() —
                                // jangan mutasi team.getLocation() langsung (objek itu
                                // di-share semua anggota tim). Selalu bikin Vgo baru.
                                Vgo arena = team.getLocation();
                                Vgo vgo = Vgo.create(p, arena.getToMap(), arena.getToX(), arena.getToY());
                                p.changeMap(p, vgo);
                                // TAMBAHAN: sinkronkan HUD battlefield (info rumah/jumlah
                                // pemain per desa) begitu pemain masuk ulang ke arena
                                // secara manual lewat menu ini, bukan cuma saat arena
                                // baru dimulai lewat teleportPlayersToArena().
                                btf.refreshBattlefieldInfoFor(p);
                            } else {
                                // TAMBAHAN: kasih tahu sisa waktu pendaftaran kalau
                                // memang masih di fase pendaftaran, bukan cuma bilang
                                // "belum berlangsung" tanpa info kapan.
                                if (btf.isActive() && btf.getState() == BTFState.REGISTRATION) {
                                    p.sendNoticeBox("Pertempuran belum berlangsung. Pendaftaran ditutup dalam " + btf.getTime());
                                } else {
                                    p.sendNoticeBox("Pertempuran belum berlangsung");
                                }
                            }
                        })
                        .menu("Item Shop", p -> {
                            Shop shop = ShopManager.getInstance().getShop("BTF Shop");
                            if (shop != null) {
                                ShopManager.getInstance().sendEquipmentShop(p.conn, shop);
                            } else {
                                p.sendNoticeBox("Shop tidak ditemukan");
                            }
                        })
                        .menu("Panduan", p -> {


                            String sb = """
                                    \s
                                    \s
                                     Waktu & Syarat\
                                    \s
                                     • Pendaftaran pukul: 20:00 - 20:15 WIB\
                                    \s
                                     • Biaya: 100.000 emas\
                                    \s
                                     • Minimal level 40\
                                    \s
                                    \s
                                     Cara Bermain\
                                    \s
                                     • Pemain dibagi acak menjadi 4 team\
                                    \s
                                     • Setiap team punya 1 markas utama\
                                    \s
                                     • Jumlah markas tergantung total pemain\
                                    \s
                                     • Semua markas terhubung ke Zona Perang\
                                    \s
                                    \s
                                     Boss Event\
                                    \s
                                     • Boss muncul menit ke-5 & ke-10\
                                    \s
                                     • Boss kedua tetap muncul walau pertama belum mati\
                                    \s
                                     • Bunuh Boss untuk dapat poin\
                                    \s
                                    \s
                                     Kondisi Menang\
                                    \s
                                     • Jika 1 team tersisa sebelum 60 menit → menang\
                                                                   \s
                                     • Point bisa di tukarkan di item shop\
                                    \s
                                    \s
                                     Poin Pertempuran\
                                    \s
                                     • Bunuh pemain: 5 poin\
                                    \s
                                     • Bunuh monster/penjaga: 2 poin\
                                    \s
                                     • Hancurkan markas: 20 poin\
                                    \s
                                     • Gunakan item perang: 10 poin\
                                    \s
                                     • Bunuh Boss: 30 poin\
                                    \s
                                     • Pemenang: 100 poin""";

                            Service.sendChatTab(p.conn, "Panduan BTF", sb);
                        })
                        .menu("Close", "Tutup", MenuManager::navigateBack)
                        .build())

                .build();

        openMenu(player, root);
    }

    /**
     * NPC "Poin Shop" (id: -112) — jual item pakai poin akumulasi topup, dipisah 2 mata uang:
     *  - Poin isi ulang emas   (dari topup GOLD, lihat topup.TopupController)
     *  - Poin isi ulang permata (dari topup GEM, lihat topup.TopupController)
     * Item & harga diatur lewat tabel `shop` (row name = "Poin Shop"), kolom `items` (JSON),
     * tiap item punya priceType sendiri: 4 = poin permata, 5 = poin emas.
     */
    public static void openPoinShopMenu(Player player) {
        final int NPC_ID = -112;
        final String SHOP_NAME = "Poin Shop";
        

        Menu root = Menu.builder()
                .npc(NPC_ID)
                .title(SHOP_NAME)
                .name("")
                .menus(MenuHelper
                        .forNpc(NPC_ID)
                        .menu("Shop Poin Gems", p -> {
                            Shop shop = ShopManager.getInstance().getShop(SHOP_NAME);
                            if (shop != null) {
                                ShopManager.getInstance().sendEquipmentShop(p.conn, shop, 4);
                            } else {
                                p.sendNoticeBox("Fitur sedang tidak tersedia");
                            }
                        })
                        .menu("Shop Poin Gold", p -> {
                            Shop shop = ShopManager.getInstance().getShop(SHOP_NAME);
                            if (shop != null) {
                                ShopManager.getInstance().sendEquipmentShop(p.conn, shop, 5);
                            } else {
                                p.sendNoticeBox("Fitur sedang tidak tersedia");
                            }
                        })
                        .menu("Cek Poin", p -> p.sendNoticeBox(String.format(
                                "Hai %s!\nBerikut informasi Poin mu:\nPoin Gold: %s\nPoin Gems: %s",
                                p.name,
                                Util.shortFormat(p.poinIsiUlangEmas),
                                Util.shortFormat(p.poinIsiUlangPermata)
                        )))
                        .menu("Close", "Tutup", MenuManager::navigateBack)
                        .build())
                .build();

        openMenu(player, root);
    }

    public static void openGlobalMenu(int npcId, Player player) {
        MenuHelper menus = MenuHelper.forNpc(npcId);

        GlobalEvent event = GameEventManager.gI().getGlobalEvent(npcId);
        if (event == null) {
            player.sendNoticeBox("No feature");
            return;
        }

        List<RequiredItem> items = event.getRequiredItems();
        if (items == null) {
            return;
        }

        menus.menu("Exchange Event", p -> {

            int max = ExchangeService.gI().getMaxExchangeCount(p, items);

            if (max <= 0) {
                p.sendNoticeBox("Bahan atau permata tidak mencukupi");
                return;
            }

            InputDialog input = InputDialog
                    .builder()
                    .title(event.getName())
                    .fields(List.of("Jumlah tukar (maks " + max + ")"))
                    .action((p1, arr) -> {
                        int count;
                        try {
                            count = Integer.parseInt(arr[0].trim());
                        } catch (Exception e) {
                            p1.sendNoticeBox("Jumlah tidak valid");
                            return;
                        }

                        if (count <= 0) {
                            p1.sendNoticeBox("Jumlah tukar tidak valid");
                            return;
                        }
                        if (count > max) {
                            p1.sendNoticeBox("Jumlah melebihi maksimum yang bisa ditukar (" + max + ")");
                            return;
                        }

                        String text = String.format("Kamu setuju ingin menukarkan sebayak %d kali  dengan biaya %d Gems", count, 100 * count);

                        Dialog confirm = Dialog
                                .builder()
                                .text(text)
                                .args(Map.of("event", event, "count", count))
                                .onRespond((p2, yOrN, args) -> {
                                    if (yOrN) {
                                        GlobalEvent ev = (GlobalEvent) args.get("event");
                                        int qty = (int) args.get("count");
                                        try {
                                            ExchangeService.gI().exchange(p2, ev, qty);
                                        } catch (IOException ignore) {
                                        }
                                    }
                                }).build();

                        p1.openDialog(confirm);
                    }).build();

            p.openInput(input);
        });


        if (player.conn.ac_admin > 5) {
            menus.menu("Dapatkan Item (Admin)", p -> {

                for (RequiredItem req : items) {
                    int totalToAdd = req.getQuantity() * 100;
                    byte category = ExchangeService.resolveCategory(req);
                    p.item.add_item_bag47((short) req.getItemId(), (short) totalToAdd, category);
                }
                p.item.updateBag();
            });
        }

        menus.menu("Guide", p -> {
            StringBuilder bahan = new StringBuilder();
            String startStr = event.getStartTime().format(DateTimeFormatter.ofPattern("dd MMMM", new Locale("id", "ID")));
            String endStr = event.getEndTime().format(DateTimeFormatter.ofPattern("dd MMMM", new Locale("id", "ID")));
            String time = "Waktu event: "+ startStr +" sampai "+ endStr;
            for (RequiredItem it : items) {

                bahan.append(" - ").append(ExchangeService.resolveItemName(it)).append(" ").append(it.getQuantity()).append(" pcs \n");
            }

            Service.sendChatTab(p.conn, event.getName(), String.format("\n%s\n %s\n %s\n", event.getGuide(), bahan, time));
        });

        menus.menu("Close", "Tutup", MenuManager::navigateBack);

        Menu root = Menu.builder()
                .npc(npcId)
                .title(event.getName())
                .name("")
                .menus(menus.build())
                .build();

        openMenu(player, root);
    }

    public static void openLiubeiMenu(Player player) {
        MenuHelper menus = MenuHelper.forNpc(-91);

        menus.menu("Tukar Item Event", p -> {
            ExchangeItem item = ExchangeService.gI().getItem(-91, "imlek");
            if (item == null) {
                p.sendNoticeBox("Item tidak ditemukan atau event sudah berakhir");
                return;
            }

            int max = ExchangeService.gI().getMaxExchangeCount(p, item.getRequirements());

            Dialog dialog = Dialog
                    .builder().
                    text(String.format("Kamu setuju ingin menukarkan sebayak %d kali  dengan biaya %d permata", max, 100 * max))
                    .args(Map.of("item", item))
                    .onRespond((p1, yOrN, args) -> {
                        if (yOrN) {
                            ExchangeItem quest = (ExchangeItem) args.get("item");
                            try {
                                ExchangeService.gI().exchange(p1, quest);
                            } catch (IOException ignore) {
                            }
                        }

                    }).build();


            p.openDialog(dialog);

        });


        if (player.conn.ac_admin > 1) {
            menus.menu("Take Item Event", p -> {
                ExchangeItem item = ExchangeService.gI().getItem(-91, "imlek");
                if (item == null) {
                    p.sendNoticeBox("Item tidak ditemukan atau event sudah berakhir");
                    return;
                }
                for (RequiredItem req : item.getRequirements()) {
                    int totalToAdd = req.getQuantity() * 100;
                    byte category = ExchangeService.resolveCategory(req);
                    p.item.add_item_bag47((short) req.getItemId(), (short) totalToAdd, category);
                }
                p.item.updateBag();
            });
        }

        menus.menu("Panduan", p -> {
            StringBuilder bahan = new StringBuilder();
            ExchangeItem item = ExchangeService.gI().getItem(-91, "imlek");
            for (RequiredItem it : item.getRequirements()) {

                bahan.append(" - ").append(ExchangeService.resolveItemName(it)).append(" ").append(it.getQuantity()).append(" pcs \n");
            }
            p.sendNoticeBox("Susun kepingan puzzle agar membentuk kalimat HAPPYNEWYEAR \n " + bahan.toString());
        });

        menus.menu("Close", "Tutup", MenuManager::navigateBack);

        Menu root = Menu.builder()
                .npc(-91)
                .title("Liu Bei")
                .name("")
                .menus(menus.build())
                .build();

        openMenu(player, root);
    }
}