package game.items.models;

import core.Util;
import lombok.Data;
import template.Option;

/**
 * Satu baris konfigurasi Option untuk guild_item, hasil parse dari JSON
 * kolom option_warrior/option_sorcerer/option_assassin/option_gunner.
 *
 * Format JSON yang didukung:
 *   {"id": 7, "value": 6000}                 -> nilai tetap (fixed)
 *   {"id": 7, "value": 6000, "maxValue": 8000} -> di-random antara value..maxValue
 */
@Data
public class GuildOptionConfig {
    private int id;
    private int value;
    private Integer maxValue; // null / <= value => dianggap fixed, tidak di-random

    /**
     * Hasilkan param akhir untuk Option ini. Jika maxValue diisi dan lebih besar
     * dari value, param akan di-random di antara [value, maxValue] (inclusive).
     * Kalau tidak, param = value (fixed), sama seperti behaviour "param" lama.
     */
    public int rollParam() {
        if (maxValue == null || maxValue <= value) {
            return value;
        }
        return Util.random(value, maxValue);
    }

    public Option toOption() {
        return new Option(id, rollParam());
    }
}
