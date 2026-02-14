/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.world;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

/**
 * AutoPlace - Automatically places blocks when bridging
 * Based on Raven XD implementation
 */
public class AutoPlace extends Module {

    private final SliderSetting tickDelay;
    private final TickSetting blocksOnly;
    private final TickSetting requireRightClick;

    // Reflection
    private Field rightClickDelayTimerField = null;

    // State
    private BlockPos lastPlacedPos = null;
    private long lastPlaceTime = 0;

    public AutoPlace() {
        super("AutoPlace", ModuleCategory.PLAYER);
        this.registerSetting(new DescriptionSetting("Auto place blocks"));
        this.registerSetting(tickDelay = new SliderSetting("Tick delay", 0, 0, 4, 1));
        this.registerSetting(blocksOnly = new TickSetting("Blocks only", true));
        this.registerSetting(requireRightClick = new TickSetting("Require right click", true));

        initReflection();
    }

    private void initReflection() {
        try {
            for (Field f : Minecraft.class.getDeclaredFields()) {
                if (f.getType() == int.class) {
                    String name = f.getName();
                    if (name.equals("rightClickDelayTimer") || name.equals("field_71467_ac")) {
                        f.setAccessible(true);
                        rightClickDelayTimerField = f;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[AutoPlace] Reflection init failed: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        lastPlacedPos = null;
    }

    @Override
    public void onUpdate() {
        if (!isInGame()) return;

        Minecraft mc = Minecraft.getMinecraft();

        if (mc.currentScreen != null) return;
        if (mc.thePlayer.capabilities.isFlying) return;

        // Check if holding block
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (blocksOnly.isEnabled()) {
            if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
                return;
            }
        }

        // Check right click requirement
        if (requireRightClick.isEnabled() && !Mouse.isButtonDown(1)) {
            return;
        }

        // Set tick delay
        int delay = (int) tickDelay.getValue();
        if (rightClickDelayTimerField != null) {
            try {
                if (delay == 0) {
                    rightClickDelayTimerField.setInt(mc, 0);
                } else {
                    int current = rightClickDelayTimerField.getInt(mc);
                    if (current == 4) {
                        rightClickDelayTimerField.setInt(mc, delay);
                    }
                }
            } catch (Exception ignored) {}
        }

        // Auto place logic
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectType.BLOCK) {
            return;
        }

        // Only place on side faces (not up/down for bridging)
        if (mop.sideHit == EnumFacing.UP || mop.sideHit == EnumFacing.DOWN) {
            return;
        }

        BlockPos pos = mop.getBlockPos();

        // Don't place on same block twice in a row
        if (pos.equals(lastPlacedPos)) {
            return;
        }

        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block == null || block == Blocks.air || block instanceof BlockLiquid) {
            return;
        }

        // Place block
        if (heldItem != null && mc.playerController.onPlayerRightClick(
                mc.thePlayer, mc.theWorld, heldItem, pos, mop.sideHit, mop.hitVec)) {
            mc.thePlayer.swingItem();
            mc.getItemRenderer().resetEquippedProgress();
            lastPlacedPos = pos;
        }
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + (int) tickDelay.getValue() + "t";
    }
}
