/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.world;

import io.github.exodar.event.Subscribe;
import io.github.exodar.event.SafeWalkEvent;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;

/**
 * SafeWalk - Prevents falling off edges
 * Port from Raven XD with event system
 *
 * Uses SafeWalkEvent to intercept isSneaking() - no visual crouch!
 * Optionally can also physically shift (visible) for more safety
 */
public class SafeWalk extends Module {

    // Settings (like Raven XD)
    private final SliderSetting shiftDelay;
    private final SliderSetting motion;
    private final TickSetting blocksOnly;
    private final TickSetting disableOnForward;
    private final TickSetting pitchCheck;
    private final TickSetting shift;

    // State
    private boolean isSneaking = false;
    private long lastShiftTime = 0L;

    public SafeWalk() {
        super("SafeWalk", ModuleCategory.MOVEMENT);

        this.registerSetting(new DescriptionSetting("Prevents falling off edges"));
        this.registerSetting(new DescriptionSetting("Uses event hook - no visual crouch!"));

        this.registerSetting(shiftDelay = new SliderSetting("Shift Delay", 0.0, 0.0, 800.0, 10.0));
        this.registerSetting(motion = new SliderSetting("Motion", 1.0, 0.5, 1.2, 0.01));
        this.registerSetting(blocksOnly = new TickSetting("Blocks Only", true));
        this.registerSetting(disableOnForward = new TickSetting("Disable Forward", false));
        this.registerSetting(pitchCheck = new TickSetting("Pitch Check", false));
        this.registerSetting(shift = new TickSetting("Shift", false));
    }

    @Override
    public void onDisable() {
        if (shift.isEnabled() && isOverAir()) {
            setSneakState(false);
        }
        isSneaking = false;
    }

    @Override
    public void onUpdate() {
        if (!enabled || !isInGame()) return;
        if (mc.thePlayer == null) return;

        // Apply motion modifier when moving on ground
        if (motion.getValue() != 1.0 && mc.thePlayer.onGround && isMoving() &&
            (!pitchCheck.isEnabled() || mc.thePlayer.rotationPitch >= 70.0f)) {
            double speed = getHorizontalSpeed() * motion.getValue();
            setSpeed(speed);
        }

        // Handle physical shift mode
        if (shift.isEnabled()) {
            handleShiftMode();
        }
    }

    /**
     * SafeWalkEvent handler - This is the magic!
     * Intercepts isSneaking() to return true when conditions are met
     * No visual crouch, but prevents walking off edges
     */
    @Subscribe
    public void onSafeWalk(SafeWalkEvent event) {
        if (!enabled || mc.thePlayer == null) return;

        if (canSafeWalk()) {
            event.setSafeWalk(true);
        }
    }

    /**
     * Check if SafeWalk should be active (all conditions met)
     */
    private boolean canSafeWalk() {
        if (mc.currentScreen != null) return false;

        // Disable when pressing forward
        if (disableOnForward.isEnabled() && Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
            return false;
        }

        // Pitch check - only active when looking down
        if (pitchCheck.isEnabled() && mc.thePlayer.rotationPitch < 70) {
            return false;
        }

        // Blocks only check
        if (blocksOnly.isEnabled()) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held == null || !(held.getItem() instanceof ItemBlock)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Handle physical shift mode (visible sneak when over air)
     */
    private void handleShiftMode() {
        // Skip if user is manually sneaking
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
            return;
        }

        if (mc.thePlayer.onGround && isOverAir()) {
            // Check blocksOnly
            if (blocksOnly.isEnabled()) {
                ItemStack held = mc.thePlayer.getHeldItem();
                if (held == null || !(held.getItem() instanceof ItemBlock)) {
                    setSneakState(false);
                    return;
                }
            }

            // Check disableOnForward
            if (disableOnForward.isEnabled() && Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
                setSneakState(false);
                return;
            }

            // Check pitchCheck
            if (pitchCheck.isEnabled() && mc.thePlayer.rotationPitch < 70.0f) {
                setSneakState(false);
                return;
            }

            setSneakState(true);
        } else if (isSneaking) {
            setSneakState(false);
        }

        // Don't sneak while flying
        if (isSneaking && mc.thePlayer.capabilities.isFlying) {
            setSneakState(false);
        }
    }

    private void setSneakState(boolean down) {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), down);

        // Handle delay
        if (isSneaking) {
            if (down) return;
        } else if (!down) {
            return;
        }

        if (down) {
            long delay = (long) shiftDelay.getValue();
            if (delay != 0L) {
                if (System.currentTimeMillis() - lastShiftTime < delay) {
                    return;
                }
                lastShiftTime = System.currentTimeMillis();
            }
        } else {
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
                return;
            }
            lastShiftTime = System.currentTimeMillis();
        }

        isSneaking = down;
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), down);
    }

    /**
     * Check if player is over air (edge of block)
     */
    private boolean isOverAir() {
        double x = mc.thePlayer.posX;
        double y = mc.thePlayer.posY;
        double z = mc.thePlayer.posZ;

        // Player hitbox half-width
        double hw = 0.3;

        // Check all corners - if ANY corner is over air, we're at an edge
        return !isSolidAt(x - hw, y - 1, z - hw) ||
               !isSolidAt(x - hw, y - 1, z + hw) ||
               !isSolidAt(x + hw, y - 1, z - hw) ||
               !isSolidAt(x + hw, y - 1, z + hw);
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

    private boolean isMoving() {
        return mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0;
    }

    private double getHorizontalSpeed() {
        return Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
    }

    private void setSpeed(double speed) {
        double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
        double forward = mc.thePlayer.moveForward;
        double strafe = mc.thePlayer.moveStrafing;

        if (forward == 0 && strafe == 0) {
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
        } else {
            if (forward != 0) {
                if (strafe > 0) {
                    yaw -= Math.PI / 4;
                } else if (strafe < 0) {
                    yaw += Math.PI / 4;
                }
                if (forward < 0) {
                    yaw += Math.PI;
                }
                strafe = 0;
            }
            mc.thePlayer.motionX = -Math.sin(yaw) * speed;
            mc.thePlayer.motionZ = Math.cos(yaw) * speed;
        }
    }

    @Override
    public String getDisplaySuffix() {
        if (shift.isEnabled()) {
            return " §7Shift";
        }
        return "";
    }
}
