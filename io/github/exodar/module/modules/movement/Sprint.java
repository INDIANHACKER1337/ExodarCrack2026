/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.movement;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;

/**
 * Sprint - Auto sprint usando KeyBinding
 * Basado en cisrc/sprint.java
 */
public class Sprint extends Module {

    private TickSetting disableOnInvisibility;

    private static Field thePlayerField;
    private static Field currentScreenField;
    private static Field isCollidedHorizontallyField;
    private static Field gameSettingsField;
    private static Field keyBindSprintField;

    static {
        try {
            Minecraft mc = Minecraft.getMinecraft();

            // Find thePlayer field
            for (Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("EntityPlayer")) {
                    f.setAccessible(true);
                    thePlayerField = f;
                    System.out.println("[Sprint] Found thePlayer: " + f.getName());
                    break;
                }
            }

            // Find currentScreen field
            for (Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("GuiScreen")) {
                    f.setAccessible(true);
                    currentScreenField = f;
                    System.out.println("[Sprint] Found currentScreen: " + f.getName());
                    break;
                }
            }

            // Find gameSettings field
            for (Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("GameSettings")) {
                    f.setAccessible(true);
                    gameSettingsField = f;
                    System.out.println("[Sprint] Found gameSettings: " + f.getName());
                    break;
                }
            }

            // Find isCollidedHorizontally in Entity
            Class<?> entityClass = Class.forName("net.minecraft.entity.Entity");
            for (Field f : entityClass.getDeclaredFields()) {
                if (f.getName().equals("isCollidedHorizontally") || f.getName().equals("field_70123_F")) {
                    f.setAccessible(true);
                    isCollidedHorizontallyField = f;
                    System.out.println("[Sprint] Found isCollidedHorizontally: " + f.getName());
                    break;
                }
            }

            // Find keyBindSprint in GameSettings
            if (gameSettingsField != null) {
                Object gameSettings = gameSettingsField.get(mc);
                if (gameSettings != null) {
                    for (Field f : gameSettings.getClass().getDeclaredFields()) {
                        if (f.getType().getName().contains("KeyBinding") && f.getName().contains("Sprint")) {
                            f.setAccessible(true);
                            keyBindSprintField = f;
                            System.out.println("[Sprint] Found keyBindSprint: " + f.getName());
                            break;
                        }
                    }
                    // Si no encontramos por nombre, buscar por el tipo
                    if (keyBindSprintField == null) {
                        for (Field f : gameSettings.getClass().getDeclaredFields()) {
                            if (f.getName().equals("keyBindSprint") || f.getName().equals("field_151444_V")) {
                                f.setAccessible(true);
                                keyBindSprintField = f;
                                System.out.println("[Sprint] Found keyBindSprint by name: " + f.getName());
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[Sprint] Error in static init: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Sprint() {
        super("Sprint", ModuleCategory.MOVEMENT);
        this.registerSetting(new DescriptionSetting("Auto sprint"));
        this.registerSetting(disableOnInvisibility = new TickSetting("Disable on Invisibility", false));
    }

    @Override
    public void onEnable() {
        System.out.println("[Sprint] Enabled");
    }

    @Override
    public void onDisable() {
        System.out.println("[Sprint] Disabled");
        // Liberar la tecla de sprint
        try {
            if (gameSettingsField != null && keyBindSprintField != null) {
                Object gameSettings = gameSettingsField.get(mc);
                if (gameSettings != null) {
                    KeyBinding keyBindSprint = (KeyBinding) keyBindSprintField.get(gameSettings);
                    if (keyBindSprint != null) {
                        KeyBinding.setKeyBindState(keyBindSprint.getKeyCode(), false);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[Sprint] Error in onDisable: " + e.getMessage());
        }
    }

    @Override
    public void onUpdate() {
        if (!enabled || mc == null) return;

        try {
            // Get player
            Object player = null;
            if (thePlayerField != null) {
                player = thePlayerField.get(mc);
            }
            if (player == null) return;

            // Check if GUI is open
            Object currentScreen = null;
            if (currentScreenField != null) {
                currentScreen = currentScreenField.get(mc);
            }

            // Get keyBindSprint primero (lo necesitamos para todo)
            KeyBinding keyBindSprint = null;
            int sprintKey = 0;
            if (gameSettingsField != null && keyBindSprintField != null) {
                Object gameSettings = gameSettingsField.get(mc);
                if (gameSettings != null) {
                    keyBindSprint = (KeyBinding) keyBindSprintField.get(gameSettings);
                    if (keyBindSprint != null) {
                        sprintKey = keyBindSprint.getKeyCode();
                    }
                }
            }

            // Si hay GUI abierta, no hacer nada
            if (currentScreen != null) {
                return;
            }

            // Check invisibilidad - liberar tecla si está invisible
            if (disableOnInvisibility.isEnabled()) {
                boolean isInvisible = (Boolean) player.getClass().getMethod("isInvisible").invoke(player);
                if (isInvisible) {
                    if (keyBindSprint != null) {
                        KeyBinding.setKeyBindState(sprintKey, false);
                    }
                    return;
                }
            }

            // Get isCollidedHorizontally
            boolean isCollidedHorizontally = false;
            if (isCollidedHorizontallyField != null) {
                isCollidedHorizontally = isCollidedHorizontallyField.getBoolean(player);
            }

            // Forzar sprint siempre que no esté chocando con pared
            if (keyBindSprint != null) {
                KeyBinding.setKeyBindState(sprintKey, !isCollidedHorizontally);
            }
        } catch (Exception e) {
            System.out.println("[Sprint] Error in onUpdate: " + e.getMessage());
        }
    }
}
