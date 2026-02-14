/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat.autoblock;

import io.github.exodar.module.modules.combat.AutoBlock;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import net.minecraft.util.ChatComponentText;

import java.util.Deque;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * LAG MODE (HypixelBlink style) - Proper packet queueing implementation
 *
 * Based on Flux client's approach:
 * 1. When attacking while blocking, start a "blink" period (~76ms)
 * 2. During blink, ALL outgoing packets get queued
 * 3. After blink expires, ALL packets flush at once
 * 4. Server receives: unblock + attack + reblock nearly simultaneously
 *
 * This creates the effect of appearing to block continuously from other players' perspective.
 */
public class LagMode extends BaseMode {

    // Packet queue for blink
    private final Deque<Packet<?>> packetQueue = new ConcurrentLinkedDeque<>();

    // Blink timing (like Flux's outboundBlinkDuration)
    private volatile long blinkEndTime = 0;
    private volatile long lastPacketTime = 0;

    // Flags
    private static volatile boolean isSendingDirect = false;
    private volatile boolean isBlinking = false;

    private final Random random = new Random();

    public LagMode(AutoBlock parent) {
        super(parent);
    }

    private void debug(String msg) {
        if (parent.isLagDebugEnabled() && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§7[§cAB-Lag§7] §f" + msg));
        }
    }

    @Override
    public void onEnable() {
        state = STATE_IDLE;
        stateStartTime = System.currentTimeMillis();
        blockingStarted = false;
        isSendingDirect = false;
        isBlinking = false;
        blinkEndTime = 0;
        packetQueue.clear();
    }

    @Override
    public void onDisable() {
        // Flush any remaining packets before disabling
        flushPackets();
        resetToIdle();
        isSendingDirect = false;
        isBlinking = false;
    }

    @Override
    public void onTick(EntityPlayerSP player, EntityLivingBase target, long now) {
        long holdMs = (long) parent.getHoldDuration();
        boolean hasTarget = target != null;

        // Check if blink period has ended
        if (isBlinking && now >= blinkEndTime) {
            flushPackets();
            isBlinking = false;
        }

        switch (state) {
            case STATE_IDLE:
                // Waiting for hit - show fake animation if target nearby
                if (parent.isBlockAnimationEnabled() && hasTarget) {
                    setFakeAnimation(true);
                } else {
                    setFakeAnimation(false);
                }
                break;

            case STATE_BLOCKING:
                // Actually blocking
                if (!blockingStarted) {
                    debug("§eStarting block from tick");
                    startBlocking();
                }
                if (player.isBlocking()) {
                    setFakeAnimation(false);
                }

                // After holdDuration, release and go idle
                long elapsed = now - stateStartTime;
                if (elapsed >= holdMs) {
                    debug("§7Hold expired, releasing block");
                    releaseBlock();
                    blockingStarted = false;
                    state = STATE_IDLE;
                    stateStartTime = now;
                }
                break;
        }
    }

    @Override
    public boolean onAttack(Packet<?> attackPacket) {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return true;

        // Must be holding sword
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemSword)) {
            debug("§cNo sword held");
            return true;
        }

        debug("§7onAttack: state=" + state + ", isBlocking=" + player.isBlocking() + ", blockingStarted=" + blockingStarted);

        // If currently blocking -> do the blink attack sequence
        if (state == STATE_BLOCKING && player.isBlocking()) {
            // Start blink period - queue ALL packets for lagDuration ms
            long lagMs = (long) parent.getLagDuration();
            // Add small randomization like Flux does (26 + random(6))
            lagMs += 26 + random.nextInt(6);

            blinkEndTime = System.currentTimeMillis() + lagMs;
            lastPacketTime = System.nanoTime();
            isBlinking = true;

            debug("§aBlink START §7- " + lagMs + "ms");

            // Now send the blink sequence - these packets will be queued
            sendBlinkSequence(player, attackPacket);

            // Reset blocking state timer
            stateStartTime = System.currentTimeMillis();
            blockingStarted = true;

            return false; // We handled the attack packet
        }

        // If idle OR blocking but player.isBlocking() is false, start blocking
        if (state == STATE_IDLE || (state == STATE_BLOCKING && !player.isBlocking())) {
            // Only block if there's a valid target
            if (mc.objectMouseOver == null ||
                mc.objectMouseOver.entityHit == null ||
                !(mc.objectMouseOver.entityHit instanceof EntityLivingBase)) {
                debug("§cNo valid target");
                return true;
            }

            debug("§eStarting block from attack");
            startBlocking();
            state = STATE_BLOCKING;
            stateStartTime = System.currentTimeMillis();
        }

        return true;
    }

    /**
     * Send the blink sequence: unblock -> attack -> reblock
     * These packets get queued during blink period
     */
    private void sendBlinkSequence(EntityPlayerSP player, Packet<?> attackPacket) {
        if (player.sendQueue == null) return;

        // 1. Create UNBLOCK packet
        C07PacketPlayerDigging unblockPacket = new C07PacketPlayerDigging(
            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
            BlockPos.ORIGIN,
            EnumFacing.DOWN
        );

        // 2. Create REBLOCK packet
        ItemStack heldItem = player.getHeldItem();
        C08PacketPlayerBlockPlacement reblockPacket = null;
        if (heldItem != null && heldItem.getItem() instanceof ItemSword) {
            reblockPacket = new C08PacketPlayerBlockPlacement(
                new BlockPos(-1, -1, -1),
                255,  // Special face value for "use item"
                heldItem,
                0.0f, 0.0f, 0.0f
            );
        }

        // 3. Queue all packets in order: unblock -> attack -> reblock
        // These will be sent through shouldSendPacket which queues them during blink
        packetQueue.add(unblockPacket);
        packetQueue.add(attackPacket);
        if (reblockPacket != null) {
            packetQueue.add(reblockPacket);
        }

        // Keep visual animation during blink
        setFakeAnimation(true);
    }

    /**
     * Flush all queued packets at once
     */
    private void flushPackets() {
        if (packetQueue.isEmpty()) return;

        EntityPlayerSP player = mc.thePlayer;
        if (player == null || player.sendQueue == null) {
            packetQueue.clear();
            return;
        }

        int count = packetQueue.size();
        isSendingDirect = true;
        try {
            while (!packetQueue.isEmpty()) {
                Packet<?> packet = packetQueue.poll();
                if (packet != null) {
                    player.sendQueue.addToSendQueue(packet);
                }
            }
        } finally {
            isSendingDirect = false;
        }
        debug("§cBlink END §7- Flushed " + count + " packets");
    }

    @Override
    public void onNotifyAttack() {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        // Must be holding sword
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemSword)) {
            return;
        }

        // If idle, start blocking (like Legit mode)
        if (state == STATE_IDLE) {
            // Only block if there's a valid target
            if (mc.objectMouseOver == null ||
                mc.objectMouseOver.entityHit == null ||
                !(mc.objectMouseOver.entityHit instanceof EntityLivingBase)) {
                return;
            }

            startBlocking();
            state = STATE_BLOCKING;
            stateStartTime = System.currentTimeMillis();
        }
    }

    @Override
    public boolean shouldSendPacket(Packet<?> packet) {
        // Don't intercept if we're sending directly (flushing)
        if (isSendingDirect) return true;

        // Don't intercept critical packets
        if (packet instanceof C00PacketKeepAlive ||
            packet instanceof C01PacketChatMessage ||
            packet instanceof C0FPacketConfirmTransaction) {
            return true;
        }

        // If blinking, queue the packet
        if (isBlinking) {
            // Update blink duration like Flux does (decrement by time since last packet)
            long now = System.nanoTime();
            long deltaMs = (now - lastPacketTime) / 1_000_000L;
            lastPacketTime = now;

            // Check if blink has expired
            if (System.currentTimeMillis() >= blinkEndTime) {
                isBlinking = false;
                flushPackets();
                return true; // Allow this packet through
            }

            // Queue the packet
            String pktName = packet.getClass().getSimpleName();
            debug("§7Queued: " + pktName + " (total: " + (packetQueue.size() + 1) + ")");
            packetQueue.add(packet);
            return false; // Block the packet
        }

        return true;
    }

    @Override
    public int getQueueSize() {
        return packetQueue.size();
    }

    @Override
    public boolean isLagging() {
        return isBlinking;
    }

    public static boolean isSendingBufferedPacket() {
        return isSendingDirect;
    }
}
