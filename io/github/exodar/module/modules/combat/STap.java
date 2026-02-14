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
 * STap - Automatically moves backward after attacking to dodge hits
 * Restores key states properly after hold time
 */
public class STap extends Module {

    private SliderSetting delay;
    private SliderSetting holdTime;

    // State
    private boolean isStapping = false;
    private long attackTime = 0;
    private long stappingStartTime = 0;
    private Field pressedField = null;

    public STap() {
        super("STap", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Auto S-tap after attack"));
        this.registerSetting(delay = new SliderSetting("Delay ms", 0.0, 0.0, 100.0, 5.0));
        this.registerSetting(holdTime = new SliderSetting("Hold Time ms", 50.0, 20.0, 150.0, 5.0));

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
     * Called when player attacks an entity
     */
    public void onAttack() {
        if (!enabled) return;
        if (mc == null || mc.thePlayer == null) return;

        // Must be on ground and moving forward
        if (!mc.thePlayer.onGround) return;
        if (!mc.gameSettings.keyBindForward.isKeyDown()) return;

        // Don't S-tap if hurt
        if (mc.thePlayer.hurtTime > 0) return;

        // Start S-tap sequence
        attackTime = System.currentTimeMillis();
        isStapping = true;
        stappingStartTime = 0;
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;
        if (mc == null || mc.thePlayer == null) return;

        if (!isStapping) return;

        long now = System.currentTimeMillis();
        long timeSinceAttack = now - attackTime;

        // Phase 1: Wait for delay
        if (stappingStartTime == 0) {
            if (timeSinceAttack >= delay.getValue()) {
                // Start S-tapping
                stappingStartTime = now;
                setKeyPressed(mc.gameSettings.keyBindForward, false);
                setKeyPressed(mc.gameSettings.keyBindBack, true);
            }
            return;
        }

        // Phase 2: Hold S for duration
        long timeSinceStart = now - stappingStartTime;
        if (timeSinceStart >= holdTime.getValue()) {
            // Done - restore keys
            // Restore W if player is still holding it
            if (org.lwjgl.input.Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
                setKeyPressed(mc.gameSettings.keyBindForward, true);
            }
            // Restore S based on actual key state
            boolean sPhysicallyDown = org.lwjgl.input.Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
            setKeyPressed(mc.gameSettings.keyBindBack, sPhysicallyDown);

            isStapping = false;
            stappingStartTime = 0;
        } else {
            // Keep S-tapping
            setKeyPressed(mc.gameSettings.keyBindForward, false);
            setKeyPressed(mc.gameSettings.keyBindBack, true);
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
        // Restore keys on disable
        if (mc != null && mc.thePlayer != null && isStapping) {
            if (org.lwjgl.input.Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
                setKeyPressed(mc.gameSettings.keyBindForward, true);
            }
            boolean sPhysicallyDown = org.lwjgl.input.Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
            setKeyPressed(mc.gameSettings.keyBindBack, sPhysicallyDown);
        }
        isStapping = false;
        stappingStartTime = 0;
    }

    @Override
    public String getDisplaySuffix() {
        return "";
    }
}
