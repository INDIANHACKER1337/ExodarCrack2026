/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.movement;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.potion.Potion;

/**
 * Speed - Multiple speed modes including Verus bypasses
 *
 * Modes:
 * - Bhop: Simple bunny hop with speed multiplier
 * - VerusHop: Verus anticheat bypass
 * - VerusFHop: Verus anticheat bypass (F variant)
 * - VerusLowHop: Verus anticheat bypass with low hop
 * - VerusLowHopNew: Verus anticheat bypass with improved low hop
 */
public class Speed extends Module {

    // Mode constants
    private static final String MODE_BHOP = "Bhop";
    private static final String MODE_VERUS_HOP = "VerusHop";
    private static final String MODE_VERUS_FHOP = "VerusFHop";
    private static final String MODE_VERUS_YPORT = "VerusYPort";
    private static final String MODE_VERUS_FAST = "VerusFast";
    private static final String MODE_SOLO_LEGENDS = "SoloLegends";
    private static final String MODE_VERUS_LOWHOP_NEW = "VerusLowHop";
    private static final String MODE_INTERACT = "Interact";
    // Vulcan modes
    private static final String MODE_VULCAN_BHOP = "VulcanBHop";
    private static final String MODE_VULCAN_LOWHOP = "VulcanLowhop";
    private static final String MODE_VULCAN_YPORT = "VulcanYport";
    private static final String MODE_VULCAN_GROUND = "SoloLegends2";
    private static final String MODE_STRAFE = "Strafe";
    // Augustus modes
    private static final String MODE_LEGIT_HOP = "LegitHop";
    private static final String MODE_LEGIT_ABUSE = "LegitAbuse";
    private static final String MODE_FIXED_STRAFE = "FixedStrafe";
    private static final String MODE_GROUND_STRAFE = "GroundStrafe";
    private static final String MODE_STRAFE_SPEED = "StrafeSpeed";
    private static final String MODE_VULCAN_SPEED = "VulcanSpeed";
    private static final String MODE_NCP_LOW = "NCPLow";

    // Settings
    private final ModeSetting mode;
    private final SliderSetting speedMultiplier;
    private final SliderSetting interactSpeed;
    // Strafe settings
    private final SliderSetting strafeSpeed;
    private final TickSetting strafeHurtBoost;
    private final SliderSetting strafeBoostSpeed;
    // VulcanGround settings
    private final TickSetting vulcanGroundStrafe;
    private final TickSetting vulcanGroundHurtBoost;
    private final SliderSetting vulcanGroundBoostSpeed;

    // LegitHop settings
    private final SliderSetting legitHopMotion;
    // LegitAbuse settings
    private final SliderSetting legitAbuseSpeed;
    private final SliderSetting legitAbuseFriction;
    // FixedStrafe settings
    private final SliderSetting fixedStrafeSpeed;
    private final TickSetting fixedStrafeJumpBoost;
    // GroundStrafe settings
    private final SliderSetting groundStrafeSpeed;
    // StrafeSpeed settings
    private final SliderSetting strafeSpdSpeed;
    // VulcanSpeed settings
    private final SliderSetting vulcanSpeedBase;
    // NCPLow settings
    private final SliderSetting ncpLowSpeed;

    // State
    private float verusSpeed = 0.0f;
    private int airTicks = 0;

    public Speed() {
        super("Speed", ModuleCategory.MOVEMENT);

        this.registerSetting(new DescriptionSetting("Speed boost modes"));
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{
            MODE_BHOP, MODE_VERUS_HOP, MODE_VERUS_FHOP, MODE_VERUS_YPORT, MODE_VERUS_FAST, MODE_SOLO_LEGENDS, MODE_VERUS_LOWHOP_NEW, MODE_INTERACT,
            MODE_VULCAN_BHOP, MODE_VULCAN_LOWHOP, MODE_VULCAN_YPORT, MODE_VULCAN_GROUND, MODE_STRAFE,
            MODE_LEGIT_HOP, MODE_LEGIT_ABUSE, MODE_FIXED_STRAFE, MODE_GROUND_STRAFE, MODE_STRAFE_SPEED, MODE_VULCAN_SPEED, MODE_NCP_LOW
        }));
        this.registerSetting(speedMultiplier = new SliderSetting("Speed", 1.0, 0.5, 3.0, 0.1));
        this.registerSetting(interactSpeed = new SliderSetting("Interact Speed", 1, 1, 10, 1));
        // Strafe settings
        this.registerSetting(strafeSpeed = new SliderSetting("Strafe Speed", 0.3, 0.1, 1.0, 0.05));
        this.registerSetting(strafeHurtBoost = new TickSetting("Hurt Boost", false));
        this.registerSetting(strafeBoostSpeed = new SliderSetting("Boost Speed", 1.0, 0.1, 9.5, 0.1));
        // VulcanGround settings
        this.registerSetting(vulcanGroundStrafe = new TickSetting("Strafe", false));
        this.registerSetting(vulcanGroundHurtBoost = new TickSetting("Hurt Boost", false));
        this.registerSetting(vulcanGroundBoostSpeed = new SliderSetting("Boost Speed", 1.0, 0.1, 9.5, 0.1));

        // LegitHop settings
        this.registerSetting(legitHopMotion = new SliderSetting("Motion", 0.26, 0.1, 0.5, 0.01));
        // LegitAbuse settings
        this.registerSetting(legitAbuseSpeed = new SliderSetting("Speed", 0.22, 0.1, 0.5, 0.01));
        this.registerSetting(legitAbuseFriction = new SliderSetting("Friction", 0.98, 0.9, 1.0, 0.01));
        // FixedStrafe settings
        this.registerSetting(fixedStrafeSpeed = new SliderSetting("Strafe Speed", 0.3, 0.1, 0.6, 0.01));
        this.registerSetting(fixedStrafeJumpBoost = new TickSetting("Jump Boost", true));
        // GroundStrafe settings
        this.registerSetting(groundStrafeSpeed = new SliderSetting("Ground Speed", 0.3, 0.1, 0.6, 0.01));
        // StrafeSpeed settings
        this.registerSetting(strafeSpdSpeed = new SliderSetting("Strafe Spd", 0.28, 0.1, 0.5, 0.01));
        // VulcanSpeed settings
        this.registerSetting(vulcanSpeedBase = new SliderSetting("Base Speed", 0.29, 0.1, 0.5, 0.01));
        // NCPLow settings
        this.registerSetting(ncpLowSpeed = new SliderSetting("NCP Speed", 0.27, 0.1, 0.5, 0.01));

        // Update visibility based on mode
        mode.onChange(this::updateSettingsVisibility);
        updateSettingsVisibility();
    }

    private void updateSettingsVisibility() {
        String selected = mode.getSelected();
        speedMultiplier.setVisible(selected.equals(MODE_BHOP));
        interactSpeed.setVisible(selected.equals(MODE_INTERACT));
        // Strafe settings
        strafeSpeed.setVisible(selected.equals(MODE_STRAFE));
        strafeHurtBoost.setVisible(selected.equals(MODE_STRAFE));
        strafeBoostSpeed.setVisible(selected.equals(MODE_STRAFE));
        // VulcanGround settings
        vulcanGroundStrafe.setVisible(selected.equals(MODE_VULCAN_GROUND));
        vulcanGroundHurtBoost.setVisible(selected.equals(MODE_VULCAN_GROUND));
        vulcanGroundBoostSpeed.setVisible(selected.equals(MODE_VULCAN_GROUND));
        // LegitHop settings
        legitHopMotion.setVisible(selected.equals(MODE_LEGIT_HOP));
        // LegitAbuse settings
        legitAbuseSpeed.setVisible(selected.equals(MODE_LEGIT_ABUSE));
        legitAbuseFriction.setVisible(selected.equals(MODE_LEGIT_ABUSE));
        // FixedStrafe settings
        fixedStrafeSpeed.setVisible(selected.equals(MODE_FIXED_STRAFE));
        fixedStrafeJumpBoost.setVisible(selected.equals(MODE_FIXED_STRAFE));
        // GroundStrafe settings
        groundStrafeSpeed.setVisible(selected.equals(MODE_GROUND_STRAFE));
        // StrafeSpeed settings
        strafeSpdSpeed.setVisible(selected.equals(MODE_STRAFE_SPEED));
        // VulcanSpeed settings
        vulcanSpeedBase.setVisible(selected.equals(MODE_VULCAN_SPEED));
        // NCPLow settings
        ncpLowSpeed.setVisible(selected.equals(MODE_NCP_LOW));
    }

    @Override
    public void onEnable() {
        verusSpeed = 0.0f;
        airTicks = 0;
    }

    @Override
    public void onDisable() {
        verusSpeed = 0.0f;
        airTicks = 0;
    }

    @Override
    public void onUpdate() {
        EntityPlayerSP player = getPlayer();
        if (player == null) return;
        if (mc.currentScreen != null) return;

        // Track air ticks
        if (player.onGround) {
            airTicks = 0;
        } else {
            airTicks++;
        }

        // Skip if in liquid or on ladder
        if (player.isInWater() || player.isInLava() || player.isOnLadder()) {
            return;
        }

        String currentMode = mode.getSelected();

        switch (currentMode) {
            case MODE_BHOP:
                handleBhop(player);
                break;
            case MODE_VERUS_HOP:
                handleVerusHop(player);
                break;
            case MODE_VERUS_FHOP:
                handleVerusFHop(player);
                break;
            case MODE_VERUS_YPORT:
                handleVerusYPort(player);
                break;
            case MODE_VERUS_FAST:
                handleVerusFast(player);
                break;
            case MODE_SOLO_LEGENDS:
                handleVerusLowHop(player);
                break;
            case MODE_VERUS_LOWHOP_NEW:
                handleVerusLowHopNew(player);
                break;
            case MODE_INTERACT:
                handleInteract(player);
                break;
            case MODE_VULCAN_BHOP:
                handleVulcanBHop(player);
                break;
            case MODE_VULCAN_LOWHOP:
                handleVulcanLowhop(player);
                break;
            case MODE_VULCAN_YPORT:
                handleVulcanYport(player);
                break;
            case MODE_VULCAN_GROUND:
                handleVulcanGround(player);
                break;
            case MODE_STRAFE:
                handleStrafe(player);
                break;
            case MODE_LEGIT_HOP:
                handleLegitHop(player);
                break;
            case MODE_LEGIT_ABUSE:
                handleLegitAbuse(player);
                break;
            case MODE_FIXED_STRAFE:
                handleFixedStrafe(player);
                break;
            case MODE_GROUND_STRAFE:
                handleGroundStrafe(player);
                break;
            case MODE_STRAFE_SPEED:
                handleStrafeSpeed(player);
                break;
            case MODE_VULCAN_SPEED:
                handleVulcanSpeed(player);
                break;
            case MODE_NCP_LOW:
                handleNCPLow(player);
                break;
        }
    }

    /**
     * Simple Bhop with speed multiplier
     */
    private void handleBhop(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        if (player.onGround) {
            player.jump();

            // Apply speed boost in movement direction
            double speed = speedMultiplier.getValue() * 0.3;
            strafe(player, (float) speed);
        }
    }

    /**
     * VerusHop - Verus anticheat bypass (Hop)
     */
    private void handleVerusHop(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        if (player.onGround) {
            // Check for Speed II potion
            if (player.isPotionActive(Potion.moveSpeed) &&
                player.getActivePotionEffect(Potion.moveSpeed).getAmplifier() >= 1) {
                verusSpeed = 0.46f;
            } else {
                verusSpeed = 0.34f;
            }

            player.jump();
        } else {
            verusSpeed *= 0.98f;
        }

        strafe(player, verusSpeed);
    }

    /**
     * VerusFHop - Verus anticheat bypass (FHop)
     */
    private void handleVerusFHop(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        boolean diagonal = player.movementInput.moveForward != 0 && player.movementInput.moveStrafe != 0;

        if (player.onGround) {
            if (diagonal) {
                strafe(player, 0.4825f);
            } else {
                strafe(player, 0.535f);
            }

            player.jump();
        } else {
            if (diagonal) {
                strafe(player, 0.334f);
            } else {
                strafe(player, 0.3345f);
            }
        }
    }

    /**
     * VerusYPort - yPort mode from Rise's VerusSpeed
     * Uses block placement packets for speed boost
     */
    private void handleVerusYPort(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        // Jump handling
        if (mc.gameSettings.keyBindJump.isKeyDown() && player.onGround) {
            player.motionY = 0.42;
        }

        // Only process when not holding jump and air ticks < 2
        if (!mc.gameSettings.keyBindJump.isKeyDown() && airTicks < 2) {
            // Get speed potion amplifier
            int speedAmp = 0;
            if (player.isPotionActive(Potion.moveSpeed)) {
                speedAmp = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1;
            }

            // Send block placement packet (helps bypass)
            if (player.sendQueue != null && player.ticksExisted % 10 == 1) {
                player.sendQueue.addToSendQueue(
                    new C08PacketPlayerBlockPlacement(player.inventory.getStackInSlot(player.inventory.currentItem))
                );
            }

            // Apply speed based on state
            if (player.onGround) {
                player.motionY = 0.0;
                float groundSpeed = speedAmp > 0 ? (0.09f * (1 + speedAmp) + 0.55f) : 0.55f;
                strafe(player, groundSpeed);
            } else {
                float airSpeed = speedAmp > 0 ? (0.17f * (1 + speedAmp) + 0.46f) : 0.46f;
                strafe(player, airSpeed);
            }
        } else {
            // When holding jump
            int speedAmp = 0;
            if (player.isPotionActive(Potion.moveSpeed)) {
                speedAmp = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1;
            }
            float jumpSpeed = speedAmp > 0 ? (0.04f * (1 + speedAmp) + 0.41f) : 0.41f;
            strafe(player, jumpSpeed);
        }
    }

    /**
     * VerusFast - Fast mode from Rise's VerusSpeed
     * Uses timer manipulation and ground spoofing
     */
    private void handleVerusFast(EntityPlayerSP player) {
        if (player.movementInput.moveForward <= 0) {
            return;
        }

        if (player.onGround) {
            if (getSpeed(player) > 0.3) {
                // lastStopped = false
            }
            strafe(player, 0.41f);
            player.motionY = 0.42F;
            // Would need timer access: mc.timer.timerSpeed = 2.1F;
            airTicks = 0;
        } else {
            if (airTicks >= 10) {
                strafe(player, 0.35F);
                return;
            }

            // Speed values based on air ticks
            if (airTicks <= 1) {
                strafe(player, 0.35F);
            } else {
                float speed = 0.69F - (airTicks - 2F) * 0.019F;
                strafe(player, speed);
            }

            player.motionY = 0F;
            // Would need: mc.timer.timerSpeed = 0.9F;
            player.onGround = true;
        }
    }

    /**
     * Get current horizontal speed
     */
    private double getSpeed(EntityPlayerSP player) {
        return Math.hypot(player.motionX, player.motionZ);
    }

    /**
     * SoloLegends - Low hop speed (formerly VerusLowHop)
     */
    private void handleVerusLowHop(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        if (player.onGround) {
            // Check for Speed II potion
            if (player.isPotionActive(Potion.moveSpeed) &&
                player.getActivePotionEffect(Potion.moveSpeed).getAmplifier() >= 1) {
                verusSpeed = 0.5f;
            } else {
                verusSpeed = 0.36f;
            }

            player.jump();
        } else {
            // Low hop - reduce Y motion on first air tick
            if (airTicks <= 1) {
                player.motionY = -0.09800000190734863;
            }

            verusSpeed *= 0.98f;
        }

        strafe(player, verusSpeed);
    }

    /**
     * VerusLowHopNew - Improved Verus low hop with potion checks
     */
    private void handleVerusLowHopNew(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        if (player.onGround) {
            player.jump();

            // Check for Speed potion effect
            if (player.isPotionActive(Potion.moveSpeed)) {
                int amplifier = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier();

                if (amplifier >= 2) {
                    verusSpeed = 0.7f;
                } else if (amplifier == 1) {
                    verusSpeed = 0.55f;
                } else {
                    verusSpeed = 0.33f;
                }
            } else {
                verusSpeed = 0.33f;
            }

            // Check for Slowness potion effect
            if (player.isPotionActive(Potion.moveSlowdown) &&
                player.getActivePotionEffect(Potion.moveSlowdown).getAmplifier() == 1) {
                verusSpeed = 0.3f;
            }
        } else {
            // Low hop - reduce Y motion on first air tick
            if (airTicks <= 1) {
                player.motionY = -0.09800000190734863;
            }

            verusSpeed *= 0.99f;
        }

        strafe(player, verusSpeed);
    }

    /**
     * InteractSpeed - Uses block placement packets to speed up movement
     * From Rise 6.2.4
     */
    private void handleInteract(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        int speed = (int) interactSpeed.getValue();

        for (int i = 0; i < speed; i++) {
            // Send block placement packet
            if (player.sendQueue != null) {
                player.sendQueue.addToSendQueue(
                    new C08PacketPlayerBlockPlacement(player.inventory.getStackInSlot(player.inventory.currentItem))
                );
                player.sendQueue.addToSendQueue(
                    new C03PacketPlayer.C04PacketPlayerPosition(player.posX, player.posY, player.posZ, player.onGround)
                );
            }

            // Store position
            double posX = player.posX;
            double posY = player.posY;
            double posZ = player.posZ;

            // Trigger living update (movement tick)
            player.onLivingUpdate();

            // Restore position (the packets already sent the movement)
            player.posX = posX;
            player.posY = posY;
            player.posZ = posZ;
        }
    }

    // ===== VULCAN MODES (from Rise 6.2.4) =====

    private int jumpCount = 0;
    private int stage = 0;
    private double moveSpeed = 0;
    private int ticksSinceVelocity = 100;

    /**
     * VulcanBHop - Bunny hop with specific speeds per air tick
     */
    private void handleVulcanBHop(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        // Track velocity
        if (player.hurtTime > 0) ticksSinceVelocity = 0;
        else ticksSinceVelocity++;

        // Check if recently took velocity
        if (player.hurtTime > 0 || ticksSinceVelocity < 50) {
            strafe(player, (float) getSpeedHypot(player));
            return;
        }

        // Ensure minimum speed
        if (getSpeedHypot(player) < 0.22) {
            strafe(player, 0.22f);
        }

        int speedAmp = getSpeedAmplifier(player);

        switch (airTicks) {
            case 0: // Ground - Jump
                player.jump();
                jumpCount++;
                if (speedAmp > 0 && getSpeedHypot(player) < getSpeedWithPot(0.487, speedAmp)) {
                    strafe(player, (float) getSpeedWithPot(0.487, speedAmp));
                } else if (getSpeedHypot(player) < 0.487) {
                    strafe(player, 0.487f);
                }
                break;

            case 1: // First air tick
                if (speedAmp >= 2 && getSpeedHypot(player) < getSpeedWithPot(0.487, speedAmp)) {
                    strafe(player, (float) getSpeedWithPot(0.487, speedAmp));
                } else if (speedAmp == 1 && getSpeedHypot(player) < getSpeedWithPot(0.41, speedAmp)) {
                    strafe(player, (float) getSpeedWithPot(0.41, speedAmp));
                } else if (getSpeedHypot(player) < 0.3355) {
                    strafe(player, 0.3355f);
                }
                break;

            case 4: // Motion Y prediction
                player.motionY = predictedMotion(player.motionY, 2);
                break;

            case 9: // Pre-landing
                if (speedAmp >= 2 && getSpeedHypot(player) < getSpeedWithPot(0.46, speedAmp)) {
                    strafe(player, (float) getSpeedWithPot(0.46, speedAmp));
                } else if (speedAmp == 1 && getSpeedHypot(player) < getSpeedWithPot(0.385, speedAmp)) {
                    strafe(player, (float) getSpeedWithPot(0.385, speedAmp));
                } else if (getSpeedHypot(player) < 0.299) {
                    strafe(player, 0.299f);
                }
                break;
        }
    }

    /**
     * VulcanLowhop - Low hop with motion prediction
     */
    private void handleVulcanLowhop(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        // Track velocity
        if (player.hurtTime > 0) ticksSinceVelocity = 0;
        else ticksSinceVelocity++;

        if (player.hurtTime > 0 || ticksSinceVelocity < 40) {
            strafe(player, (float) getSpeedHypot(player));
            return;
        }

        if (getSpeedHypot(player) < 0.22) {
            strafe(player, 0.22f);
        }

        int speedAmp = getSpeedAmplifier(player);

        switch (airTicks) {
            case 0:
                player.jump();
                jumpCount++;
                if (speedAmp > 0 && ticksSinceVelocity > 11) {
                    strafe(player, (float) getSpeedWithPot(0.485, speedAmp));
                } else if (ticksSinceVelocity > 11) {
                    strafe(player, 0.485f);
                } else {
                    strafe(player, (float) getSpeedHypot(player));
                }
                break;

            case 1:
                strafe(player, (float) getSpeedHypot(player));
                break;

            case 2:
                if ((jumpCount % 4 != 1) && !player.isCollidedVertically) {
                    player.motionY = predictedMotion(player.motionY, 2);
                }
                break;

            case 4:
                if (jumpCount % 4 == 1 || player.isCollidedVertically) {
                    player.motionY = predictedMotion(player.motionY, 4);
                }
                break;

            case 5:
                if (jumpCount % 4 == 1) {
                    strafe(player, (float) getSpeedHypot(player));
                }
                break;

            case 8:
            case 9:
                strafe(player, (float) getSpeedHypot(player));
                break;
        }
    }

    /**
     * VulcanYport - Y-port with burst movement
     */
    private void handleVulcanYport(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        // Track velocity
        if (player.hurtTime > 0) ticksSinceVelocity = 0;
        else ticksSinceVelocity++;

        if (player.hurtTime > 0 || ticksSinceVelocity < 40) {
            strafe(player, (float) getSpeedHypot(player));
            return;
        }

        if (getSpeedHypot(player) < 0.22) {
            strafe(player, 0.22f);
        }

        int speedAmp = getSpeedAmplifier(player);

        // Y-port motion
        if (airTicks == 0) {
            strafe(player, (float) getSpeedHypot(player));
            player.motionY = -0.05;
        } else if (airTicks == 1) {
            strafe(player, (float) getSpeedHypot(player));
            player.motionY = -0.22319999363422365;
        } else if (airTicks == 2 || airTicks == 3) {
            strafe(player, (float) getSpeedHypot(player));
        }

        if (player.onGround) {
            player.jump();
            if (speedAmp > 0 && ticksSinceVelocity > 11) {
                strafe(player, (float) getSpeedWithPot(0.433, speedAmp));
            } else if (ticksSinceVelocity > 11) {
                strafe(player, 0.433f);
            } else {
                strafe(player, (float) getSpeedHypot(player));
            }
        }

        if (airTicks == 1) {
            if (speedAmp > 0 && ticksSinceVelocity > 11) {
                strafe(player, (float) getSpeedWithPot(0.308, speedAmp));
            } else if (ticksSinceVelocity > 11) {
                strafe(player, 0.308f);
            }
        }

        if (airTicks == 2) {
            if (speedAmp > 0 && ticksSinceVelocity > 11) {
                strafe(player, (float) (0.053 * (1 + speedAmp) + 0.3035));
            } else if (ticksSinceVelocity > 11) {
                strafe(player, 0.3035f);
            }
        }
    }

    /**
     * VulcanGround - Ground-based speed with stage system
     * Strafe option: when enabled and you press space, combines ground speed + strafe + jump
     */
    private void handleVulcanGround(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        boolean useStrafe = vulcanGroundStrafe.isEnabled() && mc.gameSettings.keyBindJump.isKeyDown();

        // Hurt boost - apply when taking damage
        if (vulcanGroundHurtBoost.isEnabled() && player.hurtTime == 9) {
            strafe(player, (float) vulcanGroundBoostSpeed.getValue());
            return;
        }

        if (player.onGround) {
            double speed = getSpeedHypot(player);
            int speedAmp = getSpeedAmplifier(player);
            boolean boost = speedAmp >= 2;

            switch (stage) {
                case 1:
                    moveSpeed = 0.58f;
                    speed = boost ? speed + 0.2 : 0.487;
                    break;

                case 2:
                    speed = boost ? speed * 0.71 : 0.197;
                    moveSpeed -= 0.0784f;
                    break;

                default:
                    stage = 0;
                    speed /= boost ? 0.64 : 0.58;
                    break;
            }

            strafe(player, (float) speed);
            stage++;

            // Strafe + jump when user presses space with strafe enabled
            if (useStrafe) {
                player.jump();
                strafe(player, (float) speed);
            }
        } else {
            stage = 0;
            // Continue strafe while in air if strafe enabled and jumping
            if (useStrafe) {
                strafe(player);
            }
        }
    }

    /**
     * Strafe - Apply strafe with configurable speed
     * No auto-jump - manual control
     */
    private void handleStrafe(EntityPlayerSP player) {
        // Check if Flight is enabled - don't interfere with it when in air
        boolean flightEnabled = false;
        try {
            io.github.exodar.module.Module flightModule = io.github.exodar.Main.getModuleManager().getModuleByName("Flight");
            if (flightModule != null && flightModule.isEnabled()) {
                flightEnabled = true;
            }
        } catch (Exception ignored) {}

        // If Flight is active and we're in air, don't apply strafe (let Flight control)
        if (flightEnabled && !player.onGround) {
            return;
        }

        if (!isMoving(player)) {
            // Stop movement when not pressing keys (on ground only if Flight could be used)
            player.motionX = 0;
            player.motionZ = 0;
            return;
        }

        // Determine speed to use
        float speed = (float) strafeSpeed.getValue();

        // Hurt boost - apply boost speed when taking damage
        if (strafeHurtBoost.isEnabled() && player.hurtTime == 9) {
            speed = (float) strafeBoostSpeed.getValue();
        }

        // Apply strafe with configured speed
        strafe(player, speed);
    }

    // ===== AUGUSTUS MODES =====

    /**
     * LegitHop - Simple bhop based on motion (from Augustus)
     * Jumps when on ground and applies strafe motion
     */
    private void handleLegitHop(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        if (player.onGround) {
            player.jump();
        }

        double motion = legitHopMotion.getValue();
        if (getSpeedHypot(player) < motion) {
            strafe(player, (float) motion);
        }
    }

    private int legitAbuseTicks = 0;
    private double legitAbuseMotion = 0;

    /**
     * LegitAbuse - Uses tick counter and friction for speed (from Augustus)
     * More complex motion handling with decay
     */
    private void handleLegitAbuse(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        if (player.onGround) {
            player.jump();
            legitAbuseTicks = 0;
            legitAbuseMotion = legitAbuseSpeed.getValue();
        } else {
            legitAbuseTicks++;
            // Apply friction decay
            legitAbuseMotion *= legitAbuseFriction.getValue();
        }

        if (legitAbuseMotion > 0.1) {
            strafe(player, (float) legitAbuseMotion);
        }
    }

    /**
     * FixedStrafe - Fixed strafe speed with optional jump boost (from Augustus)
     * Consistent speed regardless of situation
     */
    private void handleFixedStrafe(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        if (player.onGround) {
            player.jump();

            // Apply extra boost on jump if enabled
            if (fixedStrafeJumpBoost.isEnabled()) {
                strafe(player, (float) (fixedStrafeSpeed.getValue() * 1.2));
            } else {
                strafe(player, (float) fixedStrafeSpeed.getValue());
            }
        } else {
            strafe(player, (float) fixedStrafeSpeed.getValue());
        }
    }

    /**
     * GroundStrafe - Ground-based strafe movement (from Augustus)
     * Different behavior on ground vs air
     */
    private void handleGroundStrafe(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        if (player.onGround) {
            player.jump();
            strafe(player, (float) groundStrafeSpeed.getValue());
        } else {
            // Maintain momentum in air
            strafe(player, (float) Math.max(getSpeedHypot(player), groundStrafeSpeed.getValue() * 0.8));
        }
    }

    private int strafeSpeedTicks = 0;

    /**
     * StrafeSpeed - Strafe-based speed with air tick management (from Augustus)
     * Adjusts speed based on time in air
     */
    private void handleStrafeSpeed(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        if (player.onGround) {
            player.jump();
            strafeSpeedTicks = 0;
            strafe(player, (float) (strafeSpdSpeed.getValue() * 1.5)); // Boost on ground
        } else {
            strafeSpeedTicks++;
            // Decay speed over air time
            double speed = strafeSpdSpeed.getValue() * Math.max(0.7, 1.0 - (strafeSpeedTicks * 0.02));
            strafe(player, (float) speed);
        }
    }

    private int vulcanSpeedStage = 0;

    /**
     * VulcanSpeed - Stage-based speed system (from Augustus)
     * Different speeds based on jump stage
     */
    private void handleVulcanSpeed(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        int speedAmp = getSpeedAmplifier(player);
        double baseSpeed = vulcanSpeedBase.getValue();

        if (player.onGround) {
            player.jump();
            vulcanSpeedStage++;

            // Alternate between two speeds for bypass
            if (vulcanSpeedStage % 2 == 0) {
                strafe(player, (float) (baseSpeed + (speedAmp * 0.05)));
            } else {
                strafe(player, (float) (baseSpeed * 1.1 + (speedAmp * 0.05)));
            }
        } else {
            // Different speed per air tick
            switch (airTicks) {
                case 1:
                    strafe(player, (float) (baseSpeed * 0.95));
                    break;
                case 2:
                case 3:
                    strafe(player, (float) (baseSpeed * 0.9));
                    break;
                default:
                    if (airTicks > 3) {
                        strafe(player, (float) (baseSpeed * 0.85));
                    }
                    break;
            }
        }
    }

    /**
     * NCPLow - NCP anticheat bypass with low hop (from Augustus)
     * Uses ground packet spoofing for bypass
     */
    private void handleNCPLow(EntityPlayerSP player) {
        if (!isMoving(player)) return;

        double speed = ncpLowSpeed.getValue();
        int speedAmp = getSpeedAmplifier(player);

        if (speedAmp > 0) {
            speed += 0.04 * speedAmp;
        }

        if (player.onGround) {
            player.jump();
            strafe(player, (float) (speed * 1.4));
        } else {
            // Low hop - cut Y motion early
            if (airTicks == 1 && player.motionY > 0.2) {
                player.motionY = 0.2;
            }

            // Send ground packet to bypass (NCP specific)
            if (airTicks == 2 && player.sendQueue != null) {
                player.sendQueue.addToSendQueue(new C03PacketPlayer(true));
            }

            strafe(player, (float) speed);
        }
    }

    // ===== HELPER METHODS =====

    private double predictedMotion(double motionY, int ticks) {
        for (int i = 0; i < ticks; i++) {
            motionY = (motionY - 0.08) * 0.98;
        }
        return motionY;
    }

    private double getSpeedWithPot(double base, int amplifier) {
        return 0.06 * (1 + amplifier) + base;
    }

    private int getSpeedAmplifier(EntityPlayerSP player) {
        if (player.isPotionActive(Potion.moveSpeed)) {
            return player.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1;
        }
        return 0;
    }

    private double getSpeedHypot(EntityPlayerSP player) {
        return Math.hypot(player.motionX, player.motionZ);
    }

    /**
     * Check if player is pressing movement keys (WASD)
     */
    private boolean isMoving(EntityPlayerSP player) {
        return player.movementInput.moveForward != 0 || player.movementInput.moveStrafe != 0;
    }

    /**
     * Strafe - Apply speed in the player's movement direction
     * Uses current horizontal speed
     */
    private void strafe(EntityPlayerSP player) {
        strafe(player, (float) getSpeedHypot(player));
    }

    /**
     * Strafe - Apply speed in the player's movement direction
     * Works for all movement directions (WASD)
     *
     * @param player The player
     * @param speed The speed to apply
     */
    private void strafe(EntityPlayerSP player, float speed) {
        if (speed <= 0) return;

        float forward = player.movementInput.moveForward;
        float strafe = player.movementInput.moveStrafe;

        // Normalize if moving diagonally
        if (forward != 0 && strafe != 0) {
            forward *= 0.7071067811865476f; // 1/sqrt(2)
            strafe *= 0.7071067811865476f;
        }

        // Calculate yaw in radians
        float yaw = player.rotationYaw;
        double yawRadians = Math.toRadians(yaw);

        // Calculate motion based on movement direction and yaw
        double motionX = (forward * -Math.sin(yawRadians) + strafe * Math.cos(yawRadians)) * speed;
        double motionZ = (forward * Math.cos(yawRadians) + strafe * Math.sin(yawRadians)) * speed;

        player.motionX = motionX;
        player.motionZ = motionZ;
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + mode.getSelected();
    }
}
