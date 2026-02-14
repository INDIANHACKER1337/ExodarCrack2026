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
import io.github.exodar.module.modules.misc.Teams;
import io.github.exodar.setting.*;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;

import java.util.Random;

/**
 * Velocity - Reduces knockback with multiple legit techniques
 *
 * Modes:
 * - Legit: hurtTime-based jump + strafe + damage boost (most legit)
 * - Modifier: Standard H/V percentage modification
 * - Jump: Jumps on velocity packet (Myau style)
 * - Delay: Delays velocity packet then applies H/V
 */
public class Velocity extends Module {

    private final Random random = new Random();

    // Main settings
    private ModeSetting mode;
    private SliderSetting horizontal;
    private SliderSetting vertical;
    private SliderSetting chance;

    // Legit mode settings
    private SliderSetting jumpTick;
    private SliderSetting minDelay;
    private SliderSetting maxDelay;
    private TickSetting strafe;
    private TickSetting damageBoost;
    private SliderSetting boostStrength;
    private TickSetting groundForce;
    private SliderSetting groundForceTick;
    private TickSetting onlyMoving;
    private TickSetting skipHighJumpBoost;

    // Delay mode settings
    private SliderSetting delayTicks;

    // Checks
    private TickSetting onlyWithEnemy;
    private SliderSetting enemyRange;
    private TickSetting requireClick;
    private TickSetting requireSprint;
    private TickSetting firstHitOnly;

    // State
    private int chanceCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean jumpFlag = false;
    private boolean delayActive = false;

    // Delay state
    private S12PacketEntityVelocity delayedPacket = null;
    private int delayTicksRemaining = 0;

    // First hit tracking
    private long lastHitTime = 0;
    private static final long COMBO_RESET_MS = 1000;

    // Explosion protection - track recent explosions to ignore them completely
    private long lastExplosionTime = 0;
    private static final long EXPLOSION_IGNORE_MS = 500; // Ignore velocity for 500ms after explosion

    // Legit mode state
    private int ticksSinceVelocity = 0;
    private boolean velocityReceived = false;
    private long scheduledJumpTime = 0;
    private int jumpCooldown = 0;
    private double lastVelX = 0;
    private double lastVelZ = 0;

    public Velocity() {
        super("Velocity", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Reduces knockback"));
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Legit", "Modifier", "Jump", "Delay"})
            .onChange(this::updateSettingVisibility));

        // Main settings
        this.registerSetting(horizontal = new SliderSetting("Horizontal %", 85.0, 0.0, 100.0, 1.0));
        this.registerSetting(vertical = new SliderSetting("Vertical %", 100.0, 0.0, 100.0, 1.0));
        this.registerSetting(chance = new SliderSetting("Chance %", 100.0, 0.0, 100.0, 1.0));

        // Legit mode settings
        this.registerSetting(new DescriptionSetting("--- Legit Options ---"));
        this.registerSetting(jumpTick = new SliderSetting("Jump Tick", 8.0, 6.0, 10.0, 1.0));
        this.registerSetting(minDelay = new SliderSetting("Min Delay", 0.0, 0.0, 150.0, 5.0));
        this.registerSetting(maxDelay = new SliderSetting("Max Delay", 50.0, 0.0, 150.0, 5.0));
        this.registerSetting(strafe = new TickSetting("Strafe", true));
        this.registerSetting(damageBoost = new TickSetting("Damage Boost", true));
        this.registerSetting(boostStrength = new SliderSetting("Boost Strength", 0.15, 0.05, 0.3, 0.01));
        this.registerSetting(groundForce = new TickSetting("Ground Force", false));
        this.registerSetting(groundForceTick = new SliderSetting("Ground Tick", 2.0, 1.0, 5.0, 1.0));
        this.registerSetting(onlyMoving = new TickSetting("Only Moving", true));
        this.registerSetting(skipHighJumpBoost = new TickSetting("Skip Jump Boost 2+", true));

        // Delay mode setting
        this.registerSetting(delayTicks = new SliderSetting("Delay Ticks", 3.0, 1.0, 20.0, 1.0));

        // Checks
        this.registerSetting(new DescriptionSetting("--- Conditions ---"));
        this.registerSetting(onlyWithEnemy = new TickSetting("Only With Enemy", false)
            .onChange(this::updateSettingVisibility));
        this.registerSetting(enemyRange = new SliderSetting("Enemy Range", 6.0, 3.0, 15.0, 0.5));
        this.registerSetting(requireClick = new TickSetting("Require Click", false));
        this.registerSetting(requireSprint = new TickSetting("Require Sprint", false));
        this.registerSetting(firstHitOnly = new TickSetting("First Hit Only", false));

        updateSettingVisibility();
    }

    private void updateSettingVisibility() {
        String selected = mode.getSelected();
        boolean isLegit = selected.equals("Legit");
        boolean isModifier = selected.equals("Modifier");
        boolean isJump = selected.equals("Jump");
        boolean isDelay = selected.equals("Delay");

        // H/V visible only for Modifier and Delay (not Legit or Jump)
        horizontal.setVisible(isModifier || isDelay);
        vertical.setVisible(isModifier || isDelay);

        // Legit options only for Legit mode
        jumpTick.setVisible(isLegit);
        minDelay.setVisible(isLegit);
        maxDelay.setVisible(isLegit);
        strafe.setVisible(isLegit);
        damageBoost.setVisible(isLegit);
        boostStrength.setVisible(isLegit && damageBoost.isEnabled());
        groundForce.setVisible(isLegit);
        groundForceTick.setVisible(isLegit && groundForce.isEnabled());
        onlyMoving.setVisible(isLegit);
        skipHighJumpBoost.setVisible(isLegit || isJump);

        // Delay Ticks only for Delay mode
        delayTicks.setVisible(isDelay);

        // Enemy Range only when Only With Enemy is enabled
        enemyRange.setVisible(onlyWithEnemy.isEnabled());
    }

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        resetState();
    }

    private void resetState() {
        pendingExplosion = false;
        allowNext = true;
        jumpFlag = false;
        delayActive = false;
        delayedPacket = null;
        delayTicksRemaining = 0;
        chanceCounter = 0;
        lastHitTime = 0;
        lastExplosionTime = 0;
        ticksSinceVelocity = 100;
        velocityReceived = false;
        scheduledJumpTime = 0;
        jumpCooldown = 0;
        lastVelX = 0;
        lastVelZ = 0;
    }

    /**
     * Check if conditionals allow velocity reduction
     */
    private boolean checkConditionals(EntityPlayerSP player) {
        if (requireClick.isEnabled() && !Mouse.isButtonDown(0)) {
            return false;
        }

        if (requireSprint.isEnabled() && !player.isSprinting()) {
            return false;
        }

        if (firstHitOnly.isEnabled()) {
            long now = System.currentTimeMillis();
            boolean isFirstHit = (now - lastHitTime) > COMBO_RESET_MS;
            lastHitTime = now;
            if (!isFirstHit) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if player is moving (WASD input)
     */
    private boolean isMoving() {
        EntityPlayerSP player = getPlayer();
        if (player == null) return false;
        return player.moveForward != 0 || player.moveStrafing != 0;
    }

    /**
     * Get movement yaw based on input
     */
    private float getMoveYaw() {
        EntityPlayerSP player = getPlayer();
        if (player == null) return 0;

        float yaw = player.rotationYaw;
        float forward = player.moveForward;
        float strafe = player.moveStrafing;

        if (forward != 0) {
            if (strafe > 0) {
                yaw -= forward > 0 ? 45 : -45;
            } else if (strafe < 0) {
                yaw += forward > 0 ? 45 : -45;
            }
            if (forward < 0) yaw += 180;
        } else if (strafe > 0) {
            yaw -= 90;
        } else if (strafe < 0) {
            yaw += 90;
        }

        return yaw;
    }

    /**
     * Apply strafe - maintain speed in movement direction
     */
    private void applyStrafe() {
        EntityPlayerSP player = getPlayer();
        if (player == null || !isMoving()) return;

        double speed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
        float yaw = getMoveYaw();
        float rad = (float) Math.toRadians(yaw);

        player.motionX = -Math.sin(rad) * speed;
        player.motionZ = Math.cos(rad) * speed;
    }

    /**
     * Apply damage boost - add forward momentum
     */
    private void applyDamageBoost() {
        EntityPlayerSP player = getPlayer();
        if (player == null || !isMoving()) return;

        float yaw = getMoveYaw();
        float rad = (float) Math.toRadians(yaw);
        double boost = boostStrength.getValue();

        player.motionX -= Math.sin(rad) * boost;
        player.motionZ += Math.cos(rad) * boost;
    }

    /**
     * Check if should skip due to Jump Boost
     */
    private boolean shouldSkipJumpBoost() {
        EntityPlayerSP player = getPlayer();
        if (player == null) return false;

        if (skipHighJumpBoost.isEnabled() && player.isPotionActive(Potion.jump)) {
            int amplifier = player.getActivePotionEffect(Potion.jump).getAmplifier();
            return amplifier >= 1; // Skip for Jump Boost II or higher
        }
        return false;
    }

    @Override
    public void onUpdate() {
        updateSettingVisibility();

        if (!isInGame()) return;

        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        String selected = mode.getSelected();

        // Track ticks since velocity
        if (velocityReceived) {
            ticksSinceVelocity++;
        }

        // Decrease jump cooldown
        if (jumpCooldown > 0) {
            jumpCooldown--;
        }

        // ============================================
        // LEGIT MODE - hurtTime based jump with extras
        // ============================================
        if (selected.equals("Legit")) {
            int hurtTime = player.hurtTime;

            // Ground force technique - force onGround at specific tick
            if (groundForce.isEnabled() && velocityReceived) {
                if (ticksSinceVelocity == (int) groundForceTick.getValue()) {
                    player.onGround = true;
                }
            }

            // Jump when hurtTime reaches threshold
            if (hurtTime >= (int) jumpTick.getValue() && velocityReceived) {
                // Check if we should jump
                if (player.onGround && jumpCooldown <= 0) {
                    // Only moving check
                    if (onlyMoving.isEnabled() && !isMoving()) {
                        return;
                    }

                    // Skip if high jump boost
                    if (shouldSkipJumpBoost()) {
                        return;
                    }

                    // Skip if in invalid state (liquid, ladder, web, flying, recent explosion)
                    if (isInInvalidState()) {
                        return;
                    }

                    // Check conditionals
                    if (!checkConditionals(player)) {
                        return;
                    }

                    // Apply chance
                    if (chance.getValue() < 100) {
                        if (random.nextDouble() * 100 > chance.getValue()) {
                            return;
                        }
                    }

                    // Calculate delay
                    long delay = 0;
                    if (maxDelay.getValue() > 0) {
                        delay = (long) (minDelay.getValue() + random.nextDouble() * (maxDelay.getValue() - minDelay.getValue()));
                    }

                    if (delay > 0) {
                        scheduledJumpTime = System.currentTimeMillis() + delay;
                    } else {
                        // Jump immediately
                        player.jump();
                        jumpCooldown = 2;
                        velocityReceived = false;

                        // Apply strafe after jump
                        if (strafe.isEnabled()) {
                            applyStrafe();
                        }

                        // Apply damage boost
                        if (damageBoost.isEnabled()) {
                            applyDamageBoost();
                        }
                    }
                }
            }

            // Handle scheduled jump (delayed)
            if (scheduledJumpTime > 0 && System.currentTimeMillis() >= scheduledJumpTime) {
                if (player.onGround && jumpCooldown <= 0) {
                    player.jump();
                    jumpCooldown = 2;

                    if (strafe.isEnabled()) {
                        applyStrafe();
                    }
                    if (damageBoost.isEnabled()) {
                        applyDamageBoost();
                    }
                }
                scheduledJumpTime = 0;
                velocityReceived = false;
            }

            // Reset after hurtTime ends
            if (hurtTime <= 1) {
                velocityReceived = false;
                scheduledJumpTime = 0;
            }
        }

        // ============================================
        // DELAY MODE - process delayed packet
        // ============================================
        if (selected.equals("Delay") && delayActive && delayedPacket != null) {
            delayTicksRemaining--;

            if (delayTicksRemaining <= 0) {
                applyVelocity(delayedPacket);
                delayActive = false;
                delayedPacket = null;
            }
        }

        // ============================================
        // JUMP MODE - flag based (Myau style)
        // ============================================
        if (jumpFlag) {
            jumpFlag = false;

            if (shouldSkipJumpBoost()) {
                return;
            }

            // Skip if in invalid state (liquid, ladder, web, flying, recent explosion)
            if (isInInvalidState()) {
                return;
            }

            if (player.onGround && player.isSprinting()) {
                player.movementInput.jump = true;
            }
        }
    }

    /**
     * Check if player is in an invalid state for velocity reduction
     * (liquids, ladder, flying, etc.)
     */
    private boolean isInInvalidState() {
        EntityPlayerSP player = getPlayer();
        if (player == null) return true;

        // In liquids
        if (player.isInWater() || player.isInLava()) return true;

        // On ladder or vine
        if (player.isOnLadder()) return true;

        // Flying or creative/spectator mode
        if (player.capabilities.isFlying) return true;
        if (player.capabilities.isCreativeMode) return true;

        // Recently affected by explosion - ignore for Legit/Jump modes
        if (System.currentTimeMillis() - lastExplosionTime < EXPLOSION_IGNORE_MS) return true;

        return false;
    }

    /**
     * Check if player is in liquid (for Jump mode)
     */
    private boolean isInLiquidOrWeb() {
        EntityPlayerSP player = getPlayer();
        if (player == null) return false;
        return player.isInWater() || player.isInLava();
    }

    @Override
    public boolean onReceivePacket(Object packet) {
        if (!enabled) return true;

        EntityPlayerSP player = getPlayer();
        if (player == null) return true;

        // Handle S19PacketEntityStatus (damage indicator for fakeCheck)
        if (packet instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus statusPacket = (S19PacketEntityStatus) packet;
            try {
                if (statusPacket.getEntity(mc.theWorld) == player && statusPacket.getOpCode() == 2) {
                    allowNext = false;
                }
            } catch (Exception e) {}
            return true;
        }

        // Handle Explosion packet (fireball, creeper, TNT, etc.)
        if (packet instanceof S27PacketExplosion) {
            S27PacketExplosion explosion = (S27PacketExplosion) packet;

            float motionX = explosion.func_149149_c();
            float motionY = explosion.func_149144_d();
            float motionZ = explosion.func_149147_e();

            if (motionX != 0 || motionY != 0 || motionZ != 0) {
                pendingExplosion = true;
                lastExplosionTime = System.currentTimeMillis(); // Track explosion time

                String selected = mode.getSelected();

                // For Legit and Jump modes, let explosion through completely unmodified
                // These modes should not be affected by explosions at all
                if (selected.equals("Legit") || selected.equals("Jump")) {
                    return true; // Let explosion KB happen normally
                }

                // For Modifier/Delay modes, apply H/V reduction to explosions
                if (onlyWithEnemy.isEnabled() && !hasEnemyNearby(player)) {
                    return true;
                }

                if (horizontal.getValue() == 0 && vertical.getValue() == 0) {
                    return false;
                }
            }
            return true;
        }

        // Handle Velocity packet
        if (packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity velPacket = (S12PacketEntityVelocity) packet;

            if (velPacket.getEntityID() != player.getEntityId()) {
                return true;
            }

            String selected = mode.getSelected();

            // Store velocity for strafe calculations
            lastVelX = velPacket.getMotionX() / 8000.0;
            lastVelZ = velPacket.getMotionZ() / 8000.0;

            // ============================================
            // LEGIT MODE - Pure legit, only sets flag for hurtTime jump
            // Does NOT modify velocity at all - completely legit technique
            // ============================================
            if (selected.equals("Legit")) {
                // Skip explosion velocity - don't trigger jump for explosions
                if (pendingExplosion) {
                    pendingExplosion = false;
                    velocityReceived = false;
                    return true;
                }

                // Skip if in invalid state (liquid, ladder, etc.)
                if (isInInvalidState()) {
                    return true;
                }

                if (onlyWithEnemy.isEnabled() && !hasEnemyNearby(player)) {
                    return true;
                }

                // Set flag for hurtTime-based jump (no velocity modification)
                double velY = velPacket.getMotionY() / 8000.0;
                if (velY > 0) {
                    velocityReceived = true;
                    ticksSinceVelocity = 0;
                }

                // Let velocity through completely unmodified - this is pure legit
                return true;
            }

            // ============================================
            // JUMP MODE - Completely independent (Myau style)
            // ============================================
            if (selected.equals("Jump")) {
                // Skip explosion velocity - don't jump for explosions
                if (pendingExplosion) {
                    pendingExplosion = false;
                    return true; // Don't trigger jump for explosions
                }

                // Skip if in invalid state (liquid, ladder, web, flying, recent explosion)
                if (isInInvalidState()) {
                    return true; // Let velocity through, don't trigger jump
                }

                if (onlyWithEnemy.isEnabled() && !hasEnemyNearby(player)) {
                    return true;
                }

                chanceCounter = chanceCounter % 100 + (int) chance.getValue();
                if (chanceCounter < 100) {
                    return true;
                }

                if (!checkConditionals(player)) {
                    return true;
                }

                double velY = velPacket.getMotionY() / 8000.0;
                if (velY > 0) {
                    jumpFlag = true;
                }
                return true;
            }

            // ============================================
            // MODIFIER / DELAY MODES
            // ============================================

            if (onlyWithEnemy.isEnabled() && !hasEnemyNearby(player)) {
                return true;
            }

            // FakeCheck
            if (allowNext && !pendingExplosion) {
                allowNext = true;
                return true;
            }
            allowNext = true;

            if (pendingExplosion) {
                pendingExplosion = false;
                return handleExplosionVelocity(velPacket, player);
            }

            chanceCounter = chanceCounter % 100 + (int) chance.getValue();
            if (chanceCounter < 100) {
                return true;
            }

            if (!checkConditionals(player)) {
                return true;
            }

            // Delay mode
            if (selected.equals("Delay")) {
                if (!delayActive) {
                    delayActive = true;
                    delayedPacket = velPacket;
                    delayTicksRemaining = (int) delayTicks.getValue();
                    return false;
                }
                return false;
            }

            // Modifier mode
            applyVelocity(velPacket);
            return false;
        }

        return true;
    }

    private boolean handleExplosionVelocity(S12PacketEntityVelocity packet, EntityPlayerSP player) {
        double hMult = horizontal.getValue() / 100.0;
        double vMult = vertical.getValue() / 100.0;

        double velX = packet.getMotionX() / 8000.0;
        double velY = packet.getMotionY() / 8000.0;
        double velZ = packet.getMotionZ() / 8000.0;

        player.motionX = velX * hMult;
        player.motionZ = velZ * hMult;
        player.motionY = velY * vMult;

        return false;
    }

    private void applyVelocity(S12PacketEntityVelocity packet) {
        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        double hMult = horizontal.getValue() / 100.0;
        double vMult = vertical.getValue() / 100.0;

        double velX = packet.getMotionX() / 8000.0;
        double velY = packet.getMotionY() / 8000.0;
        double velZ = packet.getMotionZ() / 8000.0;

        player.motionX = velX * hMult;
        player.motionZ = velZ * hMult;
        player.motionY = velY * vMult;
    }

    /**
     * Check if there's an enemy nearby
     */
    private boolean hasEnemyNearby(EntityPlayerSP player) {
        if (mc.theWorld == null) return false;

        double range = enemyRange.getValue();

        for (EntityPlayer entity : mc.theWorld.playerEntities) {
            if (entity == null || entity == player) continue;
            if (entity.isDead || entity.deathTime > 0) continue;

            if (Friends.isFriend(entity.getName())) continue;
            if (AntiBot.isBotForCombat(entity)) continue;

            Teams teams = Teams.getInstance();
            if (teams != null && teams.isEnabled() && teams.isTeamMate(entity)) continue;

            double dist = player.getDistanceToEntity(entity);
            if (dist <= range) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + mode.getSelected();
    }
}
