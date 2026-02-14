/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat.autoblock;

import io.github.exodar.module.modules.combat.AutoBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;

import java.lang.reflect.Field;

/**
 * Base class with common functionality for AutoBlock modes
 */
public abstract class BaseMode implements AutoBlockMode {

    protected final Minecraft mc = Minecraft.getMinecraft();
    protected final AutoBlock parent;

    // State
    protected static final int STATE_IDLE = 0;
    protected static final int STATE_BLOCKING = 1;
    protected static final int STATE_LAGGING = 2;

    protected volatile int state = STATE_IDLE;
    protected volatile long stateStartTime = 0;
    protected volatile boolean blockingStarted = false;

    // Reflection fields
    protected Field rightClickDelayTimerField = null;
    protected Field itemInUseCountField = null;

    public BaseMode(AutoBlock parent) {
        this.parent = parent;
        findFields();
    }

    private void findFields() {
        try {
            String[] names = {"rightClickDelayTimer", "field_71467_ac", "ag"};
            for (String name : names) {
                try {
                    Field f = Minecraft.class.getDeclaredField(name);
                    f.setAccessible(true);
                    rightClickDelayTimerField = f;
                    break;
                } catch (NoSuchFieldException ignored) {}
            }
        } catch (Exception e) {}

        try {
            String[] names = {"itemInUseCount", "field_71072_f"};
            for (String name : names) {
                try {
                    Field f = EntityPlayer.class.getDeclaredField(name);
                    f.setAccessible(true);
                    itemInUseCountField = f;
                    break;
                } catch (NoSuchFieldException ignored) {}
            }
        } catch (Exception e) {}
    }

    // ==================== Common Methods ====================

    protected void resetRightClickDelay() {
        if (rightClickDelayTimerField != null) {
            try {
                rightClickDelayTimerField.setInt(mc, 0);
            } catch (Exception ignored) {}
        }
    }

    protected void pressBlock() {
        try {
            int key = mc.gameSettings.keyBindUseItem.getKeyCode();
            KeyBinding.setKeyBindState(key, true);
            KeyBinding.onTick(key);
        } catch (Exception e) {}
    }

    protected void releaseBlock() {
        try {
            EntityPlayerSP player = mc.thePlayer;
            if (player == null) return;

            // Don't interfere with eating/drinking/bow
            if (player.isUsingItem()) {
                net.minecraft.item.ItemStack usingItem = player.getItemInUse();
                if (usingItem != null) {
                    net.minecraft.item.EnumAction action = usingItem.getItemUseAction();
                    if (action != net.minecraft.item.EnumAction.BLOCK) {
                        return; // Player is eating/drinking/using bow - don't release
                    }
                }
            }

            int key = mc.gameSettings.keyBindUseItem.getKeyCode();
            KeyBinding.setKeyBindState(key, false);
        } catch (Exception e) {}
    }

    protected void setFakeAnimation(boolean show) {
        if (itemInUseCountField == null) return;
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        try {
            // Don't modify if player is using an item for other purposes (eating, drinking, bow)
            if (player.isUsingItem()) {
                net.minecraft.item.ItemStack usingItem = player.getItemInUse();
                if (usingItem != null) {
                    net.minecraft.item.EnumAction action = usingItem.getItemUseAction();
                    if (action != net.minecraft.item.EnumAction.BLOCK) {
                        // Player is eating/drinking/using bow - don't interfere
                        return;
                    }
                }
            }

            if (show) {
                // Show fake animation - set itemInUseCount to 1
                // Only if not already blocking (real block shows animation naturally)
                if (!player.isBlocking()) {
                    itemInUseCountField.set(player, 1);
                }
            } else {
                // Hide fake animation - only clear if not blocking and not in blocking state
                if (!player.isBlocking() && state != STATE_BLOCKING && !blockingStarted) {
                    itemInUseCountField.set(player, 0);
                }
            }
        } catch (Exception ignored) {}
    }

    protected void startBlocking() {
        resetRightClickDelay();
        pressBlock();
        // Don't clear fake animation here - let it persist until real block registers
        // The modes will clear it when player.isBlocking() returns true
        blockingStarted = true;
    }

    /**
     * Update fake block animation based on state and settings
     * Call this in onTick for all modes that want to show animation when idle with target
     * @param hasTarget whether there's a valid target nearby
     */
    protected void updateBlockAnimation(boolean hasTarget) {
        if (!parent.isBlockAnimationEnabled()) {
            return;
        }

        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        if (state == STATE_IDLE) {
            // Show fake animation when idle with target nearby
            if (hasTarget) {
                setFakeAnimation(true);
            } else {
                setFakeAnimation(false);
            }
        } else if (state == STATE_BLOCKING) {
            // When actually blocking, clear fake animation if real block is active
            if (player.isBlocking()) {
                setFakeAnimation(false);
            }
        }
    }

    protected void resetToIdle() {
        if (state == STATE_BLOCKING || blockingStarted) {
            releaseBlock();
        }
        setFakeAnimation(false);
        state = STATE_IDLE;
        stateStartTime = System.currentTimeMillis();
        blockingStarted = false;
    }

    // ==================== Default Implementations ====================

    @Override
    public int getQueueSize() {
        return 0;
    }

    @Override
    public boolean isBlocking() {
        return state == STATE_BLOCKING;
    }

    @Override
    public boolean isLagging() {
        return state == STATE_LAGGING;
    }

    @Override
    public String getStateName() {
        switch (state) {
            case STATE_BLOCKING: return "Block";
            case STATE_LAGGING: return "Lag";
            default: return "Idle";
        }
    }
}
