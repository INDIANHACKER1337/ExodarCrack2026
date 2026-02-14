/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.TickSetting;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.Main;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Timer;

import java.lang.reflect.Field;
import java.util.Random;

/**
 * TimerRangeV4 - Based on FDPClient TimerRange
 *
 * Three modes:
 * - Normal: Simple timer boost when attacking in range
 * - Smart: Random tick delays before boosting
 * - Modern: Movement-based, checks if looking at entity
 *
 * Uses timer speed phases: Boost -> Charged -> Normal
 */
public class TimerRangeV4 extends Module {

    // Mode
    private final ModeSetting mode;

    // Timer settings
    private final SliderSetting ticks;
    private final SliderSetting timerBoost;
    private final SliderSetting timerCharged;
    private final SliderSetting boostPhase;    // % of ticks for boost
    private final SliderSetting chargedPhase;  // % of ticks for charged

    // Range settings
    private final SliderSetting minRange;
    private final SliderSetting maxRange;
    private final SliderSetting scanRange;

    // Delay settings
    private final SliderSetting minTickDelay;
    private final SliderSetting maxTickDelay;
    private final SliderSetting cooldownTicks;

    // Options
    private final SliderSetting maxAngle;
    private final TickSetting onlyOnGround;
    private final TickSetting resetOnKB;
    private final TickSetting onlyForward;

    // State
    private int playerTicks = 0;
    private int smartTick = 0;
    private int cooldownTick = 0;
    private float randomRange = 0f;
    private final Random rand = new Random();

    // Timer field access
    private static Field timerField;
    private static Field timerSpeedField;

    static {
        try {
            net.minecraft.client.Minecraft mcInstance = net.minecraft.client.Minecraft.getMinecraft();
            for (Field f : mcInstance.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("Timer")) {
                    f.setAccessible(true);
                    timerField = f;
                    break;
                }
            }
            if (timerField != null) {
                Timer timer = (Timer) timerField.get(mcInstance);
                for (Field f : timer.getClass().getDeclaredFields()) {
                    if (f.getType() == float.class) {
                        f.setAccessible(true);
                        String name = f.getName();
                        if (name.equals("timerSpeed") || name.equals("field_74278_d")) {
                            timerSpeedField = f;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[TimerRangeV4] Error initializing timer fields: " + e.getMessage());
        }
    }

    private void onModeChange() {
        System.out.println("[TimerRangeV4] Mode changed to: " + mode.getSelected());
        resetTimer();
        playerTicks = 0;
        smartTick = 0;
        cooldownTick = 0;
    }

    public TimerRangeV4() {
        super("TimerRangeV4", ModuleCategory.COMBAT);
        this.registerSetting(new DescriptionSetting("FDPClient style TimerRange"));

        // Mode selection
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Normal", "Smart", "Modern"})
            .onChange(this::onModeChange));

        // Timer settings
        this.registerSetting(ticks = new SliderSetting("Ticks", 10, 1, 20, 1));
        this.registerSetting(timerBoost = new SliderSetting("Timer Boost", 1.5, 0.1, 5.0, 0.1));
        this.registerSetting(timerCharged = new SliderSetting("Timer Charged", 0.45, 0.1, 2.0, 0.05));
        this.registerSetting(boostPhase = new SliderSetting("Boost Phase %", 50, 10, 90, 5));
        this.registerSetting(chargedPhase = new SliderSetting("Charged Phase %", 80, 20, 100, 5));

        // Range (Normal mode)
        this.registerSetting(minRange = new SliderSetting("Min Range", 2.5, 1, 8, 0.1));
        this.registerSetting(maxRange = new SliderSetting("Max Range", 3.5, 1, 8, 0.1));
        this.registerSetting(scanRange = new SliderSetting("Scan Range", 8, 2, 12, 0.5));

        // Delay settings
        this.registerSetting(minTickDelay = new SliderSetting("Min Tick Delay", 30, 1, 100, 5));
        this.registerSetting(maxTickDelay = new SliderSetting("Max Tick Delay", 60, 1, 200, 5));
        this.registerSetting(cooldownTicks = new SliderSetting("Cooldown Ticks", 10, 1, 50, 1));

        // Options
        this.registerSetting(maxAngle = new SliderSetting("Max Angle", 45, 5, 180, 5));
        this.registerSetting(onlyOnGround = new TickSetting("Only onGround", false));
        this.registerSetting(resetOnKB = new TickSetting("Reset on KB", true));
        this.registerSetting(onlyForward = new TickSetting("Only Forward", true));
    }

    @Override
    public void onEnable() {
        playerTicks = 0;
        smartTick = 0;
        cooldownTick = 0;
    }

    @Override
    public void onDisable() {
        resetTimer();
        playerTicks = 0;
        smartTick = 0;
        cooldownTick = 0;
    }

    @Override
    public void onUpdate() {
        EntityPlayerSP player = getPlayer();
        if (player == null || getWorld() == null) {
            resetTimer();
            return;
        }

        // Reset on knockback
        if (resetOnKB.isEnabled() && player.hurtTime > 0) {
            resetTimer();
            playerTicks = 0;
            return;
        }

        // Update random range each tick
        randomRange = (float) (minRange.getValue() + rand.nextDouble() * (maxRange.getValue() - minRange.getValue()));

        String currentMode = mode.getSelected();

        switch (currentMode) {
            case "Normal":
                handleNormalMode(player);
                break;
            case "Smart":
                handleSmartMode(player);
                break;
            case "Modern":
                handleModernMode(player);
                break;
        }

        // Apply timer speed based on playerTicks
        if (playerTicks > 0) {
            applyTimerSpeed();
            playerTicks--;
        } else {
            resetTimer();
        }
    }

    private void handleNormalMode(EntityPlayerSP player) {
        if (onlyOnGround.isEnabled() && !player.onGround) return;

        EntityPlayer target = findTarget(player, (float) maxRange.getValue());
        if (target == null) return;

        cooldownTick++;

        if (cooldownTick >= cooldownTicks.getValue()) {
            double distance = player.getDistanceToEntity(target);
            if (distance <= maxRange.getValue()) {
                playerTicks = (int) ticks.getValue();
                cooldownTick = 0;
            }
        }
    }

    private void handleSmartMode(EntityPlayerSP player) {
        if (onlyOnGround.isEnabled() && !player.onGround) return;
        if (!isMoving(player)) return;

        EntityPlayer target = findTarget(player, (float) scanRange.getValue());
        if (target == null) return;

        int randomTickDelay = (int) (minTickDelay.getValue() +
            rand.nextInt((int) (maxTickDelay.getValue() - minTickDelay.getValue() + 1)));

        smartTick++;

        if (smartTick >= randomTickDelay) {
            double distance = player.getDistanceToEntity(target);
            if (distance <= randomRange) {
                playerTicks = (int) ticks.getValue();
                smartTick = 0;
            }
        }
    }

    private void handleModernMode(EntityPlayerSP player) {
        if (onlyOnGround.isEnabled() && !player.onGround) return;
        if (!isMoving(player)) {
            smartTick = 0;
            return;
        }

        if (onlyForward.isEnabled()) {
            if (player.movementInput == null || player.movementInput.moveForward <= 0) {
                return;
            }
        }

        EntityPlayer target = findTarget(player, (float) scanRange.getValue());
        if (target == null) return;

        // Check if looking at target
        if (!isLookingAt(player, target, (float) maxAngle.getValue())) {
            resetTimer();
            return;
        }

        int randomTickDelay = (int) (minTickDelay.getValue() +
            rand.nextInt((int) (maxTickDelay.getValue() - minTickDelay.getValue() + 1)));

        smartTick++;

        if (smartTick >= randomTickDelay) {
            double distance = player.getDistanceToEntity(target);
            if (distance >= minRange.getValue() && distance <= maxRange.getValue()) {
                playerTicks = (int) ticks.getValue();
                smartTick = 0;
            }
        }
    }

    private void applyTimerSpeed() {
        if (timerField == null || timerSpeedField == null) return;

        try {
            Timer timer = (Timer) timerField.get(mc);

            double tickProgress = (double) playerTicks / ticks.getValue();
            float speed;

            // Boost phase (high timer)
            if (tickProgress > (1.0 - boostPhase.getValue() / 100.0)) {
                speed = (float) timerBoost.getValue();
            }
            // Charged phase (slow timer)
            else if (tickProgress > (1.0 - chargedPhase.getValue() / 100.0)) {
                speed = (float) timerCharged.getValue();
            }
            // Normal
            else {
                speed = 1.0f;
            }

            timerSpeedField.setFloat(timer, speed);
        } catch (Exception e) {
            System.out.println("[TimerRangeV4] Error setting timer: " + e.getMessage());
        }
    }

    private void resetTimer() {
        if (timerField == null || timerSpeedField == null) return;
        try {
            Timer timer = (Timer) timerField.get(mc);
            timerSpeedField.setFloat(timer, 1.0f);
        } catch (Exception e) {
            // Silent
        }
    }

    private EntityPlayer findTarget(EntityPlayerSP player, float range) {
        EntityPlayer closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Object obj : getWorld().playerEntities) {
            if (!(obj instanceof EntityPlayer)) continue;
            EntityPlayer target = (EntityPlayer) obj;

            if (target == player) continue;
            if (target.isDead) continue;

            double distance = player.getDistanceToEntity(target);
            if (distance > range) continue;

            if (distance < closestDist) {
                closestDist = distance;
                closest = target;
            }
        }

        return closest;
    }

    private boolean isMoving(EntityPlayerSP player) {
        return player.movementInput != null &&
               (player.movementInput.moveForward != 0 || player.movementInput.moveStrafe != 0);
    }

    private boolean isLookingAt(EntityPlayerSP player, EntityPlayer target, float maxAngle) {
        double deltaX = target.posX - player.posX;
        double deltaZ = target.posZ - player.posZ;
        double yawToTarget = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;

        double yawDiff = wrapAngle(player.rotationYaw - yawToTarget);
        return Math.abs(yawDiff) <= maxAngle;
    }

    private double wrapAngle(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + mode.getSelected();
    }
}
