/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.setting;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * A setting for binding a key or mouse button - renders as a button in the GUI
 * Similar to the Toggle/Hold keybind buttons
 *
 * KeyCode format:
 * - Positive numbers = keyboard keys (Keyboard.KEY_*)
 * - Negative numbers = mouse buttons (-100 + button number)
 *   e.g., -100 = Mouse 0 (left), -99 = Mouse 1 (right), -98 = Mouse 2 (middle), etc.
 */
public class KeybindSetting extends Setting {
    private int keyCode;
    private final int defaultKeyCode;
    private boolean waitingForKey;

    public KeybindSetting(String name) {
        this(name, 0);
    }

    public KeybindSetting(String name, int defaultKeyCode) {
        super(name);
        this.keyCode = defaultKeyCode;
        this.defaultKeyCode = defaultKeyCode;
        this.waitingForKey = false;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
        this.waitingForKey = false;
    }

    /**
     * Set from a mouse button (will be stored as -100 + button)
     */
    public void setMouseButton(int button) {
        this.keyCode = -100 + button;
        this.waitingForKey = false;
    }

    public boolean isWaitingForKey() {
        return waitingForKey;
    }

    public void setWaitingForKey(boolean waiting) {
        this.waitingForKey = waiting;
    }

    public void startWaiting() {
        this.waitingForKey = true;
    }

    public void clearKey() {
        this.keyCode = 0;
        this.waitingForKey = false;
    }

    /**
     * Check if this is a mouse button binding
     */
    public boolean isMouseButton() {
        return keyCode < 0 && keyCode >= -110;
    }

    /**
     * Get the mouse button number (0-indexed)
     */
    public int getMouseButtonNumber() {
        return keyCode + 100;
    }

    /**
     * Check if the bound key/button is currently pressed
     */
    public boolean isKeyPressed() {
        if (keyCode == 0) return false;
        try {
            if (isMouseButton()) {
                int button = getMouseButtonNumber();
                return Mouse.isButtonDown(button);
            } else {
                return Keyboard.isKeyDown(keyCode);
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the display name of the bound key/button
     */
    public String getKeyName() {
        if (waitingForKey) {
            return "...";
        }
        if (keyCode == 0) {
            return "-";
        }
        try {
            if (isMouseButton()) {
                int button = getMouseButtonNumber();
                return "M" + (button + 1); // M1, M2, M3, M4, M5, etc.
            } else {
                return Keyboard.getKeyName(keyCode);
            }
        } catch (Exception e) {
            return "?";
        }
    }

    @Override
    public void resetToDefault() {
        this.keyCode = defaultKeyCode;
        this.waitingForKey = false;
    }
}
