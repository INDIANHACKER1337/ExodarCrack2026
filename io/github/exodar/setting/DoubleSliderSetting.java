/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.setting;

public class DoubleSliderSetting extends Setting {
    private double valueMin;
    private double valueMax;
    private final double min;
    private final double max;
    private final double interval;
    private final double defaultValueMin;
    private final double defaultValueMax;

    public DoubleSliderSetting(String name, double defaultValueMin, double defaultValueMax,
                              double min, double max, double interval) {
        super(name);
        this.valueMin = defaultValueMin;
        this.valueMax = defaultValueMax;
        this.min = min;
        this.max = max;
        this.interval = interval;
        this.defaultValueMin = defaultValueMin;
        this.defaultValueMax = defaultValueMax;
    }

    public double getValueMin() {
        return round(valueMin, 2);
    }

    public double getValueMax() {
        return round(valueMax, 2);
    }

    public void setValueMin(double value) {
        value = clamp(value, min, valueMax);
        value = Math.round(value * (1.0 / interval)) / (1.0 / interval);
        this.valueMin = value;
    }

    public void setValueMax(double value) {
        value = clamp(value, valueMin, max);
        value = Math.round(value * (1.0 / interval)) / (1.0 / interval);
        this.valueMax = value;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getInterval() {
        return interval;
    }

    @Override
    public void resetToDefault() {
        this.valueMin = defaultValueMin;
        this.valueMax = defaultValueMax;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}
