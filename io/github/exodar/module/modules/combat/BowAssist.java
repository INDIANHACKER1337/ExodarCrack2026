/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.module.modules.misc.Friends;
import io.github.exodar.module.modules.misc.MurderMystery;
import io.github.exodar.module.modules.misc.Teams;
import io.github.exodar.setting.*;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;

import java.util.*;

/**
 * BowAssist - Aims at targets with arrow trajectory prediction
 * Predicts target movement for accurate bow shots
 */
public class BowAssist extends Module {

    // Settings
    private ModeSetting sortMode;
    private SliderSetting maxAngle;
    private SliderSetting horizontalSpeed;
    private SliderSetting verticalSpeed;
    private TickSetting predict;
    private TickSetting targetInvisibles;
    private TickSetting aimThroughWalls;

    // State
    private EntityLivingBase currentTarget = null;

    // Velocity tracking for prediction
    private Map<Integer, VelocityTracker> velocityTrackers = new HashMap<>();

    public BowAssist() {
        super("BowAssist", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Bow aim with prediction"));
        this.registerSetting(sortMode = new ModeSetting("Sort", new String[]{"Distance", "FOV"}));
        this.registerSetting(maxAngle = new SliderSetting("Max Angle", 90.0, 15.0, 360.0, 5.0));
        this.registerSetting(horizontalSpeed = new SliderSetting("H Speed", 5.0, 0.1, 15.0, 0.5));
        this.registerSetting(verticalSpeed = new SliderSetting("V Speed", 5.0, 0.1, 15.0, 0.5));
        this.registerSetting(predict = new TickSetting("Predict", true));
        this.registerSetting(targetInvisibles = new TickSetting("Target Invisibles", false));
        this.registerSetting(aimThroughWalls = new TickSetting("Through Walls", false));
    }

    @Override
    public void onEnable() {
        velocityTrackers.clear();
        currentTarget = null;
    }

    @Override
    public void onDisable() {
        velocityTrackers.clear();
        currentTarget = null;
    }

    @Override
    public void onUpdate() {
        if (!enabled || !isInGame()) return;

        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        // Check if using bow
        ItemStack heldItem = player.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBow)) {
            velocityTrackers.clear();
            currentTarget = null;
            return;
        }

        // Check if player is charging bow
        if (!player.isUsingItem()) {
            return;
        }

        // Find target
        EntityLivingBase target = findTarget(player);
        if (target == null) {
            currentTarget = null;
            return;
        }

        currentTarget = target;

        // Calculate bow charge (0.0 to 1.0)
        int useCount = player.getItemInUseCount();
        int maxUse = heldItem.getMaxItemUseDuration();
        float charge = (float)(maxUse - useCount) / 20.0f;
        charge = Math.min(charge, 1.0f);

        // Calculate aim angles with prediction
        float[] aimAngles = calculateAimAngles(target, charge);
        float targetYaw = aimAngles[0];
        float targetPitch = aimAngles[1];

        // Calculate delta (normalize to -180 to 180)
        float yawDelta = normalizeAngle(targetYaw - player.rotationYaw);
        float pitchDelta = targetPitch - player.rotationPitch;

        // Apply speed limits
        float hSpeed = (float) horizontalSpeed.getValue();
        float vSpeed = (float) verticalSpeed.getValue();

        if (yawDelta > hSpeed) yawDelta = hSpeed;
        else if (yawDelta < -hSpeed) yawDelta = -hSpeed;

        if (pitchDelta > vSpeed) pitchDelta = vSpeed;
        else if (pitchDelta < -vSpeed) pitchDelta = -vSpeed;

        // Apply rotation directly to player
        player.rotationYaw += yawDelta;
        if (!Float.isNaN(pitchDelta)) {
            player.rotationPitch += pitchDelta;
        }
    }

    /**
     * Calculate aim angles with arrow trajectory prediction
     */
    private float[] calculateAimAngles(EntityLivingBase target, float charge) {
        EntityPlayerSP player = getPlayer();
        if (player == null) return new float[]{0, 0};

        double predictedX;
        double predictedZ;
        double predictedY;

        if (predict.isEnabled()) {
            // Get target velocity
            double velX = target.posX - target.lastTickPosX;
            double velZ = target.posZ - target.lastTickPosZ;
            double velY = target.posY - target.lastTickPosY;

            // Update velocity tracker
            int entityId = target.getEntityId();
            if (velX != 0 || velZ != 0) {
                VelocityTracker tracker = velocityTrackers.get(entityId);
                if (tracker == null) {
                    tracker = new VelocityTracker();
                    velocityTrackers.put(entityId, tracker);
                }
                tracker.addSample(velX, velZ, player.ticksExisted);
            }

            // Get averaged velocity for prediction
            double[] avgVel = {0, 0};
            VelocityTracker tracker = velocityTrackers.get(entityId);
            if (tracker != null) {
                avgVel = tracker.getAverageVelocity();
            }

            // Calculate distance for prediction scaling
            double distance = player.getDistanceToEntity(target);
            distance -= distance % 0.8;

            // Predict target position
            double predictionScale = distance / 0.8 * 0.66;
            predictedX = target.posX + avgVel[0] * predictionScale;
            predictedZ = target.posZ + avgVel[1] * predictionScale;
            predictedY = target.posY + target.getEyeHeight();

            // Adjust for falling targets
            if (!target.onGround && velY < 0 && target.fallDistance > 1.0f) {
                // Reduce horizontal prediction for falling targets
                predictedX = target.posX + avgVel[0] * predictionScale * 0.15;
                predictedZ = target.posZ + avgVel[1] * predictionScale * 0.15;
                // Add vertical prediction
                double vertPred = distance / 0.8 * velY * 0.5;
                vertPred += vertPred * 0.98 - 0.08;
                predictedY += vertPred;
            }
        } else {
            // No prediction - aim directly at target
            predictedX = target.posX;
            predictedZ = target.posZ;
            predictedY = target.posY + target.getEyeHeight();
        }

        // Calculate yaw (same formula as AimAssist)
        double deltaX = predictedX - player.posX;
        double deltaZ = predictedZ - player.posZ;
        float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;

        // Calculate pitch with arrow trajectory
        float pitch = calculateBowPitch(predictedX, predictedY, predictedZ, charge);

        return new float[]{yaw, pitch};
    }

    /**
     * Calculate bow pitch using projectile physics
     */
    private float calculateBowPitch(double targetX, double targetY, double targetZ, float charge) {
        EntityPlayerSP player = getPlayer();
        if (player == null) return 0;

        // Arrow velocity based on charge (max ~2.93 at full charge)
        double arrowVelocity = charge * 2.93;

        // Gravity constant for arrows
        double gravity = 0.05;

        // Calculate horizontal distance
        double deltaX = targetX - player.posX;
        double deltaZ = targetZ - player.posZ;
        double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        // Calculate vertical difference
        double deltaY = targetY - (player.posY + player.getEyeHeight());

        // Projectile motion calculation
        double v2 = arrowVelocity * arrowVelocity;
        double v4 = v2 * v2;
        double gx = gravity * horizontalDist;
        double gx2 = gx * horizontalDist;
        double gy2 = 2.0 * deltaY * v2;

        double discriminant = v4 - gravity * (gx2 + gy2);
        if (discriminant < 0) {
            // Target too far, aim directly
            return (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDist));
        }

        double sqrtDisc = Math.sqrt(discriminant);
        double angle1 = Math.atan2(v2 + sqrtDisc, gx);
        double angle2 = Math.atan2(v2 - sqrtDisc, gx);

        // Use the lower angle (flatter trajectory)
        return (float) -Math.toDegrees(Math.min(angle1, angle2));
    }


    /**
     * Find best target
     */
    private EntityLivingBase findTarget(EntityPlayerSP player) {
        List<EntityLivingBase> validTargets = new ArrayList<>();

        for (Object obj : mc.theWorld.loadedEntityList) {
            if (!(obj instanceof EntityLivingBase)) continue;
            EntityLivingBase entity = (EntityLivingBase) obj;

            if (!isValidTarget(entity, player)) continue;

            validTargets.add(entity);
        }

        if (validTargets.isEmpty()) return null;

        // Sort by selected mode
        String mode = sortMode.getSelected();
        if (mode.equals("FOV")) {
            validTargets.sort((a, b) -> Float.compare(
                getAngleToEntity(player, a),
                getAngleToEntity(player, b)
            ));
        } else { // Distance
            validTargets.sort((a, b) -> Float.compare(
                player.getDistanceToEntity(a),
                player.getDistanceToEntity(b)
            ));
        }

        return validTargets.get(0);
    }

    /**
     * Check if entity is a valid target (players only, no mobs)
     */
    private boolean isValidTarget(EntityLivingBase entity, EntityPlayerSP player) {
        if (entity == null || entity == player) return false;

        // Only target players, not mobs
        if (!(entity instanceof EntityPlayer)) return false;

        if (entity.isDead || entity.getHealth() <= 0) return false;

        // Invisibility check
        if (!targetInvisibles.isEnabled() && entity.isInvisible()) return false;

        // FOV check
        float angle = getAngleToEntity(player, entity);
        if (angle > maxAngle.getValue() / 2) return false;

        // Line of sight check
        if (!aimThroughWalls.isEnabled() && !player.canEntityBeSeen(entity)) return false;

        // Player-specific checks
        EntityPlayer targetPlayer = (EntityPlayer) entity;

        // Friends check
        if (Friends.isFriend(targetPlayer.getName())) return false;

        // AntiBot check
        if (AntiBot.isBotForCombat(targetPlayer)) return false;

        // Teams check
        Teams teams = Teams.getInstance();
        if (teams != null && teams.isEnabled() && teams.isTeamMate(entity)) return false;

        // MurderMystery check - only target murderer when MM is enabled and in game
        if (MurderMystery.isInMurderMysteryGame()) {
            // Only target if player is a murderer
            if (!MurderMystery.isMurderer(targetPlayer.getName())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get angle from player to entity
     */
    private float getAngleToEntity(EntityPlayerSP player, Entity entity) {
        double deltaX = entity.posX - player.posX;
        double deltaZ = entity.posZ - player.posZ;
        float targetYaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
        float yawDiff = normalizeAngle(targetYaw - player.rotationYaw);
        return Math.abs(yawDiff);
    }

    /**
     * Normalize angle to -180 to 180 range (same as AimAssist)
     */
    private float normalizeAngle(float angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    @Override
    public String getDisplaySuffix() {
        if (currentTarget != null) {
            return " §7" + currentTarget.getName();
        }
        return "";
    }

    /**
     * Velocity tracker for movement prediction
     */
    private static class VelocityTracker {
        private List<double[]> samples = new ArrayList<>();
        private int lastTick = 0;
        private static final int MAX_SAMPLES = 6;

        public void addSample(double velX, double velZ, int tick) {
            // Clear old samples if too much time has passed
            if (tick - lastTick > 5) {
                samples.clear();
            }
            lastTick = tick;

            samples.add(new double[]{velX, velZ});

            // Keep only recent samples
            while (samples.size() > MAX_SAMPLES) {
                samples.remove(0);
            }
        }

        public double[] getAverageVelocity() {
            if (samples.isEmpty()) return new double[]{0, 0};

            double avgX = 0, avgZ = 0;
            for (double[] sample : samples) {
                avgX += sample[0];
                avgZ += sample[1];
            }
            return new double[]{avgX / samples.size(), avgZ / samples.size()};
        }
    }
}
