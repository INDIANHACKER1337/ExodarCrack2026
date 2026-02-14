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

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * TimerRangeAugustus3 - Augustus v2.8 OldTimerRange
 *
 * Simple balance counter based on packet timing:
 * - Tracks time between movement packets
 * - Calculates balance based on diff from 50ms tick
 * - Uses balance to determine when to boost timer
 */
public class TimerRangeAugustus3 extends Module {

    // Settings
    private final SliderSetting range;
    private final SliderSetting timerBoost;
    private final SliderSetting maxBalance;
    private final TickSetting onlyForward;
    private final TickSetting onlyOnGround;

    // State
    private final ArrayList<Integer> diffs = new ArrayList<>();
    public long balanceCounter = 0L;
    private long lastTime = 0;
    private EntityLivingBase target = null;

    // Reflection for timer
    private static Field timerSpeedField = null;
    private static Object timerObject = null;
    private static boolean reflectionInitialized = false;

    public TimerRangeAugustus3() {
        super("TimerRangeAugustus3", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Augustus OldTimerRange"));
        this.registerSetting(range = new SliderSetting("Range", 5.0, 3.0, 8.0, 0.1));
        this.registerSetting(timerBoost = new SliderSetting("TimerBoost", 1.5, 1.0, 5.0, 0.1));
        this.registerSetting(maxBalance = new SliderSetting("MaxBalance", 150, 50, 500, 10));
        this.registerSetting(onlyForward = new TickSetting("Only Forward", true));
        this.registerSetting(onlyOnGround = new TickSetting("Only OnGround", false));
    }

    @Override
    public void onEnable() {
        balanceCounter = 0L;
        lastTime = System.currentTimeMillis();
        diffs.clear();
        initTimerReflection();
        setTimerSpeed(1.0f);
    }

    @Override
    public void onDisable() {
        balanceCounter = 0L;
        diffs.clear();
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

        // Track packet timing (simulating C03PacketPlayer timing)
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - lastTime;

        if (balanceCounter > 0) {
            balanceCounter--;
        }

        // Track diff and calculate balance
        diffs.add((int) diff);
        if (diffs.size() > 20) {
            diffs.remove(0);
        }

        // Balance formula from Augustus: (diff - 50) * -3
        // If tick took longer than 50ms, we get negative balance (can speed up)
        // If tick was faster than 50ms, we get positive balance (should slow down)
        balanceCounter += (diff - 50L) * -3L;

        lastTime = currentTime;

        // Cap balance
        if (balanceCounter < -500) balanceCounter = -500;
        if (balanceCounter > (long) maxBalance.getValue()) balanceCounter = (long) maxBalance.getValue();

        // Check conditions
        if (shouldStop(player)) {
            if (getTimerSpeed() != 1.0f) {
                // Restore timer from thread
                final float currentSpeed = getTimerSpeed();
                if (currentSpeed == 0.0f) {
                    new Thread(() -> {
                        try {
                            Thread.sleep(50);
                            setTimerSpeed(1.0f);
                        } catch (InterruptedException e) {}
                    }).start();
                } else {
                    setTimerSpeed(1.0f);
                }
            }
            return;
        }

        // Find target
        target = findTarget();
        if (target == null) {
            setTimerSpeed(1.0f);
            return;
        }

        double dist = player.getDistanceToEntity(target);

        // If we have positive balance and target in range, boost timer
        if (balanceCounter > 50 && dist <= range.getValue() && dist > 3.0) {
            setTimerSpeed((float) timerBoost.getValue());
        } else if (balanceCounter < -50) {
            // We owe ticks, slow down slightly
            setTimerSpeed(0.9f);
        } else {
            setTimerSpeed(1.0f);
        }
    }

    private boolean shouldStop(EntityPlayerSP player) {
        if (onlyForward.isEnabled() && !mc.gameSettings.keyBindForward.isKeyDown()) {
            return true;
        }

        if (onlyOnGround.isEnabled() && !player.onGround) {
            return true;
        }

        if (player.hurtTime > 2) {
            return true;
        }

        return false;
    }

    private EntityLivingBase findTarget() {
        EntityPlayerSP player = getPlayer();
        if (player == null || mc.theWorld == null) return null;

        EntityLivingBase closest = null;
        double closestDist = range.getValue() + 2.0;

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
        return " §7" + balanceCounter;
    }
}
