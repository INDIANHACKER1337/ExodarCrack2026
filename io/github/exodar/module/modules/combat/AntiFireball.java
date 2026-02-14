/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * AntiFireball - Automatically hits fireballs coming towards you
 */
public class AntiFireball extends Module {

    private final SliderSetting range;
    private final SliderSetting fov;
    private final TickSetting swing;
    private final TickSetting onlyIncoming;
    private final TickSetting rotate;
    private final SliderSetting rotationSpeed;

    private Entity target = null;
    private final List<Entity> trackedFireballs = new ArrayList<>();
    private int debugTick = 0;

    // Rotation state
    private float targetYaw = 0;
    private float targetPitch = 0;
    private boolean isRotating = false;

    public AntiFireball() {
        super("AntiFireball", ModuleCategory.COMBAT);
        this.registerSetting(new DescriptionSetting("Auto-hit fireballs"));

        this.registerSetting(range = new SliderSetting("Range", 5.0, 3.0, 8.0, 0.5));
        this.registerSetting(fov = new SliderSetting("FOV", 360.0, 30.0, 360.0, 10.0));
        this.registerSetting(swing = new TickSetting("Swing", true));
        this.registerSetting(onlyIncoming = new TickSetting("Only Incoming", true));
        this.registerSetting(rotate = new TickSetting("Rotate", true));
        this.registerSetting(rotationSpeed = new SliderSetting("Rotation Speed", 180.0, 30.0, 360.0, 10.0));
    }

    @Override
    public void onDisable() {
        target = null;
        trackedFireballs.clear();
        isRotating = false;
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return;

        // Don't do anything in creative
        if (mc.thePlayer.capabilities.allowFlying) {
            target = null;
            return;
        }

        // Find all fireballs (check multiple ways for compatibility)
        List<Entity> fireballs = new ArrayList<>();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (isFireballEntity(entity)) {
                fireballs.add(entity);
            }
        }

        // Debug: print entity types every 100 ticks
        debugTick++;
        if (debugTick >= 100) {
            debugTick = 0;
            if (!fireballs.isEmpty()) {
                for (Entity e : fireballs) {
                    System.out.println("[AntiFireball] Found: " + e.getClass().getName() + " at distance " + distanceToFireball(e));
                }
            }
        }

        // Clean up tracked list
        trackedFireballs.removeIf(fb -> !fireballs.contains(fb));

        // Find best target
        target = fireballs.stream()
            .filter(this::isValidTarget)
            .min(Comparator.comparingDouble(this::distanceToFireball))
            .orElse(null);

        // Attack target
        if (target != null) {
            double dist = distanceToFireball(target);
            if (dist <= range.getValue()) {
                if (rotate.isEnabled()) {
                    // Calculate target rotation
                    float[] rotations = getRotationsToEntity(target);
                    targetYaw = rotations[0];
                    targetPitch = rotations[1];

                    // Smooth rotation toward fireball
                    float speed = (float) rotationSpeed.getValue();
                    float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw);
                    float pitchDiff = targetPitch - mc.thePlayer.rotationPitch;

                    // Clamp rotation speed
                    float yawStep = Math.max(-speed, Math.min(speed, yawDiff));
                    float pitchStep = Math.max(-speed, Math.min(speed, pitchDiff));

                    // Apply rotation
                    mc.thePlayer.rotationYaw += yawStep;
                    mc.thePlayer.rotationPitch += pitchStep;
                    mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch, -90, 90);

                    isRotating = true;

                    // Only attack when rotation is close enough (within 15 degrees)
                    float remainingYaw = Math.abs(MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw));
                    float remainingPitch = Math.abs(targetPitch - mc.thePlayer.rotationPitch);
                    if (remainingYaw < 15 && remainingPitch < 15) {
                        attackFireball(target);
                    }
                } else {
                    // No rotation, just attack
                    attackFireball(target);
                }
            } else {
                isRotating = false;
            }
        } else {
            isRotating = false;
        }
    }

    /**
     * Calculate rotations to look at an entity
     */
    private float[] getRotationsToEntity(Entity entity) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return new float[]{0, 0};

        double dx = entity.posX - mc.thePlayer.posX;
        double dy = entity.posY - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = entity.posZ - mc.thePlayer.posZ;

        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        return new float[]{yaw, pitch};
    }

    /**
     * Check if entity is a fireball (multiple detection methods for compatibility)
     */
    private boolean isFireballEntity(Entity entity) {
        if (entity == null) return false;

        // Direct instanceof checks
        if (entity instanceof EntityFireball) return true;
        if (entity instanceof EntityLargeFireball) return true;
        if (entity instanceof EntitySmallFireball) return true;
        if (entity instanceof EntityWitherSkull) return true;

        // Class name checks (for obfuscated environments)
        String className = entity.getClass().getName().toLowerCase();
        String simpleName = entity.getClass().getSimpleName().toLowerCase();

        return className.contains("fireball")
            || className.contains("witherskull")
            || simpleName.contains("fireball")
            || simpleName.contains("witherskull");
    }

    private boolean isValidTarget(Entity fireball) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return false;

        // Check distance
        double dist = distanceToFireball(fireball);
        if (dist > range.getValue() + 3.0) return false;

        // Check FOV
        if (fov.getValue() < 360) {
            float angle = getAngleToEntity(fireball);
            if (angle > fov.getValue() / 2.0f) return false;
        }

        // Check if incoming (moving towards player)
        if (onlyIncoming.isEnabled()) {
            double dx = fireball.posX - fireball.lastTickPosX;
            double dy = fireball.posY - fireball.lastTickPosY;
            double dz = fireball.posZ - fireball.lastTickPosZ;

            double toPlayerX = mc.thePlayer.posX - fireball.posX;
            double toPlayerY = (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()) - fireball.posY;
            double toPlayerZ = mc.thePlayer.posZ - fireball.posZ;

            // Dot product - positive means coming towards us
            double dot = dx * toPlayerX + dy * toPlayerY + dz * toPlayerZ;
            if (dot <= 0) return false;
        }

        return true;
    }

    private double distanceToFireball(Entity fireball) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return Double.MAX_VALUE;

        double dx = fireball.posX - mc.thePlayer.posX;
        double dy = fireball.posY - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = fireball.posZ - mc.thePlayer.posZ;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private float getAngleToEntity(Entity entity) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return 180f;

        double dx = entity.posX - mc.thePlayer.posX;
        double dz = entity.posZ - mc.thePlayer.posZ;

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float playerYaw = mc.thePlayer.rotationYaw;

        float diff = MathHelper.wrapAngleTo180_float(targetYaw - playerYaw);
        return Math.abs(diff);
    }

    private void attackFireball(Entity fireball) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        try {
            // Set the fireball as the pointed entity so the click hits it
            mc.pointedEntity = fireball;

            // Use real left-click via KeyBinding (like AutoClicker)
            int attackKey = mc.gameSettings.keyBindAttack.getKeyCode();

            // Simulate key press
            KeyBinding.setKeyBindState(attackKey, true);
            KeyBinding.onTick(attackKey);

            // Release key immediately
            KeyBinding.setKeyBindState(attackKey, false);

        } catch (Exception e) {
            System.out.println("[AntiFireball] Error attacking: " + e.getMessage());
        }
    }

    @Override
    public String getDisplaySuffix() {
        return target != null ? " §7[Target]" : null;
    }
}
