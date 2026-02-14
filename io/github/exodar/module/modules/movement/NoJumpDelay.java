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
import net.minecraft.client.entity.EntityPlayerSP;

import java.lang.reflect.Field;

/**
 * No Jump Delay - Removes jump delay for continuous jumping
 * Based on Raven B4
 */
public class NoJumpDelay extends Module {
    private static Field thePlayerField;
    private static Field jumpTicksField;

    static {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            // Find thePlayer field
            for (Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("EntityPlayer")) {
                    f.setAccessible(true);
                    thePlayerField = f;
                    System.out.println("[NoJumpDelay] Found thePlayer: " + f.getName());
                    break;
                }
            }

            // Find jumpTicks field in EntityLivingBase
            Class<?> entityClass = Class.forName("net.minecraft.entity.EntityLivingBase");
            for (Field f : entityClass.getDeclaredFields()) {
                if (f.getType() == int.class) {
                    String name = f.getName();
                    // jumpTicks or field_70773_bE
                    if (name.equals("jumpTicks") || name.equals("field_70773_bE")) {
                        f.setAccessible(true);
                        jumpTicksField = f;
                        System.out.println("[NoJumpDelay] Found jumpTicks: " + name);
                        break;
                    }
                }
            }

            if (jumpTicksField == null) {
                System.out.println("[NoJumpDelay] WARNING: Could not find jumpTicks field by name, trying by value...");
            }
        } catch (Exception e) {
            System.out.println("[NoJumpDelay] Error in static init: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public NoJumpDelay() {
        super("No Jump Delay", ModuleCategory.MOVEMENT);
        this.registerSetting(new DescriptionSetting("Spam jump when holding space"));
    }

    @Override
    public void onEnable() {
        System.out.println("[NoJumpDelay] Enabled");
    }

    @Override
    public void onDisable() {
        System.out.println("[NoJumpDelay] Disabled");
    }

    @Override
    public void onUpdate() {
        if (!enabled || mc == null) return;

        try {
            // Get player
            EntityPlayerSP player = null;
            if (thePlayerField != null) {
                player = (EntityPlayerSP) thePlayerField.get(mc);
            }
            if (player == null) return;

            // Reset jump ticks to 0 to remove delay
            if (jumpTicksField != null) {
                jumpTicksField.setInt(player, 0);
            }
        } catch (Exception e) {
            // Silently fail
        }
    }
}
