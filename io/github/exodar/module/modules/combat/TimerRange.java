/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.modules.player.Blink;
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
 * TimerRange - Uses timer to reach opponents at mid-range
 * Based on Raven XD implementation
 *
 * How it works:
 * 1. Detects target in configured range (3.6-5.0 blocks typically)
 * 2. TIMER state: Calls player onUpdate() multiple times to speed up tick rate
 * 3. LAG state: Freezes player for X ticks to stabilize
 * 4. Queues packets during both states, then releases them all at once
 */
public class TimerRange extends Module {

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
    private final TickSetting notWhileScaffold;

    private final Queue<Packet<?>> delayedPackets = new ConcurrentLinkedQueue<>();
    private State state = State.NONE;
    private int hasLag = 0;
    private long lastTimerTime = -1;

    // Saved motion during LAG state
    private float savedYaw, savedPitch;
    private double motionX, motionY, motionZ;

    // Flag to bypass packet hooks when releasing
    private static boolean isSendingDirect = false;

    public TimerRange() {
        super("TimerRange", ModuleCategory.COMBAT);
        this.registerSetting(new DescriptionSetting("Use timer to reach opponent"));
        this.registerSetting(lagTicks = new SliderSetting("Lag ticks", 2, 0, 10, 1));
        this.registerSetting(timerTicks = new SliderSetting("Timer ticks", 2, 0, 10, 1));
        this.registerSetting(minRange = new SliderSetting("Min range", 3.6, 0, 8, 0.1));
        this.registerSetting(maxRange = new SliderSetting("Max range", 5, 0, 8, 0.1));
        this.registerSetting(delay = new SliderSetting("Delay", 500, 0, 4000, 100));
        this.registerSetting(fov = new SliderSetting("FOV", 180, 0, 360, 30));
        this.registerSetting(ignoreTeammates = new TickSetting("Ignore teammates", true));
        this.registerSetting(onlyOnGround = new TickSetting("Only onGround", false));
        this.registerSetting(clearMotion = new TickSetting("Clear motion", false));
        this.registerSetting(notWhileKB = new TickSetting("Not while kb", false));
        this.registerSetting(notWhileScaffold = new TickSetting("Not while scaffold", true));
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
                for (int i = 0; i < timerTicks.getValue(); i++) {
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

                if (hasLag >= lagTicks.getValue()) {
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

        // CRITICAL: Never intercept packets that must stay in order
        if (packet instanceof C0FPacketConfirmTransaction ||
            packet instanceof C00PacketKeepAlive ||
            packet instanceof C01PacketChatMessage) {
            return true; // Always allow these through
        }

        switch (state) {
            case TIMER:
                // Queue movement packets during TIMER state
                if (packet instanceof C03PacketPlayer) {
                    synchronized (delayedPackets) {
                        delayedPackets.add((Packet<?>) packet);
                    }
                    return false; // Cancel sending
                }
                return true; // Allow other packets

            case LAG:
                // During LAG, only cancel C03PacketPlayer (movement) packets
                if (packet instanceof C03PacketPlayer) {
                    return false; // Cancel movement packets (don't queue, just drop)
                }
                return true; // Allow other packets

            default:
                return true; // Allow packet
        }
    }

    @Override
    public void onDisable() {
        done();
    }

    private void done() {
        State previousState = state;
        state = State.NONE;
        hasLag = 0;
        lastTimerTime = System.currentTimeMillis();

        // Release all queued packets
        synchronized (delayedPackets) {
            isSendingDirect = true;
            try {
                EntityPlayerSP player = getPlayer();
                if (player != null && player.sendQueue != null) {
                    int count = 0;
                    for (Packet<?> p : delayedPackets) {
                        if (p != null) {
                            player.sendQueue.addToSendQueue(p);
                            count++;
                        }
                    }
                    if (count > 0) {
                        System.out.println("[TimerRange] Released " + count + " packets");
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

        // Check scaffold (if we have it)
        if (notWhileScaffold.isEnabled()) {
            Module scaffoldModule = Main.getModuleManager() != null ?
                Main.getModuleManager().getModuleByName("Scaffold") : null;
            if (scaffoldModule != null && scaffoldModule.isEnabled()) return false;
        }

        // Check if player is moving
        if (!isMoving(player)) return false;

        // Check FOV setting
        if (fov.getValue() == 0) return false;

        // Check delay cooldown
        if (System.currentTimeMillis() - lastTimerTime < delay.getValue()) return false;

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

            // Skip self
            if (target == player) continue;

            // Skip teammates if configured
            if (ignoreTeammates.isEnabled() && isTeammate(player, target)) continue;

            // Check distance
            double distance = player.getDistanceToEntity(target);
            if (distance < minRange.getValue() || distance > maxRange.getValue()) continue;

            // Check FOV
            if (fov.getValue() < 360 && !inFov(player, target, (float) fov.getValue())) continue;

            // Track closest
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

    /**
     * Check if TimerRange is currently active (for other modules)
     */
    public boolean isActive() {
        return state != State.NONE;
    }

    /**
     * Check if we're sending packets directly (bypass hooks)
     */
    public static boolean isSendingDirectPacket() {
        return isSendingDirect;
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + (int) timerTicks.getValue();
    }

    private enum State {
        NONE,
        TIMER,
        LAG
    }
}
