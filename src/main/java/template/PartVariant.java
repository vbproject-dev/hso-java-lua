package template;

import lombok.Data;

/**
 * 1 row = 1 override "file mana yang dipakai" untuk sebuah (type, id) part_char.
 * <p>
 * variant = 0  -> pakai file default, tanpa suffix (contoh: 100_0.png)
 * variant = 1  -> pakai file dengan suffix "_1"     (contoh: 100_0_1.png)
 * variant = 2  -> pakai file dengan suffix "_2"     (contoh: 100_0_2.png), dst.
 * <p>
 * Table: part_variant (type TINYINT, id SMALLINT, variant INT, PRIMARY KEY(type, id))
 */
@Data
public class PartVariant {
    private byte type;
    private short id;
    private int variant;
}
