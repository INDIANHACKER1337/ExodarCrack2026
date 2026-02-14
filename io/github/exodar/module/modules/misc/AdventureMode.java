/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.WorldSettings;

import java.lang.reflect.Field;

/**
 * AdventureMode - Spoofs adventure mode (gamemode 2) client-side
 * Prevents breaking blocks and block targeting while enabled
 */
public class AdventureMode extends Module {

    // Cached reflection
    private Field currentGameTypeField = null;
    private Field objectMouseOverField = null;
    private WorldSettings.GameType originalGameType = null;
    private boolean initDone = false;

    private TickSetting disableBlockHighlight;

    public AdventureMode() {
        super("AdventureMode", ModuleCategory.MISC);
        this.registerSetting(new DescriptionSetting("Spoofs adventure mode"));
        this.registerSetting(disableBlockHighlight = new TickSetting("Hide Block Target", true));
    }

    @Override
    public void onEnable() {
        initDone = false;
        initReflection();
        setAdventureMode(true);
    }

    @Override
    public void onDisable() {
        setAdventureMode(false);
    }

    private void initReflection() {
        if (initDone) return;
        initDone = true;

        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.playerController == null) {
                System.out.println("[AdventureMode] PlayerController is null");
                return;
            }

            // Find currentGameType field in PlayerControllerMP
            Class<?> pcClass = mc.playerController.getClass();

            // Search for GameType field
            for (Field f : pcClass.getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getType() == WorldSettings.GameType.class) {
                    currentGameTypeField = f;
                    System.out.println("[AdventureMode] Found GameType field: " + f.getName());
                    break;
                }
            }

            // Search superclasses if not found
            if (currentGameTypeField == null) {
                Class<?> superClass = pcClass.getSuperclass();
                while (superClass != null && currentGameTypeField == null) {
                    for (Field f : superClass.getDeclaredFields()) {
                        f.setAccessible(true);
                        if (f.getType() == WorldSettings.GameType.class) {
                            currentGameTypeField = f;
                            System.out.println("[AdventureMode] Found GameType field in superclass: " + f.getName());
                            break;
                        }
                    }
                    superClass = superClass.getSuperclass();
                }
            }

            // Find objectMouseOver field in Minecraft class
            for (Field f : mc.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getType() == MovingObjectPosition.class) {
                    objectMouseOverField = f;
                    System.out.println("[AdventureMode] Found objectMouseOver field: " + f.getName());
                    break;
                }
            }

            if (currentGameTypeField == null) {
                System.out.println("[AdventureMode] ERROR: Could not find gameType field");
            } else {
                System.out.println("[AdventureMode] Init complete");
            }

        } catch (Exception e) {
            System.out.println("[AdventureMode] Init error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setAdventureMode(boolean enable) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.playerController == null) return;
            if (currentGameTypeField == null) return;

            if (enable) {
                // Save original game type
                Object current = currentGameTypeField.get(mc.playerController);
                if (current instanceof WorldSettings.GameType) {
                    originalGameType = (WorldSettings.GameType) current;
                }
                System.out.println("[AdventureMode] Saved original: " + originalGameType);

                // Set to adventure mode
                currentGameTypeField.set(mc.playerController, WorldSettings.GameType.ADVENTURE);
                System.out.println("[AdventureMode] Set to ADVENTURE mode");

            } else {
                // Restore original game type
                if (originalGameType != null) {
                    currentGameTypeField.set(mc.playerController, originalGameType);
                    System.out.println("[AdventureMode] Restored to: " + originalGameType);
                }
            }

        } catch (Exception e) {
            System.out.println("[AdventureMode] Error setting mode: " + e.getMessage());
        }
    }

    @Override
    public void onUpdate() {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.playerController == null) return;

        try {
            // Re-apply adventure mode each tick
            if (currentGameTypeField != null) {
                Object current = currentGameTypeField.get(mc.playerController);
                if (current != WorldSettings.GameType.ADVENTURE) {
                    currentGameTypeField.set(mc.playerController, WorldSettings.GameType.ADVENTURE);
                }
            }

            // Clear block targeting if enabled
            if (disableBlockHighlight.isEnabled() && objectMouseOverField != null) {
                MovingObjectPosition mop = (MovingObjectPosition) objectMouseOverField.get(mc);
                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    // Set to MISS to prevent block highlight
                    objectMouseOverField.set(mc, new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, mop.hitVec, null, null));
                }
            }

        } catch (Exception ignored) {}
    }

    /**
     * Check if we should prevent block breaking - called by other modules
     */
    public boolean shouldPreventBlockBreak() {
        return isEnabled();
    }

    @Override
    public String getDisplaySuffix() {
        if (currentGameTypeField != null) {
            return "Active";
        }
        return "Error";
    }
}
