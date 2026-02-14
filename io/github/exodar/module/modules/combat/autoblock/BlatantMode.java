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
 * BLATANT MODE - Constant blocking using playerController
 *
 * Uses mc.playerController.sendUseItem() and onStoppedUsingItem()
 * More reliable blocking but more detectable
 */
public class BlatantMode extends BaseMode {

    private volatile boolean isBlocking = false;

    public BlatantMode(AutoBlock parent) {
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
        stopBlocking();
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
                // Constant blocking using playerController
                if (!isBlocking) {
                    doBlock();
                }
                // Don't clear fake animation - let real block show naturally
                if (player.isBlocking()) {
                    setFakeAnimation(false);
                }

                // After holdDuration, release and go idle
                if (elapsed >= holdMs) {
                    stopBlocking();
                    state = STATE_IDLE;
                    stateStartTime = now;
                }
                break;
        }
    }

    @Override
    public boolean onAttack(Packet<?> attackPacket) {
        // Stop blocking momentarily to allow attack
        if (isBlocking) {
            stopBlocking();
        }

        // Start blocking again after hit
        if (state == STATE_IDLE) {
            onNotifyAttack();
        } else if (state == STATE_BLOCKING) {
            // Re-block after attack
            new Thread(() -> {
                try {
                    Thread.sleep(1);
                    doBlock();
                } catch (Exception e) {}
            }).start();
        }

        return true;
    }

    @Override
    public void onNotifyAttack() {
        if (state == STATE_BLOCKING) return;

        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        // Only block if there's a valid target in range
        // This prevents blocking when hitting air
        if (mc.objectMouseOver == null ||
            mc.objectMouseOver.entityHit == null ||
            !(mc.objectMouseOver.entityHit instanceof EntityLivingBase)) {
            return;
        }

        // Start blocking
        doBlock();
        state = STATE_BLOCKING;
        stateStartTime = System.currentTimeMillis();
    }

    @Override
    public boolean shouldSendPacket(Packet<?> packet) {
        // Blatant mode doesn't intercept packets
        return true;
    }

    /**
     * Start blocking using playerController
     */
    private void doBlock() {
        if (isBlocking) return;

        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null) return;

        try {
            if (player.inventory.getCurrentItem() != null) {
                mc.playerController.sendUseItem(player, mc.theWorld, player.inventory.getCurrentItem());
                isBlocking = true;
                blockingStarted = true;
            }
        } catch (Exception e) {
            // Fallback to KeyBinding method
            pressBlock();
            isBlocking = true;
            blockingStarted = true;
        }
    }

    /**
     * Stop blocking using playerController
     */
    private void stopBlocking() {
        if (!isBlocking) return;

        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        try {
            mc.playerController.onStoppedUsingItem(player);
        } catch (Exception e) {
            // Fallback to KeyBinding method
            releaseBlock();
        }

        isBlocking = false;
        blockingStarted = false;
    }

    @Override
    protected void resetToIdle() {
        stopBlocking();
        super.resetToIdle();
    }

    @Override
    public boolean isBlocking() {
        return isBlocking || state == STATE_BLOCKING;
    }

    @Override
    public String getStateName() {
        return isBlocking ? "Block" : "Idle";
    }
}
