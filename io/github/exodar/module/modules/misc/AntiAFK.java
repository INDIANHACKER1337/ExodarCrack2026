/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import net.minecraft.client.settings.KeyBinding;

/**
 * AntiAFK - Prevents AFK kick by simulating movement
 * Pattern: A D S W A D JUMP A D...
 */
public class AntiAFK extends Module {

    private SliderSetting idleTime;
    private int lastInputTicks = 0;
    private int patternIndex = 0;
    private int actionTicks = 0;

    // Track pressed keys for cleanup
    private KeyBinding currentKey = null;

    // Pattern: Left, Right, Back, Forward, Left, Right, Jump, Left, Right
    private static final int PATTERN_LEFT = 0;
    private static final int PATTERN_RIGHT = 1;
    private static final int PATTERN_BACK = 2;
    private static final int PATTERN_FORWARD = 3;
    private static final int PATTERN_JUMP = 4;
    private static final int[] PATTERN = {
        PATTERN_LEFT, PATTERN_RIGHT, PATTERN_BACK, PATTERN_FORWARD,
        PATTERN_LEFT, PATTERN_RIGHT, PATTERN_JUMP,
        PATTERN_LEFT, PATTERN_RIGHT
    };

    public AntiAFK() {
        super("AntiAFK", ModuleCategory.MISC);

        this.registerSetting(new DescriptionSetting("Prevents AFK kick"));
        this.registerSetting(idleTime = new SliderSetting("Idle Time (sec)", 10.0, 5.0, 60.0, 1.0));
    }

    @Override
    public void onEnable() {
        lastInputTicks = 0;
        patternIndex = 0;
        actionTicks = 0;
        currentKey = null;
    }

    @Override
    public void onDisable() {
        releaseKey();
    }

    private void releaseKey() {
        if (currentKey != null) {
            KeyBinding.setKeyBindState(currentKey.getKeyCode(), false);
            currentKey = null;
        }
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;
        if (mc == null || mc.thePlayer == null || mc.gameSettings == null) return;

        // Check if player is providing real input
        boolean hasInput = mc.gameSettings.keyBindJump.isKeyDown() ||
                          mc.gameSettings.keyBindForward.isKeyDown() ||
                          mc.gameSettings.keyBindBack.isKeyDown() ||
                          mc.gameSettings.keyBindLeft.isKeyDown() ||
                          mc.gameSettings.keyBindRight.isKeyDown() ||
                          mc.gameSettings.keyBindAttack.isKeyDown();

        // If player provides input and we're not the cause, reset
        if (hasInput && currentKey == null) {
            lastInputTicks = 0;
            return;
        }

        lastInputTicks++;

        // Convert idle time to ticks (20 ticks per second)
        int idleTicks = (int) (idleTime.getValue() * 20);
        if (lastInputTicks < idleTicks) {
            return;
        }

        // Action timing
        actionTicks++;

        // Release key after 3 ticks
        if (actionTicks == 3) {
            releaseKey();
        }

        // Press new key every 10 ticks
        if (actionTicks >= 10) {
            actionTicks = 0;

            // Get key for current pattern step
            KeyBinding key = getKeyForPattern(PATTERN[patternIndex]);
            if (key != null) {
                KeyBinding.setKeyBindState(key.getKeyCode(), true);
                currentKey = key;
            }

            // Move to next pattern step
            patternIndex = (patternIndex + 1) % PATTERN.length;
        }
    }

    private KeyBinding getKeyForPattern(int pattern) {
        switch (pattern) {
            case PATTERN_LEFT:
                return mc.gameSettings.keyBindLeft;
            case PATTERN_RIGHT:
                return mc.gameSettings.keyBindRight;
            case PATTERN_BACK:
                return mc.gameSettings.keyBindBack;
            case PATTERN_FORWARD:
                return mc.gameSettings.keyBindForward;
            case PATTERN_JUMP:
                return mc.gameSettings.keyBindJump;
            default:
                return null;
        }
    }
}
