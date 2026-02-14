/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.setting;

public class TickSetting extends Setting {
    private boolean enabled;
    private final boolean defaultValue;
    private Runnable onChangeCallback;

    public TickSetting(String name, boolean defaultValue) {
        super(name);
        this.enabled = defaultValue;
        this.defaultValue = defaultValue;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        boolean changed = this.enabled != enabled;
        this.enabled = enabled;
        if (changed) {
            triggerOnChange();
        }
    }

    public void toggle() {
        this.enabled = !this.enabled;
        triggerOnChange();
    }

    /**
     * Set a callback to be called when the setting changes
     */
    public TickSetting onChange(Runnable callback) {
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

    @Override
    public void resetToDefault() {
        boolean changed = this.enabled != defaultValue;
        this.enabled = defaultValue;
        if (changed) {
            triggerOnChange();
        }
    }
}
