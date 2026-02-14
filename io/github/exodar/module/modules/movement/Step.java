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
import io.github.exodar.setting.SliderSetting;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;

/**
 * Step - Step up blocks without jumping
 * Based on Rise 6.2.4 implementations
 *
 * Modes:
 * - Vanilla: Simple stepHeight modification (no timer)
 * - Jump: Auto-jump when colliding horizontally
 * - NCPLess: Jump + motion manipulation bypass (no packets)
 */
public class Step extends Module {

    // Mode constants
    private static final String MODE_VANILLA = "Vanilla";
    private static final String MODE_JUMP = "Jump";
    private static final String MODE_NCP_LESS = "NCPLess";

    // Settings
    private final ModeSetting mode;
    private final SliderSetting height;

    // State for NCPLess
    private boolean stepping = false;
    private int offGroundTicks = 0;

    public Step() {
        super("Step", ModuleCategory.MOVEMENT);

        this.registerSetting(new DescriptionSetting("Step up blocks"));
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{
            MODE_VANILLA, MODE_JUMP, MODE_NCP_LESS
        }));
        this.registerSetting(height = new SliderSetting("Height", 1.0, 0.6, 2.5, 0.1));

        // Update visibility
        mode.onChange(this::updateSettingsVisibility);
        updateSettingsVisibility();
    }

    private void updateSettingsVisibility() {
        String selected = mode.getSelected();
        // Height only for Vanilla mode
        height.setVisible(selected.equals(MODE_VANILLA));
    }

    @Override
    public void onEnable() {
        stepping = false;
        offGroundTicks = 0;
    }

    @Override
    public void onDisable() {
        EntityPlayerSP player = getPlayer();
        if (player != null) {
            player.stepHeight = 0.6F;
        }
        stepping = false;
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + mode.getSelected();
    }

    @Override
    public void onUpdate() {
        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        // Track off ground ticks
        if (player.onGround) {
            offGroundTicks = 0;
        } else {
            offGroundTicks++;
        }

        String currentMode = mode.getSelected();

        switch (currentMode) {
            case MODE_VANILLA:
                handleVanilla(player);
                break;
            case MODE_JUMP:
                handleJump(player);
                break;
            case MODE_NCP_LESS:
                handleNCPLess(player);
                break;
        }
    }

    /**
     * Vanilla step - simple stepHeight modification
     * No timer manipulation - just changes step height
     */
    private void handleVanilla(EntityPlayerSP player) {
        if (player.onGround && !isInLiquid(player)) {
            player.stepHeight = (float) height.getValue();
        } else {
            player.stepHeight = 0.6F;
        }
    }

    /**
     * Jump step - auto-jump when colliding horizontally
     * From Rise 6.2.4 JumpStep
     */
    private void handleJump(EntityPlayerSP player) {
        player.stepHeight = 0.6F;

        if (player.onGround && player.isCollidedHorizontally) {
            player.jump();
        }
    }

    /**
     * NCPLess step - jump + motion manipulation
     * From Rise 6.2.4 NCPPacketLessStep
     * No packets needed, manipulates motion directly
     */
    private void handleNCPLess(EntityPlayerSP player) {
        player.stepHeight = 0.6F;

        // Check if should step
        if (player.onGround && player.isCollidedHorizontally && !player.isPotionActive(Potion.jump)) {
            player.jump();
            stopMotion(player);
            stepping = true;
        }

        // Apply motion on tick 3 after jump
        if (offGroundTicks == 3 && stepping) {
            // Predict motion 2 ticks ahead
            player.motionY = predictedMotion(player.motionY, 2);

            // Apply strafe with reduced speed
            double speed = getAllowedHorizontalDistance() * 0.6 - Math.random() / 100f - 0.05;
            strafe(player, (float) speed);
            stepping = false;
        }
    }

    // ===== HELPER METHODS =====

    /**
     * Predict motion Y after N ticks
     */
    private double predictedMotion(double motionY, int ticks) {
        for (int i = 0; i < ticks; i++) {
            motionY = (motionY - 0.08) * 0.98;
        }
        return motionY;
    }

    /**
     * Get allowed horizontal distance (base move speed)
     */
    private double getAllowedHorizontalDistance() {
        EntityPlayerSP player = getPlayer();
        if (player == null) return 0.2873;

        double baseSpeed = 0.2873;
        if (player.isPotionActive(Potion.moveSpeed)) {
            int amplifier = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
            baseSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }
        return baseSpeed;
    }

    /**
     * Stop horizontal motion
     */
    private void stopMotion(EntityPlayerSP player) {
        player.motionX = 0;
        player.motionZ = 0;
    }

    /**
     * Strafe - apply speed in movement direction
     */
    private void strafe(EntityPlayerSP player, float speed) {
        if (speed <= 0) return;

        float forward = player.movementInput.moveForward;
        float strafe = player.movementInput.moveStrafe;

        if (forward != 0 && strafe != 0) {
            forward *= 0.7071067811865476f;
            strafe *= 0.7071067811865476f;
        }

        float yaw = player.rotationYaw;
        double yawRadians = Math.toRadians(yaw);

        player.motionX = (forward * -Math.sin(yawRadians) + strafe * Math.cos(yawRadians)) * speed;
        player.motionZ = (forward * Math.cos(yawRadians) + strafe * Math.sin(yawRadians)) * speed;
    }

    /**
     * Check if player is in liquid
     */
    private boolean isInLiquid(EntityPlayerSP player) {
        return player.isInWater() || player.isInLava();
    }
}
