/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;

import java.lang.reflect.Field;

/**
 * Utility class to map obfuscated fields in Minecraft 1.8.9
 * Based on actual field names found via javap
 */
public class FieldMapper {

    // Entity position fields (obfuscated)
    private static Field posXField;      // s
    private static Field posYField;      // t
    private static Field posZField;      // u
    private static Field lastPosXField;  // P
    private static Field lastPosYField;  // Q
    private static Field lastPosZField;  // R

    // Entity rotation fields (obfuscated)
    private static Field rotYawField;        // y
    private static Field rotPitchField;      // z
    private static Field prevRotYawField;    // A
    private static Field prevRotPitchField;  // B

    // RenderManager fields (obfuscated)
    private static Field viewerPosXField;  // h
    private static Field viewerPosYField;  // i
    private static Field viewerPosZField;  // j
    private static Field playerViewYField; // e
    private static Field playerViewXField; // f

    // Entity height
    private static Field heightField;

    private static boolean initialized = false;

    /**
     * Initialize all field mappings
     */
    public static void init() {
        if (initialized) return;

        try {
            System.out.println("[FieldMapper] Initializing field mappings...");

            // Map Entity fields by order and type
            Class<?> entityClass = Entity.class;

            // Get double fields for positions
            int doubleCount = 0;
            for (Field f : entityClass.getDeclaredFields()) {
                if (f.getType() == double.class) {
                    f.setAccessible(true);
                    String name = f.getName();

                    // Based on javap output:
                    // s, t, u = posX, posY, posZ
                    // P, Q, R = lastTickPosX, lastTickPosY, lastTickPosZ
                    if (name.equals("s")) posXField = f;
                    else if (name.equals("t")) posYField = f;
                    else if (name.equals("u")) posZField = f;
                    else if (name.equals("P")) lastPosXField = f;
                    else if (name.equals("Q")) lastPosYField = f;
                    else if (name.equals("R")) lastPosZField = f;
                }
            }

            // Get float fields for rotations
            for (Field f : entityClass.getDeclaredFields()) {
                if (f.getType() == float.class) {
                    f.setAccessible(true);
                    String name = f.getName();

                    // Based on javap output:
                    // y = rotationYaw, z = rotationPitch
                    // A = prevRotationYaw, B = prevRotationPitch
                    if (name.equals("y")) rotYawField = f;
                    else if (name.equals("z")) rotPitchField = f;
                    else if (name.equals("A")) prevRotYawField = f;
                    else if (name.equals("B")) prevRotPitchField = f;
                    else if (name.equals("J")) heightField = f; // height field
                }
            }

            // Map RenderManager fields - use reflection to find renderManager field
            RenderManager rm = null;
            Minecraft mc = Minecraft.getMinecraft();
            for (Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("RenderManager")) {
                    f.setAccessible(true);
                    rm = (RenderManager) f.get(mc);
                    break;
                }
            }
            if (rm == null) return; // Can't find RenderManager
            Class<?> rmClass = rm.getClass();

            for (Field f : rmClass.getDeclaredFields()) {
                f.setAccessible(true);
                String name = f.getName();

                if (f.getType() == double.class) {
                    // h, i, j = viewerPosX, viewerPosY, viewerPosZ
                    if (name.equals("h")) viewerPosXField = f;
                    else if (name.equals("i")) viewerPosYField = f;
                    else if (name.equals("j")) viewerPosZField = f;
                } else if (f.getType() == float.class) {
                    // e, f = playerViewY, playerViewX
                    if (name.equals("e")) playerViewYField = f;
                    else if (name.equals("f")) playerViewXField = f;
                }
            }

            // Verify all fields were found
            boolean success = posXField != null && posYField != null && posZField != null &&
                            lastPosXField != null && lastPosYField != null && lastPosZField != null &&
                            rotYawField != null && rotPitchField != null &&
                            prevRotYawField != null && prevRotPitchField != null &&
                            viewerPosXField != null && viewerPosYField != null && viewerPosZField != null &&
                            playerViewYField != null && playerViewXField != null &&
                            heightField != null;

            if (success) {
                System.out.println("[FieldMapper] ✓ All fields mapped successfully!");
                initialized = true;
            } else {
                System.out.println("[FieldMapper] ✗ WARNING: Some fields could not be mapped!");
                System.out.println("  posX: " + (posXField != null));
                System.out.println("  posY: " + (posYField != null));
                System.out.println("  posZ: " + (posZField != null));
                System.out.println("  lastPosX: " + (lastPosXField != null));
                System.out.println("  lastPosY: " + (lastPosYField != null));
                System.out.println("  lastPosZ: " + (lastPosZField != null));
                System.out.println("  rotYaw: " + (rotYawField != null));
                System.out.println("  rotPitch: " + (rotPitchField != null));
                System.out.println("  prevRotYaw: " + (prevRotYawField != null));
                System.out.println("  prevRotPitch: " + (prevRotPitchField != null));
                System.out.println("  viewerPosX: " + (viewerPosXField != null));
                System.out.println("  viewerPosY: " + (viewerPosYField != null));
                System.out.println("  viewerPosZ: " + (viewerPosZField != null));
                System.out.println("  playerViewY: " + (playerViewYField != null));
                System.out.println("  playerViewX: " + (playerViewXField != null));
                System.out.println("  height: " + (heightField != null));
            }

        } catch (Exception e) {
            System.out.println("[FieldMapper] ERROR during initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Position getters
    public static double getPosX(Entity entity) throws Exception {
        if (posXField == null) init();
        return posXField.getDouble(entity);
    }

    public static double getPosY(Entity entity) throws Exception {
        if (posYField == null) init();
        return posYField.getDouble(entity);
    }

    public static double getPosZ(Entity entity) throws Exception {
        if (posZField == null) init();
        return posZField.getDouble(entity);
    }

    public static double getLastPosX(Entity entity) throws Exception {
        if (lastPosXField == null) init();
        return lastPosXField.getDouble(entity);
    }

    public static double getLastPosY(Entity entity) throws Exception {
        if (lastPosYField == null) init();
        return lastPosYField.getDouble(entity);
    }

    public static double getLastPosZ(Entity entity) throws Exception {
        if (lastPosZField == null) init();
        return lastPosZField.getDouble(entity);
    }

    // Rotation getters/setters
    public static float getRotationYaw(Entity entity) throws Exception {
        if (rotYawField == null) init();
        return rotYawField.getFloat(entity);
    }

    public static void setRotationYaw(Entity entity, float yaw) throws Exception {
        if (rotYawField == null) init();
        rotYawField.setFloat(entity, yaw);
    }

    public static float getRotationPitch(Entity entity) throws Exception {
        if (rotPitchField == null) init();
        return rotPitchField.getFloat(entity);
    }

    public static void setRotationPitch(Entity entity, float pitch) throws Exception {
        if (rotPitchField == null) init();
        rotPitchField.setFloat(entity, pitch);
    }

    public static float getPrevRotationYaw(Entity entity) throws Exception {
        if (prevRotYawField == null) init();
        return prevRotYawField.getFloat(entity);
    }

    public static void setPrevRotationYaw(Entity entity, float yaw) throws Exception {
        if (prevRotYawField == null) init();
        prevRotYawField.setFloat(entity, yaw);
    }

    public static float getPrevRotationPitch(Entity entity) throws Exception {
        if (prevRotPitchField == null) init();
        return prevRotPitchField.getFloat(entity);
    }

    public static void setPrevRotationPitch(Entity entity, float pitch) throws Exception {
        if (prevRotPitchField == null) init();
        prevRotPitchField.setFloat(entity, pitch);
    }

    public static float getHeight(Entity entity) throws Exception {
        if (heightField == null) init();
        return heightField.getFloat(entity);
    }

    // RenderManager getters
    public static double getViewerPosX(RenderManager rm) throws Exception {
        if (viewerPosXField == null) init();
        return viewerPosXField.getDouble(rm);
    }

    public static double getViewerPosY(RenderManager rm) throws Exception {
        if (viewerPosYField == null) init();
        return viewerPosYField.getDouble(rm);
    }

    public static double getViewerPosZ(RenderManager rm) throws Exception {
        if (viewerPosZField == null) init();
        return viewerPosZField.getDouble(rm);
    }

    public static float getPlayerViewY(RenderManager rm) throws Exception {
        if (playerViewYField == null) init();
        return playerViewYField.getFloat(rm);
    }

    public static float getPlayerViewX(RenderManager rm) throws Exception {
        if (playerViewXField == null) init();
        return playerViewXField.getFloat(rm);
    }

    /**
     * Get interpolated position of entity
     */
    public static double[] getInterpolatedPos(Entity entity, float partialTicks) throws Exception {
        double x = getLastPosX(entity) + (getPosX(entity) - getLastPosX(entity)) * partialTicks;
        double y = getLastPosY(entity) + (getPosY(entity) - getLastPosY(entity)) * partialTicks;
        double z = getLastPosZ(entity) + (getPosZ(entity) - getLastPosZ(entity)) * partialTicks;
        return new double[]{x, y, z};
    }
}
