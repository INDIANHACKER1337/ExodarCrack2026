/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat.autoblock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;

/**
 * Base interface for AutoBlock modes
 */
public interface AutoBlockMode {

    /**
     * Called when the mode is enabled
     */
    void onEnable();

    /**
     * Called when the mode is disabled
     */
    void onDisable();

    /**
     * Main loop tick - called every 1ms
     */
    void onTick(EntityPlayerSP player, EntityLivingBase target, long now);

    /**
     * Called when an attack packet is detected
     * @return true to allow packet, false to cancel
     */
    boolean onAttack(Packet<?> attackPacket);

    /**
     * Called when AutoClicker notifies an attack
     */
    void onNotifyAttack();

    /**
     * Check if should intercept this packet
     * @return true to allow packet, false to queue/cancel
     */
    boolean shouldSendPacket(Packet<?> packet);

    /**
     * Get current state name for display
     */
    String getStateName();

    /**
     * Get queue size for display (0 if not applicable)
     */
    int getQueueSize();

    /**
     * Check if currently blocking
     */
    boolean isBlocking();

    /**
     * Check if currently lagging packets
     */
    boolean isLagging();
}
