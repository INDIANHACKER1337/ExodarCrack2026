/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.movement;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;

/**
 * Fly - Vanilla fly with motion control
 * Directly controls player motion vectors for flying
 */
public class Fly extends Module {
    private SliderSetting verticalSpeed;
    private SliderSetting horizontalSpeed;
    private SliderSetting boostMultiplier;
    private KeybindSetting boostKey;

    private static Field thePlayerField;

    static {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            for (Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("EntityPlayer")) {
                    f.setAccessible(true);
                    thePlayerField = f;
                    break;
                }
            }
        } catch (Exception e) {
            // Silent
        }
    }

    public Fly() {
        super("Fly", ModuleCategory.MOVEMENT);
        this.registerSetting(new DescriptionSetting("Vanilla fly with motion control"));
        this.registerSetting(verticalSpeed = new SliderSetting("Vertical Speed", 1.0, 0.1, 10.0, 0.1));
        this.registerSetting(horizontalSpeed = new SliderSetting("Horizontal Speed", 1.0, 0.1, 10.0, 0.1));
        this.registerSetting(boostMultiplier = new SliderSetting("Boost Multiplier", 2.0, 1.0, 5.0, 0.1));
        this.registerSetting(boostKey = new KeybindSetting("Boost Key"));
    }

    @Override
    public void onEnable() {
        // Silent
    }

    @Override
    public void onDisable() {
        try {
            EntityPlayerSP player = null;
            if (thePlayerField != null) {
                player = (EntityPlayerSP) thePlayerField.get(mc);
            }
            if (player != null) {
                // Reset motion on disable
                player.motionY = 0;
            }
        } catch (Exception e) {
            // Silent
        }
    }

    @Override
    public String getDisplaySuffix() {
        return " §7H:" + String.format("%.1f", horizontalSpeed.getValue()) +
               " V:" + String.format("%.1f", verticalSpeed.getValue());
    }

    @Override
    public void onUpdate() {
        if (!enabled || mc == null) return;

        try {
            EntityPlayerSP player = null;
            if (thePlayerField != null) {
                player = (EntityPlayerSP) thePlayerField.get(mc);
            }

            if (player == null || getWorld() == null) {
                setEnabled(false);
                return;
            }

            // Check if boost key is pressed
            boolean boosting = boostKey.isKeyPressed();
            double boostMult = boosting ? boostMultiplier.getValue() : 1.0;

            // Calculate speeds
            double hSpeed = horizontalSpeed.getValue() * boostMult;
            double vSpeed = verticalSpeed.getValue() * boostMult;

            // Prevent falling
            player.motionY = 0;
            player.fallDistance = 0;

            // Vertical movement (jump/sneak)
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
                player.motionY = vSpeed;
            }
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
                player.motionY = -vSpeed;
            }

            // Horizontal movement based on look direction
            double forward = 0;
            double strafe = 0;

            if (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
                forward += 1;
            }
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode())) {
                forward -= 1;
            }
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode())) {
                strafe += 1;
            }
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode())) {
                strafe -= 1;
            }

            // Only apply horizontal motion if moving
            if (forward != 0 || strafe != 0) {
                // Convert yaw to radians
                float yaw = player.rotationYaw;
                double yawRad = Math.toRadians(yaw);

                // Calculate motion based on yaw and input
                double motionX = 0;
                double motionZ = 0;

                if (forward != 0) {
                    motionX -= Math.sin(yawRad) * forward;
                    motionZ += Math.cos(yawRad) * forward;
                }
                if (strafe != 0) {
                    motionX += Math.cos(yawRad) * strafe;
                    motionZ += Math.sin(yawRad) * strafe;
                }

                // Normalize and apply speed
                double length = Math.sqrt(motionX * motionX + motionZ * motionZ);
                if (length > 0) {
                    motionX = (motionX / length) * hSpeed;
                    motionZ = (motionZ / length) * hSpeed;
                }

                player.motionX = motionX;
                player.motionZ = motionZ;
            } else {
                // Stop horizontal movement when no keys pressed
                player.motionX = 0;
                player.motionZ = 0;
            }

        } catch (Exception e) {
            // Silent
        }
    }
}
