/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.Minecraft;

/**
 * AntiAim - Visual-only rotations (client-side meme feature)
 * The pitch is applied during render phase so it doesn't affect camera
 * Based on Exhibition client implementation
 */
public class AntiAim extends Module {

    // Static instance for render access
    private static AntiAim instance;

    // Yaw settings
    private ModeSetting yawMode;
    private SliderSetting spinSpeed;

    // Pitch settings
    private ModeSetting pitchMode;

    // Body settings
    private TickSetting affectBody;
    private TickSetting smoothBody;
    private SliderSetting smoothSpeed;

    // State tracking
    private float spinYaw = 0;
    private float currentBodyYaw = 0;
    private float targetBodyYaw = 0;

    // Jitter/fake states
    private boolean jitterState = false;
    private boolean fakeState = false;
    private long lastFakeTime = 0;

    // Pitch state
    private float currentPitch = 0;
    private boolean pitchFlip = false;

    // Calculated values for render
    private float renderYawOffset = 0;
    private float renderPitchOffset = 0;

    public AntiAim() {
        super("AntiAim", ModuleCategory.MISC);
        instance = this;

        // Yaw modes from Exhibition (visible)
        this.registerSetting(yawMode = new ModeSetting("Yaw", new String[]{
            "None", "Spin", "SpinFast", "SpinSlow",
            "Jitter", "FakeJitter", "FakeHead",
            "Reverse", "Sideways", "Freestanding", "Lisp"
        }));

        // Pitch modes from Exhibition (visible)
        this.registerSetting(pitchMode = new ModeSetting("Pitch", new String[]{
            "Normal", "Down", "Up", "Zero", "Stutter", "Flip", "Meme"
        }));

        // Spin Speed (visible)
        this.registerSetting(spinSpeed = new SliderSetting("Spin Speed", 15.0, 1.0, 50.0, 1.0));

        // Body settings (hidden - internal defaults)
        affectBody = new TickSetting("Affect Body", true);
        smoothBody = new TickSetting("Smooth Body", false);
        smoothSpeed = new SliderSetting("Smooth Speed", 0.10, 0.05, 1.0, 0.05);

        // Set defaults
        yawMode.setSelected("Spin");
        pitchMode.setSelected("Normal");
    }

    @Override
    public void onEnable() {
        spinYaw = 0;
        currentBodyYaw = 0;
        targetBodyYaw = 0;
        jitterState = false;
        fakeState = false;
        lastFakeTime = System.currentTimeMillis();
        currentPitch = 0;
        pitchFlip = false;
        renderYawOffset = 0;
        renderPitchOffset = 0;

        if (mc != null && mc.thePlayer != null) {
            currentBodyYaw = mc.thePlayer.rotationYaw;
        }
    }

    @Override
    public void onDisable() {
        renderYawOffset = 0;
        renderPitchOffset = 0;
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;
        if (mc == null || mc.thePlayer == null) return;

        long now = System.currentTimeMillis();
        float baseYaw = mc.thePlayer.rotationYaw;

        // Calculate yaw offset based on mode
        renderYawOffset = calculateYawOffset(now);

        // Calculate pitch offset based on mode
        renderPitchOffset = calculatePitchOffset(now);

        // Calculate target head yaw
        float targetHeadYaw = baseYaw + renderYawOffset;

        // Apply head yaw rotation (this is safe - doesn't affect camera)
        mc.thePlayer.rotationYawHead = targetHeadYaw;
        mc.thePlayer.prevRotationYawHead = targetHeadYaw;

        // Apply body rotation (can be smoothed)
        if (affectBody.isEnabled()) {
            targetBodyYaw = targetHeadYaw;

            if (smoothBody.isEnabled()) {
                // Smooth interpolation for body
                float speed = (float) smoothSpeed.getValue();
                currentBodyYaw = lerp(currentBodyYaw, targetBodyYaw, speed);

                // Wrap angle to prevent issues
                float diff = targetBodyYaw - currentBodyYaw;
                if (diff > 180) currentBodyYaw += 360;
                if (diff < -180) currentBodyYaw -= 360;
            } else {
                currentBodyYaw = targetBodyYaw;
            }

            mc.thePlayer.renderYawOffset = currentBodyYaw;
            mc.thePlayer.prevRenderYawOffset = currentBodyYaw;
        }

        // Note: Pitch is applied in CustomRenderPlayer during render phase
        // so it doesn't affect the camera
    }

    /**
     * Calculate yaw offset based on selected mode
     */
    private float calculateYawOffset(long now) {
        String mode = yawMode.getSelected();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return 0;

        switch (mode) {
            case "Spin":
                spinYaw += spinSpeed.getValue();
                if (spinYaw > 360) spinYaw -= 360;
                return spinYaw;

            case "SpinFast":
                spinYaw += 45;
                if (spinYaw > 360) spinYaw -= 360;
                return spinYaw;

            case "SpinSlow":
                spinYaw += 10;
                if (spinYaw > 360) spinYaw -= 360;
                return spinYaw;

            case "Jitter":
                jitterState = (mc.thePlayer.ticksExisted % 2 == 0);
                return jitterState ? -45 : 135;

            case "FakeJitter":
                if (now - lastFakeTime > 350) {
                    fakeState = !fakeState;
                    lastFakeTime = now;
                }
                return fakeState ? 89.9f : -89.9f;

            case "FakeHead":
                if (now - lastFakeTime > 1100) {
                    fakeState = !fakeState;
                    lastFakeTime = now;
                }
                return fakeState ? 89.9f : -89.9f;

            case "Reverse":
                return 178.9f;

            case "Sideways":
                return -90f;

            case "Freestanding":
                return (float) (5 + Math.random() * 175);

            case "Lisp":
                spinYaw += 150;
                return spinYaw;

            case "None":
            default:
                return 0;
        }
    }

    /**
     * Calculate pitch offset based on selected mode
     * Returns the TARGET pitch value (not offset)
     */
    private float calculatePitchOffset(long now) {
        String mode = pitchMode.getSelected();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return 0;

        float basePitch = mc.thePlayer.rotationPitch;

        switch (mode) {
            case "Down":
                return 89.9f; // Look down

            case "Up":
                return -89.9f; // Look up

            case "Zero":
                return 0f; // Look straight

            case "Stutter":
                pitchFlip = !pitchFlip;
                return pitchFlip ? 89.9f : -45f;

            case "Flip":
                // Invert pitch
                return -basePitch;

            case "Meme":
                currentPitch += 15;
                if (currentPitch > 90) currentPitch = -90;
                return currentPitch;

            case "Normal":
            default:
                return basePitch; // Keep original pitch
        }
    }

    /**
     * Linear interpolation for smooth animations
     */
    private float lerp(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    // =====================================================
    // STATIC METHODS FOR CUSTOMRENDERPLAYER
    // =====================================================

    /**
     * Check if AntiAim is active and should modify render
     */
    public static boolean isActive() {
        return instance != null && instance.enabled;
    }

    /**
     * Check if pitch should be modified
     */
    public static boolean shouldModifyPitch() {
        if (instance == null || !instance.enabled) return false;
        return !instance.pitchMode.getSelected().equals("Normal");
    }

    /**
     * Get the render pitch value
     */
    public static float getRenderPitch() {
        if (instance == null) return 0;
        return instance.renderPitchOffset;
    }

    /**
     * Get the render yaw offset
     */
    public static float getRenderYawOffset() {
        if (instance == null) return 0;
        return instance.renderYawOffset;
    }

    @Override
    public String getDisplaySuffix() {
        return " " + yawMode.getSelected();
    }
}
