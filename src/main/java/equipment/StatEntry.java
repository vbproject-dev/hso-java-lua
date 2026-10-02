package equipment;

import template.Option;

public record StatEntry(EnumOption option, double value) {

    public Option toOption() {
        return new Option(option.getId(), option.isPercent() ? (int) (value * 100) : (int) value);
    }

    public Option toOption(short idItem) {
        return new Option(option.getId(), option.isPercent() ? (int) (value * 100) : (int) value, idItem);
    }

    @Override
    public String toString() {
        String displayValue = option.isPercent()
                ? String.format("%.1f%%", value)
                : String.format("%.0f", value);
        return option.name() + ": " + displayValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StatEntry that = (StatEntry) obj;
        return Double.compare(that.value, value) == 0 && option == that.option;
    }

}