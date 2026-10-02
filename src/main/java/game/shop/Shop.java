package game.shop;

import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
public class Shop {
    protected int id;
    protected String name;
    protected int npcId;
    protected int category;
    protected List<ShopItem> items;


    public Optional<ShopItem> find(int itemId) {
        return find(itemId, null);
    }

    /**
     * Cari ShopItem berdasarkan itemId, dan kalau priceTypeFilter diisi (mis. 4 = poin gems,
     * 5 = poin gold), harus match priceType-nya juga.
     * <p>
     * Ini penting buat shop yang sama-sama jual satu itemId di dua mata uang berbeda
     * (mis. "Poin Shop": item X ada versi harga poin gems & versi harga poin gold).
     * Tanpa filter priceType, findFirst() bisa salah ambil entri mata uang yang lain
     * dari yang lagi dilihat/dipilih pemain, sehingga poin yang dicek jadi salah
     * (poin gems cukup tapi malah ngecek poin gold, jadi "gagal" padahal seharusnya berhasil).
     */
    public Optional<ShopItem> find(int itemId, Integer priceTypeFilter) {
        return items.stream()
                .filter(item -> item.getItemId() == itemId)
                .filter(item -> priceTypeFilter == null || item.getPriceType() == priceTypeFilter)
                .findFirst();
    }
}