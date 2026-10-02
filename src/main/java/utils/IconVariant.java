package utils;

import lombok.Data;

/**
 * 1 row = 1 override "file icon mana yang dipakai" untuk sebuah (iconType, localId).
 * <p>
 * variant = 0  -> pakai file default, tanpa suffix (contoh: item_map/8.png)
 * variant = 1  -> pakai file dengan suffix "_1"     (contoh: item_map/8_1.png)
 * variant = 2  -> pakai file dengan suffix "_2"     (contoh: item_map/8_2.png), dst.
 * <p>
 * Table: icon_variant (icon_type VARCHAR(20), local_id INT, variant INT, PRIMARY KEY(icon_type, local_id))
 */
@Data
public class IconVariant {
    private IconType iconType;
    private int localId;
    private int variant;
}
