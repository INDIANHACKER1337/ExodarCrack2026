/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.movement;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;

import java.util.Random;

/**
 * Clutch - Automatically places a block when falling
 * Based on Raven XD implementation - Strict mode only
 */
public class Clutch extends Module {

    private final SliderSetting aimSpeed;
    private final SliderSetting placeDelay;
    private final TickSetting overVoid;
    private final TickSetting fallDistance;
    private final SliderSetting minFallDistance;
    private final TickSetting autoSwitch;
    private final TickSetting restoreView;

    // Current rotation state
    private float rotYaw = 0;
    private float rotPitch = 0;
    private long lastPlace = 0;
    private int lastSlot = -1;

    // Restore view state
    private float originalYaw = 0;
    private float originalPitch = 0;
    private boolean wasClutching = false;
    private boolean isRestoringView = false;

    // Random for rotation jitter
    private final Random random = new Random();

    public Clutch() {
        super("Clutch", ModuleCategory.MOVEMENT);
        this.registerSetting(new DescriptionSetting("Auto-place block when falling"));

        this.registerSetting(aimSpeed = new SliderSetting("Aim Speed", 25.0, 10.0, 50.0, 1.0));
        this.registerSetting(placeDelay = new SliderSetting("Place Delay", 50.0, 0.0, 500.0, 10.0));
        this.registerSetting(overVoid = new TickSetting("Over Void", true));
        this.registerSetting(fallDistance = new TickSetting("Fall Distance", false));
        this.registerSetting(minFallDistance = new SliderSetting("Min Fall Distance", 6.0, 0.0, 10.0, 1.0));
        this.registerSetting(autoSwitch = new TickSetting("Auto Switch", true));
        this.registerSetting(restoreView = new TickSetting("Restore View", true));
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            rotYaw = mc.thePlayer.rotationYaw;
            rotPitch = mc.thePlayer.rotationPitch;
            lastSlot = mc.thePlayer.inventory.currentItem;
        }
        lastPlace = 0;
        wasClutching = false;
        isRestoringView = false;
    }

    @Override
    public void onDisable() {
        // Restore slot if needed
        Minecraft mc = Minecraft.getMinecraft();
        if (autoSwitch.isEnabled() && lastSlot != -1 && mc.thePlayer != null) {
            if (mc.thePlayer.inventory.currentItem != lastSlot) {
                mc.thePlayer.inventory.currentItem = lastSlot;
            }
        }
        wasClutching = false;
        isRestoringView = false;
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return;

        EntityPlayerSP player = mc.thePlayer;

        // Handle view restoration
        if (isRestoringView) {
            if (restoreViewToOriginal(player)) {
                isRestoringView = false;
            }
            return;
        }

        // Check if should clutch
        boolean shouldClutchNow = shouldClutch(player);

        // If we were clutching and now we're on ground, start restoring view
        if (wasClutching && player.onGround) {
            wasClutching = false;
            if (autoSwitch.isEnabled() && lastSlot != -1) {
                player.inventory.currentItem = lastSlot;
            }
            if (restoreView.isEnabled()) {
                isRestoringView = true;
            }
            return;
        }

        if (!shouldClutchNow) {
            return;
        }

        // Save original view when starting to clutch
        if (!wasClutching) {
            wasClutching = true;
            originalYaw = player.rotationYaw;
            originalPitch = player.rotationPitch;
            lastSlot = player.inventory.currentItem;
        }

        // Auto switch to blocks
        if (autoSwitch.isEnabled()) {
            int blockSlot = findBlockSlot(player);
            if (blockSlot != -1 && player.inventory.currentItem != blockSlot) {
                player.inventory.currentItem = blockSlot;
            }
        }

        // Check if holding blocks
        ItemStack held = player.inventory.getCurrentItem();
        if (held == null || !(held.getItem() instanceof ItemBlock)) {
            return;
        }

        // Find best placement
        PlaceInfo bestPlace = findBestPlacement(player);
        if (bestPlace == null) {
            return;
        }

        // Rotate towards target (like BlockIn - directly modify player rotation)
        rotateTowardsTarget(player, bestPlace.hitVec);

        // Strict mode: Raycast to verify looking at correct block
        MovingObjectPosition mop = rayTrace(player.rotationYaw, player.rotationPitch, 4.5);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }
        if (!mop.getBlockPos().equals(bestPlace.blockPos)) {
            return;
        }

        // Check place delay
        long now = System.currentTimeMillis();
        if (now - lastPlace < placeDelay.getValue()) {
            return;
        }

        // Place the block using KeyBinding (like BlockIn - avoids anticheat flags)
        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.setKeyBindState(useKey, true);
        KeyBinding.onTick(useKey);
        KeyBinding.setKeyBindState(useKey, false);

        lastPlace = now;
    }

    /**
     * Restore view to original position smoothly
     */
    private boolean restoreViewToOriginal(EntityPlayerSP player) {
        float yawDiff = MathHelper.wrapAngleTo180_float(originalYaw - player.rotationYaw);
        float pitchDiff = originalPitch - player.rotationPitch;

        // Close enough?
        if (Math.abs(yawDiff) < 2.0f && Math.abs(pitchDiff) < 2.0f) {
            player.rotationYaw = originalYaw;
            player.rotationPitch = originalPitch;
            return true;
        }

        float speed = (float) aimSpeed.getValue();
        float yawStep = MathHelper.clamp_float(yawDiff, -speed, speed);
        float pitchStep = MathHelper.clamp_float(pitchDiff, -speed, speed);

        player.rotationYaw += yawStep;
        player.rotationPitch = MathHelper.clamp_float(player.rotationPitch + pitchStep, -90.0f, 90.0f);

        return false;
    }

    /**
     * Check if player should clutch
     */
    private boolean shouldClutch(EntityPlayerSP player) {
        if (player.onGround) return false;

        // Check over void
        if (overVoid.isEnabled() && isOverVoid(player)) {
            return true;
        }

        // Check fall distance
        if (fallDistance.isEnabled() && player.fallDistance >= minFallDistance.getValue()) {
            return true;
        }

        return false;
    }

    /**
     * Check if player is over void (no solid block below within 20 blocks)
     */
    private boolean isOverVoid(EntityPlayerSP player) {
        Minecraft mc = Minecraft.getMinecraft();
        BlockPos pos = new BlockPos(player.posX, player.posY, player.posZ);

        for (int y = 0; y < 20; y++) {
            BlockPos check = pos.down(y);
            if (check.getY() < 0) return true; // Below world
            Block block = mc.theWorld.getBlockState(check).getBlock();
            if (block != Blocks.air && block.isFullCube()) {
                return false; // Found solid block
            }
        }

        return true;
    }

    /**
     * Find the best block placement position
     */
    private PlaceInfo findBestPlacement(EntityPlayerSP player) {
        Minecraft mc = Minecraft.getMinecraft();
        Vec3 eyePos = player.getPositionEyes(1.0f);
        BlockPos playerPos = new BlockPos(player.posX, player.posY, player.posZ);

        // Target position (where we want to land)
        Vec3 groundPos = new Vec3(playerPos.getX() + 0.5, playerPos.getY() - 1, playerPos.getZ() + 0.5);

        double minDistance = Double.MAX_VALUE;
        PlaceInfo bestPlace = null;

        // Scan area around player
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 0; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);

                    // Skip if not replaceable (air/water)
                    if (!isReplaceable(checkPos, mc)) continue;

                    // Skip player's position
                    if (checkPos.equals(playerPos) || checkPos.equals(playerPos.up())) continue;

                    // Try to find a valid placement
                    PlaceInfo place = getPlaceSide(checkPos, eyePos, mc);
                    if (place == null) continue;

                    // Check distance to eye
                    double eyeDist = place.hitVec.distanceTo(eyePos);
                    if (eyeDist > 4.5) continue;

                    // Prefer blocks closer to where we need to land
                    double groundDist = place.hitVec.distanceTo(groundPos);
                    if (groundDist < minDistance) {
                        minDistance = groundDist;
                        bestPlace = place;
                    }
                }
            }
        }

        return bestPlace;
    }

    /**
     * Get the valid placement side for a block position
     */
    private PlaceInfo getPlaceSide(BlockPos targetPos, Vec3 eyePos, Minecraft mc) {
        // Check all 6 sides
        for (EnumFacing facing : EnumFacing.values()) {
            BlockPos supportPos = targetPos.offset(facing);

            // Check if support block is solid
            Block supportBlock = mc.theWorld.getBlockState(supportPos).getBlock();
            if (supportBlock == Blocks.air || !supportBlock.isFullCube()) continue;

            // Calculate hit position on the face
            EnumFacing placeFacing = facing.getOpposite();
            Vec3 hitVec = getHitVec(supportPos, placeFacing);

            // Check if in range
            if (hitVec.distanceTo(eyePos) > 4.5) continue;

            return new PlaceInfo(supportPos, placeFacing, hitVec);
        }

        return null;
    }

    /**
     * Get hit vector for a block face
     */
    private Vec3 getHitVec(BlockPos pos, EnumFacing face) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        switch (face) {
            case DOWN:
                y = pos.getY();
                break;
            case UP:
                y = pos.getY() + 1;
                break;
            case NORTH:
                z = pos.getZ();
                break;
            case SOUTH:
                z = pos.getZ() + 1;
                break;
            case WEST:
                x = pos.getX();
                break;
            case EAST:
                x = pos.getX() + 1;
                break;
        }

        return new Vec3(x, y, z);
    }

    /**
     * Check if a block position is replaceable
     */
    private boolean isReplaceable(BlockPos pos, Minecraft mc) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return block == Blocks.air
            || block == Blocks.water
            || block == Blocks.flowing_water
            || block == Blocks.lava
            || block == Blocks.flowing_lava
            || block == Blocks.tallgrass
            || block == Blocks.deadbush
            || block == Blocks.snow_layer;
    }

    /**
     * Find block slot in hotbar
     * Excludes TNT, crafting tables, and furnaces
     */
    private int findBlockSlot(EntityPlayerSP player) {
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack stack = player.inventory.getStackInSlot(slot);
            if (stack != null && stack.stackSize > 0 && stack.getItem() instanceof ItemBlock) {
                Block block = ((ItemBlock) stack.getItem()).getBlock();
                if (block.isFullCube()) {
                    // Skip TNT, crafting tables, and furnaces
                    if (block == Blocks.tnt) continue;
                    if (block == Blocks.crafting_table) continue;
                    if (block == Blocks.furnace) continue;
                    if (block == Blocks.lit_furnace) continue;
                    return slot;
                }
            }
        }
        return -1;
    }

    /**
     * Get yaw rotation to a position
     */
    private float getYaw(EntityPlayerSP player, Vec3 target) {
        Vec3 eye = player.getPositionEyes(1.0f);
        double dx = target.xCoord - eye.xCoord;
        double dz = target.zCoord - eye.zCoord;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        return MathHelper.wrapAngleTo180_float(yaw);
    }

    /**
     * Get pitch rotation to a position
     */
    private float getPitch(EntityPlayerSP player, Vec3 target) {
        Vec3 eye = player.getPositionEyes(1.0f);
        double dx = target.xCoord - eye.xCoord;
        double dy = target.yCoord - eye.yCoord;
        double dz = target.zCoord - eye.zCoord;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return MathHelper.clamp_float(pitch, -90.0f, 90.0f);
    }

    /**
     * Rotate player camera towards target (like BlockIn - avoids Grim DuplicateRotPlace)
     * Uses randomization and jitter to make rotation deltas unique
     */
    private void rotateTowardsTarget(EntityPlayerSP player, Vec3 target) {
        Vec3 eyes = player.getPositionEyes(1.0f);
        double dx = target.xCoord - eyes.xCoord;
        double dy = target.yCoord - eyes.yCoord;
        double dz = target.zCoord - eyes.zCoord;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        targetYaw = MathHelper.wrapAngleTo180_float(targetYaw);

        float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - player.rotationYaw);
        float pitchDiff = targetPitch - player.rotationPitch;

        // Add randomization to rotation speed (±30% variation like BlockIn)
        float baseSpeed = (float) aimSpeed.getValue();
        float speedVariation = baseSpeed * 0.3f;
        float maxTurn = baseSpeed + (random.nextFloat() * speedVariation * 2 - speedVariation);

        float yawStep = MathHelper.clamp_float(yawDiff, -maxTurn, maxTurn);
        float pitchStep = MathHelper.clamp_float(pitchDiff, -maxTurn, maxTurn);

        // Add micro-jitter for more human-like movement (±1.0 yaw, ±0.5 pitch)
        float jitterYaw = (random.nextFloat() - 0.5f) * 1.0f;
        float jitterPitch = (random.nextFloat() - 0.5f) * 0.5f;

        // Only apply jitter if we're still rotating (not at target yet)
        if (Math.abs(yawDiff) > 2.0f) {
            yawStep += jitterYaw;
        }
        if (Math.abs(pitchDiff) > 2.0f) {
            pitchStep += jitterPitch;
        }

        // Apply rotation to player
        player.rotationYaw += yawStep;
        player.rotationPitch = MathHelper.clamp_float(player.rotationPitch + pitchStep, -90.0f, 90.0f);
    }

    /**
     * Smooth rotation for restore view (simple version)
     */
    private float smoothRotation(float current, float target, float speed) {
        float diff = MathHelper.wrapAngleTo180_float(target - current);
        float step = MathHelper.clamp_float(diff, -speed, speed);
        return current + step;
    }

    /**
     * Raycast with specific rotation
     */
    private MovingObjectPosition rayTrace(float yaw, float pitch, double range) {
        Minecraft mc = Minecraft.getMinecraft();
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);

        Vec3 start = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 end = start.addVector(x * range, y * range, z * range);

        return mc.theWorld.rayTraceBlocks(start, end);
    }

    /**
     * Helper class for placement info
     */
    private static class PlaceInfo {
        final BlockPos blockPos;
        final EnumFacing facing;
        final Vec3 hitVec;

        PlaceInfo(BlockPos blockPos, EnumFacing facing, Vec3 hitVec) {
            this.blockPos = blockPos;
            this.facing = facing;
            this.hitVec = hitVec;
        }
    }
}
