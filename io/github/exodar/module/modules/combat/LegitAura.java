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
import io.github.exodar.module.modules.misc.Friends;
import io.github.exodar.module.modules.misc.Teams;
import io.github.exodar.setting.*;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;

import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LegitAura - All-in-one silent aim module
 * Combines: AimAssist + AutoClicker + Reach functionality
 * Sends rotations via packets (server sees rotations, camera doesn't move)
 * Shows rotations in F5 view
 */
public class LegitAura extends Module {

    // === SETTINGS ===
    private ModeSetting mode;
    private SliderSetting speed;
    private SliderSetting verticalSpeed;
    private SliderSetting distance;
    private SliderSetting minDistance;
    private SliderSetting maxAngle;
    private SliderSetting hitRange;
    private SliderSetting minCPS;
    private SliderSetting maxCPS;
    private TickSetting clickAim;
    private TickSetting weaponOnly;
    private TickSetting aimInvis;
    private TickSetting aimThroughWalls;
    private TickSetting teamsCheck;

    // === STATE ===
    private EntityPlayer currentTarget = null;
    private float serverYaw = 0;
    private float serverPitch = 0;
    private boolean hasRotation = false;
    private Random random = new Random();

    // === AUTOCLICKER THREAD ===
    private Thread clickThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    // === RENDER STATE (for F5 view) ===
    private static float renderYaw = 0;
    private static float renderPitch = 0;
    private static boolean activeRotation = false;

    // === REFLECTION CACHE ===
    private static Field gameSettingsField = null;
    private static Field keyBindAttackField = null;
    private static Field currentScreenField = null;
    private static Field thePlayerField = null;

    public LegitAura() {
        super("LegitAura", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Silent aim + AutoClicker"));
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Normal", "Center", "Nearest"}));
        this.registerSetting(speed = new SliderSetting("Speed", 15.0, 1.0, 50.0, 1.0));
        this.registerSetting(verticalSpeed = new SliderSetting("Vertical Speed", 12.0, 1.0, 50.0, 1.0));
        this.registerSetting(distance = new SliderSetting("Distance", 4.5, 3.0, 6.0, 0.1));
        this.registerSetting(minDistance = new SliderSetting("Min Distance", 0.0, 0.0, 2.0, 0.1));
        this.registerSetting(maxAngle = new SliderSetting("Max Angle", 180.0, 15.0, 180.0, 5.0));
        this.registerSetting(hitRange = new SliderSetting("Hit Range", 3.0, 2.5, 4.5, 0.1));
        this.registerSetting(minCPS = new SliderSetting("Min CPS", 10.0, 1.0, 20.0, 1.0));
        this.registerSetting(maxCPS = new SliderSetting("Max CPS", 14.0, 1.0, 20.0, 1.0));
        this.registerSetting(clickAim = new TickSetting("Click Aim", true));
        this.registerSetting(weaponOnly = new TickSetting("Weapon Only", false));
        this.registerSetting(aimInvis = new TickSetting("Aim Invis", false));
        this.registerSetting(aimThroughWalls = new TickSetting("Aim Through Walls", false));
        this.registerSetting(teamsCheck = new TickSetting("Teams", true));

        initReflection();
    }

    private void initReflection() {
        try {
            for (Field f : mc.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                String typeName = f.getType().getName();
                if (typeName.contains("EntityPlayer") && !typeName.contains("$")) {
                    thePlayerField = f;
                } else if (typeName.contains("GuiScreen")) {
                    currentScreenField = f;
                } else if (typeName.contains("GameSettings")) {
                    gameSettingsField = f;
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        currentTarget = null;
        hasRotation = false;
        activeRotation = false;

        EntityPlayerSP player = getPlayer();
        if (player != null) {
            serverYaw = player.rotationYaw;
            serverPitch = player.rotationPitch;
        }

        startClickThread();
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        hasRotation = false;
        activeRotation = false;

        stopClickThread();
    }

    private void startClickThread() {
        if (clickThread != null && clickThread.isAlive()) {
            running.set(false);
            clickThread.interrupt();
            try { clickThread.join(100); } catch (Exception e) {}
        }

        running.set(true);

        clickThread = new Thread(() -> {
            while (running.get()) {
                try {
                    if (!enabled || mc == null) {
                        Thread.sleep(50);
                        continue;
                    }

                    EntityPlayer player = (EntityPlayer) thePlayerField.get(mc);
                    Object screen = currentScreenField.get(mc);

                    if (player == null || screen != null) {
                        Thread.sleep(50);
                        continue;
                    }

                    // Check if we have a target in hit range
                    if (currentTarget == null || !hasRotation) {
                        Thread.sleep(50);
                        continue;
                    }

                    // Check if holding mouse (only if clickAim enabled)
                    if (clickAim.isEnabled() && !Mouse.isButtonDown(0)) {
                        Thread.sleep(50);
                        continue;
                    }

                    // Check weapon only
                    if (weaponOnly.isEnabled() && !isHoldingWeapon(player)) {
                        Thread.sleep(50);
                        continue;
                    }

                    // Check distance for clicking
                    double dist = player.getDistanceToEntity(currentTarget);
                    if (dist > hitRange.getValue()) {
                        Thread.sleep(50);
                        continue;
                    }

                    // Calculate delay based on CPS
                    int min = (int) minCPS.getValue();
                    int max = (int) maxCPS.getValue();
                    if (max < min) { int t = max; max = min; min = t; }

                    int targetCps = min + random.nextInt(Math.max(1, max - min + 1));
                    int baseDelay = 1000 / targetCps;

                    // Add variation
                    int variation = random.nextInt(baseDelay / 2 + 1) - (baseDelay / 4);
                    int finalDelay = Math.max(33, baseDelay + variation);

                    // Click
                    performClick();

                    Thread.sleep(finalDelay);

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    // Silent
                }
            }
        }, "LegitAura-Clicker");

        clickThread.setDaemon(true);
        clickThread.start();
    }

    private void stopClickThread() {
        running.set(false);
        if (clickThread != null) {
            clickThread.interrupt();
            try { clickThread.join(100); } catch (Exception e) {}
        }
    }

    private void performClick() {
        try {
            Object settings = gameSettingsField.get(mc);
            KeyBinding attackKey = (KeyBinding) keyBindAttackField.get(settings);
            if (attackKey != null) {
                int keyCode = attackKey.getKeyCode();
                KeyBinding.setKeyBindState(keyCode, true);
                KeyBinding.onTick(keyCode);
                Thread.sleep(15 + random.nextInt(10));
                KeyBinding.setKeyBindState(keyCode, false);
            }
        } catch (Exception e) {
            // Silent
        }
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;
        if (!isInGame()) return;
        if (mc.currentScreen != null) return;

        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        // Check weapon only
        if (weaponOnly.isEnabled() && !isHoldingWeapon(player)) {
            resetState();
            return;
        }

        // Check Click Aim - only aim when holding left click
        if (clickAim.isEnabled() && !Mouse.isButtonDown(0)) {
            resetState();
            return;
        }

        // Find target
        EntityPlayer target = findTarget(player);
        if (target == null) {
            resetState();
            return;
        }

        currentTarget = target;

        // Calculate target rotations based on mode
        float[] targetRotations = calculateTargetRotations(player, target);
        float targetYaw = targetRotations[0];
        float targetPitch = targetRotations[1];

        // Check FOV from player's ACTUAL rotation (not server rotation)
        float yawDiffFromPlayer = normalizeAngle(targetYaw - player.rotationYaw);
        if (maxAngle.getValue() < 180 && Math.abs(yawDiffFromPlayer) > maxAngle.getValue()) {
            resetState();
            return;
        }

        // Calculate smooth rotation towards target
        float currentYawDiff = normalizeAngle(targetYaw - serverYaw);
        float currentPitchDiff = targetPitch - serverPitch;

        // Speed calculation:
        // Speed 1 = very slow (0.5 degrees per tick max)
        // Speed 25 = medium (12.5 degrees per tick max)
        // Speed 50 = instant
        float yawStep, pitchStep;

        if (speed.getValue() >= 50) {
            // Instant rotation
            yawStep = currentYawDiff;
        } else {
            // Max degrees per tick based on speed (0.5 to 25 degrees)
            float maxYawPerTick = (float) speed.getValue() * 0.5f;

            // Clamp the step to maxYawPerTick
            if (Math.abs(currentYawDiff) <= maxYawPerTick) {
                yawStep = currentYawDiff;
            } else {
                yawStep = Math.signum(currentYawDiff) * maxYawPerTick;
            }

            // Add small randomness for legit feel
            yawStep += (random.nextFloat() - 0.5f) * 0.3f;
        }

        if (verticalSpeed.getValue() >= 50) {
            pitchStep = currentPitchDiff;
        } else {
            float maxPitchPerTick = (float) verticalSpeed.getValue() * 0.4f;

            if (Math.abs(currentPitchDiff) <= maxPitchPerTick) {
                pitchStep = currentPitchDiff;
            } else {
                pitchStep = Math.signum(currentPitchDiff) * maxPitchPerTick;
            }

            pitchStep += (random.nextFloat() - 0.5f) * 0.2f;
        }

        // Update server rotations
        serverYaw += yawStep;
        serverYaw = normalizeAngle(serverYaw);
        serverPitch += pitchStep;
        serverPitch = Math.max(-90, Math.min(90, serverPitch));

        // Send rotation packet to server
        sendRotationPacket(player, serverYaw, serverPitch);

        // Update render state for F5 view
        renderYaw = serverYaw;
        renderPitch = serverPitch;
        hasRotation = true;
        activeRotation = true;
    }

    private void resetState() {
        currentTarget = null;
        hasRotation = false;
        activeRotation = false;
        // Reset server rotations to player's actual rotation
        EntityPlayerSP player = getPlayer();
        if (player != null) {
            serverYaw = player.rotationYaw;
            serverPitch = player.rotationPitch;
        }
    }

    private float[] calculateTargetRotations(EntityPlayerSP player, EntityPlayer target) {
        String selectedMode = mode.getSelected();

        double eyeX = player.posX;
        double eyeY = player.posY + player.getEyeHeight();
        double eyeZ = player.posZ;

        double targetX, targetY, targetZ;

        if (selectedMode.equals("Center")) {
            targetX = target.posX;
            targetY = target.posY + target.height / 2.0;
            targetZ = target.posZ;
        } else if (selectedMode.equals("Nearest")) {
            targetX = clamp(eyeX, target.posX - target.width / 2.0, target.posX + target.width / 2.0);
            targetY = clamp(eyeY, target.posY, target.posY + target.height);
            targetZ = clamp(eyeZ, target.posZ - target.width / 2.0, target.posZ + target.width / 2.0);
        } else {
            // Normal - eye level
            targetX = target.posX;
            targetY = target.posY + target.getEyeHeight();
            targetZ = target.posZ;
        }

        return calculateRotation(eyeX, eyeY, eyeZ, targetX, targetY, targetZ);
    }

    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
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

    private float normalizeAngle(float angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    private void sendRotationPacket(EntityPlayerSP player, float yaw, float pitch) {
        try {
            // Use mc.thePlayer.sendQueue directly
            if (mc.thePlayer != null && mc.thePlayer.sendQueue != null) {
                C03PacketPlayer.C05PacketPlayerLook packet = new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, player.onGround);
                mc.thePlayer.sendQueue.addToSendQueue(packet);
            }
        } catch (Exception e) {
            // Silent
        }
    }

    private EntityPlayer findTarget(EntityPlayerSP player) {
        try {
            net.minecraft.client.multiplayer.WorldClient world = getWorld();
            if (world == null) return null;

            List<EntityPlayer> players = world.playerEntities;
            if (players == null || players.isEmpty()) return null;

            double maxDist = distance.getValue();
            double minDist = minDistance.getValue();

            EntityPlayer bestTarget = null;
            double bestScore = Double.MAX_VALUE;

            for (EntityPlayer entity : players) {
                if (entity == null || entity == player) continue;
                if (entity.isDead || entity.deathTime > 0) continue;

                if (!aimInvis.isEnabled() && entity.isInvisible()) continue;
                if (Friends.isFriend(entity.getName())) continue;
                if (AntiBot.isBotForCombat(entity)) continue;
                if (teamsCheck.isEnabled() && isTeamMate(player, entity)) continue;

                double dist = player.getDistanceToEntity(entity);
                if (dist > maxDist || dist < minDist) continue;

                // Check FOV from player's actual rotation
                float[] rots = calculateRotation(player.posX, player.posY + player.getEyeHeight(), player.posZ,
                        entity.posX, entity.posY + entity.height / 2.0, entity.posZ);
                float yawDiff = normalizeAngle(rots[0] - player.rotationYaw);

                if (maxAngle.getValue() < 180 && Math.abs(yawDiff) > maxAngle.getValue()) continue;

                if (!aimThroughWalls.isEnabled()) {
                    if (!canSee(player, entity)) continue;
                }

                double score = dist;

                if (score < bestScore) {
                    bestScore = score;
                    bestTarget = entity;
                }
            }

            return bestTarget;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean canSee(EntityPlayerSP player, EntityPlayer target) {
        try {
            return player.canEntityBeSeen(target);
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isTeamMate(EntityPlayerSP player, Entity entity) {
        try {
            ModuleManager manager = io.github.exodar.Main.getModuleManager();
            Module teamsModule = manager != null ? manager.getModuleByName("Teams") : null;
            if (teamsModule != null && teamsModule.isEnabled()) {
                if (teamsModule instanceof Teams) {
                    return ((Teams) teamsModule).isTeamMate(entity);
                }
            }

            if (entity instanceof EntityLivingBase) {
                if (player.isOnSameTeam((EntityLivingBase) entity)) {
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }

    private boolean isHoldingWeapon(EntityPlayer player) {
        try {
            if (player.getHeldItem() == null) return false;
            String itemName = player.getHeldItem().getUnlocalizedName().toLowerCase();
            return itemName.contains("sword") || itemName.contains("axe");
        } catch (Exception e) {
            return false;
        }
    }

    // === STATIC METHODS FOR RENDER ===

    public static boolean hasActiveRotation() {
        return activeRotation;
    }

    public static float getRenderYaw() {
        return renderYaw;
    }

    public static float getRenderPitch() {
        return renderPitch;
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + mode.getSelected();
    }
}
