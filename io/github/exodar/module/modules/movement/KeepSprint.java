/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.movement;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.*;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * KeepSprint - Prevents sprint from being cancelled when attacking
 *
 * In vanilla Minecraft, attacking cancels your sprint and reduces your motion.
 * This module re-enables sprint and maintains motion after attacks.
 */
public class KeepSprint extends Module {

    // Settings
    private SliderSetting chance;
    private SliderSetting retention;

    // Internal
    private Random random = new Random();
    private boolean wasSprinting = false;
    private int lastAttackTick = 0;
    private double lastMotionX = 0;
    private double lastMotionZ = 0;

    // Reflection
    private Method setSprintingMethod = null;
    private Method isSprintingMethod = null;
    private Field motionXField = null;
    private Field motionZField = null;
    private Field ticksExistedField = null;
    private boolean methodsInit = false;

    public KeepSprint() {
        super("KeepSprint", ModuleCategory.MOVEMENT);

        this.registerSetting(new DescriptionSetting("Keep sprint when attacking"));
        this.registerSetting(chance = new SliderSetting("Chance", 100.0, 0.0, 100.0, 1.0));
        this.registerSetting(retention = new SliderSetting("Retention", 0.6, 0.0, 1.0, 0.05));
    }

    @Override
    public void onEnable() {
        methodsInit = false;
        wasSprinting = false;
        lastAttackTick = 0;
    }

    @Override
    public void onUpdate() {
        if (!isInGame()) return;
        if (mc.currentScreen != null) return;

        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        initMethods(player);

        boolean currentlySprinting = isSprinting(player);
        int currentTick = getTicksExisted(player);

        // Save motion before potential attack
        double currentMotionX = getMotionX(player);
        double currentMotionZ = getMotionZ(player);

        // Detect attack (sprint cancelled this tick while we were sprinting)
        boolean justAttacked = detectAttack(player, currentlySprinting);

        if (justAttacked && wasSprinting) {
            // Chance check
            if (random.nextDouble() * 100.0 <= chance.getValue()) {
                // Re-enable sprint
                setSprinting(player, true);

                // Apply motion retention
                double retentionValue = retention.getValue();
                if (retentionValue > 0.6) {
                    // Restore some of the lost motion
                    // Default attack slowdown is 0.6, so we scale between 0.6 and 1.0
                    double motionMultiplier = retentionValue / 0.6;

                    // Only boost if we have significant motion
                    if (Math.abs(lastMotionX) > 0.01 || Math.abs(lastMotionZ) > 0.01) {
                        setMotionX(player, currentMotionX * motionMultiplier);
                        setMotionZ(player, currentMotionZ * motionMultiplier);
                    }
                }
            }
        }

        // Save state for next tick
        wasSprinting = currentlySprinting;
        lastMotionX = currentMotionX;
        lastMotionZ = currentMotionZ;
    }

    /**
     * Detect if player just attacked this tick
     * Signs: sprint was cancelled, swing animation, or target has hurtTime
     */
    private boolean detectAttack(EntityPlayerSP player, boolean currentlySprinting) {
        // Method 1: Sprint was cancelled while we were sprinting
        if (wasSprinting && !currentlySprinting) {
            // Verify it's likely an attack (not hitting a wall, etc.)
            if (!player.isCollidedHorizontally) {
                // Check if there's an entity we could have hit
                try {
                    if (mc.objectMouseOver != null) {
                        Object typeOfHit = mc.objectMouseOver.typeOfHit;
                        if (typeOfHit != null && typeOfHit.toString().equals("ENTITY")) {
                            return true;
                        }
                    }
                } catch (Exception e) {}

                // Also check swing animation
                if (player.isSwingInProgress) {
                    return true;
                }
            }
        }

        return false;
    }

    // ========== Reflection Methods ==========

    private void initMethods(EntityPlayerSP player) {
        if (methodsInit) return;
        methodsInit = true;

        try {
            // setSprinting method
            for (Method m : player.getClass().getMethods()) {
                if (m.getName().equals("setSprinting") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                    m.setAccessible(true);
                    setSprintingMethod = m;
                    break;
                }
            }

            // isSprinting method
            for (Method m : player.getClass().getMethods()) {
                if (m.getName().equals("isSprinting") && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    isSprintingMethod = m;
                    break;
                }
            }

            // Motion fields
            Class<?> clazz = player.getClass();
            while (clazz != null) {
                for (Field f : clazz.getDeclaredFields()) {
                    f.setAccessible(true);
                    String name = f.getName();
                    if (name.equals("motionX") && f.getType() == double.class) {
                        motionXField = f;
                    } else if (name.equals("motionZ") && f.getType() == double.class) {
                        motionZField = f;
                    } else if (name.equals("ticksExisted") && f.getType() == int.class) {
                        ticksExistedField = f;
                    }
                }
                clazz = clazz.getSuperclass();
            }

        } catch (Exception e) {}
    }

    private boolean isSprinting(EntityPlayerSP player) {
        try {
            if (isSprintingMethod != null) {
                return (boolean) isSprintingMethod.invoke(player);
            }
            return player.isSprinting();
        } catch (Exception e) {
            try {
                return player.isSprinting();
            } catch (Exception e2) {
                return false;
            }
        }
    }

    private void setSprinting(EntityPlayerSP player, boolean sprinting) {
        try {
            if (setSprintingMethod != null) {
                setSprintingMethod.invoke(player, sprinting);
            } else {
                player.setSprinting(sprinting);
            }
        } catch (Exception e) {
            try {
                player.setSprinting(sprinting);
            } catch (Exception e2) {}
        }
    }

    private double getMotionX(EntityPlayerSP player) {
        try {
            if (motionXField != null) {
                return motionXField.getDouble(player);
            }
            return player.motionX;
        } catch (Exception e) {
            try {
                return player.motionX;
            } catch (Exception e2) {
                return 0;
            }
        }
    }

    private double getMotionZ(EntityPlayerSP player) {
        try {
            if (motionZField != null) {
                return motionZField.getDouble(player);
            }
            return player.motionZ;
        } catch (Exception e) {
            try {
                return player.motionZ;
            } catch (Exception e2) {
                return 0;
            }
        }
    }

    private void setMotionX(EntityPlayerSP player, double value) {
        try {
            if (motionXField != null) {
                motionXField.setDouble(player, value);
            } else {
                player.motionX = value;
            }
        } catch (Exception e) {
            try {
                player.motionX = value;
            } catch (Exception e2) {}
        }
    }

    private void setMotionZ(EntityPlayerSP player, double value) {
        try {
            if (motionZField != null) {
                motionZField.setDouble(player, value);
            } else {
                player.motionZ = value;
            }
        } catch (Exception e) {
            try {
                player.motionZ = value;
            } catch (Exception e2) {}
        }
    }

    private int getTicksExisted(EntityPlayerSP player) {
        try {
            if (ticksExistedField != null) {
                return ticksExistedField.getInt(player);
            }
            return player.ticksExisted;
        } catch (Exception e) {
            try {
                return player.ticksExisted;
            } catch (Exception e2) {
                return 0;
            }
        }
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + (int) chance.getValue() + "% " + String.format("%.0f%%", retention.getValue() * 100);
    }
}
