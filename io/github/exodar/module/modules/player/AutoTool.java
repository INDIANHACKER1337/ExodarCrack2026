/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.player;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * AutoTool - Automatically selects the best tool for breaking blocks
 * Based on Raven B4
 */
public class AutoTool extends Module {
    private TickSetting hotkeyBack;
    private TickSetting silent;
    private TickSetting onlyWhileSneaking;
    private static Field thePlayerField;
    private int previousSlot = -1;
    private boolean mining = false;
    private int serverSlot = -1; // For silent mode - slot on server side
    private boolean hasSwitchedSilent = false; // Track if we already sent the packet

    static {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            // Find thePlayer field
            for (Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("EntityPlayer")) {
                    f.setAccessible(true);
                    thePlayerField = f;
                    System.out.println("[AutoTool] Found thePlayer: " + f.getName());
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("[AutoTool] Error in static init: " + e.getMessage());
        }
    }

    public AutoTool() {
        super("Auto Tool", ModuleCategory.PLAYER);
        this.registerSetting(new DescriptionSetting("Auto select best tool"));
        this.registerSetting(hotkeyBack = new TickSetting("Return to slot", true));
        // this.registerSetting(silent = new TickSetting("Silent", false)); // DISABLED - Silent mode
        this.registerSetting(onlyWhileSneaking = new TickSetting("Only While Sneaking", false));
    }

    @Override
    public void onEnable() {
        System.out.println("[AutoTool] Enabled");
    }

    @Override
    public void onDisable() {
        System.out.println("[AutoTool] Disabled");
        finishMining();
    }

    @Override
    public void onUpdate() {
        if (!enabled || mc == null) return;

        try {
            // Don't switch if any GUI is open
            if (mc.currentScreen != null) {
                if (mining) {
                    finishMining();
                }
                return;
            }

            // Get player
            EntityPlayerSP player = null;
            if (thePlayerField != null) {
                player = (EntityPlayerSP) thePlayerField.get(mc);
            }
            if (player == null) return;

            // Check "Only While Sneaking" setting
            if (onlyWhileSneaking != null && onlyWhileSneaking.isEnabled()) {
                if (!player.isSneaking()) {
                    if (mining) {
                        finishMining();
                    }
                    return;
                }
            }

            // Check if mouse button is down
            if (!Mouse.isButtonDown(0)) {
                if (mining) {
                    finishMining();
                }
                return;
            }

            // Get block being looked at
            MovingObjectPosition mouseOver = mc.objectMouseOver;
            if (mouseOver == null) {
                return;
            }

            // Check if looking at a block using reflection
            try {
                Field typeOfHitField = mouseOver.getClass().getDeclaredField("typeOfHit");
                typeOfHitField.setAccessible(true);
                Object typeOfHit = typeOfHitField.get(mouseOver);

                // Check if it's a BLOCK type (comparing toString since enum might have different names)
                if (typeOfHit == null || !typeOfHit.toString().equals("BLOCK")) {
                    return;
                }
            } catch (Exception e) {
                // If we can't determine type, assume it's not a block
                return;
            }

            // Get block using reflection
            Block block = null;
            try {
                // Try to get BlockPos and then the block
                Method getBlockPosMethod = mouseOver.getClass().getMethod("getBlockPos");
                Object blockPos = getBlockPosMethod.invoke(mouseOver);

                if (blockPos != null) {
                    // Try getBlockState method
                    Method getBlockStateMethod = mc.theWorld.getClass().getMethod("getBlockState", blockPos.getClass());
                    Object blockState = getBlockStateMethod.invoke(mc.theWorld, blockPos);

                    if (blockState != null) {
                        Method getBlockMethod = blockState.getClass().getMethod("getBlock");
                        block = (Block) getBlockMethod.invoke(blockState);
                    }
                }
            } catch (Exception e) {
                // Failed to get block, exit
                return;
            }

            if (block == null || block == Blocks.air || block instanceof BlockLiquid) {
                return;
            }

            // Start mining
            if (!mining) {
                previousSlot = player.inventory.currentItem;
                mining = true;
            }

            // Switch to best tool
            hotkeyToFastest(block);

        } catch (Exception e) {
            // Silently fail
        }
    }

    private void finishMining() {
        try {
            if (hotkeyBack.isEnabled() && previousSlot != -1) {
                EntityPlayerSP player = null;
                if (thePlayerField != null) {
                    player = (EntityPlayerSP) thePlayerField.get(mc);
                }
                if (player != null) {
                    // Both silent and normal mode - restore client slot
                    player.inventory.currentItem = previousSlot;
                }
            }
            mining = false;
            previousSlot = -1;
            serverSlot = -1;
            hasSwitchedSilent = false;
        } catch (Exception e) {
            // Silent fail
        }
    }

    private void hotkeyToFastest(Block block) {
        try {
            EntityPlayerSP player = null;
            if (thePlayerField != null) {
                player = (EntityPlayerSP) thePlayerField.get(mc);
            }
            if (player == null) return;

            int bestSlot = -1;
            float bestSpeed = 1.0f;

            for (int slot = 0; slot <= 8; slot++) {
                ItemStack itemInSlot = player.inventory.getStackInSlot(slot);
                if (itemInSlot != null) {
                    if (itemInSlot.getItem() instanceof ItemTool || itemInSlot.getItem() instanceof ItemShears) {
                        // Use getStrVsBlock instead of getDigSpeed with getDefaultState
                        float speed = itemInSlot.getStrVsBlock(block);
                        if (speed > bestSpeed) {
                            bestSpeed = speed;
                            bestSlot = slot;
                        }
                    }
                }
            }

            // Only switch if we found a better tool
            if (bestSlot != -1 && bestSpeed > 1.1f) {
                // Normal mode - change the actual slot
                player.inventory.currentItem = bestSlot;
                // DISABLED - Silent mode
                // if (silent != null && silent.isEnabled()) {
                //     // Silent mode - change client slot for correct animation
                //     if (player.inventory.currentItem != bestSlot) {
                //         serverSlot = bestSlot;
                //         player.inventory.currentItem = bestSlot;
                //         hasSwitchedSilent = true;
                //     }
                // } else {
                //     player.inventory.currentItem = bestSlot;
                // }
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

}
