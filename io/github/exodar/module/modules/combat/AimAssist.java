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
import io.github.exodar.setting.*;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.input.Mouse;

import java.util.List;
import java.util.Random;

/**
 * AimAssist - Center, Nearest, Adaptive modes
 * Vertical aim with large torso deadzone and anti-detection
 */
public class AimAssist extends Module {

    // Settings
    private ModeSetting mode;
    private ModeSetting targetPriority;
    private ModeSetting targetMode;
    private SliderSetting speed;
    private TickSetting dynamicSpeed;
    private TickSetting aimVertically;
    private SliderSetting verticalSpeed;
    private SliderSetting distance;
    private SliderSetting minDistance;
    private SliderSetting maxAngle;
    private TickSetting clickAim;
    private TickSetting stickyTarget;
    private TickSetting checkBlockBreak;
    private TickSetting weaponOnly;
    private TickSetting aimInvis;

    // Aim Through Walls (available for all modes)
    private TickSetting aimThroughWalls;

    // Noise for legit feel
    private double yawNoise = 0;
    private double pitchNoise = 0;
    private long nextNoiseRefreshTime = -1;
    private long nextNoiseEmptyTime = 200;

    // Sticky target tracking
    private EntityPlayer currentTarget = null;
    private EntityPlayer primaryTarget = null;
    private double stickyRangeBonus = 2.0;
    private boolean wasClicking = false;

    // For Adaptive mode
    private float lastYaw = 0;
    private float lastPitch = 0;

    // ========== IMPROVED VERTICAL AIM SYSTEM ==========
    // Anti-detection: randomize everything
    private Random random = new Random();

    // Vertical aim state
    private long lastVerticalCorrectionTime = 0;
    private long verticalCorrectionCooldown = 0;  // Random cooldown between corrections
    private float lastVerticalDelta = 0;
    private int skipTicksRemaining = 0;  // Skip some ticks randomly

    // Track player movement for strafe detection
    private float lastPlayerYaw = 0;
    private float lastPlayerPitch = 0;
    private boolean wasStrafing = false;

    // Fighting aim detection
    private int fightingAimTicks = 0;

    public AimAssist() {
        super("AimAssist", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("AimAssist"));
        // Normal mode first (recommended)
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Normal", "Center", "Nearest", "Adaptive"}));

        // Speed settings (shared by all modes)
        this.registerSetting(speed = new SliderSetting("Speed", 15.0, 1.0, 50.0, 0.5));
        this.registerSetting(dynamicSpeed = new TickSetting("Dynamic Speed", true));
        this.registerSetting(aimVertically = new TickSetting("Aim Vertically", true));
        this.registerSetting(verticalSpeed = new SliderSetting("Vertical Speed", 10.0, 1.0, 50.0, 0.5));

        // Range settings
        this.registerSetting(distance = new SliderSetting("Distance", 4.5, 1.0, 8.0, 0.1));
        this.registerSetting(minDistance = new SliderSetting("Min Distance", 0.2, 0.0, 2.0, 0.05));
        this.registerSetting(maxAngle = new SliderSetting("Max Angle", 90.0, 15.0, 180.0, 5.0));

        // Target settings
        this.registerSetting(targetPriority = new ModeSetting("Priority", new String[]{"Distance", "Health", "Armor", "HurtTime"}));
        this.registerSetting(targetMode = new ModeSetting("Target Mode", new String[]{"Solo", "Multi"}));

        // Behavior settings
        this.registerSetting(clickAim = new TickSetting("Click Aim", true));
        this.registerSetting(stickyTarget = new TickSetting("Sticky Target", true));
        this.registerSetting(checkBlockBreak = new TickSetting("Break Blocks", true));
        this.registerSetting(weaponOnly = new TickSetting("Weapon Only", false));
        this.registerSetting(aimInvis = new TickSetting("Aim Invis", false));

        // Aim Through Walls (available for all modes)
        this.registerSetting(aimThroughWalls = new TickSetting("Aim Through Walls", false));
    }

    @Override
    public void onEnable() {
        currentTarget = null;
        primaryTarget = null;
        wasClicking = false;
        resetAimState();
        EntityPlayerSP player = getPlayer();
        if (player != null) {
            lastYaw = player.rotationYaw;
            lastPitch = player.rotationPitch;
            lastPlayerYaw = player.rotationYaw;
            lastPlayerPitch = player.rotationPitch;
        }
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        primaryTarget = null;
        wasClicking = false;
        resetAimState();
    }

    private void resetAimState() {
        yawNoise = 0;
        pitchNoise = 0;
        nextNoiseRefreshTime = -1;
        fightingAimTicks = 0;
        lastVerticalCorrectionTime = 0;
        verticalCorrectionCooldown = 0;
        lastVerticalDelta = 0;
        skipTicksRemaining = 0;
        wasStrafing = false;
    }

    @Override
    public void onUpdate() {
        if (!isInGame()) return;
        if (mc.currentScreen != null || !mc.inGameHasFocus) return;

        // Update settings visibility based on mode
        updateSettingsVisibility();

        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        String selectedMode = mode.getSelected();

        // Normal mode has its own logic
        if (selectedMode.equals("Normal")) {
            applyNormalMode(player);
            return;
        }

        updateNoise();

        if (noAction(player)) {
            lastYaw = player.rotationYaw;
            lastPitch = player.rotationPitch;
            lastPlayerYaw = player.rotationYaw;
            lastPlayerPitch = player.rotationPitch;
            return;
        }

        EntityPlayer target = getTarget(player);
        if (target == null) {
            currentTarget = null;
            resetAimState();
            lastYaw = player.rotationYaw;
            lastPitch = player.rotationPitch;
            lastPlayerYaw = player.rotationYaw;
            lastPlayerPitch = player.rotationPitch;
            return;
        }

        if (selectedMode.equals("Center")) {
            applyCenterAim(player, target);
        } else if (selectedMode.equals("Nearest")) {
            applyNearestAim(player, target);
        } else if (selectedMode.equals("Adaptive")) {
            applyAdaptiveAim(player, target);
        }

        lastYaw = player.rotationYaw;
        lastPitch = player.rotationPitch;
        lastPlayerYaw = player.rotationYaw;
        lastPlayerPitch = player.rotationPitch;
    }

    /**
     * Update settings visibility based on selected mode
     */
    private void updateSettingsVisibility() {
        boolean isNormalMode = mode.getSelected().equals("Normal");

        // These settings are only for non-Normal modes
        targetPriority.setVisible(!isNormalMode);
        targetMode.setVisible(!isNormalMode);
        // minDistance available for all modes
        // checkBlockBreak and aimThroughWalls available for all modes
    }

    private void updateNoise() {
        long time = System.currentTimeMillis();
        if (nextNoiseRefreshTime == -1 || time >= nextNoiseRefreshTime + nextNoiseEmptyTime) {
            nextNoiseRefreshTime = (long) (time + Math.random() * 60 + 80);
            nextNoiseEmptyTime = (long) (Math.random() * 100 + 180);
            yawNoise = (Math.random() - 0.5) * 2 * ((Math.random() - 0.5) * 0.3 + 0.8);
            pitchNoise = (Math.random() - 0.5) * 2 * ((Math.random() - 0.5) * 0.35 + 0.6);
        } else if (time >= nextNoiseRefreshTime) {
            yawNoise = 0;
            pitchNoise = 0;
        }
    }

    /**
     * CENTER MODE - Aims to center of target
     * Improved version with better stability and smoothing
     */
    private void applyCenterAim(EntityPlayerSP player, EntityPlayer target) {
        float[] rotations = getRotationsToEntity(player, target);
        float targetYaw = rotations[0];

        float yawDiff = normalizeAngle(targetYaw - player.rotationYaw);
        float absYawDiff = Math.abs(yawDiff);

        // Skip if outside max angle
        if (absYawDiff > maxAngle.getValue()) return;

        // Deadzone - don't aim if already very close to center
        float deadzone = 0.5f;
        if (absYawDiff <= deadzone) return;

        // Calculate base speed
        double baseSpeed = speed.getValue();

        // Dynamic speed: faster when far, slower when close
        double hSpeed;
        if (dynamicSpeed.isEnabled()) {
            // Scale speed based on distance to target
            // Far (>30°): full speed + bonus
            // Close (<5°): reduced speed for precision
            if (absYawDiff > 30) {
                hSpeed = baseSpeed * 1.5;
            } else if (absYawDiff > 10) {
                hSpeed = baseSpeed;
            } else if (absYawDiff > 5) {
                hSpeed = baseSpeed * 0.7;
            } else {
                hSpeed = baseSpeed * 0.4; // Slow down for precision
            }
        } else {
            hSpeed = baseSpeed;
        }

        // Reduce speed if already on target (looking at hitbox)
        if (isLookingAtTarget(target)) {
            hSpeed *= 0.5;
        }

        // Calculate delta - move towards target but don't overshoot
        double deltaYaw;
        if (absYawDiff <= hSpeed) {
            // Close enough - move exactly to target (prevents oscillation)
            deltaYaw = yawDiff * 0.8; // 80% to prevent overshoot
        } else {
            // Move by speed amount in correct direction
            deltaYaw = (yawDiff > 0 ? hSpeed : -hSpeed);
        }

        // Add small randomization only when moving significantly
        if (Math.abs(deltaYaw) > 0.5) {
            deltaYaw += (Math.random() - 0.5) * 0.2;
        }

        // Apply rotation
        player.rotationYaw += (float) deltaYaw;

        // Vertical aim
        if (aimVertically.isEnabled()) {
            applyTorsoDeadzoneVerticalAim(player, target);
        }
    }

    /**
     * NEAREST MODE
     */
    private void applyNearestAim(EntityPlayerSP player, EntityPlayer target) {
        float[] rotations = getRotationsToEntity(player, target);
        float targetYaw = rotations[0];

        float yawDiff = normalizeAngle(targetYaw - player.rotationYaw);

        if (Math.abs(yawDiff) > maxAngle.getValue()) return;

        double hSpeed = calculateDynamicSpeed(speed.getValue(), Math.abs(yawDiff));

        double nearestDeadzone = 2.0;

        if (Math.abs(yawDiff) > nearestDeadzone) {
            double speedFactor = Math.min(1.0, Math.abs(yawDiff) / 20.0);
            double actualSpeed = hSpeed * speedFactor;

            double deltaYaw = yawDiff * (actualSpeed / 20.0);
            deltaYaw = Math.max(-actualSpeed * 0.5, Math.min(actualSpeed * 0.5, deltaYaw));

            player.rotationYaw += (float) deltaYaw;
        }

        // Vertical aim - NEW SYSTEM
        if (aimVertically.isEnabled()) {
            applyTorsoDeadzoneVerticalAim(player, target);
        }
    }

    /**
     * ADAPTIVE MODE
     */
    private void applyAdaptiveAim(EntityPlayerSP player, EntityPlayer target) {
        float[] rotations = getRotationsToEntity(player, target);
        float targetYaw = rotations[0];

        float yawDiff = normalizeAngle(targetYaw - player.rotationYaw);

        if (Math.abs(yawDiff) > maxAngle.getValue()) return;

        double adaptiveDeadzone = 1.5;
        if (Math.abs(yawDiff) <= adaptiveDeadzone) return;

        float yawChange = normalizeAngle(player.rotationYaw - lastYaw);

        if (Math.abs(yawChange) < 0.5) {
            return;
        }

        boolean movingToward = (yawDiff > 0 && yawChange > 0) || (yawDiff < 0 && yawChange < 0);

        if (!movingToward) {
            return;
        }

        double hSpeed = calculateDynamicSpeed(speed.getValue(), Math.abs(yawDiff));

        double boost;
        if (yawDiff > 0) {
            boost = Math.min(hSpeed * 0.5, yawDiff * 0.3);
        } else {
            boost = Math.max(-hSpeed * 0.5, yawDiff * 0.3);
        }

        player.rotationYaw += (float) boost;

        // Vertical aim - NEW SYSTEM (reduced for adaptive)
        if (aimVertically.isEnabled()) {
            applyTorsoDeadzoneVerticalAim(player, target);
        }
    }

    /**
     * NEW VERTICAL AIM SYSTEM
     *
     * Zona válida: Ajustada según distancia al target
     * - CERCA: zona grande (ojos → mitad cuerpo)
     * - LEJOS: zona chica centrada en pecho/cuello (maximiza hit range)
     * Humanizado con variación natural
     */
    private void applyTorsoDeadzoneVerticalAim(EntityPlayerSP player, EntityPlayer target) {

        // ========== DETECT CAMERA MOVEMENT ==========
        float yawChange = Math.abs(normalizeAngle(player.rotationYaw - lastPlayerYaw));
        float pitchChange = Math.abs(player.rotationPitch - lastPlayerPitch);

        // Si la cámara se mueve mucho, no corregir
        if (yawChange > 4.0f || pitchChange > 3.0f) {
            wasStrafing = true;
            return;
        }

        // Después de strafe, esperar un poco
        if (wasStrafing) {
            wasStrafing = false;
            if (random.nextFloat() < 0.7f) return;
        }

        // ========== DETECT FIGHTING AIM ==========
        float playerPitchMovement = player.rotationPitch - lastPlayerPitch;
        float[] eyeRotations = getRotationsToEntity(player, target);
        float pitchToEyes = eyeRotations[1];
        float pitchDiffToEyes = pitchToEyes - player.rotationPitch;

        boolean fightingAim = (pitchDiffToEyes > 0 && playerPitchMovement < -0.4f) ||
                (pitchDiffToEyes < 0 && playerPitchMovement > 0.4f);

        if (fightingAim) {
            fightingAimTicks++;
            if (fightingAimTicks >= 2) return;
        } else {
            fightingAimTicks = Math.max(0, fightingAimTicks - 1);
        }

        // ========== CALCULATE DISTANCE FACTOR ==========

        float distToTarget = player.getDistanceToEntity(target);
        float maxDist = (float) distance.getValue();

        // Factor de 0.0 (muy cerca) a 1.0 (en el límite de distancia)
        float distanceFactor = Math.min(1.0f, distToTarget / maxDist);

        // ========== CALCULATE BODY ZONES BASED ON DISTANCE ==========

        // Partes del cuerpo
        float pitchToHead = getPitchToBodyPart(player, target, 0.95);   // Tope de cabeza
        float pitchToNeck = getPitchToBodyPart(player, target, 0.85);   // Cuello
        float pitchToChest = getPitchToBodyPart(player, target, 0.70);  // Pecho alto
        float pitchToCenter = getPitchToBodyPart(player, target, 0.55); // Centro del torso
        float pitchToMiddle = getPitchToBodyPart(player, target, 0.50); // Mitad exacta
        float pitchToLowBody = getPitchToBodyPart(player, target, 0.40);// Parte baja

        float currentPitch = player.rotationPitch;

        // ========== AJUSTAR ZONA SEGÚN DISTANCIA ==========
        // Cerca (distanceFactor ~0): zona grande, más libertad
        // Lejos (distanceFactor ~1): zona chica, centrada en pecho/cuello

        float topLimit;
        float bottomLimit;
        float targetPitchWhenCorrect;

        if (distanceFactor < 0.4f) {
            // MUY CERCA (0-40% del rango): zona grande
            topLimit = pitchToHead - 2.0f;
            bottomLimit = pitchToMiddle + 1.0f;  // Permitir hasta un poco más de la mitad
            targetPitchWhenCorrect = pitchToChest;

        } else if (distanceFactor < 0.7f) {
            // MEDIA DISTANCIA (40-70%): zona media
            topLimit = pitchToHead - 1.0f;
            bottomLimit = pitchToCenter;  // Limitar a centro del torso
            targetPitchWhenCorrect = pitchToChest;

        } else {
            // LEJOS (70-100% del rango): zona muy chica, centrada en pecho/cuello
            // Maximizar probabilidad de hit
            topLimit = pitchToNeck + 1.0f;       // No muy arriba de cuello
            bottomLimit = pitchToChest - 1.0f;   // No muy abajo del pecho
            targetPitchWhenCorrect = pitchToNeck; // Apuntar al cuello (centro de hitbox lejana)
        }

        // ========== DETERMINAR SI NECESITA CORRECCIÓN ==========

        boolean needsCorrection = false;
        float targetPitch = targetPitchWhenCorrect;

        // Si apunta MÁS ABAJO del límite inferior → SUBIR
        if (currentPitch > bottomLimit) {
            needsCorrection = true;
            targetPitch = targetPitchWhenCorrect;
        }
        // Si apunta MÁS ARRIBA del límite superior → BAJAR
        else if (currentPitch < topLimit) {
            needsCorrection = true;
            targetPitch = targetPitchWhenCorrect;
        }

        // Si está dentro de la zona válida, no hacer nada
        if (!needsCorrection) {
            return;
        }

        // ========== APLICAR CORRECCIÓN HUMANIZADA ==========

        float pitchDiff = targetPitch - currentPitch;
        float absDiff = Math.abs(pitchDiff);

        // Solo corregir si la diferencia es significativa
        float zoneSize = Math.abs(bottomLimit - topLimit);
        float minCorrectionThreshold = Math.max(1.0f, zoneSize * 0.1f);
        if (absDiff < minCorrectionThreshold) {
            return;
        }

        // Velocidad base del slider
        float baseSpeed = (float) verticalSpeed.getValue();

        // ========== HUMANIZACIÓN ==========

        // 1. Random skip (25% chance de no hacer nada este tick)
        if (random.nextFloat() < 0.25f) {
            return;
        }

        // 2. Velocidad variable (70% - 130% del base)
        float speedVar = 0.7f + random.nextFloat() * 0.6f;
        float effectiveSpeed = baseSpeed * speedVar;

        // 3. Más urgencia cuando está lejos (necesita corregir más rápido)
        if (distanceFactor > 0.7f) {
            effectiveSpeed *= 1.2f;
        }

        // 4. Escalar velocidad según qué tan afuera está
        float urgencyFactor = Math.min(1.5f, 0.6f + (absDiff / 10.0f));
        effectiveSpeed *= urgencyFactor;

        // 5. Calcular delta (usar 40-60% de la diferencia para suavidad)
        float correctionStrength = 0.4f + random.nextFloat() * 0.2f;
        float delta;
        if (pitchDiff > 0) {
            delta = Math.min(effectiveSpeed, pitchDiff * correctionStrength);
        } else {
            delta = Math.max(-effectiveSpeed, pitchDiff * correctionStrength);
        }

        // 6. Añadir ruido pequeño
        delta += (random.nextFloat() - 0.5f) * 0.2f;

        // 7. A veces hacer movimiento más pequeño (inconsistencia humana)
        if (random.nextFloat() < 0.15f) {
            delta *= 0.5f + random.nextFloat() * 0.3f;
        }

        // 8. Evitar deltas muy similares consecutivos
        if (Math.abs(Math.abs(delta) - Math.abs(lastVerticalDelta)) < 0.12f) {
            delta *= 0.65f + random.nextFloat() * 0.7f;
        }

        // 9. Limitar delta máximo
        float maxDelta = effectiveSpeed * 0.85f;
        delta = Math.max(-maxDelta, Math.min(maxDelta, delta));

        // ========== APLICAR ==========
        player.rotationPitch += delta;
        player.rotationPitch = Math.max(-90, Math.min(90, player.rotationPitch));

        lastVerticalDelta = delta;
    }

    /**
     * Get pitch angle to a specific body part
     * @param heightPercent 0.0 = feet, 1.0 = top of head, 0.5 = center
     */
    private float getPitchToBodyPart(EntityPlayerSP player, EntityPlayer target, double heightPercent) {
        double targetHeight = target.height;  // Usually 1.8 for players
        double targetY = target.posY + (targetHeight * heightPercent);

        double deltaX = target.posX - player.posX;
        double deltaZ = target.posZ - player.posZ;
        double deltaY = targetY - (player.posY + player.getEyeHeight());

        double distXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        return (float) -(Math.atan2(deltaY, distXZ) * 180.0 / Math.PI);
    }

    /**
     * Check if currently looking at target entity
     */
    private boolean isLookingAtTarget(EntityPlayer target) {
        try {
            if (mc.objectMouseOver != null) {
                Object typeOfHit = mc.objectMouseOver.typeOfHit;
                if (typeOfHit != null && typeOfHit.toString().equals("ENTITY") &&
                        mc.objectMouseOver.entityHit == target) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Normalize angle to -180 to 180 range
     */
    public static float normalizeAngle(float angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
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

    private EntityPlayer getTarget(EntityPlayerSP player) {
        double maxDist = distance.getValue();
        double stickyDist = maxDist + stickyRangeBonus;
        boolean isClicking = Mouse.isButtonDown(0);

        if (clickAim.isEnabled() && wasClicking && !isClicking) {
            currentTarget = null;
            primaryTarget = null;
        }
        wasClicking = isClicking;

        if (targetMode.getCurrentIndex() == 1 && primaryTarget != null) {
            if (primaryTarget.isDead || primaryTarget.deathTime > 0 ||
                    player.getDistanceToEntity(primaryTarget) > stickyDist) {
                primaryTarget = null;
                currentTarget = null;
            } else {
                if (primaryTarget.hurtTime > 0) {
                    EntityPlayer altTarget = findAlternativeTarget(player, primaryTarget);
                    if (altTarget != null) {
                        currentTarget = altTarget;
                        return altTarget;
                    }
                } else {
                    currentTarget = primaryTarget;
                    return primaryTarget;
                }
            }
        }

        if (stickyTarget.isEnabled() && currentTarget != null) {
            if (currentTarget.isDead || currentTarget.deathTime > 0) {
                currentTarget = null;
                primaryTarget = null;
            } else {
                double dist = player.getDistanceToEntity(currentTarget);
                if (dist <= stickyDist) {
                    return currentTarget;
                } else {
                    currentTarget = null;
                    primaryTarget = null;
                }
            }
        }

        EntityPlayer newTarget = findTarget(player);
        if (newTarget != null) {
            currentTarget = newTarget;
            if (targetMode.getCurrentIndex() == 1) {
                primaryTarget = newTarget;
            }
        }
        return newTarget;
    }

    private EntityPlayer findAlternativeTarget(EntityPlayerSP player, EntityPlayer exclude) {
        try {
            net.minecraft.client.multiplayer.WorldClient world = getWorld();
            if (world == null) return null;

            List<EntityPlayer> players = world.playerEntities;
            if (players == null || players.isEmpty()) return null;

            double maxDist = distance.getValue();
            double minDist = minDistance.getValue();

            EntityPlayer bestAlt = null;
            double bestScore = Double.MAX_VALUE;

            for (EntityPlayer entity : players) {
                if (entity == null || entity == player || entity == exclude) continue;
                if (entity.isDead || entity.deathTime > 0) continue;
                if (entity.hurtTime > 0) continue;
                if (!aimInvis.isEnabled() && entity.isInvisible()) continue;
                if (Friends.isFriend(entity.getName())) continue;
                if (isTeamMate(player, entity)) continue;
                if (AntiBot.isBotForCombat(entity)) continue;

                double dist = player.getDistanceToEntity(entity);
                if (dist > maxDist || dist < minDist) continue;

                double fov = Math.abs(getYawDiff(player, entity));
                if (fov > maxAngle.getValue() / 2.0) continue;

                if (dist < bestScore) {
                    bestScore = dist;
                    bestAlt = entity;
                }
            }
            return bestAlt;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean noAction(EntityPlayerSP player) {
        if (weaponOnly.isEnabled()) {
            net.minecraft.item.ItemStack held = player.getHeldItem();
            if (held == null) return true;
            String itemName = held.getUnlocalizedName().toLowerCase();
            boolean isWeapon = itemName.contains("sword") || itemName.contains("axe");
            // Check for KB stick
            if (!isWeapon && itemName.contains("stick")) {
                int kbLevel = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
                    net.minecraft.enchantment.Enchantment.knockback.effectId, held);
                isWeapon = kbLevel > 0;
            }
            if (!isWeapon) return true;
        }

        if (clickAim.isEnabled() && !Mouse.isButtonDown(0)) {
            return true;
        }

        if (checkBlockBreak.isEnabled()) {
            try {
                if (mc.objectMouseOver != null) {
                    Object typeOfHit = mc.objectMouseOver.typeOfHit;
                    if (typeOfHit != null && typeOfHit.toString().equals("BLOCK")) {
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }

        return false;
    }

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
            int priority = targetPriority.getCurrentIndex();

            for (EntityPlayer entity : players) {
                if (entity == null) continue;
                if (entity == player) continue;
                if (entity.isDead || entity.deathTime > 0) continue;

                if (!aimInvis.isEnabled() && entity.isInvisible()) continue;

                // Skip friends
                if (Friends.isFriend(entity.getName())) continue;

                if (isTeamMate(player, entity)) continue;
                if (AntiBot.isBotForCombat(entity)) continue;

                double dist = player.getDistanceToEntity(entity);
                if (dist > maxDist) continue;
                if (dist < minDist) continue;

                double fov = getYawDiff(player, entity);
                if (Math.abs(fov) > maxFov / 2.0) continue;

                // Check line of sight if aim through walls disabled
                if (!aimThroughWalls.isEnabled()) {
                    double targetX = entity.posX;
                    double targetY = entity.posY + entity.height / 2.0;
                    double targetZ = entity.posZ;
                    if (!canSeePosition(player, targetX, targetY, targetZ)) {
                        continue;
                    }
                }

                double score;
                switch (priority) {
                    case 1:
                        score = entity.getHealth();
                        break;
                    case 2:
                        score = getArmorValue(entity);
                        break;
                    case 3:
                        score = entity.hurtTime;
                        break;
                    default:
                        score = Math.abs(fov);
                        break;
                }

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

    private boolean isTeamMate(EntityPlayerSP player, Entity entity) {
        try {
            io.github.exodar.module.modules.misc.Teams teams = io.github.exodar.module.modules.misc.Teams.getInstance();
            // Use shouldAffectCombat() which checks both isEnabled() AND affectCombat setting
            if (teams != null && teams.shouldAffectCombat()) {
                return teams.isTeamMate(entity);
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static float[] getRotationsToEntity(EntityPlayerSP player, EntityPlayer target) {
        double deltaX = target.posX - player.posX;
        double deltaZ = target.posZ - player.posZ;
        double deltaY = (target.posY + target.getEyeHeight()) - (player.posY + player.getEyeHeight());

        double distXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(Math.atan2(deltaY, distXZ) * 180.0 / Math.PI);

        return new float[] { yaw, pitch };
    }

    private double getYawDiff(EntityPlayerSP player, EntityPlayer target) {
        float[] rotations = getRotationsToEntity(player, target);
        float targetYaw = rotations[0];
        float diff = targetYaw - player.rotationYaw;

        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;

        return diff;
    }

    private int getArmorValue(EntityPlayer player) {
        try {
            return player.getTotalArmorValue();
        } catch (Exception e) {
            return 0;
        }
    }

    // ========== NORMAL MODE (Based on C++ Simple Mode) ==========

    // Normal mode state
    private EntityPlayer normalModeTarget = null;

    /**
     * NORMAL MODE - Uses bounding box corner calculation for natural aim assist
     * Speed slider (1-50) is converted to multiplier: speed/15 = 1.0 at speed 15
     * Dynamic speed adds +45 boost when >45° away for fast 360s
     */
    private void applyNormalMode(EntityPlayerSP player) {
        // Only run when left clicking
        if (clickAim.isEnabled() && !Mouse.isButtonDown(0)) {
            normalModeTarget = null;
            return;
        }

        // Check weapon only
        if (weaponOnly.isEnabled()) {
            net.minecraft.item.ItemStack held = player.getHeldItem();
            if (held == null) return;
            String itemName = held.getUnlocalizedName().toLowerCase();
            boolean isWeapon = itemName.contains("sword") || itemName.contains("axe");
            // Check for KB stick
            if (!isWeapon && itemName.contains("stick")) {
                int kbLevel = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
                    net.minecraft.enchantment.Enchantment.knockback.effectId, held);
                isWeapon = kbLevel > 0;
            }
            if (!isWeapon) return;
        }

        // Get player eye position
        double eyeX = player.posX;
        double eyeY = player.posY + player.getEyeHeight();
        double eyeZ = player.posZ;

        // Get current rotation
        float playerYaw = normalizeAngle(player.rotationYaw);
        float playerPitch = player.rotationPitch;

        // Find target
        EntityPlayer selectedTarget = null;
        double selectedYawToAdd = 0;
        double selectedPitchToAdd = 0;

        net.minecraft.client.multiplayer.WorldClient world = getWorld();
        if (world == null) return;

        List<EntityPlayer> players = world.playerEntities;
        if (players == null || players.isEmpty()) return;

        double maxDist = distance.getValue();
        double maxFov = maxAngle.getValue();

        for (EntityPlayer target : players) {
            if (target == null || target == player) continue;
            if (target.ticksExisted < 10) continue;

            // Check sticky target
            boolean isLockedTarget = stickyTarget.isEnabled() && normalModeTarget != null &&
                    normalModeTarget.getEntityId() == target.getEntityId();

            if (stickyTarget.isEnabled() && normalModeTarget != null && !isLockedTarget) {
                continue;
            }

            if (isLockedTarget) {
                // Check if locked target died
                if (target.getHealth() <= 0 || target.isDead) {
                    normalModeTarget = null;
                    continue;
                }

                // Check friend
                if (Friends.isFriend(target.getName())) {
                    normalModeTarget = null;
                    continue;
                }

                // Check teammate
                if (isTeamMate(player, target)) {
                    normalModeTarget = null;
                    continue;
                }

                // Check bot
                if (AntiBot.isBotForCombat(target)) {
                    normalModeTarget = null;
                    continue;
                }

                // Check invisible
                if (!aimInvis.isEnabled() && target.isInvisible()) {
                    normalModeTarget = null;
                    continue;
                }

                // Check distance of locked target
                double dist = player.getDistanceToEntity(target);
                if (dist > 8.0) {
                    normalModeTarget = null;
                    continue;
                }

                double minDist = minDistance.getValue();
                if (dist > minDist) {
                    // Calculate rotations to center of target
                    double targetX = target.posX;
                    double targetY = target.posY + target.height / 2.0;
                    double targetZ = target.posZ;

                    float[] rots = calculateRotation(eyeX, eyeY, eyeZ, targetX, targetY, targetZ);
                    double yawToAdd = normalizeAngle(rots[0] - playerYaw);
                    double pitchToAdd = rots[1] - playerPitch;

                    selectedTarget = target;
                    selectedYawToAdd = yawToAdd;
                    selectedPitchToAdd = pitchToAdd;
                    break;
                }
            }

            // Skip dead players
            if (target.getHealth() <= 0 || target.isDead) continue;

            // Skip friends
            if (Friends.isFriend(target.getName())) continue;

            // Skip teammates
            if (isTeamMate(player, target)) continue;

            // Skip bots
            if (AntiBot.isBotForCombat(target)) continue;

            // Skip invisible
            if (!aimInvis.isEnabled() && target.isInvisible()) continue;

            // Get target center position
            double targetX = target.posX;
            double targetY = target.posY + target.height / 2.0;
            double targetZ = target.posZ;

            // Check line of sight if aim through walls disabled
            if (!aimThroughWalls.isEnabled()) {
                if (!canSeePosition(player, targetX, targetY, targetZ)) {
                    continue;
                }
            }

            // Calculate required rotation
            float[] rots = calculateRotation(eyeX, eyeY, eyeZ, targetX, targetY, targetZ);
            double yawToAdd = normalizeAngle(rots[0] - playerYaw);
            double pitchToAdd = rots[1] - playerPitch;

            double dist = player.getDistanceToEntity(target);
            double minDistValue = minDistance.getValue();

            // Check if within range and FOV
            if (dist <= maxDist && dist > minDistValue && Math.abs(yawToAdd) <= maxFov) {
                // Select target with smallest yaw difference (closest to crosshair)
                if (selectedTarget == null || Math.abs(yawToAdd) < Math.abs(selectedYawToAdd)) {
                    selectedTarget = target;
                    selectedYawToAdd = yawToAdd;
                    selectedPitchToAdd = pitchToAdd;
                }
            }
        }

        // Apply aim if target found
        if (selectedTarget != null) {
            normalModeTarget = selectedTarget;

            // Get target bounding box
            double bbMinX = selectedTarget.posX - selectedTarget.width / 2.0;
            double bbMaxX = selectedTarget.posX + selectedTarget.width / 2.0;
            double bbMinY = selectedTarget.posY;
            double bbMaxY = selectedTarget.posY + selectedTarget.height;
            double bbMinZ = selectedTarget.posZ - selectedTarget.width / 2.0;
            double bbMaxZ = selectedTarget.posZ + selectedTarget.width / 2.0;

            // Calculate 4 horizontal corners at feet level (for yaw bounds)
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

            // Calculate pitch bounds (top and bottom of hitbox)
            float[] topRot = calculateRotation(eyeX, eyeY, eyeZ, bbMaxX, bbMaxY, bbMaxZ);
            float[] botRot = calculateRotation(eyeX, eyeY, eyeZ, bbMinX, bbMinY, bbMinZ);
            double minPitch = topRot[1];
            double maxPitch = botRot[1];

            // ========== CONVERT SPEED SLIDERS TO MULTIPLIERS ==========
            // Speed 1-50 maps to multiplier 0.07-3.5 (speed 15 = 1.0)
            double yawMultiplier = speed.getValue() / 15.0;
            double pitchMultiplier = verticalSpeed.getValue() / 15.0;

            // ========== DYNAMIC SPEED ==========
            // If dynamic speed enabled, scale boost based on angle difference
            double dynamicBoost = 0;
            if (dynamicSpeed.isEnabled()) {
                double absYawDiff = Math.abs(selectedYawToAdd);
                if (absYawDiff > 90.0) {
                    // More than 90° away (360s) - massive boost
                    dynamicBoost = 8.0;
                } else if (absYawDiff > 45.0) {
                    // More than 45° away - high boost
                    dynamicBoost = 5.0;
                } else if (absYawDiff > 20.0) {
                    // More than 20° away - medium boost
                    dynamicBoost = 2.5;
                }
            }

            // ========== YAW ADJUSTMENT ==========
            double maxDelta = Math.abs(normalizeAngle((float)(maxYaw - minYaw))) / 2.0;
            double yawDifference = Math.abs(selectedYawToAdd) - maxDelta;

            if (Math.abs(selectedYawToAdd) > maxDelta) {
                double targetAdjustment;
                double effectiveYawMult = yawMultiplier + dynamicBoost;

                if (yawDifference < 2.0) {
                    // Small difference - direct correction
                    targetAdjustment = yawDifference * (selectedYawToAdd > 0 ? 1.0 : -1.0);
                } else if (yawDifference >= 16.0) {
                    // Very far - high speed (3.5-7.0)
                    targetAdjustment = (3.5 + random.nextDouble() * 3.5) * effectiveYawMult *
                            (selectedYawToAdd > 0 ? 1.0 : -1.0);
                } else if (yawDifference >= 8.0) {
                    // Far - medium-high speed (2.0-3.5)
                    targetAdjustment = (2.0 + random.nextDouble() * 1.5) * effectiveYawMult *
                            (selectedYawToAdd > 0 ? 1.0 : -1.0);
                } else if (yawDifference >= 4.0) {
                    // Medium - medium speed (0.8-1.8)
                    targetAdjustment = (0.8 + random.nextDouble() * 1.0) * effectiveYawMult *
                            (selectedYawToAdd > 0 ? 1.0 : -1.0);
                } else {
                    // Close - low speed (0.2-0.8)
                    targetAdjustment = (0.2 + random.nextDouble() * 0.6) * effectiveYawMult *
                            (selectedYawToAdd > 0 ? 1.0 : -1.0);
                }

                player.rotationYaw += (float) targetAdjustment;
            }

            // ========== PITCH ADJUSTMENT ==========
            if (aimVertically.isEnabled()) {
                double maxPitchDelta = Math.abs(maxPitch - minPitch) / 2.0;
                double pitchDifference = Math.abs(selectedPitchToAdd) - maxPitchDelta;

                if (Math.abs(selectedPitchToAdd) > maxPitchDelta) {
                    double targetAdjustment;
                    // Apply dynamic boost to pitch too but less aggressively
                    double effectivePitchMult = pitchMultiplier + (dynamicBoost * 0.5);

                    if (pitchDifference < 2.0) {
                        targetAdjustment = pitchDifference * (selectedPitchToAdd > 0 ? 1.0 : -1.0);
                    } else if (pitchDifference >= 16.0) {
                        targetAdjustment = (3.5 + random.nextDouble() * 3.5) * effectivePitchMult *
                                (selectedPitchToAdd > 0 ? 1.0 : -1.0);
                    } else if (pitchDifference >= 8.0) {
                        targetAdjustment = (2.0 + random.nextDouble() * 1.5) * effectivePitchMult *
                                (selectedPitchToAdd > 0 ? 1.0 : -1.0);
                    } else if (pitchDifference >= 4.0) {
                        targetAdjustment = (0.8 + random.nextDouble() * 1.0) * effectivePitchMult *
                                (selectedPitchToAdd > 0 ? 1.0 : -1.0);
                    } else {
                        targetAdjustment = (0.2 + random.nextDouble() * 0.6) * effectivePitchMult *
                                (selectedPitchToAdd > 0 ? 1.0 : -1.0);
                    }

                    player.rotationPitch += (float) targetAdjustment;
                    player.rotationPitch = Math.max(-90, Math.min(90, player.rotationPitch));
                }
            }
        }
    }

    /**
     * Calculate yaw and pitch to look at a position
     */
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

    /**
     * Check if player can see a position (no blocks in the way)
     * Uses canEntityBeSeen which handles raytracing internally
     */
    private boolean canSeePosition(EntityPlayerSP player, double x, double y, double z) {
        try {
            // Use player.canEntityBeSeen which does raytracing internally
            // For now, if Through Walls is disabled and we're looking at a block, return false
            if (mc.objectMouseOver != null) {
                Object typeOfHit = mc.objectMouseOver.typeOfHit;
                if (typeOfHit != null && typeOfHit.toString().equals("BLOCK")) {
                    // Check if the target position is behind the block
                    double blockDist = Math.sqrt(
                            Math.pow(mc.objectMouseOver.hitVec.xCoord - player.posX, 2) +
                                    Math.pow(mc.objectMouseOver.hitVec.yCoord - (player.posY + player.getEyeHeight()), 2) +
                                    Math.pow(mc.objectMouseOver.hitVec.zCoord - player.posZ, 2)
                    );
                    double targetDist = Math.sqrt(
                            Math.pow(x - player.posX, 2) +
                                    Math.pow(y - (player.posY + player.getEyeHeight()), 2) +
                                    Math.pow(z - player.posZ, 2)
                    );
                    // If target is behind the block we're looking at, can't see
                    if (targetDist > blockDist) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return true; // Assume can see on error
        }
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + mode.getSelected();
    }
}