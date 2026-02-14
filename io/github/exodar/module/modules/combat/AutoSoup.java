/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * AutoSoup - Automatically consumes soup when health is low
 *
 * For soup PvP servers. Includes auto-refill from inventory.
 */
public class AutoSoup extends Module {

    private final SliderSetting health;
    private final SliderSetting delayMin;
    private final SliderSetting delayMax;
    private final SliderSetting cooldownMin;
    private final SliderSetting cooldownMax;
    private final TickSetting invConsume;
    private final TickSetting autoRefill;
    private final SliderSetting refillDelayMin;
    private final SliderSetting refillDelayMax;

    private State state = State.WAITING;
    private int originalSlot;
    private long nextActionTime = 0;
    private long nextRefillTime = 0;
    private boolean wasInInventory = false;
    private final List<Integer> refillSlots = new ArrayList<>();
    private final Random random = new Random();

    public AutoSoup() {
        super("AutoSoup", ModuleCategory.COMBAT);
        this.registerSetting(new DescriptionSetting("Auto-eat soup when low HP"));
        this.registerSetting(health = new SliderSetting("Health", 7.0, 1.0, 20.0, 0.5));
        this.registerSetting(delayMin = new SliderSetting("Delay Min (ms)", 50.0, 0.0, 200.0, 10.0));
        this.registerSetting(delayMax = new SliderSetting("Delay Max (ms)", 100.0, 0.0, 200.0, 10.0));
        this.registerSetting(cooldownMin = new SliderSetting("Cooldown Min (ms)", 1000.0, 0.0, 5000.0, 100.0));
        this.registerSetting(cooldownMax = new SliderSetting("Cooldown Max (ms)", 1200.0, 0.0, 5000.0, 100.0));
        this.registerSetting(invConsume = new TickSetting("Consume in Inventory", false));
        this.registerSetting(autoRefill = new TickSetting("Auto Refill", true));
        this.registerSetting(refillDelayMin = new SliderSetting("Refill Delay Min (ms)", 50.0, 0.0, 200.0, 10.0));
        this.registerSetting(refillDelayMax = new SliderSetting("Refill Delay Max (ms)", 100.0, 0.0, 200.0, 10.0));
    }

    @Override
    public void onEnable() {
        state = State.WAITING;
        nextActionTime = 0;
        nextRefillTime = 0;
        wasInInventory = false;
        refillSlots.clear();
    }

    @Override
    public void onUpdate() {
        if (!isInGame()) return;

        // Auto refill from inventory
        if (autoRefill.isEnabled() && mc.currentScreen instanceof GuiInventory) {
            handleAutoRefill();
        } else {
            wasInInventory = false;
        }

        // Don't consume if screen is open (unless invConsume is enabled)
        if (mc.currentScreen != null && !invConsume.isEnabled()) {
            return;
        }

        // Check health
        if (mc.thePlayer.getHealth() >= health.getValue()) {
            if (state != State.WAITING && state != State.COOLDOWN) {
                if (state == State.SWITCHED || state == State.CLICKED || state == State.DROPPED) {
                    mc.thePlayer.inventory.currentItem = originalSlot;
                }
                state = State.WAITING;
            }
            return;
        }

        long now = System.currentTimeMillis();
        if (now < nextActionTime) return;

        switch (state) {
            case WAITING:
                nextActionTime = now + randomDelay() / 4;
                state = State.SWITCH;
                break;

            case SWITCH:
                int slot = getSoupSlot();
                if (slot == -1) {
                    state = State.WAITING;
                    return;
                }
                originalSlot = mc.thePlayer.inventory.currentItem;
                mc.thePlayer.inventory.currentItem = slot;
                nextActionTime = now + randomDelay() / 4;
                state = State.SWITCHED;
                break;

            case SWITCHED:
                // Right click to eat
                KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
                nextActionTime = now + randomDelay() / 4;
                state = State.CLICKED;
                break;

            case CLICKED:
                // Drop empty bowl
                KeyBinding.onTick(mc.gameSettings.keyBindDrop.getKeyCode());
                nextActionTime = now + randomDelay() / 4;
                state = State.DROPPED;
                break;

            case DROPPED:
                // Return to original slot
                mc.thePlayer.inventory.currentItem = originalSlot;
                nextActionTime = now + randomCooldown();
                state = State.COOLDOWN;
                break;

            case COOLDOWN:
                state = State.WAITING;
                break;
        }
    }

    private void handleAutoRefill() {
        if (!wasInInventory) {
            // Just opened inventory, generate refill path
            wasInInventory = true;
            generateRefillPath();
            nextRefillTime = System.currentTimeMillis() + randomRefillDelay();
        }

        if (refillSlots.isEmpty()) return;

        long now = System.currentTimeMillis();
        if (now >= nextRefillTime) {
            // Shift-click soup to hotbar
            if (mc.thePlayer.openContainer instanceof ContainerPlayer) {
                int slotIndex = refillSlots.remove(0);
                mc.playerController.windowClick(
                        mc.thePlayer.openContainer.windowId,
                        slotIndex,
                        0,
                        1, // Shift-click
                        mc.thePlayer
                );
                nextRefillTime = now + randomRefillDelay();
            }
        }
    }

    private void generateRefillPath() {
        refillSlots.clear();

        // Count empty hotbar slots
        int emptySlots = 0;
        for (int i = 0; i <= 8; i++) {
            if (mc.thePlayer.inventory.getStackInSlot(i) == null) {
                emptySlots++;
            }
        }

        // Find soup in inventory (not hotbar)
        if (mc.thePlayer.openContainer instanceof ContainerPlayer) {
            ContainerPlayer inv = (ContainerPlayer) mc.thePlayer.openContainer;
            for (int i = 0; i < inv.getInventory().size() && refillSlots.size() < emptySlots; i++) {
                // Skip hotbar slots (36-44)
                if (i >= 36 && i <= 44) continue;

                ItemStack item = inv.getInventory().get(i);
                if (item != null && item.getItem() instanceof ItemSoup) {
                    refillSlots.add(i);
                }
            }
        }
    }

    private int getSoupSlot() {
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack item = mc.thePlayer.inventory.getStackInSlot(slot);
            if (item != null && item.getItem() instanceof ItemSoup) {
                return slot;
            }
        }
        return -1;
    }

    private int randomDelay() {
        int min = (int) delayMin.getValue();
        int max = (int) delayMax.getValue();
        if (max <= min) return min;
        return min + random.nextInt(max - min);
    }

    private int randomCooldown() {
        int min = (int) cooldownMin.getValue();
        int max = (int) cooldownMax.getValue();
        if (max <= min) return min;
        return min + random.nextInt(max - min);
    }

    private int randomRefillDelay() {
        int min = (int) refillDelayMin.getValue();
        int max = (int) refillDelayMax.getValue();
        if (max <= min) return min;
        return min + random.nextInt(max - min);
    }

    private enum State {
        WAITING,
        SWITCH,
        SWITCHED,
        CLICKED,
        DROPPED,
        COOLDOWN
    }
}
