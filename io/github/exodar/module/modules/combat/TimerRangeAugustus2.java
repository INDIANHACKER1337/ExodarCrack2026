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
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.lang.reflect.Field;

/**
 * TimerRangeAugustus2 - Augustus v2.8 NewTimerRange
 *
 * Advanced balance system:
 * - Slow timer (lowt) accumulates balance up to maxBalance
 * - Then runs fast ticks with timer boost
 * - Uses precise bounding box distance calculations
 */
public class TimerRangeAugustus2 extends Module {

    // Settings
    private final SliderSetting delay;
    private final SliderSetting maxRange;
    private final SliderSetting minRange;
    private final SliderSetting timerBoost;
    private final SliderSetting lowTimer;
    private final SliderSetting slowTick;
    private final SliderSetting fastTick;
    private final TickSetting onlyForward;
    private final TickSetting onlyOnGround;

    // State
    private long balanceL = 0L;
    private long lastLagTime = 0;
    private long fastBalance = 0;
    private long delayTimer = 0;
    private EntityLivingBase target = null;

    // Reflection for timer
    private static Field timerSpeedField = null;
    private static Object timerObject = null;
    private static boolean reflectionInitialized = false;

    public TimerRangeAugustus2() {
        super("TimerRangeAugustus2", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Augustus NewTimerRange"));
        this.registerSetting(delay = new SliderSetting("Delay", 100, 0, 3000, 50));
        this.registerSetting(maxRange = new SliderSetting("FarStartRange", 3.8, 0.1, 10.0, 0.1));
        this.registerSetting(minRange = new SliderSetting("NearStopDist", 3.0, 0.1, 8.0, 0.1));
        this.registerSetting(timerBoost = new SliderSetting("TimerBoost", 10.0, 1.1, 35.0, 0.1));
        this.registerSetting(lowTimer = new SliderSetting("LowTimer", 0.2, 0.0, 0.99, 0.01));
        this.registerSetting(slowTick = new SliderSetting("SlowTick", 10, 0, 10, 1));
        this.registerSetting(fastTick = new SliderSetting("FastTick", 10, 0, 10, 1));
        this.registerSetting(onlyForward = new TickSetting("Only Forward", false));
        this.registerSetting(onlyOnGround = new TickSetting("Only OnGround", false));
    }

    @Override
    public void onEnable() {
        balanceL = 0L;
        lastLagTime = 0;
        fastBalance = 0;
        delayTimer = System.currentTimeMillis();
        initTimerReflection();
        setTimerSpeed(1.0f);
    }

    @Override
    public void onDisable() {
        balanceL = 0L;
        lastLagTime = 0;
        fastBalance = 0;
        setTimerSpeed(1.0f);
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
        EntityPlayerSP player = getPlayer();
        if (player == null || mc.theWorld == null) return;

        // Track fast balance
        if (getTimerSpeed() > 1) {
            fastBalance++;
            if (fastBalance > fastTick.getValue()) {
                setTimerSpeed(1.0f);
                delayTimer = System.currentTimeMillis();
            }
        }

        // Check stop conditions
        if (shouldStop(player)) {
            if (getTimerSpeed() != 1.0f) {
                setTimerSpeed(1.0f);
            }
            return;
        }

        long currentTime = System.currentTimeMillis();

        // Find target
        target = findTarget();
        if (target == null) return;

        // Calculate distances using bounding boxes (simplified from Augustus)
        double distToTarget = getDistanceToBoundingBox(player, target);
        double targetDistToMe = getDistanceToBoundingBox(target, player);

        // Check delay
        if (currentTime - delayTimer < delay.getValue()) return;

        // Check conditions
        if (player.hurtTime == 0 &&
            distToTarget <= maxRange.getValue() &&
            targetDistToMe <= maxRange.getValue() &&
            distToTarget >= minRange.getValue() &&
            isMoving(player)) {

            // Accumulate balance with slow timer
            if (balanceL < (long) slowTick.getValue()) {
                setTimerSpeed((float) lowTimer.getValue());
                if (currentTime - lastLagTime >= 50) {
                    balanceL++;
                    lastLagTime = currentTime;
                }
                return;
            }

            // Use balance - run fast ticks
            for (int i = 0; i < (int) fastTick.getValue(); i++) {
                player.onUpdate();
                setTimerSpeed((float) timerBoost.getValue());
            }
            fastBalance = 0;
            balanceL = 0L;
        }
    }

    private double getDistanceToBoundingBox(EntityLivingBase from, EntityLivingBase to) {
        float width = to.width / 2.0f;
        float height = to.height;

        AxisAlignedBB bb = new AxisAlignedBB(
            to.posX - width, to.posY, to.posZ - width,
            to.posX + width, to.posY + height, to.posZ + width
        );

        Vec3 eyes = from.getPositionEyes(1.0f);
        double bestX = MathHelper.clamp_double(eyes.xCoord, bb.minX, bb.maxX);
        double bestY = MathHelper.clamp_double(eyes.yCoord, bb.minY, bb.maxY);
        double bestZ = MathHelper.clamp_double(eyes.zCoord, bb.minZ, bb.maxZ);

        return eyes.distanceTo(new Vec3(bestX, bestY, bestZ));
    }

    private boolean shouldStop(EntityPlayerSP player) {
        if (onlyForward.isEnabled() && !mc.gameSettings.keyBindForward.isKeyDown()) {
            return true;
        }

        if (onlyOnGround.isEnabled() && !player.onGround) {
            return true;
        }

        if (target != null && player.getDistanceToEntity(target) <= minRange.getValue()) {
            return true;
        }

        if (player.hurtTime != 0) {
            return true;
        }

        return false;
    }

    private boolean isMoving(EntityPlayerSP player) {
        return player.movementInput != null &&
               (player.movementInput.moveForward != 0 || player.movementInput.moveStrafe != 0);
    }

    private EntityLivingBase findTarget() {
        EntityPlayerSP player = getPlayer();
        if (player == null || mc.theWorld == null) return null;

        EntityLivingBase closest = null;
        double closestDist = maxRange.getValue() + 5.0;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer)) continue;
            if (entity == player) continue;
            if (!entity.isEntityAlive()) continue;

            EntityPlayer ep = (EntityPlayer) entity;

            if (Friends.isFriend(ep.getName())) continue;
            if (isTeamMate(player, ep)) continue;

            double dist = player.getDistanceToEntity(ep);
            if (dist > closestDist) continue;

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
        if (getTimerSpeed() > 1) {
            return " §aFAST";
        }
        if (getTimerSpeed() < 1) {
            return " §cSLOW";
        }
        return " §7" + String.format("%.1f", maxRange.getValue());
    }
}
