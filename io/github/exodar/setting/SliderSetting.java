/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.setting;

public class SliderSetting extends Setting {
    private double value;
    private final double min;
    private final double max;
    private final double interval;
    private final double defaultValue;
    private Runnable onChangeCallback;
    private String minDisplayText;

    public SliderSetting(String name, double defaultValue, double min, double max, double interval) {
        super(name);
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.interval = interval;
        this.defaultValue = defaultValue;
        this.minDisplayText = null;
    }

    public double getValue() {
        return round(value, 2);
    }

    public void setValue(double value) {
        value = clamp(value, min, max);
        value = Math.round(value * (1.0 / interval)) / (1.0 / interval);
        boolean changed = this.value != value;
        this.value = value;
        if (changed) {
            triggerOnChange();
        }
    }

    /**
     * Set a callback to be called when the setting changes
     */
    public SliderSetting onChange(Runnable callback) {
        this.onChangeCallback = callback;
        return this;
    }

    /**
     * Set custom display text when the value is at minimum
     */
    public SliderSetting setMinDisplayText(String text) {
        this.minDisplayText = text;
        return this;
    }

    /**
     * Get the display text for the current value
     */
    public String getDisplayText() {
        if (minDisplayText != null && value == min) {
            return minDisplayText;
        }
        if (interval >= 1.0) {
            return String.valueOf((int) getValue());
        }
        return String.valueOf(getValue());
    }

    /**
     * Check if currently at minimum value
     */
    public boolean isAtMin() {
        return value == min;
    }

    private void triggerOnChange() {
        if (onChangeCallback != null) {
            try {
                onChangeCallback.run();
            } catch (Exception e) {
                // Silent fail
            }
        }
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
        boolean changed = this.value != defaultValue;
        this.value = defaultValue;
        if (changed) {
            triggerOnChange();
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}
