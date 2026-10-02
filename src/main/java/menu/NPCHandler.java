package menu;

import client.Player;
import feature.teleport.PremiumTeleportMenu;

import java.io.IOException;

/**
 * NPCHandler — PATCH
 *
 * Tambahkan case -95 (Batu Teleport Premium) ke isHandled() dan handle().
 *
 * DIFF dari file asli:
 *
 *  public static boolean isHandled(byte npcId) {
 *      return switch (npcId) {
 * -        case -91, -32, -36, -44, -53-> true;
 * +        case -91, -32, -36, -44, -53, -95 -> true;   // ← tambah -95
 *          default -> false;
 *      };
 *  }
 *
 *  public static void handle(Player player, byte npcId) throws IOException {
 *      switch (npcId) {
 *          case -32 -> MenuManager.openRankingMenu(player);
 *          case -36 -> MenuManager.openWizardMenu(player);
 *          case -44 -> MenuManager.openAnnaMenu(player);
 *          case -91 -> MenuManager.openLiubeiMenu(player);
 *          case -53 -> MenuManager.openMenuBallard(player);
 * +        case -95 -> PremiumTeleportMenu.open(player);  // ← tambah ini
 *      }
 *  }
 */
public class NPCHandler {

    public static boolean isHandled(byte npcId) {
        return switch (npcId) {
            case -91, -32, -36, -44, -53, -95, -100, -102, -111, -112 -> true;
            default -> false;
        };
    }

    public static void handle(Player player, byte npcId) throws IOException {
        switch (npcId) {
            case -32  -> MenuManager.openRankingMenu(player);
            case -36  -> MenuManager.openWizardMenu(player);
            case -44  -> MenuManager.openAnnaMenu(player);
            case -91  -> MenuManager.openLiubeiMenu(player);
            case -53  -> MenuManager.openMenuBallard(player);
            case -95  -> PremiumTeleportMenu.open(player);
            // Batu Teleport di desa (Map 0) → teleport GRATIS ke Map 115 (Desa Ajaib)
            case -100 -> PremiumTeleportMenu.teleportToGateway(player);
            // NPC di map 115 → buka menu pilih map premium + bayar
            case -102 -> PremiumTeleportMenu.open(player);
            // Master Medal V2 — duplikat sistem medal, id/material terpisah
            case -111 -> MenuManager.openMedalV2Menu(player);
            // Poin Shop — jual item pakai poin isi ulang emas/permata (topup.TopupController)
            case -112 -> MenuManager.openPoinShopMenu(player);
        }
    }
}