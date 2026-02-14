/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.player;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.TickSetting;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.ModeSetting;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.handshake.client.C00Handshake;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * FakeLag - Based on Raven XD
 *
 * Modes:
 * - Latency: Delays all outgoing packets by X ms (simple fake ping)
 * - Dynamic: Activates based on target distance, stops on hurt
 */
public class FakeLag extends Module {

    // Mode
    private final ModeSetting mode;

    // Latency mode settings
    private final SliderSetting delay;

    // Dynamic mode settings
    private final SliderSetting startRange;
    private final SliderSetting stopRange;
    private final SliderSetting maxTargetRange;
    private final TickSetting ignoreTeammates;
    private final TickSetting stopOnHurt;
    private final SliderSetting stopOnHurtTime;

    // Packet queue with timestamps
    private final Queue<TimedPacket> packetQueue = new ConcurrentLinkedQueue<>();

    // Dynamic mode state
    private EntityPlayer target = null;
    private long lastDisableTime = -1;
    private boolean wasHurt = false;
    private boolean isLagging = false;
    private long lagStartTime = -1;

    // Flag to bypass packet hooks when releasing
    private static boolean isSendingDirect = false;

    public FakeLag() {
        super("FakeLag", ModuleCategory.PLAYER);
        this.registerSetting(new DescriptionSetting("Delay outgoing packets"));

        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Latency", "Dynamic"})
            .onChange(this::onModeChange));

        // Latency mode
        this.registerSetting(delay = new SliderSetting("Delay", 200, 25, 1000, 25));

        // Dynamic mode
        this.registerSetting(startRange = new SliderSetting("Start Range", 6.0, 3.0, 10.0, 0.5));
        this.registerSetting(stopRange = new SliderSetting("Stop Range", 3.5, 1.0, 6.0, 0.5));
        this.registerSetting(maxTargetRange = new SliderSetting("Max Target Range", 15.0, 6.0, 20.0, 1.0));
        this.registerSetting(ignoreTeammates = new TickSetting("Ignore Teammates", true));
        this.registerSetting(stopOnHurt = new TickSetting("Stop on Hurt", true));
        this.registerSetting(stopOnHurtTime = new SliderSetting("Hurt Cooldown", 500, 0, 1000, 50));
    }

    private void onModeChange() {
        System.out.println("[FakeLag] Mode changed to: " + mode.getSelected());
        releaseAllPackets();
        target = null;
        isLagging = false;
        updateSettingVisibility();
    }

    @Override
    public void onEnable() {
        packetQueue.clear();
        target = null;
        lastDisableTime = -1;
        wasHurt = false;
        isLagging = false;
        lagStartTime = -1;
    }

    @Override
    public void onDisable() {
        releaseAllPackets();
        target = null;
        isLagging = false;
    }

    @Override
    public void onUpdate() {
        EntityPlayerSP player = getPlayer();
        if (player == null || getWorld() == null) {
            releaseAllPackets();
            return;
        }

        // Update setting visibility
        updateSettingVisibility();

        String currentMode = mode.getSelected();

        if (currentMode.equals("Latency")) {
            handleLatencyMode();
        } else {
            handleDynamicMode(player);
        }
    }

    private void handleLatencyMode() {
        // Release packets that have been delayed long enough
        long now = System.currentTimeMillis();
        releaseOldPackets(now, (long) delay.getValue());
    }

    private void handleDynamicMode(EntityPlayerSP player) {
        long now = System.currentTimeMillis();

        // Cooldown after being hurt
        if (now - lastDisableTime <= stopOnHurtTime.getValue()) {
            if (isLagging) {
                stopLagging();
            }
            return;
        }

        // Check hurt status
        if (stopOnHurt.isEnabled() && !wasHurt && player.hurtTime > 0) {
            lastDisableTime = now;
            stopLagging();
        }
        wasHurt = player.hurtTime > 0;

        // Find target if we don't have one
        if (target == null) {
            target = findNearestPlayer(player);
        }

        if (target != null) {
            double distance = player.getDistanceToEntity(target);

            // Target too far, release
            if (distance > maxTargetRange.getValue()) {
                target = null;
                stopLagging();
                return;
            }

            // Check timeout
            if (isLagging && now - lagStartTime > delay.getValue()) {
                stopLagging();
                lagStartTime = now;
            }

            // Start lagging if in start range
            if (!isLagging && distance > stopRange.getValue() && distance < startRange.getValue()) {
                isLagging = true;
                lagStartTime = now;
            }

            // Stop lagging if too close
            if (isLagging && distance < stopRange.getValue()) {
                stopLagging();
            }

            // Stop lagging if too far
            if (isLagging && distance > startRange.getValue()) {
                stopLagging();
            }
        } else {
            stopLagging();
        }

        // Release old packets in dynamic mode too
        if (!isLagging) {
            releaseAllPackets();
        }
    }

    private void stopLagging() {
        isLagging = false;
        releaseAllPackets();
    }

    /**
     * Called by packet hook
     */
    @Override
    public boolean onSendPacket(Object packet) {
        if (!enabled || isSendingDirect) return true;

        EntityPlayerSP player = getPlayer();
        if (player == null) return true;

        // Never delay critical packets
        if (packet instanceof C00Handshake ||
            packet instanceof C01PacketChatMessage ||
            packet instanceof C0FPacketConfirmTransaction ||
            packet instanceof C00PacketKeepAlive) {
            return true;
        }

        String currentMode = mode.getSelected();

        if (currentMode.equals("Latency")) {
            // Always queue in latency mode
            packetQueue.add(new TimedPacket((Packet<?>) packet, System.currentTimeMillis()));
            return false;
        } else {
            // Dynamic mode - only queue if lagging
            if (isLagging) {
                packetQueue.add(new TimedPacket((Packet<?>) packet, System.currentTimeMillis()));
                return false;
            }
            return true;
        }
    }

    private void releaseOldPackets(long now, long maxDelay) {
        isSendingDirect = true;
        try {
            EntityPlayerSP player = getPlayer();
            if (player == null || player.sendQueue == null) return;

            while (!packetQueue.isEmpty()) {
                TimedPacket tp = packetQueue.peek();
                if (tp == null) {
                    packetQueue.poll();
                    continue;
                }

                if (now - tp.timestamp >= maxDelay) {
                    packetQueue.poll();
                    if (tp.packet != null) {
                        player.sendQueue.addToSendQueue(tp.packet);
                    }
                } else {
                    break;
                }
            }
        } finally {
            isSendingDirect = false;
        }
    }

    private void releaseAllPackets() {
        isSendingDirect = true;
        try {
            EntityPlayerSP player = getPlayer();
            if (player == null || player.sendQueue == null) {
                packetQueue.clear();
                return;
            }

            while (!packetQueue.isEmpty()) {
                TimedPacket tp = packetQueue.poll();
                if (tp != null && tp.packet != null) {
                    player.sendQueue.addToSendQueue(tp.packet);
                }
            }
        } finally {
            isSendingDirect = false;
        }
    }

    private EntityPlayer findNearestPlayer(EntityPlayerSP player) {
        EntityPlayer closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Object obj : getWorld().playerEntities) {
            if (!(obj instanceof EntityPlayer)) continue;
            EntityPlayer target = (EntityPlayer) obj;

            if (target == player) continue;
            if (target.isDead) continue;

            if (ignoreTeammates.isEnabled() && isTeammate(player, target)) continue;

            double distance = player.getDistanceToEntity(target);
            if (distance > maxTargetRange.getValue()) continue;

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

    private void updateSettingVisibility() {
        boolean isDynamic = mode.getSelected().equals("Dynamic");

        startRange.setVisible(isDynamic);
        stopRange.setVisible(isDynamic);
        maxTargetRange.setVisible(isDynamic);
        ignoreTeammates.setVisible(isDynamic);
        stopOnHurt.setVisible(isDynamic);
        stopOnHurtTime.setVisible(isDynamic);
    }

    public static boolean isSendingDirectPacket() {
        return isSendingDirect;
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + mode.getSelected();
    }

    // Simple class to hold packet with timestamp
    private static class TimedPacket {
        final Packet<?> packet;
        final long timestamp;

        TimedPacket(Packet<?> packet, long timestamp) {
            this.packet = packet;
            this.timestamp = timestamp;
        }
    }
}
