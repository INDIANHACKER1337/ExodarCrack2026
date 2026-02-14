/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.movement;

import io.github.exodar.event.Subscribe;
import io.github.exodar.event.BlockShapeEvent;
import io.github.exodar.event.PlayerJumpEvent;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.util.BlockPos;

/**
 * Spider - Climb walls like a spider
 *
 * Modes:
 * - Verus: Simple jump every 2 ticks (works perfectly)
 * - Basic: Constant upward motion (from Augustus)
 * - Jump: Jump when falling past threshold (from Augustus)
 * - Polar: Higher jumps with collision shrink (LiquidBounce port, uses events)
 */
public class Spider extends Module {

    // Main mode selection
    private final ModeSetting mode;

    // Basic mode settings
    private final SliderSetting basicMotion;

    // Jump mode settings
    private final SliderSetting jumpThreshold;
    private final TickSetting customMotion;
    private final SliderSetting jumpMotion;
    private final TickSetting groundPacket;

    // Polar mode settings (LiquidBounce port)
    private final SliderSetting polarJumpHeight;
    private final TickSetting polarShrink;

    public Spider() {
        super("Spider", ModuleCategory.MOVEMENT);

        this.registerSetting(new DescriptionSetting("Climb walls like a spider"));

        // Mode selection - only working modes
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Verus", "Basic", "Jump", "Polar"}));

        // === Basic Mode Settings ===
        this.registerSetting(new DescriptionSetting("--- Basic Settings ---"));
        this.registerSetting(basicMotion = new SliderSetting("Motion", 0.3, 0.0, 2.0, 0.1));

        // === Jump Mode Settings ===
        this.registerSetting(new DescriptionSetting("--- Jump Settings ---"));
        this.registerSetting(jumpThreshold = new SliderSetting("Jump Threshold", -0.2, -0.42, 0.42, 0.02));
        this.registerSetting(customMotion = new TickSetting("Custom Motion", false));
        this.registerSetting(jumpMotion = new SliderSetting("Jump Motion", 0.42, 0.1, 1.0, 0.02));
        this.registerSetting(groundPacket = new TickSetting("Ground Packet", false));

        // === Polar Mode Settings (LiquidBounce port) ===
        this.registerSetting(new DescriptionSetting("--- Polar Settings ---"));
        this.registerSetting(polarJumpHeight = new SliderSetting("Jump Height", 0.55, 0.42, 0.6, 0.01));
        this.registerSetting(polarShrink = new TickSetting("Shrink", true));

        // Set visibility based on mode
        updateSettingsVisibility();
    }

    private void updateSettingsVisibility() {
        String selected = mode.getSelected();

        // Basic mode settings - only visible when Basic mode selected
        basicMotion.setVisible(selected.equals("Basic"));

        // Jump mode settings - only visible when Jump mode selected
        jumpThreshold.setVisible(selected.equals("Jump"));
        customMotion.setVisible(selected.equals("Jump"));
        jumpMotion.setVisible(selected.equals("Jump") && customMotion.isEnabled());
        groundPacket.setVisible(selected.equals("Jump"));

        // Polar mode settings - only visible when Polar mode selected
        polarJumpHeight.setVisible(selected.equals("Polar"));
        polarShrink.setVisible(selected.equals("Polar"));
    }

    @Override
    public void onUpdate() {
        if (!enabled || !isInGame()) return;
        if (mc.thePlayer == null) return;

        // Update settings visibility each tick
        updateSettingsVisibility();

        // Only activate when colliding with a wall horizontally (except for event-based modes)
        String selectedMode = mode.getSelected();

        // Polar mode uses events, doesn't need wall collision check here
        if (!selectedMode.equals("Polar") && !mc.thePlayer.isCollidedHorizontally) return;

        // Don't activate on ladders, in water, or lava
        if (mc.thePlayer.isOnLadder() || mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) return;

        switch (selectedMode) {
            case "Verus":
                handleVerus();
                break;
            case "Basic":
                handleBasic();
                break;
            case "Jump":
                handleJump();
                break;
            case "Polar":
                // Polar mode is handled via events (@Subscribe methods)
                break;
        }
    }

    /**
     * BlockShapeEvent handler for Polar mode
     * Shrinks block collision shapes when player is at or above the block
     * Port from LiquidBounce's SpiderPolar29thMarch2025
     */
    @Subscribe
    public void onBlockShape(BlockShapeEvent event) {
        if (!enabled || mc.thePlayer == null) return;
        if (!mode.getSelected().equals("Polar")) return;
        if (!polarShrink.isEnabled()) return;

        // Only shrink blocks at or below player's Y position
        // Also shrink when sneaking and on ground (for edge cases)
        if (event.getPos().getY() >= mc.thePlayer.posY ||
            (mc.thePlayer.isSneaking() && mc.thePlayer.onGround)) {
            // Shrink the collision box slightly on X and Z
            // This allows the player to "walk inside" the block edge
            event.shrink(0.0001, 0.0001);
        }
    }

    /**
     * PlayerJumpEvent handler for Polar mode
     * Allows higher jumps when colliding horizontally with a wall
     * Port from LiquidBounce's SpiderPolar29thMarch2025
     */
    @Subscribe
    public void onPlayerJump(PlayerJumpEvent event) {
        if (!enabled || mc.thePlayer == null) return;
        if (!mode.getSelected().equals("Polar")) return;

        float jumpHeight = (float) polarJumpHeight.getValue();

        // Only modify jump when colliding horizontally (against a wall)
        // and jump height is higher than normal (0.42)
        if (mc.thePlayer.isCollidedHorizontally && jumpHeight > 0.42f) {
            event.setMotion(jumpHeight);
        }
    }

    /**
     * Verus mode - Simple jump every 2 ticks
     * Works perfectly on Verus anticheat
     */
    private void handleVerus() {
        if (mc.thePlayer.ticksExisted % 2 == 0) {
            mc.thePlayer.jump();
        }
    }

    /**
     * Basic mode - Constant upward motion (from Augustus)
     * Simple but can be detected easily
     */
    private void handleBasic() {
        mc.thePlayer.motionY = basicMotion.getValue();
    }

    /**
     * Jump mode - Jump when on ground or falling past threshold (from Augustus)
     * More natural movement pattern, harder to detect
     */
    private void handleJump() {
        if (mc.thePlayer.onGround) {
            mc.thePlayer.jump();
        } else if (mc.thePlayer.motionY < jumpThreshold.getValue()) {
            // When falling past threshold, jump again
            if (customMotion.isEnabled()) {
                // Use custom motion value instead of normal jump
                mc.thePlayer.motionY = jumpMotion.getValue();
            } else {
                mc.thePlayer.jump();
            }

            // Send ground packet if enabled (helps bypass some anticheats)
            if (groundPacket.isEnabled()) {
                try {
                    mc.thePlayer.sendQueue.addToSendQueue(
                        new net.minecraft.network.play.client.C03PacketPlayer(true)
                    );
                } catch (Exception e) {
                    // Ignore packet errors
                }
            }
        }
    }

    @Override
    public void onDisable() {
        // No cleanup needed
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + mode.getSelected();
    }
}
