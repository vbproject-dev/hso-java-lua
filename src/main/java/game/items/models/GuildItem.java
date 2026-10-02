package game.items.models;

import lombok.Data;
import template.Option;

import java.util.ArrayList;
import java.util.List;

@Data
public class GuildItem {
    private int itemId;
    private String guildName;
    private List<GuildOptionConfig> optionWarrior;
    private List<GuildOptionConfig> optionAssassin;
    private List<GuildOptionConfig> optionSorcerer;
    private List<GuildOptionConfig> optionGunner;

    /**
     * Ambil konfigurasi (value/maxValue) yang sudah diatur di database untuk class tertentu.
     * Urutan clazz mengikuti index yang dipakai di createCloakStat/createTitleStat:
     * 0 = Warrior, 1 = Sorcerer, 2 = Assassin, 3 = Gunner.
     *
     * @return list config dari DB, atau null/empty jika belum dikonfigurasi
     *         (sehingga pemanggil bisa fallback ke stat random bawaan / StatGenerator).
     */
    public List<GuildOptionConfig> getOptionsForClass(int clazz) {
        return switch (clazz) {
            case 0 -> optionWarrior;
            case 1 -> optionSorcerer;
            case 2 -> optionAssassin;
            case 3 -> optionGunner;
            default -> null;
        };
    }

    /**
     * Sama seperti {@link #getOptionsForClass(int)} tapi langsung dikonversi jadi
     * List<Option> siap pakai, dengan param di-random antara value..maxValue kalau
     * maxValue diisi di DB (kalau tidak diisi / maxValue <= value, param = value / fixed).
     *
     * @return null/empty kalau belum ada konfigurasi di DB untuk class ini,
     *         supaya pemanggil tahu harus fallback ke StatGenerator.
     */
    public List<Option> rollOptionsForClass(int clazz) {
        List<GuildOptionConfig> configs = getOptionsForClass(clazz);
        if (configs == null || configs.isEmpty()) return null;

        List<Option> options = new ArrayList<>();
        for (GuildOptionConfig config : configs) {
            options.add(config.toOption());
        }
        return options;
    }
}
