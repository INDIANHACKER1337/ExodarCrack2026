/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.player;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * AutoSword - Automatically moves best sword to hotbar slot
 * Uses PRE-motion packet sending to bypass Grim Post check
 */
public class AutoSword extends Module {

    // Settings
    private ModeSetting mode;
    private SliderSetting delay;
    private SliderSetting startDelay;
    private TickSetting autoClose;

    // State
    private final Queue<int[]> pendingClicks = new ArrayDeque<>(); // [slot, hotbarSlot] for swap
    private long lastClickTime = 0;
    private long activatedTime = 0;
    private boolean startDelayDone = false;
    private boolean wasInventoryOpen = false;
    private boolean spoofedOpen = false;
    private boolean didMove = false;
    private boolean queueBuilt = false;

    // Cached reflection
    private static Field thePlayerField = null;
    private static Field currentScreenField = null;
    private static Field sendQueueField = null;

    public AutoSword() {
        super("AutoSword", ModuleCategory.PLAYER);

        this.registerSetting(new DescriptionSetting("Auto move best sword to slot 1"));
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"OpenInv", "SpoofInv"}));
        this.registerSetting(delay = new SliderSetting("Delay", 50.0, 0.0, 500.0, 10.0));
        this.registerSetting(startDelay = new SliderSetting("Start Delay", 100.0, 0.0, 500.0, 10.0));
        this.registerSetting(autoClose = new TickSetting("Auto Close", true));

        initFields();
    }

    private void initFields() {
        try {
            for (Field f : Minecraft.class.getDeclaredFields()) {
                f.setAccessible(true);
                String typeName = f.getType().getName();
                if (typeName.contains("EntityPlayer") && !typeName.contains("$")) {
                    thePlayerField = f;
                } else if (typeName.contains("GuiScreen")) {
                    currentScreenField = f;
                }
            }

            for (Field f : EntityPlayer.class.getDeclaredFields()) {
                f.setAccessible(true);
                String typeName = f.getType().getName();
                if (typeName.contains("NetHandler") || typeName.contains("INetHandler")) {
                    sendQueueField = f;
                    break;
                }
            }
        } catch (Exception e) {
            // Silent
        }
    }

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        if (spoofedOpen) {
            closeSpoofedInventory();
        }
        resetState();
    }

    private void resetState() {
        pendingClicks.clear();
        lastClickTime = 0;
        activatedTime = 0;
        startDelayDone = false;
        wasInventoryOpen = false;
        spoofedOpen = false;
        didMove = false;
        queueBuilt = false;
    }

    @Override
    public boolean onSendPacket(Object packet) {
        if (packet == null || !enabled) return true;

        String packetName = packet.getClass().getSimpleName();

        if (packetName.contains("C03") || packetName.contains("PacketPlayer")) {
            processPendingClicks();
        }

        return true;
    }

    private void processPendingClicks() {
        if (pendingClicks.isEmpty()) return;

        try {
            EntityPlayer player = (EntityPlayer) thePlayerField.get(mc);
            if (player == null) return;

            long now = System.currentTimeMillis();
            int delayMs = (int) delay.getValue();

            // Instant mode
            if (delayMs == 0) {
                while (!pendingClicks.isEmpty()) {
                    int[] click = pendingClicks.poll();
                    int slot = click[0];
                    int hotbarSlot = click[1];
                    // Mode 2 = hotbar swap, button = hotbar slot (0-8)
                    mc.playerController.windowClick(player.inventoryContainer.windowId, slot, hotbarSlot, 2, player);
                    didMove = true;
                }
                lastClickTime = now;
                return;
            }

            // Normal mode with delay
            if (now - lastClickTime >= delayMs && !pendingClicks.isEmpty()) {
                int[] click = pendingClicks.poll();
                int slot = click[0];
                int hotbarSlot = click[1];
                mc.playerController.windowClick(player.inventoryContainer.windowId, slot, hotbarSlot, 2, player);
                lastClickTime = now;
                didMove = true;
            }
        } catch (Exception e) {
            // Silent
        }
    }

    @Override
    public void onUpdate() {
        if (!enabled || mc == null) return;

        try {
            EntityPlayer player = (EntityPlayer) thePlayerField.get(mc);
            if (player == null) return;

            Object screen = currentScreenField.get(mc);
            boolean isInventoryOpen = (screen instanceof GuiInventory);
            boolean isOtherGuiOpen = (screen != null && !isInventoryOpen);

            if (isOtherGuiOpen) {
                return;
            }

            String selectedMode = mode.getSelected();

            // SpoofInv mode - auto activate
            if (selectedMode.equals("SpoofInv") && !spoofedOpen && !wasInventoryOpen && !queueBuilt) {
                buildSwapQueue(player);
                queueBuilt = true;

                if (pendingClicks.isEmpty()) {
                    return;
                }

                openSpoofedInventory();
                spoofedOpen = true;
                wasInventoryOpen = true;
                activatedTime = System.currentTimeMillis();
                startDelayDone = false;
            }

            // OpenInv mode
            if (selectedMode.equals("OpenInv")) {
                if (isInventoryOpen && !wasInventoryOpen) {
                    wasInventoryOpen = true;
                    activatedTime = System.currentTimeMillis();
                    startDelayDone = false;
                    queueBuilt = false;
                }

                if (!isInventoryOpen && wasInventoryOpen) {
                    resetState();
                    return;
                }

                if (!isInventoryOpen) return;
            }

            // Wait for start delay
            if (!startDelayDone) {
                if (System.currentTimeMillis() - activatedTime < startDelay.getValue()) {
                    return;
                }
                startDelayDone = true;
            }

            // Build queue if not built yet
            if (!queueBuilt) {
                buildSwapQueue(player);
                queueBuilt = true;
            }

            // Check if done
            if (pendingClicks.isEmpty() && queueBuilt) {
                if (didMove) {
                    if (spoofedOpen) {
                        closeSpoofedInventory();
                    }
                    if (autoClose.isEnabled() && isInventoryOpen) {
                        mc.displayGuiScreen(null);
                    }
                }
                resetState();
            }

        } catch (Exception e) {
            // Silent
        }
    }

    private void buildSwapQueue(EntityPlayer player) {
        pendingClicks.clear();

        try {
            int targetHotbarSlot = 0; // Always slot 1

            // Check what's in the target slot
            ItemStack targetStack = player.inventory.getStackInSlot(targetHotbarSlot);

            // If target slot already has the best sword, nothing to do
            if (targetStack != null && targetStack.getItem() instanceof ItemSword) {
                // Check if there's a better sword in inventory
                int bestSlot = findBestSwordSlot(player, targetStack);
                if (bestSlot == -1) {
                    return; // Target already has best sword
                }
                // Swap with better sword
                pendingClicks.add(new int[]{bestSlot, targetHotbarSlot});
            } else {
                // Target slot doesn't have a sword, find best and move it
                int bestSlot = findBestSwordSlot(player, null);
                if (bestSlot != -1) {
                    pendingClicks.add(new int[]{bestSlot, targetHotbarSlot});
                }
            }
        } catch (Exception e) {
            // Silent
        }
    }

    /**
     * Find the slot with the best sword in inventory (excluding hotbar target)
     * Returns -1 if no better sword found
     */
    private int findBestSwordSlot(EntityPlayer player, ItemStack compareTo) {
        int bestSlot = -1;
        double bestValue = compareTo != null ? getSwordValue(compareTo) : -1;

        // Search main inventory (slots 9-35) and hotbar (slots 36-44)
        for (int i = 9; i < 45; i++) {
            ItemStack stack = player.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemSword) {
                double value = getSwordValue(stack);
                if (value > bestValue) {
                    bestValue = value;
                    bestSlot = i;
                }
            }
        }

        return bestSlot;
    }

    private double getSwordValue(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemSword)) {
            return -1;
        }

        ItemSword sword = (ItemSword) stack.getItem();
        double value = sword.getDamageVsEntity();

        // Add sharpness enchantment value
        int sharpness = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
        int fireAspect = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack);
        int knockback = EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, stack);

        value += sharpness * 1.25; // Sharpness adds 1.25 damage per level
        value += fireAspect * 0.5;
        value += knockback * 0.25;

        return value;
    }

    private void openSpoofedInventory() {
        try {
            EntityPlayer player = (EntityPlayer) thePlayerField.get(mc);
            if (player == null) return;

            Object sendQueue = sendQueueField.get(player);
            if (sendQueue != null) {
                Method addToSendQueue = sendQueue.getClass().getMethod("addToSendQueue", Packet.class);
                addToSendQueue.invoke(sendQueue, new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
            }
        } catch (Exception e) {
            // Silent
        }
    }

    private void closeSpoofedInventory() {
        try {
            EntityPlayer player = (EntityPlayer) thePlayerField.get(mc);
            if (player == null) return;

            Object sendQueue = sendQueueField.get(player);
            if (sendQueue != null) {
                Method addToSendQueue = sendQueue.getClass().getMethod("addToSendQueue", Packet.class);
                addToSendQueue.invoke(sendQueue, new C0DPacketCloseWindow(player.inventoryContainer.windowId));
            }
            spoofedOpen = false;
        } catch (Exception e) {
            // Silent
        }
    }
}
