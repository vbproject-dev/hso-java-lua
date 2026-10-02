package equipment;

import lombok.Getter;

import java.util.Random;

@Getter
public class StatConfig {
    private final EnumOption option;
    private double minValue;
    private double maxValue;

    public StatConfig(EnumOption option, double minValue, double maxValue) {
        if (minValue > maxValue) {
            throw new IllegalArgumentException("minValue cannot be greater than maxValue");
        }
        this.option = option;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * Generate a random value between min and max for this stat
     */
    public double generateRandomValue(Random random, int color) {
        if (minValue == maxValue) {
            return minValue;
        }

        if (option.isPercent()) {
            minValue = minValue + color;
            maxValue = maxValue + color;
        } else {
            switch (option) {
                case STR, VIT, DEX, INT -> {
                    minValue = minValue + (color * 5);
                    maxValue = maxValue + (color * 5);
                }

                case PASSIVE_SKILL, ATK_SKILL -> {
                    minValue = minValue + color;
                    maxValue = maxValue + color;
                }

                default -> {
                    minValue = minValue + (color * 100);
                    maxValue = maxValue + (color * 100);
                }
            }

        }

        double value = minValue + random.nextDouble() * (maxValue - minValue);

        // Round based on percent type
        if (option.isPercent()) {
            value = Math.round(value * 10.0) / 10.0; // Round to 1 decimal
        } else {
            value = Math.round(value); // Round to integer
        }

        return value;
    }
}