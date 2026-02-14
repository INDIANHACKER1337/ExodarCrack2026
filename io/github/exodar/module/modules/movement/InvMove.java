/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.movement;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;

/**
 * Inv Move - Move while inventory is open
 * Based on Raven B4
 */
public class InvMove extends Module {
    private static Field currentScreenField;
    private static KeyBinding[] movementKeys;

    static {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            // Find currentScreen field
            for (Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("GuiScreen")) {
                    f.setAccessible(true);
                    currentScreenField = f;
                    System.out.println("[InvMove] Found currentScreen: " + f.getName());
                    break;
                }
            }

            // Get movement keybindings
            movementKeys = new KeyBinding[] {
                mc.gameSettings.keyBindForward,
                mc.gameSettings.keyBindBack,
                mc.gameSettings.keyBindLeft,
                mc.gameSettings.keyBindRight,
                mc.gameSettings.keyBindJump,
                mc.gameSettings.keyBindSprint
            };
        } catch (Exception e) {
            System.out.println("[InvMove] Error in static init: " + e.getMessage());
        }
    }

    public InvMove() {
        super("Inv Move", ModuleCategory.MOVEMENT);
        this.registerSetting(new DescriptionSetting("Move with inventory open"));
    }

    @Override
    public void onEnable() {
        System.out.println("[InvMove] Enabled");
    }

    @Override
    public void onDisable() {
        System.out.println("[InvMove] Disabled");
        // Reset key states when disabled
        if (movementKeys != null) {
            for (KeyBinding key : movementKeys) {
                KeyBinding.setKeyBindState(key.getKeyCode(), false);
            }
        }
    }

    @Override
    public void onUpdate() {
        if (!enabled || mc == null) return;

        try {
            // Check if any screen is open (except chat)
            Object currentScreen = null;
            if (currentScreenField != null) {
                currentScreen = currentScreenField.get(mc);
            }

            if (currentScreen != null && !(currentScreen instanceof GuiChat)) {
                // Update movement keys based on keyboard state
                for (KeyBinding key : movementKeys) {
                    KeyBinding.setKeyBindState(key.getKeyCode(), Keyboard.isKeyDown(key.getKeyCode()));
                }
            }
        } catch (Exception e) {
            System.out.println("[InvMove] Error: " + e.getMessage());
        }
    }
}
