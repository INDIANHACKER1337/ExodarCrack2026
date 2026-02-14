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
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.*;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemEnderEye;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.util.Random;

/**
 * FastPlace - Autoclicker for block placing with gaussian timing
 * Based on Raven B+ RightClicker + Sakura tick delay
 */
public class FastPlace extends Module {

    private final SliderSetting minCPS;
    private final SliderSetting maxCPS;
    private final TickSetting blocksOnly;
    private final TickSetting ignoreObsidian;
    private final SliderSetting projectileCPS;
    private final TickSetting exhaustation;

    // Sakura tick delay option
    private final TickSetting useTickDelay;
    private final SliderSetting tickDelay;

    // Reflection
    private Field rightClickDelayTimerField = null;
    private boolean fieldFound = false;

    // Timing
    private final Random rand = new Random();
    private long nextClickTime = 0;
    private long releaseTime = 0;
    private boolean isHolding = false;

    // Pattern for human-like clicking
    private long nextPatternChange = 0;
    private boolean inSlowPattern = false;
    private double slowMultiplier = 1.0;

    public FastPlace() {
        super("FastPlace", ModuleCategory.PLAYER);

        this.registerSetting(new DescriptionSetting("Block place autoclicker"));
        this.registerSetting(minCPS = new SliderSetting("Min CPS", 12.0, 1.0, 40.0, 0.5));
        this.registerSetting(maxCPS = new SliderSetting("Max CPS", 16.0, 1.0, 40.0, 0.5));
        this.registerSetting(blocksOnly = new TickSetting("Blocks Only", true));
        this.registerSetting(ignoreObsidian = new TickSetting("Ignore Obsidian", false));
        this.registerSetting(projectileCPS = new SliderSetting("Projectile CPS", 0.0, 0.0, 30.0, 1.0));
        this.registerSetting(exhaustation = new TickSetting("Exhaustation", true));

        // Sakura tick delay option
        this.registerSetting(useTickDelay = new TickSetting("Use Tick Delay", false));
        this.registerSetting(tickDelay = new SliderSetting("Tick Delay", 0.0, 0.0, 4.0, 1.0));
    }

    @Override
    public void onEnable() {
        nextClickTime = 0;
        releaseTime = 0;
        isHolding = false;
        nextPatternChange = 0;

        if (!fieldFound) {
            findDelayField();
        }
    }

    private void findDelayField() {
        try {
            String[] names = {"rightClickDelayTimer", "field_71467_ac", "ag"};

            for (String name : names) {
                try {
                    Field f = Minecraft.class.getDeclaredField(name);
                    f.setAccessible(true);
                    rightClickDelayTimerField = f;
                    fieldFound = true;
                    return;
                } catch (NoSuchFieldException ignored) {}
            }

            // Search by value
            for (Field f : Minecraft.class.getDeclaredFields()) {
                if (f.getType() == int.class) {
                    try {
                        f.setAccessible(true);
                        if (f.getInt(mc) == 4) {
                            rightClickDelayTimerField = f;
                            fieldFound = true;
                            return;
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onUpdate() {
        if (!isInGame()) return;
        if (mc.currentScreen != null) return;
        if (!mc.inGameHasFocus) return;

        // Update settings visibility
        updateSettingsVisibility();

        ItemStack heldItem = getPlayer().getHeldItem();

        // Never affect ender pearls or eyes of ender
        if (heldItem != null) {
            if (heldItem.getItem() instanceof ItemEnderPearl || heldItem.getItem() instanceof ItemEnderEye) {
                return;
            }
        }

        // Never affect food or potions (they have their own use timing)
        if (heldItem != null) {
            if (heldItem.getItem() instanceof ItemFood || heldItem.getItem() instanceof ItemPotion) {
                return;
            }
        }

        // Check blocks only condition
        if (blocksOnly.isEnabled()) {
            if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
                return;
            }
        }

        // Check if holding obsidian and ignore is enabled
        if (ignoreObsidian.isEnabled() && heldItem != null && heldItem.getItem() instanceof ItemBlock) {
            Block block = ((ItemBlock) heldItem.getItem()).getBlock();
            if (block == Blocks.obsidian) {
                return; // Skip FastPlace for obsidian
            }
        }

        // Apply tick delay if enabled (Sakura style)
        if (useTickDelay.isEnabled()) {
            if (rightClickDelayTimerField != null) {
                try {
                    rightClickDelayTimerField.setInt(mc, (int) tickDelay.getValue());
                } catch (Exception ignored) {}
            }
        }

        // CPS autoclicker system
        handleCPSMode(heldItem);
    }

    /**
     * CPS mode - Your original clicker system
     */
    private void handleCPSMode(ItemStack heldItem) {
        // Check if right mouse button is held
        if (!Mouse.isButtonDown(1)) {
            if (isHolding) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                isHolding = false;
            }
            nextClickTime = 0;
            return;
        }

        // Check if holding projectile (eggs or snowballs)
        boolean isProjectile = heldItem != null &&
            (heldItem.getItem() instanceof ItemEgg || heldItem.getItem() instanceof ItemSnowball);

        // If projectile, check if projectile CPS is enabled (> 0)
        if (isProjectile) {
            if (projectileCPS.getValue() <= 0) {
                return; // Projectile CPS disabled
            }
        }

        // Reset right click delay timer
        if (rightClickDelayTimerField != null) {
            try {
                rightClickDelayTimerField.setInt(mc, 0);
            } catch (Exception ignored) {}
        }

        long now = System.currentTimeMillis();

        // Initialize on first click
        if (nextClickTime == 0) {
            generateNextClick();
            return;
        }

        // Click/release cycle
        if (now >= nextClickTime) {
            // Click
            int key = mc.gameSettings.keyBindUseItem.getKeyCode();
            KeyBinding.setKeyBindState(key, true);
            KeyBinding.onTick(key);
            isHolding = true;

            generateNextClick();
        } else if (now >= releaseTime && isHolding) {
            // Release
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            isHolding = false;
        }
    }

    /**
     * Update settings visibility based on tick delay checkbox
     */
    private void updateSettingsVisibility() {
        tickDelay.setVisible(useTickDelay.isEnabled());
    }

    private void generateNextClick() {
        long now = System.currentTimeMillis();

        // Check if holding projectile for CPS calculation
        ItemStack heldItem = getPlayer() != null ? getPlayer().getHeldItem() : null;
        boolean isProjectile = heldItem != null &&
            (heldItem.getItem() instanceof ItemEgg || heldItem.getItem() instanceof ItemSnowball);

        double cps;
        long delay;

        if (isProjectile) {
            // Use fixed projectile CPS (no randomization for consistency)
            cps = projectileCPS.getValue();
            delay = Math.round(1000.0 / cps);
        } else if (!exhaustation.isEnabled()) {
            // Exhaustation OFF: Constant CPS like Raven (no patterns, no spikes)
            double min = minCPS.getValue();
            double max = maxCPS.getValue();
            if (min > max) {
                double temp = min;
                min = max;
                max = temp;
            }

            // Simple random CPS in range, no variation
            cps = min + rand.nextDouble() * (max - min);
            delay = Math.round(1000.0 / cps);
        } else {
            // Exhaustation ON: Full variation system with patterns and spikes

            // Update slow pattern occasionally
            if (now > nextPatternChange) {
                if (!inSlowPattern && rand.nextInt(100) >= 85) {
                    inSlowPattern = true;
                    slowMultiplier = 1.1 + rand.nextDouble() * 0.15;
                } else {
                    inSlowPattern = false;
                    slowMultiplier = 1.0;
                }
                nextPatternChange = now + 500 + rand.nextInt(1500);
            }

            // Calculate CPS
            double min = minCPS.getValue();
            double max = maxCPS.getValue();
            if (min > max) {
                double temp = min;
                min = max;
                max = temp;
            }

            // Random CPS in range with gaussian-like distribution
            double range = max - min;
            cps = min + rand.nextDouble() * range;
            cps += 0.4 * rand.nextDouble(); // Small variation

            // Calculate delay
            delay = Math.round(1000.0 / cps);

            // Apply slow pattern
            if (inSlowPattern) {
                delay = (long)(delay * slowMultiplier);
            }

            // Occasional spike
            if (rand.nextInt(100) >= 80) {
                delay += 50 + rand.nextInt(100);
            }
        }

        nextClickTime = now + delay;
        releaseTime = now + delay / 2 - rand.nextInt(10);
    }

    @Override
    public void onDisable() {
        if (isHolding) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            isHolding = false;
        }
        // Reset delay timer to vanilla (4 ticks)
        if (rightClickDelayTimerField != null) {
            try {
                rightClickDelayTimerField.setInt(mc, 4);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + (int) minCPS.getValue() + "-" + (int) maxCPS.getValue();
    }
}
