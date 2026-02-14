/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.movement;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

/**
 * NoSlow - Removes slowdown when using items (server-side bypass)
 *
 * Modes:
 * - NCP: Sends C08 placement packet to bypass NCP anticheat
 * - Intave: Sends C07 release on start, C08 for sword
 * - GrimAC: Slot change trick
 * - Verus: Pre/post position packet trick
 */
public class NoSlow extends Module {

    // Mode constants
    private static final String MODE_NCP = "NCP";
    private static final String MODE_INTAVE = "Intave";
    private static final String MODE_GRIMAC = "GrimAC";
    private static final String MODE_VERUS = "Verus";

    // Settings
    private final ModeSetting mode;
    private final TickSetting sword;
    private final TickSetting bow;
    private final TickSetting food;

    // State
    private boolean wasUsingItem = false;
    private int tickCounter = 0;

    public NoSlow() {
        super("NoSlow", ModuleCategory.MOVEMENT);

        this.registerSetting(new DescriptionSetting("Remove item use slowdown"));
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{
            MODE_NCP, MODE_INTAVE, MODE_GRIMAC, MODE_VERUS
        }));
        this.registerSetting(sword = new TickSetting("Sword", true));
        this.registerSetting(bow = new TickSetting("Bow", true));
        this.registerSetting(food = new TickSetting("Food/Potions", true));
    }

    @Override
    public void onEnable() {
        wasUsingItem = false;
        tickCounter = 0;
    }

    @Override
    public void onDisable() {
        wasUsingItem = false;
        tickCounter = 0;
    }

    @Override
    public void onUpdate() {
        EntityPlayerSP player = getPlayer();
        if (player == null || player.sendQueue == null) return;

        tickCounter++;

        boolean isUsingItem = player.isUsingItem();
        ItemStack heldItem = player.getHeldItem();

        if (!isUsingItem || heldItem == null) {
            wasUsingItem = false;
            return;
        }

        Item item = heldItem.getItem();
        if (!shouldAffect(item)) {
            wasUsingItem = isUsingItem;
            return;
        }

        String currentMode = mode.getSelected();

        switch (currentMode) {
            case MODE_NCP:
                handleNCPMode(player, item);
                break;
            case MODE_INTAVE:
                handleIntaveMode(player, item);
                break;
            case MODE_GRIMAC:
                handleGrimACMode(player, item);
                break;
            case MODE_VERUS:
                handleVerusMode(player, item);
                break;
        }

        wasUsingItem = isUsingItem;
    }

    /**
     * NCP mode - Send C08 placement packet every tick while using item
     */
    private void handleNCPMode(EntityPlayerSP player, Item item) {
        // Send placement packet with held item every tick
        player.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(player.getHeldItem()));
    }

    /**
     * Intave mode - Send C07 release on start, C08 for sword
     */
    private void handleIntaveMode(EntityPlayerSP player, Item item) {
        if (item instanceof ItemFood || item instanceof ItemPotion || item instanceof ItemBow) {
            // Send release packet on first tick of using
            if (!wasUsingItem) {
                player.sendQueue.addToSendQueue(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN,
                    EnumFacing.DOWN
                ));
            }
        }

        if (item instanceof ItemSword) {
            // Send placement packet for sword blocking
            player.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(player.getHeldItem()));
        }
    }

    /**
     * GrimAC mode - Slot change trick
     */
    private void handleGrimACMode(EntityPlayerSP player, Item item) {
        int currentSlot = player.inventory.currentItem;

        // Only on first tick of item use
        if (!wasUsingItem) {
            // Quick slot switch trick
            int fakeSlot = (currentSlot + 1) % 9;
            player.sendQueue.addToSendQueue(new C09PacketHeldItemChange(fakeSlot));
            player.sendQueue.addToSendQueue(new C09PacketHeldItemChange(currentSlot));
        }
    }

    /**
     * Verus mode - Pre/post packet trick (from LiquidBounce)
     */
    private void handleVerusMode(EntityPlayerSP player, Item item) {
        // Send interact packet before movement
        if (tickCounter % 2 == 0) {
            player.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(
                new BlockPos(-1, -1, -1),
                255,
                player.getHeldItem(),
                0, 0, 0
            ));
        }
    }

    private boolean shouldAffect(Item item) {
        if (item instanceof ItemSword && sword.isEnabled()) {
            return true;
        }
        if (item instanceof ItemBow && bow.isEnabled()) {
            return true;
        }
        if (food.isEnabled()) {
            if (item instanceof ItemFood || item instanceof ItemPotion) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + mode.getSelected();
    }
}
