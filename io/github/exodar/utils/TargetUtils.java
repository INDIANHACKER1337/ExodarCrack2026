/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import java.lang.reflect.Field;
import java.util.List;

public class TargetUtils {
    private static Field theWorldField = null;
    private static Field thePlayerField = null;
    private static Field posXField = null;
    private static Field posYField = null;
    private static Field posZField = null;
    private static Field rotationYawField = null;
    private static Field rotationPitchField = null;
    private static int callCount = 0;
    private static boolean debugShown = false;

    private static void initFields() {
        if (theWorldField == null || thePlayerField == null) {
            try {
                Minecraft mc = Minecraft.getMinecraft();

                // Initialize FieldMapper first
                FieldMapper.init();

                // Buscar theWorld
                for (Field f : mc.getClass().getDeclaredFields()) {
                    if (f.getType().getName().contains("World")) {
                        f.setAccessible(true);
                        theWorldField = f;
                        break;
                    }
                }

                // Buscar thePlayer
                for (Field f : mc.getClass().getDeclaredFields()) {
                    if (f.getType().getName().contains("EntityPlayer")) {
                        f.setAccessible(true);
                        thePlayerField = f;
                        break;
                    }
                }

                System.out.println("[TargetUtils] Fields initialized, using FieldMapper for entity data");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Entity getClosestEntity(double fov, double range, boolean invisibles, boolean players) {
        try {
            callCount++;
            Minecraft mc = Minecraft.getMinecraft();

            if (mc == null || mc.thePlayer == null || mc.theWorld == null) {
                return null;
            }

            EntityPlayer player = mc.thePlayer;
            Object world = mc.theWorld;

            // DEBUG: Mostrar TODOS los campos en World (solo una vez)
            if (!debugShown && callCount > 20) {
                debugShown = true;
                System.out.println("[TargetUtils] ===== ALL fields in World class =====");
                System.out.println("[TargetUtils] World class: " + world.getClass().getName());

                // Mostrar campos de la clase actual
                for (Field f : world.getClass().getDeclaredFields()) {
                    System.out.println("[TargetUtils]   - " + f.getName() + " : " + f.getType().getName());
                }

                // Mostrar campos de la superclase
                if (world.getClass().getSuperclass() != null) {
                    System.out.println("[TargetUtils] === Fields from superclass: " + world.getClass().getSuperclass().getName() + " ===");
                    for (Field f : world.getClass().getSuperclass().getDeclaredFields()) {
                        if (f.getType().getName().contains("List") || f.getName().contains("entit") || f.getName().contains("player")) {
                            System.out.println("[TargetUtils]   - " + f.getName() + " : " + f.getType().getName());
                        }
                    }
                }
                System.out.println("[TargetUtils] =============================================");
            }

            // Buscar campo playerEntities (igual que Raven)
            List<Entity> entities = null;

            // Método 1: Buscar específicamente "playerEntities" (usado por Raven)
            try {
                Class<?> currentClass = world.getClass();
                while (currentClass != null && entities == null) {
                    for (Field f : currentClass.getDeclaredFields()) {
                        if (f.getName().equals("playerEntities") || f.getName().contains("player")) {
                            try {
                                f.setAccessible(true);
                                Object value = f.get(world);
                                if (value instanceof List) {
                                    List<?> list = (List<?>) value;
                                    if (!list.isEmpty()) {
                                        entities = (List<Entity>) list;
                                        System.out.println("[TargetUtils] Found playerEntities in field: " + f.getName() + " with " + list.size() + " players");
                                        break;
                                    } else if (f.getName().equals("playerEntities")) {
                                        // Campo correcto pero vacío (aún no hay jugadores)
                                        entities = (List<Entity>) list;
                                        System.out.println("[TargetUtils] Found playerEntities field (empty)");
                                        break;
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    currentClass = currentClass.getSuperclass();
                }
            } catch (Exception e) {
                System.out.println("[TargetUtils] Error searching for playerEntities: " + e.getMessage());
            }

            // Método 2: Buscar loadedEntityList si playerEntities no funciona
            if (entities == null) {
                try {
                    Class<?> currentClass = world.getClass();
                    while (currentClass != null && entities == null) {
                        for (Field f : currentClass.getDeclaredFields()) {
                            if (f.getName().contains("loadedEntity") || f.getName().contains("entityList")) {
                                try {
                                    f.setAccessible(true);
                                    Object value = f.get(world);
                                    if (value instanceof List) {
                                        List<?> list = (List<?>) value;
                                        if (!list.isEmpty()) {
                                            entities = (List<Entity>) list;
                                            System.out.println("[TargetUtils] Found entity list in field: " + f.getName() + " with " + list.size() + " entities");
                                            break;
                                        }
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                        currentClass = currentClass.getSuperclass();
                    }
                } catch (Exception e) {
                    System.out.println("[TargetUtils] Error searching for entity list: " + e.getMessage());
                }
            }

            if (entities == null) {
                return null; // No hay entidades cargadas aún
            }

            Entity closestEntity = null;
            double closestDistance = range;

            for (Entity entity : entities) {
                if (entity == player) continue;
                if (!(entity instanceof EntityLivingBase)) continue;

                EntityLivingBase entityLiving = (EntityLivingBase) entity;
                if (entityLiving.getHealth() <= 0) continue;

                // Filtrar invisibles
                if (!invisibles && entityLiving.isInvisible()) continue;

                // Filtrar solo players si está activado
                if (players && !(entity instanceof EntityPlayer)) continue;

                double distance = player.getDistanceToEntity(entity);
                if (distance > range) continue;

                // Verificar FOV
                double yawChange = Math.abs(fovFromEntity(entity, player));
                if (yawChange > fov / 2.0) continue;

                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntity = entity;
                }
            }

            return closestEntity;
        } catch (Exception e) {
            return null;
        }
    }

    // Método de Raven: fovToEntity - NOW WITH DIRECT ACCESS
    public static float fovToEntity(Entity entity, EntityPlayer player) {
        try {
            double x = entity.posX - player.posX;
            double z = entity.posZ - player.posZ;
            // Corrected: atan2(z, x) for proper Minecraft yaw calculation
            // Minecraft: +X = East, +Z = South, Yaw 0 = South, Yaw increases clockwise
            double yaw = Math.atan2(z, x) * 57.2957795 - 90.0;
            return (float) yaw;
        } catch (Exception e) {
            return 0;
        }
    }

    // Método de Raven: fovFromEntity (diferencia entre yaw actual y yaw objetivo) - NOW WITH DIRECT ACCESS
    public static double fovFromEntity(Entity entity, EntityPlayer player) {
        try {
            float playerYaw = player.rotationYaw;
            return ((((double) (playerYaw - fovToEntity(entity, player)) % 360.0) + 540.0) % 360.0) - 180.0;
        } catch (Exception e) {
            return 0;
        }
    }

    // Método de Raven: PitchFromEntity - NOW WITH DIRECT ACCESS
    public static double pitchFromEntity(Entity entity, EntityPlayer player, float offset) {
        try {
            float playerPitch = player.rotationPitch;
            return (double) (playerPitch - pitchToEntity(entity, player, offset));
        } catch (Exception e) {
            return 0;
        }
    }

    // Método de Raven: pitchToEntity - NOW WITH DIRECT ACCESS
    public static float pitchToEntity(Entity entity, EntityPlayer player, float offset) {
        try {
            double x = player.getDistanceToEntity(entity);
            double playerY = player.posY;
            double entityY = entity.posY;
            double y = playerY - (entityY + offset);
            double pitch = (((Math.atan2(x, y) * 180.0) / Math.PI));
            return (float) (90 - pitch);
        } catch (Exception e) {
            return 0;
        }
    }

    // Getters for rotation - NOW WITH DIRECT ACCESS
    public static float getRotationYaw(Entity entity) {
        try {
            return entity.rotationYaw;
        } catch (Exception e) {
            return 0;
        }
    }

    public static float getRotationPitch(Entity entity) {
        try {
            return entity.rotationPitch;
        } catch (Exception e) {
            return 0;
        }
    }

    public static void setRotationYaw(Entity entity, float yaw) {
        try {
            entity.rotationYaw = yaw;
        } catch (Exception e) {
            // Silent fail
        }
    }

    public static void setRotationPitch(Entity entity, float pitch) {
        try {
            entity.rotationPitch = pitch;
        } catch (Exception e) {
            // Silent fail
        }
    }
}
