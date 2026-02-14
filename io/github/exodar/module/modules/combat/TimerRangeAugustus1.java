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
import io.github.exodar.setting.ModeSetting;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import java.lang.reflect.Field;

/**
 * TimerRangeAugustus1 - Augustus v2.8 TimerRange (TickBase)
 *
 * Simple tick-based system:
 * - Every X ticks, triggers timer sequence
 * - lowTimer -> highTimer -> reset
 */
public class TimerRangeAugustus1 extends Module {

    // Settings
    private final ModeSetting mode;
    private final SliderSetting ticks;
    private final SliderSetting lowTimer;
    private final SliderSetting highTimer;
    private final SliderSetting range;

    // State
    private int counter = -1;
    private boolean freezing = false;
    private int last = 0;
    private boolean pls_do_the_timer_momento = false;
    private boolean newFunny = false;
    private boolean reset = false;

    // Reflection for timer
    private static Field timerSpeedField = null;
    private static Object timerObject = null;
    private static boolean reflectionInitialized = false;

    public TimerRangeAugustus1() {
        super("TimerRangeAugustus1", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Augustus TickBase"));
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"TimerRange", "Shidori", "Vestige"}));
        this.registerSetting(ticks = new SliderSetting("Ticks", 10, 0, 40, 1));
        this.registerSetting(lowTimer = new SliderSetting("FirstTimer", 0.3, 0.1, 20.0, 0.1));
        this.registerSetting(highTimer = new SliderSetting("NextTimer", 3.0, 0.1, 20.0, 0.1));
        this.registerSetting(range = new SliderSetting("Range", 5.0, 3.0, 8.0, 0.1));
    }

    @Override
    public void onEnable() {
        counter = -1;
        freezing = false;
        pls_do_the_timer_momento = false;
        newFunny = false;
        reset = false;
        initTimerReflection();

        EntityPlayerSP player = getPlayer();
        if (player != null) {
            last = player.ticksExisted;
        } else {
            last = 0;
        }
    }

    @Override
    public void onDisable() {
        setTimerSpeed(1.0f);
        freezing = false;
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

        String currentMode = mode.getSelected();

        // Handle Vestige mode freezing
        if ("Vestige".equals(currentMode) && freezing) {
            player.posX = player.lastTickPosX;
            player.posY = player.lastTickPosY;
            player.posZ = player.lastTickPosZ;
        }

        // TimerRange mode logic
        if ("TimerRange".equals(currentMode)) {
            if (player.ticksExisted == 5) {
                reset = true;
                newFunny = false;
                pls_do_the_timer_momento = false;
                last = 0;
            }

            if (reset) {
                setTimerSpeed(1.0f);
                reset = false;
            }

            if (newFunny) {
                setTimerSpeed((float) highTimer.getValue());
                newFunny = false;
                reset = true;
            }

            if (pls_do_the_timer_momento) {
                setTimerSpeed((float) lowTimer.getValue());
                pls_do_the_timer_momento = false;
                newFunny = true;
            }
        }

        // Check for extra ticks (target finding)
        int extraTicks = getExtraTicks(player);
        if (extraTicks > 0) {
            counter = extraTicks;
        }

        // Attack trigger
        EntityLivingBase target = findTarget();
        if (target != null && player.getDistanceToEntity(target) <= range.getValue()) {
            attack(player);
        }
    }

    private int getExtraTicks(EntityPlayerSP player) {
        if (counter-- > 0) {
            return -1;
        } else {
            freezing = false;
        }

        // Find target in pre-range
        EntityLivingBase target = findTarget();
        if (target != null) {
            double dist = player.getDistanceToEntity(target);
            if (dist > range.getValue() && dist <= range.getValue() + 0.75 && player.hurtTime <= 2) {
                return (int) ticks.getValue();
            }
        }

        return 0;
    }

    private void attack(EntityPlayerSP player) {
        if ((player.ticksExisted - last) >= (int) ticks.getValue()) {
            pls_do_the_timer_momento = true;
            last = player.ticksExisted;
        }
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
        return " §7" + mode.getSelected();
    }
}
