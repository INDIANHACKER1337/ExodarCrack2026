/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.setting;

/**
 * Setting para texto editable (ej: nombre del cliente)
 */
public class TextSetting extends Setting {
    private String value;
    private String defaultValue;
    private int maxLength;
    private Runnable onChangeCallback;

    public TextSetting(String name, String defaultValue) {
        this(name, defaultValue, 32);
    }

    public TextSetting(String name, String defaultValue, int maxLength) {
        super(name);
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.maxLength = maxLength;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        if (value != null && value.length() <= maxLength) {
            boolean changed = !value.equals(this.value);
            this.value = value;
            if (changed) {
                triggerOnChange();
            }
        }
    }

    /**
     * Set a callback to be called when the setting changes
     */
    public TextSetting onChange(Runnable callback) {
        this.onChangeCallback = callback;
        return this;
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

    public String getDefaultValue() {
        return defaultValue;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void reset() {
        this.value = defaultValue;
    }

    @Override
    public void resetToDefault() {
        boolean changed = !defaultValue.equals(this.value);
        this.value = defaultValue;
        if (changed) {
            triggerOnChange();
        }
    }
}
