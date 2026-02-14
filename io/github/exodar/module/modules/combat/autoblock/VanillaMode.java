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

/**
 * VANILLA MODE - Simple blocking using KeyBinding simulation
 *
 * Based on Raven XD's Vanilla autoblock mode.
 * Uses KeyBinding to simulate real mouse input (bypasses Vulcan Badpackets).
 */
public class VanillaMode extends BaseMode {

    public VanillaMode(AutoBlock parent) {
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
        boolean hasTarget = target != null;
        boolean holdingSword = player.getHeldItem() != null && player.getHeldItem().getItem() instanceof ItemSword;

        // Update block animation (shows fake block when idle with target)
        updateBlockAnimation(hasTarget && holdingSword);

        if (hasTarget && holdingSword && state == STATE_BLOCKING) {
            // Start blocking using KeyBinding simulation
            if (!blockingStarted) {
                startBlocking();
            }
        } else if (blockingStarted) {
            releaseBlock();
            blockingStarted = false;
            state = STATE_IDLE;
        }

        // Check hold duration
        if (state == STATE_BLOCKING) {
            long elapsed = now - stateStartTime;
            if (elapsed >= (long) parent.getHoldDuration()) {
                releaseBlock();
                blockingStarted = false;
                state = STATE_IDLE;
                stateStartTime = now;
            }
        }
    }

    @Override
    public boolean onAttack(Packet<?> attackPacket) {
        // Start blocking after hit
        if (state == STATE_IDLE) {
            onNotifyAttack();
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
