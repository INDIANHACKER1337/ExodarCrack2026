/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.world;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.TickSetting;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import net.minecraft.block.BlockAir;
import java.lang.reflect.Method;

/**
 * BridgeAssist - Aligns player rotation for bridging techniques
 *
 * Supports 4 bridging modes:
 * - Normal: Standard 78° pitch bridging
 * - GodBridge: 75.6° pitch fast bridging
 * - Moonwalk: 79.6° pitch backward bridging
 * - Breezily: 79.9° pitch advanced bridging
 *
 * Detects when player is over air while on ground and snaps rotation
 * to the nearest valid bridging angle within assist range.
 */
public class BridgeAssist extends Module {
    private static final String NORMAL = "Normal";
    private static final String GODBRIDGE = "GodBridge";
    private static final String MOONWALK = "Moonwalk";
    private static final String BREEZILY = "Breezily";
    private static final String[] BRIDGE_MODES = new String[]{NORMAL, GODBRIDGE, MOONWALK, BREEZILY};

    private final TickSetting onlyWhenSneaking;
    private final SliderSetting waitTimeMs;
    private final SliderSetting alignmentSpeed;
    private final SliderSetting alignmentRange;
    private final DescriptionSetting description;
    private final ModeSetting bridgeMode;

    private final float[] godbridgeAngles = {75.6f, -315, -225, -135, -45, 0, 45, 135, 225, 315};
    private final float[] moonwalkAngles = {79.6f, -340, -290, -250, -200, -160, -110, -70, -20, 0, 20, 70, 110, 160, 200, 250, 290, 340};
    private final float[] breezilyAngles = {79.9f, -360, -270, -180, -90, 0, 90, 180, 270, 360};
    private final float[] normalAngles = {78f, -315, -225, -135, -45, 0, 45, 135, 225, 315};

    private boolean waitingForAlignment;
    private boolean smoothAligning;
    private long alignmentStartTime;
    private double rotationSpeedYaw, rotationSpeedPitch;
    private float targetYaw, targetPitch;

    public BridgeAssist() {
        super("BridgeAssist", ModuleCategory.MISC);
        this.registerSetting(description = new DescriptionSetting("Aligns you for bridging"));
        this.registerSetting(waitTimeMs = new SliderSetting("Wait time (ms)", 500, 0, 5000, 25));
        this.registerSetting(bridgeMode = new ModeSetting("Mode", BRIDGE_MODES));
        this.registerSetting(onlyWhenSneaking = new TickSetting("Work only when sneaking", true));
        this.registerSetting(alignmentRange = new SliderSetting("Assist range", 10.0D, 1.0D, 40.0D, 1.0D));
        this.registerSetting(alignmentSpeed = new SliderSetting("Align speed", 150, 1, 201, 5));
    }

    @Override
    public void onEnable() {
        this.waitingForAlignment = false;
        this.smoothAligning = false;
    }

    @Override
    public void onUpdate() {
        if (!isValidEnvironment()) {
            return;
        }

        if (!isPlayerOverAirAndGrounded()) {
            return;
        }

        if (onlyWhenSneaking.isEnabled()) {
            if (!getPlayer().isSneaking()) {
                return;
            }
        }

        if (smoothAligning) {
            performSmoothAlignment();
            return;
        }

        if (!waitingForAlignment) {
            waitingForAlignment = true;
            alignmentStartTime = System.currentTimeMillis();
            return;
        }

        if (System.currentTimeMillis() - alignmentStartTime < waitTimeMs.getValue())
            return;

        float rawYaw = getPlayer().rotationYaw;
        float rawPitch = getPlayer().rotationPitch;

        float normalizedYaw = rawYaw - ((int) rawYaw / 360) * 360;
        float normalizedPitch = rawPitch - ((int) rawPitch / 360) * 360;

        float range = (float) alignmentRange.getValue();

        //GodBridge
        if (bridgeMode.getSelected().equals(GODBRIDGE)) {
            if (godbridgeAngles[0] >= (normalizedPitch - range) && godbridgeAngles[0] <= (normalizedPitch + range)) {
                for (int k = 1; k < godbridgeAngles.length; k++) {
                    if (godbridgeAngles[k] >= (normalizedYaw - range) && godbridgeAngles[k] <= (normalizedYaw + range)) {
                        alignToAngles(godbridgeAngles[0], godbridgeAngles[k], rawYaw, rawPitch);
                        this.waitingForAlignment = false;
                        return;
                    }
                }
            }
        }

        //Moonwalk
        else if (bridgeMode.getSelected().equals(MOONWALK)) {
            if (moonwalkAngles[0] >= (normalizedPitch - range) && moonwalkAngles[0] <= (normalizedPitch + range)) {
                for (int k = 1; k < moonwalkAngles.length; k++) {
                    if (moonwalkAngles[k] >= (normalizedYaw - range) && moonwalkAngles[k] <= (normalizedYaw + range)) {
                        alignToAngles(moonwalkAngles[0], moonwalkAngles[k], rawYaw, rawPitch);
                        this.waitingForAlignment = false;
                        return;
                    }
                }
            }
        }

        //Breezily
        else if (bridgeMode.getSelected().equals(BREEZILY)) {
            if (breezilyAngles[0] >= (normalizedPitch - range) && breezilyAngles[0] <= (normalizedPitch + range)) {
                for (int k = 1; k < breezilyAngles.length; k++) {
                    if (breezilyAngles[k] >= (normalizedYaw - range) && breezilyAngles[k] <= (normalizedYaw + range)) {
                        alignToAngles(breezilyAngles[0], breezilyAngles[k], rawYaw, rawPitch);
                        this.waitingForAlignment = false;
                        return;
                    }
                }
            }
        }

        //Normal
        else {
            if (normalAngles[0] >= (normalizedPitch - range) && normalAngles[0] <= (normalizedPitch + range)) {
                for (int k = 1; k < normalAngles.length; k++) {
                    if (normalAngles[k] >= (normalizedYaw - range) && normalAngles[k] <= (normalizedYaw + range)) {
                        alignToAngles(normalAngles[0], normalAngles[k], rawYaw, rawPitch);
                        this.waitingForAlignment = false;
                        return;
                    }
                }
            }
        }
        this.waitingForAlignment = false;
    }

    private void performSmoothAlignment() {
        float rawYaw = getPlayer().rotationYaw;
        float rawPitch = getPlayer().rotationPitch;

        float normalizedYaw = rawYaw - ((int) rawYaw / 360) * 360;
        float normalizedPitch = rawPitch - ((int) rawPitch / 360) * 360;

        double yawDeltaMinus = normalizedYaw - rotationSpeedYaw,
                yawDeltaPlus = normalizedYaw + rotationSpeedYaw,
                pitchDeltaMinus = normalizedPitch - rotationSpeedPitch,
                pitchDeltaPlus = normalizedPitch + rotationSpeedPitch;

        yawDeltaMinus = Math.abs(yawDeltaMinus);
        yawDeltaPlus = Math.abs(yawDeltaPlus);
        pitchDeltaMinus = Math.abs(pitchDeltaMinus);
        pitchDeltaPlus = Math.abs(pitchDeltaPlus);

        if (this.rotationSpeedYaw > yawDeltaMinus || this.rotationSpeedYaw > yawDeltaPlus)
            getPlayer().rotationYaw = this.targetYaw;

        if (this.rotationSpeedPitch > pitchDeltaMinus || this.rotationSpeedPitch > pitchDeltaPlus)
            getPlayer().rotationPitch = this.targetPitch;

        if (getPlayer().rotationYaw < this.targetYaw)
            getPlayer().rotationYaw += this.rotationSpeedYaw;

        if (getPlayer().rotationYaw > this.targetYaw)
            getPlayer().rotationYaw -= this.rotationSpeedYaw;

        if (getPlayer().rotationPitch > this.targetPitch)
            getPlayer().rotationPitch -= this.rotationSpeedPitch;

        if (getPlayer().rotationYaw == this.targetYaw && getPlayer().rotationPitch == this.targetPitch) {
            smoothAligning = false;
            this.waitingForAlignment = false;
        }
    }

    public void alignToAngles(float pitch, float yaw, float rawYaw, float rawPitch) {
        getPlayer().rotationPitch = pitch + ((int) rawPitch / 360) * 360;
        getPlayer().rotationYaw = yaw;
    }

    private boolean isValidEnvironment() {
        return mc != null && getPlayer() != null && getWorld() != null;
    }

    private boolean isPlayerOverAirAndGrounded() {
        if (!getPlayer().onGround) {
            return false;
        }

        // Use reflection for BlockPos (same as ScaffoldLegit)
        try {
            Class<?> blockPosClass = Class.forName("net.minecraft.util.BlockPos");
            Object blockPos = blockPosClass.getConstructor(double.class, double.class, double.class)
                    .newInstance(getPlayer().posX, getPlayer().posY - 1.0, getPlayer().posZ);
            Method isAirBlockMethod = getWorld().getClass().getMethod("isAirBlock", blockPosClass);
            return (Boolean) isAirBlockMethod.invoke(getWorld(), blockPos);
        } catch (Exception e) {
            return false;
        }
    }
}
