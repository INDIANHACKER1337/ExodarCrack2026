/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.Main;
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
import net.minecraft.network.play.client.C03PacketPlayer;

import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

/**
 * LegitAura6 - Silent Aim with independent settings
 *
 * Uses AimAssist's calculation METHODS (static) but has its OWN settings.
 * Changes to AimAssist's algorithms automatically benefit LegitAura6.
 * Does NOT require AimAssist to be enabled.
 *
 * How it works:
 * 1. Find target using own settings (distance, FOV, priority)
 * 2. Calculate rotation using AimAssist.getRotationsToEntity()
 * 3. Apply rotation to serverYaw/serverPitch (NOT to camera)
 * 4. Camera stays where player looks, but packets send serverYaw
 * 5. Model rotates via OpenGL (F5 view)
 */
public class LegitAura6 extends Module {

    // =====================================
    // SETTINGS (Independent from AimAssist)
    // =====================================
    private ModeSetting mode;
    private SliderSetting speed;
    private SliderSetting verticalSpeed;
    private SliderSetting distance;
    private SliderSetting minDistance;
    private SliderSetting maxAngle;
    private TickSetting clickAim;
    private TickSetting aimVertically;
    private TickSetting aimThroughWalls;
    private TickSetting weaponOnly;
    private TickSetting aimInvis;

    // AutoClicker settings
    private TickSetting autoClick;
    private SliderSetting minCPS;
    private SliderSetting maxCPS;
    private SliderSetting hitRange;

    // Server rotations (what packets send)
    private static float serverYaw = 0;
    private static float serverPitch = 0;

    // State
    private static boolean isActive = false;
    private static boolean hasRenderRotation = false;
    private static LegitAura6 instance = null;

    // Current target
    private EntityPlayer currentTarget = null;

    // Random for humanization
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

    public LegitAura6() {
        super("LegitAura6", ModuleCategory.COMBAT);
        instance = this;

        this.registerSetting(new DescriptionSetting("Silent aim (own settings)"));

        // Aim settings
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Center", "Nearest"}));
        this.registerSetting(speed = new SliderSetting("Speed", 20.0, 1.0, 50.0, 0.5));
        this.registerSetting(aimVertically = new TickSetting("Aim Vertically", true));
        this.registerSetting(verticalSpeed = new SliderSetting("Vertical Speed", 15.0, 1.0, 50.0, 0.5));

        // Range settings
        this.registerSetting(distance = new SliderSetting("Distance", 4.5, 1.0, 8.0, 0.1));
        this.registerSetting(minDistance = new SliderSetting("Min Distance", 0.0, 0.0, 2.0, 0.05));
        this.registerSetting(maxAngle = new SliderSetting("Max Angle", 180.0, 15.0, 180.0, 5.0));

        // Behavior
        this.registerSetting(clickAim = new TickSetting("Click Aim", true));
        this.registerSetting(weaponOnly = new TickSetting("Weapon Only", false));
        this.registerSetting(aimInvis = new TickSetting("Aim Invis", false));
        this.registerSetting(aimThroughWalls = new TickSetting("Aim Through Walls", false));

        // AutoClicker
        this.registerSetting(autoClick = new TickSetting("Auto Click", true));
        this.registerSetting(hitRange = new SliderSetting("Hit Range", 3.0, 3.0, 6.0, 0.05));
        this.registerSetting(minCPS = new SliderSetting("Min CPS", 10.0, 1.0, 20.0, 0.5));
        this.registerSetting(maxCPS = new SliderSetting("Max CPS", 14.0, 1.0, 20.0, 0.5));
    }

    @Override
    public void onEnable() {
        isActive = false;
        hasRenderRotation = false;
        currentTarget = null;

        EntityPlayerSP player = getPlayer();
        if (player != null) {
            serverYaw = player.rotationYaw;
            serverPitch = player.rotationPitch;
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
        stopClickThread();
    }

    @Override
    public void onUpdate() {
        if (!isInGame() || mc.currentScreen != null) {
            isActive = false;
            hasRenderRotation = false;
            return;
        }

        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        // Check click aim
        if (clickAim.isEnabled() && !Mouse.isButtonDown(0)) {
            isActive = false;
            hasRenderRotation = false;
            serverYaw = player.rotationYaw;
            serverPitch = player.rotationPitch;
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
        currentTarget = target;

        if (target == null) {
            isActive = false;
            hasRenderRotation = false;
            serverYaw = player.rotationYaw;
            serverPitch = player.rotationPitch;
            return;
        }

        // Calculate rotation using AimAssist's method
        float[] rotations = AimAssist.getRotationsToEntity(player, target);
        float targetYaw = rotations[0];
        float targetPitch = rotations[1];

        float yawDiff = AimAssist.normalizeAngle(targetYaw - serverYaw);
        float pitchDiff = targetPitch - serverPitch;

        // Check FOV (180 = no restriction)
        double maxFov = maxAngle.getValue();
        if (maxFov < 180 && Math.abs(yawDiff) > maxFov) {
            isActive = false;
            hasRenderRotation = false;
            return;
        }

        // Apply rotation based on mode
        String selectedMode = mode.getSelected();
        double hSpeed = speed.getValue();
        double vSpeed = verticalSpeed.getValue();

        if (selectedMode.equals("Center")) {
            // Center mode - aim directly at center
            double centerDeadzone = 1.0;
            if (Math.abs(yawDiff) > centerDeadzone) {
                double deltaYaw = 0;
                if (yawDiff > 0) {
                    deltaYaw = Math.min(hSpeed, yawDiff);
                } else {
                    deltaYaw = Math.max(-hSpeed, yawDiff);
                }
                deltaYaw += (random.nextDouble() - 0.5) * 0.3;
                serverYaw += (float) deltaYaw;
            }
        } else {
            // Nearest mode - gradual approach
            double nearestDeadzone = 2.0;
            if (Math.abs(yawDiff) > nearestDeadzone) {
                double speedFactor = Math.min(1.0, Math.abs(yawDiff) / 20.0);
                double actualSpeed = hSpeed * speedFactor;
                double deltaYaw = yawDiff * (actualSpeed / 20.0);
                deltaYaw = Math.max(-actualSpeed * 0.5, Math.min(actualSpeed * 0.5, deltaYaw));
                serverYaw += (float) deltaYaw;
            }
        }

        // Vertical aim
        if (aimVertically.isEnabled()) {
            double pitchDeadzone = 3.0;
            if (Math.abs(pitchDiff) > pitchDeadzone) {
                double deltaPitch = 0;
                if (pitchDiff > 0) {
                    deltaPitch = Math.min(vSpeed * 0.5, pitchDiff * 0.3);
                } else {
                    deltaPitch = Math.max(-vSpeed * 0.5, pitchDiff * 0.3);
                }
                deltaPitch += (random.nextDouble() - 0.5) * 0.2;
                serverPitch += (float) deltaPitch;
                serverPitch = Math.max(-90, Math.min(90, serverPitch));
            }
        }

        isActive = true;
        hasRenderRotation = true;
    }

    /**
     * Find best target - uses own settings
     */
    private EntityPlayer findTarget(EntityPlayerSP player) {
        try {
            net.minecraft.client.multiplayer.WorldClient world = getWorld();
            if (world == null) return null;

            List<EntityPlayer> players = world.playerEntities;
            if (players == null || players.isEmpty()) return null;

            double maxDist = distance.getValue();
            double minDist = minDistance.getValue();
            double maxFov = maxAngle.getValue();

            EntityPlayer bestTarget = null;
            double bestScore = Double.MAX_VALUE;

            for (EntityPlayer entity : players) {
                if (entity == null || entity == player) continue;
                if (entity.isDead || entity.deathTime > 0) continue;
                if (!aimInvis.isEnabled() && entity.isInvisible()) continue;

                // Check teammate
                if (isTeamMate(player, entity)) continue;

                // Check friend
                if (io.github.exodar.module.modules.misc.Friends.isFriend(entity.getName())) continue;

                // Check bot
                if (AntiBot.isBotForCombat(entity)) continue;

                double dist = player.getDistanceToEntity(entity);
                if (dist > maxDist || dist < minDist) continue;

                // Check FOV
                float[] rots = AimAssist.getRotationsToEntity(player, entity);
                float yawDiff = AimAssist.normalizeAngle(rots[0] - player.rotationYaw);
                if (maxFov < 180 && Math.abs(yawDiff) > maxFov) continue;

                // Check line of sight
                if (!aimThroughWalls.isEnabled()) {
                    if (!canSeeEntity(player, entity)) continue;
                }

                // Score by distance (closest = best)
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

    private boolean canSeeEntity(EntityPlayerSP player, EntityPlayer target) {
        try {
            if (mc.objectMouseOver != null) {
                Object typeOfHit = mc.objectMouseOver.typeOfHit;
                if (typeOfHit != null && typeOfHit.toString().equals("BLOCK")) {
                    double blockDist = Math.sqrt(
                        Math.pow(mc.objectMouseOver.hitVec.xCoord - player.posX, 2) +
                        Math.pow(mc.objectMouseOver.hitVec.yCoord - (player.posY + player.getEyeHeight()), 2) +
                        Math.pow(mc.objectMouseOver.hitVec.zCoord - player.posZ, 2)
                    );
                    double targetDist = player.getDistanceToEntity(target);
                    if (targetDist > blockDist) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isTeamMate(EntityPlayerSP player, Entity entity) {
        try {
            ModuleManager manager = Main.getModuleManager();
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

    // =====================================
    // PACKET MODIFICATION
    // =====================================

    @Override
    public boolean onSendPacket(Object packet) {
        if (!isActive || !enabled) return true;

        if (packet instanceof C03PacketPlayer) {
            C03PacketPlayer c03 = (C03PacketPlayer) packet;
            String className = packet.getClass().getSimpleName();

            if (className.contains("C05") || className.contains("C06") ||
                className.contains("Look") || className.contains("PosLook")) {

                if (!fieldsInitialized) {
                    initPacketFields(c03);
                }

                if (yawField != null && pitchField != null) {
                    try {
                        yawField.setFloat(c03, serverYaw);
                        pitchField.setFloat(c03, serverPitch);
                    } catch (Exception e) {}
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
    // STATIC METHODS FOR RENDER/HOOKS
    // =====================================

    public static float getRenderYaw() {
        return serverYaw;
    }

    public static float getRenderPitch() {
        return serverPitch;
    }

    public static boolean hasActiveRotation() {
        return hasRenderRotation && instance != null && instance.enabled;
    }

    public static boolean isActive() {
        return isActive && instance != null && instance.enabled;
    }

    public static LegitAura6 getInstance() {
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
                    if (!isActive || mc.currentScreen != null) {
                        Thread.sleep(50);
                        continue;
                    }

                    // Check if we have a target in hit range
                    if (currentTarget == null || mc.thePlayer == null) {
                        Thread.sleep(50);
                        continue;
                    }

                    double dist = mc.thePlayer.getDistanceToEntity(currentTarget);
                    if (dist > hitRange.getValue()) {
                        Thread.sleep(50);
                        continue;
                    }

                    // Calculate delay
                    double min = minCPS.getValue();
                    double max = maxCPS.getValue();
                    if (min > max) { double temp = min; min = max; max = temp; }
                    double cps = min + random.nextDouble() * (max - min);
                    int delay = (int) (1000.0 / cps);
                    delay += random.nextInt(30) - 15;
                    if (delay < 50) delay = 50;

                    // Click
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
        }, "LegitAura6-Clicker");
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

    @Override
    public String getDisplaySuffix() {
        if (currentTarget != null) {
            return " §7" + currentTarget.getName();
        }
        return " §7" + mode.getSelected();
    }
}
