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
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * TimerRangeRaven - Raven XD TimerRange port
 *
 * States:
 * - NONE: Waiting for target
 * - TIMER: Running extra ticks (mc.thePlayer.onUpdate)
 * - LAG: Freezing player motion, dropping packets
 */
public class TimerRangeRaven extends Module {

    // Settings
    private final SliderSetting lagTicks;
    private final SliderSetting timerTicks;
    private final SliderSetting minRange;
    private final SliderSetting maxRange;
    private final SliderSetting delay;
    private final SliderSetting fov;
    private final TickSetting ignoreTeammates;
    private final TickSetting onlyOnGround;
    private final TickSetting clearMotion;
    private final TickSetting notWhileKB;

    // State
    private final Queue<Packet<?>> delayedPackets = new ConcurrentLinkedQueue<>();
    private State state = State.NONE;
    private int hasLag = 0;
    private long lastTimerTime = -1;
    private float savedYaw, savedPitch;
    private double motionX, motionY, motionZ;

    // Flag to bypass packet hooks when releasing
    private boolean isSendingDirect = false;

    public TimerRangeRaven() {
        super("TimerRangeRaven", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Raven XD TimerRange"));
        this.registerSetting(lagTicks = new SliderSetting("Lag Ticks", 2, 0, 10, 1));
        this.registerSetting(timerTicks = new SliderSetting("Timer Ticks", 2, 0, 10, 1));
        this.registerSetting(minRange = new SliderSetting("Min Range", 3.6, 0, 8, 0.1));
        this.registerSetting(maxRange = new SliderSetting("Max Range", 5.0, 0, 8, 0.1));
        this.registerSetting(delay = new SliderSetting("Delay", 500, 0, 4000, 100));
        this.registerSetting(fov = new SliderSetting("FOV", 180, 0, 360, 30));
        this.registerSetting(ignoreTeammates = new TickSetting("Ignore Teammates", true));
        this.registerSetting(onlyOnGround = new TickSetting("Only OnGround", false));
        this.registerSetting(clearMotion = new TickSetting("Clear Motion", false));
        this.registerSetting(notWhileKB = new TickSetting("Not While KB", false));
    }

    @Override
    public void onEnable() {
        state = State.NONE;
        hasLag = 0;
        lastTimerTime = -1;
        delayedPackets.clear();
    }

    @Override
    public void onDisable() {
        done();
    }

    @Override
    public void onUpdate() {
        EntityPlayerSP player = getPlayer();
        if (player == null || mc.theWorld == null) {
            done();
            return;
        }

        switch (state) {
            case NONE:
                if (shouldStart(player)) {
                    state = State.TIMER;
                }
                break;

            case TIMER:
                // Speed up tick rate by calling onUpdate multiple times
                for (int i = 0; i < (int) timerTicks.getValue(); i++) {
                    player.onUpdate();
                }

                // Save current state for LAG phase
                savedYaw = player.rotationYaw;
                savedPitch = player.rotationPitch;
                motionX = player.motionX;
                motionY = player.motionY;
                motionZ = player.motionZ;

                hasLag = 0;
                state = State.LAG;
                break;

            case LAG:
                // Freeze player motion during lag ticks
                player.motionX = motionX;
                player.motionY = motionY;
                player.motionZ = motionZ;

                if (hasLag >= (int) lagTicks.getValue()) {
                    done();
                } else {
                    hasLag++;
                }
                break;
        }
    }

    /**
     * Called by Main.onSendPacket hook via Module.onSendPacket
     * @return true to allow packet, false to cancel
     */
    @Override
    public boolean onSendPacket(Object packet) {
        if (!enabled || isSendingDirect) return true;

        switch (state) {
            case TIMER:
                // Queue all packets during TIMER state
                synchronized (delayedPackets) {
                    delayedPackets.add((Packet<?>) packet);
                }
                return false; // Cancel sending

            case LAG:
                // During LAG, cancel C03PacketPlayer, queue others
                if (packet instanceof C03PacketPlayer) {
                    return false; // Cancel movement packets
                } else {
                    synchronized (delayedPackets) {
                        delayedPackets.add((Packet<?>) packet);
                    }
                    return false;
                }

            default:
                return true; // Allow packet
        }
    }

    private void done() {
        State previousState = state;
        state = State.NONE;
        hasLag = 0;
        lastTimerTime = System.currentTimeMillis();

        // Release all queued packets from separate thread
        new Thread(() -> {
            synchronized (delayedPackets) {
                isSendingDirect = true;
                try {
                    EntityPlayerSP player = getPlayer();
                    if (player != null && player.sendQueue != null) {
                        for (Packet<?> p : delayedPackets) {
                            if (p != null) {
                                player.sendQueue.addToSendQueue(p);
                            }
                        }
                    }
                } finally {
                    isSendingDirect = false;
                    delayedPackets.clear();
                }
            }
        }).start();

        // Restore or clear motion
        EntityPlayerSP player = getPlayer();
        if (player != null && previousState != State.NONE) {
            if (clearMotion.isEnabled()) {
                player.motionX = 0;
                player.motionY = 0;
                player.motionZ = 0;
            } else {
                player.motionX = motionX;
                player.motionY = motionY;
                player.motionZ = motionZ;
            }
        }
    }

    private boolean shouldStart(EntityPlayerSP player) {
        // Check Blink
        Module blinkModule = Main.getModuleManager() != null ?
            Main.getModuleManager().getModuleByName("Blink") : null;
        if (blinkModule != null && blinkModule.isEnabled()) return false;

        // Check onGround
        if (onlyOnGround.isEnabled() && !player.onGround) return false;

        // Check knockback
        if (notWhileKB.isEnabled() && player.hurtTime > 0) return false;

        // Check if moving
        if (!isMoving(player)) return false;

        // Check FOV setting
        if (fov.getValue() == 0) return false;

        // Check delay cooldown
        if (System.currentTimeMillis() - lastTimerTime < delay.getValue()) return false;

        // Find target in range
        EntityPlayer target = findTarget(player);
        if (target == null) return false;

        // Check FOV
        if (fov.getValue() < 360 && !inFov(player, target, (float) fov.getValue())) return false;

        // Check distance
        double distance = player.getDistanceToEntity(target);
        return distance >= minRange.getValue() && distance <= maxRange.getValue();
    }

    private boolean isMoving(EntityPlayerSP player) {
        return player.movementInput != null &&
               (player.movementInput.moveForward != 0 || player.movementInput.moveStrafe != 0);
    }

    private EntityPlayer findTarget(EntityPlayerSP player) {
        EntityPlayer closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer)) continue;
            EntityPlayer ep = (EntityPlayer) entity;

            if (ep == player) continue;
            if (!ep.isEntityAlive()) continue;

            if (Friends.isFriend(ep.getName())) continue;
            if (ignoreTeammates.isEnabled() && isTeammate(player, ep)) continue;

            double dist = player.getDistanceToEntity(ep);
            if (dist < closestDist) {
                closestDist = dist;
                closest = ep;
            }
        }

        return closest;
    }

    private boolean isTeammate(EntityPlayer player, EntityPlayer target) {
        try {
            ModuleManager manager = Main.getModuleManager();
            Module teams = manager != null ? manager.getModuleByName("Teams") : null;
            if (teams != null && teams.isEnabled() && teams instanceof Teams) {
                return ((Teams) teams).isTeamMate(target);
            }
            return player.isOnSameTeam(target);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean inFov(EntityPlayer player, EntityPlayer target, float fovAngle) {
        double deltaX = target.posX - player.posX;
        double deltaZ = target.posZ - player.posZ;
        double yawToTarget = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;

        double yawDiff = wrapAngle(player.rotationYaw - yawToTarget);
        return Math.abs(yawDiff) <= fovAngle / 2.0;
    }

    private double wrapAngle(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    public boolean isActive() {
        return state != State.NONE;
    }

    @Override
    public String getDisplaySuffix() {
        if (state == State.TIMER) return " §aTIMER";
        if (state == State.LAG) return " §cLAG";
        return " §7" + (int) timerTicks.getValue();
    }

    private enum State {
        NONE,
        TIMER,
        LAG
    }
}
