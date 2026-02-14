/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * AntiCrash - Protects against packet flood/spam attacks and crash exploits
 *
 * Protection against:
 * - Packet spam/flood (SpawnMob, DestroyEntities, CustomPayload, Teams, Particles)
 * - Explosion crash (S27PacketExplosion with huge values)
 * - Position crash (S08PacketPlayerPosLook with huge values)
 * - Invalid coordinate exploits
 */
public class AntiCrash extends Module {

    private static AntiCrash instance;

    // ============ EXPLOIT PROTECTION ============
    private final TickSetting blockExploits;
    private final TickSetting blockExplosionCrash;
    private final TickSetting blockPositionCrash;
    private final TickSetting blockInvalidCoords;

    // ============ SPAM PROTECTION ============
    private final TickSetting blockSpawnMob;
    private final SliderSetting maxSpawnMob;

    private final TickSetting blockDestroyEntities;
    private final SliderSetting maxDestroyEntities;

    private final TickSetting blockCustomPayload;
    private final SliderSetting maxCustomPayload;

    private final TickSetting blockTeams;
    private final SliderSetting maxTeams;

    private final TickSetting blockParticles;
    private final SliderSetting maxParticles;

    private final TickSetting blockExplosionSpam;
    private final SliderSetting maxExplosions;

    private final TickSetting notifyOnBlock;

    // Packet counters (reset every second)
    private final Map<String, Integer> packetCounts = new HashMap<>();
    private long lastResetTime = 0;
    private int totalBlocked = 0;

    // Thresholds for exploit detection
    private static final double MAX_VALID_COORD = 30000000.0; // MC world border
    private static final double CRASH_THRESHOLD = Double.MAX_VALUE / 2; // Catch MAX_VALUE and half

    public AntiCrash() {
        super("AntiCrash", ModuleCategory.MISC);
        instance = this;

        // ============ EXPLOIT PROTECTION ============
        this.registerSetting(new DescriptionSetting("--- Exploit Protection ---"));
        this.registerSetting(blockExploits = new TickSetting("Block Crash Exploits", true));
        this.registerSetting(blockExplosionCrash = new TickSetting("Block Explosion Crash", true));
        this.registerSetting(blockPositionCrash = new TickSetting("Block Position Crash", true));
        this.registerSetting(blockInvalidCoords = new TickSetting("Block Invalid Coords", true));

        // ============ SPAM PROTECTION ============
        this.registerSetting(new DescriptionSetting("--- Spam Protection ---"));

        // SpawnMob protection
        this.registerSetting(blockSpawnMob = new TickSetting("Block SpawnMob Spam", true));
        this.registerSetting(maxSpawnMob = new SliderSetting("Max SpawnMob/sec", 50, 10, 200, 10));

        // DestroyEntities protection
        this.registerSetting(blockDestroyEntities = new TickSetting("Block Destroy Spam", true));
        this.registerSetting(maxDestroyEntities = new SliderSetting("Max Destroy/sec", 100, 20, 500, 10));

        // CustomPayload protection
        this.registerSetting(blockCustomPayload = new TickSetting("Block Payload Spam", true));
        this.registerSetting(maxCustomPayload = new SliderSetting("Max Payload/sec", 200, 50, 1000, 10));

        // Teams/Scoreboard protection
        this.registerSetting(blockTeams = new TickSetting("Block Teams Spam", true));
        this.registerSetting(maxTeams = new SliderSetting("Max Teams/sec", 300, 50, 1000, 10));

        // Particles protection
        this.registerSetting(blockParticles = new TickSetting("Block Particle Spam", true));
        this.registerSetting(maxParticles = new SliderSetting("Max Particles/sec", 500, 100, 2000, 50));

        // Explosion spam protection
        this.registerSetting(blockExplosionSpam = new TickSetting("Block Explosion Spam", true));
        this.registerSetting(maxExplosions = new SliderSetting("Max Explosions/sec", 20, 5, 100, 5));

        // Notifications
        this.registerSetting(new DescriptionSetting("--- Notifications ---"));
        this.registerSetting(notifyOnBlock = new TickSetting("Notify on Block", true));
    }

    @Override
    public void onEnable() {
        packetCounts.clear();
        lastResetTime = System.currentTimeMillis();
        totalBlocked = 0;
    }

    @Override
    public void onDisable() {
        packetCounts.clear();
    }

    @Override
    public boolean onReceivePacket(Object packet) {
        if (!enabled) return true;
        if (!(packet instanceof Packet)) return true;

        // ============ EXPLOIT PROTECTION ============
        if (blockExploits.isEnabled()) {

            // Explosion crash protection
            if (packet instanceof S27PacketExplosion && blockExplosionCrash.isEnabled()) {
                S27PacketExplosion explosion = (S27PacketExplosion) packet;
                if (isInvalidCoord(explosion.getX()) ||
                    isInvalidCoord(explosion.getY()) ||
                    isInvalidCoord(explosion.getZ()) ||
                    isInvalidFloat(explosion.getStrength()) ||
                    isInvalidCoord(explosion.func_149149_c()) || // motionX
                    isInvalidCoord(explosion.func_149144_d()) || // motionY
                    isInvalidCoord(explosion.func_149147_e())) { // motionZ
                    totalBlocked++;
                    if (notifyOnBlock.isEnabled()) {
                        io.github.exodar.ui.ModuleNotification.alert("Blocked: Explosion crash exploit");
                    }
                    return false;
                }
            }

            // Position crash protection
            if (packet instanceof S08PacketPlayerPosLook && blockPositionCrash.isEnabled()) {
                S08PacketPlayerPosLook pos = (S08PacketPlayerPosLook) packet;
                if (isInvalidCoord(pos.getX()) ||
                    isInvalidCoord(pos.getY()) ||
                    isInvalidCoord(pos.getZ()) ||
                    isInvalidFloat(pos.getYaw()) ||
                    isInvalidFloat(pos.getPitch())) {
                    totalBlocked++;
                    if (notifyOnBlock.isEnabled()) {
                        io.github.exodar.ui.ModuleNotification.alert("Blocked: Position crash exploit");
                    }
                    return false;
                }
            }

            // Particle crash protection (invalid coords)
            if (packet instanceof S2APacketParticles && blockInvalidCoords.isEnabled()) {
                S2APacketParticles particles = (S2APacketParticles) packet;
                if (isInvalidCoord(particles.getXCoordinate()) ||
                    isInvalidCoord(particles.getYCoordinate()) ||
                    isInvalidCoord(particles.getZCoordinate())) {
                    totalBlocked++;
                    if (notifyOnBlock.isEnabled()) {
                        io.github.exodar.ui.ModuleNotification.alert("Blocked: Invalid particle coords");
                    }
                    return false;
                }
            }
        }

        // ============ SPAM PROTECTION ============
        // Reset counters every second
        long now = System.currentTimeMillis();
        if (now - lastResetTime >= 1000) {
            packetCounts.clear();
            lastResetTime = now;
        }

        String packetType = null;
        int maxAllowed = 0;
        boolean shouldBlock = false;

        // Check SpawnMob
        if (packet instanceof S0FPacketSpawnMob && blockSpawnMob.isEnabled()) {
            packetType = "SpawnMob";
            maxAllowed = (int) maxSpawnMob.getValue();
        }
        // Check DestroyEntities
        else if (packet instanceof S13PacketDestroyEntities && blockDestroyEntities.isEnabled()) {
            packetType = "DestroyEntities";
            maxAllowed = (int) maxDestroyEntities.getValue();
        }
        // Check CustomPayload
        else if (packet instanceof S3FPacketCustomPayload && blockCustomPayload.isEnabled()) {
            packetType = "CustomPayload";
            maxAllowed = (int) maxCustomPayload.getValue();
        }
        // Check Teams
        else if (packet instanceof S3EPacketTeams && blockTeams.isEnabled()) {
            packetType = "Teams";
            maxAllowed = (int) maxTeams.getValue();
        }
        // Check Particles
        else if (packet instanceof S2APacketParticles && blockParticles.isEnabled()) {
            packetType = "Particles";
            maxAllowed = (int) maxParticles.getValue();
        }
        // Check Explosions
        else if (packet instanceof S27PacketExplosion && blockExplosionSpam.isEnabled()) {
            packetType = "Explosions";
            maxAllowed = (int) maxExplosions.getValue();
        }

        // If this packet type is being monitored for spam
        if (packetType != null) {
            int count = packetCounts.getOrDefault(packetType, 0) + 1;
            packetCounts.put(packetType, count);

            // Check if over limit
            if (count > maxAllowed) {
                totalBlocked++;
                shouldBlock = true;

                // Notify only once per type per second (when first exceeding)
                if (count == maxAllowed + 1 && notifyOnBlock.isEnabled()) {
                    io.github.exodar.ui.ModuleNotification.warning("Packet anomaly: " + packetType + " flood");
                }
            }
        }

        // Return false to cancel packet, true to allow
        return !shouldBlock;
    }

    /**
     * Check if a coordinate value is invalid (too large, NaN, or Infinity)
     */
    private boolean isInvalidCoord(double value) {
        return Double.isNaN(value) ||
               Double.isInfinite(value) ||
               Math.abs(value) > CRASH_THRESHOLD ||
               Math.abs(value) > MAX_VALID_COORD;
    }

    /**
     * Check if a float value is invalid
     */
    private boolean isInvalidFloat(float value) {
        return Float.isNaN(value) ||
               Float.isInfinite(value) ||
               Math.abs(value) > Float.MAX_VALUE / 2;
    }

    @Override
    public String getDisplaySuffix() {
        if (totalBlocked > 0) {
            return " §c" + totalBlocked;
        }
        return "";
    }

    public static AntiCrash getInstance() {
        return instance;
    }
}
