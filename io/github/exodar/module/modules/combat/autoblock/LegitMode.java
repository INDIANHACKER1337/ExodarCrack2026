/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat.autoblock;

import io.github.exodar.module.modules.combat.AutoBlock;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;

/**
 * LEGIT MODE - Simple hit -> block -> idle
 *
 * Flow:
 *   IDLE -> HIT detected -> BLOCKING (holdDuration ms) -> IDLE
 *
 * Clean and simple, compatible with Teams module
 */
public class LegitMode extends BaseMode {

    public LegitMode(AutoBlock parent) {
        super(parent);
    }

    @Override
    public void onEnable() {
        state = STATE_IDLE;
        stateStartTime = System.currentTimeMillis();
        blockingStarted = false;
    }

    @Override
    public void onDisable() {
        resetToIdle();
    }

    @Override
    public void onTick(EntityPlayerSP player, EntityLivingBase target, long now) {
        long elapsed = now - stateStartTime;
        long holdMs = (long) parent.getHoldDuration();
        boolean hasTarget = target != null;

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
                    startBlocking();
                }
                // Don't clear fake animation - let real block show naturally
                // Only clear if player is actually blocking
                if (player.isBlocking()) {
                    setFakeAnimation(false);
                }

                // After holdDuration, release and go idle
                if (elapsed >= holdMs) {
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
        // Start blocking after hit
        if (state == STATE_IDLE) {
            onNotifyAttack();
        }
        return true; // Always allow attack packet
    }

    @Override
    public void onNotifyAttack() {
        if (state != STATE_IDLE) return;

        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        // CRITICAL: Must be holding sword - prevents eating animation when holding food
        net.minecraft.item.ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof net.minecraft.item.ItemSword)) {
            return;
        }

        // Only block if there's a valid target in range
        // This prevents blocking when hitting air
        if (mc.objectMouseOver == null ||
            mc.objectMouseOver.entityHit == null ||
            !(mc.objectMouseOver.entityHit instanceof EntityLivingBase)) {
            return;
        }

        // Start blocking
        startBlocking();
        state = STATE_BLOCKING;
        stateStartTime = System.currentTimeMillis();
    }

    @Override
    public boolean shouldSendPacket(Packet<?> packet) {
        // Legit mode doesn't intercept any packets
        return true;
    }
}
