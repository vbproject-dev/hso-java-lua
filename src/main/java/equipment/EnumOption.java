package equipment;

import lombok.Getter;

@Getter
public enum EnumOption {
    PHYSICAL_DMG(0, false),
    ICE_DMG(1,false),
    FIRE_DMG(2, false),
    LIGHTING_DMG(3, false),
    POISON_DMG(4, false),
    DARK_DMG(5, false),
    LIGHT_DMG(6, false),

    PHYSICAL_DMG_P(7, true),
    ICE_DMG_P(8, true),
    FIRE_DMG_P(9, true),
    LIGHTING_DMG_P(10, true),
    POISON_DMG_P(11, true),
    DARK_DMG_P(12, true),
    LIGHT_DMG_P(13, true),

    DEFENSE(14, false),
    DEFENSE_P(15, true),

    PHYSICAL_RES(16, true),
    ICE_RES(17, true),
    FIRE_RES(18, true),
    LIGHTING_RES(19,true),
    POISON_RES(20, true),
    DARK_RES(21, false),
    LIGHT_RES(22, false),

    STR(23, false),
    DEX(24, false),
    VIT(25, false),
    INT(26, false),

    HP(27, true),
    MP(28, true),

    HP_REGEN(29, true),
    MP_REGEN(30, true),

    LIFE_STEAL(31, true),
    MP_STEAL(32, true),

    CRIT(33, true),
    EVADE(34, true),
    REFLECT(35, true),
    PENETRATION_ARMOR(36, true),

    ATK_SKILL(37, false),
    PASSIVE_SKILL(38, false),

    BASIC_DMG(40, true),
    EXP(51, true),

    // ==== Medal V2 additional stats ====
    FREEZE_RATE(66, true),              // [66] Membeku %
    FROZEN_RATE(67, true),              // [67] Kebekuan %

    DARK_CRIT_RATE(72, true),           // [72] Tingkat Kritis Kegelapan %
    LIGHT_CRIT_RATE(73, true),          // [73] Tingkat Kritis Cahaya %

    FIRE_BURN_RATE(76, true),           // [76] Tingkat Luka Bakar Api %
    FIRE_BURN_VALUE(77, false),         // [77] Luka Bakar Api (3 Detik)
    COLD_BURN_RATE(78, true),           // [78] Tingkat Luka Bakar Dingin %
    COLD_BURN_VALUE(79, false),         // [79] Luka Bakar Dingin (3000 / 3 Detik)

    DARK_ARMOR(80, true),               // [80] Armor Kegelapan %
    INVISIBLE_RATE(81, true),           // [81] Tak Terlihat %
    INVISIBLE_VALUE(82, false),         // [82] Tingkat Tak Terlihat (3000 / 3 Detik)

    COOLDOWN_REDUCTION_RATE(83, true),  // [83] Tingkat Pengurangan Cooldown %
    COOLDOWN_REDUCTION_VALUE(84, false),// [84] Pengurangan Cooldown (3000 / 3 Detik)

    MAGIC_SHIELD_RATE(85, true),        // [85] Tingkat Perisai Sihir %
    MAGIC_SHIELD_VALUE(86, false),      // [86] Perisai Sihir (3000 / 3 Detik)

    CONFUSION_RATE(87, true),           // [87] Tingkat Kebingungan %
    CONFUSION_VALUE(88, false),         // [88] Kebingungan (3000 / 3 Detik)

    PHYSICAL_DMG_REDUCTION(90, true),   // [90] Pengurangan Kerusakan Fisik %
    ICE_DMG_REDUCTION(91, true),        // [91] Pengurangan Kerusakan Es %
    FIRE_DMG_REDUCTION(92, true),       // [92] Pengurangan Kerusakan Api %
    POISON_DMG_REDUCTION(93, true),     // [93] Pengurangan Kerusakan Racun %
    LIGHTING_DMG_REDUCTION(94, true),   // [94] Pengurangan Kerusakan Listrik %

    INTERNAL_WOUND_RATE(97, true),      // [97] Tingkat Luka Dalam %
    INTERNAL_WOUND_VALUE(98, false),    // [98] Luka Dalam (3000 / 3 Detik)

    MAX_HP_DMG(113, true);              // [113] Kerusakan Max HP %




    private final int id;
    private final boolean percent;

    EnumOption(int id, boolean percent) {
        this.id = id;
        this.percent = percent;
    }

    public static EnumOption fromId(int id) {
        for (EnumOption option : EnumOption.values()) {
            if (option.getId() == id) {
                return option;
            }
        }

        return null;
    }

    public static int getValue(EnumOption option) {
       return  option.getId();
    }

}