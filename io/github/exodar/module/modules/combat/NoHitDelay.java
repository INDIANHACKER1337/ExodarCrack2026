/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;

/**
 * No Hit Delay (Delay Remover) - Removes delay between attacks
 * Based on Raven B4
 */
public class NoHitDelay extends Module {
    private static Field leftClickCounterField;
    private int errorCount = 0;
    private static final int MAX_ERRORS = 5;

    static {
        try {
            // Find leftClickCounter field in Minecraft class
            System.out.println("[NoHitDelay] Searching for leftClickCounter field...");

            for (Field f : Minecraft.class.getDeclaredFields()) {
                if (f.getType() == int.class) {
                    f.setAccessible(true);
                    String name = f.getName();

                    // Try to match by name
                    if (name.equals("leftClickCounter") || name.equals("field_71429_W")) {
                        leftClickCounterField = f;
                        System.out.println("[NoHitDelay] Found leftClickCounter by name: " + name);
                        break;
                    }
                }
            }

            // If not found by name, try to find by value range
            if (leftClickCounterField == null) {
                System.out.println("[NoHitDelay] Field not found by name, trying by value...");
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null) {
                    for (Field f : Minecraft.class.getDeclaredFields()) {
                        if (f.getType() == int.class) {
                            try {
                                f.setAccessible(true);
                                int val = f.getInt(mc);
                                // leftClickCounter is usually between 0-10
                                if (val >= 0 && val <= 10) {
                                    String name = f.getName();
                                    // Skip obvious non-matches
                                    if (!name.contains("display") && !name.contains("width") && !name.contains("height")) {
                                        leftClickCounterField = f;
                                        System.out.println("[NoHitDelay] Found potential leftClickCounter: " + name + " (value: " + val + ")");
                                        break;
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }

            if (leftClickCounterField == null) {
                System.out.println("[NoHitDelay] WARNING: Could not find leftClickCounter field!");
            }

        } catch (Exception e) {
            System.out.println("[NoHitDelay] Error initializing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public NoHitDelay() {
        super("No Hit Delay", ModuleCategory.COMBAT);
        this.registerSetting(new DescriptionSetting("Removes delay between attacks"));
    }

    @Override
    public void onEnable() {
        errorCount = 0;
        if (leftClickCounterField == null) {
            System.out.println("[NoHitDelay] ERROR: Module cannot function - leftClickCounter field not found!");
            this.toggle();
            return;
        }
        System.out.println("[NoHitDelay] Enabled");
    }

    @Override
    public void onDisable() {
        System.out.println("[NoHitDelay] Disabled");
    }

    @Override
    public void onUpdate() {
        if (!enabled || mc == null || leftClickCounterField == null) return;

        try {
            // Set leftClickCounter to 0 to remove attack delay
            leftClickCounterField.setInt(mc, 0);
        } catch (Exception e) {
            errorCount++;
            if (errorCount <= MAX_ERRORS) {
                System.out.println("[NoHitDelay] Error in onUpdate: " + e.getMessage());
                e.printStackTrace();
            }
            if (errorCount == MAX_ERRORS) {
                System.out.println("[NoHitDelay] Too many errors, disabling module");
                this.toggle();
            }
        }
    }
}
