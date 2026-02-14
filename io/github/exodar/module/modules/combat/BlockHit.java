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
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemAxe;

import java.util.concurrent.ThreadLocalRandom;

/**
 * BlockHit module - Automatically blocks after hitting
 * Simple implementation using swing detection
 */
public class BlockHit extends Module {

    // Settings
    private final TickSetting onlyPlayers;
    private final TickSetting weaponOnly;
    private final SliderSetting blockTime;
    private final SliderSetting everyHits;
    private final SliderSetting chance;

    // State
    public static volatile boolean isCurrentlyBlocking = false;
    private int hitCounter = 0;
    private int hitsUntilBlock = 1;
    private int lastSwingProgress = 0;
    private long blockUntil = 0;
    private boolean wasSwinging = false;

    public BlockHit() {
        super("BlockHit", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Auto block after hitting"));
        this.registerSetting(onlyPlayers = new TickSetting("Only Players", true));
        this.registerSetting(weaponOnly = new TickSetting("Weapon Only", true));
        this.registerSetting(blockTime = new SliderSetting("Block Time", 80, 20, 200, 5));
        this.registerSetting(everyHits = new SliderSetting("Every X Hits", 1, 1, 5, 1));
        this.registerSetting(chance = new SliderSetting("Chance %", 100, 0, 100, 5));
    }

    @Override
    public void onEnable() {
        hitCounter = 0;
        lastSwingProgress = 0;
        blockUntil = 0;
        wasSwinging = false;
        isCurrentlyBlocking = false;
        calculateNextHitThreshold();
    }

    @Override
    public void onDisable() {
        isCurrentlyBlocking = false;
        blockUntil = 0;
        // Release block
        try {
            KeyBinding.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindUseItem.getKeyCode(), false);
        } catch (Exception ignored) {}
    }

    @Override
    public void onUpdate() {
        EntityPlayerSP player = getPlayer();
        if (player == null) return;
        if (mc.currentScreen != null) return;

        long now = System.currentTimeMillis();

        // Check if we're currently blocking
        if (blockUntil > 0) {
            if (now < blockUntil) {
                // Still blocking
                isCurrentlyBlocking = true;
                setBlockState(true);
                return;
            } else {
                // Stop blocking
                isCurrentlyBlocking = false;
                blockUntil = 0;
                setBlockState(false);
            }
        }

        // Check if left mouse is held
        if (!org.lwjgl.input.Mouse.isButtonDown(0)) {
            wasSwinging = false;
            return;
        }

        // Check weapon
        if (weaponOnly.isEnabled() && !isHoldingWeapon(player)) {
            return;
        }

        // Get target entity
        Entity target = getTargetEntity();
        if (target == null) {
            wasSwinging = false;
            return;
        }

        // Check only players
        if (onlyPlayers.isEnabled() && !(target instanceof EntityPlayer)) {
            return;
        }

        // Detect swing - when swingProgress goes from 0 to > 0
        int currentSwing = player.swingProgress > 0 ? 1 : 0;
        boolean justSwung = (currentSwing > 0 && !wasSwinging);
        wasSwinging = currentSwing > 0;

        if (justSwung) {
            // A swing just started - this means an attack
            hitCounter++;

            if (hitCounter >= hitsUntilBlock) {
                hitCounter = 0;
                calculateNextHitThreshold();

                // Check chance
                if (chance.getValue() < 100) {
                    if (ThreadLocalRandom.current().nextDouble() * 100 >= chance.getValue()) {
                        return;
                    }
                }

                // Calculate block time with variation
                int time = (int) blockTime.getValue();
                int variation = ThreadLocalRandom.current().nextInt(-15, 16);
                int finalTime = Math.max(20, time + variation);

                // Start blocking after a small delay (let the hit register)
                blockUntil = now + finalTime + 50; // 50ms delay before block starts
            }
        }
    }

    private void setBlockState(boolean blocking) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.gameSettings != null && mc.gameSettings.keyBindUseItem != null) {
                int keyCode = mc.gameSettings.keyBindUseItem.getKeyCode();
                KeyBinding.setKeyBindState(keyCode, blocking);
                if (blocking) {
                    KeyBinding.onTick(keyCode);
                }
            }
        } catch (Exception ignored) {}
    }

    private Entity getTargetEntity() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.objectMouseOver != null) {
                return mc.objectMouseOver.entityHit;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean isHoldingWeapon(EntityPlayerSP player) {
        try {
            ItemStack held = player.getHeldItem();
            if (held == null) return false;
            return held.getItem() instanceof ItemSword || held.getItem() instanceof ItemAxe;
        } catch (Exception e) {
            return true; // Allow if can't check
        }
    }

    private void calculateNextHitThreshold() {
        int base = (int) everyHits.getValue();
        int min = Math.max(1, base - 1);
        int max = base + 1;
        hitsUntilBlock = ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * Called from Main.java when an attack packet is sent
     * This is more reliable than swing detection
     */
    public void onAttackEntity(Entity target) {
        if (!enabled) return;

        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        // Check weapon
        if (weaponOnly.isEnabled() && !isHoldingWeapon(player)) {
            return;
        }

        // Check only players
        if (onlyPlayers.isEnabled() && !(target instanceof EntityPlayer)) {
            return;
        }

        hitCounter++;

        if (hitCounter >= hitsUntilBlock) {
            hitCounter = 0;
            calculateNextHitThreshold();

            // Check chance
            if (chance.getValue() < 100) {
                if (ThreadLocalRandom.current().nextDouble() * 100 >= chance.getValue()) {
                    return;
                }
            }

            // Calculate block time with variation
            int time = (int) blockTime.getValue();
            int variation = ThreadLocalRandom.current().nextInt(-15, 16);
            int finalTime = Math.max(20, time + variation);

            // Start blocking
            blockUntil = System.currentTimeMillis() + finalTime;
        }
    }

    public static boolean isBlocking() {
        return isCurrentlyBlocking;
    }
}
