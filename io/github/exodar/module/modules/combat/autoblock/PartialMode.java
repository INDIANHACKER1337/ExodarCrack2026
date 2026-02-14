/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat.autoblock;

import io.github.exodar.module.modules.combat.AutoBlock;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * PARTIAL MODE - Only blocks when target's hurtTime >= 5
 *
 * Based on Raven XD's Partial autoblock mode.
 * Very legit looking as it only blocks when enemy is in hurt animation.
 * Uses physical key press to simulate real blocking.
 */
public class PartialMode extends BaseMode {

    private boolean isBlocking = false;
    private Field pressedField = null;
    private Method setKeyBindStateMethod = null;

    public PartialMode(AutoBlock parent) {
        super(parent);
        findReflectionFields();
    }

    private void findReflectionFields() {
        try {
            // Find pressed field in KeyBinding
            String[] names = {"pressed", "field_74513_e"};
            for (String name : names) {
                try {
                    Field f = KeyBinding.class.getDeclaredField(name);
                    f.setAccessible(true);
                    pressedField = f;
                    break;
                } catch (NoSuchFieldException ignored) {}
            }
        } catch (Exception e) {}
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
        // Only release if we were actually blocking
        if (isBlocking) {
            setBlockKeyState(false);
            isBlocking = false;
        }
        resetToIdle();
    }

    @Override
    public void onTick(EntityPlayerSP player, EntityLivingBase target, long now) {
        boolean holdingSword = player.getHeldItem() != null && player.getHeldItem().getItem() instanceof ItemSword;
        boolean hasTarget = target != null;

        // Update block animation (shows fake block when idle with target)
        updateBlockAnimation(hasTarget && holdingSword);

        if (!holdingSword) {
            if (isBlocking) {
                setBlockKeyState(false);
                isBlocking = false;
            }
            state = STATE_IDLE;
            return;
        }

        // Partial mode logic: only block when target is in hurt animation
        boolean shouldBlock = false;
        if (target != null) {
            // Block when target's hurtTime >= 5 (they just got hit)
            shouldBlock = target.hurtTime >= 5;
        }

        // Also block if we're in blocking state
        if (state == STATE_BLOCKING) {
            shouldBlock = true;

            // Check hold duration
            long elapsed = now - stateStartTime;
            if (elapsed >= (long) parent.getHoldDuration()) {
                shouldBlock = false;
                state = STATE_IDLE;
                stateStartTime = now;
            }
        }

        // Apply block state
        if (shouldBlock && !isBlocking) {
            setBlockKeyState(true);
            isBlocking = true;
        } else if (!shouldBlock && isBlocking) {
            setBlockKeyState(false);
            isBlocking = false;
        }
    }

    private void setBlockKeyState(boolean pressed) {
        try {
            // When releasing, don't interfere with eating/drinking/bow
            if (!pressed && mc.thePlayer != null && mc.thePlayer.isUsingItem()) {
                net.minecraft.item.ItemStack usingItem = mc.thePlayer.getItemInUse();
                if (usingItem != null) {
                    net.minecraft.item.EnumAction action = usingItem.getItemUseAction();
                    if (action != net.minecraft.item.EnumAction.BLOCK) {
                        return; // Player is eating/drinking/using bow - don't release
                    }
                }
            }

            KeyBinding useItem = mc.gameSettings.keyBindUseItem;
            KeyBinding.setKeyBindState(useItem.getKeyCode(), pressed);

            // Also set the pressed field directly for more reliable control
            if (pressedField != null) {
                pressedField.setBoolean(useItem, pressed);
            }
        } catch (Exception e) {}
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
        return isBlocking;
    }
}
