package game.items.models;

import lombok.Data;

@Data
public class RequiredItem {
    private int itemId;
    private int quantity;

    /**
     * Kategori item eksplisit (3 = equipment, 4 = potion/quest item, 7 = material).
     * 0 = belum diset (data lama / format 2-elemen [itemId, qty] di DB) -> harus
     * fallback ke ExchangeService.resolveCategory(itemId) yang menebak lewat ID.
     * Kalau nilai ini diisi (format DB 3-elemen [itemId, qty, category]), PAKAI INI
     * langsung, jangan ditebak lagi, supaya tidak nyasar ke kategori lain saat ID
     * item4 & item7 kebetulan sama.
     */
    private int category;

    public RequiredItem(int itemId, int quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.category = 0;
    }

    public RequiredItem(int itemId, int quantity, int category) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.category = category;
    }

    public boolean hasExplicitCategory() {
        return category == 3 || category == 4 || category == 7;
    }
}
