/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.setting.*;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

/**
 * LegitAura3 - True Silent Aim
 *
 * How it works:
 * 1. Camera stays where YOU look (player.rotationYaw unchanged)
 * 2. Model rotates toward target via OpenGL (visual feedback in F5)
 * 3. Rotation packets sent to server with aim rotation
 * 4. Raycast overridden so clicks hit the target (like Penetration)
 *
 * Result: You see your screen normally, model visually aims at target,
 * server thinks you're aiming at target, clicks hit the target.
 */
public class LegitAura3 extends Module {

    // Settings
    private ModeSetting mode;
    private SliderSetting speed;
    private TickSetting dynamicSpeed;
    private SliderSetting verticalSpeed;
    private SliderSetting attackRange;
    private SliderSetting hitRange;
    private SliderSetting minDistance;
    private SliderSetting maxAngle;
    private TickSetting clickAim;
    private TickSetting stickyTarget;
    private TickSetting weaponOnly;
    private TickSetting aimInvis;
    private TickSetting aimVertically;
    private TickSetting aimThroughWalls;

    // AutoClicker settings
    private TickSetting autoClick;
    private SliderSetting minCPS;
    private SliderSetting maxCPS;

    // Render rotations (used by CustomRenderPlayer for OpenGL rotation)
    private static float renderYaw = 0;
    private static float renderPitch = 0;
    private static boolean hasRenderRotation = false;

    // Server rotations (sent in packets)
    private static float serverYaw = 0;
    private static float serverPitch = 0;

    // Smooth rotation tracking
    private float currentRenderYaw = 0;
    private float currentRenderPitch = 0;

    // State
    private static boolean isActive = false;
    private static LegitAura3 instance = null;

    // Target tracking
    private EntityPlayer currentTarget = null;
    private EntityPlayer stickyModeTarget = null;
    private Random random = new Random();

    // AutoClicker
    private Thread clickThread;
    private volatile boolean clickThreadRunning = false;
    private Field keyBindAttackField = null;
    private Field gameSettingsField = null;

    // Packet field reflection
    private static Field yawField = null;
    private static Field pitchField = null;
    private static boolean fieldsInitialized = false;

    public LegitAura3() {
        super("LegitAura3", ModuleCategory.COMBAT);
        instance = this;

        this.registerSetting(new DescriptionSetting("True silent aim + raycast"));
        // Modes like AimAssist: Normal, Center, Nearest
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Normal", "Center", "Nearest"}));

        // Speed settings (like AimAssist)
        this.registerSetting(speed = new SliderSetting("Speed", 15.0, 1.0, 50.0, 0.5));
        this.registerSetting(dynamicSpeed = new TickSetting("Dynamic Speed", true));
        this.registerSetting(aimVertically = new TickSetting("Aim Vertically", true));
        this.registerSetting(verticalSpeed = new SliderSetting("Vertical Speed", 10.0, 1.0, 50.0, 0.5));

        // Range settings
        this.registerSetting(attackRange = new SliderSetting("Attack Range", 4.5, 1.0, 8.0, 0.1));
        this.registerSetting(hitRange = new SliderSetting("Range", 3.0, 3.0, 6.0, 0.05));
        this.registerSetting(minDistance = new SliderSetting("Min Distance", 0.0, 0.0, 2.0, 0.1));
        this.registerSetting(maxAngle = new SliderSetting("Max Angle", 90.0, 15.0, 180.0, 5.0));

        // Behavior
        this.registerSetting(clickAim = new TickSetting("Click Aim", true));
        this.registerSetting(stickyTarget = new TickSetting("Sticky Target", true));
        this.registerSetting(weaponOnly = new TickSetting("Weapon Only", false));
        this.registerSetting(aimInvis = new TickSetting("Aim Invis", false));
        this.registerSetting(aimThroughWalls = new TickSetting("Through Walls", false));

        // AutoClicker
        this.registerSetting(autoClick = new TickSetting("Auto Click", true));
        this.registerSetting(minCPS = new SliderSetting("Min CPS", 10.0, 1.0, 20.0, 0.5));
        this.registerSetting(maxCPS = new SliderSetting("Max CPS", 14.0, 1.0, 20.0, 0.5));
    }

    @Override
    public void onEnable() {
        isActive = false;
        hasRenderRotation = false;
        currentTarget = null;
        stickyModeTarget = null;

        EntityPlayerSP player = getPlayer();
        if (player != null) {
            renderYaw = player.rotationYaw;
            renderPitch = player.rotationPitch;
            serverYaw = player.rotationYaw;
            serverPitch = player.rotationPitch;
            currentRenderYaw = player.rotationYaw;
            currentRenderPitch = player.rotationPitch;
        }

        initKeyBindFields();

        if (autoClick.isEnabled()) {
            startClickThread();
        }
    }

    @Override
    public void onDisable() {
        isActive = false;
        hasRenderRotation = false;
        currentTarget = null;
        stickyModeTarget = null;
        stopClickThread();
    }

    @Override
    public void onUpdate() {
        if (!isInGame()) return;
        if (mc.currentScreen != null) {
            isActive = false;
            hasRenderRotation = false;
            return;
        }

        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        // Sync when not active
        if (!isActive) {
            serverYaw = player.rotationYaw;
            serverPitch = player.rotationPitch;
            currentRenderYaw = player.rotationYaw;
            currentRenderPitch = player.rotationPitch;
        }

        // Check click aim
        if (clickAim.isEnabled() && !autoClick.isEnabled() && !Mouse.isButtonDown(0)) {
            isActive = false;
            hasRenderRotation = false;
            currentTarget = null;
            stickyModeTarget = null;
            currentRenderYaw = player.rotationYaw;
            currentRenderPitch = player.rotationPitch;
            return;
        }

        // Check weapon only
        if (weaponOnly.isEnabled()) {
            if (player.getHeldItem() == null) {
                isActive = false;
                hasRenderRotation = false;
                return;
            }
            String itemName = player.getHeldItem().getUnlocalizedName().toLowerCase();
            if (!itemName.contains("sword") && !itemName.contains("axe")) {
                isActive = false;
                hasRenderRotation = false;
                return;
            }
        }

        // Find target
        EntityPlayer target = findTarget(player);
        if (target == null) {
            isActive = false;
            hasRenderRotation = false;
            currentTarget = null;
            stickyModeTarget = null;
            currentRenderYaw = player.rotationYaw;
            currentRenderPitch = player.rotationPitch;
            return;
        }

        currentTarget = target;

        // Calculate rotations to target
        float[] rotations = getRotationsToEntity(player, target);
        float targetYaw = rotations[0];
        float targetPitch = rotations[1];

        // Check FOV (180 = no restriction)
        float yawDiff = normalizeAngle(targetYaw - player.rotationYaw);
        double maxFovCheck = maxAngle.getValue();
        if (maxFovCheck < 180 && Math.abs(yawDiff) > maxFovCheck) {
            isActive = false;
            hasRenderRotation = false;
            return;
        }

        // NOW we're active
        isActive = true;

        // Apply rotation based on mode (like AimAssist)
        String selectedMode = mode.getSelected();

        if (selectedMode.equals("Normal")) {
            applyNormalModeRotation(player, target);
        } else if (selectedMode.equals("Center")) {
            applyCenterModeRotation(targetYaw, targetPitch);
        } else if (selectedMode.equals("Nearest")) {
            applyNearestModeRotation(targetYaw, targetPitch);
        }

        // Store rotations for render and packets
        renderYaw = currentRenderYaw;
        renderPitch = currentRenderPitch;
        serverYaw = currentRenderYaw;
        serverPitch = currentRenderPitch;
        hasRenderRotation = true;
    }

    /**
     * CENTER MODE - Direct aim toward center
     */
    private void applyCenterModeRotation(float targetYaw, float targetPitch) {
        float yawDiff = normalizeAngle(targetYaw - currentRenderYaw);
        double hSpeed = calculateDynamicSpeed(speed.getValue(), Math.abs(yawDiff));

        double centerDeadzone = 1.0;
        if (Math.abs(yawDiff) > centerDeadzone) {
            double deltaYaw;
            if (yawDiff > 0) {
                deltaYaw = Math.min(hSpeed, yawDiff);
            } else {
                deltaYaw = Math.max(-hSpeed, yawDiff);
            }
            deltaYaw += (random.nextDouble() - 0.5) * 0.3;
            currentRenderYaw += (float) deltaYaw;
        }
        currentRenderYaw = normalizeAngle(currentRenderYaw);

        // Vertical
        if (aimVertically.isEnabled()) {
            float pitchDiff = targetPitch - currentRenderPitch;
            double vSpeed = calculateDynamicSpeed(verticalSpeed.getValue(), Math.abs(pitchDiff)) * 0.7;

            if (Math.abs(pitchDiff) > 1.0) {
                double deltaPitch;
                if (pitchDiff > 0) {
                    deltaPitch = Math.min(vSpeed, pitchDiff);
                } else {
                    deltaPitch = Math.max(-vSpeed, pitchDiff);
                }
                currentRenderPitch += (float) deltaPitch;
                currentRenderPitch = Math.max(-90, Math.min(90, currentRenderPitch));
            }
        }
    }

    /**
     * NEAREST MODE - Gradual approach
     */
    private void applyNearestModeRotation(float targetYaw, float targetPitch) {
        float yawDiff = normalizeAngle(targetYaw - currentRenderYaw);
        double hSpeed = calculateDynamicSpeed(speed.getValue(), Math.abs(yawDiff));

        double nearestDeadzone = 2.0;
        if (Math.abs(yawDiff) > nearestDeadzone) {
            double speedFactor = Math.min(1.0, Math.abs(yawDiff) / 20.0);
            double actualSpeed = hSpeed * speedFactor;

            double deltaYaw = yawDiff * (actualSpeed / 20.0);
            deltaYaw = Math.max(-actualSpeed * 0.5, Math.min(actualSpeed * 0.5, deltaYaw));

            currentRenderYaw += (float) deltaYaw;
        }
        currentRenderYaw = normalizeAngle(currentRenderYaw);

        // Vertical
        if (aimVertically.isEnabled()) {
            float pitchDiff = targetPitch - currentRenderPitch;
            double vSpeed = calculateDynamicSpeed(verticalSpeed.getValue(), Math.abs(pitchDiff)) * 0.7;

            double pitchDeadzone = 2.0;
            if (Math.abs(pitchDiff) > pitchDeadzone) {
                double speedFactor = Math.min(1.0, Math.abs(pitchDiff) / 20.0);
                double actualSpeed = vSpeed * speedFactor;

                double deltaPitch = pitchDiff * (actualSpeed / 20.0);
                deltaPitch = Math.max(-actualSpeed * 0.5, Math.min(actualSpeed * 0.5, deltaPitch));

                currentRenderPitch += (float) deltaPitch;
                currentRenderPitch = Math.max(-90, Math.min(90, currentRenderPitch));
            }
        }
    }

    /**
     * NORMAL MODE - Bounding box corner calculation (like AimAssist)
     */
    private void applyNormalModeRotation(EntityPlayerSP player, EntityPlayer target) {
        double eyeX = player.posX;
        double eyeY = player.posY + player.getEyeHeight();
        double eyeZ = player.posZ;

        // Get target center
        double targetX = target.posX;
        double targetY = target.posY + target.height / 2.0;
        double targetZ = target.posZ;

        float[] rots = calculateRotation(eyeX, eyeY, eyeZ, targetX, targetY, targetZ);
        float targetYaw = rots[0];
        float targetPitch = rots[1];

        float yawToAdd = normalizeAngle(targetYaw - currentRenderYaw);
        float pitchToAdd = targetPitch - currentRenderPitch;

        // Get target bounding box
        double bbMinX = target.posX - target.width / 2.0;
        double bbMaxX = target.posX + target.width / 2.0;
        double bbMinY = target.posY;
        double bbMaxY = target.posY + target.height;
        double bbMinZ = target.posZ - target.width / 2.0;
        double bbMaxZ = target.posZ + target.width / 2.0;

        // Calculate 4 horizontal corners
        double[][] corners = {
            {bbMinX, bbMinY, bbMaxZ},
            {bbMaxX, bbMinY, bbMinZ},
            {bbMaxX, bbMinY, bbMaxZ},
            {bbMinX, bbMinY, bbMinZ}
        };

        double minYaw = 361, maxYaw = -361;
        for (double[] corner : corners) {
            float[] rot = calculateRotation(eyeX, eyeY, eyeZ, corner[0], corner[1], corner[2]);
            double yaw = rot[0];
            if (yaw < 0) yaw += 360;
            if (yaw < minYaw) minYaw = yaw;
            if (yaw > maxYaw) maxYaw = yaw;
        }

        if (minYaw > 180) minYaw -= 360;
        if (maxYaw > 180) maxYaw -= 360;

        // Pitch bounds
        float[] topRot = calculateRotation(eyeX, eyeY, eyeZ, bbMaxX, bbMaxY, bbMaxZ);
        float[] botRot = calculateRotation(eyeX, eyeY, eyeZ, bbMinX, bbMinY, bbMinZ);
        double minPitch = topRot[1];
        double maxPitch = botRot[1];

        // Speed multiplier
        double yawMultiplier = speed.getValue() / 15.0;
        double pitchMultiplier = verticalSpeed.getValue() / 15.0;

        // Dynamic speed boost
        double dynamicBoost = 0;
        if (dynamicSpeed.isEnabled()) {
            double absYawDiff = Math.abs(yawToAdd);
            if (absYawDiff > 90.0) dynamicBoost = 8.0;
            else if (absYawDiff > 45.0) dynamicBoost = 5.0;
            else if (absYawDiff > 20.0) dynamicBoost = 2.5;
        }

        // YAW ADJUSTMENT
        double maxDelta = Math.abs(normalizeAngle((float)(maxYaw - minYaw))) / 2.0;
        double yawDifference = Math.abs(yawToAdd) - maxDelta;

        if (Math.abs(yawToAdd) > maxDelta) {
            double targetAdjustment;
            double effectiveYawMult = yawMultiplier + dynamicBoost;

            if (yawDifference < 2.0) {
                targetAdjustment = yawDifference * (yawToAdd > 0 ? 1.0 : -1.0);
            } else if (yawDifference >= 16.0) {
                targetAdjustment = (3.5 + random.nextDouble() * 3.5) * effectiveYawMult * (yawToAdd > 0 ? 1.0 : -1.0);
            } else if (yawDifference >= 8.0) {
                targetAdjustment = (2.0 + random.nextDouble() * 1.5) * effectiveYawMult * (yawToAdd > 0 ? 1.0 : -1.0);
            } else if (yawDifference >= 4.0) {
                targetAdjustment = (0.8 + random.nextDouble() * 1.0) * effectiveYawMult * (yawToAdd > 0 ? 1.0 : -1.0);
            } else {
                targetAdjustment = (0.2 + random.nextDouble() * 0.6) * effectiveYawMult * (yawToAdd > 0 ? 1.0 : -1.0);
            }

            currentRenderYaw += (float) targetAdjustment;
        }
        currentRenderYaw = normalizeAngle(currentRenderYaw);

        // PITCH ADJUSTMENT
        if (aimVertically.isEnabled()) {
            double maxPitchDelta = Math.abs(maxPitch - minPitch) / 2.0;
            double pitchDifference = Math.abs(pitchToAdd) - maxPitchDelta;

            if (Math.abs(pitchToAdd) > maxPitchDelta) {
                double targetAdjustment;
                double effectivePitchMult = pitchMultiplier + (dynamicBoost * 0.5);

                if (pitchDifference < 2.0) {
                    targetAdjustment = pitchDifference * (pitchToAdd > 0 ? 1.0 : -1.0);
                } else if (pitchDifference >= 16.0) {
                    targetAdjustment = (3.5 + random.nextDouble() * 3.5) * effectivePitchMult * (pitchToAdd > 0 ? 1.0 : -1.0);
                } else if (pitchDifference >= 8.0) {
                    targetAdjustment = (2.0 + random.nextDouble() * 1.5) * effectivePitchMult * (pitchToAdd > 0 ? 1.0 : -1.0);
                } else if (pitchDifference >= 4.0) {
                    targetAdjustment = (0.8 + random.nextDouble() * 1.0) * effectivePitchMult * (pitchToAdd > 0 ? 1.0 : -1.0);
                } else {
                    targetAdjustment = (0.2 + random.nextDouble() * 0.6) * effectivePitchMult * (pitchToAdd > 0 ? 1.0 : -1.0);
                }

                currentRenderPitch += (float) targetAdjustment;
                currentRenderPitch = Math.max(-90, Math.min(90, currentRenderPitch));
            }
        }
    }

    private double calculateDynamicSpeed(double baseSpeed, double angleDiff) {
        if (!dynamicSpeed.isEnabled()) {
            return baseSpeed;
        }
        double maxBonus = 20.0;
        double maxAngleForBonus = 90.0;
        double bonus = (Math.min(angleDiff, maxAngleForBonus) / maxAngleForBonus) * maxBonus;
        return baseSpeed + bonus;
    }

    private float[] calculateRotation(double fromX, double fromY, double fromZ,
                                      double toX, double toY, double toZ) {
        double deltaX = toX - fromX;
        double deltaY = toY - fromY;
        double deltaZ = toZ - fromZ;

        double distXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(Math.atan2(deltaY, distXZ) * 180.0 / Math.PI);

        return new float[]{yaw, pitch};
    }

    // =====================================
    // RAYCAST OVERRIDE (like Penetration)
    // Called from Main.onGetMouseOver hook
    // =====================================

    /**
     * Override raycast to target our aim target
     * This makes clicks hit the target even though crosshair isn't on them
     */
    public void overrideRaycast() {
        if (!enabled || !isActive || currentTarget == null) return;

        // Check if target is in hit range
        if (mc.thePlayer == null) return;
        double dist = mc.thePlayer.getDistanceToEntity(currentTarget);
        if (dist > hitRange.getValue()) return;

        // Calculate hit position on target using SERVER rotation
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0F);

        // Create look vector from server rotation
        float yawRad = (float) Math.toRadians(-serverYaw - 180);
        float pitchRad = (float) Math.toRadians(-serverPitch);
        float cosYaw = (float) Math.cos(yawRad);
        float sinYaw = (float) Math.sin(yawRad);
        float cosPitch = (float) Math.cos(pitchRad);
        float sinPitch = (float) Math.sin(pitchRad);

        Vec3 lookVec = new Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
        Vec3 reachVec = eyePos.addVector(
            lookVec.xCoord * hitRange.getValue(),
            lookVec.yCoord * hitRange.getValue(),
            lookVec.zCoord * hitRange.getValue()
        );

        // Check intersection with target
        float borderSize = currentTarget.getCollisionBorderSize();
        AxisAlignedBB targetBox = currentTarget.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
        MovingObjectPosition intercept = targetBox.calculateIntercept(eyePos, reachVec);

        Vec3 hitVec;
        if (intercept != null) {
            hitVec = intercept.hitVec;
        } else {
            // Fallback to target center
            hitVec = new Vec3(currentTarget.posX, currentTarget.posY + currentTarget.height / 2, currentTarget.posZ);
        }

        // Override Minecraft's raycast result
        mc.objectMouseOver = new MovingObjectPosition(currentTarget, hitVec);
        mc.pointedEntity = currentTarget;
    }

    // =====================================
    // PACKET MODIFICATION
    // =====================================

    @Override
    public boolean onSendPacket(Object packet) {
        if (!isActive || !enabled) return true;

        if (packet instanceof C03PacketPlayer) {
            C03PacketPlayer c03 = (C03PacketPlayer) packet;
            String className = packet.getClass().getSimpleName();

            // Only modify rotation packets
            if (className.contains("C05") || className.contains("C06") ||
                className.contains("Look") || className.contains("PosLook")) {

                if (!fieldsInitialized) {
                    initPacketFields(c03);
                }

                if (yawField != null && pitchField != null) {
                    try {
                        yawField.setFloat(c03, serverYaw);
                        pitchField.setFloat(c03, serverPitch);
                    } catch (Exception e) {
                        // Silent fail
                    }
                }
            }
        }

        return true;
    }

    private static void initPacketFields(C03PacketPlayer packet) {
        try {
            Field[] floatFields = new Field[4];
            int idx = 0;

            for (Field f : C03PacketPlayer.class.getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getType() == float.class && idx < 4) {
                    floatFields[idx++] = f;
                }
            }

            // In C03PacketPlayer: x, y, z are doubles, then yaw, pitch are floats
            // So the first two floats should be yaw and pitch
            if (idx >= 2) {
                yawField = floatFields[0];
                pitchField = floatFields[1];
            }

            fieldsInitialized = true;
        } catch (Exception e) {
            fieldsInitialized = true;
        }
    }

    // =====================================
    // STATIC METHODS FOR RENDER HOOK
    // =====================================

    public static float getRenderYaw() {
        return renderYaw;
    }

    public static float getRenderPitch() {
        return renderPitch;
    }

    public static boolean hasActiveRotation() {
        return hasRenderRotation && instance != null && instance.enabled;
    }

    public static boolean isActive() {
        return isActive && instance != null && instance.enabled;
    }

    public static LegitAura3 getInstance() {
        return instance;
    }

    public EntityPlayer getCurrentTarget() {
        return currentTarget;
    }

    // =====================================
    // AUTOCLICKER
    // =====================================

    private void initKeyBindFields() {
        if (gameSettingsField != null && keyBindAttackField != null) return;

        try {
            for (Field f : mc.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getType().getName().contains("GameSettings")) {
                    gameSettingsField = f;
                    break;
                }
            }

            if (gameSettingsField != null) {
                Object settings = gameSettingsField.get(mc);
                if (settings != null) {
                    for (Field f : settings.getClass().getDeclaredFields()) {
                        if (f.getType() == KeyBinding.class) {
                            f.setAccessible(true);
                            KeyBinding kb = (KeyBinding) f.get(settings);
                            if (kb != null && kb.getKeyDescription().contains("attack")) {
                                keyBindAttackField = f;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {}
    }

    private void startClickThread() {
        if (clickThread != null && clickThread.isAlive()) {
            clickThreadRunning = false;
            clickThread.interrupt();
            try { clickThread.join(100); } catch (Exception e) {}
        }

        clickThreadRunning = true;
        clickThread = new Thread(() -> {
            while (clickThreadRunning && enabled) {
                try {
                    if (!isActive || currentTarget == null || mc.currentScreen != null) {
                        Thread.sleep(50);
                        continue;
                    }

                    // Check hit range
                    if (mc.thePlayer != null && currentTarget != null) {
                        double dist = mc.thePlayer.getDistanceToEntity(currentTarget);
                        if (dist > hitRange.getValue()) {
                            Thread.sleep(50);
                            continue;
                        }
                    }

                    // Calculate delay
                    double min = minCPS.getValue();
                    double max = maxCPS.getValue();
                    if (min > max) { double temp = min; min = max; max = temp; }
                    double cps = min + random.nextDouble() * (max - min);
                    int delay = (int) (1000.0 / cps);
                    delay += random.nextInt(30) - 15;
                    if (delay < 50) delay = 50;

                    // Click using KeyBinding
                    if (gameSettingsField != null && keyBindAttackField != null) {
                        Object settings = gameSettingsField.get(mc);
                        KeyBinding attackKey = (KeyBinding) keyBindAttackField.get(settings);
                        if (attackKey != null) {
                            int keyCode = attackKey.getKeyCode();
                            KeyBinding.setKeyBindState(keyCode, true);
                            KeyBinding.onTick(keyCode);
                            Thread.sleep(15 + random.nextInt(10));
                            KeyBinding.setKeyBindState(keyCode, false);
                        }
                    }

                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    try { Thread.sleep(50); } catch (Exception ex) {}
                }
            }
        }, "LegitAura3-Clicker");
        clickThread.setDaemon(true);
        clickThread.start();
    }

    private void stopClickThread() {
        clickThreadRunning = false;
        if (clickThread != null) {
            clickThread.interrupt();
            try { clickThread.join(100); } catch (Exception e) {}
        }

        try {
            if (gameSettingsField != null && keyBindAttackField != null) {
                Object settings = gameSettingsField.get(mc);
                KeyBinding attackKey = (KeyBinding) keyBindAttackField.get(settings);
                if (attackKey != null) {
                    KeyBinding.setKeyBindState(attackKey.getKeyCode(), false);
                }
            }
        } catch (Exception e) {}
    }

    // =====================================
    // TARGET FINDING
    // =====================================

    private EntityPlayer findTarget(EntityPlayerSP player) {
        try {
            net.minecraft.client.multiplayer.WorldClient world = getWorld();
            if (world == null) return null;

            List<EntityPlayer> players = world.playerEntities;
            if (players == null || players.isEmpty()) return null;

            double maxDist = attackRange.getValue();
            double minDist = minDistance.getValue();
            double maxFov = maxAngle.getValue();

            // Sticky/Single mode
            String selectedMode = mode.getSelected();
            if ((selectedMode.equals("Single") || (stickyTarget.isEnabled() && selectedMode.equals("Nearest")))
                && stickyModeTarget != null) {
                if (!stickyModeTarget.isDead && stickyModeTarget.deathTime == 0) {
                    double dist = player.getDistanceToEntity(stickyModeTarget);
                    if (dist <= maxDist + 1.0 && dist >= minDist) {
                        return stickyModeTarget;
                    }
                }
                stickyModeTarget = null;
            }

            EntityPlayer bestTarget = null;
            double bestScore = Double.MAX_VALUE;

            for (EntityPlayer entity : players) {
                if (entity == null || entity == player) continue;
                if (entity.isDead || entity.deathTime > 0) continue;
                if (!aimInvis.isEnabled() && entity.isInvisible()) continue;
                if (isTeamMate(player, entity)) continue;
                if (io.github.exodar.module.modules.misc.Friends.isFriend(entity.getName())) continue;
                if (AntiBot.isBotForCombat(entity)) continue;

                double dist = player.getDistanceToEntity(entity);
                if (dist > maxDist || dist < minDist) continue;

                // FOV check (180 = no restriction)
                float[] rots = getRotationsToEntity(player, entity);
                float yawDiffCheck = Math.abs(normalizeAngle(rots[0] - player.rotationYaw));
                if (maxFov < 180 && yawDiffCheck > maxFov) continue;

                // Score by distance for Nearest, by angle for Switch
                double score;
                if (selectedMode.equals("Nearest")) {
                    score = dist;
                } else {
                    score = yawDiffCheck;
                }

                if (score < bestScore) {
                    bestScore = score;
                    bestTarget = entity;
                }
            }

            if (bestTarget != null) {
                stickyModeTarget = bestTarget;
            }

            return bestTarget;
        } catch (Exception e) {
            return null;
        }
    }

    // =====================================
    // UTILITIES
    // =====================================

    private float[] getRotationsToEntity(EntityPlayerSP player, EntityPlayer target) {
        double deltaX = target.posX - player.posX;
        double deltaZ = target.posZ - player.posZ;
        double deltaY = (target.posY + target.getEyeHeight() * 0.85) - (player.posY + player.getEyeHeight());

        double distXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(Math.atan2(deltaY, distXZ) * 180.0 / Math.PI);

        return new float[] { yaw, pitch };
    }

    private float normalizeAngle(float angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    private boolean isTeamMate(EntityPlayerSP player, Entity entity) {
        try {
            ModuleManager manager = io.github.exodar.Main.getModuleManager();
            Module teamsModule = manager != null ? manager.getModuleByName("Teams") : null;
            if (teamsModule != null && teamsModule.isEnabled()) {
                if (teamsModule instanceof io.github.exodar.module.modules.misc.Teams) {
                    return ((io.github.exodar.module.modules.misc.Teams) teamsModule).isTeamMate(entity);
                }
            }

            if (entity instanceof EntityLivingBase) {
                if (player.isOnSameTeam((EntityLivingBase) entity)) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    public String getDisplaySuffix() {
        if (currentTarget != null) {
            return " \u00a77" + currentTarget.getName();
        }
        return null;
    }
}
