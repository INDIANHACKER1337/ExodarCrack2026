/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * FreeLook - Look around freely in third person without changing movement direction
 * Based on Rise client FreeLook module
 *
 * Features:
 * - Hold keybind to look around freely
 * - Movement direction stays locked to original facing
 * - Invert pitch option
 * - Proper mouse sensitivity handling
 */
public class FreeLook extends Module {

    private final TickSetting invertPitch;

    // State
    private int previousPerspective;
    private float originalYaw, originalPitch;
    private float cameraYaw, cameraPitch;
    private boolean isFreeLooking = false;

    public FreeLook() {
        super("FreeLook", ModuleCategory.VISUALS);

        this.registerSetting(new DescriptionSetting("Look around without changing direction"));
        this.registerSetting(invertPitch = new TickSetting("Invert Pitch", false));
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;

        // Save original state
        previousPerspective = mc.gameSettings.thirdPersonView;
        originalYaw = mc.thePlayer.rotationYaw;
        originalPitch = mc.thePlayer.rotationPitch;

        // Initialize camera angles
        cameraYaw = originalYaw;
        cameraPitch = invertPitch.isEnabled() ? -originalPitch : originalPitch;

        isFreeLooking = true;

        // Switch to third person
        mc.gameSettings.thirdPersonView = 1;

        // Disable key repeat events to prevent issues
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;

        // Restore original rotation
        mc.thePlayer.rotationYaw = originalYaw;
        mc.thePlayer.rotationPitch = originalPitch;
        mc.thePlayer.prevRotationYaw = originalYaw;
        mc.thePlayer.prevRotationPitch = originalPitch;

        // Restore perspective
        mc.gameSettings.thirdPersonView = previousPerspective;

        isFreeLooking = false;
    }

    @Override
    public void onUpdate() {
        if (!enabled || !isInGame()) return;
        if (mc.thePlayer == null) return;

        // Note: Hold/Toggle behavior is handled by the keybind system

        // Handle mouse movement for camera
        if (mc.currentScreen == null) {
            handleMouseMovement();
        }

        // Force third person view
        mc.gameSettings.thirdPersonView = 1;

        // Apply camera rotation to player (visual only)
        float displayPitch = invertPitch.isEnabled() ? -cameraPitch : cameraPitch;
        mc.thePlayer.rotationYaw = cameraYaw;
        mc.thePlayer.rotationPitch = displayPitch;
        mc.thePlayer.prevRotationYaw = cameraYaw;
        mc.thePlayer.prevRotationPitch = displayPitch;
    }

    /**
     * Handle mouse movement for camera rotation
     */
    private void handleMouseMovement() {
        // Get mouse sensitivity (same calculation as Minecraft)
        float sensitivity = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float sensitivityFactor = sensitivity * sensitivity * sensitivity * 1.5F;

        // Get mouse delta
        int deltaX = Mouse.getDX();
        int deltaY = Mouse.getDY();

        // Apply to camera angles
        cameraYaw += deltaX * sensitivityFactor * 0.15F;
        cameraPitch -= deltaY * sensitivityFactor * 0.15F;

        // Clamp pitch
        cameraPitch = MathHelper.clamp_float(cameraPitch, -90.0F, 90.0F);
    }

    /**
     * Get the original yaw for movement calculations
     * Can be called by other modules that need to know the actual facing direction
     */
    public float getOriginalYaw() {
        return originalYaw;
    }

    /**
     * Get the original pitch
     */
    public float getOriginalPitch() {
        return originalPitch;
    }

    /**
     * Check if currently free looking
     */
    public boolean isFreeLooking() {
        return enabled && isFreeLooking;
    }

    /**
     * Get the camera yaw (where the player is looking visually)
     */
    public float getCameraYaw() {
        return cameraYaw;
    }

    /**
     * Get the camera pitch
     */
    public float getCameraPitch() {
        return cameraPitch;
    }
}
