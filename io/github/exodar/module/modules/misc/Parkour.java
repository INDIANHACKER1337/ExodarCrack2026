/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;

/**
 * Parkour - Automatically jumps at block edges
 *
 * Jumps when player is on ground, moving, and at edge of a block.
 */
public class Parkour extends Module {

    private int cooldownTicks = 0;

    public Parkour() {
        super("Parkour", ModuleCategory.MISC);
        this.registerSetting(new DescriptionSetting("Auto-jump at block edges"));
    }

    @Override
    public void onEnable() {
        cooldownTicks = 0;
    }

    @Override
    public void onDisable() {
        // Release jump if we were holding it
        if (cooldownTicks > 0 && mc != null && mc.gameSettings != null) {
            if (!Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
            }
        }
        cooldownTicks = 0;
    }

    @Override
    public void onUpdate() {
        if (!isInGame()) return;
        if (mc.currentScreen != null) return;

        // Handle cooldown - release jump key after cooldown
        if (cooldownTicks > 0) {
            cooldownTicks--;
            if (cooldownTicks == 0) {
                // Release jump if user isn't manually holding it
                if (!Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
                }
            }
            return;
        }

        // Check conditions: on ground, moving, at edge
        if (mc.thePlayer.onGround && isMoving() && isOverAir()) {
            // Jump
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
            cooldownTicks = 10; // ~0.5 second cooldown
        }
    }

    /**
     * Check if player is moving horizontally
     */
    private boolean isMoving() {
        return mc.thePlayer.motionX != 0 || mc.thePlayer.motionZ != 0;
    }

    /**
     * Check if player is at edge of block (any corner over air)
     */
    private boolean isOverAir() {
        double x = mc.thePlayer.posX;
        double y = mc.thePlayer.posY;
        double z = mc.thePlayer.posZ;

        // Player hitbox half-width
        double hw = 0.3;

        // Check all corners - if ANY corner is over air, we're at an edge
        boolean corner1 = !isSolidAt(x - hw, y - 1, z - hw);
        boolean corner2 = !isSolidAt(x - hw, y - 1, z + hw);
        boolean corner3 = !isSolidAt(x + hw, y - 1, z - hw);
        boolean corner4 = !isSolidAt(x + hw, y - 1, z + hw);

        return corner1 || corner2 || corner3 || corner4;
    }

    private boolean isSolidAt(double x, double y, double z) {
        try {
            BlockPos pos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            return block.getMaterial() != Material.air && block.getMaterial().blocksMovement();
        } catch (Exception e) {
            return true;
        }
    }
}
