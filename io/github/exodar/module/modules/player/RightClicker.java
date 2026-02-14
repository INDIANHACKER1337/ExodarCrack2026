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

import org.lwjgl.input.Mouse;

import java.util.Random;

/**
 * RightClicker - Autoclicker for right mouse button
 * Based on Raven B+ implementation with gaussian timing
 */
public class RightClicker extends Module {

    private final SliderSetting minCPS;
    private final SliderSetting maxCPS;
    private final SliderSetting jitter;
    private final TickSetting blocksOnly;
    private final TickSetting ignoreSwords;
    private final TickSetting ignoreRods;
    private final TickSetting allowEat;
    private final TickSetting allowBow;

    private final Random rand = new Random();

    // Timing variables
    private long nextClickTime = 0;
    private long releaseTime = 0;
    private boolean isHolding = false;

    // Spike/drop pattern for more human-like clicking
    private long nextPatternChange = 0;
    private boolean inSlowPattern = false;
    private double slowMultiplier = 1.0;

    public RightClicker() {
        super("RightClicker", ModuleCategory.PLAYER);

        this.registerSetting(new DescriptionSetting("Right click autoclicker"));
        this.registerSetting(minCPS = new SliderSetting("Min CPS", 10.0, 1.0, 25.0, 0.5));
        this.registerSetting(maxCPS = new SliderSetting("Max CPS", 15.0, 1.0, 25.0, 0.5));
        this.registerSetting(jitter = new SliderSetting("Jitter", 0.0, 0.0, 3.0, 0.1));
        this.registerSetting(blocksOnly = new TickSetting("Blocks only", false));
        this.registerSetting(ignoreSwords = new TickSetting("Ignore swords", true));
        this.registerSetting(ignoreRods = new TickSetting("Ignore rods", true));
        this.registerSetting(allowEat = new TickSetting("Allow eat/drink", true));
        this.registerSetting(allowBow = new TickSetting("Allow bow", true));
    }

    @Override
    public void onEnable() {
        nextClickTime = 0;
        releaseTime = 0;
        isHolding = false;
        nextPatternChange = 0;
    }

    @Override
    public void onDisable() {
        // Release key if holding
        if (isHolding) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            isHolding = false;
        }
    }

    @Override
    public void onUpdate() {
        if (!isInGame()) return;
        if (mc.currentScreen != null) return;
        if (!mc.inGameHasFocus) return;

        // Check if right mouse button is held
        if (!Mouse.isButtonDown(1)) {
            if (isHolding) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                isHolding = false;
            }
            nextClickTime = 0;
            releaseTime = 0;
            return;
        }

        // Check if click is allowed based on held item
        if (!isClickAllowed()) {
            return;
        }

        // Apply jitter
        if (jitter.getValue() > 0) {
            applyJitter();
        }

        long now = System.currentTimeMillis();

        // Initialize timing on first click
        if (nextClickTime == 0) {
            generateNextClick();
            return;
        }

        // Handle click release/hold cycle
        if (now >= nextClickTime) {
            // Time to click
            int key = mc.gameSettings.keyBindUseItem.getKeyCode();
            KeyBinding.setKeyBindState(key, true);
            KeyBinding.onTick(key);
            isHolding = true;

            // Generate next timing
            generateNextClick();
        } else if (now >= releaseTime && isHolding) {
            // Time to release
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            isHolding = false;
        }
    }

    private boolean isClickAllowed() {
        ItemStack item = getPlayer().getHeldItem();

        if (item == null) {
            return !blocksOnly.isEnabled();
        }

        Item heldItem = item.getItem();

        // Allow eat/drink - don't interfere
        if (allowEat.isEnabled() && (heldItem instanceof ItemFood ||
            heldItem instanceof ItemPotion || heldItem instanceof ItemBucketMilk)) {
            return false;
        }

        // Allow bow - don't interfere
        if (allowBow.isEnabled() && heldItem instanceof ItemBow) {
            return false;
        }

        // Ignore fishing rods
        if (ignoreRods.isEnabled() && heldItem instanceof ItemFishingRod) {
            return false;
        }

        // Ignore swords (don't block with autoclicker)
        if (ignoreSwords.isEnabled() && heldItem instanceof ItemSword) {
            return false;
        }

        // Blocks only
        if (blocksOnly.isEnabled() && !(heldItem instanceof ItemBlock)) {
            return false;
        }

        return true;
    }

    private void applyJitter() {
        double jitterAmount = jitter.getValue() * 0.45;

        if (rand.nextBoolean()) {
            getPlayer().rotationYaw += (float)(rand.nextFloat() * jitterAmount);
        } else {
            getPlayer().rotationYaw -= (float)(rand.nextFloat() * jitterAmount);
        }

        if (rand.nextBoolean()) {
            getPlayer().rotationPitch += (float)(rand.nextFloat() * jitterAmount * 0.45);
        } else {
            getPlayer().rotationPitch -= (float)(rand.nextFloat() * jitterAmount * 0.45);
        }
    }

    private void generateNextClick() {
        long now = System.currentTimeMillis();

        // Update pattern (occasional slow periods for realism)
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

        // Calculate CPS with gaussian distribution
        double min = minCPS.getValue();
        double max = maxCPS.getValue();

        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }

        // Random CPS in range
        double cps = min + rand.nextDouble() * (max - min);

        // Add small random variation
        cps += 0.4 * rand.nextDouble();

        // Calculate delay
        long delay = Math.round(1000.0 / cps);

        // Apply slow pattern
        if (inSlowPattern) {
            delay = (long)(delay * slowMultiplier);
        }

        // Occasional spike (longer delay)
        if (rand.nextInt(100) >= 80) {
            delay += 50 + rand.nextInt(100);
        }

        // Set next click time
        nextClickTime = now + delay;

        // Release halfway through (simulate button release)
        releaseTime = now + delay / 2 - rand.nextInt(10);
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + (int) minCPS.getValue() + "-" + (int) maxCPS.getValue();
    }
}
