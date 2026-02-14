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
 * GameSpeed (Timer) - Modify game speed
 *
 * Normal mode: Simple adjustable timer speed (0.5x - 10.0x)
 *
 * Balance mode (Raven System):
 * - Charges balance while using slow timer
 * - Spends balance to boost timer speed
 * - Auto-slow: automatically switches between charging and boosting based on movement
 * - Balance = time accumulated at slow speed, consumed when boosting
 */
public class GameSpeed extends Module {
    private ModeSetting gameSpeedMode;
    private SliderSetting normalSpeed;

    // Balance mode settings (Raven system)
    private SliderSetting balanceBoostSpeed;
    private SliderSetting balanceSlowSpeed;
    private SliderSetting maxBalanceMs;
    private SliderSetting balanceCostMultiplier;
    private TickSetting autoSlowMode;
    private TickSetting autoDisableWhenEmpty;

    private static Field timerField;
    private static Field timerSpeedField;
    private int errorCount = 0;
    private static final int MAX_ERRORS = 5;

    // Balance mode state
    private long currentBalance = 0;
    private long lastUpdateTime = -1;
    private BalanceState currentBalanceState = BalanceState.NONE;

    // GUI safety
    private boolean guiCurrentlyOpen = false;
    private float savedTimerSpeed = 1.0f;

    // Track mode changes
    private String previousMode = "";

    // Keybind watcher thread (runs in real time to catch keybind even at low timer speeds)
    private Thread keybindWatcher = null;
    private volatile boolean watcherRunning = false;

    // Cooldown to prevent instant re-enable after watcher disables
    // 80ms = blocks instant duplicate (<20ms), allows fast human double-tap (80ms+)
    private static long lastWatcherDisable = 0;
    private static final long WATCHER_COOLDOWN_MS = 80;

    static {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            System.out.println("[GameSpeed] Searching for timer field...");

            for (Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("Timer")) {
                    f.setAccessible(true);
                    timerField = f;
                    System.out.println("[GameSpeed] Found timer: " + f.getName());
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
                            System.out.println("[GameSpeed] Found timerSpeed: " + name);
                            break;
                        }
                    }
                }
            }

            if (timerField == null || timerSpeedField == null) {
                System.out.println("[GameSpeed] WARNING: Could not find timer fields!");
            }

        } catch (Exception e) {
            System.out.println("[GameSpeed] Error initializing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public GameSpeed() {
        super("GameSpeed", ModuleCategory.PLAYER);
        this.registerSetting(new DescriptionSetting("Modify game speed"));
        this.registerSetting(gameSpeedMode = new ModeSetting("Mode", new String[]{"Normal", "Balance"})
            .onChange(this::onModeChange));

        // Normal mode (minimum 0 to completely freeze)
        // Keybind watcher thread ensures you can always disable even at 0x
        this.registerSetting(normalSpeed = new SliderSetting("Speed", 1.0, 0.0, 10.0, 0.01));

        // Balance mode (Raven system)
        this.registerSetting(balanceBoostSpeed = new SliderSetting("Boost Speed", 2.0, 1.0, 10.0, 0.1));
        this.registerSetting(balanceSlowSpeed = new SliderSetting("Slow Speed", 0.5, 0.1, 1.0, 0.1));
        this.registerSetting(maxBalanceMs = new SliderSetting("Max Balance", 1000, 0, 3000, 100));
        this.registerSetting(balanceCostMultiplier = new SliderSetting("Cost Multiplier", 1.0, 0.5, 5.0, 0.1));
        this.registerSetting(autoSlowMode = new TickSetting("Auto Slow", false));
        this.registerSetting(autoDisableWhenEmpty = new TickSetting("Auto Disable", true));

        // Set initial visibility
        updateSettingVisibility();
    }

    /**
     * Called when mode changes - reset state and update visibility
     */
    private void onModeChange() {
        System.out.println("[GameSpeed] Mode changed to: " + gameSpeedMode.getSelected());
        resetBalance();
        updateSettingVisibility();
        previousMode = gameSpeedMode.getSelected();
    }

    @Override
    public void onEnable() {
        // Check cooldown - prevent instant re-enable after watcher disabled
        if (System.currentTimeMillis() - lastWatcherDisable < WATCHER_COOLDOWN_MS) {
            System.out.println("[GameSpeed] Blocked re-enable (cooldown active)");
            enabled = false;
            return;
        }

        errorCount = 0;
        if (timerField == null || timerSpeedField == null) {
            System.out.println("[GameSpeed] ERROR: Module cannot function - timer fields not found!");
            this.toggle();
            return;
        }

        // Track current mode
        previousMode = gameSpeedMode.getSelected();

        if (previousMode.equals("Balance")) {
            resetBalance();
            System.out.println("[GameSpeed] Balance mode enabled");
        } else {
            System.out.println("[GameSpeed] Normal mode - Speed: " + normalSpeed.getValue());
        }

        // Start keybind watcher thread for real-time keybind detection
        // This allows disabling the module even at very low timer speeds (0.01x)
        startKeybindWatcher();
    }

    @Override
    public void onDisable() {
        System.out.println("[GameSpeed] Disabled - Resetting to normal speed");

        // Stop keybind watcher
        stopKeybindWatcher();

        try {
            if (timerField != null && timerSpeedField != null) {
                Timer timer = (Timer) timerField.get(mc);
                timerSpeedField.setFloat(timer, 1.0f);
            }
        } catch (Exception e) {
            System.out.println("[GameSpeed] Error resetting: " + e.getMessage());
        }

        resetBalance();
        guiCurrentlyOpen = false;
        savedTimerSpeed = 1.0f;
    }

    @Override
    public String getDisplaySuffix() {
        if (gameSpeedMode.getSelected().equals("Balance")) {
            int percentage = (int) ((currentBalance / (double) maxBalanceMs.getValue()) * 100);
            return " §7" + Math.min(percentage, 100) + "%";
        } else {
            if (normalSpeed != null) {
                return " §7x" + String.format("%.1f", normalSpeed.getValue());
            }
        }
        return "";
    }

    @Override
    public void onUpdate() {
        if (!enabled || mc == null || timerField == null || timerSpeedField == null) return;

        // Auto-disable on disconnect
        if (getPlayer() == null || getWorld() == null) {
            System.out.println("[GameSpeed] Auto-disabled (disconnect)");
            setEnabled(false);
            return;
        }

        // Detect mode changes and reset state
        String currentMode = gameSpeedMode.getSelected();
        if (!currentMode.equals(previousMode)) {
            System.out.println("[GameSpeed] Mode changed from " + previousMode + " to " + currentMode);
            resetBalance(); // Reset timer to 1.0 and clear balance state
            previousMode = currentMode;
        }

        // Update visibility
        updateSettingVisibility();

        try {
            // GUI SAFETY: Restore timer when GUI is open
            boolean isGuiOpen = mc.currentScreen != null;

            if (isGuiOpen && !guiCurrentlyOpen) {
                Timer timer = (Timer) timerField.get(mc);
                savedTimerSpeed = timerSpeedField.getFloat(timer);
                timerSpeedField.setFloat(timer, 1.0f);
                guiCurrentlyOpen = true;
            } else if (!isGuiOpen && guiCurrentlyOpen) {
                guiCurrentlyOpen = false;
            }

            // Only apply timer if no GUI is open
            if (!guiCurrentlyOpen) {
                if (gameSpeedMode.getSelected().equals("Balance")) {
                    updateBalanceMode();
                } else {
                    updateNormalMode();
                }
            }
        } catch (Exception e) {
            errorCount++;
            if (errorCount <= MAX_ERRORS) {
                System.out.println("[GameSpeed] Error in onUpdate: " + e.getMessage());
                e.printStackTrace();
            }
            if (errorCount == MAX_ERRORS) {
                System.out.println("[GameSpeed] Too many errors, disabling module");
                this.toggle();
            }
        }
    }

    private void updateNormalMode() throws Exception {
        Timer timer = (Timer) timerField.get(mc);
        float targetSpeed = (float) normalSpeed.getValue();
        timerSpeedField.setFloat(timer, targetSpeed);
    }

    private void updateBalanceMode() throws Exception {
        final long currentTime = System.currentTimeMillis();

        if (lastUpdateTime == -1) {
            lastUpdateTime = currentTime;
            return;
        }

        if (!canUseTimer()) {
            resetBalance();
            return;
        }

        switch (currentBalanceState) {
            case NONE:
                lastUpdateTime = currentTime;
                if (autoSlowMode.isEnabled() && isPlayerMoving()) break;
                setTimerSpeed((float) balanceSlowSpeed.getValue());
                currentBalanceState = BalanceState.SLOW;
                break;

            case SLOW:
                if (autoSlowMode.isEnabled() && isPlayerMoving()) {
                    if (currentBalance > 0) {
                        currentBalanceState = BalanceState.TIMER;
                    } else {
                        currentBalanceState = BalanceState.NONE;
                    }
                    break;
                }
                long timeDelta = currentTime - lastUpdateTime;
                currentBalance += (long) (timeDelta * (1.0 - balanceSlowSpeed.getValue()));
                if (currentBalance >= maxBalanceMs.getValue()) {
                    currentBalance = (long) maxBalanceMs.getValue();
                    currentBalanceState = BalanceState.TIMER;
                    lastUpdateTime = currentTime;
                } else {
                    lastUpdateTime = currentTime;
                    setTimerSpeed((float) balanceSlowSpeed.getValue());
                }
                break;

            case TIMER:
                timeDelta = currentTime - lastUpdateTime;
                currentBalance -= (long) (timeDelta * balanceBoostSpeed.getValue() * balanceCostMultiplier.getValue());
                if (currentBalance <= 0) {
                    resetBalance();
                    if (autoDisableWhenEmpty.isEnabled()) {
                        System.out.println("[GameSpeed] Balance depleted, disabling module");
                        this.toggle();
                    }
                    break;
                }
                lastUpdateTime = currentTime;
                setTimerSpeed((float) balanceBoostSpeed.getValue());
                break;
        }
    }

    private void setTimerSpeed(float speed) throws Exception {
        Timer timer = (Timer) timerField.get(mc);
        // Allow 0 for complete freeze (keybind watcher ensures you can disable)
        float safeSpeed = Math.max(0.0f, speed);
        timerSpeedField.setFloat(timer, safeSpeed);
    }

    private void resetBalance() {
        try {
            setTimerSpeed(1.0f);
        } catch (Exception e) {
            // Silent
        }
        currentBalance = 0;
        currentBalanceState = BalanceState.NONE;
        lastUpdateTime = -1;
    }

    private boolean canUseTimer() {
        return getPlayer() != null && getWorld() != null;
    }

    private boolean isPlayerMoving() {
        if (getPlayer() == null) return false;
        return getPlayer().moveForward != 0 || getPlayer().moveStrafing != 0;
    }

    private void updateSettingVisibility() {
        boolean isNormalMode = gameSpeedMode.getSelected().equals("Normal");

        normalSpeed.setVisible(isNormalMode);
        balanceBoostSpeed.setVisible(!isNormalMode);
        balanceSlowSpeed.setVisible(!isNormalMode);
        maxBalanceMs.setVisible(!isNormalMode);
        balanceCostMultiplier.setVisible(!isNormalMode);
        autoSlowMode.setVisible(!isNormalMode);
        autoDisableWhenEmpty.setVisible(!isNormalMode);
    }

    private enum BalanceState {
        NONE,
        SLOW,
        TIMER
    }

    /**
     * Starts a background thread that monitors the keybind in REAL TIME
     * This allows the module to be disabled even at very low timer speeds (0.01x)
     */
    private void startKeybindWatcher() {
        if (keybindWatcher != null && keybindWatcher.isAlive()) {
            return; // Already running
        }

        watcherRunning = true;
        keybindWatcher = new Thread(() -> {
            // Check all possible keybind fields
            int key1 = this.getKeyCode();
            int key2 = this.getToggleBind();
            int key3 = this.getHoldBind();

            // Use whichever is set (can be negative for mouse buttons)
            int moduleKeyCode = key1 != 0 ? key1 : (key2 != 0 ? key2 : key3);

            // Check if it's a mouse button (negative values in some systems)
            boolean isMouse = moduleKeyCode < 0;
            int mouseButton = isMouse ? -(moduleKeyCode + 100) : -1; // -100 = button 0, -101 = button 1, etc.

            System.out.println("[GameSpeed] Watcher thread started. Keys: legacy=" + key1 +
                ", toggle=" + key2 + ", hold=" + key3 + " -> using: " + moduleKeyCode +
                (isMouse ? " (mouse button " + mouseButton + ")" : " (keyboard)"));

            if (moduleKeyCode == 0) {
                System.out.println("[GameSpeed] No keybind set, watcher exiting");
                return;
            }

            // Wait for initial key/button release (from the enable keypress)
            try {
                Thread.sleep(300);
                int waitCount = 0;
                while (watcherRunning && isKeyOrMouseDown(moduleKeyCode, isMouse, mouseButton)) {
                    Thread.sleep(10);
                    waitCount++;
                    if (waitCount > 200) { // 2 second timeout
                        System.out.println("[GameSpeed] Initial key release timeout");
                        break;
                    }
                }
                System.out.println("[GameSpeed] Initial key/button released, now watching...");
            } catch (InterruptedException e) {
                return;
            }

            boolean wasPressed = false;

            while (watcherRunning && enabled) {
                try {
                    boolean isPressed = isKeyOrMouseDown(moduleKeyCode, isMouse, mouseButton);

                    // Detect NEW key press (was not pressed, now pressed)
                    if (!wasPressed && isPressed) {
                        System.out.println("[GameSpeed] Keybind detected (real-time) - disabling...");

                        // Reset timer to 1.0 IMMEDIATELY - this is the critical part
                        try {
                            if (timerField != null && timerSpeedField != null) {
                                Timer timer = (Timer) timerField.get(mc);
                                timerSpeedField.setFloat(timer, 1.0f);
                                System.out.println("[GameSpeed] Timer reset to 1.0");
                            }
                        } catch (Exception e) {
                            System.out.println("[GameSpeed] Error resetting timer: " + e.getMessage());
                        }

                        watcherRunning = false;
                        enabled = false;

                        // Set cooldown to prevent instant re-enable
                        lastWatcherDisable = System.currentTimeMillis();

                        // Call onDisable logic
                        resetBalance();
                        guiCurrentlyOpen = false;
                        savedTimerSpeed = 1.0f;

                        System.out.println("[GameSpeed] Disabled via keybind watcher (cooldown set)");
                        break;
                    }

                    wasPressed = isPressed;
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.out.println("[GameSpeed] Watcher error: " + e.getMessage());
                }
            }
            System.out.println("[GameSpeed] Watcher thread exiting");
        }, "GameSpeed-KeybindWatcher");

        keybindWatcher.setDaemon(true);
        keybindWatcher.start();
    }

    /**
     * Stops the keybind watcher thread
     */
    private void stopKeybindWatcher() {
        watcherRunning = false;
        if (keybindWatcher != null) {
            keybindWatcher.interrupt();
            keybindWatcher = null;
        }
    }

    /**
     * Check if a key or mouse button is down
     * Handles both keyboard keys (positive) and mouse buttons (negative)
     */
    private boolean isKeyOrMouseDown(int keyCode, boolean isMouse, int mouseButton) {
        try {
            if (isMouse) {
                return org.lwjgl.input.Mouse.isButtonDown(mouseButton);
            } else {
                return org.lwjgl.input.Keyboard.isKeyDown(keyCode);
            }
        } catch (Exception e) {
            return false;
        }
    }
}
