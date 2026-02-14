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
import io.github.exodar.Main;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * TimerRangeTest - Combines TimerRange + LagRange
 *
 * How it works:
 * 1. Detects target in configured range
 * 2. TIMER state: Calls player onUpdate() multiple times (speed up)
 * 3. LAG state: Thread.sleep() + freeze motion for X ticks
 * 4. Queues packets during states, releases them at the end
 */
public class TimerRangeTest extends Module {

    // Timer settings
    private final SliderSetting timerTicks;
    private final SliderSetting lagTicks;
    private final SliderSetting lagTimeMs;

    // Range settings
    private final SliderSetting minRange;
    private final SliderSetting maxRange;
    private final SliderSetting delay;
    private final SliderSetting fov;

    // Options
    private final TickSetting ignoreTeammates;
    private final TickSetting onlyOnGround;
    private final TickSetting clearMotion;
    private final TickSetting notWhileKB;

    private final Queue<Packet<?>> delayedPackets = new ConcurrentLinkedQueue<>();
    private State state = State.NONE;
    private int currentLagTick = 0;
    private long lastActivationTime = -1;

    // Saved motion during LAG state
    private double motionX, motionY, motionZ;

    // Flag to bypass packet hooks when releasing
    private static boolean isSendingDirect = false;

    public TimerRangeTest() {
        super("TimerRangeTest", ModuleCategory.COMBAT);
        this.hidden = true; // Hidden from ClickGUI
        this.registerSetting(new DescriptionSetting("TimerRange + LagRange combined"));

        // Timer settings
        this.registerSetting(timerTicks = new SliderSetting("Timer ticks", 2, 0, 10, 1));
        this.registerSetting(lagTicks = new SliderSetting("Lag ticks", 2, 0, 10, 1));
        this.registerSetting(lagTimeMs = new SliderSetting("Lag time ms", 50, 0, 200, 10));

        // Range settings
        this.registerSetting(minRange = new SliderSetting("Min range", 3.6, 0, 8, 0.1));
        this.registerSetting(maxRange = new SliderSetting("Max range", 5, 0, 8, 0.1));
        this.registerSetting(delay = new SliderSetting("Delay", 500, 0, 4000, 100));
        this.registerSetting(fov = new SliderSetting("FOV", 180, 0, 360, 30));

        // Options
        this.registerSetting(ignoreTeammates = new TickSetting("Ignore teammates", true));
        this.registerSetting(onlyOnGround = new TickSetting("Only onGround", false));
        this.registerSetting(clearMotion = new TickSetting("Clear motion", false));
        this.registerSetting(notWhileKB = new TickSetting("Not while KB", false));
    }

    @Override
    public void onUpdate() {
        EntityPlayerSP player = getPlayer();
        if (player == null || getWorld() == null) {
            done();
            return;
        }

        switch (state) {
            case NONE:
                if (shouldStart()) {
                    state = State.TIMER;
                }
                break;

            case TIMER:
                // Speed up tick rate by calling onUpdate multiple times
                int ticks = (int) timerTicks.getValue();
                for (int i = 0; i < ticks; i++) {
                    player.onUpdate();
                }

                // Save current motion for LAG phase
                motionX = player.motionX;
                motionY = player.motionY;
                motionZ = player.motionZ;

                currentLagTick = 0;
                state = State.LAG;

                // Apply Thread.sleep lag (from LagRange)
                int sleepMs = (int) lagTimeMs.getValue();
                if (sleepMs > 0) {
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException e) {
                        // Interrupted
                    }
                }
                break;

            case LAG:
                // Freeze player motion during lag ticks
                player.motionX = motionX;
                player.motionY = motionY;
                player.motionZ = motionZ;

                if (currentLagTick >= lagTicks.getValue()) {
                    done();
                } else {
                    currentLagTick++;
                }
                break;
        }
    }

    /**
     * Called by Main.onSendPacket hook
     * @return true to allow packet, false to cancel
     */
    @Override
    public boolean onSendPacket(Object packet) {
        if (!enabled || isSendingDirect) return true;

        // Never intercept critical packets
        if (packet instanceof C0FPacketConfirmTransaction ||
            packet instanceof C00PacketKeepAlive ||
            packet instanceof C01PacketChatMessage) {
            return true;
        }

        switch (state) {
            case TIMER:
                // Queue movement packets during TIMER state
                if (packet instanceof C03PacketPlayer) {
                    synchronized (delayedPackets) {
                        delayedPackets.add((Packet<?>) packet);
                    }
                    return false;
                }
                return true;

            case LAG:
                // During LAG, cancel movement packets (don't queue)
                if (packet instanceof C03PacketPlayer) {
                    return false;
                }
                return true;

            default:
                return true;
        }
    }

    @Override
    public void onDisable() {
        done();
    }

    private void done() {
        State previousState = state;
        state = State.NONE;
        currentLagTick = 0;
        lastActivationTime = System.currentTimeMillis();

        // Release all queued packets
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

    private boolean shouldStart() {
        EntityPlayerSP player = getPlayer();
        if (player == null || getWorld() == null) return false;

        // Check if Blink is active
        Module blinkModule = Main.getModuleManager() != null ?
            Main.getModuleManager().getModuleByName("Blink") : null;
        if (blinkModule != null && blinkModule.isEnabled()) return false;

        // Check onGround requirement
        if (onlyOnGround.isEnabled() && !player.onGround) return false;

        // Check knockback
        if (notWhileKB.isEnabled() && player.hurtTime > 0) return false;

        // Check if player is moving
        if (!isMoving(player)) return false;

        // Check FOV setting
        if (fov.getValue() == 0) return false;

        // Check delay cooldown
        if (System.currentTimeMillis() - lastActivationTime < delay.getValue()) return false;

        // Find target in range
        EntityPlayer target = findTarget();
        return target != null;
    }

    private boolean isMoving(EntityPlayerSP player) {
        return player.movementInput != null &&
               (player.movementInput.moveForward != 0 || player.movementInput.moveStrafe != 0);
    }

    private EntityPlayer findTarget() {
        EntityPlayerSP player = getPlayer();
        if (player == null || getWorld() == null) return null;

        EntityPlayer closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Object obj : getWorld().playerEntities) {
            if (!(obj instanceof EntityPlayer)) continue;
            EntityPlayer target = (EntityPlayer) obj;

            if (target == player) continue;

            if (ignoreTeammates.isEnabled() && isTeammate(player, target)) continue;

            double distance = player.getDistanceToEntity(target);
            if (distance < minRange.getValue() || distance > maxRange.getValue()) continue;

            if (fov.getValue() < 360 && !inFov(player, target, (float) fov.getValue())) continue;

            if (distance < closestDist) {
                closestDist = distance;
                closest = target;
            }
        }

        return closest;
    }

    private boolean isTeammate(EntityPlayer player, EntityPlayer target) {
        if (player.getTeam() == null || target.getTeam() == null) return false;
        return player.getTeam().isSameTeam(target.getTeam());
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

    public static boolean isSendingDirectPacket() {
        return isSendingDirect;
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + (int) timerTicks.getValue() + "/" + (int) lagTicks.getValue();
    }

    private enum State {
        NONE,
        TIMER,
        LAG
    }
}
