/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.player;

import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.Module;
import io.github.exodar.setting.*;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;

/**
 * FastBreak module - Lazy initialization
 * Elimina delay entre bloques + aumenta velocidad de minado
 */
public class FastBreak extends Module {
    private SliderSetting speedMultiplier;
    private TickSetting instantShears;

    // Fields - initialized lazily
    private Field playerControllerField;
    private Field blockHitDelayField;
    private Field curBlockDamageMPField;
    private Field thePlayerField;
    private boolean fieldsInitialized = false;

    private int updateCounter = 0;
    private int accelerationCounter = 0;

    public FastBreak() {
        super("FastBreak", ModuleCategory.PLAYER);

        this.registerSetting(new DescriptionSetting("Elimina delay + aumenta velocidad"));
        this.registerSetting(speedMultiplier = new SliderSetting("Speed Multiplier", 1.30, 1.00, 10.00, 0.01));
        this.registerSetting(instantShears = new TickSetting("Instant Shears", false));
    }

    private void initFields() {
        if (fieldsInitialized) return;

        try {
            // Find thePlayer field
            for (Field f : mc.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                String typeName = f.getType().getName();
                if (typeName.contains("EntityPlayerSP") && thePlayerField == null) {
                    thePlayerField = f;
                    System.out.println("[FastBreak] Found thePlayer: " + f.getName());
                }
            }

            // Find playerController field
            for (Field f : mc.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                String typeName = f.getType().getName();

                if (typeName.contains("PlayerController") || typeName.contains("PlayerControllerMP")) {
                    try {
                        Object value = f.get(mc);
                        if (value != null) {
                            playerControllerField = f;
                            System.out.println("[FastBreak] Found playerController: " + f.getName());

                            // Find fields in PlayerController
                            Class<?> pcClass = value.getClass();

                            // Si es OptiFine wrapper, usar superclass
                            if (pcClass.getName().contains("PlayerControllerOF")) {
                                pcClass = pcClass.getSuperclass();
                            }

                            // Find blockHitDelay (int field, usually 0-5)
                            for (Field pf : pcClass.getDeclaredFields()) {
                                pf.setAccessible(true);
                                if (pf.getType() == int.class) {
                                    String name = pf.getName().toLowerCase();
                                    if (name.contains("blockhit") || name.contains("hitdelay") ||
                                        name.equals("field_78781_i") || name.equals("blockhitdelay")) {
                                        blockHitDelayField = pf;
                                        System.out.println("[FastBreak] Found blockHitDelay: " + pf.getName());
                                        break;
                                    }
                                }
                            }

                            // If not found by name, try by value pattern
                            if (blockHitDelayField == null) {
                                for (Field pf : pcClass.getDeclaredFields()) {
                                    pf.setAccessible(true);
                                    if (pf.getType() == int.class) {
                                        try {
                                            int intValue = pf.getInt(value);
                                            if (intValue >= 0 && intValue <= 5) {
                                                blockHitDelayField = pf;
                                                System.out.println("[FastBreak] Found blockHitDelay by value: " + pf.getName());
                                                break;
                                            }
                                        } catch (Exception ignored) {}
                                    }
                                }
                            }

                            // Find curBlockDamageMP (float field, 0.0-1.0)
                            for (Field pf : pcClass.getDeclaredFields()) {
                                pf.setAccessible(true);
                                if (pf.getType() == float.class) {
                                    String name = pf.getName().toLowerCase();
                                    if (name.contains("blockdamage") || name.contains("curdamage") ||
                                        name.contains("damage") || name.equals("field_78770_f") ||
                                        name.equals("curblockdamagemp")) {
                                        curBlockDamageMPField = pf;
                                        System.out.println("[FastBreak] Found curBlockDamageMP: " + pf.getName());
                                        break;
                                    }
                                }
                            }

                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }

            fieldsInitialized = true;

            System.out.println("[FastBreak] Fields initialized:");
            System.out.println("[FastBreak]   playerController: " + (playerControllerField != null));
            System.out.println("[FastBreak]   blockHitDelay: " + (blockHitDelayField != null));
            System.out.println("[FastBreak]   curBlockDamageMP: " + (curBlockDamageMPField != null));

        } catch (Exception e) {
            System.out.println("[FastBreak] Error initializing: " + e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        updateCounter = 0;
        accelerationCounter = 0;
        fieldsInitialized = false; // Re-init fields on enable
        System.out.println("[FastBreak] ENABLED - Speed: " + speedMultiplier.getValue() + "x");
    }

    @Override
    public void onDisable() {
        System.out.println("[FastBreak] Disabled - Accelerations: " + accelerationCounter);
    }

    @Override
    public String getDisplaySuffix() {
        if (speedMultiplier != null) {
            return " §7x" + String.format("%.2f", speedMultiplier.getValue());
        }
        return "";
    }

    @Override
    public void onUpdate() {
        if (!enabled || mc == null) return;

        // Lazy init fields
        if (!fieldsInitialized) {
            initFields();
        }

        if (playerControllerField == null) return;

        updateCounter++;

        try {
            Object playerController = playerControllerField.get(mc);
            if (playerController == null) return;

            // PART 1: Remove block hit delay
            if (blockHitDelayField != null) {
                try {
                    int currentDelay = blockHitDelayField.getInt(playerController);
                    if (currentDelay > 0) {
                        blockHitDelayField.setInt(playerController, 0);
                    }
                } catch (Exception ignored) {}
            }

            // PART 2: Speed up mining
            if (curBlockDamageMPField != null) {
                try {
                    float currentDamage = curBlockDamageMPField.getFloat(playerController);

                    // Only act when mining (damage > 0 and < 1)
                    if (currentDamage > 0.0f && currentDamage < 1.0f) {
                        // Check if holding shears and instant shears is enabled
                        float multiplier = (float) speedMultiplier.getValue();

                        if (instantShears.isEnabled() && isHoldingShears()) {
                            multiplier = 10.0f; // Always instant for shears
                        }

                        float threshold = 1.0f / multiplier;

                        // If we've mined enough, complete the block
                        if (currentDamage >= threshold) {
                            curBlockDamageMPField.setFloat(playerController, 1.0f);
                            accelerationCounter++;
                        }
                    }
                } catch (Exception ignored) {}
            }

        } catch (Exception ignored) {}
    }

    /**
     * Check if player is holding shears
     */
    private boolean isHoldingShears() {
        try {
            if (thePlayerField == null) return false;

            Object player = thePlayerField.get(mc);
            if (player == null) return false;

            // Get held item via reflection
            java.lang.reflect.Method getHeldItem = player.getClass().getMethod("getHeldItem");
            Object itemStack = getHeldItem.invoke(player);
            if (itemStack == null) return false;

            // Check if item name contains "shear"
            String itemString = itemStack.toString().toLowerCase();
            return itemString.contains("shear");
        } catch (Exception e) {
            return false;
        }
    }
}
