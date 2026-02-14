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
import io.github.exodar.module.modules.misc.Friends;
import io.github.exodar.module.modules.misc.Teams;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import java.lang.reflect.Field;

/**
 * DashHit - Gothaj 3.3.0 TimerRange port
 *
 * Balance system:
 * - Tracks game ticks vs real time ticks
 * - Slow timer accumulates balance (debt)
 * - Fast timer spends balance to teleport
 * - Perfect balance = undetectable
 */
public class DashHit extends Module {

    // Settings
    public SliderSetting minRange = new SliderSetting("Min Range", 3.0, 3.0, 6.0, 0.1);
    public SliderSetting maxRange = new SliderSetting("Max Range", 6.0, 3.0, 8.0, 0.1);
    public SliderSetting maxTimer = new SliderSetting("Timer", 10.0, 1.0, 10.0, 0.1);
    public SliderSetting slowTimer = new SliderSetting("Slow Timer", 0.1, 0.0, 1.0, 0.01);
    public SliderSetting chargeMultiplier = new SliderSetting("Charge Multi", 1.0, 0.1, 1.0, 0.01);
    public SliderSetting delay = new SliderSetting("Delay", 200, 0, 3000, 50);
    public TickSetting instantTimer = new TickSetting("Instant Teleport", true);
    public TickSetting notInCombo = new TickSetting("Not In Combo", false);
    public TickSetting onlyForward = new TickSetting("Only Forward", true);
    public TickSetting onlyOnGround = new TickSetting("Only On Ground", true);
    public TickSetting noFluid = new TickSetting("No Fluid", true);

    // State - Gothaj balance system
    private double balance = 0;
    private double lastBalance = 0;
    private double smartMaxBalance = 0;
    private boolean fast = false;
    private EntityLivingBase target = null;
    private long delayTimer = 0;
    private long attackTimer = 0;

    // Reflection for timer
    private static Field timerSpeedField = null;
    private static Object timerObject = null;
    private static boolean reflectionInitialized = false;

    // Prevent recursion when calling mc.runTick()
    private static boolean isRunningTick = false;

    public DashHit() {
        super("DashHit", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Gothaj TimerRange"));
        this.registerSetting(minRange);
        this.registerSetting(maxRange);
        this.registerSetting(maxTimer);
        this.registerSetting(slowTimer);
        this.registerSetting(chargeMultiplier);
        this.registerSetting(delay);
        this.registerSetting(instantTimer);
        this.registerSetting(notInCombo);
        this.registerSetting(onlyForward);
        this.registerSetting(onlyOnGround);
        this.registerSetting(noFluid);
    }

    @Override
    public void onEnable() {
        balance = 0;
        lastBalance = 0;
        smartMaxBalance = 0;
        fast = false;
        target = null;
        delayTimer = System.currentTimeMillis();
        initTimerReflection();
    }

    @Override
    public void onDisable() {
        setTimerSpeed(1.0f);
        fast = false;
        target = null;
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

    private float getTimerSpeed() {
        try {
            if (timerSpeedField != null && timerObject != null) {
                return timerSpeedField.getFloat(timerObject);
            }
        } catch (Exception e) {}
        return 1.0f;
    }

    @Override
    public void onUpdate() {
        // Prevent recursion from mc.runTick()
        if (isRunningTick) return;

        EntityPlayerSP player = getPlayer();
        if (player == null || mc.theWorld == null) return;

        // Balance tracking (like Gothaj onTick)
        if (getTimerSpeed() != 1.0f) {
            balance += chargeMultiplier.getValue();
        } else {
            balance++;
        }

        // Main logic (like Gothaj onGameLoop)
        onGameLoop();
    }

    /**
     * Called every frame - main Gothaj logic
     */
    private void onGameLoop() {
        EntityPlayerSP player = getPlayer();
        if (player == null || mc.theWorld == null) return;

        if (player.ticksExisted < 10) {
            setTimerSpeed(1.0f);
            return;
        }

        // Find target
        target = findTarget();

        if (target != null && outOfRange()) {
            target = null;
        }

        // Decrement balance (like EventTimeDelay)
        balance--;

        long currentTime = System.currentTimeMillis();

        if (fast) {
            // === FAST PHASE - Teleporting ===
            if (balance < smartMaxBalance + lastBalance) {
                if (target != null) {
                    if (!isTargetCloseOrVisible()) {
                        if (isHurtTime()) {
                            if (instantTimer.isEnabled()) {
                                // Instant teleport mode - run ticks until done
                                boolean shouldStop = false;
                                int maxIterations = 100; // Safety limit
                                int iterations = 0;

                                while (!shouldStop && iterations < maxIterations) {
                                    if (shouldStop() || balance >= smartMaxBalance + lastBalance) {
                                        shouldStop = true;
                                        delayTimer = currentTime;
                                        setTimerSpeed(1.0f);
                                        fast = false;

                                        // Attack after teleport
                                        if (currentTime - attackTimer >= 350) {
                                            if (target != null && target.isEntityAlive()) {
                                                player.swingItem();
                                                mc.playerController.attackEntity(player, target);
                                            }
                                            attackTimer = currentTime;
                                        }
                                    }

                                    if (!shouldStop) {
                                        // Simulate tick with recursion protection
                                        try {
                                            isRunningTick = true;
                                            mc.runTick();
                                        } catch (Exception e) {
                                        } finally {
                                            isRunningTick = false;
                                        }
                                        balance += chargeMultiplier.getValue();
                                    }
                                    iterations++;
                                }
                            } else {
                                // Normal fast timer mode
                                setTimerSpeed((float) maxTimer.getValue());
                                if (shouldStop() && fast) {
                                    setTimerSpeed(1.0f);
                                    fast = false;
                                }
                            }
                        } else {
                            // Not in hurt time
                            setTimerSpeed(1.0f);
                            fast = false;
                        }
                    } else {
                        // Target close or visible - stop and attack
                        setTimerSpeed(1.0f);
                        fast = false;

                        if (currentTime - attackTimer >= 350) {
                            if (target != null && target.isEntityAlive()) {
                                player.swingItem();
                                mc.playerController.attackEntity(player, target);
                            }
                            attackTimer = currentTime;
                        }
                    }
                } else {
                    // No target
                    setTimerSpeed(1.0f);
                    fast = false;
                }
            } else {
                // Balance depleted
                setTimerSpeed(1.0f);
                fast = false;
            }
        }

        if (!fast) {
            // === SLOW PHASE - Accumulating balance ===
            if (balance > lastBalance) {
                setTimerSpeed((float) slowTimer.getValue());
            } else {
                if (getTimerSpeed() == (float) slowTimer.getValue()) {
                    setTimerSpeed(1.0f);
                }

                // Check delay
                if (currentTime - delayTimer < delay.getValue()) {
                    return;
                }

                // Start fast phase if conditions met
                if (target != null && !isTargetCloseOrVisible() && isHurtTime()) {
                    setSmartBalance();
                    fast = true;
                    delayTimer = currentTime;
                    lastBalance = balance;
                }
            }
        }

        // Safety - reset timer if player just spawned
        if (player.ticksExisted <= 20) {
            setTimerSpeed(1.0f);
        }
    }

    /**
     * Calculate smart balance based on distance
     */
    private void setSmartBalance() {
        if (target == null) {
            smartMaxBalance = 0;
            return;
        }

        if (shouldStop()) {
            smartMaxBalance = 0;
            return;
        }

        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        double distance = player.getDistanceToEntity(target);
        double playerBPS = Math.max(0.1, getSpeed(player));
        double finalDistance = distance - 3.0;

        smartMaxBalance = Math.ceil(finalDistance / playerBPS);
    }

    private double getSpeed(EntityPlayerSP player) {
        double motionX = player.posX - player.lastTickPosX;
        double motionZ = player.posZ - player.lastTickPosZ;
        return Math.sqrt(motionX * motionX + motionZ * motionZ);
    }

    /**
     * Check if should stop teleporting
     */
    private boolean shouldStop() {
        EntityPlayerSP player = getPlayer();
        if (player == null || target == null) return true;

        // Predict position
        double predictX = player.posX + (player.posX - player.lastTickPosX) * 2.0;
        double predictZ = player.posZ + (player.posZ - player.lastTickPosZ) * 2.0;
        float dx = (float) (predictX - target.posX);
        float dy = (float) (player.posY - target.posY);
        float dz = (float) (predictZ - target.posZ);
        double predictedDistance = MathHelper.sqrt_float(dx * dx + dy * dy + dz * dz);

        // OnGround check
        if (onlyOnGround.isEnabled() && !player.onGround) {
            return true;
        }

        // Min range check
        if (player.getDistanceToEntity(target) <= minRange.getValue()) {
            if (!fast && getTimerSpeed() != (float) slowTimer.getValue()) {
                return true;
            }
        }

        // Target close or visible
        if (isTargetCloseOrVisible()) {
            return true;
        }

        // Hurt time check
        if (!isHurtTime()) {
            return true;
        }

        // Out of range
        if (outOfRange()) {
            return true;
        }

        // Forward check - only W, no strafing
        if (onlyForward.isEnabled()) {
            // Must be pressing W
            if (!mc.gameSettings.keyBindForward.isKeyDown()) {
                return true;
            }
            // Must NOT be strafing (A or D)
            if (mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown()) {
                return true;
            }
            // Must be moving forward
            if (getSpeed(player) <= 0.12) {
                return true;
            }
            // Must be getting closer to target
            if (predictedDistance > player.getDistanceToEntity(target) + 0.12) {
                return true;
            }
        }

        // Fluid check
        if (noFluid.isEnabled() && (player.isInWater() || player.isInLava())) {
            return true;
        }

        // Not in combo check
        if (notInCombo.isEnabled()) {
            if (player.getDistance(target.lastTickPosX, target.lastTickPosY, target.lastTickPosZ)
                < player.getDistance(target.posX, target.posY, target.posZ)) {
                return true;
            }
        }

        return false;
    }

    private boolean outOfRange() {
        EntityPlayerSP player = getPlayer();
        if (player == null || target == null) return true;
        return player.getDistanceToEntity(target) > maxRange.getValue();
    }

    private boolean isTargetCloseOrVisible() {
        if (target == null) return false;

        // Check if looking at target
        if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit == target) {
            return true;
        }

        // Check if target is within hit range (reduced to 2.5 for chasing)
        EntityPlayerSP player = getPlayer();
        if (player != null && player.getDistanceToEntity(target) <= 2.5) {
            return true;
        }

        return false;
    }

    private boolean isHurtTime() {
        EntityPlayerSP player = getPlayer();
        if (player == null) return false;
        // Allow dash even with some hurt time (up to 5)
        return player.hurtTime <= 5;
    }

    private EntityLivingBase findTarget() {
        EntityPlayerSP player = getPlayer();
        if (player == null || mc.theWorld == null) return null;

        EntityLivingBase closest = null;
        double closestDist = maxRange.getValue() + 10.0;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer)) continue;
            if (entity == player) continue;
            if (!entity.isEntityAlive()) continue;

            EntityPlayer ep = (EntityPlayer) entity;

            if (Friends.isFriend(ep.getName())) continue;
            if (isTeamMate(player, ep)) continue;

            double dist = player.getDistanceToEntity(ep);
            if (dist > maxRange.getValue() + 10.0) continue;

            if (dist < closestDist) {
                closestDist = dist;
                closest = ep;
            }
        }

        return closest;
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
        if (fast) {
            return " §aFAST §7" + String.format("%.0f", balance);
        }
        if (getTimerSpeed() < 1.0f) {
            return " §cSLOW §7" + String.format("%.0f", balance);
        }
        // Show why not activating
        EntityPlayerSP player = getPlayer();
        if (target == null) {
            return " §8No target";
        }
        if (player != null) {
            double dist = player.getDistanceToEntity(target);
            if (dist <= 2.5) {
                return " §eCLOSE §7" + String.format("%.1f", dist);
            }
            if (dist > maxRange.getValue()) {
                return " §cFAR §7" + String.format("%.1f", dist);
            }
            return " §7" + String.format("%.1f", dist) + "m";
        }
        return " §7" + String.format("%.1f", maxRange.getValue());
    }
}
