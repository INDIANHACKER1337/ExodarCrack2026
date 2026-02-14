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
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;

import java.util.Random;

/**
 * QUICKMACRO MODE - Uses C0FPacketConfirmTransaction + animation + block
 *
 * WARNING: This mode sends packets directly and will flag on Vulcan/other ACs.
 * Only use on servers with weak anticheat.
 *
 * Based on Raven XD's QuickMacro autoblock mode.
 * Sends confirm transaction packet with random values,
 * then animation packet, then block packet.
 */
public class QuickMacroMode extends BaseMode {

    private boolean isBlocking = false;
    private final Random random = new Random();

    public QuickMacroMode(AutoBlock parent) {
        super(parent);
    }

    @Override
    public void onEnable() {
        state = STATE_IDLE;
        stateStartTime = System.currentTimeMillis();
        blockingStarted = false;
        isBlocking = false;
    }

    @Override
    public void onDisable() {
        isBlocking = false;
        resetToIdle();
    }

    @Override
    public void onTick(EntityPlayerSP player, EntityLivingBase target, long now) {
        boolean hasTarget = target != null;
        boolean holdingSword = player.getHeldItem() != null && player.getHeldItem().getItem() instanceof ItemSword;

        // Update block animation (shows fake block when idle with target)
        updateBlockAnimation(hasTarget && holdingSword);

        if (!holdingSword) {
            isBlocking = false;
            state = STATE_IDLE;
            return;
        }

        if (state == STATE_BLOCKING && hasTarget) {
            // Check hold duration
            long elapsed = now - stateStartTime;
            if (elapsed >= (long) parent.getHoldDuration()) {
                isBlocking = false;
                state = STATE_IDLE;
                stateStartTime = now;
            }
        } else if (!hasTarget) {
            isBlocking = false;
            state = STATE_IDLE;
        }
    }

    private void sendQuickMacroBlock() {
        if (mc.thePlayer == null || mc.getNetHandler() == null) return;

        // Send confirm transaction with random values
        int windowId = random.nextInt(Integer.MAX_VALUE);
        short actionNumber = (short) random.nextInt(Short.MAX_VALUE + 1);
        mc.getNetHandler().addToSendQueue(new C0FPacketConfirmTransaction(windowId, actionNumber, true));

        // Send animation
        mc.getNetHandler().addToSendQueue(new C0APacketAnimation());

        // Send block
        mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));

        isBlocking = true;
    }

    @Override
    public boolean onAttack(Packet<?> attackPacket) {
        // Do QuickMacro block after attack
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return true;

        // Must be holding sword
        if (player.getHeldItem() == null || !(player.getHeldItem().getItem() instanceof ItemSword)) {
            return true;
        }

        sendQuickMacroBlock();
        state = STATE_BLOCKING;
        stateStartTime = System.currentTimeMillis();

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

        sendQuickMacroBlock();
        state = STATE_BLOCKING;
        stateStartTime = System.currentTimeMillis();
    }

    @Override
    public boolean shouldSendPacket(Packet<?> packet) {
        return true;
    }

    @Override
    public boolean isBlocking() {
        return isBlocking;
    }
}
