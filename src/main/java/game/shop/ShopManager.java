package game.shop;

import client.io.Message;
import client.io.Session;
import client.Player;
import core.Service;
import game.items.UpgradeSystem;
import template.Item3;
import template.Option;
import utils.SQLHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ShopManager {
    private final Map<String, Shop> itemShop = new HashMap<>();

    private static final class Holder {
        private static final ShopManager INSTANCE = new ShopManager();
    }

    public static ShopManager getInstance() {
        return Holder.INSTANCE;
    }

    public void load() {
        itemShop.clear();

        var list = SQLHelper.selectFrom("shop").getAsModel(Shop.class);
        list.forEach(this::addShop);

    }

    public void addShop(Shop shop) {
        itemShop.put(shop.getName(), shop);
    }

    public Shop getShop(String name) {
        return itemShop.get(name);
    }

    public void sendEquipmentShop(Session conn, Shop shop) {
        sendEquipmentShop(conn, shop, null);
    }

    /**
     * @param priceTypeFilter kalau diisi (mis. 4 = poin gems, 5 = poin gold), cuma item dengan
     *                        priceType itu yang dikirim ke client — dipakai buat misahin tampilan
     *                        "Poin Shop" jadi dua menu (gems / gold) tanpa perlu bikin row shop
     *                        baru di DB. Isi null buat kirim semua item apa adanya (perilaku lama).
     */
    public void sendEquipmentShop(Session conn, Shop shop, Integer priceTypeFilter) {
        Message m = new Message(23);
        try {
            List<ShopItem> items = shop.getItems();
            if (priceTypeFilter != null) {
                items = items.stream()
                        .filter(s -> s.getPriceType() == priceTypeFilter)
                        .toList();
            }
            m.writer().writeUTF(shop.getName());
            m.writer().writeByte(1);
            m.writer().writeShort(items.size());
            for (ShopItem s : items) {
                Item3 temp = Item3.fromTemplate((short) s.getItemId());
                m.writer().writeShort(temp.id);
                // Item sayap yang dijual di Poin Shop selalu diberikan dalam kondisi +30
                // (lihat doPurchase), jadi labelnya ditampilkan sebagai "+30" di menu shop
                // biar pemain tahu dari awal tanpa perlu buka detail item dulu.
                String displayName = (temp.type == 7 && "Poin Shop".equals(shop.getName()))
                        ? temp.name + " +30"
                        : temp.name;
                m.writer().writeUTF(displayName);
                m.writer().writeByte(temp.clazz);
                m.writer().writeByte(temp.type);
                m.writer().writeShort(temp.icon);
                m.writer().writeLong(s.getPrice()); // price
                m.writer().writeShort(temp.level); // level
                m.writer().writeByte(temp.color);
                m.writer().writeByte(temp.op.size()); // option
                for (Option op : temp.op) {
                    m.writer().writeByte(op.getId());
                    m.writer().writeInt(op.getParam(temp.tier));
                }
                m.writer().writeByte(s.getPriceType()); // type money
            }
            conn.p.setCurrentShop(shop);
            // Simpan tab priceType yang lagi dilihat pemain (null = shop biasa, tidak ada
            // pemisahan tab mata uang), dipakai lagi di doPurchase supaya beli item yang
            // dijual di >1 mata uang (mis. Poin Shop) nggak salah ambil harga/poin.
            conn.p.setCurrentShopPriceType(priceTypeFilter);
            conn.addmsg(m);
        } catch (Exception ignore) {
        }
    }

    public void doPurchase(Player player, int itemId, int quantity) {
        Shop shop = player.getCurrentShop();
        if (shop == null) return;

        // Filter berdasarkan tab priceType yang lagi aktif (mis. tab Poin Gems / Poin Gold di
        // Poin Shop), supaya kalau satu itemId dijual di >1 mata uang, yang kepilih tetap yang
        // sesuai tab yang lagi dilihat pemain, bukan entri pertama yang ketemu di list.
        Optional<ShopItem> opt = shop.find(itemId, player.getCurrentShopPriceType());
        if (opt.isEmpty()) {
            player.sendNoticeBox("Item tidak terdaftar");
            return;
        }

        ShopItem shopItem = opt.get();
        int totalPrice = shopItem.getPrice() * quantity;

        boolean tryPurchase = switch (shopItem.getPriceType()) {
            case 0 -> {
                if (player.getGold() < totalPrice) {
                    yield false;
                }
                player.updateGold(-totalPrice);
                yield true;
            }
            case 1 -> {
                if (player.getGem() < totalPrice) {
                    yield false;
                }
                player.updateGem(-totalPrice);
                yield true;
            }
            case 3 -> {
                if (player.pointarena < totalPrice) {
                    yield false;
                }
                player.pointarena -= totalPrice;
                yield true;
            }
            // Poin isi ulang permata (akumulasi topup GEM, lihat topup.TopupController)
            case 4 -> {
                if (player.poinIsiUlangPermata < totalPrice) {
                    yield false;
                }
                player.poinIsiUlangPermata -= totalPrice;
                yield true;
            }
            // Poin isi ulang emas (akumulasi topup GOLD, lihat topup.TopupController)
            case 5 -> {
                if (player.poinIsiUlangEmas < totalPrice) {
                    yield false;
                }
                player.poinIsiUlangEmas -= totalPrice;
                yield true;
            }
            default -> false;
        };

        if (!tryPurchase) {
            player.sendNoticeBox("Pembelian gagal!");
            return;
        }

        switch (shop.getCategory()) {
            case 3 -> {
                Item3 equip = Item3.fromTemplate((short) shopItem.getItemId());
                if (equip == null) return;

                boolean isPoinShopWingPurchase = equip.type == 7
                        && (shopItem.getPriceType() == 4 || shopItem.getPriceType() == 5);
                if (isPoinShopWingPurchase) {
                    if (shopItem.getPriceType() == 5 && isLegacyUpgradableWing(equip.id)) {
                        // Sayap LAMA (base wing, 8 id di bawah) dibeli pakai poin isi ulang emas/gold:
                        // langsung tier +30 (semua bonus upgrade 1-30 diterapkan lewat UpgradeSystem,
                        // yang memang hanya mengenali 8 id ini). Durasi tetap ambil dari data shop di DB.
                        for (int i = 0; i < 30; i++) {
                            UpgradeSystem.updateWingUpgradeOption(equip);
                        }
                        equip.convertWing(); // sesuai pola admin-instant di GameSrc: konversi ke bentuk evolusi +30
                    } else {
                        // Item Poin Shop yang lain (mis. Sayap Alfa-Kappa / id 5479-5489), baik yang
                        // dibeli pakai poin gems maupun poin gold, statnya SUDAH lengkap dari template
                        // `op` di DB (lihat Item3.fromTemplate), jadi TIDAK boleh ikut loop UpgradeSystem
                        // di atas — itu yang bikin acak-acakan karena UpgradeSystem cuma kenal 8 id lama
                        // dan malah numpuk Option(id=1,...) yang salah di atas stat template yang sudah benar.
                        //
                        // TAPI angka stat yang dikirim ke client dihitung lewat Option.getParam(tier)
                        // (lihat template.Option), bukan dari param mentah — jadi tier tetap harus
                        // di-set ke 30 di sini supaya param di-scale ke nilai penuh "+30", tanpa
                        // menambah option baru apa pun.
                        equip.tier = 30;
                    }
                }
                if (shopItem.getDuration() > 0) {
                    equip.expiry_date = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(shopItem.getDuration());
                }

                player.item.add_item_bag3(equip);

            }
            case 4, 7 -> {
                player.item.add_item_bag47((short) shopItem.getItemId(), (short) quantity, (byte) shop.getCategory());
            }

        }

        player.item.updateBag();
        player.sendNoticeBox("Pembelian Berhasil");

        // Refresh panel Character Info (packet id 3) di client supaya poin yang baru kepotong
        // (Poin Arena / Poin isi ulang permata / Poin isi ulang emas, dll — lihat
        // Body.getGemRechargePoints/getGemUsagePoints & MainObject.getStatInfo id 181/182/183)
        // langsung keliatan tanpa perlu logout-login dulu.
        try {
            Service.sendMainCharInfo(player);
        } catch (Exception e) {
            System.err.println("Gagal refresh char info setelah pembelian shop untuk player=" + player.name);
        }

        // Simpan langsung (item + poin yang terpakai) supaya kalau server restart/crash
        // sesaat setelah beli, tidak ada window waktu yang bikin datanya balik ke sebelum beli.
        // p.flush() menulis SEMUA kolom (termasuk item4/item7 & poin) dalam SATU query UPDATE,
        // jadi item & poin selalu tersimpan atomic/bersamaan — tidak bisa nyangkut cuma sebagian.
        try {
            player.flush();
        } catch (Exception e) {
            System.err.println("Gagal flush setelah pembelian shop untuk player=" + player.name);
        }

    }


    /**
     * Id sayap "lama" yang memang didesain untuk sistem tier-upgrade bertahap
     * (lihat switch(item.id) di UpgradeSystem.updateWingUpgradeOption & Item3.convertWing).
     * Item Poin Shop baru dengan stat sudah lengkap dari template TIDAK dimasukkan ke sini,
     * supaya tidak ikut ke-upgrade paksa +30 dan statnya tetap sesuai template aslinya.
     */
    private static boolean isLegacyUpgradableWing(int itemId) {
        return switch (itemId) {
            case 2880, 2887, 2894, 2901, 2908, 2915, 2922, 2929 -> true;
            default -> false;
        };
    }

    public static void logPurchase(Player player, int itemId, int quantity, long price, String currency) {
        System.out.println("Player " + player.name + " bought " + quantity + " of item " + itemId +
                " for " + price + " " + currency);
    }
}