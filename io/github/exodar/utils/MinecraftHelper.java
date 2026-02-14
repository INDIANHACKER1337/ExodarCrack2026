/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import java.lang.reflect.Field;

/**
 * Helper class to access obfuscated Minecraft fields
 */
public class MinecraftHelper {
    private static Field thePlayerField = null;
    private static Field theWorldField = null;
    private static Field objectMouseOverField = null;

    static {
        try {
            Minecraft mc = Minecraft.getMinecraft();

            for (Field f : mc.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                String type = f.getType().getName();

                if (type.contains("EntityPlayer") && thePlayerField == null) {
                    thePlayerField = f;
                } else if (type.contains("World") && theWorldField == null) {
                    theWorldField = f;
                } else if (type.contains("MovingObjectPosition") && objectMouseOverField == null) {
                    objectMouseOverField = f;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static EntityPlayer getPlayer() {
        try {
            if (thePlayerField == null) return null;
            return (EntityPlayer) thePlayerField.get(Minecraft.getMinecraft());
        } catch (Exception e) {
            return null;
        }
    }

    public static Object getWorld() {
        try {
            if (theWorldField == null) return null;
            return theWorldField.get(Minecraft.getMinecraft());
        } catch (Exception e) {
            return null;
        }
    }

    public static Object getObjectMouseOver() {
        try {
            if (objectMouseOverField == null) return null;
            return objectMouseOverField.get(Minecraft.getMinecraft());
        } catch (Exception e) {
            return null;
        }
    }
}
