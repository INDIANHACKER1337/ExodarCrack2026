/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;

/**
 * AutoPotion - Automatically throws health potions when key is pressed
 * Based on Exodar C++ implementation
 *
 * States:
 * IDLE -> waiting for key press
 * SLOT_CHANGED -> switched to potion slot, waiting before throwing
 * WAITING -> holding right click
 * THROWING -> releasing click and restoring original slot
 */
public class AutoPotion extends Module {

    private final SliderSetting throwDelay;
    private final SliderSetting holdDelay;
    private final KeybindSetting throwKey;

    // State machine
    private enum State { IDLE, SLOT_CHANGED, WAITING, THROWING }
    private State state = State.IDLE;
    private long stateStartTime = 0;
    private int originalSlot = -1;

    // Reflection for KeyBinding.pressed
    private static Field pressedField = null;

    static {
        try {
            for (Field f : KeyBinding.class.getDeclaredFields()) {
                if (f.getType() == boolean.class) {
                    f.setAccessible(true);
                    pressedField = f;
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    public AutoPotion() {
        super("AutoPotion", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Auto throw health pots"));
        this.registerSetting(throwKey = new KeybindSetting("Throw Key"));
        this.registerSetting(throwDelay = new SliderSetting("Throw Delay", 50, 0, 500, 10));
        this.registerSetting(holdDelay = new SliderSetting("Hold Delay", 100, 0, 500, 10));
    }

    @Override
    public void onEnable() {
        state = State.IDLE;
        stateStartTime = 0;
        originalSlot = -1;
    }

    @Override
    public void onDisable() {
        state = State.IDLE;
        stateStartTime = 0;
        originalSlot = -1;
        // Make sure right click is released
        setKeyPressed(mc.gameSettings.keyBindUseItem, false);
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;

        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        long currentTime = System.currentTimeMillis();

        switch (state) {
            case IDLE:
                // Check if throw key is pressed
                if (throwKey.isKeyPressed()) {
                    findAndUsePotion();
                }
                break;

            case SLOT_CHANGED:
                // Wait before throwing (in ms)
                if (currentTime - stateStartTime >= (long) throwDelay.getValue()) {
                    // Simulate right click down (throw potion)
                    setKeyPressed(mc.gameSettings.keyBindUseItem, true);
                    // Also trigger the use item action directly for instant throw
                    if (holdDelay.getValue() == 0) {
                        triggerUseItem(player);
                    }
                    state = State.WAITING;
                    stateStartTime = currentTime;
                }
                break;

            case WAITING:
                // Hold right click (in ms)
                if (currentTime - stateStartTime >= (long) holdDelay.getValue()) {
                    // Release right click
                    setKeyPressed(mc.gameSettings.keyBindUseItem, false);
                    state = State.THROWING;
                    stateStartTime = currentTime;
                }
                break;

            case THROWING:
                // Restore original slot
                if (originalSlot != -1) {
                    player.inventory.currentItem = originalSlot;
                }
                state = State.IDLE;
                stateStartTime = 0;
                originalSlot = -1;
                break;
        }
    }

    /**
     * Directly trigger use item for instant potion throw
     */
    private void triggerUseItem(EntityPlayerSP player) {
        try {
            if (mc.playerController != null && player.getHeldItem() != null) {
                mc.playerController.sendUseItem(player, mc.theWorld, player.getHeldItem());
            }
        } catch (Exception ignored) {}
    }

    private void findAndUsePotion() {
        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        // Save current slot
        originalSlot = player.inventory.currentItem;

        // Search for health potion in hotbar (slots 0-8)
        int potionSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack != null && isHealthPotion(stack)) {
                potionSlot = i;
                break;
            }
        }

        // If found, switch to that slot
        if (potionSlot != -1) {
            player.inventory.currentItem = potionSlot;
            state = State.SLOT_CHANGED;
            stateStartTime = System.currentTimeMillis();
        }
    }

    /**
     * Check if item is a splash health potion
     * Item ID 373 = Potion
     * Metadata 16453 = Splash Potion of Healing II
     * Metadata 16421 = Splash Potion of Healing I
     */
    private boolean isHealthPotion(ItemStack stack) {
        if (stack == null) return false;

        try {
            int itemId = Item.getIdFromItem(stack.getItem());
            int metadata = stack.getMetadata();

            // 373 = Potion
            if (itemId == 373) {
                // 16453 = Splash Health II, 16421 = Splash Health I
                return metadata == 16453 || metadata == 16421;
            }
        } catch (Exception e) {
            // Silent
        }
        return false;
    }

    @Override
    public String getDisplaySuffix() {
        // Count potions in entire inventory (hotbar + main inventory)
        EntityPlayerSP player = getPlayer();
        if (player == null) return "";

        int count = 0;
        // Hotbar (0-8) + Main inventory (9-35)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack != null && isHealthPotion(stack)) {
                count += stack.stackSize;
            }
        }
        return " §7" + count;
    }

    /**
     * Set KeyBinding.pressed using reflection
     */
    private void setKeyPressed(KeyBinding keyBinding, boolean pressed) {
        try {
            if (pressedField != null) {
                pressedField.set(keyBinding, pressed);
            }
        } catch (Exception ignored) {}
    }
}
