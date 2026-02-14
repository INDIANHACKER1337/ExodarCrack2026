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
import net.minecraft.network.play.client.C02PacketUseEntity;

/**
 * INTERACT MODE - Uses C02PacketUseEntity interact packets + KeyBinding block
 *
 * Based on Raven XD's Interact autoblock modes.
 * Sends interact packet on attack, uses KeyBinding for blocking (Vulcan safe).
 */
public class InteractMode extends BaseMode {

    private EntityLivingBase lastTarget = null;

    public InteractMode(AutoBlock parent) {
        super(parent);
    }

    @Override
    public void onEnable() {
        state = STATE_IDLE;
        stateStartTime = System.currentTimeMillis();
        blockingStarted = false;
        lastTarget = null;
    }

    @Override
    public void onDisable() {
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
            if (blockingStarted) {
                releaseBlock();
                blockingStarted = false;
            }
            state = STATE_IDLE;
            return;
        }

        if (state == STATE_BLOCKING) {
            // Start blocking using KeyBinding
            if (!blockingStarted) {
                startBlocking();
            }

            // Check hold duration
            long elapsed = now - stateStartTime;
            if (elapsed >= (long) parent.getHoldDuration()) {
                releaseBlock();
                blockingStarted = false;
                state = STATE_IDLE;
                stateStartTime = now;
            }
        }
    }

    private void sendInteract(EntityLivingBase target) {
        if (mc.thePlayer == null || target == null) return;
        mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(target, C02PacketUseEntity.Action.INTERACT));
    }

    @Override
    public boolean onAttack(Packet<?> attackPacket) {
        // Start blocking after hit
        if (state == STATE_IDLE) {
            onNotifyAttack();
        }

        // Send interact packet after attack (the "interact" part of this mode)
        if (lastTarget != null) {
            sendInteract(lastTarget);
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
        return true;
    }

    @Override
    public boolean isBlocking() {
        return blockingStarted;
    }
}
