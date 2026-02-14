/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.util;

import io.github.exodar.event.EventBus;
import io.github.exodar.event.PreMotionEvent;
import io.github.exodar.event.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.C03PacketPlayer;

/**
 * RotationManager - Manages silent rotations for modules
 * Allows modules to set a "server-side" rotation that differs from visual rotation
 */
public class RotationManager {

    private static RotationManager instance;

    private Float silentYaw = null;
    private Float silentPitch = null;
    private boolean active = false;
    private int ticksRemaining = 0;

    // For restoring after attack
    private float lastServerYaw = 0;
    private float lastServerPitch = 0;

    private Minecraft mc = Minecraft.getMinecraft();

    private RotationManager() {
        EventBus.register(this);
    }

    public static RotationManager getInstance() {
        if (instance == null) {
            instance = new RotationManager();
        }
        return instance;
    }

    /**
     * Set silent rotation - will be used for the next N ticks
     * @param yaw Target yaw
     * @param pitch Target pitch
     * @param ticks How many ticks to maintain this rotation (0 = one packet only)
     */
    public void setSilentRotation(float yaw, float pitch, int ticks) {
        this.silentYaw = yaw;
        this.silentPitch = pitch;
        this.ticksRemaining = ticks;
        this.active = true;
    }

    /**
     * Set silent rotation for a single attack
     */
    public void setSilentRotation(float yaw, float pitch) {
        setSilentRotation(yaw, pitch, 1);
    }

    /**
     * Clear silent rotation
     */
    public void clearSilentRotation() {
        this.silentYaw = null;
        this.silentPitch = null;
        this.active = false;
        this.ticksRemaining = 0;
    }

    /**
     * Check if silent rotation is active
     */
    public boolean isActive() {
        return active && silentYaw != null && silentPitch != null;
    }

    /**
     * Get the yaw to use for server (silent or real)
     */
    public float getServerYaw() {
        if (isActive()) {
            return silentYaw;
        }
        return mc.thePlayer != null ? mc.thePlayer.rotationYaw : 0;
    }

    /**
     * Get the pitch to use for server (silent or real)
     */
    public float getServerPitch() {
        if (isActive()) {
            return silentPitch;
        }
        return mc.thePlayer != null ? mc.thePlayer.rotationPitch : 0;
    }

    /**
     * Called every tick to decrement timer
     */
    public void onTick() {
        if (active && ticksRemaining > 0) {
            ticksRemaining--;
            if (ticksRemaining <= 0) {
                clearSilentRotation();
            }
        }
    }

    /**
     * Apply silent rotation to a packet (for modules that send packets manually)
     */
    public C03PacketPlayer.C06PacketPlayerPosLook createSilentPacket(
            double x, double y, double z, boolean onGround) {
        float yaw = isActive() ? silentYaw : mc.thePlayer.rotationYaw;
        float pitch = isActive() ? silentPitch : mc.thePlayer.rotationPitch;
        return new C03PacketPlayer.C06PacketPlayerPosLook(x, y, z, yaw, pitch, onGround);
    }

    /**
     * Apply silent rotation to a look packet
     */
    public C03PacketPlayer.C05PacketPlayerLook createSilentLookPacket(boolean onGround) {
        float yaw = isActive() ? silentYaw : mc.thePlayer.rotationYaw;
        float pitch = isActive() ? silentPitch : mc.thePlayer.rotationPitch;
        return new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, onGround);
    }

    /**
     * Get silent yaw (or null if not active)
     */
    public Float getSilentYaw() {
        return silentYaw;
    }

    /**
     * Get silent pitch (or null if not active)
     */
    public Float getSilentPitch() {
        return silentPitch;
    }

    /**
     * Store current player rotation as last server rotation
     */
    public void updateLastServerRotation() {
        if (mc.thePlayer != null) {
            lastServerYaw = mc.thePlayer.rotationYaw;
            lastServerPitch = mc.thePlayer.rotationPitch;
        }
    }

    public float getLastServerYaw() {
        return lastServerYaw;
    }

    public float getLastServerPitch() {
        return lastServerPitch;
    }
}
