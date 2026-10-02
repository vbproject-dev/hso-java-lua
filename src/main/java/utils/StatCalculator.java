package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Scalable Stat Calculator for equipment stats based on:
 * - Level scaling
 * - Color tier
 * - Upgrade tier (+1 to +15)
 */
public class StatCalculator {

    /** Reference level (table baseline) */
    private static final int REFERENCE_LEVEL = 200;

    /** Exponent curve (1.0 = linear, >1 = exponential) */
    private static final double LEVEL_EXPONENT = 0.02;

    /** Upgrade bonus per tier (5% per upgrade) */
    private static final double UPGRADE_STEP = 0.05;

    /** Reference stat table at REFERENCE_LEVEL */
    private static final Map<Integer, StatRange> BASE_RANGES = new HashMap<>();

    static {
        BASE_RANGES.put(5, new StatRange(6500, 7000)); // Mythic
        BASE_RANGES.put(4, new StatRange(5500, 6000)); // Legendary
        BASE_RANGES.put(3, new StatRange(5000, 5500)); // Epic
        BASE_RANGES.put(2, new StatRange(4500, 5000)); // Rare
        BASE_RANGES.put(1, new StatRange(4000, 4500)); // Uncommon
        BASE_RANGES.put(0, new StatRange(3500, 4000)); // Common
    }

    private static final Map<Integer, StatRange> PERCENT_RANGES = new HashMap<>();

    static {

        PERCENT_RANGES.put(5, new StatRange(8000, 8500)); // Mythic
        PERCENT_RANGES.put(4, new StatRange(6000, 6500)); // Legendary
        PERCENT_RANGES.put(3, new StatRange(5500, 6000)); // Epic
        PERCENT_RANGES.put(2, new StatRange(5000, 5500)); // Rare
        PERCENT_RANGES.put(1, new StatRange(4500, 5000)); // Uncommon
        PERCENT_RANGES.put(0, new StatRange(4000, 4500)); // Common
    }


    public static StatResult generateFlat(int colorTier, int equipLevel) {
        validateInputs(colorTier, equipLevel, 1);

        StatRange base = BASE_RANGES.get(colorTier);
        double levelScale = getLevelScale(equipLevel);


        // Scale min and max with both level and upgrade multipliers
        int scaledMin = (int) Math.round(base.min * levelScale );
        int scaledMax = (int) Math.round(base.max * levelScale);

        int value = randomBetween(scaledMin, scaledMax) / 2;

        return new StatResult(scaledMin, scaledMax, value);
    }

    public static StatResult generatePercent(int colorTier, int equipLevel) {
        validateInputs(colorTier, equipLevel, 1);

        StatRange base = PERCENT_RANGES.get(colorTier);
        double levelScale = getLevelScale(equipLevel);

        // Scale min and max with both level and upgrade multipliers
        int scaledMin = (int) Math.round(base.min * levelScale);
        int scaledMax = (int) Math.round(base.max * levelScale);

        int value = randomBetween(scaledMin, scaledMax);

        return new StatResult(scaledMin, scaledMax, value);
    }

    /** Compute scaling from level */
    private static double getLevelScale(int level) {
        return Math.pow((double) level / REFERENCE_LEVEL, LEVEL_EXPONENT);
    }

    /** Compute bonus from upgrade tier */
    private static double getUpgradeScale(int upgradeTier) {
        return 1.0 + (upgradeTier * UPGRADE_STEP);
    }

    /** Random integer between min and max inclusive */
    private static int randomBetween(int min, int max) {
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static void validateInputs(int colorTier, int level, int upgradeTier) {
        if (!BASE_RANGES.containsKey(colorTier)) {
            throw new IllegalArgumentException("Invalid color tier: " + colorTier);
        }
        if (level <= 0) {
            throw new IllegalArgumentException("Level must be positive: " + level);
        }
        if (upgradeTier < 0 || upgradeTier > 15) {
            throw new IllegalArgumentException("Upgrade tier must be between +0 and +15");
        }
    }

    // ---------------- Value Objects ----------------

    public static final class StatRange {
        public final int min;
        public final int max;
        public StatRange(int min, int max) {
            this.min = min;
            this.max = max;
        }
    }

    public static final class StatResult {
        public final int min;
        public final int max;
        public final int value;
        public StatResult(int min, int max, int value) {
            this.min = min;
            this.max = max;
            this.value = value;
        }

    }

}
