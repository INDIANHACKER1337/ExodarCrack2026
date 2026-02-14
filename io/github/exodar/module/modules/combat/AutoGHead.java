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
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;

import java.util.Random;

/**
 * AutoGHead - Automatically consumes Golden Heads when health is low
 *
 * For Hypixel UHC/Bedwars where Golden Heads restore health.
 */
public class AutoGHead extends Module {

    private final SliderSetting health;
    private final SliderSetting delayMin;
    private final SliderSetting delayMax;
    private final SliderSetting cooldownMin;
    private final SliderSetting cooldownMax;

    private State state = State.WAITING;
    private int originalSlot;
    private long nextActionTime = 0;
    private final Random random = new Random();

    public AutoGHead() {
        super("AutoGHead", ModuleCategory.COMBAT);
        this.registerSetting(new DescriptionSetting("Auto-eat Golden Heads"));
        this.registerSetting(health = new SliderSetting("Health", 7.0, 1.0, 20.0, 0.5));
        this.registerSetting(delayMin = new SliderSetting("Delay Min (ms)", 50.0, 0.0, 200.0, 10.0));
        this.registerSetting(delayMax = new SliderSetting("Delay Max (ms)", 100.0, 0.0, 200.0, 10.0));
        this.registerSetting(cooldownMin = new SliderSetting("Cooldown Min (ms)", 1000.0, 0.0, 5000.0, 100.0));
        this.registerSetting(cooldownMax = new SliderSetting("Cooldown Max (ms)", 1200.0, 0.0, 5000.0, 100.0));
    }

    @Override
    public void onEnable() {
        state = State.WAITING;
        nextActionTime = 0;
    }

    @Override
    public void onUpdate() {
        if (!isInGame()) return;
        if (mc.thePlayer.getHealth() >= health.getValue()) {
            // Health is fine, reset state if we were in the middle of something
            if (state != State.WAITING && state != State.COOLDOWN) {
                // Return to original slot if we switched
                if (state == State.SWITCHED || state == State.CLICKED) {
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
                // Start the healing sequence
                nextActionTime = now + randomDelay() / 3;
                state = State.SWITCH;
                break;

            case SWITCH:
                int slot = getGoldenHeadSlot();
                if (slot == -1) {
                    state = State.WAITING;
                    return;
                }
                originalSlot = mc.thePlayer.inventory.currentItem;
                mc.thePlayer.inventory.currentItem = slot;
                nextActionTime = now + randomDelay() / 3;
                state = State.SWITCHED;
                break;

            case SWITCHED:
                // Right click to eat
                KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
                nextActionTime = now + randomDelay() / 3;
                state = State.CLICKED;
                break;

            case CLICKED:
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

    private int getGoldenHeadSlot() {
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack item = mc.thePlayer.inventory.getStackInSlot(slot);
            if (item != null && item.getItem() instanceof ItemSkull) {
                String name = item.getDisplayName().toLowerCase();
                if (name.contains("golden") && name.contains("head")) {
                    return slot;
                }
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

    private enum State {
        WAITING,
        SWITCH,
        SWITCHED,
        CLICKED,
        COOLDOWN
    }
}
