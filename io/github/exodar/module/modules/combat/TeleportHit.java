/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.Main;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.module.modules.misc.Friends;
import io.github.exodar.module.modules.misc.Teams;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

import java.lang.reflect.Field;

/**
 * TeleportHit - Auto teleport toward targets
 *
 * Requirements:
 * - W key pressed (forward only)
 * - On ground (not jumping/falling)
 * - Not in water/lava
 * - Weapon in hand (sword/axe)
 * - Left click held
 * - Target in FOV (30°)
 * - Min range: 3 blocks (fixed)
 * - Max range: based on Lag Time (500ms = 6 blocks)
 */
public class TeleportHit extends Module {

    // Fixed values
    private static final double MIN_RANGE = 3.0;
    private static final double STOP_DISTANCE = 2.5; // Stop this far from target
    private static final double MAX_TRAVEL = 2.0; // Max blocks to travel per teleport
    private static final double FOV = 30.0;

    // Settings
    private final SliderSetting lagTime;
    private final SliderSetting cooldown;
    private final TickSetting onlyWeapon;
    private final TickSetting requireLeftClick;

    // State
    private EntityLivingBase target = null;
    private long lastTeleportTime = 0;
    private boolean isTeleporting = false;

    // Reflection for timer
    private static Field timerSpeedField = null;
    private static Object timerObject = null;
    private static boolean reflectionInitialized = false;

    public TeleportHit() {
        super("TeleportHit", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("TP to targets (W + LMB)"));
        this.registerSetting(lagTime = new SliderSetting("Lag Time", 150, 50, 300, 10));
        this.registerSetting(cooldown = new SliderSetting("Cooldown", 500, 100, 2000, 50));
        this.registerSetting(onlyWeapon = new TickSetting("Only Weapon", true));
        this.registerSetting(requireLeftClick = new TickSetting("Require Left Click", true));
    }

    /**
     * Calculate max range based on Lag Time
     * 50ms = 3.5 blocks, 500ms = 6 blocks
     */
    private double getMaxRange() {
        double lt = lagTime.getValue();
        // Scale: 50ms -> 3.5 blocks, 500ms -> 6 blocks
        return 3.5 + (lt - 50) / 450.0 * 2.5;
    }

    @Override
    public void onEnable() {
        target = null;
        isTeleporting = false;
        lastTeleportTime = 0;
        initTimerReflection();
    }

    @Override
    public void onDisable() {
        setTimerSpeed(1.0f);
        target = null;
        isTeleporting = false;
    }

    private void initTimerReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;

        try {
            for (Field f : mc.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object obj = f.get(mc);
                if (obj != null && obj.getClass().getSimpleName().contains("Timer")) {
                    timerObject = obj;
                    for (Field tf : timerObject.getClass().getDeclaredFields()) {
                        tf.setAccessible(true);
                        if (tf.getType() == float.class) {
                            try {
                                float val = tf.getFloat(timerObject);
                                if (val >= 0.9f && val <= 1.1f) {
                                    timerSpeedField = tf;
                                    break;
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    if (timerSpeedField != null) break;
                }
            }
        } catch (Exception e) {}
    }

    private void setTimerSpeed(float speed) {
        try {
            if (timerSpeedField != null && timerObject != null) {
                timerSpeedField.setFloat(timerObject, speed);
            }
        } catch (Exception e) {}
    }

    @Override
    public void onUpdate() {
        EntityPlayerSP player = getPlayer();
        if (player == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) {
            setTimerSpeed(1.0f);
            isTeleporting = false;
            return;
        }

        // Don't start new teleport while one is in progress
        if (isTeleporting) return;

        long currentTime = System.currentTimeMillis();

        // === CHECK ALL CONDITIONS ===

        // Must be pressing W only (not A, S, D)
        if (!mc.gameSettings.keyBindForward.isKeyDown()) return;
        if (mc.gameSettings.keyBindBack.isKeyDown()) return;
        if (mc.gameSettings.keyBindLeft.isKeyDown()) return;
        if (mc.gameSettings.keyBindRight.isKeyDown()) return;

        // Must be on ground (not jumping/falling)
        if (!player.onGround) return;

        // Must not be in liquid
        if (player.isInWater() || player.isInLava()) return;

        // Must have weapon in hand
        if (onlyWeapon.isEnabled() && !isHoldingWeapon(player)) return;

        // Must be holding left click (use Mouse directly for better compatibility)
        if (requireLeftClick.isEnabled() && !org.lwjgl.input.Mouse.isButtonDown(0)) return;

        // Check cooldown
        if (currentTime - lastTeleportTime < (long) cooldown.getValue()) return;

        // Check if player can move
        if (player.isDead || player.isSpectator()) return;

        // Find target
        target = findTarget();
        if (target == null) return;

        // === START TELEPORT ===
        double dx = target.posX - player.posX;
        double dz = target.posZ - player.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);

        // Calculate how far we need to travel (stop at STOP_DISTANCE from target)
        double distanceToTravel = dist - STOP_DISTANCE;
        if (distanceToTravel <= 0) return; // Already close enough

        // Cap max travel distance
        distanceToTravel = Math.min(distanceToTravel, MAX_TRAVEL);

        // Normalize direction
        double dirX = dx / dist;
        double dirZ = dz / dist;

        isTeleporting = true;

        // Calculate timer speed needed to travel distanceToTravel in 1 tick
        // 1 tick at timer X with speed 0.2 = 0.2 * X blocks
        // So X = distanceToTravel / 0.2
        double baseSpeed = 0.2;
        double timerForOneTick = distanceToTravel / baseSpeed;
        timerForOneTick = Math.max(5.0, Math.min(20.0, timerForOneTick)); // Clamp 5-20x

        // Set motion toward target
        player.motionX = dirX * baseSpeed;
        player.motionZ = dirZ * baseSpeed;

        // Fast timer for 1 tick = instant teleport to hit range
        setTimerSpeed((float) timerForOneTick);

        // Attack target NOW (same tick as teleport)
        if (target != null && mc.playerController != null) {
            player.swingItem();
            mc.playerController.attackEntity(player, target);
        }

        // Calculate balance debt: we used timerForOneTick for ~1 tick (50ms)
        // Debt = (timerForOneTick - 1.0) * 50ms worth of game time
        final long balanceDebt = (long) ((timerForOneTick - 1.0) * 50);

        // Separate thread: wait 1 tick, then freeze timer to compensate
        new Thread(() -> {
            try {
                // Wait for the fast tick to finish
                Thread.sleep(50);

                // IMMEDIATELY timer 0 to compensate balance
                setTimerSpeed(0.0f);

                // Stay frozen for balanceDebt time
                Thread.sleep(balanceDebt);

                // Back to normal
                setTimerSpeed(1.0f);
                isTeleporting = false;
                lastTeleportTime = System.currentTimeMillis();

            } catch (InterruptedException e) {
                setTimerSpeed(1.0f);
                isTeleporting = false;
            }
        }).start();

        target = null;
    }

    /**
     * Check if holding sword or axe
     */
    private boolean isHoldingWeapon(EntityPlayerSP player) {
        ItemStack held = player.getHeldItem();
        if (held == null) return false;
        return held.getItem() instanceof ItemSword || held.getItem() instanceof ItemAxe;
    }

    /**
     * Find closest target in range and FOV (30°)
     */
    private EntityLivingBase findTarget() {
        EntityPlayerSP player = getPlayer();
        if (player == null || mc.theWorld == null) return null;

        double maxRange = getMaxRange();
        EntityLivingBase closest = null;
        double closestDist = maxRange;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer)) continue;
            if (entity == player) continue;
            if (!entity.isEntityAlive()) continue;

            EntityPlayer ep = (EntityPlayer) entity;

            if (Friends.isFriend(ep.getName())) continue;
            if (isTeamMate(player, ep)) continue;
            if (AntiBot.isBotForCombat(ep)) continue;

            double dist = player.getDistanceToEntity(ep);
            if (dist < MIN_RANGE || dist > maxRange) continue;

            // Check FOV (30°)
            if (!isInFov(player, ep)) continue;

            if (dist < closestDist) {
                closestDist = dist;
                closest = ep;
            }
        }

        return closest;
    }

    /**
     * Check if target is in FOV (30°)
     */
    private boolean isInFov(EntityPlayerSP player, Entity target) {
        double dx = target.posX - player.posX;
        double dz = target.posZ - player.posZ;
        double yawToTarget = Math.toDegrees(Math.atan2(dz, dx)) - 90;

        double yawDiff = wrapAngle(player.rotationYaw - yawToTarget);
        return Math.abs(yawDiff) <= FOV / 2.0; // 15° each side
    }

    private double wrapAngle(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    private boolean isTeamMate(EntityPlayerSP player, Entity entity) {
        try {
            ModuleManager manager = Main.getModuleManager();
            Module teams = manager != null ? manager.getModuleByName("Teams") : null;
            if (teams != null && teams.isEnabled() && teams instanceof Teams) {
                return ((Teams) teams).isTeamMate(entity);
            }
            if (entity instanceof EntityLivingBase) {
                return player.isOnSameTeam((EntityLivingBase) entity);
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    public String getDisplaySuffix() {
        if (isTeleporting) {
            return " §aTP";
        }
        return " §7" + String.format("%.1f", getMaxRange());
    }
}
