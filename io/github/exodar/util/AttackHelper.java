/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.util;

import io.github.exodar.module.modules.combat.AimAssist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;

/**
 * AttackHelper - Utility for attacking with silent aim support
 * Sends rotation packet before attack to ensure server accepts the hit
 */
public class AttackHelper {

    private static Minecraft mc = Minecraft.getMinecraft();

    /**
     * Attack an entity with silent aim support
     * If RotationManager has a silent rotation, sends that rotation before attacking
     *
     * @param target The entity to attack
     * @param swing Whether to swing the arm visually
     */
    public static void attackEntity(Entity target, boolean swing) {
        if (mc.thePlayer == null || target == null) return;

        EntityPlayerSP player = mc.thePlayer;

        // Check RotationManager for silent rotation
        float[] silentRot = null;
        RotationManager rotManager = RotationManager.getInstance();
        if (rotManager.isActive()) {
            Float yaw = rotManager.getSilentYaw();
            Float pitch = rotManager.getSilentPitch();
            if (yaw != null && pitch != null) {
                silentRot = new float[]{yaw, pitch};
            }
        }

        // If we have a silent rotation, send look packet first
        if (silentRot != null) {
            sendLookPacket(silentRot[0], silentRot[1], player.onGround);
        }

        // Swing arm (visual only)
        if (swing) {
            player.swingItem();
        }

        // Send attack packet
        if (mc.playerController != null) {
            mc.playerController.attackEntity(player, target);
        }
    }

    /**
     * Attack with silent rotation to specific yaw/pitch
     *
     * @param target The entity to attack
     * @param yaw Silent yaw
     * @param pitch Silent pitch
     * @param swing Whether to swing
     */
    public static void attackEntitySilent(Entity target, float yaw, float pitch, boolean swing) {
        if (mc.thePlayer == null || target == null) return;

        EntityPlayerSP player = mc.thePlayer;

        // Send look packet with silent rotation
        sendLookPacket(yaw, pitch, player.onGround);

        // Swing
        if (swing) {
            player.swingItem();
        }

        // Attack
        if (mc.playerController != null) {
            mc.playerController.attackEntity(player, target);
        }
    }

    /**
     * Attack without swing (for autoclickers that handle swing separately)
     */
    public static void attackEntityNoSwing(Entity target) {
        attackEntity(target, false);
    }

    /**
     * Send a look packet with specified rotation
     */
    public static void sendLookPacket(float yaw, float pitch, boolean onGround) {
        if (mc.thePlayer != null && mc.thePlayer.sendQueue != null) {
            mc.thePlayer.sendQueue.addToSendQueue(
                new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, onGround)
            );
        }
    }

    /**
     * Send a position+look packet with silent rotation
     */
    public static void sendPosLookPacket(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        if (mc.thePlayer != null && mc.thePlayer.sendQueue != null) {
            mc.thePlayer.sendQueue.addToSendQueue(
                new C03PacketPlayer.C06PacketPlayerPosLook(x, y, z, yaw, pitch, onGround)
            );
        }
    }

    /**
     * Calculate rotations to an entity (convenience method)
     */
    public static float[] getRotationsToEntity(EntityPlayer target) {
        if (mc.thePlayer == null || target == null) return new float[]{0, 0};
        return AimAssist.getRotationsToEntity(mc.thePlayer, target);
    }
}
