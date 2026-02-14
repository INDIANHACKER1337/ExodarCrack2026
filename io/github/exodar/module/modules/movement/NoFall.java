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
import net.minecraft.network.play.client.C03PacketPlayer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * NoFall - Prevents fall damage
 * Based on Raven B4
 */
public class NoFall extends Module {
    private static Field thePlayerField;

    static {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            // Find thePlayer field
            for (Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("EntityPlayer")) {
                    f.setAccessible(true);
                    thePlayerField = f;
                    System.out.println("[NoFall] Found thePlayer: " + f.getName());
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("[NoFall] Error in static init: " + e.getMessage());
        }
    }

    public NoFall() {
        super("NoFall", ModuleCategory.MOVEMENT);
        this.registerSetting(new DescriptionSetting("Prevents fall damage"));
    }

    @Override
    public void onEnable() {
        System.out.println("[NoFall] Enabled");
    }

    @Override
    public void onDisable() {
        System.out.println("[NoFall] Disabled");
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

            // NoFall logic: Send ground packet when falling
            // This tricks the server into thinking we're on ground
            if (player.fallDistance > 3.0f) {
                // Send ground packet using reflection
                try {
                    Method sendQueueMethod = player.getClass().getMethod("sendQueue");
                    Object networkManager = sendQueueMethod.invoke(player);

                    if (networkManager != null) {
                        Method addToSendQueueMethod = networkManager.getClass().getMethod("addToSendQueue", Object.class);
                        C03PacketPlayer packet = new C03PacketPlayer(true); // onGround = true
                        addToSendQueueMethod.invoke(networkManager, packet);
                    }
                } catch (Exception e) {
                    // If reflection fails, just reset fall distance
                    player.fallDistance = 0.0f;
                }
            }
        } catch (Exception e) {
            // Silently fail
        }
    }
}
