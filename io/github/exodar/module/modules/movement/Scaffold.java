/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.movement;

import io.github.exodar.event.Listener;
import io.github.exodar.event.auxiliary.MoveInputEventAux;
import io.github.exodar.event.auxiliary.PreUpdateEventAux;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import io.github.exodar.util.auxiliary.BlockUtilsAux;
import io.github.exodar.util.auxiliary.PlayerUtilAux;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;

/**
 * Scaffold - Auto sneak at block edges for bridging
 * Modes:
 * - Blatant: Predictive edge detection
 * - Legit: Simple "over air" check
 */
public class Scaffold extends Module {

    // Mode
    private final ModeSetting mode = new ModeSetting("Mode", new String[]{"Blatant", "Legit"});

    // ==================== BLATANT MODE SETTINGS ====================
    private final SliderSetting edgeOffset = new SliderSetting("Edge Offset", 0.0, 0.0, 0.3, 0.01);
    // Prediction: displayed as 0-100 (percentage), internally mapped to 1.80-2.50
    private final SliderSetting prediction = new SliderSetting("Prediction", 50.0, 0.0, 100.0, 1.0);
    private final SliderSetting jumpDelay = new SliderSetting("Jump Delay", 50.0, 5.0, 300.0, 5.0);
    private final SliderSetting sneakDelay = new SliderSetting("Sneak Delay", 25.0, 5.0, 300.0, 5.0);
    private final TickSetting adaptive = new TickSetting("Adaptive", false);

    // ==================== LEGIT MODE SETTINGS ====================
    private final TickSetting shiftDuringJumps = new TickSetting("Shift During Jumps", false);
    private final SliderSetting shiftTime = new SliderSetting("Shift Time", 150.0, 50.0, 300.0, 5.0);

    // ==================== SHARED SETTINGS ====================
    private final TickSetting requireSneak = new TickSetting("Require Sneak", false);
    private final TickSetting requireBlocks = new TickSetting("Require Blocks", true);
    private final TickSetting notMoveForward = new TickSetting("Not Move Forward", true);
    private final TickSetting onlyLookdown = new TickSetting("Look Down Only", true);

    // Blatant mode state
    private final double[][] RADIUS = {{-0.3, -0.3}, {0.3, -0.3}, {-0.3, 0.3}, {0.3, 0.3}};
    private boolean sneaking;
    private int jumpDelayTicks;
    private int jumpStartTick = -1;
    private int delayTicks;
    private int startTick = -1;
    private boolean wasActive;
    private double lastMotion = 0.0;
    private boolean hadBlocksWhenStarted = false;
    private double cachedSpeedMultiplier = 1.0;
    private int lastPotionCheckTick = 0;
    private boolean outOfBlocksProtection = false; // Protection: keep sneaking when out of blocks

    // Legit mode state
    private boolean shouldBridge = false;
    private boolean isShiftingLegit = false;
    private long shiftTimer = 0;
    private long shiftDuration = 0;
    private boolean outOfBlocksProtectionLegit = false;

    public Scaffold() {
        super("Scaffold", ModuleCategory.MOVEMENT);
        this.registerSetting(mode);

        // Blatant settings
        this.registerSetting(edgeOffset);
        this.registerSetting(prediction);
        this.registerSetting(jumpDelay);
        this.registerSetting(sneakDelay);
        this.registerSetting(adaptive);

        // Legit settings
        this.registerSetting(shiftDuringJumps);
        this.registerSetting(shiftTime);

        // Shared settings
        this.registerSetting(requireSneak);
        this.registerSetting(requireBlocks);
        this.registerSetting(notMoveForward);
        this.registerSetting(onlyLookdown);

        updateSettingsVisibility();
    }

    @Override
    public void onEnable() {
        updateSettingsVisibility();
    }

    @Override
    public void onDisable() {
        if (mc != null && mc.gameSettings != null) {
            int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();
            KeyBinding.setKeyBindState(sneakKey, Keyboard.isKeyDown(sneakKey));
        }
        resetState();
    }

    @Override
    public void onUpdate() {
        updateSettingsVisibility();
    }

    private void updateSettingsVisibility() {
        boolean isBlatant = mode.is("Blatant");
        boolean isLegit = mode.is("Legit");

        // Blatant-only settings
        edgeOffset.setVisible(isBlatant);
        prediction.setVisible(isBlatant);
        jumpDelay.setVisible(isBlatant);
        sneakDelay.setVisible(isBlatant);
        adaptive.setVisible(isBlatant);

        // Legit-only settings
        shiftDuringJumps.setVisible(isLegit);
        shiftTime.setVisible(isLegit);
        requireSneak.setVisible(isLegit); // Only show Require Sneak in Legit mode
    }

    private void resetState() {
        // Blatant state
        this.sneaking = false;
        this.wasActive = false;
        this.jumpStartTick = this.startTick = -1;
        this.cachedSpeedMultiplier = 1.0;
        this.lastPotionCheckTick = 0;
        this.hadBlocksWhenStarted = false;
        this.lastMotion = 0.0;
        this.outOfBlocksProtection = false;

        // Legit state
        this.shouldBridge = false;
        this.isShiftingLegit = false;
        this.shiftTimer = 0;
        this.shiftDuration = 0;
        this.outOfBlocksProtectionLegit = false;
    }

    /**
     * Get the actual prediction factor (1.80 to 2.50)
     * Slider shows 0-100, internally mapped to 1.80-2.50
     */
    private double getPredictionFactor() {
        return 1.80 + (prediction.getValue() / 100.0) * 0.70;
    }

    /**
     * MoveInputEvent handler - controls the sneak INPUT state
     */
    @Listener
    public void onInput(MoveInputEventAux event) {
        if (!conditionalsBase()) {
            return;
        }

        if (mode.is("Blatant")) {
            // Blatant: module takes full control when conditions met
            if (conditionalsBlatant()) {
                event.setSneak(this.sneaking);
            }
        } else if (mode.is("Legit")) {
            // Legit: handled in onPreUpdate via KeyBinding
        }
    }

    /**
     * PreUpdate handler - main logic
     */
    @Listener
    public void onPreUpdate(PreUpdateEventAux event) {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        if (mode.is("Blatant")) {
            handleBlatantMode(player);
        } else if (mode.is("Legit")) {
            handleLegitMode(player);
        }
    }

    // ==================== BLATANT MODE ====================

    private void handleBlatantMode(EntityPlayerSP player) {
        if (!conditionalsBase() || !conditionalsBlatant()) {
            if (this.wasActive) {
                resetBlatant();
            }
            return;
        }
        this.wasActive = true;

        // In protection mode (out of blocks), just keep sneaking
        if (this.outOfBlocksProtection) {
            pressSneak();
            return;
        }

        if (PlayerUtilAux.isBlock()) {
            this.hadBlocksWhenStarted = true;
        }

        int ticks = player.ticksExisted;

        double motionXSq = player.motionX * player.motionX;
        double motionZSq = player.motionZ * player.motionZ;
        double currentMotionSquared = motionXSq + motionZSq;

        boolean isMovingFast = currentMotionSquared > 0.01;

        int earlySneak = 20;
        if (ticks - this.lastPotionCheckTick > earlySneak) {
            this.cachedSpeedMultiplier = getSpeedEffectMultiplier(player);
            this.lastPotionCheckTick = ticks;
        }

        double adaptiveEdgeOffset = this.edgeOffset.getValue();
        double adaptivePredictionFactor = getPredictionFactor();
        int adaptiveJumpDelay = (int) this.jumpDelay.getValue();
        int adaptiveSneakDelay = (int) this.sneakDelay.getValue();

        if (this.adaptive.isEnabled() && isMovingFast) {
            double currentMotion = Math.sqrt(currentMotionSquared);
            double[] adaptiveValues = calculateAdaptiveValues(currentMotion, this.cachedSpeedMultiplier);
            adaptiveEdgeOffset = adaptiveValues[0];
            adaptivePredictionFactor = adaptiveValues[1];
            adaptiveJumpDelay = (int) adaptiveValues[2];
            adaptiveSneakDelay = (int) adaptiveValues[3];
        }

        // Jump detection
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()) && !player.onGround &&
            (player.moveForward != 0.0F || player.moveStrafing != 0.0F) && adaptiveSneakDelay > 0) {
            this.jumpStartTick = ticks;
            this.jumpDelayTicks = calculateChance(adaptiveJumpDelay);
            pressSneak();
            return;
        }

        double acceleration = 0.0;
        if (isMovingFast) {
            double currentMotion = Math.sqrt(currentMotionSquared);
            acceleration = currentMotion - this.lastMotion;
            this.lastMotion = currentMotion;
        } else {
            this.lastMotion = 0.0;
        }

        double nextX = player.motionX;
        double nextZ = player.motionZ;

        if (player.isSneaking()) {
            double sneakDivisor = 0.3;
            nextX *= 1.0 / sneakDivisor;
            nextZ *= 1.0 / sneakDivisor;
        }

        double baseFactor = adaptivePredictionFactor;
        double dynamicFactor = baseFactor + acceleration * 2.5;
        dynamicFactor = Math.max(1.0, dynamicFactor);

        Vec3 predicted = new Vec3(
            player.posX + nextX * dynamicFactor,
            player.posY,
            player.posZ + nextZ * dynamicFactor
        );

        double dist = computeEdge(predicted, player.posX, player.posY, player.posZ);

        if (Double.isNaN(dist)) {
            if (this.sneaking) tryReleaseSneak();
            return;
        }

        if (dist > adaptiveEdgeOffset) {
            pressSneak();
        } else if (this.sneaking) {
            tryReleaseSneak();
        }
    }

    private boolean conditionalsBlatant() {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || player.capabilities.isFlying) return false;

        // Check if pressing W (forward) - exit protection mode and stop
        if (player.moveForward > 0) {
            this.outOfBlocksProtection = false;
            return false;
        }

        // Require Blocks with protection
        if (this.requireBlocks.isEnabled()) {
            boolean hasBlocks = PlayerUtilAux.isBlock();
            if (!hasBlocks) {
                // If we were sneaking/bridging and ran out of blocks, enter protection mode
                if (this.sneaking || this.hadBlocksWhenStarted) {
                    this.outOfBlocksProtection = true;
                }
                // In protection mode, keep returning true to maintain sneak
                if (this.outOfBlocksProtection) {
                    return true;
                }
                return false;
            } else {
                // Got blocks back, exit protection mode
                this.outOfBlocksProtection = false;
            }
        }

        if (this.onlyLookdown.isEnabled() && player.rotationPitch < 70.0F) return false;
        // Not Move Forward: only works when NOT pressing W (moveForward <= 0)
        if (this.notMoveForward.isEnabled() && player.moveForward > 0) return false;

        // Require Sneak is NOT used in Blatant mode (only Legit)
        return true;
    }

    private void resetBlatant() {
        this.sneaking = false;
        this.wasActive = false;
        this.jumpStartTick = this.startTick = -1;
        this.cachedSpeedMultiplier = 1.0;
        this.lastPotionCheckTick = 0;
        this.hadBlocksWhenStarted = false;
        this.outOfBlocksProtection = false;

        if (mc != null && mc.gameSettings != null) {
            int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();
            KeyBinding.setKeyBindState(sneakKey, Keyboard.isKeyDown(sneakKey));
        }
    }

    // ==================== LEGIT MODE ====================
    // Legit mode uses HARDCODED pitch value
    private static final float LEGIT_PITCH_MIN = 70.0F;

    /**
     * Simple check if player is standing over air (for Legit mode)
     */
    private boolean playerOverAir() {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return false;
        int x = (int) Math.floor(player.posX);
        int y = (int) Math.floor(player.posY - 1);
        int z = (int) Math.floor(player.posZ);
        return isAirBlock(x, y, z);
    }

    private void handleLegitMode(EntityPlayerSP player) {
        if (!conditionalsBase()) {
            if (isShiftingLegit) {
                setShiftLegit(false);
                isShiftingLegit = false;
            }
            shouldBridge = false;
            outOfBlocksProtectionLegit = false;
            return;
        }

        boolean shiftTimeActive = shiftTime.getValue() > 0;

        // If pressing W (forward), exit protection mode and stop
        if (player.movementInput.moveForward > 0) {
            if (isShiftingLegit) {
                isShiftingLegit = false;
                setShiftLegit(false);
                shouldBridge = false;
            }
            outOfBlocksProtectionLegit = false;
            return;
        }

        // Check pitch (Legit uses hardcoded 70° minimum like Blatant)
        if (onlyLookdown.isEnabled()) {
            if (player.rotationPitch < LEGIT_PITCH_MIN) {
                shouldBridge = false;
                if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
                    setShiftLegit(true);
                }
                return;
            }
        }

        // Require Sneak check
        if (requireSneak.isEnabled()) {
            if (!Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
                shouldBridge = false;
                return;
            }
        }

        // Require Blocks with protection
        if (requireBlocks.isEnabled()) {
            boolean hasBlocks = PlayerUtilAux.isBlock();
            if (!hasBlocks) {
                // If we were bridging and ran out of blocks, enter protection mode
                if (isShiftingLegit || shouldBridge) {
                    outOfBlocksProtectionLegit = true;
                }
                // In protection mode, keep sneaking
                if (outOfBlocksProtectionLegit) {
                    isShiftingLegit = true;
                    setShiftLegit(true);
                    return;
                }
                return;
            } else {
                // Got blocks back, exit protection mode
                outOfBlocksProtectionLegit = false;
            }
        }

        // Not Move Forward: only works when NOT pressing W (moveForward <= 0)
        if (notMoveForward.isEnabled() && player.movementInput.moveForward > 0) {
            shouldBridge = false;
            return;
        }

        // Simple "over air" check for Legit mode
        boolean isOverAir = playerOverAir();

        if (player.onGround) {
            if (isOverAir) {
                // Start shift timer
                if (shiftTimeActive) {
                    shiftDuration = (long) shiftTime.getValue();
                    shiftTimer = System.currentTimeMillis();
                }

                isShiftingLegit = true;
                setShiftLegit(true);
                shouldBridge = true;
            } else if (!isOverAir && isShiftingLegit) {
                // No longer over air, check if we should release
                if (!shiftTimeActive || (System.currentTimeMillis() - shiftTimer > shiftDuration)) {
                    isShiftingLegit = false;
                    setShiftLegit(false);
                    shouldBridge = true;
                }
            }
        } else if (shouldBridge && player.capabilities.isFlying) {
            setShiftLegit(false);
            shouldBridge = false;
        } else if (shouldBridge && isOverAir && shiftDuringJumps.isEnabled()) {
            isShiftingLegit = true;
            setShiftLegit(true);
        } else if (!player.onGround && !shiftDuringJumps.isEnabled()) {
            isShiftingLegit = false;
            setShiftLegit(false);
        }
    }

    private void setShiftLegit(boolean shift) {
        int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();
        KeyBinding.setKeyBindState(sneakKey, shift);
    }

    // ==================== SHARED METHODS ====================

    private boolean conditionalsBase() {
        if (mc.currentScreen != null) return false;
        EntityPlayerSP player = mc.thePlayer;
        return player != null && !player.capabilities.isFlying;
    }

    private double getSpeedEffectMultiplier(EntityPlayerSP player) {
        if (player.isPotionActive(Potion.moveSpeed)) {
            int amplifier = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
            return 1.0 + (amplifier + 1) * 0.2;
        }
        return 1.0;
    }

    private double[] calculateAdaptiveValues(double currentMotion, double speedMultiplier) {
        double baseOffset = this.edgeOffset.getValue();
        double basePrediction = getPredictionFactor();
        double baseJumpDelay = this.jumpDelay.getValue();
        double baseSneakDelay = this.sneakDelay.getValue();

        double speedFactor = Math.min(2.0, currentMotion * 10.0);
        double speedEffectFactor = Math.max(1.0, speedMultiplier);
        double combinedFactor = Math.min(3.0, speedFactor * speedEffectFactor);

        double offsetReduction = 1.0 - combinedFactor * 0.15;
        double adaptiveOffset = Math.max(baseOffset * 0.4, baseOffset * offsetReduction);

        double predictionIncrease = 1.0 + combinedFactor * 0.1;
        double adaptivePrediction = Math.min(2.5, basePrediction * predictionIncrease);

        double jumpDelayIncrease = 1.0 + combinedFactor * 0.3;
        double adaptiveJumpDelay = Math.min(300.0, baseJumpDelay * jumpDelayIncrease);

        double sneakDelayIncrease = 1.0 + combinedFactor * 0.2;
        double adaptiveSneakDelay = Math.min(300.0, baseSneakDelay * sneakDelayIncrease);

        return new double[]{adaptiveOffset, adaptivePrediction, adaptiveJumpDelay, adaptiveSneakDelay};
    }

    private void pressSneak() {
        this.sneaking = true;
        this.startTick = -1;

        int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();
        KeyBinding.setKeyBindState(sneakKey, true);
    }

    private void tryReleaseSneak() {
        int ticks = mc.thePlayer.ticksExisted;

        if (this.startTick == -1 && this.jumpStartTick == -1) {
            this.startTick = ticks;
            this.delayTicks = calculateChance((int) this.sneakDelay.getValue());
        }

        if (isWaiting(ticks, this.jumpStartTick, this.jumpDelayTicks) ||
            isWaiting(ticks, this.startTick, this.delayTicks)) {
            return;
        }

        releaseSneak();
    }

    private int calculateChance(Number value) {
        double raw = (value.doubleValue() - 50.0) / 50.0;
        return (int) raw + ((Math.random() < raw - (int) raw) ? 1 : 0);
    }

    private boolean isWaiting(int current, int start, int delay) {
        return start != -1 && current - start < delay;
    }

    private void releaseSneak() {
        this.sneaking = false;
        this.startTick = this.jumpStartTick = -1;

        int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();
        KeyBinding.setKeyBindState(sneakKey, Keyboard.isKeyDown(sneakKey));
    }

    private double computeEdge(Vec3 predicted, double currentX, double currentY, double currentZ) {
        int floorY = (int) (currentY - 0.01);
        double bestDist = Double.MAX_VALUE;
        boolean found = false;

        double predX = predicted.xCoord;
        double predZ = predicted.zCoord;

        for (int i = 0; i < 4; i++) {
            double[] c = this.RADIUS[i];
            int bx = (int) Math.floor(currentX + c[0]);
            int bz = (int) Math.floor(currentZ + c[1]);

            if (!isAirBlock(bx, floorY, bz)) {
                double blockEdgeX = bx + ((predX < bx + 0.5) ? 0 : 1);
                double blockEdgeZ = bz + ((predZ < bz + 0.5) ? 0 : 1);

                double offX = predX - blockEdgeX;
                double offZ = predZ - blockEdgeZ;

                offX = (offX < 0.0) ? -offX : offX;
                offZ = (offZ < 0.0) ? -offZ : offZ;

                int predBlockX = (int) Math.floor(predX);
                int predBlockZ = (int) Math.floor(predZ);

                boolean xDiff = predBlockX != bx;
                boolean zDiff = predBlockZ != bz;

                double cornerDist;
                if (xDiff && zDiff) {
                    cornerDist = Math.max(offX, offZ);
                } else if (xDiff) {
                    cornerDist = offX;
                } else if (zDiff) {
                    cornerDist = offZ;
                } else {
                    cornerDist = 0.0;
                }

                if (cornerDist < bestDist) {
                    bestDist = cornerDist;
                    found = true;
                }
            }
        }

        return found ? bestDist : Double.NaN;
    }

    private boolean isAirBlock(int x, int y, int z) {
        try {
            return BlockUtilsAux.getBlockAt(x, y, z).name.contains("air");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getDisplaySuffix() {
        return " \u00a77" + mode.getValue();
    }
}
