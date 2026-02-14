/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.player;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

import java.lang.reflect.Field;

/**
 * AirStuck - Freezes the game completely (timer = 0)
 * Same effect as GameSpeed at 0.00
 *
 * Mouse button handling: When mouse buttons are pressed, temporarily unfreezes
 * the game to process the input, preventing the "frozen" feeling.
 */
public class AirStuck extends Module {
    private static Field timerField;
    private static Field timerSpeedField;

    // Keybind watcher thread (runs in real time to catch keybind even when frozen)
    private Thread keybindWatcher = null;
    private volatile boolean watcherRunning = false;

    // Mouse button watcher thread (processes mouse inputs when frozen)
    private Thread mouseWatcher = null;
    private volatile boolean mouseWatcherRunning = false;

    // Track mouse button states for edge detection
    private volatile boolean lastLeftMouse = false;
    private volatile boolean lastRightMouse = false;

    // Cooldown to prevent instant re-enable
    private static long lastWatcherDisable = 0;
    private static final long WATCHER_COOLDOWN_MS = 80;

    static {
        try {
            Minecraft mc = Minecraft.getMinecraft();

            for (Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("Timer")) {
                    f.setAccessible(true);
                    timerField = f;
                    break;
                }
            }

            if (timerField != null) {
                Timer timer = (Timer) timerField.get(mc);
                for (Field f : timer.getClass().getDeclaredFields()) {
                    if (f.getType() == float.class) {
                        f.setAccessible(true);
                        String name = f.getName();
                        if (name.equals("timerSpeed") || name.equals("field_74278_d")) {
                            timerSpeedField = f;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[AirStuck] Error initializing: " + e.getMessage());
        }
    }

    public AirStuck() {
        super("AirStuck", ModuleCategory.PLAYER);
        this.registerSetting(new DescriptionSetting("Freezes the game completely"));
    }

    @Override
    public void onEnable() {
        // Check cooldown
        if (System.currentTimeMillis() - lastWatcherDisable < WATCHER_COOLDOWN_MS) {
            enabled = false;
            return;
        }

        if (timerField == null || timerSpeedField == null) {
            System.out.println("[AirStuck] ERROR: Timer fields not found!");
            this.toggle();
            return;
        }

        try {
            Timer timer = (Timer) timerField.get(mc);
            timerSpeedField.setFloat(timer, 0.0f);
            System.out.println("[AirStuck] Enabled - Game frozen");
        } catch (Exception e) {
            System.out.println("[AirStuck] Error: " + e.getMessage());
        }

        // Start keybind watcher for real-time disable
        startKeybindWatcher();

        // Start mouse watcher to process mouse inputs while frozen
        startMouseWatcher();
    }

    @Override
    public void onDisable() {
        stopKeybindWatcher();
        stopMouseWatcher();

        try {
            if (timerField != null && timerSpeedField != null) {
                Timer timer = (Timer) timerField.get(mc);
                timerSpeedField.setFloat(timer, 1.0f);
                System.out.println("[AirStuck] Disabled - Game resumed");
            }
        } catch (Exception e) {
            System.out.println("[AirStuck] Error resetting: " + e.getMessage());
        }
    }

    @Override
    public void onUpdate() {
        // Keep timer at 0 in case something resets it
        if (!enabled || mc == null) return;

        // Auto-disable on disconnect
        if (getPlayer() == null || getWorld() == null) {
            setEnabled(false);
            return;
        }

        // Restore timer when GUI is open
        boolean isGuiOpen = mc.currentScreen != null;
        try {
            Timer timer = (Timer) timerField.get(mc);
            if (isGuiOpen) {
                timerSpeedField.setFloat(timer, 1.0f);
            } else {
                timerSpeedField.setFloat(timer, 0.0f);
            }
        } catch (Exception ignored) {}
    }

    private void startKeybindWatcher() {
        if (keybindWatcher != null && keybindWatcher.isAlive()) {
            return;
        }

        watcherRunning = true;
        keybindWatcher = new Thread(() -> {
            int key1 = this.getKeyCode();
            int key2 = this.getToggleBind();
            int key3 = this.getHoldBind();

            int moduleKeyCode = key1 != 0 ? key1 : (key2 != 0 ? key2 : key3);

            // Determine if it's a mouse button and which one
            // Minecraft uses -100 offset for mouse buttons: -100=M1, -101=M2, -102=M3, -103=M4, -104=M5
            boolean isMouse = moduleKeyCode < 0 && moduleKeyCode >= -105;
            int mouseButton = isMouse ? (moduleKeyCode + 100) * -1 : -1;

            System.out.println("[AirStuck] Keybind watcher started. keyCode=" + moduleKeyCode +
                ", isMouse=" + isMouse + ", mouseButton=" + mouseButton);

            if (moduleKeyCode == 0) {
                System.out.println("[AirStuck] No keybind set, watcher exiting");
                return;
            }

            // Wait for initial key release
            try {
                Thread.sleep(300);
                int waitCount = 0;
                while (watcherRunning && isKeyOrMouseDown(moduleKeyCode, isMouse, mouseButton)) {
                    Thread.sleep(10);
                    waitCount++;
                    if (waitCount > 200) break;
                }
                System.out.println("[AirStuck] Initial key released, now watching...");
            } catch (InterruptedException e) {
                return;
            }

            boolean wasPressed = false;

            while (watcherRunning && enabled) {
                try {
                    boolean isPressed = isKeyOrMouseDown(moduleKeyCode, isMouse, mouseButton);

                    if (!wasPressed && isPressed) {
                        System.out.println("[AirStuck] Keybind detected! Disabling...");
                        // Reset timer immediately
                        try {
                            if (timerField != null && timerSpeedField != null) {
                                Timer timer = (Timer) timerField.get(mc);
                                timerSpeedField.setFloat(timer, 1.0f);
                            }
                        } catch (Exception ignored) {}

                        watcherRunning = false;
                        enabled = false;
                        lastWatcherDisable = System.currentTimeMillis();

                        System.out.println("[AirStuck] Disabled via keybind watcher");
                        break;
                    }

                    wasPressed = isPressed;
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception ignored) {}
            }
        }, "AirStuck-KeybindWatcher");

        keybindWatcher.setDaemon(true);
        keybindWatcher.start();
    }

    private void stopKeybindWatcher() {
        watcherRunning = false;
        if (keybindWatcher != null) {
            keybindWatcher.interrupt();
            keybindWatcher = null;
        }
    }

    private boolean isKeyOrMouseDown(int keyCode, boolean isMouse, int mouseButton) {
        try {
            if (isMouse) {
                // Make sure mouseButton is valid (0-4 for standard mice, up to ~12 for gaming mice)
                if (mouseButton >= 0 && mouseButton < org.lwjgl.input.Mouse.getButtonCount()) {
                    return org.lwjgl.input.Mouse.isButtonDown(mouseButton);
                }
                return false;
            } else {
                return org.lwjgl.input.Keyboard.isKeyDown(keyCode);
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Starts a thread that watches for mouse button presses and temporarily
     * unfreezes the game to process them, preventing the "frozen" feeling.
     */
    private void startMouseWatcher() {
        if (mouseWatcher != null && mouseWatcher.isAlive()) {
            return;
        }

        mouseWatcherRunning = true;
        lastLeftMouse = false;
        lastRightMouse = false;

        mouseWatcher = new Thread(() -> {
            System.out.println("[AirStuck] Mouse watcher started");

            while (mouseWatcherRunning && enabled) {
                try {
                    boolean leftDown = org.lwjgl.input.Mouse.isButtonDown(0);
                    boolean rightDown = org.lwjgl.input.Mouse.isButtonDown(1);

                    // Detect new mouse press (edge detection)
                    boolean leftPressed = leftDown && !lastLeftMouse;
                    boolean rightPressed = rightDown && !lastRightMouse;

                    if (leftPressed || rightPressed) {
                        // Temporarily unfreeze game to process input
                        try {
                            if (timerField != null && timerSpeedField != null && mc.currentScreen == null) {
                                Timer timer = (Timer) timerField.get(mc);

                                // Unfreeze briefly
                                timerSpeedField.setFloat(timer, 1.0f);

                                // Wait a bit for input to be processed
                                Thread.sleep(50);

                                // Re-freeze if still enabled and no GUI
                                if (enabled && mc.currentScreen == null) {
                                    timerSpeedField.setFloat(timer, 0.0f);
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    lastLeftMouse = leftDown;
                    lastRightMouse = rightDown;

                    Thread.sleep(5); // Fast polling for responsive input
                } catch (InterruptedException e) {
                    break;
                } catch (Exception ignored) {}
            }

            System.out.println("[AirStuck] Mouse watcher stopped");
        }, "AirStuck-MouseWatcher");

        mouseWatcher.setDaemon(true);
        mouseWatcher.start();
    }

    /**
     * Stops the mouse watcher thread
     */
    private void stopMouseWatcher() {
        mouseWatcherRunning = false;
        if (mouseWatcher != null) {
            mouseWatcher.interrupt();
            mouseWatcher = null;
        }
    }
}
