/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.Main;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import io.github.exodar.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * BowAimbot - Automatically aims at targets when using a bow
 * Based on Raven XD implementation
 */
public class BowAimbot extends Module {

    private final SliderSetting range;
    private final SliderSetting fov;
    private final SliderSetting prediction;
    private final ModeSetting rotationMode;
    private final TickSetting autoShoot;
    private final TickSetting requireMouseDown;
    private final TickSetting silentRelease;
    private final TickSetting ignoreTeammates;
    private final TickSetting targetInvisible;
    private final TickSetting targetPlayers;
    private final TickSetting targetMobs;

    private EntityLivingBase target;
    private float[] targetRotations = new float[2];
    private boolean shouldShoot = false;

    public BowAimbot() {
        super("BowAimbot", ModuleCategory.COMBAT);
        this.registerSetting(new DescriptionSetting("Auto-aim with bow"));
        this.registerSetting(range = new SliderSetting("Range", 50.0, 10.0, 100.0, 1.0));
        this.registerSetting(fov = new SliderSetting("FOV", 360.0, 30.0, 360.0, 10.0));
        this.registerSetting(prediction = new SliderSetting("Prediction", 0.5, 0.0, 2.0, 0.1));
        this.registerSetting(rotationMode = new ModeSetting("Rotation", new String[]{"Silent", "Lock View"}));
        this.registerSetting(autoShoot = new TickSetting("Auto Shoot", true));
        this.registerSetting(requireMouseDown = new TickSetting("Require Mouse Down", false));
        this.registerSetting(silentRelease = new TickSetting("Silent Release", false));
        this.registerSetting(ignoreTeammates = new TickSetting("Ignore Teammates", true));
        this.registerSetting(targetInvisible = new TickSetting("Target Invisible", false));
        this.registerSetting(targetPlayers = new TickSetting("Target Players", true));
        this.registerSetting(targetMobs = new TickSetting("Target Mobs", false));
    }

    @Override
    public void onEnable() {
        target = null;
        shouldShoot = false;
    }

    @Override
    public void onDisable() {
        target = null;
        shouldShoot = false;
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Check if bow is drawn
        if (!isBowDrawn()) {
            target = null;
            shouldShoot = false;
            return;
        }

        // Check mouse requirement
        if (requireMouseDown.isEnabled() && !mc.gameSettings.keyBindUseItem.isKeyDown()) {
            target = null;
            shouldShoot = false;
            return;
        }

        // Find target
        findTarget();

        if (target == null) {
            shouldShoot = false;
            return;
        }

        // Calculate rotations
        calculateRotations();

        // Apply rotations
        if (rotationMode.getSelected().equals("Lock View")) {
            mc.thePlayer.rotationYaw = targetRotations[0];
            mc.thePlayer.rotationPitch = targetRotations[1];
        }

        // Check if fully charged and should shoot
        if (autoShoot.isEnabled() && isFullyCharged()) {
            shouldShoot = true;
            releaseBow();
        }
    }

    private boolean isBowDrawn() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return false;

        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null) return false;

        return heldItem.getItem() instanceof ItemBow && mc.thePlayer.isUsingItem();
    }

    private boolean isFullyCharged() {
        Minecraft mc = Minecraft.getMinecraft();
        int useDuration = mc.thePlayer.getItemInUseDuration();
        float charge = useDuration / 20.0F;
        charge = (charge * charge + charge * 2.0F) / 3.0F;
        return charge >= 1.0F;
    }

    private void findTarget() {
        Minecraft mc = Minecraft.getMinecraft();
        List<EntityLivingBase> targets = new ArrayList<>();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) continue;
            if (entity == mc.thePlayer) continue;

            EntityLivingBase living = (EntityLivingBase) entity;

            // Check entity type
            if (living instanceof EntityPlayer) {
                if (!targetPlayers.isEnabled()) continue;
                if (ignoreTeammates.isEnabled() && isTeammate((EntityPlayer) living)) continue;
            } else {
                if (!targetMobs.isEnabled()) continue;
            }

            // Check invisibility
            if (!targetInvisible.isEnabled() && living.isInvisible()) continue;

            // Check range
            if (mc.thePlayer.getDistanceToEntity(living) > range.getValue()) continue;

            // Check FOV
            if (fov.getValue() < 360 && !isInFov(living, (float) fov.getValue())) continue;

            // Check if alive
            if (living.isDead || living.getHealth() <= 0) continue;

            targets.add(living);
        }

        // Sort by distance
        targets.sort(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e)));

        target = targets.isEmpty() ? null : targets.get(0);
    }

    private boolean isTeammate(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer.getTeam() == null || player.getTeam() == null) return false;
        return mc.thePlayer.getTeam().isSameTeam(player.getTeam());
    }

    private boolean isInFov(Entity entity, float fovAngle) {
        Minecraft mc = Minecraft.getMinecraft();
        double deltaX = entity.posX - mc.thePlayer.posX;
        double deltaZ = entity.posZ - mc.thePlayer.posZ;
        double yawToTarget = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;

        double yawDiff = MathHelper.wrapAngleTo180_float((float) (mc.thePlayer.rotationYaw - yawToTarget));
        return Math.abs(yawDiff) <= fovAngle / 2.0;
    }

    private void calculateRotations() {
        if (target == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        double predictAmount = prediction.getValue();

        // Predict target position
        double targetX = target.posX + (target.posX - target.prevPosX) * predictAmount;
        double targetY = target.posY + (target.posY - target.prevPosY) * predictAmount + target.getEyeHeight() - 0.15;
        double targetZ = target.posZ + (target.posZ - target.prevPosZ) * predictAmount;

        // Player eye position
        double eyeX = mc.thePlayer.posX;
        double eyeY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
        double eyeZ = mc.thePlayer.posZ;

        double diffX = targetX - eyeX;
        double diffY = targetY - eyeY;
        double diffZ = targetZ - eyeZ;
        double horizontalDist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        // Calculate bow charge
        int useDuration = mc.thePlayer.getItemInUseDuration();
        float charge = useDuration / 20.0F;
        charge = (charge * charge + charge * 2.0F) / 3.0F;
        if (charge > 1.0F) charge = 1.0F;

        // Arrow velocity based on charge
        double velocity = charge * 3.0;
        double gravity = 0.05;

        // Calculate pitch with arrow trajectory
        double discriminant = Math.pow(velocity, 4) - gravity * (gravity * Math.pow(horizontalDist, 2)
                + 2 * diffY * Math.pow(velocity, 2));

        double pitch;
        if (discriminant < 0) {
            // Can't reach, aim directly
            pitch = -Math.toDegrees(Math.atan2(diffY, horizontalDist));
        } else {
            double numerator = velocity * velocity - Math.sqrt(discriminant);
            double denominator = gravity * horizontalDist;
            pitch = -Math.toDegrees(Math.atan(numerator / denominator));
        }

        double yaw = Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0;

        targetRotations[0] = (float) yaw;
        targetRotations[1] = (float) MathHelper.clamp_double(pitch, -90, 90);
    }

    private void releaseBow() {
        Minecraft mc = Minecraft.getMinecraft();

        if (silentRelease.isEnabled()) {
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                BlockPos.ORIGIN,
                EnumFacing.DOWN
            ));
        } else {
            mc.thePlayer.stopUsingItem();
        }

        shouldShoot = false;
    }

    /**
     * Get target rotations for silent mode - called from rotation hook
     */
    public float[] getTargetRotations() {
        if (target != null && rotationMode.getSelected().equals("Silent")) {
            return targetRotations;
        }
        return null;
    }

    public boolean hasTarget() {
        return target != null && isBowDrawn();
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + (autoShoot.isEnabled() ? "Auto" : "Manual");
    }
}
