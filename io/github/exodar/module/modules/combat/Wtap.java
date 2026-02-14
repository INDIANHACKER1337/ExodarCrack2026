/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import java.lang.reflect.Field;

/**
 * Wtap - Releases W key briefly after attacking to reset sprint
 * This causes more knockback on the next hit
 *
 * Uses KeyBinding manipulation like STap for proper functionality
 */
public class WTap extends Module {

    private SliderSetting delay;
    private SliderSetting duration;

    // State
    private boolean isWtapping = false;
    private long attackTime = 0;
    private long releaseStartTime = 0;
    private Field pressedField = null;

    public WTap() {
        super("Wtap", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Release W after hit for more KB"));
        this.registerSetting(delay = new SliderSetting("Delay ms", 0.0, 0.0, 100.0, 5.0));
        this.registerSetting(duration = new SliderSetting("Duration ms", 50.0, 20.0, 150.0, 5.0));

        // Find pressed field in KeyBinding
        try {
            for (Field f : KeyBinding.class.getDeclaredFields()) {
                if (f.getType() == boolean.class) {
                    f.setAccessible(true);
                    pressedField = f;
                    break;
                }
            }
        } catch (Exception e) {
            // Silent
        }
    }

    /**
     * Called by Main.java when player attacks an entity
     */
    public void onAttack() {
        if (!enabled) return;
        if (mc == null || mc.thePlayer == null) return;

        // Must be sprinting and moving forward
        if (!mc.thePlayer.isSprinting()) return;
        if (!mc.gameSettings.keyBindForward.isKeyDown()) return;

        // Start W-tap sequence
        attackTime = System.currentTimeMillis();
        isWtapping = true;
        releaseStartTime = 0;
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;
        if (mc == null || mc.thePlayer == null) return;

        if (!isWtapping) return;

        long now = System.currentTimeMillis();
        long timeSinceAttack = now - attackTime;

        // Phase 1: Wait for delay
        if (releaseStartTime == 0) {
            if (timeSinceAttack >= delay.getValue()) {
                // Start releasing W
                releaseStartTime = now;
                setKeyPressed(mc.gameSettings.keyBindForward, false);
            }
            return;
        }

        // Phase 2: Hold release for duration
        long timeSinceRelease = now - releaseStartTime;
        if (timeSinceRelease >= duration.getValue()) {
            // Done - restore W key if player is still holding it
            if (org.lwjgl.input.Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
                setKeyPressed(mc.gameSettings.keyBindForward, true);
            }
            isWtapping = false;
            releaseStartTime = 0;
        } else {
            // Keep W released
            setKeyPressed(mc.gameSettings.keyBindForward, false);
        }
    }

    private void setKeyPressed(KeyBinding key, boolean pressed) {
        if (pressedField == null) return;
        try {
            pressedField.set(key, pressed);
        } catch (Exception e) {
            // Silent
        }
    }

    @Override
    public void onDisable() {
        isWtapping = false;
        releaseStartTime = 0;
    }

    @Override
    public String getDisplaySuffix() {
        return "";
    }
}
