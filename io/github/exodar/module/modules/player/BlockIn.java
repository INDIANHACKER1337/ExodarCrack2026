/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.player;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.KeybindSetting;
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
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.*;
import java.util.Random;

/**
 * BlockIn - Automatically places blocks around you to "box in"
 * Legit mode: First centers player on block, then rotates camera visibly
 */
public class BlockIn extends Module {

    private final SliderSetting range;
    private final SliderSetting rotationSpeed;
    private final SliderSetting placeDelay;
    private final SliderSetting rotationTolerance;
    private final SliderSetting centerTolerance;
    private final TickSetting showProgress;
    private final TickSetting autoSwitch;
    private final TickSetting prioritizeRoof;
    private final TickSetting autoCenter;
    private final TickSetting autoJump;
    private final TickSetting restoreLook;
    private final TickSetting autoSneak;
    private final KeybindSetting forceKey;

    // Force activation state
    private boolean isActive = false;

    // Block priority (lower = better)
    private final Map<String, Integer> BLOCK_SCORE = new HashMap<>();

    // State
    private BlockPos targetBlock = null;
    private EnumFacing targetFacing = null;
    private Vec3 targetHitVec = null;
    private float aimYaw = 0;
    private float aimPitch = 0;
    private long lastPlaceTime = 0;
    private int lastSlot = -1;
    private int activeSlot = -1; // Track slot when BlockIn started
    private float progress = 0;

    // Centering state
    private boolean isCentering = false;
    private double targetCenterX = 0;
    private double targetCenterZ = 0;

    // Roof placement state
    private boolean isPlacingRoof = false;

    // Restore look state
    private float originalYaw = 0;
    private float originalPitch = 0;
    private boolean isRestoringLook = false;

    // Track which keys WE are pressing (to distinguish from user input)
    private boolean wePressingW = false;
    private boolean wePressingA = false;
    private boolean wePressingS = false;
    private boolean wePressingD = false;
    private boolean wePressingSneak = false;

    // Directions for sides
    private static final int[][] DIRS = {{1, 0, 0}, {0, 0, 1}, {-1, 0, 0}, {0, 0, -1}};

    // Random for rotation variation
    private final Random random = new Random();

    public BlockIn() {
        super("BlockIn", ModuleCategory.PLAYER);
        this.registerSetting(new DescriptionSetting("Box yourself in with blocks"));

        this.registerSetting(forceKey = new KeybindSetting("Force BlockIn Key"));
        this.registerSetting(range = new SliderSetting("Range", 4.5, 3.0, 6.0, 0.5));
        this.registerSetting(rotationSpeed = new SliderSetting("Rotation Speed", 20.0, 5.0, 100.0, 5.0));
        this.registerSetting(placeDelay = new SliderSetting("Place Delay", 50.0, 0.0, 200.0, 10.0));
        this.registerSetting(rotationTolerance = new SliderSetting("Rotation Tolerance", 25.0, 5.0, 100.0, 5.0));
        this.registerSetting(centerTolerance = new SliderSetting("Center Tolerance", 0.15, 0.05, 0.3, 0.01));
        this.registerSetting(showProgress = new TickSetting("Show Progress", true));
        this.registerSetting(autoSwitch = new TickSetting("Auto Switch", true));
        this.registerSetting(prioritizeRoof = new TickSetting("Prioritize Roof", true));
        this.registerSetting(autoCenter = new TickSetting("Auto Center", true));
        this.registerSetting(autoJump = new TickSetting("Auto Jump (Roof)", true));
        this.registerSetting(restoreLook = new TickSetting("Restore Look", true));
        this.registerSetting(autoSneak = new TickSetting("Auto Sneak", false));

        // Block priorities
        BLOCK_SCORE.put("obsidian", 0);
        BLOCK_SCORE.put("end_stone", 1);
        BLOCK_SCORE.put("planks", 2);
        BLOCK_SCORE.put("log", 2);
        BLOCK_SCORE.put("glass", 3);
        BLOCK_SCORE.put("stained_glass", 3);
        BLOCK_SCORE.put("hardened_clay", 4);
        BLOCK_SCORE.put("stained_hardened_clay", 4);
        BLOCK_SCORE.put("wool", 5);
        BLOCK_SCORE.put("cloth", 5);
        BLOCK_SCORE.put("cobblestone", 6);
        BLOCK_SCORE.put("stone", 6);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            aimYaw = mc.thePlayer.rotationYaw;
            aimPitch = mc.thePlayer.rotationPitch;
            lastSlot = mc.thePlayer.inventory.currentItem;
        }
        targetBlock = null;
        targetFacing = null;
        targetHitVec = null;
        lastPlaceTime = 0;
        progress = 0;
        isActive = false;
        isCentering = false;
        isPlacingRoof = false;
        isRestoringLook = false;
        wePressingW = false;
        wePressingA = false;
        wePressingS = false;
        wePressingD = false;
        wePressingSneak = false;
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getMinecraft();
        if (lastSlot != -1 && mc.thePlayer != null && autoSwitch.isEnabled()) {
            if (mc.thePlayer.inventory.currentItem != lastSlot) {
                mc.thePlayer.inventory.currentItem = lastSlot;
            }
        }
        targetBlock = null;
        targetFacing = null;
        targetHitVec = null;
        progress = 0;
        isActive = false;
        isCentering = false;
        isPlacingRoof = false;
        isRestoringLook = false;
        wePressingW = false;
        wePressingA = false;
        wePressingS = false;
        wePressingD = false;

        // Release any held movement keys
        releaseMovementKeys(mc);

        // Release sneak key if we were pressing it
        if (wePressingSneak) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            wePressingSneak = false;
        }
    }

    /**
     * Release all movement keys
     */
    private void releaseMovementKeys(Minecraft mc) {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
        wePressingW = false;
        wePressingA = false;
        wePressingS = false;
        wePressingD = false;
    }

    /**
     * Cancel BlockIn and restore state
     */
    private void cancelBlockIn(EntityPlayerSP player, Minecraft mc) {
        if (autoSwitch.isEnabled() && lastSlot != -1) {
            player.inventory.currentItem = lastSlot;
        }
        isActive = false;
        isCentering = false;
        releaseMovementKeys(mc);
        // Release sneak key if we were pressing it
        if (wePressingSneak) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            wePressingSneak = false;
        }
        targetBlock = null;
        targetFacing = null;
        targetHitVec = null;
        activeSlot = -1;
    }

    /**
     * Check if USER is pressing movement keys (not us)
     * Returns true if user is pressing any WASD key that WE are not pressing
     */
    private boolean isUserPressingMovementKeys(Minecraft mc) {
        int keyW = mc.gameSettings.keyBindForward.getKeyCode();
        int keyA = mc.gameSettings.keyBindLeft.getKeyCode();
        int keyS = mc.gameSettings.keyBindBack.getKeyCode();
        int keyD = mc.gameSettings.keyBindRight.getKeyCode();

        // Check if key is physically down AND we're not the ones pressing it
        if (Keyboard.isKeyDown(keyW) && !wePressingW) return true;
        if (Keyboard.isKeyDown(keyA) && !wePressingA) return true;
        if (Keyboard.isKeyDown(keyS) && !wePressingS) return true;
        if (Keyboard.isKeyDown(keyD) && !wePressingD) return true;

        return false;
    }

    /**
     * Check if player is centered on block
     */
    private boolean isPlayerCentered(EntityPlayerSP player) {
        double tolerance = centerTolerance.getValue();
        double dx = Math.abs(player.posX - targetCenterX);
        double dz = Math.abs(player.posZ - targetCenterZ);
        return dx < tolerance && dz < tolerance;
    }

    /**
     * Move player towards center of block using WASD
     * Uses world coordinates, not player-relative movement
     */
    private void moveTowardsCenter(EntityPlayerSP player, Minecraft mc) {
        double dx = targetCenterX - player.posX; // positive = need to go +X (East)
        double dz = targetCenterZ - player.posZ; // positive = need to go +Z (South)

        double tolerance = centerTolerance.getValue();

        // Calculate the yaw angle to face the center
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - player.rotationYaw);

        // Convert world direction to player-relative direction
        // Forward in Minecraft: -sin(yaw) for X, cos(yaw) for Z
        double yawRad = Math.toRadians(player.rotationYaw);

        // Project the world delta onto player's forward/strafe axes
        // Forward axis: (-sin(yaw), cos(yaw))
        // Right axis: (cos(yaw), sin(yaw))
        double forwardComponent = -Math.sin(yawRad) * dx + Math.cos(yawRad) * dz;
        double strafeComponent = Math.cos(yawRad) * dx + Math.sin(yawRad) * dz;

        // Use a deadzone to prevent oscillation
        double deadzone = tolerance * 0.5;

        // Press W/S based on forward component
        if (forwardComponent > deadzone) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
            wePressingW = true;
            wePressingS = false;
        } else if (forwardComponent < -deadzone) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), true);
            wePressingW = false;
            wePressingS = true;
        } else {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
            wePressingW = false;
            wePressingS = false;
        }

        // Press A/D based on strafe component
        if (strafeComponent > deadzone) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), true);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
            wePressingD = true;
            wePressingA = false;
        } else if (strafeComponent < -deadzone) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), true);
            wePressingD = false;
            wePressingA = true;
        } else {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
            wePressingD = false;
            wePressingA = false;
        }
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return;

        EntityPlayerSP player = mc.thePlayer;

        // Update progress always
        updateProgress(player);

        // Check for force key activation
        boolean forceKeyPressed = forceKey.isKeyPressed();

        // Activate when force key is pressed
        if (forceKeyPressed && !isActive && !isRestoringLook) {
            isActive = true;
            lastSlot = player.inventory.currentItem;
            activeSlot = player.inventory.currentItem; // Track slot when started

            // Save original look direction for restoration
            originalYaw = player.rotationYaw;
            originalPitch = player.rotationPitch;

            // Start centering if enabled
            if (autoCenter.isEnabled()) {
                targetCenterX = Math.floor(player.posX) + 0.5;
                targetCenterZ = Math.floor(player.posZ) + 0.5;
                isCentering = true;
            }

            // Start auto sneaking if enabled
            if (autoSneak.isEnabled()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
                wePressingSneak = true;
            }
        }

        // Cancel if slot changed (user scrolled or pressed number key)
        if (isActive && activeSlot != -1 && player.inventory.currentItem != activeSlot) {
            // Only cancel if autoSwitch is off OR slot wasn't changed by us
            if (!autoSwitch.isEnabled()) {
                cancelBlockIn(player, mc);
                return;
            }
        }

        // Cancel if left click (attack)
        if (isActive && Mouse.isButtonDown(0)) {
            cancelBlockIn(player, mc);
            return;
        }

        // Handle look restoration phase
        if (isRestoringLook) {
            if (restoreLookToOriginal(player)) {
                // Restoration complete
                isRestoringLook = false;
            }
            return;
        }

        // Check if boxing is complete (progress = 100%)
        if (isActive && progress >= 1.0f) {
            // Restore slot
            if (autoSwitch.isEnabled() && lastSlot != -1) {
                player.inventory.currentItem = lastSlot;
            }

            isActive = false;
            isCentering = false;
            releaseMovementKeys(mc);
            // Release sneak key if we were pressing it
            if (wePressingSneak) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
                wePressingSneak = false;
            }
            targetBlock = null;
            targetFacing = null;
            targetHitVec = null;

            // Start restoring look if enabled
            if (restoreLook.isEnabled()) {
                isRestoringLook = true;
            }
            return;
        }

        // Not active, don't do anything
        if (!isActive) {
            return;
        }

        // Check if USER is pressing movement keys - cancel BlockIn
        if (isUserPressingMovementKeys(mc)) {
            cancelBlockIn(player, mc);
            return;
        }

        // Phase 1: Center player on block if enabled
        if (autoCenter.isEnabled() && isCentering) {
            if (isPlayerCentered(player)) {
                // Centered! Release keys and start building
                releaseMovementKeys(mc);
                isCentering = false;
            } else {
                // Move towards center
                moveTowardsCenter(player, mc);
                return; // Don't build while centering
            }
        }

        // Auto switch to blocks
        if (autoSwitch.isEnabled()) {
            int blockSlot = findBestBlockSlot(player);
            if (blockSlot != -1 && player.inventory.currentItem != blockSlot) {
                player.inventory.currentItem = blockSlot;
                activeSlot = blockSlot; // Update activeSlot so we don't cancel ourselves
            }
        }

        // Check if holding blocks
        ItemStack held = player.inventory.getCurrentItem();
        boolean holdingBlock = held != null && held.getItem() instanceof ItemBlock;
        if (!holdingBlock) {
            targetBlock = null;
            targetFacing = null;
            targetHitVec = null;
            return;
        }

        // Find best placement
        findBestPlacement(player);

        // Auto jump when placing roof and on ground
        if (autoJump.isEnabled() && isPlacingRoof && player.onGround) {
            player.jump();
        }

        // Rotate towards target
        if (targetBlock != null && targetFacing != null && targetHitVec != null) {
            rotateTowardsTarget(player);

            // Check if within tolerance and place
            if (isWithinRotationTolerance(player)) {
                tryPlace(player);
            }
        }
    }

    /**
     * Smoothly restore look to original position
     * Returns true when restoration is complete
     */
    private boolean restoreLookToOriginal(EntityPlayerSP player) {
        float yawDiff = MathHelper.wrapAngleTo180_float(originalYaw - player.rotationYaw);
        float pitchDiff = originalPitch - player.rotationPitch;

        // Check if close enough (within 2 degrees)
        if (Math.abs(yawDiff) < 2.0f && Math.abs(pitchDiff) < 2.0f) {
            // Snap to exact position
            player.rotationYaw = originalYaw;
            player.rotationPitch = originalPitch;
            return true;
        }

        // Smooth rotation with same speed as building
        float baseSpeed = (float) rotationSpeed.getValue();
        float speedVariation = baseSpeed * 0.3f;
        float maxTurn = baseSpeed + (random.nextFloat() * speedVariation * 2 - speedVariation);

        float yawStep = MathHelper.clamp_float(yawDiff, -maxTurn, maxTurn);
        float pitchStep = MathHelper.clamp_float(pitchDiff, -maxTurn, maxTurn);

        // Add micro-jitter for human-like movement
        if (Math.abs(yawDiff) > 5.0f) {
            yawStep += (random.nextFloat() - 0.5f) * 1.0f;
        }
        if (Math.abs(pitchDiff) > 5.0f) {
            pitchStep += (random.nextFloat() - 0.5f) * 0.5f;
        }

        player.rotationYaw += yawStep;
        player.rotationPitch = MathHelper.clamp_float(player.rotationPitch + pitchStep, -90.0f, 90.0f);

        return false;
    }

    /**
     * Rotate player camera towards target (legit - visible rotation with randomization)
     */
    private void rotateTowardsTarget(EntityPlayerSP player) {
        Vec3 eyes = player.getPositionEyes(1.0f);
        double dx = targetHitVec.xCoord - eyes.xCoord;
        double dy = targetHitVec.yCoord - eyes.yCoord;
        double dz = targetHitVec.zCoord - eyes.zCoord;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        targetYaw = MathHelper.wrapAngleTo180_float(targetYaw);

        float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - player.rotationYaw);
        float pitchDiff = targetPitch - player.rotationPitch;

        // Add randomization to rotation speed (±30% variation)
        float baseSpeed = (float) rotationSpeed.getValue();
        float speedVariation = baseSpeed * 0.3f;
        float maxTurn = baseSpeed + (random.nextFloat() * speedVariation * 2 - speedVariation);

        float yawStep = MathHelper.clamp_float(yawDiff, -maxTurn, maxTurn);
        float pitchStep = MathHelper.clamp_float(pitchDiff, -maxTurn, maxTurn);

        // Add micro-jitter for more human-like movement (±0.5 degrees)
        float jitterYaw = (random.nextFloat() - 0.5f) * 1.0f;
        float jitterPitch = (random.nextFloat() - 0.5f) * 0.5f;

        // Only apply jitter if we're still rotating (not at target yet)
        if (Math.abs(yawDiff) > 2.0f) {
            yawStep += jitterYaw;
        }
        if (Math.abs(pitchDiff) > 2.0f) {
            pitchStep += jitterPitch;
        }

        // Apply rotation to player (legit - visible)
        player.rotationYaw += yawStep;
        player.rotationPitch = MathHelper.clamp_float(player.rotationPitch + pitchStep, -90.0f, 90.0f);

        aimYaw = player.rotationYaw;
        aimPitch = player.rotationPitch;
    }

    /**
     * Check if player is looking close enough to target
     */
    private boolean isWithinRotationTolerance(EntityPlayerSP player) {
        if (targetHitVec == null) return false;

        Vec3 eyes = player.getPositionEyes(1.0f);
        double dx = targetHitVec.xCoord - eyes.xCoord;
        double dy = targetHitVec.yCoord - eyes.yCoord;
        double dz = targetHitVec.zCoord - eyes.zCoord;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        float dyaw = Math.abs(MathHelper.wrapAngleTo180_float(targetYaw - player.rotationYaw));
        float dpitch = Math.abs(targetPitch - player.rotationPitch);

        return dyaw <= rotationTolerance.getValue() && dpitch <= rotationTolerance.getValue();
    }

    /**
     * Try to place block using real right-click (KeyBinding)
     */
    private void tryPlace(EntityPlayerSP player) {
        Minecraft mc = Minecraft.getMinecraft();

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlaceTime < placeDelay.getValue()) return;

        // Raycast to verify we're looking at the right block
        MovingObjectPosition mop = rayTraceBlock(player.rotationYaw, player.rotationPitch, range.getValue());

        if (mop != null
            && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
            && mop.getBlockPos().equals(targetBlock)
            && mop.sideHit == targetFacing) {

            ItemStack heldStack = player.inventory.getCurrentItem();
            if (heldStack != null && heldStack.getItem() instanceof ItemBlock) {
                // Use real right-click via KeyBinding (like AutoClicker does for attacks)
                int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();

                // Simulate key press
                KeyBinding.setKeyBindState(useKey, true);
                KeyBinding.onTick(useKey);

                // Release key immediately (next tick will process the click)
                KeyBinding.setKeyBindState(useKey, false);

                lastPlaceTime = currentTime;

                // Clear target
                targetBlock = null;
                targetFacing = null;
                targetHitVec = null;
            }
        }
    }

    /**
     * Find best block slot in hotbar
     */
    private int findBestBlockSlot(EntityPlayerSP player) {
        int bestSlot = -1;
        int bestScore = Integer.MAX_VALUE;

        for (int slot = 0; slot <= 8; slot++) {
            ItemStack stack = player.inventory.getStackInSlot(slot);
            if (stack == null || stack.stackSize == 0) continue;

            if (stack.getItem() instanceof ItemBlock) {
                Block block = ((ItemBlock) stack.getItem()).getBlock();
                String blockName = block.getUnlocalizedName().replace("tile.", "");

                Integer score = BLOCK_SCORE.get(blockName);
                if (score != null && score < bestScore) {
                    bestScore = score;
                    bestSlot = slot;
                    if (score == 0) break; // Obsidian is best
                }
            }
        }

        return bestSlot;
    }

    /**
     * Find best placement position
     */
    private void findBestPlacement(EntityPlayerSP player) {
        Minecraft mc = Minecraft.getMinecraft();
        Vec3 playerPos = player.getPositionVector();
        BlockPos feetPos = new BlockPos(playerPos.xCoord, playerPos.yCoord, playerPos.zCoord);

        Vec3 eye = player.getPositionEyes(1.0f);
        double reach = range.getValue();

        // Reset roof state
        isPlacingRoof = false;

        // Priority: Roof first if enabled, then sides
        BlockPos roofTarget = feetPos.up(2);

        if (prioritizeRoof.isEnabled() && isAir(roofTarget, mc)) {
            // Try to place roof
            if (tryFindPlacementForTarget(roofTarget, eye, reach, mc)) {
                isPlacingRoof = true;
                return;
            }
            // Roof needs to be placed but couldn't find placement - mark for jump
            isPlacingRoof = true;
        }

        // Try sides at head level
        for (int[] d : DIRS) {
            BlockPos headPos = feetPos.add(d[0], 1, d[2]);
            if (isAir(headPos, mc)) {
                if (tryFindPlacementForTarget(headPos, eye, reach, mc)) {
                    return;
                }
            }
        }

        // Try sides at feet level
        for (int[] d : DIRS) {
            BlockPos feetGoal = feetPos.add(d[0], 0, d[2]);
            if (isAir(feetGoal, mc)) {
                if (tryFindPlacementForTarget(feetGoal, eye, reach, mc)) {
                    return;
                }
            }
        }

        // Try roof if not prioritized
        if (!prioritizeRoof.isEnabled() && isAir(roofTarget, mc)) {
            if (tryFindPlacementForTarget(roofTarget, eye, reach, mc)) {
                isPlacingRoof = true;
            }
        }
    }

    /**
     * Try to find a valid placement for a target position
     */
    private boolean tryFindPlacementForTarget(BlockPos target, Vec3 eye, double reach, Minecraft mc) {
        // Check all 6 directions for a support block
        for (EnumFacing facing : EnumFacing.values()) {
            BlockPos support = target.offset(facing);

            if (isAir(support, mc)) continue;

            // Check if support is in range
            Vec3 center = new Vec3(support.getX() + 0.5, support.getY() + 0.5, support.getZ() + 0.5);
            if (eye.distanceTo(center) > reach) continue;

            // Find a hit point on the support block that places at target
            EnumFacing placeFacing = facing.getOpposite();

            Vec3 hitPos = getHitPosOnFace(support, placeFacing);
            float[] rot = getRotations(eye, hitPos);

            MovingObjectPosition mop = rayTraceBlock(rot[0], rot[1], reach);
            if (mop != null
                && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && mop.getBlockPos().equals(support)
                && mop.sideHit == placeFacing) {

                targetBlock = support;
                targetFacing = placeFacing;
                targetHitVec = mop.hitVec;
                aimYaw = rot[0];
                aimPitch = rot[1];
                return true;
            }
        }

        return false;
    }

    /**
     * Get a hit position on a block face
     */
    private Vec3 getHitPosOnFace(BlockPos block, EnumFacing face) {
        double x = block.getX() + 0.5;
        double y = block.getY() + 0.5;
        double z = block.getZ() + 0.5;

        switch (face) {
            case DOWN:
                y = block.getY() + 0.1;
                break;
            case UP:
                y = block.getY() + 0.9;
                break;
            case NORTH:
                z = block.getZ() + 0.1;
                break;
            case SOUTH:
                z = block.getZ() + 0.9;
                break;
            case WEST:
                x = block.getX() + 0.1;
                break;
            case EAST:
                x = block.getX() + 0.9;
                break;
        }

        return new Vec3(x, y, z);
    }

    private boolean isAir(BlockPos pos, Minecraft mc) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return block == Blocks.air
            || block == Blocks.water
            || block == Blocks.flowing_water
            || block == Blocks.lava
            || block == Blocks.flowing_lava
            || block == Blocks.fire;
    }

    private void updateProgress(EntityPlayerSP player) {
        Minecraft mc = Minecraft.getMinecraft();
        Vec3 playerPos = player.getPositionVector();
        BlockPos feetPos = new BlockPos(playerPos.xCoord, playerPos.yCoord, playerPos.zCoord);

        int filled = 0;
        int total = 9; // 1 roof + 4 sides at feet + 4 sides at head

        // Check roof
        if (!isAir(feetPos.up(2), mc)) {
            filled++;
        }

        // Check sides
        for (int[] d : DIRS) {
            if (!isAir(feetPos.add(d[0], 0, d[2]), mc)) {
                filled++;
            }
            if (!isAir(feetPos.add(d[0], 1, d[2]), mc)) {
                filled++;
            }
        }

        progress = (float) filled / (float) total;
    }

    private MovingObjectPosition rayTraceBlock(float yaw, float pitch, double range) {
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

    private float[] getRotations(Vec3 eye, Vec3 target) {
        double dx = target.xCoord - eye.xCoord;
        double dy = target.yCoord - eye.yCoord;
        double dz = target.zCoord - eye.zCoord;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        yaw = MathHelper.wrapAngleTo180_float(yaw);

        return new float[]{yaw, pitch};
    }

    @Override
    public String getDisplaySuffix() {
        if (isRestoringLook && showProgress.isEnabled()) {
            return " §e↩";
        } else if (isActive && showProgress.isEnabled()) {
            return " §a" + (int) (progress * 100) + "%";
        } else if (showProgress.isEnabled()) {
            return " §7" + (int) (progress * 100) + "%";
        }
        return null;
    }
}
