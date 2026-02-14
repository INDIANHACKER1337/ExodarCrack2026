/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.movement;

import io.github.exodar.Main;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.TimeUnit;

/**
 * SaveMoveKeys - Re-presses movement keys when closing GUI
 *
 * Prevents the bug where movement keys don't register after closing
 * a GUI/inventory. Waits a configurable delay, then re-applies the
 * current keyboard state to the movement keybindings.
 */
public class SaveMoveKeys extends Module {
    private final SliderSetting guiCloseDelayMs;
    private boolean wasGuiOpenLastTick = false;

    public SaveMoveKeys() {
        super("SaveMoveKeys", ModuleCategory.MOVEMENT);
        this.registerSetting(new DescriptionSetting("Re-press movement keys when close gui"));
        this.registerSetting(guiCloseDelayMs = new SliderSetting("Delay", 0, 0, 1000, 10));
    }

    @Override
    public void onUpdate() {
        if (mc.currentScreen != null) {
            wasGuiOpenLastTick = true;
        } else {
            if (wasGuiOpenLastTick) {
                Main.getExecutor().schedule(() -> {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()));
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()));
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()));
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode()));
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()));
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()));
                    KeyBinding.onTick(mc.gameSettings.keyBindForward.getKeyCode());
                    KeyBinding.onTick(mc.gameSettings.keyBindBack.getKeyCode());
                    KeyBinding.onTick(mc.gameSettings.keyBindLeft.getKeyCode());
                    KeyBinding.onTick(mc.gameSettings.keyBindRight.getKeyCode());
                    KeyBinding.onTick(mc.gameSettings.keyBindSprint.getKeyCode());
                    KeyBinding.onTick(mc.gameSettings.keyBindSneak.getKeyCode());
                    KeyBinding.onTick(mc.gameSettings.keyBindJump.getKeyCode());
                }, (long) guiCloseDelayMs.getValue(), TimeUnit.MILLISECONDS);
            }

            wasGuiOpenLastTick = false;
        }
    }
}
