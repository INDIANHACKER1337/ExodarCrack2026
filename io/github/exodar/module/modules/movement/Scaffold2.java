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
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.text.DecimalFormat;

/**
 * Scaffold2 - Sakura BridgeAssist clone
 * Assists with bridging using prediction-based edge detection
 */
public class Scaffold2 extends Module {

    // Main settings
    private final SliderSetting edgeOffset;
    private final SliderSetting predictionFactor;
    private final SliderSetting jumpDelay;
    private final SliderSetting sneakDelay;
    private final TickSetting adaptive;

    // Conditions
    private final TickSetting holdSneak;
    private final TickSetting holdBlocks;
    private final TickSetting onlyBackwards;
    private final TickSetting onlyRightClick;
    private final TickSetting onlyLookdown;

    // Corner radius for edge detection
    private final double[][] RADIUS = {
        {-0.3, -0.3},
        {0.3, -0.3},
        {-0.3, 0.3},
        {0.3, 0.3}
    };

    // State
    private boolean sneaking = false;
    private int jumpDelayTicks = 0;
    private int jumpStartTick = -1;
    private int delayTicks = 0;
    private int startTick = -1;
    private boolean active = false;
    private double lastMotion = 0.0;
    private double cachedSpeedMultiplier = 1.0;
    private int lastPotionCheckTick = 0;

    public Scaffold2() {
        super("Scaffold2", ModuleCategory.MOVEMENT);

        this.registerSetting(new DescriptionSetting("Sakura BridgeAssist clone"));

        // Main settings
        this.registerSetting(edgeOffset = new SliderSetting("Edge Offset", 0.0, 0.0, 0.3, 0.01));
        this.registerSetting(predictionFactor = new SliderSetting("Prediction Factor", 0.1, 0.1, 2.5, 0.01));
        this.registerSetting(jumpDelay = new SliderSetting("Jump Delay (ms)", 50.0, 5.0, 300.0, 5.0));
        this.registerSetting(sneakDelay = new SliderSetting("Sneak Delay (ms)", 25.0, 5.0, 300.0, 5.0));
        this.registerSetting(adaptive = new TickSetting("Adaptive", false));

        // Conditions
        this.registerSetting(new DescriptionSetting("--- Conditions ---"));
        this.registerSetting(holdSneak = new TickSetting("Require Sneak", false));
        this.registerSetting(holdBlocks = new TickSetting("Require Blocks", true));
        this.registerSetting(onlyBackwards = new TickSetting("Backwards Only", true));
        this.registerSetting(onlyRightClick = new TickSetting("Right Click Only", true));
        this.registerSetting(onlyLookdown = new TickSetting("Look Down Only", true));
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        if (sneaking) {
            releaseSneak(true);
        }
        reset();
    }

    @Override
    public void onUpdate() {
        if (!isInGame()) return;
        if (mc.thePlayer == null) return;

        if (!conditionals()) {
            if (active) {
                reset();
            }
            return;
        }

        active = true;
        int ticks = mc.thePlayer.ticksExisted;

        double motionXSq = mc.thePlayer.motionX * mc.thePlayer.motionX;
        double motionZSq = mc.thePlayer.motionZ * mc.thePlayer.motionZ;
        double currentMotionSquared = motionXSq + motionZSq;
        boolean isMovingFast = currentMotionSquared > 0.01;

        // Update speed multiplier cache every 20 ticks
        if (ticks - lastPotionCheckTick > 20) {
            cachedSpeedMultiplier = getSpeedEffect();
            lastPotionCheckTick = ticks;
        }

        // Calculate adaptive values
        double adaptiveEdgeOffset = edgeOffset.getValue();
        double adaptivePredictionFactor = predictionFactor.getValue();
        int adaptiveJumpDelay = (int) jumpDelay.getValue();
        int adaptiveSneakDelay = (int) sneakDelay.getValue();

        if (adaptive.isEnabled() && isMovingFast) {
            double currentMotion = Math.sqrt(currentMotionSquared);
            double[] adaptiveValues = calculateAdaptive(currentMotion, cachedSpeedMultiplier);
            adaptiveEdgeOffset = adaptiveValues[0];
            adaptivePredictionFactor = adaptiveValues[1];
            adaptiveJumpDelay = (int) adaptiveValues[2];
            adaptiveSneakDelay = (int) adaptiveValues[3];
        }

        // Handle jump sneak
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()) &&
            !mc.thePlayer.onGround &&
            (mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F) &&
            adaptiveSneakDelay > 0) {

            jumpStartTick = ticks;
            jumpDelayTicks = calculateChance(adaptiveJumpDelay);
            pressSneak(true);
            return;
        }

        // Calculate acceleration
        double acceleration = 0.0;
        if (isMovingFast) {
            double currentMotion = Math.sqrt(currentMotionSquared);
            acceleration = currentMotion - lastMotion;
            lastMotion = currentMotion;
        } else {
            lastMotion = 0.0;
        }

        // Calculate next position
        double nextX = mc.thePlayer.motionX;
        double nextZ = mc.thePlayer.motionZ;

        if (mc.thePlayer.isSneaking()) {
            double sneakDivisor = 0.3;
            nextX *= 1.0 / sneakDivisor;
            nextZ *= 1.0 / sneakDivisor;
        }

        // Dynamic prediction factor
        double baseFactor = adaptivePredictionFactor;
        double dynamicFactor = baseFactor + acceleration * 2.5;
        dynamicFactor = Math.max(1.0, dynamicFactor);

        double posX = mc.thePlayer.posX;
        double posY = mc.thePlayer.posY;
        double posZ = mc.thePlayer.posZ;

        Vec3 predictedPos = new Vec3(
            posX + nextX * dynamicFactor,
            posY,
            posZ + nextZ * dynamicFactor
        );

        // Compute edge distance
        double dist = computeEdge(predictedPos, posX, posY, posZ);

        if (Double.isNaN(dist)) {
            if (sneaking) {
                tryReleaseSneak(true);
            }
            return;
        }

        if (dist > adaptiveEdgeOffset) {
            pressSneak(true);
        } else if (sneaking) {
            tryReleaseSneak(true);
        }
    }

    private double getSpeedEffect() {
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            int amplifier = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
            return 1.0 + (amplifier + 1) * 0.2;
        }
        return 1.0;
    }

    private double[] calculateAdaptive(double currentMotion, double speedMultiplier) {
        double baseOffset = edgeOffset.getValue();
        double basePrediction = predictionFactor.getValue();
        double baseJumpDelay = jumpDelay.getValue();
        double baseSneakDelay = sneakDelay.getValue();

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

    private boolean conditionals() {
        if (mc.currentScreen != null) return false;
        if (mc.thePlayer == null || mc.thePlayer.capabilities.isFlying) return false;

        if (onlyRightClick.isEnabled() && !Mouse.isButtonDown(1)) return false;
        if (holdBlocks.isEnabled() && !isHoldingBlock()) return false;
        if (onlyLookdown.isEnabled() && mc.thePlayer.rotationPitch < 70.0F) return false;
        if (onlyBackwards.isEnabled() && mc.thePlayer.moveForward > -0.2) return false;
        if (holdSneak.isEnabled() && !Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) return false;

        return true;
    }

    private boolean isHoldingBlock() {
        ItemStack held = mc.thePlayer.getHeldItem();
        return held != null && held.getItem() instanceof ItemBlock;
    }

    private void pressSneak(boolean resetUnsneak) {
        sneak(true);
        sneaking = true;
        if (resetUnsneak) startTick = -1;
    }

    private void tryReleaseSneak(boolean resetDelay) {
        int ticks = mc.thePlayer.ticksExisted;

        if (startTick == -1 && jumpStartTick == -1) {
            startTick = ticks;
            delayTicks = calculateChance((int) sneakDelay.getValue());
        }

        if (isWaiting(ticks, jumpStartTick, jumpDelayTicks) ||
            isWaiting(ticks, startTick, delayTicks)) {
            return;
        }

        releaseSneak(resetDelay);
    }

    private int calculateChance(int value) {
        double raw = (value - 50.0) / 50.0;
        return (int) raw + ((Math.random() < raw - (int) raw) ? 1 : 0);
    }

    private boolean isWaiting(int current, int start, int delay) {
        return start != -1 && current - start < delay;
    }

    private void releaseSneak(boolean reset) {
        sneak(false);
        sneaking = false;
        if (reset) {
            startTick = -1;
            jumpStartTick = -1;
        }
    }

    private void sneak(boolean state) {
        if (state) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
        } else {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(),
                Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()));
        }
    }

    private void reset() {
        sneaking = false;
        active = false;
        jumpStartTick = -1;
        startTick = -1;
        cachedSpeedMultiplier = 1.0;
        lastPotionCheckTick = 0;
        updateSneakState(Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()));
    }

    private void updateSneakState(boolean state) {
        if (mc.thePlayer.isSneaking() != state) {
            sneak(state);
        }
    }

    private double computeEdge(Vec3 predicted, double currentX, double currentY, double currentZ) {
        int floorY = (int) (currentY - 0.01);
        double bestDist = Double.MAX_VALUE;
        boolean found = false;

        double predX = predicted.xCoord;
        double predZ = predicted.zCoord;

        for (int i = 0; i < 4; i++) {
            double[] c = RADIUS[i];
            int bx = (int) Math.floor(currentX + c[0]);
            int bz = (int) Math.floor(currentZ + c[1]);

            if (!isAirBlock(bx, floorY, bz)) {
                double blockEdgeX = bx + ((predX < bx + 0.5) ? 0 : 1);
                double blockEdgeZ = bz + ((predZ < bz + 0.5) ? 0 : 1);

                double offX = Math.abs(predX - blockEdgeX);
                double offZ = Math.abs(predZ - blockEdgeZ);

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
            Block block = mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
            return block == Blocks.air;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + new DecimalFormat("#.##").format(edgeOffset.getValue());
    }
}
