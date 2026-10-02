package equipment;

import template.Item3;
import template.Option;

import java.util.*;

import static equipment.EnumOption.*;


public class StatGenerator {
    private final Random random;

    public StatGenerator() {
        this.random = new Random();
    }

    public List<StatEntry> generateStats(int maxStats, List<StatConfig> availableStats, int color) {
        if (availableStats == null || availableStats.isEmpty() || maxStats <= 0) {
            return Collections.emptyList();
        }

        // Remove duplicate StatConfigs with same EnumOption (keep first occurrence)
        Map<EnumOption, StatConfig> uniqueConfigs = new LinkedHashMap<>();
        for (StatConfig config : availableStats) {
            uniqueConfigs.putIfAbsent(config.getOption(), config);
        }

        List<StatConfig> uniqueAvailableStats = new ArrayList<>(uniqueConfigs.values());

        // Adjust maxStats if it exceeds available unique options
        int actualMaxStats = Math.min(maxStats, uniqueAvailableStats.size());

        List<StatEntry> selectedStats = new ArrayList<>();
        Set<EnumOption> usedOptions = new HashSet<>();

        // Create a working list of remaining configs
        List<StatConfig> remainingConfigs = new ArrayList<>(uniqueAvailableStats);

        while (selectedStats.size() < actualMaxStats && !remainingConfigs.isEmpty()) {
            // Uniform random selection from remaining configs
            int selectedIndex = random.nextInt(remainingConfigs.size());
            StatConfig selectedConfig = remainingConfigs.get(selectedIndex);

            // Double-check no duplicate (paranoid safety)
            if (usedOptions.contains(selectedConfig.getOption())) {
                remainingConfigs.remove(selectedIndex);
                continue;
            }

            // Generate random value using the config's min/max range
            double value = selectedConfig.generateRandomValue(random, color);

            // Add to results and mark as used
            selectedStats.add(new StatEntry(selectedConfig.getOption(), value));
            usedOptions.add(selectedConfig.getOption());

            Collections.shuffle(selectedStats, random);

            // Remove from remaining configs to prevent reselection
            remainingConfigs.remove(selectedIndex);
        }

        return selectedStats;
    }


    /**
     * Sama seperti generateStats(), tapi bekerja dengan "grup" StatConfig alih-alih
     * satu-satu. Sebuah grup berisi 1 config (stat berdiri sendiri) atau lebih
     * (mis. pasangan RATE + VALUE) yang harus selalu dipilih atau dilewati BERSAMA-SAMA.
     * Ini mencegah kasus di mana stat *_RATE muncul tanpa pasangan *_VALUE-nya (atau
     * sebaliknya) di Medal V2.
     */
    private List<StatEntry> generateGroupedStats(int maxStats, List<List<StatConfig>> groups, int color) {
        if (groups == null || groups.isEmpty() || maxStats <= 0) {
            return Collections.emptyList();
        }

        List<List<StatConfig>> remainingGroups = new ArrayList<>(groups);
        Collections.shuffle(remainingGroups, random);

        List<StatEntry> selectedStats = new ArrayList<>();
        Set<EnumOption> usedOptions = new HashSet<>();

        for (List<StatConfig> group : remainingGroups) {
            if (selectedStats.size() >= maxStats) break;

            // Lewati grup jika salah satu optionnya sudah terpakai (safety, tidak boleh duplikat)
            boolean alreadyUsed = group.stream().anyMatch(cfg -> usedOptions.contains(cfg.getOption()));
            if (alreadyUsed) continue;

            // Grup hanya diambil jika SEMUA anggotanya muat dalam sisa slot yang ada.
            // Kalau tidak muat (mis. cuma sisa 1 slot tapi grup berisi rate+value = 2),
            // grup dilewati seutuhnya — tidak pernah diambil sebagian.
            int remainingBudget = maxStats - selectedStats.size();
            if (group.size() > remainingBudget) continue;

            for (StatConfig cfg : group) {
                double value = cfg.generateRandomValue(random, color);
                selectedStats.add(new StatEntry(cfg.getOption(), value));
                usedOptions.add(cfg.getOption());
            }
        }

        Collections.shuffle(selectedStats, random);
        return selectedStats;
    }

    /**
     * Pool stat khusus Medal V2, dikelompokkan per grup. Stat yang punya pasangan
     * RATE + VALUE (mis. FIRE_BURN_RATE & FIRE_BURN_VALUE) digabung dalam satu grup
     * agar selalu muncul bersama. Stat yang berdiri sendiri tetap jadi grup isi 1.
     */
    private static List<List<StatConfig>> buildMedalV2Pool() {
        List<List<StatConfig>> groups = new ArrayList<>();

        // ---- Pool dasar (sama seperti Medal V1) — semua berdiri sendiri ----
        groups.add(List.of(new StatConfig(DEFENSE, 1000, 1100)));
        groups.add(List.of(new StatConfig(DEFENSE_P, 1, 2)));
        groups.add(List.of(new StatConfig(PHYSICAL_RES, 10, 11)));
        groups.add(List.of(new StatConfig(ICE_RES, 10, 11)));
        groups.add(List.of(new StatConfig(FIRE_RES, 10, 11)));
        groups.add(List.of(new StatConfig(LIGHTING_RES, 10, 11)));
        groups.add(List.of(new StatConfig(POISON_RES, 10, 11)));
        groups.add(List.of(new StatConfig(STR, 1, 5)));
        groups.add(List.of(new StatConfig(DEX, 1, 5)));
        groups.add(List.of(new StatConfig(VIT, 1, 5)));
        groups.add(List.of(new StatConfig(INT, 1, 5)));
        groups.add(List.of(new StatConfig(HP, 10, 11)));
        groups.add(List.of(new StatConfig(MP, 10, 11)));
        groups.add(List.of(new StatConfig(HP_REGEN, 1, 2)));
        groups.add(List.of(new StatConfig(MP_REGEN, 1, 2)));
        groups.add(List.of(new StatConfig(LIFE_STEAL, 1, 2)));
        groups.add(List.of(new StatConfig(MP_STEAL, 1, 2)));
        groups.add(List.of(new StatConfig(CRIT, 1, 2)));
        groups.add(List.of(new StatConfig(EVADE, 1, 2)));
        groups.add(List.of(new StatConfig(REFLECT, 1, 2)));
        groups.add(List.of(new StatConfig(ATK_SKILL, 1, 1)));
        groups.add(List.of(new StatConfig(PASSIVE_SKILL, 1, 1)));
        groups.add(List.of(new StatConfig(PENETRATION_ARMOR, 1, 2)));
        groups.add(List.of(new StatConfig(EXP, 5, 5)));

        // ---- Stat tambahan Medal V2 yang berdiri sendiri (tidak punya pasangan value) ----
        groups.add(List.of(new StatConfig(FREEZE_RATE, 6, 6)));
        groups.add(List.of(new StatConfig(FROZEN_RATE, 6, 6)));
        groups.add(List.of(new StatConfig(DARK_CRIT_RATE, 10, 10)));
        groups.add(List.of(new StatConfig(LIGHT_CRIT_RATE, 10, 10)));
        groups.add(List.of(new StatConfig(DARK_ARMOR, 10, 10)));
        groups.add(List.of(new StatConfig(PHYSICAL_DMG_REDUCTION, 30, 30)));
        groups.add(List.of(new StatConfig(ICE_DMG_REDUCTION, 30, 30)));
        groups.add(List.of(new StatConfig(FIRE_DMG_REDUCTION, 30, 30)));
        groups.add(List.of(new StatConfig(POISON_DMG_REDUCTION, 30, 30)));
        groups.add(List.of(new StatConfig(LIGHTING_DMG_REDUCTION, 30, 30)));
        groups.add(List.of(new StatConfig(MAX_HP_DMG, 6, 6)));

        // ---- Pasangan RATE + VALUE — SELALU diambil/dilewati bersama-sama ----
        groups.add(List.of(new StatConfig(FIRE_BURN_RATE, 6, 6), new StatConfig(FIRE_BURN_VALUE, 3, 3)));
        groups.add(List.of(new StatConfig(COLD_BURN_RATE, 6, 6), new StatConfig(COLD_BURN_VALUE, 3000, 3000)));
        groups.add(List.of(new StatConfig(INVISIBLE_RATE, 6, 6), new StatConfig(INVISIBLE_VALUE, 3000, 3000)));
        groups.add(List.of(new StatConfig(COOLDOWN_REDUCTION_RATE, 6, 6), new StatConfig(COOLDOWN_REDUCTION_VALUE, 3000, 3000)));
        groups.add(List.of(new StatConfig(MAGIC_SHIELD_RATE, 10, 10), new StatConfig(MAGIC_SHIELD_VALUE, 3000, 3000)));
        groups.add(List.of(new StatConfig(CONFUSION_RATE, 6, 6), new StatConfig(CONFUSION_VALUE, 3000, 3000)));
        groups.add(List.of(new StatConfig(INTERNAL_WOUND_RATE, 6, 6), new StatConfig(INTERNAL_WOUND_VALUE, 3000, 3000)));

        return groups;
    }

    public StatEntry genSingle(List<StatConfig> configs, int color) {

        List<StatEntry> stats = generateStats(1, configs, color);
        if (stats.isEmpty()) {
            return null;
        }

        return stats.get(0);
    }

    public List<Option> createMedalStat(int clazz, int color) {
        short itemId = (clazz >= 0 && clazz <= 3) ? (short)(clazz + 4587) : (short)5288;
        return createMedalStat(clazz, color, itemId);
    }

    public List<Option> createMedalStat(int clazz, int color, short idItem) {
        List<StatEntry> entries = new ArrayList<>();

        // Generate Base Damage By Clazz
        int minValue = 1000;
        int maxValue = 1100;

        List<StatConfig> base = getBaseDmgConfig(clazz, minValue, maxValue);
        StatEntry dmg = genSingle(base, color);

        if (dmg == null) {
            return List.of();
        }

        entries.add(dmg);

        minValue = color;
        maxValue = color + 1;

        StatConfig config = getPercentDmg(dmg, minValue, maxValue);

        if (config != null) {
            double value = config.generateRandomValue(random, color);
            entries.add(new StatEntry(config.getOption(), value));
        }

        List<StatConfig> remaining = List.of(
                new StatConfig(DEFENSE, 1000, 1100),
                new StatConfig(DEFENSE_P, 1, 2),
                new StatConfig(PHYSICAL_RES, 10, 11),
                new StatConfig(ICE_RES, 10, 11),
                new StatConfig(FIRE_RES, 10, 11),
                new StatConfig(LIGHTING_RES, 10, 11),
                new StatConfig(POISON_RES, 10, 11),


                new StatConfig(STR, 1, 5),
                new StatConfig(DEX, 1, 5),
                new StatConfig(VIT, 1, 5),
                new StatConfig(INT, 1, 5),

                new StatConfig(HP, 10, 11),
                new StatConfig(MP, 10, 11),

                new StatConfig(HP_REGEN, 1, 2),
                new StatConfig(MP_REGEN, 1, 2),

                new StatConfig(LIFE_STEAL, 1, 2),
                new StatConfig(MP_STEAL, 1, 2),

                new StatConfig(CRIT, 1, 2),
                new StatConfig(EVADE, 1, 2),
                new StatConfig(REFLECT, 1, 2),


                new StatConfig(ATK_SKILL, 1, 1),
                new StatConfig(PASSIVE_SKILL, 1, 1),

                new StatConfig(PENETRATION_ARMOR, 1, 2),
                new StatConfig(EXP, 5, 5)
        );
        int maxCount = 0;
        switch (color) {
            case 0 -> maxCount = 2;
            case 1 -> maxCount = 4;
            case 2 -> maxCount = 6;
            case 3 -> maxCount = 8;
            case 4 -> maxCount = 10;
            case 5 -> maxCount = 15;
        }

        entries.addAll(generateStats(maxCount, remaining, color));

        final short finalIdItem = idItem;
        return entries.stream().map(e -> e.toOption(finalIdItem)).toList();
    }

    // =====================================================================
    // MEDAL V2 STAT SYSTEM — terpisah total dari sistem stat Medal V1 di atas
    // (createMedalStat / upgradeMedalStat). Pool & jumlah stat di bawah ini
    // BOLEH diubah bebas tanpa mempengaruhi Medal V1 sama sekali, karena
    // tidak ada objek/list yang di-share antara keduanya.
    //
    // Saat ini nilai-nilainya sengaja dibuat SAMA dengan V1 sebagai baseline
    // awal — silakan sesuaikan minValue/maxValue atau MEDAL_V2_MAX_STAT_BY_COLOR
    // di bawah kalau ingin Medal V2 lebih kuat/berbeda dari V1.
    // =====================================================================

    // Index: 0=Putih, 1=Biru, 2=Kuning, 3=Ungu, 4=Oranye, 5=Hijau
    private static final int[] MEDAL_V2_MAX_STAT_BY_COLOR = new int[]{2, 4, 6, 8, 10, 15};
    private static final int[] MEDAL_V2_UPGRADE_MAX_STAT_BY_COLOR = new int[]{4, 6, 8, 10, 12, 14};

    public List<Option> createMedalStatV2(int clazz, int color) {
        short itemId = template.MedalV2Config.getMedalItemIdFromIndexV2(clazz);
        if (itemId == -1) {
            itemId = (clazz >= 0 && clazz <= 3) ? (short) (clazz + 4587) : (short) 5288;
        }
        return createMedalStatV2(clazz, color, itemId);
    }

    public List<Option> createMedalStatV2(int clazz, int color, short idItem) {
        List<StatEntry> entries = new ArrayList<>();

        // Base Damage sesuai clazz — pool sendiri, tidak di-share dengan V1
        int minValue = 1000;
        int maxValue = 1100;

        List<StatConfig> base = getBaseDmgConfig(clazz, minValue, maxValue);
        StatEntry dmg = genSingle(base, color);

        if (dmg == null) {
            return List.of();
        }

        entries.add(dmg);

        minValue = color;
        maxValue = color + 1;

        StatConfig config = getPercentDmg(dmg, minValue, maxValue);

        if (config != null) {
            double value = config.generateRandomValue(random, color);
            entries.add(new StatEntry(config.getOption(), value));
        }

        // Pool Medal V2, dikelompokkan per grup (pasangan RATE+VALUE selalu ikut bersama)
        List<List<StatConfig>> groups = buildMedalV2Pool();

        int maxCount = (color >= 0 && color < MEDAL_V2_MAX_STAT_BY_COLOR.length)
                ? MEDAL_V2_MAX_STAT_BY_COLOR[color] : 0;

        entries.addAll(generateGroupedStats(maxCount, groups, color));

        final short finalIdItem = idItem;
        return entries.stream().map(e -> e.toOption(finalIdItem)).toList();
    }

    /**
     * Menambah 1 stat baru ke Medal V2 saat upgrade berhasil.
     * Duplikat independen dari upgradeMedalStat() (Medal V1) — pool & batas
     * maksimum stat di sini bisa diubah tanpa menyentuh Medal V1.
     */
    public void upgradeMedalStatV2(Item3 item) {
        int maxStats = (item.color >= 0 && item.color < MEDAL_V2_UPGRADE_MAX_STAT_BY_COLOR.length)
                ? MEDAL_V2_UPGRADE_MAX_STAT_BY_COLOR[item.color]
                : MEDAL_V2_UPGRADE_MAX_STAT_BY_COLOR[0];

        int currentStatCount = 0;
        for (Option op : item.op) {
            if (op == null) continue;
            int oid = op.id & 0xFF;
            if (oid == 96) continue;
            if ((oid >= 58 && oid <= 60) || (oid >= 100 && oid <= 107)) continue;
            currentStatCount++;
        }

        if (currentStatCount >= maxStats) {
            return;
        }

        Set<EnumOption> usedOptions = new HashSet<>();
        for (Option op : item.op) {
            EnumOption e = EnumOption.fromId(op.id);
            if (e != null) usedOptions.add(e);
        }

        // Pool Medal V2, dikelompokkan per grup — pasangan RATE+VALUE (mis. FIRE_BURN_RATE
        // & FIRE_BURN_VALUE) selalu ditambahkan bersama-sama saat upgrade, tidak pernah sendirian.
        List<List<StatConfig>> pool = buildMedalV2Pool();

        // Buang grup yang salah satu (atau semua) optionnya sudah ada di item
        pool.removeIf(group -> group.stream().anyMatch(cfg -> usedOptions.contains(cfg.getOption())));

        // Hanya pertahankan grup yang MUAT seluruhnya di sisa slot yang tersedia.
        // Kalau sisa slot cuma 1 tapi grup berisi rate+value (2 stat), grup itu dilewati
        // supaya tidak ada rate tanpa value (atau sebaliknya) yang ke-add sebagian.
        int availableSlots = maxStats - currentStatCount;
        pool.removeIf(group -> group.size() > availableSlots);

        if (pool.isEmpty()) return; // Tidak ada grup yang muat / semua slot sudah terisi

        List<StatConfig> chosenGroup = pool.get(random.nextInt(pool.size()));
        for (StatConfig cfg : chosenGroup) {
            double value = cfg.generateRandomValue(random, item.color);
            item.op.add(new StatEntry(cfg.getOption(), value).toOption(item.id));
        }
    }

    public List<Option> createTitleStat(int clazz, int color) {
        List<StatEntry> entries = new ArrayList<>();

        List<StatConfig> config = getBaseDmgPercentConfig(clazz, 30, 30);
        switch (clazz) {
            case 0 -> config.addAll(List.of(

                    new StatConfig(HP, 30, 30),
                    new StatConfig(STR, 30, 30),
                    new StatConfig(VIT, 30, 30)

            ));
            case 1 -> config.addAll(List.of(

                    new StatConfig(HP, 30, 30),
                    new StatConfig(INT, 30, 30),
                    new StatConfig(VIT, 30, 30)
            ));
            case 2 -> config.addAll(List.of(

                    new StatConfig(HP, 30, 30),
                    new StatConfig(STR, 30, 30),
                    new StatConfig(DEX, 30, 30)
            ));
            case 3 -> config.addAll(List.of(

                    new StatConfig(HP, 30, 30),
                    new StatConfig(INT, 30, 30),
                    new StatConfig(DEX, 30, 30)
            ));
        }

        for (StatConfig sc : config) {
            entries.add(new StatEntry(sc.getOption(), sc.generateRandomValue(random, color)));
        }

        return entries.stream().map(StatEntry::toOption).toList();
    }

    public List<Option> createCloakStat(int clazz, int color) {
        List<StatEntry> entries = new ArrayList<>();

        List<StatConfig> config = getBaseDmgPercentConfig(clazz, 60, 60);
        switch (clazz) {
            case 0 -> config.addAll(List.of(
                    new StatConfig(DEFENSE, 1000, 1000),
                    new StatConfig(STR, 60, 60),
                    new StatConfig(VIT, 60, 60),
                    new StatConfig(HP, 20, 20),
                    new StatConfig(CRIT, 10, 10),
                    new StatConfig(REFLECT, 10, 10),
                    new StatConfig(HP_REGEN, 10, 10)
            ));
            case 1 -> config.addAll(List.of(
                    new StatConfig(DEFENSE, 1000, 1100),
                    new StatConfig(INT, 60, 60),
                    new StatConfig(VIT, 60, 60),
                    new StatConfig(HP, 20, 20),
                    new StatConfig(CRIT, 10, 10),
                    new StatConfig(REFLECT, 10, 10),
                    new StatConfig(HP_REGEN, 10, 10)
            ));
            case 2 -> config.addAll(List.of(
                    new StatConfig(DEFENSE, 1000, 1000),
                    new StatConfig(STR, 60, 60),
                    new StatConfig(DEX, 60, 60),
                    new StatConfig(HP, 20, 20),
                    new StatConfig(CRIT, 10, 10),
                    new StatConfig(EVADE, 10, 10),
                    new StatConfig(HP_REGEN, 10, 10)
            ));
            case 3 -> config.addAll(List.of(
                    new StatConfig(DEFENSE, 1000, 1000),
                    new StatConfig(INT, 60, 60),
                    new StatConfig(DEX, 60, 60),
                    new StatConfig(HP, 20, 20),
                    new StatConfig(CRIT, 10, 10),
                    new StatConfig(EVADE, 10, 10),
                    new StatConfig(HP_REGEN, 10, 10)
            ));
        }

        for (StatConfig sc : config) {
            entries.add(new StatEntry(sc.getOption(), sc.generateRandomValue(random, color)));
        }

        return entries.stream().map(StatEntry::toOption).toList();
    }

    /**
     * Menambah 1 stat baru ke medal saat upgrade berhasil.
     * Dipanggil setiap kali tier medal naik.
     * Stat yang sudah ada tidak akan diduplikat.
     * Jumlah stat maksimum tetap sesuai warna medal:
     *   Oranye (4) = 12, Ungu (3) = 10, Kuning (2) = 8, Biru (1) = 6, Putih (0) = 4
     */
    public void upgradeMedalStat(Item3 item) {
        // Hitung batas maksimum stat berdasarkan warna medal
        int maxStats;
        switch (item.color) {
            case 4 -> maxStats = 12;
            case 3 -> maxStats = 10;
            case 2 -> maxStats = 8;
            case 1 -> maxStats = 6;
            default -> maxStats = 4; // color 0 (putih)
        }

        // Hitung jumlah stat aktual (tidak termasuk op sistem yang tidak ditampilkan)
        int currentStatCount = 0;
        for (Option op : item.op) {
            if (op == null) continue;
            int oid = op.id & 0xFF; // treat as unsigned byte
            // Skip op sistem: countOP marker, item class info, buff internal
            if (oid == 96) continue; // countOP marker
            if ((oid >= 58 && oid <= 60) || (oid >= 100 && oid <= 107)) continue; // sistem/buff
            currentStatCount++;
        }

        // Jika sudah mencapai batas, tidak tambah stat baru
        if (currentStatCount >= maxStats) {
            return;
        }

        // Kumpulkan EnumOption yang sudah ada agar tidak duplikat
        Set<EnumOption> usedOptions = new HashSet<>();
        for (Option op : item.op) {
            EnumOption e = EnumOption.fromId(op.id);
            if (e != null) usedOptions.add(e);
        }

        List<StatConfig> pool = new ArrayList<>(List.of(
                new StatConfig(DEFENSE, 1000, 1100),
                new StatConfig(DEFENSE_P, 1, 2),
                new StatConfig(PHYSICAL_RES, 10, 11),
                new StatConfig(ICE_RES, 10, 11),
                new StatConfig(FIRE_RES, 10, 11),
                new StatConfig(LIGHTING_RES, 10, 11),
                new StatConfig(POISON_RES, 10, 11),
                new StatConfig(STR, 1, 5),
                new StatConfig(DEX, 1, 5),
                new StatConfig(VIT, 1, 5),
                new StatConfig(INT, 1, 5),
                new StatConfig(HP, 10, 11),
                new StatConfig(MP, 10, 11),
                new StatConfig(HP_REGEN, 1, 2),
                new StatConfig(MP_REGEN, 1, 2),
                new StatConfig(LIFE_STEAL, 1, 2),
                new StatConfig(MP_STEAL, 1, 2),
                new StatConfig(CRIT, 1, 2),
                new StatConfig(EVADE, 1, 2),
                new StatConfig(REFLECT, 1, 2),
                new StatConfig(ATK_SKILL, 1, 1),
                new StatConfig(PASSIVE_SKILL, 1, 1),
                new StatConfig(PENETRATION_ARMOR, 1, 2),
                new StatConfig(EXP, 5, 5)
        ));

        // Hapus stat yang sudah ada dari pool agar tidak duplikat
        pool.removeIf(cfg -> usedOptions.contains(cfg.getOption()));

        if (pool.isEmpty()) return; // Semua slot sudah terisi

        int selectedIndex = random.nextInt(pool.size());
        StatConfig chosen = pool.get(selectedIndex);
        double value = chosen.generateRandomValue(random, item.color);
        item.op.add(new StatEntry(chosen.getOption(), value).toOption(item.id));
    }

    public void changeBaseDamage(Item3 item) {
        var base = List.of(PHYSICAL_DMG, FIRE_DMG, ICE_DMG, POISON_DMG, LIGHTING_DMG);
        var random = new Random();

        for (var opt : item.op) {
            var current = EnumOption.fromId(opt.id);
            if (base.contains(current)) {
                var newEnum = base.stream()
                        .filter(e -> e != current)
                        .skip(random.nextInt(base.size() - 1))
                        .findFirst()
                        .orElse(current);

                opt.id = (byte) newEnum.getId();
                System.out.println("Changed " + current + " → " + newEnum);
                break;
            }
        }
    }

    public void changeBaseDamagePercent(Item3 item) {
        var base = List.of(PHYSICAL_DMG_P, FIRE_DMG_P, ICE_DMG_P, POISON_DMG_P, LIGHTING_DMG_P);
        var random = new Random();

        for (var opt : item.op) {
            var current = EnumOption.fromId(opt.id);
            if (base.contains(current)) {
                var newEnum = base.stream()
                        .filter(e -> e != current)
                        .skip(random.nextInt(base.size() - 1))
                        .findFirst()
                        .orElse(current);

                opt.id = (byte) newEnum.getId();
                System.out.println("Changed " + current + " → " + newEnum);
                break;
            }
        }
    }

    private static List<StatConfig> getBaseDmgConfig(int clazz, int minValue, int maxValue) {

        switch (clazz) {
            case 2 -> {
                return List.of(
                        new StatConfig(PHYSICAL_DMG, minValue, maxValue),
                        new StatConfig(POISON_DMG, minValue, maxValue));
            }
            case 1 -> {
                return List.of(
                        new StatConfig(PHYSICAL_DMG, minValue, maxValue),
                        new StatConfig(ICE_DMG, minValue, maxValue));
            }
            case 3 -> {
                return List.of(
                        new StatConfig(PHYSICAL_DMG, minValue, maxValue),
                        new StatConfig(LIGHTING_DMG, minValue, maxValue));
            }
            default -> {
                return List.of(
                        new StatConfig(PHYSICAL_DMG, minValue, maxValue),
                        new StatConfig(FIRE_DMG, minValue, maxValue));
            }
        }
    }

    private static List<StatConfig> getBaseDmgPercentConfig(int clazz, int minValue, int maxValue) {

        switch (clazz) {
            case 2 -> {
                return new ArrayList<>(List.of(
                        new StatConfig(PHYSICAL_DMG_P, minValue, maxValue),
                        new StatConfig(POISON_DMG_P, minValue, maxValue)));
            }
            case 1 -> {
                return new ArrayList<>(List.of(
                        new StatConfig(PHYSICAL_DMG_P, minValue, maxValue),
                        new StatConfig(ICE_DMG_P, minValue, maxValue)));
            }
            case 3 -> {
                return new ArrayList<>(List.of(
                        new StatConfig(PHYSICAL_DMG_P, minValue, maxValue),
                        new StatConfig(LIGHTING_DMG_P, minValue, maxValue)));
            }
            default -> {
                return new ArrayList<>(List.of(
                        new StatConfig(PHYSICAL_DMG_P, minValue, maxValue),
                        new StatConfig(FIRE_DMG_P, minValue, maxValue)));
            }
        }
    }


    private static StatConfig getPercentDmg(StatEntry dmg, int minValue, int maxValue) {
        StatConfig config = null;
        switch (dmg.option()) {
            case PHYSICAL_DMG -> config = new StatConfig(PHYSICAL_DMG_P, minValue, maxValue);
            case FIRE_DMG -> config = new StatConfig(FIRE_DMG_P, minValue, maxValue);
            case ICE_DMG -> config = new StatConfig(ICE_DMG_P, minValue, maxValue);
            case POISON_DMG -> config = new StatConfig(POISON_DMG_P, minValue, maxValue);
            case LIGHTING_DMG -> config = new StatConfig(LIGHTING_DMG_P, minValue, maxValue);
        }
        return config;
    }


}