/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.module.modules.misc.Friends;
import io.github.exodar.setting.*;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SilentAura - Silent aim + auto attack with packet-based clicks
 * Based on Ghost Client packet logic + LegitAura click system
 * Sends rotation packets to server without moving your view
 */
public class SilentAura extends Module {

    // Settings
    private SliderSetting range;
    private SliderSetting fov;
    private SliderSetting minCPS;
    private SliderSetting maxCPS;
    private ModeSetting targetPriority;
    private ModeSetting attackMode;
    private TickSetting clickAim;
    private TickSetting weaponOnly;
    private TickSetting aimInvis;
    private TickSetting throughWalls;
    private TickSetting keepSprint;

    // State
    private Random random = new Random();
    private EntityPlayer currentTarget = null;
    private float[] silentRotation = null;

    // Click thread
    private Thread clickThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Reflection for KeyBinding attack (for Simulate mode)
    private static Field gameSettingsField;
    private static Field keyBindAttackField;

    static {
        try {
            // Get GameSettings from Minecraft
            for (Field f : net.minecraft.client.Minecraft.class.getDeclaredFields()) {
                if (f.getType() == net.minecraft.client.settings.GameSettings.class) {
                    f.setAccessible(true);
                    gameSettingsField = f;
                    break;
                }
            }
            // Get keyBindAttack from GameSettings
            for (Field f : net.minecraft.client.settings.GameSettings.class.getDeclaredFields()) {
                if (f.getName().equals("keyBindAttack") ||
                    (f.getType() == KeyBinding.class && f.getName().contains("attack"))) {
                    f.setAccessible(true);
                    keyBindAttackField = f;
                    break;
                }
            }
            // Fallback - try by type
            if (keyBindAttackField == null) {
                Field[] fields = net.minecraft.client.settings.GameSettings.class.getDeclaredFields();
                for (Field f : fields) {
                    if (f.getType() == KeyBinding.class) {
                        f.setAccessible(true);
                        keyBindAttackField = f;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SilentAura() {
        super("SilentAura", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Silent rotation aura"));

        // Range & FOV
        this.registerSetting(range = new SliderSetting("Range", 4.0, 1.0, 6.0, 0.1));
        this.registerSetting(fov = new SliderSetting("FOV", 180.0, 30.0, 180.0, 5.0));

        // CPS settings
        this.registerSetting(minCPS = new SliderSetting("Min CPS", 10.0, 1.0, 20.0, 1.0));
        this.registerSetting(maxCPS = new SliderSetting("Max CPS", 14.0, 1.0, 20.0, 1.0));

        // Attack mode
        this.registerSetting(attackMode = new ModeSetting("Attack Mode", new String[]{"Packet", "Simulate"}));

        // Target settings
        this.registerSetting(targetPriority = new ModeSetting("Priority", new String[]{"Distance", "Health", "Angle", "HurtTime"}));
        this.registerSetting(clickAim = new TickSetting("Click Only", false));
        this.registerSetting(weaponOnly = new TickSetting("Weapon Only", false));
        this.registerSetting(aimInvis = new TickSetting("Aim Invis", false));
        this.registerSetting(throughWalls = new TickSetting("Through Walls", false));
        this.registerSetting(keepSprint = new TickSetting("Keep Sprint", true));
    }

    @Override
    public void onEnable() {
        currentTarget = null;
        silentRotation = null;
        startClickThread();
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        silentRotation = null;
        stopClickThread();
    }

    // ==================== CLICK THREAD ====================

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

                    EntityPlayerSP player = mc.thePlayer;
                    if (player == null || mc.theWorld == null) {
                        Thread.sleep(50);
                        continue;
                    }

                    // Screen check
                    if (mc.currentScreen != null) {
                        Thread.sleep(50);
                        continue;
                    }

                    // No target
                    if (currentTarget == null || silentRotation == null) {
                        Thread.sleep(50);
                        continue;
                    }

                    // Click only check
                    if (clickAim.isEnabled() && !Mouse.isButtonDown(0)) {
                        Thread.sleep(50);
                        continue;
                    }

                    // Weapon only check
                    if (weaponOnly.isEnabled() && !isHoldingWeapon(player)) {
                        Thread.sleep(50);
                        continue;
                    }

                    // Range check
                    double dist = player.getDistanceToEntity(currentTarget);
                    if (dist > range.getValue()) {
                        Thread.sleep(50);
                        continue;
                    }

                    // Target validation
                    if (currentTarget.isDead || currentTarget.getHealth() <= 0) {
                        Thread.sleep(50);
                        continue;
                    }

                    // Calculate delay based on CPS
                    int min = (int) minCPS.getValue();
                    int max = (int) maxCPS.getValue();
                    if (max < min) { int t = max; max = min; min = t; }

                    int targetCps = min + random.nextInt(Math.max(1, max - min + 1));
                    int baseDelay = 1000 / targetCps;

                    // Add variation for human-like clicking
                    int variation = random.nextInt(baseDelay / 2 + 1) - (baseDelay / 4);
                    int finalDelay = Math.max(33, baseDelay + variation);

                    // Perform attack
                    performAttack();

                    Thread.sleep(finalDelay);

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    // Silent
                }
            }
        }, "SilentAura-Clicker");

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

    /**
     * Perform attack based on selected mode
     */
    private void performAttack() {
        if (currentTarget == null || silentRotation == null) return;
        if (mc.thePlayer == null || mc.thePlayer.sendQueue == null) return;

        String mode = attackMode.getSelected();

        if (mode.equals("Packet")) {
            // Ghost Client style - pure packet attack
            attackWithPackets();
        } else {
            // Simulate - use KeyBinding like LegitAura
            attackWithSimulation();
        }
    }

    /**
     * Attack using pure packets (Ghost Client style)
     * Sends: Rotation -> Swing -> Attack
     */
    private void attackWithPackets() {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || currentTarget == null || silentRotation == null) return;

        // 1. Send rotation packet
        player.sendQueue.addToSendQueue(
            new C03PacketPlayer.C05PacketPlayerLook(silentRotation[0], silentRotation[1], player.onGround)
        );

        // 2. Send swing packet
        player.sendQueue.addToSendQueue(new C0APacketAnimation());

        // 3. Send attack packet
        player.sendQueue.addToSendQueue(
            new C02PacketUseEntity(currentTarget, C02PacketUseEntity.Action.ATTACK)
        );

        // 4. Keep sprint - prevent knockback slowdown on the attacker
        if (keepSprint.isEnabled() && player.isSprinting()) {
            // Server will try to reset sprint on hit, this counters it
            player.setSprinting(true);
        }
    }

    /**
     * Attack using KeyBinding simulation (LegitAura style)
     * Sends rotation first, then simulates left click
     */
    private void attackWithSimulation() {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || silentRotation == null) return;

        try {
            // 1. Send rotation packet first
            player.sendQueue.addToSendQueue(
                new C03PacketPlayer.C05PacketPlayerLook(silentRotation[0], silentRotation[1], player.onGround)
            );

            // 2. Simulate click using reflection
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
        } catch (Exception e) {
            // Fallback to packet attack
            attackWithPackets();
        }
    }

    // ==================== MAIN UPDATE ====================

    @Override
    public void onUpdate() {
        if (!isInGame()) return;
        if (mc.currentScreen != null) return;

        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        // Click only check
        if (clickAim.isEnabled() && !Mouse.isButtonDown(0)) {
            currentTarget = null;
            silentRotation = null;
            return;
        }

        // Weapon only check
        if (weaponOnly.isEnabled()) {
            if (player.getHeldItem() == null ||
                (!(player.getHeldItem().getItem() instanceof ItemSword) &&
                 !(player.getHeldItem().getItem() instanceof ItemAxe))) {
                currentTarget = null;
                silentRotation = null;
                return;
            }
        }

        // Find target
        currentTarget = findTarget(player);

        if (currentTarget == null) {
            silentRotation = null;
            return;
        }

        // Calculate silent rotation
        silentRotation = calculateRotation(currentTarget, player);
    }

    // ==================== TARGET FINDING ====================

    private EntityPlayer findTarget(EntityPlayerSP player) {
        double maxRange = range.getValue();
        double maxFov = fov.getValue();

        List<EntityPlayer> validTargets = new ArrayList<>();

        for (EntityPlayer target : mc.theWorld.playerEntities) {
            if (target == player) continue;
            if (target.isDead || target.getHealth() <= 0) continue;

            // Friends check
            if (Friends.isFriend(target.getName())) continue;

            // Bot check
            if (AntiBot.isBotForCombat(target)) continue;

            // Invisible check
            if (!aimInvis.isEnabled() && target.isInvisible()) continue;

            // Team check
            if (isTeamMate(player, target)) continue;

            // Range check
            double dist = player.getDistanceToEntity(target);
            if (dist > maxRange) continue;

            // FOV check
            float[] rotations = calculateRotation(target, player);
            float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(rotations[0] - player.rotationYaw));
            if (yawDiff > maxFov) continue;

            // Line of sight check (if not through walls)
            if (!throughWalls.isEnabled()) {
                if (!player.canEntityBeSeen(target)) continue;
            }

            validTargets.add(target);
        }

        if (validTargets.isEmpty()) return null;

        // Sort by priority
        String priority = targetPriority.getSelected();
        switch (priority) {
            case "Distance":
                validTargets.sort(Comparator.comparingDouble(player::getDistanceToEntity));
                break;
            case "Health":
                validTargets.sort(Comparator.comparingDouble(EntityPlayer::getHealth));
                break;
            case "Angle":
                validTargets.sort((a, b) -> {
                    float[] rotA = calculateRotation(a, player);
                    float[] rotB = calculateRotation(b, player);
                    float diffA = Math.abs(MathHelper.wrapAngleTo180_float(rotA[0] - player.rotationYaw));
                    float diffB = Math.abs(MathHelper.wrapAngleTo180_float(rotB[0] - player.rotationYaw));
                    return Float.compare(diffA, diffB);
                });
                break;
            case "HurtTime":
                validTargets.sort(Comparator.comparingInt(e -> e.hurtTime));
                break;
        }

        return validTargets.get(0);
    }

    // ==================== UTILITY ====================

    private float[] calculateRotation(EntityPlayer target, EntityPlayerSP player) {
        // Aim at chest (height 1.3 from feet)
        double targetX = target.posX;
        double targetY = target.posY + 1.3;
        double targetZ = target.posZ;

        Vec3 eyePos = player.getPositionEyes(1.0F);

        double deltaX = targetX - eyePos.xCoord;
        double deltaY = targetY - eyePos.yCoord;
        double deltaZ = targetZ - eyePos.zCoord;

        double dist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float) (Math.atan2(deltaZ, deltaX) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) -(Math.atan2(deltaY, dist) * (180.0 / Math.PI));

        return new float[]{yaw, MathHelper.clamp_float(pitch, -90, 90)};
    }

    private boolean isTeamMate(EntityPlayerSP player, Entity entity) {
        try {
            if (entity instanceof EntityLivingBase) {
                if (player.isOnSameTeam((EntityLivingBase) entity)) {
                    return true;
                }
            }

            String playerPrefix = player.getDisplayName().getUnformattedText()
                .substring(0, Math.min(2, player.getDisplayName().getUnformattedText().length()));
            String otherPrefix = entity.getDisplayName().getUnformattedText()
                .substring(0, Math.min(2, entity.getDisplayName().getUnformattedText().length()));

            return otherPrefix.equals(playerPrefix) && playerPrefix.contains("§");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isHoldingWeapon(EntityPlayerSP player) {
        if (player.getHeldItem() == null) return false;
        return player.getHeldItem().getItem() instanceof ItemSword ||
               player.getHeldItem().getItem() instanceof ItemAxe;
    }

    public EntityPlayer getCurrentTarget() {
        return currentTarget;
    }

    public float[] getSilentRotation() {
        return silentRotation;
    }

    @Override
    public String getDisplaySuffix() {
        if (currentTarget != null) {
            return " §7" + currentTarget.getName();
        }
        return null;
    }
}
