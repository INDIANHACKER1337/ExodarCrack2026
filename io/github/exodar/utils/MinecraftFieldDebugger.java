/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.utils;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;

/**
 * Debug utility to inspect Minecraft class fields at runtime
 */
public class MinecraftFieldDebugger {
    private static boolean debugged = false;

    public static void debugMinecraftFields() {
        if (debugged) return;
        debugged = true;

        try {
            Minecraft mc = Minecraft.getMinecraft();

            System.out.println("[MinecraftFieldDebugger] ========================================");
            System.out.println("[MinecraftFieldDebugger] MINECRAFT CLASS FIELDS:");
            System.out.println("[MinecraftFieldDebugger] ========================================");

            // Get all int fields (for rightClickDelayTimer)
            System.out.println("\n[MinecraftFieldDebugger] === INT FIELDS ===");
            for (Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType() == int.class) {
                    f.setAccessible(true);
                    try {
                        int value = f.getInt(mc);
                        System.out.println("[MinecraftFieldDebugger] int " + f.getName() + " = " + value);
                    } catch (Exception e) {
                        System.out.println("[MinecraftFieldDebugger] int " + f.getName() + " = ERROR");
                    }
                }
            }

            // Get all Object fields that might be PlayerController
            System.out.println("\n[MinecraftFieldDebugger] === POTENTIAL PLAYERCONTROLLER FIELDS ===");
            for (Field f : mc.getClass().getDeclaredFields()) {
                String typeName = f.getType().getName();
                if (typeName.contains("PlayerController") || typeName.contains("playercontrol") ||
                    typeName.toLowerCase().contains("controller")) {
                    f.setAccessible(true);
                    try {
                        Object value = f.get(mc);
                        System.out.println("[MinecraftFieldDebugger] " + typeName + " " + f.getName() + " = " + (value != null ? "NOT NULL" : "null"));

                        // If we found PlayerController, debug its fields too
                        if (value != null) {
                            debugPlayerControllerFields(value);
                        }
                    } catch (Exception e) {
                        System.out.println("[MinecraftFieldDebugger] " + typeName + " " + f.getName() + " = ERROR");
                    }
                }
            }

            // Also check for single-letter or short field names that might be obfuscated PlayerController
            System.out.println("\n[MinecraftFieldDebugger] === SHORT NAME OBJECT FIELDS (obfuscated?) ===");
            for (Field f : mc.getClass().getDeclaredFields()) {
                if (f.getName().length() <= 2 && !f.getType().isPrimitive()) {
                    f.setAccessible(true);
                    try {
                        Object value = f.get(mc);
                        System.out.println("[MinecraftFieldDebugger] " + f.getType().getSimpleName() + " " + f.getName() + " = " + (value != null ? "NOT NULL" : "null"));
                    } catch (Exception e) {
                        System.out.println("[MinecraftFieldDebugger] " + f.getType().getSimpleName() + " " + f.getName() + " = ERROR");
                    }
                }
            }

            System.out.println("\n[MinecraftFieldDebugger] ========================================");

        } catch (Exception e) {
            System.out.println("[MinecraftFieldDebugger] ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void debugPlayerControllerFields(Object playerController) {
        System.out.println("[MinecraftFieldDebugger]   -> PlayerController fields:");

        try {
            // Look for blockHitDelay (int)
            for (Field f : playerController.getClass().getDeclaredFields()) {
                if (f.getType() == int.class) {
                    f.setAccessible(true);
                    try {
                        int value = f.getInt(playerController);
                        System.out.println("[MinecraftFieldDebugger]      int " + f.getName() + " = " + value);
                    } catch (Exception e) {
                        System.out.println("[MinecraftFieldDebugger]      int " + f.getName() + " = ERROR");
                    }
                }
            }

            // Look for curBlockDamageMP (float)
            for (Field f : playerController.getClass().getDeclaredFields()) {
                if (f.getType() == float.class) {
                    f.setAccessible(true);
                    try {
                        float value = f.getFloat(playerController);
                        System.out.println("[MinecraftFieldDebugger]      float " + f.getName() + " = " + value);
                    } catch (Exception e) {
                        System.out.println("[MinecraftFieldDebugger]      float " + f.getName() + " = ERROR");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[MinecraftFieldDebugger]   ERROR debugging PlayerController: " + e.getMessage());
        }
    }

    public static void reset() {
        debugged = false;
    }
}
