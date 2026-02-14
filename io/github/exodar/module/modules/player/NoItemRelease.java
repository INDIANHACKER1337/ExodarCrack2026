/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.player;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.*;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

/**
 * NoItemRelease - Cancels the item release packet after using an item
 *
 * When you let go of right-click while using an item (sword block, potion, bow),
 * this module cancels the release packet so the server thinks you're still using it.
 *
 * For bows: Hold to charge, release to keep holding, click again to shoot.
 */
public class NoItemRelease extends Module {

    // Settings
    private final TickSetting swords;
    private final TickSetting bows;
    private final TickSetting food;
    private final TickSetting potions;

    // Track what item type was being used
    private ItemType lastItemType = ItemType.NONE;

    // Bow state tracking
    private boolean bowHolding = false;  // True when we cancelled bow release
    private boolean wasRightClickPressed = false;

    private enum ItemType {
        NONE, SWORD, BOW, FOOD, POTION
    }

    public NoItemRelease() {
        super("NoItemRelease", ModuleCategory.PLAYER);

        this.registerSetting(new DescriptionSetting("Cancel item release packet"));
        this.registerSetting(swords = new TickSetting("Swords", true));
        this.registerSetting(bows = new TickSetting("Bows", false));
        this.registerSetting(food = new TickSetting("Food", false));
        this.registerSetting(potions = new TickSetting("Potions", false));
    }

    @Override
    public void onEnable() {
        lastItemType = ItemType.NONE;
        bowHolding = false;
        wasRightClickPressed = false;
    }

    @Override
    public void onDisable() {
        lastItemType = ItemType.NONE;
        bowHolding = false;
        wasRightClickPressed = false;
    }

    @Override
    public void onUpdate() {
        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        boolean rightClickPressed = org.lwjgl.input.Mouse.isButtonDown(1);

        // Track what item type the player is using
        if (player.isUsingItem()) {
            ItemStack held = player.getHeldItem();
            if (held != null) {
                Item item = held.getItem();
                if (item instanceof ItemSword) {
                    lastItemType = ItemType.SWORD;
                } else if (item instanceof ItemBow) {
                    lastItemType = ItemType.BOW;
                } else if (item instanceof ItemFood) {
                    lastItemType = ItemType.FOOD;
                } else if (item instanceof ItemPotion) {
                    lastItemType = ItemType.POTION;
                } else {
                    lastItemType = ItemType.NONE;
                }
            }
        }

        // Bow release on second right click
        if (bows.isEnabled() && bowHolding) {
            // Check if right click was just pressed (rising edge)
            if (rightClickPressed && !wasRightClickPressed) {
                // Send the release packet to shoot the bow
                sendBowRelease(player);
                bowHolding = false;
            }
        }

        // Reset bow holding if player switches items or doesn't have bow
        if (bowHolding) {
            ItemStack held = player.getHeldItem();
            if (held == null || !(held.getItem() instanceof ItemBow)) {
                bowHolding = false;
            }
        }

        wasRightClickPressed = rightClickPressed;
    }

    private void sendBowRelease(EntityPlayerSP player) {
        try {
            // Send the release packet
            C07PacketPlayerDigging releasePacket = new C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                BlockPos.ORIGIN,
                EnumFacing.DOWN
            );
            player.sendQueue.addToSendQueue(releasePacket);
        } catch (Exception e) {
            // Silent
        }
    }

    /**
     * Called by packet hook - intercept outgoing packets
     * @return true to allow packet, false to cancel it
     */
    @Override
    public boolean onSendPacket(Object packet) {
        if (!enabled) return true;

        // Check if this is a RELEASE_USE_ITEM packet
        if (packet instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging digPacket = (C07PacketPlayerDigging) packet;

            if (digPacket.getStatus() == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                // Check if we should cancel based on item type
                if (shouldCancel()) {
                    // For bows, track that we're holding
                    if (lastItemType == ItemType.BOW) {
                        bowHolding = true;
                    }
                    // Cancel the release packet - server thinks we're still using item
                    lastItemType = ItemType.NONE;
                    return false;
                }
            }
        }

        return true;
    }

    private boolean shouldCancel() {
        switch (lastItemType) {
            case SWORD:
                return swords.isEnabled();
            case BOW:
                return bows.isEnabled();
            case FOOD:
                return food.isEnabled();
            case POTION:
                return potions.isEnabled();
            default:
                return false;
        }
    }

    @Override
    public String getDisplaySuffix() {
        if (bowHolding) {
            return " §aBow";
        }
        return "";
    }
}
