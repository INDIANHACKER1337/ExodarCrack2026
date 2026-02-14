/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.setting;

/**
 * Mode setting - permite seleccionar entre múltiples opciones
 */
public class ModeSetting extends Setting {
    private String[] modes;
    private int currentIndex = 0;
    private Runnable onChangeCallback;

    public ModeSetting(String name, String[] modes) {
        super(name);
        this.modes = modes;
        this.currentIndex = 0;
    }

    public String getSelected() {
        if (modes == null || modes.length == 0) return "";
        return modes[currentIndex];
    }

    /**
     * Alias for getSelected() - for compatibility
     */
    public String getValue() {
        return getSelected();
    }

    /**
     * Check if current mode equals the given value
     */
    public boolean is(String modeName) {
        return getSelected().equalsIgnoreCase(modeName);
    }

    public void cycle() {
        currentIndex = (currentIndex + 1) % modes.length;
        triggerOnChange();
    }

    public void cycleBack() {
        currentIndex = (currentIndex - 1 + modes.length) % modes.length;
        triggerOnChange();
    }

    public String[] getModes() {
        return modes;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int index) {
        if (index >= 0 && index < modes.length) {
            boolean changed = this.currentIndex != index;
            this.currentIndex = index;
            if (changed) {
                triggerOnChange();
            }
        }
    }

    public void setSelected(String mode) {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(mode)) {
                boolean changed = this.currentIndex != i;
                this.currentIndex = i;
                if (changed) {
                    triggerOnChange();
                }
                return;
            }
        }
    }

    /**
     * Set a callback to be called when the mode changes
     */
    public ModeSetting onChange(Runnable callback) {
        this.onChangeCallback = callback;
        return this;
    }

    private void triggerOnChange() {
        if (onChangeCallback != null) {
            try {
                onChangeCallback.run();
            } catch (Exception e) {
                System.out.println("[ModeSetting] Error in onChange callback: " + e.getMessage());
            }
        }
    }

    @Override
    public void resetToDefault() {
        boolean changed = this.currentIndex != 0;
        this.currentIndex = 0;
        if (changed) {
            triggerOnChange();
        }
    }
}
