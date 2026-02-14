/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat.autoblock;

import io.github.exodar.module.modules.combat.AutoBlock;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * HYPIXEL MODE - Hypixel-specific bypass using interact + blink
 *
 * WARNING: This mode sends packets directly and will flag on Vulcan/other ACs.
 * Only use on Hypixel servers.
 *
 * Based on Raven XD's Hypixel autoblock mode.
 * Uses interact packets with hit vector and delays (blinks) packets.
 */
public class HypixelMode extends BaseMode {

    private boolean isBlocking = false;
    private boolean blinking = false;
    private EntityLivingBase lastTarget = null;
    private final Queue<Packet<?>> blinkedPackets = new ConcurrentLinkedQueue<>();

    public HypixelMode(AutoBlock parent) {
        super(parent);
    }

    @Override
    public void onEnable() {
        state = STATE_IDLE;
        stateStartTime = System.currentTimeMillis();
        blockingStarted = false;
        isBlocking = false;
        blinking = false;
        blinkedPackets.clear();
    }

    @Override
    public void onDisable() {
        releaseBlinkPackets();
        if (isBlocking) {
            sendUnblock();
            setFakeAnimation(false);
        }
        isBlocking = false;
        blinking = false;
        resetToIdle();
    }

    @Override
    public void onTick(EntityPlayerSP player, EntityLivingBase target, long now) {
        boolean hasTarget = target != null;
        boolean holdingSword = player.getHeldItem() != null && player.getHeldItem().getItem() instanceof ItemSword;

        lastTarget = target;

        // Update block animation (shows fake block when idle with target)
        updateBlockAnimation(hasTarget && holdingSword);

        if (!hasTarget || !holdingSword) {
            releaseBlinkPackets();
            if (isBlocking) {
                sendUnblock();
                setFakeAnimation(false);
                isBlocking = false;
            }
            blinking = false;
            state = STATE_IDLE;
            return;
        }

        if (state == STATE_BLOCKING) {
            // First tick of blocking - send block packet ONCE
            if (!isBlocking) {
                sendBlockPacket();
                setFakeAnimation(true); // Show animation without pressing key (avoids PacketOrderI)
                isBlocking = true;
                blinking = true; // Start capturing packets
            }

            // Check hold duration - when expired, unblock ONCE and return to idle
            long elapsed = now - stateStartTime;
            if (elapsed >= (long) parent.getHoldDuration()) {
                releaseBlinkPackets();
                sendUnblock();
                setFakeAnimation(false);
                isBlocking = false;
                blinking = false;
                state = STATE_IDLE;
                stateStartTime = now;
            }
        }
    }

    private void sendBlockPacket() {
        if (mc.thePlayer == null) return;
        mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
    }

    private void sendUnblock() {
        if (mc.thePlayer == null) return;
        mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
            BlockPos.ORIGIN,
            EnumFacing.DOWN
        ));
    }

    private void sendInteractAt(EntityLivingBase target) {
        if (mc.thePlayer == null || target == null) return;

        // Get eye position as hit vector
        Vec3 eyePos = new Vec3(
            mc.thePlayer.posX,
            mc.thePlayer.posY + mc.thePlayer.getEyeHeight(),
            mc.thePlayer.posZ
        );

        // Create relative hit vec
        Vec3 hitVec = new Vec3(
            eyePos.xCoord - target.posX,
            eyePos.yCoord - target.posY,
            eyePos.zCoord - target.posZ
        );

        mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(target, hitVec));
        mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(target, C02PacketUseEntity.Action.INTERACT));
    }

    private void releaseBlinkPackets() {
        while (!blinkedPackets.isEmpty()) {
            Packet<?> packet = blinkedPackets.poll();
            if (packet != null && mc.getNetHandler() != null) {
                mc.getNetHandler().addToSendQueue(packet);
            }
        }
        blinking = false;
    }

    @Override
    public boolean onAttack(Packet<?> attackPacket) {
        // Start blocking after hit
        if (state == STATE_IDLE) {
            onNotifyAttack();
        }

        // Send interact at packet after attack
        if (lastTarget != null) {
            sendInteractAt(lastTarget);
        }

        return true;
    }

    @Override
    public void onNotifyAttack() {
        if (state != STATE_IDLE) return;

        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        // Must be holding sword
        if (player.getHeldItem() == null || !(player.getHeldItem().getItem() instanceof ItemSword)) {
            return;
        }

        state = STATE_BLOCKING;
        stateStartTime = System.currentTimeMillis();
    }

    @Override
    public boolean shouldSendPacket(Packet<?> packet) {
        // When blinking, capture certain packets
        if (blinking) {
            // Don't capture keep alive, chat, or transaction packets
            if (packet instanceof C00PacketKeepAlive ||
                packet instanceof C01PacketChatMessage ||
                packet instanceof C0FPacketConfirmTransaction) {
                return true;
            }

            // Capture movement and action packets
            if (packet instanceof C03PacketPlayer ||
                packet instanceof C0APacketAnimation ||
                packet instanceof C0BPacketEntityAction) {
                blinkedPackets.add(packet);
                return false; // Cancel packet, store for later
            }
        }
        return true;
    }

    @Override
    public boolean isBlocking() {
        return isBlocking;
    }

    @Override
    public boolean isLagging() {
        return blinking;
    }

    @Override
    public int getQueueSize() {
        return blinkedPackets.size();
    }
}
