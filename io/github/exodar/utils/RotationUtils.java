/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import java.lang.reflect.Field;

public class RotationUtils {

    private static Field posXField = null;
    private static Field posYField = null;
    private static Field posZField = null;
    private static Field thePlayerField = null;

    private static void initFields() {
        if (posXField == null) {
            try {
                // Buscar campos de posición en Entity
                for (Field f : Entity.class.getDeclaredFields()) {
                    if (f.getType() == double.class) {
                        f.setAccessible(true);
                        String name = f.getName();
                        if (name.length() == 1 || name.contains("pos") || name.contains("X")) {
                            if (posXField == null) posXField = f;
                            else if (posYField == null) posYField = f;
                            else if (posZField == null) {
                                posZField = f;
                                break;
                            }
                        }
                    }
                }

                // Buscar thePlayer en Minecraft
                for (Field f : Minecraft.class.getDeclaredFields()) {
                    if (f.getType().getName().contains("EntityPlayer")) {
                        f.setAccessible(true);
                        thePlayerField = f;
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static double getEntityX(Entity entity) {
        try {
            initFields();
            if (posXField != null) return (double) posXField.get(entity);
        } catch (Exception e) {}
        return 0;
    }

    private static double getEntityY(Entity entity) {
        try {
            initFields();
            if (posYField != null) return (double) posYField.get(entity);
        } catch (Exception e) {}
        return 0;
    }

    private static double getEntityZ(Entity entity) {
        try {
            initFields();
            if (posZField != null) return (double) posZField.get(entity);
        } catch (Exception e) {}
        return 0;
    }

    private static Entity getPlayer(Minecraft mc) {
        try {
            initFields();
            if (thePlayerField != null) return (Entity) thePlayerField.get(mc);
        } catch (Exception e) {}
        return null;
    }

    public static float getYawChange(Entity entity) {
        if (entity == null) return 0;

        Minecraft mc = Minecraft.getMinecraft();
        Entity player = getPlayer(mc);
        if (player == null) return 0;

        double deltaX = getEntityX(entity) - getEntityX(player);
        double deltaZ = getEntityZ(entity) - getEntityZ(player);
        double yawToEntity;

        if (deltaZ < 0.0 && deltaX < 0.0) {
            yawToEntity = 90.0 + Math.toDegrees(Math.atan(deltaZ / deltaX));
        } else if (deltaZ < 0.0 && deltaX > 0.0) {
            yawToEntity = -90.0 + Math.toDegrees(Math.atan(deltaZ / deltaX));
        } else {
            yawToEntity = Math.toDegrees(-Math.atan(deltaX / deltaZ));
        }

        try {
            Field yawField = null;
            for (Field f : Entity.class.getDeclaredFields()) {
                if (f.getType() == float.class) {
                    String name = f.getName();
                    if (name.contains("Yaw") || name.contains("yaw") || name.contains("rot")) {
                        f.setAccessible(true);
                        yawField = f;
                        break;
                    }
                }
            }
            if (yawField != null) {
                float currentYaw = (float) yawField.get(player);
                return MathHelper.wrapAngleTo180_float(-(currentYaw - (float) yawToEntity));
            }
        } catch (Exception e) {}

        return 0;
    }

    public static float getPitchChange(Entity entity, double offsetY) {
        if (entity == null) return 0;

        Minecraft mc = Minecraft.getMinecraft();
        Entity player = getPlayer(mc);
        if (player == null) return 0;

        double deltaX = getEntityX(entity) - getEntityX(player);
        double deltaZ = getEntityZ(entity) - getEntityZ(player);
        double deltaY = getEntityY(entity) - offsetY;

        try {
            // Get player eye height
            if (player instanceof EntityLivingBase) {
                EntityLivingBase livingPlayer = (EntityLivingBase) player;
                deltaY -= (getEntityY(player) + livingPlayer.getEyeHeight());
            }
        } catch (Exception e) {}

        double distanceXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double pitchToEntity = -Math.toDegrees(Math.atan(deltaY / distanceXZ));

        try {
            Field pitchField = null;
            for (Field f : Entity.class.getDeclaredFields()) {
                if (f.getType() == float.class) {
                    String name = f.getName();
                    if (name.contains("Pitch") || name.contains("pitch")) {
                        f.setAccessible(true);
                        pitchField = f;
                        break;
                    }
                }
            }
            if (pitchField != null) {
                float currentPitch = (float) pitchField.get(player);
                return -MathHelper.wrapAngleTo180_float(currentPitch - (float) pitchToEntity);
            }
        } catch (Exception e) {}

        return 0;
    }
}
